package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.example.keyboard.service.ToxicInputMethodService

data class SystemImeStatus(
    val isEnabled: Boolean,
    val isSelectedDefault: Boolean
)

object ImeStatusHelper {

    /**
     * Queries Android's InputMethodManager and Secure Settings to check whether
     * ToxicInputMethodService is genuinely enabled and selected as the active default IME.
     */
    fun checkStatus(context: Context): SystemImeStatus {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val pkg = context.packageName
            val serviceComp = ComponentName(context, ToxicInputMethodService::class.java)
            val expectedFlat = serviceComp.flattenToShortString()
            val expectedFull = serviceComp.flattenToString()

            val enabledList = imm?.enabledInputMethodList.orEmpty()
            val isEnabledByManager = enabledList.any { info ->
                info.packageName == pkg ||
                    info.id == expectedFlat ||
                    info.id == expectedFull ||
                    info.serviceName.contains("ToxicInputMethodService")
            }

            val enabledSecure = try {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_INPUT_METHODS
                ).orEmpty()
            } catch (_: Exception) {
                ""
            }
            val isEnabled = isEnabledByManager ||
                enabledSecure.contains(expectedFlat) ||
                enabledSecure.contains(pkg)

            val defaultIme = try {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD
                ).orEmpty()
            } catch (_: Exception) {
                ""
            }
            val isDefault = defaultIme == expectedFlat ||
                defaultIme == expectedFull ||
                defaultIme.startsWith("$pkg/") ||
                defaultIme.contains(pkg)

            SystemImeStatus(
                isEnabled = isEnabled,
                isSelectedDefault = isEnabled && isDefault
            )
        } catch (_: Exception) {
            SystemImeStatus(isEnabled = false, isSelectedDefault = false)
        }
    }

    fun launchEnableKeyboardSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun showKeyboardPickerDialog(context: Context): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun summonSoftKeyboard(view: View): Boolean {
        return try {
            view.requestFocus()
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT) ?: false
        } catch (_: Exception) {
            false
        }
    }
}
