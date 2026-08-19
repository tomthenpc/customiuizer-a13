# A13 B2B — SecurityCenter Architecture Selection

## 元信息

| 项目 | 值 |
|---|---|
| AUTHORITATIVE_BASE_SHA | `34ec6cf7d7bea827eb2ede233dfab4aa30619a19` |
| BRANCH | `devin/a13-foundation-parity-r13.11.1` |
| PHASE_A_FREEZE_SHA | `03a7a082048c028c185eaf351ea167af6bdb4697` |
| B1_FREEZE_SHA | `ba5c2c1f796bec3fb714fe16d83687d14c7dbd02` |
| B2A_FREEZE_SHA | `34ec6cf7d7bea827eb2ede233dfab4aa30619a19` |
| PINNED_A14_REFERENCE | `tomthenpc/customiuizer-a14` @ `d20d96b543a49a584970e312da7d704958a155aa` |
| PRODUCTION_AUTHORIZATION | YES（仅 B2B-D1/D2/D3/D4） |
| PRODUCTION_CHANGED | YES（仅 `SecurityCenterInstaller.java` + `Various.kt`） |
| 性质 | 选型证据 + 授权 corrective 最小更新 |
| 范围 | `SecurityCenterInstaller` 及其 16 条直接可达生产 hook；为证明路由 / install-once / fatal / catalog 交叉半边所必需的 `MainModule`、`ProcessScopes`、`FeatureCatalog` / `FeatureDispatcher`、`ModuleHelper`、`RuntimeFatality` |

```text
STATIC_VERIFIED = YES
BUILD_VERIFIED  = NO   (docs-only; Android compile not required)
LOG_VERIFIED    = NO
DEVICE_VERIFIED = NO
DEPENDENCY_VERIFICATION_CLEAN_GATE = NOT_PROVEN
B2B_PRODUCTION_AUTHORIZATION = NO
B3_STARTED = NO
PHASE_A_REOPENED = NO
B1_REOPENED = NO
B2A_REOPENED = NO
```

本文件是 Phase B2B 的选型证据，不是 PASS authority。ChatGPT 是最终 Gatekeeper。

A1 / A2 / A3 / B1 / B2A 保持 CLOSED。本轮没有证明需要重开那些 freeze 的、与 SecurityCenter 直接相关的新生产缺陷属于那些阶段的责任范围。

**本轮未审计：** `LauncherInstaller`、`SystemUiInstaller`、`SystemServerInstaller` 内部实现。仅在 catalog 交叉半边需要时引用其调用点 / FeatureSpec。未做 ROM 提取。

---

## 1. 结论先行

`SecurityCenterInstaller` 是 `SECURITY_CENTER_MAIN` 上的 LEGACY_DIRECT 直装路径。16 条路径全部只在 MAIN 安装。`SECURITY_CENTER_REMOTE` 与 `SECURITY_CENTER_BOOTAWARE` 的 `isInstallable = false` 会在 `MainModule.onPackageReady` 进入 installer **之前** return。没有一条路径证明需要 remote / bootaware。

当前 **不经过** `FeatureDispatcher` / `FeatureInstallRegistry`。把 SecurityCenter 变成 catalog 宿主会在 `com.miui.securitycenter` 主进程执行：

```text
FeatureDispatcher.<clinit>
  → FeatureInstallRegistry.registerAll(FeatureCatalog.specs())
  → 构建并注册全部 FeatureSpec（含全部 CONTRACT_REQUIRED contract）
```

证据：`FeatureDispatcher.kt:23-25`。这与 AGENTS.md「无关进程不初始化无关 Feature」冲突。现有 HYBRID installer（SystemUI / Launcher / system_server / android 包）已经支付这笔成本；SecurityCenter **尚未支付**。

因此：

- 不允许用「catalog 更统一」或「更好诊断」作为迁移理由。
- **0 条 `CATALOG_MIGRATION_VALUE`。**
- 已证明的缺口是局部 fail-open / 嵌套 hook 一次安装 / 静态 Fragment 持有，catalog 不能诚实表达 Activity 回调内的二次 `hookAllMethods`。
- 在 `FeatureDispatcher` 具备 **按 ProcessScope 惰性注册** 之前，SecurityCenter 不应成为 catalog 宿主。该 dispatcher 改造本身也不是 B2B。

本轮 **已实施** 授权 corrective（catalog 仍为 0）：

| ID | 摘要 | 落地动作 |
|---|---|---|
| B2B-D1 | 三条裸 `findClass` 可中止后续路径 | **COMPATIBILITY_FAIL_OPEN**（`findClassIfExists`）。`AppsRestrictHook` 缺 `AppManageUtils` 只跳过依赖块，networkassistant 子 hook 继续 |
| B2B-D2 | 双 `if` + 每次 `onCreate` 再 hook `onActivityCreated` | installer 合并 predicate（**保留 `various_skip`**）；PACKAGE_READY 尝试解析 fragment field，否则 first-success 守卫 |
| B2B-D3 | class-level `onPreferenceTreeClick` 捕获第一次 `Activity` | 独立 MethodHook 从当前 `param.thisObject` 取 Activity / PackageInfo。**class-level hook callback captures Activity = CONFIRMED lifecycle ownership defect**（已局部纠正） |
| B2B-D4 | 直接类型 fatal 检查不传播 wrapped fatal | **CONFIRMED_DEFECT**；16 条路径直接可达 catch 改为 `RuntimeFatality.throwIfFatal` |

`mSupportFragment` 未改（仍为 LIKELY，非本轮授权）。`CATALOG_MIGRATION_CANDIDATES = 0`。

`various_skip` 分支是 **MIGRATION_RESIDUE**（无 UI、hook 不读该 key），并入 B2B-D2 兼容 predicate，**未删除**。

---

## 2. 共同路由事实

### 2.1 入口

`MainModule.onPackageReady` 顺序（`MainModule.java:89-115`）：

```text
API 33 gate
→ lpparam.isFirstPackage()
→ ProcessScopes.resolve(pkg, processName)
→ ProcessScopes.isRejected(pkg, processName)
→ PreferenceLoadRegistry.shouldLoad(remote, pkg)
→ PreferenceBootstrap.start()
→ if (scope == SECURITY_CENTER_MAIN) SecurityCenterInstaller.install(lpparam); return;
```

