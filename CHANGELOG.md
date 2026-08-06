# 更新日志

## r13.10.1（2026-08-06）

### 性能与资源治理

- 状态栏设备信息采样改为固定缓冲区与逐字节 sysfs 解析，移除周期性 Properties、RandomAccessFile、临时结果对象和屏幕状态 Binder 查询；
- Launcher HotSeats 手势阈值和触摸状态按 View 实例缓存，减少每个触摸事件中的 density 与 ViewConfiguration 重复读取；
- Launcher FSG 的 BaseRecentsImpl Class 在 Hook 安装阶段解析一次并由相关 callback 复用；
- Launcher 动画缩放使用 `ValueAnimator.getDurationScale()` 快路径，移除反射与 `Settings.System` Binder 调用。

### 稳定性与兼容性

- 保持原有 DeviceInfo 显示类型、文本格式、采样周期、失败退避和屏幕生命周期合同；
- 保持 Launcher 左右滑动距离、速度、touchSlop、动作 key 和 FSG 调用范围行为；
- FSG stack-scope 优化因缺少 MIUI 14 / HyperOS 1 Launcher APK 与反编译证据而冻结，不进行无证据替换。

### 工程与仓库

- 删除未引用的 miui.jar 和异常 java.lang external annotation；
- 规范化 IDE、本地工具、构建产物、APK/AAB、系统文件及 ROM intelligence 输入的共享忽略规则；
- 保留 framework.jar、miuisystem.jar、Gradle wrapper 和项目级共享 Android Studio 配置。

### 发布说明

- 面向 MIUI 14 / Android 13；
- HyperOS 1 / Android 13 继续作为实验兼容目标；
- 本阶段仅准备 Release Candidate 元数据，不生成 APK；
- 正式签名 Release APK 必须在完整静态发布门禁通过后，由精确 RC commit 单独构建。

## r13.10.0（2026-08-05）

### ROM 兼容、安装架构与 Hook 合同

- 新增 `ProcessScope` 与 `ProcessScopes.resolve()` 作为包/进程路由唯一来源；`MainModule` 按 scope 分发到 `AndroidPackageInstaller`、`SystemUiInstaller`、`LauncherInstaller` 等独立 Installer，避免非目标进程加载偏好与 Hook；
- 新增 `FeatureCatalog` + `FeatureInstallRegistry` 类型化特性目录与安装注册表，统一 id 归一化、原子申领、兼容政策与失败诊断；
- 从 `MainModule` 提取 `PreferenceLoadRegistry`，集中处理四个遗留偏好的加载判断，保持原有短路和异常语义；
- 完善 Android 13 上 MIUI 14 与 HyperOS 1 的 ROM 环境识别、target contract、variant 选择和安装诊断，缺少目标时保持安全跳过，不进行跨候选混装；
- 加固反射参数、Hook 安装与兼容性失败分类，使 resolver、installer 和诊断记录保持一致。

### 稳定性与生命周期

- 在 Hook 回调、反射 fallback、Receiver 生命周期、Preference observer、延迟 callback 和 diagnostics 边界统一传播直接或包装的 OutOfMemoryError、ThreadDeath 与 VirtualMachineError；
- `ModuleHelper` 增加 Receiver / Observer / Callback 的完整注册、替换和释放闭环；`ActivitySelector` / `AppSelector` 改用 `postDelayed` 替代 `Thread.sleep`；
- 普通 ROM 差异、反射失败和 callback 异常继续按原有 sentinel、日志或 fallback 隔离，不改变既有功能行为；
- 增加 cause-chain、原实例身份、状态机顺序与 mutation 合同测试。

### 界面与用户行为

- 来电不打断、移除安全窗口、临时隐藏悬浮窗、强制浮窗打开、小窗 Plus、去除浮窗黑名单等用户行为功能接入类型化 catalog；
- 状态栏时钟、自动亮度、网速指示、通知、控制中心、电池、亮度、音量等大量 SystemUI Hook 迁移到类型化目录，提升安装稳定性。

### 发布说明

- 面向 MIUI 14 / Android 13；HyperOS 1 / Android 13 继续作为实验兼容目标；
- 本版通过静态门禁、Release JVM、Release/Vital Lint、R8、正式签名、zipalign、包名、版本和 Xposed 元数据核验；
- 本地正式 APK 构建完成后仍标记为 BUILD_VERIFIED，实机状态由用户安装使用后确认。

## r13.9.2（2026-08-01）

### 内存与稳定性

- 锁屏专辑图所属 View 脱离窗口时，立即取消未完成任务，并释放模块设置的背景、单帧输出缓存和静态处理结果；保留源图引用供重新附着后按需生成，功能行为不变；
- 保持一条有界处理队列、latest-generation 发布门禁和中间 Bitmap 回收，避免已取消结果重新占用 SystemUI 内存。

### 界面与交互

- 设置页切换动画按 A14 当前实现调整为 `350ms`，恢复更易感知的页面节奏；
- 开关直接继承所在行的按压状态，按下即反馈；移除每次点击创建的透明度动画，减少临时渲染状态和连续点击干扰。

### 兼容与发布

- 未修改 MIUI 14 / Android 13 或 HyperOS 1 / Android 13 的 ROM Hook 目标、Contract 或 fallback；
- LSPosed 仓库增加独立 `SUMMARY`，模块列表只显示简洁摘要，不再展开整段 README；
- Kotlin/Java 编译、静态不变量、单元测试、Lint、版本、签名、zipalign、Xposed 元数据和 `debuggable=false` 作为发布门禁；新增改动仍需实机 LSPosed 日志确认。

## 历代核心实现总结

A13 版本线已完成独立包名与 A13 专用签名、libxposed API 101/102、System/SystemUI/Launcher 领域拆分、小批量 Kotlin 化、资源与偏好 Hook 加固、Receiver/Observer/Handler 生命周期治理、有界缓存、可取消异步任务、OOM 边界和 Contract/Resolver 兼容诊断。详细历史保留在 Git tag 与提交记录中。
