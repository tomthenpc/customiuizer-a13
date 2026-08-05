# FIX-security-adapter-readchecked-diagnostics

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

修复 `LockedAppAdapter.readChecked` 和 `PrivacyAppAdapter.readChecked` 的静默反射失败和不完整 fatal guard。

## 完成摘要

两个 `readChecked()` 现已具备完整 fatal guard，普通反射失败通过 `SettingsDiagnostics` 记录，并通过实例级 `readCheckedFailureLogged` 标志实现每个 adapter 对象最多记录一次。失败仍返回 `false`，不清空 manager 或 Method，继续后续反射尝试。

## 修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt`
- `tools/tests/test_security_adapter_readchecked_diagnostics.py`（新增）
- `tools/tests/test_remaining_diagnostics.py`（更新 operation 计数）

## Mutation 结果

- 删除 Locked `ThreadDeath` guard → 合同测试失败 → 已恢复
- 将 Privacy fallback 从 `false` 改为 `true` → 合同测试失败 → 已恢复
- 删除 `if (!readCheckedFailureLogged)` → 合同测试失败 → 已恢复
- 在 Locked catch 中加入 `getApplicationAccessControlEnabledAsUser = null` → 合同测试失败 → 已恢复

## 验证结果

- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：520 tests passed, 0 failed, skipped 2
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

- Base SHA: 78ed0f07cbf9c002b97a0e5d4d52976afdabcb9b
- Implementation SHA: 9e9bcc62343ee5d4013c64cc9147bd61c5e84953
- Python test count: 520
- Baseline: 保持为 0
