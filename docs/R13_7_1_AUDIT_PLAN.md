# r13.7.1 审计：MainModule、偏好 Schema、Hook 目标能力探测与诊断

> 本轮只输出实施方案，不进行大规模重写。审计范围以当前 `devin/r13.7.1-maintenance-foundation` 为基线。

## 审计基线

- `MainModule.java`：模块入口、进程分发、偏好加载/监听、`RemotePreferences` 生命周期。
- `PrefMap` / `AppHelper` / `MainApplication`：偏好存储、镜像、本地/远程同步。
- `XposedHelpers` / `ModuleHelper`：反射封装、日志、注册表、异常边界、Hook 目标探测。
- `app/src/main/res/xml/prefs_*.xml`：偏好 Schema、key 命名、默认值、依赖关系。

## 审计结论（仅方案，不落地）

### 1. MainModule 偏好加载与进程分发

**当前状态**

- `initPrefs(Map<String, ?>)` 已按 A14 Claude 审计修复空快照问题：空快照不置 `prefsLoaded`，非空才写入并置位。
- `watchPreferenceChange()` 注册监听器后设置 `prefsWatcherRegistered`，顺序已调整。
- `onPackageReady` 按 `needLoadPrefs(pkg, remote)` 过滤进程；无关进程不加载偏好，但 `needLoadPrefs` 仍有一次 `SharedPreferences` 读取和部分字符串比较。
- `MainModule` 仍由 Java 编写，属 AGENTS 规定的稳定 Java 边界；不建议本轮迁移。

**风险点**

1. `needLoadPrefs` 在 **每个 `PackageReady` 进程** 都调用 `getRemotePrefs().getAll()` 以判断是否需要加载。对无关进程而言这是一次 binder 调用和全量快照构造。
2. `mPrefs` 是 `PrefMap<String, Object>`；`getXxx` 时若 key 不存在，默认值类型可能与 `PrefMap` 中实际类型不一致，导致 `ClassCastException` 在热路径。
3. `MainModule.onPackageReady` 中大量 `mPrefs.getXxx` 调用分散在条件判断里，某些功能关闭时仍读取了偏好（虽然值本身来自内存，但 `PrefMap` 的 `get`/`contains` 调用会触发 `hashCode` 与装箱）。
4. `processName` 字段在 `onModuleLoaded` 赋值，但 `onPackageReady` 也可能运行在不同进程；目前没有 `MainModule` 级别的进程能力缓存。

**实施方案（r13.7.2 或后续专门分支）**

- 在 `MainModule` 内新增 `ProcessProfile`：
  - 只计算一次 `Build.VERSION.SDK_INT`、`isMIUI14`、进程名、包名；
  - 按包名维护 `BooleanArray` 或 bitset 记录是否允许加载偏好；
  - 无关进程在 `onPackageReady` 立即 `return`，不再触碰 `SharedPreferences`。
- 将 `needLoadPrefs` 的结果缓存到 `MainModule` 静态 map，key 为 `processName + "/" + pkg`，value 为 `Boolean`；包列表或偏好变化时失效。
- 对 `mPrefs` 增加 `getXxx(key, default, expectedClass)` 的调试封装（仅在 debug 构建启用）用于 CI 捕获类型不一致；release 构建保持现有 `PrefMap` 调用。
- 提取 `MainModule.onPackageReady` 中的分支判断为 `System/server/SystemUI/Launcher` 三个 domain 路由对象，保持当前判断语义，但减少 `MainModule` 直接引用 60+ 个 hook 类。

### 2. 偏好 Schema

**当前状态**

- 偏好 key 分散在 XML：`prefs_system.xml`、`prefs_systemui.xml`、`prefs_launcher.xml`、`prefs_various.xml` 等。
- 默认值硬编码在 `MainModule` 和各个 hook 的 `getXxx` 调用中，未与 XML `defaultValue` 对齐。
- 没有统一的键名常量文件；存在字符串拼写风险。

**风险点**

1. 同一个 key 在 XML、`MainModule`、`AppHelper`、`subs` 多处以字符串字面量出现，改名或新增易遗漏。
2. `getStringAsInt` 与 `getInt` 默认值不一致时，同一 key 在不同进程读取到不同类型/值。
3. 偏好依赖（例如 `system_statusbar_clocktweak` 启用后 `system_statusbar_clock_show_seconds` 才生效）只在 UI 层 `dependency` 控制；Hook 层未校验，可能导致无意义 hook 注册。

**实施方案**

- 新增 `docs/PREFERENCE_SCHEMA.md`：列出所有功能开关 key、数据 key、默认值、值类型、允许进程、依赖项。
- 引入 `pref_keys` 生成脚本（可选）：从 XML 提取 key 生成 Kotlin `const val`，减少拼写错误；或维持现有 XML 为权威来源，新增 `tools/check-prefs-schema.py` 校验源码中 `getXxx` 的 key 在 XML 中存在且类型一致。
- 在 `MainModule` 初始化阶段增加 `SchemaValidator.checkDefaults()`（仅 debug/CI）：
  - 对比 `MainModule` 中 `getXxx` 默认值与 XML `defaultValue`；
  - 标记 `getStringAsInt` 的 key 默认字符串为可解析整数；
  - 标记依赖 key 缺失。

