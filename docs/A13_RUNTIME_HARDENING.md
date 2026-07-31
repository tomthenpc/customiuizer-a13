# A13 Runtime Hardening

> Branch: `devin/a13-runtime-hardening`
> Reviewed code baseline: `devin/a13-runtime-hardening` tip
> Date: 2026-08-01


## Status words

| Word | Meaning |
|---|---|
| `COMPLETED` | Code, unit tests and `verify.py full` / `lintDebug` pass in the branch. |
| `VERIFIED_STATIC` | Code and static evidence (unit tests / `check-invariants.py`) are in place; runtime evidence is static only. |
| `PARTIAL` | Implementation is in place with a documented, acceptable remaining gap. |
| `DEFERRED_EXTERNAL` | Requires a real device, release build or other external validation; not possible in this round. |

## Current implementation summary

| Area | Status | Evidence | Notes |
|---|---|---|---|
| `AGENTS.md` current rules | COMPLETED | `AGENTS.md` updated for `devin/a13-runtime-hardening`, no ADB/APK |
| `tools/verify.py` | COMPLETED | `fast` / `full` modes; no APK build |
| `tools/check-invariants.py` | COMPLETED | static source invariants |
| `tools/analyze_lsposed_log.py` | COMPLETED | offline log analyzer only |
| `.github/workflows/build.yml` | COMPLETED | `check-invariants.py` + `verify.py full`; no APK |
| `PrefMap` atomic snapshot + typed getters | COMPLETED | `PrefMap.kt`, `PrefMapTest.kt`; map-style `get` and `in` normalize keys |
| `PreferenceBootstrap` state machine | COMPLETED | `PreferenceBootstrap.java`, `PreferenceBootstrapTest.kt` |
| `ModuleHelper` receiver lifecycle | COMPLETED | two-phase module/owned registration, stale tracking with bound, identity checks; `ModuleHelperReceiverTest.kt` |
| `ModuleHelper.guarded` | COMPLETED | rethrows `OutOfMemoryError`; `check-invariants.py` enforces callback guarding |
| `FeatureDispatcher` typed IDs | COMPLETED | `FeatureId.kt`, `FeatureDispatcher.kt`, `FeatureCatalogTest.kt` |
| `ResourceHooks` active cache | COMPLETED | `ResourceHooks.java`; active cache checked before Context lookup; `applyHooks` idempotent |
| `MainModule` per-process installer split | VERIFIED_STATIC | `SystemServerInstaller`, `SystemUiInstaller`, `LauncherInstaller`, `PackageInstallerRouter` |
| `PackageInstallerRouter` return semantics | VERIFIED_STATIC | input method block returned to `MainModule` so `various_alarmcompat` runs for non-input packages |
| `mods/` callback boundaries | VERIFIED_STATIC | `ModuleHelper.guarded` present; manual pass over anonymous callbacks still recommended |
| Hot paths (network, clock, notification, launcher) | VERIFIED_STATIC | no `getArgsArray` without rewrite, no regex split on literal, callbacks guarded |
| Release build | DEFERRED_EXTERNAL | requires out-of-tree keystore and real device |
| Real-device LSPosed validation | DEFERRED_EXTERNAL | `PreferenceBootstrap` listener protocol, `ResourceHooks` active cache, `PackageInstallerRouter` routing need logs |

## Local verification

| Check | Command | Result |
|---|---|---|
| Invariants | `python tools/check-invariants.py` | PASS |
| Kotlin compile | `gradlew :app:compileDebugKotlin` | PASS |
| Java compile | `gradlew :app:compileDebugJavaWithJavac` | PASS |
| Unit tests | `gradlew :app:testDebugUnitTest` | PASS |
| Lint | `gradlew :app:lintDebug` | PASS |
| APK build | intentionally not run | N/A |

## Remaining risks

1. **Device validation** — `PreferenceBootstrap` listener protocol and `ResourceHooks` active cache need real LSPosed logs.
2. **APK / Release build** — not run; out-of-tree signature required.
3. **Manual `mods/` callback audit** — `ModuleHelper.guarded` and `check-invariants.py` cover structure; a manual pass over anonymous `BroadcastReceiver`, `ContentObserver`, `Runnable` and view listeners is still recommended before a major release.
