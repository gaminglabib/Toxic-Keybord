# TOXIC KEYBOARD STUDIO

**TOXIC KEYBOARD STUDIO** is a local-first custom Android keyboard theme studio and native English + Bengali (`বাংলা`) `InputMethodService` built with Kotlin, Jetpack Compose, Room, and a custom hardware-accelerated Canvas keyboard engine compatible with Android 9 (API 28, vivo Y19 / vivo 1915) and newer devices.

---

## 1. Architecture & Package Organization

All source code lives under `app/src/main/java/com/example/`:

- `MainActivity.kt` — Adaptive Studio host with bottom navigation (compact phones) and navigation rail (tablets/landscape), SAF import/export launchers, and lifecycle-aware IME status refresh.
- `keyboard/service/ToxicInputMethodService.kt` — Real Android `InputMethodService` implementation bound to `@xml/method`.
- `keyboard/ui/NativeKeyboardRendererView.kt` — Custom Android `View` rendering engine shared between the real system keyboard and the Studio Live Preview for 100% visual parity.
- `keyboard/layout/KeyboardLayouts.kt` — Validated layout definitions for English QWERTY, Bengali Unicode (`স্বরবর্ণ`, `কার`, `হসন্ত/যুক্তাক্ষর`, `ব্যঞ্জনবর্ণ`), primary & secondary Symbol pages, number row, and Emoji grid.
- `keyboard/actions/KeyboardActionHandler.kt` — Cursor-aware text insertion, selection-aware UTF-16 codepoint deletion, suggestion replacement, and editor action dispatch (`Search`, `Send`, `Go`, `Next`, `Done`, multiline `\n`).
- `keyboard/state/KeyboardState.kt` — Immutable state model for shift (`UNSHIFTED`, `SHIFTED_SINGLE`, `CAPS_LOCK`), language (`ENGLISH`, `BENGALI`), keyboard modes, and `EditorFieldContext` password/sensitive field detection.
- `theme/model/KeyboardThemeModel.kt` & `DefaultThemes.kt` — Strongly typed theme schema, category & per-key overrides, 5 built-in presets, and the offline natural-language Theme Synthesizer.
- `theme/editor/ThemeStudioScreen.kt` — Full custom theme editor (Background Editor, Individual & Category Key Styling, Effects & Audio, Undo/Redo, Save/Save As/Rename/Duplicate/Restore/Reset, and Interactive Key Picker).
- `theme/storage/ThemeRepository.kt` & `data/LocalEntitiesAndDatabase.kt` — Room persistence (`StudioDatabase`) + SharedPreferences + `ActiveKeyboardBridge` for instant live synchronization between Studio and `ToxicInputMethodService`.
- `theme/importexport/ThemeImportExportManager.kt` — Versioned `.tks.json` package exporter/importer with schema validation, size checks, path traversal protection, and optional Base64 asset bundling.
- `fonts/FontManager.kt` — Bundled fonts (`Space Grotesk`, `JetBrains Mono`, `Orbitron`, `Comfortaa`, system fonts) + SAF `.ttf`/`.otf` custom font validation and automatic Bengali Unicode fallback.
- `ui/screens/` — `HomeDashboardScreen.kt`, `TextStyleStudioScreen.kt`, `LayoutStudioScreen.kt`, `MyThemesScreen.kt`, `SettingsAndPrivacyScreen.kt`.
- `util/` — `ColorAndImageUtils.kt` (WCAG contrast calculator, downsampled bitmap importer, crop/zoom/rotate/brightness/blur processor) and `ImeStatusHelper.kt`.

---

## 2. Build & Installation Instructions

### Standard Android Studio / Command Line Build
```bash
# Compile debug APK
gradle assembleDebug

# Run local JVM unit & Robolectric tests
gradle :app:testDebugUnitTest
```
The compiled debug APK is output to:
`app/build/outputs/apk/debug/app-debug.apk`

### Building on ARM64 Android / Termux Notes
- Ensure an ARM64-compatible JDK 17+ (`openjdk-17`) and Android SDK command-line tools (`platforms;android-36`, `build-tools`) are installed.
- If building inside Termux on ARM64 where x86_64 AAPT2 binaries fail, pass `-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` (after installing Termux's native `aapt2` package).

---

## 3. Enabling & Selecting TOXIC Keyboard on Android 9 (vivo Y19 / Funtouch OS & Stock Android)

1. Open **TOXIC KEYBOARD STUDIO**.
2. On the **Home Dashboard**, check the **System Keyboard Activation** card:
   - Tap **1. Enable Keyboard** -> This opens Android's *Available Virtual Keyboards* settings (`Settings.ACTION_INPUT_METHOD_SETTINGS`). Toggle ON **TOXIC Keyboard** and confirm the standard Android system prompt.
   - Return to the app (the status automatically refreshes on resume).
   - Tap **2. Select Default** -> This opens the system Input Method Picker (`InputMethodManager.showInputMethodPicker()`). Choose **TOXIC Keyboard**.
3. Tap the **"Tap here to summon active system keyboard"** field on the Dashboard (or open any messaging/notes app) to type with your custom theme!

---

## 4. Implemented Features & Known Technical Limitations

### Implemented
- Real `InputMethodService` with English QWERTY, Bengali Unicode (`কার` vowel signs with long-press independent vowels, `্` Hasant for conjuncts, shifted vowels & `ক্ষ/জ্ঞ`), Number Row toggle, 2 Symbol pages, Emoji panel, and opt-in Clipboard drawer.
- Complete Theme Studio with gallery photo import (Android Photo Picker + SAF fallback), non-destructive zoom/rotate/pan/brightness/dimming/opacity/blur, category & individual per-key customization, WCAG contrast legibility ratio checker, and full Undo/Redo stack.
- Text Style Studio with bundled Google Fonts, validated `.ttf`/`.otf` SAF import, weight/italic/size/spacing/shadow/outline/glow controls, and automatic Bengali Unicode fallback so Latin-only custom fonts never break Bengali rendering.
- Portable `.tks.json` theme export/import with preview confirmation modal.
- Zero-network privacy architecture (`INTERNET` permission is not requested; password/sensitive fields automatically disable dictionary learning and clipboard recording).

### Known Limitations & Future Roadmap
- **External App Font Rendering**: Custom fonts selected in Text Style Studio style the keyboard's own keycaps, popups, and toolbar. Committed text in third-party apps is rendered using standard Unicode codepoints in the target app's own font.
- **Bengali Layout vs Full Phonetic Transliteration**: Provides a direct, reliable Bengali Unicode character + vowel-sign (`কার`) + conjunct (`্`) layout with local prefix dictionary suggestions. Full Avro-style multi-syllable phonetic English-to-Bengali transliteration is left as an extensible future module.
- **AI Theme Creator & Marketplace**: Uses a 100% offline rule-based natural-language prompt synthesizer and local `.tks.json` sharing in this offline-first release; no remote servers or API keys are embedded.
