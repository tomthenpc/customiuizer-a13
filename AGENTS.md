# AGENTS.md

## 1. 适用范围与优先级
本文件适用于整个仓库；更深目录的 `AGENTS.md` 只补充其子树规则。
指令优先级：
1. 用户本轮明确要求
2. 本文件
3. 当前任务文档与 checkpoint
4. 仓库说明文档
5. 历史版本与上游参考
修改前按需阅读：
- `docs/PROJECT_LINEAGE.md`
- `docs/ENGINEERING_METHOD.md`
- `docs/KOTLIN_POST_MIGRATION_REVIEW.md`
- `docs/R13_CHECKPOINT.md`
- 与任务直接相关的入口、调用链、测试、Git 历史和 R8 规则
过期文档不得覆盖当前源码、构建结果、日志和实机事实。

## 2. 固定项目边界
- 仓库：`tomthenpc/customiuizer-a13`
- 项目：CustoMIUIzer A13
- 目标：MIUI 14 / Android 13，运行 API 33
- `applicationId=tv.withaibuild.customiuizer.r13`
- `minSdk=33`，`targetSdk=34`
- ABI：`arm64-v8a`
- libxposed：`minApiVersion=101`，`targetApiVersion=102`
- `staticScope=false`
- Hot Reload 关闭
- 禁止恢复 `de.robv.android.xposed` Legacy API
- A13 功能语义基线：`MonwF/customiuizer v23.11.26`
Gradle、AGP、Kotlin、依赖、版本号和签名配置必须从当前仓库核对，不得照抄本文。
A14 只能参考工程方法，不能作为 A13 的 ROM 类名、资源、Hook target、进程行为、preference key、Manifest 或界面行为依据。

## 3. 技术事实来源
判断顺序：
1. 用户当前要求
2. 当前 A13 源码、Git、构建配置、APK metadata、日志和实机结果
3. 当前 A13 Git 历史
4. Android、Kotlin、Gradle、libxposed 官方资料
5. A13 上游 `v23.11.26`
6. A14 工程方法
不得用 A14 或上游文件覆盖当前 A13 实现。

## 4. 固定优先级
1. 实际可构建、可安装、可运行
2. 用户可见行为和 Hook 语义正确
3. `system_server`、SystemUI、Launcher 稳定
4. Android 13 / MIUI 14 兼容
5. JVM、反射、ClassLoader、进程、R8、libxposed 兼容
6. 生命周期和资源安全
7. 性能、内存和功耗
8. 可维护性
9. Kotlin 覆盖率和代码简短
不得用低优先级目标交换高优先级目标。

## 5. 执行协议
每轮开始：
- 检查当前分支、HEAD、`git status` 和相关 diff
- 保护来源不明或用户已有的未提交修改
- 先建立可复现问题或明确成本，再修改
- 追踪入口、调用方、所属进程、生命周期、动态引用、测试和近期历史
- 使用最小但完整、可解释、可验证的改动
- 先运行最快的相关验证，再按风险扩大
- 完成前审查完整 diff 和工作区状态
不要在首次编译成功后停止；继续修复由本轮改动直接引起的失败。
普通读取、搜索、编辑、测试和构建无需反复询问。仅在破坏性操作、未知凭据、清除设备/应用数据、修改或合并 `main`、公开发布，或产品语义无法由证据判断时询问。

## 6. Git 与敏感信息
未经明确授权，禁止：
- `git reset --hard`
- `git restore .`
- `git checkout -- .`
- `git clean -fd`
- force push
- 覆盖未知本地工作
不得提交或输出：
- keystore、密码、token、真实 `keystore.properties`
- APK、签名备份、私人日志、本地缓存和构建目录
- 私有设备数据或机器专属敏感信息
交付构建不得静默回退到 Android Debug 证书。

## 7. A13 兼容契约
Java → Kotlin 或重构时，按实际依赖保持：
- FQCN、构造器、重载、JVM descriptor、可见性、primitive/boxed 类型
- static 字段/方法、初始化顺序、同步、volatile/atomic 语义
- `@JvmStatic`、`@JvmField`、`@JvmName` 和 Java 互操作
- 反射类名/成员名、DexKit、字符串入口、JNI/native 边界
- Manifest、authority、XML 引用、preference key、资源名
- `META-INF/xposed/java_init.list`、`module.prop` 和 libxposed metadata
- process gate、ClassLoader、初始化时机
- Hook target、priority、注册条件/顺序、before/after、参数修改、early return、result、throwable、`Chain.proceed()` 和回调次数
- Release/R8 可达性和 resource shrink 行为
编译通过不等于兼容。
ROM 目标缺失时只记录一次，并仅停用当前单项功能；不得高频重试或拖垮关键进程。

## 8. Kotlin-first 与代码风格
目标是 Kotlin-first，不强制 100% Kotlin。
优先迁移：
- model、data、普通 utility、Adapter
- 设置 UI、Activity、Fragment
- Service、Receiver、Provider、Preference
- 非 Hook 业务逻辑、纯计算、测试和边界清晰的 helper
高风险边界仅在证据充分时迁移：
- Hook 注册/回调基础设施
- 反射、DexKit、Remote Preferences、ResourceHooks
- 进程级静态状态、R8 动态入口、JNI/native
- `MainModule`、`XposedHelpers`、`MemberUtilsX`
- 第三方或 vendored 代码
Kotlin 特性必须实际提高安全、状态建模、资源所有权或可读性。避免：
- `!!`
- 深层 scope function
- 复杂 DSL 和隐藏副作用
- 热路径长集合链、无必要 `Sequence`
- 用 Flow/coroutine 替代简单回调却增加生命周期或调度成本
项目采用：低抽象、强边界、状态显式、控制流直接、热路径可预测、资源所有权清楚、兼容代码集中。不得曲解为超长函数、全局可变状态、复制逻辑或吞异常。

