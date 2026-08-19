# A13 B3B — SystemUI HYBRID Architecture Selection

| Field | Value |
|---|---|
| **AUTHORITATIVE_BASE_SHA** | `5ec24cc1f08f1fe2cc0dfbc619c315ca313b3fc9` (B3A-R2 freeze; B3A docs freeze follows) |
| **Task** | HYBRID selection + CONFIRMED install-isolation / wrapped-fatal correctives |
| **PRODUCTION_AUTHORIZATION** | YES (B3B-D1..D4 only) |
| **Scope** | `SystemUiInstaller` HYBRID routing + 13 existing SystemUI catalog entries |

Frozen (do not reopen): A1, A2, A3, B1, B2A, B2B, B3A. Inventory baseline drift deferred to B3D.

```text
STATIC_VERIFIED = YES
BUILD_VERIFIED  = YES (targeted; see corrective commit)
LOG_VERIFIED    = NO
DEVICE_VERIFIED = NO
DEPENDENCY_VERIFICATION_CLEAN_GATE = NOT_PROVEN
CATALOG_MIGRATION_CANDIDATES = 0
FULL_GATE_RUN = NO
```

---

## 1. Executive summary

`SystemUiInstaller` is **HYBRID**:

- Package check is `com.android.systemui` only.
- `SYSTEM_UI_PLUGIN` is `isInstallable = false` and rejected in `MainModule.onPackageReady` before the installer runs.
- `FeatureDispatcher.createRuntime(pkg, …)` then **13 catalog** `installById` calls plus a large **legacy direct** surface.
- `SystemUIApplication.onCreate` has a local `isHooked` once-guard.
- `isWithinSystemUiRestartGuard` (~10s) skips the heavy feature install with **no retry**. That is **ARCHITECTURE_DEBT / INTENTIONAL_DIVERGENCE**, not a newly proven user-facing CONFIRMED defect. Not changed.

Catalog first-load is **already paid** on this process. Still **0 migration candidates**: remaining legacy hooks do not have a catalog-unique install-once / process / ClassLoader / resolver / lifecycle defect.

Installer-reachable **naked `XposedHelpers.findClass`** at function top-level throws `ClassNotFoundError` (an `Error`). `SystemUiInstaller.install` has no per-feature try/catch, so that abort skips **later independent installer features**. Same class of defect as B2B-D1 / B3A-R2.

Callback-time `findClass` (Dependency lookups inside MethodHook) is per-event. Not auto-fixed.

Mass replacement of every OOM-only catch in Controls / GlobalActions / other SystemUI files is **ARCHITECTURE_DEBT**, not this round.

---

## 2. Catalog (KEEP_CURRENT_CATALOG)

Installer `installById` IDs (13):

`tempHideOverlaySystemUI`, `hideStatusBarBeforeScreenshot`, `networkIndicatorWifi`, `statusBarClockTweak`, `noMoreIcon`, `hideDismissView`, `hideNavBarBeforeScreenshot`, `batteryIndicator`, `hideLockScreenHint`, `noNetworkSpeedSeparator`, `hideIconsClock`, `chargingInfo`, `muteVisibleNotifications`

All `processScope = SYSTEM_UI`, `processTarget = ProcessTarget.SystemUI`. No contract/target mismatch proven.

```text
B3B_CATALOG_MIGRATION_CANDIDATES = 0
```

---

## 3. Process / ClassLoader / runtime identity

| Fact | Evidence |
|---|---|
| Main SystemUI scope | `ProcessScopes.resolve("com.android.systemui", "com.android.systemui")` → `SYSTEM_UI` |
| Secondary SystemUI | non-main process → `SYSTEM_UI_PLUGIN` → rejected |
| Installer package gate | `SystemUiInstaller.install` returns unless `pkg.equals("com.android.systemui")` |
| `createRuntime` first arg | **package name**, not OS process name (same F1 P1-3 DEBT as Launcher). Safer here because plugin processes never enter the installer |

Do **not** change `createRuntime` to processName without a CONFIRMED defect.

---

## 4. CONFIRMED_DEFECT (authorized)

