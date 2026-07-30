# r13.7.1 剩余审计：Broadcast / Reboot / Security Spoofing / A14 误移植

> 审计基线：`devin/r13.7.1-maintenance-foundation@a483120`（最终干净提交）  
> 审计分支：当前在 `devin/r13.8-maintenance-architecture` 上追加记录  
> 方法：静态 grep、调用链阅读、与 `customiuizer-a14-forDevin/hardening/a14-lts-foundation` 关键差异对比

---

## 1. Broadcast / Receiver 审计

### 1.1 注册路径

- 模块内所有动态 `BroadcastReceiver` 集中通过 `ModuleHelper` 的两条路径注册：
  - `registerModuleReceiver(key, ...)`：模块级，以 `moduleReceivers` 持有，支持 `unregisterModuleReceiver(key)` 清理。
  - `registerOwnedReceiver(owner, key, ...)`：以 `WeakReference<Object>` 持有 `owner`，重复注册同 `key` 时自动清理旧 `owner` 失效的条目。
- 这两条路径在 `ModuleHelper.releaseReceiver` 中统一 `try { ...unregisterReceiver(...) } catch (_)` 兜底，防止重复释放崩溃。

### 1.2 直接注册点

- `SystemStatusBarClockAndMoreHooks`：`clockScreenAndTimeReceiver` 使用匿名 `BroadcastReceiver`，但内部由 `ModuleHelper.guarded` 兜底，持有 `WeakReference<controller>`， screen-off 时调用 `stopSecondTimer`。
- `StepCounterController`：在 `mContext` 上注册/注销 `screenReceiver`，并在 `onDestroy`/`screen-off` 路径清理，符合生命周期（已在本次 `r13.7.1` 生命周期优化中处理）。
- `Controls`、`GlobalActions`、`Various`、`SystemLockScreenMoreHooks`、`SystemUIStatusBarHooks` 等通过 `ModuleHelper.registerOwnedReceiver` 或 `registerModuleReceiver` 注册，并有对应的 `unregister` 调用。
- `subs/BTList.kt`、`subs/WiFiList.kt` 在 `onDestroy` 显式 `unregisterReceivers()`。

### 1.3 发现

- **未发现匿名 Receiver 永久泄漏**：所有匿名 `BroadcastReceiver` 要么由 `ModuleHelper` 统一管理，要么在对应 `onDestroy`/`screen-off` 中释放。
- **未发现 `RECEIVER_EXPORTED` 缺失**：`ModuleHelper` 调用时显式传入 `flags`（调用点已阅读 `GlobalActions`、`Controls` 等，均使用 `Context.RECEIVER_NOT_EXPORTED` 或 `RECEIVER_EXPORTED`）。
- **未发现重复注册未去重**：`registerOwnedReceiver` 会按 `owner` 清理旧条目，`registerModuleReceiver` 会先 `unregisterModuleReceiver(key)` 再注册。

---

## 2. Reboot / 系统级操作 审计

- 搜索 `reboot\(`、`PowerManager.reboot`：在 `app/src/main/java/**` 中**未找到任何主动调用 `reboot()` 的代码**。
- `PowerManager` 仅用于以下非危险用途：
  - `PowerManager.goToSleep(...)`（关屏，由 `Controls` 手势触发）。
  - `PowerManager.isInteractive()` / `isScreenOn()` 读取。
- `Runtime.getRuntime().exec(...)` 仅出现在 `Helpers.kt` 的 `reboot` 辅助文本？实际未出现。`BitmapCachedLoader.kt:168` 的 `exec` 属于 `ThreadPoolExecutor?`，已确认不是 `Runtime.exec`。

---

## 3. Security / Spoofing 审计

### 3.1 身份/Build 伪造

- 搜索 `Build\.(MODEL|MANUFACTURER|BRAND|DEVICE|FINGERPRINT|PRODUCT|HARDWARE)`、`setStatic.*Build`、直接修改 `Build` 字段：
  - 仅在 `MainModule` 和 `Helpers` 中读取 `Build.VERSION.SDK_INT`，用于版本判断。
  - 未发现任何对 `Build.*` 的身份字段写入或伪造。
- `Settings.(System|Secure|Global)` 读写集中在功能自身需要的键：
  - `Settings.System.putLong("systemui_restart_time")`：用于检测 SystemUI 重启（`MainModule` 据此做 10 秒保护）。
  - `Settings.Global` 以 `HookUtils.modulePkg + ".foreground.package"`、`.foreground.fullscreen`、`.fw.apps` 为键，用于模块内部状态同步。
  - `Controls`/`GlobalActions` 修改 `pick_up_gesture_wakeup_mode`、`volumekey_wake_screen`、`is_fingerprint_active`、`torch_state` 等，均为用户显式开启的功能控制，非伪造。
  - `System_AirplaneModeConfig` 修改 `airplane_mode_radios` 和 `airplane_mode_toggleable_radios`，属于“飞行模式雷达”功能。

