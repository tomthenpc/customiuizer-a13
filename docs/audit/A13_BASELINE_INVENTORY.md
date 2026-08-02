# A13 Baseline Inventory

> Branch: `devin/a13-rom-intelligence-audit`
> Baseline commit: `89a93b1`
> Generated: 2026-08-02
> Repository: `tomthenpc/customiuizer-a13`
> Device evidence: `NOT_EXERCISED`

---

## 1. Source inventory

| Category | Count |
|---|---|
| Production Java files | 22 |
| Production Kotlin files | 72 |
| Total production source files | 94 |
| Java test files | 4 |
| Kotlin test files | 53 |
| Total test files | 57 |

Package/process categories:

- `tv.withaibuild.customiuizer` — settings app entry (`MainModule`, `MainApplication`, fragments, etc.)
- `installers` — 13 Java installers (`SystemServer`, `SystemUI`, `Launcher`, etc.)
- `mods/catalog` — Feature catalog, contracts, registry, dispatcher
- `mods/compat` — ROM environment
- `mods/diagnostics` — diagnostics and install state
- `mods/utils` — `ModuleHelper`, `HookInstaller`, `ResourceHooks`, etc.
- `mods/*` — 35+ feature hook files
- `org.apache.commons.lang3.reflect` — `MemberUtilsX`

---

## 2. Typed Feature catalog

- 25 typed `FeatureId` entries in `FeatureCatalog.kt` / `FeatureId.kt`.
- Detailed process/phase/contract mapping: see `docs/rom-intelligence/A13_PROCESS_MATRIX.md`.
- 8 features routed through `FeatureInstallRegistry` (Canary batch).
- 17 features still routed through legacy `FeatureDispatcher` direct install paths (Catalog batches 1-3).
- `autoBrightnessRange` has `AUTOMATIC_BRIGHTNESS_CONTROLLER` / `DISPLAY_POWER_CONTROLLER` variants.
- `fixAppInfoLaunch` uses `AnyOf` with two launcher entry points.
- All typed features have `FeatureSpec`, `FeatureId`, contract and diagnostics ID.

---

## 3. Legacy / untyped Hook inventory

- `FeatureDispatcher.install*` calls in installers: 25 (one per catalog feature).
- `FeatureInstallRegistry` references in source: 16.
- `ModuleHelper.(findAndHookMethod|hookAllConstructors|hookAllMethods)` text matches in `mods/`: 627 (includes `*Silently` variants and direct callback declarations).
- Installer infrastructure hook sites:
  - `SystemUiInstaller.java` — `SystemUIApplication.onCreate` bootstrap
  - `LauncherInstaller.java` — `Application.attach` bootstrap
  - `GenericAppInstaller.java` — `Application.attach` bootstrap
- Untyped hook work remains in `SystemUIStatusBarHooks.kt`, `SystemUINotificationHooks.kt`, `LauncherSystemHooks.kt`, `Controls.kt`, `GlobalActions.kt`, `Various.kt`, etc.
- Classification: `INSTALLER_INFRASTRUCTURE` for bootstrap, `LEGACY_EXCEPTION` for catalog features not yet migrated, 0 `UNKNOWN`/`DEAD` candidates.

---

## 4. Contracts and variants

- `CanaryContracts.kt` — 8 contracts (`packagePermissions`, `statusBarClockTweak`, `autoBrightnessRange`, `muffledVibration`, `noMoreIcon`, `batteryIndicator`, `noClockHide`, `noWidgetOnly`).
- `CatalogContracts.kt` — 17 contracts (`screenDimTime`, `firstVolumePress`, `networkIndicatorWifi`, `muteVisibleNotifications`, `hideLauncherTitles`, `fixAppInfoLaunch`, `hideProximityWarning`, `clearAllTasks`, `hideDismissView`, `hideLockScreenHint`, `folderColumns`, `titleTopMargin`, `noLightUpOnCharge`, `allRotations`, `noNetworkSpeedSeparator`, `hideIconsClock`, `noUnlockAnimation`).

---

## 5. Preference keys

- 659 `android:key` entries across `app/src/main/res/xml/prefs_*.xml` (628 unique).
- ~25 map to typed catalog features; the remainder are legacy/untyped feature keys.
- Canonical key lists per feature: see `FeatureCatalog.kt` `preferenceKeys` sets.

---

## 6. Tests and tools

| Type | Count/Items |
|---|---|
| Python tools | `verify.py`, `check-invariants.py`, `check-compat-contracts.py`, `r8_audit.py`, `rom_inventory.py`, `rom_target_diff.py`, `analyze_lsposed_log.py`, `apk_size_report.py`, `a13_view_tree_sim.py` |
| Python tool tests | 16 test files under `tools/tests` |
| PowerShell scripts | `scripts/verify.ps1` |
| Docs | 18 files under `docs/`, `docs/audit/`, `docs/rom-intelligence/`, `docs/releases/` |

---

## 7. Build and APK

| Item | Value |
|---|---|
| `scripts/verify.ps1 -Mode Full` | PASSED |
| Debug APK path | `app/build/outputs/apk/debug/CustoMIUIzer-A13-r13.9.2-debug.apk` |
| Debug APK size | 12,336,006 bytes (12.3 MB) |

---

## 8. Initial discovery notes

- 0 `TODO`/`FIXME` comments in production Java/Kotlin.
- No dead-code candidates under current mechanical gate.
- 17/25 typed features still on legacy dispatcher path; main P1-P2 migration target.
- ~627 `ModuleHelper` hook-helper matches remain to be classified and migrated.
- No `UNKNOWN` production hook owners identified at baseline.
- All runtime-invariant and compat-contract gates pass.
- Device/LSPosed evidence remains `NOT_EXERCISED`.
