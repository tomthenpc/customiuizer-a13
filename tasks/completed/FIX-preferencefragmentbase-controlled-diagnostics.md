# FIX-preferencefragmentbase-controlled-diagnostics

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

移除 `app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt` 中的全部 5 处 `.printStackTrace()`，复用现有的 `tv.withaibuild.customiuizer.utils.SettingsDiagnostics` 建立 settings-app 统一的受控诊断入口。

完成后：

- `PreferenceFragmentBase.kt` 中不存在 `.printStackTrace(`
- 增加 `import tv.withaibuild.customiuizer.utils.SettingsDiagnostics`
- 所有 fatal error 重抛、dialog、fallback 和 stream 生命周期保持不变
- `SOURCE_HAZARD_BASELINE.json` 从 26 条降至 21 条
- baseline 仅删除该文件的 5 条 finding

## 当前问题

`PreferenceFragmentBase.kt` 中 5 处生产代码调用 `.printStackTrace()`，触发 `PRINT_STACK_TRACE` source hazard 规则。需要替换为 `SettingsDiagnostics.failure(...)`，同时保持既有行为。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt`
- `tools/tests/test_preference_fragment_base_diagnostics.py`（新增）
- `docs/audit/SOURCE_HAZARD_BASELINE.json`
- `tasks/active/FIX-preferencefragmentbase-controlled-diagnostics.md`
- `tasks/completed/FIX-preferencefragmentbase-controlled-diagnostics.md`

## 必须保持

- 每个 catch 仍先判断并立即重抛 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError`；
- fatal guard 不进入 `SettingsDiagnostics`；
- `setDefaultValues` 失败后只记录并继续 fragment 初始化，不新增 dialog，不抛普通异常；
- backup 主 catch 仍显示 `R.string.warning` + `R.string.storage_cannot_backup` + `e.message` + 正按钮行为；
- backup close catch 仍 `output?.flush()`、`output?.close()`，普通异常吞掉；
- restore 主 catch 仍显示 `R.string.warning` + `R.string.storage_cannot_restore`，普通反序列化异常不传播；
- restore 成功路径仍调用 `AppHelper.syncPrefsToAnother`、`AppLocaleController.invalidateFastPath`、Activity finish/restart；
- restore close catch 仍 `input?.close()`，普通异常吞掉；
- 不修改方法签名、调用方、循环逻辑、排序、备份/恢复格式、`ObjectInputStream` / `ObjectOutputStream` 创建时机、Activity Result API、dialog 文案、preference key；
- 不重构成 `use`。

## 实现要求

使用以下精确 operation 名称：

- `PreferenceFragmentBase.setDefaultValues`
- `PreferenceFragmentBase.backup.write`
- `PreferenceFragmentBase.backup.close`
- `PreferenceFragmentBase.restore.read`
- `PreferenceFragmentBase.restore.close`

对应位置：

1. `onCreate(savedInstanceState, pref_defaults)` 中的 `PreferenceManager.setDefaultValues`
2. `onActivityResult` 备份输出主 catch
3. 备份输出流 `finally` 中的 close/flush catch
4. `doRestoreSettings` 恢复输入主 catch
5. 恢复输入流 `finally` 中的 close catch

替换示例：

```kotlin
catch (t: Throwable) {
    if (
        t is OutOfMemoryError ||
        t is ThreadDeath ||
        t is VirtualMachineError
    ) throw t

    SettingsDiagnostics.failure(
        "PreferenceFragmentBase.setDefaultValues",
        t
    )
}
```

## 明确不做

- 不修改 `SettingsDiagnostics.kt`、`Helpers.kt`、`HookUtils.kt`；
- 不修改其他剩余 21 条或 26 条 baseline finding；
- 不修复 `commitAllowingStateLoss`、deprecated API 或 stream `use` 重构；
- 不修改 Gradle 构建配置；
- 不修改 `local.properties`。

## 静态合同测试

新增 `tools/tests/test_preference_fragment_base_diagnostics.py`，至少验证：

1. `PreferenceFragmentBase.kt` 不存在 `.printStackTrace(`
2. 存在 `SettingsDiagnostics` import
3. 上述五个 operation 精确存在
4. 每个 operation 恰好调用一次
5. 五处对应 catch 均保留 fatal error 重抛
6. `setDefaultValues` catch 后没有新增 throw 或 dialog
7. `backup.write` catch 仍包含 `R.string.storage_cannot_backup` 和 `e.message`
8. `backup.close` 所在 finally 仍包含 `output?.flush()` 和 `output?.close()`
9. `restore.read` catch 仍包含 `R.string.storage_cannot_restore`
10. `restore.close` 所在 finally 仍包含 `input?.close()`
11. 不出现 `System.out`、`System.err` 或新的日志 tag
12. 不修改 `SettingsDiagnostics.kt`

## 验收标准

- [x] 五处 `printStackTrace()` 全部移除
- [x] 五个固定 operation 全部存在且唯一
- [x] 复用 `SettingsDiagnostics`
- [x] fatal error 重抛完整保留
- [x] backup/restore 成功和失败 UI 行为不变
- [x] stream flush/close 时序不变
- [x] 不修改序列化格式
- [x] baseline 从 26 降至 21
- [x] source hazard 为 21 reviewed、0 new
- [x] dependency verification strict 通过
- [x] Python tests、Gradle unit tests、lint、fast verify、full verify 全部通过
- [x] `git diff --check` 通过
- [x] 工作区干净
- [x] 不要求 APK
- [x] 完成状态：`STATIC_VERIFIED`

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

### 实际结果

- `compileall tools`：通过
- `unittest discover`：461 tests passed, 0 failed, skipped 2
- `compileDebugKotlin`：通过
- `compileDebugJavaWithJavac`：通过
- `testDebugUnitTest`：通过
- `lintDebug`：通过
- `testDebugUnitTest --dependency-verification=strict`：通过
- `source_hazard_scan.py --path app/src/main/java`：`Source hazard scan passed: 21 reviewed finding(s), 0 new`
- `verify.py fast --changed`：通过
- `verify.py full`：通过
- `git diff --check`：通过
- `git status --short`：干净

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: 5c2291c9744c3c05f197a4083bea2d43d1cdacd1
- Implementation SHA: 806ed7a0c62e9d4c820f26d89bc31c7e92a4a6d3
- Final branch HEAD: （归档后填写）
