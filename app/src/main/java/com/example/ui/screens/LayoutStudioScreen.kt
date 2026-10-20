package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.theme.model.KeyboardTheme
import com.example.theme.model.LayoutConfig
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.LabeledSliderRow
import com.example.ui.components.LiveKeyboardPreviewCard
import com.example.ui.components.SectionHeader
import java.util.Locale

@Composable
fun LayoutStudioScreen(
    draftTheme: KeyboardTheme,
    globalSettings: GlobalKeyboardSettings,
    sandboxText: String,
    onSandboxTextChange: (String) -> Unit,
    onUpdateDraft: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    onSaveAndApply: () -> Unit
) {
    val layout = draftTheme.layout

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("layout_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keyboard Layout Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Number row, portrait/landscape heights, key spacing & special key widths",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onSaveAndApply,
                        modifier = Modifier.testTag("layout_studio_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply")
                    }
                }
            }
        }

        item {
            LiveKeyboardPreviewCard(
                theme = draftTheme,
                globalSettings = globalSettings,
                sandboxText = sandboxText,
                onSandboxTextChange = onSandboxTextChange
            )
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "Rows & Special Keys Visibility",
                    subtitle = "Mandatory typing keys (Space, Backspace, Enter, Shift) are always protected"
                )

                LayoutToggleItem(
                    title = "Dedicated Top Number Row (1–0 / ১–০)",
                    subtitle = "Displays persistent numbers above the top letter row",
                    checked = layout.showNumberRow,
                    testTag = "toggle_number_row",
                    onCheckedChange = { c ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(showNumberRow = c)) }
                    }
                )

                LayoutToggleItem(
                    title = "Top Suggestion & Quick-Action Toolbar",
                    subtitle = "Shows word suggestions, language switch, emoji & clipboard buttons",
                    checked = layout.showTopToolbar,
                    testTag = "toggle_top_toolbar",
                    onCheckedChange = { c ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(showTopToolbar = c)) }
                    }
                )

                LayoutToggleItem(
                    title = "Dedicated Bottom Language Switch Key (🌐 EN/বাং)",
                    subtitle = "One-tap toggle between English QWERTY and Bengali Unicode",
                    checked = layout.showLanguageKey,
                    testTag = "toggle_language_key",
                    onCheckedChange = { c ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(showLanguageKey = c)) }
                    }
                )

                LayoutToggleItem(
                    title = "Dedicated Bottom Emoji Key (☺)",
                    subtitle = "Quick emoji palette key next to the spacebar",
                    checked = layout.showEmojiKey,
                    testTag = "toggle_emoji_key",
                    onCheckedChange = { c ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(showEmojiKey = c)) }
                    }
                )
            }
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "Dimensions, Spacing & Proportions",
                    subtitle = "Validated ranges guarantee comfortable thumb ergonomics on Android 9+"
                )

                LabeledSliderRow(
                    label = "Portrait Row Key Height",
                    value = layout.keyHeightDp,
                    valueRange = 38f..64f,
                    valueLabel = String.format(Locale.US, "%.0f dp", layout.keyHeightDp),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(keyHeightDp = v)) }
                    },
                    onReset = {
                        onUpdateDraft { it.copy(layout = it.layout.copy(keyHeightDp = 48f)) }
                    }
                )

                LabeledSliderRow(
                    label = "Landscape Row Key Height",
                    value = layout.landscapeKeyHeightDp,
                    valueRange = 32f..52f,
                    valueLabel = String.format(Locale.US, "%.0f dp", layout.landscapeKeyHeightDp),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(landscapeKeyHeightDp = v)) }
                    },
                    onReset = {
                        onUpdateDraft { it.copy(layout = it.layout.copy(landscapeKeyHeightDp = 40f)) }
                    }
                )

                LabeledSliderRow(
                    label = "Horizontal Key Gap Spacing",
                    value = layout.horizontalSpacingDp,
                    valueRange = 2f..10f,
                    valueLabel = String.format(Locale.US, "%.1f dp", layout.horizontalSpacingDp),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(horizontalSpacingDp = v)) }
                    },
                    onReset = {
                        onUpdateDraft { it.copy(layout = it.layout.copy(horizontalSpacingDp = 4.5f)) }
                    }
                )

                LabeledSliderRow(
                    label = "Vertical Row Gap Spacing",
                    value = layout.verticalSpacingDp,
                    valueRange = 2f..12f,
                    valueLabel = String.format(Locale.US, "%.1f dp", layout.verticalSpacingDp),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(verticalSpacingDp = v)) }
                    },
                    onReset = {
                        onUpdateDraft { it.copy(layout = it.layout.copy(verticalSpacingDp = 6.0f)) }
                    }
                )

                LabeledSliderRow(
                    label = "Special Action Key Width (Shift / Backspace / Enter)",
                    value = layout.specialKeyWidthMultiplier,
                    valueRange = 1.1f..1.7f,
                    valueLabel = String.format(Locale.US, "%.2fx", layout.specialKeyWidthMultiplier),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(specialKeyWidthMultiplier = v)) }
                    },
                    onReset = {
                        onUpdateDraft { it.copy(layout = it.layout.copy(specialKeyWidthMultiplier = 1.35f)) }
                    }
                )

                LabeledSliderRow(
                    label = "Spacebar Relative Width Weight",
                    value = layout.spacebarWidthWeight,
                    valueRange = 3.0f..6.0f,
                    valueLabel = String.format(Locale.US, "%.1fx", layout.spacebarWidthWeight),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(spacebarWidthWeight = v)) }
                    },
                    onReset = {
                        onUpdateDraft { it.copy(layout = it.layout.copy(spacebarWidthWeight = 4.2f)) }
                    }
                )

                LabeledSliderRow(
                    label = "Bottom Chin Margin Padding",
                    value = layout.keyboardBottomPaddingDp,
                    valueRange = 0f..32f,
                    valueLabel = String.format(Locale.US, "%.0f dp", layout.keyboardBottomPaddingDp),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(layout = it.layout.copy(keyboardBottomPaddingDp = v)) }
                    },
                    onReset = {
                        onUpdateDraft { it.copy(layout = it.layout.copy(keyboardBottomPaddingDp = 8f)) }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onUpdateDraft { it.copy(layout = LayoutConfig()) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_layout_defaults_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Layout to Default Proportions")
                }
            }
        }
    }
}

@Composable
private fun LayoutToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
