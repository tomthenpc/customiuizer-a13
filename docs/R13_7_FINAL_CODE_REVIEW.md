# R13.7 最终代码审查

## 审查范围

- 基线：`505c97e5d12b237a7022f77c0392a9771cff6cb6`
- 静态候选：`31d60bafb6304e05f7a728ee954cf4fe1ab93af4`
- 审查分支：`codex/r13.7-final-audit`
- 原阶段提交：26 个
- 原候选实际变更：65 个路径，4481 行新增、2579 行删除
- 审查方法：逐提交与合并 diff、迁移前 Java 控制流、生命周期/所有权、异常边界、线程与队列、R8/反射入口、测试与静态门禁交叉检查


## 严重度与修复

| 严重度 | 数量 | 结论 |
|---|---:|---|
| P0 | 0 | 未发现会直接造成系统不可启动、数据破坏或签名边界失效的问题 |
| P1 | 4 | owner 回调可能被 GC 静默失效；Launcher 重命名丢失 Java `break`；DeviceInfo 固定槽位可被动态主开关破坏；AudioVisualizer observer 强持有已 detach View |
| P2 | 3 | deferred callback 失败日志可重复刷屏；Palette 队列未严格限为 latest-wins；专辑图用 `identityHashCode` 作为来源唯一标识 |

对应最小修复：

| 提交 | 修复 |
|---|---|
| `9e17e54` | owner-aware 偏好回调由注册表强持有 callback、弱持有 owner，调用点只使用解引用后的 owner |
| `a642994` | owner-aware 注册回归测试 |
| `eef4fc8` | 恢复 Launcher.java 的 `continue` / 命中后 `break`，并加入静态门禁 |
| `6859029` | 同名 deferred callback 异常每进程只记录一次 |
| `a1ca520` | DeviceInfoMonitor 使用 Hook 安装时的固定主开关，动态更新只作用于可运行时变更的子配置 |
| `aae21d7` | AudioVisualizer observer 随 attach/detach 注册注销，Palette 使用单槽队列并在取消时清理 |
| `eb03775` | 专辑图来源改用单调 `Long` token，消除 identity hash 碰撞 |

未改变 Hook target、priority、注册顺序、偏好 key、applicationId、ABI、Xposed metadata、API 101/102 边界或 Android 13 行为基线。

## Java → Kotlin 控制流复核

本轮修改的迁移文件已按 `AGENTS.md` 对照迁移前 Java：

- `LauncherIconHooks.kt` ← `Launcher.java`：发现唯一真实迁移回归。原 Java 对非应用 `continue`，唯一 key 命中更新后 `break`；Kotlin `forEach` 无法非局部跳出。已由 `eef4fc8` 恢复显式 `for`。
- `SystemDisplayAndWindowHooks.kt`、`SystemNotificationMoreHooks.kt`、`SystemNotificationPopupsHooks.kt` ← `System.java`：本轮只更换 owner 取得方式，未修改循环控制、Hook 顺序或参数。
- `SystemUIStatusBarHooks.kt` ← `SystemUI.java`：本轮只向 controller 传递安装时布尔值；原 Java 循环 `break/continue` 区域未触及。
- `AudioVisualizer.kt` ← `AudioVisualizer.java`：原 `switch` 的 `break` 在 Kotlin `when` 中自然消失；原 animator 循环已被 K15 单帧调度器替代，持续时间由每帧读取 `animDur`，本轮未引入循环控制差异。

## 逐文件审查记录

表中“回滚提交”指原 K11–K17 阶段提交；K18 修复提交另列在结论中。

### 跨阶段入口与 K11

