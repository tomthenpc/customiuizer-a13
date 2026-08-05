# FIX-static-context-view-fields

- Platform: A13
- Status: Done
- Priority: P2
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

处理 Lint `StaticFieldLeak` 报告的 12 处静态 Context/View 字段（`Controls.kt`、
`GlobalActions.kt`、`LauncherGestureHooks.kt`、`StepCounterController.kt`、
`SystemUIControlCenterHooks.kt`），消除 Lint 警告，同时不引入行为回归。

## 当前问题

`app/build/reports/lint-results-debug.xml` 中 `StaticFieldLeak` 命中 12 处。逐一核实后：

- `Controls.kt`（`sScreenOnContext`/`sPowerContext`/`sVolumeContext`）、
  `GlobalActions.kt`（`mGlobalReceiverContext`/`mSBReceiverContext`）：均为
  `system_server` 内单例服务（`PhoneWindowManager`/`AccessibilityManagerService`/
  `BaseMiuiPhoneWindowManager`）的 `mContext`，已有"先反注册旧 receiver 再赋值新
  Context"的所有权替换逻辑，不是未管理的悬空引用；Lint 的通用规则无法识别这种
  进程内单例语义。
- `StepCounterController.kt`（`sContext`）：已经使用 `context.applicationContext`
  而非 Activity Context，并有显式 `destroy()` 清空引用、`stepViews` 用
  `WeakReference` 持有 View；已经是正确的所有权/释放闭环。
- `SystemUIControlCenterHooks.kt`（`mPct`）：已有 `initPct()`/`removePct()` 的
  创建-挂载-卸载-置空闭环，字段本身已带 `@SuppressLint("StaticFieldLeak")`；
  Lint 对 Kotlin `object` 会在类声明处额外发一条"该类含有指向 Context/View 的
  静态字段"报告，字段级注解无法覆盖这条。
- `LauncherGestureHooks.kt`（`mHotSeatContext`）：**真实可优化点**——该字段只是
  为了在同一次 hotseat 手势的 `ACTION_DOWN` 和 `ACTION_UP` 之间传递 Context，
  但 `before()` 在两次回调中都会从 `hotSeat.context` 重新计算出同一个
  `helperContext`，因此这个静态字段是完全冗余的，可以直接删除。

## 允许修改

- `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`

## 必须保持

- Hook 时序、参数语义、receiver 注册/反注册顺序不变；
- `StepCounterController` 的 `applicationContext` + `destroy()` 释放闭环不变；
- `SystemUIControlCenterHooks` 的 `initPct`/`removePct` 创建卸载闭环不变；
- 不引入新的静态强引用。

## 实现要求

1. `LauncherGestureHooks.kt`：删除 `mHotSeatContext` 字段，`ACTION_UP` 分支直接
   使用当前回调里的 `helperContext`。
2. `Controls.kt`、`GlobalActions.kt`、`StepCounterController.kt`：在对应字段上
   补充 `@SuppressLint("StaticFieldLeak")` 并加注释说明是进程内单例
   Context/`applicationContext`，已有所有权替换或释放闭环，不是遗漏。
3. `SystemUIControlCenterHooks.kt`：把 `@SuppressLint("StaticFieldLeak")` 从字段
   移到 `object SystemUIControlCenterHooks` 声明上，覆盖类级报告。

## 非目标

- 不重构 Hook 架构或 receiver 生命周期模型；
- 不处理本次未涉及的其它 Lint 类别（`UnusedResources`、`DiscouragedApi` 等）。

## 验收标准

- [x] `app/build/reports/lint-results-debug.xml` 中 `StaticFieldLeak` 命中数为 0
- [x] `compileDebugKotlin`/`compileDebugJavaWithJavac` 通过
- [x] `testDebugUnitTest` 通过
- [x] `check-invariants.py` 无新增 violation
- [x] 未实机验证内容已明确分级（本任务无行为变化，标记 `STATIC_VERIFIED`）
- [x] 最终 diff 已审查
- [x] 工作区没有未解释改动

## 验证

```powershell
.\gradlew.bat :app:lintDebug :app:compileDebugKotlin :app:compileDebugJavaWithJavac :app:testDebugUnitTest
python tools/check-invariants.py
git diff --check
```

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: 6444325878b51fb8cbc3d4e1cfcf03848729cd97
- Final SHA: 本记录所在的收口 commit（`fix: suppress false-positive StaticFieldLeak ...`）
- Commits: 1（本任务单次收口提交）
- Behavior changed: 否，仅注解与一处冗余字段删除
- Verification: lintDebug / compileDebugKotlin / compileDebugJavaWithJavac / testDebugUnitTest / check-invariants.py
- Device evidence: 无（本任务不涉及行为变化，STATIC_VERIFIED）
- Known limits: `system_server`/SystemUI 单例 Context 复用模式未改为强所有权模型，仅补充说明性抑制
