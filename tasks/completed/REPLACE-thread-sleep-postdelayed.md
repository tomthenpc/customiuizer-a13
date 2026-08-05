# REPLACE-thread-sleep-postdelayed

- Platform: A13
- Status: Done
- Priority: P3
- Owner: Devin
- Reviewer: ChatGPT / human
- Related version: optional
- Cross-repo task: optional

## 目标

将 `ActivitySelector` 和 `AppSelector` 中的 `Thread.sleep(animDur)` 等待逻辑改为
`View.postDelayed`，消除 `source_hazard_scan.py` 的 `THREAD_SLEEP` 命中，同时保持
后台加载 App/Activity 列表的行为不变。

## 当前问题

两处 `Thread { Thread.sleep(animDur.toLong()); ... 后台加载 ... }.start()`：

- 会启动并阻塞一个后台线程专门做延时，浪费线程资源。
- `source_hazard_scan.py` 的 `THREAD_SLEEP` 规则命中这两处。

## 实现

- `subs/ActivitySelector.kt`
  - 移除 `Thread.sleep`，改为 `view?.postDelayed({ Thread { ... }.start() }, animDur.toLong())`。
  - `postDelayed` 在主线程安排延时，延时到期后在主线程启动原后台 `Thread`，
    继续执行 `PackageManager.getPackageInfo` 并在完成后 `runOnUiThread(process)`。
  - 保留后台线程的 `try/catch` 边界和 `OutOfMemoryError`/`ThreadDeath`/`VirtualMachineError`
    再抛逻辑。

- `subs/AppSelector.kt`
  - 同样改为 `view?.postDelayed({ if (act != null) Thread { ... }.start() }, animDur.toLong())`。
  - 保持 `isActivity/share/openwith/multi/privacy/applock` 分支和 `initialized` 赋值逻辑。
  - 保留 `act.runOnUiThread(process)` 和异常处理。

## 行为影响

- 功能等价：仍然先延时 `animDur`，再在后台加载列表，最后回主线程刷新 UI。
- 不再为了一次简单的延时而创建并阻塞后台线程；延时由 `View` 的 `Handler` 调度。
- 原有 `OutOfMemoryError` 等致命错误继续抛出，普通异常继续 `printStackTrace()`。

## 验收标准

- [x] `Thread.sleep` 命中数为 0
- [x] `ActivitySelector`/`AppSelector` 编译通过
- [x] `python tools/source_hazard_scan.py --path app/src/main/java` 通过（0 new）
- [x] `python tools/verify.py fast --changed` 通过
- [x] `docs/audit/SOURCE_HAZARD_BASELINE.json` 已更新

## 验证

```powershell
python tools/source_hazard_scan.py --path app/src/main/java
python tools/verify.py fast --changed
```

## 构建产物

未要求 APK。

## 完成记录

- Base SHA: ef46dce
- Final SHA: 本记录所在的收口 commit
- Commits: 1
- Behavior changed: 否，仅调度方式由 `Thread.sleep` 改为 `postDelayed`
- Verification: source_hazard_scan.py / verify.py fast
- Device evidence: 无（本任务不涉及行为变化，STATIC_VERIFIED）
