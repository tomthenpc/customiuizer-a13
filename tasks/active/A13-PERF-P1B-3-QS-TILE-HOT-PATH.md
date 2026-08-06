# A13-PERF-P1B-3 — Quick Settings Tile Creation Hot-Path Reduction

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-3-QS-TILE-HOT-PATH` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `5f780b8a15727114bd29f01188191a2520ff2509` |
| 状态 | `COMPLETED` |
| 终点 commit | 待提交后填入 |
| P0 真实运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |
| 授权范围 | 仅 Quick Settings Tile 创建路径：`tileHostCls` 构造器 Hook、`FactoryImpl#createTileInternal` Hook 及二者直接使用的模块 Tile 创建辅助逻辑 |

## 目标

在不改变 MIUI 14 / HyperOS 1 Android 13 的现有 Quick Settings Tile 行为、顺序、默认值、异常回退、ROM 分支和偏好生效语义的前提下，减少 Tile 创建回调中的重复稳定反射查询、重复偏好解析和临时对象分配。

## 授权修改范围

- 回调期间重复的稳定反射查询（`findClass`、`findField`、`findMethod`）。
- 回调期间重复的偏好解析或临时集合构造。
- 构造器回调中无意义重复的模块初始化。
- `createTileInternal` 中对稳定 Factory / Host / Tile 接口的重复解析。
- 稳定参数签名、不可变 Tile spec 判断条件的外提。

## 禁止范围

- 不处理 `SystemUIApplication#onCreate`、通知、锁屏、状态栏、音量面板。
- 不处理第三方应用进程。
- 不合并其他重复 Hook。
- 不修改 UI 设置页面、签名或构建配置。
- 不新增 Tile 功能、缓存框架、线程、listener、observer 或依赖。
- 不支持 Android 14 或 HyperOS 2。

## 缓存与状态约束

- 只允许缓存稳定反射元数据（Class / Method / Field / Constructor）和不可变常量。
- 禁止缓存 TileHost、FactoryImpl、QSTile、Context、Handler、View、Activity 等短生命周期 Android owner。
- 禁止无上限 Map / List / ThreadLocal / 对象池 / 全局状态。
- 不持有跨实例的 Tile 实例集合。

## 验收标准

- 稳定 Tile 反射元数据不在每次 Tile 创建时重复解析。
- 动态 Tile Class 没有被错误静态化。
- 未缓存任何 Tile、Host、Factory、Context 实例。
- 未新增无上限缓存、线程、Handler、listener、observer。
- Hook call site 不增加，进程覆盖范围不扩大。
- Tile 默认和异常回退行为保持。
- MIUI 14 / HyperOS 1 A13 兼容逻辑保持。
- 新增/扩展 JVM 单元测试覆盖指定场景。
- 更新 `a13_hook_cost_scan.py` 回归门禁与审计文档。
- 全部 Python 测试、Android 编译测试、lint、R8、正式 Release 签名通过。
- 工作区干净并推送。
- 不声明未经真机测量的性能收益。

## 实现摘要

1. 在 `SystemUILockScreenHooks.SecureQSTilesHook` 安装阶段一次性解析 `QSTileHost`、`QSTileImpl`、`Dependency`、`KeyguardViewMediator`、`CentralSurfaces`、`ControlCenterControllerImpl`、`ControlPanelController`、`MiuiQSFactory` 类与相关 `Field`/`Method`。
2. `QSTileHost` 构造器 `after` 回调改用缓存的 `mContext`/`mTiles` `Field`，避免 `getObjectField`。
3. `mAfterUnlockReceiver.onReceive` 改用缓存的 `Field.get()`/`Method.invoke()`；唯一保留的动态反射是 `findMethodExact(tile.javaClass, "handleClick", View::class.java)`。
4. `createTileInternal` `after` 回调删除 `HashSet` 构造，改为 `isSecureTile(name)` 单次 `mPrefs.getBoolean` 判断，并使用 `when` 归一化 tile spec 名称。
5. `tileHook.before` 回调删除所有 `findClass`/`findClassIfExists` 调用，使用缓存的 `Method`/`Field`；`Handler(mContext.mainLooper).post { ... }` 内保留必要运行时 `Intent` 与 `Runnable`。
6. `securedTiles` 改为 `ArrayList<String>()`。

## 新增/修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/SecureQSTilesHookTest.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/FakeContext.kt`
- `app/src/test/java/android/content/FakeIntent.kt`
- `app/src/test/java/com/android/systemui/qs/QSTileHost.kt`
- `app/src/test/java/com/android/systemui/qs/tiles/FakeWifiTile.kt`
- `app/src/test/java/com/android/systemui/qs/tiles/FakeNfcTile.kt`
- `app/src/test/java/com/android/systemui/qs/MiuiQSFactory.kt`
- `app/src/test/java/com/android/systemui/qs/QSTileImpl.kt`
- `app/src/test/java/com/android/systemui/Dependency.kt`
- `app/src/test/java/com/android/systemui/controlcenter/policy/ControlCenterControllerImpl.kt`
- `app/src/test/java/com/android/systemui/keyguard/KeyguardViewMediator.kt`
- `app/src/test/java/com/android/systemui/statusbar/phone/CentralSurfaces.kt`
- `app/src/test/java/android/app/FakeKeyguardManager.kt`
- `docs/audit/A13_QS_TILE_HOT_PATH.md`
- `docs/audit/A13_HOOK_COST_MAP.md`
- `docs/audit/A13_HOOK_COST_MAP.json`
- `docs/audit/A13_LEGACY_EXCEPTION_REGISTRY.json`
- `tools/a13_hook_cost_scan.py`
- `tools/tests/test_launcher_gesture_state_cache.py`

## 验证结果

- `python tools/verify.py fast --changed` OK
- `python tools/verify.py full` OK
- `python -m compileall tools` OK
- `python -m unittest discover -s tools/tests -p "test_*.py"` OK (944 tests, 2 skipped)
- `git diff --check` OK
- `tools/a13_hook_cost_scan.py` OK (669 records, P1B-3 regression checks pass)
- `tools/a13_hook_cost_scan.py --verify-stability` OK
- `.\gradlew.bat :app:compileDebugKotlin` OK
- `.\gradlew.bat :app:compileDebugJavaWithJavac` OK
- `.\gradlew.bat :app:testDebugUnitTest --tests "tv.withaibuild.customiuizer.mods.SecureQSTilesHookTest"` OK
- `.\gradlew.bat :app:lintDebug` OK
- `.\gradlew.bat :app:assembleDebug` OK
- 正式 Release 签名：当前环境无 A13 外部签名配置（`keystorePropertiesPath` 未设置），未执行；待用户提供签名配置或 CI 执行。

## 风险与未完成项

1. **正式 Release 签名**：需要在仓库外配置 `customiuizerA13KeystoreProperties` 或 `CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES` 后，由授权环境运行 `gradlew :app:assembleRelease` 或 CI 完成。
2. **实机验证**：P0 真实运行时基线仍为 `RUNTIME_BASELINE_PENDING_DEVICE`，所有性能收益数字均为静态推断，不声称具体 KB/MB/百分比/延迟/耗电变化。
3. **R8**：已通过 Debug 构建与 lint；Release R8 优化需正式打包后验证。
