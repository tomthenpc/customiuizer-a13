# A13 兼容矩阵

## 平台

| 平台 | 级别 | 规则 |
|---|---|---|
| MIUI 14 / Android 13 | 主支持 | 已有行为优先，回归必须阻断 |
| HyperOS 1 / Android 13 | 实验兼容 | 完整 target variant、能力探测、安全跳过、证据分级 |
| Android 14+ | 不支持 | 转到 A14 或更高版本仓库 |

## 跨版本移植状态

每个 `PORT` 任务只记录以下结果：

| 功能 | A14 来源 | A13 状态 | 平台差异 | 证据 |
|---|---|---|---|---|
| 示例 | commit/文件 | backlog/active/done | API/ROM/生命周期 | static/log/device |

禁止维护“所有文件逐行一致”的 parity 表。

## 证据

- `STATIC_VERIFIED`：静态规则、编译、测试；
- `BUILD_VERIFIED`：APK 实际构建；
- `LOG_VERIFIED`：A13 目标 ROM LSPosed 日志；
- `DEVICE_VERIFIED`：A13 设备实际行为；
- `UNVERIFIED`：只有推断。
