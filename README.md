# CustoMIUIzer A13

简体中文 | [English](README_EN.md)

CustoMIUIzer A13 是面向 **MIUI 14 / Android 13** 的系统界面与交互定制模块，并为 **HyperOS 1 / Android 13** 提供基于能力探测的兼容路径。项目使用独立包名、版本线和现代 libxposed API。

- 当前版本：`r13.11.1`（versionCode `137`）
- 应用 ID：`tv.withaibuild.customiuizer.r13`
- 源码仓库：<https://github.com/tomthenpc/customiuizer-a13>
- 用户下载：<https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases>
- 实装框架：LSPosed / Vector

## 核心功能

- 状态栏时钟、日期、温度、网速、电池、信号与图标布局；
- 控制中心、通知、音量、亮度、锁屏、媒体与充电界面；
- Launcher 图标、文件夹、Dock、最近任务、手势与动画；
- 导航键、按键动作、电源菜单、浮窗、多窗口、安装器、分享与应用权限。

## 兼容范围

| 项目 | 支持范围 |
| --- | --- |
| 主要系统 | MIUI 14 / Android 13 |
| 兼容探测目标 | HyperOS 1 / Android 13，具体功能取决于 ROM 与系统应用版本 |
| SDK | minSdk 33 / targetSdk 34 |
| ABI | `arm64-v8a` |
| Xposed 框架 | LSPosed / Vector |
| 模块元数据 | `minApiVersion=101`、`targetApiVersion=102`、`staticScope=false` |

不支持 Android 14 及以上版本，也不建议与上游版或其他 CustoMIUIzer 派生模块同时启用。

已知实装基线：Redmi Note 11T Pro（`xaga`）、MIUI `V14.0.10.0.TLOINXM`。

## 运行架构

- SystemUI、Launcher、`system_server` 与普通应用入口按目标进程路由到独立 Installer，避免无关进程加载不属于自己的安装路径；
- Feature 使用稳定身份、进程范围、安装阶段和一次安装状态，关闭或不兼容的功能跳过无关注册与对象创建；
- ROM Contract、Resolver 与 Installer 共享目标选择结果，缺失目标时仅安全跳过受影响功能；
- Receiver、Observer、View 与控制器按所有者管理替换、失效和释放，降低重复注册与长期引用风险；
- 普通 ROM、反射和回调异常保持隔离，`OutOfMemoryError`、`ThreadDeath` 与 `VirtualMachineError` 不会被伪装成普通兼容失败；
- DeviceInfo 与 Launcher 高频路径减少重复 Binder、反射、I/O、配置读取和临时对象。

本版本变化见 [CHANGELOG.md](CHANGELOG.md)。架构、兼容与验证文档见 [DOCUMENTATION.md](DOCUMENTATION.md)。

`r13.11.1` 进一步加固设置页与应用选择器的异步生命周期，并优化状态栏时钟默认格式的高频刷新路径；同时延续按进程安装、ROM 兼容探测、异常边界和资源生命周期治理。

项目依据 GPL-3.0 分发，派生自 Mikanoshi/CustoMIUIzer，并参考 MonwF/customiuizer 的 Android 13 实现。
