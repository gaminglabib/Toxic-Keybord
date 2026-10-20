package com.example.theme.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.theme.model.BackgroundSourceType
import com.example.theme.model.KeyCategory
import com.example.theme.model.KeyStyleConfig
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveColorPickerSection
import com.example.ui.components.LabeledSliderRow
import com.example.ui.components.LiveKeyboardPreviewCard
import com.example.ui.components.SectionHeader
import java.util.Locale

@Composable
fun ThemeStudioScreen(
    draftTheme: KeyboardTheme,
    globalSettings: GlobalKeyboardSettings,
    hasUnsavedChanges: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    selectedCategory: KeyCategory,
    selectedIndividualKeyId: String?,
    currentKeyStyle: KeyStyleConfig,
    sandboxText: String,
    onSandboxTextChange: (String) -> Unit,
    onSelectCategory: (KeyCategory) -> Unit,
    onSelectIndividualKey: (String?) -> Unit,
    onUpdateDraft: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    onUpdateKeyStyle: ((KeyStyleConfig) -> KeyStyleConfig) -> Unit,
    onApplyStyleToAllCategories: (KeyStyleConfig) -> Unit,
    onResetKeyOrCategory: () -> Unit,
    onResetEntireTheme: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCancelUnsaved: () -> Unit,
    onRestorePreviousSaved: () -> Unit,
    onSaveAndApply: () -> Unit,
    onSaveAsNew: (String) -> Unit,
    onRenameTheme: (String) -> Unit,
    onDuplicateTheme: () -> Unit,
    onImportBackgroundUri: (Uri) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker (with automatic SAF fallback on Android 9)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportBackgroundUri(uri)
        }
    }

    val safDocumentFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportBackgroundUri(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("theme_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Theme Studio Header & Action Controls (Save, Save As, Undo, Redo, Reset, Duplicate)
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draftTheme.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (hasUnsavedChanges) "Unsaved draft modifications" else "All changes saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onUndo,
                            enabled = canUndo,
                            modifier = Modifier.testTag("editor_undo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = onRedo,
                            enabled = canRedo,
                            modifier = Modifier.testTag("editor_redo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.testTag("editor_rename_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename Theme")
                        }
                        IconButton(
                            onClick = onDuplicateTheme,
                            modifier = Modifier.testTag("editor_duplicate_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Theme")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveAndApply,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("editor_save_apply_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Apply", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showSaveAsDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_save_as_button")
                    ) {
                        Text("Save As", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onRestorePreviousSaved,
                        modifier = Modifier.testTag("editor_restore_saved_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Saved", maxLines = 1)
                    }
                    TextButton(
                        onClick = {
                            if (hasUnsavedChanges) showCancelConfirmDialog = true else onCancelUnsaved()
                        },
                        enabled = hasUnsavedChanges,
                        modifier = Modifier.testTag("editor_cancel_changes_button")
                    ) {
                        Text("Discard Changes", maxLines = 1)
                    }
                    TextButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.testTag("editor_reset_theme_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Theme", maxLines = 1)
                    }
                }
            }
        }

        // 2. Live Keyboard Preview with Interactive Key Picker
        item {
            LiveKeyboardPreviewCard(
                theme = draftTheme,
                globalSettings = globalSettings,
                allowKeyInspector = true,
                inspectedKeyId = selectedIndividualKeyId,
                onKeyInspected = { keySpec ->
                    onSelectCategory(keySpec.category)
                    onSelectIndividualKey(keySpec.id)
                    selectedSubTab = 1 // Jump to Key Style editor
                },
                sandboxText = sandboxText,
                onSandboxTextChange = onSandboxTextChange
            )
        }

        // 3. Sub-Navigation Tabs: Background | Key Colors & Shapes | Effects & Sound
        item {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Background", maxLines = 1) },
                    modifier = Modifier.testTag("tab_background_editor")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Key Styling", maxLines = 1) },
                    modifier = Modifier.testTag("tab_key_styling")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("Effects & Audio", maxLines = 1) },
                    modifier = Modifier.testTag("tab_effects_editor")
                )
            }
        }

        // 4. Active Sub-Tab Content
        when (selectedSubTab) {
            0 -> item {
                BackgroundEditorSection(
                    draftTheme = draftTheme,
                    onPickGalleryPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickDocumentPhotoFallback = {
                        safDocumentFallbackLauncher.launch(arrayOf("image/*"))
                    },
                    onUpdateDraft = onUpdateDraft
                )
            }
            1 -> item {
                IndividualAndCategoryKeyEditorSection(
                    draftTheme = draftTheme,
                    selectedCategory = selectedCategory,
                    selectedIndividualKeyId = selectedIndividualKeyId,
                    currentKeyStyle = currentKeyStyle,
                    onSelectCategory = onSelectCategory,
                    onSelectIndividualKey = onSelectIndividualKey,
                    onUpdateKeyStyle = onUpdateKeyStyle,
                    onApplyStyleToAllCategories = { onApplyStyleToAllCategories(currentKeyStyle) },
                    onResetKeyOrCategory = onResetKeyOrCategory
                )
            }
            2 -> item {
                EffectsAndFeedbackEditorSection(
                    draftTheme = draftTheme,
                    onUpdateDraft = onUpdateDraft
                )
            }
        }
    }

    if (showSaveAsDialog) {
        TextPromptDialog(
            title = "Save Theme As",
            initialValue = "${draftTheme.name} Custom",
            confirmText = "Save New Theme",
            onConfirm = { newName ->
                onSaveAsNew(newName)
                showSaveAsDialog = false
            },
            onDismiss = { showSaveAsDialog = false }
        )
    }

    if (showRenameDialog) {
        TextPromptDialog(
            title = "Rename Theme",
            initialValue = draftTheme.name,
            confirmText = "Rename",
            onConfirm = { newName ->
                onRenameTheme(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmationDialog(
            title = "Reset Entire Theme?",
            message = "This will revert all background, key colors, and effects in '${draftTheme.name}' back to factory defaults.",
            confirmText = "Reset Theme",
            isDestructive = true,
            onConfirm = onResetEntireTheme,
            onDismiss = { showResetConfirmDialog = false }
        )
    }

    if (showCancelConfirmDialog) {
        ConfirmationDialog(
            title = "Discard Unsaved Changes?",
            message = "All unsaved modifications since your last save will be reverted.",
            confirmText = "Discard",
            isDestructive = true,
            onConfirm = onCancelUnsaved,
            onDismiss = { showCancelConfirmDialog = false }
        )
    }
}

@Composable
private fun BackgroundEditorSection(
    draftTheme: KeyboardTheme,
    onPickGalleryPhoto: () -> Unit,
    onPickDocumentPhotoFallback: () -> Unit,
    onUpdateDraft: ((KeyboardTheme) -> KeyboardTheme) -> Unit
) {
    val bg = draftTheme.background
    GlassCard {
        SectionHeader(
            title = "Background Studio",
            subtitle = "Use personal gallery photos, cyber textures, or custom gradients"
        )

        // Source Mode Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = bg.sourceType == BackgroundSourceType.SOLID_OR_GRADIENT,
                onClick = {
                    onUpdateDraft {
                        it.copy(background = it.background.copy(sourceType = BackgroundSourceType.SOLID_OR_GRADIENT))
                    }
                },
                label = { Text("Solid / Gradient") }
            )
            FilterChip(
                selected = bg.sourceType == BackgroundSourceType.PRESET_DRAWABLE && bg.presetDrawableName == "img_bg_toxic_cyber",
                onClick = {
                    onUpdateDraft {
                        it.copy(
                            background = it.background.copy(
                                sourceType = BackgroundSourceType.PRESET_DRAWABLE,
                                presetDrawableName = "img_bg_toxic_cyber"
                            )
                        )
                    }
                },
                label = { Text("Emerald Grid Art") }
            )
            FilterChip(
                selected = bg.sourceType == BackgroundSourceType.PRESET_DRAWABLE && bg.presetDrawableName == "img_bg_crimson_glass",
                onClick = {
                    onUpdateDraft {
                        it.copy(
                            background = it.background.copy(
                                sourceType = BackgroundSourceType.PRESET_DRAWABLE,
                                presetDrawableName = "img_bg_crimson_glass"
                            )
                        )
                    }
                },
                label = { Text("Crimson Glass Art") }
            )
            FilterChip(
                selected = bg.sourceType == BackgroundSourceType.CUSTOM_IMAGE,
                onClick = onPickGalleryPhoto,
                label = { Text("Custom Photo") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickGalleryPhoto,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_gallery_photo_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Photo")
            }
            OutlinedButton(
                onClick = onPickDocumentPhotoFallback,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_saf_photo_button")
            ) {
                Text("Browse Files (SAF)")
            }
        }

        if (bg.sourceType == BackgroundSourceType.CUSTOM_IMAGE || bg.sourceType == BackgroundSourceType.PRESET_DRAWABLE) {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(
                title = "Image Crop, Transform & Atmosphere",
                subtitle = "Non-destructive zoom, rotation, offset, dimming & blur"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "Rotation: %.0f°", bg.rotationDegrees),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = {
                        onUpdateDraft {
                            val nextRot = (it.background.rotationDegrees + 90f) % 360f
                            it.copy(background = it.background.copy(rotationDegrees = nextRot))
                        }
                    },
                    modifier = Modifier.testTag("rotate_background_90_button")
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate +90°")
                }
            }

            LabeledSliderRow(
                label = "Crop / Scale Zoom",
                value = bg.scaleZoom,
                valueRange = 1.0f..3.0f,
                valueLabel = String.format(Locale.US, "%.2fx", bg.scaleZoom),
                onValueChange = { v -> onUpdateDraft { it.copy(background = it.background.copy(scaleZoom = v)) } },
                onReset = { onUpdateDraft { it.copy(background = it.background.copy(scaleZoom = 1.0f)) } }
            )
            LabeledSliderRow(
                label = "Horizontal Reposition (Pan X)",
                value = bg.offsetXPercent,
                valueRange = -0.5f..0.5f,
                valueLabel = String.format(Locale.US, "%+.0f%%", bg.offsetXPercent * 100f),
                onValueChange = { v -> onUpdateDraft { it.copy(background = it.background.copy(offsetXPercent = v)) } },
                onReset = { onUpdateDraft { it.copy(background = it.background.copy(offsetXPercent = 0f)) } }
            )
            LabeledSliderRow(
                label = "Vertical Reposition (Pan Y)",
                value = bg.offsetYPercent,
                valueRange = -0.5f..0.5f,
                valueLabel = String.format(Locale.US, "%+.0f%%", bg.offsetYPercent * 100f),
                onValueChange = { v -> onUpdateDraft { it.copy(background = it.background.copy(offsetYPercent = v)) } },
                onReset = { onUpdateDraft { it.copy(background = it.background.copy(offsetYPercent = 0f)) } }
            )
            LabeledSliderRow(
                label = "Image Brightness",
                value = bg.brightness,
                valueRange = -0.5f..0.5f,
                valueLabel = String.format(Locale.US, "%+.0f%%", bg.brightness * 100f),
                onValueChange = { v -> onUpdateDraft { it.copy(background = it.background.copy(brightness = v)) } },
                onReset = { onUpdateDraft { it.copy(background = it.background.copy(brightness = 0f)) } }
            )
            LabeledSliderRow(
                label = "Background Dimming Overlay",
                value = bg.dimmingOverlay,
                valueRange = 0.0f..0.90f,
                valueLabel = String.format(Locale.US, "%.0f%%", bg.dimmingOverlay * 100f),
                onValueChange = { v -> onUpdateDraft { it.copy(background = it.background.copy(dimmingOverlay = v)) } },
                onReset = { onUpdateDraft { it.copy(background = it.background.copy(dimmingOverlay = 0.35f)) } }
            )
            LabeledSliderRow(
                label = "Image Opacity",
                value = bg.imageOpacity,
                valueRange = 0.1f..1.0f,
                valueLabel = String.format(Locale.US, "%.0f%%", bg.imageOpacity * 100f),
                onValueChange = { v -> onUpdateDraft { it.copy(background = it.background.copy(imageOpacity = v)) } },
                onReset = { onUpdateDraft { it.copy(background = it.background.copy(imageOpacity = 1.0f)) } }
            )
            LabeledSliderRow(
                label = "Background Blur",
                value = bg.blurRadius.toFloat(),
                valueRange = 0f..20f,
                valueLabel = "${bg.blurRadius} px",
                onValueChange = { v -> onUpdateDraft { it.copy(background = it.background.copy(blurRadius = v.toInt())) } },
                onReset = { onUpdateDraft { it.copy(background = it.background.copy(blurRadius = 0)) } }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Diagonal Background Gradient", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = bg.useLinearGradient,
                onCheckedChange = { checked ->
                    onUpdateDraft { it.copy(background = it.background.copy(useLinearGradient = checked)) }
                }
            )
        }

        InteractiveColorPickerSection(
            title = "Primary Backdrop Color",
            currentHex = bg.baseColorHex,
            onColorSelected = { hex ->
                onUpdateDraft { it.copy(background = it.background.copy(baseColorHex = hex)) }
            }
        )

        if (bg.useLinearGradient) {
            InteractiveColorPickerSection(
                title = "Secondary Gradient Color",
                currentHex = bg.secondaryGradientColorHex,
                onColorSelected = { hex ->
                    onUpdateDraft { it.copy(background = it.background.copy(secondaryGradientColorHex = hex)) }
                }
            )
        }
    }
}

