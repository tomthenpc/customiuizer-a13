# AGENTS.md — CustoMIUIzer A13

本文件是 `devin/a13-runtime-hardening` 分支的本地执行规则。

> 禁止执行 ADB、APK 构建、`assemble`、`package`、`bundle`、`install`、`sign`、`publish`、`officialRelease`、`lintVitalRelease` 或任何实机自动化；只允许 `python tools/verify.py`、`python tools/check-invariants.py`、`python tools/analyze_lsposed_log.py` 及单元测试等静态/本地验证。

## 项目边界

- Android 13 / MIUI 14
- 唯一分支：`devin/a13-runtime-hardening`
- 仓库：`tomthenpc/customiuizer-a13`
- applicationId：`tv.withaibuild.customiuizer.r13`
- libxposed：`minApiVersion=101`，`targetApiVersion=102`
- 禁止 Legacy Xposed API

## 编码原则

- 关闭功能时不注册后台组件；
- 开启功能时只响应真实事件；
- 高频 Hook 禁止重复反射；
- 高频 Hook 禁止集合链和临时数组；
- 兼容 fallback 集中管理；
- Receiver、Observer 必须有明确注销方；
- Application Context 可进程级持有；
- Activity、View、Controller 不得静态强持有；
- Hook 时序不得因文件拆分改变；
- 不为减少代码行数牺牲可读性。

## 当前验证方式

只进行：

- 静态不变量检查
- 针对性单元测试
- Kotlin 编译
- Java 编译
- 必要的 Debug Lint
- 用户导出的 LSPosed 详细日志离线分析

明确不进行：

- ADB 自动化
- adb logcat
- adb shell
- dumpsys
- 自动安装
- 自动重启
- Tasker
- UI 自动化
- 设备 PSS 自动采集
- 实机截图

说明：

- 用户不会与 Devin 同步进行 ADB 实机测试；
- 不得等待用户连接设备；
- 实机结果只能标记为「待 LSPosed 日志验证」；
- 不得声称完成实机回归。

## 禁止构建 APK

禁止执行包含以下任务名称的 Gradle 任务：

- `assemble`
- `package`
- `bundle`
- `install`
- `sign`
- `publish`
- `officialRelease`
- `lintVitalRelease`

禁止 R8、zipalign、签名、APK 上传和 Release。

## Agent 阅读顺序

进行运行时代码任务时，按以下顺序阅读：

1. `AGENTS.md`
2. 相关 `installers/*Installer.java`
3. 相关 `mods/utils/FeatureDispatcher.kt` 与 `FeatureDefinition.kt`
4. 目标 Hook 或 Controller 文件
5. 对应单元测试
6. `docs/A13_RUNTIME_HARDENING.md` 中对应组件的当前状态

不要默认读取：

- `README.md` / `README_EN.md`（仅用户安装说明）
- `CHANGELOG.md` / `CHANGELOG_EN.md`（仅历史记录）
- 旧 Release 记录
- 全部 `strings.xml`
- 全部 `docs/`（只读指定的 `A13_RUNTIME_HARDENING.md` 等）

## 搜索纪律

先按进程和组件限定目录，再搜索精确类/方法/Preference key：

1. 猜测目标组件（`system_server`、`com.android.systemui`、`com.miui.home`、Settings 等）。
2. 使用 `installers/` 的 `*Installer` 文件确认 feature 到 Hook 的映射。
3. 使用 `PrefMap.kt` 或 `R.xml.*_pref_` 确认 preference key。
4. 只有调用链不清楚时才全仓搜索；全仓搜索后记录关键路径，减少下一次搜索。
5. 对 Xposed 字符串、DexKit 目标和 `META-INF/xposed` 使用 `grep` 后必须核对 `ProGuard/R8` keep 规则。

## 基础纪律

- 用户本轮明确要求 > 本文件 > 源码与验证证据。
- 以当前分支 HEAD、构建产物和 LSPosed 日志为最终依据。
- 修改前确认分支、`git status`、相关 diff 与调用链。
- 改动最小、完整、可解释、可验证。
- 未启用功能不注册 Hook；无关进程不初始化对应功能。
- 单项功能可安全失败，不得拖垮 `system_server`、SystemUI、Launcher。
- 高频路径不反射、不 I/O、不阻塞、不分配临时集合、不重复格式化。
- 删除/重命名前检查 R8、反射、DexKit、Manifest、preference key、资源和 `META-INF/xposed` 可达性。
- 每个提交单一目的，完成后检查 `git diff --check`。

## 完成与推送

- 所有本地任务完成、工作树干净后再输出最终报告。
- 只推送 `devin/a13-runtime-hardening`，禁止 force push。
- 未完成实机验证不得标记为「已验证」。
