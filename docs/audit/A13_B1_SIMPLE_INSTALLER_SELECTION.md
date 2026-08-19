# A13 B1 — Simple Installer Architecture Selection

## 元信息

| 项目 | 值 |
|---|---|
| AUTHORITATIVE_BASE_SHA | `03a7a082048c028c185eaf351ea167af6bdb4697` |
| BRANCH | `devin/a13-foundation-parity-r13.11.1` |
| PHASE_A_FREEZE_SHA | `03a7a082048c028c185eaf351ea167af6bdb4697` |
| PINNED_A14_REFERENCE | `tomthenpc/customiuizer-a14` @ `d20d96b543a49a584970e312da7d704958a155aa` |
| PRODUCTION_AUTHORIZATION | NO |
| PRODUCTION_CHANGED | NO |
| 性质 | 只读静态选型 / 文档证据 |
| 范围 | 5 个 LEGACY_DIRECT installer 及其实际调用的 hook、ProcessScopes / MainModule 路由、catalog 中与这些 feature 直接相关的部分 |

```text
STATIC_VERIFIED = YES
BUILD_VERIFIED  = NO   (docs-only; Android compile not required)
LOG_VERIFIED    = NO
DEVICE_VERIFIED = NO
B1_PRODUCTION_CANDIDATES = none
B1_PRODUCTION_AUTHORIZATION = NO
B2_STARTED = NO
```

本文件是 Phase B1 的选型证据，不是 PASS authority。ChatGPT 是最终 Gatekeeper。

---

## 1. 结论先行

这 5 个 installer 都已经是 `ProcessScope` 单次路由 + `lpparam.isFirstPackage()` + 关闭功能不建业务 Hook 的 legacy 直装路径。它们当前**不经过** `FeatureDispatcher` / `FeatureInstallRegistry`。

把其中任何一条迁入 catalog，在当前 dispatcher 形态下都会在该宿主进程执行：

```text
FeatureDispatcher.<clinit>
  → FeatureInstallRegistry.registerAll(FeatureCatalog.specs())
  → 构建并注册全部 FeatureSpec（含全部 CONTRACT_REQUIRED contract）
```

证据：`FeatureDispatcher.kt:23-25`。这与 AGENTS.md「无关进程不初始化无关 Feature」冲突。现有 HYBRID installer（SystemUI / Launcher / system_server / android 包）已经支付这笔成本；IME / Phone / PowerKeeper / Wallpaper / Media **尚未支付**。

因此：

- 不允许用「catalog 更统一」作为迁移理由。
- 本轮 **B1_PRODUCTION_CANDIDATES = 空集**。
- Gallery 路径有一条独立的 fail-open 缺口，正确修正是局部 `findClassIfExists` / `RuntimeFatality` 隔离，不是 catalog。
- 在 `FeatureDispatcher` 具备 **按 ProcessScope 惰性注册** 之前，这 5 个进程不应成为 catalog 宿主。该 dispatcher 改造本身也不是 B1。

---

## 2. 共同路由事实

### 2.1 入口

`MainModule.onPackageReady` 顺序（`MainModule.java:89-136`）：

```text
API 33 gate
→ lpparam.isFirstPackage()          // 本进程 package-ready 只进一次
→ ProcessScopes.resolve(pkg, processName)
→ ProcessScopes.isRejected(...)
→ PreferenceLoadRegistry.shouldLoad(remote, pkg)
→ PreferenceBootstrap.start()
→ ProcessScope if-chain → 本轮 5 个 installer
```

`onPackageReady` **没有** try/catch。installer / hook 安装期抛出的非 fatal 异常会离开模块入口。libxposed 是否隔离该回调：`INSUFFICIENT_EVIDENCE`。静态上应按「可杀死宿主进程」处理。

### 2.2 ProcessScope

| scope | 包 | `isMainProcess` 门 | 次要进程 |
|---|---|---|---|
| `INPUT_METHOD` | Gboard / 百度 / 讯飞 / 搜狗 / SwiftKey / WeType 前缀或精确名 | 否 | 未拒绝 |
| `POWER_KEEPER` | `com.miui.powerkeeper` | 否 | 未拒绝 |
| `WALLPAPER` | `com.miui.miwallpaper` | 否 | 未拒绝 |
| `MEDIA` | `com.miui.screenshot`, `com.miui.gallery` | 否 | 未拒绝 |
| `PHONE` | `com.android.incallui` | 否 | 未拒绝 |

