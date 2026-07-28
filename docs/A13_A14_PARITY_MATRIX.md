# A13 / A14 工程进度差距矩阵

> 参考 A14 `main` 正式版 `r14.13.5`（`4225d80e`）与 `devin/r14.13-kotlin-refactor` 最新 `18d962a5`（父 `321081d2`），
> 不照搬 A14 源码，只评估工程方法、迁移策略和已验证优化模式是否适用于 A13。
> 证据标签：`已验证`（构建/测试/Git）、`代码确认`（静态可判定）、`待实机`（需真机）。

## 一、基础事实

| 项目 | A14 `r14.13.5` | A13 `r13.2.2-devin` |
|------|--------------|---------------------|
| applicationId | `tv.withaibuild.customiuizer.r14` | `tv.withaibuild.customiuizer.r13` |
| versionCode/Name | `183 / r14.13.5` | `120 / r13.2.2-devin` |
| 支持系统 | HyperOS 1 / Android 14 | MIUI 14 / Android 13 |
| minSdk / targetSdk | `34 / 34` | `33 / 34` |
| compileSdk | `37` | `36` |
| ABI | `arm64-v8a` | `arm64-v8a` |
| libxposed | `minApiVersion=101`，`targetApiVersion=102`，`staticScope=false` | 同 A14 |
| JDK | `17` | `17` |
| Gradle | `9.6.1` | `8.9` |
| AGP | `9.2.1` | `8.7.2` |
| Kotlin | `2.3.21` | `2.0.21` |
| 主入口 | `MainModule.java`（保留 Java） | `MainModule.java`（保留 Java） |

## 二、差距矩阵

