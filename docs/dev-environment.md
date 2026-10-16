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

`local.properties` points `sdk.dir` at `~/Library/Android/sdk`. The SDK must
contain `platform-tools`, `platforms;android-35`, and `build-tools;35.0.0`.

## Commands

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew installDebug
```

## Responsive verification

Screenshot review uses an emulator or a physical device. Window size is driven
with `adb shell wm size` and `adb shell wm density`, or `scrcpy` for a device.
The required viewport matrix is defined in `docs/prd.md` section 12.
