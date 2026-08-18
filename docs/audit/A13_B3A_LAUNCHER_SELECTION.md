# A13 B3A — Launcher HYBRID Architecture Selection

| Field | Value |
|---|---|
| **AUTHORITATIVE_BASE_SHA** | `c4dbc907953de6bf445ab180d89d59f1a61a87d3` |
| **Task** | READ-ONLY static architecture selection |
| **PRODUCTION_AUTHORIZATION** | NO |
| **Scope** | `LauncherInstaller` HYBRID routing + 7 existing Launcher catalog entries |

Frozen (do not reopen): A1, A2, A3, B1, B2A, B2B (`c4dbc907…`). Inventory baseline drift deferred to Phase-B final closure.

---

## 1. Executive summary

Launcher is a **HYBRID** installer:

- **PackageReady family** — legacy direct hooks + `ResourceHooks`; **no** `FeatureDispatcher`.
- **Application.attach family** — always `FeatureDispatcher.createRuntime`, then **7 catalog** + **~36 legacy direct** hook installs.

Static audit result:

| Metric | Count |
|---|---|
| **B3A_CATALOG_MIGRATION_CANDIDATES** | **0** |
| **B3A_CONFIRMED_CORRECTIVES** | **2 landed (D1, D2)**; D3/D4/D5 = **CONFIRMED / NOT_YET_IMPLEMENTED** |
| LIKELY_DEFECT | remaining ownership / attach items (R2) |
| COMPATIBILITY_GAP | 4 |
| ARCHITECTURE_DEBT | 6 |
| INSUFFICIENT_EVIDENCE | 5 |

B3A is **not CLOSED**. R1 landed D1/D2 only.

**Recommendation:** keep current hybrid shape. Do **not** migrate PackageReady legacy features to catalog (first-load cost + no concrete benefit). Do **not** migrate remaining Application legacy features to catalog without a **confirmed** corrective that catalog uniquely fixes (install-once, process identity, or resolver alignment)—none qualify this round.

A3 `Application.attach` package-identity filter **remains frozen**; no reopen required for Launcher-only findings.

---

## 2. Launcher route map

### 2.1 Entry (`MainModule.onPackageReady`)

```text
onModuleLoaded → processName = param.getProcessName()          MainModule.java:55
onPackageReady:
  if (!lpparam.isFirstPackage()) return                         MainModule.java:91
  scope = ProcessScopes.resolve(pkg, processName)              MainModule.java:94
  if (ProcessScopes.isRejected(...)) return
  if (!PreferenceLoadRegistry.shouldLoad(remote, pkg)) return
  initPrefs()

  scope == LAUNCHER  (com.miui.home | com.mi.android.globallauncher)
    if hasAnyLauncherPackageReadyFeature(mPrefs)
        LauncherInstaller.installPackageReady(lpparam)           MainModule.java:165-166
    if hasAnyLauncherApplicationFeature(mPrefs)
        LauncherInstaller.installApplication(lpparam)            MainModule.java:168-169
        watchPreferenceChange()                                  MainModule.java:171-172
```

### 2.2 PackageReady family

```text
LauncherInstaller.installPackageReady(lpparam)                 LauncherInstaller.java:30-45
  → LauncherLayoutHooks.* (Res + method hooks)
  → LauncherSystemHooks.DisableLauncherLogHook
  (no FeatureDispatcher / no catalog)
```

### 2.3 Application.attach family

```text
LauncherInstaller.installApplication(lpparam)                  LauncherInstaller.java:104-111
  ModuleHelper.findAndHookMethod(Application.class, "attach", ...)
    after:
      if (!isTargetPackage(thisObject, lpparam)) return        A3 package filter :108
      handleLoadLauncher(lpparam)                              :109

handleLoadLauncher(lpparam)                                    LauncherInstaller.java:47-102
  FeatureRuntime = FeatureDispatcher.createRuntime(
      lpparam.getPackageName(),   ← package name, NOT processName
      lpparam, lpparam.getClassLoader(), mPrefs)                :49
  legacy direct hooks (pref-gated)
  FeatureDispatcher.installById(...) × 7 catalog ids
  com.miui.home-only block :74-99
```

### 2.4 Catalog bootstrap side effect

Any Application-family entry pays:

```text
FeatureDispatcher.<clinit> → FeatureInstallRegistry.registerAll(FeatureCatalog.specs())
```

Full catalog registration runs even when only legacy prefs are enabled (`FeatureDispatcher.kt:23-25`). This is **already sunk cost** for Application family; it is **not** paid by PackageReady-only users.

