# FIX-remaining-controlled-diagnostics-baseline-zero

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

移除当前 source hazard baseline 中最后 8 处 `.printStackTrace()`，统一接入 `tv.withaibuild.customiuizer.utils.SettingsDiagnostics`。

## 完成摘要

七个目标文件中的 8 处 `.printStackTrace()` 已全部替换为 `SettingsDiagnostics.failure(...)`，其中 `LockedAppAdapter` 和 `PrivacyAppAdapter` 补齐了 `ThreadDeath` / `VirtualMachineError` fatal guard。`SeekBarPreference` 仍只捕获 `IllegalFormatException`。生产源码全局已无 `.printStackTrace(`，baseline 归零并保留。

## Mutation 结果

- 删除 Locked adapter `ThreadDeath` guard → 合同测试失败 → 已恢复
- AutoRotateService read fallback `0` → `1` → 合同测试失败 → 已恢复
- SeekBarPreference catch 扩大为 `Throwable` → 合同测试失败 → 已恢复
- PreferenceAdapter catch 添加 `return row` → 合同测试失败 → 已恢复

## 验证结果

- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：507 tests passed, 0 failed, skipped 2
- `python tools/source_hazard_scan.py --path app/src/main/java`：`0 reviewed, 0 new`
- `python tools/verify.py fast --changed`：通过
- `python tools/verify.py full`：通过
- `\gradlew.bat :app:compileDebugKotlin`：通过
- `\gradlew.bat :app:compileDebugJavaWithJavac`：通过
- `\gradlew.bat :app:testDebugUnitTest`：通过
- `\gradlew.bat :app:lintDebug`：通过
- `\gradlew.bat :app:testDebugUnitTest --dependency-verification=strict`：通过
- `Get-ChildItem app/src/main/java -Recurse -Include *.kt,*.java | Select-String -SimpleMatch ".printStackTrace("`：无输出
- `git diff --check`：通过
- `git status --short`：干净

## 完成记录

- Base SHA: adbea7dcd663cfd09c2cb0a31da4f1e7b5c9e8aa
- Implementation SHA: b66fe06f5060164738d18d9cc307e71708693cd2
- Python test count: 507
- Baseline 8 → 0