对照：Settings / SecurityCenter / SystemUI 次要进程有显式拒绝。F1 baseline P1-3 写「ProcessScopes 已拒绝所有多进程包的次要进程」——对这 5 个 scope **过宽**。这是架构债，不是本轮已证实的生产缺陷：没有这些包的 A13 manifest / 次要进程名证据。

`PreferenceLoadRegistry.shouldLoad`：known package（含上述 IME 集合）一律加载 prefs。关闭功能仍加载快照，但不进入各 installer 的 hook 调用。这是 known-package 既有策略，不是这 5 个 installer 独有的 catalog 收益。

### 2.3 install-once

| 层 | 这 5 个 installer |
|---|---|
| `isFirstPackage()` | 是；本进程 `onPackageReady` 只走一次 |
| 局部 `isHooked` / AtomicBoolean | 无（navbar 修复的 `isHooked` 是 **回调内嵌套 hook** 守卫，不是 installer 安装期守卫） |
| `FeatureInstallRegistry` | 不经过 |
| preference 变更重装 | 否（与全局不变式一致） |

对本轮路径：`isFirstPackage()` 已经足够提供 package-ready 安装一次。这些 installer **没有** `Application.attach` / `onCreate` 二次入口（A3 范围外）。

### 2.4 ClassLoader

全部安装期查找使用 `PackageReadyParam.classLoader`（应用 ClassLoader，framework 类经 parent 解析）。没有把 `packageName` 当成 process identity 的误用：`MainModule` 用 `ProcessScopes.resolve(pkg, processName)`，installer 再用 `lpparam.classLoader`。

例外：`FixInputMethodBottomMarginHook` 在 `addMiuiBottomView` **之后** 从 `InputMethodServiceInjector.sClassLoader` 取第二套 loader 去 hook `com.miui.inputmethod.InputMethodUtil`。PACKAGE_READY 时该 loader 可能尚未就绪。install-time `HookTargetResolver` **不能**诚实表达这条 nested target。

### 2.5 Catalog 现状（与本轮相关）

- `FeatureId` / `FeatureCatalog` **没有** 这 12 条 feature。
- `PreferenceSchema` **没有** 这些 preference key（schema 目前只覆盖已 catalog 的 owner）。
- `ProcessTarget` 只有 `SystemServer` / `SystemUI` / `Launcher` / `Package(name)` / `Any`。没有 IME 集合匹配器。
- `FeatureSpec.condition` 只接收 `PrefMap`，**看不到 packageName**。Gboard padding 的 `pkg.startsWith("com.google.android.inputmethod")` 无法用现有 condition 表达。
- 现有 hybrid 的 `FeatureDispatcher.createRuntime(...)` 传入的是**包名**不是真实 `processName`（F1 P1-3）。按当前用法把这 5 个迁 catalog，`ProcessTarget.Package` **不会**挡住 `package:remote` 次要进程。

### 2.6 普通失败 vs fatal

`ModuleHelper.findAndHookMethod` / `hookAllMethods` 与 `HookerClassHelper` 回调包装均 `throwIfFatal`（OOM / ThreadDeath / VirtualMachineError），普通失败 log + 跳过。

本轮直接路径上的例外见第 5 节。不要把全项目 fatal 扫描算进 B1。

---

## 3. 判定规则（本轮实际使用）

允许 `CATALOG_MIGRATION_VALUE` 的 concrete benefit，必须是下列之一且不被更大的反收益抵消：

1. install-once 不完整
2. process / ClassLoader ownership 错误
3. compatibility probe 与实际 hook target 不一致
4. 诊断无法表达重要失败
5. duplicate registration 风险
6. lifecycle ownership 无法证明
7. 能删除平行重复 routing
8. 能阻止 disabled feature 的明显不必要 runtime work

本轮观察到的 **catalog 反收益**（对这 5 个进程）：

