# DEVIN R13 CHECKPOINT

> 本文件记录当前真实状态，不记录长期规则。长期规则统一放在根目录 `AGENTS.md`。
> 每完成一个有意义的代码、构建、Git 或实机闭环，立即更新本文件；替换过期事实，不要只在末尾追加。

## 当前目标

- 完成 B1 / B2-1 / B2-2 / B2-3 / B2-4 / B2-5 / B2-6 / B2-7 / B2-8 / B2-9 Java → Kotlin 迁移；
- 完成 A13 Claude 审计修复适配；
- 完成 B3 Hook 层拆分审计，完成 B3-1 `mods/PackagePermissions`、B3-2 `mods/Various` 迁移到 Kotlin；
- 按阶段自动验证 build / lint / Release 并 push `devin/r13.3-kotlin-migration`。

## 当前基线

- **Repository:** `tomthenpc/customiuizer-a13`
- **Branch:** `devin/r13.3-kotlin-migration`
- **Last verified commit:** `bd6cc772440789d335a4bd1fd7d766934bbe2b07`
- **versionName / versionCode:** `r13.2.4-devin` / `123`
- **applicationId:** `tv.withaibuild.customiuizer.r13`
- **libxposed API:** `minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`
- **Hot Reload:** `false`
- **Legacy Xposed API:** `false`
- **SDK:** `minSdk=33`，`targetSdk=34`，`compileSdk=36`
- **ABI:** `arm64-v8a`
- **本轮新增/更新文档:**
  - `docs/KOTLIN_MIGRATION_BASELINE_R13.3.md`
  - `docs/KOTLIN_MIGRATION_MATRIX_R13.3.md`（已记录 B1-1 结果与 B1-2 候选）
- **Java 文件风险统计:** GREEN=0，YELLOW=9，RED=6（B2-9 后 `LockedAppAdapter`、`PrivacyAppAdapter` 从 YELLOW 移除）
- **B3 已迁移文件:** `PackagePermissions`（B3-1）、`Various`（B3-2）、`SystemUI`（K4）
- **B2 已迁移文件:** `SortableListView`、`SortableList`、`SubFragmentWithSearch`、`ActivitySelector`、`MainActivity`、`MainFragment`、`PreferenceFragmentBase`、`SubFragment`、`AudioVisualizer`、`AppDataAdapter`、`WiFiList`、`AppSelector`、`BTList`、`LockedAppAdapter`、`PrivacyAppAdapter`
- **B1+B2 代码规模:** 删除 Java 5874 LOC，新增 Kotlin 5290 LOC，新增测试 771 LOC
- **B2 剩余候选:** 无
- **最后正常行为基线:** `MonwF/customiuizer v23.11.26`

## 本轮已完成（A14 工程对齐 Pre-release 批次）

### 工程审计与文档
- 创建 `docs/A13_A14_ENGINEERING_PARITY.md`：Git、构建、签名、API 101/102、生命周期、热路径、Locale、R8 等差距矩阵。
- 创建 `docs/LSPOSED_FULL_LOG_REVIEW_PROTOCOL.md`：动态日志路径审查协议。
- 创建 `.devin/ACTIVE_TASK.md`：当前 Pre-release 任务状态。

### 可移植追赶项
- 新增 `PrefPair.kt` 与 `PrefPairTest.kt`：`first`/`second`/`firstEquals`/`containsFirst` 避免 `toRegex()` 分配。
- `AppHelper.removeStringPair`、`PreferenceAdapter.updateItems`、`Helpers.containsStringPair` 改为使用 `PrefPair`。
- `SystemUI.mStatusbarTextIcons` 从 `ArrayList<View>` 改为 `WeakReference<View>`，注册与遍历自动清理失效引用。

### 构建配置
- `gradle.properties`：移除 `android.enableResourceOptimizations` 与 `org.gradle.unsafe.configuration-cache`；启用 `org.gradle.configuration-cache=true` 与 `org.gradle.caching=true`。
- `app/build.gradle.kts`：`release`/`develop` 签名缺失时 `check()` fail-fast；设置统一 APK 输出名 `CustoMIUIzer-A13-<versionName>.apk`。