@Composable
private fun IndividualAndCategoryKeyEditorSection(
    draftTheme: KeyboardTheme,
    selectedCategory: KeyCategory,
    selectedIndividualKeyId: String?,
    currentKeyStyle: KeyStyleConfig,
    onSelectCategory: (KeyCategory) -> Unit,
    onSelectIndividualKey: (String?) -> Unit,
    onUpdateKeyStyle: ((KeyStyleConfig) -> KeyStyleConfig) -> Unit,
    onApplyStyleToAllCategories: () -> Unit,
    onResetKeyOrCategory: () -> Unit
) {
    GlassCard {
        SectionHeader(
            title = "Key Group & Individual Key Customization",
            subtitle = "Style key categories or override individual keys (tap 'Pick Key' on preview)"
        )

        // Category Selector Chips
        Text("1. Select Key Group Category:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedIndividualKeyId == null && selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category.name.replace("_", " ")) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Individual Key Override Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedIndividualKeyId != null) {
                    "Editing Individual Key Override: [$selectedIndividualKeyId]"
                } else {
                    "2. Or Select Individual Key Override:"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (selectedIndividualKeyId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (selectedIndividualKeyId != null) {
                TextButton(onClick = { onSelectIndividualKey(null) }) {
                    Text("Back to Group")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val popularKeys = listOf("Q", "W", "E", "A", "S", "D", "SHIFT", "SPACE", "ENTER", "BACKSPACE", "LANG", "EMOJI")
            popularKeys.forEach { keyId ->
                val hasOverride = draftTheme.individualKeyOverrides.containsKey(keyId)
                FilterChip(
                    selected = selectedIndividualKeyId == keyId,
                    onClick = { onSelectIndividualKey(keyId) },
                    label = { Text(if (hasOverride) "$keyId ★" else keyId) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Reset / Apply All buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onResetKeyOrCategory,
                modifier = Modifier.weight(1f).testTag("reset_key_style_button")
            ) {
                Text(if (selectedIndividualKeyId != null) "Clear Key Override" else "Reset Category")
            }
            OutlinedButton(
                onClick = onApplyStyleToAllCategories,
                modifier = Modifier.weight(1f).testTag("apply_style_all_keys_button")
            ) {
                Text("Apply to All Keys")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Shape, Border, Opacity, Shadow, Glow Sliders
        LabeledSliderRow(
            label = "Corner Radius",
            value = currentKeyStyle.cornerRadiusDp,
            valueRange = 0f..24f,
            valueLabel = String.format(Locale.US, "%.1f dp", currentKeyStyle.cornerRadiusDp),
            onValueChange = { v -> onUpdateKeyStyle { it.copy(cornerRadiusDp = v) } },
            onReset = { onUpdateKeyStyle { it.copy(cornerRadiusDp = 8f) } },
            testTagName = "slider_corner_radius"
        )

        LabeledSliderRow(
            label = "Border Width",
            value = currentKeyStyle.borderWidthDp,
            valueRange = 0f..4f,
            valueLabel = String.format(Locale.US, "%.1f dp", currentKeyStyle.borderWidthDp),
            onValueChange = { v -> onUpdateKeyStyle { it.copy(borderWidthDp = v) } },
            onReset = { onUpdateKeyStyle { it.copy(borderWidthDp = 1f) } }
        )

        LabeledSliderRow(
            label = "Key Opacity",
            value = currentKeyStyle.keyOpacity,
            valueRange = 0.20f..1.0f,
            valueLabel = String.format(Locale.US, "%.0f%%", currentKeyStyle.keyOpacity * 100f),
            onValueChange = { v -> onUpdateKeyStyle { it.copy(keyOpacity = v) } },
            onReset = { onUpdateKeyStyle { it.copy(keyOpacity = 0.94f) } }
        )

        LabeledSliderRow(
            label = "Key Shadow Elevation",
            value = currentKeyStyle.shadowRadiusDp,
            valueRange = 0f..12f,
            valueLabel = String.format(Locale.US, "%.1f dp", currentKeyStyle.shadowRadiusDp),
            onValueChange = { v -> onUpdateKeyStyle { it.copy(shadowRadiusDp = v) } },
            onReset = { onUpdateKeyStyle { it.copy(shadowRadiusDp = 3f) } }
        )

        // Optional Key Glow Toggle + Radius
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Key Border Glow", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = currentKeyStyle.enableGlow,
                onCheckedChange = { checked -> onUpdateKeyStyle { it.copy(enableGlow = checked) } }
            )
        }
        if (currentKeyStyle.enableGlow) {
            LabeledSliderRow(
                label = "Glow Intensity Radius",
                value = currentKeyStyle.glowRadiusDp,
                valueRange = 1f..16f,
                valueLabel = String.format(Locale.US, "%.1f dp", currentKeyStyle.glowRadiusDp),
                onValueChange = { v -> onUpdateKeyStyle { it.copy(glowRadiusDp = v) } }
            )
            InteractiveColorPickerSection(
                title = "Key Glow Color",
                currentHex = currentKeyStyle.glowColorHex,
                onColorSelected = { hex -> onUpdateKeyStyle { it.copy(glowColorHex = hex) } }
            )
        }

        // Color Pickers for Key Background, Label (with WCAG Contrast check!), Border, Pressed State, Gradient
        InteractiveColorPickerSection(
            title = "Key Background Color",
            currentHex = currentKeyStyle.backgroundColorHex,
            onColorSelected = { hex -> onUpdateKeyStyle { it.copy(backgroundColorHex = hex) } }
        )

        InteractiveColorPickerSection(
            title = "Key Label Color",
            currentHex = currentKeyStyle.labelColorHex,
            contrastAgainstHex = currentKeyStyle.backgroundColorHex,
            onColorSelected = { hex -> onUpdateKeyStyle { it.copy(labelColorHex = hex) } }
        )

        InteractiveColorPickerSection(
            title = "Key Border Color",
            currentHex = currentKeyStyle.borderColorHex,
            onColorSelected = { hex -> onUpdateKeyStyle { it.copy(borderColorHex = hex) } }
        )

        InteractiveColorPickerSection(
            title = "Pressed-State Highlight Color",
            currentHex = currentKeyStyle.pressedBackgroundColorHex,
            onColorSelected = { hex -> onUpdateKeyStyle { it.copy(pressedBackgroundColorHex = hex) } }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Vertical Key Fill Gradient", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = currentKeyStyle.enableGradient,
                onCheckedChange = { checked ->
                    onUpdateKeyStyle {
                        it.copy(
                            enableGradient = checked,
                            gradientEndColorHex = it.gradientEndColorHex ?: "#0A1410"
                        )
                    }
                }
            )
        }
        if (currentKeyStyle.enableGradient) {
            InteractiveColorPickerSection(
                title = "Key Gradient Bottom Color",
                currentHex = currentKeyStyle.gradientEndColorHex ?: "#0A1410",
                onColorSelected = { hex -> onUpdateKeyStyle { it.copy(gradientEndColorHex = hex) } }
            )
        }
    }
}

@Composable
private fun EffectsAndFeedbackEditorSection(
    draftTheme: KeyboardTheme,
    onUpdateDraft: ((KeyboardTheme) -> KeyboardTheme) -> Unit
) {
    val eff = draftTheme.effects
    GlassCard {
        SectionHeader(
            title = "Effects, Animations & Tactile Feedback",
            subtitle = "Every visual and audio effect has an explicit toggle for smooth performance"
        )

        EffectToggleRow(
            title = "Key Press Color Highlight",
            subtitle = "Illuminates key surface with pressed accent color on touch",
            checked = eff.enableKeyPressHighlight,
            onCheckedChange = { c -> onUpdateDraft { it.copy(effects = it.effects.copy(enableKeyPressHighlight = c)) } }
        )
        EffectToggleRow(
            title = "Key Press Scale Pop",
            subtitle = "Subtle tactile expansion on key down",
            checked = eff.enableKeyPressScalePop,
            onCheckedChange = { c -> onUpdateDraft { it.copy(effects = it.effects.copy(enableKeyPressScalePop = c)) } }
        )
        EffectToggleRow(
            title = "Key Shadows",
            subtitle = "Renders soft depth shadows beneath keys",
            checked = eff.enableKeyShadows,
            onCheckedChange = { c -> onUpdateDraft { it.copy(effects = it.effects.copy(enableKeyShadows = c)) } }
        )
        EffectToggleRow(
            title = "Theme Neon Glow",
            subtitle = "Enables ambient border glow on keys configured with glow",
            checked = eff.enableKeyGlow,
            onCheckedChange = { c -> onUpdateDraft { it.copy(effects = it.effects.copy(enableKeyGlow = c)) } }
        )
        EffectToggleRow(
            title = "Reduced-Animation Performance Mode",
            subtitle = "Disables scale pop, shadows, and glow for maximum speed on low-resource devices",
            checked = eff.reducedAnimationMode,
            onCheckedChange = { c -> onUpdateDraft { it.copy(effects = it.effects.copy(reducedAnimationMode = c)) } }
        )
        EffectToggleRow(
            title = "Keypress Sound Feedback",
            subtitle = "Plays crisp system key-click audio on press",
            checked = eff.enableSoundFeedback,
            onCheckedChange = { c -> onUpdateDraft { it.copy(effects = it.effects.copy(enableSoundFeedback = c)) } }
        )
        if (eff.enableSoundFeedback) {
            LabeledSliderRow(
                label = "Sound Volume",
                value = eff.soundVolume,
                valueRange = 0.05f..1.0f,
                valueLabel = String.format(Locale.US, "%.0f%%", eff.soundVolume * 100f),
                onValueChange = { v -> onUpdateDraft { it.copy(effects = it.effects.copy(soundVolume = v)) } }
            )
        }
        EffectToggleRow(
            title = "Haptic Vibration Feedback",
            subtitle = "Tactile vibration pulse on key press",
            checked = eff.enableVibrationFeedback,
            onCheckedChange = { c -> onUpdateDraft { it.copy(effects = it.effects.copy(enableVibrationFeedback = c)) } }
        )
        if (eff.enableVibrationFeedback) {
            LabeledSliderRow(
                label = "Theme Vibration Pulse Duration",
                value = eff.vibrationDurationMs.toFloat(),
                valueRange = 5f..80f,
                valueLabel = "${eff.vibrationDurationMs} ms",
                onValueChange = { v -> onUpdateDraft { it.copy(effects = it.effects.copy(vibrationDurationMs = v.toInt())) } }
            )
        }
    }
}

@Composable
private fun EffectToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun TextPromptDialog(
    title: String,
    initialValue: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(48) },
                label = { Text("Theme Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("dialog_theme_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
