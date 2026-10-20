package com.example

import com.example.keyboard.layout.KeyActionType
import com.example.keyboard.layout.KeyboardLayouts
import com.example.keyboard.state.KeyboardEngineState
import com.example.keyboard.state.KeyboardLanguage
import com.example.keyboard.state.KeyboardMode
import com.example.keyboard.state.ShiftState
import com.example.theme.model.DefaultThemes
import com.example.theme.model.KeyCategory
import com.example.theme.model.KeyStyleConfig
import com.example.theme.model.KeyboardTheme
import com.example.theme.model.LayoutConfig
import com.example.theme.model.sanitizeHexColor
import com.example.util.ColorAndImageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun hexColorSanitization_handlesInvalidAndValidHexStrings() {
        assertEquals("#00E676", sanitizeHexColor("#00e676", "#000000"))
        assertEquals("#FF1744", sanitizeHexColor("FF1744", "#000000"))
        assertEquals("#14221B", sanitizeHexColor("not_a_color", "#14221B"))
        assertEquals("#14221B", sanitizeHexColor("#12345", "#14221B"))
    }

    @Test
    fun themeValidation_clampsOutOfRangeValuesAndResolvesKeyOverrides() {
        val rawStyle = KeyStyleConfig(
            backgroundColorHex = "INVALID",
            borderWidthDp = 99f,
            cornerRadiusDp = -10f,
            keyOpacity = 5f
        )
        val clamped = rawStyle.validated()
        assertEquals("#14221B", clamped.backgroundColorHex)
        assertEquals(4.0f, clamped.borderWidthDp, 0.01f)
        assertEquals(0.0f, clamped.cornerRadiusDp, 0.01f)
        assertEquals(1.0f, clamped.keyOpacity, 0.01f)

        val customEnterOverride = KeyStyleConfig(backgroundColorHex = "#FF1744")
        val theme = DefaultThemes.TOXIC_EMERALD_CORE.copy(
            individualKeyOverrides = mapOf("Q" to customEnterOverride)
        ).validated()

        assertEquals("#FF1744", theme.styleForKey("q", KeyCategory.ALPHABETIC).backgroundColorHex)
    }

    @Test
    fun keyboardStateTransitions_shiftAndLanguageAndModes() {
        var state = KeyboardEngineState()
        assertEquals(KeyboardLanguage.ENGLISH, state.language)
        assertEquals(ShiftState.UNSHIFTED, state.shiftState)

        // Single tap shift -> SHIFTED_SINGLE
        state = state.onShiftClicked(isDoubleTap = false)
        assertEquals(ShiftState.SHIFTED_SINGLE, state.shiftState)
        assertTrue(state.isShifted)

        // Character committed -> returns to UNSHIFTED
        state = state.afterCharacterCommitted()
        assertEquals(ShiftState.UNSHIFTED, state.shiftState)

        // Double tap shift -> CAPS_LOCK (persists after character commit)
        state = state.onShiftClicked(isDoubleTap = true)
        assertEquals(ShiftState.CAPS_LOCK, state.shiftState)
        state = state.afterCharacterCommitted()
        assertEquals(ShiftState.CAPS_LOCK, state.shiftState)

        // Toggle language -> BENGALI
        state = state.onLanguageToggled()
        assertEquals(KeyboardLanguage.BENGALI, state.language)
        assertEquals(KeyboardMode.ALPHABET, state.mode)
    }

    @Test
    fun keyboardLayouts_englishAndBengaliContainMandatoryKeysAndUnicode() {
        val layoutCfg = LayoutConfig(showNumberRow = true)
        val enState = KeyboardEngineState(language = KeyboardLanguage.ENGLISH)
        val enRows = KeyboardLayouts.buildRows(enState, layoutCfg)
        assertEquals(5, enRows.size) // 1 number row + 4 QWERTY rows

        val allEnKeys = enRows.flatMap { it.keys }
        assertTrue(allEnKeys.any { it.actionType == KeyActionType.BACKSPACE })
        assertTrue(allEnKeys.any { it.actionType == KeyActionType.SPACE })
        assertTrue(allEnKeys.any { it.actionType == KeyActionType.ENTER })
        assertTrue(allEnKeys.any { it.actionType == KeyActionType.SHIFT })

        // Bengali unshifted layout check (vowel signs, Hasant, consonants)
        val bnState = KeyboardEngineState(language = KeyboardLanguage.BENGALI)
        val bnRows = KeyboardLayouts.buildRows(bnState, layoutCfg)
        val allBnLabels = bnRows.flatMap { it.keys }.map { it.primaryLabel }
        assertTrue(allBnLabels.contains("ক"))
        assertTrue(allBnLabels.contains("া"))
        assertTrue(allBnLabels.contains("্"))

        // Bengali shifted layout check (independent vowels & conjuncts)
        val bnShiftedRows = KeyboardLayouts.buildRows(
            bnState.copy(shiftState = ShiftState.SHIFTED_SINGLE),
            layoutCfg
        )
        val shiftedLabels = bnShiftedRows.flatMap { it.keys }.map { it.primaryLabel }
        assertTrue(shiftedLabels.contains("অ"))
        assertTrue(shiftedLabels.contains("আ"))
        assertTrue(shiftedLabels.contains("ক্ষ"))
    }

    @Test
    fun offlineSynthesizer_createsValidThemeFromNaturalLanguagePrompt() {
        val generated = DefaultThemes.synthesizeFromDescription(
            "Create a crimson red cyberpunk keyboard with rounded pill keys and bold white letters"
        )
        assertNotNull(generated)
        assertEquals("#FF1744", generated.defaultKeyStyle.pressedBackgroundColorHex)
        assertEquals("#FFFFFF", generated.defaultKeyStyle.labelColorHex)
        assertEquals(18f, generated.defaultKeyStyle.cornerRadiusDp, 0.1f)
        assertTrue(ColorAndImageUtils.calculateContrastRatio(
            generated.defaultKeyStyle.labelColorHex,
            generated.defaultKeyStyle.backgroundColorHex
        ) > 4.5)
    }
}
