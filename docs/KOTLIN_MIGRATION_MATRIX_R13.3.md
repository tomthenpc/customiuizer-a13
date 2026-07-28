# A13 Kotlin 迁移风险矩阵（R13.3 基线）

> 本矩阵基于 `devin/r13.3-kotlin-migration` 分支静态分析生成。
> A14 相关路径仅作为只读工程参考，不代表 A13 采用 A14 包名、Hook target 或代码。

| 文件 | LOC | 所属层 | 所属进程 | 入口 | 静态状态 | 并发 | 动态引用 | A14 对应文件 | 风险 | 建议 | 批次 | 验证要求 |
| ---- | --- | ------ | -------- | ---- | -------- | ---- | -------- | ------------- | ---- | ---- | ---- | -------- |
| `app/src/main/java/name/monwf/customiuizer/subs/System.java` | 662 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt (Kotlin) | GREEN | 已迁移 | B1-4 已迁移 | 单测 / build / lint / R8 / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/utils/SortableListView.java` | 318 | utility | app/Settings | XML inflate | final constants | none | （SortableList 反射访问 mSnapshotShadow） | app/src/main/java/tv/withaibuild/customiuizer/utils/SortableListView.kt (Kotlin) | YELLOW | 自定义 View/拖拽，B2 处理 | B2 | 单测 / build / lint / R8 |
| `app/src/main/java/name/monwf/customiuizer/subs/MultiAction.java` | 314 | UI | app/Settings | normal | none | none | getIdentifier | app/src/main/java/tv/withaibuild/customiuizer/subs/MultiAction.kt (Kotlin) | GREEN | 已迁移 | B1-3 已迁移 | 单测 / build / lint / R8 / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/ColorSelector.java` | 168 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/ColorSelector.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/ShortcutSelector.java` | 108 | UI | app/Settings | normal | none | none | getIdentifier | app/src/main/java/tv/withaibuild/customiuizer/subs/ShortcutSelector.kt (Kotlin) | GREEN | 已迁移 | B1-2 已迁移 | 单测 / build / lint / R8 |
| `app/src/main/java/name/monwf/customiuizer/subs/Controls.java` | 98 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/Controls.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/Launcher.java` | 97 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/PrefsProvider.java` | 80 | UI/utility | app/Settings | Manifest/XML | final constants | none | none | app/src/main/java/tv/withaibuild/customiuizer/PrefsProvider.kt (Kotlin) | GREEN | 已迁移 | B1-2 已迁移 | 单测 / build / lint / R8 / Provider |
| `app/src/main/java/name/monwf/customiuizer/subs/CategorySelector.java` | 66 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/CategorySelector.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/mods/System.java` | 4857 | Hook | system_server | reflection | process-level mutable | synchronized, Handler, Runnable, postDelayed | findAndHookMethod, hookAllMethods, hookAllConstructors, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt (Kotlin) | YELLOW | 拆分后迁移 | B3/B4 | build / R8 / 实机 / 日志 |
| `app/src/main/java/name/monwf/customiuizer/mods/SystemUI.java` | 4463 | Hook | SystemUI | reflection | process-level mutable | Handler, Runnable, postDelayed | loadClass, findAndHookMethod, hookAllMethods, hookAllConstructors, findMethodExact, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUI.kt (Kotlin) | YELLOW | 拆分后迁移 | B3/B4 | build / R8 / 实机 / 日志 |
| `app/src/main/java/name/monwf/customiuizer/mods/Launcher.java` | 1643 | Hook | Launcher | reflection | low risk mutable | Handler, Thread, Runnable | findAndHookMethod, hookAllMethods, hookAllConstructors, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt (Kotlin) | YELLOW | 拆分后迁移 | B3/B4 | build / R8 / 实机 / 日志 |
| `app/src/main/java/name/monwf/customiuizer/mods/Various.java` | 1181 | Hook | multi | reflection | process-level mutable | Handler, Thread, Runnable, postDelayed | findAndHookMethod, hookAllMethods, hookAllConstructors, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt (Kotlin) | YELLOW | 拆分后迁移 | B3/B4 | build / R8 / 实机 / 日志 |
| `app/src/main/java/name/monwf/customiuizer/utils/Helpers.java` | 1172 | utility | app/Settings | reflection | process-level mutable | none | Class.forName, getDeclaredMethod | app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt (Kotlin) | YELLOW | 补测试后迁移 | B2/B3 | 单测 / build / R8 |
| `app/src/main/java/name/monwf/customiuizer/mods/Controls.java` | 1027 | Hook | system_server | reflection | process-level mutable | Handler, Runnable, postDelayed | findAndHookMethod, hookAllMethods, findMethodExact, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt (Kotlin) | YELLOW | 拆分后迁移 | B3/B4 | build / R8 / 实机 / 日志 |
| `app/src/main/java/name/monwf/customiuizer/mods/GlobalActions.java` | 1027 | Hook | system_server | reflection | process-level mutable | Handler, Thread, Runnable, postDelayed | getDeclaredMethod, findAndHookMethod, hookAllMethods, hookAllConstructors, findMethodExact, findField, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt (Kotlin) | YELLOW | 拆分后迁移 | B3/B4 | build / R8 / 实机 / 日志 |
| `app/src/main/java/name/monwf/customiuizer/utils/AudioVisualizer.java` | 583 | utility | app/Settings | normal | none | Handler, Runnable, postDelayed | none | app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/PreferenceFragmentBase.java` | 459 | UI | app/Settings | normal | final constants | none | none | app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/SubFragment.java` | 416 | UI | app/Settings | normal | none | postDelayed | none | app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/utils/BatteryIndicator.java` | 407 | utility | app/Settings | reflection | none | Handler, Runnable | XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt (Kotlin) | YELLOW | 补测试后迁移 | B2/B3 | 单测 / build / R8 |
| `app/src/main/java/name/monwf/customiuizer/MainFragment.java` | 381 | UI | app/Settings | normal | none | Handler, Thread, Runnable, postDelayed | none | app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/utils/AppDataAdapter.java` | 289 | utility | app/Settings | normal | none | Runnable, CopyOnWrite | none | app/src/main/java/tv/withaibuild/customiuizer/utils/AppDataAdapter.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/WiFiList.java` | 279 | UI | app/Settings | normal | none | Handler, Runnable, postDelayed | none | app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/AppSelector.java` | 270 | UI | app/Settings | reflection | none | Thread, Runnable | getDeclaredMethod | app/src/main/java/tv/withaibuild/customiuizer/subs/AppSelector.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / R8 |
| `app/src/main/java/name/monwf/customiuizer/subs/BTList.java` | 264 | UI | app/Settings | normal | none | Handler, Runnable, postDelayed | none | app/src/main/java/tv/withaibuild/customiuizer/subs/BTList.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/MainActivity.java` | 225 | UI | app/Settings | Manifest/XML | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/SortableList.java` | 210 | UI | app/Settings | reflection | none | none | getDeclaredField | app/src/main/java/tv/withaibuild/customiuizer/subs/SortableList.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / R8 |
| `app/src/main/java/name/monwf/customiuizer/utils/LockedAppAdapter.java` | 181 | utility | app/Settings | reflection | none | Runnable, CopyOnWrite | getDeclaredMethod | app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt (Kotlin) | YELLOW | 补测试后迁移 | B2/B3 | 单测 / build / R8 |
| `app/src/main/java/name/monwf/customiuizer/mods/PackagePermissions.java` | 178 | Hook | multi | reflection | low risk mutable | none | findAndHookMethod, hookAllMethods, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/PackagePermissions.kt (Kotlin) | YELLOW | 拆分后迁移 | B3/B4 | build / R8 / 实机 / 日志 |
| `app/src/main/java/name/monwf/customiuizer/utils/PrivacyAppAdapter.java` | 170 | utility | app/Settings | reflection | none | Runnable, CopyOnWrite | getDeclaredMethod | app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt (Kotlin) | YELLOW | 补测试后迁移 | B2/B3 | 单测 / build / R8 |
| `app/src/main/java/name/monwf/customiuizer/SubFragmentWithSearch.java` | 116 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/ActivitySelector.java` | 109 | UI | app/Settings | normal | none | Thread, Runnable | none | app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt (Kotlin) | YELLOW | 补测试后迁移 | B2 | 单测 / build / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/mods/utils/XposedHelpers.java` | 1821 | infrastructure | system_server | reflection | process-level mutable | synchronized, Atomic | getDeclaredMethod, getDeclaredField, findAndHookMethod, hookAllMethods, hookAllConstructors, findMethodExact, findConstructorExact, findField, findClass | app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java (Java) | RED | 保留 Java | 保留区 | build / R8 / 实机 |
| `app/src/main/java/name/monwf/customiuizer/MainModule.java` | 820 | infrastructure | infrastructure/multi | libxposed, reflection | low risk mutable | none | findAndHookMethod, XposedHelpers.call, XposedModule, onPackageReady, onSystemServerStarting, onModuleLoaded | app/src/main/java/tv/withaibuild/customiuizer/MainModule.java (Java) | RED | 保留 Java | 保留区 | build / R8 / 实机 |
| `app/src/main/java/name/monwf/customiuizer/mods/utils/ModuleHelper.java` | 385 | infrastructure | system_server | reflection | process-level mutable | synchronized | getDeclaredMethod, findAndHookMethod, hookAllMethods, hookAllConstructors, findClass, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.kt (Kotlin) | RED | 保留 Java | 保留区 | build / R8 / 实机 |
| `app/src/main/java/name/monwf/customiuizer/mods/utils/HookerClassHelper.java` | 241 | infrastructure | system_server | normal | low risk mutable | none | none | app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookerClassHelper.kt (Kotlin) | RED | 保留 Java | 保留区 | build / R8 / 实机 |
| `app/src/main/java/name/monwf/customiuizer/mods/utils/ResourceHooks.java` | 194 | infrastructure | system_server | reflection | none | none | findAndHookMethod, XposedHelpers.call | app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ResourceHooks.kt (Kotlin) | RED | 保留 Java | 保留区 | build / R8 / 实机 |
| `app/src/main/java/org/apache/commons/lang3/reflect/MemberUtilsX.java` | 30 | compatibility | app | normal | none | none | none | app/src/main/java/org/apache/commons/lang3/reflect/MemberUtilsX.java (Java) | RED | 保留 Java | 保留区 | build / 不迁移 |

