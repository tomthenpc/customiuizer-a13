# A13 对齐 A14 工程体系计划

> 目标仓库：`tomthenpc/customiuizer-a13`
> 开发分支：`devin/r13.3-kotlin-migration`
> 当前 HEAD：`d43dfcf68206e53d928d3e85415abf63fec0058c`
> 参考仓库：`tomthenpc/customiuizer-a14`
> 参考分支：`main`
> 参考 HEAD：`0b277609a79dfd36e8c35807bf90c5c9793c71d3`（tag r14.13.7）
> 目标系统：MIUI 14 / Android 13
> A14 仅作为工程方法、流程与验证门禁的参考基线，不复制 Android 14 专属代码。

---

## 一、基线

### 1.1 A13 仓库

- 项目：CustoMIUIzer A13
- applicationId：`tv.withaibuild.customiuizer.r13`
- 运行平台：MIUI 14 / Android 13 / SDK 33-34 / `arm64-v8a`
- libxposed：`minApiVersion=101`、`targetApiVersion=102`、`staticScope=false`
- Hot Reload：`false`
- Legacy Xposed API：`false`

### 1.2 A14 参考仓库

- 项目：CustoMIUIzer A14
- applicationId：`tv.withaibuild.customiuizer.r14`
- 运行平台：HyperOS 1 / Android 14 / SDK 34 / `arm64-v8a`
- libxposed：`minApiVersion=101`、`targetApiVersion=102`、`staticScope=false`
- 正式 tag：`r14.13.7`

### 1.3 当前 K1～K6 迁移状态

| 阶段 | 状态 |
| ---- | ---- |
| K1 基础设施与测试 | 已完成 |
| K2 小工具与偏好状态 | 已完成 |
| K3 `Launcher.java` 拆分迁移 | 已完成 |
| K4 `SystemUI.java` 拆分迁移 | 已完成 |
| K5 `System.java` 拆分迁移 | **暂停中**（18 个 `System*.kt` 已创建，未生成 facade、未删除 `System.java`） |
| K6 `HookerClassHelper`/`ModuleHelper`/`ResourceHooks` 评估 | 未开始 |

---

## 二、对齐矩阵

> 状态标记：
> - `已完成`：A13 已具备同等能力
> - `部分完成`：有实现但存在可验证差距
> - `待适配`：需按 A13 结构与 A14 方法移植
> - `不适用`：Android 14 / A14 仓库专属
> - `禁止移植`：会直接破坏 A13 兼容或不符合 AGENTS 边界
> - `待实机验证`：需真机或 LSPosed 日志验证

| A14 能力 | A13 当前状态 | 是否适配 | A13 实现位置 | 风险 | 验证方法 |
| --- | --- | --- | --- | --- | --- |
| Hook 回调异常保护 | 部分完成：`MethodHook` 有 try/catch，但 hook 内注册出的框架回调（Receiver/Runnable/Observer/Listener）多数未统一保护 | 待适配 | 引入 `ModuleHelper.guarded` 后覆盖 `mods/` 下 `onReceive` / `run` / `onChange` / lambda 回调 | 高 | 静态门禁 + 实机 system_server / SystemUI / Launcher 日志 |
| deferred callback 保护 | 未开始：A13 仍有 `postDelayed(Runnable { ... })`、`setOnXxxListener {}` 等 lambda 未受保护 | 待适配 | `Controls.java` 等 Handler/Runnable 路径 | 高 | `check-invariants.py` 门禁 + 实机 |
| Coroutine failure handler | A13 无协程 | 不适用 | — | 低 | — |
| Receiver 所有权 | 部分完成：已有 `RECEIVER_EXPORTED` / `RECEIVER_NOT_EXPORTED` 显式标记，但缺乏统一注册表和 owner 弱引用治理 | 待适配 | `ModuleHelper` 新增 `registerModuleReceiver` / `registerOwnedReceiver` | 中 | 静态门禁 + 实机重建/主题切换 |
| ContentObserver 所有权 | 未统一：个别 observer 通过 additional instance field 反注册，清理代码未在 `hookAllConstructors` 生效 | 待适配 | 锁屏手电筒等 observer 注册点 | 中 | 实机反复重建 |
| Preference observer 生命周期 | 已完成：空快照不固化 `mPrefsLoaded`，`watchPreferenceChange` 成功后置位 | 已完成 | `MainModule.java` | 低 | `:app:test` |
| 只读参数零复制 | 部分完成：未系统治理 `getArgsArray` / `chain.proceed(args)` | 待适配 | `mods/*.kt` 迁移后的 `before` / `intercept` 路径 | 高 | 热路径 Review + 测试 |
| 无 Looper Handler 检查 | 未开始：`Handler()` 无参构造可能绑定当前线程 Looper | 待适配 | 含 `Handler()` 的 mod 文件 | 中 | 静态门禁 |
| Legacy Xposed API 检查 | 已完成：禁止 `de.robv.android.xposed` 运行期使用 | 已完成 | `AGENTS.md`、`build.gradle` 扫描 | 低 | R8 mapping / DEX 扫描 |
| Regex 退化检查 | 部分完成：`PrefPair` 已避免单字符 `toRegex()`；新迁移文件需逐文件复查 | 待适配 | 全部 `*.kt` | 高 | `check-invariants.py` + 代码审查 |
| primitive key 装箱检查 | 部分完成：`ResourceHooks` 仍用 `ConcurrentHashMap<Int, _>` | 待适配 | `ResourceHooks.java` / `*.kt` | 中 | 热路径分析 |
| additional instance field 身份语义 | 已完成：A13 `XposedHelpers` 保持身份语义字段（同 A14 修复后实现） | 已完成 | `mods/utils/XposedHelpers.java` | 低 | 构建/测试 |
| RemotePreferences 空快照 | 已完成：空 `getAll()` 不设置 `mPrefsLoaded` | 已完成 | `MainModule.java` | 低 | `:app:test` + 实机解锁前 |
| Preference listener 注册状态 | 已完成：成功注册后置位，避免重复 | 已完成 | `MainModule.java` | 低 | `:app:test` |
| 屏幕关闭停止 ticker | 未开始：`MonitorDeviceInfoHook` 等 2s 周期任务息屏仍继续 | 待适配 | `SystemUIMonitorAndTileHooks` 等 | 中 | 实机电量/日志 |
| Bitmap 缓存上限 | 未评估：需检查 `SystemChargingAndWallpaperHooks` 等壁纸/图片处理 | 待适配 | 锁屏壁纸、用户头像等 | 中 | 代码审查 |
| SystemUI View 弱引用 | 已完成：`SystemUI.kt` 状态栏图标已改为 `WeakReference<View>` | 已完成 | `SystemUI.kt` | 低 | 编译 + 实机主题切换 |
| Java → Kotlin 控制流核对 | 部分完成：部分迁移文件按 AGENTS 已做 break/continue 审计 | 待适配 | 全部 Java→Kotlin 迁移文件 | 高 | 迁移时逐文件 `git show` 对比 |
| 大文件安全拆分 | 进行中：K5 已拆分但尚未完成 facade 与删除旧文件 | 待适配 | `System*.kt` | 高 | `split-hook-domain` 工具 + 调用序列校验 |
| R8 和动态入口核对 | 部分完成：R8 已启用，但需确认 `System*.kt` 新增对象后 keep 规则覆盖 | 待适配 | `app/proguard-rules.pro` | 高 | `assembleRelease` + DEX 扫描 |
| 实机内存基线 | 未开始 | 待适配 | `.devin/memory-audit` | 中 | `tools/capture-memory-baseline.ps1` |
| LSPosed 日志分析 | 未开始 | 待适配 | `tools/analyze_lsposed_log.py` | 中 | 有实机日志后脚本化分析 |

