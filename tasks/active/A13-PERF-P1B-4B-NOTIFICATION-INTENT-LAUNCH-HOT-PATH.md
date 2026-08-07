# A13-PERF-P1B-4B — Notification Intent Launch Hot-Path Reduction

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-4B-NOTIFICATION-INTENT-LAUNCH-HOT-PATH` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `7f0f37c` |
| 状态 | `QA_CONDITIONAL` |
| blockers | - |
| QA-1 | `COMPLETED` |
| 终点 commit | 待填入 |
| P0 真实运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |
| 授权范围 | 仅 `MiuiStatusBarNotificationActivityStarter#startNotificationIntent` 及该回调直接调用、属于本模块的通知点击启动辅助逻辑 |

## 目标

在不改变 MIUI 14 / HyperOS 1 Android 13 现有通知点击启动、PendingIntent 发送、锁屏/解锁流程、原方法调用次数和偏好生效语义的前提下，降低 `startNotificationIntent` 回调中的重复稳定反射、临时对象分配、重复状态查询和重复 preference 解析。

本轮只处理 `MiuiStatusBarNotificationActivityStarter#startNotificationIntent` 路径，不处理 `NotificationRowMenuHook` / `createMenuViews`。

## 授权修改范围

- `startNotificationIntent` `before` 回调中的稳定反射查询。
- 回调中的重复 `MainModule.mPrefs` 读取和复合配置解析。
- 同一次点击中重复获取的 NotificationEntry、Row、StatusBarNotification、PendingIntent 和包名。
- 已确认缺失的稳定目标在每次点击时重复反射和重复日志。
- 功能关闭时仍执行的模块通知启动逻辑。

## 禁止范围

- 不修改 `NotificationRowMenuHook` 或 `createMenuViews`。
- 不修改通知菜单项、顺序、图标、文字、显示逻辑。
- 不修改通知排序、过滤、折叠、分组、重要性。
- 不修改通知监听服务。
- 不修改 Quick Settings、状态栏、锁屏、Launcher、AudioService。
- 不修改管理应用 UI。
- 不新增通知启动功能。
- 不扩大包、进程或 ROM 支持范围。
- 不支持 Android 14 或 HyperOS 2。
- 不修改签名配置。
- 不引入新的缓存框架、线程、监听器或依赖。
- 不缓存 `NotificationEntry`、`ExpandableNotificationRow`、`StatusBarNotification`、`Notification`、`PendingIntent`、`Intent`、`Context`、`ActivityStarter` 等短生命周期 Android owner 实例。

## 验收标准

- 稳定反射元数据不在每次 `startNotificationIntent` 点击时重复解析。
- 同一次点击中的 Entry、Row、SBN、PendingIntent 和包名不重复查询。
- 不缓存任何通知、Row、PendingIntent、Intent、Context 或 ActivityStarter 实例。
- 原方法调用次数和行为保持。
- `PendingIntent` 不重复发送。
- 锁屏和解锁流程保持。
- 异常降级保持。
- 偏好生效语义保持。
- 功能关闭时通知启动模块逻辑尽早退出。
- Hook call site 不增加，进程覆盖范围不扩大。
- 不新增线程、Handler、listener、observer、锁、缓存或依赖。
- 新增/扩展 JVM 测试覆盖指定场景。
- 更新 `a13_hook_cost_scan.py` 回归门禁并重新生成 `A13_HOOK_COST_MAP.*`。
- 全部 Python 测试、Android 编译测试、lint、R8 和正式 Release 签名通过。
- 工作区干净并推送。
- 不声明未经真机测量的性能收益。

## 最终状态约束

- 工程完成状态只能是 `ENGINEERING_COMPLETE_DEVICE_EVIDENCE_PENDING`。
- P0 真实运行时基线继续为 `RUNTIME_BASELINE_PENDING_DEVICE`。
- 不自动开始下一阶段。