`onPackageReady` **没有** try/catch。installer / hook 安装期抛出的 `Error` / 非 fatal 异常会离开模块入口。libxposed 是否隔离该回调：`INSUFFICIENT_EVIDENCE`。静态上应按「可杀死宿主进程，并中止同一 `install()` 中后续路径」处理。

`MainModule.processName` 来自 `onModuleLoaded` 的 `param.getProcessName()`，**不是**从 `packageName` 推断。`ProcessScopes.resolve(pkg, processName)` 同时使用两者。

### 2.2 SecurityCenter ProcessScope

证据：`ProcessScope.kt:32-38, 94-97, 121-122` + `tools/tests/test_process_scope_table.py`。

| 条件 | PROCESS | PROCESS_SCOPE | `isInstallable` | installer |
|---|---|---|---|---|
| `pkg == com.miui.securitycenter` 且 `isMainProcess` | 主进程：`packageName == processName \|\| processName.isEmpty()` | `SECURITY_CENTER_MAIN` | true | `SecurityCenterInstaller.install` |
| 同包且 `processName` 以 `.bootaware` 或 `:bootaware` 结尾 | bootaware | `SECURITY_CENTER_BOOTAWARE` | **false** | 不进入。`isRejected` 在 installer 之前 return |
| 同包、非 bootaware、非 main | 例如 `:remote` | `SECURITY_CENTER_REMOTE` | **false** | 同上 |

`isBootawareProcess` **先于** remote 判定。`com.miui.securitycenter:bootaware` 与 `com.miui.securitycenter.bootaware` 均为 BOOTAWARE，不是 REMOTE。

`com.miui.securitycenter` 在 `KNOWN_PACKAGES`。MAIN 上 `PreferenceLoadRegistry.shouldLoad` 会加载 prefs。关闭功能仍加载快照，但不进入各 `if (pref)` 的 hook 调用。

### 2.3 16 条路径的共同身份

对全部 16 条（在 installer 被调用的前提下）：

| 字段 | 值 |
|---|---|
| PACKAGE | `com.miui.securitycenter` |
| PROCESS | 主进程（上表 `isMainProcess`） |
| PROCESS_SCOPE | `SECURITY_CENTER_MAIN` |
| INSTALL_PHASE | `PACKAGE_READY` |
| CLASSLOADER_OWNER | 默认 `PackageReadyParam.classLoader`。例外见各路径：framework `Class` 字面量、`Settings.*` |

REMOTE / BOOTAWARE 上这 16 条 **全部不可达**。这是证明过的排除，不是包名推断。

是否有 feature **需要** remote / bootaware：静态上 16 个 hook 的目标类名都属于 SecurityCenter APK / 其合并组件 / framework。没有路径以 remote 或 bootaware ClassLoader 为安装条件。`AppsRestrictHook` 的 `FirewallService` / `ShowAppDetailFragment` 是否实际只存在于 `:remote`：**INSUFFICIENT_EVIDENCE**（无 ROM / manifest）。若它们只在 remote，则当前排除会让这两处 **在 MAIN 上成为死 hook**，但 `AppManageUtils` 仍可能在 MAIN 成功；不能把「exclusion makes the whole feature dead」写成 CONFIRMED。

### 2.4 install-once（package-ready 层）

| 层 | SecurityCenterInstaller |
|---|---|
| `isFirstPackage()` | 是；本进程 `onPackageReady` 只走一次 |
| `ProcessScope` | 仅 MAIN 调用 installer |
| 局部 `AtomicBoolean` / `isHooked` | 安装期无。`AddSideBarExpandReceiverHook` 的 `isHooked[]` 是 **构造回调内** 守卫 |
| `FeatureInstallRegistry` | 不经过 |
| preference 变更重装 | 否 |

**package-ready 安装一次 ≠ Activity 回调里再 `hookAllMethods` 一次。** 见 B2B-D2 / B2B-D3。

### 2.5 ModuleHelper / libxposed 是否去重

`XposedHelpers.doHookMethod`（`XposedHelpers.java:853-858`）：

```text
moduleInst.hook(m).setPriority(...).setExceptionMode(PASSTHROUGH).intercept(hook)
```

每次调用创建一个新的 `HookHandle` 并对 **传入的那一个** `MethodHook` 实例 `intercept`。生产 helper **没有**「已 hook 成员集合」。`hookAllMethods` 对每个匹配 `Method` 再调一次 `doHookMethod`。

两次安装传入的是两个 **不同的** 匿名 `MethodHook` 实例。即使 libxposed 按 callback 身份去重（未证明），也不会合并这两个实例。

libxposed 是否在内部再折叠：**不要假设**。静态结论：本模块会注册 **两条独立 interceptor**。

### 2.6 普通失败 vs fatal（B2A 标准）

- `ModuleHelper.findAndHookMethod` / `hookAllMethods` / `findAndHookConstructor`：`catch (Throwable)` → `throwIfFatal`（cause 链最多 8 层，OOM / ThreadDeath / VirtualMachineError）→ log → fail-open。
- `XposedHelpers.findClass` 失败抛 `ClassNotFoundError extends Error`。它 **不是** fatal。在裸 `onPackageReady` 上未捕获时会中止 `SecurityCenterInstaller.install()` 的后续语句。
- 回调内 `HookerClassHelper` 有同等 cause 链 `throwIfFatal`。
- 本轮若干 `catch (Throwable)` 只对 **直接类型** 判断 OOM / ThreadDeath / VME，不走 `RuntimeFatality`。这与 B2A 对共享原语 `ResourceHooks` 的 CONFIRMED 不同：此处标 **ARCHITECTURE_DEBT**，除非是 OOM-only 或裸 `findClass`。
- 未扫描 `Various.kt` 中与这 16 条无关的函数。

---

## 3. 判定规则（本轮实际使用）

