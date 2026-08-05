# FIX-hookerclasshelper-fatal-propagation

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookerClassHelper.java`，确保 `MethodHook` 的 before、host、after 三条路径都不会吞掉或覆盖 fatal error。

## 完成摘要

将 `throwIfOutOfMemory(Throwable throwable)` 重命名为 `throwIfFatal(Throwable throwable)`，并在 8 层 cause-chain 遍历中显式检查 `OutOfMemoryError`、`ThreadDeath` 和 `VirtualMachineError`，匹配到原始实例后原样重抛。

三处调用点全部更新：
- `beforeHook()` catch 中：先 `throwIfFatal(t)`，再 `XposedHelpers.log(t)`
- `afterHook()` catch 中：先 `throwIfFatal(t)`，再 `XposedHelpers.log(t)`
- `intercept()` host path：`throwIfFatal(throwable)` 位于 `if (hasAfter)` 之前，确保 fatal host error 不进入 after callback

普通异常隔离行为保持不变，包括 `AssertionError` 仍按普通 callback failure 处理。

## 修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookerClassHelper.java`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/HookerClassHelperTest.java`
- `tools/tests/test_hooker_class_helper_fatality.py`

## Mutation 结果

- 删除 `ThreadDeath` 分支 → ThreadDeath JVM 测试失败 → 已恢复
- 删除 `VirtualMachineError` 分支 → InternalError JVM 测试失败 → 已恢复
- 删除 cause-chain 遍历，只检查最外层 → wrapped fatal 测试失败 → 已恢复
- 将 host 的 `throwIfFatal(throwable)` 移到 `if (hasAfter)` 之后 → 静态顺序合同失败 / afterCalled 测试失败 → 已恢复
- 将 before helper 调用移到日志之后 → 静态顺序合同失败 → 已恢复
- 将条件扩大为 `current instanceof Error` → AssertionError 回归测试失败 / 静态合同失败 → 已恢复

## 验证结果

- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：566 tests passed, 0 failed, skipped 2
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

- Base SHA: dc90b75f03fe95686593d0f80254acccfbfd2144
- Implementation SHA: 12b010fdad72332c88920f3a9d7b2691e30ec17c
- Python test count: 566
- JVM test result: 21 passed
- Baseline: 保持为 0
