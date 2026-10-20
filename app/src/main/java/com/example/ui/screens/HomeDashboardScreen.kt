package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.ui.StudioDestination
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveKeyboardPreviewCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ToxicAmberWarning
import com.example.ui.theme.ToxicCrimsonAccent
import com.example.ui.theme.ToxicEmeraldPrimary
import com.example.util.ColorAndImageUtils
import com.example.util.ImeStatusHelper
import com.example.util.SystemImeStatus

@Composable
fun HomeDashboardScreen(
    imeStatus: SystemImeStatus,
    activeTheme: KeyboardTheme,
    savedThemes: List<KeyboardTheme>,
    globalSettings: GlobalKeyboardSettings,
    sandboxText: String,
    onSandboxTextChange: (String) -> Unit,
    onRefreshImeStatus: () -> Unit,
    onNavigate: (StudioDestination) -> Unit,
    onEditTheme: (KeyboardTheme, StudioDestination) -> Unit,
    onApplyTheme: (KeyboardTheme) -> Unit,
    onCreateNewTheme: () -> Unit,
    onGenerateFromPrompt: (String) -> Unit,
    onRequestImportTheme: () -> Unit,
    onRequestExportTheme: (KeyboardTheme) -> Unit
) {
    val context = LocalContext.current
    val hostView = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val testFieldFocusRequester = remember { FocusRequester() }

    var promptInput by remember {
        mutableStateOf("Create a black and emerald green cyberpunk keyboard with bold white letters")
    }
    var systemKeyboardTestField by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Branding Header
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF06150E),
                                    Color(0xFF0D291C),
                                    Color(0xFF1A0910)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ToxicEmeraldPrimary.copy(alpha = 0.16f))
                                        .border(1.dp, ToxicEmeraldPrimary, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Keyboard,
                                        contentDescription = "Toxic Keyboard Studio Emblem",
                                        tint = ToxicEmeraldPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "TOXIC KEYBOARD STUDIO",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Custom Theme & Native Input Engine • API 28+",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFA7F3D0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onNavigate(StudioDestination.SETTINGS_AND_PRIVACY) },
                                modifier = Modifier.testTag("dashboard_settings_shortcut")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Keyboard Settings",
                                    tint = ToxicEmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = "Active Theme: ${activeTheme.name}",
                                color = ToxicEmeraldPrimary
                            )
                            StatusPill(
                                label = "100% Offline • Zero Telemetry",
                                color = Color(0xFF69F0AE)
                            )
                        }
                    }
                }
            }
        }

        // 2. Real System Keyboard Activation & Default Selection Status Card
        item {
            GlassCard(
                borderColor = if (imeStatus.isEnabled && imeStatus.isSelectedDefault) {
                    ToxicEmeraldPrimary.copy(alpha = 0.6f)
                } else {
                    ToxicAmberWarning.copy(alpha = 0.7f)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (imeStatus.isEnabled && imeStatus.isSelectedDefault) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Warning
                            },
                            contentDescription = "Keyboard Activation Status",
                            tint = if (imeStatus.isEnabled && imeStatus.isSelectedDefault) {
                                ToxicEmeraldPrimary
                            } else {
                                ToxicAmberWarning
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System Keyboard Activation",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onRefreshImeStatus,
                        modifier = Modifier.testTag("refresh_ime_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Keyboard Status"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActivationStepBadge(
                        modifier = Modifier.weight(1f),
                        title = "Step 1: Enabled",
                        statusText = if (imeStatus.isEnabled) "Enabled in Android" else "Not Enabled",
                        isComplete = imeStatus.isEnabled
                    )
                    ActivationStepBadge(
                        modifier = Modifier.weight(1f),
                        title = "Step 2: Default IME",
                        statusText = if (imeStatus.isSelectedDefault) "Active Default" else "Not Selected",
                        isComplete = imeStatus.isSelectedDefault
                    )
                }

                if (!imeStatus.isEnabled || !imeStatus.isSelectedDefault) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (!imeStatus.isEnabled) {
                            "To use TOXIC Keyboard across your phone: Tap '1. Enable Keyboard' below and toggle ON 'TOXIC KEYBOARD STUDIO' in Android's On-screen keyboard list."
                        } else {
                            "TOXIC Keyboard is enabled! Now tap '2. Select Default' and choose 'TOXIC KEYBOARD STUDIO' from the system chooser."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { ImeStatusHelper.launchEnableKeyboardSettings(context) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("enable_keyboard_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (imeStatus.isEnabled) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Text(
                            text = "1. Enable Keyboard",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (imeStatus.isEnabled) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    }

                    Button(
                        onClick = { ImeStatusHelper.showKeyboardPickerDialog(context) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_default_keyboard_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (imeStatus.isSelectedDefault) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Text(
                            text = "2. Select Default",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (imeStatus.isSelectedDefault) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = systemKeyboardTestField,
                    onValueChange = { systemKeyboardTestField = it },
                    label = { Text("Tap here to summon active system keyboard") },
                    placeholder = { Text("Test English & বাংলা input...") },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                try {
                                    testFieldFocusRequester.requestFocus()
                                    keyboardController?.show()
                                    ImeStatusHelper.summonSoftKeyboard(hostView)
                                } catch (_: Exception) {
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("summon_keyboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Summon System Keyboard",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Show", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(testFieldFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                keyboardController?.show()
                                ImeStatusHelper.summonSoftKeyboard(hostView)
                            }
                        }
                        .testTag("system_keyboard_test_input")
                )
            }
        }

        // 3. Studio Modules Grid (Theme Studio, Text Style Studio, Layout Studio, My Themes)
        item {
            SectionHeader(
                title = "Studio Workspaces",
                subtitle = "Customize every pixel of your keyboard"
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioEntryCard(
                        modifier = Modifier.weight(1f),
                        title = "Theme Studio",
                        subtitle = "Photo BG, Per-Key Colors, Borders & Glow",
                        icon = Icons.Default.ColorLens,
                        accentColor = ToxicEmeraldPrimary,
                        testTag = "entry_theme_studio",
                        onClick = { onEditTheme(activeTheme, StudioDestination.THEME_STUDIO) }
                    )
                    StudioEntryCard(
                        modifier = Modifier.weight(1f),
                        title = "Text Style Studio",
                        subtitle = "Custom .TTF/.OTF Fonts, Size, Outline & বাংলা",
                        icon = Icons.Default.FontDownload,
                        accentColor = Color(0xFF00E5FF),
                        testTag = "entry_text_studio",
                        onClick = { onEditTheme(activeTheme, StudioDestination.TEXT_STYLE_STUDIO) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioEntryCard(
                        modifier = Modifier.weight(1f),
                        title = "Layout Studio",
                        subtitle = "Key Height, Number Row, Spacing & Widths",
                        icon = Icons.Default.GridView,
                        accentColor = Color(0xFFFFD600),
                        testTag = "entry_layout_studio",
                        onClick = { onEditTheme(activeTheme, StudioDestination.LAYOUT_STUDIO) }
                    )
                    StudioEntryCard(
                        modifier = Modifier.weight(1f),
                        title = "My Themes (${savedThemes.size})",
                        subtitle = "Manage, Duplicate, Import & Export",
                        icon = Icons.Default.Tune,
                        accentColor = ToxicCrimsonAccent,
                        testTag = "entry_my_themes",
                        onClick = { onNavigate(StudioDestination.MY_THEMES) }
                    )
                }
            }
        }

        // 4. Interactive Live Preview of Active Theme
        item {
            LiveKeyboardPreviewCard(
                theme = activeTheme,
                globalSettings = globalSettings,
                sandboxText = sandboxText,
                onSandboxTextChange = onSandboxTextChange
            )
        }

        // 5. Recent & Built-in Theme Cards Carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Theme Library & Recent",
                    subtitle = "Tap Apply to switch your live keyboard immediately"
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Create New Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, ToxicEmeraldPrimary),
                    modifier = Modifier
                        .width(175.dp)
                        .height(160.dp)
                        .clickable { onCreateNewTheme() }
                        .testTag("create_new_theme_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Theme",
                            tint = ToxicEmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New Custom Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Start from scratch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                savedThemes.forEach { themeItem ->
                    val isCurrent = themeItem.id == activeTheme.id
                    ThemeMiniPreviewCard(
                        theme = themeItem,
                        isCurrent = isCurrent,
                        onApply = { onApplyTheme(themeItem) },
                        onCustomize = { onEditTheme(themeItem, StudioDestination.THEME_STUDIO) }
                    )
                }
            }
        }

        // 6. Offline Preset & Prompt Theme Synthesizer (AI-Ready Extensible Architecture)
        item {
            GlassCard {
                SectionHeader(
                    title = "Instant Theme Synthesizer",
                    subtitle = "Describe a look to synthesize a custom theme 100% offline",
                    badgeText = "OFFLINE ENGINE"
                )
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = { Text("Describe your theme colors, key shape, and font") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_synthesizer_prompt_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val samplePrompts = listOf(
                        "Black and emerald green cyberpunk with bold white letters",
                        "Crimson red glass keyboard with rounded pill keys",
                        "Pitch black OLED hacker matrix mono with green glow",
                        "Gold and obsidian luxury keyboard with sharp borders"
                    )
                    samplePrompts.forEach { presetPrompt ->
                        AssistChip(
                            onClick = {
                                promptInput = presetPrompt
                                onGenerateFromPrompt(presetPrompt)
                            },
                            label = { Text(presetPrompt.take(32) + "…") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onGenerateFromPrompt(promptInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("synthesize_theme_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synthesize & Open in Theme Studio", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // 7. Quick Import / Export & Privacy Actions
        item {
            GlassCard {
                SectionHeader(
                    title = "Portable Theme Packages & Privacy",
                    subtitle = "Backup or share .tks.json themes with embedded photos & fonts"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRequestImportTheme,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_import_theme_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Theme", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { onRequestExportTheme(activeTheme) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_export_theme_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Active", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(StudioDestination.SETTINGS_AND_PRIVACY) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Guarantee",
                            tint = ToxicEmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zero-Network Privacy Architecture",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "No internet permission requested. Passwords & sensitive fields automatically disable learning and clipboard history.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ActivationStepBadge(
    modifier: Modifier = Modifier,
    title: String,
    statusText: String,
    isComplete: Boolean
) {
    val accent = if (isComplete) ToxicEmeraldPrimary else ToxicAmberWarning
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudioEntryCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 128.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ThemeMiniPreviewCard(
    theme: KeyboardTheme,
    isCurrent: Boolean,
    onApply: () -> Unit,
    onCustomize: () -> Unit
) {
    val bgCol = Color(ColorAndImageUtils.parseColorSafe(theme.background.baseColorHex))
    val keyCol = Color(ColorAndImageUtils.parseColorSafe(theme.defaultKeyStyle.backgroundColorHex))
    val labelCol = Color(ColorAndImageUtils.parseColorSafe(theme.defaultKeyStyle.labelColorHex))
    val borderCol = Color(ColorAndImageUtils.parseColorSafe(theme.defaultKeyStyle.borderColorHex))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent) ToxicEmeraldPrimary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .width(205.dp)
            .height(160.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active Theme",
                            tint = ToxicEmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mini visual keycaps row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgCol)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Q", "W", "E", "বাং").forEach { k ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(keyCol)
                                .border(1.dp, borderCol, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = k,
                                style = MaterialTheme.typography.labelSmall,
                                color = labelCol,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onApply,
                    enabled = !isCurrent,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text(
                        text = if (isCurrent) "Active" else "Apply",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onCustomize,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
