# Isaac Item Scanner — Rebuild Status & Handoff

**Goal:** an Android app that identifies *The Binding of Isaac* items shown on a TV/console
screen by pointing a phone (Pixel 10 Pro) at them, tells you the item's stats and whether
to take it given your current run, and works **fully offline** (on-device OCR + a bundled
~700-item database), with Google Gemini as an *optional* booster for AI verdicts and
sprite-only identification.

Owner: Kenny (GitHub `Kmill26`). Repo: `Kmill26/Isaac-scanner-app`, branch `main`.

---

## Target architecture

1. **Bundled offline item DB** — `app/src/main/assets/isaac_items.json`, every collectible
   (+ trinkets) with name / id / quality / type / pools / effect / transformations.
   Loaded into memory at startup; powers the Compendium and all name-matching.
2. **On-device identification (no network, no key):** capture a frame → crop to the
   on-screen reticle → **ML Kit text recognition** reads the item-name banner Isaac shows
   on the pedestal/pickup → fuzzy-match the text to the bundled DB → show the result.
3. **Gemini (optional):** only when a key is configured. Used for (a) the run-aware
   "should I take this?" verdict, (b) fallback identification when OCR finds no readable
   name (sprite only). Structured JSON output. When no key: clean "add a key to enable AI
   verdicts" state — **never a fake/random result**.
4. **Run tracking:** current-run items persist across process death; synergies +
   transformations computed from the bundled DB.

---

## Progress log

### DONE — P0: build + CI pipeline (repo→APK)
- Added Gradle wrapper: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
  (fetched from `github.com/gradle/gradle` tag `v9.3.1`, checksums verified against the
  synthesis plan / release notes).
- `debug.keystore` generated and **committed** (removed from `.gitignore`), so CI and local
  debug APKs share one signature — reinstall over the top, no uninstall needed. Password is
  the universal `android`/`android`; no security value.
- `app/build.gradle.kts`:
  - `compileSdk { version = release(36) }` (was `release(36){ minorApiLevel = 1 }` — needed
    a 36.1 SDK package for no benefit; app uses no 36.1 APIs).
  - Removed `com.google.gms.google-services` plugin + all Firebase deps
    (`firebase-ai`, `firebase-appcheck-*`, `firebase-bom`) — never referenced in code, and a
    startup-crash risk with no `google-services.json`. App calls Gemini via raw Retrofit.
  - Release signing config is now guarded (`System.getenv("STORE_PASSWORD") != null`) so
    `assembleRelease` no longer NPEs without secrets; falls back to debug signing locally.
  - Added deps (for the rebuild): `mlkit-text-recognition:16.0.1`,
    `kotlinx-coroutines-play-services`, `androidx-datastore-preferences` (un-commented).
- Root `build.gradle.kts`: dropped the `google-services` plugin alias.
- `gradle.properties`: dropped `googleServices.missing.passthrough`.
- `.github/workflows/blank.yml` → **`.github/workflows/build.yml`**: JDK 21,
  `android-actions/setup-android@v3` (accepts licenses, installs platform 36 + build-tools),
  `gradle/actions/setup-gradle@v4`, writes `.env` from optional `secrets.GEMINI_API_KEY`
  (defaults to `NONE`), `./gradlew :app:assembleDebug`, uploads `isaac-scanner-debug` artifact.
- `.env.example` → `GEMINI_API_KEY=NONE` (was a fake placeholder that the old code treated
  as a real key and silently entered random-guess mode). Still generates
  `BuildConfig.GEMINI_API_KEY` via the secrets plugin.
- Deleted broken `GreetingScreenshotTest.kt` (referenced a non-existent `Greeting`
  composable → whole test source set failed to compile) + its screenshot. Fixed
  `ExampleRobolectricTest` expected string to `"Isaac Item Scanner"`.

**Local build:** `assembleDebug` — baseline (pre-fix) failed only at `validateSigningDebug`.
Post-fix build status: <PENDING — see build2.log>.

### IN PROGRESS
- **Item dataset:** a research subagent is sourcing/normalizing the full item list into
  `app/src/main/assets/isaac_items.json` (target 600–740 collectibles). Not yet committed.

### TODO — the rebuild (ordered)
See `/private/tmp/.../scratchpad/synth-plan.md` for the full analysis; condensed:

1. **Data layer**
   - `IsaacItemDatabase` → load from `assets/isaac_items.json` (keep the same public API:
     `items`, `findItemByName`, `calculateSynergies`, `calculateTransformations`,
     `getXboxPresets`). Parse with Moshi. Cache in a singleton / repository.
   - `findItemByName`: word-boundary / token matching, ranked exact > token-equals > alias;
     return a match-confidence so the UI can say "closest match" vs "exact". Current
     substring match maps "Technology" → "Tech X" etc.
   - Keep the 27 hand-authored synergy entries as an overlay on top of the bundled data.
2. **OCR identification** (new `data/ocr/` package)
   - `TextRecognizer` (ML Kit, Latin script, bundled). `suspend fun readItemName(bitmap): String?`
   - Isaac's pedestal/pickup banner shows the item name in caps. OCR the reticle crop,
     take the largest/most-central text block, normalise, fuzzy-match to the DB
     (Levenshtein / token-set ratio, threshold ~0.8).
   - Flow: `ScannerViewModel.scanBitmap` → OCR first (offline, ~200ms). If matched with
     good confidence → show result immediately, no network. If not → if key present, call
     Gemini vision; else show "couldn't read a name — move closer / fill the box / add an
     API key for AI recognition".