## 9. 性能与生命周期
额外成本模型：
`触发频率 × 单次成本 × 进程数量 × 存活时间`
要求：
- 功能关闭时接近零运行成本
- 无关进程不初始化无关功能
- 事件和生命周期回调优先于轮询
- 注册幂等，释放可重复
- 长期资源必须明确创建者、所有者、停止/注销路径和防重复状态
- 缓存必须有容量、范围、生命周期或失效规则
- 禁止静态持有 Activity、Fragment、View、临时 Context 或临时 ClassLoader
绘制、动画、触摸、通知绑定、状态栏/控制中心、网速、音频回调和高频 SystemUI/Launcher Hook 中避免：
- 反射、DexKit、磁盘 I/O、同步远程 Binder
- 重复 SharedPreferences、API/ROM 判断
- 临时集合/数组、Pair/Triple、装箱、捕获 lambda、重复格式化
- 大锁、正常运行日志、重复兼容探测
反射、解析、资源查找和兼容探测放到冷路径；热路径只读取准备好的不可变或原子状态。

## 10. UI 与 Release 回归
恢复原有界面语义，不重新设计无关 UI。
主题、字体、Preference、Switch 回归必须：
- 对比最后正常版本、当前 diff 和最终 merged resources
- 验证 Debug/Release、light/dark、启用/禁用和覆盖升级
- 优先恢复正确的 theme/style/resource 契约
- 禁止用全局强制字体或固定颜色掩盖主题问题
- 禁止将关闭 R8、resource shrink、Lint 或测试作为最终修复
- 未经授权不得卸载或清除应用数据

## 11. 验证
使用仓库实际存在的任务，不伪造执行结果。按风险覆盖：
- targeted/unit test
- `lint`、`lintRelease`、`lintVitalRelease`
- `assembleDebug`、`assembleRelease`
- R8、resource shrink
- applicationId、version、SDK、ABI、Xposed metadata、动态入口
- zipalign、APK SHA-256、签名证书连续性
- Legacy Xposed API 扫描
- API 101/102 边界
Gradle 退出码为 0 或生成 APK，不等于目标进程和实机行为正确。最终必须区分已验证、未验证和待实机项目。

## 12. Commit 与发布
当前任务或仓库规则已授权时，可在当前非 `main` 开发分支创建清晰 commit 并 push。
未经本轮明确要求，不得：
- 修改或合并 `main`
- 创建 PR、tag、GitHub Release
- 公开上传 APK
- 删除旧 Release
- 将未实机验证版本称为稳定版
不同根因应拆分提交，不混入无关改动。

## 13. 文档实时同步
文档职责：
- `AGENTS.md`：长期、仓库级规则和不变量
- `AGENTS.md`：Devin 每轮任务都必须遵守的长期仓库规则
- `docs/DEVIN_R13_CHECKPOINT.md`：当前目标、事实、HEAD、最新绿色构建、commit、阻塞、下一步和待实机状态
- README/CHANGELOG：稳定的用户可见状态和正式变更
每完成一个有意义的工程闭环，必须在同一任务立即更新 `docs/DEVIN_R13_CHECKPOINT.md`，不得拖到下次会话。
仅当长期不变量、反复出现的纠正或仓库级流程变化时更新 `AGENTS.md`。应替换过期或冲突内容，不得无限追加。
共享文档和记忆中不得写入密码、凭据、keystore、私有日志和临时敏感信息。
最终报告前，核对文档与实际 HEAD、工作区、构建结果和发布状态一致。

## 14. Devin 执行方式（轻度 Claude 风格）
Devin 仍以自主执行和实际修改为主，不改成只分析不落地的模式。

执行时采用以下风格：
- 先建立事实，再提出判断；先读当前代码、Git、构建和日志，不凭印象修改
- 计划保持简短，并根据新证据动态调整，不机械坚持旧 Phase
- 可以并行完成互不依赖的只读调查，但代码改动应保持根因清晰
- 普通读取、搜索、编辑、测试、构建、commit 和已授权开发分支 push 不逐项询问
- 长时间任务只在完成工程闭环、发现重要风险、改变路线或遇到硬阻塞时简短汇报
- 汇报结论时同时给出证据、未验证项和下一步，不输出冗长命令流水账
- 发现问题后优先修根因；不得用吞异常、关闭检查、扩大 keep 或伪造结果快速收口
- 对不确定结论主动寻找反证，避免把日志噪声、A14 差异或理论风险直接当成 A13 缺陷
- 不因为一次构建成功、一个 commit 或一次 push 就提前停止；仍有安全、独立、可验证工作时继续推进
- 最终结论必须区分：已验证、代码层面确认、需要实机验证、当前无法确认

这种“Claude 感”只体现在分析质量、证据纪律和表达方式，不降低 Devin 的自主执行力度。

## 14. 最终报告
只报告高价值事实：
- 范围与根因
- 修改文件与行为变化
- 兼容性和安全影响
- 实际执行的测试/构建及结果
- 适用时的 APK/签名检查
- 分支、HEAD、commit、push 和工作区状态
- 剩余风险与明确待实机项
- 本轮已同步的文档
