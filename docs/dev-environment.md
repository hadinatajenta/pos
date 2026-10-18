# Development Environment

Required to build and verify this project. The PRD targets a Mac with Android
command-line tools.

## Java

Gradle and the Android Gradle Plugin require **JDK 17**. The default `java` on
this machine is JDK 8, so export JDK 17 before running Gradle:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
```

## Android SDK

`local.properties` points `sdk.dir` at `~/Library/Android/sdk`. With `compileSdk 36`
the SDK must contain `platform-tools`, `platforms;android-36`, and a build-tools
version supported by AGP 8.7.3 (`build-tools;35.0.0` or newer; this machine has
34.0.0, 35.0.0, and 36.0.0).

## Commands

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
./gradlew installDebug
ANDROID_SERIAL=<serial> ./gradlew connectedDebugAndroidTest
./gradlew lintRelease
```

`lintRelease` currently fails with 11 errors (9 guarded `NewApi` calls and 2
`ProduceStateDoesNotAssignValue`). The fixes are listed in `docs/prd-gaps.md`.

## AAPT2 daemon constraint

On this machine, AGP 8.7.3's AAPT2 daemon fails during parallel resource
compilation with `Unexpected standard output: Ready`. The failure appears once
more than two parallel resource workers start, so `gradle.properties` pins
`org.gradle.workers.max=2`. Do not raise this without re-verifying
`./gradlew assembleRelease`.

## Responsive verification

Screenshot review uses an emulator or a physical device. Window size is driven
with `adb shell wm size` and `adb shell wm density`, or `scrcpy` for a device.
The required viewport matrix is defined in `docs/prd.md` section 12.
