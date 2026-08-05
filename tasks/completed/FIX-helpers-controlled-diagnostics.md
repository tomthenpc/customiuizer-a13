# FIX-helpers-controlled-diagnostics

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

移除 `Helpers.kt` 中全部 15 处 `.printStackTrace()`，建立 settings-app 共用的轻量受控诊断入口。

完成后：

- `Helpers.kt` 不再包含 `.printStackTrace(`
- 新增统一的 `SettingsDiagnostics`
- 所有现有 fallback、异常边界、循环行为和方法签名保持不变
- `SOURCE_HAZARD_BASELINE.json` 从 41 条降至 26 条
- baseline 只删除 `Helpers.kt` 的 15 条 finding

## 当前问题

`Helpers.kt` 中 15 处生产代码调用 `.printStackTrace()`，触发 `PRINT_STACK_TRACE` source hazard 规则。需要替换为 settings-app 统一的受控 `Log.e` 诊断入口。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/utils/SettingsDiagnostics.kt`（新增）
- `app/src/test/java/tv/withaibuild/customiuizer/utils/SettingsDiagnosticsHelpersTest.kt`（新增）
- `tools/tests/test_helpers_diagnostics.py`（新增）
- `docs/audit/SOURCE_HAZARD_BASELINE.json`

## 必须保持

- 每个 catch 仍先判断并立即重抛 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError`；
- fatal guard 不进入 `SettingsDiagnostics`；
- 所有 fallback 返回值不变；
- 应用列表单条失败后继续处理后续条目；
- `parsePrefXml` 单 item 失败后继续读取后续 XML event；
- `copyFile` 仍使用 `StandardCopyOption.REPLACE_EXISTING`；
- 不修改方法签名、调用方、循环逻辑、排序或 dual-app 判断。

## 实现要求

1. 新增 `SettingsDiagnostics.kt`：

   ```kotlin
   package tv.withaibuild.customiuizer.utils

   import android.util.Log

   internal object SettingsDiagnostics {
       private const val TAG = "CustoMIUIzer-Settings"

       fun failure(operation: String, throwable: Throwable) {
           Log.e(TAG, operation, throwable)
       }
   }
   ```

2. `Helpers.kt` 中 15 处 `.printStackTrace()` 替换为 `SettingsDiagnostics.failure("operation", throwable)`。
3. 15 个 operation 名称固定为：
   - `Helpers.hideKeyboard`
   - `Helpers.updateNewModsMarking`
   - `Helpers.getAnimationScale`
   - `Helpers.setAnimationScale`
   - `Helpers.getPackageInfoAsUser`
   - `Helpers.getInstalledApps.item`
   - `Helpers.getLaunchableApps.item`
   - `Helpers.getShareApps.item`
   - `Helpers.getOpenWithApps.item`
   - `Helpers.getAppName.application`
   - `Helpers.getAppName.activity`
   - `Helpers.getAppIcon.application`
   - `Helpers.getAppIcon.activity`
   - `Helpers.parsePrefXml.item`
   - `Helpers.copyFile`
4. 不修改 `HookUtils.kt`、`XposedHelpers`、Installer、FeatureCatalog、PreferenceSchema 或其他 26 条 baseline 所在文件。

## 非目标

- 不处理 `HookUtils.kt`；
- 不处理其他 26 条 baseline finding；
- 不给 `emptyFile`、静默 `ignore` 等 catch 添加日志；
- 不修改 XML、资源或方法签名。

## 验收标准

- [x] `Helpers.kt` 的 15 处 `.printStackTrace()` 全部移除
- [x] 新增统一且可复用的 settings-app logger
- [x] 15 个固定 operation 全部存在且唯一
- [x] fatal error 重抛完整保留
- [x] 所有 fallback 保持不变
- [x] 应用列表仍按单条失败隔离并继续遍历
- [x] XML 解析仍按单 item 失败隔离并继续
- [x] 不改变方法签名和调用方
- [x] 不增加依赖、线程、协程或 Context 所有权
- [x] baseline 从 41 降至 26
- [x] source hazard 为 26 reviewed、0 new
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

.\gradlew.bat :app:testDebugUnitTest --tests "tv.withaibuild.customiuizer.utils.SettingsDiagnosticsHelpersTest"
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
- `unittest discover`：449 tests passed, 0 failed, skipped 2
- `testDebugUnitTest --tests SettingsDiagnosticsHelpersTest`：通过
- `compileDebugKotlin`：通过
- `compileDebugJavaWithJavac`：通过
- `testDebugUnitTest`：通过
- `source_hazard_scan.py --path app/src/main/java`：`Source hazard scan passed: 26 reviewed finding(s), 0 new`
- `verify.py fast --changed`：通过
- `verify.py full`：通过（含 compileDebugKotlin、compileDebugJavaWithJavac、testDebugUnitTest-all、lintDebug）
- `git diff --check`：通过（仅 CRLF 转 LF 正常提示）
- `git status --short`：干净

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: ab18d7dcad4a02c7d8ebc415b772b7b7b59fbd50
- Final SHA: 4a5bab377a4765f0657b8057180a826fd9183e97
- Commits:
  - `40408c9` docs: add active FIX-helpers-controlled-diagnostics task
  - `4a5bab3` fix: replace Helpers printStackTrace diagnostics
- Behavior changed:
  - `Helpers.kt` 的 15 处 `.printStackTrace()` 全部替换为 `SettingsDiagnostics.failure(operation, throwable)`；
  - 新增 `SettingsDiagnostics` 轻量无状态 logger，固定 tag `CustoMIUIzer-Settings`；
  - 15 个 operation 名称按规范固定，每个恰好调用一次；
  - 每处 catch 仍先重抛 fatal error，fallback 全部保持；
  - 应用列表循环和 `parsePrefXml` 仍单条失败后继续；
  - `SOURCE_HAZARD_BASELINE.json` 从 41 条降至 26 条，仅移除 `Helpers.kt` 的 15 条 `PRINT_STACK_TRACE`；
  - 新增 `SettingsDiagnosticsHelpersTest.kt` 和 `test_helpers_diagnostics.py` 覆盖运行时与静态合同。
- Verification: 见“验证”章节
- Device evidence: 未涉及设备；本任务仅 settings-app 诊断入口和门禁改动，标记为 `STATIC_VERIFIED`
- Known limits: 未修改 `HookUtils.kt`、其他 26 条 baseline finding 文件或全局日志设施；`SettingsDiagnostics` 设计为后续 settings-app 文件复用。