| 文件 | 阶段 | 风险 | 控制流 | 生命周期 | 异常边界 | 线程 | 所有权 | 回滚提交 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| `app/src/main/java/tv/withaibuild/customiuizer/MainModule.java` | K11/K17 | 高 | Hook 条件、顺序和参数逐项核对 | 进程入口一次初始化 | 单项 Hook 安全失败边界保留 | 无新增线程 | 领域对象仅静态无状态入口 | `18d74a1`–`7c1b146` | 119 个直接 System 调用；顺序与基线一致，无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt` | K11 | 中 | 偏好通知路径不重复派发 | Fragment 重建沿既有路径 | 设置进程异常边界不影响系统进程 | 主线程 | Fragment 自有 | `e30294f` | 无 owner 反向强持有，无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt` | K11 | 高 | guarded 内原副作用只执行一次 | Receiver 使用稳定注册 | deferred callback 已隔离 | Handler/回调线程不变 | 注册 key 含功能域 | `c5ea08b`,`e30294f` | 无重复副作用或 key 冲突 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt` | K11 | 高 | 返回值 fallback 按“不消费”语义 | Receiver/Handler 注销路径保留 | 框架回调已 guarded | 原线程模型 | owner 与注册表一致 | `c5ea08b`,`e30294f` | 无新增问题 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt` | K11/K17 | 高 | 直接分派签名不变 | 无新增长期资源 | deferred lambda 已 guarded | Launcher 主线程 | View/owner 范围不变 | `c5ea08b`,`7c1b146` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt` | K11/K17 | 高 | Hook 顺序和触摸返回值不变 | Listener 随原 owner | 回调外层隔离 | Launcher UI 线程 | 无静态 View | `c5ea08b`,`7c1b146` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt` | K11/K17 | 高 | Java `continue/break` 对照发现回归 | Launcher owner 弱持有 | observer 内 try 保留 | UI 更新仍 `runOnUiThread` | callback 不捕获 owner/param | `e30294f`,`7c1b146` | `9e17e54` 修 owner；`eef4fc8` 恢复命中后退出 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt` | K11/K17 | 高 | 直接分派参数不变 | 无新增注册 | Hook 内边界不变 | 原线程 | 原 owner | `b91f68f`,`7c1b146` | 热路径参数零复制，无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt` | K11/K17 | 高 | proceed 次数与参数不变 | Receiver 注册稳定 | deferred callback 已隔离 | 原 Handler | key 含 Launcher 域 | `c5ea08b`,`e30294f`,`7c1b146` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt` | K11/K17 | 高 | 直接调用顺序不变 | Receiver 所有权明确 | callback guarded | 原线程 | 模块注册表 | `c5ea08b`,`18d74a1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt` | K11/K17 | 高 | Hook target/字段更新语义不变 | observer 随 controller owner | preference dispatch 隔离 | 原 SystemUI 线程 | callback 只使用解引用 owner | `e30294f`,`18d74a1` | `9e17e54` 修匿名 observer 可被 GC 问题 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt` | K11/K17 | 高 | 参数只读路径不复制 | 无新增长期资源 | guarded 不重复副作用 | 原线程 | 原 owner | `b91f68f`,`18d74a1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt` | K11/K17 | 高 | Receiver/回调分支保持 | 注册/注销幂等 | deferred callback 已 guarded | 原 Handler | stable key 含功能域 | `c5ea08b`,`e30294f`,`18d74a1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt` | K11/K17 | 高 | 震动、壁纸偏好更新语义不变 | observer 随服务/控制器 owner | dispatch 内逐 observer 隔离 | 原 Binder/SystemUI 线程 | callback 不捕获 Hook param | `e30294f`,`18d74a1` | `9e17e54` 修 owner 回调存活 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationPopupsHooks.kt` | K11/K17 | 高 | heads-up delay 计算不变 | observer 随 owner | dispatch 隔离 | 原线程 | 弱 owner、强 callback | `e30294f`,`18d74a1` | `9e17e54` 修 owner 回调存活 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt` | K11/K17 | 高 | getArg 替代数组复制，不改参数 | 无新增资源 | Hook 边界保留 | 热路径 | 无新 owner | `b91f68f`,`18d74a1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt` | K11/K17 | 高 | Hook 返回/early return 不变 | Listener 路径保留 | deferred callback 已 guarded | UI/Handler 不变 | 注册 key 独立 | `c5ea08b`,`e30294f`,`18d74a1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt` | K11/K17 | 高 | 参数读取零复制 | 无新增资源 | callback guarded | 热路径 | 原 owner | `c5ea08b`,`b91f68f`,`18d74a1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt` | K11/K17 | 高 | 分派与偏好条件不变 | View 弱引用治理不变 | deferred callback 隔离 | SystemUI 主线程 | 无静态强 View | `c5ea08b`,`b538d55` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt` | K11/K17 | 高 | volume blur stable key 替换旧回调 | 进程级唯一回调 | observer 自身 catch | SystemUI 主线程 | callback 只写静态数值 | `e30294f`,`b538d55` | 重复 init 由相同 key 替换，无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt` | K11/K16/K17 | 高 | 锁屏 Hook 顺序不变 | controller owner attach/detach | deferred callback guarded | 主线程仅提交/回写 | controller 弱持有 View/Context | `c5ea08b`,`c0a2c36`,`b538d55` | 与 K16 controller 交叉核对，无额外修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt` | K11/K17 | 高 | 回调返回语义不变 | Receiver/observer owner 明确 | guarded | 原 Handler | stable key 分域 | `c5ea08b`,`e30294f`,`b538d55` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIScreenshotHooks.kt` | K11/K17 | 高 | 截图副作用仅一次 | Receiver 生命周期不扩张 | deferred callback guarded | 原线程 | 模块注册表 | `c5ea08b`,`e30294f`,`b538d55` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt` | K11/K12/K17 | 高 | 直接分派与参数保持 | DeviceInfo owner 交由 controller | Hook/Handler 边界保留 | SystemUI 主/工作线程分离 | 状态归 controller | `b91f68f`,`441b79f`,`b538d55` | `a1ca520` 传入固定槽位开关 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt` | K11 | 高 | getArg 替代复制，proceed 不变 | 无新增资源 | Hook 边界保留 | 热路径 | 无新 owner | `b91f68f` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java` | K11 | 最高 | guarded 正常路径一次、catch 不重放 | 注册替换/注销幂等 | 每 callback 隔离 | 并发注册表 | 弱 owner；callback 不反向捕获 | `c5ea08b`,`e30294f` | `9e17e54` 修 observer 存活；`6859029` 限流失败日志 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java` | K11 | 最高 | 反射缓存不改调用结果 | ClassLoader 弱键/失效边界核对 | miss/throw 语义保持 | 并发缓存 | 不持有临时 Context/View | `b91f68f` | 分配测试通过，无修复 |
| `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperGuardedTest.kt` | K11/K18 | 低 | 覆盖一次执行、fallback、隔离 | 无运行时资源 | 覆盖异常边界 | JVM 单测 | 测试局部 | `4c6a3e0` | K18 增加重复失败只记一次 |
| `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperRegistrationTest.kt` | K11/K18 | 低 | 覆盖同 key 不同 owner | 覆盖替换/清理 | 无 | JVM 单测 | 验证 callback/owner 结构 | `4c6a3e0` | `a642994` 增加 owner-aware 回归 |
| `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ReflectionCacheAllocationTest.kt` | K11 | 低 | 反射调用结果一致 | 缓存可清理 | 覆盖 miss | JVM 多线程 | ClassLoader 范围 | `4c6a3e0` | 通过 |
| `docs/K11A_CALLBACK_SAFETY.md` | K11 | 低 | 文档与实现对照 | 记录所有权边界 | 记录 guarded 范围 | 记录线程边界 | 记录 stable key | `35d2524` | 需结合本 K18 修复阅读 |

