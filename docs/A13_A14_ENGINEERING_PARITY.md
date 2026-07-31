# A13 / A14 工程差距矩阵

> 目标仓库：`tomthenpc/customiuizer-a13`  
> 开发分支：`devin/r13.2-kotlin-api102`（当前 HEAD 以最终提交为准）  
> 参考仓库：`tomthenpc/customiuizer-a14`  
> 参考基线：`main` / `r14.13.5` / `4225d80e95ed9965ab68a09b575aff4046666a5d`  

本矩阵只记录 A13 可移植的工程能力差距，不复制 A14 的 Android 14 专属 Hook、包名、资源或 Manifest。

## 状态图例

| 标记 | 含义 |
| ---- | ---- |
| 已对齐 | A13 当前已具备同等工程能力 |
| 部分对齐 | 有实现但存在可验证差距 |
| 可移植差距 | 可从 A14 方法中移植到 A13 |
| A13 不适用 | 只适用于 Android 14 / A14 仓库 |
| 待实机证据 | 需要真机或 LSPosed 日志验证 |

## 矩阵

| 领域 | A13 当前事实 | A14 参考事实 | 是否适用于 A13 | 差距 | 动作 | 验证 |
| ---- | ------------ | ----------- | -------------- | ---- | ---- | ---- |
| Git/Release | 开发分支领先 main 14 个提交，无共同祖先 | main 即正式版 `r14.13.5` | 是 | main 仍是 Groovy DSL 旧历史，需要历史桥接 | 完成本轮追赶后创建 `release/r13.2.3-test1` 并合并 main | `git merge-base`、`git diff --stat`、`gh release` |
| Build DSL | Kotlin DSL + `gradle/libs.versions.toml` | Kotlin DSL + version catalog | 是 | 已对齐 | 保持 | `assembleDebug`、`assembleRelease` |
| 签名失败行为 | 原配置缺少签名时指向无效 keystore 路径，包签名阶段才失败 | 明确 fail-fast | 是 | 已改为 `check(hasReleaseSigning)` 在 `release`/`develop` 配置期直接抛错 | 已修改 | `assembleRelease` 缺签名时立即失败 |
| API 101/102 | `minApiVersion=101`、`targetApiVersion=102`；版本 catalog 中 `libxposed-api=101.0.1`、`libxposed-service=101.0.0` | 使用 `libxposed-api/service 102.0.0` 编译 | 是（需验证） | 当前依赖实际为 101，编译产物仍是 API 101 类型 | 已记录状态；允许继续发布，但 Release notes 必须写明 | DEX 扫描、module.prop、构建产物 |
| Kotlin/JVM 边界 | 38 Java / 48 Kotlin（main），保留 `MainModule.java` 等稳定边界 | 3 Java / 92 Kotlin | 部分 | A13 稳定 Java 边界可继续保留，重点审计迁移后的控制流 | 本轮完成 PrefPair 与 SystemUI 优化；其余按批次处理 | Java/Kotlin 编译、R8、反射入口 |
| RemotePreferences | `initPrefs` 不缓存空快照，`watchPreferenceChange` 注册成功后置位 | 同审计修复 | 是 | 已对齐 | 已有正确实现，补测试与文档 | 单元测试、空快照场景 |
| SystemUI 生命周期 | 状态栏文本图标原为 `ArrayList<View>` 强引用 | 使用弱引用与清理机制 | 是 | 已改为 `WeakReference<View>`，注册/遍历时清理失效引用 | 已修改 | 编译、热路径 Review |
| Launcher 生命周期 | 未在本次审计中直接改动 | 弱引用与反注册治理 | 可移植 | 未发现 A13 强引用 View/Context 静态集合的类似问题 | 标记为低风险，后续批次 Review | 代码审查 |
| ResourceHooks | `SparseIntArray fakes` + 单次 `get()` 命中路径 | 更完整的 `SparseArray` + 拷贝写入 | 部分 | 热路径已优化；写路径仍用 `ConcurrentHashMap`，在本线程模型下可接受 | 已验证，未引入额外重构 | 构建、R8、实机资源替换 |
| ClassLoader/DexKit | 未在本轮广泛使用 DexKit | A14 有 DexKit 边界治理 | A13 不适用 | A13 当前实现不依赖 DexKit；追赶不得新增无用 DexKit | 标记为不适用 | 无 |
| 热路径分配 | PrefPair 避免 Regex；SystemUI 避免静态强引用 | 完整的高频路径治理 | 是 | 完成本轮两处热点 | 已修改 | Lint、测试、编译 |
| Search 状态机 | `SearchNavigation.kt` + 10/11 个单元测试 | A14 显式状态机 | 是 | 已对齐 | 已有 | 单元测试、实机搜索返回 |
| Locale/主题 | `AppLocaleController` 单一状态源，17 个测试 | A14 仍有已确认的状态问题 | 已对齐（以 A13 为准） | A13 当前实现以自身为基线，不复制 A14 | 验证通过则不修改 | 实机语言切换 |
| 单元测试 | 57+ 单元测试通过 | 大量测试 | 是 | 已增加 `PrefPairTest` | 已修改 | `:app:test` |
| R8/resource shrink | `isMinifyEnabled=true`、`isShrinkResources=true` | 同 | 是 | 已对齐 | 保持 | `assembleRelease`、APK 审计 |
| APK metadata | `module.prop` 正确，入口 `name.monwf.customiuizer.MainModule` | 同类型结构 | 是 | 已对齐 | 保持 | APK 反编译、签名验证 |
| 日志审查 | 无动态路径协议 | A14 有日志审查协议 | 可移植 | 需新增 `docs/LSPOSED_FULL_LOG_REVIEW_PROTOCOL.md` | 创建文档 | 文档审查 |
| 文档/checkpoint | `DEVIN_R13_CHECKPOINT.md` 已过期，指向 `3c03ccb` | 与代码和 HEAD 实时同步 | 是 | 需要更新到当前 HEAD，清理过期任务 | 更新 | 文档审查 |

## 明确不移植的 A14 项

- Android 14 / API 34 版本闸门、Hook target、类名。
- A14 包名 `tv.withaibuild.customiuizer.r14`、namespace、资源目录。
- A14 当前仍有问题的 Locale 实现。
- A14 过期 checkpoint、测试数字或 Release 文案。

## A13 基线统计

- Java 主源码：38 个
- Kotlin 主源码：48 个
- 测试 Kotlin：5 个
- 总提交：从 `c11f6f4` 起已完成多轮 Kotlin 清理、搜索状态机、Locale 状态机、P1/P2 修复、PrefPair、SystemUI 弱引用、构建配置清理。
