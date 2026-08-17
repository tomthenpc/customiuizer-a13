# A13 B2A — Specialized Installer Architecture Selection

## 元信息

| 项目 | 值 |
|---|---|
| AUTHORITATIVE_BASE_SHA | `ba5c2c1f796bec3fb714fe16d83687d14c7dbd02` |
| BRANCH | `devin/a13-foundation-parity-r13.11.1` |
| PHASE_A_FREEZE_SHA | `03a7a082048c028c185eaf351ea167af6bdb4697` |
| B1_FREEZE_SHA | `ba5c2c1f796bec3fb714fe16d83687d14c7dbd02` |
| PINNED_A14_REFERENCE | `tomthenpc/customiuizer-a14` @ `d20d96b543a49a584970e312da7d704958a155aa` |
| PRODUCTION_AUTHORIZATION | NO |
| PRODUCTION_CHANGED | NO |
| 性质 | 只读静态选型 / 文档证据 |
| 范围 | 4 个 specialized installer 及其实际 hook、`ProcessScopes` / `MainModule` 路由、以及证明这 4 个 installer 所必需的 `FeatureDispatcher` / `FeatureInstallRegistry` / `FeatureCatalog` 条目 |

```text
STATIC_VERIFIED = YES
BUILD_VERIFIED  = NO   (docs-only; Android compile not required)
LOG_VERIFIED    = NO
DEVICE_VERIFIED = NO
B2A_PRODUCTION_CANDIDATES = none
B2A_PRODUCTION_AUTHORIZATION = NO
B2B_STARTED = NO
PHASE_A_REOPENED = NO
B1_REOPENED = NO
```

本文件是 Phase B2A 的选型证据，不是 PASS authority。ChatGPT 是最终 Gatekeeper。

**本轮未审计（后续 batch）：** `SecurityCenterInstaller`、`LauncherInstaller`、`SystemUiInstaller`、`SystemServerInstaller`。仅在需要证明双进程产品路径或 Settings 死分支时引用它们的调用点，不把那些 installer 纳入选型。

A1 / A2 / A3 / B1 保持 CLOSED。A3 的 `Application.attach` package-identity filter 仍在 GenericApp 四条 attach 安装之前；本轮没有独立新缺陷要求重开 A3。

---

## 1. 结论先行

这 4 个 installer 的结论不是「尚未 catalog，所以应该迁」。

| Installer | 当前形态 | 选型结果 |
|---|---|---|
| `GenericAppInstaller` | LEGACY_DIRECT；可进入任意 `GENERIC_APP` 进程 | 5 条路径均 **不迁 catalog**。其中 clipboard / nooverscroll 为局部 fail-open / fatal 缺口 |
| `PackageInstallerRouter` | LEGACY_DIRECT；仅 `com.miui.packageinstaller` | 2/2 `KEEP_LEGACY_SAFE` |
| `SettingsInstaller` | LEGACY_DIRECT；仅 `SETTINGS_MAIN` | 6/6 `KEEP_LEGACY_SAFE`。次要进程排除已证明 |
| `AndroidPackageInstaller` | **已经 HYBRID** | 4 条 resource/legacy `KEEP_LEGACY_SAFE`；2 条 catalog 路径保持现状。`createRuntime(pkg)` 在本路径上是 `SEMANTICALLY_EQUIVALENT`，全局命名是 `ARCHITECTURE_DEBT`，不是本路径 `CONFIRMED_DEFECT` |

把 GenericApp / PackageInstaller / Settings 迁入 catalog，在当前 dispatcher 形态下都会执行：

```text
FeatureDispatcher.<clinit>
  → FeatureInstallRegistry.registerAll(FeatureCatalog.specs())
  → 构建并注册全部 FeatureSpec（含全部 CONTRACT_REQUIRED contract）
```

证据：`FeatureDispatcher.kt:23-25`。这与 AGENTS.md「无关进程不初始化无关 Feature」冲突。

GenericApp 的反收益最大：任意被选中的用户 App（以及 known package `com.lbe.security.miui`）都会变成全量 catalog 宿主。

因此：

- 不允许用「catalog 更统一」作为迁移理由。
- 本轮 **B2A_PRODUCTION_CANDIDATES = 空集**。
- clipboard / nooverscroll 的正确修正是局部 `findClassIfExists` / `RuntimeFatality`，不是 catalog。
- 在 `FeatureDispatcher` 具备 **按 ProcessScope 惰性注册** 之前，这三个尚未支付 catalog 成本的进程不应成为 catalog 宿主。该 dispatcher 改造本身也不是 B2A。
- Android 包上已经 catalog 的 `cleanShareMenu` / `cleanOpenWithMenu` 保持 HYBRID；不要为了形状把 4 条 resource 路径再拉进 dispatcher。

---

## 2. 共同路由事实

### 2.1 入口

`MainModule.onPackageReady` 顺序（`MainModule.java:89-150`）：

```text
API 33 gate
→ lpparam.isFirstPackage()
→ ProcessScopes.resolve(pkg, processName)
→ ProcessScopes.isRejected(pkg, processName)
→ PreferenceLoadRegistry.shouldLoad(remote, pkg)
→ PreferenceBootstrap.start()
→ ProcessScope if-chain
     SETTINGS_MAIN        → SettingsInstaller
     PACKAGE_INSTALLER    → PackageInstallerRouter
     GENERIC_APP          → GenericAppInstaller
     ANDROID_PACKAGE      → AndroidPackageInstaller
```

`onPackageReady` **没有** try/catch。installer / hook 安装期抛出的非 fatal 异常会离开模块入口。libxposed 是否隔离该回调：`INSUFFICIENT_EVIDENCE`。静态上应按「可杀死宿主进程」处理。

`MainModule.processName` 来自模块字段，不是从 `packageName` 推断。`ProcessScopes.resolve(pkg, processName)` 同时使用两者。

### 2.2 ProcessScope（本轮四条）

| scope | 判定 | `isMainProcess` 门 | `isInstallable` | 次要进程 |
|---|---|---|---|---|
| `SETTINGS_MAIN` | `pkg == com.android.settings` 且 main | 是：`packageName == processName \|\| processName.isEmpty()` | true | 否；见下 |
| `SETTINGS_REMOTE` | Settings 且非 main | 是 | **false** | `isRejected` → `onPackageReady` 在 installer 之前 return |
| `PACKAGE_INSTALLER` | `pkg == com.miui.packageinstaller` | **否** | true | 未拒绝 |
| `GENERIC_APP` | `resolve()` 的 `else` | **否** | true | 未拒绝 |
| `ANDROID_PACKAGE` | `pkg == android` 且 main | 是 | true | 非 main 的 `android` 被分类为 `SYSTEM_SERVER` |
| `SYSTEM_SERVER`（仅分类） | `pkg == android` 且非 main | — | true | `onPackageReady` **没有** `SYSTEM_SERVER` 分支。真实 system_server 走 `onSystemServerStarting` |

`SETTINGS_REMOTE.isInstallable = false` 是 **证明过的** 次要进程排除，不是假设。证据：`ProcessScope.kt:32-38` + `ProcessScopes.resolve` Settings 分支 + `MainModule.java:95-97`。

`com.lbe.security.miui` 在 `KNOWN_PACKAGES`（`ProcessScope.kt:59`），但 `resolve()` **没有** dedicated `when` 分支。它落入 `else → GENERIC_APP`。因此 clipboard 特殊分支是生产可达的（前提：LSPosed 作用域包含该包，且 `isFirstPackage`）。

