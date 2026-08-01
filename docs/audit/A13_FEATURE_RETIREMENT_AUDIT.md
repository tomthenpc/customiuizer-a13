# A13 Feature Retirement Audit

> Branch: `devin/a13-rom-intelligence-audit`
> Scope: the 25 strongly-typed `FeatureId` catalog entries in `tv.withaibuild.customiuizer.mods.catalog`.

## Methodology

| Check | Source |
|---|---|
| Feature list & pref keys | `FeatureCatalog.kt` (`FeatureSpec` list, `id` + `preferenceKeys`) |
| Runtime dispatch | `FeatureDispatcher.kt` `when (feature)` table, `install*` functions |
| Installer reachability | `SystemServerInstaller.java`, `SystemUiInstaller.java`, `LauncherInstaller.java` `FeatureDispatcher.installById(...)` calls |
| Hook contracts | `CanaryContracts.kt`, `CatalogContracts.kt` `featureId = "..."` blocks |
| Settings entry | `app/src/main/res/xml/prefs_*.xml` `android:key` entries |
| R8 / keep | `app/proguard-rules.pro` (no preference-key keep rules) |
| Runtime status | `docs/A13_RUNTIME_HARDENING.md`, `docs/releases/r13.7.0.md`, `docs/releases/r13.9.2.md` |

## Category rules applied

- `KEEP` — supported, reachable, evidence exists, risk acceptable.
- `KEEP_GUARDED` — partial ROM support, uses a `Contract` / `HookTargetResolver`, must skip when incompatible, or has an optional target that must not be downgraded to avoid deletion.
- `EXPERIMENTAL` / `FREEZE_LEGACY` / `DELETE_DEAD` — none of the 25 catalog entries qualify.

Special rules respected:
- `STATUSBAR_CLOCK_TWEAK` is kept (`KEEP_GUARDED`), not deleted, despite the historical `INSTALLER_FAILED` summary.
- No `REQUIRED` contract target was downgraded to `OPTIONAL` to avoid deletions.
- No HyperOS 1 candidate targets were deleted for lack of samples.

## Classification table

