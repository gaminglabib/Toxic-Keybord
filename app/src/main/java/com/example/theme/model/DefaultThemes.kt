package com.example.theme.model

import java.util.Locale
import java.util.UUID

object DefaultThemes {

    val TOXIC_EMERALD_CORE = KeyboardTheme(
        id = "builtin_toxic_emerald",
        name = "Toxic Emerald Core",
        isBuiltIn = true,
        isDefault = true,
        background = BackgroundConfig(
            sourceType = BackgroundSourceType.PRESET_DRAWABLE,
            baseColorHex = "#050A08",
            secondaryGradientColorHex = "#0B1D14",
            useLinearGradient = true,
            presetDrawableName = "img_bg_toxic_cyber",
            dimmingOverlay = 0.28f
        ),
        defaultKeyStyle = KeyStyleConfig(
            backgroundColorHex = "#12221B",
            pressedBackgroundColorHex = "#00E676",
            enableGradient = true,
            gradientEndColorHex = "#0B1510",
            labelColorHex = "#ECFDF5",
            pressedLabelColorHex = "#03120A",
            borderColorHex = "#1F4433",
            borderWidthDp = 1.0f,
            cornerRadiusDp = 8.0f,
            keyOpacity = 0.94f,
            shadowRadiusDp = 4.0f,
            enableGlow = true,
            glowColorHex = "#00E676",
            glowRadiusDp = 5.0f
        ),
        categoryStyles = KeyboardTheme.defaultCategoryStyles(),
        textStyle = TextStyleConfig(
            fontFamilyId = BuiltInFontFamily.SPACE_GROTESK.id,
            fontWeight = 600,
            fontSizeSp = 18f,
            enableTextShadow = true,
            enableLabelGlow = false
        ),
        layout = LayoutConfig(),
        effects = EffectsConfig(
            enableKeyPressHighlight = true,
            enableKeyPressScalePop = true,
            enableKeyGlow = true,
            enableVibrationFeedback = true
        )
    )

    val CRIMSON_OBSIDIAN = KeyboardTheme(
        id = "builtin_crimson_obsidian",
        name = "Crimson Obsidian Glass",
        isBuiltIn = true,
        isDefault = false,
        background = BackgroundConfig(
            sourceType = BackgroundSourceType.PRESET_DRAWABLE,
            baseColorHex = "#080506",
            secondaryGradientColorHex = "#1A080C",
            useLinearGradient = true,
            presetDrawableName = "img_bg_crimson_glass",
            dimmingOverlay = 0.30f
        ),
        defaultKeyStyle = KeyStyleConfig(
            backgroundColorHex = "#1C1014",
            pressedBackgroundColorHex = "#FF1744",
            enableGradient = true,
            gradientEndColorHex = "#11090C",
            labelColorHex = "#FFF1F2",
            pressedLabelColorHex = "#FFFFFF",
            borderColorHex = "#4C1D28",
            borderWidthDp = 1.0f,
            cornerRadiusDp = 10.0f,
            keyOpacity = 0.90f,
            shadowRadiusDp = 4.0f,
            enableGlow = true,
            glowColorHex = "#FF1744",
            glowRadiusDp = 6.0f
        ),
        categoryStyles = KeyboardTheme.defaultCategoryStyles().mapValues { (key, value) ->
            when (key) {
                KeyCategory.ENTER.name -> value.copy(
                    backgroundColorHex = "#D50000",
                    pressedBackgroundColorHex = "#FF5252",
                    labelColorHex = "#FFFFFF",
                    borderColorHex = "#FF1744",
                    glowColorHex = "#FF1744"
                )
                KeyCategory.SHIFT.name, KeyCategory.SPECIAL.name -> value.copy(
                    backgroundColorHex = "#241117",
                    borderColorHex = "#5E1F2E",
                    labelColorHex = "#FF8A80"
                )
                KeyCategory.SPACEBAR.name -> value.copy(
                    backgroundColorHex = "#211217",
                    borderColorHex = "#682233",
                    labelColorHex = "#FECDD3"
                )
                else -> value.copy(
                    backgroundColorHex = "#1A0F13",
                    borderColorHex = "#451A24",
                    labelColorHex = "#FFE4E6",
                    pressedBackgroundColorHex = "#FF1744"
                )
            }
        },
        textStyle = TextStyleConfig(
            fontFamilyId = BuiltInFontFamily.ORBITRON.id,
            fontWeight = 700,
            fontSizeSp = 17f,
            enableTextShadow = true
        )
    )

