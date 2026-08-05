# FIX-modulehelper-hook-install-fatal-propagation

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`，确保 12 个 hook 安装入口捕获异常后均原样传播三类 fatal error，同时保持普通失败记录、日志和 fallback 不变。

## 完成摘要

新增 package-private 静态 helper：

```java
static void throwIfFatal(Throwable throwable) {
    Throwable current = throwable;
    for (int depth = 0; current != null && depth < 8; depth++) {
        if (current instanceof OutOfMemoryError) throw (OutOfMemoryError) current;
        if (current instanceof ThreadDeath) throw (ThreadDeath) current;
        if (current instanceof VirtualMachineError) throw (VirtualMachineError) current;

        Throwable next = current.getCause();
        if (next == current) return;
        current = next;
    }
}
```

将 12 个目标 catch 中原来的 `if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;` 统一替换为 `throwIfFatal(t);`，并确保其位于 recording、日志和 return fallback 之前。

普通 ROM、类、方法 hook 安装失败的原有 fallback 保持不变：

- 4 个返回 `null` 的入口仍返回 `null`
- 4 个 silent 入口仍返回 `false`
- 4 个 `void` 的 `hookAll*` 入口仍不返回值

## 修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperHookInstallFatalityTest.kt`
- `tools/tests/test_module_helper_hook_install_fatality.py`

## Mutation 结果

- 删除 helper 的 `ThreadDeath` 分支 → ThreadDeath JVM 测试失败 → 已恢复
- 删除 helper 的 `VirtualMachineError` 分支 → InternalError JVM 测试失败 → 已恢复
- 删除 cause-chain 遍历，只检查最外层 → wrapped fatal/深度 8 JVM 测试失败 → 已恢复
- 将 `findAndHookMethod` 的 helper 调用移到 `recordHookFailure` 之后 → 静态顺序合同失败 → 已恢复
- 从 `findAndHookMethodSilently(String)` 删除 `throwIfFatal(t)` → 静态入口覆盖合同失败 → 已恢复
- 将条件扩大为 `current instanceof Error` → AssertionError JVM 测试与静态合同失败 → 已恢复
- 将 silent 入口 fallback 从 `false` 改为 `true` → 静态 fallback 合同失败 → 已恢复

## 验证结果

- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：592 tests passed, 0 failed, skipped 2
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

- Base SHA: 8aa5bc6de012b6172d8e93d9b6e69f86ee7325cb
- Implementation SHA: 908bd7b94cbd251275d5d5ae942b6b76df9e55ee
- Python test count: 592
- JVM test result: 全部通过
- Baseline: 保持为空
