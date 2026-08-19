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

`MainModule.isSupportedAndroidVersion()` 限制为 Android 13（`Build.VERSION_CODES.TIRAMISU`）。

`MainModule.onPackageReady` 是包进程唯一入口，顺序为：
版本判定 → `lpparam.isFirstPackage()` → `ProcessScopes.resolve(pkg, processName)` →
`ProcessScopes.isRejected(...)` → `PreferenceBootstrap.resolveRemote()` →
`PreferenceLoadRegistry.shouldLoad(remote, pkg)` → `PreferenceBootstrap.start()` →
按 `ProcessScope` 的 if-chain 进入 installer。

`MainModule.onSystemServerStarting` 是独立入口，**不经过** `ProcessScopes`，直接调用
`SystemServerInstaller`。`ProcessScope.SYSTEM_SERVER` 因此不参与 `onPackageReady` 分发。

`PreferenceBootstrap` 准备进程内偏好快照。只有在监听器注册成功并完成第二次
`getAll()` 之后才进入 `LOADED` / `VALID_EMPTY`。快照通过 `PrefMap.replaceSnapshot`
原子发布；偏好变化只更新快照，不改变任何已安装 Hook 的安装状态。

`ProcessScopes.resolve` 把包名和进程名解析成 `ProcessScope`，是路由的主要来源。
不可安装的 scope（`UNSUPPORTED`、`SYSTEM_UI_PLUGIN`、`SETTINGS_REMOTE`、
`SECURITY_CENTER_REMOTE`、`SECURITY_CENTER_BOOTAWARE`）在进入 installer 前被拒绝。

共 13 个 installer，每个只服务自己的进程：
`AndroidPackageInstaller`、`GenericAppInstaller`、`InputMethodInstaller`、
`LauncherInstaller`、`MediaInstaller`、`PackageInstallerRouter`、`PhoneInstaller`、
`PowerKeeperInstaller`、`SecurityCenterInstaller`、`SettingsInstaller`、
`SystemServerInstaller`、`SystemUiInstaller`、`WallpaperInstaller`。

Installer 检查对应 preference 标志后才安装；关闭功能不创建业务 Hook。

安装存在两条并行路径：

- **catalog 路径**：`FeatureDispatcher` → `FeatureInstallRegistry` → `FeatureSpec`，
  由 `HookTargetContract` / `HookTargetResolver` 完成候选解析，`HookInstaller.withSession`
  保证 installer 只能使用 resolver 选定的 variant。install-once 由
  `FeatureInstallRegistry` 按 (进程标识, canonical feature id) 保证。
- **legacy 直接 Hook 路径**：installer 直接调用 `mods` 中的 Hook 函数，
  不经过 contract/resolver，install-once 依赖 `lpparam.isFirstPackage()`。

因此 install-once 保证是**部分性**的，仅 catalog 路径由注册表强制。
当前 installer 分布：4 个 HYBRID、9 个 LEGACY_DIRECT、0 个纯 catalog。
完整测绘见 `docs/audit/A13_F1_ARCHITECTURE_BASELINE.md`。

## 平台

- MIUI 14 / Android 13 / SDK 33：主支持
- HyperOS 1 / Android 13：实验兼容，能力探测 + 安全跳过
- `minSdk=33`、`targetSdk=33`
- libxposed API 101 为最低运行基线；API 102 类型不得进入 API 101 必经生产路径
- `module.prop`：`minApiVersion=101`、`targetApiVersion=102`

## 生命周期与失败

- 关闭功能不创建业务 Hook、Receiver、Observer 或任务。
- Receiver 与 preference observer 通过 `ModuleHelper` 的 owner 绑定原语管理，
  使用弱引用 owner 与 PENDING_REGISTER / ACTIVE / STALE / RELEASED 状态机。
- 不静态强持有 Activity、View 或短生命周期 controller。
- 普通异常局部隔离；`OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError` 继续抛出。
- `RuntimeFatality.throwIfFatal` 是致命错误判定的 canonical 实现。

## 热路径与缓存

热路径禁止磁盘 I/O、DexKit、重复反射、同步 Binder、Regex 重建、临时集合链、无界缓存和日志洪泛。
热路径只读预计算、不可变、原子或有界状态。
反射缓存按 ClassLoader 隔离且有界。

## A13 / A14 关系

A13 与 A14 共享产品意图、用户可见语义、缺陷描述、验收标准和可复用的纯逻辑测试思路。

A13 与 A14 不共享 ROM 目标合同、Android API 边界、ClassLoader 假设、生产分支、APK、签名、版本，以及“已实机验证”结论。
