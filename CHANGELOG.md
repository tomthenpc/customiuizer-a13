# Changelog

## r13.2.2-devin（语言切换修复 + P1 export flag + A13/A14 差距矩阵）

### 修复

- 修复应用内语言从 English 切换为“跟随系统”后不能立即恢复系统中文的问题。
- 根因：`AppHelper.getLocaleContext` 直接使用 `context.resources.configuration` 并 `setLocale`，修改了应用原始 `Resources` 的 `Configuration` 对象；当后续选择“跟随系统”并返回原始 `Context` 时，其 `Resources` 仍保留上次手动语言（English）。
- 改为使用 `Configuration(context.resources.configuration)` 先复制一份配置再设置 locale，与 A14 实现一致，避免污染原始 `Context` 的 `Configuration`。

### 新增工程资产

- 新增 `docs/A13_A14_PARITY_MATRIX.md`：对照 A14 `r14.13.5`/`devin/r14.13-kotlin-refactor` 的 22+ 领域差距矩阵、适用/改写/不适用判定、A13 Java/Kotlin 文件统计与分层迁移建议。
- 更新 `docs/ARCHITECTURE_AUDIT_A13.md` 与 `docs/DEVIN_R13_CHECKPOINT.md` P1 状态。

### P1 registerReceiver export flag

- 扫描 A13 全部动态 `registerReceiver`，为 11 处未指定 export flag 的调用显式添加 `Context.RECEIVER_EXPORTED` 或 `RECEIVER_NOT_EXPORTED`。
- 判定原则：系统广播用 `RECEIVER_NOT_EXPORTED`，模块自定义跨进程广播用 `RECEIVER_EXPORTED`；保持 Receiver 成对注销与 hook 注册顺序不变。
- 涉及文件：`Controls.java`、`GlobalActions.java`、`Launcher.java`、`System.java`、`Various.java`、`StepCounterController.kt`、`WiFiList.java`。

### 验证

- `:app:test`、`:app:lintDebug`、`:app:lintRelease`、`:app:lintVitalRelease`、`:app:assembleDebug`、`:app:assembleRelease` 通过；
- Release APK 使用 A13 正式签名 v2，zipalign 对齐通过；
- APK SHA-256：`62ED16BEFE47144548D1C862B640396FDD163D3B19897126DFE9F7BF75276AD1`；
- 签名证书 SHA-256：`C0:EF:F2:DC:4E:66:27:17:19:54:90:DA:78:B1:2A:98:4C:6F:2E:6B:D3:8A:CF:4E:DA:D1:4D:53:E3:D2:2E:70`；
- `module.prop`、`scope.list`、`java_init.list` 元数据正确；
- applicationId、Xposed metadata、API 边界保持不变；
- 未合并 main、未创建 tag 或 GitHub Release。

### 架构审计

- 新增 `docs/ARCHITECTURE_AUDIT_A13.md`，覆盖入口/生命周期、PrefMap、ResourceHooks、ModuleHelper、XposedHelpers 与 Phase 3 模式统计。
- P0：0 项（构建与核心 Hook 未发现阻断问题）。
- P1：已修复。全部动态 `registerReceiver` 显式指定 `RECEIVER_EXPORTED` 或 `RECEIVER_NOT_EXPORTED`。
- P2：`ResourceHooks.mReplaceHook` 热路径 `findContext()` 开销待评估。
- P3：`toRegex()`（`AppHelper.kt`、`PreferenceAdapter.kt`）与 `System.java:2094` 的 `forEach(new Consumer())` 可优化；用户指示“卡了跳过”，已记录待后续处理。

## r13.2.1-devin（UI 回归修复）

### 修复

- 修复 Kotlin 迁移后自定义 Preference 类默认样式丢失导致的两个 UI 回归：
  - 字体 / textAppearance 与重构前不一致；
  - 开关（SwitchPreference/CheckBoxPreferenceEx）widget 不可见。
- 根因：迁移后的 `@JvmOverloads` 主构造使用 `defStyleAttr = 0`，导致 `SwitchPreference`、`Preference`、`EditTextPreference`、`ListPreference`、`DropDownPreference`、`PreferenceCategory` 等子类没有使用 `androidx.preference` 主题中定义的 `switchPreferenceStyle`、`preferenceStyle` 等默认样式属性，布局和 widget 未能正确初始化。
- 将上述自定义 Preference 的默认 `defStyleAttr` 恢复为 `androidx.preference.R.attr.*` 对应值，与 Java 原版的二参构造语义一致。

### 验证

- `:app:test`、`:app:lint`、`:app:assembleRelease` 通过；
- Release APK 使用 A13 正式签名 v2，zipalign 对齐通过；
- applicationId、Xposed metadata、API 101/102 边界保持不变；
- 未合并 main、未创建 tag 或 GitHub Release。

## r13.2.0-devin（构建现代化与新签名）

### 包名与版本

- `versionCode` 升级到 `118`，`versionName` 升级到 `r13.2.0-devin`。
- 目标分支 `devin/r13.2-kotlin-api102`。

### 构建系统

