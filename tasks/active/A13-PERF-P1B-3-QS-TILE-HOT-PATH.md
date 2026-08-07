# A13-PERF-P1B-3 — Quick Settings Tile Creation Hot-Path Reduction

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-3-QS-TILE-HOT-PATH` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `5f780b8a15727114bd29f01188191a2520ff2509` |
| 状态 | `QA_CONDITIONAL` |
| blocking finding | `AFTER_UNLOCK_ROUND_TRIP_EVIDENCE` — 已关闭 |
| blocking finding | `RECEIVER_REPLACEMENT_LIFECYCLE_EVIDENCE` — 已关闭 |
| blocking finding | `MUTATION_CDE_INCOMPLETE` — 已关闭 |
| Engineering Final SHA | `3cf73a0` |
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
4. `createTileInternal` `after` 回调删除 `HashSet` 构造，改为 `isSecureTile(name)` 单次 `mPrefs.getBoolean` 判断，并使用 `when` 归一化 tile spec 名称；每次创建都通过 `XposedHelpers.setAdditionalInstanceField(tile, TILE_SPEC_KEY, tileName)` 写入实例级 exact spec。
5. `tileHook.before` 回调删除所有 `findClass`/`findClassIfExists` 调用，使用缓存的 `Method`/`Field`；从 `param2.getThisObject()` 读取 `TILE_SPEC_KEY` 与 `mCalledAfterUnlock`，保证 `custom(foo)` / `custom(bar)` 等共享类实例各自独立。
6. `Handler(mContext.mainLooper).post { ... }` 内保留必要运行时 `Intent` 与 `Runnable`。
7. `securedTiles` 改为 `ArrayList<String>()`。

## 新增/修改文件

- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/SecureQSTilesHookTest.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/FakeContext.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/ControlCenterControllerImpl.kt`
- `app/src/test/java/android/os/Handler.kt`
- `app/src/test/java/android/os/Looper.kt`
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

### Python / 静态门禁

- `python -m compileall tools` OK
- `python -m unittest discover -s tools/tests -p "test_*.py"` OK（1043 tests, 2 skipped）
- `python tools/a13_hook_cost_scan.py --verify` OK（Stability check passed）
- `python tools/source_hazard_scan.py` 报告 10 条 `CATCH_THROWABLE_NO_FATAL` 在 `SystemUINotificationHooks.kt`，均为 pre-existing，不属于 P1B-3 范围
- `python tools/verify.py full` OK
- `git diff --check` OK

### Android 构建

- `python tools/verify.py full` 已执行：
  - `check-invariants` OK
  - `check-compat-contracts` OK
  - `check-hook-contract-parity` OK
  - `:app:compileDebugKotlin` OK
  - `:app:compileDebugJavaWithJavac` OK
  - `:app:testDebugUnitTest` OK（1017 tests, 0 failures）
  - `:app:lintDebug` OK
- `\gradlew.bat :app:testDebugUnitTest --rerun-tasks --no-build-cache` OK（1017 tests, 0 failures）
- `\gradlew.bat :app:minifyReleaseWithR8 --no-daemon` OK（BUILD SUCCESSFUL）
- `\gradlew.bat :app:testDebugUnitTest --tests "*SecureQSTiles*" --rerun-tasks --no-build-cache` OK（BUILD SUCCESSFUL）

### 新增 JVM 单元测试覆盖

- `SecureQSTilesHookTest.afterUnlockRoundTrip_usesCorrectExactSpecForSharedClass`
- `SecureQSTilesHookTest.afterUnlockRoundTrip_fullSharedClassLifecycle`
- `SecureQSTilesHookTest.mCalledAfterUnlock_isPerInstance_strong`
- `SecureQSTilesHookTest.createTileInternal_rebindsSpecOnSameInstance`
- `SecureQSTilesHookTest.hostReceiver_successfulReplacement_unregistersOldReceiver`
- `SecureQSTilesHookTest.hostReceiver_registerFailure_keepsOldReceiverActive`
- `SecureQSTilesHookTest.hostReceiver_unregisterFailure_movesOldReceiverToStale`
- `SecureQSTilesHookTest.tileHook_before_rethrowsWrappedFatalFromPost`

### Mutation tests C/D/E

所有 mutation 仅临时注入、记录失败证据后已完全还原；生产文件最终无差异。

- **Mutation C — `mCalledAfterUnlock` 改为 class/shared static semantics**：临时将 `XposedHelpers.setAdditionalInstanceField(tile, "mCalledAfterUnlock", ...)` 与 `getAdditionalInstanceField(param2.getThisObject(), ...)` 改为 `tile.javaClass` 上的 static 语义。`\gradlew.bat :app:testDebugUnitTest --tests "*SecureQSTiles*"` 出现 4 个失败（包括 `afterUnlockRoundTrip_usesCorrectExactSpecForSharedClass`、`afterUnlockRoundTrip_fullSharedClassLifecycle`、`handleClick_before_returnsWhenCalledAfterUnlock`、`createTileInternal_rebindsSpecOnSameInstance`），证明当前测试能够捕获 class-level 共享 flag 回归。已还原。
- **Mutation D — 拒绝在 same instance 上 rebind `TILE_SPEC_KEY`**：临时将 `XposedHelpers.setAdditionalInstanceField(tile, TILE_SPEC_KEY, tileName)` 改为仅在首次创建时写入。`SecureQSTilesHookTest.createTileInternal_rebindsSpecOnSameInstance` 失败，证明 spec rebind 路径有效。已还原。
- **Mutation E — `ModuleHelper.releaseReceiver` 不再将 unregister 失败加入 stale 注册表**：临时移除 `addToStale(...)` 调用。`SecureQSTilesHookTest.hostReceiver_unregisterFailure_movesOldReceiverToStale` 失败，证明 bounded stale 路径有效。已还原。

