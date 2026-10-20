package com.example.keyboard.phonetic

import java.util.Locale

/**
 * Pure Kotlin, offline-capable, high-speed phonetic English-to-Bangla transliteration engine.
 * Supports Avro-compatible phonetic mappings, exact consonant conjuncts (যুক্তাক্ষর),
 * vowel signs (কার), and diacritics:
 * - "ami" -> "আমি"
 * - "amar" -> "আমার"
 * - "bangla" -> "বাংলা"
 * - "tumi" -> "তুমি"
 * - "kemon" -> "কেমন"
 * - "acho" -> "আছো"
 * - "dhonnobad" -> "ধন্যবাদ"
 * - "desh" -> "দেশ"
 * - "shonar" -> "সোনার"
 * - "nam" -> "নাম"
 */
object BanglaPhoneticEngine {

    // High-frequency dictionary for instantaneous, 100% natural conversational Bangla
    private val COMMON_WORDS_MAP: Map<String, String> = mapOf(
        "ami" to "আমি",
        "amar" to "আমার",
        "amader" to "আমাদের",
        "amake" to "আমাকে",
        "tumi" to "তুমি",
        "tomar" to "তোমার",
        "tomader" to "তোমাদের",
        "tomake" to "তোমাকে",
        "tui" to "তুই",
        "tor" to "তোর",
        "toke" to "তোকে",
        "apni" to "আপনি",
        "apnar" to "আপনার",
        "apnader" to "আপনাদের",
        "apnake" to "আপনাকে",
        "she" to "সে",
        "tar" to "তার",
        "take" to "তাকে",
        "tader" to "তাদের",
        "tini" to "তিনি",
        "tahar" to "তাহার",
        "kemon" to "কেমন",
        "acho" to "আছো",
        "achen" to "আছেন",
        "achis" to "আছিস",
        "bangla" to "বাংলা",
        "bangladesh" to "বাংলাদেশ",
        "bengali" to "বাঙালি",
        "dhonnobad" to "ধন্যবাদ",
        "dhonnobaad" to "ধন্যবাদ",
        "bhalo" to "ভালো",
        "valo" to "ভালো",
        "bhalobashi" to "ভালোবাসি",
        "valobashi" to "ভালোবাসি",
        "valobasi" to "ভালোবাসি",
        "ki" to "কী",
        "kire" to "কিরে",
        "keno" to "কেন",
        "kokhon" to "কখন",
        "kothay" to "কোথায়",
        "kivabe" to "কীভাবে",
        "ke" to "কে",
        "kara" to "কারা",
        "kake" to "কাকে",
        "konta" to "কোনটা",
        "kon" to "কোন",
        "shob" to "সব",
        "sob" to "সব",
        "shokol" to "সকল",
        "onek" to "অনেক",
        "ektu" to "একটু",
        "khub" to "খুব",
        "beshi" to "বেশি",
        "kom" to "কম",
        "shundor" to "সুন্দর",
        "sundor" to "সুন্দর",
        "shanti" to "শান্তি",
        "mukti" to "মুক্তি",
        "desh" to "দেশ",
        "shonar" to "সোনার",
        "sonar" to "সোনার",
        "nam" to "নাম",
        "naam" to "নাম",
        "hobe" to "হবে",
        "hoy" to "হয়",
        "hoyeche" to "হয়েছে",
        "hyeche" to "হয়েছে",
        "honi" to "হয়নি",
        "korbo" to "করবো",
        "kori" to "করি",
        "koro" to "করো",
        "koren" to "করেন",
        "koris" to "করিস",
        "korlam" to "করলাম",
        "korechi" to "করেছি",
        "korchi" to "করছি",
        "jai" to "যাই",
        "jabo" to "যাব",
        "jabe" to "যাবে",
        "jachhi" to "যাচ্ছি",
        "jachhe" to "যাচ্ছে",
        "gelam" to "গেলাম",
        "gechi" to "গেছি",
        "geche" to "গেছে",
        "khai" to "খাই",
        "khabo" to "খাব",
        "kheyechi" to "খেয়েছি",
        "khaccho" to "খাচ্ছ",
        "khabar" to "খাবার",
        "pani" to "পানি",
        "jol" to "জল",
        "bhat" to "ভাত",
        "cha" to "চা",
        "dekhi" to "দেখি",
        "dekho" to "দেখো",
        "dekhen" to "দেখেন",
        "dekhte" to "দেখতে",
        "dekhbo" to "দেখব",
        "dekhlam" to "দেখলাম",
        "parbo" to "পারবো",
        "pari" to "পারি",
        "paro" to "পারো",
        "paren" to "পারেন",
        "shokal" to "সকাল",
        "sokal" to "সকাল",
        "shuvo" to "শুভ",
        "suvo" to "শুভ",
        "raat" to "রাত",
        "shondha" to "সন্ধ্যা",
        "sondha" to "সন্ধ্যা",
        "bikel" to "বিকেল",
        "dupur" to "দুপুর",
        "kaaj" to "কাজ",
        "kaj" to "কাজ",
        "bari" to "বাড়ি",
        "basha" to "বাসা",
        "bondhu" to "বন্ধু",
        "bondhura" to "বন্ধুরা",
        "kichu" to "কিছু",
        "sobuj" to "সবুজ",
        "lal" to "লাল",
        "shada" to "সাদা",
        "kalo" to "কালো",
        "akash" to "আকাশ",
        "nodi" to "নদী",
        "pakhi" to "পাখি",
        "phul" to "ফুল",
        "ful" to "ফুল",
        "brikkho" to "বৃক্ষ",
        "shikkhok" to "শিক্ষক",
        "chhatro" to "ছাত্র",
        "biddaloy" to "বিদ্যালয়",
        "boi" to "বই",
        "kolom" to "কলম",
        "khata" to "খাতা",
        "gaan" to "গান",
        "kobita" to "কবিতা",
        "golpo" to "গল্প",
        "somoy" to "সময়",
        "juddho" to "যুদ্ধ",
        "bijoy" to "বিজয়",
        "swadhinota" to "স্বাধীনতা",
        "bhasha" to "ভাষা",
        "shahid" to "শহীদ"
    )

