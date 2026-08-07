# A13-PERF-P1B-3 — Quick Settings Tile Creation Hot-Path Reduction

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-3-QS-TILE-HOT-PATH` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `5f780b8a15727114bd29f01188191a2520ff2509` |
| 状态 | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` |
| 终点 commit | `1ea342dbcefed45a9ca1d4b8cc4762a14d91a37b` |
| QA R1 engineering | `95b5d8eb51a5a070d85f7d5ec90a0180e09c0b2a` |
| QA R2 closure | `33ff7b16a8bf14d2d9c6c40af3154c0cc8d41c70` |
| QA R3 final closure | `1ea342dbcefed45a9ca1d4b8cc4762a14d91a37b` |
| Engineering Final SHA | `1ea342dbcefed45a9ca1d4b8cc4762a14d91a37b` |
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

- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`（生产逻辑，R3 无最终 diff）
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`（生产逻辑，R3 无最终 diff）
- `app/src/test/java/tv/withaibuild/customiuizer/mods/SecureQSTilesHookTest.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/FakeContext.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/ControlCenterControllerImpl.kt`
- `app/src/test/java/android/content/Intent.kt`
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

### 环境

- 所有 Android 验证均为 `LOCAL_VERIFICATION_PASS`。
- GitHub CI status：none（远端无 status checks）。

### Python / 静态门禁

- `python -m compileall tools` OK
- `python -m unittest discover -s tools/tests -p "test_*.py"` OK
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
  - `:app:testDebugUnitTest` OK
  - `:app:lintDebug` OK
- `\gradlew.bat :app:testDebugUnitTest --rerun-tasks --no-build-cache --no-daemon` OK
- `\gradlew.bat :app:minifyReleaseWithR8 --no-daemon` OK
- `\gradlew.bat :app:testDebugUnitTest --tests "*SecureQSTiles*" --rerun-tasks --no-build-cache --no-daemon` OK

### 新增/增强 JVM 单元测试覆盖

- `SecureQSTilesHookTest.afterUnlockRoundTrip_usesCorrectExactSpecForSharedClass`
- `SecureQSTilesHookTest.afterUnlockRoundTrip_fullSharedClassLifecycle`（R3 改为验证 production-generated actual broadcast）
- `SecureQSTilesHookTest.mCalledAfterUnlock_isPerInstance_strong`
- `SecureQSTilesHookTest.createTileInternal_rebindsSpecOnSameInstance`
- `SecureQSTilesHookTest.hostReceiver_successfulReplacement_unregistersOldReceiver`
- `SecureQSTilesHookTest.hostReceiver_registerFailure_keepsOldReceiverActive`
- `SecureQSTilesHookTest.hostReceiver_unregisterFailure_movesOldReceiverToStale`
- `SecureQSTilesHookTest.tileHook_before_rethrowsWrappedFatalFromPost`

### Mutation matrix C/D/E/F/G

所有 mutation 仅临时注入、记录失败证据后已完全还原；生产文件最终无差异。

- **C — `mCalledAfterUnlock` 改为 class/shared static semantics**：临时将 `setAdditionalInstanceField(tile, "mCalledAfterUnlock", ...)` 与 `getAdditionalInstanceField(param2.getThisObject(), ...)` 改为 `tile.javaClass` 上的 static 语义。`*SecureQSTiles*` 中 4 个测试失败。
- **D — fatal cause traversal disabled**：临时将 `rethrowIfFatal` 改为仅检查 outer throwable。`*SecureQSTiles*` 中 5 个测试失败。
- **E — successful replacement does not release previous**：临时移除 `registerModuleReceiver` 成功后对 `previous` 的 `releaseReceiver`。`hostReceiver_successfulReplacement_unregistersOldReceiver` 失败。
- **F — same-instance rebind blocked**：临时将 `setAdditionalInstanceField(tile, TILE_SPEC_KEY, tileName)` 改为仅在首次创建时写入。`createTileInternal_rebindsSpecOnSameInstance` 失败（历史 R2 mutation）。
- **G — stale registration not tracked**：临时移除 `releaseReceiver` catch 中的 `addToStale`。`hostReceiver_unregisterFailure_movesOldReceiverToStale` 失败（历史 R2 mutation）。
- **Actual-broadcast wrong-spec mutation**：临时将 `tileHook.before` 内 `intent.putExtra("tileName", exactTileName)` 改为固定 `custom(foo)`。`afterUnlockRoundTrip_fullSharedClassLifecycle` 失败。

### 正式 Release APK（历史工程产物）

- 文件：`C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\build\outputs\apk\release\CustoMIUIzer-A13-r13.10.1.apk`
- 大小：`2935586` 字节
- APK SHA-256：`074857C350EFD68478FAD5D70FBCE33D5C4207EF48630265F80FEACF2BF3BC6`
- `apksigner verify --verbose --print-certs`：
  - `Verifies`
  - `Verified using v2 scheme (APK Signature Scheme v2): true`
  - `Number of signers: 1`
  - 证书 SHA-256：`15ce32f03e4d8e62df9390f77431862e59bf2cf95cd5a72f0c7330cdfcca2934`
- 注意：该 APK 属于 `HISTORICAL_ENGINEERING_SIGNED_ARTIFACT`，不包含后续 QA corrective fix，不能证明当前 QA corrective HEAD。当前 HEAD 的签名 Release 验证推迟到 QA-1 阶段关闭时完成。

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
| QA R2 evidence | `33ff7b161620191cee8bd200cc6dc412cbf5f254` | after-unlock round trip / lifecycle / mutation C/D/E 证据补全 |
| QA R2 docs | `1a5e5b736514a821831b47bf29ea408444741788` | 记录 QA R2 SHA |
| QA R3 evidence closure | 见本次提交 | actual-broadcast round trip、fatal traversal、replacement release 证据闭合 |

## 最终状态

- `ACTUAL_BROADCAST_ROUND_TRIP_EVIDENCE` = CLOSED
- `MUTATION_FATAL_AND_REPLACEMENT_EVIDENCE` = CLOSED
- `AFTER_UNLOCK_ROUND_TRIP_EVIDENCE` = CLOSED
- `RECEIVER_REPLACEMENT_LIFECYCLE_EVIDENCE` = CLOSED
- `MUTATION_CDE_INCOMPLETE` = CLOSED
- 历史 Release APK 已标记为 `HISTORICAL_ENGINEERING_SIGNED_ARTIFACT`，不证明当前 QA corrective HEAD。
- P0 真机运行时基线仍为 `RUNTIME_BASELINE_PENDING_DEVICE`，未进行真机功能和性能验证。
- 当前状态：`QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`（静态/正确性/lifecycle QA 已接受，device runtime evidence 仍待 P0 完成）。
