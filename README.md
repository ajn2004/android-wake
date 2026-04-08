# android-wake

Android app for sending Wake-on-LAN (WoL) magic packets, gated by approved local Wi-Fi networks.

## General Idea

This repository is for a simple Android utility that wakes machines on a local network.  
The app should only allow wake actions when the phone is connected to a user-approved network identity (`SSID + BSSID`).

When connected to a non-approved network, the app should show a clear message and only expose network settings management.

## Goals

- Provide a fast, simple UI to wake machines on the current approved network.
- Improve safety by restricting wake actions to approved `SSID + BSSID` pairs.
- Support both:
  - Wake all registered machines on the current network
  - Wake a specific registered machine
- Let users manage:
  - Approved networks
  - Machines (MAC-based), with optional display names
- Enforce global MAC uniqueness and prompt users to move an existing machine entry instead of duplicating it.
- Target broader Android device compatibility (lower minimum API).

## Current Scope

In scope right now:
- Local-only WoL behavior over Wi-Fi
- Local persistence for approved networks and machine registry
- Basic validation and test coverage for network gating and machine management

Out of scope right now:
- Remote/internet wake scenarios
- Background scheduling or automation
- Cloud sync, user accounts, or multi-device state

## Status

Phase 1 (Discovery and Design) is complete.

- Ideation source: [`docs/ideation.md`](docs/ideation.md)
- Execution checklist: [`docs/todo.md`](docs/todo.md)
- Phase 1 design output: [`docs/phase1-design.md`](docs/phase1-design.md)

## Planned Implementation Principles

- `KISS`: keep flows simple and explicit
- `YAGNI`: avoid extra features outside the current scope
- `DRY`: centralize validation and wake logic
- `SOLID`: separate network detection, persistence, and wake packet sending concerns
