# Changelog

[简体中文](CHANGELOG.md) | English

## r13.10.1 — 2026-08-06

`versionCode 135`, targeting MIUI 14 / Android 13, capability-detected HyperOS 1 / Android 13 compatibility, `arm64-v8a`, and libxposed API 101/102.

### Core Changes

- Split SystemUI, Launcher, `system_server`, and regular-app entry points into process-routed Installers. Stable feature identities, process scopes, install phases, and install-once state prevent unrelated loading and duplicate installation.
- Hardened early preference snapshots, empty-snapshot handling, concurrent loading, and failed-install state so preference updates cannot incorrectly reset installed Hooks or trigger duplicate installation.
- Improved MIUI 14 and HyperOS 1 / Android 13 environment detection, Hook Contracts, target resolution, and variant selection. Missing targets skip only the affected feature without mixing candidates.
- Unified Hook, reflection, Receiver, Observer, delayed-callback, and diagnostics boundaries. Ordinary compatibility failures remain isolated, while direct or wrapped `OutOfMemoryError`, `ThreadDeath`, and `VirtualMachineError` continue to propagate.
- Completed owner, replacement, stale-state, and release paths for Receivers, Observers, Views, Handlers, and controllers, and removed blocking waits from selector UI paths.
- Hardened callback paths for call-interruption control, secure-window removal, temporary overlay hiding during screenshots, notification/share floating-window actions, and multi-window restrictions; aligned installation and compatibility boundaries across status bar, control center, volume, lock screen, and settings Hooks.
- Replaced Launcher animation-scale reflection with a direct API, cached HotSeats density, touch thresholds, and gesture state per View, and reused the install-time `BaseRecentsImpl` Class in FSG callbacks.
- Reworked DeviceInfo sampling around fixed buffers and byte-wise sysfs parsing, reducing periodic `Properties`, `RandomAccessFile`, Binder queries, and temporary objects while preserving sampling and failure backoff.
- Added runtime invariant, Hook-contract, source-hazard, ROM-compatibility, Release compilation, unit-test, Lint, R8, and dependency-integrity gates.
- Updated the build toolchain and dependency verification, removed the unused `miui.jar` and an invalid external annotation, and normalized shared ignore rules.

### Verification Scope

- The current code passes Python and Gradle static gates, Release Kotlin/Java compilation, Release unit tests, Release/Vital Lint, R8, strict dependency verification, Manifest checks, and Xposed metadata checks.
- The known deployed baseline is Redmi Note 11T Pro (`xaga`), MIUI `V14.0.10.0.TLOINXM`, . HyperOS 1 feature availability depends on the ROM and system-app versions.

## r13.9.2 — 2026-08-01

- Cancelled unfinished lock-screen album-art work and released module-owned backgrounds, one-frame caches, and static processed results when the owner View detached;
- Set settings-page transitions to `350ms`;
- Reused the containing row's pressed state for switches and removed per-tap alpha animators;
- Added a dedicated concise LSPosed module summary.

### Historical Core Implementation Summary

The A13 line established an independent package and Android 13 maintenance path; delivered libxposed API 101/102 compatibility; separated System, SystemUI, and Launcher domains; performed staged Kotlin migrations; hardened resource and preference Hooks; governed lifecycles; bounded caches; added cancellable asynchronous work; defined fatal-error boundaries; and introduced Contract/Resolver compatibility diagnostics. Fine-grained history remains in Git commits and historical tags.
