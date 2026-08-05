# FIX-security-adapter-reflection-fatal-unwrapping

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

确保 `LockedAppAdapter.readChecked` 和 `PrivacyAppAdapter.readChecked` 不仅重抛直接 fatal error，也重抛被 `InvocationTargetException` 包装的 fatal cause。

目标文件：

- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/ReflectionFatality.kt`（新增）
- `app/src/test/java/tv/withaibuild/customiuizer/utils/ReflectionFatalityTest.kt`（新增）
- `tools/tests/test_security_adapter_readchecked_diagnostics.py`（更新）

## 新增 ReflectionFatality.kt

```kotlin
package tv.withaibuild.customiuizer.utils

import java.lang.reflect.InvocationTargetException

internal object ReflectionFatality {

    fun rethrowIfFatal(error: Throwable) {
        val candidate =
            if (error is InvocationTargetException) {
                error.cause ?: error
            } else {
                error
            }

        if (
            candidate is OutOfMemoryError ||
            candidate is ThreadDeath ||
            candidate is VirtualMachineError
        ) throw candidate
    }
}
```

要求：

- 只解包 `InvocationTargetException`
- 使用 `error.cause ?: error`
- 检查三类 fatal
- 不重抛普通异常
- 不记录日志、不引用 Android/Xposed、不包装异常
- 重抛原始 fatal 对象

## Adapter 集成

`LockedAppAdapter.readChecked` 和 `PrivacyAppAdapter.readChecked` 捕获 `Throwable` 后：

```kotlin
ReflectionFatality.rethrowIfFatal(error)

if (!readCheckedFailureLogged) {
    readCheckedFailureLogged = true
    SettingsDiagnostics.failure("...Adapter.readChecked", error)
}

false
```

必须保持：

- 直接 fatal 和 wrapped fatal 都重抛
- 普通失败仍记录并返回 `false`
- log-once 标志不变
- `SettingsDiagnostics.failure` 仍记录外层 `error`
- 不将 manager/method 设为 null
- 初始化 operation 不修改

## 新增 JVM 单元测试

`app/src/test/java/tv/withaibuild/customiuizer/utils/ReflectionFatalityTest.kt` 覆盖：

- 直接 OOM/ThreadDeath/InternalError 原实例重抛（`assertSame`）
- `InvocationTargetException(OOM)` / `InvocationTargetException(ThreadDeath)` / `InvocationTargetException(InternalError)` 重抛原 cause
- 普通 `IllegalStateException` 正常返回
- `InvocationTargetException(IllegalStateException)` 正常返回
- cause 为 null 的 `InvocationTargetException` 正常返回

## 静态合同测试更新

`tools/tests/test_security_adapter_readchecked_diagnostics.py`：

- 验证两个 `readChecked()` 调用 `ReflectionFatality.rethrowIfFatal(error)`
- helper 调用位于 log-once 判断和 `SettingsDiagnostics.failure` 之前
- 每个 catch 中 helper 恰好一次
- 仍记录外层 `error`，不记录 `error.cause`
- 普通 fallback 仍为 false
- log-once、不清空 manager/Method、method.invoke 参数等合同保持
- `ReflectionFatality.kt` 纯 JVM、不引用 Android/Xposed、只解包 `InvocationTargetException`、使用 `error.cause ?: error`、检查三类 fatal、无日志

## Mutation 验证

至少五项：

1. `error.cause ?: error` 改为 `error` → wrapped fatal JVM 测试失败
2. 删除 `ThreadDeath` 分支 → ThreadDeath 测试失败
3. 普通 wrapped exception 也抛出 → ordinary wrapped 测试失败
4. adapter helper 调用移到日志之后 → 静态顺序合同失败
5. 日志参数从 `error` 改为 `error.cause ?: error` → 外层诊断合同失败

所有 mutation 必须恢复。

## Baseline 合同

不修改 `docs/audit/SOURCE_HAZARD_BASELINE.json`，保持为空。source hazard 必须 `0 reviewed, 0 new`。

## 验收标准

- [ ] `ReflectionFatality.kt` 正确实现
- [ ] 两个 adapter 调用 `ReflectionFatality.rethrowIfFatal(error)`
- [ ] 直接和 wrapped fatal 均重抛原始 fatal 实例
- [ ] 普通异常仍记录并返回 false
- [ ] log-once 行为保持
- [ ] 不修改初始化 operation
- [ ] baseline 保持为空
- [ ] source hazard `0 reviewed, 0 new`
- [ ] JVM 单元测试通过
- [ ] mutation 验证有效且恢复
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

额外检查：

```powershell
Get-ChildItem app/src/main/java -Recurse -Include *.kt,*.java |
    Select-String -SimpleMatch ".printStackTrace("
```

## 提交建议

```text
docs: add active FIX-security-adapter-reflection-fatal-unwrapping task
fix: unwrap fatal reflection failures in security adapters
docs: complete FIX-security-adapter-reflection-fatal-unwrapping task
```

## 完成记录

- Base SHA: 6973725b82b97b34909206a5529d1d7183bd0b52
- Implementation SHA: （完成后填写）