- 迁移到 Kotlin DSL 和 Gradle version catalog（`gradle/libs.versions.toml`）。
- 工具链：AGP 8.7.2 + Gradle 8.9 + Kotlin 2.0.21 + JDK 17。
- 通过 `enforcedPlatform(kotlin-bom)` 强制统一 Kotlin 标准库版本，解决 `libxposed` 传递依赖 `kotlin-stdlib` 2.2.10 带来的元数据版本冲突。
- 保留 Debug / Release / Develop 三个构建类型；Release / Develop 必须正式签名，缺少签名配置时明确失败，不再静默回退到 Android Debug 签名。

### Xposed 元数据

- `module.prop` 保持 `minApiVersion=101`，`targetApiVersion` 提升到 `102`，`staticScope=false`。
- `java_init.list` 保持入口 `name.monwf.customiuizer.MainModule`。
- 同一 APK 以 API 101 公共加载路径运行，兼容 API 102 框架；未在 API 101 必经入口引入 API 102 专属类型。

### 签名

- 原 A13 签名私钥已遗失，建立新的独立长期签名。
- 新证书 `CN=CustoMIUIzer A13`，`RSA 4096`，`SHA256withRSA`，有效期 10950 天。
- 新证书 SHA-256：`15ce32f03e4d8e62df9390f77431862e59bf2cf95cd5a72f0c7330cdfcca2934`。
- 旧签名版本不能覆盖安装；后续 A13 版本固定使用该证书。

### Kotlin 迁移

- 第二批迁移：`AboutFragment`、`Credentials`、`CredentialsShortcut`、`PreferenceState`、`ListViewEx`、`BitmapCachedLoader`、`ShakeManager`、`StepCounterController`、`WebPage`。
- 第三批迁移：`CheckBoxPreferenceEx`、`EditTextPreferenceEx`、`PreferenceCategoryEx`、`PreferenceEx`、`ListPreferenceEx`、`DropDownPreferenceEx`、`SeekBarPreference`、`SpinnerEx`、`SpinnerExFake`。
- 第四批迁移：`System_AutoBrightness`、`System_LockScreenShortcuts`、`System_StatusbarControls`、`System_Visualizer`、`System_BatteryIndicator`、`System_NoScreenLock`、`System_AirplaneModeConfig`、`System_VibrationAmp`、`System_ScreenshotConfig`、`Various`、`Various_CallUIBright`、`Various_HiddenFeatures`。
- 第五批迁移：`SoundData`、`PreferenceAdapter`、`ModSearchAdapter`、`ResolveInfoAdapter`、`GetPathUtils`、`ColorCircle`、`AppHelper`。
- `AppHelper` 改为 Kotlin `object`，`getBooleanOfAppPrefs`、`showInputDialog` 使用 `@JvmOverloads` 保持 Java 重载兼容；`syncPrefsToAnother` 使用 `when` 替代 `instanceof` 链；`removeStringPair` 改用迭代器安全移除。
- `StepCounterController` 修复 `Handler.removeCallbacks` 与 `Runnable` 生命周期问题；`BitmapCachedLoader` 使用 `@file:Suppress` 处理 `AsyncTask` 弃用。
- 删除所有对应 Java 源文件，避免重复类定义。

### 测试与 CI

- 新增 `ModuleMetadataTest` 单元测试，验证 `module.prop` 与 `java_init.list`。
- 更新 GitHub Actions：运行单元测试、`lintRelease`、Debug 构建、Release R8 代码路径，并校验 Xposed metadata、`applicationId`、`minSdk`、`targetSdk`、Legacy Xposed API 扫描。
- 本地已验证：`clean`、`:app:test`、`:app:lint`、`:app:assembleDebug`、`:app:assembleRelease` 通过；Release APK 使用新 A13 证书 v2 签名，zipalign 对齐通过。

### 已知未决

- 输出 APK 名当前使用 AGP 默认 `app-<variant>.apk`，输出命名恢复为 `CustoMIUIzer-A13-r13.x.x.apk` 将在 AGP VariantOutput API 可用后补齐。
- 生命周期与高频 Hook 的系统性治理已纳入路线，将在后续提交按功能组推进。

## r13.1.2（宏观优化）

### 优化
- 推进「功能关闭零成本」：在 `MainModule` 中引入 `processHooked` 标记。
- `ModuleHelper` 在成功注册 Hook 时设置 `processHooked = true`，仅在当前进程确实有 Hook 注册时才注册 `SharedPreferences` 变化监听器。
- 避免在未启用任何功能的进程中长期持有 `OnSharedPreferenceChangeListener`，减少不必要的 IPC 与内存开销。
- 版本号升级到 `r13.1.2`（`versionCode` 117），APK 输出保持 `CustoMIUIzer-A13-r13.1.2.apk`。

## r13.1.1（热修复）

### 修复
- 修复 `XposedHelpers.findMethodExact` 仅在当前类搜索方法的问题，改为向上搜索父类中声明的方法，解决部分 ROM 上 `SystemUIApplication.onCreate`、`StatusBarMobileView`/`KeyguardBottomAreaView` 等系列 `Failed to hook` 警告。
- 增强 `ModuleHelper` 失败日志：Hook 失败时输出异常原因，便于后续日志定位。
- 版本号升级到 `r13.1.1`（`versionCode` 116），APK 输出保持 `CustoMIUIzer-A13-r13.1.1.apk`。