| 规则 | 应用 |
|---|---|
| Catalog 迁移必须有具体收益 | 重复安装预防、进程所有权、resolver 不匹配、无法用 legacy 表达的生命周期、拆除真实平行路由。一致性 / 诊断不够 |
| `FeatureInstallRegistry` 不能表达 Activity 内嵌套 hook | 嵌套一次安装必须局部 `isHooked` / unhook |
| throwing `findClass` 在裸 package-ready | 与 B2A-D1 同类 → `CONFIRMED_DEFECT` |
| 静态 Android 引用 ≠ 自动泄漏 | 必须证明赋值 / 无清除 / 可跨过销毁 |
| 双进程半边不得合成一个 FeatureId | 即使共享 preference |
| 无 ROM 时 ABI | `INSUFFICIENT_EVIDENCE`，不是自动缺陷 |
| 热路径 | 只标记回调内有意义的反射 / Binder / 堆栈遍历。不做微优化选型 |

RECOMMENDATION 仅允许：`KEEP_LEGACY_SAFE` / `CATALOG_MIGRATION_VALUE` / `CORRECTIVE_BEFORE_MIGRATION` / `INSUFFICIENT_EVIDENCE`。

---

## 4. HIGH-PRIORITY：AppsDefaultSort 双安装证明

Installer（`SecurityCenterInstaller.java:21-22`）：

```java
if (MainModule.mPrefs.getStringAsInt("various_appsort", 1) > 1) Various.AppsDefaultSortHook(lpparam);
if (MainModule.mPrefs.getStringAsInt("various_skip", 0) > 0) Various.AppsDefaultSortHook(lpparam);
```

### 4.1 两个 preference 能否同时合法开启？

| Key | UI | 默认 | 合法可设值 |
|---|---|---|---|
| `various_appsort` | `prefs_various.xml` `ListPreferenceEx` `pref_key_various_appsort`；`appsort_val` = `1,2,3,4` | `"1"` | 用户可选 2/3/4，使 `> 1` 为真 |
| `various_skip` | **无** `pref_key_various_skip`。仓库内无其它生产读写。易混淆的是 `various_skip_interceptperm` / `various_skip_securityscan` | `getStringAsInt(..., 0)` → **0** | 当前 UI **不能**设置 |

`PrefMap` 若含导入 / 旧快照中的 `various_skip` 且解析为 `> 0`，第二分支为真。这不是当前产品 UI 合约。

**结论：** 当前 UI 不能同时打开两条。PrefMap 残留可以使两条同时为真。`various_skip` 分支 = **MIGRATION_RESIDUE**。

### 4.2 一次 `onPackageReady` 会不会调用两次 `AppsDefaultSortHook`？

会，当两个 predicate 都为真。两条 `if` 独立，中间无合并、无 `isHooked`。

仅 `various_appsort > 1`：调用一次。  
仅 `various_skip > 0`（残留 key）：仍调用一次，但见 4.5。

### 4.3 会不会对同一 method 装两次 hook？

`AppsDefaultSortHook` 每次都 `ModuleHelper.findAndHookMethod("com.miui.appmanager.AppManagerMainActivity", ..., "onCreate", ...)`。两次调用 → 两个不同 `MethodHook` → 按 §2.5 **两条** `onCreate` interceptor。

### 4.4 ModuleHelper / libxposed 去重？

见 §2.5。生产代码不去重。不要假设 libxposed 折叠。

### 4.5 重复回调的可观察效果？

`checkBundle` **只读** `various_appsort`（默认 1），写入 `current_sory_type` / `current_sort_type` = `order - 1`。不读 `various_skip`。

| 场景 | 效果 |
|---|---|
| 双 `onCreate` before | 对同一 Bundle 写两次相同 int。排序结果幂等 |
| 仅 skip 残留、appsort 保持 1 | 仍安装 hook，写入 sort type **0**。这不是「skip」功能 |
| 嵌套 `onActivityCreated`（见下） | 每次 Activity `onCreate` 再注册一条 class-level 回调。N 次打开 App 管理 → N 条 `onActivityCreated` interceptor。`checkBundle` 仍幂等 |

### 4.6 局部幂等守卫？

无。`AppsDefaultSortHook` 无 `isHooked` / AtomicBoolean / unhook。

### 4.7 修正形态？语义是否不同？

**没有** appsort vs skip 的语义分叉。Hook 实现只有 appsort。

后续局部修正（本轮不做）：

1. 删除 `various_skip` 分支（残留），**或** 合并 predicate 后只调用一次。
2. **另外** 必须给嵌套 `onActivityCreated` 加安装一次守卫。只合并 installer `if` **不能**修复嵌套重复。

Catalog 不能修复嵌套问题：`FeatureInstallRegistry` 键是 `(process, featureId)`，管的是 package-ready 一次，不是 Activity `onCreate`。

### 4.8 分类

| 项 | 分类 |
|---|---|
| 当前 UI 双 predicate | 不可达。skip = **MIGRATION_RESIDUE** |
| PrefMap 残留导致双调用 | 代码路径 **CONFIRMED**；产品 UI 不可达 → 不单独作为最高优先级，并入 B2B-D2 |
| 嵌套 `onActivityCreated` 每次 `onCreate` 再 hook | **CONFIRMED_DEFECT**（B2B-D2）。可达性：`various_appsort > 1` 时用户每次进入 App 管理 |
| INTENTIONAL_DIVERGENCE | 否 |

---

## 5. 16 路径矩阵

表中 PACKAGE / PROCESS_SCOPE / INSTALL_PHASE 除非注明，否则同 §2.3。

### 5.1 总表

