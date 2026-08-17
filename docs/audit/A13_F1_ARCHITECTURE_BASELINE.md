# A13 F1 架构基线（Architecture Baseline / A14 结构 parity 审计）

本文件是 **证据文档**，不是控制文档。控制权见 `AGENTS.md`、`ARCHITECTURE.md`、`COMPATIBILITY.md`、`docs/A13_PARITY.md`。

```text
A13 base SHA:      08d61565d9bfbc5e62e23a4058c7cbdc27c4a54e
A14 reference SHA: d20d96b543a49a584970e312da7d704958a155aa
PRODUCTION_CHANGED: false
```

本批不修改 `app/src/main/**`。所有结论为 `STATIC_VERIFIED`，无 `DEVICE_VERIFIED`。

---

## 1. 实际运行时管线

以下管线由源码重建，不引用 `ARCHITECTURE.md`。

### 1.1 onModuleLoaded

```text
MainModule.onModuleLoaded(ModuleLoadedParam)     MainModule.java:53-58
  → processName = param.getProcessName()
  → XposedHelpers.moduleInst = this
  → XposedHelpers.log(version/process)
```

无进程分类，无 preference 读取，无 Hook。

### 1.2 onSystemServerStarting

```text
MainModule.onSystemServerStarting(...)           MainModule.java:80-86
  → isSupportedAndroidVersion()  (SDK_INT == TIRAMISU)   MainModule.java:60-66
  → initPrefs()                  → PreferenceBootstrap.start()
  → SystemServerInstaller.install(lpparam)
  → watchPreferenceChange()      → PreferenceBootstrap.ensureWatcher()
```

**注意**：system_server 路径**不经过** `ProcessScopes.resolve`。它是独立入口，`ProcessScope.SYSTEM_SERVER` 在此路径中从未被计算。

### 1.3 onPackageReady（通用骨架）

```text
MainModule.onPackageReady(PackageReadyParam)     MainModule.java:88-175
  → isSupportedAndroidVersion()
  → if (!lpparam.isFirstPackage()) return          ← 全局 install-once 的实际来源
  → scope = ProcessScopes.resolve(pkg, processName)
  → if (ProcessScopes.isRejected(pkg, processName)) return   ← 第二次 resolve()
  → remote = getRemotePrefs()      → PreferenceBootstrap.resolveRemote()
  → if (remote == null || !PreferenceLoadRegistry.shouldLoad(remote, pkg)) return
  → initPrefs()                    → PreferenceBootstrap.start()
  → if-chain on scope → Installer
```

`ProcessScopes.resolve` 在同一次回调中被计算两次（`resolve` 一次、`isRejected` 内部一次）。纯函数，无正确性影响。

### 1.4 Launcher package-ready 路径

```text
scope == LAUNCHER                                MainModule.java:162-174
  → LauncherInstaller.hasAnyLauncherPackageReadyFeature(mPrefs)
      → LauncherInstaller.installPackageReady(lpparam)      LauncherInstaller.java:30-45
          → 直接 Hook / Res 修改（9 个 legacy 分支，无 catalog）
  → LauncherInstaller.hasAnyLauncherApplicationFeature(mPrefs)
      → LauncherInstaller.installApplication(lpparam)       LauncherInstaller.java:104-111
  → hasAnyLauncherApplicationFeature(mPrefs) 再次求值
      → watchPreferenceChange()
```

### 1.5 Launcher Application.attach 路径

```text
ModuleHelper.findAndHookMethod(Application.class, "attach", Context.class)   LauncherInstaller.java:105
  → after(param)
      → LauncherInstaller.handleLoadLauncher(lpparam)       LauncherInstaller.java:47-102
          → FeatureDispatcher.createRuntime(packageName, ...)   :49
          → 混合：8 × FeatureDispatcher.installById(...)
                  + 约 40 个直接 Hook 调用
```

**此 after 回调没有任何幂等保护。** 参见 P1-1。

### 1.6 SystemUI 路径

```text
scope == SYSTEM_UI                               MainModule.java:153-158
  → SystemUiInstaller.hasAnySystemUiStartupFeature(mPrefs)
  → SystemUiInstaller.install(lpparam, watchPreferenceChange)  SystemUiInstaller.java:47
      → pkg 再次校验 ("com.android.systemui")                 :49
      → 2 个 Res 修改                                          :51-52
      → ModuleHelper.findContext(lpparam)                      :53
      → 读取 Settings.System "systemui_restart_time"           :54
      → Hook SystemUIApplication.onCreate                      :60-71
            after: if (!isHooked) { isHooked = true;
                     setupStatusBar(context); watchPreferences.run(); }
      → GlobalActions.setupStatusBar(lpparam)                  :72
      → if (isWithinSystemUiRestartGuard(restartTime, now)) return;   :74-76
      → FeatureDispatcher.createRuntime(pkg, ...)              :78
      → 混合：约 15 × installById + 大量直接 Hook              :80-330
```

`isWithinSystemUiRestartGuard` = `now - restartTime < 10000`（SystemUiInstaller.java:492-494）。
命中时**大部分 SystemUI 功能在本次进程生命周期内完全不安装**，且没有后续重试路径。

### 1.7 通用应用路径

```text
scope == GENERIC_APP                             MainModule.java:143-146
  → GenericAppInstaller.install(lpparam, pkg)    GenericAppInstaller.java:23-53
      → 3 组 preference + 应用选择集判定
      → 命中时 Hook Application.attach → after: 3 个直接 Hook
```

同样**无幂等保护**。

### 1.8 system_server 路径

```text
SystemServerInstaller.install(lpparam)           SystemServerInstaller.java:27
  → FeatureDispatcher.createRuntime("android", lpparam, ...)   :28  ← 字面量 "android"
  → 混合：约 40 × installById + 约 8 个直接 Hook
  → needGlobalActions() 遍历 mPrefs.entrySet()                 :102-124
```

