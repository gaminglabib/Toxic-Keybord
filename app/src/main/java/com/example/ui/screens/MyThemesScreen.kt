package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.theme.editor.TextPromptDialog
import com.example.theme.importexport.ImportedThemePreviewResult
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.ui.StudioDestination
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveKeyboardPreviewCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ToxicEmeraldPrimary
import com.example.util.ColorAndImageUtils

@Composable
fun MyThemesScreen(
    savedThemes: List<KeyboardTheme>,
    activeTheme: KeyboardTheme,
    globalSettings: GlobalKeyboardSettings,
    pendingImportPreview: ImportedThemePreviewResult?,
    onApplyTheme: (KeyboardTheme) -> Unit,
    onEditTheme: (KeyboardTheme, StudioDestination) -> Unit,
    onDuplicateTheme: (KeyboardTheme) -> Unit,
    onRenameTheme: (KeyboardTheme, String) -> Unit,
    onDeleteTheme: (KeyboardTheme) -> Unit,
    onCreateNewTheme: () -> Unit,
    onRequestImportFile: () -> Unit,
    onRequestExportFile: (KeyboardTheme, Boolean, Boolean) -> Unit,
    onConfirmPendingImport: (Boolean) -> Unit,
    onCancelPendingImport: () -> Unit
) {
    var filterBuiltIn by remember { mutableStateOf<Boolean?>(null) } // null = All, false = Custom, true = Built-in
    var themeToRename by remember { mutableStateOf<KeyboardTheme?>(null) }
    var themeToDelete by remember { mutableStateOf<KeyboardTheme?>(null) }
    var themeToExport by remember { mutableStateOf<KeyboardTheme?>(null) }
    var previewedThemeInModal by remember { mutableStateOf<KeyboardTheme?>(null) }

    val displayedThemes = remember(savedThemes, filterBuiltIn) {
        when (filterBuiltIn) {
            null -> savedThemes
            false -> savedThemes.filter { !it.isBuiltIn }
            true -> savedThemes.filter { it.isBuiltIn }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("my_themes_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard {
                SectionHeader(
                    title = "My Themes Library",
                    subtitle = "Create, preview, apply, duplicate, import & export portable .tks.json themes",
                    badgeText = "${savedThemes.size} SAVED"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateNewTheme,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("my_themes_new_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Theme")
                    }
                    OutlinedButton(
                        onClick = onRequestImportFile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("my_themes_import_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import .JSON")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterBuiltIn == null,
                        onClick = { filterBuiltIn = null },
                        label = { Text("All (${savedThemes.size})", maxLines = 1) }
                    )
                    FilterChip(
                        selected = filterBuiltIn == false,
                        onClick = { filterBuiltIn = false },
                        label = { Text("My Custom (${savedThemes.count { !it.isBuiltIn }})", maxLines = 1) }
                    )
                    FilterChip(
                        selected = filterBuiltIn == true,
                        onClick = { filterBuiltIn = true },
                        label = { Text("Built-In Presets (${savedThemes.count { it.isBuiltIn }})", maxLines = 1) }
                    )
                }
            }
        }

        if (displayedThemes.isEmpty()) {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Custom Themes Yet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Create Theme' above or duplicate any built-in preset to build your custom keyboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(displayedThemes, key = { it.id }) { themeItem ->
            val isActive = themeItem.id == activeTheme.id
            val bgCol = Color(ColorAndImageUtils.parseColorSafe(themeItem.background.baseColorHex))
            val keyCol = Color(ColorAndImageUtils.parseColorSafe(themeItem.defaultKeyStyle.backgroundColorHex))
            val labelCol = Color(ColorAndImageUtils.parseColorSafe(themeItem.defaultKeyStyle.labelColorHex))
            val borderCol = Color(ColorAndImageUtils.parseColorSafe(themeItem.defaultKeyStyle.borderColorHex))

            GlassCard(
                borderColor = if (isActive) ToxicEmeraldPrimary else MaterialTheme.colorScheme.outline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = themeItem.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active Keyboard Theme",
                                    tint = ToxicEmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = buildString {
                                append(if (themeItem.isBuiltIn) "Built-in Preset" else "Custom Theme")
                                append(" • Font: ${themeItem.textStyle.fontFamilyId}")
                                append(" • Radius: ${themeItem.defaultKeyStyle.cornerRadiusDp.toInt()}dp")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Row {
                        IconButton(onClick = { themeToRename = themeItem }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename ${themeItem.name}")
                        }
                        IconButton(onClick = { onDuplicateTheme(themeItem) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate ${themeItem.name}")
                        }
                        IconButton(onClick = { themeToExport = themeItem }) {
                            Icon(Icons.Default.Share, contentDescription = "Export ${themeItem.name}")
                        }
                        if (!themeItem.isBuiltIn) {
                            IconButton(onClick = { themeToDelete = themeItem }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete ${themeItem.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Visual Swatch Row of Keys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgCol)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Q", "W", "E", "R", "T", "Y", "ক", "খ", "↵").forEach { keyLabel ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(themeItem.defaultKeyStyle.cornerRadiusDp.dp))
                                .background(keyCol)
                                .border(
                                    width = 1.dp,
                                    color = borderCol,
                                    shape = RoundedCornerShape(themeItem.defaultKeyStyle.cornerRadiusDp.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = keyLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = labelCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApplyTheme(themeItem) },
                        enabled = !isActive,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).testTag("apply_theme_${themeItem.id}")
                    ) {
                        Text(if (isActive) "Active" else "Apply", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { previewedThemeInModal = themeItem },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Preview", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onEditTheme(themeItem, StudioDestination.THEME_STUDIO) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Customize", maxLines = 1)
                    }
                }
            }
        }
    }

    // Modal for Live Previewing any saved theme
    previewedThemeInModal?.let { modalTheme ->
        AlertDialog(
            onDismissRequest = { previewedThemeInModal = null },
            title = { Text("Preview: ${modalTheme.name}") },
            text = {
                LiveKeyboardPreviewCard(
                    theme = modalTheme,
                    globalSettings = globalSettings
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyTheme(modalTheme)
                        previewedThemeInModal = null
                    }
                ) {
                    Text("Apply to Keyboard")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { previewedThemeInModal = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Export Options Dialog (Include background image? Include custom font with license confirmation?)
    themeToExport?.let { exporting ->
        var includeBg by remember { mutableStateOf(!exporting.background.customImagePath.isNullOrBlank()) }
        var includeFont by remember { mutableStateOf(false) }
        val hasCustomBg = !exporting.background.customImagePath.isNullOrBlank()
        val hasCustomFont = !exporting.textStyle.customFontPath.isNullOrBlank()

        AlertDialog(
            onDismissRequest = { themeToExport = null },
            title = { Text("Export '${exporting.name}'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Creates a versioned .tks.json theme package compatible with TOXIC KEYBOARD STUDIO.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (hasCustomBg) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeBg, onCheckedChange = { includeBg = it })
                            Text(
                                text = "Bundle background photo inside JSON package",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (hasCustomFont) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeFont, onCheckedChange = { includeFont = it })
                            Text(
                                text = "Include custom font (I confirm its license permits redistribution)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if ((hasCustomBg && !includeBg) || (hasCustomFont && !includeFont)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "Note: Omitted image/font assets will safely fall back to theme gradient and system font when imported on another device.",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = exporting
                        themeToExport = null
                        onRequestExportFile(target, includeBg, includeFont)
                    }
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Save Location")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { themeToExport = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Pending Import Preview & Confirmation Modal
    pendingImportPreview?.let { previewResult ->
        AlertDialog(
            onDismissRequest = onCancelPendingImport,
            title = { Text("Import Theme: '${previewResult.theme.name}'?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = buildString {
                            append("Validated schema v${previewResult.theme.schemaVersion}. ")
                            if (previewResult.hasEmbeddedBackground) append("Includes custom background image. ")
                            if (previewResult.hasEmbeddedFont) append("Includes validated custom font. ")
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (previewResult.warnings.isNotEmpty()) {
                        previewResult.warnings.forEach { warn ->
                            Text(
                                text = "• $warn",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    LiveKeyboardPreviewCard(
                        theme = previewResult.theme,
                        globalSettings = globalSettings
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onConfirmPendingImport(true) }) {
                    Text("Import & Apply")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onConfirmPendingImport(false) }) {
                        Text("Import Only")
                    }
                    OutlinedButton(onClick = onCancelPendingImport) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    themeToRename?.let { target ->
        TextPromptDialog(
            title = "Rename '${target.name}'",
            initialValue = target.name,
            confirmText = "Save Name",
            onConfirm = { newName ->
                onRenameTheme(target, newName)
                themeToRename = null
            },
            onDismiss = { themeToRename = null }
        )
    }

    themeToDelete?.let { target ->
        ConfirmationDialog(
            title = "Delete '${target.name}'?",
            message = "Are you sure you want to permanently delete this custom theme from local storage?",
            confirmText = "Delete Theme",
            isDestructive = true,
            onConfirm = { onDeleteTheme(target) },
            onDismiss = { themeToDelete = null }
        )
    }
}
