# Changelog

[简体中文](CHANGELOG.md) | English

## r13.12.0 — 2026-08-19

`versionCode 138`, targeting MIUI 14 / Android 13 with capability-detected HyperOS 1 / Android 13 compatibility.

### New features

- USB default-mode options mapped onto the existing A13 hook.
- Installer purify, and hiding the app-details report entry.
- Launcher dock height and hiding the IME dismiss button.
- Additional status-bar visual options.
- Folder blur can be disabled without discarding opacity.
- Settings backup V2 with legacy restore.
- Battery indicator custom colors from existing colorval prefs; screen-dim ratio writes AOSP fields from the 0–99% slider.

### Fixes

- Custom/gesture actions no longer snap back to "No action": invalid or unknown values normalize to legal default 1, and save no longer uses an out-of-range spinner index.
- The Launcher gesture page now exposes Restart Launcher, matching hook ownership in the Launcher process.
- USB default mode reapplies after unplug/replug.

### Stability and compatibility

- Selector results are delivered only while the owning Fragment is added.
- Features still install per process; disabled features create no business hooks.
- 63 ROM/device-gated capabilities remain holds; they are not guessed without device evidence.

### Performance

- No new device performance claims. Existing clock and gesture hot paths keep bounded state caches; this release does not add speculative micro-optimizations.

### Architecture and maintenance

- A13 architecture vs A14 product capability review is statically closed. Existing capabilities are proven against the A13 ABI; Dynamic Island stays out of A13.

### Verification

- Static gates, unit tests, Release compile, Lint, R8, and signed-artifact inspection run before publication.
- DEVICE_VERIFIED = NO
- LOG_VERIFIED = NO
- No ADB; this release does not claim on-device or MIUI/HyperOS runtime verification.

### Artifact

- File: `CustoMIUIzer-A13-r13.12.0.apk`
- versionName: `r13.12.0`
- versionCode: `138`
- SHA-256: `643e93834c7028a4355f9915efbfe3aa49393ff18577331a76a485c6d9382e29`

## r13.11.1 — 2026-08-08

`versionCode 137`, released as the completed Android 13 performance, lifecycle, and build-governance milestone.

### Core Changes

- Hardened SubFragment delayed-scrolling lifecycle: pending callbacks are cancelled when the View is destroyed, preventing delayed actions on invalid Views.
- Hardened AppSelector asynchronous app-list loading by using the application context, input snapshots, and owner cleanup to reduce Activity/View lifecycle coupling.
- Hardened ActivitySelector asynchronous loading so results are only committed within the current valid View lifecycle, while preserving the existing re-query behavior on View recreation.
- Optimized the status-bar clock's default-format hot path by caching stable format-conversion and resource-resolution results, reducing repeated work on every time update.
- Preserved existing system time format, seconds, 12/24-hour mode, AM/PM, leading zero, and custom-format behavior.
- Completed Android 13 Release compilation, unit tests, Lint, R8, strict dependency verification, signing, and core on-device loading verification.

### Compatibility Notes

- MIUI 14 / Android 13 remains the primary target.
- HyperOS 1 / Android 13 SystemUI customizations depend on the specific ROM internal classes and system-app versions; a missing target only affects the corresponding feature and does not prevent other module functions from loading.
- No numerical claims are made for memory, CPU, GC, or power-consumption improvements without completed device metric sampling.

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
- The known deployed baseline is Redmi Note 11T Pro (`xaga`), MIUI `V14.0.10.0.TLOINXM`. HyperOS 1 feature availability depends on the ROM and system-app versions.

## r13.9.2 — 2026-08-01

- Cancelled unfinished lock-screen album-art work and released module-owned backgrounds, one-frame caches, and static processed results when the owner View detached;
- Set settings-page transitions to `350ms`;
- Reused the containing row's pressed state for switches and removed per-tap alpha animators;
- Added a dedicated concise LSPosed module summary.

### Historical Core Implementation Summary

The A13 line established an independent package and Android 13 maintenance path; delivered libxposed API 101/102 compatibility; separated System, SystemUI, and Launcher domains; performed staged Kotlin migrations; hardened resource and preference Hooks; governed lifecycles; bounded caches; added cancellable asynchronous work; defined fatal-error boundaries; and introduced Contract/Resolver compatibility diagnostics. Fine-grained history remains in Git commits and historical tags.