---

## 3. Package / process / ClassLoader matrix

| Concept | Launcher static fact | Evidence |
|---|---|---|
| **PACKAGE** | `com.miui.home` or `com.mi.android.globallauncher` | `ProcessScopes.kt:46-47,92` |
| **PROCESS (OS)** | `MainModule.processName` from `onModuleLoaded` | `MainModule.java:55` |
| **ProcessScope** | Always `LAUNCHER` for both packages; **no** main/secondary split | `ProcessScopes.kt:92` (contrast `SECURITY_CENTER_*`, `SYSTEM_UI_PLUGIN`) |
| **ProcessTarget.Launcher** | Matches **package name strings** only | `ProcessTarget.kt:27-31` |
| **FeatureRuntime.processName** | `lpparam.getPackageName()` at create site | `LauncherInstaller.java:49` |
| **PackageReadyParam.classLoader** | Target launcher APK loader | passed through runtime |
| **Application.classLoader** | Same package loader as `lpparam` on attach | A3 proof §10 |

### 3.1 HIGH PRIORITY 1 — process identity Q&A

| Question | Static answer | Classification |
|---|---|---|
| **A. Launcher only enters `handleLoadLauncher` from main process?** | **Not proven.** Entry is `Application.attach` after A3 filter. Typical MIUI launcher is single-process for UI, but `ProcessScopes` does not reject `:remote`/`:widget` style names. | INSUFFICIENT_EVIDENCE |
| **B. Secondary process production-reachable?** | Module loads per-process; `isFirstPackage()` is per-process true. Secondary **could** register its own attach hook if prefs gate passes. No in-repo MIUI manifest for launcher secondary components. | INSUFFICIENT_EVIDENCE |
| **C. Secondary reachable → `createRuntime(packageName)` masks identity?** | **Yes, statically.** Secondary process `com.miui.home:remote` would still pass `processName="com.miui.home"` into registry keys and `ProcessTarget.Launcher.matches`. Diagnostics/registry key would not distinguish main vs secondary. | ARCHITECTURE_DEBT (F1 P1-3 equivalent) |
| **D. Catalog installer ClassLoader from secondary?** | If secondary process receives `onPackageReady`, `lpparam.classLoader` is that process loader; hooks target classes visible there. | INSUFFICIENT_EVIDENCE (needs ROM manifest + runtime log) |
| **E. Legacy hooks also install in secondary?** | Same attach path; legacy calls are **not** registry-guarded. If attach fires in secondary, all pref-gated legacy hooks in `handleLoadLauncher` run. | LIKELY_DEFECT **if** secondary is reachable and hooks assume main UI |
| **F. A3 filter solves cross-process?** | **No.** A3 solves **cross-package** same-process attach (`isTargetPackage`). Same-package second `Application` or secondary process same package name is **out of A3 scope** (frozen). | ARCHITECTURE_DEBT |

**Static structure proves:** package-name-based runtime identity + undifferentiated `ProcessScope.LAUNCHER`.

**Missing evidence to upgrade to CONFIRMED:** launcher APK manifest `android:process` entries; logcat of `MainModule.processName` vs `lpparam.getPackageName()` on user devices; reproduction of hooks running in non-UI launcher process.

---

## 4. PackageReady family matrix

| # | Pref gate | Hook entry | Catalog | Disabled skips catalog? | Install-once | Recommendation |
|---|---|---|---|---|---|---|
| 1 | `launcher_horizmargin > 0` | `HorizontalSpacingRes` | no | n/a | `ResourceHooks` bit mask | **KEEP_LEGACY_SAFE** |
| 2 | `launcher_indicatorheight > 9` | `IndicatorHeightRes` | no | n/a | ResourceHooks | **KEEP_LEGACY_SAFE** |
| 3 | `launcher_indicator_topmargin > 0` | `IndicatorMarginTopHook` | no | n/a | `isFirstPackage` | **KEEP_LEGACY_SAFE** |
| 4 | `launcher_unlockgrids` | `UnlockGridsRes` + `UnlockGridsHook` | no | n/a | ResourceHooks + first package | **KEEP_LEGACY_SAFE** |
| 5 | `launcher_docktitles` | `ShowHotseatTitlesRes` | no | n/a | ResourceHooks | **KEEP_LEGACY_SAFE** |
| 6 | `launcher_disable_log` | `DisableLauncherLogHook` | no | n/a | first package | **KEEP_LEGACY_SAFE** |
| 7 | `launcher_topmargin > 0` | `WorkspaceCellPaddingTopHook` | no | n/a | first package | **KEEP_LEGACY_SAFE** |
| 8 | `launcher_dock_topmargin > 0` | `DockMarginTopHook` | no | n/a | first package | **KEEP_LEGACY_SAFE** |
| 9 | `launcher_dock_bottommargin > 0` | `DockMarginBottomHook` | no | n/a | first package | **KEEP_LEGACY_SAFE** |