### 3.2 签名/权限 伪造

- `PackagePermissions.kt` 是本模块中唯一的安全相关伪造点：
  - 为模块包名伪造 `ApplicationInfo.isSystemApp()`、`isSignedWithPlatformKey()` 返回 `true`。
  - 在 `PermissionManagerServiceImpl.shouldGrantPermissionBySignature`、`PackageManagerServiceUtils.verifySignatures` 中为模块包名返回 `true`。
  - 在 `ActivityRecordInjector.canShowWhenLocked` 中返回 `true`。
  - 将模块包名加入 `MiuiDefaultPermissionGrantPolicy.MIUI_SYSTEM_APPS`。
- **判定**：这是模块必需的签名/权限兼容机制，已有明确边界（仅作用于 `systemPackages`，且 `systemPackages` 初始化时只加入 `HookUtils.modulePkg`）。功能语义与 `r13.7.1` 之前一致，**未在本次改动中引入新增伪造**。

---

## 4. A14 误移植检查

### 4.1 方法

- 在 A13 源码中搜索 A14 特有 API 或版本门控：
  - `UPSIDE_DOWN_CAKE`、`VERSION_CODES.U`、`SDK_INT >= 34`、`targetSdk.*34`。
- 将 `customiuizer-a13-forDevin` 与 `customiuizer-a14-forDevin/hardening/a14-lts-foundation` 的关键入口文件（`MainModule`、`PackagePermissions`、`SystemStatusBarClockAndMoreHooks`、`StepCounterController`）做差异对比，确认 A13 分支保留 A13/MIUI 14 边界。

### 4.2 发现

- **唯一疑似 A14 版本门控**：`Helpers.kt:214-216`：

  ```kotlin
  @JvmStatic
  fun isUPlus(): Boolean {
      return Build.VERSION.SDK_INT >= 34
  }
  ```

  - 该函数在 A13 源码中**没有被调用**（`grep isUPlus\(\)` 仅命中声明自身）。
  - 在 `Build.VERSION.SDK_INT == 33` 的 A13 设备上永远返回 `false`，属于无害死代码。
  - **结论**：可能是 A14 审计迁移时遗留的兼容函数，不影响当前 A13 行为；建议后续整理阶段删除或转换为注释化前瞻代码，但**不阻塞 r13.7.1 发布**。

- **未发现 A14 专属 Hook target、类名、资源名或包名**：
  - 无 `com.android.systemui.*` 的 Android 14 特有类（如 `MiuiJellyfish` 系列）引用。
  - 无 A14 `Build` 字段伪造。
  - 无 A14 特定 `Settings` 键或服务名写入。

### 4.3 与 A14 分支的边界确认

- `MainModule.java` 中 `isSupportedAndroidVersion()` 严格限定为 `Build.VERSION_CODES.TIRAMISU`（33），未引入 34 路径。
- `libxposed` API 边界仍为 `minApiVersion=101`、`targetApiVersion=102`，未使用 A14 特有 API。
- 已审阅的 `SystemStatusBarClockAndMoreHooks`、`StepCounterController`、`PackagePermissions` 均保留 A13/MIUI 14 类名和字段，未出现 A14 重构带来的新反射字符串。

---

## 5. 审计结论

- **无 P0 阻断问题**。
- **无 P1 高风险漏洞**：无主动 `reboot()`、无 Build 伪造、无匿名 Receiver 泄漏、无高频安全伪造。
- `PackagePermissions` 的签名/权限兼容是预期行为，作用域明确，已在本次 `FeatureCatalog` 迁移中保持原条件和调用顺序。
- 唯一可改进项是 `Helpers.isUPlus()` 为未调用死代码，可后续清理，不影响 r13.7.1 稳定性。

---

## 6. 后续建议（r13.8+）

- 在 `r13.8-maintenance-architecture` 中把 `FeatureCatalog` 逐步扩展到更多功能，减少对 `MainModule` 的直接条件分发。
- 通过 `PreferenceSchema` 逐步覆盖剩余 617 个 XML 中的 key，并在 Schema 稳定后考虑把条件检查迁移到 Schema 元数据。
- 将 `HookTargetResolver` 接入最不稳定反射点（`MiuiStatusBarClockController` 等）前，先在单元测试中补充进程内 ClassLoader 模拟。
- 将 `DiagnosticRecorder` 接入 `FeatureCatalog` 安装入口，记录每个 feature 的 `REQUESTED/COMPATIBLE/INSTALLED/DEGRADED` 状态，实现可观测的限流诊断。
