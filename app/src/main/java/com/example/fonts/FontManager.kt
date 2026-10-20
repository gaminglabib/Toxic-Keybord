package com.example.fonts

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.theme.model.BuiltInFontFamily
import com.example.theme.model.TextStyleConfig
import java.io.File
import java.io.FileOutputStream

data class ImportedFontInfo(
    val id: String,
    val displayName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val importedAtMs: Long
)

object FontManager {

    private const val MAX_FONT_SIZE_BYTES = 8L * 1024L * 1024L // 8 MB max safety limit
    private val typefaceCache = mutableMapOf<String, Typeface>()

    /**
     * Validates a .ttf or .otf file from SAF Uri using magic header bytes and Android Typeface parser,
     * then copies it into app-private storage (filesDir/fonts).
     */
    fun importCustomFont(
        context: Context,
        uri: Uri,
        suggestedName: String
    ): Result<ImportedFontInfo> {
        return try {
            val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
            val cleanName = suggestedName
                .replace(Regex("[^a-zA-Z0-9._ -]"), "")
                .trim()
                .ifEmpty { "custom_font_${System.currentTimeMillis()}.ttf" }

            val ext = when {
                cleanName.endsWith(".otf", ignoreCase = true) -> ".otf"
                else -> ".ttf"
            }
            val fontId = "custom_${System.currentTimeMillis()}"
            val targetFile = File(fontsDir, "$fontId$ext")

            var totalBytes = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        totalBytes += read
                        if (totalBytes > MAX_FONT_SIZE_BYTES) {
                            targetFile.delete()
                            return Result.failure(
                                IllegalArgumentException("Font file exceeds 8 MB limit.")
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return Result.failure(IllegalArgumentException("Unable to read selected font file."))

            if (!isValidFontHeader(targetFile)) {
                targetFile.delete()
                return Result.failure(
                    IllegalArgumentException("Invalid font format. Only authentic .ttf and .otf files are supported.")
                )
            }

            // Verify Android's native Typeface engine can parse the font safely
            val testTypeface = try {
                Typeface.createFromFile(targetFile)
            } catch (e: Exception) {
                targetFile.delete()
                return Result.failure(IllegalArgumentException("Corrupt or unsupported font file."))
            }

            if (testTypeface == null || testTypeface == Typeface.DEFAULT) {
                targetFile.delete()
                return Result.failure(IllegalArgumentException("Android could not load this font file."))
            }

            typefaceCache[targetFile.absolutePath] = testTypeface

            val displayTitle = cleanName.removeSuffix(".ttf").removeSuffix(".TTF")
                .removeSuffix(".otf").removeSuffix(".OTF")
                .take(36)

            Result.success(
                ImportedFontInfo(
                    id = fontId,
                    displayName = displayTitle,
                    filePath = targetFile.absolutePath,
                    fileSizeBytes = totalBytes,
                    importedAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks TrueType (00 01 00 00 or 'true') and OpenType ('OTTO') magic headers.
     */
    fun isValidFontHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 12) return false
        return try {
            val header = ByteArray(4)
            file.inputStream().use {
                if (it.read(header) != 4) return false
            }
            val b0 = header[0].toInt() and 0xFF
            val b1 = header[1].toInt() and 0xFF
            val b2 = header[2].toInt() and 0xFF
            val b3 = header[3].toInt() and 0xFF

            val isTtf = (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00) ||
                (b0 == 't'.code && b1 == 'r'.code && b2 == 'u'.code && b3 == 'e'.code)
            val isOtf = (b0 == 'O'.code && b1 == 'T'.code && b2 == 'T'.code && b3 == 'O'.code)
            isTtf || isOtf
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resolves an Android native Typeface for Canvas / InputMethodService rendering.
     * Automatically falls back to system Sanskrit/Bengali-capable Sans-Serif when rendering
     * Bengali Unicode text so Latin-only custom fonts never break Bengali conjuncts.
     */
    fun resolveTypeface(
        context: Context,
        textStyle: TextStyleConfig,
        isBengaliScript: Boolean = false
    ): Typeface {
        val styleInt = when {
            textStyle.fontWeight >= 700 && textStyle.isItalic -> Typeface.BOLD_ITALIC
            textStyle.fontWeight >= 700 -> Typeface.BOLD
            textStyle.isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        if (isBengaliScript) {
            return Typeface.create(Typeface.SANS_SERIF, styleInt)
        }

        val customPath = textStyle.customFontPath
        if (!customPath.isNullOrBlank()) {
            val cached = typefaceCache[customPath]
            if (cached != null) {
                return Typeface.create(cached, styleInt)
            }
            val file = File(customPath)
            if (file.exists() && file.canRead()) {
                try {
                    val loaded = Typeface.createFromFile(file)
                    if (loaded != null) {
                        typefaceCache[customPath] = loaded
                        return Typeface.create(loaded, styleInt)
                    }
                } catch (_: Exception) {
                    // Safe fallback below
                }
            }
        }

        val baseTypeface = when (textStyle.fontFamilyId) {
            BuiltInFontFamily.SERIF.id -> Typeface.SERIF
            BuiltInFontFamily.MONOSPACE.id -> Typeface.MONOSPACE
            BuiltInFontFamily.SPACE_GROTESK.id -> loadResFontSafe(context, R.font.space_grotesk)
            BuiltInFontFamily.JETBRAINS_MONO.id -> loadResFontSafe(context, R.font.jetbrains_mono)
            BuiltInFontFamily.ORBITRON.id -> loadResFontSafe(context, R.font.orbitron)
            BuiltInFontFamily.COMFORTAA.id -> loadResFontSafe(context, R.font.comfortaa)
            else -> Typeface.SANS_SERIF
        }
        return Typeface.create(baseTypeface, styleInt)
    }

    private fun loadResFontSafe(context: Context, fontResId: Int): Typeface {
        return try {
            ResourcesCompat.getFont(context, fontResId) ?: Typeface.SANS_SERIF
        } catch (_: Exception) {
            Typeface.SANS_SERIF
        }
    }

    fun resolveComposeFontFamily(textStyle: TextStyleConfig): FontFamily {
        val weight = when {
            textStyle.fontWeight >= 700 -> FontWeight.Bold
            textStyle.fontWeight >= 500 -> FontWeight.Medium
            else -> FontWeight.Normal
        }
        val style = if (textStyle.isItalic) FontStyle.Italic else FontStyle.Normal

        return when (textStyle.fontFamilyId) {
            BuiltInFontFamily.SERIF.id -> FontFamily.Serif
            BuiltInFontFamily.MONOSPACE.id -> FontFamily.Monospace
            BuiltInFontFamily.SPACE_GROTESK.id -> FontFamily(Font(R.font.space_grotesk, weight, style))
            BuiltInFontFamily.JETBRAINS_MONO.id -> FontFamily(Font(R.font.jetbrains_mono, weight, style))
            BuiltInFontFamily.ORBITRON.id -> FontFamily(Font(R.font.orbitron, weight, style))
            BuiltInFontFamily.COMFORTAA.id -> FontFamily(Font(R.font.comfortaa, weight, style))
            else -> FontFamily.SansSerif
        }
    }
}
