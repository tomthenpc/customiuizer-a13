# A13 Dead Code Candidates

> Branch: `devin/a13-rom-intelligence-audit`
> Result: **0 `DELETE_DEAD` candidates identified.** No code is deleted.

## `DELETE_DEAD` mechanical gates

A feature or preference key is only a `DELETE_DEAD` candidate when **all** of the following are true:

1. No XML `android:key` in `app/src/main/res/xml/prefs_*.xml`.
2. No `MainModule.mPrefs` / `PrefMap` read in `app/src/main/java`.
3. No `FeatureDispatcher.installById(...)` / installer invocation.
4. No `CanaryContracts.kt` / `CatalogContracts.kt` contract target.
5. No `app/proguard-rules.pro` R8 keep reference to the key or hook class.

## Catalog `FeatureId` gate check

All 25 typed catalog `FeatureId`s fail the `DELETE_DEAD` definition because at least one gate is satisfied. A sample of the gate evidence:

| `FeatureId` | XML key | mPrefs / condition | Installer call | Contract | R8 keep | Verdict |
|---|---|---|---|---|---|---|
| `PACKAGE_PERMISSIONS` | none (by design) | condition `{ true }` `FeatureCatalog.kt:42` | `SystemServerInstaller.java:32` | `CanaryContracts.kt:28` | none | not dead |
| `STATUS_BAR_CLOCK_TWEAK` | `pref_key_system_statusbar_clocktweak` `prefs_system_statusbar_clock.xml:9` | `FeatureCatalog.kt:71-74` | `SystemUiInstaller.java:128` | `CanaryContracts.kt:144` | none | not dead |
| `AUTO_BRIGHTNESS_RANGE` | `pref_key_system_autobrightness` `prefs_system_autobrightness.xml:8` | `FeatureCatalog.kt:93-94` | `SystemServerInstaller.java:65` | `CanaryContracts.kt:123` | none | not dead |
| `MUFFLED_VIBRATION` | `pref_key_system_vibration_amp` `prefs_system_vibration_amp.xml:8` | `FeatureCatalog.kt:112-113` | `SystemServerInstaller.java:76` | `CanaryContracts.kt:129` | none | not dead |
| `NO_MORE_ICON` | `pref_key_system_hidemoreicon` `prefs_system.xml:471` | `FeatureCatalog.kt:137-138` | `SystemUiInstaller.java:139` | `CanaryContracts.kt:223` | none | not dead |
| `BATTERY_INDICATOR` | `pref_key_system_batteryindicator` `prefs_system_batteryindicator.xml:8` | `FeatureCatalog.kt:161-162` | `SystemUiInstaller.java:166` | `CanaryContracts.kt:239` | none | not dead |
| `NO_CLOCK_HIDE` | `pref_key_launcher_noclockhide` `prefs_launcher.xml:214` | `FeatureCatalog.kt:186-187` | `LauncherInstaller.java:37` | `CanaryContracts.kt:307` | none | not dead |
| `NO_WIDGET_ONLY` | `pref_key_launcher_nowidgetonly` `prefs_launcher.xml:220` | `FeatureCatalog.kt:210-211` | `LauncherInstaller.java:44` | `CanaryContracts.kt:323` | none | not dead |
| `SCREEN_DIM_TIME` | `pref_key_system_dimtime` `prefs_system.xml:56` | `FeatureCatalog.kt:235-236` | `SystemServerInstaller.java:53` | `CatalogContracts.kt:24` | none | not dead |
| `FIRST_VOLUME_PRESS` | `pref_key_system_firstpress` `prefs_system.xml:120` | `FeatureCatalog.kt:255` | `SystemServerInstaller.java:73` | `CatalogContracts.kt:49` | none | not dead |
| `NETWORK_INDICATOR_WIFI` | `pref_key_system_networkindicator_wifi` `prefs_system.xml:294` | `FeatureCatalog.kt:275` | `SystemUiInstaller.java:111` | `CatalogContracts.kt:65` | none | not dead |
| `MUTE_VISIBLE_NOTIFICATIONS` | `pref_key_system_mutevisiblenotif` `prefs_system.xml:648` | `FeatureCatalog.kt:294` | `SystemUiInstaller.java:244` | `CatalogContracts.kt:80` | none | not dead |
| `HIDE_LAUNCHER_TITLES` | `pref_key_launcher_hidetitles` `prefs_launcher.xml:57` | `FeatureCatalog.kt:314` | `LauncherInstaller.java:42` | `CatalogContracts.kt:95` | none | not dead |
| `FIX_APP_INFO_LAUNCH` | `pref_key_launcher_fixlaunch` `prefs_launcher.xml:196` | `FeatureCatalog.kt:333-334` | `LauncherInstaller.java:43` | `CatalogContracts.kt:111` | none | not dead |
| `HIDE_PROXIMITY_WARNING` | `pref_key_system_hideproxywarn` `prefs_system.xml:1316` | `FeatureCatalog.kt:352-353` | `SystemServerInstaller.java:72` | `CatalogContracts.kt:137` | none | not dead |
| `CLEAR_ALL_TASKS` | `pref_key_system_clearalltasks` `prefs_system.xml:911` | `FeatureCatalog.kt:371` | `SystemServerInstaller.java:77` | `CatalogContracts.kt:161` | none | not dead |
| `HIDE_DISMISS_VIEW` | `pref_key_system_removedismiss` `prefs_system.xml:490` | `FeatureCatalog.kt:391` | `SystemUiInstaller.java:143` | `CatalogContracts.kt:177` | none | not dead |
| `HIDE_LOCK_SCREEN_HINT` | `pref_key_system_hidelshint` `prefs_system.xml:1138` | `FeatureCatalog.kt:410` | `SystemUiInstaller.java:208` | `CatalogContracts.kt:193` | none | not dead |
| `FOLDER_COLUMNS` | `pref_key_launcher_folder_cols` `prefs_launcher.xml:30` | `FeatureCatalog.kt:430-431` | `LauncherInstaller.java:33` | `CatalogContracts.kt:211` | none | not dead |
| `TITLE_TOP_MARGIN` | `pref_key_launcher_titletopmargin` `prefs_launcher.xml:79` | `FeatureCatalog.kt:449` | `LauncherInstaller.java:36` | `CatalogContracts.kt:244` | none | not dead |
| `NO_LIGHT_UP_ON_CHARGE` | `pref_key_system_nolightuponcharges` `prefs_system.xml:48` | `FeatureCatalog.kt:469-470` | `SystemServerInstaller.java:88` | `CatalogContracts.kt:262` | none | not dead |
| `ALL_ROTATIONS` | `pref_key_system_allrotations2` `prefs_system.xml:17` | `FeatureCatalog.kt:489` | `SystemServerInstaller.java:87` | `CatalogContracts.kt:277` | none | not dead |
| `NO_NETWORK_SPEED_SEPARATOR` | `pref_key_system_nonetspeedseparator` `prefs_system_hideicons.xml:33` | `FeatureCatalog.kt:508-509` | `SystemUiInstaller.java:215` | `CatalogContracts.kt:293` | none | not dead |
| `HIDE_ICONS_CLOCK` | `pref_key_system_statusbaricons_clock` `prefs_system_hideicons.xml:28` | `FeatureCatalog.kt:527-528` | `SystemUiInstaller.java:216` | `CatalogContracts.kt:318` | none | not dead |
| `NO_UNLOCK_ANIMATION` | `pref_key_launcher_nounlockanim` `prefs_launcher.xml:232` | `FeatureCatalog.kt:548` | `LauncherInstaller.java:64` | `CatalogContracts.kt:335` | none | not dead |

