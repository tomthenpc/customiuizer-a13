# CustoMIUIzer_forA13

An independently installable MIUI 14 / Android 13 maintenance branch of
[CustoMIUIzer](https://github.com/MonwF/customiuizer), based on upstream
[v23.11.26](https://github.com/MonwF/customiuizer/releases/tag/v23.11.26).

This branch targets the modern libxposed API 101 runtime used by LSPosed 2.0
and Vector 2.0. It does not support API 100-only frameworks.

中文说明：[README_ZH.md](README_ZH.md)

## Primary target

- Device: Redmi Note 11T Pro / Pro+ (`xaga`)
- OS: MIUI 14 based on Android 13
- ROMs: `V14.0.10.0.TLOINXM` and `V14.0.7.0.TLOCNXM`
- Framework API: libxposed API 101
- Reference frameworks:
  - LSPosed `v2.0.4 (7741)`
  - Vector `v2.0 (3046)`, [Actions run 29805285935](https://github.com/JingMatrix/Vector/actions/runs/29805285935)

Other Android 13 MIUI 14 builds may work, but they are not part of the primary
compatibility target. The module intentionally refuses to install hooks on
Android versions other than API 33.

## API 101 migration

- Uses `io.github.libxposed:api:101.0.1` and service API `101.0.0`.
- Declares `minApiVersion=101` and `targetApiVersion=101`.
- Uses the API 101 lifecycle: `onModuleLoaded`, `onPackageReady`, and
  `onSystemServerStarting`.
- Registers hooks through `HookBuilder` and `Chain.intercept` with
  `ExceptionMode.PASSTHROUGH`.
- Resource and package-permission hooks already use native API 101
  interceptors.
- Remaining MIUI feature hooks use a contained compatibility adapter that
  preserves mutable arguments, early return, throwable propagation, and
  after-hook result replacement while they are migrated incrementally.

The application ID is `tv.withaibuild.customiuizer.r13`, so it can coexist with
the upstream build and the separate Android 14 branch. Preferences are not
automatically copied between these independently installed variants.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew :app:assembleRelease
```

If `../keystore.properties` is present, the release key configured there is
used. Otherwise Gradle produces a debug-signed release artifact for local
testing. Output is written under `app/build/outputs/apk/release/`.

## Device validation checklist

1. Remove or disable the API 100 edition with the same feature set.
2. Install `CustoMIUIzer-A13` (米客 A13), enable it only for the packages in its default
   scope, then reboot.
3. Confirm the settings UI can write remote preferences.
4. Validate System UI, launcher, system-server, Security Center, Power Keeper,
   package installer, screenshot, and in-call features separately.
5. Collect a full LSPosed/Vector log before enabling another feature group if a
   target ROM uses different MIUI class or method signatures.

## Credits and license

Based on CustoMIUIzer by Mikanoshi and MonwF. API 101 migration and this A13
maintenance branch are maintained separately.

Licensed under [GPL-3.0](LICENSE).
