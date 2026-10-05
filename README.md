# Treasure Hunt Buah-Buahan — V1.1

An offline, native Kotlin Android fruit-catching game based on the supplied Malaysian supermarket storyboard. Minimum SDK **24** (Android 7.0), landscape orientation.

## Play and story

First launch: **Splash → skippable story intro → teacher briefing → How to Play → basket selection → 3, 2, 1, MULA → gameplay → results → optional checkout/celebration ending**.

After the intro is completed or skipped, future launches open the main menu directly. **CERITA** lets the player revisit the full story, including fruit-collection scenes. NEXT, BACK and SKIP INTRO keep the story optional.

Drag the basket left/right; movement is smoothed and clamped to the screen. Catch **fruit only** in a **60-second** round. Ordinary fruit gives **10 points**; the basket's matching fruit gives **20 points total**. There are green apples, red apples, oranges, grapes, mangoes, pears, strawberries and bananas.

| Basket | Matching bonus fruit |
| --- | --- |
| HIJAU | Green apple |
| MERAH | Red apple |
| UNGU | Grapes |
| OREN | Orange |

You start with **3 hearts**. Bottles, toy cars, cans, boxes and shoes are wrong objects: catching one costs **1 heart**, shakes/flashes the scene, plays local feedback and shows **BUKAN BUAH!**. Losing all hearts ends the round immediately. Missed items have no penalty. Finishing the timer with hearts remaining shows **TAHNIAH!**; there is no minimum score target.

Difficulty increases at 45, 30 and 15 seconds. At 10 seconds the teacher warns **MASA HAMPIR TAMAT! / 10 SAAT LAGI!**; the final five seconds have a pulsing countdown. The HUD shows MASA, SKOR and hearts. JEDA opens the pause menu with SAMBUNG, MULA SEMULA, MENU UTAMA and BUNYI ON/OFF. Pausing/backgrounding freezes round time; resuming requires an explicit choice. Restart creates a fresh countdown and round. Sound settings, intro completion and best score are saved locally.

Results include final score, fruit count, bonus count, mistakes and best score. Successful results offer the ending sequence: fruit check → cashier checkout → celebration. Generated local tones cover normal fruit, bonus, mistake, countdown, warning, success and game over.

## Storyboard assets

All **24 supplied images** and a **still from the supplied MP4** are used. Optimised assets are under `app/src/main/res/drawable-nodpi/`, around 2.1 MB total. [Scene mapping and optimisation](docs/storyboard/README.md) and [source asset manifest](docs/storyboard/assets.json) describe each use. Originals/video are not bundled. Story images decode off the UI thread, one scene at a time. Gameplay uses cached aisle art, a storyboard lighting crop and cached sprites to keep falling objects visible. The app has a fruit-basket icon and needs no account, network or storage permissions.

## Build

Use JDK **17** (21 also works), Android SDK platform **35**, build tools **35.0.0**, and the included checksum-verified Gradle **8.9** wrapper. AGP **8.7.3**, Kotlin **2.0.21**.

Open this folder in Android Studio, or set `ANDROID_HOME` to the SDK directory (alternatively create ignored `local.properties` with `sdk.dir=/your/sdk/path`):

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Install using Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

The `.github/workflows/android-build.yml` workflow builds on push or manual dispatch. Its downloadable artifact is **Treasure-Hunt-Buah-Buahan-debug**. The APK is debug-signed; release signing is not configured.

## Validation

- `GameStateTest`: scores, eight fruit types, basket bonuses, three lives, exact timer and difficulty boundaries.
- `MainActivityTest` (Robolectric): launch, story/skip persistence, selection, countdown, pause/sound/restart/background, statistics and ending navigation.
- `FruitGameViewTest` (Robolectric): deterministic fruit/wrong-object collisions, dragging bounds, round duration, pause clock and teacher warning.
- `VisualSmokeTest` (native Android graphics simulation): decodes actual storyboard resources, renders title/selection/gameplay/results/checkout and the adaptive basket icon; PNG reports are generated under `app/build/reports/visual-smoke/`.

For a physical-device smoke test, visit every scene, select each basket, drag, listen to each tone, reach Results through both timer expiry and heart exhaustion, pause/background/resume, and reopen the app to check preferences. JVM Android simulation does not measure performance or audio output on a physical phone.
