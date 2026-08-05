# EVALUATE-needloadprefs-knowledge-sink

- Platform: A13
- Status: Done
- Priority: P4
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

评估 `MainModule.needLoadPrefs` 中沉淀的功能知识，并提出“功能知识下沉”方案，
使 `MainModule` 不再直接掌握具体 feature 的 preference key/app-list 映射。

## 当前问题

`MainModule.java` 中 `needLoadPrefs(String pkg, SharedPreferences prefs)` 硬编码了
四条功能开关及其对应的“目标应用列表” preference key：

| 功能开关 key | 目标应用列表 key | 所属 feature | 当前位置 |
|---|---|---|---|
| `pref_key_various_alarmcompat` | `pref_key_various_alarmcompat_apps` | 闹钟兼容（Various） | `mods/Various.kt` |
| `pref_key_system_statusbarcolor` | `pref_key_system_statusbarcolor_apps` | 状态栏着色（System） | `subs/System.kt` |
| `pref_key_system_nooverscroll` | `pref_key_system_nooverscroll_apps` | 去除过度滚动（System） | `subs/System.kt` |
| `pref_key_controls_mediaplayer` | `pref_key_controls_mediaplayer_apps` | 媒体播放器控制（Controls） | `subs/Controls.kt` |

这些 key 的“所有者”在 UI/Settings 层（`Various.kt`、`System.kt`、`Controls.kt`），
但“是否需要为该包加载 prefs”的判定逻辑却放在 `MainModule` 中，
造成 `MainModule` 必须知道每个 feature 的 preference key 才能做加载决策。

## 调用链

```text
XposedModule.onPackageReady
  -> ProcessScopes.resolve(pkg, processName)
  -> if ProcessScopes.isRejected -> return
  -> getRemotePrefs()
  -> if remote == null || !needLoadPrefs(pkg, remote) -> return
  -> initPrefs()   // 加载 PrefMap
  -> 根据 ProcessScope 调用对应 Installer
```

`needLoadPrefs` 是在 `initPrefs()` 之前执行的“粗筛”：
只要没有任何 feature 需要为该包工作，就不加载远程 preference，
避免为无关 first-package 做完整的 `PreferenceBootstrap.start()`。

## 障碍

现有 `FeatureCatalog` / `PreferenceSchema` 已具备 `ownerFeature` 和
`preferenceKeys` 概念，但存在两个关键差异：

1. **`FeatureSpec.condition` 使用 `PrefMap`，而 `needLoadPrefs` 使用 `SharedPreferences`。**
   `PrefMap` 是 `initPrefs()` 之后的产物，`needLoadPrefs` 却在 `initPrefs()` 之前运行。
   因此不能直接把 `FeatureSpec.condition` 当成加载前判定条件。

2. **目标应用列表 key 尚未进入 `PreferenceSchema`。**
   这四条 `*_apps` key 只在 XML 和 `MainModule` 中出现，
   `PreferenceSchema` 没有声明它们，也没有 `ownerFeature` 归属。

3. **`MainModule.onPackageReady` 走的是 legacy `Installer` 路由，不是 `FeatureCatalog`。**
   目前 installer 层还没有统一的“我需要为这个包加载 prefs”接口。

## 下沉方案

### 方案 A：新增 `PreferenceLoadPredicate` 接口（推荐）

在 `mods/catalog` 中新增一个与 `PrefMap` 无关的接口：

```kotlin
fun interface PreferenceLoadPredicate {
    fun shouldLoad(remote: SharedPreferences, pkg: String): Boolean
}
```

每个 feature 在 `FeatureCatalog` 的 `FeatureSpec` 旁边声明自己的
`preferenceLoadPredicate`；`FeatureCatalog` 提供一个聚合入口：

```kotlin
object FeatureLoadRegistry {
    fun shouldLoadAny(remote: SharedPreferences, pkg: String): Boolean
}
```

`MainModule.needLoadPrefs` 替换为：

```java
if (remote == null || !FeatureLoadRegistry.INSTANCE.shouldLoadAny(remote, pkg)) return;
```

- **优点**：`MainModule` 完全不需要知道具体 key；feature 自己拥有加载条件；
  与现有 `FeatureCatalog` / `PreferenceSchema` 保持同一owner语义。
- **缺点**：需要为 legacy `*_apps` key 补充 `PreferenceSchema` 条目，
  并让相关 `FeatureSpec` 声明 predicate；涉及 catalog 与 installer 的衔接。

### 方案 B：把 `needLoadPrefs` 搬到 `ProcessScopes`

由 `ProcessScopes` 维护一个静态 `Map<String, Set<String>>` 描述
“feature -> (enableKey, appListKey)” 对，`MainModule` 只调用
`ProcessScopes.needsPreferences(pkg, remote)`。

- **优点**：改动小，集中管理。
- **缺点**：只是把集中地点从 `MainModule` 移到 `ProcessScopes`，
  没有真正把知识还给 feature owner，知识仍然停留在框架层。

### 方案 C：移除 `needLoadPrefs`，always `initPrefs()`

对非 rejected 的 first-package 都调用 `initPrefs()`，把过滤推迟到各 installer
或 `FeatureSpec.condition`。

- **优点**：彻底去掉 `MainModule` 对 key 的依赖。
- **缺点**：会为更多 first-package 执行 `PreferenceBootstrap.start()`，
  可能轻微增加冷启动 I/O；需要确认性能影响。

## 评估结论

- **当前 `MainModule.needLoadPrefs` 确实存在功能知识泄漏**：
  它掌握了 `Various`/`System`/`Controls` 三处的 preference key 与 app-list key。
- **最合适的下沉路径是方案 A**：通过 `PreferenceLoadPredicate` 把判定权还给
  `FeatureCatalog`，`MainModule` 只负责调用聚合入口。
- **实施前提**：先把四条 `*_apps` key 加入 `PreferenceSchema`，
  并建立对应的 `FeatureSpec` 或 legacy installer predicate。
- **风险评估**：这是一个架构调整，会改变 `MainModule` 与 installer/catalog 的边界。
  在没有明确的 runtime 验证需求时，建议先以本评估报告收口，
  后续用一个单独的 `REFACTOR` slice 实施方案 A 的具体迁移。

## 验收标准

- [x] 完成 `MainModule.needLoadPrefs` 的知识归属分析
- [x] 列出所有被 `MainModule` 硬编码的 preference key 及其真实 owner
- [x] 评估 `FeatureCatalog` / `PreferenceSchema` 对下沉的适用性
- [x] 提出至少三种候选方案并比较优缺点
- [x] 给出明确推荐方案与实施前提
- [x] 不引入未经评估的代码改动

## 验证

```powershell
python tools/check-invariants.py
python tools/source_hazard_scan.py --path app/src/main/java
```

（本任务为评估，无源码改动。）

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: f88f077
- Final SHA: 本记录所在的收口 commit
- Commits: 1
- Behavior changed: 否
- Verification: check-invariants / source_hazard_scan
- Device evidence: 无（本任务不涉及行为变化，STATIC_VERIFIED）
