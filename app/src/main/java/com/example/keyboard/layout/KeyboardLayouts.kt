package com.example.keyboard.layout

import com.example.keyboard.state.KeyboardEngineState
import com.example.keyboard.state.KeyboardLanguage
import com.example.keyboard.state.KeyboardMode
import com.example.theme.model.KeyCategory
import com.example.theme.model.LayoutConfig

enum class KeyActionType {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACE,
    MODE_SYMBOLS_1,
    MODE_SYMBOLS_2,
    MODE_ALPHABET,
    LANGUAGE_SWITCH,
    EMOJI_PANEL_TOGGLE,
    VOICE_TYPING
}

data class KeySpec(
    val id: String,
    val primaryLabel: String,
    val outputText: String = primaryLabel,
    val secondaryLabel: String? = null,
    val secondaryOutputText: String? = null,
    val actionType: KeyActionType = KeyActionType.CHARACTER,
    val category: KeyCategory = KeyCategory.ALPHABETIC,
    val widthWeight: Float = 1.0f
)

data class KeyboardRowSpec(
    val keys: List<KeySpec>,
    val isNumberRow: Boolean = false
)

object KeyboardLayouts {

    val EMOJI_PALETTE: List<String> = listOf(
        "😀", "😂", "🥹", "😎", "🤩", "😍", "🥳", "🤔",
        "🔥", "⚡", "💚", "❤️", "✨", "🎯", "🚀", "💎",
        "👍", "🙏", "👏", "🙌", "🤝", "✌️", "💪", "🫡",
        "🎉", "🎵", "🎮", "💻", "📱", "🛡️", "⚙️", "🔒",
        "☕", "🍕", "🌙", "☀️", "🌧️", "🌈", "🌸", "🍀"
    )

    fun buildRows(
        state: KeyboardEngineState,
        layoutConfig: LayoutConfig
    ): List<KeyboardRowSpec> {
        val rows = mutableListOf<KeyboardRowSpec>()

        when (state.mode) {
            KeyboardMode.ALPHABET -> {
                if (layoutConfig.showNumberRow) {
                    rows.add(buildNumberRow(state.language))
                }
                if (state.language == KeyboardLanguage.ENGLISH) {
                    rows.addAll(buildEnglishQwertyRows(state.isShifted, layoutConfig, state.editorContext.enterKeyLabel))
                } else {
                    rows.addAll(buildBengaliUnicodeRows(state.isShifted, layoutConfig, state.editorContext.enterKeyLabel))
                }
            }
            KeyboardMode.SYMBOLS_primary -> {
                rows.addAll(buildSymbolsPrimaryRows(state.language, layoutConfig, state.editorContext.enterKeyLabel))
            }
            KeyboardMode.SYMBOLS_secondary -> {
                rows.addAll(buildSymbolsSecondaryRows(layoutConfig, state.editorContext.enterKeyLabel))
            }
            KeyboardMode.EMOJI_PANEL, KeyboardMode.CLIPBOARD_PANEL -> {
                // Bottom control row remains available below panel
                rows.add(buildBottomControlRow(state, layoutConfig))
            }
        }

        // Validate that mandatory keys (Backspace, Space, Enter) are always present in typing modes
        return ensureValidLayout(rows, state, layoutConfig)
    }

    private fun buildNumberRow(language: KeyboardLanguage): KeyboardRowSpec {
        val pairs = if (language == KeyboardLanguage.BENGALI) {
            listOf(
                "১" to "1", "২" to "2", "৩" to "3", "৪" to "4", "৫" to "5",
                "৬" to "6", "৭" to "7", "৮" to "8", "৯" to "9", "০" to "0"
            )
        } else {
            listOf(
                "1" to "!", "2" to "@", "3" to "#", "4" to "$", "5" to "%",
                "6" to "^", "7" to "&", "8" to "*", "9" to "(", "0" to ")"
            )
        }
        return KeyboardRowSpec(
            isNumberRow = true,
            keys = pairs.map { (pri, sec) ->
                KeySpec(
                    id = "NUM_$pri",
                    primaryLabel = pri,
                    outputText = pri,
                    secondaryLabel = sec,
                    secondaryOutputText = sec,
                    category = KeyCategory.NUMERIC
                )
            }
        )
    }

