# CLASSIFY-static-strong-android-owner

- Platform: A13
- Status: Done
- Priority: P2
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

对 `source_hazard_scan.py` 报告的 `STATIC_STRONG_ANDROID_OWNER` 274 条命中进行分类，
区分真实静态强引用与普通的局部变量、实例字段，并确保扫描器只保留有决策价值的命中。

## 当前问题

`docs/audit/SOURCE_HAZARD_BASELINE.json` 中记录 274 条 `STATIC_STRONG_ANDROID_OWNER`。
逐一分析后发现扫描规则噪音极高，原因是正则过于宽松：

- **标识符后缀误伤**：例如 `private var isActivity = false` 被拆成 `is` + `Activity`，
  `val inSearchView = ...` 被拆成 `inSearch` + `View`，当成 Android owner 类型命中。
- **推断类型命中**：`val mContext = ...` 等没有显式类型的声明，仅靠变量名中的
  `Context` 等字样被命中。
- **局部变量命中**：函数内部的 `val mContext: Context = ...` 或
  `Context context = ...` 被当成字段扫描出来。
- **普通实例字段命中**：`class` 中的 `private val ctx: Context`、Fragment/Adapter 等
  生命周期内的实例字段被当成静态强引用。

## 分类结果

修复后扫描器仅命中 **9** 处真实静态/单例强引用，其它 265 条全部归类为噪音或普通字段：

| 类别 | 数量 | 说明 |
|------|------|------|
| 真实静态 `Context` 强引用 | 8 | Kotlin `object` 的 `private var` 字段，JVM 层面是 `static`，对应 `system_server`/SystemUI/模块单例服务 |
| 真实 `public static Context` 强引用 | 1 | `ModuleHelper.mModuleContext`，模块级全局 Context |
| 普通实例字段 | 11 | 位于 `class` 内部的 Adapter/Fragment/View/Preference 等实例字段，非 `static` 或 `object` |
| 局部变量 | 20 | 方法/构造函数/初始化块内声明，生命周期短 |
| 名称后缀/推断类型误报 | ~234 | `isActivity`、`inSearchView`、无显式类型的 `val mContext` 等 |

### 真实静态强引用（已标记 `BRUTAL_ALLOW`）

| 文件 | 行 | 字段 | 说明 |
|------|----|------|------|
| `mods/Controls.kt` | 59 | `sScreenOnContext` | `object Controls` 单例；`system_server` `PhoneWindowManager` 的 `mContext`，每次重赋值前先反注册旧 receiver |
| `mods/Controls.kt` | 60 | `sPowerContext` | 同上，`Power` 键处理对应 Context |
| `mods/Controls.kt` | 87 | `sVolumeContext` | 同上，`Volume` 键处理对应 Context |
| `mods/Controls.kt` | 591 | `miuiPWMContext` | `object Controls` 单例；Miui 指纹电源管理 Context |
| `mods/Controls.kt` | 737 | `basePWMContext` | `object Controls` 单例；Base 电源管理 Context |
| `mods/GlobalActions.kt` | 72 | `mGlobalReceiverContext` | `object GlobalActions` 单例；`system_server`/`SystemUI` 单例 Context，重赋值前反注册旧 receiver |
| `mods/GlobalActions.kt` | 73 | `mSBReceiverContext` | 同上，StatusBar 对应 Context |
| `mods/utils/StepCounterController.kt` | 181 | `sContext` | `object StepCounterController` 单例；存的是 `context.applicationContext`，`destroy()` 时清空 |
| `mods/utils/ModuleHelper.java` | 60 | `mModuleContext` | `public static Context`；模块自身 `createDeviceProtectedStorageContext()`，全局单点 |

### 普通实例字段（被新的作用域过滤排除）

