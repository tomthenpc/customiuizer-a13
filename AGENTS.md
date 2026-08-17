# AGENTS.md — CustoMIUIzer A13

共同工程规则，适用于 ChatGPT、Cursor、Devin、Codex 和其它 Agent。

## 1. 产品边界

- Android 13 / API 33。
- 主支持：MIUI 14 / Android 13。
- 实验兼容：HyperOS 1 / Android 13。
- 不支持 Android 14+；Android 14+ 功能见 A14 仓库。
- `applicationId`：`tv.withaibuild.customiuizer.r13`
- ABI：`arm64-v8a`
- libxposed：`minApiVersion=101`、`targetApiVersion=102`
- A13 与 A14 不共享 APK、签名、运行时库或生产分支。

## 2. 优先级

```text
correctness
> compatibility
> lifecycle
> stability
> maintainability
> memory/performance
> repository cleanliness
> elegance
```

工程风格：高性能、高稳定、高兼容、低内存、低占用。
少层、少状态、少框架、少重复、少魔法、少后台行为。直接、明确、有界、可验证。

不为微优化增加框架。不重做 PrefMap、ResourceHooks、ProcessRouter、PreferenceBootstrap。
不把整个 Java 树 Kotlin 化。不用 Compose 或新 DI。

## 3. 控制权

```text
用户本轮明确要求
> 当前 active 任务合同
> AGENTS.md
> ARCHITECTURE.md / COMPATIBILITY.md / docs/A13_PARITY.md
> 当前源码、测试、构建和日志证据
> Git 历史
```

`ROADMAP` 只决定优先级。`tasks/completed/` 和 `docs/rom-intelligence/**`、`docs/audit/**` 是证据，不具有当前控制权。

## 4. A13 / A14 关系

- A14 是工程 / 产品语义参考，不是代码移植来源。
- 所有 A14 功能进入 A13 前必须通过 `docs/A13_PARITY.md` 记录并分类。
- 灵动额头 / Dynamic Island 明确排除，不进入 A13。
- FEATURE PARITY != CODE PARITY：每个 A14 功能回移前必须独立审计：
  - A13 上游语义意图
  - Android 13 API 边界
  - MIUI 14 目标 ABI
  - HyperOS 1 / Android 13 变体证据
  - 进程 / ClassLoader
  - 生命周期
  - 资源 / preference 映射
  - 失败语义
- 不得整文件盲拷、混用两版 target 成员或把 A14 实机结果当成 A13 证据。

## 5. Git

- `devin/a13-*` 为 Agent 工作分支，`main` 为长期稳定线。
- 从 exact SHA 创建分支，不用移动分支名作为基线。
- 禁止 force push、破坏性 reset、rebase 已公开历史、无差别 `clean`。
- 不为 Review 建立平行分支。不覆盖未知工作区修改。
- 普通任务可以 commit；push / merge main / tag / release 必须由当前任务合同明确授权。
- 最终报告记录 Base SHA、Final SHA 和 commits。
- 用户不需要人工核对 HEAD。

## 6. 运行时规则

- 无关进程不初始化无关 Feature。
- Feature 同一进程只安装一次；preference 变化不得把已安装 Hook 重置为未安装。
- 关闭功能不创建业务 Hook、Receiver、Observer 或任务。
- Hook 时序、参数改写和 `chain.proceed()` 次数必须保持。
- 回调最外层使用项目既有安全边界；普通异常局部隔离。
- `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError` 不得吞掉。
- Receiver / Observer / listener / controller 必须绑定所有者，并有替换、失效、释放闭环。
- 不静态强持有 Activity、View 或短生命周期 controller。
- 反射、DexKit、磁盘 I/O 和同步 Binder 留在冷路径。
- 热路径只读已准备好的不可变或原子状态。
- 缓存有界、按 ClassLoader 隔离。

## 7. 生命周期 / 所有权

- 每个 Hook、Receiver、Observer、View 和 controller 有明确所有者。
- 安装、替换、失效、释放路径完整。
- 部分安装需要终端失败状态；不得无界重试。
- 删除/改名必须核对 Manifest、R8、反射、DexKit、资源和 preference key。

## 8. 热路径 / 缓存

热路径禁止：
- 磁盘 I/O
- 同步阻塞
- 重复反射
- DexKit
- Regex 重建
- 临时集合链
- 无界缓存
- 日志洪泛

热路径只读：
- 预计算状态
- 不可变常量
- 原子或有界状态

反射缓存按 ClassLoader 隔离且有界。

## 9. Java / Kotlin

- 行为保持优先于迁移数量。
- Java→Kotlin 必须小批量、可回退，并与功能变化分开提交。
- 允许利用 null safety、sealed class、extension、inline 等降低错误面。
- 热路径避免多层 lambda、隐式装箱、临时集合和难审计 DSL。
- 不以减少代码行数为目标。
- 默认保留 Java：`MainModule.java`、`XposedHelpers.java`、`MemberUtilsX.java`。

## 10. 上游语义规则

修改已有 A13 功能语义前，按顺序检查：

1. MonwF/customiuizer 当前上游（相关部分）
2. A13 初始 / 历史工作实现
3. 当前 A13 实现
4. A14 参考实现

分类：
- `UPSTREAM_EXACT`
- `UPSTREAM_INTENT_EQUIVALENT`
- `A13_COMPAT_VARIANT`
- `SEMANTIC_DRIFT`
- `A14_NEW_FEATURE`
- `DEAD_UPSTREAM_PATH`

优先级：
- 已有 A13 上游用户意图 > A14 实现形状（对已有功能）。
- 真正全新 A14 功能：A14 用户可见合约为产品参考，但 A13 target 必须独立证明。

## 11. 验证

开发中：

```powershell
python tools/verify.py fast --changed
```

针对性测试：

```powershell
python tools/verify.py fast --tests <TestClassName>
```

收口：

```powershell
python tools/verify.py full
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
git diff --check
```

文档专用任务不运行 Android 编译，除非 verify.py 因非文档控制文件改动而要求。
失败在原任务内修复。不通过删测试、降断言或吞异常制造通过。

## 12. 构建与发布

- Debug / develop 构建不得冒充正式版。
- 正式 Release 仅在用户明确要求、仓库外 A13 专用签名配置有效时执行。
- 禁止提交 APK、keystore、密码或本地签名配置。
- GitHub Actions 不得硬编码本机路径，不得使用正式 keystore。
- ROM 样本、trace、mapping、profiler 数据不得入库。
- 版本名与 `CHANGELOG` 必须同步；`versionCode` 必须单调增加。

## 13. ROM 证据与实机分级

- 静态扫描不能替代目标 ROM 实机验证。
- 无实机证据不得修改成熟热路径基础设施。
- 候选缺陷必须可复现，并有内存 / 线程 / 日志 / CPU 证据。
- “这里可以 cache” 不构成缺陷。

证据等级：

```text
STATIC_VERIFIED  : 静态规则、编译、单元测试
BUILD_VERIFIED   : APK 实际构建
LOG_VERIFIED     : A13 目标 ROM LSPosed 日志
DEVICE_VERIFIED  : A13 设备实际行为
UNVERIFIED       : 仅推断
```

A14 设备证据 != A13 设备证据；ROM 样本证据 != 通用 ROM 支持。
