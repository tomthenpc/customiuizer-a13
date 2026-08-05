# OPTIMIZE-needloadprefs-knowledge-sink

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

将 `MainModule.java` 中 `needLoadPrefs()` 掌握的具体功能 preference key 和目标应用列表判断下沉到独立的 `PreferenceLoadRegistry`。

完成后，`MainModule` 只负责：

1. 获取 remote `SharedPreferences`；
2. 调用统一的 `PreferenceLoadRegistry.shouldLoad(remote, pkg)`；
3. 判断是否执行 `initPrefs()`；
4. 继续现有 ProcessScope / Installer 路由。

`MainModule` 不再掌握任何具体 feature preference key。

## 当前问题

`MainModule` 当前内聚了 9 个具体 preference key 的判断逻辑，包括三组布尔开关+应用集合和一组音量媒体控制解析。这使得 `MainModule` 承担了本不属于它的 feature 知识，难以扩展和测试。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceLoadRegistry.kt`（新增）
- `app/src/main/java/tv/withaibuild/customiuizer/MainModule.java`
- `app/src/test/java/tv/withaibuild/customiuizer/prefs/PreferenceLoadRegistryTest.kt`（新增）
- `tools/tests/test_main_module_routing.py`

## 必须保持

- 运行时行为完全不变；
- known package 始终加载 prefs；
- remote 为 null 时直接返回；
- rejected process 在获取 remote prefs 前返回；
- `initPrefs()` 的调用次数和位置不变；
- 所有 ProcessScope / Installer 路由不变；
- watcher 时序不变；
- Volume Media Control 的短路和异常行为与旧 Java 实现一致。

## 实现要求

1. 新增 `PreferenceLoadRegistry.kt`，包含：
   - `fun interface PreferenceLoadPredicate`
   - `internal data class PreferenceLoadRule`
   - `object PreferenceLoadRegistry` 与 `shouldLoad(prefs, packageName)`
2. 完整迁移四组 legacy 判断：
   - Alarm Compat
   - Status Bar Color
   - No Overscroll
   - Volume Media Control
3. `MainModule.java` 删除 `needLoadPrefs`、`isPrefEnabled`、`isInPrefSet`、`isVolumeMediaEnabled` 及相关 import、preference key 字面量。
4. 使用 `PreferenceLoadRegistry.shouldLoad(remote, pkg)` 替代原有判断。
5. Java 可通过静态入口调用；rules 只初始化一次；不捕获 `Throwable`；不引入协程或异步调度。
6. 不修改 `FeatureSpec`、`FeatureCatalog`、`FeatureDispatcher`、`FeatureInstallRegistry`、`PreferenceSchema`、`PreferenceEntry`、`ProcessScopes` 的包分类、Hook、Installer、XML key 或资源。

## 非目标

- 不接入 `FeatureCatalog` 或 `PreferenceSchema`；
- 不修改 Hook 或 Installer 行为；
- 不修改 A14 仓库；
- 不实施其他架构重构。

## 验收标准

- [x] `MainModule` 中 9 个具体 preference key 全部消失；
- [x] `MainModule` 不再包含 `needLoadPrefs()` 及其三个辅助方法；
- [x] known package 和四组 legacy feature 判断全部进入统一 registry；
- [x] 所有行为矩阵测试通过；
- [x] volume media 的短路和异常行为与旧 Java 实现一致；
- [x] remote null、rejected process、initPrefs 和 installer 时序不变；
- [x] 不修改 Hook、Installer、FeatureCatalog 或 PreferenceSchema；
- [x] source hazard 为 47 reviewed、0 new；
- [x] Python tests、Gradle unit tests、fast verify、full verify 全部通过；
- [x] `git diff --check` 通过；
- [x] 工作区无未解释文件；
- [x] 不要求 APK；
- [x] 完成状态标记为 `STATIC_VERIFIED`。

## 验证

```powershell
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"

.\gradlew.bat :app:testDebugUnitTest --tests "tv.withaibuild.customiuizer.prefs.PreferenceLoadRegistryTest"
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:compileDebugJavaWithJavac

python tools/source_hazard_scan.py --path app/src/main/java
python tools/verify.py fast --changed
python tools/verify.py full

git diff --check
git status --short
```

### 实际结果

- `compileall tools`：通过
- `unittest discover`：426 tests passed, 0 failed, skipped 2
- `testDebugUnitTest --tests PreferenceLoadRegistryTest`：通过
- `compileDebugKotlin`：通过
- `compileDebugJavaWithJavac`：通过
- `source_hazard_scan.py --path app/src/main/java`：`Source hazard scan passed: 47 reviewed finding(s), 0 new`
- `verify.py fast --changed`：通过
- `verify.py full`：通过（含 compileDebugKotlin、compileDebugJavaWithJavac、testDebugUnitTest-all、lintDebug）
- `git diff --check`：通过（仅 MainModule.java CRLF 转 LF 的正常 git 提示）
- `git status --short`：工作区干净

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: 164c45653b2e6066b8d519f2fd59efc49e66ce40
- Final SHA: 3fd569cc7e8ba543b921ebaae9d849f777928cf0
- Commits:
  - `09a6b19` docs: add active OPTIMIZE-needloadprefs-knowledge-sink task
  - `3fd569c` refactor: extract preference load registry
- Behavior changed:
  - 新增 `PreferenceLoadRegistry`，聚合四组 legacy preference-load 规则；
  - `MainModule` 删除 `needLoadPrefs`、`isPrefEnabled`、`isInPrefSet`、`isVolumeMediaEnabled` 及 9 个 preference key 字面量；
  - `MainModule.onPackageReady` 现在调用 `PreferenceLoadRegistry.shouldLoad(remote, pkg)`；
  - Volume Media 的 `||` 短路和 `NumberFormatException` 行为保持与旧 Java 实现一致；
  - known package、remote null、rejected process、`initPrefs()` 调用时序保持不变。
- Verification: 见“验证”章节
- Device evidence: 未涉及设备；本任务仅静态重构与单元测试，标记为 `STATIC_VERIFIED`
- Known limits:
  - `PreferenceSchema` / `FeatureCatalog` integration remains a separate phase after the four legacy common-package features receive real `FeatureSpec` ownership.
  - `PreferenceLoadPredicate` 仅用于 legacy 规则下沉，未接入 installer 或 dispatcher。
