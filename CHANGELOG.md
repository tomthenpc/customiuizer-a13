# Changelog

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
