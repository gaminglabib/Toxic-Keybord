package com.example.theme.model

import com.squareup.moshi.JsonClass
import kotlin.math.max
import kotlin.math.min

const val CURRENT_THEME_SCHEMA_VERSION = 1

enum class KeyCategory {
    ALPHABETIC,
    NUMERIC,
    SYMBOL,
    SPECIAL,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACEBAR,
    LANGUAGE_SWITCH,
    EMOJI_KEY,
    TOOLBAR_BUTTON
}

enum class BackgroundSourceType {
    SOLID_OR_GRADIENT,
    PRESET_DRAWABLE,
    CUSTOM_IMAGE
}

enum class KeyLabelAlignment {
    CENTER,
    TOP_LEFT,
    BOTTOM_RIGHT
}

enum class BuiltInFontFamily(val id: String, val displayName: String) {
    SANS_SERIF("sans_serif", "System Sans-Serif"),
    SERIF("serif", "System Serif"),
    MONOSPACE("monospace", "System Monospace"),
    SPACE_GROTESK("space_grotesk", "Space Grotesk (Tech Sans)"),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono (Code)"),
    ORBITRON("orbitron", "Orbitron (Cyber Display)"),
    COMFORTAA("comfortaa", "Comfortaa (Rounded)")
}

@JsonClass(generateAdapter = true)
data class KeyStyleConfig(
    val backgroundColorHex: String = "#14221B",
    val pressedBackgroundColorHex: String = "#00E676",
    val gradientEndColorHex: String? = null,
    val enableGradient: Boolean = false,
    val labelColorHex: String = "#E8F5E9",
    val pressedLabelColorHex: String = "#04100A",
    val borderColorHex: String = "#1F4231",
    val borderWidthDp: Float = 1.0f,
    val cornerRadiusDp: Float = 8.0f,
    val keyOpacity: Float = 0.94f,
    val shadowRadiusDp: Float = 3.0f,
    val shadowColorHex: String = "#88000000",
    val enableGlow: Boolean = false,
    val glowColorHex: String = "#00E676",
    val glowRadiusDp: Float = 6.0f
) {
    fun validated(): KeyStyleConfig = copy(
        backgroundColorHex = sanitizeHexColor(backgroundColorHex, "#14221B"),
        pressedBackgroundColorHex = sanitizeHexColor(pressedBackgroundColorHex, "#00E676"),
        gradientEndColorHex = gradientEndColorHex?.let { sanitizeHexColor(it, "#0D1913") },
        labelColorHex = sanitizeHexColor(labelColorHex, "#E8F5E9"),
        pressedLabelColorHex = sanitizeHexColor(pressedLabelColorHex, "#04100A"),
        borderColorHex = sanitizeHexColor(borderColorHex, "#1F4231"),
        borderWidthDp = borderWidthDp.coerceIn(0f, 4f),
        cornerRadiusDp = cornerRadiusDp.coerceIn(0f, 24f),
        keyOpacity = keyOpacity.coerceIn(0.20f, 1.0f),
        shadowRadiusDp = shadowRadiusDp.coerceIn(0f, 12f),
        shadowColorHex = sanitizeHexColor(shadowColorHex, "#88000000"),
        glowColorHex = sanitizeHexColor(glowColorHex, "#00E676"),
        glowRadiusDp = glowRadiusDp.coerceIn(0f, 16f)
    )
}

@JsonClass(generateAdapter = true)
data class BackgroundConfig(
    val sourceType: BackgroundSourceType = BackgroundSourceType.SOLID_OR_GRADIENT,
    val baseColorHex: String = "#060B09",
    val secondaryGradientColorHex: String = "#0B1913",
    val useLinearGradient: Boolean = true,
    val presetDrawableName: String? = null,
    val customImagePath: String? = null,
    val brightness: Float = 0.0f,         // -0.5f to +0.5f
    val dimmingOverlay: Float = 0.35f,    // 0.0f to 0.90f
    val imageOpacity: Float = 1.0f,       // 0.1f to 1.0f
    val blurRadius: Int = 0,              // 0 to 25
    val scaleZoom: Float = 1.0f,          // 1.0f to 3.0f
    val rotationDegrees: Float = 0f,      // 0, 90, 180, 270 or free
    val offsetXPercent: Float = 0f,       // -0.5f to 0.5f
    val offsetYPercent: Float = 0f        // -0.5f to 0.5f
) {
    fun validated(): BackgroundConfig = copy(
        baseColorHex = sanitizeHexColor(baseColorHex, "#060B09"),
        secondaryGradientColorHex = sanitizeHexColor(secondaryGradientColorHex, "#0B1913"),
        brightness = brightness.coerceIn(-0.5f, 0.5f),
        dimmingOverlay = dimmingOverlay.coerceIn(0.0f, 0.90f),
        imageOpacity = imageOpacity.coerceIn(0.1f, 1.0f),
        blurRadius = blurRadius.coerceIn(0, 25),
        scaleZoom = scaleZoom.coerceIn(1.0f, 3.0f),
        rotationDegrees = ((rotationDegrees % 360f) + 360f) % 360f,
        offsetXPercent = offsetXPercent.coerceIn(-0.5f, 0.5f),
        offsetYPercent = offsetYPercent.coerceIn(-0.5f, 0.5f)
    )
}

