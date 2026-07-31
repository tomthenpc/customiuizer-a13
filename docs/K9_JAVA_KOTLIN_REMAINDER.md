# K9 Java → Kotlin 收尾清单

> 本轮不强行迁移 RED 边界与高风险基础设施；仅记录剩余 Java 文件、风险评估与建议批次。

## 剩余 Java 文件

| 文件 | 行数（约） | 风险等级 | 保留/迁移建议 | 备注 |
|---|---|---|---|---|
| `org/apache/commons/lang3/reflect/MemberUtilsX.java` | 30 | 低 | 不迁移 | 反射兼容 shim，Apache 公共代码，A14 同样保留 |
| `tv/withaibuild/customiuizer/MainModule.java` | 820 | 高（RED） | 保留 Java | libxposed 入口、进程分发、反射加载主开关 |
| `tv/withaibuild/customiuizer/mods/utils/HookerClassHelper.java` | 285 | 高（RED） | 保留 Java | API 101 before/after 适配器，已稳定 |
| `tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java` | 385 | 高（RED） | 保留 Java | Hook 注册、资源加载、guard，跨进程调用链入口 |
| `tv/withaibuild/customiuizer/mods/utils/ResourceHooks.java` | 194 | 高（RED） | 保留 Java | 资源替换热路径，R8 / resource shrink 关键 |
| `tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java` | 1821 | 高（RED） | 保留 Java | 反射缓存、附加字段、跨 Hook 调用，已本地化修复 |

## 本轮改动说明

- **不迁移上述 RED 边界文件**；仅对 `XposedHelpers.java` 进行了 A14 `AdditionalField` 身份键修复的安全移植。
- `Various.kt` 中的 `AppsDisableServiceHook` 仅做了回调加固，`MIUI_CORE_APPS` 改为 `Set`，未引入大结构变更。
- `K8.3.1` 中新增/修改的 Kotlin 测试与主代码通过构建与单元测试。

## 后续可选的低风险批次

若后续专门启动 Kotlin 迁移阶段，可按以下顺序：

1. `MemberUtilsX.java`（纯兼容、30 行，可作为 Kotlin 练兵）。
2. `mods/System.java` / `mods/Launcher.java` 拆分后，保留 `XposedHelpers`/`ModuleHelper` 调用语义不变。
3. `ResourceHooks.java` 在实机验证反射入口后再评估迁移。

## 结论

本轮 K9 夜间运行时收口以 A13 代码现状为基线，重点修复运行时稳定性（`XposedHelpers` 附加字段、`Various` 的 `canBeDisabled` hook），未扩展 Kotlin 迁移范围。RED 边界 Java 文件继续保留。
