# DEVIN R13 CHECKPOINT

> 本文件记录当前真实状态，不记录长期规则。长期规则统一放在根目录 `AGENTS.md`。
> 每完成一个有意义的代码、构建、Git 或实机闭环，立即更新本文件；替换过期事实，不要只在末尾追加。

## 当前目标

- 对 A13 项目进行全局代码审查、死代码清理、Kotlin 惯用化与性能优化（低频低风险优先），保持功能行为、JVM/Hook 兼容和构建稳定。

## 当前基线

- **Repository:** `tomthenpc/customiuizer-a13`
- **Branch:** `devin/r13.2-kotlin-api102`
- **Last verified code commit:** `553c5ca`
- **Checkpoint based on commit:** `553c5ca`
- **versionName / versionCode:** `r13.2.2-devin` / `120`
- **applicationId:** `tv.withaibuild.customiuizer.r13`
- **libxposed API:** `minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
- **Hot Reload:** `false`
- **Legacy Xposed API:** `false`
- **SDK:** `minSdk=33`，`targetSdk=34`，`compileSdk=36`
- **ABI:** `arm64-v8a`
- **最新已确认实机版本:** 未确认
- **最后正常行为基线:** `MonwF/customiuizer v23.11.26`

## 本轮已完成（全局代码审查、Kotlin 清理与高频优化第二批）

### 代码
- Kotlin 文件去 `!!`（共移除 11 处）：`AppHelper.kt`、`SpinnerEx.kt`、`SpinnerExFake.kt`、`SeekBarPreference.kt`、`System_ScreenshotConfig.kt`、`System_VibrationAmp.kt`、`Various_CallUIBright.kt`、`ModuleMetadataTest.kt`。
- `AppHelper` preference 访问改为安全调用，未初始化时返回默认值而非 NPE。
- `PrefMap.getStringSet(key)` 返回 `emptySet()` 替代 `new HashSet()`，减少高频 Hook 中 `contains()` 调用的对象分配。
- `System_AirplaneModeConfig.kt`：移除 `lateinit` 与 Java `ArrayList/Arrays`，改用 `MutableList` 与 `split(...).toMutableList()`。
- `System_AutoBrightness.kt`：在 SeekBar 监听器内使用 `val max = maxBrightness ?: return`，减少重复 null 检查。
- `BitmapCachedLoader.kt`：`theTag` 改为 `val`，`tag as Int` 改为 `as? Int`。
- `StepCounterController.kt`：`ArrayList` 改为 `mutableListOf()`。
- `WebPage.kt`：移除未使用的 `WebSettings` import。
- 删除死代码 `utils/SoundData.kt`（仓库内无静态引用、无反射/资源/ProGuard 引用）。
- `Helpers.java`：`getAppName`/`getAppIcon` 改用 `splitPkgAct`（`indexOf('|')` + `substring`）解析固定 `pkg|activity` 格式。该辅助方法仍创建 `String[]` 与 substring；仅保证合法单分隔符格式下的两段语义，前后/中间空格会被 trim；对前导/尾部分隔符、多分隔符返回安全结果。
- `SystemUI.java`、`AppDataAdapter.java`、`LockedAppAdapter.java`、`PrivacyAppAdapter.java`、`SortableList.java`：显式使用 `Locale` 修复 `DefaultLocale` lint 警告，避免依赖默认 locale 带来的土耳其语等异常。
- `ResourceHooks.java`：`getResourceReplacement` 用单次 `get` + 懒加载 fallback key 替代 `containsKey` + `get` 两次查找，并将局部 `modResId` 改为 `int` 原语，以减少 map 查询次数。`Pair.second` 中仍保存 `Integer` 装箱对象，因此未消除装箱，仅减少局部变量分配与 map 查找。
- `PrefMap.kt`：`getStringSet` 返回 `emptySet()` 单例。`keyCache` 已撤销：经检查，`MainModule.mPrefs.getXxx` 存在大量动态 key（UUID、包名/Activity 名、user hash 等后缀，如 `key + "_" + pkgName + "|0"`、`key + "_" + uuid + "_activity"`、`system_` + subKey + ...），key 不稳定且数量无界；使用 `ConcurrentHashMap` 缓存会引入无界内存占用，其收益未经基准验证，因此恢复为 `"pref_key_$key"` 直接拼接。
- ~~`Helpers.java`：新增 `PIPE_SPLIT_PATTERN` 与 `COLON_SPLIT_PATTERN` 预编译 Pattern~~（已撤销）。`String.split(":")` / `String.split("\\|")` 在 Android 中走 `Pattern.fastSplit()` 快路径，不会每次重新编译正则；预编译 Pattern 替换属于无证据优化，已恢复为原始 `String.split`。

### 测试与构建
- `./gradlew --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`：BUILD SUCCESSFUL（`cleanup-build8.log`）。
- `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest`：BUILD SUCCESSFUL（`cleanup-build7.log`）。
- `./gradlew --no-daemon :app:assembleRelease`：BUILD SUCCESSFUL，包含 `lintVitalRelease`（`release-build4.log`）。
- lintDebug：0 errors / 527 warnings（大量 `UnusedResources` 为 `getIdentifier` 动态引用导致，未删除）。

### Git
- 代码提交 1：`07b1090`（Kotlin 去 `!!` + 死代码清理）
- 代码提交 2：`22bc856`（`Helpers.splitPkgAct` + `Locale` lint 修复）
- 代码提交 3：`9713e3d`（`ResourceHooks` 单次 `get()` + `PrefMap` 理论优化）
- 代码提交 4：`80a4ae6`（预编译 Pattern split；本轮已撤销其代码改动）
- 文档提交：`c7ee511`（checkpoint/最终报告更新）
- 当前工作区：证据纠偏 commit 待提交（撤销 Pattern split、`PrefMap.keyCache`、修正文档表述）。

### 文档
- 更新 `docs/DEVIN_R13_CHECKPOINT.md` 为代码审查与证据纠偏状态。

## 最新绿色验证

- **任务/命令：** `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon test lintDebug lintRelease lintVitalRelease assembleDebug assembleRelease`
- **结果：** BUILD SUCCESSFUL
- **产物：** `.devin/a13_p1_build.log`
- **验证日期：** 2026-07-28
- **APK 审计：**
  - `app/build/outputs/apk/release/app-release.apk`
  - SHA-256：`62ED16BEFE47144548D1C862B640396FDD163D3B19897126DFE9F7BF75276AD1`
  - 签名：v2 only，证书 SHA-256：`C0:EF:F2:DC:4E:66:27:17:19:54:90:DA:78:B1:2A:98:4C:6F:2E:6B:D3:8A:CF:4E:DA:D1:4D:53:E3:D2:2E:70`
  - `module.prop`：`minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
  - scope 列表完整，入口为 `name.monwf.customiuizer.MainModule`
