package com.example.theme.importexport

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.fonts.FontManager
import com.example.theme.model.BackgroundSourceType
import com.example.theme.model.CURRENT_THEME_SCHEMA_VERSION
import com.example.theme.model.KeyboardTheme
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@JsonClass(generateAdapter = true)
data class PortableThemePackage(
    val packageFormat: String = "TOXIC_KEYBOARD_THEME_PACKAGE",
    val schemaVersion: Int = CURRENT_THEME_SCHEMA_VERSION,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val includesBackgroundAsset: Boolean = false,
    val backgroundAssetBase64: String? = null,
    val includesCustomFontAsset: Boolean = false,
    val customFontFileName: String? = null,
    val customFontAssetBase64: String? = null,
    val fontLicenseConfirmedByUser: Boolean = false,
    val theme: KeyboardTheme
)

data class ImportedThemePreviewResult(
    val theme: KeyboardTheme,
    val hasEmbeddedBackground: Boolean,
    val hasEmbeddedFont: Boolean,
    val warnings: List<String>
)

object ThemeImportExportManager {

    private const val MAX_IMPORT_BYTES = 10L * 1024L * 1024L // 10 MB max package size
    private const val MAX_ASSET_BYTES = 4L * 1024L * 1024L   // 4 MB max embedded asset

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val packageAdapter = moshi.adapter(PortableThemePackage::class.java).indent("  ")