- `FeatureDispatcher` 初始化注册 **全部** catalog spec
- 不能删除 Launcher 上同一 wallpaper pref 的平行路径（Launcher 不在 B1 范围）
- 现有 `createRuntime(pkg)` 不会修正次要进程键
- Gboard / IME 子集无法用现有 `condition` + `ProcessTarget` 干净表达

---

## 4. FEATURE_MATRIX

RECOMMENDATION 仅四值：`KEEP_LEGACY_SAFE` / `CATALOG_MIGRATION_VALUE` / `CORRECTIVE_BEFORE_MIGRATION` / `INSUFFICIENT_EVIDENCE`。

### 4.1 InputMethodInstaller

#### controls_volumecursor

| 字段 | 值 |
|---|---|
| INSTALLER | `InputMethodInstaller` |
| FEATURE | proposed `volumeCursor` |
| PREF_KEYS | `controls_volumecursor`（安装门）；热路径另读 `controls_volumecursor_apps`, `controls_volumecursor_reverse` |
| PACKAGE | `ProcessScopes` IME 允许列表（精确名 + 3 个前缀） |
| PROCESS_SCOPE | `INPUT_METHOD` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `PackageReadyParam.classLoader`；target 为 framework `android.inputmethodservice.InputMethodService`（经 app loader 解析 boot 类） |
| HOOK_TARGETS | `InputMethodService.onKeyDown(int, KeyEvent)`, `onKeyUp(int, KeyEvent)` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()`；无局部守卫；无 resolver |
| LIFECYCLE_OWNER | 无 receiver / observer / Handler / coroutine / controller；无静态可变业务状态 |
| COMPATIBILITY_MODE | legacy `ModuleHelper.findAndHookMethod` fail-open |
| FATAL_BOUNDARY | 安装与回调走 `ModuleHelper` / `HookerClassHelper.throwIfFatal`。`VolumeCursorHook` 自身无 `catch (Throwable)` |
| HOT_PATH_COST | 键事件路径：`Settings.Global.getString`（Binder）+ `PrefMap.getStringSet`。**不是** target discovery |
| CURRENT_RISK | 低。热路径 Binder 是既有产品行为，catalog 不消除 |
| MIGRATION_BENEFIT | 无。install-once / ownership / fail-open 已成立。迁 catalog 会在每个 IME 进程加载全量 FeatureCatalog |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

不值得新增 resolver：目标是稳定 framework ABI。

#### controls_nonavbar_fix_inputmethod

| 字段 | 值 |
|---|---|
| INSTALLER | `InputMethodInstaller` |
| FEATURE | proposed `fixInputMethodBottomMargin` |
| PREF_KEYS | `controls_nonavbar_fix_inputmethod` **且** `controls_nonavbar` |
| PACKAGE | 全部 `INPUT_METHOD` 包 |
| PROCESS_SCOPE | `INPUT_METHOD` |
| INSTALL_PHASE | `PACKAGE_READY` 安装外层 hook；嵌套 hook 延迟到第一次 `addMiuiBottomView` |
| CLASSLOADER_OWNER | 外层：`lpparam.classLoader` → `android.inputmethodservice.InputMethodServiceInjector`。内层：`InputMethodServiceInjector.sClassLoader` → `com.miui.inputmethod.InputMethodUtil` |
| HOOK_TARGETS | `InputMethodServiceInjector.addMiuiBottomView`；然后 `InputMethodUtil.updateGestureLineEnable(Context)` + 静态字段 `sIsGestureLineEnable` |
| CURRENT_INSTALL_ONCE | 外层：`isFirstPackage()`。内层：MethodHook 实例字段 `isHooked`（防 `addMiuiBottomView` 重入） |
| LIFECYCLE_OWNER | 无长生命周期 listener。静态字段写入属于 ROM 类，模块不持有 View |
| COMPATIBILITY_MODE | `findClassIfExists`；若 class 为 null，`ModuleHelper.hookAllMethods(null, ...)` NPE 被 catch + `throwIfFatal`，fail-open |
| FATAL_BOUNDARY | 安装 fail-open；回调走 HookerClassHelper |
| HOT_PATH_COST | 安装后仅常量字段写入 + 一个 before skip。无 ROM scan |
| CURRENT_RISK | 中低。nested ClassLoader 使 CONTRACT_REQUIRED 在 PACKAGE_READY **不可诚实探测** `InputMethodUtil` |
| MIGRATION_BENEFIT | 无。强行 contract 会造成 resolver selected target ≠ 实际 nested target |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### various_gboardpadding_*

| 字段 | 值 |
|---|---|
| INSTALLER | `InputMethodInstaller` |
| FEATURE | proposed `gboardPadding` |
| PREF_KEYS | `various_gboardpadding_port`, `various_gboardpadding_land`（任一 `> 0`） |
| PACKAGE | **仅** `pkg.startsWith("com.google.android.inputmethod")`（在 INPUT_METHOD 之内再过滤） |
| PROCESS_SCOPE | `INPUT_METHOD` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` → framework `android.os.SystemProperties` |
| HOOK_TARGETS | `SystemProperties.get(String)`（单参数重载） |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 无 |
| COMPATIBILITY_MODE | `XposedHelpers.findClass`（非 IfExists）+ `ModuleHelper.findAndHookMethod`。`SystemProperties` 缺失会抛出 `ClassNotFoundError` 并离开 `onPackageReady`。A13 上该 framework 类缺失的实际概率极低 |
| FATAL_BOUNDARY | findClass 未包 `throwIfFatal`；普通 ClassNotFound 可杀进程。framework 类场景下视为可忽略的理论路径 |
| HOT_PATH_COST | Gboard 进程内每一次 `SystemProperties.get(String)` 都做字符串比较。无 discovery |
| CURRENT_RISK | 产品有效性：`INSUFFICIENT_EVIDENCE` Gboard 是否走单参数 `get` 而非 `get(String, String)` / native。架构本身简单 |
| MIGRATION_BENEFIT | 负。`FeatureSpec.condition` 看不到 package；`ProcessTarget.Any` 会在搜狗等 IME 上把该 feature 标成 INSTALLED |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

