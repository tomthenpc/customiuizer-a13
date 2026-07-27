# CustoMIUIzer A13 架构审计

## 1. 事实快照

- **仓库**：`tomthenpc/customiuizer-a13`
- **分支**：`devin/r13.2-kotlin-api102`
- **HEAD**：`990e977`
- **版本**：`r13.2.2-devin` / `120`
- **applicationId**：`tv.withaibuild.customiuizer.r13`
- **SDK**：`compileSdk=36`，`minSdk=33`，`targetSdk=34`
- **ABI**：`arm64-v8a`
- **libxposed**：`minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
- **工具链**：Gradle 8.9，AGP 8.7.2，Kotlin 2.0.21，JDK 17
- **审计时间**：2026-07-28

## 2. 绿色基线

| 命令 | 结果 | 日志 |
|------|------|------|
| `test` | 成功 | `.devin/a13_baseline.log` |
| `lintDebug` | 成功 | `.devin/a13_baseline.log` |
| `assembleDebug` | 成功 | `.devin/a13_baseline.log` |
| `assembleRelease` | 成功 | `.devin/a13_release_baseline.log` |

## 3. Release APK 元数据

- **路径**：`app/build/outputs/apk/release/app-release.apk`
- **SHA-256**：`9D9FE7D6CDD3825571F5490D3840ED0D15608FD27C6D190937CC9B4CC2794CBE`
- **签名**：v2 only，1 个 signer
- **签名证书 SHA-256**：`C0:EF:F2:DC:4E:66:27:17:19:54:90:DA:78:B1:2A:98:4C:6F:2E:6B:D3:8A:CF:4E:DA:D1:4D:53:E3:D2:2E:70`
- **包名**：`tv.withaibuild.customiuizer.r13`
- **versionCode/Name**：`120` / `r13.2.2-devin`
- **scope**：`android`、`system`、`com.android.incallui`、`com.android.settings`、`com.android.systemui`、`com.google.android.packageinstaller`、`com.miui.miwallpaper`、`com.miui.packageinstaller`、`com.miui.powerkeeper`、`com.miui.securitycenter`、`com.miui.home`、`com.miui.screenshot`、`com.mi.android.globallauncher`、`com.miui.gallery`、`com.lbe.security.miui`

## 4. 入口与生命周期

- **Xposed 入口**：`META-INF/xposed/java_init.list` → `name.monwf.customiuizer.MainModule`
- **MainModule** 继承 `XposedModule`，关键回调：
  - `onModuleLoaded`：设置 `XposedHelpers.moduleInst`、重置 `processHooked=false`。
  - `onSystemServerStarting`：加载 `remotePrefs`，拷贝到 `mPrefs`（`PrefMap`），按开关分发系统级 Hook。
  - `onPackageReady`：仅在 `isFirstPackage` 时处理，过滤 Settings/SecurityCenter 等进程后加载偏好并 Hook。
- **processHooked 标志**：`ModuleHelper` 系列 `hook*/findAndHook*` 方法在成功 Hook 后设置 `MainModule.processHooked = true`，用于触发 `watchPreferenceChange()`。
- **运行时偏好同步**：`watchPreferenceChange()` 在 `remotePrefs` 上注册 `OnSharedPreferenceChangeListener`，更新 `mPrefs` 并通过 `ModuleHelper.handlePreferenceChanged(key)` 通知模块。

## 5. 核心基础设施

### 5.1 PrefMap

- 扩展 `HashMap<String, Object>`，提供类型安全的 `getString`、`getInt`、`getBoolean`、`getStringSet`、`getStringAsInt`。
- `normalizeKey` 自动补 `pref_key_` 前缀。
- `getStringSet` 对 null 返回 `emptySet()`，避免 hot path 分配空集合。

### 5.2 ResourceHooks

- `SparseIntArray fakes`：假资源 ID 到模块资源 ID 的映射。
- `ConcurrentHashMap<String, Replacement> replacements`：按 `package:type/name` 维度替换 `ID`、`DENSITY`、`OBJECT`。
- `applyHooks()` 通过 `hooksApplied` 保证只注册一次 `Resources` 方法 Hook。
- `mReplaceHook` 每条资源访问都会调用 `getFakeResource` / `getResourceReplacement`；两者都有 `isEmpty()` 快速路径。

### 5.3 ModuleHelper

- 封装 `XposedHelpers.findAndHookMethod`、`hookAllMethods`、`hookAllConstructors` 等，统一处理异常并打日志。
- `getModuleContext / getModuleRes` 为 `synchronized` 方法，使用 `createPackageContext` 获取模块资源。
- `processHooked` 由成功 Hook 设置。

### 5.4 XposedHelpers

- 继承 LSPosed 工具类，包含 `fieldCache / methodCache / constructorCache`（`ConcurrentHashMap`）。
- `additionalFields` / `objectFields` 为 `WeakHashMap` + `HashMap`，使用 `synchronized` 块。
- `sMethodDepth` 为 `HashMap<String, ThreadLocal<AtomicInteger>>`，用于 Hook 递归计数。

## 6. Phase 3 迁移回归自动审计

统计来源：`.devin/a13_phase3_patterns.txt`。

| 模式 | 计数 | 位置/说明 | 风险初判 |
|------|------|-----------|----------|
| `toRegex()` | 2 | `AppHelper.kt:185`、`PreferenceAdapter.kt:32` | P3：每次调用都编译 Regex，可改为 char split |
| `forEach(new Consumer())` | 1 | `System.java:2094` | P3：创建匿名类，可改为增强 for |
| `use { }` | 1（有效） | `StepCounterController.kt:31` | 正常，Cursor 关闭 |
| `Map<Int` | 1 | `System.java:1955` ROM 字段反射 | 非模块 Map，无迁移问题 |
| `HashMap<Int` | 1 | `PreferenceFragmentBase.java:50` | Java 端列表映射，非 hot path |
| `SparseIntArray` | 6 | `ResourceHooks`、`System`、`SystemUI` | 正确使用 |
| `registerReceiver` | 63 | `Controls.java`、`GlobalActions.java`、`StepCounterController.kt` | P2：未显式指定 `RECEIVER_EXPORTED/NOT_EXPORTED`，在 Android 14 环境存在 `SecurityException` 隐患 |
| `registerContentObserver` | 6 | `System.java`、`SystemUI.java`、`Various.java` | 已见注销配对 |
| `registerOnSharedPreferenceChangeListener` | 3 | `MainActivity.java` 注册/注销配对；`MainModule.java` 注册 | P1：需确认 `watchPreferenceChange` 不会重复注册（目前由 `prefsWatcherRegistered` 保护） |
| `Handler(` | 21 | `Controls.java`、`System.java`、`SystemUI.java`、`Launcher.java` 等 | P2/P3：`postDelayed` 需确认 `removeCallbacks` 配对，目前代码基本有成对处理 |
| `postDelayed` | 42 | 多处 | 同上 |
| `WeakReference` | 7 | `BitmapCachedLoader.kt`、`Various.java`、`System.java` | 正常使用 |
| `synchronized` | 14 | `ModuleHelper`、`XposedHelpers` | 基本合理 |
| `XposedHelpers.callMethod` | 403 | 广泛 | 反射调用，无明确回归 |
| `setResReplacement` / `setDensityReplacement` / `addResource` | 28 / 44 / 20 | `ResourceHooks` | 正常 |

## 7. P0/P1/P2/P3 问题清单

| 级别 | 数量 | 问题 | 证据 | 状态 |
|------|------|------|------|------|
| P0 | 0 | 未发现阻断构建或核心 Hook 失效的问题 | `assembleRelease` 成功，APK 签名/元数据正常 | 已验证 |
| P1 | 1 | 部分 `registerReceiver` 未显式指定 export flag。MIUI 14 / Android 13 当前目标系统可运行，但若在 Android 14 环境运行可能触发 `SecurityException` | `Controls.java:153`、`GlobalActions.java:787/843`、`StepCounterController.kt:64` | 待验证（需要 Android 14 真机） |
| P2 | 1 | `ResourceHooks.mReplaceHook` 在资源替换启用后每条资源调用都执行 `ModuleHelper.findContext()`，热路径开销可优化 | `ResourceHooks.java` | 代码层面确认，待 profile |
| P3 | 2 | Kotlin 中 `toRegex()` 用于简单 `|` 分隔，可改为 char split；`System.java:2094` 的 `forEach(new Consumer())` 可改为增强 for | `AppHelper.kt:185`、`PreferenceAdapter.kt:32`、`System.java:2094` | 用户指示“卡了跳过”，待后续处理 |

## 8. 本次处理与跳过项

- **已完成**：绿色基线（test / lint / assembleDebug / assembleRelease）、APK 审计、架构地图、Phase 3 统计、P0-P3 分类。
- **已跳过**：Phase 5 低风险的 `toRegex()`、`forEach` 小修复，原因：用户指令“卡了跳过”。这些已记录为 P3，后续可随时安全执行。

## 9. 下一步建议

1. 处理 P1 `registerReceiver` export flag（优先在模块自身进程 `StepCounterController.kt`）。
2. 评估 P2 `ResourceHooks` 热路径优化成本。
3. 完成 P3 `toRegex()` / `forEach` 替换。
4. 执行真机验证矩阵（状态栏、导航栏、手势、锁屏、音量、通知菜单、QS 网格等）。
5. 同步 `CHANGELOG.md` 与 `VERIFICATION.md`。
