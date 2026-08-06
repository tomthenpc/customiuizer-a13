# CustoMIUIzer A13（米客 A13）

简体中文 | [English](README_EN.md)

CustoMIUIzer A13 是面向 Android 13 的 MIUI / HyperOS 系统界面与交互定制模块，使用独立包名、版本线和 libxposed API。

## 核心功能

- 状态栏：时钟、日期、温度、网速、电池、信号和图标布局；
- 系统界面：控制中心、通知、音量、亮度、锁屏、媒体和充电信息；
- 桌面：图标、文件夹、Dock、最近任务、手势和动画；
- 系统行为：导航键、按键动作、电源菜单、浮窗、安装器、分享和应用权限。

## 兼容范围

- MIUI 14 / Android 13：主要兼容目标；
- HyperOS 1 / Android 13：正式兼容目标，以 Contract/Resolver 能力探测选择完整目标，不假设 ROM 内部结构与 MIUI 14 相同；
- `arm64-v8a`，applicationId `tv.withaibuild.customiuizer.r13`；
- libxposed `minApiVersion=101`、`targetApiVersion=102`；
- 不支持 Android 14 及以上版本。

已知实机基线：Redmi Note 11T Pro（`xaga`）、MIUI `V14.0.10.0.TLOINXM`、LSPosed 2.1.1 / [Vector v2.2](https://github.com/JingMatrix/Vector/releases/tag/v2.2)。HyperOS 1 / Android 13 仍需按 ROM 提供完整 LSPosed / Vector 日志验证。

## 构建与开发

需要 JDK 17、Android SDK 和 Python 3。开发门禁：

```bash
python tools/verify.py full
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
```

正式构建使用仓库外的 A13 专用签名配置；不得提交密钥、密码、令牌、APK 或本地签名配置。开发时保持关闭功能零后台成本、Hook 热路径低分配、Receiver/Observer 可释放，且不得吞掉 `OutOfMemoryError`。

- 当前正式版：`r13.10.1`（versionCode `135`）
- 用户下载：<https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases>
- 源码：<https://github.com/tomthenpc/customiuizer-a13>

本项目依据 GPL-3.0 分发，派生自 Mikanoshi/CustoMIUIzer，并参考 MonwF/customiuizer 的 Android 13 实现。