| `FeatureId` | Category | Evidence & recommendation |
|---|---|---|
| `PACKAGE_PERMISSIONS` | `KEEP_GUARDED` | No user-facing pref by design (`FeatureCatalog.kt:41`). Condition always true (`FeatureCatalog.kt:42`). Dispatched from `SystemServerInstaller.java:32`. Contract with optional targets at `CanaryContracts.kt:28` (signature/platform-key/activity-record checks). **Recommendation:** retain; let the resolver handle missing optional ROM targets; do not delete. |
| `STATUS_BAR_CLOCK_TWEAK` | `KEEP_GUARDED` | XML key `pref_key_system_statusbar_clocktweak` at `app/src/main/res/xml/prefs_system_statusbar_clock.xml:9`. Condition reads 4 prefs (`FeatureCatalog.kt:71-74`). Dispatched from `SystemUiInstaller.java:128`. Contract has an optional `MiuiClock.setClockVisibility` target at `CanaryContracts.kt:144`. **Special rule:** not deleted. **Recommendation:** retain with resolver guarding. |
| `AUTO_BRIGHTNESS_RANGE` | `KEEP_GUARDED` | XML key `pref_key_system_autobrightness` at `app/src/main/res/xml/prefs_system_autobrightness.xml:8`. Condition reads `system_autobrightness` (`FeatureCatalog.kt:93-94`). Dispatched from `SystemServerInstaller.java:65`. Contract has `automatic_brightness_controller` and `display_power_controller` variants at `CanaryContracts.kt:78-126`. **Recommendation:** retain; resolver selects the variant at runtime. |
| `MUFFLED_VIBRATION` | `KEEP_GUARDED` | XML key `pref_key_system_vibration_amp` at `app/src/main/res/xml/prefs_system_vibration_amp.xml:8`. Condition reads `system_vibration_amp` (`FeatureCatalog.kt:112-113`). Dispatched from `SystemServerInstaller.java:76`. Explicit class resolve in `compatibilityCheck` (`FeatureCatalog.kt:115-119`). Contract at `CanaryContracts.kt:129`. **Recommendation:** retain; resolver must find `VibratorService`. |
| `NO_MORE_ICON` | `KEEP_GUARDED` | XML key `pref_key_system_hidemoreicon` at `app/src/main/res/xml/prefs_system.xml:471`. Condition reads `system_hidemoreicon` (`FeatureCatalog.kt:137-138`). Dispatched from `SystemUiInstaller.java:139`. Explicit class resolve (`FeatureCatalog.kt:140-144`). Contract at `CanaryContracts.kt:223`. **Recommendation:** retain; resolver-guarded. |
| `BATTERY_INDICATOR` | `KEEP_GUARDED` | XML key `pref_key_system_batteryindicator` at `app/src/main/res/xml/prefs_system_batteryindicator.xml:8`. Condition reads `system_batteryindicator` (`FeatureCatalog.kt:161-162`). Dispatched from `SystemUiInstaller.java:166`. Explicit class resolve (`FeatureCatalog.kt:164-168`). Contract at `CanaryContracts.kt:239`. **Recommendation:** retain; resolver-guarded. |
| `NO_CLOCK_HIDE` | `KEEP_GUARDED` | XML key `pref_key_launcher_noclockhide` at `app/src/main/res/xml/prefs_launcher.xml:214`. Condition reads `launcher_noclockhide` (`FeatureCatalog.kt:186-187`). Dispatched from `LauncherInstaller.java:37`. Explicit class resolve (`FeatureCatalog.kt:189-192`). Contract at `CanaryContracts.kt:307`. **Recommendation:** retain; resolver-guarded. |
| `NO_WIDGET_ONLY` | `KEEP_GUARDED` | XML key `pref_key_launcher_nowidgetonly` at `app/src/main/res/xml/prefs_launcher.xml:220`. Condition reads `launcher_nowidgetonly` (`FeatureCatalog.kt:210-211`). Dispatched from `LauncherInstaller.java:44`. Explicit class resolve (`FeatureCatalog.kt:213-216`). Contract at `CanaryContracts.kt:323`. **Recommendation:** retain; resolver-guarded. |
| `FIX_APP_INFO_LAUNCH` | `KEEP_GUARDED` | XML key `pref_key_launcher_fixlaunch` at `app/src/main/res/xml/prefs_launcher.xml:196`. Condition reads `launcher_fixlaunch` (`FeatureCatalog.kt:333-334`). Dispatched from `LauncherInstaller.java:43`. `AnyOfRequirement` with `ShortcutMenuManager.startAppDetailsActivity` / `Utilities.startDetailsActivityForInfo` at `CatalogContracts.kt:111-132`. **Recommendation:** retain; resolver picks the reachable launcher entry. |
| `FOLDER_COLUMNS` | `KEEP_GUARDED` | XML key `pref_key_launcher_folder_cols` at `app/src/main/res/xml/prefs_launcher.xml:30`. Condition reads `launcher_folder_cols` (`FeatureCatalog.kt:430-431`). Dispatched from `LauncherInstaller.java:33`. Contract has an optional `Folder.resetViewsLayoutParams` target at `CatalogContracts.kt:211-240`. **Recommendation:** retain; optional target must not be deleted. |
| `SCREEN_DIM_TIME` | `KEEP` | XML key `pref_key_system_dimtime` at `app/src/main/res/xml/prefs_system.xml:56`. Condition reads `system_dimtime` (`FeatureCatalog.kt:235-236`). Dispatched from `SystemServerInstaller.java:53`. Contract at `CatalogContracts.kt:24-46`. **Recommendation:** retain, no retirement action. |
| `FIRST_VOLUME_PRESS` | `KEEP` | XML key `pref_key_system_firstpress` at `app/src/main/res/xml/prefs_system.xml:120`. Condition reads `system_firstpress` (`FeatureCatalog.kt:255`). Dispatched from `SystemServerInstaller.java:73`. Contract at `CatalogContracts.kt:48-62`. **Recommendation:** retain, no retirement action. |
| `NETWORK_INDICATOR_WIFI` | `KEEP` | XML key `pref_key_system_networkindicator_wifi` at `app/src/main/res/xml/prefs_system.xml:294`. Condition reads `system_networkindicator_wifi` (`FeatureCatalog.kt:275`). Dispatched from `SystemUiInstaller.java:111`. Contract at `CatalogContracts.kt:64-77`. **Recommendation:** retain, no retirement action. |
| `MUTE_VISIBLE_NOTIFICATIONS` | `KEEP` | XML key `pref_key_system_mutevisiblenotif` at `app/src/main/res/xml/prefs_system.xml:648`. Condition reads `system_mutevisiblenotif` (`FeatureCatalog.kt:294`). Dispatched from `SystemUiInstaller.java:244`. Contract at `CatalogContracts.kt:79-92`. **Recommendation:** retain, no retirement action. |
| `HIDE_LAUNCHER_TITLES` | `KEEP` | XML key `pref_key_launcher_hidetitles` at `app/src/main/res/xml/prefs_launcher.xml:57`. Condition reads `launcher_hidetitles` (`FeatureCatalog.kt:314`). Dispatched from `LauncherInstaller.java:42`. Contract at `CatalogContracts.kt:94-108`. **Recommendation:** retain, no retirement action. |
| `HIDE_PROXIMITY_WARNING` | `KEEP` | XML key `pref_key_system_hideproxywarn` at `app/src/main/res/xml/prefs_system.xml:1316`. Condition reads `system_hideproxywarn` (`FeatureCatalog.kt:352-353`). Dispatched from `SystemServerInstaller.java:72`. Contract at `CatalogContracts.kt:135-159`. **Recommendation:** retain, no retirement action. |
| `CLEAR_ALL_TASKS` | `KEEP` | XML key `pref_key_system_clearalltasks` at `app/src/main/res/xml/prefs_system.xml:911`. Condition reads `system_clearalltasks` (`FeatureCatalog.kt:371`). Dispatched from `SystemServerInstaller.java:77`. Contract at `CatalogContracts.kt:161-174`. **Recommendation:** retain, no retirement action. |
| `HIDE_DISMISS_VIEW` | `KEEP` | XML key `pref_key_system_removedismiss` at `app/src/main/res/xml/prefs_system.xml:490`. Condition reads `system_removedismiss` (`FeatureCatalog.kt:391`). Dispatched from `SystemUiInstaller.java:143`. Contract at `CatalogContracts.kt:176-191`. **Recommendation:** retain, no retirement action. |
| `HIDE_LOCK_SCREEN_HINT` | `KEEP` | XML key `pref_key_system_hidelshint` at `app/src/main/res/xml/prefs_system.xml:1138`. Condition reads `system_hidelshint` (`FeatureCatalog.kt:410`). Dispatched from `SystemUiInstaller.java:208`. Contract at `CatalogContracts.kt:193-209`. **Recommendation:** retain, no retirement action. |
| `TITLE_TOP_MARGIN` | `KEEP` | XML key `pref_key_launcher_titletopmargin` at `app/src/main/res/xml/prefs_launcher.xml:79`. Condition reads `launcher_titletopmargin` (`FeatureCatalog.kt:449`). Dispatched from `LauncherInstaller.java:36`. Contract at `CatalogContracts.kt:244-258`. **Recommendation:** retain, no retirement action. |
| `NO_LIGHT_UP_ON_CHARGE` | `KEEP` | XML key `pref_key_system_nolightuponcharges` at `app/src/main/res/xml/prefs_system.xml:48`. Condition reads `system_nolightuponcharges` (`FeatureCatalog.kt:469-470`). Dispatched from `SystemServerInstaller.java:88`. Contract at `CatalogContracts.kt:262-275`. **Recommendation:** retain, no retirement action. |
| `ALL_ROTATIONS` | `KEEP` | XML key `pref_key_system_allrotations2` at `app/src/main/res/xml/prefs_system.xml:17`. Condition reads `system_allrotations2` (`FeatureCatalog.kt:489`). Dispatched from `SystemServerInstaller.java:87`. Contract at `CatalogContracts.kt:277-291`. **Recommendation:** retain, no retirement action. |
| `NO_NETWORK_SPEED_SEPARATOR` | `KEEP` | XML key `pref_key_system_nonetspeedseparator` at `app/src/main/res/xml/prefs_system_hideicons.xml:33`. Condition reads `system_nonetspeedseparator` (`FeatureCatalog.kt:508-509`). Dispatched from `SystemUiInstaller.java:215`. Contract at `CatalogContracts.kt:293-316`. **Recommendation:** retain, no retirement action. |
| `HIDE_ICONS_CLOCK` | `KEEP` | XML key `pref_key_system_statusbaricons_clock` at `app/src/main/res/xml/prefs_system_hideicons.xml:28`. Condition reads `system_statusbaricons_clock` (`FeatureCatalog.kt:527-528`). Dispatched from `SystemUiInstaller.java:216`. Contract at `CatalogContracts.kt:318-335`. **Recommendation:** retain, no retirement action. |
| `NO_UNLOCK_ANIMATION` | `KEEP` | XML key `pref_key_launcher_nounlockanim` at `app/src/main/res/xml/prefs_launcher.xml:232`. Condition reads `launcher_nounlockanim` (`FeatureCatalog.kt:548`). Dispatched from `LauncherInstaller.java:64`. Contract at `CatalogContracts.kt:335-347`. **Recommendation:** retain, no retirement action. |

## Counts

| Category | Count |
|---|---|
| `KEEP` | 15 |
| `KEEP_GUARDED` | 10 |
| `EXPERIMENTAL` | 0 |
| `FREEZE_LEGACY` | 0 |
| `DELETE_DEAD` | 0 |

## Notes

- No `FeatureId` was assigned `DELETE_DEAD`.
- No code was removed; this is an audit-only branch.
- `proguard-rules.pro` contains no `-keep` references to these preference keys, so the R8 gate is not a blocker.
