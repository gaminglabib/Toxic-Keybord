package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ImportedFontEntity
import com.example.fonts.FontManager
import com.example.theme.model.BuiltInFontFamily
import com.example.theme.model.KeyCategory
import com.example.theme.model.KeyLabelAlignment
import com.example.theme.model.KeyStyleConfig
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveColorPickerSection
import com.example.ui.components.LabeledSliderRow
import com.example.ui.components.LiveKeyboardPreviewCard
import com.example.ui.components.SectionHeader
import java.util.Locale

@Composable
fun TextStyleStudioScreen(
    draftTheme: KeyboardTheme,
    globalSettings: GlobalKeyboardSettings,
    importedFonts: List<ImportedFontEntity>,
    sandboxText: String,
    onSandboxTextChange: (String) -> Unit,
    onUpdateDraft: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    onUpdateKeyStyle: ((KeyStyleConfig) -> KeyStyleConfig) -> Unit,
    onImportCustomFont: (Uri, String) -> Unit,
    onRemoveCustomFont: (ImportedFontEntity) -> Unit,
    onSaveAndApply: () -> Unit
) {
    val context = LocalContext.current
    val textCfg = draftTheme.textStyle
    val alphaKeyStyle = draftTheme.styleForKey("A", KeyCategory.ALPHABETIC)

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName = "custom_font.ttf"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx) ?: displayName
                        }
                    }
                }
            } catch (_: Exception) {
            }
            onImportCustomFont(uri, displayName)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("text_style_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Save Button
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Text Style Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Typography, custom .TTF/.OTF fonts, shadows, glow & বাংলা fallback",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onSaveAndApply,
                        modifier = Modifier.testTag("text_studio_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply")
                    }
                }
            }
        }

        // 2. Live Keyboard Preview
        item {
            LiveKeyboardPreviewCard(
                theme = draftTheme,
                globalSettings = globalSettings,
                sandboxText = sandboxText,
                onSandboxTextChange = onSandboxTextChange
            )
        }

        // 3. Mixed English & Bengali Unicode Specimen Card
        item {
            GlassCard {
                SectionHeader(
                    title = "Bilingual Unicode & Conjunct Preview",
                    subtitle = "Verifies Latin custom font + Bengali Unicode conjunct rendering"
                )
                val composeFont = FontManager.resolveComposeFontFamily(textCfg)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "QWERTY • 0123456789 • !@#$%^&*()",
                            fontFamily = composeFont,
                            fontWeight = if (textCfg.fontWeight >= 700) FontWeight.Bold else FontWeight.Medium,
                            fontStyle = if (textCfg.isItalic) FontStyle.Italic else FontStyle.Normal,
                            fontSize = textCfg.fontSizeSp.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "বাংলা স্বরবর্ণ ও কার: অ আ ই ঈ উ ঊ ঋ এ ঐ ও ঔ | া ি ী ু ূ ৃ ে ৈ ো ৌ",
                            fontSize = (textCfg.fontSizeSp * 0.9f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "যুক্তাক্ষর ও মিশ্র টেক্সট: ক্ষ জ্ঞ প্র শ্র ত্র স্ত ন্দ — TOXIC বাংলা Keyboard ২০২৬",
                            fontSize = (textCfg.fontSizeSp * 0.9f).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Technical note",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Technical Note: Customizing keyboard fonts styles keycaps, popups, and the suggestion bar inside TOXIC Keyboard. Other apps (messages, browsers, notes) render committed Unicode characters using their own app/system fonts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Built-in & System Font Family Choices + Custom .TTF/.OTF Import
        item {
            GlassCard {
                SectionHeader(
                    title = "Font Family & Custom .TTF / .OTF Import",
                    subtitle = "Choose a bundled typeface or import your own font file safely"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BuiltInFontFamily.entries.forEach { fontItem ->
                        val isSelected = textCfg.customFontPath == null && textCfg.fontFamilyId == fontItem.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fontItem.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onUpdateDraft {
                                            it.copy(
                                                textStyle = it.textStyle.copy(
                                                    fontFamilyId = fontItem.id,
                                                    customFontPath = null
                                                )
                                            )
                                        }
                                    },
                                    label = { Text(if (isSelected) "Active" else "Use") }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        fontPickerLauncher.launch(
                            arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream", "*/*")
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_custom_font_button")
                ) {
                    Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Custom .TTF or .OTF Font File")
                }

                if (importedFonts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Imported Custom Fonts:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    importedFonts.forEach { fontEntity ->
                        val isUsed = textCfg.customFontPath == fontEntity.filePath
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fontEntity.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${fontEntity.fileSizeBytes / 1024} KB • Validated TrueType/OpenType",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(
                                        selected = isUsed,
                                        onClick = {
                                            onUpdateDraft {
                                                it.copy(
                                                    textStyle = it.textStyle.copy(customFontPath = fontEntity.filePath)
                                                )
                                            }
                                        },
                                        label = { Text(if (isUsed) "Active" else "Apply") }
                                    )
                                    IconButton(onClick = { onRemoveCustomFont(fontEntity) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove imported font",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Weight, Italic, Size, Spacing, Alignment
        item {
            GlassCard {
                SectionHeader(
                    title = "Size, Weight, Alignment & Effects",
                    subtitle = "Fine-tune label dimensions and visual treatments"
                )

                Text("Font Weight:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        400 to "Regular (400)",
                        500 to "Medium (500)",
                        700 to "Bold (700)",
                        800 to "Heavy (800)"
                    ).forEach { (weightVal, label) ->
                        FilterChip(
                            selected = textCfg.fontWeight == weightVal,
                            onClick = {
                                onUpdateDraft { it.copy(textStyle = it.textStyle.copy(fontWeight = weightVal)) }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Italic Key Labels", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = textCfg.isItalic,
                        onCheckedChange = { c ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(isItalic = c)) }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Secondary Long-Press Hint Symbols", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = textCfg.showSecondaryHintSymbols,
                        onCheckedChange = { c ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(showSecondaryHintSymbols = c)) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Key Label Alignment:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyLabelAlignment.entries.forEach { align ->
                        FilterChip(
                            selected = textCfg.labelAlignment == align,
                            onClick = {
                                onUpdateDraft { it.copy(textStyle = it.textStyle.copy(labelAlignment = align)) }
                            },
                            label = { Text(align.name.replace("_", " ")) }
                        )
                    }
                }

                LabeledSliderRow(
                    label = "Primary Key Font Size",
                    value = textCfg.fontSizeSp,
                    valueRange = 12f..26f,
                    valueLabel = String.format(Locale.US, "%.0f sp", textCfg.fontSizeSp),
                    onValueChange = { v -> onUpdateDraft { it.copy(textStyle = it.textStyle.copy(fontSizeSp = v)) } },
                    onReset = { onUpdateDraft { it.copy(textStyle = it.textStyle.copy(fontSizeSp = 18f)) } }
                )

                LabeledSliderRow(
                    label = "Secondary Hint Symbol Size",
                    value = textCfg.secondaryLabelSizeSp,
                    valueRange = 8f..15f,
                    valueLabel = String.format(Locale.US, "%.0f sp", textCfg.secondaryLabelSizeSp),
                    onValueChange = { v -> onUpdateDraft { it.copy(textStyle = it.textStyle.copy(secondaryLabelSizeSp = v)) } },
                    onReset = { onUpdateDraft { it.copy(textStyle = it.textStyle.copy(secondaryLabelSizeSp = 10f)) } }
                )

                LabeledSliderRow(
                    label = "Toolbar & Suggestion Font Size",
                    value = textCfg.toolbarLabelSizeSp,
                    valueRange = 10f..18f,
                    valueLabel = String.format(Locale.US, "%.0f sp", textCfg.toolbarLabelSizeSp),
                    onValueChange = { v -> onUpdateDraft { it.copy(textStyle = it.textStyle.copy(toolbarLabelSizeSp = v)) } },
                    onReset = { onUpdateDraft { it.copy(textStyle = it.textStyle.copy(toolbarLabelSizeSp = 13f)) } }
                )

                LabeledSliderRow(
                    label = "Latin Letter Spacing",
                    value = textCfg.letterSpacingEm,
                    valueRange = -0.05f..0.25f,
                    valueLabel = String.format(Locale.US, "%.2f em", textCfg.letterSpacingEm),
                    onValueChange = { v -> onUpdateDraft { it.copy(textStyle = it.textStyle.copy(letterSpacingEm = v)) } },
                    onReset = { onUpdateDraft { it.copy(textStyle = it.textStyle.copy(letterSpacingEm = 0.02f)) } }
                )

                Spacer(modifier = Modifier.height(8.dp))

                InteractiveColorPickerSection(
                    title = "Primary Text Color",
                    currentHex = alphaKeyStyle.labelColorHex,
                    contrastAgainstHex = alphaKeyStyle.backgroundColorHex,
                    onColorSelected = { hex -> onUpdateKeyStyle { it.copy(labelColorHex = hex) } }
                )

                // Gradient Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Gradient Key Labels", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = textCfg.enableGradientText,
                        onCheckedChange = { c ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(enableGradientText = c)) }
                        }
                    )
                }
                if (textCfg.enableGradientText) {
                    InteractiveColorPickerSection(
                        title = "Gradient Text Secondary Color",
                        currentHex = textCfg.gradientTextEndColorHex,
                        onColorSelected = { hex ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(gradientTextEndColorHex = hex)) }
                        }
                    )
                }

                // Text Shadow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Text Drop Shadow", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = textCfg.enableTextShadow,
                        onCheckedChange = { c ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(enableTextShadow = c)) }
                        }
                    )
                }
                if (textCfg.enableTextShadow) {
                    LabeledSliderRow(
                        label = "Text Shadow Radius",
                        value = textCfg.textShadowRadius,
                        valueRange = 0.5f..8f,
                        valueLabel = String.format(Locale.US, "%.1f px", textCfg.textShadowRadius),
                        onValueChange = { v ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(textShadowRadius = v)) }
                        }
                    )
                }

                // Text Outline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Latin Key Stroke Outline", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = textCfg.enableTextOutline,
                        onCheckedChange = { c ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(enableTextOutline = c)) }
                        }
                    )
                }
                if (textCfg.enableTextOutline) {
                    LabeledSliderRow(
                        label = "Outline Stroke Width",
                        value = textCfg.textOutlineWidthPx,
                        valueRange = 0.5f..4f,
                        valueLabel = String.format(Locale.US, "%.1f dp", textCfg.textOutlineWidthPx),
                        onValueChange = { v ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(textOutlineWidthPx = v)) }
                        }
                    )
                    InteractiveColorPickerSection(
                        title = "Outline Stroke Color",
                        currentHex = textCfg.textOutlineColorHex,
                        onColorSelected = { hex ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(textOutlineColorHex = hex)) }
                        }
                    )
                }

                // Label Glow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Subtle Key Label Glow", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = textCfg.enableLabelGlow,
                        onCheckedChange = { c ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(enableLabelGlow = c)) }
                        }
                    )
                }
                if (textCfg.enableLabelGlow) {
                    InteractiveColorPickerSection(
                        title = "Label Glow Color",
                        currentHex = textCfg.labelGlowColorHex,
                        onColorSelected = { hex ->
                            onUpdateDraft { it.copy(textStyle = it.textStyle.copy(labelGlowColorHex = hex)) }
                        }
                    )
                }
            }
        }
    }
}