- **lintDebug / lintRelease：** 0 errors，warnings 数与基线持平（debug 519 / release 510），无 `UnspecifiedRegisterReceiverFlag` 新增错误。

## 当前问题与阻塞

- 真机验证未完成：LSPosed 加载、SystemUI/Launcher/Settings 主要功能、搜索返回、旋转重建均待确认。
- API 101/102 实机边界未验证：未在只支持 API 101 的环境运行。
- 已记录的 P1/P2/P3 问题，详见 `docs/ARCHITECTURE_AUDIT_A13.md`：
  - ~~P1：`registerReceiver` 未显式指定 export flag~~（本轮已修复，见下方 P1 清单）。
  - P2：`ResourceHooks.mReplaceHook` 热路径 `findContext()` 开销。
  - P3：`toRegex()` 与 `System.java:2094` 的 `forEach(new Consumer())`（用户指示“卡了跳过”，仍待后续处理）。

## 待实机验证

- **项目：** 搜索定位返回行为
  - **条件：** 安装 `CustoMIUIzer-A13-r13.2.2-search-fix-debug.apk`，搜索关键词，点击结果进入目标页，按系统返回或 Toolbar 返回。
  - **预期：** 一次返回直接到达主设置列表（首页），无搜索页闪现，无二次跳转。
  - **状态：** pending
- **项目：** 搜索框旋转/页面重建
  - **条件：** 搜索状态下旋转设备或触发配置变更。
  - **预期：** 搜索关键词与结果列表状态保持或按设计重置，不崩溃。
  - **状态：** pending
