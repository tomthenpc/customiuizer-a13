# R13.7 连续优化交接

## 结论与边界

本轮 K11-K17 已在 `codex/r13.7-optimization-run` 完成代码、测试、静态审计、Lint、Debug/Release 构建和产物检查。该分支是**静态可合并候选**，但不是实机验收版本。

以下项目仍为**待实机验证**：

- K7 原有设备验证；
- K12 DeviceInfoMonitor 在 Redmi Note 11T Pro（xaga）上的显示、熄屏/亮屏恢复、SystemUI 重建和 sysfs 失败退避；
- K15 AudioVisualizer 的采样、视觉效果、播放/可见性/熄屏/面板关闭和 detach 生命周期；
- K16 锁屏专辑图的 MIUI 14 目标兼容、视觉效果、AOD/熄屏取消、快速切歌和锁屏重建。

静态测试、构建、Lint、APK、签名或旧版本结果均不能替代上述实机结论。本轮未合并 `main`，未创建 tag 或 Release，未上传 APK。

## 基线

- 仓库：`tomthenpc/customiuizer-a13`
- 已快进并推送的 `main`：`505c97e5d12b237a7022f77c0392a9771cff6cb6`
- 工作分支：`codex/r13.7-optimization-run`
- 起始 HEAD：`505c97e5d12b237a7022f77c0392a9771cff6cb6`
- A14 工程方法参考：`tomthenpc/customiuizer-a14` / `r14.13.7`
- A13 运行边界：MIUI 14 / Android 13，`minSdk=33`，`targetSdk=34`，`arm64-v8a`
- applicationId：`tv.withaibuild.customiuizer.r13`
- libxposed：`minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`

## 各阶段实际修改

### K11：回调、生命周期与热路径

- 为脱离 MethodHook 主体执行的 Receiver、Handler、Runnable、Observer、listener 和异步回调补齐 `ModuleHelper.guarded` 异常边界；有返回值的回调保留各自“不消费”兜底语义。
- 建立 `process + stable key` 与 `owner + stable key` 注册模型，旧注册可替换、注销幂等，避免 SystemUI 重建和新 Hook 实例导致 Receiver/Observer 数量增长。
- 只读 Hook 参数改用 `getArg(index)` 或现有参数列表；只在确需改写时物化数组。
- 为字段和无参方法反射缓存移除命中路径复合 key 分配，保持原解析规则、异常类型和继承搜索顺序。
- 增加回调隔离、注册替换/注销和反射缓存分配测试；新增 `docs/K11A_CALLBACK_SAFETY.md`。

### K12：DeviceInfoMonitor

- 从 SystemUI 状态栏 Hook 中分离 `DeviceInfoMonitor` controller，保留原偏好 key、sysfs 节点和模式 1/2/3 显示语义。
- 功能关闭不启动 ticker；start/stop 幂等；owner 替换会终止旧任务；Context/View 不由静态强引用长期持有。
- 熄屏时真正取消调度，亮屏立即刷新一次；读取失败使用有上限退避，成功后恢复正常间隔。
- 增加生命周期、屏幕状态、旧 generation 和退避恢复测试。

实机状态：**待实机验证**。

### K13：设置列表与图标加载

- `AppDataAdapter` 改为明确主线程所有权的可替换列表；后台构建结果，主线程一次替换；预计算包名、Activity 和搜索 key。
- 保留原筛选、排序、选中和导航语义，ViewHolder 复用并减少 `getView()` 临时字符串与对象。
- 图标加载改为共享有界 executor、同 key in-flight 去重和字节计量缓存；队列拒绝会清理 loading 状态，不使用静默丢任务策略。
- 增加包安装/卸载/更新失效和 `onTrimMemory` 收缩路径，删除主动 `Runtime.gc()`。
- 增加队列、去重、拒绝清理、缓存和 adapter 复用测试。

### K14：Locale 与搜索

- Android 13 统一通过 framework `LocaleManager.applicationLocales` 写入明确语言或清空为跟随系统；移除多头 Locale 控制。
- 从未设置语言且当前为 auto 时走低成本启动路径；语言切换后安全重建设置进程。
- LSPosed service 尚未绑定时不再直接误报模块未激活。
- 搜索 key 和索引预计算，输入路径不重复创建 Regex；保留结果顺序、导航、高亮和返回后的开关刷新语义。
- 增加 Locale 调和、auto 快路径、搜索索引和状态恢复测试。

### K15：AudioVisualizer

