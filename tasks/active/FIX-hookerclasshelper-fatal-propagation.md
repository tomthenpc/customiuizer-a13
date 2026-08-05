# FIX-hookerclasshelper-fatal-propagation

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/HookerClassHelper.java`，确保 `MethodHook` 的 before、host、after 三条路径都不会吞掉或覆盖 fatal error。

## 当前问题

当前 `throwIfOutOfMemory(Throwable throwable)` 只识别 `OutOfMemoryError`，`ThreadDeath` 和 `VirtualMachineError` 会被隔离并记录，破坏 fatal propagation。

## 生产实现

将 helper 重命名为 `throwIfFatal`，实现语义：

```java
private static void throwIfFatal(Throwable throwable) {
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

三处原调用 `throwIfOutOfMemory(...)` 全部改为 `throwIfFatal(...)`。

## 必须保持

- before、host、after 的普通异常仍隔离并记录
- fatal before 不执行 host
- fatal host 不执行 after
- fatal after 在 host 执行后立即传播
- 不得扩大为 `current instanceof Error`
- cause-chain 深度限制为 8，保留自引用保护
- 不得新增日志、依赖、锁、缓存或 Kotlin helper
- 不修改 callback public API 和 result/throwable semantics

## JVM 单元测试

更新 `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/HookerClassHelperTest.java`：

- 保留现有测试
- 新增 ThreadDeath / InternalError 的 direct/wrapped 三类路径测试
- 新增 wrapped ordinary failure 仍被隔离、AssertionError 仍被隔离

## 静态合同测试

新增 `tools/tests/test_hooker_class_helper_fatality.py`。

## 验收标准

- [ ] helper 重命名并实现三类 fatal 检查
- [ ] 三处调用点更新
- [ ] JVM 测试覆盖新增 13+ 项
- [ ] 静态合同测试覆盖 23 项
- [ ] mutation 验证有效且恢复
- [ ] baseline 保持为空
- [ ] source hazard `0 reviewed, 0 new`
- [ ] 严格验证通过
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
docs: add active FIX-hookerclasshelper-fatal-propagation task
fix: preserve fatal errors across hook callbacks
docs: complete FIX-hookerclasshelper-fatal-propagation task
```

## 完成记录

- Base SHA: dc90b75f03fe95686593d0f80254acccfbfd2144
- Implementation SHA: （完成后填写）
