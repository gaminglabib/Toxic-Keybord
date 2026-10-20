package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClipboardItemEntity
import com.example.data.ImportedFontEntity
import com.example.data.PersonalWordEntity
import com.example.fonts.FontManager
import com.example.theme.importexport.ImportedThemePreviewResult
import com.example.theme.importexport.ThemeImportExportManager
import com.example.theme.model.BackgroundSourceType
import com.example.theme.model.DefaultThemes
import com.example.theme.model.KeyCategory
import com.example.theme.model.KeyStyleConfig
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.ActiveKeyboardBridge
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.theme.storage.ThemeRepository
import com.example.util.ColorAndImageUtils
import com.example.util.ImeStatusHelper
import com.example.util.SystemImeStatus
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StudioDestination {
    DASHBOARD,
    THEME_STUDIO,
    TEXT_STYLE_STUDIO,
    LAYOUT_STUDIO,
    MY_THEMES,
    SETTINGS_AND_PRIVACY
}

data class StatusBannerMessage(
    val text: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ThemeRepository.getInstance(application)

    private val _currentDestination = MutableStateFlow(StudioDestination.DASHBOARD)
    val currentDestination: StateFlow<StudioDestination> = _currentDestination.asStateFlow()

    private val _imeStatus = MutableStateFlow(ImeStatusHelper.checkStatus(application))
    val imeStatus: StateFlow<SystemImeStatus> = _imeStatus.asStateFlow()

    val savedThemes: StateFlow<List<KeyboardTheme>> = repository.themesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DefaultThemes.ALL_PRESETS
    )

    val activeTheme: StateFlow<KeyboardTheme> = ActiveKeyboardBridge.activeTheme
    val globalSettings: StateFlow<GlobalKeyboardSettings> = ActiveKeyboardBridge.globalSettings

    val importedFonts: StateFlow<List<ImportedFontEntity>> = repository.importedFontsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val clipboardItems: StateFlow<List<ClipboardItemEntity>> = repository.clipboardFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dictionaryWords: StateFlow<List<PersonalWordEntity>> = repository.dictionaryFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Draft theme in Editor + Undo/Redo stack + Previous saved snapshot
    private val _draftTheme = MutableStateFlow(DefaultThemes.TOXIC_EMERALD_CORE)
    val draftTheme: StateFlow<KeyboardTheme> = _draftTheme.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val undoStack = ArrayDeque<KeyboardTheme>()
    private val redoStack = ArrayDeque<KeyboardTheme>()
    private var lastSavedVersionSnapshot: KeyboardTheme? = null

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Selected Key Category or Individual Key in Theme Studio
    private val _selectedKeyCategory = MutableStateFlow(KeyCategory.ALPHABETIC)
    val selectedKeyCategory: StateFlow<KeyCategory> = _selectedKeyCategory.asStateFlow()

    private val _selectedIndividualKeyId = MutableStateFlow<String?>(null)
    val selectedIndividualKeyId: StateFlow<String?> = _selectedIndividualKeyId.asStateFlow()

    // Pending Import Preview for user confirmation before saving/applying
    private val _pendingImportPreview = MutableStateFlow<ImportedThemePreviewResult?>(null)
    val pendingImportPreview: StateFlow<ImportedThemePreviewResult?> = _pendingImportPreview.asStateFlow()

    // User-facing feedback banner
    private val _bannerMessage = MutableStateFlow<StatusBannerMessage?>(null)
    val bannerMessage: StateFlow<StatusBannerMessage?> = _bannerMessage.asStateFlow()

    // Interactive Sandbox Text in Studio
    private val _sandboxText = MutableStateFlow("Type here to test English & বাংলা কীবোর্ড!")
    val sandboxText: StateFlow<String> = _sandboxText.asStateFlow()

    // Dark/Light UI appearance mode for the Studio app itself (default: Premium Dark)
    private val _studioLightMode = MutableStateFlow(false)
    val studioLightMode: StateFlow<Boolean> = _studioLightMode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadAndPublishActiveState()
            val initial = ActiveKeyboardBridge.activeTheme.value
            _draftTheme.value = initial
            lastSavedVersionSnapshot = initial
        }
    }

    fun navigateTo(destination: StudioDestination) {
        _currentDestination.value = destination
        refreshImeStatus()
    }

    fun refreshImeStatus() {
        _imeStatus.value = ImeStatusHelper.checkStatus(getApplication())
    }

    fun toggleStudioLightMode(enableLight: Boolean) {
        _studioLightMode.value = enableLight
    }

    fun updateSandboxText(newText: String) {
        _sandboxText.value = newText
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    fun postBanner(message: String, isError: Boolean = false) {
        _bannerMessage.value = StatusBannerMessage(message, isError)
    }

    // --- Theme Editor & Undo/Redo ---

    fun startEditingTheme(theme: KeyboardTheme, destination: StudioDestination = StudioDestination.THEME_STUDIO) {
        undoStack.clear()
        redoStack.clear()
        _draftTheme.value = theme.validated()
        lastSavedVersionSnapshot = theme.validated()
        _hasUnsavedChanges.value = false
        syncUndoRedoFlags()
        _currentDestination.value = destination
    }

    fun createNewCustomTheme() {
        val now = System.currentTimeMillis()
        val fresh = DefaultThemes.TOXIC_EMERALD_CORE.copy(
            id = "theme_${UUID.randomUUID()}",
            name = "Custom Theme ${(savedThemes.value.size + 1)}",
            isBuiltIn = false,
            isDefault = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        startEditingTheme(fresh, StudioDestination.THEME_STUDIO)
        postBanner("Started new custom theme draft.")
    }

    fun updateDraft(mutator: (KeyboardTheme) -> KeyboardTheme) {
        val current = _draftTheme.value
        val updated = mutator(current).validated()
        if (updated == current) return
        if (undoStack.size >= 30) {
            undoStack.removeFirst()
        }
        undoStack.addLast(current)
        redoStack.clear()
        _draftTheme.value = updated
        _hasUnsavedChanges.value = true
        syncUndoRedoFlags()
    }

    fun undoDraft() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(_draftTheme.value)
        _draftTheme.value = prev
        _hasUnsavedChanges.value = (prev != lastSavedVersionSnapshot)
        syncUndoRedoFlags()
    }

    fun redoDraft() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(_draftTheme.value)
        _draftTheme.value = next
        _hasUnsavedChanges.value = true
        syncUndoRedoFlags()
    }

    fun cancelUnsavedChanges() {
        val snapshot = lastSavedVersionSnapshot ?: activeTheme.value
        undoStack.clear()
        redoStack.clear()
        _draftTheme.value = snapshot
        _hasUnsavedChanges.value = false
        syncUndoRedoFlags()
        postBanner("Reverted unsaved changes.")
    }

    fun restorePreviousSavedVersion() {
        val snapshot = lastSavedVersionSnapshot
        if (snapshot != null) {
            updateDraft { snapshot }
            postBanner("Restored previous saved version of '${snapshot.name}'.")
        } else {
            postBanner("No previous saved snapshot available.", isError = true)
        }
    }

    fun selectKeyCategory(category: KeyCategory) {
        _selectedKeyCategory.value = category
        _selectedIndividualKeyId.value = null
    }

    fun selectIndividualKeyForOverride(keyId: String?) {
        _selectedIndividualKeyId.value = keyId?.uppercase()
    }

    fun currentEditedKeyStyle(): KeyStyleConfig {
        val draft = _draftTheme.value
        val indKey = _selectedIndividualKeyId.value
        if (!indKey.isNullOrBlank()) {
            return draft.individualKeyOverrides[indKey]
                ?: draft.styleForKey(indKey, _selectedKeyCategory.value)
        }
        return draft.categoryStyles[_selectedKeyCategory.value.name] ?: draft.defaultKeyStyle
    }

    fun updateActiveKeyStyle(transform: (KeyStyleConfig) -> KeyStyleConfig) {
        val indKey = _selectedIndividualKeyId.value
        val category = _selectedKeyCategory.value
        updateDraft { draft ->
            if (!indKey.isNullOrBlank()) {
                val base = draft.individualKeyOverrides[indKey] ?: draft.styleForKey(indKey, category)
                val updatedMap = draft.individualKeyOverrides.toMutableMap()
                updatedMap[indKey] = transform(base).validated()
                draft.copy(individualKeyOverrides = updatedMap)
            } else {
                val base = draft.categoryStyles[category.name] ?: draft.defaultKeyStyle
                val nextStyle = transform(base).validated()
                val updatedCats = draft.categoryStyles.toMutableMap()
                updatedCats[category.name] = nextStyle
                val nextDefault = if (category == KeyCategory.ALPHABETIC) nextStyle else draft.defaultKeyStyle
                draft.copy(defaultKeyStyle = nextDefault, categoryStyles = updatedCats)
            }
        }
    }

    fun applyStyleToAllCategories(style: KeyStyleConfig) {
        updateDraft { draft ->
            val validated = style.validated()
            val allCats = KeyCategory.entries.associate { it.name to validated }
            draft.copy(
                defaultKeyStyle = validated,
                categoryStyles = allCats,
                individualKeyOverrides = emptyMap()
            )
        }
        postBanner("Applied key style across all key categories.")
    }

    fun resetCurrentKeyOrCategoryToDefault() {
        val indKey = _selectedIndividualKeyId.value
        val cat = _selectedKeyCategory.value
        updateDraft { draft ->
            if (!indKey.isNullOrBlank()) {
                val updatedOverrides = draft.individualKeyOverrides.toMutableMap()
                updatedOverrides.remove(indKey)
                draft.copy(individualKeyOverrides = updatedOverrides)
            } else {
                val defaultMap = KeyboardTheme.defaultCategoryStyles()
                val fallback = defaultMap[cat.name] ?: KeyStyleConfig()
                val updatedCats = draft.categoryStyles.toMutableMap()
                updatedCats[cat.name] = fallback
                draft.copy(categoryStyles = updatedCats)
            }
        }
        postBanner("Reset selected key style to default.")
    }

    fun resetEntireDraftTheme() {
        val base = DefaultThemes.ALL_PRESETS.find { it.id == _draftTheme.value.id }
            ?: DefaultThemes.TOXIC_EMERALD_CORE
        updateDraft {
            base.copy(
                id = _draftTheme.value.id,
                name = _draftTheme.value.name,
                isBuiltIn = _draftTheme.value.isBuiltIn
            )
        }
        postBanner("Reset entire theme configuration to factory defaults.")
    }

    fun saveCurrentDraft(applyToKeyboard: Boolean = true) {
        viewModelScope.launch {
            val current = _draftTheme.value
            val saved = repository.saveTheme(current, applyImmediately = applyToKeyboard)
            _draftTheme.value = saved
            lastSavedVersionSnapshot = saved
            _hasUnsavedChanges.value = false
            undoStack.clear()
            redoStack.clear()
            syncUndoRedoFlags()
            val msg = if (applyToKeyboard) {
                "Saved & applied '${saved.name}' to TOXIC Keyboard!"
            } else {
                "Saved '${saved.name}'."
            }
            postBanner(msg)
        }
    }

    fun saveDraftAsNewTheme(newName: String, applyToKeyboard: Boolean = true) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cleanName = newName.trim().take(48).ifEmpty { "${_draftTheme.value.name} Copy" }
            val copy = _draftTheme.value.copy(
                id = "theme_${UUID.randomUUID()}",
                name = cleanName,
                isBuiltIn = false,
                isDefault = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                lastUsedAtEpochMs = now
            )
            val saved = repository.saveTheme(copy, applyImmediately = applyToKeyboard)
            _draftTheme.value = saved
            lastSavedVersionSnapshot = saved
            _hasUnsavedChanges.value = false
            undoStack.clear()
            redoStack.clear()
            syncUndoRedoFlags()
            postBanner("Saved as new theme '${saved.name}'!")
        }
    }

    fun renameTheme(theme: KeyboardTheme, newName: String) {
        val clean = newName.trim().take(48)
        if (clean.isEmpty()) {
            postBanner("Theme name cannot be empty.", isError = true)
            return
        }
        viewModelScope.launch {
            val updated = theme.copy(name = clean)
            val saved = repository.saveTheme(updated, applyImmediately = (activeTheme.value.id == theme.id))
            if (_draftTheme.value.id == theme.id) {
                _draftTheme.value = saved
            }
            postBanner("Renamed theme to '$clean'.")
        }
    }

    fun duplicateExistingTheme(theme: KeyboardTheme) {
        viewModelScope.launch {
            val copy = repository.duplicateTheme(theme)
            postBanner("Duplicated '${theme.name}' as '${copy.name}'.")
        }
    }

    fun applyExistingTheme(theme: KeyboardTheme) {
        viewModelScope.launch {
            val applied = repository.applyTheme(theme.id)
            _draftTheme.value = applied
            lastSavedVersionSnapshot = applied
            _hasUnsavedChanges.value = false
            postBanner("Applied '${applied.name}' to TOXIC Keyboard.")
        }
    }

    fun deleteCustomTheme(theme: KeyboardTheme) {
        if (theme.isBuiltIn) {
            postBanner("Built-in factory themes cannot be deleted.", isError = true)
            return
        }
        viewModelScope.launch {
            repository.deleteTheme(theme.id)
            if (_draftTheme.value.id == theme.id) {
                _draftTheme.value = activeTheme.value
                _hasUnsavedChanges.value = false
            }
            postBanner("Deleted '${theme.name}'.")
        }
    }

    // --- Background Image Selection & Processing ---

    fun importBackgroundFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = ColorAndImageUtils.importAndProcessBackgroundImage(getApplication(), uri)
            result.onSuccess { localPath ->
                updateDraft { draft ->
                    draft.copy(
                        background = draft.background.copy(
                            sourceType = BackgroundSourceType.CUSTOM_IMAGE,
                            customImagePath = localPath,
                            dimmingOverlay = 0.35f
                        )
                    )
                }
                postBanner("Background photo imported to app-private storage.")
            }.onFailure { err ->
                postBanner(err.message ?: "Failed to decode selected image.", isError = true)
            }
        }
    }

    // --- Custom Font Import & Management ---

    fun importCustomFontFromUri(uri: Uri, displayNameHint: String) {
        viewModelScope.launch {
            val result = FontManager.importCustomFont(getApplication(), uri, displayNameHint)
            result.onSuccess { info ->
                val entity = ImportedFontEntity(
                    id = info.id,
                    displayName = info.displayName,
                    filePath = info.filePath,
                    fileSizeBytes = info.fileSizeBytes,
                    importedAtMs = info.importedAtMs
                )
                repository.saveImportedFont(entity)
                updateDraft { draft ->
                    draft.copy(
                        textStyle = draft.textStyle.copy(
                            customFontPath = info.filePath
                        )
                    )
                }
                postBanner("Imported custom font '${info.displayName}'.")
            }.onFailure { err ->
                postBanner(err.message ?: "Unsupported or corrupt font file.", isError = true)
            }
        }
    }

    fun removeCustomFont(font: ImportedFontEntity) {
        viewModelScope.launch {
            repository.removeImportedFont(font)
            if (_draftTheme.value.textStyle.customFontPath == font.filePath) {
                updateDraft { draft ->
                    draft.copy(textStyle = draft.textStyle.copy(customFontPath = null))
                }
            }
            postBanner("Removed custom font '${font.displayName}'.")
        }
    }

    // --- Offline Preset / Prompt Theme Synthesizer ---

    fun generateThemeFromDescription(prompt: String) {
        if (prompt.isBlank()) {
            postBanner("Enter a description or choose a preset style first.", isError = true)
            return
        }
        val synthesized = DefaultThemes.synthesizeFromDescription(prompt, _draftTheme.value)
        startEditingTheme(synthesized, StudioDestination.THEME_STUDIO)
        _hasUnsavedChanges.value = true
        postBanner("Synthesized '${synthesized.name}' offline! Customize or Save to apply.")
    }

    // --- Import / Export ---

    fun exportThemeToUri(
        theme: KeyboardTheme,
        targetUri: Uri,
        includeBackgroundImage: Boolean,
        includeCustomFontWithLicense: Boolean
    ) {
        viewModelScope.launch {
            val res = ThemeImportExportManager.exportThemeToUri(
                context = getApplication(),
                theme = theme,
                targetUri = targetUri,
                includeBackgroundImage = includeBackgroundImage,
                includeCustomFontWithLicensePermission = includeCustomFontWithLicense
            )
            res.onSuccess { summary ->
                postBanner(summary)
            }.onFailure { err ->
                postBanner("Export failed: ${err.message}", isError = true)
            }
        }
    }

    fun stageImportFromUri(sourceUri: Uri) {
        viewModelScope.launch {
            val res = ThemeImportExportManager.parseAndValidateImportUri(getApplication(), sourceUri)
            res.onSuccess { preview ->
                _pendingImportPreview.value = preview
            }.onFailure { err ->
                postBanner(err.message ?: "Invalid theme file.", isError = true)
            }
        }
    }

    fun cancelPendingImport() {
        _pendingImportPreview.value = null
    }

    fun confirmAndApplyPendingImport(applyImmediately: Boolean) {
        val pending = _pendingImportPreview.value ?: return
        _pendingImportPreview.value = null
        viewModelScope.launch {
            val existingNames = savedThemes.value.map { it.name.lowercase() }.toSet()
            val safeName = if (pending.theme.name.lowercase() in existingNames) {
                "${pending.theme.name} (Imported)"
            } else {
                pending.theme.name
            }
            val finalTheme = pending.theme.copy(name = safeName.take(48))
            val saved = repository.saveTheme(finalTheme, applyImmediately = applyImmediately)
            _draftTheme.value = saved
            lastSavedVersionSnapshot = saved
            _hasUnsavedChanges.value = false
            postBanner("Imported '${saved.name}' successfully!")
        }
    }

    // --- Global Settings & Privacy ---

    fun updateGlobalSettings(transform: (GlobalKeyboardSettings) -> GlobalKeyboardSettings) {
        viewModelScope.launch {
            repository.updateGlobalSettings(transform)
        }
    }

    fun addPersonalWord(word: String, lang: String) {
        viewModelScope.launch {
            repository.addDictionaryWord(word, lang)
            postBanner("Added '$word' to local dictionary.")
        }
    }

    fun removePersonalWord(word: String) {
        viewModelScope.launch {
            repository.removeDictionaryWord(word)
        }
    }

    fun clearPersonalDictionary() {
        viewModelScope.launch {
            repository.clearPersonalDictionary()
            postBanner("Cleared local personal dictionary.")
        }
    }

    fun clearClipboardHistory() {
        viewModelScope.launch {
            repository.clearClipboardHistory()
            postBanner("Cleared local clipboard history.")
        }
    }

    fun deleteSingleClipboardItem(id: Long) {
        viewModelScope.launch {
            repository.deleteClipboardItem(id)
        }
    }

    fun resetAllSettingsAndRestoreDefaults() {
        viewModelScope.launch {
            repository.restoreDefaults()
            _draftTheme.value = DefaultThemes.TOXIC_EMERALD_CORE
            lastSavedVersionSnapshot = DefaultThemes.TOXIC_EMERALD_CORE
            _hasUnsavedChanges.value = false
            postBanner("All keyboard settings and built-in themes restored to defaults.")
        }
    }

    private fun syncUndoRedoFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}