| # | 领域 | A14 当前状态 | A13 当前状态 | 是否适用于 A13 | 风险 | 建议动作 | 验证方式 | 状态 |
|---|------|--------------|--------------|----------------|------|----------|----------|------|
| 1 | Gradle Groovy/Kotlin DSL | 已迁移到 `settings.gradle.kts` + `app/build.gradle.kts`（Kotlin DSL） | 已迁移到 Kotlin DSL（`settings.gradle.kts`、`app/build.gradle.kts`） | 直接适用 | 低 | 保持，不迁移回 Groovy | 构建通过 | 已完成 |
| 2 | version catalog | `gradle/libs.versions.toml` 管理依赖与插件版本 | 同 A14 已有 `gradle/libs.versions.toml` | 直接适用 | 低 | 保持，按需要补齐 A14 新依赖（如 `dexkit`） | 构建通过 | 已完成 |
| 3 | JDK / Gradle / AGP / Kotlin 版本 | JDK 17、Gradle 9.6.1、AGP 9.2.1、Kotlin 2.3.21 | JDK 17、Gradle 8.9、AGP 8.7.2、Kotlin 2.0.21 | 需要按 A13 改写 | 中 | 不盲目升级；A13 在 8.9/8.7.2/2.0.21 已绿，若升级需逐个维度验证，且 compileSdk 37 与 minSdk 33 组合需测试 | `test` / `lint` / `assembleDebug` / `assembleRelease` 全绿 | 暂缓 |
| 4 | R8 / 资源压缩 / zipalign / APK 签名 | Release 启用 R8 + shrinkResources + zipalign + v2 签名，命名 `CustoMIUIzer-A14-*.apk` | Release 启用 R8 + shrinkResources + zipalign + v2 签名，命名未统一 | 直接适用 | 低 | 可同步 `outputFileName` 命名为 `CustoMIUIzer-A13-*.apk`（可选）；签名位置保持外部 `../keystore.properties` | APK 审计 | 已完成（命名可选） |
| 5 | libxposed API 101/102 | `api 102.0.0` / `service 102.0.0`，单 APK 兼容 | `api 101.0.1` / `service 101.0.0` | 需要按 A13 改写 | 中 | A14 升级到 102 专属类型和 DexKit 2.2.0；A13 当前 101 基线稳定，若升级需确认 API 101 管理器兼容性 | 构建 + 实机 | 暂缓 |
| 6 | Java / Kotlin 文件数量 | 3 `.java` / 91 `.kt`（核心 mods 15 kt、utils 20 kt、subs 21 kt） | 38 `.java` / 46 `.kt`（mods 7 java、utils 7 java、subs 12 java） | 需要按 A13 改写 | 高 | 按批次迁移：测试/小工具 → UI → 基础设施 → 核心 Hook；保留 `MainModule.java`、`XposedHelpers.java` 等边界 | 每批次 `test` / `lint` / `assemble*` | 进行中 |
| 7 | 设置 UI Kotlin 化 | Activity/Fragment/Preference/搜索/选择页基本 Kotlin 化；`Locale` 状态机由 `AppLocaleController` 统一 | 仍有 `MainActivity.java`、`PreferenceFragmentBase.java`、`AppSelector` 等 Java 设置 UI | 需要按 A13 改写 | 中 | 按 A13 页面结构分批迁移，不能复制 A14 页面代码 | `lint` + 实机主题/语言/重建 | 未开始 |
| 8 | Adapter / ViewHolder | `subs` 多为 Kotlin，使用 ViewHolder 和 `lifecycleScope` | `BTList.java`、`WiFiList.java`、`LockedAppAdapter.java` 等 Java Adapter | 需要按 A13 改写 | 中 | 先迁移纯列表 Adapter（`LockedAppAdapter`、`PrivacyAppAdapter`、`AppDataAdapter` 等） | 实机列表页 | 未开始 |
| 9 | Coroutine / 生命周期 | 使用 `lifecycleScope`、`viewScope`；协程有明确取消点 | 仍有 Handler/Runnable 和 Thread 风格，无统一协程 | 需要按 A13 改写 | 高 | 不为了 Kotlin 形式引入 Flow/Sequence；只在生命周期明确处替换 | 实机长时间运行/重建 | 未开始 |
| 10 | Helpers / AppHelper / ModuleHelper | `Helpers.kt`、`ModuleHelper.kt` 已 Kotlin 化；`AppLocaleController` 统一 Locale | `Helpers.java`、`AppHelper.kt`、`ModuleHelper.java` 混合；Locale 处理在 `AppHelper.getLocaleContext` | 需要按 A13 改写 | 高 | 逐个文件迁移并核对 Java 调用方与 JVM 互操作 | 构建 + 实机 | 未开始 |
| 11 | HookerClassHelper | A14 有 `mods/utils/HookerClassHelper.java` 适配 `XposedInterface.Hooker` | A13 未引入 HookerClassHelper，仍走旧回调/直接 `MethodHook` | 直接适用（可选） | 中 | 可引入以统一 Hook 回调封装，但需确认不改变 Hook target/顺序 | 构建 + 实机 | 待评估 |
| 12 | ResourceHooks | `ResourceHooks.kt` 已优化：volatile copy-on-write、`SparseArray`、延迟 `chain.executable.name`、未命中短路径 | `ResourceHooks.java` 仍是 Java，已做单次 `get()` 优化，但 `mReplaceHook` 每次调用 `findContext()` 与 `executable.name` | 需要按 A13 改写 | 高 | 已按 A14 延迟执行模式优化：先查 `fakes`/`replacements` 命中再调用 `findContext()`/`executable.name`，未命中直接短路 | `assembleRelease` + 实机资源替换 | 已修复（P2） |
| 13 | 核心 System / SystemUI / Launcher mods | `System.kt`、`SystemUI.kt`、`Launcher.kt` 全部 Kotlin；已修复状态栏 View 强引用、thermal zone `break`、Regex 等 | `System.java`、`SystemUI.java`、`Launcher.java` 仍为 Java，存在 `toRegex()`、`Consumer.forEach`、未加 export flag 等 P3 | 需要按 A13 改写 | 高 | 最后阶段迁移；先按 A14 模式修复当前 Java 中的 P1/P2/P3 | 完整实机回归矩阵 | P1/P2/P3 |
| 14 | RemotePreferences | 已修复 `initPrefs()` 空快照永久固化问题；Listener 注册成功后设置状态 | A13 仍可能空快照被标记为 `mPrefsLoaded`；`watchPreferenceChange` 需确认幂等 | 需要按 A13 改写 | 高 | 核对 `MainModule` 中 `mPrefsLoaded` 与 listener 注册顺序 | 实机解锁前启动窗口 | P1（潜在） |
| 15 | Receiver / Observer / Listener | A14 已明确所有动态 `registerReceiver` 的 `RECEIVER_EXPORTED` / `RECEIVER_NOT_EXPORTED` | A13 多处 `registerReceiver` 仍使用 2-arg 未指定 flag（`Controls`、`GlobalActions`、`WiFiList`、`BatteryIndicator`、`StepCounterController`、`System.java` 等） | 需要按 A13 改写 | 高 | 立即执行 P1：逐个判断广播来源并添加 flag，保持成对注销 | `lint` + 构建 + 实机 Android 14 | P1 |
| 16 | DexKit 生命周期 | A14 使用 DexKit 2.2.0，已防止 `XposedHelpers.createBridge` 重复创建，用完关闭 | A13 未集成 DexKit；`XposedHelpers` 无 DexKit bridge | A14 专属 | 低 | A13 不需要 DexKit 时不引入；如后续需要，按 A14 模式封装 | — | 不适用 |
| 17 | 资源和反射缓存 | A14 `XposedHelpers` 已缓存 `getApplicationClassLoader` 失败回退；`ResourceHooks` 线程安全发布 | A13 `XposedHelpers` 仍可能每次反射探测失败类；`ResourceHooks` 仍用 `ConcurrentHashMap` + `SparseIntArray` 非线程安全发布 | 需要按 A13 改写 | 中 | 移植失败回退缓存和 copy-on-write 资源替换表 | 构建 + 实机 | 未开始 |
| 18 | 搜索导航 | A14 已建立 `0/1/2` 搜索状态机、`SearchRouteResolver`、`SearchStateMachine` 单元测试；修复 `Various`/子分类返回 | A13 已引入 `SearchNavigation`，`MainFragment` 集成 `SearchStateMachine` / `SearchRouteResolver`，补充 `SearchStateMachineTest` + `SearchRouteResolverTest` | 需要按 A13 改写 | 高 | 已按 A14 模式建立三态状态机与路由解析，`MainFragment` 返回时清理搜索视图 | 实机搜索/返回/重建 | 已完成（P2） |
| 19 | Locale / 主题 / Fragment 重建 | A14 有 `AppLocaleController`、`LOCALE_STATE_MACHINE.md`、完整 `AppLocaleControllerTest` | A13 已引入 `AppLocaleController`，集成到 `MainApplication`/`MainActivity`/`MainFragment`/`AppHelper`，并补充 `AppLocaleControllerTest` 17 个用例 | 需要按 A13 改写 | 中 | 已引入 `AppLocaleController` 单一状态源并补充测试；`LOCALE_STATE_MACHINE.md` 可选补充 | `test` + 实机语言/主题/旋转 | 已完成（P2） |
| 20 | 单元测试 | 68+ tests，覆盖 Locale、Search、PrefPair 等；`AppLocaleControllerTest` 23 个 | `AppLocaleControllerTest` 17 + `SearchStateMachineTest` 10 + `SearchRouteResolverTest` 11 + `ModuleMetadataTest` 1；其他领域待补充 | 需要按 A13 改写 | 中 | 已为 Locale 与 Search 添加测试；后续按修复问题补充 ResourceHooks / PrefPair 等测试 | `test` | 进行中 |
| 21 | APK 元数据审计 | 已建立完整流程：`apksigner verify`、aapt2 `dump badging`、`module.prop`、scope、签名证书 SHA-256 记录 | 同 A14，已有 `docs/ARCHITECTURE_AUDIT_A13.md` 记录 | 直接适用 | 低 | 每次 Release 后执行并记录 | APK 审计 | 已完成 |
| 22 | 实机验证和日志审计 | 已执行 API 101 完整重启日志审计（Xiaomi 13 / Android 14 / SDK 34 实际 API 101） | 未执行完整真机验证矩阵 | 需要按 A13 改写 | 高 | 待设备可用时执行 LSPosed/Vector 日志审计 | 实机 | 待实机 |

