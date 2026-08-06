# PERF-A13-LAUNCHER-GESTURE-STATE-CACHE

## 目标

降低 Launcher HotSeats `dispatchTouchEvent()` 高频路径的重复工作，并删除 FSG callbacks 中重复的 `BaseRecentsImpl` Class 查找。

## 范围

- 修改 `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- 新增 `app/src/test/java/tv/withaibuild/customiuizer/mods/LauncherHotSeatGestureStateTest.kt`
- 新增 `tools/tests/test_launcher_gesture_state_cache.py`
- 可能新增 completed 文档并在完成后删除本 active 文档

## 禁止

- 创建/切换/合并分支
- 修改 A14、版本号、发布配置、APK/Tag/Release
- 顺带重写其他 Launcher 手势
- 在没有 ROM 调用点证据时删除 FSG stack-scope 合同

## 验收标准

- 删除 object 级 `mHotSeatDownX/Y/Time`
- `HotSeatGestureState` 使用 additional instance field 绑定到 HotSeats View
- 稳态 `dispatchTouchEvent` 不调用 `ViewConfiguration.get` 和 `displayMetrics.density`
- `FSGesturesHook` 回调使用安装阶段捕获的 `BaseRecentsImpl` Class
- JVM 与静态合同测试通过
- `git diff --check` 无报错
- source hazard `0 reviewed, 0 new`
- 无新增 APK
- 状态：`STATIC_VERIFIED`
