# FIX-security-adapter-readchecked-diagnostics

- Platform: A13
- Status: In Progress
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

修复 `LockedAppAdapter.readChecked` 和 `PrivacyAppAdapter.readChecked` 的静默反射失败和不完整 fatal guard。

目标文件：

- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt`

要求：

- 普通反射读取失败仍返回 `false`
- `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError` 立即重抛
- 普通失败通过 `SettingsDiagnostics` 记录
- 每个 adapter 实例最多记录一次 read failure
- 后续读取仍继续尝试反射，不建立全局熔断
- 不改变列表、排序、刷新和筛选行为

## 当前问题

两个 `readChecked()` 当前 catch 只重抛 `OutOfMemoryError`，会吞掉 `ThreadDeath` 和 `VirtualMachineError`，并且普通反射失败完全不可观测。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt`
- `tools/tests/test_security_adapter_readchecked_diagnostics.py`（新增）
- `tasks/active/FIX-security-adapter-readchecked-diagnostics.md`
- `tasks/completed/FIX-security-adapter-readchecked-diagnostics.md`

## 必须保持

- 反射调用方法名和参数不变（Locked `getApplicationAccessControlEnabledAsUser`，Privacy `isPrivacyApp`，参数 `app.pkgName.orEmpty()`、`app.user`）
- `as? Boolean ?: false` 不变
- 普通失败仍返回 `false`
- 不删除应用、不过滤列表、不弹 Toast、不修改 `checkedApps`、不调用 `notifyDataSetChanged()`
- 不将 manager 或 Method 设为 null
- 不使用 `AtomicBoolean`、synchronized、lock、coroutine、全局集合或静态缓存
- 不修改已完成的初始化 operation：`LockedAppAdapter.initializeSecurityManager` 和 `PrivacyAppAdapter.initializeSecurityManager`
- 不修改 `SettingsDiagnostics.kt`、`Helpers.kt`、`HookUtils.kt`、baseline、scanner、构建配置

## 实现要求

### LockedAppAdapter

增加实例字段：

```kotlin
private var readCheckedFailureLogged = false
```

catch 结构：

```kotlin
catch (error: Throwable) {
    if (
        error is OutOfMemoryError ||
        error is ThreadDeath ||
        error is VirtualMachineError
    ) throw error

    if (!readCheckedFailureLogged) {
        readCheckedFailureLogged = true
        SettingsDiagnostics.failure("LockedAppAdapter.readChecked", error)
    }

    false
}
```

### PrivacyAppAdapter

增加实例字段：

```kotlin
private var readCheckedFailureLogged = false
```

operation 精确为 `PrivacyAppAdapter.readChecked`，结构相同。

## 静态合同测试

新增 `tools/tests/test_security_adapter_readchecked_diagnostics.py`，覆盖：

1. 两个文件全局不存在 `.printStackTrace(`
2. 两个 `readChecked()` 返回 `Boolean`
3. 两个方法仍通过缓存的 `Method.invoke` 读取
4. Locked 参数仍为 `app.pkgName.orEmpty(), app.user`
5. Privacy 参数仍为 `app.pkgName.orEmpty(), app.user`
6. 两个 catch 均包含 `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError`、fatal rethrow
7. 两个普通 fallback 均为 `false`
8. 两个固定 operation 各出现一次
9. 两个类各自存在实例级 log-once 布尔字段
10. 日志调用被 `if (!readCheckedFailureLogged)` 保护
11. 标志在日志前或同一保护块内设为 true
12. catch 中不把 manager 或 Method 设为 null
13. catch 中不存在 `return true`
14. catch 中不存在 `notifyDataSetChanged`
15. `refresh()` 行为未被重写
16. 初始化 operation 仍各存在一次
17. source hazard baseline 仍为空
18. 生产源码全局仍不存在 `.printStackTrace(`

## Mutation 验证

临时执行并恢复至少四项：

1. 删除 Locked `ThreadDeath` guard → fatal guard 测试失败
2. 将 Privacy fallback 从 `false` 改为 `true` → fallback 测试失败
3. 删除 `if (!readCheckedFailureLogged)` → log-once 测试失败
4. 在 Locked catch 中加入 `getApplicationAccessControlEnabledAsUser = null` → 后续反射尝试测试失败

## Baseline 合同

`docs/audit/SOURCE_HAZARD_BASELINE.json` 不修改，仍为空。

## 验收标准

- [ ] 两个 `readChecked()` 具备完整 fatal guard
- [ ] 普通失败仍返回 false
- [ ] 两个固定 operation 唯一
- [ ] 每个 adapter 实例最多记录一次失败
- [ ] 后续调用仍继续尝试反射
- [ ] 不清空 manager 或 Method
- [ ] 初始化、排序、筛选、刷新和图标行为不变
- [ ] baseline 保持为空
- [ ] source hazard 保持 `0 reviewed, 0 new`
- [ ] mutation 验证有效且全部恢复
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
docs: add active FIX-security-adapter-readchecked-diagnostics task
fix: harden security adapter state reads
docs: complete FIX-security-adapter-readchecked-diagnostics task
```

## 完成记录

- Base SHA: 78ed0f07cbf9c002b97a0e5d4d52976afdabcb9b
- Implementation SHA: （完成后填写）