    /**
     * Exports a KeyboardTheme into a documented, versioned JSON package at the user-chosen SAF Uri.
     * Embeds background image and custom font only if requested and permitted.
     */
    fun exportThemeToUri(
        context: Context,
        theme: KeyboardTheme,
        targetUri: Uri,
        includeBackgroundImage: Boolean,
        includeCustomFontWithLicensePermission: Boolean
    ): Result<String> {
        return try {
            var bgBase64: String? = null
            var hasBg = false
            val bgPath = theme.background.customImagePath
            if (includeBackgroundImage && !bgPath.isNullOrBlank()) {
                val bgFile = File(bgPath)
                if (bgFile.exists() && bgFile.canRead() && bgFile.length() <= MAX_ASSET_BYTES) {
                    bgBase64 = Base64.encodeToString(bgFile.readBytes(), Base64.NO_WRAP)
                    hasBg = true
                }
            }

            var fontBase64: String? = null
            var fontName: String? = null
            var hasFont = false
            val fontPath = theme.textStyle.customFontPath
            if (includeCustomFontWithLicensePermission && !fontPath.isNullOrBlank()) {
                val fontFile = File(fontPath)
                if (fontFile.exists() && fontFile.canRead() && fontFile.length() <= MAX_ASSET_BYTES) {
                    fontBase64 = Base64.encodeToString(fontFile.readBytes(), Base64.NO_WRAP)
                    fontName = fontFile.name
                    hasFont = true
                }
            }

            // Strip device-local absolute paths from the exported metadata
            val portableTheme = theme.copy(
                isBuiltIn = false,
                isDefault = false,
                background = theme.background.copy(customImagePath = null),
                textStyle = theme.textStyle.copy(customFontPath = null)
            ).validated()

            val pkg = PortableThemePackage(
                schemaVersion = CURRENT_THEME_SCHEMA_VERSION,
                includesBackgroundAsset = hasBg,
                backgroundAssetBase64 = bgBase64,
                includesCustomFontAsset = hasFont,
                customFontFileName = fontName,
                customFontAssetBase64 = fontBase64,
                fontLicenseConfirmedByUser = includeCustomFontWithLicensePermission,
                theme = portableTheme
            )

            val json = packageAdapter.toJson(pkg)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return Result.failure(IllegalArgumentException("Could not open destination file for writing."))

            val assetNote = buildString {
                append("Exported '${theme.name}'")
                if (hasBg) append(" + background image")
                if (hasFont) append(" + custom font")
                if (!hasBg && theme.background.sourceType == BackgroundSourceType.CUSTOM_IMAGE) {
                    append(" (Note: custom background image was omitted; recipient will see fallback gradient)")
                }
            }
            Result.success(assetNote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses and validates an imported theme file from SAF Uri without applying it yet,
     * allowing the user to preview and confirm before importing.
     */
    fun parseAndValidateImportUri(
        context: Context,
        sourceUri: Uri
    ): Result<ImportedThemePreviewResult> {
        return try {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                var total = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    total += read
                    if (total > MAX_IMPORT_BYTES) {
                        return Result.failure(
                            IllegalArgumentException("Theme file exceeds 10 MB safety limit.")
                        )
                    }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            } ?: return Result.failure(IllegalArgumentException("Unable to read selected theme file."))

            val rawJson = String(bytes, Charsets.UTF_8)
            parseAndValidateJsonString(context, rawJson)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Corrupt or unreadable theme file: ${e.message}"))
        }
    }

    fun parseAndValidateJsonString(
        context: Context,
        rawJson: String
    ): Result<ImportedThemePreviewResult> {
        return try {
            val pkg = packageAdapter.fromJson(rawJson)
                ?: return Result.failure(IllegalArgumentException("Invalid theme JSON structure."))

            if (pkg.packageFormat != "TOXIC_KEYBOARD_THEME_PACKAGE") {
                return Result.failure(
                    IllegalArgumentException("Unrecognized package format: ${pkg.packageFormat}")
                )
            }

            if (pkg.schemaVersion < 1 || pkg.schemaVersion > CURRENT_THEME_SCHEMA_VERSION) {
                return Result.failure(
                    IllegalArgumentException(
                        "Unsupported theme schema version (${pkg.schemaVersion}). Max supported is $CURRENT_THEME_SCHEMA_VERSION."
                    )
                )
            }

            val warnings = mutableListOf<String>()
            var extractedBgPath: String? = null
            if (pkg.includesBackgroundAsset && !pkg.backgroundAssetBase64.isNullOrBlank()) {
                try {
                    val decoded = Base64.decode(pkg.backgroundAssetBase64, Base64.DEFAULT)
                    if (decoded.size <= MAX_ASSET_BYTES) {
                        val bgDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
                        val safeFile = File(bgDir, "imported_bg_${System.currentTimeMillis()}.png")
                        // Prevent path traversal by verifying canonical parent
                        if (safeFile.canonicalFile.parentFile == bgDir.canonicalFile) {
                            FileOutputStream(safeFile).use { it.write(decoded) }
                            extractedBgPath = safeFile.absolutePath
                        }
                    } else {
                        warnings.add("Embedded background image exceeded 4 MB and was skipped.")
                    }
                } catch (_: Exception) {
                    warnings.add("Embedded background image could not be decoded; using fallback color.")
                }
            }

            var extractedFontPath: String? = null
            if (pkg.includesCustomFontAsset && !pkg.customFontAssetBase64.isNullOrBlank()) {
                try {
                    val decoded = Base64.decode(pkg.customFontAssetBase64, Base64.DEFAULT)
                    if (decoded.size <= MAX_ASSET_BYTES) {
                        val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
                        // Sanitize filename to prevent path traversal ("../")
                        val safeExt = if (pkg.customFontFileName?.endsWith(".otf", true) == true) ".otf" else ".ttf"
                        val safeFontFile = File(fontsDir, "imported_font_${System.currentTimeMillis()}$safeExt")
                        if (safeFontFile.canonicalFile.parentFile == fontsDir.canonicalFile) {
                            FileOutputStream(safeFontFile).use { it.write(decoded) }
                            if (FontManager.isValidFontHeader(safeFontFile)) {
                                extractedFontPath = safeFontFile.absolutePath
                            } else {
                                safeFontFile.delete()
                                warnings.add("Embedded font failed TrueType/OpenType verification and was rejected.")
                            }
                        }
                    }
                } catch (_: Exception) {
                    warnings.add("Embedded custom font could not be loaded; using system fallback font.")
                }
            }

            val now = System.currentTimeMillis()
            val sanitizedTheme = pkg.theme.copy(
                id = "theme_${UUID.randomUUID()}",
                isBuiltIn = false,
                isDefault = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                lastUsedAtEpochMs = now,
                background = pkg.theme.background.copy(
                    customImagePath = extractedBgPath,
                    sourceType = if (extractedBgPath != null) BackgroundSourceType.CUSTOM_IMAGE else pkg.theme.background.sourceType
                ),
                textStyle = pkg.theme.textStyle.copy(
                    customFontPath = extractedFontPath
                )
            ).validated()

            Result.success(
                ImportedThemePreviewResult(
                    theme = sanitizedTheme,
                    hasEmbeddedBackground = extractedBgPath != null,
                    hasEmbeddedFont = extractedFontPath != null,
                    warnings = warnings
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Theme validation failed: ${e.message}"))
        }
    }
}
