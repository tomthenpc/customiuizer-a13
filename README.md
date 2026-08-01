# CustoMIUIzer A13（米客 A13）

简体中文 | [English](README_EN.md)

CustoMIUIzer A13 是面向 Android 13 的 MIUI / HyperOS 系统界面与交互定制模块，基于 CustoMIUIzer 历史功能语义持续维护，采用独立包名、版本线和 libxposed API。

## 核心功能

- 状态栏：时钟、日期、温度、网速、电池、信号与图标布局；
- 系统界面：控制中心、通知、音量、亮度、锁屏、媒体与充电信息；
- 桌面：图标、文件夹、Dock、最近任务、手势与动画；
- 系统行为：导航键、按键动作、电源菜单、浮窗、安装器、分享和应用权限。

## 兼容范围

- MIUI 14 / Android 13：主要兼容目标；
- HyperOS 1 / Android 13：正式兼容目标，使用独立 Contract/Resolver 能力探测，不假设 ROM 内部结构与 MIUI 14 相同；
- ABI：`arm64-v8a`；
- applicationId：`tv.withaibuild.customiuizer.r13`；
- libxposed：`minApiVersion=101`、`targetApiVersion=102`；
- Android 14 及以上不属于本项目范围。

已知实机基线为 Redmi Note 11T Pro（`xaga`）、MIUI `V14.0.10.0.TLOINXM`、LSPosed 2.1.1。HyperOS 1 / Android 13 的候选目标必须通过能力探测并继续以 LSPosed 详细日志验证；静态验证不等同于全功能实机回归。

## 构建与验证

需要 JDK 17、Android SDK 和 Python 3。常规开发验证：

```bash
python tools/verify.py full
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
```

正式 Release 构建使用仓库外的 A13 专用签名配置：

```bash
./gradlew :app:assembleRelease
```

不得提交 keystore、密码、令牌、APK 或本地签名配置。

## 源码与开发

- 源码仓库：<https://github.com/tomthenpc/customiuizer-a13>
- 用户下载：<https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases>
- 当前正式版：`r13.9.1`（versionCode `132`）
- 开发约束：关闭功能零后台成本；高频 Hook 不重复反射、不阻塞并减少临时分配；Receiver/Observer 必须幂等且可清理；普通异常隔离，`OutOfMemoryError` 必须继续抛出。

提交兼容问题时，请附设备、ROM、SystemUI/Launcher 版本、框架版本、实际作用域、复现步骤和完整 LSPosed 日志。

本项目依据 GPL-3.0 分发，派生自 Mikanoshi/CustoMIUIzer，并参考 MonwF/customiuizer 的 Android 13 实现。
