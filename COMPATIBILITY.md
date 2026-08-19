# A13 兼容矩阵

## 平台

| 平台 | 状态 | 规则 |
|---|---|---|
| MIUI 14 / Android 13 | PRIMARY | 已有行为优先；回归必须阻断 |
| HyperOS 1 / Android 13 | COMPATIBILITY_WITH_EVIDENCE | 完整 target variant、能力探测、安全跳过、证据分级 |
| Android 14+ | UNSUPPORTED | 转到 A14 仓库 |

## libxposed

| 能力 | 状态 | 规则 |
|---|---|---|
| API 101 | 最低生产基线 | 所有必经路径可加载 |
| API 102 | 条件能力 | 类型和初始化隔离、能力探测、安全降级 |

## 证据等级

每个等级只记录其定义范围内的事实，不自动升级到下一级。

```text
STATIC_VERIFIED  : 源码 / ROM / DEX / resource / ABI 静态证据；
                    纯静态语义推理；
                    相关纯静态 / 单元测试；
                    不含有运行时或设备行为声明；
                    Android 编译成功不改变 STATIC 或 RUNTIME 声明。

BUILD_VERIFIED   : 相关 Gradle 编译 / lint / test / build 门禁通过；
                    APK assembly 仅在任务要求 APK 时才计入；
                    仍不含有设备运行时声明。

LOG_VERIFIED     : 来自相关 A13 目标环境的 LSPosed / logcat 日志支持该行为。

DEVICE_VERIFIED  : 在 A13 设备上直接验证该行为。

UNVERIFIED       : 只有推断，证据不足。
```

静态与构建证据分别记录：

```text
STATIC_VERIFIED = YES/NO
BUILD_VERIFIED  = YES/NO
```

- A14 设备证据不是 A13 设备证据。
- ROM 样本证据不是通用 ROM 支持。
- 静态扫描不能替代目标 ROM 实机验证。

## 跨版本移植

每个 `PORT` 任务只记录：功能、A14 来源、A13 状态、平台差异、证据。
禁止维护“所有文件逐行一致”的 parity 表。
A14 功能进入 A13 前必须通过 `docs/A13_PARITY.md` 记录并分类。
