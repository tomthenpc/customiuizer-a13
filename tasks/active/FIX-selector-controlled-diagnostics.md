# FIX-selector-controlled-diagnostics

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

移除以下四个 selector/list 文件中的全部 7 处 `.printStackTrace()`，统一接入现有的 `tv.withaibuild.customiuizer.utils.SettingsDiagnostics`：

- `app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/AppSelector.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/ShortcutSelector.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/SortableList.kt`

完成后：

- 上述四个文件均不存在 `.printStackTrace(`
- 七处异常有固定、可搜索的 operation 名称
- 所有正常 fallback 和 UI 流程保持不变
- 所有七处均使用完整 fatal guard
- baseline 从 21 条降至 14 条
- source hazard 为 `14 reviewed, 0 new`

## 当前问题

四个 selector/list 文件存在 7 处 `.printStackTrace()`，其中 `AppSelector.privacy.toggle` 和 `AppSelector.applock.toggle` 仅重抛 `OutOfMemoryError`，缺少 `ThreadDeath` / `VirtualMachineError` fatal guard。需要替换为 `SettingsDiagnostics.failure(...)` 并补齐 fatal guard。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/AppSelector.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/ShortcutSelector.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/SortableList.kt`
- `tools/tests/test_selector_diagnostics.py`（新增）
- `docs/audit/SOURCE_HAZARD_BASELINE.json`
- `tasks/active/FIX-selector-controlled-diagnostics.md`
- `tasks/completed/FIX-selector-controlled-diagnostics.md`

## 必须保持

- 每个 catch 仍先判断并立即重抛 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError`；
- `AppSelector.privacy.toggle` 和 `AppSelector.applock.toggle` 补齐完整 fatal guard（唯一允许的异常行为调整）；
- `ActivitySelector.loadActivities` 普通异常后不调用 `process`，成功路径仍 `act.runOnUiThread(process)`；
- `AppSelector.privacy.toggle` 成功后仍刷新 `PrivacyAppAdapter` 并发送 `content://com.miui.securitycenter.provider/update_privacyapps_icon`；
- `AppSelector.applock.toggle` 成功后仍刷新 `LockedAppAdapter`；
- `AppSelector.loadApps` 成功后才 `initialized = true` 并 `act.runOnUiThread(process)`，失败后不把 `initialized` 设为 true；
- `ShortcutSelector.loadIconResource` 失败后仍尝试 `Intent.EXTRA_SHORTCUT_ICON` bitmap fallback；
- `ShortcutSelector.persistIcon` 仍使用 PNG quality 100、FileOutputStream、`tmp.png`、`shortcut_icon` 只在 compress 成功后设置，失败后仍继续设置 shortcut contents/name/intent；
- `SortableList.loadDragShadow` 失败后仍继续设置 `PreferenceAdapter`，不 return，不改变排序/点击/长按逻辑；
- 不修改方法签名、调用方、循环逻辑、排序、Thread/postDelayed 时序、SecurityManager 反射、shortcut 文件格式、list ordering、Gradle 构建配置。

## 实现要求

使用以下精确 operation 名称，每个只出现一次：

- `ActivitySelector.loadActivities`
- `AppSelector.privacy.toggle`
- `AppSelector.applock.toggle`
- `AppSelector.loadApps`
- `ShortcutSelector.loadIconResource`
- `ShortcutSelector.persistIcon`
- `SortableList.loadDragShadow`

每个文件增加：

```kotlin
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics
```

调用格式：

```kotlin
SettingsDiagnostics.failure(
    "ActivitySelector.loadActivities",
    error
)
```

每个 operation 必须位于对应 catch block 的 fatal guard 之后。

## 明确不做

- 不修改 `SettingsDiagnostics.kt`、`Helpers.kt`、`HookUtils.kt`、`PreferenceFragmentBase.kt`；
- 不修改其余 14 条 baseline finding；
- 不迁移协程、Executor、ViewModel、Activity Result Contract 或 lifecycleScope；
- 不修改 Thread / postDelayed 架构；
- 不修改 Activity Result API、SecurityManager 反射方法、shortcut 文件格式和路径、list ordering 数据格式。

## 静态合同测试

新增 `tools/tests/test_selector_diagnostics.py`，至少覆盖：

1. 四个文件均不存在 `.printStackTrace(`
2. 四个文件均 import `SettingsDiagnostics`
3. 七个 operation 精确存在，每个恰好一次
4. 不出现 `System.out`、`System.err`
5. 不出现新 `Log.` 调用
6. `SettingsDiagnostics.kt` 未修改
7. 七个对应 catch 均重抛 `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError`
8. `AppSelector.privacy.toggle` 和 `AppSelector.applock.toggle` 补齐三类 fatal error
9. `ActivitySelector.loadActivities` catch 后不调用 `process`，成功路径仍 `act.runOnUiThread(process)`
10. `AppSelector` privacy/applock 成功路径仍刷新对应 adapter；loadApps 成功路径才设置 `initialized = true`
11. `ShortcutSelector.loadIconResource` catch 后仍存在 bitmap fallback；`persistIcon` 仍 PNG/quality 100/FileOutputStream，`shortcut_icon` 只在 compress 成功分支，catch 后继续设置 contents/name/intent
12. `SortableList.loadDragShadow` 对应 `mSnapshotShadow`，catch 后继续 `PreferenceAdapter`，不存在 `return`

测试应提取 operation 所在 catch block，不得只统计整个文件中 fatal 类型出现次数。

## Baseline 更新

执行：

```powershell
python tools/source_hazard_scan.py --path app/src/main/java --write-baseline
```

baseline diff 必须只移除：

- `ActivitySelector.kt` 1 条
- `AppSelector.kt` 3 条
- `ShortcutSelector.kt` 2 条
- `SortableList.kt` 1 条

更新后必须确认：

```text
reviewed findings = 14
new findings = 0
```

不得删除其他文件 finding，不得增加 allow 注释，不得重写 scanner。

## 验收标准

- [ ] 四个目标文件共 7 处 `printStackTrace()` 全部移除
- [ ] 七个固定 operation 唯一
- [ ] 统一使用 `SettingsDiagnostics`
- [ ] 七处全部采用完整 fatal guard
- [ ] privacy/applock 不再吞掉 ThreadDeath 或 VirtualMachineError
- [ ] 所有普通异常 fallback 保持不变
- [ ] selector 成功路径和 UI 时序不变
- [ ] baseline 从 21 降至 14
- [ ] source hazard 为 `14 reviewed, 0 new`
- [ ] strict dependency verification 通过
- [ ] Python tests、Gradle tests、lint、fast verify、full verify 全部通过
- [ ] `git diff --check` 通过
- [ ] 工作区干净
- [ ] 不要求 APK
- [ ] 完成状态：`STATIC_VERIFIED`

## 验证

```powershell
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"

.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug

.\gradlew.bat :app:testDebugUnitTest --dependency-verification=strict

python tools/source_hazard_scan.py --path app/src/main/java
python tools/verify.py fast --changed
python tools/verify.py full

git diff --check
git status --short
```

## 提交建议

```text
docs: add active FIX-selector-controlled-diagnostics task
fix: replace selector printStackTrace diagnostics
docs: complete FIX-selector-controlled-diagnostics task
```

## 完成记录

- Base SHA: d39f9dfedbedd5ef9db5d4d3d8d70cd889293a4f
- 最终分支 HEAD: （完成后填写）