@JsonClass(generateAdapter = true)
data class TextStyleConfig(
    val fontFamilyId: String = BuiltInFontFamily.SPACE_GROTESK.id,
    val customFontPath: String? = null,
    val fontWeight: Int = 600,            // 400 (Regular), 500 (Medium), 700 (Bold), 800 (Heavy)
    val isItalic: Boolean = false,
    val fontSizeSp: Float = 18f,          // 12f to 26f
    val secondaryLabelSizeSp: Float = 10f,// 8f to 15f
    val toolbarLabelSizeSp: Float = 13f,
    val letterSpacingEm: Float = 0.02f,   // -0.05f to 0.25f
    val enableGradientText: Boolean = false,
    val gradientTextEndColorHex: String = "#00E676",
    val enableTextShadow: Boolean = true,
    val textShadowColorHex: String = "#AA000000",
    val textShadowRadius: Float = 2.5f,
    val enableTextOutline: Boolean = false,
    val textOutlineColorHex: String = "#000000",
    val textOutlineWidthPx: Float = 1.5f,
    val enableLabelGlow: Boolean = false,
    val labelGlowColorHex: String = "#00E676",
    val labelAlignment: KeyLabelAlignment = KeyLabelAlignment.CENTER,
    val showSecondaryHintSymbols: Boolean = true
) {
    fun validated(): TextStyleConfig = copy(
        fontWeight = fontWeight.coerceIn(300, 900),
        fontSizeSp = fontSizeSp.coerceIn(12f, 26f),
        secondaryLabelSizeSp = secondaryLabelSizeSp.coerceIn(8f, 15f),
        toolbarLabelSizeSp = toolbarLabelSizeSp.coerceIn(10f, 18f),
        letterSpacingEm = letterSpacingEm.coerceIn(-0.05f, 0.25f),
        gradientTextEndColorHex = sanitizeHexColor(gradientTextEndColorHex, "#00E676"),
        textShadowColorHex = sanitizeHexColor(textShadowColorHex, "#AA000000"),
        textShadowRadius = textShadowRadius.coerceIn(0f, 8f),
        textOutlineColorHex = sanitizeHexColor(textOutlineColorHex, "#000000"),
        textOutlineWidthPx = textOutlineWidthPx.coerceIn(0f, 4f),
        labelGlowColorHex = sanitizeHexColor(labelGlowColorHex, "#00E676")
    )
}

@JsonClass(generateAdapter = true)
data class LayoutConfig(
    val showNumberRow: Boolean = true,
    val showTopToolbar: Boolean = true,
    val showLanguageKey: Boolean = true,
    val showEmojiKey: Boolean = true,
    val keyHeightDp: Float = 48f,           // 38f to 64f
    val landscapeKeyHeightDp: Float = 40f,  // 32f to 52f
    val horizontalSpacingDp: Float = 4.0f,  // 2f to 10f
    val verticalSpacingDp: Float = 5.5f,    // 2f to 12f
    val keyboardBottomPaddingDp: Float = 8f,// 0f to 32f
    val specialKeyWidthMultiplier: Float = 1.35f, // 1.1f to 1.7f
    val spacebarWidthWeight: Float = 3.2f   // 2.4f to 5.5f
) {
    fun validated(): LayoutConfig = copy(
        keyHeightDp = keyHeightDp.coerceIn(38f, 64f),
        landscapeKeyHeightDp = landscapeKeyHeightDp.coerceIn(32f, 52f),
        horizontalSpacingDp = horizontalSpacingDp.coerceIn(2f, 10f),
        verticalSpacingDp = verticalSpacingDp.coerceIn(2f, 12f),
        keyboardBottomPaddingDp = keyboardBottomPaddingDp.coerceIn(0f, 32f),
        specialKeyWidthMultiplier = specialKeyWidthMultiplier.coerceIn(1.1f, 1.7f),
        spacebarWidthWeight = spacebarWidthWeight.coerceIn(2.4f, 5.5f)
    )
}

@JsonClass(generateAdapter = true)
data class EffectsConfig(
    val enableKeyPressHighlight: Boolean = true,
    val enableKeyPressScalePop: Boolean = true,
    val enableRippleEffect: Boolean = true,
    val enableKeyShadows: Boolean = true,
    val enableKeyGlow: Boolean = false,
    val enableSoundFeedback: Boolean = false,
    val soundVolume: Float = 0.5f,
    val enableVibrationFeedback: Boolean = true,
    val vibrationDurationMs: Int = 18,
    val reducedAnimationMode: Boolean = false
) {
    fun validated(): EffectsConfig = copy(
        soundVolume = soundVolume.coerceIn(0.0f, 1.0f),
        vibrationDurationMs = vibrationDurationMs.coerceIn(5, 80)
    )
}