- 31 个独立 `ValueAnimator` 收口为单一帧调度器；FFT band/bin 索引预计算，每帧只触发一次 invalidate。
- 停止播放、不可见、熄屏、面板关闭或 detach 时停止采样/帧回调，detach 释放 Visualizer。
- Palette 工作改为 generation latest-wins，旧封面结果不能覆盖新封面；移除 AsyncTask。
- 保留 A13 现有视觉样式、颜色和动画偏好语义。
- 增加帧调度、生命周期、generation 和旧结果丢弃测试。

实机状态：**待实机验证**。

### K16：锁屏专辑图

- 新增 `LockScreenAlbumArtController` 与 `AlbumArtPolicy`，将缩放、灰度、blur 和 `WallpaperColors` 处理移出主线程。
- 使用单线程、单等待槽的有界 worker；新请求取消旧 Future 并用 generation 保证 latest-wins。
- 先按目标尺寸降采样；缓存 key 覆盖源图、源/目标尺寸、blur、rescale、灰度和主题相关参数。
- 使用按字节计量、最多两帧的缓存；尺寸、主题或处理参数改变会立即清理不兼容缓存。
- 不回收仍可能由 View 使用的 Bitmap；熄屏和 owner detach 会取消任务；Context/View 使用弱所有权边界。
- 增加 generation、取消、缓存容量、缓存 key 和 Long 溢出边界测试。

实机状态：**待实机验证**。

### K17：删除 facade

- `System`：MainModule 的 119 个 facade 调用机械替换为 17 个 `System*Hooks` 领域对象直连；124/124 个旧 public static 方法在领域对象中可解析，调用顺序摘要保持一致，随后删除 `System.kt`。
- `SystemUI`：48 个 facade 调用机械替换为 7 个领域对象直连；唯一共享字段 `newStyle` 移入 `SystemUIStatusBarHooks`，写入时机和 12 个读取点不变，随后删除 `SystemUI.kt`。
- `Launcher`：50 个 facade 调用机械替换为 6 个领域对象直连；偏好条件、顺序、参数和实现不变，随后删除 `Launcher.kt`。
- 保留 `MainModule.java`、`XposedHelpers.java`、`ModuleHelper.java`、`HookerClassHelper.java`、`ResourceHooks.java` 和 `org/apache/commons/lang3/reflect/MemberUtilsX.java`。

## 与原计划不同之处

- K11 在三个要求的行为提交外增加了一个先行测试提交和一个回调审计文档提交，使行为修改可独立验证与回退。
- K15 保留 A13 的 Visualizer session 0 选择，没有移植 A14 的真实 session 选择逻辑，因为这会改变 A13 运行语义和设备兼容边界。
- K16 只移植有界调度、latest-wins、缓存和生命周期方法，没有复制 Android 14 独有锁屏类、字段或 Hook target。
- K17 的 SystemUI facade 含唯一共享状态，删除前将该字段机械搬到其唯一领域 owner；未复制实现或新增路由。
- 最终产物门禁后发现 Linux workflow 直接执行 `./gradlew`，而仓库模式为 `100644`；增加独立提交只把 wrapper 模式改为 `100755`，脚本内容不变。

## 提交列表

按依赖顺序：

```text
4c6a3e0 test(k11): cover deferred callback isolation
c5ea08b fix(k11): guard deferred framework callbacks
35d2524 docs(k11): record callback safety audit
e30294f fix(k11): bind registrations to stable owners
b91f68f perf(k11): remove hook and reflection hot-path allocations
441b79f refactor(k12): isolate device monitor controller
aaaf96a perf(k12): suspend device monitor while screen is off
f7b258d test(k12): cover monitor lifecycle and backoff
1fad51c perf(k13): bound and deduplicate icon loading
aa8b321 perf(k13): remove settings list copy-on-write overhead
0428bc1 test(k13): cover icon queue and adapter reuse
7a6d519 fix(k14): apply Android 13 locale through LocaleManager
8ecdbc1 perf(k14): precompute settings search index
33fe9db test(k14): cover locale reconciliation and search state
6b3c503 perf(k15): use a single visualizer frame scheduler
b9b1991 fix(k15): make palette processing latest-wins
727a39a test(k15): cover visualizer lifecycle and generation
c0a2c36 refactor(k16): isolate lockscreen artwork processing
11a15dc perf(k16): bound and cancel lockscreen artwork work
37b9f20 test(k16): cover artwork generation and cache limits
18d74a1 refactor(k17): dispatch System hooks directly
b538d55 refactor(k17): dispatch SystemUI hooks directly
7c1b146 refactor(k17): dispatch Launcher hooks directly
caad0d2 build: make Gradle wrapper executable
```

## 最终静态门禁

在 `7c1b146` 源码状态执行一次完整 clean 门禁；后续 `caad0d2` 仅改变 Git 文件模式，不改变构建输入内容。

