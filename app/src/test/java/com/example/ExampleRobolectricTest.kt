package com.example

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import com.example.keyboard.layout.KeyboardLayouts
import com.example.keyboard.service.ToxicInputMethodService
import com.example.keyboard.state.EditorFieldContext
import com.example.keyboard.state.KeyboardEngineState
import com.example.keyboard.state.KeyboardLanguage
import com.example.keyboard.state.KeyboardMode
import com.example.keyboard.state.ShiftState
import com.example.keyboard.ui.NativeKeyboardRendererView
import com.example.theme.importexport.PortableThemePackage
import com.example.theme.importexport.ThemeImportExportManager
import com.example.theme.model.KeyCategory
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.ThemeRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34])
class ExampleRobolectricTest {

    @Test
    fun keyboardLayouts_generateEnglishAndBengaliRowsCorrectly() {
        val theme = KeyboardTheme(id = "test_default", name = "Toxic Emerald Cyber")

        val enState = KeyboardEngineState(
            language = KeyboardLanguage.ENGLISH,
            mode = KeyboardMode.ALPHABET,
            shiftState = ShiftState.UNSHIFTED
        )
        val enRows = KeyboardLayouts.buildRows(enState, theme.layout)
        assertEquals("English layout with number row should have 5 rows", 5, enRows.size)
        assertEquals("q", enRows[1].keys[0].outputText)

        // Verify Complete Bengali Unicode Layout (independent vowels, vowel signs, conjuncts, consonants, numerals)
        val bnState = enState.onLanguageToggled()
        assertEquals(KeyboardLanguage.BENGALI, bnState.language)
        val bnRows = KeyboardLayouts.buildRows(bnState, theme.layout)
        assertEquals("Bengali layout should have 5 rows including number & vowel sign rows", 5, bnRows.size)

        val bnShiftedRows = KeyboardLayouts.buildRows(bnState.onShiftClicked(isDoubleTap = false), theme.layout)
        val bnSymbolsRows = KeyboardLayouts.buildRows(bnState.onModeChanged(KeyboardMode.SYMBOLS_primary), theme.layout)

        val allBnOutputs: Set<String> = (bnRows + bnShiftedRows + bnSymbolsRows)
            .flatMap { row -> row.keys }
            .flatMap { key ->
                listOfNotNull(key.primaryLabel, key.outputText, key.secondaryOutputText)
            }
            .toSet()

        // Verify independent vowels
        listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ").forEach { vowel ->
            assertTrue("Bengali layout must include independent vowel $vowel", allBnOutputs.contains(vowel))
        }
        // Verify dependent vowel signs (কার)
        listOf("া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ").forEach { sign ->
            assertTrue("Bengali layout must include dependent vowel sign $sign", allBnOutputs.contains(sign))
        }
        // Verify Hasant, phalas, diacritics, and punctuation
        listOf("্", "্র", "্য", "ং", "ঃ", "ঁ", "ৎ", "।", "৳").forEach { mark ->
            assertTrue("Bengali layout must include mark/conjunct helper $mark", allBnOutputs.contains(mark))
        }
        // Verify Bengali digits
        listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০").forEach { digit ->
            assertTrue("Bengali layout must include digit $digit", allBnOutputs.contains(digit))
        }
    }

    @Test
    fun toxicInputMethodService_createsNonZeroVisibleInputViewAndForcesDisplay() {
        val serviceController = Robolectric.buildService(ToxicInputMethodService::class.java)
        val service = serviceController.create().get()

        assertTrue(
            "Service must always evaluate input view shown as true even when emulator/hardware keyboard is present",
            service.onEvaluateInputViewShown()
        )
        assertFalse(
            "Service must disable fullscreen extract mode so custom keyboard is always visible",
            service.onEvaluateFullscreenMode()
        )

        val inputView = service.onCreateInputView()
        assertNotNull("onCreateInputView must return a non-null View", inputView)
        assertEquals("InputView must be VISIBLE", View.VISIBLE, inputView.visibility)
        assertTrue("InputView must be NativeKeyboardRendererView", inputView is NativeKeyboardRendererView)

        // Measure the input view as Android's IME container does
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST)
        inputView.measure(widthSpec, heightSpec)
        assertTrue("Measured width must be > 0", inputView.measuredWidth > 0)
        assertTrue("Measured height must be > 100", inputView.measuredHeight > 100)

        // Start input view with an EditorInfo
        val editorInfo = EditorInfo().apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        service.onStartInput(editorInfo, false)
        service.onStartInputView(editorInfo, false)
        assertEquals(View.VISIBLE, inputView.visibility)
    }

    @Test
    fun themeSerializationAndCategoryFallback_resolvesPerKeyAndCategoryStyles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseTheme = KeyboardTheme(id = "test_theme", name = "Base Theme")

        val customized = baseTheme.copy(
            name = "Custom Space & Q Theme",
            categoryStyles = baseTheme.categoryStyles + (
                KeyCategory.SPACEBAR.name to baseTheme.defaultKeyStyle.copy(backgroundColorHex = "#FF1744")
            ),
            individualKeyOverrides = mapOf(
                "EN_Q" to baseTheme.defaultKeyStyle.copy(backgroundColorHex = "#00E5FF")
            )
        )

        assertEquals("#00E5FF", customized.styleForKey("en_q", KeyCategory.ALPHABETIC).backgroundColorHex)
        assertEquals("#FF1744", customized.styleForKey("key_space", KeyCategory.SPACEBAR).backgroundColorHex)

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(PortableThemePackage::class.java)
        val json = adapter.toJson(PortableThemePackage(theme = customized))
        assertTrue(json.contains("Custom Space & Q Theme"))

        val parsed = ThemeImportExportManager.parseAndValidateJsonString(context, json)
        assertTrue(parsed.isSuccess)
        assertEquals(
            "#00E5FF",
            parsed.getOrThrow().theme.styleForKey("en_q", KeyCategory.ALPHABETIC).backgroundColorHex
        )
    }

    @Test
    fun sensitiveInputFieldDetection_blocksPasswordAndPinFields() {
        val passwordEditor = EditorInfo().apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue(EditorFieldContext.fromEditorInfo(passwordEditor).isPasswordOrSensitive)

        val normalEditor = EditorInfo().apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_NORMAL
        }
        assertFalse(EditorFieldContext.fromEditorInfo(normalEditor).isPasswordOrSensitive)
    }

    @Test
    fun singletonThemeRepository_sharesLiveUpdatesBetweenStudioAndIme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = ThemeRepository.getInstance(context)
        val repo2 = ThemeRepository.getInstance(context)
        assertTrue("ThemeRepository.getInstance must return shared singleton", repo1 === repo2)
    }

    @Test
    fun banglaPhoneticEngine_transliteratesAvroPhrasesAndWordsCorrectly() {
        // High-frequency words
        assertEquals("আমি", com.example.keyboard.phonetic.BanglaPhoneticEngine.transliterateWord("ami").first())
        assertEquals("বাংলা", com.example.keyboard.phonetic.BanglaPhoneticEngine.transliterateWord("bangla").first())
        assertEquals("কেমন", com.example.keyboard.phonetic.BanglaPhoneticEngine.transliterateWord("kemon").first())
        assertEquals("ধন্যবাদ", com.example.keyboard.phonetic.BanglaPhoneticEngine.transliterateWord("dhonnobad").first())
        assertEquals("সোনার", com.example.keyboard.phonetic.BanglaPhoneticEngine.transliterateWord("shonar").first())

        // Transliterate phrase
        val phrase = com.example.keyboard.phonetic.BanglaPhoneticEngine.transliteratePhrase("ami bangla bhalobashi")
        assertTrue(phrase.contains("আমি"))
        assertTrue(phrase.contains("বাংলা"))
        assertTrue(phrase.contains("ভালোবাসি"))
    }

    @Test
    fun fontManager_validatesFontHeadersCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempTtf = java.io.File(context.cacheDir, "sample_test.ttf").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        }
        assertTrue("TrueType header must validate", com.example.fonts.FontManager.isValidFontHeader(tempTtf))

        val tempOtf = java.io.File(context.cacheDir, "sample_test.otf").apply {
            writeBytes(byteArrayOf('O'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(), 'O'.code.toByte(), 0, 0, 0, 0, 0, 0, 0, 0))
        }
        assertTrue("OpenType header must validate", com.example.fonts.FontManager.isValidFontHeader(tempOtf))

        val invalidFile = java.io.File(context.cacheDir, "sample_test.bad").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        }
        assertFalse("Random bytes must not validate as font", com.example.fonts.FontManager.isValidFontHeader(invalidFile))

        tempTtf.delete()
        tempOtf.delete()
        invalidFile.delete()
    }
}
