# FIX-helpers-controlled-diagnostics

- Platform: A13
- Status: Active
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

- [ ] `Helpers.kt` 的 15 处 `.printStackTrace()` 全部移除
- [ ] 新增统一且可复用的 settings-app logger
- [ ] 15 个固定 operation 全部存在且唯一
- [ ] fatal error 重抛完整保留
- [ ] 所有 fallback 保持不变
- [ ] 应用列表仍按单条失败隔离并继续遍历
- [ ] XML 解析仍按单 item 失败隔离并继续
- [ ] 不改变方法签名和调用方
- [ ] 不增加依赖、线程、协程或 Context 所有权
- [ ] baseline 从 41 降至 26
- [ ] source hazard 为 26 reviewed、0 new
- [ ] Python tests 通过
- [ ] Gradle unit tests 通过
- [ ] fast verify 通过
- [ ] full verify 通过
- [ ] `git diff --check` 通过
- [ ] 工作区干净
- [ ] 不需要 APK
- [ ] 不需要实机验证
- [ ] 完成状态标记为 `STATIC_VERIFIED`

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

## 构建产物

未要求 APK。

## 完成记录

- Base SHA:
- Final SHA:
- Commits:
- Behavior changed:
- Verification:
- Device evidence:
- Known limits:
