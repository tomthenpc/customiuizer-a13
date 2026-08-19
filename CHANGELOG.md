# Changelog

简体中文 | [English](CHANGELOG_EN.md)

## r13.12.0 — 2026-08-19

`versionCode 138`，面向 MIUI 14 / Android 13，并为 HyperOS 1 / Android 13 提供能力探测兼容。

### 新增功能

- USB 默认用途沿用现有 A13 Hook，并映射 A14 的模式选项。
- 安装器净化，以及隐藏应用详情页的举报入口。
- 桌面 Dock 高度调节，以及隐藏输入法关闭按钮。
- 状态栏若干视觉增强（时钟/指示相关）。
- 文件夹可关闭模糊，同时保留原有透明度设置。
- 设置备份升级为 V2 格式，并继续可恢复旧备份。
- 自定义电池指示条颜色读取已有 colorval 偏好；息屏变暗比例按 0–99% 滑条写入 AOSP 字段。

### 修复

- 自定义动作/桌面手势保存后可能回到“无动作”：无效或未知动作值会规范为合法默认值 1，保存时不再因 Spinner 越界丢失选择。
- 桌面手势设置页补齐“重启桌面”菜单，与 Launcher 进程中的手势 Hook 安装范围一致。
- USB 默认用途在拔线后重新插上会再次应用。

### 稳定性与兼容性

- 选择器结果只在当前有效 Fragment 生命周期内回传。
- 继续按进程安装 Feature；关闭功能不创建业务 Hook。
- 63 项 ROM/设备相关能力仍保持证据门，不在无设备条件下猜测实现。

### 性能优化

- 无新增设备性能数值声明。既有时钟与手势热路径保持有界状态缓存，本版本未做推测性微优化。

### 架构与维护

- 完成 A13 架构与 A14 产品能力对照的静态收口：已有能力按 A13 ABI 独立证明，灵动额头不进入 A13。

### 验证状态

- 静态门禁、单元测试、Release 编译、Lint、R8 与签名产物检查在发布前执行。
- DEVICE_VERIFIED = NO
- LOG_VERIFIED = NO
- 无 ADB，不声明实机或 MIUI/HyperOS 运行时已验证。

### 产物信息

- 文件：`CustoMIUIzer-A13-r13.12.0.apk`
- versionName：`r13.12.0`
- versionCode：`138`
- SHA-256：将在签名产物审计后回填。

## r13.11.1 — 2026-08-08

`versionCode 137`，基于已完成的 Android 13 性能、生命周期与构建治理阶段发布。

### 核心变化

- 加固 SubFragment 延迟滚动生命周期，在 View 销毁时取消待执行回调，避免对失效 View 执行延迟操作。
- 加固 AppSelector 异步应用列表加载，通过 application context、输入快照和 owner 清理减少 Activity / View 生命周期耦合。
- 加固 ActivitySelector 异步加载，只允许结果在当前有效 View 生命周期内提交，同时保持界面重新创建时重新查询的既有行为。
- 优化状态栏时钟默认格式高频路径，缓存稳定的格式转换结果和资源解析结果，减少每次时间更新时的重复处理。
- 保持原有系统时间格式、秒钟、12/24 小时制、AM/PM、前导零与自定义格式行为。
- 完成 Android 13 Release 编译、单元测试、Lint、R8、严格依赖校验、签名和核心实机加载验证。

### 兼容说明

- MIUI 14 / Android 13 仍为主要目标。
- HyperOS 1 / Android 13 的部分 SystemUI 定制取决于具体 ROM 内部类和系统应用版本；目标不存在时仅影响对应功能，不阻止模块其他功能加载。
- 不对尚未完成设备指标采样的内存、CPU、GC 或耗电改进做数值声明。

## r13.10.1 — 2026-08-06

`versionCode 135`，面向 MIUI 14 / Android 13、HyperOS 1 / Android 13 兼容探测、`arm64-v8a` 与 libxposed API 101/102。

### 核心变化

- 将 SystemUI、Launcher、`system_server` 与普通应用入口拆分为按进程路由的 Installer，并以稳定 Feature 身份、进程范围、安装阶段和一次安装状态避免无关加载与重复安装。
- 加固启动早期偏好快照、空快照、并发加载和安装失败状态，防止偏好更新将已安装 Hook 错误重置或触发重复安装。
- 完善 MIUI 14 与 HyperOS 1 / Android 13 的环境识别、Hook Contract、目标解析和 variant 选择；缺失目标时只跳过受影响功能，不跨候选混装。
- 统一 Hook、反射、Receiver、Observer、延迟回调与诊断边界：普通兼容异常继续隔离，直接或包装的 `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError` 保持传播。
- 完善 Receiver、Observer、View、Handler 与控制器的所有者、替换、失效和释放路径，并移除选择器主线程中的阻塞等待。
- 加固来电不中断、移除安全窗口、截图临时隐藏悬浮窗、通知/分享浮窗、小窗与多窗口限制等回调路径；同步整理状态栏、控制中心、音量、锁屏和设置相关 Hook 的安装与兼容边界。
- Launcher 动画缩放改用直接 API，HotSeats 手势按 View 缓存密度、触摸阈值与状态，FSG 回调复用安装阶段解析的 `BaseRecentsImpl` Class。
- DeviceInfo 使用固定缓冲区和逐字节 sysfs 解析，减少周期性 `Properties`、`RandomAccessFile`、Binder 查询和临时对象，同时保持采样与失败退避行为。
- 增加运行期不变量、Hook 合同、源码危害、ROM 兼容、Release 编译、单元测试、Lint、R8 与依赖完整性门禁。
- 升级构建工具链与依赖校验，清理未引用的 `miui.jar` 和异常 external annotation，并规范共享忽略规则。

### 验证范围

- 当前代码通过 Python 与 Gradle 静态门禁、Release Kotlin/Java 编译、Release 单元测试、Release/Vital Lint、R8、严格依赖校验、Manifest 与 Xposed 元数据检查。
- 已知实装基线为 Redmi Note 11T Pro（`xaga`）、MIUI `V14.0.10.0.TLOINXM`。HyperOS 1 的具体功能可用性取决于 ROM 与系统应用版本。

## r13.9.2 — 2026-08-01

- 锁屏专辑图所属 View 脱离后取消未完成任务，并释放模块背景、单帧缓存和静态处理结果；
- 设置页切换动画调整为 `350ms`；
- 开关复用所在行的按压状态，并移除逐次创建的透明度动画；
- LSPosed 模块列表使用独立简洁摘要。

### 历代核心实现总结

A13 版本线建立了独立包名和 Android 13 维护线，完成 libxposed API 101/102 兼容、System/SystemUI/Launcher 领域拆分、分批 Kotlin 化、资源与偏好 Hook 加固、生命周期治理、有界缓存、可取消异步任务、致命异常边界与 Contract/Resolver 兼容诊断。细节保留在 Git commits 和历史 tags 中。
