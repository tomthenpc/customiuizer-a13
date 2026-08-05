# FIX-hookutils-controlled-diagnostics

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

移除 `HookUtils.kt` 中全部 6 处生产代码 `printStackTrace()`，改为带固定 tag 和明确 operation 的受控日志。

本任务只处理：

```text
app/src/main/java/tv/withaibuild/customiuizer/utils/HookUtils.kt
```

不得扩大到其他 41 条 baseline finding。

完成后：

- `HookUtils.kt` 不再出现 `.printStackTrace(`
- `SOURCE_HAZARD_BASELINE.json` 从 47 条降至 41 条
- 运行时返回值、异常边界和 fatal error 行为保持不变

## 当前问题

`HookUtils.kt` 中 6 个 catch 块使用 `printStackTrace()` 输出诊断信息，违反了 source hazard 门禁的 `PRINT_STACK_TRACE` 规则。需要替换为带固定 tag 和 operation 的 `Log.e(TAG, operation, throwable)`。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/utils/HookUtils.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/utils/HookUtilsDiagnosticsTest.kt`（新增）
- `tools/tests/test_hookutils_diagnostics.py`（新增）
- `docs/audit/SOURCE_HAZARD_BASELINE.json`

## 必须保持

每个 catch 必须继续先执行 fatal error 判断：

```kotlin
if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
```

顺序必须是：

```text
catch Throwable
→ fatal error 立即重抛
→ 记录普通异常
→ 返回原有 fallback
```

各方法 fallback 必须保持：

- `copyFile`：`false`
- `getAnimationScale`：`1.0f`
- `getAppName`：`null`
- `getAppIcon`：`null`

不得：

- 吞掉 `OutOfMemoryError`
- 吞掉 `ThreadDeath`
- 吞掉 `VirtualMachineError`
- 将 catch 改为空
- 改为 `System.out` / `System.err`
- 使用 `printStackTrace`
- 新增线程、协程或异步日志
- 引入新的日志依赖
- 调用 settings-app 的重型 `Helpers`
- 修改 `XposedHelpers` 或其他全局日志设施

## 实现要求

1. 在 `HookUtils` 内增加：

   ```kotlin
   private const val TAG = "CustoMIUIzer-HookUtils"

   private fun logFailure(operation: String, throwable: Throwable) {
       Log.e(TAG, operation, throwable)
   }
   ```

2. 增加 `import android.util.Log`。
3. 六处 operation 名称固定为：
   - `copyFile`
   - `getAnimationScale`
   - `getAppName.application`
   - `getAppName.activity`
   - `getAppIcon.application`
   - `getAppIcon.activity`
4. 每处 catch 保持 fatal error 重抛后调用 `logFailure`。
5. 不改变方法签名、算法、反射目标、文件复制逻辑、PackageManager 查询逻辑。

## 非目标

- 不处理其他文件的 baseline finding；
- 不修改 Hook、Installer、FeatureCatalog、PreferenceSchema；
- 不修改 `XposedHelpers` 或 settings-app 的 `Helpers`。

## 验收标准

- [x] `HookUtils.kt` 的 6 处 `printStackTrace()` 全部移除
- [x] 使用一个统一、轻量、固定 tag 的日志入口
- [x] 六处日志均有明确 operation 名称
- [x] fatal error 重抛逻辑完整保留
- [x] 所有 fallback 返回值不变
- [x] 不改变方法签名和调用方
- [x] 不引入新依赖
- [x] 不修改其他 baseline finding
- [x] source hazard 从 47 降到 41，0 new
- [x] Python tests 通过
- [x] Gradle unit tests 通过
- [x] fast verify 通过
- [x] full verify 通过
- [x] `git diff --check` 通过
- [x] 工作区干净
- [x] 不需要 APK
- [x] 不需要实机验证
- [x] 完成状态标记为 `STATIC_VERIFIED`

## 验证

```powershell
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"

.\gradlew.bat :app:testDebugUnitTest --tests "tv.withaibuild.customiuizer.utils.HookUtilsDiagnosticsTest"
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:testDebugUnitTest

python tools/source_hazard_scan.py --path app/src/main/java
python tools/verify.py fast --changed
python tools/verify.py full

git diff --check
git status --short
```

### 实际结果

- `compileall tools`：通过
- `unittest discover`：433 tests passed, 0 failed, skipped 2
- `testDebugUnitTest --tests HookUtilsDiagnosticsTest`：通过
- `compileDebugKotlin`：通过
- `compileDebugJavaWithJavac`：通过
- `testDebugUnitTest`：通过
- `source_hazard_scan.py --path app/src/main/java`：`Source hazard scan passed: 41 reviewed finding(s), 0 new`
- `verify.py fast --changed`：通过
- `verify.py full`：通过（含 compileDebugKotlin、compileDebugJavaWithJavac、testDebugUnitTest-all、lintDebug）
- `git diff --check`：通过（仅 SOURCE_HAZARD_BASELINE.json CRLF 转 LF 的正常 git 提示）
- `git status --short`：干净

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: 0ed5e700da3f7f4edd05328a5534bf25b2c3c874
- Final SHA: 8aefa75c9bf01ee1c8ab658718b5028c5de6a9aa
- Commits:
  - `8999ced` docs: add active FIX-hookutils-controlled-diagnostics task
  - `8aefa75` fix: replace HookUtils printStackTrace diagnostics
- Behavior changed:
  - `HookUtils.kt` 的 6 处 `.printStackTrace()` 全部替换为 `logFailure(operation, throwable)`；
  - 新增固定 `TAG = "CustoMIUIzer-HookUtils"` 和统一 `logFailure` 入口；
  - 六处 operation 名称：`copyFile`、`getAnimationScale`、`getAppName.application`、`getAppName.activity`、`getAppIcon.application`、`getAppIcon.activity`；
  - 每处 catch 仍先重抛 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError`；
  - 所有 fallback 返回值不变；
  - `SOURCE_HAZARD_BASELINE.json` 从 47 条降至 41 条，仅移除 `HookUtils.kt` 的 6 条 `PRINT_STACK_TRACE`；
  - 新增 `HookUtilsDiagnosticsTest.kt` 和 `test_hookutils_diagnostics.py` 覆盖运行时与静态合同。
- Verification: 见“验证”章节
- Device evidence: 未涉及设备；本任务仅工具/门禁改动，标记为 `STATIC_VERIFIED`
- Known limits: 未修改 `XposedHelpers`、settings-app `Helpers` 或其他全局日志设施；其他 41 条 baseline finding 保留待后续处理。
