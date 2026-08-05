# FIX-modulehelper-reflection-fallback-fatal-propagation

- Platform: A13
- Status: Active
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java` 的四个反射 fallback 方法，使其捕获 `Throwable` 后首先调用已有的 `throwIfFatal(t)`，原样传播 `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError`，同时保持普通反射失败的原有 fallback 行为不变。

## 当前问题

- `getStaticObjectFieldSilently` 与 `getObjectFieldSilently` 仍使用直接 `if (t instanceof OutOfMemoryError)` guard，未覆盖 `ThreadDeath` 和 `VirtualMachineError`，也未遍历 cause-chain。
- 两个 `findContext` overload 同样使用直接 OOM guard，无法原样传播完整 fatal chain。
- 新任务与上一任务的旧静态合同 `test_non_target_field_helpers_unchanged` 冲突，需要更新为反映新稳态的合同。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`
- `tools/tests/test_module_helper_reflection_fallback_fatality.py`
- `tools/tests/test_module_helper_hook_install_fatality.py`
- `tasks/active/FIX-modulehelper-reflection-fallback-fatal-propagation.md`
- `tasks/completed/FIX-modulehelper-reflection-fallback-fatal-propagation.md`

## 必须保持

- `throwIfFatal(Throwable)` 方法体不修改。
- 字段读取失败继续返回 `NOT_EXIST_SYMBOL`（值为 `ObjectFieldNotExist`）。
- 上下文发现失败继续返回当前 `context` 值（通常为 `null`）。
- `findContext()` 使用 `MainModule.class.getClassLoader()`。
- `findContext(PackageReadyParam)` 使用 `lpparam.getClassLoader()`。
- ActivityThread 反射顺序和四个缓存字段不变。
- 12 个 hook 安装入口、`recordHookFailure`、`extractParameterTypes`、receiver、alarm、observer、guarded 等方法不修改。
- `docs/audit/SOURCE_HAZARD_BASELINE.json` 继续为空。

## 实现要求

1. 将 `getStaticObjectFieldSilently`、`getObjectFieldSilently`、两个 `findContext` 的 catch 改为：

   ```java
   catch (Throwable t) {
       throwIfFatal(t);
       // field helpers 随后 return NOT_EXIST_SYMBOL
       // findContext 不返回，最终方法末尾 return context
   }
   ```

2. 复用 package-private `throwIfFatal(Throwable)`，不新增 helper，不修改可见性，不复制 fatal 判断。
3. 向 `findContext` catch 传递完整外层 `Throwable`（例如 `InvocationTargetException`），而非仅 `t.getCause()`。
4. 普通失败（`NoSuchFieldError`、`NoSuchFieldException`、`IllegalAccessException`、`ClassNotFoundException`、普通 `InvocationTargetException`、`RuntimeException`、`AssertionError`、`LinkageError`）仍被 fallback 吸收。
5. 新增静态合同测试 `tools/tests/test_module_helper_reflection_fallback_fatality.py`。
6. 更新 `tools/tests/test_module_helper_hook_install_fatality.py` 中与本任务新稳态冲突的合同。
7. 执行并恢复至少六项 mutation 验证。

## 非目标

- 不修改 `ModuleHelperHookInstallFatalityTest.kt` 等已有 JVM 测试。
- 不引入 Robolectric、Mockito 或新依赖。
- 不修改 Gradle、dependency verification、source hazard scanner 或 baseline。
- 不扩大 fatal helper 到 `Error` 或 `AssertionError`。

## 验收标准

- [ ] 四个目标方法统一调用已有 `throwIfFatal(t)`
- [ ] direct/wrapped 三类 fatal 均可原样传播
- [ ] 字段 ordinary failure 继续返回 `NOT_EXIST_SYMBOL`
- [ ] 上下文 ordinary failure 继续返回当前 `context`
- [ ] 两个 `findContext` ClassLoader 来源不变
- [ ] ActivityThread 反射顺序和缓存不变
- [ ] 不新增日志或诊断
- [ ] 冲突静态合同已更新而非删除
- [ ] 12 个 hook 安装入口合同继续通过
- [ ] 非目标 ModuleHelper 方法不修改
- [ ] baseline 保持为空
- [ ] source hazard 保持 `0 reviewed, 0 new`
- [ ] mutation 验证有效且全部恢复
- [ ] Python、Gradle unit、lint、fast、full 全部通过
- [ ] `git diff --check` 通过
- [ ] 工作区干净

## 验证

```powershell
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
python tools/source_hazard_scan.py --path app/src/main/java
python tools/verify.py fast --changed
python tools/verify.py full
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest --dependency-verification=strict
git diff --check
```

## 构建产物

本任务不要求 APK。

## 完成记录

- Base SHA:
- Implementation SHA:
- Python test count:
- Mutation results:
- Verification results:
