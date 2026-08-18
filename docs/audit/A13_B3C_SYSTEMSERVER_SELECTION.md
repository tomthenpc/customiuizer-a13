# A13 B3C — system_server HYBRID Architecture Selection

| Field | Value |
|---|---|
| **AUTHORITATIVE_BASE_SHA** | B3B corrective freeze (see B3B doc) |
| **Task** | HYBRID selection + CONFIRMED wrapped-fatal / local-helper correctives |
| **PRODUCTION_AUTHORIZATION** | YES (B3C-D1, B3C-D2 only) |
| **Scope** | `SystemServerInstaller` + installer-reachable catalog/legacy hooks |

Frozen: A1–A3, B1, B2A, B2B, B3A, B3B. Inventory drift deferred to B3D.

```text
STATIC_VERIFIED = YES
LOG_VERIFIED = NO
DEVICE_VERIFIED = NO
DEPENDENCY_VERIFICATION_CLEAN_GATE = NOT_PROVEN
B3C_CATALOG_MIGRATION_CANDIDATES = 0
FULL_GATE_RUN = NO
```

---

## 1. Executive summary

`SystemServerInstaller.install` is **HYBRID** and **catalog-heavy**:

- Always `FeatureDispatcher.createRuntime("android", lpparam, …)` then many `installById`.
- Remaining **legacy direct**: `USBConfigHook`, `AlarmCompatServiceHook`, `Controls.PowerKeyHook` / fingerprint / `VolumeMediaButtonsHook`, `GlobalActions.setupGlobalActions`.
- Installer file itself has **no** `XposedHelpers.findClass`.
- `ModuleHelper.findAndHookMethod` already fail-opens ordinary lookup failures.

Catalog first-load is already paid. Remaining legacy paths do not have a catalog-unique install-once / process / ClassLoader / resolver defect.

```text
B3C_CATALOG_MIGRATION_CANDIDATES = 0
```

---

## 2. CONFIRMED_DEFECT (authorized)

| ID | Defect | Fix |
|---|---|---|
| **B3C-D1** | `SystemServerInstaller.needGlobalActions` local `rethrowIfFatal` checks only **direct** `VirtualMachineError` / `ThreadDeath`. Wrapped OOM/TD/VME inside `RuntimeException` is swallowed and can fall through to the media-key fallback | Delete local helper; `RuntimeFatality.throwIfFatal` |
| **B3C-D2** | `PackagePermissions.hook` install-time catch around `MiuiDefaultPermissionGrantPolicy` uses the same direct-type fatal check | `RuntimeFatality.throwIfFatal`; ordinary miss still logs (does not abort later installer features) |

Not modified: catalog specs, `createRuntime("android")`, USB/Controls callback OOM-only catches, `VolumeMediaPlayerHook` (not server-reachable).

---

## 3. Rejected / not auto-fixed

| Class | Item |
|---|---|
| ARCHITECTURE_DEBT | OOM-only catches in Controls / USBConfigHook **callbacks** |
| INTENTIONAL_DIVERGENCE | `createRuntime("android")` hardcoded package identity for system_server |
| INSUFFICIENT_EVIDENCE | `android.media.MediaPlayer` missing on system_server ClassLoader |

---

## 4. Process identity

`MainModule.onSystemServerStarting` → `SystemServerInstaller`. `ProcessScope.SYSTEM_SERVER` vs `ANDROID_PACKAGE` split remains A3/B1 territory. No reopen.

---

## 5. Validation

```text
PRODUCTION_CHANGED = YES (D1/D2)
TEST_CHANGED       = YES
FULL_GATE_RUN      = NO
```
