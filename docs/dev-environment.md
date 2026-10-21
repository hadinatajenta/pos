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

## Hardware and camera verification

Keep the owner's device intact. Replacing a build with `install -r` preserves app data, but
`connectedDebugAndroidTest` removes the app APKs after the run unless it is started with:

```bash
ANDROID_SERIAL=<serial> ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

Never run `adb uninstall` or `adb shell pm clear` on a connected device without the owner asking.
Pass `--user 0` to `adb install` so the app lands in the owner's profile rather than a Secure Folder,
and restore any device setting you changed (font scale, display size) before finishing.

- The camera path needs a physical device. There is no emulator image in this setup.
- A USB or Bluetooth HID scanner can be simulated without hardware by sending fast keystrokes and a
  terminator, which is what the keyboard-wedge decoder expects:

  ```bash
  adb shell input text 8991234567890
  adb shell input keyevent 66
  ```

- Confirm the app ignores the system dark setting: `adb shell cmd uimode night yes` must leave every
  screen light, and `adb shell cmd uimode night no` must restore the device setting.
- On a device that has a Secure Folder or a second profile, pass `--user 0` to `adb install`.
  Without it the package can land in the other profile, and then the launcher and `am start` cannot
  see it.

## Responsive verification

Screenshot review uses an emulator or a physical device. Window size is driven
with `adb shell wm size` and `adb shell wm density`, or `scrcpy` for a device.
The required viewport matrix is defined in `docs/prd.md` section 12.