| # | FEATURE | PREF_KEYS | REACHABILITY | HOOK_TARGETS | CURRENT_INSTALL_ONCE | NESTED_HOOK_BEHAVIOR | LIFECYCLE_OWNER | COMPATIBILITY_MODE | FATAL_BOUNDARY | HOT_PATH_COST | CURRENT_RISK | CATALOG_BENEFIT | RECOMMENDATION |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | AppInfo | `various_appdetails` | PRODUCTION_REACHABLE MAIN | `AMAppInfomationActivity.onCreate`；`androidx.fragment.app.Fragment` 构造；嵌套 `onPreferenceTreeClick` | `isFirstPackage` + MAIN。无 registry | **每次** Activity `onCreate` 再 `hookAllMethods`。无守卫 | `mSupportFragment` **PROCESS_SINGLETON** 强引用；`mLastPackageInfo` 为数据 | `findClassIfExists` 缺 activity 则 return | 安装走 ModuleHelper。若干 catch 仅直接类型 fatal | `onCreate` 后主线程 post：反射找 field/method + `addPref`。点击：clipboard / startActivity | 嵌套重复回调；Fragment 强持有 | 无。registry 不管嵌套 | **CORRECTIVE_BEFORE_MIGRATION** |
| 2 | AppsDisable UI | `various_disableapp` | PRODUCTION_REACHABLE MAIN | `ApplicationsDetailsActivity.onCreateOptionsMenu` / `onOptionsItemSelected` | `isFirstPackage` + MAIN | 无嵌套 hook。菜单项 666 在每次 **新 Menu** 上 add，符合 Android 菜单重建 | 无静态 Activity。AlertDialog 局部 | ModuleHelper fail-open | `setAppState` catch 仅直接类型 fatal | 菜单回调：PM query + 可选 dialog | 低（UI 半边） | 无。system_server 半边已 catalog，不得合并 FeatureId | **KEEP_LEGACY_SAFE** |
| 3 | AppsRestrict SC | `various_restrictapp` | PRODUCTION_REACHABLE MAIN；PowerKeeper 另有半边 | throwing `findClass(AppManageUtils)` + `getAppInfo`；`ShowAppDetailFragment.initFirewallData`；`FirewallService.setSystemAppWifiRuleAllow` | `isFirstPackage` + MAIN | 无 Activity 内再 hook | 无 | **裸 `findClass` fail-closed** | **B2B-D1**。失败会中止 installer 后续路径 | `getAppInfo` after：改 flags。冷 | 高：安装期 abort | 无。局部 `findClassIfExists` | **CORRECTIVE_BEFORE_MIGRATION** |
| 4 | ScrambleAppLockPIN | `system_applock_scramblepin` | PRODUCTION_REACHABLE MAIN | `MiuiNumericInputView` 全部构造 after | `isFirstPackage` + MAIN | 无 | 无静态 View 保留。shuffle 发生在该实例 | ModuleHelper `hookAllConstructors` 缺类 fail-open | 构造 after 无 catch；回调包装 throwIfFatal | 构造：View 树 shuffle | 低 | 无。与 catalog `appLock`（`system_applock`）无关 | **KEEP_LEGACY_SAFE** |
| 5 | AppsDefaultSort | `various_appsort` | PRODUCTION_REACHABLE 当 `> 1` | `AppManagerMainActivity.onCreate`；嵌套 fragment `onActivityCreated` | package-ready 一次；**嵌套否** | **B2B-D2 CONFIRMED** | 无静态 UI 对象 | ModuleHelper | 嵌套 catch 仅直接类型 fatal | `onCreate`/`onActivityCreated`：pref 读 + Bundle put。字段扫描在每次 onCreate（冷到中） | 嵌套 interceptor 累积 | 无 | **CORRECTIVE_BEFORE_MIGRATION** |
| 6 | AppsDefaultSort via skip | `various_skip` | **UI 不可达**。仅残留 PrefMap | 同 #5 | 同 #5 | 同 #5 | 同 #5 | 同 #5 | 同 #5 | 同 #5 | MIGRATION_RESIDUE | 无 | **CORRECTIVE_BEFORE_MIGRATION**（删除或合并分支） |
| 7 | InterceptPerm | `various_skip_interceptperm` | PRODUCTION_REACHABLE MAIN | throwing `findClass(InterceptBaseFragment)` + 内部 Handler 构造 / `handleMessage` 形方法 | `isFirstPackage` + MAIN | 无 | 无 | **裸 `findClass` fail-closed** | **B2B-D1** | Handler 构造：改 delay 参数 | 高：安装期 abort | 无 | **CORRECTIVE_BEFORE_MIGRATION** |
| 8 | OpenByDefault | `various_replace_defaultopen_with_openbydefault` | PRODUCTION_REACHABLE MAIN | `ApplicationsDetailsActivity.initView` / `onClick` | `isFirstPackage` + MAIN | 无 | `defaultViewId` 为 hook 函数局部 var，进程闭包 int，不是 Activity | ModuleHelper；resource id 0 则 onClick 不拦截 | ModuleHelper | 点击：id 比较 + startActivity | 低 | 无 | **KEEP_LEGACY_SAFE** |
| 9 | SkipSecurityScan | `various_skip_securityscan` | PRODUCTION_REACHABLE MAIN | `ModelFactory.produce*GroupModel`；`ScoreTextView.setScore`；`ContentResolver.call`；`MainContentFrame.onClick` | `isFirstPackage` + MAIN | 无 | 无 | ModuleHelper fail-open | ModuleHelper。`ContentResolver.call` 为 framework `Class` | `call`：字符串比较。`setScore`：改参数。`onClick` DO_NOTHING | 中：该进程内所有 `ContentResolver.call` | 无。热路径过滤廉价，catalog 不消除 | **KEEP_LEGACY_SAFE** |
| 10 | ShowBatteryTemp | `various_show_battery_temperature` | PRODUCTION_REACHABLE MAIN | throwing `findClass(BatteryFragment)` + 内部 WeakReference Handler `handleMessage` | `isFirstPackage` + MAIN | 无 | 使用 ROM 已有 WeakReference，未新增静态 View | **裸 `findClass` fail-closed** | **B2B-D1** | `handleMessage` what==1：sticky `BATTERY_CHANGED` query（`registerReceiver(null, ...)`，不保留 receiver）+ findViewById | 高：安装期 abort | 无 | **CORRECTIVE_BEFORE_MIGRATION** |
| 11 | UnlockClipboardLocation | `various_enable_sc_ai_clipboard_location` | PRODUCTION_REACHABLE MAIN | `PrivacyLabActivity.onCreateFragment`；`findClassIfExists("com.miui.permcenter.utils.h")` | `isFirstPackage` + MAIN | 无 | 改静态 map 条目，不持有 Activity | ModuleHelper + `findClassIfExists` | catch 仅直接类型 fatal | `onCreateFragment`：map put 两个 key | 低 | 无 | **KEEP_LEGACY_SAFE** |
| 12 | SideBarSuggestBlacklist | `various_disable_freeform_suggest_blacklist` | PRODUCTION_REACHABLE MAIN | `DisableFloatingWindowBlacklistHook(lpparam.classLoader)`：`MiuiMultiWindowAdapter` / `MiuiMultiWindowUtils` | `isFirstPackage` + MAIN | 无 | 无 | `hookAllMethodsSilently` 部分 fail-open | ModuleHelper | 黑名单 getter after：clear + 留 camera | 低 | 无。与 catalog `noFloatingWindowBlacklist`（`system_fw_noblacklist`）**不同 pref** | **KEEP_LEGACY_SAFE** |
| 13 | DisableDockSuggest | `various_disable_dock_suggest` | PRODUCTION_REACHABLE MAIN | `MiuiMultiWindowUtils.getFreeformSuggestionList` Silently | `isFirstPackage` + MAIN | 无 | 无 | Silently fail-open | ModuleHelper | **每次** getter：`Thread.getStackTrace()` 扫描。有成本，但是否构成设备问题：未宣称 | 中（分配） | 无。不为微优化迁 catalog | **KEEP_LEGACY_SAFE** |
| 14 | SideBarExpandReceiver | `various_enable_expand_sidebar`（另读 `various_swipe_expand_sidebar`） | MAIN 可达；`RegionSamplingHelper` 是否在 SC loader：**INSUFFICIENT** | `RegionSamplingHelper` 构造 / `onViewDetachedFromWindow` / Rect 方法；嵌套 OnTouchListener / background.draw | package-ready 一次；`isHooked[0/1]` | 构造：receiver 一次直至 detach。`isHooked[1]` 永不复位 → 触摸/draw hook 进程级一次 | VIEW 作用域 receiver；detach 反注册。未用 `registerOwnedReceiver` | `findClassIfExists` 缺类则 return（密度替换可能已做） | 延迟 Runnable catch 仅直接类型 fatal | `onReceive`：合成 MotionEvent。非热路径洪泛 | 缺类则 expand 半边死；第二实例可能被 `isHooked[0]` 跳过 | 无 | **KEEP_LEGACY_SAFE** |
| 15 | NoLowBatteryWarning | `system_hidelowbatwarn` | PRODUCTION_REACHABLE MAIN | `Settings.System.getInt`；`Settings.Global.getString`（framework `Class`） | `isFirstPackage` + MAIN | 无 | 无对象保留 | 方法存在于 framework | ModuleHelper `hookAllMethods` | 该进程每次 getInt/getString：key 字符串比较 | 低 | 无 catalog 对应 | **KEEP_LEGACY_SAFE** |
| 16 | PrivacyAppsLayout | `various_privacyapps_column_nums4` | PRODUCTION_REACHABLE MAIN。pref XML 在 launcher 页 | `PrivacyAppsActivity.onCreate` | `isFirstPackage` + MAIN | 无 | 无静态 View。改该 Activity 的 GridView | ModuleHelper | ModuleHelper | `onCreate`：findViewById + numColumns | 低 | 无 | **KEEP_LEGACY_SAFE** |