- **项目：** 本次代码审查/清理后的模块加载与主要 Hook 行为
  - **条件：** 安装最新的 debug/release APK，检查 LSPosed 日志无新增加载失败、Hook 崩溃或 NPE。
  - **预期：** 模块正常加载，SystemUI/Launcher/Settings 主要功能行为与清理前一致。
  - **状态：** pending

## 本轮证据纠偏

- `80a4ae6` 中预编译 `Pattern` split 已撤销：`GlobalActions.java`、`System.java`、`SystemUI.java` 恢复 `String.split`；`Helpers.java` 删除 `PIPE_SPLIT_PATTERN`/`COLON_SPLIT_PATTERN` 与 `Pattern` import。
- 原因：Android `String.split` 对 `":"` 和 `"\\|"` 走 `Pattern.fastSplit()`，不会每次重新编译正则；预编译 Pattern 没有已证收益，按“无证据不优化”撤销。
- `Helpers.splitPkgAct()` 保留，文档已修正为“固定 `pkg|activity` 格式直接解析”，明确仍创建 `String[]` 与 substring、边界行为与输入检查。
- `ResourceHooks` 表述修正：不再声称消除装箱，改为“单次 `get()` + 懒加载 fallback key，使用原语局部变量减少 map 查询”。
- `PrefMap.keyCache` 已撤销：经遍历 `mPrefs.get*` 调用，发现 41 处含动态 key 拼接，key 不稳定且无界，缓存收益无基准证据，恢复直接拼接。

## A13 架构审计

- 文档：`docs/ARCHITECTURE_AUDIT_A13.md`
- 覆盖：入口/生命周期、PrefMap、ResourceHooks、ModuleHelper、XposedHelpers、Phase 3 模式统计、P0-P3 问题清单。
- P0：0 项（构建与核心 Hook 未发现阻断问题）。
- P1：1 项，`registerReceiver` export flag 在 Android 14 环境隐患。
- P2：1 项，`ResourceHooks` 热路径 `findContext()` 开销。
- P3：2 项，`toRegex()` 与 `forEach(new Consumer())`，用户指示“卡了跳过”，记录待处理。

## 本轮新增文档

- `docs/A13_A14_PARITY_MATRIX.md`：A13 与 A14 `r14.13.5`/`devin/r14.13-kotlin-refactor` 的工程进度差距矩阵，包含 22 个领域、A13/A14 状态、适用性、风险、建议动作和验证方式，以及 A13 Java/Kotlin 文件分层迁移清单。

## P1 registerReceiver export flag 修复

- 扫描并修复 11 处动态 `registerReceiver` 2-arg 调用，为每处显式指定 `Context.RECEIVER_EXPORTED` 或 `RECEIVER_NOT_EXPORTED`：
  - `RECEIVER_NOT_EXPORTED`（仅接收系统广播）：`Controls` SCREEN_ON、`System` TIME_SET/USB_STATE/alarm 时钟、`StepCounterController` TIME_TICK、`WiFiList` 系统 WiFi 广播、`Various` BATTERY_CHANGED。
  - `RECEIVER_EXPORTED`（接收模块自定义跨进程广播）：`GlobalActions` mGlobalReceiver / windowReceiver / mSBReceiver。
  - `RECEIVER_NOT_EXPORTED`（A14 对应实现）：`Launcher` SECRET_CODE。
- 保持 Receiver 成对注销逻辑不变；未改变 hook target、注册顺序或回调语义。
- 构建验证：`test` / `lintDebug` / `lintRelease` / `lintVitalRelease` / `assembleDebug` / `assembleRelease` 全绿。

## 下一步

- 处理 `docs/ARCHITECTURE_AUDIT_A13.md` 中的 P2（`ResourceHooks` 热路径）。
- 按 `docs/A13_A14_PARITY_MATRIX.md` 推进批次 1/2/3 Kotlin 迁移与单元测试。
- 执行真机验证矩阵：LSPosed 加载、SystemUI/Launcher/Settings 主要功能、搜索返回、旋转重建。

## 发布状态

- main 已合并：否
- PR 已创建：否
- tag 已创建：否
- GitHub Release 已创建：否
- APK 已公开上传：否
