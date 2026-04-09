# android-wake

Android app for sending Wake-on-LAN (WoL) magic packets, gated by approved local Wi-Fi networks.

## What It Does

- Sends WoL packets only when the phone is connected to an approved network identity (`SSID + BSSID`).
- Supports waking:
  - All machines registered to the current approved network
  - One selected machine
- Lets you manage:
  - Approved networks (`SSID + BSSID`)
  - Machines (`name + MAC`) mapped to approved networks
- Provides machine quality-of-life UX:
  - MAC input auto-format while typing (for example `1122334455` -> `11:22:33:44:55`)
  - Automatic return to the home screen after successful add/edit
  - Long-press machine actions on home: `Edit computer` and `Remove computer`
- Enforces global MAC uniqueness and prompts to move an existing machine between networks.

## Required Network Conditions

- Phone must be connected to Wi-Fi.
- Current Wi-Fi network must exactly match a saved approved pair:
  - Same `SSID`
  - Same `BSSID`
- Target machine must be reachable on the same local network and configured to accept WoL.
- Router/AP must allow local broadcast traffic used by WoL.

## Build And Run

Prerequisites:
- JDK 17
- Android SDK installed (`ANDROID_HOME` or `local.properties` with `sdk.dir`)

If multiple JDKs are installed, force Gradle to use Java 17:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Commands:

```bash
./gradlew :app:testDebugUnitTest
./gradlew assembleDebug
./gradlew installDebug
```

Release build:

```bash
./gradlew assembleRelease
```

## How To Use

1. Open app and go to `Manage Approved Networks`.
2. If prompted, grant Wi-Fi-related runtime permissions so the app can read current Wi-Fi identity.
   - Depending on Android version/device behavior, this may include location permission.
   - If permissions were denied previously, use `Grant Wi-Fi Permissions` from the home screen.
3. Add one or more approved network identities (`SSID` + `BSSID`), or use `Add Current Network` when already connected to Wi-Fi.
4. Connect phone to one of those approved Wi-Fi networks.
5. From home, choose:
   - `Add Machine` and enter target machine MAC (name optional). MAC can be entered with or without separators and is auto-formatted, or
   - `Wake All Machines`, or
   - `Wake` for a specific machine.
6. If MAC already exists on a different network, confirm the move prompt.
7. Long-press a machine row to:
   - `Edit computer` (pre-filled name + MAC), or
   - `Remove computer` (with confirmation).

## Home Screen Widget

- Add the `Android Wake` widget from the launcher widget picker.
- Widget behavior:
  - If not on an approved Wi-Fi network, it shows `Not connected to an approved network.`
  - If on an approved network, it shows:
    - `Wake all`
    - Scrollable machine list; tap any machine row to send wake for that machine.
- Widget refreshes on:
  - Widget update events
  - App resume
  - App data changes (approved networks/machines add, edit, remove, move)
- If you tap wake while no longer on an approved network, action is safely blocked and a short status toast is shown.

## Known Limitations

- Local network only; internet/remote wake is out of scope.
- Wi-Fi identity access depends on Android permissions/version behavior.
- Home-screen widget refresh timing can vary by launcher/OEM power management behavior.
- No background scheduling, automation, cloud sync, or multi-user support.
- Smoke testing on a physical device depends on having an attached test device/emulator and compatible local network setup.

## Project Docs

- Ideation: [`docs/ideation.md`](docs/ideation.md)
- Design: [`docs/phase1-design.md`](docs/phase1-design.md)
- Execution checklist: [`docs/todo.md`](docs/todo.md)