### 验证
- 完整构建：`clean test lint lintRelease lintVitalRelease assembleDebug assembleDevelop assembleRelease` 全部成功。
- 测试：57 个单元测试通过（含新增 `PrefPairTest`）。
- Release APK：`tv.withaibuild.customiuizer.r13` / `r13.2.3-test1` / `121` / v2 签名 / R8 / resource shrink / zipalign 对齐 / `module.prop` / `scope.list` / `java_init.list` 正确。
- 已安全桥接 `devin/r13.2-kotlin-api102` 与 `main`（无共同祖先），`main` 已 fast-forward 到 release 分支，并推送 origin。
- 已创建并推送 annotated tag `r13.2.3-test1`。
- 已创建 GitHub Pre-release 并上传签名 APK。

## 本轮已完成（Kotlin 迁移基线 R13.3）

### 文档与审计
- 生成 `docs/KOTLIN_MIGRATION_BASELINE_R13.3.md`：生产/测试代码统计、Java/Kotlin 文件分布、迁移目标与风险概览。
- 生成 `docs/KOTLIN_MIGRATION_MATRIX_R13.3.md`：38 个生产 Java 文件的层、进程、入口、静态状态、并发、动态引用、A14 对应、风险、批次、验证要求、第一批候选清单。
- 生成工具：`build_matrix.py`（仓库外临时脚本，仅用于本阶段文档生成）。

### 风险分级结果
- **GREEN（9 个）:** `CategorySelector`, `ColorSelector`, `Controls`, `Launcher`, `MultiAction`, `ShortcutSelector`, `SortableListView`, `PrefsProvider`, `subs/System`（`System.java` 过大，列入 B1 候选但未进入第一批）。
- **YELLOW（23 个）:** 基础 UI/生命周期组件、`mods/*` Hook 中心、含 `Handler`/`Thread`/`Executor`/`CopyOnWrite` 的 utility、含 `getDeclaredMethod`/`getDeclaredField` 的 adapter。
- **RED（6 个）:** `MainModule.java`、`XposedHelpers.java`、`ModuleHelper.java`、`HookerClassHelper.java`、`ResourceHooks.java`、`org/apache/commons/lang3/reflect/MemberUtilsX.java`。

### 第一批候选（B1）
- 7 个文件，合计 931 LOC：`CategorySelector` → `PrefsProvider` → `Launcher` → `Controls` → `ShortcutSelector` → `ColorSelector` → `MultiAction`。
- 筛选条件：GREEN、无类反射、无复杂并发、静态状态简单、独立可测、总 LOC ≤ ~1000。

### 验证
- `git diff --check`：通过。
- `.​gradlew.bat --no-daemon test assembleDebug`：BUILD SUCCESSFUL，57 tests / 0 failures / 0 errors。

## 本轮已完成（B1-1 批次 Kotlin 迁移）

### 迁移文件
- `app/src/main/java/name/monwf/customiuizer/subs/CategorySelector.java` → `CategorySelector.kt`（66 → 68 LOC）
- `app/src/main/java/name/monwf/customiuizer/subs/Controls.java` → `Controls.kt`（99 → 84 LOC）
- `app/src/main/java/name/monwf/customiuizer/subs/Launcher.java` → `Launcher.kt`（97 → 85 LOC）
- `app/src/main/java/name/monwf/customiuizer/subs/ColorSelector.java` → `ColorSelector.kt`（168 → 138 LOC）

小计：删除 Java 433 LOC，新增 Kotlin 375 LOC；新增 `B1MigrationInteropTest.kt`（80 LOC）。

### JVM 兼容要点
- package / FQCN / 公开无参构造器保持不变；
- `MainFragment` 的 `catSelector`、`prefLauncher`、`prefControls` 字段类型仍为对应 Kotlin 类；
- `SubFragment.openColorSelector` 中 `new ColorSelector()` 仍可用；
- 生命周期方法签名（`onCreate`/`onCreatePreferences`/`onActivityCreated`/`onSaveInstanceState`）保持不变；
- 未使用 `!!`、coroutine/Flow、深层 scope，未改动 Preference key / Hook target / 资源名。

### 验证
- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，68 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings（基线持平）；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过；
- Release APK 审计：applicationId / versionName / versionCode / `module.prop` / `scope.list` / `java_init.list` 均未变；R8 mapping 确认 4 个类均保留。