`PreferenceLoadRegistry.shouldLoad`：known package 一律加载 prefs（含 LBE / Settings / packageinstaller / android）。未知 generic 包只在 4 条 legacy 规则命中时加载：`various_alarmcompat`、`system_statusbarcolor`、`system_nooverscroll`、`controls_volumemedia`。clipboard **不在** 这 4 条规则里，但 LBE 是 known package，所以 clipboard 不依赖这些规则。

### 2.3 install-once

| 层 | GenericApp / PackageInstaller / Settings | AndroidPackage catalog 路径 |
|---|---|---|
| `isFirstPackage()` | 是 | 是 |
| 局部 `isHooked` / AtomicBoolean | GenericApp 的 attach 外层无；inner hooks 无 installer 级守卫 | 无局部守卫 |
| `FeatureInstallRegistry` | 不经过 | `(runtime.processName, canonicalId)` + atomic `INSTALLING` |
| preference 变更重装 | 否 | 否；`ConfigReloadMode.NONE`。watcher 只更新 PrefMap 快照 |

GenericApp `Application.attach` 是二次入口。A3 已用 `isTargetPackage` 挡住同进程外包装的 attach。同一 `Application` 实例是否会 attach 两次：A3 按 `makeApplicationInner(allowDuplicateInstances=false)` 处理；本轮不重开。clipboard / AlarmCompat **不走** attach。

### 2.4 ClassLoader 规则（本轮强制区分）

| 符号 | 含义 |
|---|---|
| `packageName` | `lpparam.getPackageName()` |
| `processName` | `MainModule.processName` / libxposed 进程名 |
| `ProcessScope` | `ProcessScopes.resolve(packageName, processName)` |
| `PackageReadyParam.classLoader` | 该 package-ready 的应用 ClassLoader |
| framework boot loader | `Settings.System.class`、`Application.class` 等定义 loader |
| app loader | 用于解析 ROM / 应用类，以及经 parent 解析 framework 类 |

**禁止**用 package identity 推断 process identity。本轮唯一接近该模式的是 hybrid `createRuntime(pkg, ...)`：参数名叫 `processName`，AndroidPackage 传入的是 package `"android"`。见第 8 节分类。

### 2.5 Catalog 初始化成本

`FeatureDispatcher` 一旦被该进程第一次主动使用（`createRuntime` / `installById` / 任何触发 `<clinit>` 的引用），就 `registerAll(FeatureCatalog.specs())`。

AndroidPackageInstaller 把 `FeatureDispatcher.createRuntime` 放在 `system_cleanshare \|\| system_cleanopenwith` 分支内。Java 对该符号的解析发生在该分支首次执行时，不是 `AndroidPackageInstaller` 类加载时。因此：

- 仅 resource 功能开启：走 `isAnyFeatureEnabled()`，**不**调用 `createRuntime`，**不**初始化 dispatcher。
- 全部关闭：`isAnyFeatureEnabled()` 为 false，直接 return。测试：`AndroidPackageInstallerTest`。
- share / openwith 开启：支付全量 catalog 注册成本。该进程已经是 HYBRID 宿主。

GenericApp / Settings / PackageInstaller 当前 **不是** catalog 宿主。

### 2.6 判定规则（本轮实际使用）

允许 `CATALOG_MIGRATION_VALUE` 的 concrete benefit，必须是下列之一且不被更大的反收益抵消：

1. install-once 不完整
2. process / ClassLoader ownership 错误
3. compatibility probe 与实际 hook target 不一致
4. 诊断无法表达重要失败
5. duplicate registration 风险
6. lifecycle ownership 无法证明
7. 能删除平行重复 routing
8. 能阻止 disabled feature 的明显不必要 runtime work

本轮观察到的 **catalog 反收益**：

- GenericApp：全量 catalog 进入任意用户 App / LBE
- Settings / PackageInstaller：全量 catalog 进入这两个尚未支付成本的进程
- AndroidPackage resource 路径：把它们迁 catalog 会让「只改状态栏高度」的用户也支付 dispatcher `<clinit>`
- 若干 pref 已有 **system_server catalog 半边**（`notificationVolume`、`disableAnyNotificationBlock`、`allRotations`、`rotationAnimation`、`cleanShareMenuService`、`cleanOpenWithMenuService`、`USBConfigHook`）。迁 Settings/AndroidPackage UI/resource 半边 **不能**删除 server 路径，也不得合并 FeatureId

---

## 3. SPECIAL CHECKS

### 3.1 重复 preference 分支调用同一 hook

| 观察 | 结论 |
|---|---|
| `system_statusbarcolor` 连续调用 Compat + Background | **两个不同 hook 函数**，一次 attach 各装一次。不是同一 hook 装两次 |
| `system_disableanynotif` 调用 Hook + BlockHook | **两个不同 target**，同一用户功能。Settings 进程内不会把同一方法 hook 两次 |
| `system_separatevolume` 调用 Res + SettingsHook | resource 注册 + Java hook，不是重复安装 |
| `system_allrotations2` / `system_rotateanim` / `system_cleanshare` / `system_cleanopenwith` / `system_separatevolume` / `system_disableanynotif` / `system_defaultusb` | Android 包或 Settings 与 **system_server** 各有半边。不同进程，不是同进程双装 |
| `DisableAnyNotificationBlockHook` 有 PackageReady 与 SystemServer 重载 | Settings 只调用 PackageReady 重载 |

**没有**证实「同一启动、同一进程把同一 hook 安装两次」的 duplicate-install bug。

### 3.2 ProcessScopes 造成的不可达 installer 分支

| 代码 | 分类 |
|---|---|
| `PackageInstallerRouter` 内层 `"com.miui.packageinstaller".equals(pkg)` | **MIGRATION_RESIDUE**：与 `ProcessScope.PACKAGE_INSTALLER` 冗余，但该 installer **仍被调用**，不是死 installer |
| `AndroidPackageInstaller` 内层 `"android".equals(pkg)` | **MIGRATION_RESIDUE**：与 `ANDROID_PACKAGE` 冗余 |
| `DisableAnyNotificationHook` 中 `packageName.contains("systemui")` 在 Settings 调用时 | **DEAD_PATH / MIGRATION_RESIDUE**（仅 Settings 调用上下文）。同一函数仍被 `SystemUiInstaller` 调用（后续 batch，不在本轮删除） |
| `com.lbe.security.miui` clipboard 分支 | **不是死路径**。KNOWN 但无 dedicated resolve → `GENERIC_APP` → 该分支 |

本轮不删除任何死路径。

### 3.3 GenericApp Application.attach 与 A3 filter

`GenericAppInstaller.java:41-51`：

```text
Application.attach after
  → isTargetPackage(this, lpparam)     // A3 门，必须先于下列四条
  → StatusBarBackgroundCompatHook
  → StatusBarBackgroundHook
  → NoOverscrollAppHook
  → VolumeMediaPlayerHook
```

源码顺序与 `ApplicationAttachPackageFilterSourceTest.genericAppInstaller_packageFilter_precedes_hookInstallations` 一致：guard index < 第一条 hook 安装。后三条在同一 `after` 中、同一 `if (!isTargetPackage) return` 之后。

**不在 attach 上的路径：** clipboard、AlarmCompat。A3 门对它们不适用，也不需要重开 A3。