3. **Camera capture quality** (`CameraViewfinder.kt`) — the single biggest accuracy lever
   - Crop the captured bitmap to the reticle rectangle before OCR/upload (shared constant
     with the Canvas overlay). Currently the whole frame is sent and the sprite is ~30px.
   - `imageProxyToBitmap`: handle `null` decode, `Config.HARDWARE` bitmaps (copy to
     ARGB_8888), surface capture errors instead of `printStackTrace()`.
   - Zoom: `setZoomRatio` bounded to `maxZoomRatio.coerceAtMost(8f)` (was `setLinearZoom`
     mapping 1–5 onto the sensor's full 0.5–100x range). Add pinch-to-zoom.
   - Tap-to-focus + spot metering on the reticle; EV slider / "TV mode" (negative EV) for
     bright emissive screens. Replace the torch button (reflections ruin screen scans).
   - Bump `bitmapToBase64` to 1024px / JPEG 90 now that it's a tight crop.
4. **Gemini service** (`data/gemini/`)
   - Model `gemini-2.5-flash` (current), path `v1beta/models/{model}:generateContent`,
     key as `x-goog-api-key` header (not `?key=`), `HttpLoggingInterceptor` only in
     `BuildConfig.DEBUG`.
   - Structured output: `responseMimeType=application/json` + `responseSchema`,
     `thinkingConfig.thinkingBudget=0`. Parse a typed `ScanPayload`.
   - **Delete `fallbackLocalScan()`** and the `?: items.first()` coalesce. Throw a typed
     `ScanException` (NoApiKey / NoItemDetected / RateLimited / ServerError / Network /
     BadResponse); `ScannerViewModel` already catches → `scanErrorMessage`. Render it.
   - `Response<GenerateContentResponse>` so error bodies are readable; map 429/5xx; one
     retry on 429/503 honouring `RetryInfo`.
5. **UI / navigation / insets**
   - `ScannerUiState.currentRunItems` default `emptyList()` (was a fake seeded "Soy Milk").
   - Persist active run (tiny Room table or DataStore) across process death; "Resume run?"
     on cold start.
   - `MainActivity`: `BackHandler` to return to tab 0 instead of exiting;
     `enableEdgeToEdge` with transparent bars + `statusBarsPadding()` on each screen root;
     remove `contentWindowInsets = WindowInsets(0,0,0,0)` and the hardcoded top spacers.
   - `ScanResultCard` capped ~45% height + scroll so the viewfinder stays visible; add a
     one-tap "Rescan".
   - Button contrast (dark maroon on crimson ≈ 3:1 everywhere) — drop the `contentColor`
     overrides, let Material use `onPrimary` (already white).
   - `items(list, key = { it.id })` on every lazy list. Empty state in the Compendium when
     filters match nothing. Remove impossible filter chips.
   - Permission flow: don't auto-launch on first composition; "Open Settings" path on
     permanent denial; re-check on `ON_RESUME`.
   - Gate the "Xbox preset" test scaffolding + `createSyntheticConsoleBitmap` behind
     `BuildConfig.DEBUG`.
   - Wire or delete the dead `isAutoScanEnabled` toggle (recommend delete).
6. **Housekeeping** — drop `KotlinJsonAdapterFactory`/kotlin-reflect (codegen already
   wired), real Room migrations (or `fallbackToDestructiveMigrationFrom` pre-release only),
   remove deprecated `Divider` import, fill in `Type.kt` typography, ≥48dp touch targets,
   `contentDescription` on emoji icons.

### Distribution
- Kenny is installing Android Studio (macOS, `brew install --cask android-studio`, done) +
  SDK platform 36. Then USB-installs to the Pixel via Run.
- CI also produces a sideloadable `app-debug.apk` on every push to `main` (Actions →
  "Build Debug APK" → artifact `isaac-scanner-debug`).
- Optional: add `GEMINI_API_KEY` repo secret (Settings → Secrets and variables → Actions)
  to bake a key into CI builds. Free key: https://aistudio.google.com/apikey . Note: a
  "Gemini Ultra" consumer subscription does **not** include API access — separate key.

---

## Local build commands

```bash
cd /Users/kennymiller/Documents/Claude/Projects/Isaac-scanner-app
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug --no-daemon --console=plain
# APK: app/build/outputs/apk/debug/app-debug.apk
# Install to a USB-connected phone:
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Toolchain: AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.10, JBR Java 25 (local) / Temurin 21 (CI),
compileSdk/targetSdk 36, minSdk 24. There is **no** system JDK — use the Android Studio JBR.

## Handoff to Codex

If picking this up cold: the P0 build work is committed and CI is green (verify: Actions
tab). Start from **TODO → the rebuild**, in order. The item dataset in
`app/src/main/assets/isaac_items.json` is the foundation — confirm it exists and is
complete (`python3 -m json.tool`, count `collectibles`) before wiring the data layer.
Keep changes compiling at each step (`./gradlew :app:assembleDebug`); push to `main` in
small commits so CI stays a useful signal. Namespace is still `com.example` — a rename to
`com.kmill26.isaacscanner` is optional polish, do it last if at all (touches ~25 files +
tests + manifest).