## Wider XML preference-key sweep

A mechanical sweep of the whole `prefs_*.xml` surface was also performed to guard against orphan keys:

- 628 unique `android:key` values exist in `app/src/main/res/xml/prefs_*.xml`.
- 401 of those full keys do **not** appear as a literal string in `app/src/main/java` or `app/proguard-rules.pro`.
- However, every one of those 401 keys has a shorter prefix (the base preference name) that is read by `MainModule.mPrefs` / `PrefMap`, referenced by `findPreference(...)`, or used in a concatenated subkey read.
- Therefore **none of the 401 keys is a `DELETE_DEAD` candidate** under the strict "no mPrefs read, no source reference" rule.

## Recommendation

- **No code is deleted on this branch.**
- All 25 catalog `FeatureId`s remain reachable and protected by contracts; the 15 `KEEP` and 10 `KEEP_GUARDED` classifications are in `A13_FEATURE_RETIREMENT_AUDIT.md`.
- `STATUSBAR_CLOCK_TWEAK` is retained despite the historical `INSTALLER_FAILED` log; no `REQUIRED` target is downgraded; no HyperOS 1 candidate is removed.
- Legacy, non-catalog hooks (for example `system_networkindicator_mobile` still invoked by `SystemUiInstaller.java`) are **not** `DELETE_DEAD` candidates because they retain `mPrefs` reads and hook invocations.
