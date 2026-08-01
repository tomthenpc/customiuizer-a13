# Changelog

## r13.9.2 (2026-08-01)

### Memory and stability

- When the lock-screen album-art owner detaches, unfinished work is cancelled and the module-owned background, one-frame output cache, and static processed result are released. The source remains available for on-demand regeneration after reattach, preserving behavior;
- The single bounded queue, latest-generation publication gate, and intermediate Bitmap recycling remain in place so cancelled results cannot repopulate SystemUI memory.

### UI and interaction

- Settings transitions now match the current A14 implementation at `350ms`, restoring a more readable navigation pace;
- Switches inherit the pressed state of their row for immediate feedback. Per-tap alpha animators were removed, reducing temporary render state and interference during repeated taps.

### Compatibility and release

- No MIUI 14 / Android 13 or HyperOS 1 / Android 13 ROM Hook target, Contract, or fallback changed;
- The LSPosed repository now provides a dedicated `SUMMARY`, keeping the module-list card concise instead of flattening the full README;
- Kotlin/Java compilation, static invariants, unit tests, Lint, version, signing, zip alignment, Xposed metadata, and `debuggable=false` remain release gates. The new changes still require on-device LSPosed-log confirmation.

## Historical implementation summary

The A13 line established an independent package and A13 signing identity, libxposed API 101/102, System/SystemUI/Launcher separation, small Kotlin migrations, hardened resource and preference Hooks, receiver/observer/handler lifecycle ownership, bounded caches, cancellable asynchronous work, explicit OOM boundaries, and Contract/Resolver compatibility diagnostics. Git tags and commits retain the detailed history.
