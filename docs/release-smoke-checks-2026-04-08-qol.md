# QoL Smoke Checks - 2026-04-08

## Environment

- Host JDK: `17` (`JAVA_HOME=/usr/lib/jvm/java-17-openjdk`)
- Device: `R5CN208B19N` (`SM-G986U`, Android 13)

## Automated Checks

- Unit tests: PASS
  - Command: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testDebugUnitTest`
- Release build: PASS
  - Command: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleRelease`
  - Artifact: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Device detected: PASS
  - Command: `adb devices -l`
- Debug install on device: PASS
  - Command: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew installDebug`
- Connected instrumentation task: PASS
  - Command: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew connectedDebugAndroidTest`
- App launch on device: PASS
  - Command: `adb shell am start -n com.example.androidwake/.MainActivity`
  - Verified resumed/focused activity via `dumpsys activity`.

## Manual QoL Scenarios

The following are still manual UX validations and were not auto-asserted by instrumentation tests:

- Verify permission prompt appears on first launch when Wi-Fi permissions are missing.
- Verify `Grant Wi-Fi Permissions` button successfully re-prompts after denial.
- Verify `Add Current Network` auto-fills and saves connected `SSID + BSSID`.
- Verify add/edit MAC auto-formatting while typing (for example `1122334455` -> `11:22:33:44:55`).
- Verify successful add returns to home automatically.
- Verify long-press machine row shows `Edit computer` and `Remove computer`.
- Verify edit flow updates machine values and returns to home.
- Verify remove flow confirms and removes machine from list immediately.