### 3.4 AndroidPackage `FeatureRuntime.processName`

见第 8 节。分类：**SEMANTICALLY_EQUIVALENT**（本路径）+ **ARCHITECTURE_DEBT**（命名 / hybrid 惯例）。**不是** `CONFIRMED_DEFECT`。

---

## 4. FEATURE_MATRIX

RECOMMENDATION 仅四值：`KEEP_LEGACY_SAFE` / `CATALOG_MIGRATION_VALUE` / `CORRECTIVE_BEFORE_MIGRATION` / `INSUFFICIENT_EVIDENCE`。

REACHABILITY 取值：`PRODUCTION_REACHABLE`（静态路由已证明，仍依赖 LSPosed 作用域）/ `ROUTING_EXCLUDED` / `DEAD_PATH`。

---

### 4.1 GenericAppInstaller

#### various_clipboard_defaultaction → SmartClipboardActionHook

| 字段 | 值 |
|---|---|
| INSTALLER | `GenericAppInstaller` |
| FEATURE | proposed `smartClipboardAction`（**不建议现在建**） |
| PREF_KEYS | `various_clipboard_defaultaction`（`> 1` 才装；默认 `1` = 关） |
| REACHABILITY | **PRODUCTION_REACHABLE**。`com.lbe.security.miui` ∈ `KNOWN_PACKAGES`，无 dedicated resolve → `GENERIC_APP` → `GenericAppInstaller.install` → `pkg.equals("com.lbe.security.miui")` |
| PACKAGE | `com.lbe.security.miui` |
| PROCESS | LBE 进程名；**无** `isMainProcess` 拒绝。次要进程是否存在：INSUFFICIENT_EVIDENCE |
| PROCESS_SCOPE | `GENERIC_APP` |
| INSTALL_PHASE | `PACKAGE_READY`（**不是** `Application.attach`） |
| CLASSLOADER_OWNER | `PackageReadyParam.classLoader`（LBE app loader） |
| HOOK_TARGETS | `com.lbe.security.ui.ClipboardTipDialog.customReadClipboardDialog(Context, String)`（ModuleHelper）。`opt != 3` 时另：`XposedHelpers.findClass("com.lbe.security.ui.SecurityPromptHandler")` + `handleNewRequest` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()`；无 registry |
| LIFECYCLE_OWNER | 无 receiver / observer / 静态 Context。`setAdditionalInstanceField("currentStopped")` 绑在 ROM handler 实例上，after 路径 `removeAdditionalInstanceField`。**PROCESS_SINGLETON** 仅指 hook 本身 |
| COMPATIBILITY_MODE | Dialog：ModuleHelper fail-open。Handler：`findClass` **抛** `XposedHelpers.ClassNotFoundError extends Error` |
| FATAL_BOUNDARY | `ClassNotFoundError` 不是 OOM/ThreadDeath/VME。发生在 `onPackageReady` 无 try/catch 路径上 → **安装期 fail-closed**。这是 B2A 路径上的独立缺陷，不是 A3 |
| HOT_PATH_COST | 权限请求回调：字段读写 + `gotChoice`。无 DexKit / 磁盘 |
| CURRENT_RISK | **高（安装期）**：LBE ROM 若重命名 `SecurityPromptHandler`，模块入口抛 Error，可杀死 LBE 进程。opt==3 只 hook dialog，不走这条 |
| MIGRATION_BENEFIT | **无**。catalog 不会修复 throwing `findClass`，还会在 LBE 加载全量 FeatureCatalog |
| RECOMMENDATION | **CORRECTIVE_BEFORE_MIGRATION**（局部 `findClassIfExists` + `RuntimeFatality`；纠正后仍应 `KEEP_LEGACY_SAFE`） |

#### various_alarmcompat → AlarmCompatHook

| 字段 | 值 |
|---|---|
| INSTALLER | `GenericAppInstaller` |
| FEATURE | proposed `alarmCompatApp`（与 system_server `AlarmCompatServiceHook` **不得**合成一个 FeatureId） |
| PREF_KEYS | `various_alarmcompat` + `various_alarmcompat_apps.contains(pkg)` |
| REACHABILITY | PRODUCTION_REACHABLE。未知包靠 `PreferenceLoadRegistry` 规则；known 包一律加载后再做 contains 检查 |
| PACKAGE | `various_alarmcompat_apps` 中的应用包 |
| PROCESS | 该应用主/任意同包进程（无 main-process 门） |
| PROCESS_SCOPE | `GENERIC_APP` |
| INSTALL_PHASE | `PACKAGE_READY`（不走 attach） |
| CLASSLOADER_OWNER | **framework defining loader**：`Settings.System::class.java`，不用 `lpparam.classLoader` |
| HOOK_TARGETS | `android.provider.Settings.System.getStringForUser(ContentResolver, String, int)`；before 把 `next_alarm_formatted` 改写成 `next_alarm_clock_formatted` |
| CURRENT_INSTALL_ONCE | 每选中 **应用进程** 一次（`isFirstPackage`）。N 个选中 App = N 个进程各装一次，这是产品语义 |
| LIFECYCLE_OWNER | 无。App 侧不注册 ContentObserver。Observer 在 **system_server** `AlarmCompatServiceHook`（后续 batch） |
| COMPATIBILITY_MODE | `ModuleHelper.findAndHookMethod` fail-open |
| FATAL_BOUNDARY | 安装走 ModuleHelper；回调走 HookerClassHelper `throwIfFatal`（含 cause chain） |
| HOT_PATH_COST | 每次 `getStringForUser`：字符串相等比较。无反射 |
| CURRENT_RISK | 低。迁 catalog 不能删除 server 半边，且会污染每个选中 App 进程 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_statusbarcolor → StatusBarBackgroundCompatHook + StatusBarBackgroundHook

| 字段 | 值 |
|---|---|
| INSTALLER | `GenericAppInstaller` |
| FEATURE | proposed `statusBarBackgroundColor` |
| PREF_KEYS | `system_statusbarcolor` + `system_statusbarcolor_apps.contains(pkg)` |
| REACHABILITY | PRODUCTION_REACHABLE。先 `PACKAGE_READY` 注册 `Application.attach`，A3 门通过后才装业务 hook |
| PACKAGE | 选中的应用包 |
| PROCESS | 该应用进程（无 main-process 门） |
| PROCESS_SCOPE | `GENERIC_APP` |
| INSTALL_PHASE | 外层 `PACKAGE_READY`；业务 hook 在 `Application.attach` **after**，且 **A3 `isTargetPackage` 之后** |
| CLASSLOADER_OWNER | 外层 attach：framework `Application.class`。业务：`lpparam.classLoader` 解析 `PhoneWindow` / ActionBar（framework 经 parent；androidx/v7 在 app loader） |
| HOOK_TARGETS | Compat：`androidx` 或 `android.support.v7` `ToolbarActionBar` / `WindowDecorActionBar.setBackgroundDrawable`（`findClassIfExists` / `findMethodExactIfExists`）。Background：`PhoneWindow.generateLayout`、`PhoneWindow.setStatusBarColor`、`ToolbarActionBar` / `WindowDecorActionBar.setBackgroundDrawable` |
| CURRENT_INSTALL_ONCE | `isFirstPackage` 保证 attach hook 注册一次；A3 挡住外包装。无 registry |
| LIFECYCLE_OWNER | 静态 `actionBarColor`：**PROCESS_SINGLETON**（`SystemStatusBarAndClockHooks` 文件级 var）。WeakReference 只用于读取 Activity，不静态强持有 Activity |
| COMPATIBILITY_MODE | Compat 探测 fail-open；Background 走 ModuleHelper |
| FATAL_BOUNDARY | 业务 hook 安装在 attach `after` 内，外层 `HookerClassHelper.throwIfFatal`。普通失败 fail-open |
| HOT_PATH_COST | window/actionbar 回调：字段/theme 读取。`isIgnored` 读 PrefMap string set。无 DexKit |
| CURRENT_RISK | 低。进程级 `actionBarColor` 可能在多 Activity 间串色，属于既有产品语义，不是 catalog 收益 |
| MIGRATION_BENEFIT | 无。全量 catalog 进入每个选中 App |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_nooverscroll → NoOverscrollAppHook

| 字段 | 值 |
|---|---|
| INSTALLER | `GenericAppInstaller` |
| FEATURE | proposed `noOverscrollApp` |
| PREF_KEYS | `system_nooverscroll` + `system_nooverscroll_apps.contains(pkg)` |
| REACHABILITY | PRODUCTION_REACHABLE。attach + A3 门之后 |
| PACKAGE | 选中的应用包 |
| PROCESS | 该应用进程 |
| PROCESS_SCOPE | `GENERIC_APP` |
| INSTALL_PHASE | `Application.attach` after，A3 之后 |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | `miuix.springback.view.SpringBackLayout` 构造 + `setSpringBackEnable`；`androidx.recyclerview.widget.RemixRecyclerView` 构造 + `setSpringEnabled`；`android.widget.AbsListView.initAbsListView`。前两类 `findClassIfExists` |
| CURRENT_INSTALL_ONCE | 同 statusbarcolor attach 模型 |
| LIFECYCLE_OWNER | 无长生命周期对象。构造 after 只改 View 实例字段 |
| COMPATIBILITY_MODE | class 缺失 skip；`findAndHookMethodSilently` fail-open。AbsListView 走 ModuleHelper |
| FATAL_BOUNDARY | **CONFIRMED_DEFECT**。构造 after 内两层 `catch (Throwable)` **只** `if (t is OutOfMemoryError) throw t`（`SystemAudioAndVisualAndMoreHooks.kt:173-178` 与 `:193-198`）。`ThreadDeath` / `VirtualMachineError`（含 `StackOverflowError`）及 wrapped fatal **被吞掉**。同类于已关闭的 B1-D2，但是 **B2A 路径上的新独立缺陷**，不重开 B1 |
| HOT_PATH_COST | 构造 after：一次 `callMethod` 或字段写入。列表 init：改 `overScrollMode` |
| CURRENT_RISK | 中。fatal 吞掉违反 AGENTS.md。普通 ROM 失败本意 fail-open，但 catch 过宽 |
| MIGRATION_BENEFIT | **无**。catalog 不会改 inner catch，还会把全量 catalog 拉进用户 App |
| RECOMMENDATION | **CORRECTIVE_BEFORE_MIGRATION**（局部 `RuntimeFatality.throwIfFatal`；纠正后仍应 `KEEP_LEGACY_SAFE`） |

#### controls_volumemedia_up/down → VolumeMediaPlayerHook

| 字段 | 值 |
|---|---|
| INSTALLER | `GenericAppInstaller` |
| FEATURE | proposed `volumeMediaPlayer`（与 system_server `VolumeMediaButtonsHook` **不得**合成一个 FeatureId） |
| PREF_KEYS | `controls_volumemedia_up` 或 `controls_volumemedia_down` `> 0`，且 `controls_mediaplayer_apps.contains(pkg)` |
| REACHABILITY | PRODUCTION_REACHABLE。attach + A3 之后 |
| PACKAGE | 选中的媒体应用包 |
| PROCESS | 该应用进程 |
| PROCESS_SCOPE | `GENERIC_APP` |
| INSTALL_PHASE | `Application.attach` after，A3 之后 |
| CLASSLOADER_OWNER | `lpparam.classLoader` 解析 framework `android.media.MediaPlayer` |
| HOOK_TARGETS | `MediaPlayer.pause`；安装期 `getDeclaredMethod("getAudioStreamType")` |
| CURRENT_INSTALL_ONCE | 同 attach 模型 |
| LIFECYCLE_OWNER | 无。热路径 `sendBroadcast` 一次性 Intent，不保留 receiver |
| COMPATIBILITY_MODE | 安装期 throwing `findClass` / `getDeclaredMethod`。发生在 attach `after` → `HookerClassHelper.throwIfFatal` 后 log → **普通失败 fail-open**（`ClassNotFoundError` 不是 fatal） |
| FATAL_BOUNDARY | 回调包装完整。与 clipboard 的关键差别是：**安装点在 attach 回调内，不在裸 `onPackageReady`** |
| HOT_PATH_COST | `pause`：反射 invoke 已缓存的 Method + `findContext` + broadcast。无 class lookup |
| CURRENT_RISK | 低。`getAudioStreamType` 为 hidden API，缺失时 fail-open |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

GenericApp 汇总：把 FeatureDispatcher 引入 `GENERIC_APP` 会违反「无关进程不初始化无关 Feature」。**0 条 CATALOG_MIGRATION_VALUE。**

---

### 4.2 PackageInstallerRouter

Router 仅在 `ProcessScope.PACKAGE_INSTALLER` 被调用。内层 `pkg.equals("com.miui.packageinstaller")` 是冗余守卫（MIGRATION_RESIDUE），不是死 installer。

无 `isMainProcess` 拒绝。次要进程若存在且 package 仍为 `com.miui.packageinstaller`，会再次安装。无 A13 manifest 证据 → 不升级为缺陷。

#### various_miuiinstaller → MiuiPackageInstallerHook

| 字段 | 值 |
|---|---|
| INSTALLER | `PackageInstallerRouter` |
| FEATURE | proposed `miuiPackageInstallerAllowSysUpdate` |
| PREF_KEYS | `various_miuiinstaller` |
| REACHABILITY | PRODUCTION_REACHABLE（exact package + scope） |
| PACKAGE | `com.miui.packageinstaller` |
| PROCESS | 该包进程（无 main-process 门） |
| PROCESS_SCOPE | `PACKAGE_INSTALLER` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | `android.os.SystemProperties.getBoolean(String, boolean)`；`com.miui.packageInstaller.InstallStart.getCallingPackage`（Silently） |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 无 Activity/View 保留。无 receiver |
| COMPATIBILITY_MODE | ModuleHelper + Silently fail-open |
| FATAL_BOUNDARY | 安装/回调走 ModuleHelper / HookerClassHelper |
| HOT_PATH_COST | 该进程内每次 `SystemProperties.getBoolean`：字符串比较。与 Gboard padding 同类，catalog 不消除 |
| CURRENT_RISK | 低 |
| MIGRATION_BENEFIT | 无。会把全量 catalog 拉进 packageinstaller |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### various_installappinfo → AppInfoDuringMiuiInstallHook

| 字段 | 值 |
|---|---|
| INSTALLER | `PackageInstallerRouter` |
| FEATURE | proposed `appInfoDuringMiuiInstall` |
| PREF_KEYS | `various_installappinfo` |
| REACHABILITY | PRODUCTION_REACHABLE |
| PACKAGE | `com.miui.packageinstaller` |
| PROCESS | 该包进程 |
| PROCESS_SCOPE | `PACKAGE_INSTALLER` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | 主路径：`AppInfoViewObject` 中参数为 `ViewHolder` 的 void 方法（冷路径扫描 declaredFields）。fallback：`PackageInstallerActivity` 中参数为 `String` 的 void 方法 |
| CURRENT_INSTALL_ONCE | `isFirstPackage()`；fallback 可能 hook 多个同签名方法，那是同一启动的多 target，不是 duplicate installer 调用 |
| LIFECYCLE_OWNER | after 里临时拿 Activity/TextView，**不保留**。**VIEW_SCOPED** 短暂突变 |
| COMPATIBILITY_MODE | `findClassIfExists`；缺失则 fallback 或 log return |
| FATAL_BOUNDARY | fallback `getPackageInfo` catch 直接 `OOM \|\| ThreadDeath \|\| VME`，**不**走 `RuntimeFatality` cause-chain。wrapped fatal 可能漏。概率低，记 ARCHITECTURE_DEBT，不单独升级为 B2A 生产缺陷 |
| HOT_PATH_COST | 冷路径：class/field/method 扫描。热路径：View 文本拼接 + `getModuleRes` |
| CURRENT_RISK | 低-中（UI 布局假设）。无 install-once 缺口 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

---

### 4.3 SettingsInstaller

仅 `SETTINGS_MAIN` 进入。`SETTINGS_REMOTE` 在 `isRejected` 被排除。**次要进程排除已证明。**

resource helper（`addResource` / `NotificationVolumeSettingsRes`）与 Java method hook 都在同一 `PACKAGE_READY` 调用栈、method hook 之前或同时注册。resource 必须在 Settings UI inflate 前登记；PACKAGE_READY 是既有产品时序。本轮不把 resource 与 method 合成错误的单一 FeatureId，除非它们已是同一用户功能的两部分（separatevolume / wifipassword / settings icon 属于「一功能两步」，仍用 **一个** 用户功能行描述，不与 system_server catalog id 合并）。

#### miuizer_settingsiconpos → miuizerSettingsHook

| 字段 | 值 |
|---|---|
| INSTALLER | `SettingsInstaller` |
| FEATURE | proposed `miuizerSettingsIcon` |
| PREF_KEYS | `miuizer_settingsiconpos`（`> 0`；**默认 `1` = 开**） |
| REACHABILITY | PRODUCTION_REACHABLE；仅 SETTINGS_MAIN |
| PACKAGE | `com.android.settings` |
| PROCESS | main（`settings` 或空） |
| PROCESS_SCOPE | `SETTINGS_MAIN` |
| INSTALL_PHASE | `PACKAGE_READY`：先 `addResource`，再 method hook |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | resource `ic_miuizer_settings`。`com.android.settings.MiuiSettings.updateHeaderList(List)`。`MiuiSettings$HeaderAdapter.setIcon` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 无静态 Activity。Header 对象插入 ROM list，由 Settings 持有 |
| COMPATIBILITY_MODE | ModuleHelper；回调内 `findClassIfExists` Header，null 则 return |
| FATAL_BOUNDARY | 安装/回调走 ModuleHelper / HookerClassHelper。`ResourceHooks.addResource` 只显式重抛 OOM（共享 helper 债，见第 6 节） |
| HOT_PATH_COST | `updateHeaderList`：扫 header id。非 target discovery |
| CURRENT_RISK | 低 |
| MIGRATION_BENEFIT | 无。Settings 会变成全量 catalog 宿主 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_separatevolume → NotificationVolumeSettingsRes + NotificationVolumeSettingsHook

| 字段 | 值 |
|---|---|
| INSTALLER | `SettingsInstaller` |
| FEATURE | proposed `notificationVolumeSettings`（**禁止**与已 catalog 的 system_server `notificationVolume` 合并 FeatureId） |
| PREF_KEYS | `system_separatevolume` |
| REACHABILITY | PRODUCTION_REACHABLE；仅 SETTINGS_MAIN。server 半边在 `SystemServerInstaller`（后续 batch） |
| PACKAGE | `com.android.settings` |
| PROCESS | Settings main |
| PROCESS_SCOPE | `SETTINGS_MAIN` |
| INSTALL_PHASE | `PACKAGE_READY`：Res 先于 Hook |
| CLASSLOADER_OWNER | Res：`ResourceHooks` 进程单例。Hook：`lpparam.classLoader` |
| HOOK_TARGETS | `addResource("ring_volume_option_newtitle")` 写入静态 `callsResId`。`com.android.settings.MiuiSoundSettings.onCreate`：动态加 notification/system volume Preference |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | `callsResId` **PROCESS_SINGLETON**。Preference 由 Settings fragment 持有，模块不静态持有 |
| COMPATIBILITY_MODE | `findClassIfExists` / `findMethodsByExactParameters`；失败 log return |
| FATAL_BOUNDARY | `onCreate` after 的 catch 走 `rethrowAudioFatal`（遍历 cause；OOM 是 `VirtualMachineError`）。未用 `RuntimeFatality` 但覆盖三类 fatal |
| HOT_PATH_COST | 设置页 onCreate：反射 Preference API。冷于音量键 |
| CURRENT_RISK | 低。双进程产品路径正确 |
| MIGRATION_BENEFIT | 无。迁 Settings 半边不能删 `notificationVolume` server spec，且引入 dispatcher |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_disableanynotif → DisableAnyNotificationHook + DisableAnyNotificationBlockHook(PackageReady)

| 字段 | 值 |
|---|---|
| INSTALLER | `SettingsInstaller` |
| FEATURE | proposed `disableAnyNotificationSettings`（**禁止**与 catalog `disableAnyNotificationBlock` 合并） |
| PREF_KEYS | `system_disableanynotif` |
| REACHABILITY | PRODUCTION_REACHABLE。Settings 侧 `NotificationFilterHelper` / `NotificationChannel` 钩子可达。`packageName.contains("systemui")` 分支在 Settings **DEAD_PATH** |
| PACKAGE | `com.android.settings` |
| PROCESS | Settings main |
| PROCESS_SCOPE | `SETTINGS_MAIN` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader`（`miui.util.NotificationFilterHelper`、`android.app.NotificationChannel` 经 parent） |
| HOOK_TARGETS | Settings 实际执行：`NotificationFilterHelper` 若干方法。BlockHook：`NotificationChannel.isBlockable` / `setBlockable`。systemui 专用 `NotificationSettingsManager` **本 installer 不执行** |
| CURRENT_INSTALL_ONCE | `isFirstPackage()`。与 SystemUI 调用同一函数是 **另一进程** |
| LIFECYCLE_OWNER | 无 |
| COMPATIBILITY_MODE | ModuleHelper fail-open（FilterHelper）。systemui 分支若误执行会 throwing `findClass`；Settings 包名不含 systemui，静态不可达 |
| FATAL_BOUNDARY | Settings 实际路径走 ModuleHelper |
| HOT_PATH_COST | 设置/通道 API 常量返回 |
| CURRENT_RISK | 低（Settings）。systemui 死分支留给后续 SystemUI batch |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_defaultusb → USBConfigSettingsHook

