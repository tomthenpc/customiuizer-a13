# A13 Device Regression Checklist

> Branch: `devin/a13-rom-intelligence-audit`
> Base: `ac49cae8deb4fe24df2621c0a2f2aae9d510ba86`
> This checklist is **manual only**. Do not run ADB or device automation. Tick each item after an exported LSPosed detail log or real-device repro confirms it.

## Status bar temperature / current / battery custom view attach

| # | Step | Expected | Evidence required |
|---|---|---|---|
| 1.1 | Enable `system_batteryindicator` on MIUI 14 / HyperOS 1 A13. | `SystemUI` does not crash and the battery indicator renders. | LSPosed log: no `IndexOutOfBoundsException` in `com.android.systemui` and `BatteryIndicator` `InstallOutcome` is `DISPATCHED` / `HOOK_OK`. |
| 1.2 | Toggle the setting off and on, then restart `SystemUI`. | No duplicate battery indicators; icon count remains stable. | Screenshot or log showing a single `BatteryIndicator` view instance. |
| 1.3 | Enable status bar `system_showdevicetemperature` / current if present. | The temperature/current text is added once and the icon area count does not exceed the parent child count. | LSPosed log around `IconManager.addHolder` shows no `IndexOutOfBoundsException`. |
| 1.4 | Lock and unlock the device. | The custom view is removed from the old owner before being re-added to the new one. | Log of `onViewAttachedToWindow` / `onViewDetachedFromWindow` with single add/remove. |
| 1.5 | Trigger `SystemUI` recreation (theme change or volume up/down). | The hook is re-installed once; no stale `View` is reused. | `InstallSummary` shows `statusBarClockTweak` or `batteryIndicator` once per process. |

## Status bar seconds / clock

| # | Step | Expected | Evidence required |
|---|---|---|---|
| 2.1 | Enable `system_statusbar_clocktweak` and set a seconds-capable format. | The clock updates every second. | LSPosed log or video: seconds digit changes; `MiuiClock.updateTime` fires at 1 Hz. |
| 2.2 | Disable seconds, then re-enable. | The old update callback is removed and a single new one is created. | Log shows no duplicate `Handler`/`Runnable` posts. |
| 2.3 | Change the clock format string while the screen is on. | The display updates with the new format immediately or after the next second tick. | `FeatureDispatcher` reload does not crash `SystemUI`. |
| 2.4 | Verify `CanaryContracts.statusBarClockTweak` on the target ROM. | The contract resolves `MiuiStatusBarClockController`, `MiuiClock` and the visibility setter. | `HookTargetResolver` output in LSPosed shows all REQUIRED targets `FOUND`. |
| 2.5 | Confirm `STATUSBAR_CLOCK_TWEAK` does not report `INSTALLER_FAILED` disguised as `DISPATCHED`. | `InstallSummary` or `DiagnosticRecorder` contains `INSTALLER_FAILED` with a reason code, not `DISPATCHED`. | Exported LSPosed detail log. |

## Launcher / receiver / observer lifecycle

| # | Step | Expected | Evidence required |
|---|---|---|---|
| 3.1 | Enable a Launcher-only feature (e.g. `launcher_noclockhide`) and restart Launcher. | The hook installs once; `handleLoadLauncher` is not invoked twice in the same process. | Log shows one `LauncherInstaller.handleLoadLauncher` entry. |
| 3.2 | Rotate the device. | No new receivers or observers are registered; existing ones remain active. | `ModuleHelper` receiver map size does not grow. |
| 3.3 | Kill and relaunch the Launcher process. | The old `owner`/`Activity` is stale; no `View` or `Receiver` continues to act on it. | No `NullPointerException` / stale owner logs. |
| 3.4 | Enable a Launcher gesture (e.g. swipe-up action). | The gesture callback is guarded and does not throw into the framework. | LSPosed log: `LauncherGestureHooks` actions run inside `ModuleHelper.guarded`. |

## Notification / floating window (Issue #665 pattern)

| # | Step | Expected | Evidence required |
|---|---|---|---|
| 4.1 | Enable any notification-related "open in floating window" feature. | The selected notification opens without black screen or `Activity` crash. | LSPosed log: no `IllegalStateException` / `Window` crash. |

## Screenshot format (Issue #477 pattern)

| # | Step | Expected | Evidence required |
|---|---|---|---|
| 5.1 | Set screenshot format to `png` and capture. | File is saved in PNG format if the target `com.miui.screenshot` supports it. | File extension / media type. |

## General process / ClassLoader sanity

| # | Step | Expected | Evidence required |
|---|---|---|---|
| 6.1 | Check `com.android.systemui` process log. | Only SystemUI features are installed; `system_server` hooks are not re-attempted. | LSPosed log per process. |
| 6.2 | Check `com.miui.home` / `com.mi.android.globallauncher` process log. | Only Launcher features are installed; `SystemUI` hooks are not loaded. | LSPosed log per process. |
| 6.3 | Check `com.miui.securitycenter` process log. | `bootaware` helper process is rejected or feature-gated. | Log shows `bootaware` skipped. |

## Evidence status tags

Use these tags when reporting results:

* `STATIC_VERIFIED` — proven by source / unit tests / `check-invariants.py`.
* `LOG_VERIFIED` — proven by an exported LSPosed detail log.
* `DEVICE_HOOK_VERIFIED` — the hook is installed and the class/method is resolved.
* `DEVICE_BEHAVIOR_VERIFIED` — the user-visible behavior is confirmed on a real device.
* `NOT_EXERCISED` — no device evidence available this round.