### K12 设备监控

| 文件 | 阶段 | 风险 | 控制流 | 生命周期 | 异常边界 | 线程 | 所有权 | 回滚提交 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt` | K12 | 最高 | start/stop generation、退避和亮屏单刷核对 | stop 清 handler/receiver；旧 generation 失效 | sysfs 单项失败不停止 controller | 主 Handler + 工作 Handler | application Context、无静态 View | `441b79f`,`aaaf96a` | `a1ca520` 修固定槽位主开关；待实机 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt` | K12 | 高 | 仅真实事件刷新 | screen receiver 配对注销 | 回调 guarded | Handler | controller 自有 | `aaaf96a` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt` | K12 | 高 | 关闭不启动 ticker | observer 字段随 attach/detach 注册注销 | preference dispatch 隔离 | UI Handler | View 自有 observer | `aaaf96a` | 无修复 |
| `app/src/test/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooksDeviceMonitorTest.kt` | K12/K18 | 低 | 覆盖偏好模式与固定主开关 | 无 | 无 | JVM | 测试局部 | `f7b258d` | K18 增加动态偏好不得改槽位断言 |
| `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitorLifecycleTest.kt` | K12 | 低 | 覆盖 generation/backoff | 覆盖屏幕状态 | 覆盖失败恢复 | JVM | 测试局部 | `f7b258d` | 通过 |

### K13 设置列表与图标

| 文件 | 阶段 | 风险 | 控制流 | 生命周期 | 异常边界 | 线程 | 所有权 | 回滚提交 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| `app/src/main/java/tv/withaibuild/customiuizer/MainApplication.kt` | K13/K14 | 中 | trim/locale 启动顺序不变 | application 级缓存 | 单项清理安全 | 主线程入口 | application owner | `1fad51c`,`7a6d519` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/AppData.kt` | K13 | 中 | cache key 含用户/包更新信息 | 不持有 Activity | 无新增异常吞没 | 数据对象 | 值所有权 | `1fad51c` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/AppDataAdapter.kt` | K13 | 高 | 主线程原子替换列表，position 绑定时重取 | Adapter/ViewHolder 不被 worker 持有 | 拒绝/取消不留状态 | 后台加载、主线程提交 | adapter 自有列表 | `aa8b321` | 无 copy-on-write 回归 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/AppHelper.kt` | K13 | 中 | 包更新失效路径核对 | application 级 | 异常边界保持 | 后台允许 | 不持有 Activity | `1fad51c` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/BitmapCachedLoader.kt` | K13 | 高 | in-flight 完成/失败/拒绝均清理 | 等待者弱引用 | loader 失败逐项隔离 | 有界 executor | 字节 LRU、弱 ImageView | `1fad51c` | 无 DiscardOldest、无 Runtime.gc；无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/Helpers.kt` | K13 | 中 | 图标调用语义不变 | 无新增 owner | 原边界 | 原线程 | application Context | `1fad51c` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/LockedAppAdapter.kt` | K13 | 中 | Adapter 迁移到共享 loader | View 复用检查 key | loader 隔离 | 主线程绑定 | 弱等待者 | `1fad51c` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/PrivacyAppAdapter.kt` | K13 | 中 | 同上 | 同上 | 同上 | 同上 | 同上 | `1fad51c` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/ResolveInfoAdapter.kt` | K13 | 中 | 同上 | 同上 | 同上 | 同上 | 同上 | `1fad51c` | 无修复 |
| `app/src/test/java/tv/withaibuild/customiuizer/utils/BitmapCachedLoaderTest.kt` | K13 | 低 | 覆盖去重、拒绝、复用 | 覆盖弱等待者清理 | 覆盖失败路径 | JVM executor | 测试局部 | `0428bc1` | 通过 |

### K14 Locale 与搜索

| 文件 | 阶段 | 风险 | 控制流 | 生命周期 | 异常边界 | 线程 | 所有权 | 回滚提交 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| `app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt` | K14 | 高 | 语言应用与重建顺序核对 | Activity 不被单例持有 | LocaleManager 失败可见 | 主线程 | Activity 临时 owner | `7a6d519` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt` | K14 | 高 | 搜索返回刷新开关状态 | Fragment generation | 搜索失败不污染新结果 | worker 计算、主线程提交 | Fragment owner | `8ecdbc1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/AppLocaleController.kt` | K14 | 高 | auto 快路径、明确语言调和 | application locale 唯一路径 | 无 Activity 时不静默丢失 | LocaleManager 调用主线程 | 不持有 Activity | `7a6d519` | Android 13 路径正确，无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/ModData.kt` | K14 | 中 | 预计算搜索字段不改排序语义 | 不持有 UI | 无 | 不可变读取 | 值所有权 | `8ecdbc1` | 无修复 |
| `app/src/main/java/tv/withaibuild/customiuizer/utils/ModSearchAdapter.kt` | K14 | 高 | generation 阻止旧结果覆盖 | Adapter 列表主线程替换 | 失败不提交陈旧状态 | 后台搜索、主线程展示 | Adapter owner | `8ecdbc1` | 高亮/路由不变 |
| `app/src/test/java/tv/withaibuild/customiuizer/utils/AppLocaleControllerTest.kt` | K14 | 低 | 覆盖 auto/明确语言 | 无 | 覆盖服务调用条件 | JVM | 测试局部 | `33fe9db` | 通过 |
| `app/src/test/java/tv/withaibuild/customiuizer/utils/ModSearchIndexTest.kt` | K14 | 低 | 覆盖 generation/排序 | 无 | 无 | JVM | 测试局部 | `33fe9db` | 通过 |

### K15 AudioVisualizer

| 文件 | 阶段 | 风险 | 控制流 | 生命周期 | 异常边界 | 线程 | 所有权 | 回滚提交 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt` | K15 | 最高 | 单 Choreographer 帧、单帧一次 invalidate | detach 停帧、注销 observer、释放 Visualizer | worker/post 回调 guarded | UI 帧 + 两个单 worker | View 自有 observer/executor；无全局强 View | `6b3c503`,`b9b1991` | `aae21d7` 修 observer 泄漏并严格限制 Palette 队列；待实机 |
| `app/src/test/java/tv/withaibuild/customiuizer/utils/AudioVisualizerLifecycleTest.kt` | K15 | 低 | 覆盖帧 gate 与 latest generation | 覆盖显示条件 | 无 | JVM | 测试局部 | `727a39a` | 通过 |