| 字段 | 值 |
|---|---|
| INSTALLER | `SettingsInstaller` |
| FEATURE | proposed `defaultUsbSettings`（**禁止**与 server `USBConfigHook` 合并） |
| PREF_KEYS | `system_defaultusb` ≠ `"none"` |
| REACHABILITY | PRODUCTION_REACHABLE；仅 SETTINGS_MAIN |
| PACKAGE | `com.android.settings` |
| PROCESS | Settings main |
| PROCESS_SCOPE | `SETTINGS_MAIN` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | `com.android.settings.connecteddevice.usb.UsbModeChooserReceiver.onReceive`（Silently） |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 不注册自己的 receiver；hook 已有 ROM receiver。**PROCESS_SINGLETON** method hook |
| COMPATIBILITY_MODE | `findAndHookMethodSilently` fail-open |
| FATAL_BOUNDARY | ModuleHelper / HookerClassHelper |
| HOT_PATH_COST | USB 相关 broadcast：读 PrefMap 字符串 |
| CURRENT_RISK | 低 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_notifimportance → NotificationImportanceHook

| 字段 | 值 |
|---|---|
| INSTALLER | `SettingsInstaller` |
| FEATURE | proposed `notificationImportance` |
| PREF_KEYS | `system_notifimportance` |
| REACHABILITY | PRODUCTION_REACHABLE |
| PACKAGE | `com.android.settings` |
| PROCESS | Settings main |
| PROCESS_SCOPE | `SETTINGS_MAIN` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | `BaseNotificationSettings.setPrefVisible`；`ChannelNotificationSettings.setupChannelDefaultPrefs` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | `Proxy` `OnPreferenceChangeListener` 设到 Preference 上，由 fragment 持有。模块不静态持有 Activity。**ACTIVITY_SCOPED** via ROM preference。无显式 unregister；Xposed 典型模式 |
| COMPATIBILITY_MODE | ModuleHelper；listener 类 `findClassIfExists` |
| FATAL_BOUNDARY | 回调包装完整。Proxy handler 无自己的 catch |
| HOT_PATH_COST | 偏好变更：updateChannel Binder |
| CURRENT_RISK | 低 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_wifipassword → ViewWifiPasswordHook

