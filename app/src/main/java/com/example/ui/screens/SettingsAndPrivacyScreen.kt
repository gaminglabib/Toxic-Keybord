package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ClipboardItemEntity
import com.example.data.PersonalWordEntity
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.LabeledSliderRow
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ToxicCrimsonAccent
import com.example.ui.theme.ToxicEmeraldPrimary

@Composable
fun SettingsAndPrivacyScreen(
    globalSettings: GlobalKeyboardSettings,
    studioLightMode: Boolean,
    dictionaryWords: List<PersonalWordEntity>,
    clipboardItems: List<ClipboardItemEntity>,
    onToggleStudioLightMode: (Boolean) -> Unit,
    onUpdateGlobalSettings: ((GlobalKeyboardSettings) -> GlobalKeyboardSettings) -> Unit,
    onAddDictionaryWord: (String, String) -> Unit,
    onRemoveDictionaryWord: (String) -> Unit,
    onClearDictionary: () -> Unit,
    onDeleteClipboardItem: (Long) -> Unit,
    onClearClipboard: () -> Unit,
    onResetAllSettings: () -> Unit
) {
    var newWordInput by remember { mutableStateOf("") }
    var newWordLang by remember { mutableStateOf("en") }
    var showClearDictConfirm by remember { mutableStateOf(false) }
    var showClearClipConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_privacy_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Typing, Audio, Haptics & Language Preferences
        item {
            GlassCard {
                SectionHeader(
                    title = "Keyboard Typing & Audio Preferences",
                    subtitle = "Global defaults shared with ToxicInputMethodService"
                )

                Text("Default Startup Language:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = globalSettings.defaultLanguage == "en",
                        onClick = { onUpdateGlobalSettings { it.copy(defaultLanguage = "en") } },
                        label = { Text("English (QWERTY)") }
                    )
                    FilterChip(
                        selected = globalSettings.defaultLanguage == "bn",
                        onClick = { onUpdateGlobalSettings { it.copy(defaultLanguage = "bn") } },
                        label = { Text("বাংলা (Bengali Unicode)") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Keypress Sound Feedback",
                    subtitle = "Play system click sound on key taps",
                    checked = globalSettings.globalSoundEnabled,
                    testTag = "settings_toggle_sound",
                    onCheckedChange = { c -> onUpdateGlobalSettings { it.copy(globalSoundEnabled = c) } }
                )

                SettingsSwitchRow(
                    title = "Haptic Vibration Feedback",
                    subtitle = "Vibrate on key presses and long-press secondary symbols",
                    checked = globalSettings.globalVibrationEnabled,
                    testTag = "settings_toggle_vibration",
                    onCheckedChange = { c -> onUpdateGlobalSettings { it.copy(globalVibrationEnabled = c) } }
                )

                if (globalSettings.globalVibrationEnabled) {
                    LabeledSliderRow(
                        label = "Vibration Intensity Duration",
                        value = globalSettings.globalVibrationStrengthMs.toFloat(),
                        valueRange = 5f..80f,
                        valueLabel = "${globalSettings.globalVibrationStrengthMs} ms",
                        onValueChange = { v ->
                            onUpdateGlobalSettings { it.copy(globalVibrationStrengthMs = v.toInt()) }
                        },
                        onReset = {
                            onUpdateGlobalSettings { it.copy(globalVibrationStrengthMs = 18) }
                        }
                    )
                }
            }
        }

        // 2. Accessibility & Visual Readability
        item {
            GlassCard {
                SectionHeader(
                    title = "Accessibility & Display",
                    subtitle = "High-contrast overrides, readable key labels & reduced motion"
                )

                SettingsSwitchRow(
                    title = "High-Contrast Accessibility Mode",
                    subtitle = "Forces pitch-black backdrop, crisp emerald borders, and pure white key labels",
                    checked = globalSettings.highContrastOverride,
                    testTag = "settings_toggle_high_contrast",
                    onCheckedChange = { c -> onUpdateGlobalSettings { it.copy(highContrastOverride = c) } }
                )

                SettingsSwitchRow(
                    title = "Boost Key Label Readability (+2sp)",
                    subtitle = "Enlarges primary keycap labels across all layouts for easier reading",
                    checked = globalSettings.readableKeyLabelsBoost,
                    testTag = "settings_toggle_readable_labels",
                    onCheckedChange = { c -> onUpdateGlobalSettings { it.copy(readableKeyLabelsBoost = c) } }
                )

                SettingsSwitchRow(
                    title = "Reduced Motion & Effects Override",
                    subtitle = "Disables key pop animations, shadows, and glow globally",
                    checked = globalSettings.reducedMotionOverride,
                    testTag = "settings_toggle_reduced_motion",
                    onCheckedChange = { c -> onUpdateGlobalSettings { it.copy(reducedMotionOverride = c) } }
                )

                SettingsSwitchRow(
                    title = "Studio App Daylight Appearance",
                    subtitle = "Switch the Studio editor interface between Premium Dark (default) and Light mode",
                    checked = studioLightMode,
                    testTag = "settings_toggle_light_mode",
                    onCheckedChange = onToggleStudioLightMode
                )
            }
        }

        // 3. Local Suggestions & Personal Dictionary
        item {
            GlassCard {
                SectionHeader(
                    title = "Suggestion Bar & Personal Dictionary",
                    subtitle = "100% on-device word suggestions • Automatically paused in password fields",
                    badgeText = "${dictionaryWords.size} WORDS"
                )

                SettingsSwitchRow(
                    title = "Show Top Word Suggestion Strip",
                    subtitle = "Displays 3 local word completions in the keyboard toolbar",
                    checked = globalSettings.enableSuggestions,
                    testTag = "settings_toggle_suggestions",
                    onCheckedChange = { c -> onUpdateGlobalSettings { it.copy(enableSuggestions = c) } }
                )

                SettingsSwitchRow(
                    title = "Learn Typed Words Locally",
                    subtitle = "Saves non-sensitive words to your local Room database dictionary",
                    checked = globalSettings.enablePersonalDictionaryLearning,
                    testTag = "settings_toggle_dict_learning",
                    onCheckedChange = { c ->
                        onUpdateGlobalSettings { it.copy(enablePersonalDictionaryLearning = c) }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newWordInput,
                        onValueChange = { newWordInput = it.take(32) },
                        label = { Text("Add word (EN or বাংলা)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dictionary_word_input")
                    )
                    FilterChip(
                        selected = newWordLang == "en",
                        onClick = { newWordLang = if (newWordLang == "en") "bn" else "en" },
                        label = { Text(if (newWordLang == "en") "EN" else "বাং") }
                    )
                    Button(
                        onClick = {
                            if (newWordInput.isNotBlank()) {
                                onAddDictionaryWord(newWordInput, newWordLang)
                                newWordInput = ""
                            }
                        },
                        modifier = Modifier.testTag("add_dictionary_word_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add word")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                dictionaryWords.take(8).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${entry.word} (${entry.languageCode.uppercase()})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick = { onRemoveDictionaryWord(entry.word) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ${entry.word}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showClearDictConfirm = true },
                    modifier = Modifier.fillMaxWidth().testTag("clear_dictionary_button")
                ) {
                    Text("Clear Local Personal Dictionary")
                }
            }
        }

        // 4. Opt-in Local Clipboard History
        item {
            GlassCard {
                SectionHeader(
                    title = "Opt-In Local Clipboard Manager",
                    subtitle = "Disabled by default for privacy. Never records during password fields.",
                    badgeText = "${clipboardItems.size} CLIPS"
                )

                SettingsSwitchRow(
                    title = "Enable Local Clipboard History",
                    subtitle = "Keep recent copied snippets accessible from the 📋 toolbar button",
                    checked = globalSettings.enableClipboardHistory,
                    testTag = "settings_toggle_clipboard",
                    onCheckedChange = { c -> onUpdateGlobalSettings { it.copy(enableClipboardHistory = c) } }
                )

                if (clipboardItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    clipboardItems.take(6).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.textContent.replace("\n", " ").take(45),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteClipboardItem(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete clip",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showClearClipConfirm = true },
                    modifier = Modifier.fillMaxWidth().testTag("clear_clipboard_button")
                ) {
                    Text("Clear Clipboard History")
                }
            }
        }

        // 5. Privacy, Security & Permission Explanations
        item {
            GlassCard(borderColor = ToxicEmeraldPrimary.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ToxicEmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy, Security & Architecture",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PrivacyBullet(
                    title = "100% Offline • Zero Network Permissions",
                    body = "TOXIC KEYBOARD STUDIO does not request android.permission.INTERNET. Your keystrokes, themes, photos, and personal dictionary cannot leave your device."
                )
                PrivacyBullet(
                    title = "Sensitive & Password Input Protection",
                    body = "When Android signals a password, PIN, filter, or IME_FLAG_NO_PERSONALIZED_LEARNING field, the keyboard displays '🔒 Protected Input', suppresses suggestions, and halts dictionary learning and clipboard recording."
                )
                PrivacyBullet(
                    title = "Zero Broad Storage Permissions",
                    body = "Background photos and custom .TTF/.OTF fonts are imported via the Android Photo Picker and Storage Access Framework (SAF) directly into app-private storage without requesting READ_EXTERNAL_STORAGE."
                )
                PrivacyBullet(
                    title = "Minimal Hardware Permission (VIBRATE only)",
                    body = "Only android.permission.VIBRATE is declared in AndroidManifest.xml so the keyboard can provide optional tactile key-press vibration."
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showResetAllConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ToxicCrimsonAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_all_settings_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Settings & Restore Built-In Themes")
                }
            }
        }
    }

    if (showClearDictConfirm) {
        ConfirmationDialog(
            title = "Clear Personal Dictionary?",
            message = "This will remove all learned and custom words from your on-device dictionary.",
            confirmText = "Clear Dictionary",
            isDestructive = true,
            onConfirm = onClearDictionary,
            onDismiss = { showClearDictConfirm = false }
        )
    }

    if (showClearClipConfirm) {
        ConfirmationDialog(
            title = "Clear Clipboard History?",
            message = "All saved clipboard snippets in TOXIC Keyboard will be permanently deleted.",
            confirmText = "Clear Clipboard",
            isDestructive = true,
            onConfirm = onClearClipboard,
            onDismiss = { showClearClipConfirm = false }
        )
    }

    if (showResetAllConfirm) {
        ConfirmationDialog(
            title = "Reset All Keyboard Settings?",
            message = "This will restore default keyboard preferences and reset built-in themes to their factory configuration.",
            confirmText = "Reset Everything",
            isDestructive = true,
            onConfirm = onResetAllSettings,
            onDismiss = { showResetAllConfirm = false }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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

@Composable
private fun PrivacyBullet(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = ToxicEmeraldPrimary,
                modifier = Modifier.size(16.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