    // Multi-character consonant conjunct patterns sorted by pattern length descending
    // Supports case sensitivity for retroflexes (T vs t, D vs d, N vs n, R vs r, S/Sh vs s)
    private val CONSONANT_MAP: List<Pair<String, String>> = listOf(
        "kkhy" to "ক্ষ্য",
        "kkh" to "ক্ষ",
        "ksh" to "ক্ষ",
        "jny" to "জ্ঞ",
        "ggy" to "জ্ঞ",
        "gya" to "জ্ঞ",
        "ngkh" to "ঙ্খ",
        "nggh" to "ঙ্ঘ",
        "ngk" to "ঙ্ক",
        "ngg" to "ঙ্গ",
        "nchh" to "ঞ্ছ",
        "nch" to "ঞ্চ",
        "njh" to "ঞঝ",
        "nj" to "ঞ্জ",
        "nTh" to "ণ্ঠ",
        "nT" to "ণ্ট",
        "nDh" to "ণ্ঢ",
        "nD" to "ণ্ড",
        "nN" to "ণ্ণ",
        "shsh" to "শশ",
        "ShSh" to "ষষ",
        "thth" to "থ্থ",
        "dhdh" to "ধ্ধ",
        "phph" to "ফ্ফ",
        "bhbh" to "ভ্ভ",
        "cchw" to "চ্ছ্ব",
        "cch" to "চ্ছ",
        "jjh" to "জ্ঝ",
        "tth" to "ট্ঠ",
        "ddh" to "দ্ধ",
        "bndh" to "বন্ধ",
        "ndh" to "ন্ধ",
        "shch" to "শ্চ",
        "shth" to "ষ্ঠ",
        "sht" to "ষ্ট",
        "mph" to "ম্ফ",
        "mbh" to "ম্ভ",
        "tt" to "ত্ত",
        "dd" to "দ্দ",
        "nn" to "ন্ন",
        "pp" to "প্প",
        "bb" to "ব্ব",
        "mm" to "ম্ম",
        "yy" to "য্য",
        "rr" to "র্র",
        "ll" to "ল্ল",
        "ss" to "স্স",
        "hh" to "হহ",
        "kt" to "ক্ত",
        "ks" to "ক্স",
        "gd" to "গ্দ",
        "ghn" to "ঘ্ন",
        "kh" to "খ",
        "kH" to "খ",
        "gh" to "ঘ",
        "ng" to "ঙ",
        "Ng" to "ঙ",
        "ch" to "চ",
        "Ch" to "ছ",
        "chh" to "ছ",
        "jh" to "ঝ",
        "Th" to "ঠ",
        "th" to "থ",
        "Dh" to "ঢ",
        "dh" to "ধ",
        "ph" to "ফ",
        "bh" to "ভ",
        "sh" to "শ",
        "Sh" to "ষ",
        "rh" to "ঢ়",
        "Rh" to "ঢ়",
        "t`" to "ৎ",
        "k" to "ক",
        "g" to "গ",
        "j" to "জ",
        "t" to "ত",
        "T" to "ট",
        "d" to "দ",
        "D" to "ড",
        "n" to "ন",
        "N" to "ণ",
        "p" to "প",
        "f" to "ফ",
        "b" to "ব",
        "v" to "ভ",
        "m" to "ম",
        "z" to "য",
        "Z" to "য",
        "r" to "র",
        "R" to "ড়",
        "l" to "ল",
        "s" to "স",
        "S" to "শ",
        "h" to "হ",
        "y" to "য়",
        "w" to "ওয়",
        "x" to "ক্স"
    )

