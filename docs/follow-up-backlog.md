# Follow-up Backlog (Post v0.1.0)

Date: 2026-04-08

## Priority 1

- Replace deprecated Compose `Divider` usage with `HorizontalDivider`.
- Add runtime permission request flow (`ACCESS_FINE_LOCATION` / `NEARBY_WIFI_DEVICES`) instead of relying on external grant state.
- Add instrumentation/UI tests for end-to-end add-network/add-machine/wake flows.

## Priority 2

- Add explicit network diagnostics UI (permission state, current SSID/BSSID visibility, why gating failed).
- Add per-machine edit/delete actions.
- Add machine import/export backup option (local file only).

## Priority 3

- Improve WoL sender options (custom port and subnet broadcast target).
- Add app icons, polish copy, and accessibility pass.
- Add signed release pipeline and CI workflow for build/test artifacts.