**PackageReady family properties:**

- **No FeatureDispatcher** when only these prefs enabled → **no** full catalog `<clinit>` cost.
- **ResourceHooks** uses atomic install state + per-method bitmask (`ResourceHooks.java:92-186`).
- Migrating to catalog would force Application-family bootstrap or duplicate catalog init at package-ready → **negative** cost; **0 migration value**.

`PACKAGE_READY_FEATURE_COUNT = 9` (pref gates).

---

## 5. Application family matrix (legacy direct)

Grouped by pref gate in `handleLoadLauncher` / `hasAnyLauncherApplicationFeature`. All install from **Application.attach** after A3 filter. **None** use `FeatureInstallRegistry`.

| Group | Pref(s) | Hook module | `com.miui.home` only? | Recommendation |
|---|---|---|---|---|
| Homescreen swipes | `launcher_swipe*_action != 1` | `LauncherGestureHooks.HomescreenSwipesHook` | no | **KEEP_LEGACY_SAFE** |
| Hotseat swipes | `launcher_swipeleft/right_action` | `HotSeatSwipesHook` | no | **KEEP_LEGACY_SAFE** |
| Shake / double-tap / pinch | respective `*_action` | `ShakeHook`, `LauncherDoubleTapHook`, `LauncherPinchHook` | no | **KEEP_LEGACY_SAFE** |
| Icon scale / title size | `launcher_iconscale`, `launcher_titlefontsize` | `LauncherIconHooks` | no | **KEEP_LEGACY_SAFE** |
| Rename / shadow | `launcher_renameapps`, `launcher_darkershadow` | `LauncherIconHooks` | no | **KEEP_LEGACY_SAFE** |
| Hide nav bar | `controls_nonavbar` | `LauncherLayoutHooks.HideNavBarHook` | no | **KEEP_LEGACY_SAFE** |
| Infinite scroll | `launcher_infinitescroll` | `InfiniteScrollHook` | no | **KEEP_LEGACY_SAFE** |
| Sensor portrait | `launcher_sensorportrait` | `ReverseLauncherPortraitHook` | no | **KEEP_LEGACY_SAFE** |
| Unlock hotseat | `launcher_unlockhotseat` | `MaxHotseatIconsCountHook` | no (has globallauncher branch) | **KEEP_LEGACY_SAFE** |
| Close folders | `launcher_closefolders > 1` | `CloseFolderOnLaunchHook` + shortcut menu | no | **KEEP_LEGACY_SAFE** |
| Recents blur | `system_recents_blur < 100` | `RecentsBlurRatioHook` | **yes** `:74` | **KEEP_LEGACY_SAFE** (compat gap for globallauncher) |
| FSG coverage/width/horiz | `controls_fsg_*` | `Controls.*`, `FSGesturesHook` | **yes** | **COMPATIBILITY_GAP** |
| Memory cleaner / wallpaper / sticky FW / recents SB / split / fix anim / seek points / privacy folder / hide recents / folder blur / no-zoom / old anim / close drawer / widget margin / assist / swipe-stop | various `system_*`, `launcher_*`, `controls_*` | mixed | **yes** block | **KEEP_LEGACY_SAFE** (miui.home-intended) |
| Resizable widgets | `system_resizablewidgets` | `ResizableWidgetsHook` | no | **KEEP_LEGACY_SAFE** |

**APPLICATION legacy direct count:** **36** distinct pref-gated legacy paths (excluding 7 catalog ids).

**Application family sunk cost:** entering `handleLoadLauncher` **always** calls `createRuntime` → catalog registration already paid.

**Migration opposition (Application legacy → catalog):**

- Catalog `<clinit>` cost **already paid** — “avoid first catalog load” is **not** a valid blocker here.
- Still **0 candidates** because migration provides **no** proven fix for: legacy install-once gap (registry does not wrap direct calls), process identity key, nested gesture ownership, or resolver/target drift.
- Catalog migration without corrective would add contract surface area only.

---

## 6. Current catalog matrix (7 entries)

