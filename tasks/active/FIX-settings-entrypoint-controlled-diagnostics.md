# FIX-settings-entrypoint-controlled-diagnostics

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

移除以下六个 settings-app 入口/UI 文件中的全部 6 处 `.printStackTrace()`，统一接入现有的 `tv.withaibuild.customiuizer.utils.SettingsDiagnostics`：

- `app/src/main/java/tv/withaibuild/customiuizer/AboutFragment.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/PrefsProvider.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`

完成后：

- 六个文件均不存在 `.printStackTrace(`
- 六个 operation 固定且唯一
- 所有正常 fallback 和 UI 生命周期保持不变
- 所有六处均使用完整 fatal guard
- baseline 从 14 条降至 8 条
- source hazard 为 `8 reviewed, 0 new`

## 当前问题

六个 settings 入口文件存在 6 处 `.printStackTrace()`，触发 `PRINT_STACK_TRACE` source hazard 规则。需要替换为 `SettingsDiagnostics.failure(...)`，同时保持既有行为。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/AboutFragment.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/PrefsProvider.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- `tools/tests/test_settings_entrypoint_diagnostics.py`（新增）
- `docs/audit/SOURCE_HAZARD_BASELINE.json`
- `tasks/active/FIX-settings-entrypoint-controlled-diagnostics.md`
- `tasks/completed/FIX-settings-entrypoint-controlled-diagnostics.md`

## 必须保持

- 每个 catch 仍先判断并立即重抛 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError`；
- `AboutFragment.bindVersionText` 仍读取 `BuildConfig.VERSION_NAME`、`BUILD_TIME`，develop 仍使用 `yy.MM.dd` 和 `-test`；
- `Credentials.initializeCredentialFlow` 只替换最外层 catch；内层 KeyStore catch 仍 `createConfirmDeviceCredentialIntent` + `startActivityForResult`；内层 catch 不调用 `SettingsDiagnostics`；
- `MainActivity.unregisterPreferenceListener` catch 后仍执行 `super.onDestroy()`，不修改 listener 注销目标；
- `PrefsProvider.openTestAsset` 仍保持 `AUTHORITY`、matcher `test/*`、文件名映射、`assets.openFd(filename)`，最终返回 `null`；
- `SubFragmentWithSearch.styleSearchView` 仍 `setSaveFromParentEnabled(false)`、夜间/ MIUI drawable、TextWatcher / ListView / filter 初始化；catch 中无 `return`；
- `System.writeQqsCount` 仍只在 `fromUser` true 时写入；progress `< 3` 映射为 `5`；key 仍为 `sysui_qqs_count`；仍调用 `Settings.Secure.putInt`；普通失败只记录，不弹 Toast、不 return；
- 不修改方法签名、调用方、UI 时序、ContentProvider 对外能力、preference 数据结构、构建配置。

## 实现要求

使用以下精确 operation 名称，每个只出现一次：

- `AboutFragment.bindVersionText`
- `Credentials.initializeCredentialFlow`
- `MainActivity.unregisterPreferenceListener`
- `PrefsProvider.openTestAsset`
- `SubFragmentWithSearch.styleSearchView`
- `System.writeQqsCount`

每个文件增加：

```kotlin
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics
```

调用形式：

```kotlin
SettingsDiagnostics.failure(
    "AboutFragment.bindVersionText",
    error
)
```

## 明确不做

- 不修改 `SettingsDiagnostics.kt`、`tools/source_hazard_scan.py`、`gradle/verification-metadata.xml`、构建配置；
- 不处理 `SeekBarPreference.kt`、`SpinnerEx.kt`、`AutoRotateService.kt`、`AppHelper.kt`、adapters；
- 不迁移 Activity Result API、serialization、coroutine/lifecycleScope、KeyStore 流程、ContentProvider API、preference 数据结构、MIUI reflection/resource lookup。

## 静态合同测试

新增 `tools/tests/test_settings_entrypoint_diagnostics.py`，至少覆盖：

1. 六个文件均不存在 `.printStackTrace(`
2. 六个文件均 import `SettingsDiagnostics`
3. 六个 operation 精确存在
4. 每个 operation 恰好调用一次
5. 每个 operation 所在 catch 都包含完整三类 fatal guard
6. 六个文件不存在新增的 `Log.`、`System.out`、`System.err`
7. `SettingsDiagnostics.kt` 未修改
8. `AboutFragment.bindVersionText` 仍关联 `BuildConfig.VERSION_NAME`、`BUILD_TIME`、`yy.MM.dd`、`-test`
9. `Credentials.initializeCredentialFlow` 只位于外层 catch
10. Credentials 内层 catch 仍包含 `createConfirmDeviceCredentialIntent` 和 `startActivityForResult`
11. Credentials 内层 catch 不调用 `SettingsDiagnostics`
12. `MainActivity.unregisterPreferenceListener` catch 后仍执行 `super.onDestroy()`
13. `PrefsProvider.openTestAsset` 仍包含全部文件名映射与 `assets.openFd`
14. Provider catch 后仍可到达最终 `return null`
15. `SubFragmentWithSearch.styleSearchView` catch 中不存在 `return`
16. style catch 后仍存在 TextWatcher、ListView 和 filter 初始化
17. `System.writeQqsCount` 仍关联 `Settings.Secure.putInt`
18. `sysui_qqs_count`、`value < 3` 和 `value = 5` 保持
19. System catch 中不存在 `return`

测试应提取具体方法和 catch block，不要只统计整个文件中的字符串数量。

## Mutation 验证

临时执行并恢复至少两个 mutation：

1. 删除任意一个目标 catch 的 `ThreadDeath` guard，确认静态合同测试失败
2. 将任意一个 operation 名称改错，确认唯一 operation 测试失败
3. 可选：在 `SubFragmentWithSearch` catch 中加入 `return`，确认 fallback 合同测试失败

mutation 只用于验证，不得提交。

## Baseline 更新

执行：

```powershell
python tools/source_hazard_scan.py --path app/src/main/java --write-baseline
```

baseline diff 必须只删除以下 6 条：

- `AboutFragment.kt` fingerprint `d7407313a25d60591550`
- `Credentials.kt` fingerprint `6fb50aa89d886ffea5c2`
- `MainActivity.kt` fingerprint `5151c9f99364ad247926`
- `PrefsProvider.kt` fingerprint `5d76ef433a5818f61ebd`
- `SubFragmentWithSearch.kt` fingerprint `0db4399e3dea5150c370`
- `subs/System.kt` fingerprint `d6e39d1a5ff2ad4dbd3b`

更新后确认：

```text
reviewed findings = 8
new findings = 0
```

## 验收标准

- [ ] 目标 6 处 `printStackTrace()` 全部移除
- [ ] 六个 operation 唯一
- [ ] 六处完整 fatal guard 保留
- [ ] Credentials 内层认证 fallback 未修改
- [ ] 所有 UI、Provider 和 lifecycle fallback 保持
- [ ] baseline 从 14 降至 8
- [ ] source hazard 为 `8 reviewed, 0 new`
- [ ] mutation 测试确认有效
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
docs: add active FIX-settings-entrypoint-controlled-diagnostics task
fix: replace settings entrypoint printStackTrace diagnostics
docs: complete FIX-settings-entrypoint-controlled-diagnostics task
```

## 完成记录

- Base SHA: 5618e29fb88255e7132b28f9305f8e3093cd3b75
- Implementation SHA: （完成后填写）