### 待实机验证
- 设置主页面点击各分类进入子页面；
- `ColorSelector` 颜色选择、透明度拖动、十六进制输入、旋转恢复；
- `Launcher` / `Controls` 各 preference 点击响应；
- 日间/夜间主题、Toolbar 菜单、返回栈行为；
- MIUI 14 / Android 13 真机 LSPosed 加载无新增异常。

### 下一批候选（B2-7）
- `AudioVisualizer`（583 LOC，音频可视化工具类）

> 继续按 YELLOW 矩阵分批处理，单个批次控制在 800–1200 LOC。

## 本轮已完成（B2-4 批次 Kotlin 迁移）

### 迁移文件
- `app/src/main/java/name/monwf/customiuizer/MainActivity.java` → `MainActivity.kt`（226 → 199 LOC）
- `app/src/main/java/name/monwf/customiuizer/MainFragment.java` → `MainFragment.kt`（382 → 355 LOC）

小计：删除 Java 608 LOC，新增 Kotlin 554 LOC；新增 `B2_4_MigrationInteropTest.kt`（88 LOC）。

### JVM 兼容要点
- package / FQCN / 公开无参构造器保持不变；
- `MainActivity` 生命周期与菜单方法签名保持不变，`SharedPreferences` 到 `RemotePreferences` 同步逻辑保持原 Java 行为；
- `MainFragment` 搜索状态恢复、搜索框展开/收起、`findMod` 与 `openModCat` 行为保持一致；
- `prefSystem` / `prefLauncher` / `prefControls` / `prefVarious` / `isSearchFocused` / `inSearchView` / `lastFilter` 使用 `@JvmField` 保持公开字段；
- 已废弃 API 调用（`MenuItemCompat`、`onActivityCreated`、`targetFragment`）已加 `@Suppress("DEPRECATION")`；
- 未使用 `!!`、coroutine/Flow，未改动 preference key / Hook target / 资源名。

### 验证
- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，89 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings（基线持平）；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过；
- Release APK 审计：applicationId / versionName / versionCode / `module.prop` / `scope.list` / `java_init.list` 均未变；R8 mapping 确认 `MainActivity` / `MainFragment` 保留。

## 本轮已完成（B2-9 批次 Kotlin 迁移）

### 迁移文件

- `app/src/main/java/name/monwf/customiuizer/utils/LockedAppAdapter.java` → `LockedAppAdapter.kt`（181 → 177 LOC）
- `app/src/main/java/name/monwf/customiuizer/utils/PrivacyAppAdapter.java` → `PrivacyAppAdapter.kt`（170 → 168 LOC）
- 新增 `app/src/test/java/name/monwf/customiuizer/utils/B2_9_MigrationInteropTest.kt`（48 LOC）

小计：删除 Java 351 LOC，新增 Kotlin 345 LOC；新增测试 48 LOC。

### JVM 兼容要点

- `LockedAppAdapter` / `PrivacyAppAdapter`：公开继承 `BaseAdapter` / `Filterable`；保留 `Context` + `ArrayList<AppData>` 两参构造器；`getCount` / `getItem` / `getItemId` / `getView` / `getFilter` 签名不变；`LockedAppAdapter` 额外保留 `isEnabled` 方法；
- `mSecurityManager` 与反射 `getDeclaredMethod` 在 `init` 中初始化，失败时安全回退，不拖垮列表；
- `ItemFilter` 为私有内部类，继续从 `originalAppList` 过滤并写入 `filteredAppList`（`CopyOnWriteArrayList`），`publishResults` 后重新 `sortList`；
- 未引入 `!!`、coroutine/Flow；未改动 preference key / Hook target / 资源名。

### 验证

- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，111 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings（基线持平）；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过；
- Release APK 审计：applicationId / versionName / versionCode / `module.prop` / `scope.list` / `java_init.list` 均未变；R8 mapping 确认 B2-9 迁移类保留。

## 本轮已完成（B2-8 批次 Kotlin 迁移）

### 迁移文件