| Feature id | Pref | Condition | processScope | processTarget | installPhase (spec) | Contract selected target | Actual installer hook target | Install-once key | Verdict |
|---|---|---|---|---|---|---|---|---|---|
| `folderColumns` | `launcher_folder_cols` | `> 1` | LAUNCHER | Launcher | PACKAGE_READY | `Folder.onFinishInflate`, `onLayout`, optional `resetViewsLayoutParams` | Same (`FolderColumnsHook`) | `(runtime.processName, folderColumns)` | **KEEP_CURRENT_CATALOG** |
| `titleTopMargin` | `launcher_titletopmargin` | `> 0` | LAUNCHER | Launcher | PACKAGE_READY | `ItemIcon.onFinishInflate` | Same (`TitleTopMarginHook`) | registry | **KEEP_CURRENT_CATALOG** |
| `noClockHide` | `launcher_noclockhide` | boolean | LAUNCHER | Launcher | PACKAGE_READY | `Launcher.updateStatusBarClock` | Same (`NoClockHideHook`) | registry | **KEEP_CURRENT_CATALOG** |
| `hideLauncherTitles` | `launcher_hidetitles` | boolean | LAUNCHER | Launcher | PACKAGE_READY | `ItemIcon.onFinishInflate` | Same (`HideTitlesHook`) | registry | **KEEP_CURRENT_CATALOG** |
| `fixAppInfoLaunch` | `launcher_fixlaunch` | boolean | LAUNCHER | Launcher | PACKAGE_READY | AnyOf `ShortcutMenuManager.startAppDetailsActivity` **or** `Utilities.startDetailsActivityForInfo` | Package branch: home→ShortcutMenuManager; globallauncher→Utilities (`FixAppInfoLaunchHook:40-76`) | registry | **KEEP_CURRENT_CATALOG** |
| `noWidgetOnly` | `launcher_nowidgetonly` | boolean | LAUNCHER | Launcher | PACKAGE_READY | `CellLayout.setScreenType` | Same (`NoWidgetOnlyHook`) | registry | **KEEP_CURRENT_CATALOG** |
| `noUnlockAnimation` | `launcher_nounlockanim` | boolean | LAUNCHER | Launcher | PACKAGE_READY | `MiuiSettingsUtils.isSystemAnimationOpen` | Same (`NoUnlockAnimationHook`) | registry | **KEEP_CURRENT_CATALOG** (installer only calls from `com.miui.home` block — see gap) |

**Resolver vs hook alignment:** Unit tests `CatalogBatch1Test`, `CatalogBatch2Test`, `CatalogBatch3Test`, `FeatureCatalogTest` install with fixture classloaders and assert `INSTALLED`. Static contract member names match legacy hook class/method strings above.

**Semantic debt:** all 7 specs declare `installPhase = PACKAGE_READY` but production installs them from **`Application.attach`** path (`handleLoadLauncher`). Registry validates spec phase, **not** call-site phase → label mismatch only (**ARCHITECTURE_DEBT**).

**Unconditional catalog calls:** `noClockHide` and `noWidgetOnly` always invoke `installById` (`LauncherInstaller.java:63,70`); disabled prefs fail at registry `condition` → cheap no-op after runtime creation.

**CURRENT_CATALOG_FEATURE_COUNT = 7**

---

## 7. Install-once matrix

| Layer | Mechanism | Protects | Does not protect |
|---|---|---|---|
| `MainModule` | `lpparam.isFirstPackage()` | One `installApplication` hook **registration** per process | Repeated `attach` callback execution |
| A3 | `isTargetPackage(Application, lpparam)` | Cross-package attach in same process | Same-package duplicate attach; secondary process attach |
| `FeatureInstallRegistry` | `FeatureStateKey(processName, canonicalId)` | 7 catalog features per runtime.processName | Legacy direct `ModuleHelper.findAndHookMethod` in `handleLoadLauncher` |
| `ResourceHooks` | atomic install state + mask | PackageReady Res hooks | Application family |
| Legacy local | `HotSeatGestureState` on View via additional instance field | per-View gesture state | n/a |

**A3 frozen design:** package-identity filter only. A3 proof (`A13_A3_APPLICATION_ATTACH_INSTALL_ONCE_PROOF.md`) documents optional `Set<String>` defense-in-depth **not** implemented — do not reinvent this round.