| 字段 | 值 |
|---|---|
| INSTALLER | `SettingsInstaller` |
| FEATURE | proposed `viewWifiPassword` |
| PREF_KEYS | `system_wifipassword` |
| REACHABILITY | PRODUCTION_REACHABLE |
| PACKAGE | `com.android.settings` |
| PROCESS | Settings main |
| PROCESS_SCOPE | `SETTINGS_MAIN` |
| INSTALL_PHASE | `PACKAGE_READY`：`addResource` 然后 method hooks |
| CLASSLOADER_OWNER | `lpparam.classLoader` |
| HOOK_TARGETS | `SavedAccessPointPreference.onBindViewHolder`；`miuix.appcompat.app.AlertDialog$Builder.setTitle/setMessage`；`AlertDialog.onCreate`；`MiuiSavedAccessPointsWifiSettings.showDeleteDialog` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | 闭包数组 `wifiSharedKey` / `passwordTitle`：**PROCESS_SINGLETON**。`showDeleteDialog` after 在 `canShare` 时清 key。Dialog/View 不静态持有。并发两个 dialog：UNPROVEN |
| COMPATIBILITY_MODE | ModuleHelper。`WifiDppUtils` 在 before 里 throwing `findClass`；发生在回调内 → HookerClassHelper fail-open |
| FATAL_BOUNDARY | 回调包装完整。`addResource` 共享 helper 债 |
| HOT_PATH_COST | bind/dialog：`getIdentifier` / 静态方法取 PSK |
| CURRENT_RISK | 低。进程级 key 槽是既有实现 |
| MIGRATION_BENEFIT | 无 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