---

### 4.2 MediaInstaller

`ProcessScope.MEDIA` 覆盖两个包；installer 用 `pkg` 再分发。这是合法的 scope 内二次分发，不是 ownership 错误。

#### system_screenshot

| 字段 | 值 |
|---|---|
| INSTALLER | `MediaInstaller` |
| FEATURE | proposed `screenshotConfig` |
| PREF_KEYS | 安装门 `system_screenshot`；hook 内 `system_screenshot_format`, `system_screenshot_quality`, `system_screenshot_path`, `system_screenshot_mypath` |
| PACKAGE | `com.miui.screenshot` |
| PROCESS_SCOPE | `MEDIA` |
| INSTALL_PHASE | `PACKAGE_READY`；`format > 2` 时嵌套 hook 延迟到 `MiuiScreenshotApplication.attachBaseContext` |
| CLASSLOADER_OWNER | `lpparam.classLoader`。framework：`ContentResolver`, `Bitmap`。app：`MiuiScreenshotApplication`。混淆名 `com.miui.screenshot.u0.f$a` / `x0.e$a` 按 `versionCode` 选择 |
| HOOK_TARGETS | `ContentResolver.update` / `insert`；`Bitmap.compress`；可选 `attachBaseContext` + 混淆 `a` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()`。嵌套 format hook 无二次 `isHooked`（`attachBaseContext` 通常一次） |
| LIFECYCLE_OWNER | 无 receiver/observer。`mkdirs()` 仅在 insert 回调、且 folder>1 |
| COMPATIBILITY_MODE | 主路径 ModuleHelper fail-open。混淆分支 versionCode 门 + fail-open。无 resolver，因此不存在 resolver/installer mismatch |
| FATAL_BOUNDARY | 安装走 ModuleHelper |
| HOT_PATH_COST | screenshot 进程内所有 `ContentResolver.insert/update` 与 `Bitmap.compress`。prefs 只读。无 ROM scan |
| CURRENT_RISK | 混淆类 ABI 在 HyperOS 1 / 不同 screenshot versionCode 上是否覆盖：`INSUFFICIENT_EVIDENCE`。主路径是稳定 framework 类 |
| MIGRATION_BENEFIT | 合同/variant 对混淆类可能有诊断价值，但缺少 A13 ROM 样本，不能在 B1 证明。全量 catalog 初始化成本确定 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

不值得在无 ROM 证据时新增 resolver。

#### system_screenshot_floattime

| 字段 | 值 |
|---|---|
| INSTALLER | `MediaInstaller` |
| FEATURE | proposed `screenshotFloatTime` |
| PREF_KEYS | `system_screenshot_floattime`（`> 0`） |
| PACKAGE | `com.miui.screenshot` |
| PROCESS_SCOPE | `MEDIA` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` → `com.miui.screenshot.GlobalScreenshot` |
| HOOK_TARGETS | `GlobalScreenshot.startGotoThumbnailAnimation(Runnable)`；回调内使用 ROM 已有 `mHandler` / `mQuitThumbnailRunnable`（不创建模块 Handler） |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 模块不拥有 Handler；只 `removeCallbacks` + `postDelayed` ROM runnable |
| COMPATIBILITY_MODE | ModuleHelper fail-open |
| FATAL_BOUNDARY | **缺口**：回调内 `catch (t: Throwable) { if (t is OutOfMemoryError) throw t; false }` 吞掉 `ThreadDeath` / `VirtualMachineError`。`getBooleanField` 缺字段抛 `NoSuchFieldError`（非 fatal），该 fail-open 意图成立，但 fatal 集合不完整 |
| HOT_PATH_COST | 动画启动路径，非逐帧 discovery |
| CURRENT_RISK | fatal 缺口真实但局部。catalog **不会**改写这段 inner catch |
| MIGRATION_BENEFIT | 无（就 B1 catalog 选型而言） |
| RECOMMENDATION | **CORRECTIVE_BEFORE_MIGRATION** |

