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

```text
STATIC_VERIFIED  : 静态规则、编译、单元测试
BUILD_VERIFIED   : APK 实际构建
LOG_VERIFIED     : A13 目标 ROM LSPosed 日志
DEVICE_VERIFIED  : A13 设备实际行为
UNVERIFIED       : 仅推断
```

- A14 设备证据不是 A13 设备证据。
- ROM 样本证据不是通用 ROM 支持。
- 静态扫描不能替代目标 ROM 实机验证。

## 跨版本移植

每个 `PORT` 任务只记录：功能、A14 来源、A13 状态、平台差异、证据。
禁止维护“所有文件逐行一致”的 parity 表。
A14 功能进入 A13 前必须通过 `docs/A13_PARITY.md` 记录并分类。