### 5.2 逐路径补证（只写总表放不下的证明）

#### 1 — AppInfoHook

- `findClassIfExists("com.miui.appmanager.AMAppInfomationActivity")` 缺类 return。ROM 类名拼写 `Infomation` 保持原样。
- 若 `androidx.fragment.app.Fragment` **或** `android.app.Fragment` 任一存在，则只 hook **androidx** 构造字符串。仅 platform Fragment 的 ROM：构造 hook 走 ModuleHelper fail-open，**不会** hook `android.app.Fragment`。`COMPATIBILITY_GAP`，非 CONFIRMED（无 ROM）。
- `mSupportFragment` 赋值：任意 androidx Fragment 构造，且 runtime class 有 `PackageInfo` 字段。替换：下一个此类 Fragment。清除：**无**。可跨过 Activity/Fragment 销毁。功能成功路径需要该字段，因此工作路径上会赋值。分类：强持有 Fragment = **LIKELY_DEFECT**（无 destroy 证据链上的 DEVICE 证明，不升 CONFIRMED）。`mLastPackageInfo` 是 `PackageInfo` 数据，不是 Activity。
- 嵌套 `hookAllMethods(frag.javaClass, "onPreferenceTreeClick")` 在 **每次** `onCreate` after 的 handler post 内。class-level。用户多次打开应用信息 → 多条 click interceptor。可观察：clipboard / `startActivity` 触发多次。`addPref` 针对当前 fragment 实例；新 Activity 通常新 fragment，重复行 **INSUFFICIENT**；重复 class-level 回调 **CONFIRMED**（B2B-D3）。
- 匿名 `MethodHook` 捕获 `act`。class-level hook 在第一次 Activity 销毁后仍可能调用 `act`。这是嵌套 hook 的额外生命周期风险，并入 B2B-D3，不单列 CONFIRMED 泄漏。

#### 2 — AppsDisableHook

- system_server 半边：`FeatureCatalog` `appsDisableService`，`ProcessScope.SYSTEM_SERVER`，同一 pref `various_disableapp`，`Various.AppsDisableServiceHook`。**SAME_SEMANTIC_DIFFERENT_PROCESS_HALF**。禁止合成 FeatureId。
- `onCreateOptionsMenu` after 每次向传入的 Menu add `id=666`。Android 会重建 Menu。不是嵌套 `hookAllMethods`。

#### 3 — AppsRestrictHook

- `XposedHelpers.findClass("com.miui.appmanager.AppManageUtils", lpparam.classLoader)` 无 try。`ClassNotFoundError` 离开 `install()`。后续 scramble / sort / intercept / scan / battery / sidebar / lowbat / privacy **全部不再安装**。与 B2A-D1 同类，且 blast radius 更大。
- PowerKeeper：`PowerKeeperInstaller` 同 pref 调 `AppsRestrictPowerHook`。B1 已记录。**SAME_SEMANTIC_DIFFERENT_PROCESS_HALF**。本轮不打开 PowerKeeper。

#### 5/6 — AppsDefaultSortHook

见第 4 节。`onCreate` before 扫描 Activity 字段找 Fragment 类型，再 `hookAllMethods(fragCls, onActivityCreated)`。每次 App 管理 `onCreate` 执行该扫描 + 再注册。

#### 7 — InterceptPermHook