@JsonClass(generateAdapter = true)
data class KeyboardTheme(
    val id: String,
    val name: String,
    val schemaVersion: Int = CURRENT_THEME_SCHEMA_VERSION,
    val isBuiltIn: Boolean = false,
    val isDefault: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val lastUsedAtEpochMs: Long = System.currentTimeMillis(),
    val background: BackgroundConfig = BackgroundConfig(),
    val defaultKeyStyle: KeyStyleConfig = KeyStyleConfig(),
    val categoryStyles: Map<String, KeyStyleConfig> = defaultCategoryStyles(),
    val individualKeyOverrides: Map<String, KeyStyleConfig> = emptyMap(),
    val textStyle: TextStyleConfig = TextStyleConfig(),
    val layout: LayoutConfig = LayoutConfig(),
    val effects: EffectsConfig = EffectsConfig()
) {
    fun styleForKey(keyCodeOrLabel: String, category: KeyCategory): KeyStyleConfig {
        val normalizedKey = keyCodeOrLabel.uppercase()
        individualKeyOverrides[normalizedKey]?.let { return it.validated() }
        categoryStyles[category.name]?.let { return it.validated() }
        return defaultKeyStyle.validated()
    }

    fun validated(): KeyboardTheme {
        val safeName = name.trim().take(48).ifEmpty { "Untitled Theme" }
        val validatedCategoryMap = categoryStyles.mapValues { (_, v) -> v.validated() }
        val validatedOverrides = individualKeyOverrides.mapValues { (_, v) -> v.validated() }
        return copy(
            name = safeName,
            schemaVersion = max(1, min(schemaVersion, CURRENT_THEME_SCHEMA_VERSION)),
            background = background.validated(),
            defaultKeyStyle = defaultKeyStyle.validated(),
            categoryStyles = validatedCategoryMap,
            individualKeyOverrides = validatedOverrides,
            textStyle = textStyle.validated(),
            layout = layout.validated(),
            effects = effects.validated()
        )
    }

    companion object {
        fun defaultCategoryStyles(): Map<String, KeyStyleConfig> {
            val alpha = KeyStyleConfig(
                backgroundColorHex = "#13221B",
                pressedBackgroundColorHex = "#00E676",
                labelColorHex = "#ECFDF5",
                pressedLabelColorHex = "#04120B",
                borderColorHex = "#1E3D2F",
                borderWidthDp = 1.0f,
                cornerRadiusDp = 8f
            )
            val numeric = alpha.copy(
                backgroundColorHex = "#0F1C16",
                borderColorHex = "#1B382B",
                labelColorHex = "#A7F3D0"
            )
            val symbol = alpha.copy(
                backgroundColorHex = "#101E18",
                labelColorHex = "#6EE7B7"
            )
            val special = alpha.copy(
                backgroundColorHex = "#0D1713",
                borderColorHex = "#244737",
                labelColorHex = "#34D399"
            )
            val enter = alpha.copy(
                backgroundColorHex = "#00C853",
                pressedBackgroundColorHex = "#69F0AE",
                labelColorHex = "#03120A",
                pressedLabelColorHex = "#03120A",
                borderColorHex = "#00E676",
                enableGlow = true,
                glowColorHex = "#00E676"
            )
            val backspace = special.copy(
                backgroundColorHex = "#261015",
                pressedBackgroundColorHex = "#FF1744",
                borderColorHex = "#5C1926",
                labelColorHex = "#FF8A80",
                pressedLabelColorHex = "#FFFFFF"
            )
            val shift = special.copy(
                backgroundColorHex = "#12261E",
                pressedBackgroundColorHex = "#00E676",
                labelColorHex = "#00E676"
            )
            val space = alpha.copy(
                backgroundColorHex = "#162920",
                borderColorHex = "#28543F",
                labelColorHex = "#A7F3D0"
            )
            return mapOf(
                KeyCategory.ALPHABETIC.name to alpha,
                KeyCategory.NUMERIC.name to numeric,
                KeyCategory.SYMBOL.name to symbol,
                KeyCategory.SPECIAL.name to special,
                KeyCategory.SHIFT.name to shift,
                KeyCategory.BACKSPACE.name to backspace,
                KeyCategory.ENTER.name to enter,
                KeyCategory.SPACEBAR.name to space,
                KeyCategory.LANGUAGE_SWITCH.name to special,
                KeyCategory.EMOJI_KEY.name to special,
                KeyCategory.TOOLBAR_BUTTON.name to special.copy(cornerRadiusDp = 12f)
            )
        }
    }
}

fun sanitizeHexColor(input: String?, fallback: String): String {
    if (input.isNullOrBlank()) return fallback
    val cleaned = input.trim().removePrefix("#").uppercase()
    val validChars = cleaned.all { it in '0'..'9' || it in 'A'..'F' }
    if (!validChars) return fallback
    return when (cleaned.length) {
        6 -> "#$cleaned"
        8 -> "#$cleaned"
        else -> fallback
    }
}
