# FIX-xposedhelpers-reflection-fatal-unwrapping

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java` 的中心反射工具，确保通过反射调用的目标方法或构造器抛出的 fatal error 被解包并原样重抛。

## 当前问题

`invocationTargetError(InvocationTargetException exception)` 只处理 `OutOfMemoryError`，`ThreadDeath` 和 `VirtualMachineError` 会被包装成 `InvocationTargetError`，破坏 fatal propagation。

## 生产实现

修改 `invocationTargetError` 为：

```java
private static InvocationTargetError invocationTargetError(InvocationTargetException exception) {
    Throwable cause = exception.getCause();

    if (cause instanceof OutOfMemoryError) throw (OutOfMemoryError) cause;
    if (cause instanceof ThreadDeath) throw (ThreadDeath) cause;
    if (cause instanceof VirtualMachineError) throw (VirtualMachineError) cause;

    return new InvocationTargetError(cause);
}
```

## 必须保持

- 重抛原始 cause 对象
- 普通异常仍包装为 `new InvocationTargetError(cause)`
- 不把所有 `Error` 都视为 fatal
- null cause 仍返回 `new InvocationTargetError(null)`
- 8 个公共入口继续统一调用 `throw invocationTargetError(e)`
- 不修改 method/constructor resolution、parameter type 推断、`InvocationTargetError` 定义

## 与 ReflectionFatality 的边界

- `XposedHelpers.java` 不调用 `tv.withaibuild.customiuizer.utils.ReflectionFatality`
- 不修改 `ReflectionFatality.kt` 或 `ReflectionFatalityTest.kt`

## JVM 单元测试

将 `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpersOomTest.java` 重命名为 `XposedHelpersFatalityTest.java`，并扩展覆盖：

- OOM / ThreadDeath / InternalError 在 instance / static / constructor 路径的原样重抛
- `IllegalStateException` 仍包装为 `InvocationTargetError` 且 cause 相同

## 静态合同测试

新增 `tools/tests/test_xposed_helpers_reflection_fatality.py`，覆盖：

- helper 唯一存在且检查三类 fatal
- 普通路径返回 `new InvocationTargetError(cause)`
- 不存在 `cause instanceof Error`
- 8 个公共入口都调用 `throw invocationTargetError(e)`
- 不直接构造 `new InvocationTargetError(e.getCause())`
- `InvocationTargetError extends Error` 且 serialVersionUID 不变
- 旧测试文件不存在、新文件名与 public class 名一致
- 生产源码无 `.printStackTrace(`
- baseline 为空

## Mutation 验证

至少五项：

1. 删除 `ThreadDeath` 分支 → ThreadDeath JVM 测试失败
2. 删除 `VirtualMachineError` 分支 → InternalError JVM 测试失败
3. 将 fatal cause 包装为 `InvocationTargetError` → assertSame 失败
4. 将 ordinary cause 直接重抛 → wrapper 测试失败
5. 将 parameterTypes overload 改回 `throw new InvocationTargetError(e.getCause())` → 八入口合同失败

## 验收标准

- [ ] 中心 helper 解包并原样重抛三类 fatal
- [ ] JVM 测试覆盖 12 项
- [ ] 普通异常仍转换为 `InvocationTargetError`
- [ ] 8 个反射入口统一使用 helper
- [ ] 不扩大为全部 Error
- [ ] null cause 语义保持
- [ ] baseline 保持为空
- [ ] source hazard `0 reviewed, 0 new`
- [ ] mutation 有效且恢复
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
docs: add active FIX-xposedhelpers-reflection-fatal-unwrapping task
fix: preserve fatal causes through Xposed reflection
docs: complete FIX-xposedhelpers-reflection-fatal-unwrapping task
```

## 完成记录

- Base SHA: 87546528c43617d3b35013696aa91eef0d739c21
- Implementation SHA: （完成后填写）