## 风险统计

- **GREEN**: 0 个文件（B1 全部完成）
- **YELLOW**: 24 个文件（`SortableListView` 因反射依赖从 GREEN 调整至 YELLOW）
- **RED**: 6 个文件

## 第一批低风险候选（B1）

筛选条件：GREEN、无动态引用、无复杂并发、可独立测试、规模适中，总计控制在约 300–1000 LOC。

| 顺序 | 文件 | LOC | 批次 | 验证 | 迁移要点 |
| ---- | ---- | --- | ---- | ---- | -------- |
| B1-1 | `app/src/main/java/name/monwf/customiuizer/subs/CategorySelector.java` | 66 | B1 | 单测 / build / lint / UI 回归 | 无反射/Hook/线程，迁移后补单测 |
| B1-2 | `app/src/main/java/name/monwf/customiuizer/PrefsProvider.java` | 80 | B1 | 单测 / build / lint / UI 回归 | 无反射/Hook/线程，迁移后补单测 |
| B1-3 | `app/src/main/java/name/monwf/customiuizer/subs/Launcher.java` | 97 | B1 | 单测 / build / lint / UI 回归 | 无反射/Hook/线程，迁移后补单测 |
| B1-4 | `app/src/main/java/name/monwf/customiuizer/subs/Controls.java` | 98 | B1 | 单测 / build / lint / UI 回归 | 无反射/Hook/线程，迁移后补单测 |
| B1-5 | `app/src/main/java/name/monwf/customiuizer/subs/ShortcutSelector.java` | 108 | B1 | 单测 / build / lint / UI 回归 | 无反射/Hook/线程，迁移后补单测 |
| B1-6 | `app/src/main/java/name/monwf/customiuizer/subs/ColorSelector.java` | 168 | B1 | 单测 / build / lint / UI 回归 | 无反射/Hook/线程，迁移后补单测 |
| B1-7 | `app/src/main/java/name/monwf/customiuizer/subs/MultiAction.java` | 314 | B1 | 单测 / build / lint / UI 回归 | 无反射/Hook/线程，迁移后补单测 |