    // Independent initial vowels
    private val INITIAL_VOWELS: List<Pair<String, String>> = listOf(
        "aa" to "আ",
        "ee" to "ঈ",
        "oo" to "ঊ",
        "oi" to "ঐ",
        "ou" to "ঔ",
        "ri" to "ঋ",
        "a" to "অ",
        "i" to "ই",
        "u" to "উ",
        "e" to "এ",
        "o" to "ও"
    )

    // Dependent vowel signs (কার) after a consonant
    private val VOWEL_SIGNS: List<Pair<String, String>> = listOf(
        "aa" to "া",
        "ee" to "ী",
        "oo" to "ূ",
        "oi" to "ৈ",
        "ou" to "ৌ",
        "ri" to "ৃ",
        "a" to "া",
        "i" to "ি",
        "u" to "ু",
        "e" to "ে",
        "o" to "ো"
    )

    /**
     * Given an English string (e.g. "ami", "bangla", "kemon"), returns a ranked list
     * of Bengali suggestions. The primary candidate is the top suggestion.
     */
    fun transliterateWord(input: String): List<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lower = trimmed.lowercase(Locale.US)
        val suggestions = mutableListOf<String>()

        // 1. Direct dictionary match
        COMMON_WORDS_MAP[lower]?.let {
            suggestions.add(it)
        }

        // 2. Rule-based algorithmic transliteration
        val algorithmic = ruleBasedTransliterate(trimmed)
        if (algorithmic.isNotEmpty() && !suggestions.contains(algorithmic)) {
            suggestions.add(algorithmic)
        }

        // 3. Alternative vowel-ending variant
        if (lower.endsWith("o")) {
            val alt = ruleBasedTransliterate(trimmed.dropLast(1))
            if (alt.isNotEmpty() && !suggestions.contains(alt)) {
                suggestions.add(alt)
            }
        }

        // 4. Special cases like "bangla" -> "বাংলা"
        if (lower == "bangla" && !suggestions.contains("বাংলা")) {
            suggestions.add(0, "বাংলা")
        }

