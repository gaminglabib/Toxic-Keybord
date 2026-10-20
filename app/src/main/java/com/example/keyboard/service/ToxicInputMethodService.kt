package com.example.keyboard.service

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.MainActivity
import com.example.R
import com.example.keyboard.actions.KeyboardActionHandler
import com.example.keyboard.layout.KeyActionType
import com.example.keyboard.layout.KeySpec
import com.example.keyboard.state.EditorFieldContext
import com.example.keyboard.state.KeyboardEngineState
import com.example.keyboard.state.KeyboardLanguage
import com.example.keyboard.state.KeyboardMode
import com.example.keyboard.ui.NativeKeyboardRendererView
import com.example.theme.storage.ActiveKeyboardBridge
import com.example.theme.storage.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Real Android system InputMethodService for TOXIC KEYBOARD STUDIO.
 * Dynamically shares the active KeyboardTheme and GlobalKeyboardSettings with the Studio app,
 * works completely independently of MainActivity, and forces input view display across
 * Android 9+ devices and emulators.
 */
class ToxicInputMethodService : InputMethodService(), NativeKeyboardRendererView.Listener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: ThemeRepository
    private var keyboardView: NativeKeyboardRendererView? = null
    private var engineState = KeyboardEngineState()
    private var recentClips: List<String> = emptyList()
    private var lastShiftTapTimeMs: Long = 0L

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.coerceToText(this)?.toString()
                if (!text.isNullOrBlank() && ::repository.isInitialized) {
                    serviceScope.launch {
                        repository.recordClipboardTextIfAllowed(
                            text = text,
                            isSensitiveField = engineState.editorContext.isPasswordOrSensitive
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureRepositoryInitialized()

        try {
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {
        }

        // Prime initial language immediately from synchronous settings cache
        val initialSettings = ActiveKeyboardBridge.globalSettings.value
        engineState = engineState.copy(
            language = if (initialSettings.defaultLanguage == "bn") {
                KeyboardLanguage.BENGALI
            } else {
                KeyboardLanguage.ENGLISH
            }
        )

        serviceScope.launch {
            repository.loadAndPublishActiveState()
            val prefLang = ActiveKeyboardBridge.globalSettings.value.defaultLanguage
            engineState = engineState.copy(
                language = if (prefLang == "bn") KeyboardLanguage.BENGALI else KeyboardLanguage.ENGLISH
            )
            refreshSuggestionsAsync()
        }

        serviceScope.launch {
            ActiveKeyboardBridge.activeTheme.collectLatest {
                syncView()
            }
        }

        serviceScope.launch {
            ActiveKeyboardBridge.globalSettings.collectLatest { settings ->
                syncView()
            }
        }

        serviceScope.launch {
            repository.clipboardFlow.collectLatest { items ->
                recentClips = items.map { it.textContent }
                syncView()
            }
        }
    }

    private fun ensureRepositoryInitialized() {
        if (!::repository.isInitialized) {
            repository = ThemeRepository.getInstance(applicationContext)
        }
    }

    /**
     * Critical for emulators and Android 9+ OEM devices:
     * Always evaluate input view as shown so Android creates and displays the soft keyboard
     * whenever any text field is focused, even if a virtual/hardware keyboard is reported.
     */
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    /**
     * Never cover the underlying application with an opaque fullscreen extract editor.
     */
    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        super.onShowInputRequested(flags, configChange)
        return true
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return
        outInsets.contentTopInsets = outInsets.visibleTopInsets
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
    }

    override fun onCreateInputView(): View {
        ensureRepositoryInitialized()
        val view: NativeKeyboardRendererView = try {
            val inflated = layoutInflater.inflate(R.layout.keyboard_input_view, null)
            (inflated as? NativeKeyboardRendererView)
                ?: inflated.findViewById(R.id.toxic_keyboard_view)
                ?: NativeKeyboardRendererView(this)
        } catch (_: Exception) {
            NativeKeyboardRendererView(this)
        }

        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.visibility = View.VISIBLE
        view.listener = this@ToxicInputMethodService
        keyboardView = view
        syncView()
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        ensureRepositoryInitialized()
        applyEditorInfoToState(attribute)
        syncView()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        ensureRepositoryInitialized()
        applyEditorInfoToState(info)

        // Synchronously read latest saved theme & settings so keyboard is always up-to-date on frame 1
        val latestSettings = repository.readSettingsFromPrefs()
        val latestTheme = repository.readActiveThemeSynchronously()
        ActiveKeyboardBridge.publishSettings(latestSettings)
        ActiveKeyboardBridge.publishTheme(latestTheme)

        keyboardView?.visibility = View.VISIBLE
        syncView()
        keyboardView?.requestLayout()

        serviceScope.launch {
            repository.loadAndPublishActiveState()
            refreshSuggestionsAsync()
            syncView()
        }
    }

    private fun applyEditorInfoToState(info: EditorInfo?) {
        val fieldContext = EditorFieldContext.fromEditorInfo(info)
        val targetMode = if (fieldContext.isNumericOnly) {
            KeyboardMode.SYMBOLS_primary
        } else if (engineState.mode == KeyboardMode.EMOJI_PANEL || engineState.mode == KeyboardMode.CLIPBOARD_PANEL) {
            KeyboardMode.ALPHABET
        } else {
            KeyboardMode.ALPHABET
        }
        engineState = engineState.copy(
            editorContext = fieldContext,
            mode = targetMode,
            composingWord = ""
        )
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        try {
            currentInputConnection?.finishComposingText()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onKeyTapped(key: KeySpec, isLongPress: Boolean) {
        val ic = currentInputConnection
        if (isLongPress) {
            when (key.actionType) {
                KeyActionType.SHIFT -> {
                    engineState = engineState.onShiftClicked(isDoubleTap = true)
                    syncView()
                    return
                }
                else -> {
                    val altText = key.secondaryOutputText
                    if (!altText.isNullOrEmpty()) {
                        KeyboardActionHandler.commitTextSafe(ic, altText)
                        engineState = engineState.afterCharacterCommitted()
                        refreshSuggestionsAsync()
                        syncView()
                        return
                    }
                }
            }
        }

        when (key.actionType) {
            KeyActionType.CHARACTER -> {
                KeyboardActionHandler.commitTextSafe(ic, key.outputText)
                if (KeyboardActionHandler.isWordSeparator(key.outputText)) {
                    val prefix = engineState.composingWord
                    if (prefix.length >= 2) {
                        serviceScope.launch {
                            repository.learnWordIfAllowed(
                                word = prefix,
                                lang = engineState.language.code,
                                isSensitiveField = engineState.editorContext.isPasswordOrSensitive
                            )
                        }
                    }
                }
                engineState = engineState.afterCharacterCommitted()
                refreshSuggestionsAsync()
            }

            KeyActionType.SPACE -> {
                val lastWord = KeyboardActionHandler.extractCurrentWordPrefix(ic)
                KeyboardActionHandler.commitTextSafe(ic, " ")
                if (lastWord.length >= 2) {
                    serviceScope.launch {
                        repository.learnWordIfAllowed(
                            word = lastWord,
                            lang = engineState.language.code,
                            isSensitiveField = engineState.editorContext.isPasswordOrSensitive
                        )
                    }
                }
                refreshSuggestionsAsync()
            }

            KeyActionType.BACKSPACE -> {
                KeyboardActionHandler.handleBackspace(ic)
                refreshSuggestionsAsync()
            }

            KeyActionType.SHIFT -> {
                val now = System.currentTimeMillis()
                val isDouble = (now - lastShiftTapTimeMs) < 320L
                lastShiftTapTimeMs = now
                engineState = engineState.onShiftClicked(isDoubleTap = isDouble)
            }

            KeyActionType.ENTER -> {
                KeyboardActionHandler.handleEnterKey(ic, engineState.editorContext)
                refreshSuggestionsAsync()
            }

            KeyActionType.MODE_SYMBOLS_1 -> {
                engineState = engineState.onModeChanged(KeyboardMode.SYMBOLS_primary)
            }

            KeyActionType.MODE_SYMBOLS_2 -> {
                engineState = engineState.onModeChanged(KeyboardMode.SYMBOLS_secondary)
            }

            KeyActionType.MODE_ALPHABET -> {
                engineState = engineState.onModeChanged(KeyboardMode.ALPHABET)
            }

            KeyActionType.LANGUAGE_SWITCH -> {
                engineState = engineState.onLanguageToggled()
                repository.saveQuickLanguagePreference(engineState.language.code)
                refreshSuggestionsAsync()
            }

            KeyActionType.EMOJI_PANEL_TOGGLE -> {
                val nextMode = if (engineState.mode == KeyboardMode.EMOJI_PANEL) {
                    KeyboardMode.ALPHABET
                } else {
                    KeyboardMode.EMOJI_PANEL
                }
                engineState = engineState.onModeChanged(nextMode)
            }
        }
        syncView()
    }

    override fun onSuggestionClicked(word: String) {
        val ic = currentInputConnection
        val prefix = KeyboardActionHandler.extractCurrentWordPrefix(ic)
        KeyboardActionHandler.commitSuggestion(ic, prefix, word)
        serviceScope.launch {
            repository.learnWordIfAllowed(
                word = word,
                lang = engineState.language.code,
                isSensitiveField = engineState.editorContext.isPasswordOrSensitive
            )
            refreshSuggestionsAsync()
        }
    }

    override fun onEmojiSelected(emoji: String) {
        KeyboardActionHandler.commitTextSafe(currentInputConnection, emoji)
    }

    override fun onClipboardItemSelected(text: String) {
        KeyboardActionHandler.commitTextSafe(currentInputConnection, text)
        engineState = engineState.onModeChanged(KeyboardMode.ALPHABET)
        syncView()
    }

    override fun onToolbarModeToggle(mode: KeyboardMode) {
        engineState = engineState.onModeChanged(mode)
        syncView()
    }

    override fun onToolbarLanguageToggle() {
        engineState = engineState.onLanguageToggled()
        repository.saveQuickLanguagePreference(engineState.language.code)
        refreshSuggestionsAsync()
        syncView()
    }

    override fun onOpenStudioAppRequested() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun refreshSuggestionsAsync() {
        if (engineState.editorContext.isPasswordOrSensitive) {
            engineState = engineState.copy(composingWord = "", suggestions = emptyList())
            syncView()
            return
        }
        val prefix = KeyboardActionHandler.extractCurrentWordPrefix(currentInputConnection)
        serviceScope.launch {
            val list = repository.querySuggestions(engineState.language.code, prefix)
            engineState = engineState.copy(composingWord = prefix, suggestions = list)
            syncView()
        }
    }

    private fun syncView() {
        keyboardView?.updateConfiguration(
            newTheme = ActiveKeyboardBridge.activeTheme.value,
            newSettings = ActiveKeyboardBridge.globalSettings.value,
            newState = engineState,
            newClipboard = recentClips
        )
    }
}
