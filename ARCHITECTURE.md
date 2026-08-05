# A13 架构

## 控制面

```text
User goal
  ↓
ChatGPT task contract
  ↓
Devin implementation + build
  ↓
A13 verification gates
  ↓
ChatGPT final diff review
  ↓
same-task fixes
```

## 运行时边界

```text
Preference / FeatureDefinition
        ↓
Process Dispatcher / Installer
        ↓
A13 Contract / Resolver
        ↓
Hook / Controller
        ↓
MIUI 14 or HyperOS 1 / Android 13 target
```

兼容判断集中在 Contract、Resolver、Installer 或边界 Adapter。业务 Hook 回调不应
散布 ROM 分支。

## 冷路径

- 进程和 ROM 识别；
- ClassLoader 目标解析；
- 反射成员查找；
- DexKit；
- preference 初始化；
- 有界缓存准备；
- 日志诊断。

## 热路径

- 只读已准备状态；
- 常数时间判断；
- 必要参数或 View 修改；
- 无磁盘 I/O；
- 无同步 Binder；
- 无重复反射；
- 无临时集合链；
- 无高频格式化和日志洪泛。

## A13/A14 关系

A13 与 A14 共享：

- 产品意图；
- 用户可见语义；
- 缺陷描述；
- 验收标准；
- 可复用的纯逻辑测试思路。

A13 与 A14 不共享：

- ROM 目标合同；
- Android API 边界；
- ClassLoader 假设；
- 生产分支；
- APK、签名和版本；
- “已实机验证”结论。