- `app/src/main/java/name/monwf/customiuizer/utils/AppDataAdapter.java` → `AppDataAdapter.kt`（289 → 312 LOC）
- `app/src/main/java/name/monwf/customiuizer/subs/WiFiList.java` → `WiFiList.kt`（279 → 256 LOC）
- `app/src/main/java/name/monwf/customiuizer/subs/AppSelector.java` → `AppSelector.kt`（270 → 289 LOC）
- `app/src/main/java/name/monwf/customiuizer/subs/BTList.java` → `BTList.kt`（264 → 230 LOC）
- 新增 `app/src/test/java/name/monwf/customiuizer/subs/B2_8_MigrationInteropTest.kt`（110 LOC）

小计：删除 Java 1102 LOC，新增 Kotlin 1087 LOC；新增测试 110 LOC。

### JVM 兼容要点

- `AppDataAdapter`：公开继承 `BaseAdapter` / `Filterable`；保留三 / 四 / 五参数构造器；`getCount` / `getItem` / `getItemId` / `getView` / `getFilter` / `updateSelectedApps` 签名不变；`ItemFilter` 为私有内部类；继续使用 `ThreadPoolExecutor` 与 `BitmapCachedLoader`；避免 `toRegex()` 拆分，使用 `lowercase(Locale)` 与 `removeAll { }`。
- `WiFiList` / `BTList`：保留公开无参构造器；继承 `SubFragment` 不变；`onCreate` / `onActivityCreated` / `onResume` / `onPause` / `onDestroy` 签名不变；`registerReceivers` / `unregisterReceivers` / `updateProgressBar` / `isWiFiReady` / `isLocationServicesEnabled` / `fetchCachedDevices` 公开方法保留；`WiFiAdapter` / `BTAdapter` 内部类保留；使用 `Context.RECEIVER_NOT_EXPORTED` / `RECEIVER_EXPORTED` 注册；避免 `toRegex()` 拆分 BSSID/地址。
- `AppSelector`：继承 `SubFragmentWithSearch`；保留公开无参构造器；`onCreate` / `onActivityCreated` / `onActivityResult` 签名不变；保留 `setTargetFragment`（已 `@Suppress("DEPRECATION")`）与 `targetRequestCode`；继续使用 `Thread` 加载应用列表；`Helpers.MimeType` 位运算逻辑不变。

### 验证

- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，107 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings（基线持平）；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过；
- Release APK 审计：applicationId / versionName / versionCode / `module.prop` / `scope.list` / `java_init.list` 均未变；R8 mapping 确认 B2-8 迁移类保留。

## 本轮已完成（B2-7 批次 Kotlin 迁移）

### 迁移文件
- `app/src/main/java/name/monwf/customiuizer/utils/AudioVisualizer.java` → `AudioVisualizer.kt`（583 → 603 LOC）

小计：删除 Java 583 LOC，新增 Kotlin 603 LOC；新增 `B2_7_MigrationInteropTest.kt`（68 LOC）。

### JVM 兼容要点
- package / FQCN / 公开三参/两参/单参构造器（`Context` / `AttributeSet` / `defStyle`）保持不变；
- 继承 `android.view.View` 不变；
- 公开枚举 `BarStyle`、`ColorMode`、`RenderType` 保留为 `inner enum class`；
- `isScreenOn`、`showOnCustom`、`colorMode`、`barStyle`、`renderType`、`glowLevel`、`customColor`、`showInDrawer`、`showWithControllerOnly` 以 `@JvmField` 保持公开字段；
- 公开方法 `setPlaying`、`updateViewState`、`updateScreenOn`、`updateMusicArt`、`setColor`、`setBitmap`、`onDraw`、`onSizeChanged`、`hasOverlappingRendering` 签名不变；
- 静态方法 `allZeros(byte[])` 以 `companion object @JvmStatic` 保留；
- `PaletteTask`（`AsyncTask`）保持并加 `@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")`；
- 未使用 `!!`、coroutine/Flow，未改动 preference key / Hook target / 资源名。

### 验证
- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，98 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings（基线持平）；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过；
- Release APK 审计：applicationId / versionName / versionCode / `module.prop` / `scope.list` / `java_init.list` 均未变；R8 mapping 确认 `AudioVisualizer` 保留。