| ID | Defect | Fix |
|---|---|---|
| **B3B-D1** | `AddCustomTileHook` `findClass(QSTileImpl$ResourceIcon)` at install aborts later `MiuiQSFactory` / `MiuiNfcTile` hooks and remaining `SystemUiInstaller` features | `findClassIfExists`; skip only icon assignment; independent tile hooks still install |
| **B3B-D2** | `MonitorDeviceInfoHook` three `findClass` at start abort later `NetworkSpeedView.getSlot` + `DeviceInfoMonitor.hook` and remaining installer features | `findClassIfExists`; skip only blocks that need those classes |
| **B3B-D3** | `DisableAnyNotificationHook` `findClass(NotificationSettingsManager)` then `setStaticBooleanField(USE_WHITE_LISTS)` abort later `NotificationFilterHelper` hooks | `findClassIfExists`; field write `RuntimeFatality` fail-open; FilterHelper always attempted |
| **B3B-D4** | `ChargeAnimationHook` install-time catch uses direct-type fatal only; wrapped OOM/TD/VME swallowed and may fall through | `RuntimeFatality.throwIfFatal`; ordinary miss still logs and returns (does not abort installer) |

Not modified: `SystemUiInstaller`, `FeatureCatalog`, `FeatureDispatcher`, restart guard, globallauncher/Issue #2, catalog migration, callback-time findClass, local `rethrowFatal` / `rethrowNotificationFatal` (already walk VME/ThreadDeath causes).

---

## 5. Rejected / not auto-fixed

| Class | Item |
|---|---|
| ARCHITECTURE_DEBT | 10s restart guard skip-without-retry |
| ARCHITECTURE_DEBT | `createRuntime(packageName)` |
| ARCHITECTURE_DEBT | mass OOM-only catch rewrite across Controls / GlobalActions / other SystemUI files |
| LIKELY_DEFECT | callback-time throwing `findClass` (per-event) |
| INTENTIONAL_DIVERGENCE | local `rethrowFatal` in notification hooks (cause-walk includes VME) |
| INSUFFICIENT_EVIDENCE | DEVICE_VERIFIED SystemUI crash from missing ResourceIcon / DarkIconDispatcher |

---

## 6. Validation

```text
PRODUCTION_CHANGED = YES (D1–D4 authorized files)
TEST_CHANGED       = YES (targeted + fixtures)
FULL_GATE_RUN      = NO
B3B_CLOSED         = YES (unattended freeze; ChatGPT PASS not claimed)
```

---

## 7. B3B FINAL AUDIT (unattended freeze)

Independent re-check after D1–D4. Does **not** claim ChatGPT PASS.

```text
B3B_SELECTION_SHA = de3b2e1
B3B_CORRECTIVE_SHA = 1c89214
B3A_CLOSED_SHA     = 27356df
DIRECT_ANCESTRY    = 5ec24cc → 27356df → de3b2e1 → 1c89214
B3B_CATALOG_MIGRATION_CANDIDATES = 0
```

| ID | Still present |
|---|---|
| D1 | `AddCustomTileHook` `findClassIfExists(ResourceIcon)`; NfcTile / createTileInternal still install; icon assignment skipped if class missing |
| D2 | `MonitorDeviceInfoHook` `findClassIfExists` for DarkIconDispatcher / Dependency / StatusBarIconHolder / NetworkSpeedView; `DeviceInfoMonitor.hook` always reached |
| D3 | `DisableAnyNotificationHook` SettingsManager + `USE_WHITE_LISTS` fail-open; CloudData and FilterHelper still attempted |
| D4 | `ChargeAnimationHook` `RuntimeFatality.throwIfFatal`; wireless → wired fallback then return |

Unchanged: `SystemUiInstaller`, restart guard, catalog, `FeatureDispatcher`, callback-time findClass, local notification `rethrowFatal`.

Remaining: restart-guard skip-without-retry (DEBT); mass OOM-only rewrite (DEBT); callback findClass (LIKELY).

```text
B3B_UNATTENDED_FREEZE = YES
CHATGPT_PASS          = NOT_CLAIMED
```
