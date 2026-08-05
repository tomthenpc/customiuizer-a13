# FIX-catch-fatal-rethrow-and-empty-catch

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

修复 `CATCH_THROWABLE_NO_FATAL`（152 处）和 `EMPTY_CATCH`（36 处）两类代码质量门禁：
- 所有 `catch (Throwable)` 在兼容处理前先显式重新抛出 `VirtualMachineError` 与 `ThreadDeath`；
- 剩余 7 处非 Throwable 空 catch 补充语义注释，避免被误判为隐藏运行时失败。

## 当前问题

`tools/source_hazard_scan.py` 扫描出：
- `CATCH_THROWABLE_NO_FATAL` 152 处：`catch (Throwable)` 块未保留 fatal error 传播；
- `EMPTY_CATCH` 36 处：空 catch 块，可能隐藏失败。

这些 catch 块分布在 50 余个源文件，包括 MainModule 流程、Hook 回调、SystemUI/Launcher/Settings 等安装器，以及 `ResourceHooks.java`、`XposedHelpers.java` 等核心工具类。若发生 `OutOfMemoryError` 或 `ThreadDeath` 被吞掉，会导致模块以静默崩溃或卡死方式失败，难以诊断。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/**/*.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ResourceHooks.java`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java`

## 必须保持

- 非 fatal 异常的原有处理语义不变（printStackTrace / XposedHelpers.log / 默认值 / 返回 null 等）；
- 不引入新的空 catch；
- 不修改 Hook 目标、参数索引、反射边界或生命周期；
- `XposedHelpers.findMethodBestMatch` / `findConstructorBestMatch` 的 fallback 语义不变。

## 实现要求

1. 对每个 `catch (Throwable)` 块，在首行插入：
   ```kotlin
   if (<ex> is OutOfMemoryError || <ex> is ThreadDeath || <ex> is VirtualMachineError) throw <ex>
   ```
   并在 `ResourceHooks.java` 中使用 Java 等价的 `instanceof` 判断。
2. 修复自动插入导致的单表达式 catch 体语法错误（如 `throw t false }`、`throw t XposedHelpers.log(t) }`、空 catch 的 `throw fatal }`）。
3. 对 7 处非 Throwable 空 catch 添加语义注释，说明异常为何可安全忽略。

## 非目标

- 不修复 `STATIC_STRONG_ANDROID_OWNER`、`PRINT_STACK_TRACE`、`THREAD_SLEEP` 等其它门禁类别（归属 P2/P3）；
- 不做大规模 Java→Kotlin 迁移；
- 不修改功能行为。

## 验收标准

- [x] `source_hazard_scan.py` 中 `CATCH_THROWABLE_NO_FATAL` 降为 0
- [x] `source_hazard_scan.py` 中 `EMPTY_CATCH` 降为 0
- [x] `compileDebugKotlin` 通过
- [x] `compileDebugJavaWithJavac` 通过
- [x] `check-invariants.py` 通过
- [x] `check-compat-contracts.py` 通过
- [x] `check_hook_contract_parity.py` 通过
- [x] 最终 diff 已审查
- [x] 工作区没有未解释改动

## 验证

```powershell
python tools/source_hazard_scan.py --strict-all --path app/src/main/java --json-output hazard_after.json
python tools/verify.py fast --changed
git diff --check
```

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: 003c777f02ce81822d7689397bc4414692c48f65
- Final SHA: 本记录所在的收口 commit
- Commits: 1（本任务单次收口提交）
- Behavior changed: 否；fatal error（VirtualMachineError / ThreadDeath）现在会正确继续抛出，其它异常处理语义不变
- Verification: `source_hazard_scan.py` / `verify.py fast --changed` / `git diff --check`
- Device evidence: 无（本任务为静态错误边界修复，BUILD_VERIFIED）
- Known limits: 仅保证 fatal error 继续传播；非 fatal 异常的诊断粒度未增强，仍依赖原有 `log`/`printStackTrace`