## 本轮已完成（B2-5/B2-6 批次 Kotlin 迁移）

### 迁移文件
- `app/src/main/java/name/monwf/customiuizer/PreferenceFragmentBase.java` → `PreferenceFragmentBase.kt`（459 → 406 LOC）
- `app/src/main/java/name/monwf/customiuizer/SubFragment.java` → `SubFragment.kt`（416 → 430 LOC）

小计：删除 Java 875 LOC，新增 Kotlin 836 LOC；新增 `B2_5_6_MigrationInteropTest.kt`（97 LOC）。

### JVM 兼容要点
- `PreferenceFragmentBase`：
  - package / FQCN / 公开无参构造器保持不变；
  - 继承 `PreferenceFragmentCompat` 不变；
  - `PICK_BACKFILE` / `SAVE_BACKFILE` 以 `companion object const val` 保持公开静态常量；
  - `toolbarMenu` / `activeMenus` / `headLayoutId` / `tailLayoutId` / `isCustomActionBar` / `pageUrl` / `mapKeys` 以 `@JvmField protected` 保持字段访问语义；
  - `getActionBar` / `onCreateOptionsMenu` / `onOptionsItemSelected` / `onCreate` / `onCreatePreferences` / `onViewCreated` / `openSubFragment` / `backupSettings` / `restoreSettings` / `doRestoreSettings` / `onActivityResult` 签名不变；
  - 已废弃 API 调用已加 `@Suppress("DEPRECATION")`；
  - 未使用 `!!`、coroutine/Flow。
- `SubFragment`：
  - package / FQCN / 公开无参构造器保持不变；
  - 继承 `PreferenceFragmentBase`（Kotlin）不变；
  - `settingsType` / `abType` / `settingTitle` / `padded` / `sub` / `catInfo` / `isStandalone` / `openAppsEdit` 等公开字段使用 `@JvmField` 保持公开字段；
  - `onCreate` / `onCreatePreferences` / `onCreateView` / `onViewCreated` / `onActivityCreated` / `onStart` / `saveSharedPrefs` / `loadSharedPrefs` / `selectSub` / `finish` / `confirmEdit` 签名不变；
  - `openColorSelector` 字段保留，原 `openColorSelector(Preference)` 方法重命名为 `doOpenColorSelector(Preference)` 以避免 Kotlin 属性/方法命名冲突，调用点与外部使用只依赖字段；
  - `setTargetFragment` 已废弃调用已加 `@Suppress("DEPRECATION")`；
  - 未使用 `!!`、coroutine/Flow。

### 验证
- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，94 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings（基线持平）；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过；
- Release APK 审计：applicationId / versionName / versionCode / `module.prop` / `scope.list` / `java_init.list` 均未变；R8 mapping 确认 `PreferenceFragmentBase` / `SubFragment` 保留。

## 本轮已完成（B1-2 批次 Kotlin 迁移）

### 迁移文件
- `app/src/main/java/name/monwf/customiuizer/PrefsProvider.java` → `PrefsProvider.kt`（81 → 56 LOC）
- `app/src/main/java/name/monwf/customiuizer/subs/ShortcutSelector.java` → `ShortcutSelector.kt`（109 → 90 LOC）

小计：删除 Java 190 LOC，新增 Kotlin 146 LOC；新增 `B1_2_MigrationInteropTest.kt`（41 LOC）。

### JVM 兼容要点
- `PrefsProvider` FQCN/package 不变，`AUTHORITY` 为 `companion object const val`，Java 侧仍可读 `PrefsProvider.AUTHORITY`；
- `ContentProvider` 重写方法签名不变，公开无参构造器保留；
- Manifest `android:name=".PrefsProvider"` / `authorities` 未变，R8 mapping 显示类名未被重命名；
- `ShortcutSelector` 继承 `SubFragmentWithSearch`（Java）不变，公开无参构造器保留；
- `onCreate` / `onActivityCreated` / `onActivityResult` 签名一致；
- 未使用 `!!`、coroutine/Flow，未改动 `startActivityForResult` requestCode 与 Intent extra key。

### 验证
- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，70 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过；
- R8 mapping 确认 `PrefsProvider` 保留原名，`ShortcutSelector` 可达。