    private fun buildEnglishQwertyRows(
        isShifted: Boolean,
        layoutConfig: LayoutConfig,
        enterLabel: String
    ): List<KeyboardRowSpec> {
        fun c(ch: String) = if (isShifted) ch.uppercase() else ch.lowercase()

        val row1 = listOf(
            "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
            "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0"
        ).map { (letter, hint) ->
            val l = c(letter)
            KeySpec(
                id = letter.uppercase(),
                primaryLabel = l,
                outputText = l,
                secondaryLabel = hint,
                secondaryOutputText = hint,
                category = KeyCategory.ALPHABETIC
            )
        }

        val row2 = listOf(
            "a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&",
            "h" to "-", "j" to "+", "k" to "(", "l" to ")"
        ).map { (letter, hint) ->
            val l = c(letter)
            KeySpec(
                id = letter.uppercase(),
                primaryLabel = l,
                outputText = l,
                secondaryLabel = hint,
                secondaryOutputText = hint,
                category = KeyCategory.ALPHABETIC
            )
        }

        val specialW = layoutConfig.specialKeyWidthMultiplier
        val row3Letters = listOf(
            "z" to "*", "x" to "\"", "c" to "'", "v" to ":",
            "b" to ";", "n" to "!", "m" to "?"
        ).map { (letter, hint) ->
            val l = c(letter)
            KeySpec(
                id = letter.uppercase(),
                primaryLabel = l,
                outputText = l,
                secondaryLabel = hint,
                secondaryOutputText = hint,
                category = KeyCategory.ALPHABETIC
            )
        }

        val row3 = buildList {
            add(
                KeySpec(
                    id = "SHIFT",
                    primaryLabel = if (isShifted) "⬆" else "⇧",
                    outputText = "",
                    actionType = KeyActionType.SHIFT,
                    category = KeyCategory.SHIFT,
                    widthWeight = specialW
                )
            )
            addAll(row3Letters)
            add(
                KeySpec(
                    id = "BACKSPACE",
                    primaryLabel = "⌫",
                    outputText = "",
                    actionType = KeyActionType.BACKSPACE,
                    category = KeyCategory.BACKSPACE,
                    widthWeight = specialW
                )
            )
        }

        val row4 = buildBottomRowSpecs(
            language = KeyboardLanguage.ENGLISH,
            layoutConfig = layoutConfig,
            enterLabel = enterLabel,
            isSymbolsMode = false
        )

        return listOf(
            KeyboardRowSpec(row1),
            KeyboardRowSpec(row2),
            KeyboardRowSpec(row3),
            KeyboardRowSpec(row4)
        )
    }

