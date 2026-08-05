# FIX-xposedhelpers-reflection-fatal-unwrapping

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `XposedHelpers.java` 的中心反射 helper，确保通过 `Method.invoke()` 或 `Constructor.newInstance()` 抛出的 fatal error 被解包并原样重抛。

## 完成摘要

`invocationTargetError(InvocationTargetException exception)` 现在检查并原样重抛 `OutOfMemoryError`、`ThreadDeath` 和 `VirtualMachineError`，普通异常仍包装为 `InvocationTargetError(cause)`。八个公共反射入口继续统一调用 helper。

## 修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpersOomTest.java`（删除）
- `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpersFatalityTest.java`（新增）
- `tools/tests/test_xposed_helpers_reflection_fatality.py`（新增）

## Mutation 结果

- 删除 `ThreadDeath` 分支 → JVM 单元测试失败 → 已恢复
- 删除 `VirtualMachineError` 分支 → JVM 单元测试失败 → 已恢复
- 将 fatal cause 包装为 `InvocationTargetError` → `assertSame` 失败 → 已恢复
- 将 ordinary cause 通过 `(RuntimeException)` 直接重抛 → JVM 单元测试失败 → 已恢复
- 将 `callStaticMethod(... Class<?>[] parameterTypes ...)` 改回 `throw new InvocationTargetError(e.getCause())` → 静态八入口合同失败 → 已恢复
- 将条件扩大为 `cause instanceof Error` → 静态合同失败 → 已恢复

## 验证结果

- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：543 tests passed, 0 failed, skipped 2
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

- Base SHA: 87546528c43617d3b35013696aa91eef0d739c21
- Implementation SHA: 1351d11ef2cd4cc8e161f5c3c0b62c12eb0b0224
- Python test count: 543
- JVM test result: 12 passed
- Baseline: 保持为 0
