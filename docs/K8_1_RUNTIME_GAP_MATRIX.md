# K8.1 A13 vs A14 r14.13.7 运行期修复差距矩阵

基线：
- A13：`devin/r13.5-k6-invariants` HEAD `17eac1d`
- A14：`r14.13.7` release `0b277609`

审计范围来自 A14 r14.13.7 关键修复：LSPosed service / preference mirror、设置未送达、DeviceInfoMonitor 配置、锁屏专辑图缓存/并发/过期、soft reboot 对 bind 状态的错误依赖。

## 差距矩阵

| 修复点 | A13 已有 | A13 缺失 | Android 14 专用 | 可安全回移 A13 | 本轮处理 |
|---|---|---|---|---|---|
| **1. PrefMap.getStringAsInt 解析缓存与异常回退** | `toIntOrNull` 已不抛 `NumberFormatException` | 缺少解析缓存；非 `String/Number` 类型统一返回 defValue，无法复用已解析结果；无 `Number` 快速路径 | 否 | **是** | 回移 |
| **2. AppLocaleController.toLocaleListCompat 区域 tag** | 用 `LocaleListCompat.create(Locale)`，丢失区域/脚本信息 | `pt-BR`、`zh-CN` 等带区域 tag 可能只保留语言 | 否 | **是** | 回移 |
| **3. 设置变更在 remotePrefs 未绑定时被丢弃** | 有 `MainActivity` 监听器直接写 remote | 未绑定时直接 `return`，后续无重放队列 | 否（A14 用 `PrefsMirror/PrefsMirrorState`，但队列思想通用） | **是** | 回移 |
| **4. LSPosed bind 状态机（TIMED_OUT、预算窗口）** | 仅有 `AppHelper.moduleActive` Boolean | 无状态机、无 `TIMED_OUT/UNKNOWN` 预算等待 | 是（A14 `XposedServiceManager` 架构） | 部分思想可借鉴，但类不可直接复制 | 跳过 |
| **5. Soft reboot / 重启菜单误依赖 `moduleActive`** | `PreferenceFragmentBase` 四项动作都先判断 `moduleActive` | 与设置进程 binder 状态耦合，服务未绑定时会拒绝可执行动作 | 否（A14 用 root/ordered broadcast，逻辑思想可回移） | 可移除 `moduleActive` 门限，但本轮回避 UI 测试缺口 | 记录为差距，后续处理 |
| **6. DeviceInfoMonitor 配置旧快照** | `SystemUIStatusBarHooks.MonitorDeviceInfoHook` 在 hook 时捕获 `showBatteryDetail`/`showDeviceTemp` | `handleMessage` 内部分格式/单位会重新读取，但顶层开关未在 tick 中刷新 | 是（A14 抽出 `DeviceInfoMonitor` 类） | 可改为 tick 内重新读取开关，但单元测试需 Android 环境 | 跳过 |
| **7. 锁屏专辑图缓存/并发/过期** | 专辑图逻辑内联在 `SystemUILockScreenHooks.kt` | 无独立 `AlbumArtPolicy`、`LockScreenAlbumArtController`，缓存策略和并发控制不明 | 是（A14 类结构与 Hook target） | 架构差异大，直接复制类会违反 A13 边界 | 跳过 |
| **8. BitmapCachedLoader DiscardOldestPolicy** | A13 使用 `AsyncTask`，无 `inFlight` 线程池排队 | 不存在 A14 的 `inFlight` / `LinkedBlockingQueue` 模型，因此也不存在同种“永久 loading” bug | 是（A14 线程池实现） | 不匹配 | 跳过 |

## 本轮回移的 3 项

1. `PrefMap.getStringAsInt` 解析缓存 + `Number` 快速路径 + 非 `String/Number` 安全回退。
2. `AppLocaleController.toLocaleListCompat` 对显式 tag 改用 `LocaleListCompat.forLanguageTags(tag)`，保留区域/脚本。
3. `AppHelper` / `MainActivity` 在 `remotePrefs` 未绑定时，将设置变更缓存并在绑定成功后 flush 到 remote，避免设置未送达。

## 明确跳过的 A14 专用项

- `XposedServiceManager.kt`、`PrefsMirror.kt`、`PrefsMirrorState.kt`：A14 自定义 binder 状态机与偏好镜像，A13 直接通过 `libxposed` `XposedServiceHelper` + `MainModule.watchPreferenceChange` 工作，结构和生命周期不同，直接复制会破坏 A13 入口。
- `DeviceInfoMonitor.kt`：A14 将状态栏设备信息抽出为独立 ticker；A13 内联在 `SystemUIStatusBarHooks` 中。可局部修复，但本轮已选 3 项，且需实机验证 UI 刷新，留待 K8.2/K9。
- `LockScreenAlbumArtController.kt` / `AlbumArtPolicy.kt`：A14 独立实现专辑图并发/缓存/过期；A13 代码在 `SystemUILockScreenHooks.kt` 中。类名和 Hook target 属于 Android 14 实现，不回移。
- `BitmapCachedLoader.kt` 线程池重写：A13 仍用 `AsyncTask`，不存在同种 `inFlight` 排队问题，不回移。