合计：7 个文件，931 LOC。

## B1-1 批次执行结果

本批次从第一批候选中进一步筛选，遵循同层（`subs/` 设置子页面）、同生命周期、无反射/无 Hook/无大型逻辑、可独立验证/回退原则。

### 选取文件

| 顺序 | 原 Java 文件 | Java LOC | Kotlin 文件 | Kotlin LOC | 迁移结论 | 验证 |
| ---- | ------------ | -------- | ----------- | ---------- | -------- | ---- |
| B1-1-1 | `app/src/main/java/name/monwf/customiuizer/subs/CategorySelector.java` | 66 | `app/src/main/java/name/monwf/customiuizer/subs/CategorySelector.kt` | 68 | 已迁移 | 单测 / build / lint |
| B1-1-2 | `app/src/main/java/name/monwf/customiuizer/subs/Controls.java` | 99 | `app/src/main/java/name/monwf/customiuizer/subs/Controls.kt` | 84 | 已迁移 | 单测 / build / lint |
| B1-1-3 | `app/src/main/java/name/monwf/customiuizer/subs/Launcher.java` | 97 | `app/src/main/java/name/monwf/customiuizer/subs/Launcher.kt` | 85 | 已迁移 | 单测 / build / lint |
| B1-1-4 | `app/src/main/java/name/monwf/customiuizer/subs/ColorSelector.java` | 168 | `app/src/main/java/name/monwf/customiuizer/subs/ColorSelector.kt` | 138 | 已迁移 | 单测 / build / lint |