Settings 汇总：6/6 `KEEP_LEGACY_SAFE`。次要进程排除已证明。不要把多个用户功能合成一个 FeatureId。

---

### 4.4 AndroidPackageInstaller（HYBRID）

前置：

```text
pkg == "android"          // 冗余于 ANDROID_PACKAGE
isAnyFeatureEnabled()     // 全关则 return：无 dispatcher、无 watcher、无 resource hook
```

`ANDROID_PACKAGE` 要求 `isMainProcess` ⇒ `processName == "android"` 或空。非 main 的 android 包被标成 `SYSTEM_SERVER`，**不会**进入本 installer。

#### system_statusbarheight → StatusBarHeightRes

| 字段 | 值 |
|---|---|
| INSTALLER | `AndroidPackageInstaller` |
| FEATURE | proposed `statusBarHeight`（目前无 catalog id） |
| PREF_KEYS | `system_statusbarheight`（`> 19`） |
| REACHABILITY | PRODUCTION_REACHABLE；仅 ANDROID_PACKAGE |
| PACKAGE | `android` |
| PROCESS | `"android"` 或空 |
| PROCESS_SCOPE | `ANDROID_PACKAGE` |
| INSTALL_PHASE | `PACKAGE_READY` resource replacement（非 Java method hook） |
| CLASSLOADER_OWNER | `ResourceHooks` 进程单例；不使用 app class lookup |
| HOOK_TARGETS | density replacement：`status_bar_height*`（pkg `"*"`） |
| CURRENT_INSTALL_ONCE | `isFirstPackage()` |
| LIFECYCLE_OWNER | ResourceHooks **PROCESS_SINGLETON** |
| COMPATIBILITY_MODE | ResourceHooks 内部 try/catch；缺失资源时无 Java target |
| FATAL_BOUNDARY | ResourceHooks 只显式重抛 OOM（共享 helper） |
| HOT_PATH_COST | 资源查询路径读 replacement map。安装期无 ROM scan |
| CURRENT_RISK | 低。迁 catalog 会让「只改高度」支付全量 dispatcher |
| MIGRATION_BENEFIT | **负** |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### controls_navbarheight → NavbarHeightRes

同 statusbarheight 模型。PREF `controls_navbarheight > 19`。targets：`navigation_bar_*` density + SystemUI `navigation_bar_size`。**KEEP_LEGACY_SAFE**。

完整矩阵字段与上一条相同，仅 PREF/HOOK_TARGETS 不同。不重复。

#### system_allrotations2 → setObjectReplacement(config_allowAllRotations)

| 字段 | 值 |
|---|---|
| INSTALLER | `AndroidPackageInstaller` |
| FEATURE | Android 包 resource 半边。catalog `allRotations` 已在 **system_server**。**禁止合并 FeatureId** |
| PREF_KEYS | `system_allrotations2`（`> 1`；`== 2` 写入 true） |
| REACHABILITY | PRODUCTION_REACHABLE |
| PACKAGE | `android` |
| PROCESS | `"android"` 或空 |
| PROCESS_SCOPE | `ANDROID_PACKAGE` |
| INSTALL_PHASE | `PACKAGE_READY` object replacement |
| CLASSLOADER_OWNER | ResourceHooks |
| HOOK_TARGETS | `android:bool/config_allowAllRotations` |
| CURRENT_INSTALL_ONCE | `isFirstPackage()`。server catalog 是另一进程 |
| LIFECYCLE_OWNER | ResourceHooks PROCESS_SINGLETON |
| COMPATIBILITY_MODE | 无 Java contract；纯资源 |
| FATAL_BOUNDARY | ResourceHooks OOM-only 显式重抛 |
| HOT_PATH_COST | 资源查询 |
| CURRENT_RISK | 低。双进程产品路径 |
| MIGRATION_BENEFIT | 无。再 catalog 会在 android 包进程为 resource-only 用户加载 dispatcher（若用户未开 share/openwith） |
| RECOMMENDATION | **KEEP_LEGACY_SAFE** |

