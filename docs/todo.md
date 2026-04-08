# TODO Plan: Android Wake-on-LAN App (Approved WLAN Only)

## Source Document
- Path: `docs/ideation.md`
- Reviewed on: `2026-04-08`

## Objective
Build a simple Android app that sends Wake-on-LAN magic packets only when the device is connected to an approved Wi-Fi network. Provide machine management and wake actions with clear UX for approved vs non-approved network states.

## Scope
- In scope:
  - Enforce approved-WLAN gating before showing wake controls.
  - Manage approved network identities (SSID + BSSID) (add/remove).
  - Manage per-network machine list (MAC address + display name).
  - Send WoL packet(s): wake all and wake specific machine.
  - Validate MAC address input and show user-facing success/error states.
- Out of scope:
  - Background wake scheduling or automation.
  - Internet/remote wake outside local network.
  - User accounts, cloud sync, or multi-device state sync.

## Plan of Attack (Checklist)

### Phase 1: Discovery and Design
- [x] Define app architecture (single-activity + Compose screens, local repository abstraction, network monitor service).
- [x] Confirm Android SDK minimum version for broader device support and required permissions for Wi-Fi state and UDP networking.
- [x] Define local data model and persistence schema:
  - `ApprovedNetwork(ssid, bssid)`
  - `Machine(id, networkSsid, name, macAddress)`
- [x] Draft UI state map for two main modes:
  - Not on approved WLAN: warning + settings access.
  - On approved WLAN: wake controls + machine list + add machine.
- [x] Define validation and error rules for MAC address format, invalid SSID/BSSID input, and global MAC uniqueness with “move existing machine” prompt flow.

### Phase 2: Implementation
- [x] Initialize Android project structure and baseline dependencies.
- [x] Implement local persistence layer for approved SSIDs and machines.
- [x] Implement current Wi-Fi SSID + BSSID detection and approved-network check.
- [x] Implement non-approved-network screen with message and settings entry point.
- [x] Implement approved-network home screen with:
  - `Wake all machines`
  - `Wake specific machine` list
  - `Add machine to wake`
- [x] Implement settings screen to add/remove approved SSID+BSSID pairs.
- [x] Implement add-machine flow:
  - Optional name input with generated default label
  - MAC address input + validation
  - Auto-associate machine to currently connected approved SSID
  - If MAC already exists globally, prompt user to move it instead of creating duplicate
- [x] Implement WoL packet sender utility (broadcast UDP magic packet from MAC).
- [x] Wire wake actions:
  - Wake selected machine
  - Wake all machines associated with current SSID

### Phase 3: Validation
- [x] Add unit tests for MAC validation logic and duplicate detection.
- [x] Add unit tests for machine filtering by current SSID.
- [x] Add integration test(s) for gating logic (approved vs non-approved network state).
- [x] Add integration test(s) for add/remove approved SSID behavior.
- [x] Add integration test(s) for add machine and wake-action enablement.
- [x] Run full test suite and verify passing in CI/local environment.

### Phase 4: Release and Follow-up
- [x] Add concise in-app help text clarifying local-network-only behavior.
- [x] Verify release build, install on test device, and run smoke checks:
  - Non-approved WLAN state
  - Approved WLAN state
  - Wake one / wake all
- [x] Document setup and usage in project README:
  - Required network conditions
  - How to register SSIDs and machines
  - Known limitations
- [ ] Tag initial release version.
- [x] Capture follow-up improvements backlog.

Phase 4 verification notes:
- `assembleRelease` passed on `2026-04-08` (see `docs/release-smoke-checks-2026-04-08.md`).
- On-device smoke checks passed on `R5CN208B19N` after authorization (see `docs/release-smoke-checks-2026-04-08.md`).

### Phase 5: Quality-of-Life UX Improvements
- [x] Implement lenient MAC typing UX in add/edit machine forms:
  - Accept user input without separators and auto-format into colon-delimited pairs while typing.
  - Example transform: `1122334455` -> `11:22:33:44:55`.
  - Preserve normalized storage and existing validation behavior.
- [x] Auto-return to main screen after successful machine add.
- [x] Add long-press actions for machine rows on main screen:
  - `Edit computer` opens edit flow with pre-filled values.
  - `Remove computer` deletes the selected machine with confirmation prompt.
- [x] Add repository/view-model support for machine update and delete operations.
- [x] Add tests for QoL behavior:
  - MAC auto-format input behavior (including edge cases and partial input).
  - Successful add triggers navigation back to main screen.
  - Long-press edit/remove actions update UI state and persistence correctly.
- [ ] Run full test suite and smoke-check add/edit/remove flows on device/emulator.
  - Unit suite passed with `:app:testDebugUnitTest` on `2026-04-08`.
  - Device/emulator QoL smoke check still pending.

## Outstanding Decisions

All previously identified decisions are now resolved.

### 1. Should approved networks be matched by exact SSID only, or include additional constraints (for example BSSID)?
- Status: Resolved
- Decision: Match using `SSID + BSSID`.
- Rationale: Higher security and fewer false-positive matches.

### 2. What minimum Android API level should we support?
- Status: Resolved
- Decision: Use lower minimum API for broader device coverage.
- Rationale: Maximizes compatibility and adoption.

### 3. Should machine names be required or optional when adding a machine?
- Status: Resolved
- Decision: Machine name is optional with generated default label.
- Rationale: Lower user friction while keeping machine list readable.

### 4. How should we handle duplicate MAC addresses across different approved SSIDs?
- Status: Resolved
- Decision: Enforce global uniqueness and prompt user to move existing entry.
- Rationale: Prevents conflicting records and ambiguous wake behavior.

## Risks and Unknowns
- Android Wi-Fi SSID access behavior may vary by OS version and permissions, impacting reliability of network gating.
- Some routers or network configurations may block local broadcast packets needed for WoL.
- Missing validation could lead to silent wake failures (invalid MAC input or malformed packets).
- Insufficient device/network testing could hide environment-specific failures.

## Definition of Done
- [x] App shows only warning/settings when not connected to an approved WLAN.
- [x] App shows wake actions and machine list when connected to an approved WLAN.
- [x] User can add/remove approved SSID+BSSID pairs and changes persist across app restarts.
- [x] User can add machine with validated MAC and optional name (with generated default label when omitted), and machine is bound to current approved network.
- [x] Duplicate MAC creation is blocked globally and offers a move prompt flow.
- [x] `Wake all` sends WoL packets to all machines for current SSID.
- [x] `Wake specific machine` sends WoL packet to selected machine.
- [x] Core tests for validation, gating, and machine association pass.
- [x] README documents setup, usage, and limitations.
- [x] MAC input auto-formats user-entered hex into colon-delimited pairs on add/edit screens.
- [x] After adding a machine successfully, app navigates back to main screen automatically.
- [x] Long-pressing a machine shows `Edit computer` and `Remove computer` options.
- [x] User can edit an existing machine and save changes with validation preserved.
- [x] User can remove an existing machine with confirmation and immediate list update.