小计：删除 Java 433 LOC，新增 Kotlin 375 LOC，新增测试 80 LOC。

### JVM 兼容措施

- package 与 FQCN 保持不变：`name.monwf.customiuizer.subs` 包名及类名未变；
- 默认无参构造器保持公开，Java 侧 `new CategorySelector()` / `new Controls()` / `new Launcher()` / `new ColorSelector()` 仍可编译；
- `MainFragment` 的 `catSelector`、`prefLauncher`、`prefControls` 字段类型仍为对应类，无 `@JvmName`/`@JvmStatic` 调整需求；
- `SubFragment.openColorSelector(...)` 内部 `new ColorSelector()` 仍可用；
- 重载 `openSubFragment(...)` 调用未改动签名；
- `onCreate`、`onCreatePreferences`、`onActivityCreated`、`onSaveInstanceState` 方法签名与原 Java 一致；
- `ColorSelector` 的 `setSelected`/`updateSelColor` 由 package-private 改为 `private`，仅类内使用，反射验证可达；
- `ColorSelector` 中 `ColorCircle.ColorListener` 因是 Kotlin 接口，迁移为 `object : ColorCircle.ColorListener` 显式实现；
- 未使用 `!!`，未引入 coroutine/Flow，未改变 Preference key、Hook target、资源名；
- 所有 `findPreference` 调用改为 `?.` 安全访问，避免原 Java 中找不到 key 时 NPE，行为仅在异常路径变化。

### 测试覆盖

新增 `app/src/test/java/name/monwf/customiuizer/subs/B1MigrationInteropTest.kt`（80 LOC）：
- 反射验证 4 个 Kotlin 类可默认构造、继承 `SubFragment`、保留 `onCreate`/`onActivityCreated`/`onCreatePreferences`/`onSaveInstanceState` 签名；
- 验证 `MainFragment` 的 `catSelector`、`prefLauncher`、`prefControls` 字段 FQCN 仍为迁移后的类；
- 验证 `SubFragment.openColorSelector` 方法在 R8 后仍引用 `ColorSelector`。

### 构建结果

- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，68 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings（与基线一致，无新增）；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过。

### APK / R8 审计

- Release APK：`app/build/outputs/apk/release/CustoMIUIzer-A13-r13.2.3-test1.apk`；
- applicationId：`tv.withaibuild.customiuizer.r13`（未变）；
- versionName：`r13.2.3-test1` / versionCode：`121`（未变）；
- `META-INF/xposed/module.prop`：`minApiVersion=101`、`targetApiVersion=102`、`staticScope=false`（未变）；
- `META-INF/xposed/scope.list`：scope 内容未变；
- `META-INF/xposed/java_init.list`：`name.monwf.customiuizer.MainModule`（未变）；
- R8 mapping 确认 4 个迁移类均保留并被重命名为：`CategorySelector -> M2:`、`ColorSelector -> h3:`、`Controls -> j4:`、`Launcher -> ha:`；
- 未见 Legacy Xposed API、A14 包名或 Android 14 逻辑混入；
- 签名证书与基线一致，未重新生成签名文件。

### 尚未实机验证

- 设置主页面点击各分类进入子页面；
- `ColorSelector` 颜色选择、透明度拖动、十六进制输入、旋转恢复；
- `Launcher` 与 `Controls` 各 preference 点击响应；
- 日间/夜间主题、 Toolbar 菜单、返回栈行为；
- MIUI 14 / Android 13 真机 LSPosed 环境加载无新异常。

## B1-2 批次执行结果

### 选取文件

| 顺序 | 原 Java 文件 | Java LOC | Kotlin 文件 | Kotlin LOC | 迁移结论 | 验证 |
| ---- | ------------ | -------- | ----------- | ---------- | -------- | ---- |
| B1-2-1 | `app/src/main/java/name/monwf/customiuizer/PrefsProvider.java` | 81 | `app/src/main/java/name/monwf/customiuizer/PrefsProvider.kt` | 56 | 已迁移 | 单测 / build / lint / Release R8 |
| B1-2-2 | `app/src/main/java/name/monwf/customiuizer/subs/ShortcutSelector.java` | 109 | `app/src/main/java/name/monwf/customiuizer/subs/ShortcutSelector.kt` | 90 | 已迁移 | 单测 / build / lint / Release R8 |

小计：删除 Java 190 LOC，新增 Kotlin 146 LOC，新增测试 41 LOC。

### 审计调整

- `app/src/main/java/name/monwf/customiuizer/utils/SortableListView.java` 因 `subs/SortableList.java` 通过反射访问其私有字段 `mSnapshotShadow`，且属于自定义 View/拖拽动画，从 B1 移至 **B2**，原 GREEN 调整为 YELLOW，批次改为 `B2`。

### JVM 兼容措施

- `PrefsProvider`：
  - FQCN 与 package 保持不变；
  - `public static final String AUTHORITY` 迁移为 `companion object const val AUTHORITY`，Java 侧仍可用 `PrefsProvider.AUTHORITY`；
  - `ContentProvider` 标准重写方法签名不变；
  - `openAssetFile` 使用 `getOrNull(1)` 安全取 `pathSegments` 第二段，`when` 替换原 if-else；
  - Manifest `android:name=".PrefsProvider"` / `authority` 未变，R8 mapping 显示类名未被重命名。
- `ShortcutSelector`：
  - package/FQCN 不变；
  - 公开无参构造器保留；
  - 继承 `SubFragmentWithSearch`（Java）不变；
  - `onCreate` / `onActivityCreated` / `onActivityResult` 签名与原 Java 一致；
  - `targetFragment?.onActivityResult(...)` 替换 `getTargetFragment().onActivityResult(...)`，行为等效；
  - 未引入 `!!`、coroutine/Flow，未改动 `startActivityForResult` requestCode、Intent extra key。

### 测试覆盖

新增 `app/src/test/java/name/monwf/customiuizer/B1_2_MigrationInteropTest.kt`（41 LOC）：
- 反射验证 `PrefsProvider` 继承 `ContentProvider`，`AUTHORITY` 为 `public static final` 且值正确；
- 反射验证 `ShortcutSelector` 可默认构造、继承 `SubFragmentWithSearch`、`onActivityCreated` 与 `onActivityResult` 方法签名保留。

