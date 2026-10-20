package com.example.theme.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.ClipboardItemEntity
import com.example.data.ImportedFontEntity
import com.example.data.PersonalWordEntity
import com.example.data.StudioDatabase
import com.example.data.ThemeEntity
import com.example.theme.model.DefaultThemes
import com.example.theme.model.KeyboardTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GlobalKeyboardSettings(
    val activeThemeId: String = DefaultThemes.TOXIC_EMERALD_CORE.id,
    val defaultLanguage: String = "en", // "en" or "bn"
    val enableSuggestions: Boolean = true,
    val enablePersonalDictionaryLearning: Boolean = true,
    val enableClipboardHistory: Boolean = false, // Opt-in for privacy
    val highContrastOverride: Boolean = false,
    val readableKeyLabelsBoost: Boolean = false,
    val reducedMotionOverride: Boolean = false,
    val globalSoundEnabled: Boolean = false,
    val globalVibrationEnabled: Boolean = true,
    val globalVibrationStrengthMs: Int = 18
)

/**
 * Process-wide live state holder that bridges the Studio UI and ToxicInputMethodService
 * so applying a theme or toggling a keyboard setting updates the real system keyboard immediately.
 */
object ActiveKeyboardBridge {
    private val _activeTheme = MutableStateFlow(DefaultThemes.TOXIC_EMERALD_CORE)
    val activeTheme: StateFlow<KeyboardTheme> = _activeTheme.asStateFlow()

    private val _globalSettings = MutableStateFlow(GlobalKeyboardSettings())
    val globalSettings: StateFlow<GlobalKeyboardSettings> = _globalSettings.asStateFlow()

    fun publishTheme(theme: KeyboardTheme) {
        _activeTheme.value = theme.validated()
    }

    fun publishSettings(settings: GlobalKeyboardSettings) {
        _globalSettings.value = settings
    }
}

class ThemeRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db by lazy { StudioDatabase.getInstance(appContext) }
    private val dao by lazy { db.studioDao() }
    private val prefs: SharedPreferences? = try {
        appContext.getSharedPreferences("toxic_keyboard_prefs", Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val themeAdapter = moshi.adapter(KeyboardTheme::class.java)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "active_theme_json", "active_theme_id" -> {
                ActiveKeyboardBridge.publishTheme(readActiveThemeSynchronously())
            }
            else -> {
                ActiveKeyboardBridge.publishSettings(readSettingsFromPrefs())
            }
        }
    }

    init {
        // Synchronously prime ActiveKeyboardBridge from SharedPreferences so cold-starting
        // ToxicInputMethodService has the user's saved theme & language on Frame 1 without waiting for Room IO.
        try {
            prefs?.registerOnSharedPreferenceChangeListener(prefChangeListener)
            val quickSettings = readSettingsFromPrefs()
            ActiveKeyboardBridge.publishSettings(quickSettings)
            val quickTheme = readActiveThemeSynchronously()
            ActiveKeyboardBridge.publishTheme(quickTheme)
        } catch (_: Exception) {
            ActiveKeyboardBridge.publishTheme(DefaultThemes.TOXIC_EMERALD_CORE)
        }

        repoScope.launch {
            try {
                seedBuiltInThemesIfNeeded()
                loadAndPublishActiveState()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ThemeRepository? = null

        fun getInstance(context: Context): ThemeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val themesFlow: Flow<List<KeyboardTheme>> = dao.observeAllThemes()
        .map { entities ->
            val parsed = entities.mapNotNull { entity ->
                decodeThemeSafe(entity.jsonPayload)
            }
            if (parsed.isEmpty()) DefaultThemes.ALL_PRESETS else parsed
        }
        .catch { emit(DefaultThemes.ALL_PRESETS) }

    val importedFontsFlow: Flow<List<ImportedFontEntity>> = dao.observeImportedFonts()
        .catch { emit(emptyList()) }

    val clipboardFlow: Flow<List<ClipboardItemEntity>> = dao.observeClipboardItems()
        .catch { emit(emptyList()) }

    val dictionaryFlow: Flow<List<PersonalWordEntity>> = dao.observeAllWords()
        .catch { emit(emptyList()) }

    /**
     * Fast, non-blocking synchronous theme read from SharedPreferences cache with safe
     * fallback to built-in presets so InputMethodService never blocks or displays blank state.
     */
    fun readActiveThemeSynchronously(): KeyboardTheme {
        return try {
            val cachedJson = prefs?.getString("active_theme_json", null)
            val activeId = prefs?.getString("active_theme_id", DefaultThemes.TOXIC_EMERALD_CORE.id)
                ?: DefaultThemes.TOXIC_EMERALD_CORE.id
            val decoded = if (!cachedJson.isNullOrBlank()) {
                decodeThemeSafe(cachedJson)
            } else {
                null
            }
            val resolved = decoded
                ?: DefaultThemes.ALL_PRESETS.find { it.id == activeId }
                ?: DefaultThemes.TOXIC_EMERALD_CORE
            sanitizeMissingAssets(resolved)
        } catch (_: Exception) {
            DefaultThemes.TOXIC_EMERALD_CORE
        }
    }

    fun saveQuickLanguagePreference(languageCode: String) {
        val normalized = if (languageCode == "bn") "bn" else "en"
        try {
            prefs?.edit()?.putString("default_language", normalized)?.apply()
            val updated = readSettingsFromPrefs().copy(defaultLanguage = normalized)
            ActiveKeyboardBridge.publishSettings(updated)
        } catch (_: Exception) {
        }
    }

    suspend fun seedBuiltInThemesIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getAllThemesOnce()
            val existingIds = existing.map { it.id }.toSet()
            for (preset in DefaultThemes.ALL_PRESETS) {
                if (preset.id !in existingIds) {
                    dao.upsertTheme(toEntity(preset))
                }
            }
            val sampleEn = listOf("keyboard", "studio", "android", "theme", "custom", "welcome", "thank", "hello", "morning", "project")
            val sampleBn = listOf("বাংলা", "ধন্যবাদ", "স্বাগতম", "কেমন", "আছেন", "ভালো", "কীবোর্ড", "প্রিয়", "শুভ", "সকাল")
            sampleEn.forEach { dao.upsertWord(PersonalWordEntity(word = it, languageCode = "en", frequency = 5)) }
            sampleBn.forEach { dao.upsertWord(PersonalWordEntity(word = it, languageCode = "bn", frequency = 5)) }
        } catch (_: Exception) {
        }
    }

    suspend fun loadAndPublishActiveState() = withContext(Dispatchers.IO) {
        try {
            val settings = readSettingsFromPrefs()
            ActiveKeyboardBridge.publishSettings(settings)

            val entity = dao.getThemeById(settings.activeThemeId)
            val theme = entity?.let { decodeThemeSafe(it.jsonPayload) }
                ?: readActiveThemeSynchronously()

            val sanitized = sanitizeMissingAssets(theme)
            cacheActiveThemeInPrefs(sanitized)
            ActiveKeyboardBridge.publishTheme(sanitized)
        } catch (_: Exception) {
            ActiveKeyboardBridge.publishTheme(DefaultThemes.TOXIC_EMERALD_CORE)
        }
    }

    fun readSettingsFromPrefs(): GlobalKeyboardSettings {
        val p = prefs ?: return GlobalKeyboardSettings()
        return try {
            GlobalKeyboardSettings(
                activeThemeId = p.getString("active_theme_id", DefaultThemes.TOXIC_EMERALD_CORE.id)
                    ?: DefaultThemes.TOXIC_EMERALD_CORE.id,
                defaultLanguage = p.getString("default_language", "en") ?: "en",
                enableSuggestions = p.getBoolean("enable_suggestions", true),
                enablePersonalDictionaryLearning = p.getBoolean("enable_dict_learning", true),
                enableClipboardHistory = p.getBoolean("enable_clipboard", false),
                highContrastOverride = p.getBoolean("high_contrast_override", false),
                readableKeyLabelsBoost = p.getBoolean("readable_labels_boost", false),
                reducedMotionOverride = p.getBoolean("reduced_motion_override", false),
                globalSoundEnabled = p.getBoolean("global_sound_enabled", false),
                globalVibrationEnabled = p.getBoolean("global_vibration_enabled", true),
                globalVibrationStrengthMs = p.getInt("global_vibration_ms", 18).coerceIn(5, 80)
            )
        } catch (_: Exception) {
            GlobalKeyboardSettings()
        }
    }

    suspend fun updateGlobalSettings(transform: (GlobalKeyboardSettings) -> GlobalKeyboardSettings) =
        withContext(Dispatchers.IO) {
            val current = readSettingsFromPrefs()
            val updated = transform(current)
            try {
                prefs?.edit()
                    ?.putString("active_theme_id", updated.activeThemeId)
                    ?.putString("default_language", updated.defaultLanguage)
                    ?.putBoolean("enable_suggestions", updated.enableSuggestions)
                    ?.putBoolean("enable_dict_learning", updated.enablePersonalDictionaryLearning)
                    ?.putBoolean("enable_clipboard", updated.enableClipboardHistory)
                    ?.putBoolean("high_contrast_override", updated.highContrastOverride)
                    ?.putBoolean("readable_labels_boost", updated.readableKeyLabelsBoost)
                    ?.putBoolean("reduced_motion_override", updated.reducedMotionOverride)
                    ?.putBoolean("global_sound_enabled", updated.globalSoundEnabled)
                    ?.putBoolean("global_vibration_enabled", updated.globalVibrationEnabled)
                    ?.putInt("global_vibration_ms", updated.globalVibrationStrengthMs)
                    ?.apply()
            } catch (_: Exception) {
            }
            ActiveKeyboardBridge.publishSettings(updated)
        }

    suspend fun saveTheme(theme: KeyboardTheme, applyImmediately: Boolean = false): KeyboardTheme =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val validated = sanitizeMissingAssets(theme.validated().copy(updatedAtEpochMs = now))
            try {
                dao.upsertTheme(toEntity(validated))
            } catch (_: Exception) {
            }
            if (applyImmediately || ActiveKeyboardBridge.activeTheme.value.id == validated.id) {
                cacheActiveThemeInPrefs(validated)
                updateGlobalSettings { it.copy(activeThemeId = validated.id) }
                ActiveKeyboardBridge.publishTheme(validated)
            }
            validated
        }

    suspend fun applyTheme(themeId: String): KeyboardTheme = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entity = try {
            dao.getThemeById(themeId)
        } catch (_: Exception) {
            null
        }
        val rawTheme = entity?.let { decodeThemeSafe(it.jsonPayload) }
            ?: DefaultThemes.ALL_PRESETS.find { it.id == themeId }
            ?: DefaultThemes.TOXIC_EMERALD_CORE

        val updatedTheme = sanitizeMissingAssets(rawTheme.copy(lastUsedAtEpochMs = now))
        try {
            dao.upsertTheme(toEntity(updatedTheme))
        } catch (_: Exception) {
        }

        cacheActiveThemeInPrefs(updatedTheme)
        updateGlobalSettings { it.copy(activeThemeId = updatedTheme.id) }
        ActiveKeyboardBridge.publishTheme(updatedTheme)
        updatedTheme
    }

    private fun cacheActiveThemeInPrefs(theme: KeyboardTheme) {
        try {
            val json = themeAdapter.toJson(theme.validated())
            prefs?.edit()
                ?.putString("active_theme_id", theme.id)
                ?.putString("active_theme_json", json)
                ?.apply()
        } catch (_: Exception) {
        }
    }

    suspend fun duplicateTheme(source: KeyboardTheme, customName: String? = null): KeyboardTheme =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val copyTheme = source.copy(
                id = "theme_${UUID.randomUUID()}",
                name = customName?.trim()?.take(48)?.ifEmpty { "${source.name} Copy" }
                    ?: "${source.name} Copy",
                isBuiltIn = false,
                isDefault = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                lastUsedAtEpochMs = now
            ).validated()
            try {
                dao.upsertTheme(toEntity(copyTheme))
            } catch (_: Exception) {
            }
            copyTheme
        }

    suspend fun deleteTheme(themeId: String) = withContext(Dispatchers.IO) {
        val target = try {
            dao.getThemeById(themeId)
        } catch (_: Exception) {
            null
        } ?: return@withContext
        if (target.isBuiltIn) return@withContext
        try {
            dao.deleteCustomTheme(themeId)
        } catch (_: Exception) {
        }
        if (ActiveKeyboardBridge.activeTheme.value.id == themeId) {
            applyTheme(DefaultThemes.TOXIC_EMERALD_CORE.id)
        }
    }

    suspend fun restoreDefaults() = withContext(Dispatchers.IO) {
        try {
            for (preset in DefaultThemes.ALL_PRESETS) {
                dao.upsertTheme(toEntity(preset))
            }
        } catch (_: Exception) {
        }
        val defaultSettings = GlobalKeyboardSettings()
        updateGlobalSettings { defaultSettings }
        applyTheme(DefaultThemes.TOXIC_EMERALD_CORE.id)
    }

    // Personal Dictionary & Suggestions
    suspend fun querySuggestions(lang: String, prefix: String): List<String> =
        withContext(Dispatchers.IO) {
            if (prefix.isBlank()) {
                return@withContext if (lang == "bn") {
                    listOf("বাংলা", "ধন্যবাদ", "কেমন", "ভালো", "হ্যালো")
                } else {
                    listOf("the", "and", "you", "that", "have")
                }
            }
            val matches = try {
                dao.findSuggestions(lang, prefix.lowercase())
                    .map { it.word }
                    .distinct()
            } catch (_: Exception) {
                emptyList()
            }
            if (matches.size >= 3) {
                matches.take(3)
            } else {
                val fallback = if (lang == "bn") {
                    listOf("বাংলা", "ধন্যবাদ", "স্বাগতম", "ভালো", "আছেন")
                        .filter { it.startsWith(prefix) }
                        .ifEmpty { listOf("বাংলা", "ধন্যবাদ", "ভালো") }
                } else {
                    listOf("${prefix}s", "${prefix}ing", "${prefix}ed")
                }
                (matches + fallback).distinct().take(3)
            }
        }

    suspend fun learnWordIfAllowed(word: String, lang: String, isSensitiveField: Boolean) =
        withContext(Dispatchers.IO) {
            if (isSensitiveField) return@withContext
            val settings = ActiveKeyboardBridge.globalSettings.value
            if (!settings.enablePersonalDictionaryLearning) return@withContext
            val cleaned = word.trim()
            if (cleaned.length < 2 || cleaned.length > 32) return@withContext
            if (cleaned.any { it.isDigit() }) return@withContext
            try {
                dao.upsertWord(PersonalWordEntity(word = cleaned, languageCode = lang, frequency = 2))
            } catch (_: Exception) {
            }
        }

    suspend fun addDictionaryWord(word: String, lang: String) = withContext(Dispatchers.IO) {
        val cleaned = word.trim()
        if (cleaned.isNotEmpty()) {
            try {
                dao.upsertWord(PersonalWordEntity(word = cleaned, languageCode = lang, frequency = 10))
            } catch (_: Exception) {
            }
        }
    }

    suspend fun removeDictionaryWord(word: String) = withContext(Dispatchers.IO) {
        try {
            dao.deleteWord(word)
        } catch (_: Exception) {
        }
    }

    suspend fun clearPersonalDictionary() = withContext(Dispatchers.IO) {
        try {
            dao.clearPersonalDictionary()
        } catch (_: Exception) {
        }
    }

    // Clipboard
    suspend fun recordClipboardTextIfAllowed(text: String, isSensitiveField: Boolean) =
        withContext(Dispatchers.IO) {
            if (isSensitiveField) return@withContext
            if (!ActiveKeyboardBridge.globalSettings.value.enableClipboardHistory) return@withContext
            val trimmed = text.trim()
            if (trimmed.isEmpty() || trimmed.length > 500) return@withContext
            try {
                dao.insertClipboardItem(ClipboardItemEntity(textContent = trimmed))
            } catch (_: Exception) {
            }
        }

    suspend fun deleteClipboardItem(id: Long) = withContext(Dispatchers.IO) {
        try {
            dao.deleteClipboardItem(id)
        } catch (_: Exception) {
        }
    }

    suspend fun clearClipboardHistory() = withContext(Dispatchers.IO) {
        try {
            dao.clearAllClipboard()
        } catch (_: Exception) {
        }
    }

    // Custom Fonts
    suspend fun saveImportedFont(entity: ImportedFontEntity) = withContext(Dispatchers.IO) {
        try {
            dao.insertImportedFont(entity)
        } catch (_: Exception) {
        }
    }

    suspend fun removeImportedFont(font: ImportedFontEntity) = withContext(Dispatchers.IO) {
        try {
            val f = File(font.filePath)
            if (f.exists() && f.absolutePath.startsWith(appContext.filesDir.absolutePath)) {
                f.delete()
            }
            dao.deleteImportedFont(font.id)
        } catch (_: Exception) {
        }
    }

    /**
     * Ensures missing background images or removed custom font files gracefully fall back
     * rather than crashing the keyboard or editor.
     */
    fun sanitizeMissingAssets(theme: KeyboardTheme): KeyboardTheme {
        var updatedBg = theme.background
        if (!updatedBg.customImagePath.isNullOrBlank()) {
            val file = File(updatedBg.customImagePath)
            if (!file.exists() || !file.canRead()) {
                updatedBg = updatedBg.copy(customImagePath = null)
            }
        }
        var updatedText = theme.textStyle
        if (!updatedText.customFontPath.isNullOrBlank()) {
            val file = File(updatedText.customFontPath)
            if (!file.exists() || !file.canRead()) {
                updatedText = updatedText.copy(customFontPath = null)
            }
        }
        return theme.copy(background = updatedBg, textStyle = updatedText).validated()
    }

    fun encodeThemeToJson(theme: KeyboardTheme): String {
        return themeAdapter.toJson(theme.validated())
    }

    fun decodeThemeSafe(json: String): KeyboardTheme? {
        return try {
            themeAdapter.fromJson(json)?.validated()
        } catch (_: Exception) {
            null
        }
    }

    private fun toEntity(theme: KeyboardTheme): ThemeEntity {
        val valid = theme.validated()
        return ThemeEntity(
            id = valid.id,
            name = valid.name,
            isBuiltIn = valid.isBuiltIn,
            isDefault = valid.isDefault,
            jsonPayload = themeAdapter.toJson(valid),
            createdAtEpochMs = valid.createdAtEpochMs,
            updatedAtEpochMs = valid.updatedAtEpochMs,
            lastUsedAtEpochMs = valid.lastUsedAtEpochMs
        )
    }
}
