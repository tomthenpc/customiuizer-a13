# A13 Runtime Hardening Audit

> Branch: `devin/a13-runtime-hardening`  
> Base: `main` after `r13.8.6`  
> Final HEAD: `ef61d76f55300ca1ec8494b418e3996c3ba6824c`  
> Date: 2026-07-31

## Status words

| Word | Meaning |
|---|---|
| `COMPLETED` | Code, unit tests and `verify.py full` / `lintDebug` pass in the branch. |
| `VERIFIED_STATIC` | Code and static evidence (unit tests / `check-invariants.py`) are in place; runtime evidence is static only. |
| `PARTIAL` | Implementation is in place with a documented, acceptable remaining gap. |
| `DEFERRED_EXTERNAL` | Requires a real device, release build or other external validation; not possible in this round. |

## Final state

| Item | Status | Evidence | Remaining risk |
|---|---|---|---|
| AGENTS.md simplified | COMPLETED | `AGENTS.md` rewritten |
| ADB/device automation removed | COMPLETED | `tools/k7-device-smoke.ps1` and device smoke docs deleted |
| `tools/verify.py` | COMPLETED | `fast`/`full` modes; no APK build task |
| `tools/analyze_lsposed_log.py` | COMPLETED | offline log analyzer + `docs/LSPOSED_LOG_ANALYSIS.md` |
| CI workflow | COMPLETED | `.github/workflows/build.yml` does not build APK |
| `PrefMap` atomic snapshot + typed getters | COMPLETED | `PrefMap.kt`, `PrefMapTest.kt`; map-style `get`/`in` now normalize keys |
| `PreferenceBootstrap` state machine | COMPLETED | `PreferenceBootstrap.java`, `PreferenceBootstrapTest.kt` (15 cases) |
| `ModuleHelper` module / owned receiver identity | COMPLETED | `ModuleHelper.java`, `ModuleHelperReceiverTest.kt`, `ModuleHelperRegistrationTest.kt` |
| `FeatureDispatcher` typed IDs | COMPLETED | `FeatureId.kt`, `FeatureDispatcher.kt`, `FeatureCatalogTest.kt` |
| `ResourceHooks` hot-path SparseArray | VERIFIED_STATIC | `ResourceHooks.java`, `verify.py full`; no dedicated resource-hook unit tests |
| `MainModule` per-process installer split | VERIFIED_STATIC | `SystemServerInstaller`, `SystemUiInstaller`, `LauncherInstaller`, `PackageInstallerRouter`; `verify.py full` passes; package hook continuation order restored; no dedicated routing unit tests |
| `PackageInstallerRouter` return semantics | VERIFIED_STATIC | `MainModule.java`; input method block returned to `MainModule` so `various_alarmcompat` runs for non-input package hooks; no routing unit tests |
| Full callback boundary in `mods/` | VERIFIED_STATIC | `ModuleHelper.guarded` present; `check-invariants.py` enforces; manual `mods/` callback audit not performed in this round |
| Release build | DEFERRED_EXTERNAL | requires out-of-tree keystore and real device |
| Real-device LSPosed validation | DEFERRED_EXTERNAL | `PreferenceBootstrap` double `getAll()` listener protocol, `ResourceHooks` active cache, `PackageInstallerRouter` routing need device logs |

## Build and test evidence

| Check | Command | Result |
|---|---|---|
| Java compile | `gradlew :app:compileDebugJavaWithJavac` | PASS |
| Kotlin compile | `gradlew :app:compileDebugKotlin` | PASS |
| Unit tests | `gradlew :app:testDebugUnitTest` | PASS |
| Lint | `gradlew :app:lintDebug` | PASS |
| Invariants | `python tools/check-invariants.py` | PASS |
| APK build | intentionally not run | N/A |

## Remaining risks

1. **Device validation** — `PreferenceBootstrap` listener protocol and `ResourceHooks` active cache need real LSPosed logs.
2. **APK / Release build** — not run; out-of-tree signature required.
3. **Manual `mods/` callback audit** — `ModuleHelper.guarded` and `check-invariants.py` cover structure; a manual pass over anonymous `BroadcastReceiver`, `ContentObserver`, `Runnable` and view listeners is still recommended before a major release.