### 1.9 层使用状态

| 层 | 状态 |
|---|---|
| `MainModule` if-chain 路由 | USED_IN_PRODUCTION |
| `PreferenceBootstrap` | USED_IN_PRODUCTION |
| `PrefMap` | USED_IN_PRODUCTION |
| `PreferenceLoadRegistry.shouldLoad` | USED_IN_PRODUCTION |
| `ProcessScopes.resolve` / `isRejected` / `isKnownPackage` | USED_IN_PRODUCTION |
| `ProcessScopes.shouldLoadPrefs` / `shouldHook` | **DEAD/UNREFERENCED** |
| `FeatureCatalog` / `FeatureDispatcher` / `FeatureInstallRegistry` | PARTIALLY_USED |
| `HookTargetContract` / `HookTargetResolver` / `HookInstaller` | PARTIALLY_USED（仅 catalog spec 路径） |
| `mods/utils/FeatureState`、`InstallPhase`、`FeatureInstallResult` | USED_IN_PRODUCTION |
| `mods/utils/FeatureInstallState` | **DEAD/UNREFERENCED** |
| `mods/utils/FeatureId` | **DEAD/UNREFERENCED** |
| `RuntimeFatality` | USED_IN_PRODUCTION |
| `DiagnosticRecorder` | USED_IN_PRODUCTION |

`shouldLoadPrefs` / `shouldHook` 在 `app/src/main`、`app/src/test`、`tools/` 中均无调用者，仅在旧 audit 文档中被提及。
`FeatureInstallState` 与 `mods/utils/FeatureId` 互相引用，无任何外部生产或测试调用者。

---

## 2. PreferenceBootstrap 状态机

实现：`app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceBootstrap.java`

### 2.1 状态

```text
UNINITIALIZED
UNAVAILABLE
SNAPSHOT_PENDING_LISTENER
EMPTY_PENDING          ← 已声明但从未被赋值（无 production 写入点）
LOADED
VALID_EMPTY
```

### 2.2 转移

```text
start()  (:82-152)
  guard: LOADED/VALID_EMPTY → 直接返回（幂等）
  guard: attempts >= 3      → UNINITIALIZED 转 UNAVAILABLE，否则保持当前态
  attempts++
  resolveRemote() == null           → UNAVAILABLE
  first getAll() 抛出/为 null        → UNAVAILABLE
  ensureListenerLocked() 失败        → publish(first) + SNAPSHOT_PENDING_LISTENER
  second getAll() 抛出/为 null       → UNAVAILABLE   ← 注意：此处不回滚 first 快照
  publish(second)
  second 非空 → LOADED；second 为空 → VALID_EMPTY (emptyConfirmations++)

ensureWatcher()  (:158-188)
  watcherRegistered            → 返回当前态
  remote == null               → 递归 start()
  ensureListenerLocked() 失败   → 保持当前态（不降级）
  getAll() 成功 → publish + (LOADED | VALID_EMPTY)

onPreferenceChanged(prefs, key)  (:249-288)
  key == null → return
  synchronized(lock):
    state ∉ {LOADED, VALID_EMPTY, SNAPSHOT_PENDING_LISTENER} → 丢弃更新
    经 getAll() 取原始值（不用 typed getter）
    null → snapshot.remove(key)；否则 snapshot.put(key, value)
    watcherRegistered → state = (size==0 ? VALID_EMPTY : LOADED)
  锁外：key != "pref_key_systemui_restart_time" → ModuleHelper.handlePreferenceChanged(key)
```

### 2.3 关键性质

| 性质 | 结论 | 证据 |
|---|---|---|
| 初始态 | `UNINITIALIZED` | :44 |
| 最大尝试次数 | 3（`MAX_ATTEMPTS`） | :35 |
| 稳定快照定义 | listener 注册成功后的第二次 `getAll()` | :113-141 |
| listener 唯一性 | `watcherRegistered` 布尔守卫，注册失败不置位 | :228-242 |
| 重复 watcher 防护 | 有 | :114, :160, :229 |
| 线程安全 | 全部状态转移在 `synchronized(lock)` 内 | :64, :83, :159, :255 |
| 快照替换原子性 | `PrefMap.replaceSnapshot` → `AtomicReference.set(unmodifiableMap)` | PrefMap.kt:28-31 |
| 快照读原子性 | `AtomicReference.get()` 单次读 | PrefMap.kt:16 |
| 更新与全量替换互斥 | listener 更新持 bootstrap lock，避免与 publish 交错丢键 | :252-255 |
| preference 变化能否重置安装状态 | **不能**。listener 只写 `PrefMap` 与 `state`，不触碰 `FeatureInstallRegistry.states` | :273-282 |
| listener 生命周期 | 进程级；只随进程结束释放，无显式 unregister 路径 | 全文件无 `unregisterOnSharedPreferenceChangeListener` |
| system_server 行为 | 与包进程一致；`initPrefs()` → `start()`，`watchPreferenceChange()` → `ensureWatcher()` | MainModule.java:83-85 |
| 包进程行为 | `resolveRemote()` 先于 `PreferenceLoadRegistry.shouldLoad`，`start()` 在门通过后 | MainModule.java:99-101 |
| 致命错误边界 | **缺失**（见 P0-1） | :69, :102, :129, :175, :237, :268 |

### 2.4 分类

```text
PREFERENCE_BOOTSTRAP = A13_BETTER_KEEP（状态机与快照语义），
                       但含 FOUNDATIONAL_DEFECT（致命错误边界）
```

A13 的双读 + listener-before-publish 语义与 A14 `PreferenceBootstrap.kt` 概念等价，
A13 额外提供 `SNAPSHOT_PENDING_LISTENER` 降级态与 `attempts` 上限。
A14 在同类 `catch` 中通过 `FatalErrors.rethrowIfFatal` 保留致命错误，A13 未做。

