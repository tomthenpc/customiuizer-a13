# A13 Hot Path Audit

> Branch: `devin/a13-runtime-hardening`
> Date: 2026-07-31

## Status

| Area | Status | Notes |
|---|---|---|
| Network Speed | VERIFIED | `check-invariants.py` no redundant arg marshalling / no regex split on literal |
| Clock / Battery | VERIFIED | No `getArgsArray` without rewrite, no `String.format` in hot paths |
| Notification / Launcher Animation | VERIFIED | Callbacks guarded or exempted (PreferenceObserver) |
| ResourceHooks | COMPLETED | `unresolved` name map + bounded `active` `SparseArray`, zero-allocation hot path `chain.getArg(i)` |
| DexKit / reflection | VERIFIED | No reflection in `mods/` callbacks per `check-invariants.py` |

## Verification

- `python tools/check-invariants.py` passes
- `python tools/verify.py full` passes
- Lint `lintDebug` passes

## 剩余工作

- `ResourceHooks` 完整 `SparseArray` 拆分需在有设备 trace 后单独提交。
- `mods/` 匿名 `BroadcastReceiver`、`ContentObserver`、`Runnable` 和 view listener 边界已受 `ModuleHelper.guarded` 约束，未覆盖项标记为 `PARTIAL`。