B1 最终分类：`CORRECTIVE_BEFORE_MIGRATION`。D2 局部 corrective（`RuntimeFatality.throwIfFatal`）完成后预计仍为 `KEEP_LEGACY_SAFE`，**不是** catalog migration candidate。

#### system_gallery_screenshots_path

| 字段 | 值 |
|---|---|
| INSTALLER | `MediaInstaller` |
| FEATURE | proposed `galleryScreenshotPath` |
| PREF_KEYS | `system_gallery_screenshots_path`（`> 1`） |
| PACKAGE | `com.miui.gallery` |
| PROCESS_SCOPE | `MEDIA` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` → app 类 `com.miui.gallery.storage.constants.MIUIStorageConstants` |
| HOOK_TARGETS | **无 Method hook**。安装期 `setStaticObjectField(..., "DIRECTORY_SCREENSHOT_PATH", ssPath)` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()`；静态字段写入一次 |
| LIFECYCLE_OWNER | 无 |
| COMPATIBILITY_MODE | `XposedHelpers.findClass`（非 IfExists）+ `setStaticObjectField`。缺类 → `ClassNotFoundError`；缺字段 → `NoSuchFieldError`。**都不经 ModuleHelper**，直接离开 `onPackageReady` |
| FATAL_BOUNDARY | 安装期普通兼容失败 **fail-closed（可杀 Gallery 进程）**。违反 Phase B invariant 8 |
| HOT_PATH_COST | 安装后零回调 |
| CURRENT_RISK | **高（正确性）**：HyperOS 1 Gallery 若改名/删字段，开启该功能即可在包加载时崩溃。是否真改名：`UNVERIFIED`。代码路径本身是确定的 |
| MIGRATION_BENEFIT | catalog 的 installer try/catch 能隔离该异常，但会在 Gallery 进程加载全量 FeatureCatalog。更小的正确性修复是局部 fail-open |
| RECOMMENDATION | **CORRECTIVE_BEFORE_MIGRATION** |

Corrective：`findClassIfExists` + 缺字段 / ordinary reflection `RuntimeFatality.throwIfFatal` 后 fail-open。完成后预计仍为 `KEEP_LEGACY_SAFE`，**不是** catalog migration candidate。

---

### 4.3 PhoneInstaller