---

## 3. 进程路由

### 3.1 路由矩阵

| 包 / 进程 | scope | isInstallable | prefs 加载 | 进入 installer | Hook |
|---|---|---|---|---|---|
| `android`（主） | `ANDROID_PACKAGE` | 是 | 是（known） | `AndroidPackageInstaller` | 是 |
| `android`（非主） | `SYSTEM_SERVER` | 是 | 是（known） | **无分支** | 否 |
| `system_server`（真实入口） | 不经 resolve | — | 是 | `SystemServerInstaller` | 是 |
| `com.android.systemui`（主） | `SYSTEM_UI` | 是 | 是 | `SystemUiInstaller` | 是 |
| SystemUI 次要/插件进程 | `SYSTEM_UI_PLUGIN` | 否 | 否 | 否 | 否 |
| `com.miui.home` | `LAUNCHER` | 是 | 是 | `LauncherInstaller` | 是 |
| `com.mi.android.globallauncher` | `LAUNCHER` | 是 | 是 | `LauncherInstaller` | 是（部分分支限 `com.miui.home`） |
| `com.android.settings`（主） | `SETTINGS_MAIN` | 是 | 是 | `SettingsInstaller` | 是 |
| `com.android.settings`（非主） | `SETTINGS_REMOTE` | 否 | 否 | 否 | 否 |
| `com.miui.securitycenter`（主） | `SECURITY_CENTER_MAIN` | 是 | 是 | `SecurityCenterInstaller` | 是 |
| `com.miui.securitycenter`（非主） | `SECURITY_CENTER_REMOTE` | 否 | 否 | 否 | 否 |
| `com.miui.securitycenter`（`*.bootaware` / `*:bootaware`） | `SECURITY_CENTER_BOOTAWARE` | 否 | 否 | 否 | 否 |
| `com.miui.powerkeeper` | `POWER_KEEPER` | 是 | 是 | `PowerKeeperInstaller` | 是 |
| `com.miui.miwallpaper` | `WALLPAPER` | 是 | 是 | `WallpaperInstaller` | 是 |
| `com.miui.screenshot`, `com.miui.gallery` | `MEDIA` | 是 | 是 | `MediaInstaller` | 是 |
| `com.android.incallui` | `PHONE` | 是 | 是 | `PhoneInstaller` | 是 |
| `com.miui.packageinstaller` | `PACKAGE_INSTALLER` | 是 | 是 | `PackageInstallerRouter` | 是 |
| 输入法（9 个精确 + 3 个前缀） | `INPUT_METHOD` | 是 | 是（known） | `InputMethodInstaller` | 是 |
| `com.android.networkstack*` | `NETWORK_STACK` | 是 | **否**（非 known 且无规则命中） | 否 | 否 |
| `com.android.location.fused` | `UNSUPPORTED` | 否 | 否 | 否 | 否 |
| 其它应用 | `GENERIC_APP` | 是 | 仅当 4 条 `PreferenceLoadRegistry` 规则之一命中 | `GenericAppInstaller` | 条件 |

### 3.2 一致性缺口

**ROUTING_SINGLE_SOURCE_OF_TRUTH = PARTIAL**

1. `ANDROID_PACKAGE` 与 `SYSTEM_SERVER` 语义分裂：`resolve()` 把 `android` 非主进程判为 `SYSTEM_SERVER`（ProcessScope.kt:90），但 `MainModule.onPackageReady` 对该 scope **没有任何分支**（MainModule.java:103-174），真正的 system_server 走独立入口。`ProcessScope.SYSTEM_SERVER` 在 `onPackageReady` 中是死分支。
2. `NETWORK_STACK` 是 `isInstallable`，但既非 `isKnownPackage`，也没有对应 installer 分支，实际永远不会加载。scope 存在但无消费者。
3. `ProcessScopes.shouldLoadPrefs` / `shouldHook`（ProcessScope.kt:128-134）是**完全无调用者**的第二套 preference 门设计；真正生效的是 `PreferenceLoadRegistry.shouldLoad`（内部只复用 `isKnownPackage`）。
4. installer 内部存在冗余包名再校验，例如 `SystemUiInstaller.java:49`、`LauncherInstaller.java:74`。这些不是缺陷，但说明 scope 不是唯一权威。
5. `PreferenceLoadRegistry` 的 4 条规则用**原始 `SharedPreferences`**（`pref_key_` 前缀），installer 用 `PrefMap`（自动补前缀）。两套键命名约定并存。

---

## 4. Installer 拓扑

`INSTALLER_COUNT = 13`，全部可从 `MainModule.java` 到达，无死 installer，无自建 preference 源（全部读 `MainModule.mPrefs`）。

| Installer | scope | 入口相位 | catalog | 直接 Hook | install-once | 失败行为 | 分类 |
|---|---|---|---|---|---|---|---|
| `AndroidPackageInstaller` | ANDROID_PACKAGE | package-ready | 是 | 是 | isFirstPackage | 静默跳过 | HYBRID |
| `LauncherInstaller` | LAUNCHER | package-ready + `Application.attach` | 是 | 是 | isFirstPackage（attach 无守卫） | 静默跳过 | HYBRID |
| `SystemUiInstaller` | SYSTEM_UI | package-ready + `SystemUIApplication.onCreate` | 是 | 是 | isFirstPackage + `isHooked`（仅 onCreate 体） | 静默跳过 | HYBRID |
| `SystemServerInstaller` | SYSTEM_SERVER | system-server-starting | 是 | 是 | 每次启动一次 | `rethrowIfFatal` + log | HYBRID |
| `GenericAppInstaller` | GENERIC_APP | `Application.attach` | 否 | 是 | isFirstPackage（attach 无守卫） | 静默跳过 | LEGACY_DIRECT |
| `InputMethodInstaller` | INPUT_METHOD | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |
| `MediaInstaller` | MEDIA | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |
| `PackageInstallerRouter` | PACKAGE_INSTALLER | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |
| `PhoneInstaller` | PHONE | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |
| `PowerKeeperInstaller` | POWER_KEEPER | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |
| `SecurityCenterInstaller` | SECURITY_CENTER_MAIN | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |
| `SettingsInstaller` | SETTINGS_MAIN | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |
| `WallpaperInstaller` | WALLPAPER | package-ready | 否 | 是 | isFirstPackage | 静默跳过 | LEGACY_DIRECT |