        // Always provide original word as fallback option
        if (!suggestions.contains(trimmed)) {
            suggestions.add(trimmed)
        }

        return suggestions
    }

    /**
     * Transliterates multi-word phrases (e.g. "amar nam", "tumi kemon acho")
     */
    fun transliteratePhrase(phrase: String): String {
        val words = phrase.split(Regex("\\s+"))
        return words.joinToString(" ") { word ->
            if (word.isBlank()) ""
            else transliterateWord(word).firstOrNull() ?: word
        }
    }

    private fun ruleBasedTransliterate(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        var isConsonantJustWritten = false

        while (i < raw.length) {
            // Check for punctuation / digits / diacritics
            val ch = raw[i]
            if (ch in '0'..'9') {
                val bnDigit = '০' + (ch - '0')
                sb.append(bnDigit)
                isConsonantJustWritten = false
                i++
                continue
            }
            if (ch == '^') {
                sb.append("ঁ") // Candrabindu
                isConsonantJustWritten = false
                i++
                continue
            }
            if (ch == ':') {
                sb.append("ঃ") // Visarga
                isConsonantJustWritten = false
                i++
                continue
            }
            if (ch == ' ' || ch == '.' || ch == ',' || ch == '?' || ch == '!') {
                sb.append(ch)
                isConsonantJustWritten = false
                i++
                continue
            }

            // If a consonant was just written, check if next is a vowel sign
            if (isConsonantJustWritten) {
                var vowelSignFound = false
                for ((vKey, vSign) in VOWEL_SIGNS) {
                    if (raw.startsWith(vKey, i, ignoreCase = true)) {
                        // "a" after a consonant can be implicit or explicit 'া'
                        if (vKey == "a") {
                            sb.append("া")
                        } else {
                            sb.append(vSign)
                        }
                        i += vKey.length
                        vowelSignFound = true
                        isConsonantJustWritten = false
                        break
                    }
                }
                if (vowelSignFound) continue

                // Check for Hasant/conjunct with another consonant
                var nextConsonantFound = false
                for ((cKey, cVal) in CONSONANT_MAP) {
                    val matches = if (cKey.any { it.isUpperCase() }) {
                        raw.startsWith(cKey, i, ignoreCase = false)
                    } else {
                        raw.startsWith(cKey, i, ignoreCase = true)
                    }
                    if (matches) {
                        // If user types two consonants together without a vowel, join with Hasant (্)
                        // Exception: 'r' after consonant becomes র-ফলা (্র), 'y' becomes য-ফলা (্য)
                        when (cKey.lowercase(Locale.US)) {
                            "r" -> sb.append("্র")
                            "y" -> sb.append("্য")
                            else -> {
                                sb.append("্")
                                sb.append(cVal)
                            }
                        }
                        i += cKey.length
                        isConsonantJustWritten = true
                        nextConsonantFound = true
                        break
                    }
                }
                if (nextConsonantFound) continue
            }

            // Check for initial vowels if not right after a consonant
            var initialVowelFound = false
            for ((vKey, vChar) in INITIAL_VOWELS) {
                if (raw.startsWith(vKey, i, ignoreCase = true)) {
                    sb.append(vChar)
                    i += vKey.length
                    isConsonantJustWritten = false
                    initialVowelFound = true
                    break
                }
            }
            if (initialVowelFound) continue

            // Check consonants
            var consonantFound = false
            for ((cKey, cVal) in CONSONANT_MAP) {
                val matches = if (cKey.any { it.isUpperCase() }) {
                    raw.startsWith(cKey, i, ignoreCase = false)
                } else {
                    raw.startsWith(cKey, i, ignoreCase = true)
                }
                if (matches) {
                    sb.append(cVal)
                    i += cKey.length
                    isConsonantJustWritten = true
                    consonantFound = true
                    break
                }
            }
            if (consonantFound) continue

            // Unrecognized character: append as-is
            sb.append(ch)
            isConsonantJustWritten = false
            i++
        }

        return sb.toString()
    }
}
