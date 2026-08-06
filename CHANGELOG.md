# Changelog

简体中文 | [English](CHANGELOG_EN.md)

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
