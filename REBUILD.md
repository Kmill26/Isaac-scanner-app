# Isaac Item Scanner — Rebuild Status & Handoff

**Goal:** an Android app that identifies *The Binding of Isaac* items shown on a TV/console
screen by pointing a phone (Pixel 10 Pro) at them, tells you the item's stats and whether
to take it given your current run, and works **fully offline** (on-device OCR + a bundled
~700-item database), with Google Gemini as an *optional* booster for AI verdicts and
sprite-only identification.

Owner: Kenny (GitHub `Kmill26`). Repo: `Kmill26/Isaac-scanner-app`, branch `main`.

**Status:** rebuild fully implemented and merged. `main` passes `:app:assembleDebug`,
`:app:testDebugUnitTest` (17), `:app:lintDebug` (0 errors), and **CI is green**.

**Emulator smoke test (API 36, Pixel 7 AVD) — PASSED:** cold launch, all four tabs render,
721-item catalog loads, camera binds + captures + crops, **ML Kit OCR runs on-device end to
end**, a no-item frame produces the honest "couldn't read it" banner + Rescan (never a fake
result), Compendium search + tier/pool filters + detail sheet all work. Zero crashes across
extensive navigation. What the emulator can't tell us: real OCR hit-rate on photos of an
actual TV, and the Gemini paths (no key in this checkout). See "Handoff" at the bottom.

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
Post-fix build status: **BUILD SUCCESSFUL** (clean `:app:assembleDebug`). CI green.

### DONE — the rebuild (P1–P6, all merged to `main`)

The whole rebuild described in the old TODO below is **implemented and committed**. Every
phase ends on a green `./gradlew :app:assembleDebug :app:testDebugUnitTest`; the final
state also passes `:app:lintDebug` with 0 errors. 17 unit tests (`CatalogTest`,
`ScanEngineTest`, 2 example). Commits `4db0d43` (P1) → P6.

- **P1 — Data layer.** `IsaacItemDatabase` is now a façade over `assets/isaac_items.json`
  (721 collectibles) parsed with codegen Moshi in `data/catalog/`. Public API unchanged
  (`items`, `findItemByName`, `calculateSynergies`, `calculateTransformations`,
  `getXboxPresets`) + new `match()` / `itemById()`. `NameMatcher`: ranked exact >
  token-containment > Levenshtein/token-set ≥ 0.82 > curated alias map; returns `null`
  rather than an arbitrary item. The 27 hand-authored synergies are overlaid by name.
  Catalog is warmed on a daemon thread from `IsaacApp.onCreate` (registered in the manifest).
- **P2 — Scan engine.** `data/ocr/ItemTextRecognizer` (bundled ML Kit Latin model, no Play
  Services) + `data/scan/ScanEngine` (OCR-first, Gemini demoted to optional fallback).
  `identify()` returns a sealed `ScanOutcome` (`Identified` / `Unrecognized` /
  `NeedCloserLook` / `Failed`). Gemini rewritten: `gemini-2.5-flash`, `x-goog-api-key`
  header, structured `responseSchema`, `thinkingBudget=0`, `Response<…>` error bodies,
  one retry on 429/503 honouring `RetryInfo`, typed `ScanException`. **No `fallbackLocalScan`,
  no `items.random()` / `items.first()` coalesce anywhere.** `moshi-kotlin` (reflection) removed.
- **P3 — Camera.** Capture is cropped to the shared `ScanReticle` rectangle before OCR.
  `imageProxyToBitmap` handles null decode + `Config.HARDWARE` (guarded ≥ API 26) + rotation;
  capture failures surface through an `onCaptureError` callback (main thread) instead of
  `printStackTrace`. Real `setZoomRatio` bounded to `maxZoomRatio` (≤ 8×) + pinch-to-zoom,
  tap-to-focus/metering, "TV mode" negative-EV toggle (torch removed). `UseCaseGroup` +
  `ViewPort` so the capture buffer matches the preview crop.
- **P4 — Wiring.** `ScannerViewModel.scanBitmap` drives a real state machine off
  `ScanOutcome`; every branch has a user-facing message and no branch shows a fabricated
  item. On-demand "Get AI verdict" button (hidden unless a real key is configured). Active
  run persists across process death via `data/prefs/RunStore` (DataStore, CSV of catalog
  ids) with a "Resume run? / Start fresh" banner on cold start. `scanXboxPreset` resolves
  through the catalog or errors — the synthetic-bitmap history write and
  `createSyntheticConsoleBitmap` are gone.
- **P5 — UI polish.** `BackHandler` → tab 0; `enableEdgeToEdge` transparent bars +
  `statusBarsPadding()` per screen (no more `WindowInsets(0,0,0,0)` / hardcoded spacers);
  real `Type.kt` typography; nav-bar contrast fixed (`onPrimaryContainer`); debounced
  Compendium search + empty state + keyed lazy lists everywhere; permission flow no longer
  auto-launches, has an "Open Settings" path on permanent denial and re-checks on
  `ON_RESUME`; Xbox preset bar gated behind `BuildConfig.DEBUG`; dead
  `isAutoScanEnabled` / `torch` / `zoom` VM members deleted. `fallbackToDestructiveMigration`
  kept with a `// TODO real migrations before release`.
- **P6 — Review / hardening.** Static review of the full P0→HEAD diff. Fixed two `NewApi`
  lint errors that were latent crashes/no-ops on minSdk-24 devices: `Bitmap.Config.HARDWARE`
  (guarded with an SDK check) and `android:windowLightNavigationBar` in `themes.xml`
  (removed — redundant with the runtime `enableEdgeToEdge` call). Added `match()` OCR-noise
  unit tests.
