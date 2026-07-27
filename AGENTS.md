# AGENTS.md

## 适用范围

本文件适用于整个仓库。源码、`META-INF/xposed`、Manifest、反射/DexKit 字符串、R8 规则、资源、Hook target 和进程逻辑的修改，都必须以本文件为约束。

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

## 上游参考边界

遇到功能回归时，可对照 `MonwF/customiuizer v23.11.26` 确认原意，但：

- 当前独立仓库是修改和实现基线；
- 上游只用于确认原始行为和迁移前语义；
- 不得机械复制上游代码覆盖当前 Kotlin/Java 实现；
- 先判断差异是否来自独立包名、libxposed API、性能优化或有意重构；
- Release/R8 和实机验证是最终验收标准，上游行为不能替代。

## 固定优先级

1. 实际可运行
2. 功能行为正确
3. API 101 基线稳定
4. API、ROM、ClassLoader 和 R8 兼容
5. 性能、内存和功耗
6. 可维护性
7. Kotlin 覆盖率和形式简洁

不得以低优先级目标交换高优先级目标。

## 核心原则

> 功能关闭时接近零额外成本；功能开启时只响应真实事件；高频路径避免不必要分配、重复反射、阻塞和日志；兼容代码限制在明确边界内。

额外成本主要取决于：

```
触发频率 × 单次成本 × 进程数量 × 存活时间
```

代码变短不等于性能提高。

## 修改前

必须：

- 检查当前分支、HEAD 和 `git status`
- 阅读相关入口、调用链、测试和 R8 规则
- 保护所有未提交工作
- 先复现或确认问题，再修改
- 使用最小、完整、可解释的变更

未经用户授权，不得执行：

- `git reset --hard`
- `git restore .`
- `git checkout -- .`
- `git clean -fd`
- force push
- 用远程旧代码覆盖当前工作树

不得提交 keystore、密码、日志、缓存、构建目录或本地 APK。

## Hook 与进程

- 未启用功能尽量不注册 Hook。
- 无关进程不初始化对应功能、DexKit、资源、缓存、线程或监听器。
- 注册 Hook、Receiver、Observer、Listener、Callback、Runnable、Coroutine、Executor 必须防重复。
- ROM 目标不存在时只记录一次，安全禁用当前单项功能，不得高频重试。
- 入口层尽早按 Android 版本、包名和进程退出无关路径。

## 热路径

绘制、动画、状态栏、控制中心、网络速度、触摸、通知绑定、音频回调及高频 SystemUI/Launcher Hook 中避免：

- 反射和 DexKit 搜索
- 磁盘访问
- 同步远程 Binder
- 重复 SharedPreferences 读取
- 临时数组、集合、Sequence、Pair/Triple、捕获 lambda
- 重复字符串格式化
- 大范围锁
- 正常运行日志
- 重复 API/ROM 判断

反射、解析和兼容探测放到冷路径，热路径只读取已准备状态。

## Kotlin/JVM 兼容

不要机械翻译 Java。必须保持：

- Hook target、priority、注册条件和顺序
- before/after、参数修改、提前返回和异常语义
- ClassLoader 与进程边界
- Java/Kotlin 静态互操作
- `@JvmStatic`、`@JvmField`、必要 JVM 签名
- 初始化时机和同步语义
- 反射、DexKit、字符串类名和动态方法查找
- Release/R8 行为

稳定 Java 边界可以保留。100% Kotlin 不是验收条件。

## libxposed API 101/102

非 API 迁移任务不得顺带改变 API 版本。

当前固定兼容边界：

- API 101 为最低运行基线
- 使用 API 102 编译
- `minApiVersion=101`
- `targetApiVersion=102`
- 公共路径只依赖 API 101 能力
- API 102 专属逻辑集中在冷边界
- 不反射调用 libxposed API
- 不混用 `de.robv.android.xposed`
- Hot Reload 保持关闭

## 生命周期与内存

每个长期资源必须有创建者、所有者、停止/注销路径和防重复状态。

不得静态持有 Activity、Fragment、View、临时 Context 或 ClassLoader。缓存必须有上限或明确生命周期。UI 异步任务必须随生命周期取消。

## 错误处理

- 修根因，不用大范围 `try/catch` 隐藏问题。
- 不用空实现、假返回、吞异常或禁用功能伪造成功。
- 不无限重试。
- Release 日志必须限流。
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

不得为了通过构建无边界扩大 keep 规则。

## 变更纪律

- 没有实际收益证据，不修改稳定代码。
- 删除死代码必须证明不被动态引用。
- 完成前审查完整 `git diff` 和 `git status`。
- 不混入无关依赖升级、工具链升级或大型架构替换。

## 仓库与分支

- 源码仓库：`tomthenpc/customiuizer-a13`
- 正常维护分支：`main`
- 开发工作在短期独立分支；需要审查时使用短期分支和 PR，合并后删除。
- 不恢复已清理的上游旧分支、旧 tag、备份、日志或阶段性报告。
- 不默认执行 `git reset --hard`、force push 或覆盖未提交修改的破坏性 Git 操作。

## 验证

使用项目实际可用任务，至少覆盖：

- 单元测试
- Debug 构建
- Release 构建
- R8/资源压缩
- Lint、`lintRelease`、`lintVitalRelease`
- APK 元数据、签名、大小、SHA-256

编译通过不等于目标进程可用。不能完成实机验证时，必须明确标注。

纯文档变更至少执行 UTF-8、相对链接和 `git diff --check`；没有修改源码、资源、构建配置、Manifest 或 Xposed 元数据时，不为形式完整重复生成已实机验证的 APK。

## 发布

代码应提交到短期独立分支。未经用户明确确认：

- 不合并 `main`
- 不 force push
- 不创建 tag
- 不创建 GitHub Release
- 不上传正式 APK

用户明确要求更新远端、发布或清理分支时，应完成提交、PR/合并、推送和最终远端复核。

最终报告必须区分已验证、未验证和需要实机测试的内容，不得声称未经测量的性能或续航提升。

## 执行方式

以 Devin 自主执行为主，吸收少量 Claude 式工作习惯：

- 先建立事实，再修改代码。
- 使用当前源码、Git、构建、日志和实机结果作为主要证据。
- 计划简短，根据新证据动态调整。
- 对互不依赖的只读调查可并行处理。
- 普通读取、搜索、编辑、测试、构建、commit 和已授权开发分支 push 不逐项询问。
- 长任务只在完成工程闭环、发现重要风险、改变路线或遇到硬阻塞时简短汇报。
- 不输出冗长命令流水账。
- 主动寻找反证，避免把日志噪声、理论风险或 A14 差异直接判定为 A13 缺陷。
- 修根因，不用吞异常、关闭检查、扩大 keep 范围或伪造成功快速收口。
- 最终区分：已验证、代码层面确认、待实机验证、无法确认。
- 不因为一次构建、一个 commit 或一次 push 就提前停止。