### 3. Hook 目标能力探测

**当前状态**

- 大量使用 `XposedHelpers.findClassIfExists` / `ModuleHelper.findAndHookMethod` / `findAndHookConstructor`，失败时打印日志并跳过。
- 失败日志未限流：某些 ROM 缺少目标类/方法时，每次 `PackageReady` 或每次功能触发都可能打印。
- 没有统一的“目标能力缓存”：同一个类/方法在同一个进程中可能被多个 hook 重复 `findClass`。

**风险点**

1. 高频路径的 `findMethodExact` / `findClass` 仍可能反复反射（虽然 `XposedHelpers` 有缓存，但命中时仍可能分配 `MemberCacheKey`）。
2. ROM 差异导致目标不存在时，日志可能刷屏（A14 审计已发现 `DeviceInfoMonitor` sysfs 节点缺失导致重复异常）。
3. Hook 安装阶段没有清晰的“能力矩阵”；排查时难以从 LSPosed 日志直接看出哪些 hook 未命中及其原因。

**实施方案**

- 新增 `HookCapabilityProbe`（轻量单例，仅冷路径使用）：
  - 以 `ClassLoader + 类名 + 方法/字段名` 为 key，`Class<?>` / `Member` / `NotFound` 为 value；
  - 命中时不分配；未命中时只记录一次 `NotFound` 日志；
  - 与 `XposedHelpers.findClassIfExists` 共享现有缓存，但补充一个 `ProbeResult` 返回值。
- 在 `MainModule.onPackageReady` 各进程入口处，增加 `probeClassLoader(lpparam.getClassLoader())`：
  - 仅探测该进程需要的目标类/方法集合；
  - 探测失败的功能在本进程禁用，避免运行时反复 `try/catch`。
- 为每个 hook 包（`System`、`SystemUI`、`Launcher`）增加 `ProbeReport`，在 `XposedHelpers.log` 中按进程输出一次：
  - `CustoMIUIzer A13 r13.7.1 in <pkg>：loaded hooks = N, skipped = M`。
- 保持 `XposedHelpers` 的反射缓存不变，仅在其外层增加一次 `ProbeResult` 包装。

### 4. 诊断体系

**当前状态**

- `XposedHelpers.log(String)` / `log(Throwable)` 输出到 logcat，TAG 统一为 `LSPosed-Bridge`。
- `ModuleHelper.guarded` 已覆盖大部分回调异常边界。
- 没有按模块版本/进程/功能聚合的诊断计数器或采样开关。

**风险点**

1. 模块用户报告问题时，需要手动从 LSPosed 导出日志；没有 `dumpState` 机制。
2. 高频 hook（状态栏时钟、网络速度）异常若被吞在 `guarded` 中，可能长期静默失败。
3. `XposedHelpers.log(Throwable)` 在热路径可能分配 stack trace 字符串；需要限流。

**实施方案**

- 新增 `Diagnostics`（object，轻量）：
  - 每个进程一个 `LinkedHashMap<String, Int>` 计数器：`hookError`、`preferenceMiss`、`capabilityNotFound`；
  - 首次异常记录完整 stack；30 秒内同类型异常只计数不重复打印；
  - 提供 `dumpState()` 在 `MainModule.onModuleLoaded` 或收到 `DUMP_INTENT` 时输出一次统计。
- 在 `ModuleHelper.guarded` 的回调失败路径中调用 `Diagnostics.count(...)` 而非直接 `XposedHelpers.log`；
  - 失败计数超过阈值（如 3 次）才打印一次降级提示。
- 不替换现有 `XposedHelpers.log` 调用；只在新代码或新审计点逐步接入 `Diagnostics`。

## 本轮不做的边界

- 不迁移 `MainModule.java` 到 Kotlin（AGENTS 稳定 Java 边界）。
- 不新增第三方日志/分析库。
- 不改 `versionName`、`versionCode`、签名、包名、libxposed API。
- 不修改 Xposed 入口、scope 或 `module.prop`。

## 验证计划（后续）

- `tools/check-invariants.py`：增加 MainModule 入口调用点、偏好 Schema 一致性规则。
- 单元测试：增加 `ProcessProfile` 缓存、`HookCapabilityProbe` 命中/未命中、`Diagnostics` 限流测试。
- 构建：`compileDebugKotlin` / `compileReleaseKotlin`、R8、resource shrink、三档 lint、Debug/Release APK、zipalign、v2 签名。
- 实机：SystemUI 重建、熄屏/亮屏秒针、设置变更实时同步、模块在不同进程加载日志。
