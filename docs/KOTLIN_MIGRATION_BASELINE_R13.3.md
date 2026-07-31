# A13 Kotlin 迁移基线（R13.3）

## 分支与基线

- 仓库：`tomthenpc/customiuizer-a13`
- 基线分支：`main`
- 迁移分支：`devin/r13.3-kotlin-migration`
- 基线 HEAD：733db38fbfeab76bcb5fd1cde11543cd95fef33e
- applicationId：`tv.withaibuild.customiuizer.r13`
- 目标系统：MIUI 14 / Android 13
- libxposed API：`minApiVersion=101`，`targetApiVersion=102`，`staticScope=false`

## 统计方法

物理 LOC 使用 `git ls-files` 过滤后的文件，按换行符计数。
测试代码单独统计，不计入生产代码比例。
GitHub 语言百分比可能受 XML、资源、历史大文件影响，不作为唯一结论。

## 生产代码统计

- 生产 Java 文件：38，LOC：25309
- 生产 Kotlin 文件：49，LOC：3567
- 按文件数量 Kotlin 占比：56.3%
- 按 LOC Kotlin 占比：12.4%

## 测试代码统计

- 测试 Java 文件：0，LOC：0
- 测试 Kotlin 文件：6，LOC：582

## 风险分级汇总

- GREEN：9
- YELLOW：23
- RED：6

## 最大的 20 个 Java 文件（按 LOC）

| 文件 | LOC |
| ---- | --- |
| `app/src/main/java/name/monwf/customiuizer/mods/System.java` | 4857 |
| `app/src/main/java/name/monwf/customiuizer/mods/SystemUI.java` | 4463 |
| `app/src/main/java/name/monwf/customiuizer/mods/utils/XposedHelpers.java` | 1821 |
| `app/src/main/java/name/monwf/customiuizer/mods/Launcher.java` | 1643 |
| `app/src/main/java/name/monwf/customiuizer/mods/Various.java` | 1181 |
| `app/src/main/java/name/monwf/customiuizer/utils/Helpers.java` | 1172 |
| `app/src/main/java/name/monwf/customiuizer/mods/Controls.java` | 1027 |
| `app/src/main/java/name/monwf/customiuizer/mods/GlobalActions.java` | 1027 |
| `app/src/main/java/name/monwf/customiuizer/MainModule.java` | 820 |
| `app/src/main/java/name/monwf/customiuizer/subs/System.java` | 662 |
| `app/src/main/java/name/monwf/customiuizer/utils/AudioVisualizer.java` | 583 |
| `app/src/main/java/name/monwf/customiuizer/PreferenceFragmentBase.java` | 459 |
| `app/src/main/java/name/monwf/customiuizer/SubFragment.java` | 416 |
| `app/src/main/java/name/monwf/customiuizer/utils/BatteryIndicator.java` | 407 |
| `app/src/main/java/name/monwf/customiuizer/mods/utils/ModuleHelper.java` | 385 |
| `app/src/main/java/name/monwf/customiuizer/MainFragment.java` | 381 |
| `app/src/main/java/name/monwf/customiuizer/utils/SortableListView.java` | 318 |
| `app/src/main/java/name/monwf/customiuizer/subs/MultiAction.java` | 314 |
| `app/src/main/java/name/monwf/customiuizer/utils/AppDataAdapter.java` | 289 |
| `app/src/main/java/name/monwf/customiuizer/subs/WiFiList.java` | 279 |

## Java 集中区域

- `mods/`：SystemUI、System、Launcher、Controls、GlobalActions、Various 等大型 Hook 注册中心。
- `mods/utils/`：XposedHelpers、ModuleHelper、ResourceHooks、HookerClassHelper 等 Hook 基础设施。
- `utils/`：Helpers、AudioVisualizer、BatteryIndicator 等通用工具。
- `subs/`：设置子页面与选择器 UI。
- 根目录：`MainModule.java` 为 libxposed 入口。

## 最终目标

1. 所有 GREEN Java 文件迁移完成；
2. YELLOW 文件补测试后尽量迁移，大型 Hook 文件先拆分可独立逻辑；
3. Java 保留文件数量少且边界明确（libxposed 入口、XposedHelpers、反射核心、DexKit、R8 动态入口、第三方 vendored 代码）；
4. 生产代码 Kotlin LOC 显著高于 Java LOC；
5. 不以降低稳定性换取覆盖率。

## A14 只读参考说明

A14 工程结果仅用于判断“哪些 Java 文件在 A14 中仍被保留”以及学习迁移模式。
A13 矩阵中的 A14 对应路径只说明文件存在性，不做迁移必然性判断。
