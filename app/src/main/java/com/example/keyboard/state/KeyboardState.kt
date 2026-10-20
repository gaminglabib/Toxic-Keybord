package com.example.keyboard.state

import android.os.Build
import android.text.InputType
import android.view.inputmethod.EditorInfo

enum class KeyboardLanguage(val code: String, val badge: String, val displayName: String) {
    ENGLISH("en", "EN", "English (US)"),
    BENGALI("bn", "বাং", "বাংলা (Bengali)")
}

enum class KeyboardMode {
    ALPHABET,
    SYMBOLS_primary,
    SYMBOLS_secondary,
    EMOJI_PANEL,
    CLIPBOARD_PANEL
}

enum class ShiftState {
    UNSHIFTED,
    SHIFTED_SINGLE,
    CAPS_LOCK
}

data class EditorFieldContext(
    val isPasswordOrSensitive: Boolean = false,
    val isMultiline: Boolean = false,
    val isNumericOnly: Boolean = false,
    val imeActionId: Int = EditorInfo.IME_ACTION_NONE,
    val enterKeyLabel: String = "↵"
) {
    companion object {
        fun fromEditorInfo(info: EditorInfo?): EditorFieldContext {
            if (info == null) return EditorFieldContext()
            val inputType = info.inputType
            val klass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val flags = inputType and InputType.TYPE_MASK_FLAGS

            val isPassword = when (klass) {
                InputType.TYPE_CLASS_TEXT -> {
                    variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                }
                InputType.TYPE_CLASS_NUMBER -> {
                    variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
                }
                else -> false
            }

            val noPersonalizedLearning = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
            } else {
                false
            }

            val isSensitive = isPassword || noPersonalizedLearning ||
                variation == InputType.TYPE_TEXT_VARIATION_FILTER

            val isMulti = klass == InputType.TYPE_CLASS_TEXT &&
                (flags and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

            val isNumeric = klass == InputType.TYPE_CLASS_NUMBER ||
                klass == InputType.TYPE_CLASS_PHONE

            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            val noEnterAction = (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

            val label = if (isMulti && noEnterAction) {
                "↵"
            } else {
                when (action) {
                    EditorInfo.IME_ACTION_SEARCH -> "Search"
                    EditorInfo.IME_ACTION_SEND -> "Send"
                    EditorInfo.IME_ACTION_GO -> "Go"
                    EditorInfo.IME_ACTION_NEXT -> "Next"
                    EditorInfo.IME_ACTION_DONE -> "Done"
                    else -> "↵"
                }
            }

            return EditorFieldContext(
                isPasswordOrSensitive = isSensitive,
                isMultiline = isMulti,
                isNumericOnly = isNumeric,
                imeActionId = action,
                enterKeyLabel = label
            )
        }
    }
}

data class KeyboardEngineState(
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val mode: KeyboardMode = KeyboardMode.ALPHABET,
    val shiftState: ShiftState = ShiftState.UNSHIFTED,
    val editorContext: EditorFieldContext = EditorFieldContext(),
    val composingWord: String = "",
    val suggestions: List<String> = listOf("the", "and", "you"),
    val isPhoneticMode: Boolean = true,
    val voiceState: com.example.keyboard.voice.VoiceInputState = com.example.keyboard.voice.VoiceInputState.IDLE,
    val voiceMessage: String = "",
    val voiceRms: Float = 0f,
    val selectedEmojiCategoryIndex: Int = 0
) {
    val isShifted: Boolean
        get() = shiftState == ShiftState.SHIFTED_SINGLE || shiftState == ShiftState.CAPS_LOCK

    fun onShiftClicked(isDoubleTap: Boolean = false): KeyboardEngineState {
        val nextShift = when {
            isDoubleTap -> ShiftState.CAPS_LOCK
            shiftState == ShiftState.UNSHIFTED -> ShiftState.SHIFTED_SINGLE
            shiftState == ShiftState.SHIFTED_SINGLE -> ShiftState.CAPS_LOCK
            else -> ShiftState.UNSHIFTED
        }
        return copy(shiftState = nextShift)
    }

    fun onLanguageToggled(): KeyboardEngineState {
        val nextLang = if (language == KeyboardLanguage.ENGLISH) {
            KeyboardLanguage.BENGALI
        } else {
            KeyboardLanguage.ENGLISH
        }
        return copy(
            language = nextLang,
            mode = KeyboardMode.ALPHABET,
            shiftState = ShiftState.UNSHIFTED,
            composingWord = ""
        )
    }

    fun onModeChanged(targetMode: KeyboardMode): KeyboardEngineState {
        return copy(mode = targetMode, composingWord = "")
    }

    fun afterCharacterCommitted(): KeyboardEngineState {
        val nextShift = if (shiftState == ShiftState.SHIFTED_SINGLE) {
            ShiftState.UNSHIFTED
        } else {
            shiftState
        }
        return copy(shiftState = nextShift)
    }
}
