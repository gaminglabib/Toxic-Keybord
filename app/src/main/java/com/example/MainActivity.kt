package com.example

import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.theme.editor.ThemeStudioScreen
import com.example.theme.model.KeyboardTheme
import com.example.ui.StudioDestination
import com.example.ui.StudioViewModel
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LayoutStudioScreen
import com.example.ui.screens.MyThemesScreen
import com.example.ui.screens.SettingsAndPrivacyScreen
import com.example.ui.screens.TextStyleStudioScreen
import com.example.ui.theme.ToxicCrimsonAccent
import com.example.ui.theme.ToxicEmeraldPrimary
import com.example.ui.theme.ToxicKeyboardStudioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    private val imeSettingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            viewModel.refreshImeStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
                false,
                imeSettingsObserver
            )
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS),
                false,
                imeSettingsObserver
            )
        } catch (_: Exception) {
        }

        setContent {
            val useLightMode by viewModel.studioLightMode.collectAsStateWithLifecycle()
            ToxicKeyboardStudioTheme(useLightMode = useLightMode) {
                ToxicKeyboardStudioApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically refresh actual system keyboard status when returning from Android Settings
        viewModel.refreshImeStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Refreshes immediately when Android's InputMethodPicker dialog overlay closes
            viewModel.refreshImeStatus()
        }
    }

    override fun onDestroy() {
        try {
            contentResolver.unregisterContentObserver(imeSettingsObserver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}

private data class NavTabSpec(
    val destination: StudioDestination,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val STUDIO_NAV_TABS = listOf(
    NavTabSpec(StudioDestination.DASHBOARD, "Home", Icons.Default.Dashboard, "nav_tab_dashboard"),
    NavTabSpec(StudioDestination.THEME_STUDIO, "Theme", Icons.Default.ColorLens, "nav_tab_theme_studio"),
    NavTabSpec(StudioDestination.TEXT_STYLE_STUDIO, "Text", Icons.Default.FontDownload, "nav_tab_text_studio"),
    NavTabSpec(StudioDestination.LAYOUT_STUDIO, "Layout", Icons.Default.GridView, "nav_tab_layout_studio"),
    NavTabSpec(StudioDestination.MY_THEMES, "Themes", Icons.Default.Style, "nav_tab_my_themes"),
    NavTabSpec(StudioDestination.SETTINGS_AND_PRIVACY, "Settings", Icons.Default.Settings, "nav_tab_settings")
)

@Composable
fun ToxicKeyboardStudioApp(viewModel: StudioViewModel) {
    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
    val savedThemes by viewModel.savedThemes.collectAsStateWithLifecycle()
    val activeTheme by viewModel.activeTheme.collectAsStateWithLifecycle()
    val globalSettings by viewModel.globalSettings.collectAsStateWithLifecycle()
    val draftTheme by viewModel.draftTheme.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedKeyCategory.collectAsStateWithLifecycle()
    val selectedIndividualKeyId by viewModel.selectedIndividualKeyId.collectAsStateWithLifecycle()
    val importedFonts by viewModel.importedFonts.collectAsStateWithLifecycle()
    val clipboardItems by viewModel.clipboardItems.collectAsStateWithLifecycle()
    val dictionaryWords by viewModel.dictionaryWords.collectAsStateWithLifecycle()
    val pendingImportPreview by viewModel.pendingImportPreview.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val sandboxText by viewModel.sandboxText.collectAsStateWithLifecycle()
    val studioLightMode by viewModel.studioLightMode.collectAsStateWithLifecycle()

    // Pending export state for SAF CreateDocument launcher
    var pendingExportTheme by remember { mutableStateOf<KeyboardTheme?>(null) }
    var pendingExportIncludeBg by remember { mutableStateOf(true) }
    var pendingExportIncludeFont by remember { mutableStateOf(false) }

    val importThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.stageImportFromUri(uri)
            viewModel.navigateTo(StudioDestination.MY_THEMES)
        }
    }

    val exportThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val target = pendingExportTheme
        if (uri != null && target != null) {
            viewModel.exportThemeToUri(
                theme = target,
                targetUri = uri,
                includeBackgroundImage = pendingExportIncludeBg,
                includeCustomFontWithLicense = pendingExportIncludeFont
            )
        }
        pendingExportTheme = null
    }

    // Observe lifecycle ON_RESUME to refresh keyboard status when returning from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshImeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // BackHandler on all secondary destinations to return cleanly to Dashboard
    BackHandler(enabled = destination != StudioDestination.DASHBOARD) {
        viewModel.navigateTo(StudioDestination.DASHBOARD)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        STUDIO_NAV_TABS.forEach { tab ->
                            NavigationBarItem(
                                selected = destination == tab.destination,
                                onClick = { viewModel.navigateTo(tab.destination) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        STUDIO_NAV_TABS.forEach { tab ->
                            NavigationRailItem(
                                selected = destination == tab.destination,
                                onClick = { viewModel.navigateTo(tab.destination) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Status / Error Toast Banner
                    AnimatedVisibility(visible = bannerMessage != null) {
                        bannerMessage?.let { msg ->
                            val accent = if (msg.isError) ToxicCrimsonAccent else ToxicEmeraldPrimary
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accent.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("status_feedback_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.dismissBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss notification",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (destination) {
                            StudioDestination.DASHBOARD -> HomeDashboardScreen(
                                imeStatus = imeStatus,
                                activeTheme = activeTheme,
                                savedThemes = savedThemes,
                                globalSettings = globalSettings,
                                sandboxText = sandboxText,
                                onSandboxTextChange = viewModel::updateSandboxText,
                                onRefreshImeStatus = viewModel::refreshImeStatus,
                                onNavigate = viewModel::navigateTo,
                                onEditTheme = viewModel::startEditingTheme,
                                onApplyTheme = viewModel::applyExistingTheme,
                                onCreateNewTheme = viewModel::createNewCustomTheme,
                                onGenerateFromPrompt = viewModel::generateThemeFromDescription,
                                onRequestImportTheme = {
                                    importThemeLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onRequestExportTheme = { theme ->
                                    pendingExportTheme = theme
                                    pendingExportIncludeBg = true
                                    pendingExportIncludeFont = false
                                    val safeFileName = theme.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
                                    exportThemeLauncher.launch("${safeFileName}.tks.json")
                                }
                            )

                            StudioDestination.THEME_STUDIO -> ThemeStudioScreen(
                                draftTheme = draftTheme,
                                globalSettings = globalSettings,
                                hasUnsavedChanges = hasUnsavedChanges,
                                canUndo = canUndo,
                                canRedo = canRedo,
                                selectedCategory = selectedCategory,
                                selectedIndividualKeyId = selectedIndividualKeyId,
                                currentKeyStyle = viewModel.currentEditedKeyStyle(),
                                sandboxText = sandboxText,
                                onSandboxTextChange = viewModel::updateSandboxText,
                                onSelectCategory = viewModel::selectKeyCategory,
                                onSelectIndividualKey = viewModel::selectIndividualKeyForOverride,
                                onUpdateDraft = viewModel::updateDraft,
                                onUpdateKeyStyle = viewModel::updateActiveKeyStyle,
                                onApplyStyleToAllCategories = viewModel::applyStyleToAllCategories,
                                onResetKeyOrCategory = viewModel::resetCurrentKeyOrCategoryToDefault,
                                onResetEntireTheme = viewModel::resetEntireDraftTheme,
                                onUndo = viewModel::undoDraft,
                                onRedo = viewModel::redoDraft,
                                onCancelUnsaved = viewModel::cancelUnsavedChanges,
                                onRestorePreviousSaved = viewModel::restorePreviousSavedVersion,
                                onSaveAndApply = { viewModel.saveCurrentDraft(applyToKeyboard = true) },
                                onSaveAsNew = { newName -> viewModel.saveDraftAsNewTheme(newName, applyToKeyboard = true) },
                                onRenameTheme = { newName -> viewModel.renameTheme(draftTheme, newName) },
                                onDuplicateTheme = { viewModel.duplicateExistingTheme(draftTheme) },
                                onImportBackgroundUri = viewModel::importBackgroundFromUri
                            )

                            StudioDestination.TEXT_STYLE_STUDIO -> TextStyleStudioScreen(
                                draftTheme = draftTheme,
                                globalSettings = globalSettings,
                                importedFonts = importedFonts,
                                sandboxText = sandboxText,
                                onSandboxTextChange = viewModel::updateSandboxText,
                                onUpdateDraft = viewModel::updateDraft,
                                onUpdateKeyStyle = viewModel::updateActiveKeyStyle,
                                onImportCustomFont = viewModel::importCustomFontFromUri,
                                onRemoveCustomFont = viewModel::removeCustomFont,
                                onSaveAndApply = { viewModel.saveCurrentDraft(applyToKeyboard = true) }
                            )

                            StudioDestination.LAYOUT_STUDIO -> LayoutStudioScreen(
                                draftTheme = draftTheme,
                                globalSettings = globalSettings,
                                sandboxText = sandboxText,
                                onSandboxTextChange = viewModel::updateSandboxText,
                                onUpdateDraft = viewModel::updateDraft,
                                onSaveAndApply = { viewModel.saveCurrentDraft(applyToKeyboard = true) }
                            )

                            StudioDestination.MY_THEMES -> MyThemesScreen(
                                savedThemes = savedThemes,
                                activeTheme = activeTheme,
                                globalSettings = globalSettings,
                                pendingImportPreview = pendingImportPreview,
                                onApplyTheme = viewModel::applyExistingTheme,
                                onEditTheme = viewModel::startEditingTheme,
                                onDuplicateTheme = viewModel::duplicateExistingTheme,
                                onRenameTheme = viewModel::renameTheme,
                                onDeleteTheme = viewModel::deleteCustomTheme,
                                onCreateNewTheme = viewModel::createNewCustomTheme,
                                onRequestImportFile = {
                                    importThemeLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onRequestExportFile = { theme, includeBg, includeFont ->
                                    pendingExportTheme = theme
                                    pendingExportIncludeBg = includeBg
                                    pendingExportIncludeFont = includeFont
                                    val safeFileName = theme.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
                                    exportThemeLauncher.launch("${safeFileName}.tks.json")
                                },
                                onConfirmPendingImport = viewModel::confirmAndApplyPendingImport,
                                onCancelPendingImport = viewModel::cancelPendingImport
                            )

                            StudioDestination.SETTINGS_AND_PRIVACY -> SettingsAndPrivacyScreen(
                                globalSettings = globalSettings,
                                studioLightMode = studioLightMode,
                                dictionaryWords = dictionaryWords,
                                clipboardItems = clipboardItems,
                                onToggleStudioLightMode = viewModel::toggleStudioLightMode,
                                onUpdateGlobalSettings = viewModel::updateGlobalSettings,
                                onAddDictionaryWord = viewModel::addPersonalWord,
                                onRemoveDictionaryWord = viewModel::removePersonalWord,
                                onClearDictionary = viewModel::clearPersonalDictionary,
                                onDeleteClipboardItem = viewModel::deleteSingleClipboardItem,
                                onClearClipboard = viewModel::clearClipboardHistory,
                                onResetAllSettings = viewModel::resetAllSettingsAndRestoreDefaults
                            )
                        }
                    }
                }
            }
        }
    }
}
