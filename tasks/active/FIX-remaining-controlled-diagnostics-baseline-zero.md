# FIX-remaining-controlled-diagnostics-baseline-zero

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

移除当前 source hazard baseline 中最后 8 处 `.printStackTrace()`，统一接入 `tv.withaibuild.customiuizer.utils.SettingsDiagnostics`。

目标文件：

- `app/src/main/java/tv/withaibuild/customiuizer/prefs/SeekBarPreference.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/prefs/SpinnerEx.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/qs/AutoRotateService.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/AppHelper.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt`

完成后：

- 生产源码全局不再包含 `.printStackTrace(`
- 八个固定 operation 固定且唯一
- baseline 从 8 降至 0
- source hazard 为 `0 reviewed, 0 new`
- `SOURCE_HAZARD_BASELINE.json` 保留且为空

## 当前问题

剩余 8 处 `.printStackTrace()` 分布在 7 个文件中，其中 `AutoRotateService.kt` 有 2 处。`LockedAppAdapter.kt` 和 `PrivacyAppAdapter.kt` 的初始化 catch 缺少 `ThreadDeath` / `VirtualMachineError` fatal guard，需要补齐。

## 允许修改

- 七个目标 Kotlin 文件
- `tools/tests/test_remaining_diagnostics.py`（新增）
- `docs/audit/SOURCE_HAZARD_BASELINE.json`
- `tasks/active/FIX-remaining-controlled-diagnostics-baseline-zero.md`
- `tasks/completed/FIX-remaining-controlled-diagnostics-baseline-zero.md`

## 必须保持

- `SeekBarPreference.formatDisplayValue` 仍只捕获 `IllegalFormatException`，fallback 为 `display.toString()`；
- `SpinnerEx.configurePopupHeight` 仍反射 `mPopup`，高度公式 `40 * 10 * scale`；
- `AutoRotateService.switchState` 仍读写 `pref_key_qs_autorotate_state`，状态轮转 `state >= 2 ? 0 : state + 1`；
- `AutoRotateService.readState` 失败后仍返回 `0`；
- `AppHelper.resolveActionName` 失败后仍返回 `null`；
- `LockedAppAdapter.initializeSecurityManager` 仍反射 `getApplicationAccessControlEnabledAsUser`；
- `PreferenceAdapter.bindActionIcon` catch 后继续 `row.setPadding` 并 `return row`；
- `PrivacyAppAdapter.initializeSecurityManager` 仍反射 `isPrivacyApp`；
- 不修改方法签名、调用方、UI 时序、preference 数据结构、构建配置。

## 实现要求

使用以下精确 operation 名称，每个调用一次：

- `SeekBarPreference.formatDisplayValue`
- `SpinnerEx.configurePopupHeight`
- `AutoRotateService.switchState`
- `AutoRotateService.readState`
- `AppHelper.resolveActionName`
- `LockedAppAdapter.initializeSecurityManager`
- `PreferenceAdapter.bindActionIcon`
- `PrivacyAppAdapter.initializeSecurityManager`

目标文件按需增加 `import tv.withaibuild.customiuizer.utils.SettingsDiagnostics`（`utils` 包内文件可直接使用）。

## 明确不做

- 不修改 `SettingsDiagnostics.kt`、`HookUtils.kt`、`Helpers.kt`、构建配置、scanner；
- 不处理 `LockedAppAdapter.readChecked` / `PrivacyAppAdapter.readChecked` 等其它静默 catch；
- 不重构 adapter 公共基类、不引入 coroutine、不修改 preference XML 或 UI 文案。

## 静态合同测试

新增 `tools/tests/test_remaining_diagnostics.py`，覆盖：

1. 七个目标文件均不存在 `.printStackTrace(`
2. 全部生产源码递归不存在 `.printStackTrace(`
3. 八个 operation 精确存在且唯一
4. 不新增 `Log.`、`System.out`、`System.err`
5. `SettingsDiagnostics.kt` 未修改
6. 七个 `Throwable` catch 均包含完整 fatal guard
7. `SeekBarPreference` 仍为 `IllegalFormatException` catch，fallback 为 `display.toString()`
8. `SpinnerEx` 仍反射 `mPopup`，高度公式 `40 * 10 * scale`
9. `AutoRotateService` key 与状态逻辑不变
10. `AppHelper` 失败后返回 `null`
11. `Locked/Privacy` adapter 初始化补齐 fatal guard，无 `return`
12. `PreferenceAdapter` catch 后继续 padding 并返回 row

## Mutation 验证

临时执行并恢复至少三个 mutation：

1. 删除 Locked adapter 初始化 catch 的 `ThreadDeath` guard
2. 将 AutoRotateService read fallback 从 `0` 改为 `1`
3. 将 SeekBarPreference catch 扩大为 `Throwable`
4. 可选：在 PreferenceAdapter catch 中添加 `return row`

## Baseline 归零

执行：

```powershell
python tools/source_hazard_scan.py --path app/src/main/java --write-baseline
```

预期 `SOURCE_HAZARD_BASELINE.json` 保留 schema 1，但 fingerprints 和 findings 为空。不得删除文件。

## 验收标准

- [ ] 8 处 `printStackTrace()` 全部移除
- [ ] 生产源码全局无 `.printStackTrace(`
- [ ] 八个 operation 固定且唯一
- [ ] 完整 fatal guard 保留/补齐
- [ ] SeekBarPreference 仍为 IllegalFormatException
- [ ] 所有原 fallback 保持不变
- [ ] baseline 归零
- [ ] source hazard 为 `0 reviewed, 0 new`
- [ ] mutation 验证有效且恢复
- [ ] strict dependency verification 通过
- [ ] Python、Gradle unit、lint、fast、full 全部通过
- [ ] `git diff --check` 通过
- [ ] 工作区干净
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
docs: add active FIX-remaining-controlled-diagnostics-baseline-zero task
fix: remove remaining printStackTrace diagnostics
docs: complete FIX-remaining-controlled-diagnostics-baseline-zero task
```

## 完成记录

- Base SHA: adbea7dcd663cfd09c2cb0a31da4f1e7b5c9e8aa
- Implementation SHA: （完成后填写）
