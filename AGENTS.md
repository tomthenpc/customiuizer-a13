# AGENTS.md

## 适用范围

本文件适用于整个仓库。源码、`META-INF/xposed`、Manifest、反射/DexKit 字符串、R8 规则、资源、Hook target 和进程逻辑的修改，都必须以本文件为约束。

更深目录存在 `AGENTS.md` 时，只补充对应子树规则，不得覆盖本文件的安全、兼容和验证边界。

## 项目谱系与定位

- 当前维护仓库：`tomthenpc/customiuizer-a13`
- 项目：CustoMIUIzer A13
- 目标系统：MIUI 14 / Android 13
- 上游功能语义基线：`MonwF/customiuizer v23.11.26`
- 运行 API：`minSdk=33`，`targetSdk=34`
- ABI：`arm64-v8a`
- applicationId：`tv.withaibuild.customiuizer.r13`
- libxposed：`minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
- Hot Reload：`false`
- Legacy Xposed API：`false`

A14 或其他仓库的代码、类名、Hook target、资源、Manifest、preference key、进程逻辑和 Android 13 行为差异只能作为工程方法参考，不能覆盖或替换 A13 当前独立仓库的已实现行为。

`compileSdk`、Build Tools、Gradle、AGP、Kotlin、依赖版本、versionName、versionCode 和签名配置必须从当前分支实时读取，不得照抄历史文档。

## 指令与任务文档优先级

优先级如下：

1. 用户本轮明确要求；
2. 本文件；
3. 当前任务文档；
4. 当前分支源码、Git 状态、构建配置和验证证据；
5. 仓库其他工程文档；
6. Git 历史和上游参考。

执行 A14 Claude 审计改动的 A13 适配任务时，必须读取：

```text
docs/A13_CLAUDE_PORTING_TASK.md
```

任务文档只定义当前适配范围；本文件记录长期工程规则。若文档与当前源码或验证证据冲突，以当前源码、当前分支、构建产物、日志和实机结果为准，并同步修正文档。

## 上游参考边界

遇到功能回归时，可对照 `MonwF/customiuizer v23.11.26` 确认原意，但：

- 当前独立仓库是修改和实现基线；
- 上游只用于确认原始行为和迁移前语义；
- 不得机械复制上游代码覆盖当前 Kotlin/Java 实现；
- 先判断差异是否来自独立包名、libxposed API、性能优化或有意重构；
- Release/R8 和实机验证是最终验收标准，上游行为不能替代。

A14 分支只能用于提取可跨版本成立的工程方法，例如：

- Java → Kotlin 控制流审计；
- 生命周期和弱引用治理；
- 热路径分配与装箱治理；
- 偏好加载和监听注册状态修复；
- Gradle 已废弃属性清理。

不得直接复制 A14 的 Android 14 类名、Hook target、资源名称、包名、Manifest、SystemUI tag 数据结构或版本闸门。

## 固定优先级

1. 实际可构建、可安装、可运行；
2. 功能和 Hook 行为正确；
3. `system_server`、SystemUI、Launcher 稳定；
4. MIUI 14 / Android 13 兼容；
5. API 101 基线稳定；
6. JVM、反射、ClassLoader、进程、R8 和 libxposed 兼容；
7. 生命周期与资源安全；
8. 性能、内存和功耗；
9. 可维护性；
10. Kotlin 覆盖率和形式简洁。

不得以低优先级目标交换高优先级目标。

## 核心原则

> 功能关闭时接近零额外成本；功能开启时只响应真实事件；高频路径避免不必要分配、重复反射、阻塞和日志；兼容代码限制在明确边界内。

额外成本主要取决于：

```text
触发频率 × 单次成本 × 进程数量 × 存活时间
```

代码变短不等于性能提高。

## 修改前

必须：

- 检查当前仓库、分支、HEAD、tracking、remote 和 `git status --short`；
- 检查 `git diff --stat` 和任务相关完整 diff；
- 阅读相关入口、调用链、测试、R8 规则和近期提交；
- 任务涉及 A14 Claude 审计适配时读取 `docs/A13_CLAUDE_PORTING_TASK.md`；
- 保护所有用户已有或来源不明的未提交工作；
- 先复现、建立静态证据或确认明确成本，再修改；
- 使用最小、完整、可解释、可验证的变更；
- 先运行最快的相关验证，再按风险扩大；
- 完成前重新检查完整 diff、HEAD、工作区和远端同步状态。

未经用户授权，不得执行：

- `git reset --hard`
- `git restore .`
- `git checkout -- .`
- `git clean -fd`
- force push
- 用远程旧代码覆盖当前工作树

不得提交 keystore、密码、token、真实 `keystore.properties`、私人日志、缓存、构建目录、本地 APK 或签名备份。

## Hook 与进程

- 未启用功能尽量不注册 Hook。
- 无关进程不初始化对应功能、DexKit、资源、缓存、线程或监听器。
- 注册 Hook、Receiver、Observer、Listener、Callback、Runnable、Coroutine、Executor 必须防重复。
- 释放和注销必须允许重复调用。
- ROM 目标不存在时只记录一次，安全禁用当前单项功能，不得高频重试。
- 入口层尽早按 Android 版本、包名和进程退出无关路径。
- 不改变 Hook target、priority、注册条件、注册顺序、before/after、参数修改、early return、result、throwable、`Chain.proceed()` 次数和回调次数，除非任务明确要求且有验证覆盖。
- 单项功能可以安全失败，但不能拖垮 SystemUI、Launcher 或 system_server。

## 热路径

绘制、动画、状态栏、控制中心、网络速度、触摸、通知绑定、音频回调及高频 SystemUI/Launcher Hook 中避免：

- 反射和 DexKit 搜索；
- 磁盘访问；
- 同步远程 Binder；
- 重复 SharedPreferences 读取；
- 临时数组、集合、Sequence、Pair/Triple、捕获 lambda；
- primitive key 装箱；
- Regex 编译；
- 重复字符串格式化；
- 大范围锁；
- 正常运行日志；
- 重复 API/ROM 判断；
- 重复资源名称解析和 Context 查找。

反射、解析、资源查找和兼容探测放到冷路径；热路径只读取已准备好的不可变、`volatile`、原子或安全发布状态。

## Kotlin/JVM 兼容

不要机械翻译 Java。必须保持：

- FQCN、构造器、重载、JVM descriptor、可见性和 primitive/boxed 类型；
- Hook target、priority、注册条件和顺序；
- before/after、参数修改、提前返回、result 和异常语义；
- ClassLoader、进程边界和初始化时机；
- Java/Kotlin 静态互操作；
- `@JvmStatic`、`@JvmField`、`@JvmName` 和必要 JVM 签名；
- static 字段/方法及初始化顺序；
- `synchronized`、`volatile`、atomic 和线程可见性语义；
- 反射、DexKit、字符串类名和动态方法查找；
- Manifest、authority、XML、preference key 和资源名；
- `META-INF/xposed/java_init.list`、`module.prop`、scope 和 libxposed metadata；
- Release/R8 可达性和 resource shrink 行为。

`MainModule.java`、`mods/utils/XposedHelpers.java` 等已评估的稳定 Java 边界继续保留 Java，除非用户另行启动独立迁移和实机验证阶段。

稳定 Java 边界可以保留。100% Kotlin 不是验收条件。

### Java → Kotlin 迁移回归核对（强制）

改动或审计任何由 Java 迁移而来的 Kotlin 文件时，必须先与迁移前的 Java 原版比对控制流。

先定位迁移提交：

```powershell
git log --follow -- app/src/main/java/<path>.kt
```

再检查迁移前 Java 中的循环控制语句：

```powershell
git show "<迁移commit>^:app/src/main/java/<path>.java" |
    Select-String -Pattern '\bbreak\b|\bcontinue\b' -Context 6,1