```text
CATALOG_NATIVE:   0
HYBRID:           4   AndroidPackage, Launcher, SystemUi, SystemServer
LEGACY_DIRECT:    9   GenericApp, InputMethod, Media, PackageInstallerRouter,
                      Phone, PowerKeeper, SecurityCenter, Settings, Wallpaper
PLATFORM_SPECIAL: 0
```

没有任何 installer 完全通过 catalog 安装。

---

## 5. Feature catalog 架构

### 5.1 身份

- 权威身份 = `mods/catalog/FeatureId`（enum，79 个值）+ `FeatureSpec.id`（canonical 字符串）。
- `FeatureIdentity.normalizeLookupId` 提供大小写 / 标点归一化与别名解析；canonical、normalized、alias 三类冲突均显式抛出（FeatureInstallRegistry.kt:60-104）。
- `mods/utils/FeatureId`（interface，整数 id）与 `mods/utils/FeatureInstallState` 是 A14 风格的**休眠副本**，无任何调用者。

### 5.2 preference → feature

映射分散在每个 `FeatureSpec` 上：`preferenceKeys: Set<String>` 声明所有权，`condition: (PrefMap) -> Boolean` 决定是否安装（FeatureSpec.kt:55-56）。不存在集中式反向索引，因此无法由 preference key 反查 feature，也无法静态证明 preference 全覆盖。

### 5.3 spec → 进程

`FeatureSpec` 同时携带两个进程维度：
- `processScope: ProcessScope?`（编译期分类，可空）
- `processTarget: ProcessTarget`（运行期匹配：`SystemServer` / `SystemUI` / `Launcher` / `Package(name)` / `Any`）

`FeatureInstallRegistry` 先比 `processScope`（:166-175），再比 `processTarget.matches(runtime.processName)`（:177-185）。

### 5.4 catalog 规模与权威性

```text
FeatureSpec 总数:      68
  registrySpecsInternal:  8（canary）
  adaptedSpecsInternal:  60（legacyInstall 包装）
全部 68 个在 FeatureDispatcher 初始化时注册（FeatureDispatcher.kt:24）
```

catalog 是 **ADVISORY**：13 个 installer 中 9 个完全不经过它，4 个 HYBRID 也在 catalog 之外做大量直接 Hook。

```text
FEATURE_CATALOG_STATE = PARALLEL_WITH_LEGACY
```

### 5.5 兼容性

`CompatibilityState` = `COMPATIBLE` / `DEGRADED` / `INCOMPATIBLE`。
唯一生产求值点为 `FeatureInstallRegistry.runCompatibilityAndInstaller` → `spec.compatibilityCheck(runtime)`（:300）。
`INCOMPATIBLE` → 置 `FAILED_TRANSIENT` 并返回 `Incompatible`（:314-317）。
非 catalog 的 legacy 分支**没有任何兼容性求值**，只有隐式的“找不到就跳过”。

---

## 6. FeatureInstallRegistry 状态机

```text
键:  FeatureStateKey(processName: String, canonicalId: String)     :42-45
态:  NOT_INSTALLED → INSTALLING → { INSTALLED | FAILED_TRANSIENT | FAILED_PERMANENT }
并发: ConcurrentHashMap + compute() 原子占位                       :36, :241-258
     注册用 synchronized(registerLock)                            :39, :61
```

| 检查 | 结论 |
|---|---|
| 稳定 Feature ID | 是（`FeatureSpec.id`，含别名冲突拒绝） |
| 进程隔离 | 是（键含 processName） |
| ClassLoader 隔离 | **否**（键不含 ClassLoader 身份） |
| INSTALLING 态 | 是，`compute()` 原子获取，仅一个线程可持有 |
| INSTALLED 终态 | 是，阻止重装 |
| FAILED_PERMANENT 终态 | 是，阻止重试 |
| FAILED_TRANSIENT | 允许重试 |
| 每条出口都释放 INSTALLING | 是（:214-220 统一落终态；致命路径 `states.remove` 后重抛 :303, :323） |
| 致命错误保留 | 是（`isFatal` = OOM / ThreadDeath / VirtualMachineError，:402-403） |
| preference 变更导致重装 | **否**（listener 不触碰 registry） |

**关键实测**：4 处 `createRuntime` 调用全部传入**包名**而非真实进程名 ——
`SystemUiInstaller.java:78`（`pkg`）、`LauncherInstaller.java:49`（`getPackageName()`）、
`AndroidPackageInstaller.java:50`（`pkg`）、`SystemServerInstaller.java:28`（字面量 `"android"`）。
因此 `FeatureStateKey.processName` 在生产中语义上是**包名**。

当前不产生缺陷，原因是 `ProcessScopes` 已经拒绝了所有多进程包的次要进程（SystemUI plugin、Settings remote、SecurityCenter remote/bootaware），使得每个包实际只有一个可安装进程。这是**依赖外部不变量的隐式正确性**，不是键设计本身的保证。

```text
INSTALL_ONCE_GUARANTEE = PARTIAL
```

