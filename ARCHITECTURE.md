# A13 架构

当前 production 运行时，不是阶段历史。

## 调用链

```text
LSPosed / libxposed
  → MainModule
  → PreferenceBootstrap
  → ProcessRouter / ProcessScope
  → Installer
  → A13 Contract / Resolver
  → Hook / Controller
  → owned runtime state
```

`MainModule` 在 `onPackageReady` / `onSystemServerStarting` 中按包名和进程名分发。
`MainModule.isSupportedAndroidVersion()` 限制为 Android 13（`Build.VERSION_CODES.TIRAMISU`）。

`PreferenceBootstrap` 准备进程内偏好快照。快照未就绪时不安装业务 Hook。

`ProcessRouter` 把包名和进程名解析成 `ProcessScope`。

各 `*Installer` 只安装本进程相关 Feature：
- `InputMethodInstaller`
- `SettingsInstaller`
- `SecurityCenterInstaller`
- `PowerKeeperInstaller`
- `WallpaperInstaller`
- `MediaInstaller`
- `PhoneInstaller`
- `PackageInstallerRouter`
- `GenericAppInstaller`
- `AndroidPackageInstaller`
- `SystemUiInstaller`
- `LauncherInstaller`
- `SystemServerInstaller`

Installer 检查对应 preference 标志后才安装；关闭功能不创建业务 Hook。
Hook 安装后按进程缓存；同一进程不重复安装。

## 平台

- MIUI 14 / Android 13 / SDK 33：主支持
- HyperOS 1 / Android 13：实验兼容，能力探测 + 安全跳过
- `minSdk=33`、`targetSdk=33`
- libxposed API 101 为最低运行基线；API 102 类型不得进入 API 101 必经生产路径
- `module.prop`：`minApiVersion=101`、`targetApiVersion=102`

## 生命周期与失败

- 关闭功能不创建业务 Hook、Receiver、Observer 或任务。
- 注册绑定进程级或实例级所有者；stale / replace / release 路径完整。
- 不静态强持有 Activity、View 或短生命周期 controller。
- 普通异常局部隔离；`OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError` 继续抛出。

## 热路径与缓存

热路径禁止磁盘 I/O、DexKit、重复反射、同步 Binder、Regex 重建、临时集合链、无界缓存和日志洪泛。
热路径只读预计算、不可变、原子或有界状态。
反射缓存按 ClassLoader 隔离且有界。

## A13 / A14 关系

A13 与 A14 共享产品意图、用户可见语义、缺陷描述、验收标准和可复用的纯逻辑测试思路。

A13 与 A14 不共享 ROM 目标合同、Android API 边界、ClassLoader 假设、生产分支、APK、签名、版本，以及“已实机验证”结论。
