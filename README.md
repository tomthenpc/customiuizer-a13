# 米客 A13 · Kotlin 重构版

[简体中文](README.md) | [English](README_EN.md)

米客 A13（CustoMIUIzer A13）Kotlin 重构版是面向 MIUI 14 / Android 13 的独立维护版 Xposed 模块，功能语义以 [MonwF/customiuizer v23.11.26](https://github.com/MonwF/customiuizer) 为上游基线。

当前维护线以 Kotlin 为主体完成工程重构，同时保留经过审计的稳定 Java/JVM 边界；“Kotlin 重构版”不表示 100% Kotlin。

本仓库保存完整源码、构建配置、工程文档和验证记录。面向 LSPosed 用户的安装与下载说明位于 [Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13](https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13)。

## r13.7.0

`r13.7.0` 是 A13 工程追平与稳定性治理后的首个正式版本：

- 将包命名空间统一为 `tv.withaibuild.customiuizer`，保持应用 ID `tv.withaibuild.customiuizer.r13`；
- 完成 System、SystemUI、Launcher 大文件拆分和 Kotlin 迁移，并用迁移审计固定 Hook 注册顺序、参数与调用次数；
- 为异步 Receiver、Observer、Listener、Handler 和 Runnable 增加模块异常边界与所有权治理；
- 修复 RemotePreferences 初始化、监听注册、弱引用清理和 additional instance field 生命周期问题；
- 优化设备信息监控、应用图标加载、设置搜索、AudioVisualizer 与锁屏专辑图的队列、缓存和失效边界；
- 保持 libxposed API 101 最低运行基线，并以 API 102 元数据发布。

完整变更见 [CHANGELOG.md](CHANGELOG.md)，英文版见 [CHANGELOG_EN.md](CHANGELOG_EN.md)。

## 兼容范围

| 项目 | 值 |
|---|---|
| 系统 | MIUI 14 / Android 13（API 33） |
| 主要设备 | Redmi Note 11T Pro / Pro+（`xaga`） |
| 参考 ROM | `V14.0.10.0.TLOINXM`、`V14.0.7.0.TLOCNXM` |
| ABI | `arm64-v8a` |
| 应用 ID | `tv.withaibuild.customiuizer.r13` |
| libxposed | `minApiVersion=101`、`targetApiVersion=102`、`staticScope=false` |
| 建议框架 | LSPosed 2.x / Vector 2.x |

其他 MIUI 14 / Android 13 版本可能可用，但不同 ROM 的 SystemUI、Launcher 和系统应用签名可能存在差异。Android 14 及更高版本不在本仓库支持范围内。

## 主要功能

- 状态栏、电池、信号、网速、时钟、日期和温度；
- 控制中心、音量、亮度、通知和系统动画；
- 锁屏、充电信息、媒体界面、快捷操作和专辑图；
- 桌面、最近任务、文件夹、图标、Dock、抽屉和桌面手势；
- 导航栏、按键、自定义动作、电源菜单、浮窗和 Tasker；
- 应用权限、安装器、分享、隐藏应用、应用锁及其他 MIUI 行为。

具体功能是否生效取决于设备、MIUI 版本、系统应用版本和启用的作用域。

## 安装

1. 从 [LSPosed 发布仓库](https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases) 下载正式 APK。
2. 安装 APK，在 LSPosed / Vector 中启用建议作用域。
3. 打开模块设置一次，并完整重启设备。
4. 按功能组逐项启用和验证；遇到异常时先关闭对应功能，再导出 LSPosed 日志。

早期使用不同签名的 A13 构建不能直接覆盖安装。若系统提示签名不一致，请先备份设置，再卸载旧版本。

## 构建

需要 JDK 17 和 Android SDK API 36。项目未固定 `buildToolsVersion`，由当前 AGP 与本机 SDK 选择兼容的 Build Tools。

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release / Develop 构建必须使用仓库外的 `../keystore.properties` 指向 A13 正式签名。缺失正式签名时，打包任务会明确失败，不会回退到 Debug 证书。

推荐的完整本地门禁：

```bash
python tools/check-invariants.py
python tools/audit-system-migration.py --baseline-ref backup/r13-k5-before-system-java-removal
./gradlew clean :app:test :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease
./gradlew :app:lintVitalRelease --rerun-tasks
```

Release 构建启用 R8 与资源压缩。正式发布还会校验 zipalign、v2 签名、证书、APK 元数据、Xposed 元数据、Legacy Xposed API 和最终 SHA-256。

## 工程边界

- A13 是独立运行基线；A14 仅用于参考工程方法，不能提供 A13 Hook target 或 ROM 事实。
- 稳定的 Java/JVM 边界继续保留，Kotlin 覆盖率不是发布条件。
- 构建、Lint、R8 和签名属于静态验证，不能代替设备与 ROM 行为验证。
- 当前版本不启用 Legacy Xposed API、Hot Reload、hook ID 或原子 replacement。

## 反馈

请在本仓库提交 issue，并附上模块版本、设备与 ROM、系统应用版本、LSPosed/Vector 版本、实际作用域、复现步骤和完整日志。包名出现在系统日志中不等于模块因果问题，需要同时提供 Hook 失败、模块栈或崩溃上下文。

## 许可证与致谢

项目基于 [Mikanoshi](https://github.com/Mikanoshi) 与 [MonwF](https://github.com/MonwF) 的 CustoMIUIzer，按 [GPL-3.0](LICENSE) 许可证发布。
