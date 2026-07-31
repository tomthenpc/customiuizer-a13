# A13 Hot Path Audit

> Branch: `devin/a13-runtime-hardening`
> Final HEAD: `e76605d82a6030255e4d54a20ebdc358c7aeee36`
> Date: 2026-07-31

## Status

| Area | Status | Notes |
|---|---|---|
| Network Speed | VERIFIED_STATIC | `check-invariants.py` no redundant arg marshalling / no regex split on literal; no device traces |
| Clock / Battery | VERIFIED_STATIC | No `getArgsArray` without rewrite, no `String.format` in hot paths; static check only |
| Notification / Launcher Animation | VERIFIED_STATIC | Callbacks guarded or exempted (PreferenceObserver); static check only |
| `ResourceHooks` | VERIFIED_STATIC | `unresolved` name map + bounded `active` `SparseArray`, zero-allocation `chain.getArg(i)`; no dedicated unit tests |
| DexKit / reflection | VERIFIED_STATIC | No reflection in `mods/` callbacks per `check-invariants.py` |

## Verification

- `python tools/check-invariants.py` PASS
- `python tools/verify.py full` PASS
- `gradlew :app:lintDebug` PASS
- No APK build