## 三、A13 Java / Kotlin 文件统计与分层迁移建议

### 3.1 当前数量

| 目录 | Java | Kotlin | 备注 |
|------|------|--------|------|
| `name/monwf/customiuizer` | 7 | 6 | 入口 `MainModule.java` 保留；`MainActivity.java` 可迁 |
| `name/monwf/customiuizer/mods` | 7 | 0 | `System.java`、`SystemUI.java`、`Launcher.java` 等核心 Hook 最后迁 |
| `name/monwf/customiuizer/mods/utils` | 4 | 2 | `ModuleHelper.java`、`ResourceHooks.java` 可在 P2 后迁 |
| `name/monwf/customiuizer/prefs` | 0 | 11 | 已完成 |
| `name/monwf/customiuizer/qs` | 0 | 1 | 已完成 |
| `name/monwf/customiuizer/subs` | 12 | 13 | `BTList.java`、`WiFiList.java` 等设置子页先迁 |
| `name/monwf/customiuizer/tasker` | 0 | 3 | 已完成 |
| `name/monwf/customiuizer/utils` | 7 | 10 | `Helpers.java`、`BatteryIndicator.java` 等先迁 |
| `org/apache/commons/lang3/reflect` | 1 | 0 | 外部派生，不迁 |
| **合计** | **38** | **46** | — |

### 3.2 分层建议

- **批次 1（低风险）**：单元测试、`org/apache` 外的小工具、`BatteryIndicator`、`AppDataAdapter`、`LockedAppAdapter`、`PrivacyAppAdapter`。
- **批次 2（UI）**：`MainActivity.java`、`subs/*` 列表页、`PreferenceFragmentBase.java`。
- **批次 3（基础设施）**：`Helpers.java`、`AppHelper.kt` 重构、`ModuleHelper.java`、`ResourceHooks.java`。
- **批次 4（核心 Hook）**：`Controls.java`、`GlobalActions.java`、`Various.java`、`Launcher.java`、`SystemUI.java`、`System.java` 逐个域迁移，每域独立提交。

## 四、A14 专属（不适用）项

- Android 14 专属 Hook target（如 HyperOS 1 新 SystemUI 类名）。
- `compileSdk=37` / `buildTools=37.0.0`（A13 当前 `compileSdk=36` 已满足 MIUI 14 构建，升级无直接收益）。
- DexKit 2.2.0 依赖（A13 当前未使用 DexKit，引入会增加构建风险）。
- A14 新版 libxposed API 102.0.0 service（A13 当前 101.0.0 service 已支持 API 101/102 边界）。

## 五、立即执行项

按收益/风险排序：

1. **P1 动态 Receiver export flag**（已完成审计，见 `docs/ARCHITECTURE_AUDIT_A13.md`）。
2. **P2 `ResourceHooks` 热路径优化**（移植 A14 copy-on-write / 未命中短路径）。
3. **P3 低风险 Kotlin 性能修复**（`toRegex()` / `forEach(Consumer)`）。
4. **Locale 状态机 + 单元测试**。
5. **搜索导航状态机 + 单元测试**。
