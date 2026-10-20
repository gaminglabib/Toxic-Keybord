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
import com.example.keyboard.layout.KeyActionType
import com.example.keyboard.layout.KeySpec
import com.example.keyboard.layout.KeyboardLayouts
import com.example.keyboard.layout.KeyboardRowSpec
import com.example.keyboard.state.KeyboardEngineState
import com.example.keyboard.state.KeyboardLanguage
import com.example.keyboard.state.KeyboardMode
import com.example.keyboard.state.ShiftState
import com.example.theme.model.BackgroundSourceType
import com.example.theme.model.KeyCategory
import com.example.theme.model.KeyLabelAlignment
import com.example.theme.model.KeyStyleConfig
import com.example.theme.model.KeyboardTheme
import com.example.theme.storage.GlobalKeyboardSettings
import com.example.util.ColorAndImageUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * High-performance native Canvas keyboard view shared between:
 * 1) The real system InputMethodService (ToxicInputMethodService)
 * 2) The Theme Studio / Text Studio / Layout Studio Live Preview
 *
 * Supports both XML layout inflation (via @JvmOverloads constructors) and programmatic creation.
 * Ensures 100% visual and behavioral parity between preview and actual system keyboard,
 * while avoiding Compose-in-IME window token lifecycle crashes on Android 9 devices.
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
    private var overlayPanelBounds: RectF? = null

    private var activePressedKey: KeyHitTarget? = null
    private var activeToolbarTarget: ToolbarHitTarget? = null
    private var activePanelTarget: PanelCellTarget? = null
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
    private val textOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
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

    private val longPressRunnable = object : Runnable {
        override fun run() {
            val current = activePressedKey ?: return
            longPressTriggered = true
            performFeedback()
            if (current.spec.actionType == KeyActionType.BACKSPACE) {
                listener?.onKeyTapped(current.spec, isLongPress = false)
                mainHandler.postDelayed(this, 75L)
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

        rebuildRows()
        if (layoutChanged) {
            requestLayout()
        }
        invalidate()
    }

    private fun rebuildRows() {
        rows = KeyboardLayouts.buildRows(engineState, theme.layout)
    }

    private fun isLandscapeActive(): Boolean {
        previewLandscapeOverride?.let { return it }
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * Computes the exact pixel height required by the toolbar, active rows/panels,
     * vertical gaps, and top/bottom padding so onMeasure and onDraw are 100% aligned.
     */
    fun computeRequiredHeightPx(): Int {
        if (rows.isEmpty()) rebuildRows()
        val isLand = isLandscapeActive()
        val baseKeyH = (if (isLand) theme.layout.landscapeKeyHeightDp else theme.layout.keyHeightDp) * density
        val vSpace = theme.layout.verticalSpacingDp * density
        val topPad = 6f * density
        val toolbarBlock = if (theme.layout.showTopToolbar) (36f * density) + (6f * density) else 0f
        val bottomPad = (theme.layout.keyboardBottomPaddingDp.coerceAtLeast(6f)) * density

        val bodyHeight = when (engineState.mode) {
            KeyboardMode.EMOJI_PANEL, KeyboardMode.CLIPBOARD_PANEL -> {
                val panelH = (baseKeyH * 3.2f) + (vSpace * 2f)
                panelH + (6f * density) + baseKeyH
            }
            else -> {
                val activeRows = rows.ifEmpty { KeyboardLayouts.buildRows(engineState, theme.layout) }
                var sum = 0f
                activeRows.forEachIndexed { index, row ->
                    val rH = if (row.isNumberRow) baseKeyH * 0.86f else baseKeyH
                    sum += rH
                    if (index < activeRows.lastIndex) {
                        sum += vSpace
                    }
                }
                sum
            }
        }
        return (topPad + toolbarBlock + bodyHeight + bottomPad).roundToInt()
            .coerceAtLeast((180f * density).roundToInt())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenWidth = resources.displayMetrics.widthPixels.coerceAtLeast((320 * density).roundToInt())
        val specMode = MeasureSpec.getMode(widthMeasureSpec)
        val specSize = MeasureSpec.getSize(widthMeasureSpec)

        val measuredW = when {
            specMode == MeasureSpec.UNSPECIFIED || specSize <= 0 -> screenWidth
            else -> specSize.coerceAtLeast((280 * density).roundToInt())
        }

        val desiredH = computeRequiredHeightPx()
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val measuredH = when (heightMode) {
            MeasureSpec.EXACTLY -> if (heightSize > 0) heightSize else desiredH
            MeasureSpec.AT_MOST -> if (heightSize > 0) desiredH.coerceAtMost(heightSize).coerceAtLeast((160f * density).roundToInt()) else desiredH
            else -> desiredH
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

        // 2. Draw Top Suggestion & Quick Toolbar
        toolbarRects.clear()
        if (theme.layout.showTopToolbar) {
            val toolbarHeight = 36f * density
            drawTopToolbar(canvas, w, currentTop, toolbarHeight)
            currentTop += toolbarHeight + (6f * density)
        }

        // 3. Draw Emoji / Clipboard Panel OR Standard Key Rows
        hitRects.clear()
        panelCellRects.clear()
        overlayPanelBounds = null

        val isLand = isLandscapeActive()
        val baseKeyH = (if (isLand) theme.layout.landscapeKeyHeightDp else theme.layout.keyHeightDp) * density
        val vSpace = theme.layout.verticalSpacingDp * density

        if (engineState.mode == KeyboardMode.EMOJI_PANEL || engineState.mode == KeyboardMode.CLIPBOARD_PANEL) {
            val bottomPad = (theme.layout.keyboardBottomPaddingDp.coerceAtLeast(6f)) * density
            val panelHeight = (h - currentTop - baseKeyH - bottomPad - (6f * density)).coerceAtLeast(88f * density)
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

        // 4. Draw Long-Press Popup Preview Bubble if active
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
        val btnCorner = btnStyle.cornerRadiusDp.coerceAtMost(height / 2f) * density
        val btnBgColor = if (globalSettings.highContrastOverride) Color.parseColor("#111111") else ColorAndImageUtils.parseColorSafe(btnStyle.backgroundColorHex)
        val pressedBgColor = ColorAndImageUtils.parseColorSafe(btnStyle.pressedBackgroundColorHex, Color.parseColor("#00E676"))
        val labelColor = if (globalSettings.highContrastOverride) Color.WHITE else ColorAndImageUtils.parseColorSafe(btnStyle.labelColorHex)
        val borderColor = if (globalSettings.highContrastOverride) Color.parseColor("#00E676") else ColorAndImageUtils.parseColorSafe(btnStyle.borderColorHex)

        // Left action pills: Studio (⚙), Lang (EN/বাং), Clipboard (📋), Emoji (☺)
        val actionItems = listOf(
            "STUDIO" to "⚙",
            "LANG" to engineState.language.badge,
            "CLIP" to "📋",
            "EMOJI" to "☺"
        )
        val pillWidth = 37f * density
        val gap = 4f * density
        var curX = padH

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = FontManager.resolveTypeface(context, theme.textStyle, isBengaliScript = true)
        textPaint.textAlign = Paint.Align.CENTER

        for ((id, text) in actionItems) {
            val rect = RectF(curX, top, curX + pillWidth, top + height)
            val isActiveModePill = (id == "CLIP" && engineState.mode == KeyboardMode.CLIPBOARD_PANEL) ||
                (id == "EMOJI" && engineState.mode == KeyboardMode.EMOJI_PANEL) ||
                (activeToolbarTarget?.id == id)

            keyFillPaint.shader = null
            keyFillPaint.color = if (isActiveModePill) pressedBgColor else btnBgColor
            keyFillPaint.alpha = if (isActiveModePill) 245 else 225
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyFillPaint)

            keyBorderPaint.color = if (isActiveModePill) Color.WHITE else borderColor
            keyBorderPaint.strokeWidth = 1f * density
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyBorderPaint)

            textPaint.color = if (isActiveModePill) Color.BLACK else labelColor
            drawFittedCenteredText(
                canvas = canvas,
                text = text,
                rect = rect,
                paint = textPaint,
                desiredSizePx = theme.textStyle.toolbarLabelSizeSp * scaledDensity,
                minSizePx = 10f * scaledDensity,
                horizontalPadPx = 4f * density
            )
            toolbarRects.add(ToolbarHitTarget(id, text, rect, isSuggestion = false))
            curX += pillWidth + gap
        }

        // Remaining space on right: 3 Suggestion chips (or sensitive field badge)
        val remainingW = (totalWidth - curX - padH).coerceAtLeast(60f * density)
        if (engineState.editorContext.isPasswordOrSensitive || !globalSettings.enableSuggestions) {
            val rect = RectF(curX, top, curX + remainingW, top + height)
            keyFillPaint.color = Color.parseColor("#111C16")
            keyFillPaint.alpha = 195
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyFillPaint)

            keyBorderPaint.color = borderColor
            keyBorderPaint.strokeWidth = 0.8f * density
            canvas.drawRoundRect(rect, btnCorner, btnCorner, keyBorderPaint)

            textPaint.color = Color.parseColor("#6EE7B7")
            val msg = if (engineState.editorContext.isPasswordOrSensitive) {
                "🔒 Protected Input"
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
            for (word in suggestions) {
                val id = "SUG_$word"
                val isPressedSug = activeToolbarTarget?.id == id
                val rect = RectF(curX, top, curX + chipW, top + height)
                keyFillPaint.color = if (isPressedSug) pressedBgColor else btnBgColor
                keyFillPaint.alpha = if (isPressedSug) 245 else 210
                canvas.drawRoundRect(rect, btnCorner, btnCorner, keyFillPaint)

                keyBorderPaint.color = borderColor
                keyBorderPaint.strokeWidth = 0.8f * density
                canvas.drawRoundRect(rect, btnCorner, btnCorner, keyBorderPaint)

                textPaint.color = if (isPressedSug) Color.BLACK else labelColor
                drawFittedCenteredText(
                    canvas = canvas,
                    text = word,
                    rect = rect,
                    paint = textPaint,
                    desiredSizePx = theme.textStyle.toolbarLabelSizeSp * scaledDensity,
                    minSizePx = 9.5f * scaledDensity,
                    horizontalPadPx = 5f * density
                )
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
        keyFillPaint.color = Color.parseColor("#EE08120E")
        canvas.drawRoundRect(panelRect, 12f * density, 12f * density, keyFillPaint)

        keyBorderPaint.color = Color.parseColor("#1F4231")
        keyBorderPaint.strokeWidth = 1f * density
        canvas.drawRoundRect(panelRect, 12f * density, 12f * density, keyBorderPaint)

        if (engineState.mode == KeyboardMode.EMOJI_PANEL) {
            val cols = 8
            val rowsCount = 5
            val cellW = panelRect.width() / cols
            val cellH = panelRect.height() / rowsCount
            textPaint.reset()
            textPaint.isAntiAlias = true
            textPaint.textSize = (cellH * 0.56f).coerceIn(14f * scaledDensity, 22f * scaledDensity)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE

            KeyboardLayouts.EMOJI_PALETTE.take(cols * rowsCount).forEachIndexed { idx, emoji ->
                val r = idx / cols
                val c = idx % cols
                val cellRect = RectF(
                    panelRect.left + c * cellW,
                    panelRect.top + r * cellH,
                    panelRect.left + (c + 1) * cellW,
                    panelRect.top + (r + 1) * cellH
                )
                if (activePanelTarget?.value == emoji && activePanelTarget?.isClipboard == false) {
                    keyFillPaint.color = Color.parseColor("#3300E676")
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
            if (!globalSettings.enableClipboardHistory) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = Color.parseColor("#A7F3D0")
                drawFittedCenteredText(
                    canvas = canvas,
                    text = "Clipboard History is off. Enable it in Settings.",
                    rect = panelRect,
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
                    text = "Clipboard is empty. Copied snippets appear here.",
                    rect = panelRect,
                    paint = textPaint,
                    desiredSizePx = 12.5f * scaledDensity,
                    minSizePx = 10f * scaledDensity,
                    horizontalPadPx = 14f * density
                )
            } else {
                val maxItems = 4
                val itemH = (panelRect.height() - 16f * density) / maxItems
                clipboardItems.take(maxItems).forEachIndexed { idx, clip ->
                    val itemRect = RectF(
                        panelRect.left + 8f * density,
                        panelRect.top + 8f * density + idx * itemH,
                        panelRect.right - 8f * density,
                        panelRect.top + 8f * density + (idx + 1) * itemH - 4f * density
                    )
                    val isPressedClip = activePanelTarget?.value == clip && activePanelTarget?.isClipboard == true
                    keyFillPaint.color = if (isPressedClip) Color.parseColor("#00E676") else Color.parseColor("#182C22")
                    canvas.drawRoundRect(itemRect, 8f * density, 8f * density, keyFillPaint)
                    textPaint.color = if (isPressedClip) Color.BLACK else Color.WHITE
                    textPaint.textAlign = Paint.Align.LEFT
                    val display = clip.replace("\n", " ").take(42)
                    val ty = itemRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                    val saveCount = canvas.save()
                    canvas.clipRect(itemRect)
                    canvas.drawText(display, itemRect.left + 10f * density, ty, textPaint)
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

        // Add subtle side margin for 9-key home row in English QWERTY
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
        val radius = (style.cornerRadiusDp * density).coerceAtMost(maxRadius)
        val bgColor = when {
            isPressed && effects.enableKeyPressHighlight ->
                ColorAndImageUtils.parseColorSafe(style.pressedBackgroundColorHex, Color.parseColor("#00E676"))
            globalSettings.highContrastOverride ->
                Color.parseColor("#101010")
            else ->
                ColorAndImageUtils.parseColorSafe(style.backgroundColorHex, Color.parseColor("#14221B"))
        }

        // Key Fill + Optional Gradient + Shadow/Glow
        keyFillPaint.reset()
        keyFillPaint.isAntiAlias = true
        keyFillPaint.style = Paint.Style.FILL

        if (!isPressed && style.enableGradient && !style.gradientEndColorHex.isNullOrBlank() && !globalSettings.highContrastOverride) {
            val endColor = ColorAndImageUtils.parseColorSafe(style.gradientEndColorHex, bgColor)
            keyFillPaint.shader = LinearGradient(
                drawRect.left,
                drawRect.top,
                drawRect.left,
                drawRect.bottom,
                bgColor,
                endColor,
                Shader.TileMode.CLAMP
            )
        } else {
            keyFillPaint.color = bgColor
        }

        val alphaInt = if (globalSettings.highContrastOverride) 255 else (style.keyOpacity.coerceIn(0.2f, 1f) * 255).toInt()
        keyFillPaint.alpha = alphaInt

        if (!reducedMotion) {
            if ((effects.enableKeyGlow || style.enableGlow) && !globalSettings.highContrastOverride) {
                val glowColor = ColorAndImageUtils.parseColorSafe(style.glowColorHex, Color.parseColor("#00E676"))
                keyFillPaint.setShadowLayer(style.glowRadiusDp * density, 0f, 0f, glowColor)
            } else if (effects.enableKeyShadows && style.shadowRadiusDp > 0f) {
                val shadowCol = ColorAndImageUtils.parseColorSafe(style.shadowColorHex, Color.argb(140, 0, 0, 0))
                keyFillPaint.setShadowLayer(style.shadowRadiusDp * density, 0f, 1.5f * density, shadowCol)
            }
        }

        canvas.drawRoundRect(drawRect, radius, radius, keyFillPaint)
        keyFillPaint.clearShadowLayer()

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

        // Clip key text rendering inside key bounds so glyphs never bleed outside narrow keys
        val clipSaveCount = canvas.save()
        canvas.clipRect(drawRect)

        val secLabel = spec.secondaryLabel
        val hasSecondaryHint = textCfg.showSecondaryHintSymbols && !secLabel.isNullOrBlank()

        // Primary Key Label
        val isBengaliKey = engineState.language == KeyboardLanguage.BENGALI ||
            spec.primaryLabel.any { it.code in 0x0980..0x09FF }
        val typeface = FontManager.resolveTypeface(context, textCfg, isBengaliScript = isBengaliKey)

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = typeface

        val baseSizeSp = when {
            spec.primaryLabel.length > 5 -> (textCfg.fontSizeSp * 0.65f).coerceAtLeast(10.5f)
            spec.primaryLabel.length > 2 -> (textCfg.fontSizeSp * 0.74f).coerceAtLeast(11.5f)
            else -> textCfg.fontSizeSp
        }
        val boostedSp = if (globalSettings.readableKeyLabelsBoost) baseSizeSp + 2f else baseSizeSp
        var targetTextSizePx = (boostedSp * scaledDensity).coerceAtMost(drawRect.height() * 0.56f)
        textPaint.textSize = targetTextSizePx
        textPaint.letterSpacing = if (isBengaliKey) 0f else textCfg.letterSpacingEm

        // Auto-fit primary label width inside keycap
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
            globalSettings.highContrastOverride -> Color.WHITE
            else -> ColorAndImageUtils.parseColorSafe(style.labelColorHex, Color.WHITE)
        }
        textPaint.color = labelColor

        if (!isPressed && textCfg.enableGradientText && !globalSettings.highContrastOverride) {
            val gradEnd = ColorAndImageUtils.parseColorSafe(textCfg.gradientTextEndColorHex, labelColor)
            textPaint.shader = LinearGradient(
                drawRect.left,
                drawRect.top,
                drawRect.right,
                drawRect.bottom,
                labelColor,
                gradEnd,
                Shader.TileMode.CLAMP
            )
        }

        if (textCfg.enableLabelGlow && !globalSettings.highContrastOverride) {
            val glowCol = ColorAndImageUtils.parseColorSafe(textCfg.labelGlowColorHex, labelColor)
            textPaint.setShadowLayer(5f * density, 0f, 0f, glowCol)
        } else if (textCfg.enableTextShadow && !globalSettings.highContrastOverride) {
            val shadCol = ColorAndImageUtils.parseColorSafe(textCfg.textShadowColorHex, Color.BLACK)
            textPaint.setShadowLayer(textCfg.textShadowRadius * density, 0f, 1f * density, shadCol)
        }

        val labelX: Float
        val labelY: Float
        val fm = textPaint.fontMetrics
        val textCenterOffset = (fm.descent + fm.ascent) / 2f
        val hintVerticalOffset = if (hasSecondaryHint) 2.2f * density else 0f

        when (textCfg.labelAlignment) {
            KeyLabelAlignment.TOP_LEFT -> {
                if (spec.category == KeyCategory.ALPHABETIC && spec.primaryLabel.length <= 2) {
                    textPaint.textAlign = Paint.Align.LEFT
                    labelX = drawRect.left + 6f * density
                    labelY = drawRect.centerY() - textCenterOffset + hintVerticalOffset
                } else {
                    textPaint.textAlign = Paint.Align.CENTER
                    labelX = drawRect.centerX()
                    labelY = drawRect.centerY() - textCenterOffset + hintVerticalOffset
                }
            }
            KeyLabelAlignment.BOTTOM_RIGHT -> {
                if (spec.category == KeyCategory.ALPHABETIC && spec.primaryLabel.length <= 2) {
                    textPaint.textAlign = Paint.Align.RIGHT
                    labelX = drawRect.right - 6f * density
                    labelY = drawRect.centerY() - textCenterOffset + (3.2f * density)
                } else {
                    textPaint.textAlign = Paint.Align.CENTER
                    labelX = drawRect.centerX()
                    labelY = drawRect.centerY() - textCenterOffset + hintVerticalOffset
                }
            }
            KeyLabelAlignment.CENTER -> {
                textPaint.textAlign = Paint.Align.CENTER
                labelX = drawRect.centerX()
                labelY = drawRect.centerY() - textCenterOffset + hintVerticalOffset
            }
        }

        if (textCfg.enableTextOutline && textCfg.textOutlineWidthPx > 0f && !isBengaliKey) {
            textOutlinePaint.typeface = typeface
            textOutlinePaint.textSize = textPaint.textSize
            textOutlinePaint.textAlign = textPaint.textAlign
            textOutlinePaint.strokeWidth = textCfg.textOutlineWidthPx * density
            textOutlinePaint.color = ColorAndImageUtils.parseColorSafe(textCfg.textOutlineColorHex, Color.BLACK)
            canvas.drawText(spec.primaryLabel, labelX, labelY, textOutlinePaint)
        }

        canvas.drawText(spec.primaryLabel, labelX, labelY, textPaint)
        textPaint.clearShadowLayer()
        textPaint.shader = null

        // Secondary Hint Symbol in Top-Right corner with proper font-metrics baseline
        if (hasSecondaryHint && secLabel != null) {
            secondaryTextPaint.reset()
            secondaryTextPaint.isAntiAlias = true
            secondaryTextPaint.typeface = FontManager.resolveTypeface(
                context,
                textCfg,
                isBengaliScript = secLabel.any { it.code in 0x0980..0x09FF }
            )
            val secMaxPx = (drawRect.height() * 0.25f).coerceIn(8f * scaledDensity, 12.5f * scaledDensity)
            secondaryTextPaint.textSize = (textCfg.secondaryLabelSizeSp * scaledDensity).coerceAtMost(secMaxPx)
            secondaryTextPaint.color = labelColor
            secondaryTextPaint.alpha = 165
            secondaryTextPaint.textAlign = Paint.Align.RIGHT
            val secBaselineY = drawRect.top + (2.5f * density) - secondaryTextPaint.ascent()
            canvas.drawText(
                secLabel,
                drawRect.right - (4f * density),
                secBaselineY,
                secondaryTextPaint
            )
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
        val popW = max(r.width() * 1.25f, 44f * density)
        val popH = 50f * density
        val popLeft = (r.centerX() - popW / 2f).coerceIn(4f * density, width - popW - 4f * density)
        val popTop = (r.top - popH - 6f * density).coerceAtLeast(2f * density)
        val popRect = RectF(popLeft, popTop, popLeft + popW, popTop + popH)

        popupPaint.color = ColorAndImageUtils.parseColorSafe(
            theme.defaultKeyStyle.pressedBackgroundColorHex,
            Color.parseColor("#00E676")
        )
        canvas.drawRoundRect(popRect, 10f * density, 10f * density, popupPaint)

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
        textPaint.textSize = 22f * scaledDensity
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

                // 1. Priority check: Top Suggestion & Quick-Action Toolbar
                val toolbarHit = toolbarRects.firstOrNull { it.rect.contains(x, y) }
                if (toolbarHit != null) {
                    activeToolbarTarget = toolbarHit
                    performFeedback()
                    invalidate()
                    return true
                }

                // 2. Priority check: Emoji / Clipboard Overlay Panel
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

                // 3. Key rows hit check
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
                val releasedKey = activePressedKey

                activeToolbarTarget = null
                activePanelTarget = null
                activePressedKey = null
                invalidate()

                // 1. Handle toolbar release
                if (releasedToolbar != null) {
                    val confirmHit = toolbarRects.firstOrNull { it.rect.contains(x, y) } ?: releasedToolbar
                    handleToolbarTap(confirmHit)
                    return true
                }

                // 2. Handle emoji / clipboard panel release
                if (releasedPanel != null) {
                    val confirmPanel = panelCellRects.firstOrNull { it.rect.contains(x, y) } ?: releasedPanel
                    if (confirmPanel.isClipboard) {
                        listener?.onClipboardItemSelected(confirmPanel.value)
                    } else {
                        listener?.onEmojiSelected(confirmPanel.value)
                    }
                    return true
                }

                // 3. Handle key release
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
            "CLIP" -> {
                val target = if (engineState.mode == KeyboardMode.CLIPBOARD_PANEL) {
                    KeyboardMode.ALPHABET
                } else {
                    KeyboardMode.CLIPBOARD_PANEL
                }
                listener?.onToolbarModeToggle(target)
            }
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
        // Ignore touches inside the top toolbar band so toolbar buttons are never stolen
        if (theme.layout.showTopToolbar && y < (42f * density)) {
            return null
        }
        hitRects.firstOrNull { it.rect.contains(x, y) }?.let { return it }
        val maxThreshold = 12f * density
        return hitRects.minByOrNull { target ->
            val dx = max(0f, max(target.rect.left - x, x - target.rect.right))
            val dy = max(0f, max(target.rect.top - y, y - target.rect.bottom))
            dx * dx + dy * dy
        }?.takeIf { target ->
            val dx = max(0f, max(target.rect.left - x, x - target.rect.right))
            val dy = max(0f, max(target.rect.top - y, y - target.rect.bottom))
            dx <= maxThreshold && dy <= maxThreshold
        }
    }

    private fun performFeedback() {
        val vibrateOn = theme.effects.enableVibrationFeedback && globalSettings.globalVibrationEnabled
        if (vibrateOn) {
            try {
                val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                val duration = theme.effects.vibrationDurationMs
                    .takeIf { it != 18 }
                    ?.toLong()
                    ?: globalSettings.globalVibrationStrengthMs.toLong()
                val clampedDuration = duration.coerceIn(5L, 80L)

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(clampedDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(clampedDuration)
                    }
                }
            } catch (_: Exception) {
            }
        }

        val soundOn = theme.effects.enableSoundFeedback || globalSettings.globalSoundEnabled
        if (soundOn) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, theme.effects.soundVolume)
            } catch (_: Exception) {
            }
        }
    }
}