三个 feature 共用 `com.android.incallui` / `ProcessScope.PHONE` / `lpparam.classLoader` / `isFirstPackage()` / ModuleHelper fail-open / 无长生命周期对象。

#### various_showcallui

| 字段 | 值 |
|---|---|
| FEATURE | proposed `showCallUi` |
| PREF_KEYS | `various_showcallui`（`> 0`）；回调再读同一 key 的 1/3 模式 |
| HOOK_TARGETS | `com.android.incallui.InCallPresenter.startUi`（hookAllMethods） |
| LIFECYCLE_OWNER | 无。`Settings.Global` 只读 |
| HOT_PATH_COST | 来电 UI 启动路径 |
| CURRENT_RISK | 低 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### various_calluibright

| 字段 | 值 |
|---|---|
| FEATURE | proposed `inCallBrightness` |
| PREF_KEYS | 安装门 `various_calluibright`；回调 `various_calluibright_type`, `_val`, `_night`, `_night_start_hour/minute`, `_night_end_hour/minute` |
| HOOK_TARGETS | `com.android.incallui.InCallActivity.onCreate(Bundle)`；回调内 `InCallPresenter.getInstance()` |
| LIFECYCLE_OWNER | 不持有 Activity；只改 `window.attributes` |
| HOT_PATH_COST | Activity onCreate；`SimpleDateFormat` 每次分配（冷/事件路径，非逐帧） |
| CURRENT_RISK | 低 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### various_answerinheadup

| 字段 | 值 |
|---|---|
| FEATURE | proposed `answerCallInHeadUp` |
| PREF_KEYS | `various_answerinheadup` |
| HOOK_TARGETS | `InCallPresenter.answerIncomingCall(Context, String, int, boolean)` |
| LIFECYCLE_OWNER | 无。回调调用 `miui.process.ProcessManager.getForegroundInfo()` |
| HOT_PATH_COST | 接听路径一次 Binder |
| CURRENT_RISK | `ProcessManager` 在 HyperOS 1 A13 是否仍存在：`INSUFFICIENT_EVIDENCE`。缺方法时 ModuleHelper 安装 fail-open；回调抛错由 HookerClassHelper fail-open |
| MIGRATION_BENEFIT | 无足够 ROM 证据证明需要 contract |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

---

### 4.4 PowerKeeperInstaller

`ProcessScope.POWER_KEEPER` **不**区分主进程。次要进程若存在，会重复尝试 hook；ModuleHelper 对缺类 fail-open。是否存在次要进程：`INSUFFICIENT_EVIDENCE`。

#### various_restrictapp

| 字段 | 值 |
|---|---|
| INSTALLER | `PowerKeeperInstaller` |
| FEATURE | proposed `appsRestrictPower` |
| PREF_KEYS | `various_restrictapp` |
| PACKAGE | `com.miui.powerkeeper` |
| PROCESS_SCOPE | `POWER_KEEPER` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | `PowerKeeperConfigureManager.pkgHasIcon`；`PreSetGroup.initGroup`（after 清空 ROM 静态 `mGroupHeadUidMap`）；`PreSetApp.isPreSetApp`；`Utils.pkgHasIcon` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 不拥有该 Map；一次性 mutate ROM 静态表 |
| COMPATIBILITY_MODE | ModuleHelper fail-open |
| FATAL_BOUNDARY | 安装/回调 throwIfFatal |
| HOT_PATH_COST | `pkgHasIcon` 可能被 UI 多次调用；回调是 `returnConstant(true)`，无 discovery |
| CURRENT_RISK | 低 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### various_persist_batteryoptimization

| 字段 | 值 |
|---|---|
| FEATURE | proposed `persistBatteryOptimization` |
| PREF_KEYS | `various_persist_batteryoptimization` |
| HOOK_TARGETS | `CommonAdapter.addPowerSaveWhitelistApps` DO_NOTHING；`MilletPolicy.dealSleepModeWhiteList`；`ForceDozeController.restoreWhiteListAppsIfQuitForceIdle` DO_NOTHING |
| LIFECYCLE_OWNER | 无 |
| CURRENT_RISK | 低。类名随 ROM 变化时 fail-open |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

不值得新增 resolver：没有已证明的多 variant ABI。

---

