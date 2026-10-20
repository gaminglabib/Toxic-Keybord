package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Universal clipboard integration helper for TOXIC KEYBOARD STUDIO.
 * Provides safe copy actions with real ClipData creation, confirmation feedback,
 * and graceful fallback handling on Android 9 through Android 15+.
 */
object ClipboardUtils {

    /**
     * Copies plain text to Android's ClipboardManager and displays an immediate
     * confirmation Toast so users know their copy succeeded.
     */
    fun copyToClipboard(
        context: Context,
        label: String = "TOXIC Keyboard",
        text: String?,
        showFeedbackToast: Boolean = true,
        feedbackMessage: String? = null
    ): Boolean {
        if (text.isNullOrBlank()) {
            if (showFeedbackToast) {
                showToast(context, "Nothing to copy")
            }
            return false
        }

        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard == null) {
                if (showFeedbackToast) {
                    showToast(context, "Clipboard service unavailable")
                }
                return false
            }

            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)

            if (showFeedbackToast) {
                val msg = feedbackMessage ?: "Copied to clipboard"
                showToast(context, msg)
            }
            true
        } catch (e: Exception) {
            if (showFeedbackToast) {
                showToast(context, "Could not copy: ${e.message}")
            }
            false
        }
    }

    private fun showToast(context: Context, message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