### K16 锁屏专辑图

| 文件 | 阶段 | 风险 | 控制流 | 生命周期 | 异常边界 | 线程 | 所有权 | 回滚提交 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/AlbumArtPolicy.kt` | K16 | 高 | cache key 含全部视觉参数 | 值对象 | 尺寸/字节计算有界 | worker 使用 | 不持有 Bitmap | `11a15dc` | `eb03775` 将来源标识改为 Long token |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt` | K16 | 最高 | Future + generation 双重 latest-wins | owner detach/熄屏取消，弱 View/Context | OOM/拒绝/取消边界 | 有界单 worker；主线程仅回写 | controller 管理缓存和 listener | `c0a2c36`,`11a15dc` | `eb03775` 消除 identity hash 碰撞；待实机 |
| `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/AlbumArtPolicyTest.kt` | K16/K18 | 低 | 覆盖 key 与缓存尺寸 | 无 | 覆盖溢出边界 | JVM | 测试局部 | `37b9f20` | K18 更新 source token 断言 |

### K17 facade、构建与交接

| 文件 | 阶段 | 风险 | 控制流 | 生命周期 | 异常边界 | 线程 | 所有权 | 回滚提交 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt` | K17 删除 | 高 | MainModule 改为领域对象直调 | facade 无状态 | 无隐藏异常 | 无 | 无复制实现 | `7c1b146` | 文件不存在，残留调用 0 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt` | K17 删除 | 最高 | 124 个入口机械映射 | facade 无状态 | 无隐藏异常 | 无 | 无复制实现 | `18d74a1` | 文件不存在，审计 124/124 |
| `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUI.kt` | K17 删除 | 最高 | 有序调用改接收者，参数不变 | `newStyle` 搬到唯一领域 owner | 无隐藏异常 | 无新线程 | 无重复状态 | `b538d55` | 文件不存在，残留调用 0 |
| `tools/check-invariants.py` | K11/K18 | 中 | 静态规则不改运行时 | 无 | 违规明确失败 | 构建机 | 工具局部 | `4c6a3e0` | K18 加 Launcher 循环退出门禁 |
| `gradlew` | 构建 | 高 | 内容不变，仅 executable mode | 无 | CI 可执行 | CI | 仓库工具 | `caad0d2` | mode `100755`，无源码语义变化 |
| `docs/R13_7_OPTIMIZATION_HANDOFF.md` | 交接 | 低 | 记录阶段顺序 | 记录待实机项 | 区分静态/实机 | 无 | 文档 | `6080fa3`,`31d60ba` | CI 旧阻塞已由 K18 修复，实机边界仍有效 |

## 分阶段结论

- K11：callback 正常路径、fallback、stable key、幂等注销通过；发现并修复 owner 回调存活、Launcher 循环退出和日志限流问题。
- K12：ticker、generation、熄屏、退避与所有权静态成立；修复固定槽位主开关语义；仍待实机。
- K13：Adapter 主线程替换、图标 in-flight 清理、弱等待者、有界队列与字节 LRU 静态成立；无需修改。
- K14：Android 13 LocaleManager 单一路径和搜索 generation 静态成立；无需修改。
- K15：单帧调度与 latest-wins 静态成立；修复 observer 生命周期和 Palette 队列；仍待实机。
- K16：有界 worker、取消、generation、缓存参数静态成立；修复来源标识碰撞；仍待实机。
- K17：三个 facade 均不存在；调用顺序、偏好条件、参数、共享状态和 R8/反射入口审计通过。

## 静态与实机边界

本审查只能确认代码和静态门禁。K7、K12、K15、K16 继续标记为“待实机验证”。在完成 [R13_7_DEVICE_ACCEPTANCE_CHECKLIST.md](R13_7_DEVICE_ACCEPTANCE_CHECKLIST.md) 前，不得把本分支称为实机通过，也不授权合并 `main`、创建 tag 或发布 Release。