- throwing `findClass("com.miui.permcenter.privacymanager.InterceptBaseFragment")`。缺类 abort installer 剩余路径（open-by-default 及之后）。
- 找到内部 Handler 子类后 hook 构造（2 参则 delay=0）及 `findMethodsByExactParameters(..., Void, int)`。无内部类则安静 return（在 findClass 成功之后）。

#### 10 — ShowTempInBatteryHook

- throwing `findClass("com.miui.powercenter.BatteryFragment")`。缺类 abort clipboard 及之后。
- 内部类匹配 `WeakReference` 可赋值，字段名同样。找不到则 `return`（findClass 已成功）。
- `registerReceiver(null, BATTERY_CHANGED)` 是 sticky 查询，不是注册长期 receiver。

#### 12 — DisableSideBarSuggestionHook

- 与 catalog `noFloatingWindowBlacklist` **共享** `DisableFloatingWindowBlacklistHook` helper。
- Catalog：pref `system_fw_noblacklist`，`SYSTEM_SERVER`，另 hook `MiuiFreeformServicesUtils.supportsFreeform` + 资源替换。
- SC：pref `various_disable_freeform_suggest_blacklist`，无 server 那两步。
- **SAME_SEMANTIC_DIFFERENT_PROCESS_HALF**（相关产品意图，不同 pref / 不同进程 / 不同额外 target）。不得合并 FeatureId。

#### 14 — AddSideBarExpandReceiverHook

- Installer 门：`various_enable_expand_sidebar`。
- `various_swipe_expand_sidebar` 控制是否保留侧滑（false 则密度压成 8dp 并 hook 触摸/背景）。XML dependency 正确。不是第二 installer 路径。
- `RegionSamplingHelper` 全名是 SystemUI navigationbar 类，用 **SC ClassLoader** 查找。MIUI14 SC 是否打包该类：**INSUFFICIENT_EVIDENCE**。缺类：log + return。若此前已做密度替换，隐藏条可能仍在、广播展开死。**COMPATIBILITY_GAP**，非自动缺陷。
- Receiver：`view.context.registerReceiver(..., RECEIVER_EXPORTED)`，**不是** `ModuleHelper.registerOwnedReceiver` / `registerModuleReceiver`。`onViewDetachedFromWindow` 后 `unregisterReceiver` 并 `isHooked[0]=false`。有替换/释放闭环。View context 可能是 Activity：detach 路径存在则 **不是** 故意的进程级永久 receiver。
- 现有 owned-receiver helper 使用 `applicationContext`，会把生命周期拉长到进程。**不要**为了「用 helper」把 VIEW 作用域改成 APPLICATION 作用域。未用 helper = **ARCHITECTURE_DEBT**，不是 CONFIRMED 泄漏。
- 两实例重叠：`isHooked[0]` 使第二构造跳过注册。ROM 是否同时存在两个 helper：**INSUFFICIENT_EVIDENCE**。
- 延迟 150ms `Handler` Runnable：detach 时 `removeCallbacks`。`isHooked[1]` 不复位，嵌套 touch/draw hook 进程内一次。

#### 15 — NoLowBatteryWarningHook

- 无 `lpparam`。`Settings.System::class.java` / `Settings.Global::class.java` 为 boot classpath。仅 SC MAIN 安装，但 hook 的是该进程内所有调用。
- 无 system_server catalog 半边。

#### 16 — PrivacyAppsLayoutHook

- Pref 放在 `prefs_launcher.xml`，hook 在 SecurityCenter。UI 分组 ≠ 安装进程。不是 LauncherInstaller 路径。

---

## 6. 静态保留 / 生命周期

| 对象 | 赋值 | 替换 | 清除 | 进程寿命 | 销毁后仍持有？ | 是否必须 PROCESS_SINGLETON | 强弱 | 与重复安装 |
|---|---|---|---|---|---|---|---|---|
| `Various.mSupportFragment` | AppInfo androidx Fragment 构造，且有 PackageInfo 字段 | 下一个此类 Fragment | **无** | SC MAIN | 可以 | 仅作 content fragment 找不到时的 fallback | 强 | 双装 AppInfo 会双构造 hook |
| `Various.mLastPackageInfo` | AppInfo `onCreate` post | 下一次成功读取 | 无 | SC MAIN | 是（数据对象） | 点击回调读它 | 强，但是 `PackageInfo` | 过期 PI 可能导致错误包名操作 |
| `OpenByDefaultHook.defaultViewId` | 首次 `initView` | 不重置（-1 守卫） | 无（int） | 闭包 | n/a | 资源 id 缓存 | n/a | 双装仍是同一函数调用两次会有两个闭包；当前 installer 只调一次 |
| `AddSideBarExpandReceiverHook` receiver | 构造 after | detach 后下一构造 | detach unregister | 绑定 View | detach 后否 | 否；VIEW_SCOPED | 强，但有反注册 | `isHooked[0]` |
| `originDockLocation` | 构造 after 读 SP | 广播更新 SP | 无（int 于 hook 实例） | 该 MethodHook | n/a | 否 | n/a | 一次安装一个 hook 实例 |

生命周期 owner 标签：

| 对象 | 分类 |
|---|---|
| `mSupportFragment` | **PROCESS_SINGLETON**（强 Fragment）。LIKELY_DEFECT，非 CONFIRMED |
| `mLastPackageInfo` | **PROCESS_SINGLETON** 数据。低风险 |
| AppInfo 嵌套 click hook 捕获的 `act` | 随 class-level hook 泄漏 Activity 引用风险。并入 B2B-D3 |
| SideBar receiver | **VIEW_SCOPED** |
| 延迟 Runnable / Handler | 绑定 helper 实例；detach 取消 |
| PIN shuffle / GridView / Menu / Dialog | **ACTIVITY_SCOPED** 或瞬时 |

---

## 7. Receiver / Observer / 其它注册

本 installer 可达范围内：