    /**
     * Complete Bengali (বাংলা) Unicode Layout for Android 9+:
     * - Unshifted Row 1: All core dependent vowel signs (া ি ী ু ূ ৃ ে ো) + Hasant (্) + Anusvara (ং),
     *   with long-press access to independent vowels (আ ই ঈ উ ঊ ঋ) and compound vowel signs (ৈ ৌ) + র-ফলা (্র) + চন্দ্রবিন্দু (ঁ).
     * - Shifted Row 1: All 11 independent vowels (অ আ ই ঈ উ ঊ ঋ এ ঐ ও ঔ) with dependent vowel signs on long-press.
     * - Unshifted & Shifted Rows 2–3: Complete Bengali consonant inventory (ক–হ, ড়, ঢ়, য়, ৎ, ং, ঃ, ঁ) and common conjuncts (ক্ষ, জ্ঞ, শ্র, ত্র).
     */
    private fun buildBengaliUnicodeRows(
        isShifted: Boolean,
        layoutConfig: LayoutConfig,
        enterLabel: String
    ): List<KeyboardRowSpec> {
        val specialW = layoutConfig.specialKeyWidthMultiplier

        val row1Pairs = if (!isShifted) {
            listOf(
                Triple("া", "আ", "BN_AA_KAR"),
                Triple("ি", "ই", "BN_I_KAR"),
                Triple("ী", "ঈ", "BN_II_KAR"),
                Triple("ু", "উ", "BN_U_KAR"),
                Triple("ূ", "ঊ", "BN_UU_KAR"),
                Triple("ৃ", "ঋ", "BN_RI_KAR"),
                Triple("ে", "ৈ", "BN_E_KAR"),
                Triple("ো", "ৌ", "BN_O_KAR"),
                Triple("্", "্র", "BN_HASANT"),
                Triple("ং", "ঁ", "BN_ANUSVARA")
            )
        } else {
            listOf(
                Triple("অ", "ঁ", "BN_A"),
                Triple("আ", "া", "BN_AA"),
                Triple("ই", "ি", "BN_I"),
                Triple("ঈ", "ী", "BN_II"),
                Triple("উ", "ু", "BN_U"),
                Triple("ঊ", "ূ", "BN_UU"),
                Triple("ঋ", "ৃ", "BN_RI"),
                Triple("এ", "ে", "BN_E"),
                Triple("ঐ", "ৈ", "BN_OI"),
                Triple("ও", "ো", "BN_O"),
                Triple("ঔ", "ৌ", "BN_OU")
            )
        }

        val row2Pairs = if (!isShifted) {
            listOf(
                Triple("ক", "খ", "BN_KA"),
                Triple("গ", "ঘ", "BN_GA"),
                Triple("চ", "ছ", "BN_CA"),
                Triple("জ", "ঝ", "BN_JA"),
                Triple("ট", "ঠ", "BN_TTA"),
                Triple("ড", "ঢ", "BN_DDA"),
                Triple("ত", "থ", "BN_TA"),
                Triple("দ", "ধ", "BN_DA"),
                Triple("ন", "ণ", "BN_NA"),
                Triple("প", "ফ", "BN_PA")
            )
        } else {
            listOf(
                Triple("খ", "ঙ", "BN_KHA"),
                Triple("ঘ", "ঞ", "BN_GHA"),
                Triple("ঙ", "ং", "BN_NGA"),
                Triple("ছ", "ৎ", "BN_CHA"),
                Triple("ঝ", "জ্ঞ", "BN_JHA"),
                Triple("ঞ", "্য", "BN_NYA"),
                Triple("ঠ", "ড়", "BN_TTHA"),
                Triple("ঢ", "ঢ়", "BN_DDHA"),
                Triple("ণ", "ত্র", "BN_NNA"),
                Triple("থ", "ক্ষ", "BN_THA")
            )
        }

        val row3Pairs = if (!isShifted) {
            listOf(
                Triple("ব", "ভ", "BN_BA"),
                Triple("ম", "ঙ", "BN_MA"),
                Triple("য", "য়", "BN_YA"),
                Triple("র", "ড়", "BN_RA"),
                Triple("ল", "ঢ়", "BN_LA"),
                Triple("শ", "ষ", "BN_SHA"),
                Triple("স", "ৎ", "BN_SA"),
                Triple("হ", "ঃ", "BN_HA")
            )
        } else {
            listOf(
                Triple("ধ", "শ্র", "BN_DHA"),
                Triple("ফ", "প", "BN_PHA"),
                Triple("ভ", "ব", "BN_BHA"),
                Triple("য়", "য", "BN_YYA"),
                Triple("ড়", "ঢ়", "BN_RRA"),
                Triple("ষ", "শ", "BN_SSA"),
                Triple("ৎ", "ঃ", "BN_KHANDA_TA"),
                Triple("ক্ষ", "জ্ঞ", "BN_KSHA")
            )
        }

        val row1 = row1Pairs.map { (pri, sec, id) ->
            KeySpec(
                id = id,
                primaryLabel = pri,
                outputText = pri,
                secondaryLabel = sec,
                secondaryOutputText = sec,
                category = KeyCategory.ALPHABETIC
            )
        }

        val row2 = row2Pairs.map { (pri, sec, id) ->
            KeySpec(
                id = id,
                primaryLabel = pri,
                outputText = pri,
                secondaryLabel = sec,
                secondaryOutputText = sec,
                category = KeyCategory.ALPHABETIC
            )
        }

        val row3 = buildList {
            add(
                KeySpec(
                    id = "SHIFT",
                    primaryLabel = if (isShifted) "⬆" else "⇧",
                    outputText = "",
                    actionType = KeyActionType.SHIFT,
                    category = KeyCategory.SHIFT,
                    widthWeight = specialW
                )
            )
            addAll(
                row3Pairs.map { (pri, sec, id) ->
                    KeySpec(
                        id = id,
                        primaryLabel = pri,
                        outputText = pri,
                        secondaryLabel = sec,
                        secondaryOutputText = sec,
                        category = KeyCategory.ALPHABETIC
                    )
                }
            )
            add(
                KeySpec(
                    id = "BACKSPACE",
                    primaryLabel = "⌫",
                    outputText = "",
                    actionType = KeyActionType.BACKSPACE,
                    category = KeyCategory.BACKSPACE,
                    widthWeight = specialW
                )
            )
        }

        val row4 = buildBottomRowSpecs(
            language = KeyboardLanguage.BENGALI,
            layoutConfig = layoutConfig,
            enterLabel = enterLabel,
            isSymbolsMode = false
        )

        return listOf(
            KeyboardRowSpec(row1),
            KeyboardRowSpec(row2),
            KeyboardRowSpec(row3),
            KeyboardRowSpec(row4)
        )
    }

