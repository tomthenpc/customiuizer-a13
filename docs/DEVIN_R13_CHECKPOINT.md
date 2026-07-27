# DEVIN R13 CHECKPOINT

> 本文件记录当前真实状态，不记录长期规则。长期规则统一放在根目录 `AGENTS.md`。
> 每完成一个有意义的代码、构建、Git 或实机闭环，立即更新本文件；替换过期事实，不要只在末尾追加。

## 当前目标

- 对 A13 项目进行全局代码审查、死代码清理、Kotlin 惯用化与性能优化（低频低风险优先），保持功能行为、JVM/Hook 兼容和构建稳定。

## 当前基线

- **Repository:** `tomthenpc/customiuizer-a13`
- **Branch:** `devin/r13.2-kotlin-api102`
- **HEAD:** `362cf83`
- **versionName / versionCode:** `r13.2.2-devin` / `120`
- **applicationId:** `tv.withaibuild.customiuizer.r13`
- **libxposed API:** `minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
- **Hot Reload:** `false`
- **Legacy Xposed API:** `false`
- **SDK:** `minSdk=33`，`targetSdk=34`，`compileSdk=36`
- **ABI:** `arm64-v8a`
- **最新已确认实机版本:** 未确认
- **最后正常行为基线:** `MonwF/customiuizer v23.11.26`

## 本轮已完成（全局代码审查与 Kotlin 清理第一批）

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

### 测试与构建
- `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest`：BUILD SUCCESSFUL（`cleanup-build3.log`）。
- `./gradlew --no-daemon :app:assembleRelease`：BUILD SUCCESSFUL，包含 `lintVitalRelease`（`release-build.log`）。
- `./gradlew --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`：BUILD SUCCESSFUL（`cleanup-build2.log`）。
- lintDebug：0 errors / 527 warnings（大量 `UnusedResources` 为 `getIdentifier` 动态引用导致，未删除）。

### Git
- 当前工作区有 15 个文件变更：`*.kt` 修改 + `SoundData.kt` 删除；未跟踪的 `.vscode/`、APK、构建日志保持未提交。

### 文档
- 更新 `docs/DEVIN_R13_CHECKPOINT.md` 为代码审查阶段状态。

## 最新绿色验证

- **任务/命令：** `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest`
- **结果：** BUILD SUCCESSFUL
- **产物：** `cleanup-build3.log`
- **验证日期：** 2026-07-27
- **任务/命令：** `./gradlew --no-daemon :app:assembleRelease`
- **结果：** BUILD SUCCESSFUL（包含 lintVitalRelease）
- **产物：** `release-build.log`

## 当前问题与阻塞

- 无。

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

## 下一步

- 提交并 push 当前代码审查/清理第一批到 `devin/r13.2-kotlin-api102`。
- 继续 Phase 5 高频路径专项审查：重点检查 `mods/*.java` 中 `mPrefs.getStringSet`/`getBoolean` 是否可被缓存、Hook 回调中是否存在重复反射/对象分配、主线程阻塞等问题。

## 发布状态

- main 已合并：否
- PR 已创建：否
- tag 已创建：否
- GitHub Release 已创建：否
- APK 已公开上传：否
