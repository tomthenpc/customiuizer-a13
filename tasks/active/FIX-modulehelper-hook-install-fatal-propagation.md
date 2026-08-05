# FIX-modulehelper-hook-install-fatal-propagation

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`，确保 12 个 hook 安装入口捕获异常后，均原样传播三类 fatal error，同时保持普通失败记录、日志和 fallback 不变。

## 当前问题

12 个 hook 安装 catch 目前仅显式检查 `OutOfMemoryError`，`ThreadDeath` 和 `VirtualMachineError` 会被静默转换为 `null`、`false` 或记录成普通 HookInstaller failure。

## 生产实现

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

在 12 个目标 catch 中，将原有的 `t instanceof OutOfMemoryError` 检查替换为 `throwIfFatal(t);` 并置于 recording、日志和 fallback 之前。

## 必须保持

- 普通 ROM、类、方法 hook 安装失败仍按原路径记录并 fallback
- silent 入口继续 silent
- 不修改 `recordHookFailure`、`extractParameterTypes`、`guarded`、field/context helpers
- 不引入新依赖或跨文件公共 helper

## JVM 单元测试

新增 `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperHookInstallFatalityTest.kt`：

- 直接/ wrapped OOM、ThreadDeath、InternalError
- 普通 IllegalStateException / AssertionError 正常返回
- null 输入正常返回
- 深度 8/9 边界

## 静态合同测试

新增 `tools/tests/test_module_helper_hook_install_fatality.py`。

## 验收标准

- [ ] 12 个入口统一调用 `throwIfFatal`
- [ ] direct/wrapped fatal 原样重抛
- [ ] fatal 不进入 HookInstaller recording
- [ ] ordinary 记录/日志/fallback 不变
- [ ] 静态合同测试通过
- [ ] mutation 验证有效且恢复
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
docs: add active FIX-modulehelper-hook-install-fatal-propagation task
fix: preserve fatal errors during hook installation
docs: complete FIX-modulehelper-hook-install-fatal-propagation task
```

## 完成记录

- Base SHA: 8aa5bc6de012b6172d8e93d9b6e69f86ee7325cb
- Implementation SHA: （完成后填写）
