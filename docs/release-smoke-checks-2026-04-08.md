# Release Smoke Checks - 2026-04-08

## Build Verification

- Command: `./gradlew assembleRelease`
- Result: PASS
- Artifact: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Device Install Verification

- Command: `adb devices -l`
- Result: PASS (`device` authorized).
- Device: `R5CN208B19N`

- Command: `./gradlew installDebug`
- Result: PASS (`app-debug.apk` installed on `SM-G986U - 13`).

## Smoke Scenarios

- Non-approved WLAN state: PASS
  - UI showed: `Current WLAN is not approved.` and network identity.
  - UI showed settings entry: `Manage Approved Networks`.

- Approved WLAN state: PASS
  - Added approved pair: `Balton + E8:48:B8:90:66:96`.
  - UI showed: `Connected to approved WLAN: Balton`.

- Wake one / wake all: PASS
  - Added machine: `11:22:33:44:55:66` (resolved name `Machine 445566`).
  - Wake one snackbar: `Wake sent to Machine 445566.`
  - Wake all snackbar: `Wake sent to 1 machine(s).`

## Next Actions

1. Sign release APK/AAB for distribution and validate install path with release signing.
2. Capture screenshots and attach to release notes.
3. Commit and tag release candidate.