- catalog 路径（68 个 spec）：由 registry 保证每 (包, featureId) 一次。
- legacy 直接 Hook 路径：仅由 `lpparam.isFirstPackage()` 保证，且 `Application.attach` / `onCreate` 回调层无统一守卫。

```text
PREFERENCE_CHANGE_REINSTALL_RISK = NO
```

---

## 7. Contract / Resolver

```text
FeatureSpec
  → HookTargetContract（variants: List<Variant>，每个 variant 是固定 requirement 列表）
  → HookTargetResolver.evaluateContract(contract, diagnosticId)      :206-230
  → (CompatibilityState, HookInstallResult{selectedVariant})
  → HookInstaller.withSession(resolver, contract, id, cl, result) { ... }   :64-106
  → ModuleHelper.hook*
```

| 检查 | 结论 | 证据 |
|---|---|---|
| 精确描述符优先 | 是（`getDeclaredMethod` / `getDeclaredConstructor`） | HookTargetResolver.kt:61-87, 115-140 |
| 候选有界 | 是（contract 内固定 variant / `AnyOfRequirement.candidates`），无扫描 | HookTargetContract.kt:69-79, 96 |
| ROM 分支位置 | 冷路径 `evaluateContract`，回调内无 ROM 分支 | HookTargetResolver.kt:206-230 |
| 选定目标不可变 | **是**。`withSession` 校验 `selectedVariant` 归属本 contract 且目标数一致，不匹配即抛异常 | HookInstaller.kt:75-94 |
| resolver / installer 可能选不同候选 | **否** | HookInstaller.kt:75-94 |
| 歧义行为 | 声明序 first-match（variant 与 `AnyOf` 候选均是） | HookTargetResolver.kt:212-223, 375-427 |
| 缺失目标 | REQUIRED → `INCOMPATIBLE`（fail-closed）；OPTIONAL → `DEGRADED`（fail-open） | HookTargetResolver.kt:225-229, 491-570 |
| 失败缓存 | 是，`NULL` 哨兵，上限 128 条 | HookTargetResolver.kt:36-38, 455-459 |
| 回调期目标解析 | catalog 路径 NONE；legacy 路径 FOUND（见 P2-4） | — |

```text
RESOLVER_INSTALLER_SINGLE_TARGET = YES（catalog 路径）
```

legacy 直接 Hook 路径不使用 contract/resolver，因此“单一目标”命题在该路径上不适用而非被违反。

---

## 8. ClassLoader 边界

| 缓存 | 键 | ClassLoader 归属 | 判定 |
|---|---|---|---|
| `XposedHelpers.fieldCache` / `noArgMethodCache` | `Class<?>` → 成员名 | 隐含（Class 身份已含定义 loader） | SAFE（正确性） |
| `XposedHelpers.methodCache` / `constructorCache` | `MemberCacheKey(clazz, name, params)` | 同上 | SAFE（正确性） |
| `XposedHelpers.additionalFields` | `WeakInstanceKey` + `ReferenceQueue` | 实例级弱引用 | SAFE |
| `HookTargetResolver.cache` | 字符串，实例按 ClassLoader 构造，上限 128 | 显式 per-ClassLoader | SAFE |
| `ModuleHelper` ActivityThread 静态缓存 | 无键，单例静态字段 | `android.app.ActivityThread` 属 boot classpath，各进程同一 Class | SAFE |
| `ModuleHelper` 观察者 / receiver 表 | 字符串键 | 与 Class 无关 | SAFE |
| `DiagnosticRecorder` | 字符串键，有界 | 与 Class 无关 | SAFE |

```text
CLASSLOADER_SCOPING = SAFE
```

**明确否定一个常见误判**：`Class<?>` 作为缓存键**不会**跨 ClassLoader 串味，因为同名类由不同 loader 定义时是不同的 `Class` 对象，天然是不同的键。

剩余的真实问题只有容量：`XposedHelpers` 的四个反射缓存在条目数上无上限，且以强引用持有 `Class`。在宿主应用进程内，宿主 loader 本身与进程同寿，因此这不是泄漏，只是无界增长（P2-5）。

---

## 9. 失败 / 致命语义