### 正式 Release APK（历史工程产物）

- 文件：`C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\build\outputs\apk\release\CustoMIUIzer-A13-r13.10.1.apk`
- 大小：`2935586` 字节
- APK SHA-256：`074857C350EFD68478FAD5D70FBCE33D5C4207EF48630265F80FEACF2BF3BC6`
- `apksigner verify --verbose --print-certs`：
  - `Verifies`
  - `Verified using v2 scheme (APK Signature Scheme v2): true`
  - `Number of signers: 1`
  - 证书 SHA-256：`15ce32f03e4d8e62df9390f77431862e59bf2cf95cd5a72f0c7330cdfcca2934`
- 注意：该 APK 属于 `HISTORICAL_ENGINEERING_SIGNED_ARTIFACT`，不包含后续 QA corrective fix（`95b5d8e` 及之后），不能证明当前 QA corrective HEAD。当前 HEAD 的签名 Release 验证推迟到 QA-1 阶段关闭时完成。

## 范围完整性核对

| 入口 | Hook 注册位置 | 回调位置 | 修改前反射 | 修改后反射 | 修改前临时集合 | 修改后临时集合 | 保留动态反射 | 原因 |
|---|---|---|---|---|---|---|---|---|
| `QSTileHost` 构造器 | `ModuleHelper.hookAllConstructors(tileHostClass, hostHook)` | `hostHook.after` | `getObjectField` (反射) | `qTileHostContextField.get` / `ModuleHelper.registerModuleReceiver` | 每次构造新建 `BroadcastReceiver` | 同前（仍新建 Receiver，但 `registerModuleReceiver` 按 key 替换，实际只保留一个活跃实例） | 无 | 无 |
| `FactoryImpl#createTileInternal` | `ModuleHelper.findAndHookMethod(miuiQSFactoryClass, "createTileInternal", String::class.java, ...)` | `after` 回调 | 每次 `HashSet<String>()` + 10 次 `getBoolean` | `isSecureTile(name)` 单次 `getBoolean` | `HashSet`（每个 Tile） | 无 | 无 | 无 |
| `tileHook` `before` | `ModuleHelper.findAndHookMethod(tile.javaClass, "handleClick", ...)` | `before` 回调 | 6 次 `findClass`/`findClassIfExists` | 0 次类查找；使用缓存 `Method.invoke` | 每次点击 `Runnable` + `Intent` | 同前（必要运行时对象） | 无 | 无 |
| `mAfterUnlockReceiver.onReceive` | 在 `hostHook.after` 内创建并注册 | `onReceive` | `getObjectField` + 链式 `findClass`/`callMethod` | 缓存 `Field.get` / `Method.invoke` | `Intent`、`arrayOfNulls` | 同前 | `findMethodExact(tile.javaClass, "handleClick", View::class.java)` | `tile.javaClass` 是运行时具体 Tile 类，无法在安装阶段静态确定；该方法只用于触发 `handleClick`，不缓存按 Tile 实例增长的映射 |

- 未缓存 `QSTileHost`、`MiuiQSFactory`、`QSTile`、`Context`、`Handler`、`View`、`CentralSurfaces` 或 `ControlCenterControllerImpl` 实例。
- 未新增线程、Handler、listener、observer、全局锁或无上限缓存。
- Hook call site 数量与进程覆盖范围未扩大。
- `TILE_SPEC_KEY` 与 `mCalledAfterUnlock` 均为 tile **实例级** additional field，确保 `custom(foo)` 与 `custom(bar)` 共享类但不共享状态。

## QA provenance

| 阶段 | SHA | 说明 |
|------|-----|------|
| Original engineering base | `5f780b8a15727114bd29f01188191a2520ff2509` | P1B-3 原始工程起点 |
| Original engineering final | `3cf73a0` | 原始工程最终（历史记录） |
| Independent QA base | `3744bd95eea2dad35724f5c51aed924b1e70845d` | QA-1 起始基线 |
| QA R1 | `95b5d8e18dd6bc2c08e9639ac0b82610b0721796` | shared-class spec identity 与 wrapped fatal 修复 |
| QA R2 | 见本次提交 | after-unlock round trip / lifecycle / mutation C/D/E 证据补全 |

## 最终状态

- 工程实现与本地验证已完成。
- AFTER_UNLOCK_ROUND_TRIP_EVIDENCE、RECEIVER_REPLACEMENT_LIFECYCLE_EVIDENCE、MUTATION_CDE_INCOMPLETE 三处 QA 条件已关闭。
- 历史 Release APK 已标记为 `HISTORICAL_ENGINEERING_SIGNED_ARTIFACT`，不证明当前 QA corrective HEAD。
- P0 真机运行时基线仍为 `RUNTIME_BASELINE_PENDING_DEVICE`，未进行真机功能和性能验证。
- 当前状态：`QA_CONDITIONAL`（P0 真机运行时基线完成前不晋级为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`）。