- **P7 — Emulator pass (API 36 AVD).** Installed the emulator + system image, smoke-tested
  the real APK (see Status). Fixes from what it surfaced: memoized the Compendium filter
  (`remember(query, filters)` — it was re-filtering 721 items every recomposition); pushed
  the viewfinder top HUD below the status bar and the run pill below the HUD so the
  zoom/EV buttons aren't covered. Audited catalog `quality` — it's an exact match to the
  game's `items_metadata.xml`, so left as-is (details under Handoff).

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

## How to test on the Pixel 10 Pro

1. **Android Studio → Run.** Open the project, plug the Pixel in over USB (USB debugging
   on), pick it as the target, hit Run. The committed `debug.keystore` means reinstalls
   don't need an uninstall.
2. **Or sideload the CI artifact.** GitHub → Actions → latest "Build Debug APK" run →
   download `isaac-scanner-debug` → unzip → `adb install -r app-debug.apk`.
3. **What to actually check on-device** (none of this is covered by unit tests or an
   emulator — the camera + ML Kit + Gemini paths are all real-hardware-only):
   - Cold launch → scanner tab renders, no ANR, catalog-backed Compendium fills.
   - Grant camera → viewfinder preview is live, reticle centered, pinch-zoom + tap-focus
     + "TV mode" all respond.
   - Point at a real Isaac pedestal on the TV, line the **item-name banner** up inside the
     box, tap scan → OCR should resolve the name offline in ~1 s. Try 10–15 different items.
   - Deny camera permanently → "Open Settings" button; re-grant → viewfinder returns on
     resume without a restart.
   - Add items to a run, force-stop the app, relaunch → "Resume run?" banner shows the
     right count.

### Gemini key (optional — the app is fully functional offline without it)

- Free key: <https://aistudio.google.com/apikey>. A **Google AI Ultra / "Gemini" consumer
  subscription does NOT include API access** — it's a separate, free API key.
- Local: put `GEMINI_API_KEY=...` in a `.env` at the repo root (the secrets-gradle-plugin
  bakes it into `BuildConfig`). CI: add a `GEMINI_API_KEY` repo secret (Settings → Secrets
  and variables → Actions). With no key, `BuildConfig.GEMINI_API_KEY == "NONE"`, every AI
  affordance is hidden, and the offline OCR path is the whole app.

## Handoff to Codex

The rebuild (P1–P6) is **done and merged**; `main` builds, tests, and lints clean. This is
a real offline scanner now, not a demo. What's solid vs. what still needs work:

**Solid / trust it:**
- Data layer + name matching (`NameMatcher`, `IsaacItemDatabase`) — unit-tested against the
  full 721-item catalog with noisy inputs.
- `ScanEngine` decision flow — unit-tested with injected fakes (`ScanEngineTest`).
- No path anywhere shows a fake/random item; every failure has a specific message.
- Gemini service structure (schema, retries, typed errors) — code-reviewed, not yet run
  against the live API from this checkout (no key).

**Verified on the emulator (see Status above):** the CameraX bind + `UseCaseGroup`/`ViewPort`
path, `imageProxyToBitmap`, `cropToReticle`, ML Kit OCR invocation, the full
`ScanEngine.identify` decision flow, and every screen render — all run without crashing.

**Rough / unverified — needs a real Pixel:**
- **OCR hit rate on actual photos of a TV is unknown.** The 0.82 match bar and the
  candidate-building heuristic in `ScanEngine.buildCandidates` are guesses; tune them once
  you see real ML Kit output on Isaac's pickup-banner font over screen glare/moiré. A
  greyscale/contrast pre-pass before `InputImage.fromBitmap` may help a lot.
- **`cropToReticle` alignment vs. the on-screen reticle** looked right on the emulator's
  virtual scene but needs a real "does the box crop what I aimed at" check.
- **Gemini calls have never executed** — no key in this checkout. First real key + first
  429 / schema-mismatch will probably surface something.
- If OCR reads text but it doesn't match the catalog, the engine returns `Unrecognized` and
  never tries Gemini vision even when a key is present — revisit once you know how noisy
  real OCR is.
- Debug-only clutter: the `BuildConfig.DEBUG` "Xbox preset" bar overlaps the run pill on
  the scanner screen; `getXboxPresets()` runs in the scanner's first composition (fast on a
  Pixel, but it can land the catalog parse on the main thread — make it lazy/deferred).
- Room still uses `fallbackToDestructiveMigration()` — fine pre-release, must become real
  migrations before any store build.
- Catalog `quality` (item tier) was audited against `items_metadata.xml` (the game resource,
  via `2o181o28/eden-seed-finder`, 2025-06 "Repentance V2.1") and matches it **exactly on all
  721**. It reflects Repentance-launch values; ~20-25% differ from the *current* Repentance+
  Fandom wiki, almost always by 1 and usually lower (Repentance+ rebalanced many mediocre
  items down). To adopt current values, drop your install's
  `resources-dlc3/items_metadata.xml` in and re-map the `quality` field — the wiki scrape is
  the only other machine-readable source and it's noisier. Not worth doing blind.

**Exact next steps:** (1) sideload to the Pixel, run the on-device checklist above, watch
`adb logcat`. (2) Point it at 15–20 real pedestals, record OCR hit/miss, tune `OCR_MATCH_BAR`
and `buildCandidates`. (3) Add a Gemini key, exercise the sprite-fallback + verdict paths.
(4) Only then consider the namespace rename `com.example` → `com.kmill26.isaacscanner`
(optional, last — touches ~25 files + tests + manifest).