### 审计调整
- `SortableListView` 因 `SortableList` 反射访问 `mSnapshotShadow`，且属于自定义 View/拖拽，从 B1 移至 B2（YELLOW）。

## 历史批次（全局代码审查、Kotlin 清理与高频优化第二批）

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

- **任务/命令：** `git diff --check` + `\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:lintVitalRelease :app:assembleDebug :app:assembleRelease`
- **结果：** BUILD SUCCESSFUL
- **验证日期：** 2026-07-29
- **git diff --check：** 0 errors
- **单元测试：** 129 tests，0 failures，0 errors
- **lintDebug / lintRelease / lintVitalRelease：** 0 errors
- **assembleDebug / assembleRelease：** 成功
- **Release APK：** `app/build/outputs/apk/release/CustoMIUIzer-A13-r13.2.4-devin.apk`
- **当前代码变更范围：** K4 完成 `mods/SystemUI.java` 拆分迁移为 8 个 Kotlin 文件并删除原 Java

## 当前问题与阻塞

- B3 剩余 Hook 层：`mods/System`（B3-5）待按批次迁移；`SystemUI` 已于 K4 完成拆分迁移；
- 真机验证未完成：LSPosed 加载、SystemUI/Launcher/Settings 主要功能、搜索返回、旋转重建均待确认；
- B1-1 4 个设置子页面仍需在 MIUI 14 / Android 13 真机上验证 UI 行为与返回栈。
- API 101/102 实机边界未验证：未在只支持 API 101 的环境运行。
- 已记录的 P1/P2/P3 问题，详见 `docs/ARCHITECTURE_AUDIT_A13.md`：
  - ~~P1：`registerReceiver` 未显式指定 export flag~~（本轮已修复，见下方 P1 清单）。
  - ~~P2：`ResourceHooks.mReplaceHook` 热路径 `findContext()` 开销~~（本轮已修复，见下方 P2 清单）。
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
- P1：0 项（已修复）。
- P2：0 项（已修复）。
- P3：2 项，`toRegex()` 与 `forEach(new Consumer())`，用户指示“卡了跳过”，记录待处理。
- `MainModule` / `RemotePreferences`：空 `SharedPreferences` 快照不再固化 `prefsLoaded`；`watchPreferenceChange` 无条件注册并在成功后设置 `prefsWatcherRegistered`；监听回调按旧值类型读取；`ModuleHelper` 移除 `processHooked` 死引用。

## 本轮新增文档

- `docs/A13_A14_PARITY_MATRIX.md`：A13 与 A14 `r14.13.5`/`devin/r14.13-kotlin-refactor` 的工程进度差距矩阵，包含 22 个领域、A13/A14 状态、适用性、风险、建议动作和验证方式，以及 A13 Java/Kotlin 文件分层迁移清单。

## P1 registerReceiver export flag 修复

- 扫描并修复 11 处动态 `registerReceiver` 2-arg 调用，为每处显式指定 `Context.RECEIVER_EXPORTED` 或 `RECEIVER_NOT_EXPORTED`：
  - `RECEIVER_NOT_EXPORTED`（仅接收系统广播）：`Controls` SCREEN_ON、`System` TIME_SET/USB_STATE/alarm 时钟、`StepCounterController` TIME_TICK、`WiFiList` 系统 WiFi 广播、`Various` BATTERY_CHANGED。
  - `RECEIVER_EXPORTED`（接收模块自定义跨进程广播）：`GlobalActions` mGlobalReceiver / windowReceiver / mSBReceiver。
  - `RECEIVER_NOT_EXPORTED`（A14 对应实现）：`Launcher` SECRET_CODE。
- 保持 Receiver 成对注销逻辑不变；未改变 hook target、注册顺序或回调语义。
- 构建验证：`test` / `lintDebug` / `lintRelease` / `lintVitalRelease` / `assembleDebug` / `assembleRelease` 全绿。

## P2 ResourceHooks 热路径优化