**Duplicate attach risk (legacy):** If same-package `Application.attach` fires twice abnormally, ~36 legacy hook sites re-run (`ModuleHelper.findAndHookMethod`); catalog paths return `AlreadyInstalled`. Classification: **LIKELY_DEFECT** / **CORRECTIVE_BEFORE_MIGRATION** (local attach guard or legacy registry — **not** catalog migration by itself).

---

## 8. Lifecycle / ownership matrix (Launcher reachable)

| Site | Owner | Span | Release | Stale risk | Classification |
|---|---|---|---|---|---|
| `HotSeatGestureState` on hotseat View | **VIEW_SCOPED** (additional instance field) | View lifetime | GC with View | low | OK |
| `RenameShortcutsHook` preference observer | **ACTIVITY_SCOPED** (`observeOwnedPreferenceChange` on Launcher) | Activity | ModuleHelper owner binding | medium if Activity replaced | OK pattern |
| `HideSeekPointsHook` Handler on workspace | **VIEW_SCOPED** | workspace View | none explicit | Handler may outlive if View leaks | ARCHITECTURE_DEBT (minor) |
| `AssistGestureActionHook.inDirection` int array | **PROCESS_SINGLETON** mutable | process | none | cross-gesture bleed | ARCHITECTURE_DEBT (minor) |
| `StickyFloatingWindowsLauncherHook` module receiver | **CONTEXT_SCOPED** via `registerModuleReceiver` | RecentsContainer context | not unregistered on detach in snippet | receiver leak if container respawns | LIKELY_DEFECT (needs lifecycle proof) |
| Catalog / legacy class-level hooks | **CLASSLOADER_SCOPED** | process | none | intentional | OK |

No static strong `Activity`/`Fragment` module fields found in Launcher hook modules (contrast B2B `mSupportFragment`).

---

## 9. Failure / fatal matrix (Launcher reachable)

| Path | Ordinary failure | Fatal handling | Wrapped fatal |
|---|---|---|---|
| `FeatureDispatcher` / `FeatureInstallRegistry` | fail-open per feature | **B3A-D2 landed:** `RuntimeFatality.throwIfFatal`; fatal removes `INSTALLING` | wrapped fatal propagates original |
| `LauncherSystemHooks`, `LauncherIconHooks`, `LauncherAnimationHooks`, `LauncherFolderHooks` reachable OOM-only catches | log / ordinary fallback | **B3A-D1 landed:** `RuntimeFatality.throwIfFatal` | wrapped fatal propagates; fallback skipped |
| `ModuleHelper.findAndHookMethod` (majority) | fail-open at install | ModuleHelper boundary | varies |

`FeatureDispatcher` was **not** modified in R1. D3/D4/D5 remain **NOT_YET_IMPLEMENTED**.

---

## 10. Hot-path matrix

| Hook area | Cold (install) | Hot (callback) | Notes |
|---|---|---|---|
| Gesture swipes | class/method resolve | pref read + `GlobalActions.handleAction` | acceptable |
| FSG / Assist | `findClassIfExists` | touch dispatch, float compares | no disk I/O |
| Folder columns | 3 method hooks | layout/inflate | per-callback pref read |
| Icon title margin / hide | single onFinishInflate | field access | low |
| Recents blur | multiple `hookAllMethods` | blur utils calls | medium reflection in callback |
| Rename shortcuts | constructor hooks + observer | iteration over app set on pref change | cold path for pref change |

Performance findings are **not** upgraded to corrective unless high-frequency regression is proven on device.

---

## 11. Package variant matrix (`com.miui.home` vs `com.mi.android.globallauncher`)

