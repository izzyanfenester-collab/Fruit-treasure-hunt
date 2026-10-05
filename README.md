# Treasure Hunt Buah-Buahan

A colourful, offline Kotlin Android game set in **Pasar Ceria**, a Malaysian supermarket. Minimum Android SDK **24** (Android 7.0).

## Play

Intro → Main menu → Choose basket → Gameplay → Results.

Drag anywhere in the play area to move your basket horizontally. You have **60 seconds** and **3 lives**. Catch any fruit for **10 points**, or the fruit matching your basket for **25 points total**. Catching a grey tin costs one life; missing an item has no penalty. The hunt ends at zero seconds or zero lives.

| Basket | Bonus fruit |
| --- | --- |
| Green | Guava |
| Red | Apple |
| Purple | Grapes |
| Orange | Orange |

Pause freezes the game and offers Resume, Restart and Main menu. Restart during play asks before clearing the score. Sound can be switched on or off; the preference and best score are saved on-device. Moving the app to the background pauses play. Large buttons, bright vector/canvas graphics and short synthesised sounds need no downloads, account or permissions.

## Build

Use JDK **17** (JDK 21 also works), Android SDK platform **35**, build tools **35.0.0**, and the included Gradle **8.9** wrapper. Android Gradle Plugin is **8.7.3**; Kotlin is **2.0.21**.

Open this folder in Android Studio and allow Gradle sync, or set `ANDROID_HOME` to your SDK directory (alternatively create ignored `local.properties` with `sdk.dir=/your/sdk/path`), then run:

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Install on an Android 7.0+ device using Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds on pushes, pull requests and manual dispatch. Download the APK from the run’s **Treasure-Hunt-Buah-Buahan-debug** artifact. Debug APKs use a debug signing key; release signing is not configured.

## Structure and validation

- `MainActivity.kt`: screen flow, pause/restart dialogs, preferences and audio.
- `FruitGameView.kt`: frame loop, drag controls, falling items, collision handling and canvas artwork.
- `GameState.kt`: score, life and timer rules, independently unit tested.

The unit tests cover matching bonuses, life exhaustion and the 60-second limit. For a device smoke test: visit each screen, select each basket, drag to catch fruit/tins, toggle sound, pause for several seconds, resume, restart, background/resume the app, and reach Results both through the timer and life exhaustion. Verify the timer freezes while paused and best score persists after reopening. Device gameplay checks require an emulator or physical Android device.