    val CYBER_MATRIX_MONO = KeyboardTheme(
        id = "builtin_cyber_matrix",
        name = "Cyber Matrix Terminal",
        isBuiltIn = true,
        isDefault = false,
        background = BackgroundConfig(
            sourceType = BackgroundSourceType.SOLID_OR_GRADIENT,
            baseColorHex = "#020604",
            secondaryGradientColorHex = "#07170F",
            useLinearGradient = true,
            dimmingOverlay = 0.15f
        ),
        defaultKeyStyle = KeyStyleConfig(
            backgroundColorHex = "#08140E",
            pressedBackgroundColorHex = "#00E676",
            enableGradient = false,
            labelColorHex = "#00E676",
            pressedLabelColorHex = "#020805",
            borderColorHex = "#00C853",
            borderWidthDp = 1.2f,
            cornerRadiusDp = 4.0f,
            keyOpacity = 0.96f,
            enableGlow = true,
            glowColorHex = "#00E676",
            glowRadiusDp = 8.0f
        ),
        categoryStyles = KeyboardTheme.defaultCategoryStyles().mapValues { (_, v) ->
            v.copy(
                backgroundColorHex = "#08140E",
                borderColorHex = "#00C853",
                labelColorHex = "#00E676",
                cornerRadiusDp = 4.0f,
                borderWidthDp = 1.2f
            )
        },
        textStyle = TextStyleConfig(
            fontFamilyId = BuiltInFontFamily.JETBRAINS_MONO.id,
            fontWeight = 700,
            fontSizeSp = 17f,
            enableLabelGlow = true,
            labelGlowColorHex = "#00E676"
        )
    )

    val STEALTH_GLASS_ROUNDED = KeyboardTheme(
        id = "builtin_stealth_glass",
        name = "Stealth Frosted Pill",
        isBuiltIn = true,
        isDefault = false,
        background = BackgroundConfig(
            sourceType = BackgroundSourceType.SOLID_OR_GRADIENT,
            baseColorHex = "#0A0E12",
            secondaryGradientColorHex = "#121A21",
            useLinearGradient = true,
            dimmingOverlay = 0.20f
        ),
        defaultKeyStyle = KeyStyleConfig(
            backgroundColorHex = "#1A242D",
            pressedBackgroundColorHex = "#1DE9B6",
            enableGradient = true,
            gradientEndColorHex = "#131B22",
            labelColorHex = "#E0F2F1",
            pressedLabelColorHex = "#041411",
            borderColorHex = "#2C3E4C",
            borderWidthDp = 0.8f,
            cornerRadiusDp = 16.0f,
            keyOpacity = 0.86f
        ),
        categoryStyles = KeyboardTheme.defaultCategoryStyles().mapValues { (_, v) ->
            v.copy(cornerRadiusDp = 16.0f, keyOpacity = 0.86f)
        },
        textStyle = TextStyleConfig(
            fontFamilyId = BuiltInFontFamily.COMFORTAA.id,
            fontWeight = 600,
            fontSizeSp = 17f
        )
    )

    val HIGH_CONTRAST_PRO = KeyboardTheme(
        id = "builtin_high_contrast",
        name = "High-Contrast Daylight/Night",
        isBuiltIn = true,
        isDefault = false,
        background = BackgroundConfig(
            sourceType = BackgroundSourceType.SOLID_OR_GRADIENT,
            baseColorHex = "#000000",
            secondaryGradientColorHex = "#000000",
            useLinearGradient = false,
            dimmingOverlay = 0.0f
        ),
        defaultKeyStyle = KeyStyleConfig(
            backgroundColorHex = "#121212",
            pressedBackgroundColorHex = "#00E676",
            enableGradient = false,
            labelColorHex = "#FFFFFF",
            pressedLabelColorHex = "#000000",
            borderColorHex = "#00E676",
            borderWidthDp = 2.0f,
            cornerRadiusDp = 6.0f,
            keyOpacity = 1.0f,
            shadowRadiusDp = 0f
        ),
        categoryStyles = KeyboardTheme.defaultCategoryStyles().mapValues { (k, v) ->
            when (k) {
                KeyCategory.ENTER.name -> v.copy(
                    backgroundColorHex = "#00E676",
                    labelColorHex = "#000000",
                    borderColorHex = "#FFFFFF",
                    borderWidthDp = 2.0f
                )
                KeyCategory.BACKSPACE.name -> v.copy(
                    backgroundColorHex = "#330009",
                    labelColorHex = "#FF8A80",
                    borderColorHex = "#FF1744",
                    borderWidthDp = 2.0f
                )
                else -> v.copy(
                    backgroundColorHex = "#121212",
                    labelColorHex = "#FFFFFF",
                    borderColorHex = "#00E676",
                    borderWidthDp = 2.0f,
                    keyOpacity = 1.0f
                )
            }
        },
        textStyle = TextStyleConfig(
            fontFamilyId = BuiltInFontFamily.SANS_SERIF.id,
            fontWeight = 800,
            fontSizeSp = 20f,
            enableTextShadow = false
        ),
        effects = EffectsConfig(
            reducedAnimationMode = true,
            enableKeyGlow = false,
            enableKeyShadows = false
        )
    )

    val ALL_PRESETS: List<KeyboardTheme> = listOf(
        TOXIC_EMERALD_CORE,
        CRIMSON_OBSIDIAN,
        CYBER_MATRIX_MONO,
        STEALTH_GLASS_ROUNDED,
        HIGH_CONTRAST_PRO
    )

