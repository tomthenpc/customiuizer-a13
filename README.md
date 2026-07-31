# 米客 A13 Kotlin 重构

简体中文 | [English](README_EN.md)

面向 **MIUI 14 / Android 13** 的 CustoMIUIzer Kotlin 重构维护版。

本项目以 MonwF/customiuizer v23.11.26 作为 Android 13 功能语义参考，使用独立包名、版本线、签名和现代 libxposed API。项目不是上游官方版本，也不支持 Android 14 及更高版本。

## 当前版本

| 项目           | 值                                          |
| ------------ | ------------------------------------------ |
| 版本           | `r13.8.6`                                  |
| versionCode  | `131`                                      |
| 系统           | MIUI 14 / Android 13（API 33）               |
| ABI          | `arm64-v8a`                                |
| 应用 ID        | `tv.withaibuild.customiuizer.r13`          |
| libxposed    | `minApiVersion=101`、`targetApiVersion=102` |
| staticScope  | `false`                                    |
| APK          | `CustoMIUIzer-A13-r13.8.6.apk`             |
| APK SHA-256  | `ABF31CE311253AE863F7B2CEB87BF95140EE706EFF39ADA219033552B6FA7287`                           |
| 签名证书 SHA-256 | `C0EFF2DC4E662717195490DA78B12A984C6F2E6BD38ACF4EDAD14D53E3D22E70`                          |

面向 LSPosed 用户的下载页面位于：

`Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13` 

> Releases 页面仅保留当前正式版。旧版本的变更记录已合并到当前 Release 和 CHANGELOG；旧版 APK 不再提供下载，历史源码 tag 继续保留。

## r13.8.6 更新重点

* 将 A13 最新维护、Catalog、兼容诊断、作用域与 UI 修复统一合并到 `main`；
* 完善功能目录、进程目标、重启要求和 Hook 安装结果记录；
* 加强 Hook 目标解析、兼容回退、安装证据和异常诊断；
* 优化 Receiver、Observer、Step Counter、设备监控和锁屏专辑图的生命周期；
* 减少状态栏、通知、网速、电池、时钟和 Launcher 高频路径中的临时对象与重复计算；
* 状态栏网速保留系统字体家族，并支持双排网速行距调整；
* 修复设置文本样式继承与 About 页面文字换行；
* 统一 README、CHANGELOG、版本元数据和发布流程。

完整变化见 [CHANGELOG.md](CHANGELOG.md)。

## 兼容范围

| 项目          | 值                                                 |
| ----------- | ------------------------------------------------- |
| 系统          | MIUI 14 / Android 13                              |
| 主要设备        | Redmi Note 11T Pro / Pro+（`xaga`）                 |
| 参考 ROM      | `V14.0.10.0.TLOINXM`、`V14.0.7.0.TLOCNXM`          |
| 框架          | 实现 libxposed API 101 或 API 102 的 LSPosed / Vector |
| Android 14+ | 不支持                                               |

不同 ROM 的 SystemUI、Launcher 和系统应用实现可能存在差异，部分功能需要针对具体 ROM 适配。

## 功能范围

* 状态栏、电池、信号、网速、时钟、日期和温度；
* 控制中心、音量、亮度、通知和系统动画；
* 锁屏、充电信息、媒体界面、快捷操作和专辑图；
* Launcher、最近任务、文件夹、图标、Dock 和桌面手势；
* 导航栏、按键、自定义动作、电源菜单、浮窗和 Tasker；
* 应用权限、安装器、分享、隐藏应用和应用锁行为。

## 安装

1. 从 LSPosed 发布仓库下载正式 APK；
2. 安装 APK；
3. 在 LSPosed / Vector 中启用模块并确认建议作用域；
4. 打开一次模块设置；
5. 完整重启设备。

使用不同签名的早期构建不能直接覆盖安装。遇到签名不一致时，请先备份设置，再卸载旧版。

## 构建

需要 JDK 17 和 Android SDK API 36。

```bash
./gradlew :app:assembleRelease
```

Release 构建必须使用仓库外的正式签名配置。不得提交 keystore、密码、令牌或本地构建文件。

## 验证说明

`r13.8.6` 已完成正式 Release APK 构建及以下基础检查：

* APK v2 签名；
* zipalign；
* applicationId、versionCode、versionName；
* libxposed module.prop、scope.list 和 java_init.list；
* APK SHA-256 与签名证书校验。

本次发布未执行完整单元测试、Lint、工程 Audit 或全功能实机回归。构建与 APK 校验不能证明所有功能在全部 MIUI 14 ROM 上均可用。

## 反馈

提交问题时请提供：

* 模块版本与 APK 来源；
* 设备和 ROM 版本；
* SystemUI、Launcher 等系统应用版本；
* LSPosed / Vector 版本；
* 实际作用域；
* 复现步骤和完整日志。

## 许可证与致谢

项目派生自 Mikanoshi/CustoMIUIzer，并参考 MonwF/customiuizer 的 Android 13 工作，依据 GPL-3.0 分发。