## r13.1.0（关键节点）

### 包名与品牌
- 应用包名统一为 `tv.withaibuild.customiuizer.r13`。
- 应用名统一为 `米客 A13`。
- 版本号按中版本升级规则提升到 `r13.1.0`，APK 输出保持 `CustoMIUIzer-A13-r13.1.0.apk`。
- 运行目标仍为 MIUI 14 / Android 13（API 33），Xposed 框架接口仍为 libxposed API 101。
- README / CHANGELOG 同步更新。

## r13.0.0-api101

- 从上游 CustoMIUIzer v23.11.26 分叉，适配 MIUI 14 / Android 13。
- 使用独立的 `name.monwf.customiuizer.a13` 应用标识与 `CustoMIUIzer_forA13` / `米客_forA13` 品牌。
- 升级模块元数据与依赖到 libxposed API 101。
- 将模块生命周期回调迁移到 API 101。
- 使用 HookBuilder 与异常透传拦截器替换注解/类 Hook 注册。
- 添加 R8 安全兼容适配层，保留 before/after 回调。
- 迁移共享资源与包权限 Hook 到原生 Chain 拦截器。
- 限制 Hook 激活在 Android 13（API 33），并记录 xaga 目标 ROM 矩阵。
- 添加无签名环境构建回退与 GitHub Actions 发布构建。

## r13.0.8（关键节点）

合并 r13.0.3-api101、r13.0.5、r13.0.6、r13.0.7、r13.0.8 的生命周期 / Receiver / Handler 修复与版本修正。

### 启动与权限
- `PackagePermissions`：修复 `systemPackages.contains()` 空指针安全。
- 在 `onModuleLoaded` 中预取 system_server 远程偏好，减少 `Binder.setWarnOnBlocking` 开启后的 `FLAG_ONEWAY` Binder 警告。
- `Controls`：`powerLongPress` 使用 `hookAllMethodsSilently`，避免 Hook 失败日志并兼容不同 ROM 签名变体。

### 生命周期与内存泄漏
- `GlobalActions`：全局接收器在重新注册前先 `unregisterReceiver` 旧实例，避免 `ActivityManager` 中累积同进程同 Filter 的接收器。
- `System`：闹钟 `ContentObserver` 移除旧 Handler，改用 `mContext.getMainLooper()`，避免绑定错误线程。
- `System`：充电动画 `WakeLock` 释放 `Runnable` 去重，避免同一对象上叠加多个延迟任务。
- `System`：锁屏壁纸设置 `Handler` 去重，避免同一对象叠加多个延迟任务。
- `SystemUI`：锁屏相机快捷入口的 `resetViews` 延迟 `Runnable` 去重，防止快速连点时队列中堆积多个相同任务。
- `SystemUI`：截图隐藏覆盖层与 `SecureQSTiles` 广播接收器重注册时先注销旧实例。
- `SystemUI`：侧边栏展开接收器在 `onAttachedToWindow` / `onDetachedFromWindow` 时正确注册/注销。

### 版本与品牌
- 修正版本名、应用 ID 与输出 APK 命名，统一为 A13 风格（`r13.x.x`，`CustoMIUIzer-A13-r13.x.x.apk`）。
- 修正 `MainModule` 中残留的 A14 品牌字符串。

## r13.0.12（关键节点）

合并 r13.0.9、r13.0.10、r13.0.11、r13.0.12 的 Handler / SharedPreferences 优化与测试验证。

### 生命周期与 Handler
- `GlobalActions` / `Various`：一次性 `Handler` 明确使用 `Looper.getMainLooper()`，不再依赖当前线程 Looper。
- `GlobalActions`：复用静态 `mMainHandler` 执行 `FloatingWindow` 和 `ScrollToTop` 的延迟注入事件，减少 `Handler` 对象创建。
- `SystemUI`：进一步修复广播接收器与延迟 `Runnable` 重复注册问题。

### SharedPreferences 读取优化
- `MainModule.needLoadPrefs` 改为按具体 key 读取，避免对每个包都调用 `getAll()`。
- `onSharedPreferenceChanged` 改为按类型逐键读取变化值，避免每次修改偏好都复制整份 `getAll()` 映射。
- `MainModule` 增加 `getRemotePrefs()` 缓存，进程内复用同一个 `RemotePreferences` 实例。

### 版本与品牌
- 版本号继续沿用 `r13` 前缀，输出 APK 保持 `CustoMIUIzer-A13-r13.x.x.apk`。
- 运行时不支持 Android 13（API 33）以外的系统。

### 测试结论
- r13.0.12 测试日志未发现 `name.monwf.customiuizer.r13` 相关崩溃、`AndroidRuntime` 或 `XposedBridge` 错误；Vector 加载模块成功。
- `AntiDefraudAppManager` 对 `name.monwf.customiuizer` / `.a13` / `.r14` 的 `NameNotFoundException` 属于外部反诈应用扫描包签名，非本模块报错，无需修复。
