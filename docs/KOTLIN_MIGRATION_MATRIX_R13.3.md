# A13 Kotlin 迁移风险矩阵（R13.3 基线）

> 本矩阵基于 `devin/r13.3-kotlin-migration` 分支静态分析生成。
> A14 相关路径仅作为只读工程参考，不代表 A13 采用 A14 包名、Hook target 或代码。

| 文件 | LOC | 所属层 | 所属进程 | 入口 | 静态状态 | 并发 | 动态引用 | A14 对应文件 | 风险 | 建议 | 批次 | 验证要求 |
| ---- | --- | ------ | -------- | ---- | -------- | ---- | -------- | ------------- | ---- | ---- | ---- | -------- |
| `app/src/main/java/name/monwf/customiuizer/subs/System.java` | 662 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/utils/SortableListView.java` | 318 | utility | app/Settings | normal | final constants | none | none | app/src/main/java/tv/withaibuild/customiuizer/utils/SortableListView.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint |
| `app/src/main/java/name/monwf/customiuizer/subs/MultiAction.java` | 314 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/MultiAction.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/ColorSelector.java` | 168 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/ColorSelector.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/ShortcutSelector.java` | 108 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/ShortcutSelector.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/Controls.java` | 98 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/Controls.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/subs/Launcher.java` | 97 | UI | app/Settings | normal | none | none | none | app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
| `app/src/main/java/name/monwf/customiuizer/PrefsProvider.java` | 80 | UI/utility | app/Settings | Manifest/XML | final constants | none | none | app/src/main/java/tv/withaibuild/customiuizer/PrefsProvider.kt (Kotlin) | GREEN | 迁移 | B1 | 单测 / build / lint / UI 回归 |
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

- **GREEN**: 9 个文件
- **YELLOW**: 23 个文件
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

## 长期保留的 Java 边界

以下文件在当前阶段标记为 RED，原因已在矩阵中体现：

- `app/src/main/java/name/monwf/customiuizer/mods/utils/XposedHelpers.java`：getDeclaredMethod, getDeclaredField, findAndHookMethod, hookAllMethods, hookAllConstructors, findMethodExact, findConstructorExact, findField, findClass；process-level mutable。
- `app/src/main/java/name/monwf/customiuizer/MainModule.java`：findAndHookMethod, XposedHelpers.call, XposedModule, onPackageReady, onSystemServerStarting, onModuleLoaded；low risk mutable。
- `app/src/main/java/name/monwf/customiuizer/mods/utils/ModuleHelper.java`：getDeclaredMethod, findAndHookMethod, hookAllMethods, hookAllConstructors, findClass, XposedHelpers.call；process-level mutable。
- `app/src/main/java/name/monwf/customiuizer/mods/utils/HookerClassHelper.java`：none；low risk mutable。
- `app/src/main/java/name/monwf/customiuizer/mods/utils/ResourceHooks.java`：findAndHookMethod, XposedHelpers.call；none。
- `app/src/main/java/org/apache/commons/lang3/reflect/MemberUtilsX.java`：none；none。
