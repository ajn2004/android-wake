# Phase 1 Design: Android Wake-on-LAN App

Date: 2026-04-08  
Status: Complete (Discovery and Design)

## 1. Architecture

Chosen architecture: single-activity app with Jetpack Compose UI and a small layered design.

- `ui` layer:
  - Compose screens and state holders (`ViewModel`)
  - Screens:
    - `HomeScreen` (approved vs non-approved mode)
    - `NetworkSettingsScreen` (manage SSID+BSSID pairs)
    - `AddMachineScreen`
- `domain` layer:
  - Use cases:
    - `GetCurrentNetworkIdentity`
    - `IsCurrentNetworkApproved`
    - `AddApprovedNetwork`
    - `RemoveApprovedNetwork`
    - `AddMachine`
    - `MoveMachineToCurrentNetwork`
    - `WakeMachine`
    - `WakeAllMachinesForCurrentNetwork`
- `data` layer:
  - Room database + DAOs
  - Repository interfaces and implementations
  - Network identity monitor + WoL sender implementation

Why this shape:
- `KISS`: minimal layers with explicit responsibilities.
- `SOLID`: network monitor, persistence, and packet sending stay separated and testable.
- `DRY`: shared validation and wake packet logic live in one place.

## 2. Android SDK and Permissions

Minimum SDK choice: `minSdk = 23` (Android 6.0) for broader device coverage.

Planned permissions:
- `android.permission.INTERNET` (send UDP magic packets)
- `android.permission.ACCESS_NETWORK_STATE` (network connectivity checks)
- `android.permission.ACCESS_WIFI_STATE` (read Wi-Fi connection details)
- `android.permission.ACCESS_FINE_LOCATION` (needed on many Android versions to access SSID/BSSID)
- `android.permission.NEARBY_WIFI_DEVICES` (Android 13+ behavior for Wi-Fi access)

Operational note:
- App should handle missing permission and unavailable SSID/BSSID gracefully by treating network as non-approved.

## 3. Local Data Model and Persistence Schema

Storage engine: Room (SQLite).

### Entity: `ApprovedNetwork`
- `id: Long` (PK, auto-generated)
- `ssid: String` (non-empty, trimmed)
- `bssid: String` (normalized uppercase `XX:XX:XX:XX:XX:XX`)
- Unique constraint: `(ssid, bssid)`

### Entity: `Machine`
- `id: Long` (PK, auto-generated)
- `networkId: Long` (FK -> `ApprovedNetwork.id`)
- `name: String` (optional user input; stored as resolved display label)
- `macAddress: String` (normalized uppercase MAC)
- Unique constraint: `macAddress` (global uniqueness)

### Duplicate MAC flow
If `macAddress` already exists under a different network:
- Show prompt: move existing machine to current network?
- If user confirms:
  - Update `Machine.networkId` to current network
  - Keep existing `name` unless user provided a replacement

## 4. UI State Map

## 4.1 App State Inputs
- Current Wi-Fi identity: `{ssid, bssid}` or unavailable
- Permission status
- Approved network list
- Machine list for current approved network

## 4.2 Main Modes

Mode A: Not on approved network
- Show text: "You have to connect to an approved WLAN network."
- Disable/hide wake actions
- Show button to open network settings screen

Mode B: On approved network
- Show connected network label (`SSID`)
- Show actions:
  - `Wake all machines`
  - List of machines with per-item `Wake`
  - `Add machine`
- Show entry to network settings screen

## 4.3 Add Machine Flow
1. User opens add-machine screen from Mode B.
2. User enters MAC address (required) and machine name (optional).
3. On submit:
   - Validate fields
   - Resolve default name if empty (`Machine <last-6-mac>`)
   - Insert machine or handle duplicate-MAC move flow
4. Return to Mode B with refreshed list.

## 5. Validation and Error Rules

### 5.1 MAC address
- Accept separators `:` or `-`.
- Normalize to uppercase colon form.
- Reject if not 6 hex octets.
- Reject broadcast/all-zero addresses:
  - `FF:FF:FF:FF:FF:FF`
  - `00:00:00:00:00:00`

### 5.2 SSID/BSSID
- SSID:
  - Required, trimmed, non-empty after trim
  - Max length aligned with Wi-Fi standard practical limit (32 chars)
- BSSID:
  - Required
  - Must match MAC format and normalize uppercase colon form

### 5.3 Machine name
- Optional input
- If blank after trim, generate default: `Machine <last-6-mac>`
- If provided, trim and cap length (50 chars)

### 5.4 Global MAC uniqueness
- Before insert, check existing record by normalized MAC.
- If record exists on same network: show "Machine already exists."
- If record exists on different network: show move prompt.

### 5.5 Non-approved-network behavior
- If network identity unavailable, permissions denied, or identity not matched:
  - Treat as non-approved mode
  - Show actionable guidance to settings/permissions

## 6. Implementation Notes for Phase 2

- Keep repositories behind interfaces for testability.
- Prefer a single source of truth for normalization/validation helpers.
- Add tests for:
  - MAC normalization + validation
  - SSID+BSSID approval matching
  - Duplicate-MAC move behavior