- 优化 `mReplaceHook.intercept`：先按 `resId` 查 `fakes` 表，命中或 `replacements` 非空时才调用 `ModuleHelper.findContext()`；`chain.getExecutable().getName()` 也延迟到命中路径。
- 未命中时直接 `chain.proceed()`，避免每次系统资源访问都执行反射/ActivityThread 探测。
- 保持 `fakes`/`replacements` 写入路径、`applyHooks()`、`getFakeResource()`、`getResourceReplacement()` 语义不变；`getDimensionPixelOffset` / `getDimensionPixelSize` Float 转 Int 逻辑保留。
- 构建验证：`test` / `lintDebug` / `lintRelease` / `lintVitalRelease` / `assembleDebug` / `assembleRelease` 全绿；lint warnings 519 / 510 与基线持平。

## 本轮新增工程资产

- `app/src/main/java/name/monwf/customiuizer/utils/AppLocaleController.kt`：单一状态源语言控制器，统一 `Locale.setDefault`、`AppCompatDelegate.setApplicationLocales`、语言选择器绑定与 `Context` 派生。
- `app/src/test/.../AppLocaleControllerTest.kt`：17 个覆盖 `normalizeLocaleTag` / `getUserLocale` / `getEffectiveLocale` / `setUserLocale` / `buildLocaleDisplayData` / `toLocaleListCompat` / 状态转换的单元测试。
- `app/src/test/.../FakeSharedPreferences.kt`：用于单元测试的内存 SharedPreferences 实现。
- `app/src/test/.../SearchStateMachineTest.kt`：10 个状态机转换单元测试。
- `app/src/test/.../SearchRouteResolverTest.kt`：11 个搜索路由解析单元测试。

## 本轮主要改动

- `AppLocaleController`：接管 `MainApplication.onCreate`、`MainActivity.attachBaseContext`、`MainFragment` 语言选择器、`AppHelper.getLocaleContext` / `getProtectedContext` 的 locale 逻辑。
- `MainApplication` 移除 `attachBaseContext` 中直接 `Locale.setDefault`，改为 `onCreate` 调用 `AppLocaleController.applyLocale`。
- `MainActivity.attachBaseContext` 改为 `AppLocaleController.getLocaleContext(base, AppHelper.appPrefs)`。
- `MainFragment` 语言选择器改为 `AppLocaleController.setupLocalePreference`。
- `AppHelper.getLocaleContext` 委托给 `AppLocaleController`。
- `app/build.gradle.kts` 添加 `testOptions.unitTests.isReturnDefaultValues = true` 以支持 `AppLocaleController` 单元测试中的 Android `Log` / `Resources` 调用。
- 新增 `SearchNavigation.kt`：纯 JVM 可测的 `SearchRoute`/`SearchRouteResolver`/`SearchStateMachine`。
- `MainFragment` 搜索流程改为三态 `IDLE/SEARCHING/NAVIGATED`，返回后通过 `shouldClearOnReturn` 清理搜索视图，修复搜索页闪现/二次返回问题。
- `MainFragment.openModCat` 使用 `SearchRouteResolver` 判断子分类选择器与直接跳转，统一返回 `true`/`false` 语义并处理 `onPreferenceTreeClick` 空指针。
- `MainModule` 空 `SharedPreferences` 快照不再固化 `prefsLoaded`，避免模块在 provider 未就绪时永久以空配置运行；`watchPreferenceChange` 无条件注册并在成功后设置 `prefsWatcherRegistered`。
- `MainModule` 偏好变化监听回调按旧值类型读取（Boolean/Integer/Long/Float/String/Set），避免每次 `getAll()` 全量复制并减少跨进程快照成本。
- `ModuleHelper` 移除所有 `MainModule.processHooked = true` 死引用并整理 `hookAllConstructors` / `hookAllMethods` 空块。

## 下一步

- 按 `docs/KOTLIN_MIGRATION_MATRIX_R13.3.md` 推进 B1 批次（7 个候选文件）的 Java → Kotlin 迁移。
- 每个迁移文件配对单元测试，确保 `build / lint / R8 / 实机` 不回归。
- 继续完成 B2/B3/B4 批次，逐步降低 Java 占比，但保留 RED 边界文件。

## 发布状态

- 迁移分支 `devin/r13.3-kotlin-migration` 已创建并准备提交/推送。
- main 已合并：否
- PR 已创建：否
- tag 已创建：否
- GitHub Release 已创建：否
- APK 已公开上传：否
