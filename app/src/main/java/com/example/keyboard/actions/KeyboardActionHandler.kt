package com.example.keyboard.actions

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.example.keyboard.state.EditorFieldContext

/**
 * Handles cursor-aware text insertion, selection-aware Unicode codepoint deletion,
 * composing text, and editor actions safely via Android's InputConnection.
 */
object KeyboardActionHandler {

    private val WORD_SEPARATORS = setOf(
        ' ', '.', ',', '!', '?', ';', ':', '\n', '(', ')', '[', ']', '{', '}', '।', '-', '/', '"'
    )

    fun isWordSeparator(text: String): Boolean {
        return text.length == 1 && text[0] in WORD_SEPARATORS
    }

    /**
     * Commits character or multi-codepoint string (such as Bengali conjuncts or emoji)
     * at the current cursor/selection position.
     */
    fun commitTextSafe(
        ic: InputConnection?,
        text: String
    ) {
        if (ic == null || text.isEmpty()) return
        try {
            ic.beginBatchEdit()
            ic.commitText(text, 1)
            ic.endBatchEdit()
        } catch (_: Exception) {
        }
    }

    /**
     * Selection-aware deletion:
     * - If the user has an active text selection, deletes the selected range.
     * - Otherwise deletes 1 Unicode code point before the cursor so surrogate pairs
     *   (emoji) and Bengali characters are deleted cleanly without corrupting UTF-16 surrogates.
     */
    fun handleBackspace(ic: InputConnection?) {
        if (ic == null) return
        try {
            ic.beginBatchEdit()
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                ic.deleteSurroundingTextInCodePoints(1, 0)
            }
            ic.endBatchEdit()
        } catch (_: Exception) {
            try {
                ic.deleteSurroundingText(1, 0)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Replaces the current trailing word prefix with the chosen suggestion bar word
     * and appends a trailing space.
     */
    fun commitSuggestion(
        ic: InputConnection?,
        currentPrefix: String,
        suggestion: String
    ) {
        if (ic == null || suggestion.isEmpty()) return
        try {
            ic.beginBatchEdit()
            if (currentPrefix.isNotEmpty()) {
                ic.deleteSurroundingText(currentPrefix.length, 0)
            }
            ic.commitText("$suggestion ", 1)
            ic.endBatchEdit()
        } catch (_: Exception) {
        }
    }

    /**
     * Sends the appropriate editor action (Search, Send, Go, Next, Done) or commits
     * a newline in multiline fields.
     */
    fun handleEnterKey(
        ic: InputConnection?,
        editorContext: EditorFieldContext
    ) {
        if (ic == null) return
        try {
            if (editorContext.isMultiline) {
                ic.commitText("\n", 1)
                return
            }
            when (editorContext.imeActionId) {
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND,
                EditorInfo.IME_ACTION_GO,
                EditorInfo.IME_ACTION_NEXT,
                EditorInfo.IME_ACTION_DONE -> {
                    val handled = ic.performEditorAction(editorContext.imeActionId)
                    if (!handled) {
                        sendEnterKeyEvent(ic)
                    }
                }
                else -> sendEnterKeyEvent(ic)
            }
        } catch (_: Exception) {
        }
    }

    private fun sendEnterKeyEvent(ic: InputConnection) {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    /**
     * Reads up to 24 characters before the cursor to extract the active word prefix
     * for local dictionary suggestions without assuming a fixed cursor index.
     */
    fun extractCurrentWordPrefix(ic: InputConnection?): String {
        if (ic == null) return ""
        return try {
            val before = ic.getTextBeforeCursor(24, 0)?.toString() ?: return ""
            val lastSepIndex = before.indexOfLast { it in WORD_SEPARATORS }
            if (lastSepIndex == -1) {
                before.trim()
            } else {
                before.substring(lastSepIndex + 1).trim()
            }
        } catch (_: Exception) {
            ""
        }
    }
}
