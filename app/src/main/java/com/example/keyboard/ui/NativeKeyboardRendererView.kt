package com.example.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.fonts.FontManager
import com.example.keyboard.emoji.EmojiRepository
import com.example.keyboard.layout.KeyActionType
import com.example.keyboard.layout.KeySpec
import com.example.keyboard.layout.KeyboardLayouts
import com.example.keyboard.layout.KeyboardRowSpec
import com.example.keyboard.state.KeyboardEngineState
import com.example.keyboard.state.KeyboardLanguage
import com.example.keyboard.state.KeyboardMode
import com.example.keyboard.state.ShiftState
import com.example.keyboard.voice.VoiceInputState
import com.example.theme.model.BackgroundSourceType
import com.example.theme.model.KeyCategory
import com.example.theme.model.KeyLabelAlignment
import com.example.theme.model.KeyStyleConfig
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.util.ColorAndImageUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Modern, high-performance native Canvas keyboard view shared between:
 * 1) The real system InputMethodService (ToxicInputMethodService)
 * 2) The Theme Studio / Text Studio / Layout Studio Live Preview
 *
 * Implements a modern keyboard design inspired by contemporary Android keyboards:
 * - Tactile key depth & modern corner radiuses
 * - Modern suggestion bar with accented primary candidate
 * - Quick toolbar with Microphone (Voice Typing), Language, Clipboard, Emoji, and Settings
 * - Categorized Emoji picker with category tabs and Recents
 * - Rich Clipboard panel with paste/insert actions
 * - Floating keypress magnifying popup bubble
 */
class NativeKeyboardRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onKeyTapped(key: KeySpec, isLongPress: Boolean)
        fun onSuggestionClicked(word: String)
        fun onEmojiSelected(emoji: String)
        fun onClipboardItemSelected(text: String)
        fun onToolbarModeToggle(mode: KeyboardMode)
        fun onToolbarLanguageToggle()
        fun onVoiceTypingRequested() {}
        fun onVoiceCancelRequested() {}
        fun onEmojiCategorySelected(index: Int) {}
        fun onClipboardClearRequested() {}
        fun onOpenStudioAppRequested()
        fun onKeySelectedInEditor(key: KeySpec) {}
    }

    var listener: Listener? = null
    var isEditorInspectMode: Boolean = false
    var inspectedKeyId: String? = null
        set(value) {
            field = value
            invalidate()
        }

    var previewLandscapeOverride: Boolean? = null
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    private var theme: KeyboardTheme = KeyboardTheme(id = "default", name = "Default")
    private var globalSettings: GlobalKeyboardSettings = GlobalKeyboardSettings()
    private var engineState: KeyboardEngineState = KeyboardEngineState()
    private var clipboardItems: List<String> = emptyList()

    private var rows: List<KeyboardRowSpec> = emptyList()
    private val hitRects = mutableListOf<KeyHitTarget>()
    private val toolbarRects = mutableListOf<ToolbarHitTarget>()
    private val panelCellRects = mutableListOf<PanelCellTarget>()
    private val emojiTabRects = mutableListOf<EmojiTabHitTarget>()
    private var overlayPanelBounds: RectF? = null

    private var activePressedKey: KeyHitTarget? = null
    private var activeToolbarTarget: ToolbarHitTarget? = null
    private var activePanelTarget: PanelCellTarget? = null
    private var activeEmojiTabTarget: EmojiTabHitTarget? = null
    private var longPressTriggered: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cachedBgBitmap: Bitmap? = null
    private var cachedBgSignature: String = ""

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val density: Float
        get() = resources.displayMetrics.density.coerceAtLeast(1f)
    private val scaledDensity: Float
        get() = resources.displayMetrics.scaledDensity.coerceAtLeast(1f)

    private data class KeyHitTarget(
        val spec: KeySpec,
        val rect: RectF
    )

    private data class ToolbarHitTarget(
        val id: String,
        val label: String,
        val rect: RectF,
        val isSuggestion: Boolean = false
    )

    private data class PanelCellTarget(
        val value: String,
        val rect: RectF,
        val isClipboard: Boolean
    )

    private data class EmojiTabHitTarget(
        val index: Int,
        val icon: String,
        val rect: RectF
    )

    private val longPressRunnable = object : Runnable {
        override fun run() {
            val current = activePressedKey ?: return
            longPressTriggered = true
            performFeedback()
            if (current.spec.actionType == KeyActionType.BACKSPACE) {
                listener?.onKeyTapped(current.spec, isLongPress = false)
                mainHandler.postDelayed(this, 65L)
            } else {
                listener?.onKeyTapped(current.spec, isLongPress = true)
                invalidate()
            }
        }
    }

    init {
        visibility = VISIBLE
        isClickable = true
        isFocusable = false
        rebuildRows()
    }

    fun updateConfiguration(
        newTheme: KeyboardTheme,
        newSettings: GlobalKeyboardSettings,
        newState: KeyboardEngineState,
        newClipboard: List<String> = clipboardItems
    ) {
        val layoutChanged = this.theme.layout != newTheme.layout ||
            this.engineState.mode != newState.mode ||
            this.engineState.language != newState.language ||
            this.engineState.isShifted != newState.isShifted ||
            this.rows.isEmpty()

        this.theme = newTheme
        this.globalSettings = newSettings
        this.engineState = newState
        this.clipboardItems = newClipboard

        if (visibility != VISIBLE) {
            visibility = VISIBLE
        }

        if (layoutChanged) {
            rebuildRows()
            requestLayout()
        }
        invalidate()
    }

    private fun isLandscapeActive(): Boolean {
        previewLandscapeOverride?.let { return it }
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun rebuildRows() {
        rows = KeyboardLayouts.buildRows(engineState, theme.layout)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val screenW = resources.displayMetrics.widthPixels
        val defaultWidth = if (screenW > 0) screenW else (360f * density).roundToInt()

        val measuredW = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(defaultWidth, widthSize)
            else -> defaultWidth
        }

        val isLand = isLandscapeActive()
        val baseKeyH = (if (isLand) theme.layout.landscapeKeyHeightDp else theme.layout.keyHeightDp) * density
        val rowSpacing = theme.layout.verticalSpacingDp * density
        val toolbarHeight = if (theme.layout.showTopToolbar) 42f * density else 0f
        val bottomPadding = (theme.layout.keyboardBottomPaddingDp.coerceAtLeast(6f)) * density
        val topPadding = 6f * density

        val rowCount = if (rows.isNotEmpty()) rows.size else 4
        val totalKeysHeight = (rowCount * baseKeyH) + ((rowCount - 1).coerceAtLeast(0) * rowSpacing)
        val calculatedHeight = (topPadding + toolbarHeight + totalKeysHeight + bottomPadding).roundToInt()

        val measuredH = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(calculatedHeight, heightSize)
            else -> calculatedHeight
        }

        setMeasuredDimension(measuredW, measuredH)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cachedBgSignature = ""
    }

    private fun ensureBackgroundBitmap(w: Int, h: Int) {
        val bg = theme.background
        val sig = "${w}x${h}_${bg.sourceType}_${bg.customImagePath}_${bg.presetDrawableName}_${bg.scaleZoom}_${bg.rotationDegrees}_${bg.offsetXPercent}_${bg.offsetYPercent}_${bg.brightness}_${bg.blurRadius}"
        if (sig == cachedBgSignature) return
        cachedBgSignature = sig
        cachedBgBitmap?.recycle()
        cachedBgBitmap = null

        if (bg.sourceType == BackgroundSourceType.CUSTOM_IMAGE || bg.sourceType == BackgroundSourceType.PRESET_DRAWABLE) {
            cachedBgBitmap = ColorAndImageUtils.loadTransformedBackgroundBitmap(
                context = context,
                customPath = if (bg.sourceType == BackgroundSourceType.CUSTOM_IMAGE) bg.customImagePath else null,
                presetDrawableName = if (bg.sourceType == BackgroundSourceType.PRESET_DRAWABLE) bg.presetDrawableName else null,
                targetWidth = w,
                targetHeight = h,
                zoom = bg.scaleZoom,
                rotationDegrees = bg.rotationDegrees,
                offsetXPercent = bg.offsetXPercent,
                offsetYPercent = bg.offsetYPercent,
                brightness = bg.brightness,
                blurRadius = bg.blurRadius
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (rows.isEmpty()) rebuildRows()

        // 1. Draw Background
        drawKeyboardBackground(canvas, w, h)

        var currentTop = 6f * density

        // 2. Draw Top Toolbar & Suggestion Strip
        toolbarRects.clear()
        if (theme.layout.showTopToolbar) {
            val toolbarHeight = 40f * density
            drawTopToolbar(canvas, w, currentTop, toolbarHeight)
            currentTop += toolbarHeight + (6f * density)
        }

        // 3. Draw Overlay Panel (Emoji / Clipboard) OR Standard Key Rows
        hitRects.clear()
        panelCellRects.clear()
        emojiTabRects.clear()
        overlayPanelBounds = null

        val isLand = isLandscapeActive()
        val baseKeyH = (if (isLand) theme.layout.landscapeKeyHeightDp else theme.layout.keyHeightDp) * density
        val vSpace = theme.layout.verticalSpacingDp * density

        if (engineState.mode == KeyboardMode.EMOJI_PANEL || engineState.mode == KeyboardMode.CLIPBOARD_PANEL) {
            val bottomPad = (theme.layout.keyboardBottomPaddingDp.coerceAtLeast(6f)) * density
            val panelHeight = (h - currentTop - baseKeyH - bottomPad - (6f * density)).coerceAtLeast(110f * density)
            drawOverlayPanel(canvas, w, currentTop, panelHeight)
            currentTop += panelHeight + (6f * density)
            rows.lastOrNull()?.let { bottomRow ->
                drawSingleKeyRow(canvas, bottomRow, w, currentTop, baseKeyH)
            }
        } else {
            for (row in rows) {
                val rowH = if (row.isNumberRow) baseKeyH * 0.86f else baseKeyH
                drawSingleKeyRow(canvas, row, w, currentTop, rowH)
                currentTop += rowH + vSpace
            }
        }

        // 4. Draw Floating Key Popup Magnifier Bubble if pressed
        activePressedKey?.let { pressed ->
            if (!isEditorInspectMode && pressed.spec.actionType == KeyActionType.CHARACTER) {
                drawKeyPopupPreview(canvas, pressed)
            }
        }
    }

    private fun drawKeyboardBackground(canvas: Canvas, w: Float, h: Float) {
        val bg = theme.background
        val baseColor = if (globalSettings.highContrastOverride) Color.BLACK else ColorAndImageUtils.parseColorSafe(bg.baseColorHex, Color.parseColor("#060B09"))
        val secColor = if (globalSettings.highContrastOverride) Color.BLACK else ColorAndImageUtils.parseColorSafe(bg.secondaryGradientColorHex, baseColor)

        bgPaint.shader = if (bg.useLinearGradient && !globalSettings.highContrastOverride) {
            LinearGradient(0f, 0f, w, h, baseColor, secColor, Shader.TileMode.CLAMP)
        } else {
            null
        }
        bgPaint.color = baseColor
        bgPaint.alpha = 255
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        if (!globalSettings.highContrastOverride) {
            ensureBackgroundBitmap(w.toInt(), h.toInt())
            cachedBgBitmap?.let { bmp ->
                bgPaint.alpha = (bg.imageOpacity.coerceIn(0.1f, 1.0f) * 255).toInt()
                val dst = RectF(0f, 0f, w, h)
                canvas.drawBitmap(bmp, null, dst, bgPaint)
            }
            if (bg.dimmingOverlay > 0.01f) {
                bgPaint.color = Color.BLACK
                bgPaint.alpha = (bg.dimmingOverlay.coerceIn(0f, 0.90f) * 255).toInt()
                canvas.drawRect(0f, 0f, w, h, bgPaint)
            }
        }
    }

    private fun drawTopToolbar(canvas: Canvas, totalWidth: Float, top: Float, height: Float) {
        val padH = 6f * density
        val btnStyle = theme.styleForKey("TOOLBAR", KeyCategory.TOOLBAR_BUTTON)
        val btnCorner = 12f * density
        val btnBgColor = if (globalSettings.highContrastOverride) Color.parseColor("#121212") else ColorAndImageUtils.parseColorSafe(btnStyle.backgroundColorHex, Color.parseColor("#121F17"))
        val pressedBgColor = ColorAndImageUtils.parseColorSafe(btnStyle.pressedBackgroundColorHex, Color.parseColor("#00E676"))
        val labelColor = if (globalSettings.highContrastOverride) Color.WHITE else ColorAndImageUtils.parseColorSafe(btnStyle.labelColorHex, Color.WHITE)
        val borderColor = if (globalSettings.highContrastOverride) Color.parseColor("#00E676") else ColorAndImageUtils.parseColorSafe(btnStyle.borderColorHex, Color.parseColor("#1F3E2E"))

        // If Voice Input is actively listening or processing, display modern Voice Banner across the toolbar!
        if (engineState.voiceState == VoiceInputState.LISTENING || engineState.voiceState == VoiceInputState.PROCESSING) {
            val bannerRect = RectF(padH, top, totalWidth - padH, top + height)
            keyFillPaint.color = Color.parseColor("#0F291D")
            keyFillPaint.alpha = 245
            canvas.drawRoundRect(bannerRect, btnCorner, btnCorner, keyFillPaint)

            keyBorderPaint.color = Color.parseColor("#00E676")
            keyBorderPaint.strokeWidth = 1.5f * density
            canvas.drawRoundRect(bannerRect, btnCorner, btnCorner, keyBorderPaint)

            // Cancel button on right
            val cancelW = 44f * density
            val cancelRect = RectF(bannerRect.right - cancelW, top, bannerRect.right, top + height)
            textPaint.reset()
            textPaint.isAntiAlias = true
            textPaint.color = Color.parseColor("#FF5252")
            textPaint.textSize = 14f * scaledDensity
            textPaint.textAlign = Paint.Align.CENTER
            val cty = cancelRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("✕", cancelRect.centerX(), cty, textPaint)
            toolbarRects.add(ToolbarHitTarget("VOICE_CANCEL", "✕", cancelRect, isSuggestion = false))

            // Pulsing mic indicator + text
            textPaint.color = Color.parseColor("#A7F3D0")
            textPaint.textAlign = Paint.Align.LEFT
            val infoMsg = if (engineState.voiceState == VoiceInputState.LISTENING) {
                "🎙️ Listening... ${engineState.voiceMessage.ifEmpty { "Speak now" }}"
            } else {
                "⏳ Processing speech..."
            }
            drawFittedCenteredText(
                canvas = canvas,
                text = infoMsg,
                rect = RectF(bannerRect.left + 8f * density, top, bannerRect.right - cancelW - 4f * density, top + height),
                paint = textPaint,
                desiredSizePx = 13f * scaledDensity,
                minSizePx = 10f * scaledDensity,
                horizontalPadPx = 6f * density
            )
            return
        }

        // Left action buttons: Settings (⚙), Language (🌐/বাং), Mic (🎙️), Clipboard (📋), Emoji (😊)
        val langLabel = if (engineState.language == KeyboardLanguage.BENGALI) "বাং" else "EN"
        val actionItems = listOf(
            "STUDIO" to "⚙",
            "LANG" to langLabel,
            "VOICE" to "🎙️",
            "CLIP" to "📋",
            "EMOJI" to "😊"
        )
        val pillWidth = 34f * density
        val gap = 4f * density
        var curX = padH

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = FontManager.resolveTypeface(context, theme.textStyle, isBengaliScript = true)
        textPaint.textAlign = Paint.Align.CENTER

        for ((id, text) in actionItems) {
            val rect = RectF(curX, top, curX + pillWidth, top + height)
            val isActiveMode = (id == "CLIP" && engineState.mode == KeyboardMode.CLIPBOARD_PANEL) ||
                (id == "EMOJI" && engineState.mode == KeyboardMode.EMOJI_PANEL) ||
                (activeToolbarTarget?.id == id)

            keyFillPaint.color = if (isActiveMode) pressedBgColor else btnBgColor
            keyFillPaint.alpha = if (isActiveMode) 250 else 220
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyFillPaint)

            keyBorderPaint.color = if (isActiveMode) Color.WHITE else borderColor
            keyBorderPaint.strokeWidth = 1f * density
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyBorderPaint)

            textPaint.color = if (isActiveMode) Color.BLACK else labelColor
            drawFittedCenteredText(
                canvas = canvas,
                text = text,
                rect = rect,
                paint = textPaint,
                desiredSizePx = theme.textStyle.toolbarLabelSizeSp * scaledDensity,
                minSizePx = 10f * scaledDensity,
                horizontalPadPx = 2f * density
            )
            toolbarRects.add(ToolbarHitTarget(id, text, rect, isSuggestion = false))
            curX += pillWidth + gap
        }

        // Right side: Modern pill-shaped word suggestions
        val remainingW = (totalWidth - curX - padH).coerceAtLeast(60f * density)
        if (engineState.editorContext.isPasswordOrSensitive || !globalSettings.enableSuggestions) {
            val rect = RectF(curX, top, curX + remainingW, top + height)
            keyFillPaint.color = Color.parseColor("#101C15")
            keyFillPaint.alpha = 205
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyFillPaint)

            keyBorderPaint.color = borderColor
            keyBorderPaint.strokeWidth = 0.8f * density
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyBorderPaint)

            textPaint.color = Color.parseColor("#6EE7B7")
            val msg = if (engineState.editorContext.isPasswordOrSensitive) {
                "🔒 Secure Protected Field"
            } else {
                "TOXIC • ${engineState.language.displayName}"
            }
            drawFittedCenteredText(
                canvas = canvas,
                text = msg,
                rect = rect,
                paint = textPaint,
                desiredSizePx = 11.5f * scaledDensity,
                minSizePx = 9f * scaledDensity,
                horizontalPadPx = 6f * density
            )
        } else {
            val defaultWords = if (engineState.language == KeyboardLanguage.BENGALI) {
                listOf("বাংলা", "ধন্যবাদ", "ভালো")
            } else {
                listOf("the", "and", "you")
            }
            val suggestions = engineState.suggestions.take(3).ifEmpty { defaultWords }
            val chipW = (remainingW - (gap * (suggestions.size - 1))) / suggestions.size

            suggestions.forEachIndexed { index, word ->
                val id = "SUG_$word"
                val isPressedSug = activeToolbarTarget?.id == id
                val isPrimaryCandidate = (index == 0) // Primary candidate highlighted
                val rect = RectF(curX, top, curX + chipW, top + height)

                keyFillPaint.color = when {
                    isPressedSug -> pressedBgColor
                    isPrimaryCandidate -> Color.parseColor("#173626")
                    else -> btnBgColor
                }
                keyFillPaint.alpha = if (isPressedSug) 250 else 225
                canvas.drawRoundRect(rect, btnCorner, btnCorner, keyFillPaint)

                keyBorderPaint.color = if (isPrimaryCandidate) Color.parseColor("#00E676") else borderColor
                keyBorderPaint.strokeWidth = if (isPrimaryCandidate) 1.2f * density else 0.8f * density
                canvas.drawRoundRect(rect, btnCorner, btnCorner, keyBorderPaint)

                textPaint.color = when {
                    isPressedSug -> Color.BLACK
                    isPrimaryCandidate -> Color.parseColor("#ECFDF5")
                    else -> labelColor
                }
                textPaint.isFakeBoldText = isPrimaryCandidate
                drawFittedCenteredText(
                    canvas = canvas,
                    text = word,
                    rect = rect,
                    paint = textPaint,
                    desiredSizePx = (theme.textStyle.toolbarLabelSizeSp + (if (isPrimaryCandidate) 0.5f else 0f)) * scaledDensity,
                    minSizePx = 9.5f * scaledDensity,
                    horizontalPadPx = 4f * density
                )
                textPaint.isFakeBoldText = false
                toolbarRects.add(ToolbarHitTarget(id, word, rect, isSuggestion = true))
                curX += chipW + gap
            }
        }
    }

    private fun drawOverlayPanel(canvas: Canvas, totalWidth: Float, top: Float, height: Float) {
        val pad = 6f * density
        val panelRect = RectF(pad, top, totalWidth - pad, top + height)
        overlayPanelBounds = panelRect
        keyFillPaint.shader = null
        keyFillPaint.color = Color.parseColor("#F208130E")
        canvas.drawRoundRect(panelRect, 14f * density, 14f * density, keyFillPaint)

        keyBorderPaint.color = Color.parseColor("#1F4231")
        keyBorderPaint.strokeWidth = 1.2f * density
        canvas.drawRoundRect(panelRect, 14f * density, 14f * density, keyBorderPaint)

        if (engineState.mode == KeyboardMode.EMOJI_PANEL) {
            // Category Tabs Header Strip (Height: 32dp)
            val tabH = 32f * density
            val tabY = panelRect.top + 4f * density
            val categories = EmojiRepository.CATEGORIES
            val tabCount = categories.size
            val tabW = (panelRect.width() - (8f * density)) / tabCount

            textPaint.reset()
            textPaint.isAntiAlias = true
            textPaint.textSize = 14f * scaledDensity
            textPaint.textAlign = Paint.Align.CENTER

            categories.forEachIndexed { idx, cat ->
                val tabRect = RectF(
                    panelRect.left + 4f * density + (idx * tabW),
                    tabY,
                    panelRect.left + 4f * density + ((idx + 1) * tabW),
                    tabY + tabH
                )
                val isSelected = idx == engineState.selectedEmojiCategoryIndex
                if (isSelected) {
                    keyFillPaint.color = Color.parseColor("#2600E676")
                    canvas.drawRoundRect(tabRect, 8f * density, 8f * density, keyFillPaint)
                    keyBorderPaint.color = Color.parseColor("#00E676")
                    keyBorderPaint.strokeWidth = 1.2f * density
                    canvas.drawRoundRect(tabRect, 8f * density, 8f * density, keyBorderPaint)
                }
                val ty = tabRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(cat.icon, tabRect.centerX(), ty, textPaint)
                emojiTabRects.add(EmojiTabHitTarget(idx, cat.icon, tabRect))
            }

            // Grid of emojis for active category
            val activeCategory = categories.getOrNull(engineState.selectedEmojiCategoryIndex) ?: categories[0]
            val gridTop = tabY + tabH + 6f * density
            val gridHeight = panelRect.bottom - gridTop - 4f * density
            val cols = 8
            val rowsCount = 4
            val cellW = (panelRect.width() - 8f * density) / cols
            val cellH = gridHeight / rowsCount

            textPaint.textSize = (cellH * 0.58f).coerceIn(16f * scaledDensity, 24f * scaledDensity)

            activeCategory.emojis.take(cols * rowsCount).forEachIndexed { idx, emoji ->
                val r = idx / cols
                val c = idx % cols
                val cellRect = RectF(
                    panelRect.left + 4f * density + c * cellW,
                    gridTop + r * cellH,
                    panelRect.left + 4f * density + (c + 1) * cellW,
                    gridTop + (r + 1) * cellH
                )
                if (activePanelTarget?.value == emoji && activePanelTarget?.isClipboard == false) {
                    keyFillPaint.color = Color.parseColor("#4400E676")
                    canvas.drawRoundRect(cellRect, 8f * density, 8f * density, keyFillPaint)
                }
                val ty = cellRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(emoji, cellRect.centerX(), ty, textPaint)
                panelCellRects.add(PanelCellTarget(emoji, cellRect, isClipboard = false))
            }
        } else if (engineState.mode == KeyboardMode.CLIPBOARD_PANEL) {
            textPaint.reset()
            textPaint.isAntiAlias = true
            textPaint.typeface = FontManager.resolveTypeface(context, theme.textStyle, isBengaliScript = true)
            textPaint.textSize = 12.5f * scaledDensity

            // Header Row: Title & Clear Action
            val headerH = 26f * density
            val headerRect = RectF(panelRect.left + 8f * density, panelRect.top + 4f * density, panelRect.right - 8f * density, panelRect.top + 4f * density + headerH)
            textPaint.color = Color.parseColor("#A7F3D0")
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.isFakeBoldText = true
            val hty = headerRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("📋 Clipboard Snippets (Tap to Paste)", headerRect.left, hty, textPaint)
            textPaint.isFakeBoldText = false

            if (clipboardItems.isNotEmpty()) {
                val clearRect = RectF(headerRect.right - 64f * density, headerRect.top, headerRect.right, headerRect.bottom)
                keyFillPaint.color = Color.parseColor("#26FF1744")
                canvas.drawRoundRect(clearRect, 6f * density, 6f * density, keyFillPaint)
                textPaint.color = Color.parseColor("#FF8A80")
                textPaint.textAlign = Paint.Align.CENTER
                val cty = clearRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText("Clear All", clearRect.centerX(), cty, textPaint)
                toolbarRects.add(ToolbarHitTarget("CLIP_CLEAR", "Clear All", clearRect, isSuggestion = false))
            }

            if (!globalSettings.enableClipboardHistory) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = Color.parseColor("#A7F3D0")
                drawFittedCenteredText(
                    canvas = canvas,
                    text = "Clipboard History is disabled. Enable in Settings.",
                    rect = RectF(panelRect.left, panelRect.top + headerH, panelRect.right, panelRect.bottom),
                    paint = textPaint,
                    desiredSizePx = 12.5f * scaledDensity,
                    minSizePx = 10f * scaledDensity,
                    horizontalPadPx = 14f * density
                )
            } else if (clipboardItems.isEmpty()) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = Color.parseColor("#A7F3D0")
                drawFittedCenteredText(
                    canvas = canvas,
                    text = "Clipboard is empty. Copy text anywhere to paste here.",
                    rect = RectF(panelRect.left, panelRect.top + headerH, panelRect.right, panelRect.bottom),
                    paint = textPaint,
                    desiredSizePx = 12.5f * scaledDensity,
                    minSizePx = 10f * scaledDensity,
                    horizontalPadPx = 14f * density
                )
            } else {
                val maxItems = 3
                val itemTop = panelRect.top + headerH + 6f * density
                val itemH = (panelRect.bottom - itemTop - 6f * density) / maxItems
                clipboardItems.take(maxItems).forEachIndexed { idx, clip ->
                    val itemRect = RectF(
                        panelRect.left + 8f * density,
                        itemTop + idx * itemH,
                        panelRect.right - 8f * density,
                        itemTop + (idx + 1) * itemH - 4f * density
                    )
                    val isPressedClip = activePanelTarget?.value == clip && activePanelTarget?.isClipboard == true
                    keyFillPaint.color = if (isPressedClip) Color.parseColor("#00E676") else Color.parseColor("#15281E")
                    canvas.drawRoundRect(itemRect, 8f * density, 8f * density, keyFillPaint)

                    keyBorderPaint.color = Color.parseColor("#1F4231")
                    keyBorderPaint.strokeWidth = 0.8f * density
                    canvas.drawRoundRect(itemRect, 8f * density, 8f * density, keyBorderPaint)

                    textPaint.color = if (isPressedClip) Color.BLACK else Color.WHITE
                    textPaint.textAlign = Paint.Align.LEFT
                    val display = clip.replace("\n", " ").take(48)
                    val ty = itemRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                    val saveCount = canvas.save()
                    canvas.clipRect(itemRect)
                    canvas.drawText("📄 $display", itemRect.left + 10f * density, ty, textPaint)
                    canvas.restoreToCount(saveCount)
                    panelCellRects.add(PanelCellTarget(clip, itemRect, isClipboard = true))
                }
            }
        }
    }

    private fun drawSingleKeyRow(
        canvas: Canvas,
        row: KeyboardRowSpec,
        totalWidth: Float,
        top: Float,
        rowHeight: Float
    ) {
        if (row.keys.isEmpty()) return
        val hSpace = theme.layout.horizontalSpacingDp * density
        val totalWeight = row.keys.sumOf { it.widthWeight.toDouble() }.toFloat().coerceAtLeast(1f)

        val sideIndent = if (row.keys.size == 9 && totalWeight == 9f) totalWidth * 0.042f else max(hSpace, 4f * density)
        val availableWidth = (totalWidth - (sideIndent * 2f) - (hSpace * (row.keys.size - 1))).coerceAtLeast(row.keys.size * 16f)

        var curX = sideIndent
        for (spec in row.keys) {
            val keyW = availableWidth * (spec.widthWeight / totalWeight)
            val rect = RectF(curX, top, curX + keyW, top + rowHeight)
            val isPressed = activePressedKey?.spec?.id == spec.id
            val isInspected = isEditorInspectMode &&
                (inspectedKeyId.equals(spec.id, ignoreCase = true) ||
                    inspectedKeyId.equals(spec.primaryLabel, ignoreCase = true))

            drawSingleKey(canvas, spec, rect, isPressed, isInspected)
            hitRects.add(KeyHitTarget(spec, rect))
            curX += keyW + hSpace
        }
    }

    private fun drawSingleKey(
        canvas: Canvas,
        spec: KeySpec,
        rect: RectF,
        isPressed: Boolean,
        isInspected: Boolean
    ) {
        val style: KeyStyleConfig = theme.styleForKey(spec.id, spec.category)
        val effects = theme.effects
        val textCfg = theme.textStyle
        val reducedMotion = effects.reducedAnimationMode || globalSettings.reducedMotionOverride

        val drawRect = if (isPressed && effects.enableKeyPressScalePop && !reducedMotion) {
            val inset = -1.5f * density
            RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        } else {
            rect
        }

        val maxRadius = minOf(drawRect.width(), drawRect.height()) / 2f
        val radius = (style.cornerRadiusDp.coerceAtLeast(6f) * density).coerceAtMost(maxRadius)

        // Modern tactile key depth: draw subtle bottom shadow line
        if (!reducedMotion && !isPressed) {
            keyFillPaint.reset()
            keyFillPaint.isAntiAlias = true
            keyFillPaint.color = Color.parseColor("#44000000")
            val shadowRect = RectF(drawRect.left, drawRect.top + (1.5f * density), drawRect.right, drawRect.bottom + (1.5f * density))
            canvas.drawRoundRect(shadowRect, radius, radius, keyFillPaint)
        }

        val bgColor = when {
            isPressed && effects.enableKeyPressHighlight ->
                ColorAndImageUtils.parseColorSafe(style.pressedBackgroundColorHex, Color.parseColor("#00E676"))
            spec.actionType == KeyActionType.ENTER ->
                ColorAndImageUtils.parseColorSafe(theme.styleForKey("ENTER", KeyCategory.ENTER).backgroundColorHex, Color.parseColor("#00C853"))
            globalSettings.highContrastOverride ->
                Color.parseColor("#121212")
            else ->
                ColorAndImageUtils.parseColorSafe(style.backgroundColorHex, Color.parseColor("#14221B"))
        }

        keyFillPaint.reset()
        keyFillPaint.isAntiAlias = true
        keyFillPaint.style = Paint.Style.FILL

        if (!isPressed && style.enableGradient && !style.gradientEndColorHex.isNullOrBlank() && !globalSettings.highContrastOverride) {
            val endColor = ColorAndImageUtils.parseColorSafe(style.gradientEndColorHex, bgColor)
            keyFillPaint.shader = LinearGradient(
                drawRect.left, drawRect.top, drawRect.left, drawRect.bottom,
                bgColor, endColor, Shader.TileMode.CLAMP
            )
        } else {
            keyFillPaint.color = bgColor
        }

        val alphaInt = if (globalSettings.highContrastOverride) 255 else (style.keyOpacity.coerceIn(0.2f, 1f) * 255).toInt()
        keyFillPaint.alpha = alphaInt
        canvas.drawRoundRect(drawRect, radius, radius, keyFillPaint)

        // Key Border
        val borderWidth = when {
            isInspected -> 2.4f * density
            globalSettings.highContrastOverride -> max(1.8f, style.borderWidthDp) * density
            else -> style.borderWidthDp * density
        }
        if (borderWidth > 0f) {
            keyBorderPaint.strokeWidth = borderWidth
            keyBorderPaint.color = when {
                isInspected -> Color.parseColor("#FFD600")
                globalSettings.highContrastOverride -> Color.parseColor("#00E676")
                spec.actionType == KeyActionType.SHIFT && engineState.shiftState == ShiftState.CAPS_LOCK ->
                    Color.parseColor("#00E676")
                else -> ColorAndImageUtils.parseColorSafe(style.borderColorHex, Color.parseColor("#1F4231"))
            }
            canvas.drawRoundRect(drawRect, radius, radius, keyBorderPaint)
        }

        val clipSaveCount = canvas.save()
        canvas.clipRect(drawRect)

        val secLabel = spec.secondaryLabel
        val hasSecondaryHint = textCfg.showSecondaryHintSymbols && !secLabel.isNullOrBlank()

        val isBengaliKey = engineState.language == KeyboardLanguage.BENGALI ||
            spec.primaryLabel.any { it.code in 0x0980..0x09FF }
        val typeface = FontManager.resolveTypeface(context, textCfg, isBengaliScript = isBengaliKey)

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = typeface

        val baseSizeSp = when {
            spec.actionType == KeyActionType.SPACE -> 12f
            spec.primaryLabel.length > 5 -> (textCfg.fontSizeSp * 0.65f).coerceAtLeast(10.5f)
            spec.primaryLabel.length > 2 -> (textCfg.fontSizeSp * 0.74f).coerceAtLeast(11.5f)
            else -> textCfg.fontSizeSp
        }
        val boostedSp = if (globalSettings.readableKeyLabelsBoost) baseSizeSp + 2f else baseSizeSp
        var targetTextSizePx = (boostedSp * scaledDensity).coerceAtMost(drawRect.height() * 0.56f)
        textPaint.textSize = targetTextSizePx
        textPaint.letterSpacing = if (isBengaliKey) 0f else textCfg.letterSpacingEm

        val maxAllowedTextWidth = (drawRect.width() - (6f * density)).coerceAtLeast(8f * density)
        val measuredW = textPaint.measureText(spec.primaryLabel)
        if (measuredW > maxAllowedTextWidth && measuredW > 0f) {
            val scale = (maxAllowedTextWidth / measuredW).coerceIn(0.45f, 1f)
            targetTextSizePx = (targetTextSizePx * scale).coerceAtLeast(8.5f * scaledDensity)
            textPaint.textSize = targetTextSizePx
        }

        val labelColor = when {
            isPressed && effects.enableKeyPressHighlight ->
                ColorAndImageUtils.parseColorSafe(style.pressedLabelColorHex, Color.BLACK)
            spec.actionType == KeyActionType.ENTER -> Color.parseColor("#021207")
            globalSettings.highContrastOverride -> Color.WHITE
            else -> ColorAndImageUtils.parseColorSafe(style.labelColorHex, Color.WHITE)
        }
        textPaint.color = labelColor

        val fm = textPaint.fontMetrics
        val textCenterOffset = (fm.descent + fm.ascent) / 2f
        val hintVerticalOffset = if (hasSecondaryHint) 2.2f * density else 0f

        textPaint.textAlign = Paint.Align.CENTER
        val labelX = drawRect.centerX()
        val labelY = drawRect.centerY() - textCenterOffset + hintVerticalOffset

        // Modern spacebar label
        val renderedLabel = if (spec.actionType == KeyActionType.SPACE) {
            when (engineState.language) {
                KeyboardLanguage.BENGALI -> "বাংলা • Phonetic"
                else -> "English (US)"
            }
        } else {
            spec.primaryLabel
        }

        canvas.drawText(renderedLabel, labelX, labelY, textPaint)

        // Secondary Hint Symbol in Top-Right corner
        if (hasSecondaryHint && secLabel != null) {
            secondaryTextPaint.reset()
            secondaryTextPaint.isAntiAlias = true
            secondaryTextPaint.typeface = typeface
            secondaryTextPaint.textSize = (textCfg.secondaryLabelSizeSp * scaledDensity).coerceAtMost(drawRect.height() * 0.32f)
            secondaryTextPaint.color = labelColor
            secondaryTextPaint.alpha = 165
            secondaryTextPaint.textAlign = Paint.Align.RIGHT
            val secBaselineY = drawRect.top + (2.5f * density) - secondaryTextPaint.ascent()
            canvas.drawText(secLabel, drawRect.right - (4f * density), secBaselineY, secondaryTextPaint)
        }

        canvas.restoreToCount(clipSaveCount)
    }

    private fun drawFittedCenteredText(
        canvas: Canvas,
        text: String,
        rect: RectF,
        paint: Paint,
        desiredSizePx: Float,
        minSizePx: Float,
        horizontalPadPx: Float
    ) {
        if (text.isEmpty()) return
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = desiredSizePx
        val availableW = (rect.width() - (horizontalPadPx * 2f)).coerceAtLeast(8f)
        val measured = paint.measureText(text)
        if (measured > availableW && measured > 0f) {
            val scaled = (desiredSizePx * (availableW / measured)).coerceAtLeast(minSizePx)
            paint.textSize = scaled
        }
        val save = canvas.save()
        canvas.clipRect(rect)
        val textY = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, rect.centerX(), textY, paint)
        canvas.restoreToCount(save)
    }

    private fun drawKeyPopupPreview(canvas: Canvas, pressed: KeyHitTarget) {
        val r = pressed.rect
        val popW = max(r.width() * 1.35f, 48f * density)
        val popH = 56f * density
        val popLeft = (r.centerX() - popW / 2f).coerceIn(4f * density, width - popW - 4f * density)
        val popTop = (r.top - popH - 8f * density).coerceAtLeast(2f * density)
        val popRect = RectF(popLeft, popTop, popLeft + popW, popTop + popH)

        // Shadow under popup
        popupPaint.reset()
        popupPaint.isAntiAlias = true
        popupPaint.color = Color.parseColor("#55000000")
        val shadowPop = RectF(popRect.left, popRect.top + 2f * density, popRect.right, popRect.bottom + 2f * density)
        canvas.drawRoundRect(shadowPop, 12f * density, 12f * density, popupPaint)

        popupPaint.color = ColorAndImageUtils.parseColorSafe(
            theme.defaultKeyStyle.pressedBackgroundColorHex,
            Color.parseColor("#00E676")
        )
        canvas.drawRoundRect(popRect, 12f * density, 12f * density, popupPaint)

        val previewText = if (longPressTriggered && !pressed.spec.secondaryOutputText.isNullOrEmpty()) {
            pressed.spec.secondaryOutputText
        } else {
            pressed.spec.primaryLabel
        }
        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = FontManager.resolveTypeface(
            context,
            theme.textStyle,
            isBengaliScript = previewText.any { it.code in 0x0980..0x09FF }
        )
        textPaint.color = ColorAndImageUtils.parseColorSafe(
            theme.defaultKeyStyle.pressedLabelColorHex,
            Color.BLACK
        )
        textPaint.textSize = 24f * scaledDensity
        textPaint.textAlign = Paint.Align.CENTER
        val ty = popRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(previewText, popRect.centerX(), ty, textPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressTriggered = false
                mainHandler.removeCallbacks(longPressRunnable)
                activePressedKey = null
                activeToolbarTarget = null
                activePanelTarget = null
                activeEmojiTabTarget = null

                // 1. Priority check: Top Suggestion & Toolbar
                val toolbarHit = toolbarRects.firstOrNull { it.rect.contains(x, y) }
                if (toolbarHit != null) {
                    activeToolbarTarget = toolbarHit
                    performFeedback()
                    invalidate()
                    return true
                }

                // 2. Priority check: Emoji Category Tabs
                if (engineState.mode == KeyboardMode.EMOJI_PANEL) {
                    val tabHit = emojiTabRects.firstOrNull { it.rect.contains(x, y) }
                    if (tabHit != null) {
                        activeEmojiTabTarget = tabHit
                        performFeedback()
                        invalidate()
                        return true
                    }
                }

                // 3. Priority check: Emoji / Clipboard Cells
                val panelBounds = overlayPanelBounds
                if (panelBounds != null && panelBounds.contains(x, y)) {
                    val panelHit = panelCellRects.firstOrNull { it.rect.contains(x, y) }
                    if (panelHit != null) {
                        activePanelTarget = panelHit
                        performFeedback()
                        invalidate()
                    }
                    return true
                }

                // 4. Key rows hit check
                val keyTarget = findClosestKey(x, y)
                if (keyTarget != null) {
                    activePressedKey = keyTarget
                    performFeedback()
                    invalidate()
                    if (!isEditorInspectMode) {
                        mainHandler.postDelayed(longPressRunnable, 380L)
                    }
                    return true
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeToolbarTarget != null) {
                    val stillInToolbar = toolbarRects.firstOrNull { it.rect.contains(x, y) }
                    if (stillInToolbar?.id != activeToolbarTarget?.id) {
                        activeToolbarTarget = stillInToolbar
                        invalidate()
                    }
                    return true
                }
                if (activePanelTarget != null) {
                    val stillInPanel = panelCellRects.firstOrNull { it.rect.contains(x, y) }
                    if (stillInPanel?.value != activePanelTarget?.value) {
                        activePanelTarget = stillInPanel
                        invalidate()
                    }
                    return true
                }

                val current = activePressedKey
                if (current != null) {
                    val movedOutside = !current.rect.contains(x, y) &&
                        (abs(x - current.rect.centerX()) > current.rect.width() * 0.8f ||
                            abs(y - current.rect.centerY()) > current.rect.height() * 0.8f)
                    if (movedOutside && !longPressTriggered) {
                        mainHandler.removeCallbacks(longPressRunnable)
                        activePressedKey = findClosestKey(x, y)
                        invalidate()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(longPressRunnable)
                val releasedToolbar = activeToolbarTarget
                val releasedPanel = activePanelTarget
                val releasedEmojiTab = activeEmojiTabTarget
                val releasedKey = activePressedKey

                activeToolbarTarget = null
                activePanelTarget = null
                activeEmojiTabTarget = null
                activePressedKey = null
                invalidate()

                if (releasedToolbar != null) {
                    val confirmHit = toolbarRects.firstOrNull { it.rect.contains(x, y) } ?: releasedToolbar
                    handleToolbarTap(confirmHit)
                    return true
                }

                if (releasedEmojiTab != null) {
                    val confirmTab = emojiTabRects.firstOrNull { it.rect.contains(x, y) } ?: releasedEmojiTab
                    engineState = engineState.copy(selectedEmojiCategoryIndex = confirmTab.index)
                    listener?.onEmojiCategorySelected(confirmTab.index)
                    invalidate()
                    return true
                }

                if (releasedPanel != null) {
                    val confirmPanel = panelCellRects.firstOrNull { it.rect.contains(x, y) } ?: releasedPanel
                    if (confirmPanel.isClipboard) {
                        listener?.onClipboardItemSelected(confirmPanel.value)
                    } else {
                        listener?.onEmojiSelected(confirmPanel.value)
                        EmojiRepository.recordEmojiUsage(context, confirmPanel.value)
                    }
                    return true
                }

                if (releasedKey != null) {
                    if (isEditorInspectMode) {
                        inspectedKeyId = releasedKey.spec.id
                        listener?.onKeySelectedInEditor(releasedKey.spec)
                    } else if (!longPressTriggered) {
                        listener?.onKeyTapped(releasedKey.spec, isLongPress = false)
                    }
                }
                longPressTriggered = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                activeToolbarTarget = null
                activePanelTarget = null
                activeEmojiTabTarget = null
                activePressedKey = null
                longPressTriggered = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleToolbarTap(hit: ToolbarHitTarget) {
        if (hit.isSuggestion) {
            listener?.onSuggestionClicked(hit.label)
            return
        }
        when (hit.id) {
            "STUDIO" -> listener?.onOpenStudioAppRequested()
            "LANG" -> listener?.onToolbarLanguageToggle()
            "VOICE" -> listener?.onVoiceTypingRequested()
            "VOICE_CANCEL" -> listener?.onVoiceCancelRequested()
            "CLIP" -> {
                val target = if (engineState.mode == KeyboardMode.CLIPBOARD_PANEL) {
                    KeyboardMode.ALPHABET
                } else {
                    KeyboardMode.CLIPBOARD_PANEL
                }
                listener?.onToolbarModeToggle(target)
            }
            "CLIP_CLEAR" -> listener?.onClipboardClearRequested()
            "EMOJI" -> {
                val target = if (engineState.mode == KeyboardMode.EMOJI_PANEL) {
                    KeyboardMode.ALPHABET
                } else {
                    KeyboardMode.EMOJI_PANEL
                }
                listener?.onToolbarModeToggle(target)
            }
        }
    }

    private fun findClosestKey(x: Float, y: Float): KeyHitTarget? {
        val exact = hitRects.firstOrNull { it.rect.contains(x, y) }
        if (exact != null) return exact

        var closest: KeyHitTarget? = null
        var minDistanceSq = Float.MAX_VALUE
        val tolerance = 24f * density

        for (target in hitRects) {
            val r = target.rect
            if (y < r.top - tolerance || y > r.bottom + tolerance) continue
            val cx = r.centerX()
            val cy = r.centerY()
            val dx = x - cx
            val dy = y - cy
            val distSq = (dx * dx) + (dy * dy)
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq
                closest = target
            }
        }
        return closest
    }

    private fun performFeedback() {
        val effects = theme.effects
        if (effects.enableVibrationFeedback && globalSettings.globalVibrationEnabled) {
            try {
                val ms = globalSettings.globalVibrationStrengthMs.coerceIn(5, 80).toLong()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    v?.vibrate(ms)
                }
            } catch (_: Exception) {
            }
        }

        if (effects.enableSoundFeedback && globalSettings.globalSoundEnabled) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, effects.soundVolume)
            } catch (_: Exception) {
            }
        }
    }
}
