package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.keyboard.layout.KeyActionType
import com.example.keyboard.layout.KeySpec
import com.example.keyboard.state.KeyboardEngineState
import com.example.keyboard.state.KeyboardLanguage
import com.example.keyboard.state.KeyboardMode
import com.example.keyboard.ui.NativeKeyboardRendererView
import com.example.theme.model.KeyboardTheme
import com.example.theme.model.sanitizeHexColor
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ToxicAmberWarning
import com.example.ui.theme.ToxicCrimsonAccent
import com.example.ui.theme.ToxicEmeraldPrimary
import com.example.util.ColorAndImageUtils
import java.util.Locale

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!badgeText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LabeledSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    onReset: (() -> Unit)? = null,
    testTagName: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onReset != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset $label",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = if (testTagName != null) Modifier.testTag(testTagName) else Modifier,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private val PRESET_COLOR_SWATCHES = listOf(
    "#00E676", "#00C853", "#1DE9B6", "#00E5FF",
    "#FF1744", "#D50000", "#FF9100", "#FFD600",
    "#050807", "#0D1913", "#14221B", "#1F4231",
    "#1C1014", "#263238", "#ECFDF5", "#FFFFFF"
)

@Composable
fun InteractiveColorPickerSection(
    title: String,
    currentHex: String,
    contrastAgainstHex: String? = null,
    onColorSelected: (String) -> Unit
) {
    var hexInput by remember(currentHex) { mutableStateOf(currentHex.removePrefix("#")) }
    val parsedColorInt = remember(currentHex) {
        ColorAndImageUtils.parseColorSafe(currentHex, AndroidColor.parseColor("#00E676"))
    }
    val red = AndroidColor.red(parsedColorInt)
    val green = AndroidColor.green(parsedColorInt)
    val blue = AndroidColor.blue(parsedColorInt)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(parsedColorInt))
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentHex.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (contrastAgainstHex != null) {
                val ratio = remember(currentHex, contrastAgainstHex) {
                    ColorAndImageUtils.calculateContrastRatio(currentHex, contrastAgainstHex)
                }
                val isReadable = ratio >= 3.0
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isReadable) ToxicEmeraldPrimary.copy(alpha = 0.15f) else ToxicAmberWarning.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isReadable) ToxicEmeraldPrimary else ToxicAmberWarning)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isReadable) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low contrast warning",
                                tint = ToxicAmberWarning,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = String.format(Locale.US, "Contrast %.1f:1", ratio),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isReadable) ToxicEmeraldPrimary else ToxicAmberWarning
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PRESET_COLOR_SWATCHES.forEach { swatchHex ->
                val isSelected = swatchHex.equals(currentHex, ignoreCase = true)
                val cInt = ColorAndImageUtils.parseColorSafe(swatchHex)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(cInt))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) ToxicEmeraldPrimary else Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable {
                            hexInput = swatchHex.removePrefix("#")
                            onColorSelected(swatchHex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected color $swatchHex",
                            tint = if (ColorAndImageUtils.calculateContrastRatio("#FFFFFF", swatchHex) > 2.5) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // RGB Sliders for continuous custom color mixing
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF5252), modifier = Modifier.width(18.dp))
            Slider(
                value = red.toFloat(),
                onValueChange = { r ->
                    val newHex = String.format("#%02X%02X%02X", r.toInt(), green, blue)
                    hexInput = newHex.removePrefix("#")
                    onColorSelected(newHex)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
            Text("$red", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(28.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", style = MaterialTheme.typography.labelSmall, color = Color(0xFF69F0AE), modifier = Modifier.width(18.dp))
            Slider(
                value = green.toFloat(),
                onValueChange = { g ->
                    val newHex = String.format("#%02X%02X%02X", red, g.toInt(), blue)
                    hexInput = newHex.removePrefix("#")
                    onColorSelected(newHex)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
            Text("$green", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(28.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B", style = MaterialTheme.typography.labelSmall, color = Color(0xFF40C4FF), modifier = Modifier.width(18.dp))
            Slider(
                value = blue.toFloat(),
                onValueChange = { b ->
                    val newHex = String.format("#%02X%02X%02X", red, green, b.toInt())
                    hexInput = newHex.removePrefix("#")
                    onColorSelected(newHex)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
            Text("$blue", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(28.dp))
        }

        // Direct Hex Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = { raw ->
                    val filtered = raw.uppercase(Locale.US).filter { it in '0'..'9' || it in 'A'..'F' }.take(8)
                    hexInput = filtered
                    if (filtered.length == 6 || filtered.length == 8) {
                        onColorSelected(sanitizeHexColor("#$filtered", currentHex))
                    }
                },
                prefix = { Text("#", fontFamily = JetBrainsMonoFamily) },
                label = { Text("Hex Color (RRGGBB)") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("hex_color_input_$title")
            )
        }
    }
}

@Composable
fun LiveKeyboardPreviewCard(
    theme: KeyboardTheme,
    globalSettings: GlobalKeyboardSettings,
    allowKeyInspector: Boolean = false,
    inspectedKeyId: String? = null,
    onKeyInspected: ((KeySpec) -> Unit)? = null,
    sandboxText: String = "",
    onSandboxTextChange: ((String) -> Unit)? = null
) {
    var previewState by remember { mutableStateOf(KeyboardEngineState()) }
    var isLandscapePreview by remember { mutableStateOf(false) }
    var isInspectMode by remember { mutableStateOf(false) }

    GlassCard(
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Live Keyboard Preview",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE ENGINE PREVIEW",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (isInspectMode) "Tap any key to style" else "Interactive Test Mode",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preview State Controls: EN / বাংলা / ?123 / =\< / Portrait vs Landscape / Inspect Key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = previewState.mode == KeyboardMode.ALPHABET && previewState.language == KeyboardLanguage.ENGLISH,
                onClick = {
                    previewState = previewState.copy(
                        language = KeyboardLanguage.ENGLISH,
                        mode = KeyboardMode.ALPHABET
                    )
                },
                label = { Text("EN Letters") }
            )
            FilterChip(
                selected = previewState.mode == KeyboardMode.ALPHABET && previewState.language == KeyboardLanguage.BENGALI,
                onClick = {
                    previewState = previewState.copy(
                        language = KeyboardLanguage.BENGALI,
                        mode = KeyboardMode.ALPHABET
                    )
                },
                label = { Text("বাংলা (BN)") }
            )
            FilterChip(
                selected = previewState.mode == KeyboardMode.SYMBOLS_primary,
                onClick = {
                    previewState = previewState.copy(mode = KeyboardMode.SYMBOLS_primary)
                },
                label = { Text("123 & Symbols") }
            )
            FilterChip(
                selected = previewState.mode == KeyboardMode.SYMBOLS_secondary,
                onClick = {
                    previewState = previewState.copy(mode = KeyboardMode.SYMBOLS_secondary)
                },
                label = { Text("More Symbols") }
            )
            FilterChip(
                selected = previewState.mode == KeyboardMode.EMOJI_PANEL,
                onClick = {
                    val next = if (previewState.mode == KeyboardMode.EMOJI_PANEL) KeyboardMode.ALPHABET else KeyboardMode.EMOJI_PANEL
                    previewState = previewState.copy(mode = next)
                },
                label = { Text("Emoji Panel") }
            )
            FilterChip(
                selected = isLandscapePreview,
                onClick = { isLandscapePreview = !isLandscapePreview },
                label = { Text(if (isLandscapePreview) "Landscape" else "Portrait") }
            )
            if (allowKeyInspector) {
                FilterChip(
                    selected = isInspectMode,
                    onClick = { isInspectMode = !isInspectMode },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Inspect individual key",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Pick Key") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
        }

        if (onSandboxTextChange != null && !isInspectMode) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = sandboxText.ifEmpty { "Tap keys below to test typing..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sandboxText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (sandboxText.isNotEmpty()) {
                        TextButton(
                            onClick = { onSandboxTextChange("") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Embedded NativeKeyboardRendererView — uses the exact same renderer as ToxicInputMethodService
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    NativeKeyboardRendererView(ctx)
                },
                update = { view ->
                    view.isEditorInspectMode = isInspectMode
                    view.inspectedKeyId = inspectedKeyId
                    view.previewLandscapeOverride = isLandscapePreview
                    view.updateConfiguration(
                        newTheme = theme,
                        newSettings = globalSettings,
                        newState = previewState
                    )
                    view.listener = object : NativeKeyboardRendererView.Listener {
                        override fun onKeyTapped(key: KeySpec, isLongPress: Boolean) {
                            if (isLongPress && !key.secondaryOutputText.isNullOrEmpty()) {
                                onSandboxTextChange?.invoke(sandboxText + key.secondaryOutputText)
                                previewState = previewState.afterCharacterCommitted()
                                return
                            }
                            when (key.actionType) {
                                KeyActionType.CHARACTER -> {
                                    onSandboxTextChange?.invoke(sandboxText + key.outputText)
                                    previewState = previewState.afterCharacterCommitted()
                                }
                                KeyActionType.SPACE -> onSandboxTextChange?.invoke("$sandboxText ")
                                KeyActionType.BACKSPACE -> {
                                    if (sandboxText.isNotEmpty()) {
                                        val dropCount = if (Character.isLowSurrogate(sandboxText.last()) && sandboxText.length >= 2) 2 else 1
                                        onSandboxTextChange?.invoke(sandboxText.dropLast(dropCount))
                                    }
                                }
                                KeyActionType.SHIFT -> previewState = previewState.onShiftClicked(isDoubleTap = isLongPress)
                                KeyActionType.ENTER -> onSandboxTextChange?.invoke("$sandboxText\n")
                                KeyActionType.MODE_SYMBOLS_1 -> previewState = previewState.onModeChanged(KeyboardMode.SYMBOLS_primary)
                                KeyActionType.MODE_SYMBOLS_2 -> previewState = previewState.onModeChanged(KeyboardMode.SYMBOLS_secondary)
                                KeyActionType.MODE_ALPHABET -> previewState = previewState.onModeChanged(KeyboardMode.ALPHABET)
                                KeyActionType.LANGUAGE_SWITCH -> previewState = previewState.onLanguageToggled()
                                KeyActionType.EMOJI_PANEL_TOGGLE -> {
                                    val next = if (previewState.mode == KeyboardMode.EMOJI_PANEL) KeyboardMode.ALPHABET else KeyboardMode.EMOJI_PANEL
                                    previewState = previewState.onModeChanged(next)
                                }
                                KeyActionType.VOICE_TYPING -> {
                                    onSandboxTextChange?.invoke("$sandboxText🎙️ ")
                                }
                            }
                        }

                        override fun onSuggestionClicked(word: String) {
                            onSandboxTextChange?.invoke("$sandboxText$word ")
                        }

                        override fun onEmojiSelected(emoji: String) {
                            onSandboxTextChange?.invoke(sandboxText + emoji)
                        }

                        override fun onClipboardItemSelected(text: String) {
                            onSandboxTextChange?.invoke(sandboxText + text)
                        }

                        override fun onToolbarModeToggle(mode: KeyboardMode) {
                            previewState = previewState.onModeChanged(mode)
                        }

                        override fun onToolbarLanguageToggle() {
                            previewState = previewState.onLanguageToggled()
                        }

                        override fun onOpenStudioAppRequested() {}

                        override fun onKeySelectedInEditor(key: KeySpec) {
                            onKeyInspected?.invoke(key)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_keyboard_preview_view")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Preview uses the identical rendering engine as the system keyboard. Tap 'Save & Apply' to push unsaved draft edits to other apps.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) ToxicCrimsonAccent else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