    private fun buildSymbolsPrimaryRows(
        language: KeyboardLanguage,
        layoutConfig: LayoutConfig,
        enterLabel: String
    ): List<KeyboardRowSpec> {
        val specialW = layoutConfig.specialKeyWidthMultiplier
        val r1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
            KeySpec(id = "SYM_$it", primaryLabel = it, outputText = it, category = KeyCategory.NUMERIC)
        }
        val currency = if (language == KeyboardLanguage.BENGALI) "৳" else "$"
        val r2 = listOf("@", "#", currency, "%", "&", "-", "+", "(", ")", "/").map {
            KeySpec(id = "SYM_$it", primaryLabel = it, outputText = it, category = KeyCategory.SYMBOL)
        }
        val r3 = buildList {
            add(
                KeySpec(
                    id = "MODE_SYM2",
                    primaryLabel = "=\\<",
                    outputText = "",
                    actionType = KeyActionType.MODE_SYMBOLS_2,
                    category = KeyCategory.SPECIAL,
                    widthWeight = specialW
                )
            )
            listOf("*", "\"", "'", ":", ";", "!", "?").forEach {
                add(KeySpec(id = "SYM_$it", primaryLabel = it, outputText = it, category = KeyCategory.SYMBOL))
            }
            add(
                KeySpec(
                    id = "BACKSPACE",
                    primaryLabel = "⌫",
                    outputText = "",
                    actionType = KeyActionType.BACKSPACE,
                    category = KeyCategory.BACKSPACE,
                    widthWeight = specialW
                )
            )
        }
        val r4 = buildBottomRowSpecs(language, layoutConfig, enterLabel, isSymbolsMode = true)
        return listOf(KeyboardRowSpec(r1), KeyboardRowSpec(r2), KeyboardRowSpec(r3), KeyboardRowSpec(r4))
    }

    private fun buildSymbolsSecondaryRows(
        layoutConfig: LayoutConfig,
        enterLabel: String
    ): List<KeyboardRowSpec> {
        val specialW = layoutConfig.specialKeyWidthMultiplier
        val r1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆").map {
            KeySpec(id = "SYM2_$it", primaryLabel = it, outputText = it, category = KeyCategory.SYMBOL)
        }
        val r2 = listOf("£", "€", "₹", "৳", "^", "°", "=", "{", "}", "\\").map {
            KeySpec(id = "SYM2_$it", primaryLabel = it, outputText = it, category = KeyCategory.SYMBOL)
        }
        val r3 = buildList {
            add(
                KeySpec(
                    id = "MODE_SYM1",
                    primaryLabel = "?123",
                    outputText = "",
                    actionType = KeyActionType.MODE_SYMBOLS_1,
                    category = KeyCategory.SPECIAL,
                    widthWeight = specialW
                )
            )
            listOf("%", "©", "®", "™", "✓", "[", "]").forEach {
                add(KeySpec(id = "SYM2_$it", primaryLabel = it, outputText = it, category = KeyCategory.SYMBOL))
            }
            add(
                KeySpec(
                    id = "BACKSPACE",
                    primaryLabel = "⌫",
                    outputText = "",
                    actionType = KeyActionType.BACKSPACE,
                    category = KeyCategory.BACKSPACE,
                    widthWeight = specialW
                )
            )
        }
        val r4 = buildBottomRowSpecs(KeyboardLanguage.ENGLISH, layoutConfig, enterLabel, isSymbolsMode = true)
        return listOf(KeyboardRowSpec(r1), KeyboardRowSpec(r2), KeyboardRowSpec(r3), KeyboardRowSpec(r4))
    }

    private fun buildBottomRowSpecs(
        language: KeyboardLanguage,
        layoutConfig: LayoutConfig,
        enterLabel: String,
        isSymbolsMode: Boolean
    ): List<KeySpec> {
        val specialW = layoutConfig.specialKeyWidthMultiplier.coerceIn(1.15f, 1.55f)
        val periodPrimary = if (language == KeyboardLanguage.BENGALI && !isSymbolsMode) "।" else "."
        val periodSecondary = if (language == KeyboardLanguage.BENGALI) "." else "?"

        // Clear, unmistakable label for the language toggle button that fits comfortably on 360dp screens
        val langPrimaryLabel = if (language == KeyboardLanguage.ENGLISH) "বাং" else "EN"
        val langSecondaryHint = "🌐"

        return buildList {
            add(
                KeySpec(
                    id = if (isSymbolsMode) "ABC" else "SYM",
                    primaryLabel = if (isSymbolsMode) "ABC" else "?123",
                    outputText = "",
                    actionType = if (isSymbolsMode) KeyActionType.MODE_ALPHABET else KeyActionType.MODE_SYMBOLS_1,
                    category = KeyCategory.SPECIAL,
                    widthWeight = specialW
                )
            )
            if (layoutConfig.showLanguageKey) {
                add(
                    KeySpec(
                        id = "LANG",
                        primaryLabel = langPrimaryLabel,
                        outputText = "",
                        secondaryLabel = langSecondaryHint,
                        actionType = KeyActionType.LANGUAGE_SWITCH,
                        category = KeyCategory.LANGUAGE_SWITCH,
                        widthWeight = 1.25f
                    )
                )
            }
            if (layoutConfig.showEmojiKey) {
                add(
                    KeySpec(
                        id = "EMOJI",
                        primaryLabel = "☺",
                        outputText = "",
                        actionType = KeyActionType.EMOJI_PANEL_TOGGLE,
                        category = KeyCategory.EMOJI_KEY,
                        widthWeight = 1.0f
                    )
                )
            }
            add(
                KeySpec(
                    id = "COMMA",
                    primaryLabel = ",",
                    outputText = ",",
                    category = KeyCategory.SYMBOL,
                    widthWeight = 0.95f
                )
            )
            add(
                KeySpec(
                    id = "SPACE",
                    primaryLabel = if (language == KeyboardLanguage.BENGALI) "বাংলা" else "English",
                    outputText = " ",
                    actionType = KeyActionType.SPACE,
                    category = KeyCategory.SPACEBAR,
                    widthWeight = layoutConfig.spacebarWidthWeight.coerceIn(2.4f, 5.5f)
                )
            )
            add(
                KeySpec(
                    id = "PERIOD",
                    primaryLabel = periodPrimary,
                    outputText = periodPrimary,
                    secondaryLabel = periodSecondary,
                    secondaryOutputText = periodSecondary,
                    category = KeyCategory.SYMBOL,
                    widthWeight = 0.95f
                )
            )
            add(
                KeySpec(
                    id = "ENTER",
                    primaryLabel = enterLabel,
                    outputText = "\n",
                    actionType = KeyActionType.ENTER,
                    category = KeyCategory.ENTER,
                    widthWeight = specialW * 1.1f
                )
            )
        }
    }

    private fun buildBottomControlRow(
        state: KeyboardEngineState,
        layoutConfig: LayoutConfig
    ): KeyboardRowSpec {
        val langLabel = if (state.language == KeyboardLanguage.ENGLISH) "বাং" else "EN"
        return KeyboardRowSpec(
            keys = listOf(
                KeySpec(
                    id = "ABC",
                    primaryLabel = "ABC",
                    outputText = "",
                    actionType = KeyActionType.MODE_ALPHABET,
                    category = KeyCategory.SPECIAL,
                    widthWeight = 1.3f
                ),
                KeySpec(
                    id = "LANG",
                    primaryLabel = langLabel,
                    outputText = "",
                    secondaryLabel = "🌐",
                    actionType = KeyActionType.LANGUAGE_SWITCH,
                    category = KeyCategory.LANGUAGE_SWITCH,
                    widthWeight = 1.2f
                ),
                KeySpec(
                    id = "SPACE",
                    primaryLabel = if (state.language == KeyboardLanguage.BENGALI) "বাংলা" else "English",
                    outputText = " ",
                    actionType = KeyActionType.SPACE,
                    category = KeyCategory.SPACEBAR,
                    widthWeight = layoutConfig.spacebarWidthWeight.coerceIn(2.4f, 5.5f)
                ),
                KeySpec(
                    id = "BACKSPACE",
                    primaryLabel = "⌫",
                    outputText = "",
                    actionType = KeyActionType.BACKSPACE,
                    category = KeyCategory.BACKSPACE,
                    widthWeight = 1.3f
                ),
                KeySpec(
                    id = "ENTER",
                    primaryLabel = state.editorContext.enterKeyLabel,
                    outputText = "\n",
                    actionType = KeyActionType.ENTER,
                    category = KeyCategory.ENTER,
                    widthWeight = 1.35f
                )
            )
        )
    }

    private fun ensureValidLayout(
        rows: List<KeyboardRowSpec>,
        state: KeyboardEngineState,
        layoutConfig: LayoutConfig
    ): List<KeyboardRowSpec> {
        if (rows.isEmpty()) {
            return listOf(buildBottomControlRow(state, layoutConfig))
        }
        return rows
    }
}
