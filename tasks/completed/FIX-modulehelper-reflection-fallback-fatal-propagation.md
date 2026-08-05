# FIX-modulehelper-reflection-fallback-fatal-propagation

- Platform: A13
- Status: Done
- Priority: P1
- Owner: Devin
- Reviewer: ChatGPT / human

## 目标

完善 `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`，使四个反射 fallback 方法捕获 `Throwable` 后统一调用已有 `throwIfFatal(t)`，原样传播 `OutOfMemoryError`、`ThreadDeath` 与 `VirtualMachineError`，同时保持普通反射失败的原有 fallback 行为不变。

## 完成摘要

将四个目标 catch 中原来的直接 OOM guard：

```java
if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;
```

统一替换为：

```java
throwIfFatal(t);
```

目标方法：

- `getStaticObjectFieldSilently`
- `getObjectFieldSilently`
- `findContext()`
- `findContext(PackageReadyParam)`

保持：

- 字段读取失败继续返回 `NOT_EXIST_SYMBOL`（值为 `ObjectFieldNotExist`）。
- 上下文发现失败继续返回当前 `context` 值（通常为 `null`）。
- `findContext()` 仍使用 `MainModule.class.getClassLoader()`。
- `findContext(PackageReadyParam)` 仍使用 `lpparam.getClassLoader()`。
- ActivityThread 反射顺序与四个缓存字段不变。
- `throwIfFatal(Throwable)` 方法体不修改。

新增 `tools/tests/test_module_helper_reflection_fallback_fatality.py`，覆盖四个方法 catch 形状、字段/上下文 fallback 合同、ClassLoader 来源、ActivityThread 反射顺序、范围保护等。

更新 `tools/tests/test_module_helper_hook_install_fatality.py` 中与本任务新稳态冲突的合同，从要求 `if (t instanceof OutOfMemoryError)` 改为要求 `throwIfFatal(t)` 与 `return NOT_EXIST_SYMBOL`。

## 修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`
- `tools/tests/test_module_helper_reflection_fallback_fatality.py`
- `tools/tests/test_module_helper_hook_install_fatality.py`
- `tasks/active/FIX-modulehelper-reflection-fallback-fatal-propagation.md`
- `tasks/completed/FIX-modulehelper-reflection-fallback-fatal-propagation.md`

## Mutation 结果

所有 mutation 均恢复，未进入提交。

1. 删除 `getStaticObjectFieldSilently` 中的 `throwIfFatal(t)` → `throwIfFatal` 入口合同失败 → 已恢复
2. 将 `getObjectFieldSilently` fallback 改为 `null` → sentinel fallback 合同失败 → 已恢复
3. 将无参数 `findContext()` 改用 `ClassLoader.getSystemClassLoader()` → ClassLoader 合同失败 → 已恢复
4. 将 `findContext(PackageReadyParam)` 改用 `MainModule.class.getClassLoader()` → overload ClassLoader 合同失败 → 已恢复
5. 将 `findContext()` catch 改回直接 OOM guard → shared helper 合同失败 → 已恢复
6. 在 `getStaticObjectFieldSilently` catch 中 `throwIfFatal(t)` 前增加 `return NOT_EXIST_SYMBOL` → first-statement 顺序合同失败 → 已恢复
7. 在 `findContext()` catch 中加入 `context = null` → context preservation 合同失败 → 已恢复

## 验证结果

- `git diff --check`：通过
- `python -m compileall tools`：通过
- `python -m unittest discover -s tools/tests -p "test_*.py"`：624 tests passed, 0 failed, skipped 2
- `python tools/source_hazard_scan.py --path app/src/main/java`：`0 reviewed, 0 new`
- `python tools/verify.py fast --changed`：通过
- `python tools/verify.py full`：通过
- `.\gradlew.bat :app:compileDebugKotlin`：通过
- `.\gradlew.bat :app:compileDebugJavaWithJavac`：通过
- `.\gradlew.bat :app:testDebugUnitTest`：通过
- `.\gradlew.bat :app:lintDebug`：通过
- `.\gradlew.bat :app:testDebugUnitTest --dependency-verification=strict`：通过
- `Get-ChildItem app/src/main/java -Recurse -Include *.kt,*.java | Select-String -SimpleMatch ".printStackTrace("`：无输出
- `git status --short`：干净

## 完成记录

- Base SHA: 57a1e50988ce404cdc1453e5710ac49fd842eda4
- Implementation SHA: df4faaf87649c9f866a26ce8d181e3b8e1e02750
- Python test count: 624
- JVM test result: 全部通过
- Baseline: 保持为空