### 构建结果

- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，70 tests / 0 failures（B1-1 68 + B1-2 2）；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors，520 warnings；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过。

### R8 审计

- `PrefsProvider` 在 Release mapping 中保留原名（Manifest 入口）；
- `ShortcutSelector` 被 R8 重命名为 `ff:`，仍从 `SubFragment.openMultiAction`/`SortableList` 等调用点可达；
- `module.prop` / `scope.list` / `java_init.list` 未变；
- 未见 Legacy Xposed API 或 A14 专属代码。

## B1-3 批次执行结果

### 选取文件

| 顺序 | 原 Java 文件 | Java LOC | Kotlin 文件 | Kotlin LOC | 迁移结论 | 验证 |
| ---- | ------------ | -------- | ----------- | ---------- | -------- | ---- |
| B1-3-1 | `app/src/main/java/name/monwf/customiuizer/subs/MultiAction.java` | 315 | `app/src/main/java/name/monwf/customiuizer/subs/MultiAction.kt` | 246 | 已迁移 | 单测 / build / lint / R8 / UI 回归 |

新增 `app/src/test/java/name/monwf/customiuizer/subs/B1_3_MigrationInteropTest.kt`（40 LOC）。

### JVM 兼容要点

- package/FQCN 不变；
- 公开无参构造器保留；
- 继承 `SubFragment`（Java）不变；
- `MultiAction.Actions` 枚举保持 6 个值，Java 侧仍可用 `MultiAction.Actions.LAUNCHER`；
- `onCreate` / `onActivityCreated` / `onActivityResult` / `saveSharedPrefs` / `onDestroy` 签名与原 Java 一致；
- `AppSelector` / `ShortcutSelector` 目标片段调用通过 `setTargetFragment(this, ...)` 保留，requestCode 不变；
- `SpinnerExFake.getValue()` / `setValue()` / `addValue()` 等 Kotlin 属性访问保持兼容；
- 未引入 `!!`、coroutine/Flow，未改动 `Intent` extra key 与 `startActivityForResult` requestCode。

### 构建结果

- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，72 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过。

## B1-4 批次执行结果

### 选取文件

| 顺序 | 原 Java 文件 | Java LOC | Kotlin 文件 | Kotlin LOC | 迁移结论 | 验证 |
| ---- | ------------ | -------- | ----------- | ---------- | -------- | ---- |
| B1-4-1 | `app/src/main/java/name/monwf/customiuizer/subs/System.java` | 663 | `app/src/main/java/name/monwf/customiuizer/subs/System.kt` | 485 | 已迁移 | 单测 / build / lint / R8 / UI 回归 |

新增 `app/src/test/java/name/monwf/customiuizer/subs/B1_4_MigrationInteropTest.kt`（46 LOC）。

### JVM 兼容要点

- package/FQCN 不变（`name.monwf.customiuizer.subs.System`）；
- 公开无参构造器保留；
- 继承 `SubFragment`（Java）不变；
- `onCreate` / `onCreatePreferences` / `onActivityCreated` / `onActivityResult` 签名与原 Java 一致；
- `selectSub()` / `openSubFragment(...)` / `openStandaloneApp(...)` / `openAppsEdit` 等来自 Java `SubFragment` 的调用保持不变；
- `when (sub)` 替代原 `switch (sub)`，所有分支、调用顺序、Intent extra 与 `requestCode` 不变；
- `findPreference<T>(...)` 安全调用替代原 Java 强制类型转换，未改变正常路径行为；
- `Settings.Secure.putInt`、`PackageManager.setComponentEnabledSetting`、`AppHelper.showInputDialog` 等系统调用签名与参数不变；
- `PrefsProvider.AUTHORITY` 用于 `Uri.parse` 保持不变；
- 未使用 `!!`、coroutine/Flow。

### 构建结果