`RuntimeFatality.throwIfFatal`（RuntimeFatality.kt:14-34）是 **CANONICAL**：沿 `cause` 链最多 8 层，遇 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError` 重抛。

| 助手 | 位置 | 语义 | 分类 |
|---|---|---|---|
| `RuntimeFatality.throwIfFatal` | RuntimeFatality.kt:14-34 | OOM / ThreadDeath / VME，深度 8 | **CANONICAL** |
| `ReflectionFatality.rethrowIfFatal` | utils/ReflectionFatality.kt:5-21 | 先解包 `InvocationTargetException` | SPECIALIZED_VALID |
| `FeatureInstallRegistry.isFatal` | catalog/FeatureInstallRegistry.kt:402-403 | 布尔谓词，不抛 | SPECIALIZED_VALID |
| `ModuleHelper.throwIfFatal` | ModuleHelper.java:111-128 | 与 canonical 等价 | DUPLICATE |
| `HookerClassHelper.throwIfFatal` | HookerClassHelper.java:265-282 | 与 canonical 等价 | DUPLICATE |
| `SystemUINotificationHooks.rethrowNotificationFatal` | :38-45 | 与 canonical 等价 | DUPLICATE |
| `SystemUILockScreenHooks.rethrowLockScreenFatal` | :55-65 | 与 canonical 等价 | DUPLICATE |
| `SystemNotificationMoreHooks.rethrowFatal` | :313-323 | VME + ThreadDeath；**OOM 由 VME 覆盖**，语义完整 | DUPLICATE |
| `SystemServerInstaller.rethrowIfFatal` | :126-133 | VME + ThreadDeath，语义完整 | DUPLICATE |
| `HookTargetResolver.findFatalCause` | :466-480 | **仅 `OutOfMemoryError`** | **SEMANTIC_DRIFT** |
| `Helpers.kt` 内联判定（26 处） | utils/Helpers.kt | 三元内联 | LEGACY |

```text
FATAL_VM_ERRORS_PRESERVED = PARTIAL
```

两个实测缺口：

1. `HookTargetResolver.findFatalCause` 只识别 `OutOfMemoryError`，漏掉 `ThreadDeath` 与 `StackOverflowError` / `InternalError` / `UnknownError` 等非 OOM 的 `VirtualMachineError`。该函数服务于 6 个 `catch (t: Throwable)` 站点（:50, :82, :105, :135, :156, :186）。
2. `PreferenceBootstrap.java` 的 6 个 `catch (Throwable)` 站点**完全没有**致命重抛（:69, :102, :129, :175, :237, :268）。

`tools/check-invariants.py` 的 `check_installer_oom_boundary`（:378-406）只扫描 `/customiuizer/installers/`，因此上述两处均不在现有门禁覆盖范围内。

明确否定的误判：`SystemNotificationMoreHooks.rethrowFatal` 并**未**遗漏 OOM——`OutOfMemoryError` 是 `VirtualMachineError` 的子类。

---

## 10. 生命周期 / 所有权

现有可复用原语（均在 `ModuleHelper.java`）：

| 原语 | 位置 | 保证 |
|---|---|---|
| `OwnedPreferenceObserver` | :71-90 | 弱引用 owner + 弱引用 observer |
| `observeOwnedPreferenceChange` | :596-604 | owner 感知的 preference 回调 |
| `dropOwnedObserver` | :614-625 | owner 被回收或显式注销时移除 |
| `ReceiverRegistration` | :680-693 | 状态机 PENDING_REGISTER / ACTIVE / STALE / RELEASED / REGISTER_FAILED |
| `OwnedReceiverRegistration` | :755-762 | 弱引用 owner |
| `OwnedReceiverBucket` | :765-767 | 按 key 分桶降低锁粒度 |
| `registerOwnedReceiver` / `unregisterOwnedReceiver` | :774-848 | owner 绑定的注册/释放闭环 |

覆盖的所有权类别：`PROCESS_SINGLETON`、`APPLICATION_SCOPED`（receiver / preference observer）。
未覆盖：`CLASSLOADER_SCOPED`、`ACTIVITY_SCOPED`、`VIEW_SCOPED`、`CONTROLLER_SCOPED` 无通用原语，由各 feature 自行约定。

```text
OWNERSHIP_PRIMITIVES  = receiver + preference observer（owner 绑定、弱引用、状态机）
FOUNDATION_LIFECYCLE_STATE = A13_VARIANT
```

A14 提供 `OwnedRegistrations<V>`、`WeakIdentityMap<K,V>`、`WeakOwnerReceiver` 三个**类型无关**原语；A13 提供的是**按用途特化**的等价物。两者在 receiver / observer 上语义相当。是否需要通用原语取决于 F4 实际移植的功能，不因 A14 有而必须建。

---

## 11. API 101 / 102 边界

```text
module.prop: minApiVersion=101, targetApiVersion=102
```

强制加载路径（`onModuleLoaded` / `onSystemServerStarting` / `onPackageReady` 及其签名所需类型）只使用
`XposedModule`、`XposedModuleInterface.ModuleLoadedParam` / `SystemServerStartingParam` / `PackageReadyParam`。

API 102 专属类型的出现位置：
- `XposedInterface.Chain`、`XposedInterface.Hooker`（HookerClassHelper.java:40, 174, 218）
- `XposedInterface.HookHandle`、`ExceptionMode.PASSTHROUGH`（XposedHelpers.java:854-864）
- `PRIORITY_DEFAULT` / `PRIORITY_HIGHEST`（HookerClassHelper.java:181, 290, 297）

全部位于 Hook 安装与回调执行路径，不出现在三个入口方法的签名或其初始化依赖中。

```text
API101_REQUIRED_PATH = SAFE
API102_ISOLATION     = SAFE
```

---

## 12. A14 对比矩阵（概念级，A14 @ d20d96b5）

| 组件 | A13 现状 | A14 现状 | 判定 |
|---|---|---|---|
| MainModule 路由 | if-chain on `ProcessScope` | if-chain on `ProcessScope` | ALIGNED |
| PreferenceBootstrap | Java，6 态，含 `SNAPSHOT_PENDING_LISTENER` + attempts 上限 | Kotlin，6 态，`isReady()` 门 | ALIGNED（A13 状态机略强，但致命边界弱） |
| ProcessRouter / ProcessScope | `ProcessScopes` + `when`，18 值 | `ProcessRouter` + `when`，20 值 | ALIGNED |
| preference-load 门 | 独立 `PreferenceLoadRegistry`（4 条规则） | 集成在 `PreferenceBootstrap.isReady()` + `prefReady` 参数传递 | A14_PATTERN_WORTH_BACKPORTING |
| Installer 边界 | 0 catalog-native / 4 HYBRID / 9 LEGACY_DIRECT | 5 catalog-native + 若干 direct | **A14_PATTERN_WORTH_BACKPORTING** |
| Feature 身份 | 字符串 canonical id + enum，含别名冲突拒绝 | interface + 稳定整数 id | A13_BETTER_KEEP（别名冲突检测更强） |
| Feature catalog / spec | 68 spec，ADVISORY | `*Features.all()` 分域列表，installer 侧权威 | A14_PATTERN_WORTH_BACKPORTING |
| FeatureInstallRegistry | `ConcurrentHashMap` + `compute()` 原子占位，键 (包名, id) | `HashMap<Int, FeatureState>` + `synchronized`，进程单例 | A13_BETTER_KEEP（并发模型更强）；键语义需修正 |
| 兼容性策略 | `CompatibilityPolicy` + `CompatibilityState` 三态，contract 驱动 | 仅 SDK 版本判定 | **A13_BETTER_KEEP** |
| 目标 resolver | `HookTargetResolver` 通用 contract/variant/AnyOf，有界+缓存 | 按功能特化（如 `ClockResolver`/`ClockAbi`） | **A13_BETTER_KEEP** |
| HookInstaller | `withSession` 校验 selectedVariant 归属，REQUIRED/OPTIONAL criticality | `HookInstallerFacade`，无 REQUIRED/OPTIONAL 区分 | **A13_BETTER_KEEP** |
| 致命边界 | `RuntimeFatality` canonical + 6 个重复 + 2 处缺口 | `FatalErrors` canonical + `CallbackGuard`，静态门禁强制 | A14_PATTERN_WORTH_BACKPORTING |
| ClassLoader 缓存策略 | 隐式（Class 身份），`HookTargetResolver` 显式 per-loader，条目无上限 | `ReflectionCache` 显式按 loader 分层，MAX_LOADERS=4 / 64 类，LRU | A14_PATTERN_WORTH_BACKPORTING |
| 所有权 / 生命周期原语 | receiver + preference observer 特化原语 | `OwnedRegistrations` / `WeakIdentityMap` / `WeakOwnerReceiver` 通用原语 | A13_VARIANT |
| 诊断 | `DiagnosticRecorder`，有界 LRU + 节流 | `HookDiagnostics`，有界，release 压缩计数 | ALIGNED |
| 重启元数据 | `RestartTarget`（catalog 内） | 无架构级等价物（仅 UI 层） | A13_PLATFORM_SPECIFIC |
| 验证 / 不变式覆盖 | `check-invariants.py` + `check-compat-contracts.py` + `check_hook_contract_parity.py` | `check-invariants.py`，48+ 架构检查（含 `check_no_direct_hook_installation`、`check_guard_framework_callbacks`、`check_api102_isolation`） | **A14_PATTERN_WORTH_BACKPORTING** |

灵动额头 / Dynamic Island 架构不在对比范围内，其专属基础设施不计为 parity 缺口。

---

## 13. 并行架构清单

| 重复机制 | A13 现状 | 分类 |
|---|---|---|
| catalog dispatch vs 直接 Hook | 68 spec 经 `FeatureDispatcher`；9 个 installer 完全绕过 | **ARCHITECTURE_DEBT** |
| `FeatureInstallRegistry` vs `isFirstPackage` / `isHooked` 局部布尔 | 两套 install-once 并存，覆盖面不同 | **ARCHITECTURE_DEBT** |
| `FeatureInstallRegistry`（catalog）vs `FeatureInstallState`（utils） | 后者完全无调用者 | **MIGRATION_RESIDUE** |
| `mods/catalog/FeatureId` vs `mods/utils/FeatureId` | 后者完全无调用者 | **MIGRATION_RESIDUE** |
| `HookTargetResolver` vs 回调内直接反射 | legacy 回调直接 `findClass` | ARCHITECTURE_DEBT |
| `ProcessScopes.resolve` vs `shouldLoadPrefs`/`shouldHook` | 后两者无调用者 | **MIGRATION_RESIDUE** |
| `PreferenceLoadRegistry` vs `ProcessScopes.shouldLoadPrefs` | 两套 preference 门设计，仅前者生效 | MIGRATION_RESIDUE |
| `RuntimeFatality` vs 9 个局部致命助手 | 6 个语义等价重复 + 1 个 drift + 2 个特化 | ARCHITECTURE_DEBT |
| `PreferenceBootstrap` vs installer 直接读 remote prefs | 无第二条路径，全部走 `MainModule.mPrefs` | FEATURE_LOCAL_VALID（无重复） |

---

## 14. findings

### P0_FOUNDATIONAL_CORRECTNESS

**P0-1  `PreferenceBootstrap` 吞掉致命 JVM 错误**
`app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceBootstrap.java:69, 102, 129, 175, 237, 268`
6 个 `catch (Throwable t)` 直接 `recordFailure(...)` 后返回，无 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError` 重抛。
该类位于 **每个被注入进程（含 `system_server`、SystemUI）的强制启动路径**（`MainModule.java:83, 101`）。
`OutOfMemoryError` 在此被记为 `UNAVAILABLE`，模块以“preference 不可用”继续运行，宿主进程得到错误的根因。
现有门禁 `check_installer_oom_boundary`（tools/check-invariants.py:378-406）只扫描 `/customiuizer/installers/`，未覆盖。
最小修正：改用 `RuntimeFatality.throwIfFatal(t)` 作为每个 catch 的首行，并把不变式检查范围从 `installers/` 扩展到启动路径类。

