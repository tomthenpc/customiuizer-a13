# PERF-A13-LAUNCHER-GESTURE-STATE-CACHE — Completed

## 元数据

- Base SHA: `3f0ac11bd35da3ee2254dd06793d617e4e167ab6`
- Implementation SHA: `3c9f6453d5c4ac1322e6c0748b88663794e38859`
- Branch: `devin/a13-rom-intelligence-audit`

## 实现要点

- 在 `LauncherGestureHooks` 内新增 `HotSeatGestureState`：
  - 保存 `densityDpi`、`minDistance`、`velocityThreshold`、`touchSlop` 三个阈值。
  - 保存 `downX`、`downY`、`downTime` 实例状态。
  - `updateThresholdsIfNeeded` 只在 `densityDpi` 变化时重新计算阈值，否则返回 false。
- 删除 object 级 `mHotSeatDownX`、`mHotSeatDownY`、`mHotSeatDownTime`。
- 新增 `hotSeatGestureState(hotSeat: ViewGroup)` helper，使用 `XposedHelpers` 的 additional instance field 为每个 HotSeats View 实例缓存一个状态对象。
- `HotSeats.dispatchTouchEvent`：
  - 稳态触摸事件只比较 `state.densityDpi` 与 `configuration.densityDpi`，只有变化时才读取 `displayMetrics.density` 并调用 `ViewConfiguration.get`。
  - `ACTION_DOWN` / `ACTION_UP` 使用 `state` 的 down 坐标与时间。
  - 保留 `dt == 0L` 防护、左右 action key、`velocity = abs(dx) * 1000 / dt`。
- `FSGesturesHook`：
  - 安装阶段只调用一次 `XposedHelpers.findClass` 解析 `BaseRecentsImpl`。
  - 三个 callback（`createAndAddNavStubView`、`updateFsgWindowState`、`getGlobalBoolean`）使用捕获的 `baseRecentsClass`。
  - 保留 `Thread.currentThread().stackTrace` 和 `el.className == "com.miui.home.recents.BaseRecentsImpl"` 的调用范围合同。
- 同步 `docs/audit/A13_LEGACY_EXCEPTION_REGISTRY.json` 的 `inputDigest`（`LauncherGestureHooks` 行号变化导致）。

## 新增/更新测试

- `app/src/test/java/tv/withaibuild/customiuizer/mods/LauncherHotSeatGestureStateTest.kt`：12 项，覆盖阈值计算、gate、实例独立性、字段类型约束。
- `tools/tests/test_launcher_gesture_state_cache.py`：32 项静态合同测试，覆盖字段删除、状态缓存、ViewConfiguration 调用位置、BaseRecentsImpl 缓存、stack scope 保持、范围保护。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `python -m compileall tools` | 通过 |
| `python -m unittest discover -s tools/tests -p "test_*.py"` | 902 通过，2 skipped |
| `tools/tests/test_launcher_gesture_state_cache.py` | 32/32 通过 |
| `python tools/verify.py fast --tests LauncherHotSeatGestureStateTest` | 通过 |
| `gradlew :app:compileDebugKotlin` | 通过 |
| `gradlew :app:compileDebugJavaWithJavac` | 通过 |
| `gradlew :app:testDebugUnitTest` | 通过 |
| `gradlew :app:lintDebug` | 通过 |
| `gradlew :app:testDebugUnitTest --dependency-verification=strict` | 通过 |
| `python tools/source_hazard_scan.py --path app/src/main/java` | `0 reviewed, 0 new` |
| 生产源码 `.printStackTrace(` 搜索 | 无输出 |
| `git diff --check` | 无报错 |
| `Get-ChildItem ... -Include *.apk` | 无新增 APK |

## Mutation 验证

所有 mutation 均临时引入、触发对应测试失败、并恢复，未进入提交：

1. 恢复 object 级 DOWN 字段 → `test_object_level_down_fields_removed` 失败。
2. 每次事件新建 `HotSeatGestureState()` → `test_dispatch_uses_helper` 失败。
3. 每次事件调用 `ViewConfiguration.get` → `test_view_configuration_only_on_density_change` 失败。
4. 删除 `densityDpi` 更新判断 → `sameDensityDpiReturnsFalse` 失败。
5. 相同 `densityDpi` 覆盖 `touchSlop` → `sameDensityDpiDoesNotOverwriteThresholds` 失败。
6. 距离常数改为 74 → `minDistanceIs75dp` 失败。
7. callback 内恢复重复 `findClass` → `test_fs_gestures_callbacks_use_captured_class` / `test_base_recents_class_parsed_once` 失败。
8. 删除 `Thread.currentThread().stackTrace` → `test_stack_trace_call_preserved` 失败。
9. `force_fsg_nav_bar` 对所有调用全局返回 true → `test_global_force_fsg_not_always_true` 失败。

## 状态

`STATIC_VERIFIED`