| 路径 | 类型 | 触发 | Context | 重复 | 释放 | 进程寿命 | 双装 |
|---|---|---|---|---|---|---|---|
| AddSideBarExpandReceiverHook | `BroadcastReceiver` | `RegionSamplingHelper` 构造 after | `view.context` | `isHooked[0]` | `onViewDetachedFromWindow` unregister | 随 View | installer 不会双调；两 View 见上 |
| ShowTempInBatteryHook | sticky query | `handleMessage` | Activity | 每次 what==1 | 立即，receiver=null | 无保留 | n/a |
| 其它 14 条 | 无 ContentObserver / executor / coroutine | | | | | | |

`ModuleHelper.registerOwnedReceiver` / `registerModuleReceiver` 存在，本路径未使用。侧栏已有 View 级 unregister。**不**提议为 B2B 生产项引入新 registry。

`NoLowBatteryWarningHook` / `SkipSecurityScanHook` 的 framework 方法 hook 不是 receiver。

---

## 8. UI hook 所有权与嵌套注册

| 路径 | package-ready vs 动态 | 每 Activity 重复注册？ | 静态 per-Activity 数据 | Dialog/View 释放 | 重入重复 UI |
|---|---|---|---|---|---|
| AppInfo | ready：activity onCreate + Fragment 构造。动态：onCreate 内 onPreferenceTreeClick | **是，CONFIRMED** | `mSupportFragment` / `mLastPackageInfo` | 无模块 Dialog | click 多次；pref 行重复 INSUFFICIENT |
| AppsDisable | 仅 ready | 否 | 无 | AlertDialog 由用户关闭 | 新 Menu 一个 666 |
| PrivacyApps | 仅 ready | 否 | 无 | 改现有 GridView | 幂等 numColumns=4 |
| ShowTemp | 仅 ready（内部类 handleMessage） | 否 | 无 | 改现有 TextView | 幂等 |
| ScramblePIN | 仅 ready 构造 | 否（每新 View  shuffle 一次，正确） | 无 | 该 View 树 | 双装会 shuffle 两次，仍随机 |
| OpenByDefault | 仅 ready | 否 | `defaultViewId` | 无 | 无 |
| InterceptPerm | 仅 ready | 否 | 无 | 无 | 无 |
| SkipScan | 仅 ready | 否 | 无 | 无 | 无 |

不是所有嵌套 hook 都是缺陷。侧栏 `isHooked[1]` 已证明一次。AppInfo / AppsDefaultSort **没有**守卫且 class-level 累积 → CONFIRMED。

---

## 9. Failure / fatal 边界

直接可达的问题形态：

| 形态 | 位置 | 分类 |
|---|---|---|
| 裸 throwing `findClass` @ package-ready | `AppsRestrictHook` `AppManageUtils`；`InterceptPermHook` `InterceptBaseFragment`；`ShowTempInBatteryHook` `BatteryFragment` | **CONFIRMED_DEFECT** B2B-D1 |
| OOM-only catch | 这 16 条内未发现 NoOverscroll 那种四段 OOM-only | 无 |
| catch 只判断直接 OOM/TD/VME | AppInfo、AppsDefaultSort 嵌套、setAppState、UnlockClipboard map、侧栏 Runnable | **ARCHITECTURE_DEBT**（非 B2A 共享原语） |
| ModuleHelper 包装 | 绝大多数 `findAndHookMethod` | 符合 B2A |
| fallback 反射掩盖 fatal | 未发现 B2A-D2 那种 primary/fallback 链 | 无 |

`ClassNotFoundError` 不是 fatal；危险在于 **未捕获时中止后续 feature 安装**。

---

## 10. ClassLoader / ROM ABI

| 目标族 | Loader | MIUI14/A13 | HyperOS1/A13 |
|---|---|---|---|
| `com.miui.appmanager.*` / `permcenter.*` / `securityscan.*` / `privacyapps.*` / `applicationlock.widget.*` / `powercenter.BatteryFragment` | `lpparam.classLoader` | 假定存在于 SC APK；缺失时除 B2B-D1 外 ModuleHelper fail-open | **INSUFFICIENT_EVIDENCE** |
| `com.miui.networkassistant.*` / `FirewallService` | 同上 | 常合并进 SC；是否分进程：**INSUFFICIENT** | 同上 |
| `androidx.fragment.app.Fragment` | SC loader | 取决于 SC 打包 | 同上 |
| `android.util.MiuiMultiWindowAdapter` / `MiuiMultiWindowUtils` | SC loader 解析 framework/miui | Silently 或 hookAll | 同上 |
| `Settings.System` / `Settings.Global` / `ContentResolver` | boot `Class` 字面量 | 存在 | 存在 |
| `com.android.systemui.navigationbar.gestural.RegionSamplingHelper` | **SC loader** | **INSUFFICIENT_EVIDENCE** 是否在 SC DEX | 同上 |
| `com.miui.permcenter.utils.h` | `findClassIfExists` | 短名混淆，缺则 skip | 同上 |

无 ROM 提取。ABI 未知不得写成缺陷。

---

## 11. 热路径

| 阶段 | 允许的成本 | 本 installer |
|---|---|---|
| PACKAGE_READY / 冷 | 反射、找类、findMethods | 全部 16 条的安装期查找 |
| 回调 | 应只读已准备状态 | 见下 |

回调内有意义成本（不建议本轮改）：

- `DisableDockSuggestHook`：每次 `getFreeformSuggestionList` `stackTrace`。KEEP。不为微优化迁 catalog。
- `SkipSecurityScanHook`：该进程每个 `ContentResolver.call` 做字符串比较。KEEP。
- `NoLowBatteryWarningHook`：该进程每个 Settings getInt/getString。KEEP。
- AppInfo / AppsDefaultSort 嵌套重复会把冷反射放大 N 倍 → 由 B2B-D2/D3 修复，不是独立性能项目。

---

## 12. Catalog 交叉半边

| SC 路径 | Catalog / 其它进程 | 关系 |
|---|---|---|
| `various_disableapp` AppsDisableHook | `appsDisableService` system_server，同一 pref | **SAME_SEMANTIC_DIFFERENT_PROCESS_HALF** |
| `various_restrictapp` AppsRestrictHook | PowerKeeper `AppsRestrictPowerHook`，同一 pref，无 catalog | **SAME_SEMANTIC_DIFFERENT_PROCESS_HALF** |
| `various_disable_freeform_suggest_blacklist` | `noFloatingWindowBlacklist` / `system_fw_noblacklist` / system_server | **SAME_SEMANTIC_DIFFERENT_PROCESS_HALF**（不同 pref，共享 helper） |
| `system_applock_scramblepin` | catalog `appLock` / `system_applock` | **UNRELATED_FEATURE** |
| 其余 12 条 | FeatureCatalog 无对应 key | **NO_CATALOG_COUNTERPART** |