    /**
     * Offline rule-based natural-language & preset theme synthesizer.
     * Translates user descriptions (e.g., "Create a black and emerald green cyberpunk keyboard
     * with bold white letters and rounded keys") into a validated KeyboardTheme without network calls.
     */
    fun synthesizeFromDescription(
        prompt: String,
        basePreset: KeyboardTheme = TOXIC_EMERALD_CORE
    ): KeyboardTheme {
        val lower = prompt.lowercase(Locale.US)

        val isCrimson = lower.contains("red") || lower.contains("crimson") || lower.contains("blood")
        val isMatrix = lower.contains("matrix") || lower.contains("terminal") || lower.contains("hacker") || lower.contains("code")
        val isGold = lower.contains("gold") || lower.contains("amber") || lower.contains("yellow")
        val isCyan = lower.contains("cyan") || lower.contains("blue") || lower.contains("ice")
        val isHighContrast = lower.contains("contrast") || lower.contains("accessible") || lower.contains("large")

        val accentHex = when {
            isCrimson -> "#FF1744"
            isGold -> "#FFD600"
            isCyan -> "#00E5FF"
            else -> "#00E676"
        }

        val keyBgHex = when {
            lower.contains("pitch black") || lower.contains("oled") -> "#060807"
            isCrimson -> "#1D0E13"
            isCyan -> "#0D1B22"
            isGold -> "#1C180D"
            else -> "#12221B"
        }

        val labelHex = when {
            lower.contains("white") -> "#FFFFFF"
            lower.contains("green letter") || isMatrix -> "#00E676"
            lower.contains("red letter") -> "#FF5252"
            lower.contains("gold letter") -> "#FFE57F"
            else -> "#F0FDF4"
        }

        val cornerRadius = when {
            lower.contains("pill") || lower.contains("round") || lower.contains("circle") -> 18f
            lower.contains("sharp") || lower.contains("square") || lower.contains("box") || isMatrix -> 3f
            else -> 9f
        }

        val fontFamily = when {
            lower.contains("mono") || isMatrix -> BuiltInFontFamily.JETBRAINS_MONO.id
            lower.contains("cyber") || lower.contains("futur") || lower.contains("sci-fi") -> BuiltInFontFamily.ORBITRON.id
            lower.contains("round") || lower.contains("soft") -> BuiltInFontFamily.COMFORTAA.id
            lower.contains("serif") -> BuiltInFontFamily.SERIF.id
            else -> BuiltInFontFamily.SPACE_GROTESK.id
        }

        val fontWeight = if (lower.contains("bold") || lower.contains("heavy") || isHighContrast) 700 else 600
        val fontSize = if (lower.contains("large") || lower.contains("big") || isHighContrast) 20f else 18f
        val enableGlow = lower.contains("glow") || lower.contains("neon") || lower.contains("cyber") || isMatrix

        val keyStyle = basePreset.defaultKeyStyle.copy(
            backgroundColorHex = keyBgHex,
            pressedBackgroundColorHex = accentHex,
            labelColorHex = labelHex,
            borderColorHex = accentHex,
            borderWidthDp = if (isHighContrast || isMatrix) 1.6f else 1.0f,
            cornerRadiusDp = cornerRadius,
            enableGlow = enableGlow,
            glowColorHex = accentHex
        )

        val updatedCategories = basePreset.categoryStyles.mapValues { (cat, existing) ->
            when (cat) {
                KeyCategory.ENTER.name -> existing.copy(
                    backgroundColorHex = accentHex,
                    labelColorHex = "#050A08",
                    borderColorHex = labelHex,
                    cornerRadiusDp = cornerRadius,
                    enableGlow = enableGlow,
                    glowColorHex = accentHex
                )
                else -> keyStyle
            }
        }

        val generatedName = prompt.trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            .take(28)
            .ifEmpty { "Synthesized Cyber Theme" }

        return basePreset.copy(
            id = "theme_${UUID.randomUUID()}",
            name = generatedName,
            isBuiltIn = false,
            isDefault = false,
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis(),
            background = basePreset.background.copy(
                sourceType = if (isCrimson) BackgroundSourceType.PRESET_DRAWABLE else basePreset.background.sourceType,
                presetDrawableName = if (isCrimson) "img_bg_crimson_glass" else "img_bg_toxic_cyber",
                baseColorHex = "#040706",
                secondaryGradientColorHex = keyBgHex
            ),
            defaultKeyStyle = keyStyle,
            categoryStyles = updatedCategories,
            textStyle = basePreset.textStyle.copy(
                fontFamilyId = fontFamily,
                fontWeight = fontWeight,
                fontSizeSp = fontSize,
                enableLabelGlow = enableGlow,
                labelGlowColorHex = accentHex
            ),
            effects = basePreset.effects.copy(
                enableKeyGlow = enableGlow
            )
        ).validated()
    }
}