### P1_PARITY_ENABLER

**P1-1  `Application.attach` after 回调无幂等守卫**
`LauncherInstaller.java:104-111`、`GenericAppInstaller.java:40-52`
两处 hook `Application.attach(Context)` 并在 after 中安装全部 legacy Hook，无 `isHooked` 之类守卫。
对照 `SystemUiInstaller.java:60-71` 的 `onCreate` 有守卫，存在不对称。
`attach` 挂在基类 `android.app.Application` 上，任何在该进程实例化的 `Application` 子类都会触发。
Launcher 侧受影响面尤其大：`handleLoadLauncher` 内约 40 个直接 Hook 无 registry 保护。
修正前不应扩大 legacy 直接 Hook 的数量。

**P1-2  install-once 只覆盖 catalog 路径**
catalog 侧 68 个 spec 由 `FeatureInstallRegistry` 保证；其余全部依赖 `MainModule.java:91` 的 `isFirstPackage()` 加各自回调时机。
F4 批量移植时若沿用 legacy 直接 Hook，将继承这一缺口。

**P1-3  `FeatureStateKey.processName` 实为包名**
`FeatureDispatcher.createRuntime` 的 4 个调用点全部传包名（SystemUiInstaller.java:78、LauncherInstaller.java:49、AndroidPackageInstaller.java:50、SystemServerInstaller.java:28）。
当前无缺陷，但正确性依赖 “`ProcessScopes` 已拒绝所有多进程包的次要进程” 这一外部不变量，而键本身不含 ClassLoader 身份。
任何未来放开次要进程的路由改动都会静默破坏 install-once。