无 **POTENTIAL_DUPLICATE**（同一进程双路由）。SC installer 与 catalog 没有平行安装同一 MAIN hook。

---

## 13. Catalog 迁移价值

SecurityCenter MAIN **当前不付** dispatcher 全量注册成本。

逐条核对声称收益：

| 声称收益 | 实际 |
|---|---|
| 重复安装预防 | B2B-D2/D3 是 Activity 嵌套 hook。registry 不能表达。局部 `isHooked` / 合并 if |
| 未解决进程所有权 | MAIN / REMOTE / BOOTAWARE 已证明 |
| resolver/installer target 不匹配 | 无 catalog 路径，无 mismatch |
| 重大兼容选择 | 需要的是 `findClassIfExists`，不是 CONTRACT_REQUIRED |
| 生命周期无法用 legacy 表达 | 侧栏已有 detach 闭环。Fragment 弱引用是局部字段改，不是 FeatureSpec |
| 拆除真实平行路由 | 无 MAIN 平行路由。双半边必须保持两个 id |

**0 条 CATALOG_MIGRATION_VALUE。** 无 FeatureSpec 设计。

---

## 14. 发现分类汇总

### CONFIRMED_DEFECT

1. **B2B-D1** — 三条裸 `findClass` 可中止后续路径。**已落地为 COMPATIBILITY_FAIL_OPEN。**
2. **B2B-D2** — `AppsDefaultSortHook` 嵌套重复安装 + installer 双 if。**已落地**（合并 predicate，保留 `various_skip`；`onActivityCreated` first-success / PACKAGE_READY 一次）。
3. **B2B-D3** — class-level `onPreferenceTreeClick` MethodHook **捕获第一次 Activity** = **CONFIRMED lifecycle ownership defect**。**已落地**（独立 hook 从当前 fragment 取 Activity / PackageInfo；不再用 stale `mLastPackageInfo` 作为 click identity）。
4. **B2B-D4** — 直接类型 OOM/ThreadDeath/VME 检查 **不传播 wrapped fatal** = **CONFIRMED_DEFECT**。**已落地**（`RuntimeFatality.throwIfFatal`）。

### LIKELY_DEFECT

- `mSupportFragment` 强引用 Fragment 且无清除。**本轮未改**（`MSUPPORTFRAGMENT_CHANGED = NO`）。
- 侧栏 `isHooked[0]` 跳过第二 `RegionSamplingHelper` 实例。

### COMPATIBILITY_GAP

- AppInfo 只 hook androidx Fragment 构造。
- `RegionSamplingHelper` 是否在 SC DEX。
- networkassistant 类是否在 MAIN。

### INTENTIONAL_DIVERGENCE

- REMOTE / BOOTAWARE 拒绝（有测试表）。
- 关闭功能不调用 hook。
- `various_swipe_expand_sidebar` 是 expand 的子选项，不是第二 installer 入口。
- 双进程半边保持分离。

### ARCHITECTURE_DEBT

- 侧栏未用 owned-receiver helper（且 helper 会改 Context 作用域，不能直接替换）。
- `FeatureDispatcher` 全量 `registerAll` 阻止 SC 成为廉价 catalog 宿主。
- `Various` 对象字段 `@SuppressLint("StaticFieldLeak")`。

### MIGRATION_RESIDUE

- `various_skip` → `AppsDefaultSortHook`：无 UI，hook 不读该 key。
- Pref XML 把 privacy columns 放在 launcher 页（安装仍在 SC）。

### INSUFFICIENT_EVIDENCE

- libxposed 对重复 intercept 的运行时表现（静态已证明本模块会注册两条）。
- HyperOS1 类是否存在。
- `FirewallService` 是否只在 `:remote`。
- AppInfo `addPref` 是否在同一 fragment 实例上重复出 UI 行。
- Gradle dependency-verification 干净门（见下）。

---

## 15. B2B_PRODUCTION_CANDIDATES

`CATALOG_MIGRATION_CANDIDATES = 0`。

D1–D4 已在授权范围内落地。`mSupportFragment` 仍不是生产候选。

### B2B-D1 — COMPATIBILITY_FAIL_OPEN（已落地）

- `findClassIfExists`；`AppsRestrictHook` 缺 `AppManageUtils` **不** whole-function return。

### B2B-D2 — LOCAL_CORRECTIVE_ONLY（已落地）

- 合并 predicate，**保留 `various_skip`**。
- PACKAGE_READY 解析 fragment field；否则 first-success，未找到不得永久成功。

### B2B-D3 — LOCAL_CORRECTIVE_ONLY（已落地）

- class-level click hook 一次；callback 不 capture Activity。
- 未改 `mSupportFragment`。

### B2B-D4 — FATAL_BOUNDARY_CORRECTIVE（已落地）

- 仅 16 条路径直接可达 catch 改为 `RuntimeFatality.throwIfFatal`。

### 非候选

- Catalog 迁移：0。
- 侧栏改 owned-receiver：会改变 Context 作用域，禁止无证据改动。
- Dock stackTrace：非缺陷。
- 双半边合并 FeatureId：禁止。

### PROPOSED_MINIMAL_DESIGN

不适用。无 catalog 迁移。

---

## 16. 依赖验证

```text
DEPENDENCY_VERIFICATION_CLEAN_GATE = NOT_PROVEN
```

B2A 曾在 `--dependency-verification=off` 预热后出现 fast gate 绿。B2B **不得**修改 `gradle/verification-metadata.xml`。本任务 docs-only，未跑 Android 编译。

---

## 17. 关闭状态

```text
PRODUCTION_CHANGED = NO
A1/A2/A3/B1/B2A = CLOSED
B3_STARTED = NO
B2B_STATIC_RESULT = SELECTION_DOCUMENTED
B2B_PRODUCTION_AUTHORIZATION = NO
```

未 merge / rebase / force-push / tag / release / signing。