- `git diff --check`：通过。
- `python tools/check-invariants.py`：117 个文件，无违规。
- `python tools/audit-system-migration.py --baseline-ref backup/r13-k5-before-system-java-removal`：通过；124/124 baseline public static 方法可解析，MainModule 有 119 个 System 领域直连调用、0 个 System facade 调用。
- `:app:testDebugUnitTest`：224 个测试，0 失败。
- `:app:lintDebug`：通过；报告 521 个既有 warning，0 error。
- `:app:lintRelease`：通过；报告 512 个既有 warning，0 error。
- `:app:lintVitalRelease`：命令已选择，但在完整 `lintRelease` 已执行的任务图中由 Gradle 标记为 `SKIPPED`；没有将其冒充为独立执行成功。
- `:app:assembleDebug`：通过。
- `:app:assembleRelease`：通过；`minifyReleaseWithR8` 与 `shrinkReleaseRes` 已执行。
- Kotlin Debug/Release 与 Java Debug/Release 编译：通过。

编译输出仍有 MIUI/Android 13 兼容代码的 deprecated、unchecked cast 和静态条件 warning；本轮未通过全局 suppress、禁用 Lint、扩大 keep 或删除功能处理这些非阻断项。

## APK、签名与配置检查

- Debug APK：`app/build/outputs/apk/debug/CustoMIUIzer-A13-r13.2.4-devin.apk`
  - 大小：11,901,371 bytes
  - SHA-256：`3D78ACBDEA01A63754A5EE543CE3DF76FF7386D288AED92D55D27CE296B6E1D0`
- Release APK：`app/build/outputs/apk/release/CustoMIUIzer-A13-r13.2.4-devin.apk`
  - 大小：2,780,790 bytes
  - SHA-256：`6860A6CA5A75A5D0EF05A0593430D822399672E0CA766BE34C4807354F1C9D53`
- Release zipalign：4-byte 对齐检查通过。
- Release 签名：v1=false，v2=true，单签名者；证书 SHA-256 为 `c0eff2dc4e662717195490da78b12a984c6f2e6bd38acf4edad14d53e3d22e70`，与 clean 前基线一致。
- APK badging：applicationId `tv.withaibuild.customiuizer.r13`，versionCode `123`，versionName `r13.2.4-devin`，minSdk `33`，targetSdk `34`，compileSdk `36`。
- 构建配置、Manifest、Xposed metadata 和 scope 相对起始 HEAD 无变更。
- Release APK 内含 `META-INF/xposed/java_init.list`、`module.prop` 和 `scope.list`；入口仍为 `tv.withaibuild.customiuizer.MainModule`。
- libxposed metadata 仍为 API 101/102、`staticScope=false`；Legacy Xposed Java API 扫描为 0 命中。
- 未跟踪 APK、AAB、keystore、`keystore.properties`、日志、hprof 或 dump。

## 风险与回滚

主要剩余风险来自 ROM/设备行为而不是静态构建：

1. SystemUI 重建、锁屏/AOD、媒体快速切换和 Launcher 进程生命周期只能通过目标设备确认。
2. MIUI 私有类和字段在不同 ROM 小版本可能变化；本轮保持了 A13 target，但静态编译不能证明运行时解析成功。
3. K15/K16 涉及帧调度、异步取消和 View 生命周期，应重点检查旧任务是否停止、视觉结果是否与原版一致、异常是否被隔离。
4. Lint warning 仍存在，但最终报告为 0 error；不应在未区分 MIUI 兼容代码和真实缺陷前批量清理。

推荐按反向依赖顺序回滚：

1. `caad0d2`（仅 wrapper 文件模式）；
2. K17：Launcher → SystemUI → System；
3. K16 三个提交；
4. K15 三个提交；
5. K14 三个提交；
6. K13 三个提交；
7. K12 三个提交；
8. K11 按性能 → 生命周期 → callback → 文档/测试顺序。

不要只恢复已删除的 facade 文件而保留 MainModule 直连，也不要跳过对应测试提交后声称阶段已完整回滚。

## 合并建议

建议保持当前单一候选分支用于整体验证，但评审或分批合并时按以下依赖块拆分：

1. K11；
2. K12；
3. K13 + K14（设置应用侧）；
4. K15（独立高风险运行时阶段）；
5. K16（独立高风险锁屏阶段）；
6. K17 + wrapper 文件模式修复。

这些块是线性依赖，不建议创建相互平行、各自从 `main` 分叉的实现分支。尤其 K15、K16 在实机验收前应保持可独立回退；K17 应在前序领域对象稳定后最后合并。