#### system_rotateanim → RotationAnimationRes

同 allrotations 模型。PREF `system_rotateanim > 1`。`setResReplacement` 若干 `screen_rotate_*_enter/exit`。server 半边 catalog `rotationAnimation`。**KEEP_LEGACY_SAFE**。

#### system_cleanshare → cleanShareMenu

| 字段 | 值 |
|---|---|
| INSTALLER | `AndroidPackageInstaller` via `FeatureDispatcher.installById("cleanShareMenu")` |
| FEATURE | catalog id `cleanShareMenu`（**禁止**与 `cleanShareMenuService` 合并） |
| PREF_KEYS | `system_cleanshare`（安装门）；热路径另读 `system_cleanshare_apps` |
| REACHABILITY | PRODUCTION_REACHABLE；仅 ANDROID_PACKAGE 且 pref true |
| PACKAGE | `android` |
| PROCESS | `"android"` 或空；runtime 键为传入的 `"android"` |
| PROCESS_SCOPE | `ANDROID_PACKAGE`（spec 一致） |
| INSTALL_PHASE | spec `PACKAGE_READY`；dispatcher 传入同一 phase |
| CLASSLOADER_OWNER | `lpparam.getClassLoader()` 传入 FeatureRuntime |
| HOOK_TARGETS | contract = `CatalogContracts.cleanShareMenu`：`miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner.run`（ALL_METHODS_BY_NAME）。installer 调用 `CleanShareMenuHook`，target **一致** |
| CURRENT_INSTALL_ONCE | registry `(processName="android", "cleanShareMenu")` + `isFirstPackage` |
| LIFECYCLE_OWNER | 无长生命周期。after 里临时 View `performClick`。**VIEW_SCOPED** 短暂 |
| COMPATIBILITY_MODE | `CONTRACT_REQUIRED` + `legacyInstall` / resolver。INCOMPATIBLE → 不装 hook |
| FATAL_BOUNDARY | registry `runCompatibilityAndInstaller`：fatal 重抛（**不** unwrap cause chain，ARCHITECTURE_DEBT）。普通失败 FailedTransient。回调走 HookerClassHelper |
| HOT_PATH_COST | resolver run after：字段读取 + `getStringSet` + `getIdentifier`/`findViewById`。无 DexKit。PrefMap 快照由 watcher 更新 |
| CURRENT_RISK | 低。已是正确 hybrid 路径 |
| MIGRATION_BENEFIT | **已在 catalog**。无需再迁 |
| RECOMMENDATION | **KEEP_LEGACY_SAFE**（保持现状；不是新迁移候选项） |

`listenerNeeded`：`installById` 返回 `FeatureInstallResult.isActive`（Installed 或 AlreadyInstalled）。失败/disabled 不注册 watcher。`watchPreferences` = `PreferenceBootstrap.ensureWatcher()`，**PROCESS_SINGLETON**，有 lock 与替换语义。`ConfigReloadMode.NONE` 与「仍 watch 以更新 apps set 快照」并存：热路径读 live PrefMap，不重装 hook。这是具体收益，不是缺陷。

disabled：`system_cleanshare=false` 时不进入 `createRuntime` 分支（除非 openwith 开）。两者都关且仅 resource 开：dispatcher 不初始化。

#### system_cleanopenwith → cleanOpenWithMenu

与 cleanShare 对称。id `cleanOpenWithMenu`；contract `ResolverActivityRunner.run`；hook 过滤 `ACTION_VIEW`。server 半边 `cleanOpenWithMenuService`。**KEEP_LEGACY_SAFE**（保持现状）。

resolver selected target == 实际 hook target：**是**。

---

## 5. AndroidPackage `createRuntime` 分类

调用：

```java
FeatureDispatcher.createRuntime(pkg, lpparam, lpparam.getClassLoader(), MainModule.mPrefs);
```

`pkg` 来自 `lpparam.getPackageName()`，在此 installer 中为 `"android"`。

`FeatureRuntime.processName` 文档/参数名表示进程名。`FeatureInstallRegistry` 用它做：

- `FeatureStateKey(processName, canonicalId)`
- `spec.processTarget.matches(runtime.processName)`

`cleanShareMenu` / `cleanOpenWithMenu` 的 `processTarget = ProcessTarget.Package("android")`，`matches` 为 `processName == "android"`。

本路径事实：

1. 只有 `ANDROID_PACKAGE` 进入该 installer。
2. `ANDROID_PACKAGE` 要求 `isMainProcess` ⇒ `processName == "android"` **或** `processName.isEmpty()`。
3. 传入的始终是 package `"android"`，不是 raw `MainModule.processName`。

| 真实 processName | 传入 runtime.processName | ProcessTarget.Package("android") | 判定 |
|---|---|---|---|
| `"android"` | `"android"` | match | **CORRECT** for this pair |
| `""`（empty 仍算 main） | `"android"` | match | **SEMANTICALLY_EQUIVALENT**（若传真实空串，`matches` 会失败） |
| `"system"` / 其它 | 不会进入本 installer | n/a | 由 ProcessScopes 挡在外面 |

**总分类（本路径）：`SEMANTICALLY_EQUIVALENT`。**

**总分类（hybrid 惯例）：`ARCHITECTURE_DEBT`。** 参数名叫 processName，实际传 packageName。F1 P1-3 已记录。在 ANDROID_PACKAGE 上恰好 package≈process。

**不是 `CONFIRMED_DEFECT`：** 没有证实错误安装、错误拒绝、或错误的 install-once 键。empty processName 时传 package 反而避免 `Package("android").matches("") == false`。

**不是 `INSUFFICIENT_EVIDENCE`：** 路由与 matches 语义静态可证。

---

## 6. CONFIRMED_DEFECTS / ARCHITECTURE_DEBT / MIGRATION_RESIDUE

### CONFIRMED_DEFECTS（B2A 路径，静态）

| ID | 路径 | 事实 | 正确修正方向 |
|---|---|---|---|
| B2A-D1 | `SmartClipboardActionHook` | `onPackageReady` 上 throwing `findClass(SecurityPromptHandler)`；`ClassNotFoundError extends Error`；无入口 catch | LOCAL_CORRECTIVE：`findClassIfExists` + `RuntimeFatality`。**不是 catalog** |
| B2A-D2 | `NoOverscrollAppHook` | 4 个 `catch (Throwable)` 只重抛 `OutOfMemoryError` | LOCAL_CORRECTIVE：`RuntimeFatality.throwIfFatal`。**不是 catalog** |

两者都是 `CORRECTIVE_BEFORE_MIGRATION`。纠正后预期仍 `KEEP_LEGACY_SAFE`。本轮 **不实现**。

不要把它们算进 B1：B1 已 CLOSED；这是 GenericApp 路径上的独立证据。

### ARCHITECTURE_DEBT（不升级为本轮生产候选项）