### 4.5 WallpaperInstaller

#### launcher_disable_wallpaperscale

| 字段 | 值 |
|---|---|
| INSTALLER | `WallpaperInstaller`（本轮） |
| FEATURE | proposed `disableUnlockWallpaperScale` |
| PREF_KEYS | `launcher_disable_wallpaperscale` |
| PACKAGE | `com.miui.miwallpaper` |
| PROCESS_SCOPE | `WALLPAPER` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` → `com.miui.miwallpaper.manager.WallpaperServiceController` |
| HOOK_TARGETS | `noNeedDesktopWallpaperScaleAnim` → `returnConstant(true)` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 无 |
| COMPATIBILITY_MODE | ModuleHelper fail-open |
| FATAL_BOUNDARY | throwIfFatal |
| HOT_PATH_COST | 常量返回 |
| CURRENT_RISK | 低。**同一 pref 另有 Launcher 路径**：`LauncherInstaller.handleLoadLauncher` → `DisableLauncherWallpaperScale`（B1 范围外）。迁 WallpaperInstaller **不能**删除该平行 routing |
| MIGRATION_BENEFIT | 无。catalog 不能在本轮收敛双进程产品语义 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

`FeatureCatalog` 已有 `wallpaperScaleLevel`（`system_other_wallpaper_scale` / system_server）。那是**另一条**功能，不是本 pref。禁止合并身份。

---

## 5. CONFIRMED_DEFECTS

仅限本轮 5 个 installer 的直接调用路径。

### B1-D1  Gallery 静态字段写入 fail-closed

- 路径：`MediaInstaller` → `GalleryScreenshotPathHook`
- 证据：`SystemAudioAndVisualAndMoreHooks.kt:649-659` 使用 `XposedHelpers.findClass` + `setStaticObjectField`；`XposedHelpers.java:315-323` 缺类抛 `ClassNotFoundError`；`MainModule.onPackageReady` 无 catch
- 违反：ordinary ROM/API failure should fail-open
- 证据等级：STATIC_VERIFIED（路径）；UNVERIFIED（HyperOS 1 Gallery 是否缺该类）
- 正确修正：局部 fail-open + `RuntimeFatality`。**不是** catalog，也不是本轮生产

### B1-D2  ScreenshotFloatTime 回调 catch 不完整

- 路径：`MediaInstaller` → `ScreenshotFloatTimeHook` after
- 证据：`SystemAudioAndVisualAndMoreHooks.kt:666-670` 只重抛 `OutOfMemoryError`
- 违反：fatal JVM state must propagate
- 正确修正：`RuntimeFatality.throwIfFatal(t)`。catalog 不修复 inner catch
- 不构成 B1 catalog 候选

非缺陷（记录以免误判）：

- `FixInputMethodBottomMarginHook` 对 null class 的 NPE：已被 `ModuleHelper.hookAllMethods` catch。
- `VolumeCursorHook` / Phone / PowerKeeper / Wallpaper / Screenshot 主路径：ModuleHelper fail-open。
- `Controls.handleNavBarAction` 的不完整 catch：**不在** VolumeCursor 调用路径。

---

## 6. ARCHITECTURE_DEBT

1. 这 5 个 scope 无 `isMainProcess` 拒绝；F1 P1-3 的外部不变量对它们不成立。无次要进程名证据，不升级为缺陷。
2. `FeatureDispatcher.<clinit>` 全量 `registerAll(FeatureCatalog.specs())`。在 IME/Phone/PowerKeeper/Wallpaper/Media 启用 catalog 会违反「无关进程不初始化无关 Feature」。**按 scope 惰性注册是任何后续 catalog 收敛的前置**，不是 B1。
3. `FeatureSpec.condition` 无 package；`ProcessTarget` 无 IME 集合。Gboard 过滤无法干净建模。
4. `launcher_disable_wallpaperscale` 双进程双 hook（Wallpaper + Launcher）。产品语义可能正确；B1 不能也不应只迁一半。
5. `PreferenceSchema` 尚未拥有这 12 条 key。这是 catalog 未收录的结果，不是独立缺陷。
6. Gallery / Gboard 安装期 `findClass` 不走 ModuleHelper 边界，与同文件其它 hook 不一致。

---

## 7. INSUFFICIENT_EVIDENCE

| 项 | 缺什么 | 影响 |
|---|---|---|
| PowerKeeper / Wallpaper / Media / Phone / IME 次要进程是否存在 | A13 目标 ROM manifest / process 列表 | 不能声称 duplicate install 已发生 |
| HyperOS 1 Gallery `MIUIStorageConstants` | ROM/DEX | B1-D1 是否在兼容目标上触发未知 |
| Screenshot 混淆类 versionCode 表 | 多 ROM screenshot APK | 不能证明需要 contract/variant |
| Gboard 是否调用 `SystemProperties.get(String)` | Gboard DEX | 不能证明 padding hook 有效或无效 |
| `ProcessManager.getForegroundInfo` on HyperOS 1 | 设备/日志 | answer-in-heads-up 运行时有效性 |
| libxposed 是否隔离 `onPackageReady` 异常 | 框架行为 | Gallery fail-closed 的最终进程命运 |

A14 @ `d20d96b5` 仅作工程标准参考。未把 A14 installer 形状当作 A13 迁移命令。Dynamic Island 不在范围。

---

## 8. B1_PRODUCTION_CANDIDATES

```text
B1_PRODUCTION_CANDIDATES = []
```

逐 installer：

| Installer | 结果 |
|---|---|
| InputMethodInstaller | 3/3 `KEEP_LEGACY_SAFE` |
| MediaInstaller | screenshot `KEEP_LEGACY_SAFE`；floattime / gallery `CORRECTIVE_BEFORE_MIGRATION`（局部 fail-open / fatal boundary，非 catalog）。D1/D2 corrective 后预计仍为 `KEEP_LEGACY_SAFE`，不是 catalog candidate |
| PhoneInstaller | 3/3 `KEEP_LEGACY_SAFE` |
| PowerKeeperInstaller | 2/2 `KEEP_LEGACY_SAFE` |
| WallpaperInstaller | 1/1 `KEEP_LEGACY_SAFE` |

**不选 migration 的原因不是数量不够，而是没有一条同时满足：concrete catalog benefit，且不把全量 FeatureCatalog 拉进当前未加载 catalog 的进程。**

---

## 9. PROPOSED_MINIMAL_DESIGN

无 B1 生产候选项。无 FeatureId / FeatureSpec / dispatcher 设计。

若未来（B2+，需单独授权）仍要收敛这些进程，**前置条件**不是本轮实现：

1. `FeatureDispatcher` / `FeatureInstallRegistry.registerAll` 改为按 `ProcessScope` 惰性注册，IME 进程不得构建 SystemUI/system_server spec。
2. Gallery B1-D1 局部 fail-open。
3. 若要挡住次要进程：`createRuntime` 使用真实 `processName`，且 `ProcessTarget` 与 `ProcessScopes` 对这 5 个 scope 一致。
4. Gboard 需要 package-aware condition 或专用 `ProcessTarget`，禁止用 `ProcessTarget.Any` 污染其它 IME 的 registry 状态。

---

## 10. FUTURE_MAINTENANCE_NOTE（不扩大本轮）

- IME 允许列表是 6 个精确名 + 3 个前缀；F1 表格写「9 个精确 + 3 个前缀」不精确。
- VolumeCursor 热路径 `Settings.Global.getString` 可在未来性能任务评估，不是 B1。
- ScreenshotFloatTime / Gallery 的局部 fatal/fail-open 已作为独立 Media corrective 处理；完成后仍 KEEP_LEGACY_SAFE，不是 catalog candidate。不打开 A1/A2/A3。
- `wallpaperScaleLevel`（已 catalog）与 `launcher_disable_wallpaperscale`（本轮）禁止混身份。

---

## 11. 证据等级与冻结

```text
B1_STATIC_RESULT = NO_PRODUCTION_CANDIDATE
PHASE_A_REOPENED = NO
A1/A2/A3 未发现同生产路径新缺陷
PRODUCTION_CHANGED = NO
```

未修改任何 production Java/Kotlin。未开始 B2。未 merge / rebase / force-push / tag / release。