---

## 三、可直接适配的通用工程方法

1. **文档驱动开发**：每项修改前先建立 `docs/` 风险条目和验证清单。
2. **静态门禁**：引入 `tools/check-invariants.py`，每条规则对应真实缺陷。
3. **拆分证明**：先用脚本量化 public 入口、私有 helper、跨域调用，再动手。
4. **调用序列不变**：hook 注册顺序只与 `MainModule` 调用序列有关，与文件布局无关。
5. **热路径公式**：`触发频率 × 单次成本 × 进程数量 × 存活时间`。
6. **回调保护**：所有注册到 framework 的回调用 `ModuleHelper.guarded` 兜底。
7. **LSPosed 日志分诊**：不读原始日志，用脚本归并 P0/P1/P2。
8. **内存基线对照**：A/B/C 三组实验，中位数 PSS/RSS/Heap 比较。

---

## 四、需要按 A13 结构改写的方法

1. **回调保护 API**：A14 的 `ModuleHelper.guarded` 依赖 A14 `ModuleHelper.kt` 设计；A13 当前 `ModuleHelper.java` 需先引入 `guard`-family 工具，再在 Java 边界保持可用。
2. **资源 Hook 热路径**：`ResourceHooks.java` 优化需保持 Java 实现，不能直接复制 A14 Kotlin 代码。
3. **大文件拆分工具**：A14 `split-hook-domain.py` 解析 A14 包名 `tv.withaibuild.customiuizer.mods`；A13 需改为 `name.monwf.customiuizer.mods` 并检查 `MainModule.java` 调用。
4. **日志分析 profile**：`analyze_lsposed_log.py` 的 `--profile a14` 需新增 `--profile a13`，匹配 A13 applicationId 与 A13 进程签名。
5. **A13 分支名与版本线**：A13 开发分支为 `devin/r13.3-kotlin-migration`，不得硬编码 A14 `main` 作为默认分支。

---

## 五、Android 14 专属，禁止移植

- HyperOS 1 / Android 14 版本闸门、类名、Hook target。
- `tv.withaibuild.customiuizer.r14` 包名、namespace、资源前缀。
- A14 `compileSdk=37`、`targetSdk=34` 等版本组合在 A13 的强制升级。
- A14 新版 libxposed API 102.0.0 专属类型与 DexKit 2.2.0 依赖（A13 当前 101 基线稳定）。
- A14 `AppLocaleController` 等 UI 状态机代码不能直接覆盖 A13 已验证实现。
- A14 `MainModule.java` 包名 `tv.withaibuild.customiuizer.MainModule` 不能替换 A13 入口。