| 文件 | 行 | 字段 | 说明 |
|------|----|------|------|
| `PreferenceFragmentBase.kt` | 38 | `actContext` | Fragment 实例字段 |
| `subs/BTList.kt` | 40 | `mAppContext` | Fragment 实例字段 |
| `subs/ColorSelector.kt` | 24 | `selectedColorView` | Fragment 实例字段 |
| `subs/WiFiList.kt` | 44 | `mAppContext` | Fragment 实例字段 |
| `utils/LockedAppAdapter.kt` | 26 | `ctx` | Adapter 实例字段，与 Adapter 同生命周期 |
| `utils/PrivacyAppAdapter.kt` | 26 | `ctx` | Adapter 实例字段，与 Adapter 同生命周期 |
| `utils/ResolveInfoAdapter.kt` | 22 | `ctx` | Adapter 实例字段，与 Adapter 同生命周期 |
| `utils/SortableListView.kt` | 48/49 | `mSnapshot...` | 自定义 View 实例字段，绘制快照 |
| `mods/utils/ModuleHelper.java` | 674 | `applicationContext` | 类实例字段（`ReceiverRegistration`） |
| `mods/utils/ResourceHooks.java` | 322 | `mContext` | 类实例字段 |

### 局部变量（被新的作用域过滤排除）

共 20 处，分布在 `installers/SystemUiInstaller.java`、`mods/Controls.kt`、
`mods/LauncherGestureHooks.kt`、`mods/SystemAudioAndVisualAndMoreHooks.kt`、
`mods/SystemFreeformAndMultiWindowHooks.kt`、`mods/SystemUIControlCenterHooks.kt`、
`mods/SystemUILockScreenHooks.kt`、`mods/utils/ModuleHelper.java`、
`mods/utils/ResourceHooks.java`、`subs/BTList.kt`、`subs/WiFiList.kt`、
`utils/AppDataAdapter.kt`、`utils/AppLocaleController.kt`、`utils/PreferenceAdapter.kt`。

均为方法体/构造函数内声明，生命周期受方法作用域限制，不属于静态强引用。

## 实现改动

- `tools/source_hazard_scan.py`
  - 修复 `STATIC_STRONG_ANDROID_OWNER` 正则：要求显式类型、加 `\b` 词边界、
    泛型部分限制在单行、空白符不跨行，避免 `isActivity`/`inSearchView` 等名称后缀误报。
  - 增加 `fun`/`class`/`object` 作用域追踪：`STATIC_STRONG_ANDROID_OWNER`
    仅保留以下两类字段：
    - Kotlin `object` / `companion object` 内部字段（JVM 层面为 `static`）。
    - Java 声明中带 `static` 关键字的字段。
  - 局部变量与 `class` 普通实例字段不再命中。

- 生产源码
  - 对上述 9 处真实静态强引用字段追加 `// BRUTAL_ALLOW:STATIC_STRONG_ANDROID_OWNER`，
    与既有 `@SuppressLint("StaticFieldLeak")` 或块注释共同说明其为受管单例 Context。

## 验收标准

- [x] `STATIC_STRONG_ANDROID_OWNER` 从 274 条收敛到 0
- [x] 真实静态强引用（9 处）已明确标记并说明
- [x] 局部变量与普通实例字段不再被扫描器误报
- [x] `python tools/source_hazard_scan.py --path app/src/main/java` 通过（0 new）
- [x] `python -m compileall tools` 通过
- [x] `python -m unittest discover -s tools/tests -p "test_*.py"` 通过
- [x] `docs/audit/SOURCE_HAZARD_BASELINE.json` 已更新

## 验证

```powershell
python tools/source_hazard_scan.py --path app/src/main/java
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
```

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: 42f0d78ae88af5e08759685726cc5499df08c276
- Final SHA: 本记录所在的收口 commit
- Commits: 1
- Behavior changed: 否，仅扫描器规则与注释
- Verification: source_hazard_scan.py / compileall / python unit tests
- Device evidence: 无（本任务不涉及行为变化，STATIC_VERIFIED）
- Known limits: 9 处单例 Context 保持既有所有权模型，未改为 WeakReference