**P1-4  `HookTargetResolver` 致命判定漏项**
`HookTargetResolver.kt:466-480` 的 `findFatalCause` 只识别 `OutOfMemoryError`，服务于 6 个 `catch (t: Throwable)`（:50, :82, :105, :135, :156, :186）。
`ThreadDeath` 与非 OOM 的 `VirtualMachineError` 会被吞掉。目标解析属冷路径，影响面小于 P0-1，但同属致命边界缺口。

**P1-5  catalog 为 advisory，无 preference 反查索引**
`FeatureSpec.preferenceKeys` 分散声明，无集中反向索引，无法静态证明 preference 全覆盖，也无法在 F2 用工具自动生成完整功能库存。F2 需要人工枚举加 UI 资源交叉校验。

### P2_MAINTAINABILITY

- **P2-1** 10 个致命助手，其中 6 个与 `RuntimeFatality` 语义等价重复。
- **P2-2** `ProcessScopes.shouldLoadPrefs` / `shouldHook`（ProcessScope.kt:128-134）无任何调用者。
- **P2-3** `mods/utils/FeatureInstallState` 与 `mods/utils/FeatureId` 无任何调用者（A14 风格休眠副本）。
- **P2-4** 回调内直接反射：`SystemNotificationMoreHooks.kt:676, 698`、`SystemUINotificationHooks.kt:133-135, 144-146`。这些是用户交互触发路径（通知菜单、快捷启动），不是逐帧热路径，但未复用已解析成员。
- **P2-5** `XposedHelpers` 四个反射缓存（:70-73）条目数无上限；对照 A14 `ReflectionCache` 的 MAX_LOADERS=4 / 每 loader 64 类 LRU。
- **P2-6** `SystemUiInstaller` 10 秒重启守卫（:74-76, :492-494）命中时静默跳过绝大多数 SystemUI 功能，无重试、无诊断记录。
- **P2-7** `ProcessScope.SYSTEM_SERVER` 在 `onPackageReady` 中是死分支；`NETWORK_STACK` 有 scope 无消费者。
- **P2-8** `PreferenceLoadRegistry` 用带 `pref_key_` 前缀的原始键，installer 用 `PrefMap` 自动补前缀，两套键约定并存。

### P3_CLEANUP_ONLY

- **P3-1** `MainModule.onPackageReady` 中 `ProcessScopes.resolve` 被计算两次。
- **P3-2** `PreferenceBootstrap.State.EMPTY_PENDING` 已声明但无任何写入点。
- **P3-3** installer 内冗余包名再校验（SystemUiInstaller.java:49 等）。
- **P3-4** `MainModule.java:171` 重复求值 `hasAnyLauncherApplicationFeature`。

### FALSE_POSITIVE / NO_ACTION

- `XposedHelpers` 以 `Class<?>` 为键的缓存**不是**跨 ClassLoader 正确性风险：同名类由不同 loader 定义时是不同 `Class` 对象，天然不同键。
- `ModuleHelper` 的静态 `ActivityThread` 缓存**不是**跨 loader 风险：`android.app.ActivityThread` 属 boot classpath，各进程同一 `Class`。
- `SystemNotificationMoreHooks.rethrowFatal`**未**遗漏 `OutOfMemoryError`：它是 `VirtualMachineError` 子类，已被覆盖。
- A13 缺少 A14 的通用 `OwnedRegistrations` / `WeakIdentityMap` 原语**不构成缺陷**：现有特化原语在 receiver / observer 上语义相当，不应为对齐而新建框架。

---

## 15. 就绪度

```text
F2_FEATURE_INVENTORY_READY = YES
F4_PRODUCTION_PORT_READY   = NO
ARCHITECTURE_CORRECTIVE_REQUIRED = YES
```

F2 可以开始：进程路由、installer 拓扑、catalog 规模与 preference 归属已完整测绘，功能库存可以可靠映射（P1-5 意味着 F2 需人工枚举而非工具自动生成，这是工作量问题，不是理解缺口）。

F4 暂不放行：P0-1 位于每个被注入进程的强制启动路径；P1-1 / P1-2 意味着以 legacy 直接 Hook 方式移植功能会继承 install-once 缺口。

建议的最小修正批次（顺序）：

```text
C1  PreferenceBootstrap 致命边界 + 不变式覆盖扩展            (关闭 P0-1)
C2  Application.attach 回调幂等守卫（Launcher / GenericApp）  (关闭 P1-1)
C3  HookTargetResolver.findFatalCause 补全 + 致命助手收敛     (关闭 P1-4, 缓解 P2-1)
C4  FeatureRuntime 进程键语义修正（含 ClassLoader 身份）      (关闭 P1-3)
C5  删除 migration residue（shouldLoadPrefs/shouldHook、
    FeatureInstallState、utils/FeatureId）                    (关闭 P2-2, P2-3)
```

C1 至 C3 是 F4 的前置条件。C4、C5 可与 F2 并行。

---

## 16. 证据等级

```text
STATIC_VERIFIED = YES
BUILD_VERIFIED  = YES   (python tools/verify.py full)
LOG_VERIFIED    = NO
DEVICE_VERIFIED = NO
PRODUCTION_CHANGED = false
```

本文件的所有结论来自源码静态阅读与构建门禁，不含任何实机运行时验证。