| Behavior | `com.miui.home` | `com.mi.android.globallauncher` | Classification |
|---|---|---|---|
| ProcessScope / routing | LAUNCHER | LAUNCHER | OK |
| FSG / recents / controls block | installed (`:74-99`) | **skipped** | **COMPATIBILITY_GAP** (documented Issue #2 intelligence) |
| `noUnlockAnimation` catalog | installed | **skipped** (inside home block) | **COMPATIBILITY_GAP** if pref enabled on global build |
| `fixAppInfoLaunch` | ShortcutMenuManager path | Utilities path | **OK** (explicit branch) |
| `MaxHotseatIconsCountHook` | `getHotseatMaxCount` | `getHotseatCount` | **OK** (explicit branch) |
| Contract class names `com.miui.home.launcher.*` | assumed present | **INSUFFICIENT_EVIDENCE** global APK exposes same MIUI home DEX symbols | **COMPATIBILITY_GAP** / INSUFFICIENT_EVIDENCE |

Do **not** auto-remove `com.miui.home` guards without ROM ABI proof for globallauncher.

---

## 12. Finding classification summary

### CONFIRMED_DEFECT

1. **B3A-D1** — Launcher reachable OOM-only callback catches swallow wrapped fatal. **CONFIRMED. Landed R1.**
2. **B3A-D2** — `FeatureInstallRegistry.isFatal` direct-only; wrapped fatal swallowed. **CONFIRMED. Landed R1.**
3. **B3A-D3** — **CONFIRMED / NOT_YET_IMPLEMENTED** (R2).
4. **B3A-D4** — **CONFIRMED / NOT_YET_IMPLEMENTED** (R2).
5. **B3A-D5** — **CONFIRMED / NOT_YET_IMPLEMENTED** (R2).

### LIKELY_DEFECT

1. **Legacy direct hooks re-install on abnormal same-package repeat `Application.attach`** (registry does not cover).
2. **`StickyFloatingWindowsLauncherHook` receiver** — register on `onAttachedToWindow` without paired unregister in snippet. Ownership redesign is **not** R1.

### COMPATIBILITY_GAP — 4

1. FSG / miui.home-only block not applied to `com.mi.android.globallauncher`.
2. `noUnlockAnimation` catalog unreachable on globallauncher despite pref in `hasAnyLauncherApplicationFeature`.
3. Catalog contracts assume `com.miui.home.launcher.*` types on both packages.
4. `hasAnyLauncherApplicationFeature` includes miui.home-only prefs → attach hook registered even when only home-only prefs set on globallauncher package (wasted attach path).

### ARCHITECTURE_DEBT — 6

1. HYBRID dual routing (PackageReady legacy vs Application catalog+legacy).
2. `FeatureRuntime.processName` stores **package name**, not OS process name (F1 P1-3).
3. `ProcessScope.LAUNCHER` does not split main/secondary (by design today).
4. Catalog `installPhase=PACKAGE_READY` vs Application.attach call site.
5. Dual install-once systems (`isFirstPackage` + registry).
6. `AssistGestureActionHook` process-local `inDirection` mutable array.

### INSUFFICIENT_EVIDENCE — 5

1. Launcher secondary process production reachability.
2. Hooks running in secondary process causing user-visible bug.
3. globallauncher DEX parity for `com.miui.home.launcher.*` contracts.
4. Wrapped fatal swallow on Launcher **device** path (unit-tested in R1; not DEVICE_VERIFIED).
5. Receiver leak on recents lifecycle.

---

## 13. Per-feature recommendations

### Legacy features — all **KEEP_LEGACY_SAFE** unless noted

No **CATALOG_MIGRATION_VALUE** for any legacy PackageReady or Application feature.

**CORRECTIVE_BEFORE_MIGRATION** (future, not B3A implementation):

- Attach-callback legacy install-once guard (same-package duplicate) — **before** any legacy→catalog bulk move.
- `RuntimeFatality` on Launcher OOM-only catches — **landed B3A-D1**.
- globallauncher routing policy — product/compat decision, not catalog shape.

### Catalog features — all **KEEP_CURRENT_CATALOG**

None require **CORRECTIVE_REQUIRED** at catalog layer; globallauncher gaps are **installer routing**, not wrong contract target for `com.miui.home`.

---

## 14. A3 reopen assessment

```text
A3_REOPEN_REQUIRED = NO
```

A3 package filter is present (`LauncherInstaller.java:108-122`). Findings are **same-package repeat**, **secondary process identity**, and **legacy install-once** — explicitly out of frozen A3 scope unless ChatGPT authorizes new corrective gate.

---

## 15. Validation (this task)

```text
PRODUCTION_CHANGED = YES (R1 D1/D2 only; authorized files)
TEST_CHANGED       = YES (targeted)
FULL_GATE_RUN      = NO
B3A_CLOSED         = NO
```

Known inventory baseline drift may fail `tools/tests/test_hook_ownership_inventory.py` if run — **do not sync** this round.

---

## 16. Counts for gatekeeper report

```text
PACKAGE_READY_FEATURE_COUNT      = 9
APPLICATION_FEATURE_COUNT      = 36 legacy direct + 7 catalog = 43 pref-gated paths
CURRENT_CATALOG_FEATURE_COUNT  = 7
B3A_CATALOG_MIGRATION_CANDIDATES = 0
B3A_CONFIRMED_CORRECTIVES      = 2 landed (D1, D2); D3/D4/D5 CONFIRMED / NOT_YET_IMPLEMENTED
B3A_CLOSED                     = NO
```