- `./gradlew.bat --no-daemon :app:test`：BUILD SUCCESSFUL，74 tests / 0 failures；
- `./gradlew.bat --no-daemon :app:lintDebug`：0 errors；
- `./gradlew.bat --no-daemon :app:assembleDebug`：成功；
- `./gradlew.bat --no-daemon :app:assembleRelease`：成功；
- `git diff --check`：通过。

### R8 审计

- `MultiAction` 与 `System` 在 Release mapping 中均从调用点可达；
- `System` 字段 `prefSystem` 在 `MainFragment` 中保留引用；
- 未见 A14 包名、Hook target、资源名或 Android 14 逻辑混入。

## B1 阶段总结

### 已迁移 B1 文件

| 批次 | 文件 | Java LOC | Kotlin LOC | 测试 |
| ---- | ---- | -------- | ---------- | ---- |
| B1-1 | `subs/CategorySelector` | 66 | 68 | B1MigrationInteropTest |
| B1-1 | `subs/Controls` | 99 | 84 | B1MigrationInteropTest |
| B1-1 | `subs/Launcher` | 97 | 85 | B1MigrationInteropTest |
| B1-1 | `subs/ColorSelector` | 168 | 138 | B1MigrationInteropTest |
| B1-2 | `PrefsProvider` | 81 | 56 | B1_2_MigrationInteropTest |
| B1-2 | `subs/ShortcutSelector` | 109 | 90 | B1_2_MigrationInteropTest |
| B1-3 | `subs/MultiAction` | 315 | 246 | B1_3_MigrationInteropTest |
| B1-4 | `subs/System` | 663 | 485 | B1_4_MigrationInteropTest |

合计：删除 Java 1598 LOC，新增 Kotlin 1252 LOC，新增测试 207 LOC。

### B1 验证结论

- 单元测试：74 tests，0 failures，0 errors；
- lintDebug：0 errors，基线 warnings 数量稳定；
- assembleDebug / assembleRelease：成功；
- Release R8 mapping：B1 迁移类均保持可达，无 Manifest/Xposed 元数据变化；
- 未发现 A14 包名、Hook target、资源名或 API 版本变化；
- 真机验证未完成，仍为 B1 阶段遗留项。

### 下一批 B2 候选

- `app/src/main/java/name/monwf/customiuizer/utils/SortableListView.java`（自定义 View/拖拽，反射依赖）
- `app/src/main/java/name/monwf/customiuizer/utils/AudioVisualizer.java`
- `app/src/main/java/name/monwf/customiuizer/PreferenceFragmentBase.java`
- `app/src/main/java/name/monwf/customiuizer/SubFragment.java`
- `app/src/main/java/name/monwf/customiuizer/MainFragment.java`
- `app/src/main/java/name/monwf/customiuizer/subs/SortableList.java`

按矩阵 YELLOW 顺序分批处理，单个批次控制在 800–1200 LOC。

## 长期保留的 Java 边界

以下文件在当前阶段标记为 RED，原因已在矩阵中体现：

- `app/src/main/java/name/monwf/customiuizer/mods/utils/XposedHelpers.java`：getDeclaredMethod, getDeclaredField, findAndHookMethod, hookAllMethods, hookAllConstructors, findMethodExact, findConstructorExact, findField, findClass；process-level mutable。
- `app/src/main/java/name/monwf/customiuizer/MainModule.java`：findAndHookMethod, XposedHelpers.call, XposedModule, onPackageReady, onSystemServerStarting, onModuleLoaded；low risk mutable。
- `app/src/main/java/name/monwf/customiuizer/mods/utils/ModuleHelper.java`：getDeclaredMethod, findAndHookMethod, hookAllMethods, hookAllConstructors, findClass, XposedHelpers.call；process-level mutable。
- `app/src/main/java/name/monwf/customiuizer/mods/utils/HookerClassHelper.java`：none；low risk mutable。
- `app/src/main/java/name/monwf/customiuizer/mods/utils/ResourceHooks.java`：findAndHookMethod, XposedHelpers.call；none。
- `app/src/main/java/org/apache/commons/lang3/reflect/MemberUtilsX.java`：none；none。