| 项 | 说明 |
|---|---|
| `FeatureDispatcher.<clinit>` 全量 registerAll | 任何新 catalog 宿主的前置反收益 |
| `createRuntime(pkg)` 命名 | SEMANTICALLY_EQUIVALENT on ANDROID_PACKAGE |
| `PACKAGE_INSTALLER` 无 `isMainProcess` | 与 B1 五 scope 同类；无 manifest 不升级 |
| `GENERIC_APP` 无 `isMainProcess` | 产品需要多 App；次要进程 UNPROVEN |
| `ResourceHooks` catch 只显式 OOM | 共享 helper；Settings/AndroidPackage resource 路径用到。不在 B2A 改 helper |
| `FeatureInstallRegistry.isFatal` 不 unwrap cause | catalog 路径债 |
| AppInfo fallback catch 无 cause-chain | 低概率 |
| `actionBarColor` / `callsResId` / wifi 闭包数组 | PROCESS_SINGLETON 既有状态 |
| `ConfigReloadMode.NONE` vs ensureWatcher | 快照更新仍有用 |
| F1 P1-3「已拒绝所有多进程次要进程」 | 对本轮 PACKAGE_INSTALLER / GENERIC_APP 过宽 |

### MIGRATION_RESIDUE / DEAD_PATH

| 项 | 分类 |
|---|---|
| `PackageInstallerRouter` 内层 package equals | MIGRATION_RESIDUE（冗余，可达） |
| `AndroidPackageInstaller` 内层 `"android".equals(pkg)` | MIGRATION_RESIDUE（冗余，可达） |
| `DisableAnyNotificationHook` systemui 分支（Settings 调用） | DEAD_PATH in this installer；函数对 SystemUI 仍活 |
| `ProcessScopes.shouldLoadPrefs` / `shouldHook` | 未用于 `onPackageReady`（F1 已记录；本轮不删） |

本轮不删除。

---

## 7. INSUFFICIENT_EVIDENCE

| 项 | 缺什么 | 影响 |
|---|---|---|
| 用户 LSPosed 是否勾选 `com.lbe.security.miui` | 设备模块作用域 | 不否定路由可达性；只影响现场是否触发 D1 |
| LBE / packageinstaller / 选中 generic App 的次要进程名 | A13 manifest | 不能声称 duplicate install 已发生 |
| HyperOS 1 LBE 是否仍有 `SecurityPromptHandler` | ROM/DEX | D1 触发频率未知；静态缺陷仍成立 |
| libxposed 是否隔离 `onPackageReady` 异常 | 框架行为 | D1 最终是否杀死进程 |
| android 包 resource replacement 是否覆盖 SystemUI 进程资源 | 设备/日志 | 不把既有产品路径升级为缺陷 |
| `Application.attach` 对同一 Application 是否可触发两次 | 运行时 | 不重开 A3 |
| wifi 双 dialog 竞态 | UI 时序 | 不升级 |

A14 @ `d20d96b5` 仅作工程标准参考。未把 A14 installer 形状当作 A13 迁移命令。Dynamic Island 不在范围。

---

## 8. B2A_PRODUCTION_CANDIDATES

```text
B2A_PRODUCTION_CANDIDATES = []
```

逐 installer：

| Installer | 结果 |
|---|---|
| GenericAppInstaller | clipboard / nooverscroll = `CORRECTIVE_BEFORE_MIGRATION`（局部 fatal/fail-open，**非 catalog**）。其余 3 条 `KEEP_LEGACY_SAFE` |
| PackageInstallerRouter | 2/2 `KEEP_LEGACY_SAFE` |
| SettingsInstaller | 6/6 `KEEP_LEGACY_SAFE`；`SETTINGS_REMOTE` 排除已证明 |
| AndroidPackageInstaller | 4 resource/legacy `KEEP_LEGACY_SAFE`；2 catalog 路径保持 HYBRID。`createRuntime` = SEMANTICALLY_EQUIVALENT + ARCHITECTURE_DEBT |

**不选 catalog migration 的原因不是数量不够，而是没有一条同时满足：concrete catalog benefit，且不把全量 FeatureCatalog 拉进当前未加载 catalog 的进程。**

已 catalog 的 `cleanShareMenu` / `cleanOpenWithMenu` 不是「迁移候选项」；它们已经在正确的 HYBRID 位置。

D1/D2 明确需要 `LOCAL_CORRECTIVE_ONLY`，但与 B1 一样：**选型轮不授权生产修改**。它们不是 CATALOG_MIGRATION / ROUTING_CORRECTIVE / LIFECYCLE_CORRECTIVE。

---

## 9. PROPOSED_MINIMAL_DESIGN

无 B2A 生产候选项。无新 FeatureId / FeatureSpec / dispatcher 设计。

若未来（需单独授权）仍要收敛 GenericApp / Settings / PackageInstaller：

1. **前置：** `FeatureDispatcher` / `registerAll` 改为按 `ProcessScope` 惰性注册。GenericApp 进程不得构建 SystemUI/system_server spec。
2. B2A-D1 / B2A-D2 局部 corrective **先于** 任何 GenericApp catalog 讨论。
3. 双进程功能保持 **两个 FeatureId**（Settings UI vs system_server；AndroidPackage resource vs server hook；`cleanShareMenu` vs `cleanShareMenuService`）。
4. `createRuntime` 若改为真实 `processName`，必须同步证明 `ProcessTarget.Package("android")` 在 empty processName 下仍 match；当前传 package 在该路径是等价且对 empty 更稳。
5. GenericApp 的 package-set 条件（`*_apps.contains(pkg)`）无法用现有只接收 `PrefMap` 的 `FeatureSpec.condition` 表达。`ProcessTarget.Any` 会污染其它 generic 进程的 registry 状态。

**不要实现以上任何一项。**

---

## 10. INFRASTRUCTURE_MAINTENANCE_NOTE

B1 freeze 机器上 Gradle 9.6.1 对严格 `gradle/verification-metadata.xml` 存在已知缺口（缺少 `junit-bom-*.module`、`guava-parent-33.4.0-jre.pom` 一类 metadata）。

本任务是 docs/static only，**未**修改：

- `gradle/verification-metadata.xml`
- dependencies
- repositories
- Gradle configuration

`python tools/verify.py fast --changed` 的 Gradle 步在该机器上仍会被该缺口挡住。这不是 B2A 生产问题。

---

## 11. FUTURE_MAINTENANCE_NOTE（不扩大本轮）

- B2A-D1 / B2A-D2 若另开 corrective 任务：只改对应 hook 函数 + 窄测试；不改 installer / catalog / MainModule / ProcessScopes。
- `SystemAudioAndVisualAndMoreHooks.kt` 中其它非 B2A 路径的 OOM-only catch 不在本轮。
- `DisableAnyNotificationHook` 的 systemui 分支留给 SystemUI batch。
- 不要把 `PreferenceLoadRegistry` 注释里的「will be migrated in a separate phase」当成 B2A 授权。

---

## 12. 证据等级与冻结

```text
B2A_STATIC_RESULT = NO_PRODUCTION_CANDIDATE
PHASE_A_REOPENED = NO
B1_REOPENED = NO
A1/A2/A3 未发现要求重开的同路径新缺陷
A3_PACKAGE_IDENTITY_FILTER = STILL_BEFORE_GENERICAPP_ATTACH_HOOKS
PRODUCTION_CHANGED = NO
B2A_PRODUCTION_AUTHORIZATION = NO
B2B_STARTED = NO
```

未修改任何 production Java/Kotlin。未修改 FeatureCatalog / FeatureDispatcher / MainModule / ProcessScopes。未开始 B2B。未 merge / rebase / force-push / tag / release。
