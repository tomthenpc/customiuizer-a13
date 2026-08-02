# A13 APK Baseline

```text
Generated: 2026-08-02T13:09:19+00:00
Source: app/build/outputs/apk/debug/CustoMIUIzer-A13-r13.9.2-debug.apk
Size: 12628260 bytes
SHA-256: c9030eaf82fb0591f68d0aa9814bfb192d4bd422220eb795472f2671ec17c174
Build: ./gradlew.bat :app:assembleDebug (no signing)
```

## Delta from P0.4 baseline

| Metric | P0.4 baseline | Current | Delta |
|---|---:|---:|---:|
| Size (bytes) | 12,336,006 | 12,628,260 | +292,254 |
| Build type | debug | debug | — |

The size increase is attributable to:

- Additional Python tool tests under `tools/tests/`;
- New progress snapshot generator and tests;
- New GitHub Actions workflow;
- No change to production Android source in this delta.

This file is a machine-auditable artifact delta. It is not a public release.
