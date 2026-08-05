# FIX-security-adapter-reflection-fatal-unwrapping

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

确保 `LockedAppAdapter.readChecked` 和 `PrivacyAppAdapter.readChecked` 不仅重抛直接 fatal error，也重抛被 `InvocationTargetException` 包装的 fatal cause。

## 完成摘要

新增 `ReflectionFatality` 纯 JVM helper，使用 `error.cause ?: error` 仅解包 `InvocationTargetException`，检查 `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError` 并重抛原始 fatal 对象。两个 adapter 的 `readChecked()` catch 已替换为 `ReflectionFatality.rethrowIfFatal(error)`，保留 log-once 和 `false` fallback。

## 修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/utils/ReflectionFatality.kt`（新增）
- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/utils/ReflectionFatalityTest.kt`（新增）
- `tools/tests/test_security_adapter_readchecked_diagnostics.py`（更新）

## Mutation 结果

- 将 `error.cause ?: error` 改为 `error` → JVM 单元测试失败 → 已恢复
- 删除 `ThreadDeath` 分支 → JVM 单元测试失败 → 已恢复
- 将普通异常也抛出 → JVM 单元测试失败 → 已恢复
- 将 adapter helper 调用移到日志之后 → 静态顺序合同失败 → 已恢复
- 将日志参数从 `error` 改为 `error.cause ?: error` → 外层诊断合同失败 → 已恢复

## 验证结果

- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：527 tests passed, 0 failed, skipped 2
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

- Base SHA: 6973725b82b97b34909206a5529d1d7183bd0b52
- Implementation SHA: 4deb50772609615c0e010fecda5cde1da1335047
- Python test count: 527
- JVM test result: 9 passed
- Baseline: 保持为 0
