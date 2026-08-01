# Changelog

## r13.9.1 (2026-08-01)

### Compatibility and diagnostics

- Bound feature Contracts, target Resolvers, and production installers to the same ClassLoader and target variant, preventing mixed MIUI 14 / HyperOS member bundles;
- Closed `STATUSBAR_CLOCK_TWEAK` install-record gaps so Contract operations, actual Hook operations, failed targets, and exception types can be correlated without downgrading REQUIRED targets or adding speculative fallbacks;
- Fixed ResourceHooks getter argument expansion and moved method-kind resolution out of high-frequency callbacks;
- Added HyperOS 1 / Android 13 ROM capability identification and diagnostics. Missing candidates fail safely; each ROM bundle still requires on-device LSPosed-log confirmation.

### Stability, memory, and performance

- Isolated ordinary Hook, reflection, and asynchronous failures by feature while preserving direct or reflectively wrapped `OutOfMemoryError` propagation;
- Hardened registration, cancellation, and lifecycle boundaries for receivers, preferences, step counter, device monitor, AudioVisualizer, lock-screen album art, battery indicator, and delayed input events;
- Reduced repeated reflection, regular expressions, temporary collections, argument arrays, and callback objects in clock, network-speed, notification, controls, Launcher, and resource-replacement hot paths;
- Lazily initialized shared handlers, resource Hooks, and preference infrastructure so disabled features create no unrelated background work;
- Streamlined security/privacy app-list updates and icon requests to reduce repeated lookups and short-lived allocations.

### UI and interaction

- Shortened page transitions and streamlined Preference click dispatch;
- Made switches update their visible state immediately while retaining accessibility semantics, reducing perceived no-feedback clicks;
- Cleared delayed input, callbacks, and references when Views are destroyed so stale screens cannot keep reacting.

### Verification boundary

- Full static verification, Kotlin/Java compilation, JVM tests, Python tool tests, and Lint are release gates;
- The formal APK is separately checked for version, SHA-256, signing certificate, zip alignment, Xposed metadata, and `debuggable=false`;
- MIUI 14 / Android 13 retains the established device baseline. This release's new changes and HyperOS 1 / Android 13 still require new detailed LSPosed logs.

## Historical implementation summary

Starting from an independent Android 13/libxposed API 101 port, the project established its own package and signing line, API 101/102 metadata, System/SystemUI/Launcher domain split, small behavior-preserving Java-to-Kotlin migrations, hardened RemotePreferences and resource Hooks, receiver/observer/handler lifecycle ownership, bounded icon and media caches, latest-wins search and asynchronous work, explicit OOM boundaries, feature Catalog/Contract/Resolver diagnostics, and continuing performance work across status bar, lock screen, notifications, Launcher, and settings UI. Preserved Git tags and commits provide the detailed history.
