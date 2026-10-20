package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ColorAndImageUtils {

    fun parseColorSafe(hex: String?, defaultColor: Int = -0x1000000): Int {
        if (hex.isNullOrBlank()) return defaultColor
        val cleaned = hex.trim().removePrefix("#")
        return try {
            when (cleaned.length) {
                6 -> (0xFF000000L or cleaned.toLong(16)).toInt()
                8 -> cleaned.toLong(16).toInt()
                else -> defaultColor
            }
        } catch (_: Exception) {
            defaultColor
        }
    }

    fun intToHexString(color: Int, includeAlpha: Boolean = false): String {
        return if (includeAlpha) {
            String.format("#%08X", color)
        } else {
            String.format("#%06X", 0xFFFFFF and color)
        }
    }

    /**
     * Computes WCAG 2.1 contrast ratio between two colors (1.0 to 21.0).
     * Used in Theme Studio to warn users if key labels have low legibility.
     */
    fun calculateContrastRatio(foregroundHex: String, backgroundHex: String): Double {
        val fg = parseColorSafe(foregroundHex, -0x1)
        val bg = parseColorSafe(backgroundHex, -0x1000000)
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun channelLum(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channelLum((color shr 16) and 0xFF)
        val g = channelLum((color shr 8) and 0xFF)
        val b = channelLum(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Safely decodes a downsampled bitmap from a content Uri without OOM on Android 9 devices,
     * applies crop/zoom/rotation/offset, and saves into app-private storage.
     */
    fun importAndProcessBackgroundImage(
        context: Context,
        sourceUri: Uri,
        maxDimensionPx: Int = 1280
    ): Result<String> {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return Result.failure(IllegalArgumentException("Unable to open selected image"))

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return Result.failure(IllegalArgumentException("Selected file is not a valid image"))
            }

            var sampleSize = 1
            while (
                boundsOptions.outWidth / sampleSize > maxDimensionPx ||
                boundsOptions.outHeight / sampleSize > maxDimensionPx
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return Result.failure(IllegalArgumentException("Failed to decode image data"))

            val bgDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
            val outFile = File(bgDir, "bg_${System.currentTimeMillis()}.png")

            FileOutputStream(outFile).use { out ->
                decoded.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            decoded.recycle()

            Result.success(outFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads a background bitmap from app-private path or drawable preset and applies
     * zoom, rotation, offset, brightness, and fast stack blur for the keyboard backdrop.
     */
    fun loadTransformedBackgroundBitmap(
        context: Context,
        customPath: String?,
        presetDrawableName: String?,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
        rotationDegrees: Float,
        offsetXPercent: Float,
        offsetYPercent: Float,
        brightness: Float,
        blurRadius: Int
    ): Bitmap? {
        if (targetWidth <= 0 || targetHeight <= 0) return null
        val rawBitmap: Bitmap = when {
            !customPath.isNullOrBlank() -> {
                val file = File(customPath)
                if (!file.exists() || !file.canRead()) return null
                BitmapFactory.decodeFile(file.absolutePath) ?: return null
            }
            !presetDrawableName.isNullOrBlank() -> {
                val resId = context.resources.getIdentifier(
                    presetDrawableName,
                    "drawable",
                    context.packageName
                )
                if (resId == 0) return null
                BitmapFactory.decodeResource(context.resources, resId) ?: return null
            }
            else -> return null
        }

        return try {
            val outWidth = targetWidth.coerceAtMost(960)
            val outHeight = targetHeight.coerceAtMost(540)
            val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val matrix = Matrix()
            val srcW = rawBitmap.width.toFloat()
            val srcH = rawBitmap.height.toFloat()

            val baseScale = max(outWidth / srcW, outHeight / srcH) * zoom.coerceIn(1f, 3f)
            matrix.postTranslate(-srcW / 2f, -srcH / 2f)
            matrix.postRotate(rotationDegrees)
            matrix.postScale(baseScale, baseScale)
            val dx = (outWidth / 2f) + (offsetXPercent.coerceIn(-0.5f, 0.5f) * outWidth)
            val dy = (outHeight / 2f) + (offsetYPercent.coerceIn(-0.5f, 0.5f) * outHeight)
            matrix.postTranslate(dx, dy)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            if (brightness != 0f) {
                val shift = (brightness.coerceIn(-0.5f, 0.5f) * 255f)
                val cm = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, shift,
                        0f, 1f, 0f, 0f, shift,
                        0f, 0f, 1f, 0f, shift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }

            canvas.drawBitmap(rawBitmap, matrix, paint)
            if (rawBitmap != output) {
                rawBitmap.recycle()
            }

            if (blurRadius > 0) {
                fastBoxBlur(output, blurRadius.coerceIn(1, 20))
            } else {
                output
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lightweight, Android 9 compatible fast downscaled blur that avoids deprecated RenderScript.
     */
    private fun fastBoxBlur(source: Bitmap, radius: Int): Bitmap {
        val scaleFactor = (radius / 3).coerceIn(2, 6)
        val smallW = (source.width / scaleFactor).coerceAtLeast(16)
        val smallH = (source.height / scaleFactor).coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        val blurred = Bitmap.createScaledBitmap(small, source.width, source.height, true)
        if (small != blurred) small.recycle()
        if (source != blurred) source.recycle()
        return blurred
    }
}