```

判定规则：

- Java `switch` 中的 `break` 在 Kotlin `when` 中消失属于正常；
- 循环体内的 `break` / `continue` 消失，一律先按回归处理；
- Kotlin 无法在 `use {}`、`forEach {}` 等普通 lambda 中直接完成 Java 循环的非局部跳出，机械迁移可能静默改变控制流；
- Java `String.split("\\|")` 等单字符快路径不得翻译成 `split("\\|".toRegex())`，避免每次调用创建 Regex/Pattern；
- 只读取 `first|second` 第一段时，优先使用 `indexOf('|')`、`regionMatches()` 或项目统一的 `PrefPair`；
- 热路径上的 `Map<Int, *>` / `Map<Long, *>` 必须优先评估 `SparseArray` / `LongSparseArray`，避免 primitive key 装箱；
- `SparseArray`、`SparseIntArray` 等非线程安全容器跨线程读取时，必须使用锁内更新、copy-on-write、`volatile` 安全发布或其他可证明的同步方案；
- 迁移后字段的 `@Volatile`、同步和线程可见性必须与原 Java 语义一致，不得因“通常只在初始化阶段写入”而无证据删除；
- 不使用 `runCatching`、深层 scope function、集合链或 Sequence 隐藏 Hook 热路径控制流；
- 迁移或优化后必须核对 Java 调用点、R8、反射入口和 Release 构建。

本轮 A13 适配中的已知案例记录在：

```text
docs/A13_CLAUDE_PORTING_TASK.md
```

重点对应：

- `PrefPair` 和 Regex 退化；
- SystemUI View 强引用；
- `ResourceHooks.java` 热路径与 `SparseArray` 安全发布；
- `MainModule.java` 偏好加载及监听注册状态。

## libxposed API 101/102

非 API 迁移任务不得顺带改变 API 版本。

当前固定兼容边界：

- API 101 为最低运行基线；
- 使用 API 102 编译；
- `minApiVersion=101`；
- `targetApiVersion=102`；
- 公共加载和 Hook 路径只依赖 API 101 已有能力；
- API 102 专属类型不得进入 API 101 必经类的字段、方法签名或静态初始化；
- API 102 专属逻辑集中在入口或冷边界；
- 不反射调用 libxposed API；
- 不混用 `de.robv.android.xposed`；
- Hot Reload、hook ID 和原子 replacement 保持关闭；
- API 101 验证结果不得冒充 API 102 实机证据。

## 生命周期与内存

每个长期资源必须有：

- 创建者；
- 所有者；
- 停止或注销路径；
- 防重复状态；
- 明确失效条件。

不得静态持有 Activity、Fragment、View、临时 Context 或临时 ClassLoader。缓存必须有容量、范围、生命周期或失效规则。UI 异步任务必须随生命周期取消。

使用弱引用时必须在注册和遍历阶段清理失效引用，不能只把强引用替换为 `WeakReference` 而继续永久积累空壳条目。

## 错误处理

- 修根因，不用大范围 `try/catch` 隐藏问题。
- 不用空实现、假返回、吞异常或禁用功能伪造成功。
- 不无限重试。
- Release 日志必须限流。
- 一次注册失败不得提前设置“已注册”状态。
- 一次空配置读取不得无证据永久设置“已加载”状态。
- 单项功能可以安全失败，但不能拖垮 SystemUI、Launcher 或 system_server。

## R8 与动态入口

删除、重命名、私有化或移动代码前检查：

- `META-INF/xposed`
- Manifest
- 反射和字符串类名
- DexKit
- XML 和动态资源
- JNI/native
- ProGuard/R8
- Java/Kotlin 静态入口
- preference key
- resource shrink 输出

不得为了通过构建无边界扩大 keep 规则。

## 设置 UI、Locale 与资源

设置应用变更必须验证：

- 日间/夜间主题；
- 状态栏和导航栏图标明暗；
- Toolbar、Preference title/summary、Switch、弹窗和 About 页面；
- 主页面、子页面、搜索、旋转、返回栈和 Fragment 重建；
- 应用内语言切换、跟随系统、配置变化和恢复；
- 普通、分享和打开方式选择器；
- BT/WiFi 列表；
- 资源收缩和多语言 fallback。

不得因为 Lint 或“未使用资源”报告直接批量删除资源。删除前必须搜索：

- XML 引用；
- 代码 `R.*`；
- `getIdentifier`；
- 反射和字符串名称；
- ROM/Xposed 动态访问；
- R8/resource shrink 输出。

行尾、格式化和资源清理应独立提交，避免掩盖真实行为差异。

## 变更纪律

- 没有实际收益证据，不修改稳定代码。
- 删除死代码必须证明不被动态引用。
- 不把日志级别、理论风险、A14 差异或代码形式直接判定为缺陷。
- 不混入无关依赖升级、工具链升级或大型架构替换。
- 高风险 Hook 基础设施修改必须独立提交。
- SystemUI 生命周期修复不得与资源 Hook 优化混成一个提交。
- 每个提交应对应单一根因，并允许独立回退。
- 完成前审查完整 `git diff`、`git diff --check` 和 `git status`。

## 仓库与分支

- 源码仓库：`tomthenpc/customiuizer-a13`
- 正常维护分支：`main`
- 用户明确指定开发分支时，该分支是唯一直接代码基线。
- 当前 A14 Claude 审计适配基线：`devin/r13.2-kotlin-api102`
- 不因为 `main` 是默认分支就切回 `main`。
- 不重新创建用途相同的平行开发分支。
- 不用 `main`、旧 Release、旧 APK 或上游覆盖当前开发分支。
- 未经用户明确要求，不创建或合并 PR，不合并 `main`，不创建 tag 或 Release，不公开上传 APK。
- 不恢复已清理的上游旧分支、旧 tag、备份、日志或阶段性报告。

## 验证

使用项目实际可用任务，不伪造任务或结果。

至少按风险覆盖：

- targeted/unit test；
- `test`；
- Debug 构建；
- Release 构建；
- R8/resource shrink；
- Lint、`lintRelease`、`lintVitalRelease`；
- applicationId、version、SDK、ABI；
- Xposed metadata、scope 和动态入口；
- API 101/102 边界；
- Legacy Xposed API 扫描；
- zipalign；
- APK 大小和 SHA-256；
- 实际签名证书 SHA-256。

涉及 Hook、入口、反射、R8、Manifest、资源、Locale、主题、Fragment 生命周期或 libxposed 时，必须增加 Release 和实机验证。

编译通过、Gradle 退出码为 0 或生成 APK，不等于目标进程可用。不能完成实机验证时，必须明确标注“待实机验证”。

纯文档变更至少执行：

```powershell
git diff --check
```

并检查：

- UTF-8；
- 相对链接；
- 文件名大小写；
- 最终 Git 状态。

没有修改源码、资源、构建配置、Manifest 或 Xposed 元数据时，不为形式完整重复生成已实机验证的 APK。

## 发布

代码应提交到用户指定的开发分支。未经用户明确确认：

- 不合并 `main`；
- 不 force push；
- 不创建或合并 PR；
- 不创建 tag；
- 不创建 GitHub Release；
- 不上传正式 APK。

用户明确要求更新远端时，可以完成当前已授权开发分支的 commit 和 push，并在推送后核对远端 HEAD。

正式签名配置必须位于仓库外部。缺少正式签名配置时，不得使用 Debug 证书或临时证书冒充 Release 成功。

最终报告必须区分：

- 已验证；
- 代码层面确认；
- 待实机验证；
- 无法确认。

不得声称未经测量的性能、内存或续航提升。

## 执行方式

以 Devin 自主执行为主，吸收 Claude 的审计纪律：

- 先陈述证据，再给判断；
- 使用当前源码、Git、构建、日志和实机结果作为主要证据；
- 计划简短，根据新证据动态调整；
- 对互不依赖的只读调查可并行处理；
- 主动寻找反证；
- 普通读取、搜索、编辑、测试、构建、commit 和已授权开发分支 push 不逐项询问；
- 长任务只在完成工程闭环、发现重要风险、改变路线或遇到硬阻塞时简短汇报；
- 不输出冗长命令流水账；
- 不把日志噪声、理论风险、上游差异或代码形式直接判定为 A13 缺陷；
- 修根因，不用吞异常、关闭检查、扩大 keep 范围或伪造成功快速收口；
- 最终区分：已验证、代码层面确认、待实机验证、无法确认；
- 不因为一次构建、一个 commit、一次 push 或一个阶段完成就提前停止。

不得因此降低自主执行力度，也不得变成只分析不修改。
