# AGENTS.md — CustoMIUIzer A13 执行规则 v2

## 1. 身份与职责

本仓库的长期架构和任务合同由用户与 ChatGPT 决定。

Devin 的职责是：

- 按当前任务合同实现；
- 自行定位普通编译和测试问题；
- 在原任务内持续修复；
- 运行 A13 门禁；
- 按需构建 APK；
- 给出最终 diff、验证和产物证据。

Devin 不再创建独立 Review、Implement、Audit 或 HEAD 核对任务。

## 2. 平台边界

- Android 13 / API 33；
- 主支持：MIUI 14 / Android 13；
- 实验兼容：HyperOS 1 / Android 13；
- applicationId：`tv.withaibuild.customiuizer.r13`；
- ABI：`arm64-v8a`；
- libxposed：`minApiVersion=101`、`targetApiVersion=102`；
- Android 14+ 不属于本仓库；
- A13 与 A14 不共享 APK、签名、运行时库或生产分支。

## 3. 控制权

```text
用户本轮明确要求
> 当前 active 任务合同
> AGENTS.md
> PROJECT.md / ARCHITECTURE.md / WORKFLOW.md
> 当前源码、测试、构建和日志证据
> Git 历史
```

`ROADMAP.md` 只决定优先级。`tasks/completed/` 不具有当前控制权。

## 4. 单任务闭环

```text
读取任务
→ 定位最小调用链
→ 形成可执行假设
→ 直接实现
→ 针对性验证
→ 修复
→ 完整验证
→ 按需构建
→ 最终报告
```

禁止把上述步骤拆成多个任务。

已有明确修改路径时，不得继续无边界审计。普通不确定性由执行者通过源码、测试和
最小实验解决，不要求用户逐步确认。

## 5. Git 规则

任务开始自动记录：

```powershell
git branch --show-current
git status --short
git rev-parse HEAD
git fetch origin
```

- 不在长期文档写死分支或 HEAD；
- 不为 Review 建立平行分支；
- 不覆盖未知工作区修改；
- 不使用 force push、`reset --hard`、无差别 `clean`；
- 当前任务分支可以正常 commit 和已授权 push；
- 最终报告记录 Base SHA、Final SHA 和 commits；
- 用户不需要人工核对 HEAD。

## 6. A13 兼容策略

### MIUI 14 / Android 13

- 是稳定主基线；
- 已有可用行为优先；
- HyperOS 兼容不得降低 MIUI 14 稳定性；
- Hook 时序、参数改写、调用次数和失败语义不得随意改变。

### HyperOS 1 / Android 13

- 采用完整 target contract/variant；
- resolver 与 installer 使用同一 resolved target；
- 一次只安装一套完整候选；
- 缺少目标时安全跳过；
- ROM/profile 判断留在冷路径；
- 未有目标设备证据时只标记“待实机”。

### A14 回移

A14 只能作为语义和候选实现参考。回移必须检查：

- API 34 类型、权限、资源和常量；
- 类、方法、参数、进程和生命周期；
- ClassLoader 和反射可达性；
- A13 对应 preference 和资源；
- 安全失败能力；
- A13 独立测试与门禁。

不得整文件盲拷、混用两版 target 成员或把 A14 实机结果当成 A13 证据。

## 7. 性能与安全

- 关闭功能接近零成本；
- 无关进程不初始化无关功能；
- 热路径无磁盘 I/O、同步阻塞、重复反射、Regex 重建和临时集合链；
- 反射、DexKit 和目标解析在冷路径；
- 缓存有界并按 ClassLoader 隔离；
- Receiver、Observer、listener、View 和 controller 有所有者与释放闭环；
- 不静态强持有 Activity/View；
- 单项功能异常不得拖垮 SystemUI、Launcher 或 `system_server`；
- 普通异常可隔离，`OutOfMemoryError` 继续抛出；
- 删除/改名必须核对 Manifest、R8、反射、DexKit、资源和 preference key。

## 8. Java/Kotlin

- 行为保持优先于迁移数量；
- Java→Kotlin 使用小批量、可回退提交；
- 迁移和行为重构原则上分开；
- 允许利用 null safety、sealed class、extension、inline 等降低错误面；
- 热路径避免多层 lambda、隐式装箱、临时集合和难审计 DSL；
- 不以减少代码行数为目标。

## 9. 验证

开发中：

```powershell
python tools/verify.py fast --changed
```

有针对性测试时：

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

文档专用任务不运行 Android 编译。失败时在原任务内修复，不通过删测试、降断言或
吞异常制造通过。

## 10. 构建

任务明确要求 APK 且验证通过后，允许：

```powershell
.\gradlew.bat :app:assembleDebug
```

正式 Release 仅在用户明确要求且仓库外 A13 专用签名配置有效时执行。禁止：

- 用 Debug 签名冒充正式版；
- 提交 APK、keystore、密码和本地签名配置；
- 自动创建 Tag/Release；
- 自动公开上传产物。

## 11. 完成定义

- 任务目标实际完成；
- 验收标准逐项有证据；
- 相关门禁实际通过或明确分类；
- 没有未解释改动；
- 最终 diff 已交付审查；
- 需要 APK 时给出实际路径和 SHA-256；
- 实机状态分为 `STATIC_VERIFIED`、`BUILD_VERIFIED`、`LOG_VERIFIED`、
  `DEVICE_VERIFIED` 或 `UNVERIFIED`；
- 最终报告简洁，不复述无价值命令流水。
