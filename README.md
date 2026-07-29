# CustoMIUIzer A13

`CustoMIUIzer-A13` is a long-term independently maintained fork of [CustoMIUIzer](https://github.com/MonwF/customiuizer) upstream version `v23.11.26`, targeting MIUI 14 / Android 13 devices.

> **Note**: The `customiuizer-a14` branch is used only as an **engineering reference** (Kotlin DSL, version catalog, CI, testing patterns). It is not a source of A13 runtime facts. A13-specific MIUI class names, method signatures, Hook targets, resource IDs, SystemUI / Launcher structure, ROM gating, preference keys, and Manifest components are not copied from A14.

## Primary Targets

- **Device**: Redmi Note 11T Pro / Pro+ (`xaga`)
- **System**: MIUI 14 / Android 13 (API 33)
- **Reference ROMs**: `V14.0.10.0.TLOINXM`, `V14.0.7.0.TLOCNXM`
- **Application ID**: `tv.withaibuild.customiuizer.r13`
- **ABI**: `arm64-v8a`
- **libxposed**: `minApiVersion=101`, `targetApiVersion=102`, `staticScope=false`
- **Recommended Framework**: LSPosed 2.0 / Vector 2.0

Other MIUI 14 / Android 13 builds may work but are not the primary validation target.

## Relationship to Upstream and A14

- **Upstream baseline**: Functionality is anchored to `MonwF/customiuizer v23.11.26`.
- **A13 independence**: Application ID, signing key, version codes, and build system are isolated from upstream and A14; they cannot be installed over each other.
- **A14 as engineering reference only**: A14 can inform build tooling, version catalog layout, CI structure, and API 101/102 compatibility approaches, but not A13 ROM facts.

## Build

Requirements: JDK 17, Android SDK (compile SDK 36, build-tools 37.0.1).

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release / Develop builds require a `../keystore.properties` file pointing to the A13 release keystore (`C:/Users/tv/Documents/buildkey/r13/customiuizer-a13-release.p12`). The build fails explicitly if release signing is missing and never falls back to the Android debug key. Debug builds and regular tests are unaffected.

Default output directories:

- Debug: `app/build/outputs/apk/debug/`
- Release: `app/build/outputs/apk/release/`

> As of `r13.2.0-devin`, the APK output name follows the AGP default `app-<variant>.apk` because AGP 8.7's VariantOutput API does not expose `outputFileName`. This will be restored to `CustoMIUIzer-A13-r13.x.x.apk` once the API is available.

## Verification

Recommended local verification:

```bash
./gradlew clean :app:test :app:lintRelease :app:assembleDebug :app:assembleRelease
```

Verification includes:

- Unit tests (`ModuleMetadataTest` checks `module.prop` / `java_init.list`)
- `lintRelease`
- Debug and Release compilation
- Release R8 + resource shrink
- zipalign verification
- `META-INF/xposed/module.prop`: `minApiVersion=101`, `targetApiVersion=102`, `staticScope=false`
- `META-INF/xposed/java_init.list`: `tv.withaibuild.customiuizer.MainModule`
- `applicationId = tv.withaibuild.customiuizer.r13`, `minSdk=33`, `targetSdk=34`
- Release APK signed with v2, certificate CN=`CustoMIUIzer A13`

## Installation

1. Uninstall or disable any previous A13 build signed with the old key (the previous signing key is lost; older builds cannot be overwritten).
2. Install the new APK, enable the default scope in LSPosed / Vector, and reboot.
3. Open the app and confirm remote preferences can be read/written.
4. Verify features by functional group: System UI, Launcher, system_server, Security, PowerKeeper, Package installer, Screenshot, InCallUI.
5. If a feature fails, disable it and capture full framework logs; MIUI class names and signatures can vary between ROM builds.

## Functionality

The feature set matches upstream `v23.11.26` for the A13 target. Main groups include:

- System: lock screen, status bar, control center, clock, charging animation, screenshot, wallpaper
- Launcher: icons, gestures, dock, drawer
- Calls and contacts: in-call UI brightness, hidden features
- Misc: global gestures, scroll-to-top, freeform, Tasker
- Settings and preference storage

## Roadmap and Known Limitations

- ✅ New A13 long-term signing key established and verified
- ✅ Build system migrated to Kotlin DSL + version catalog
- ✅ API 101/102 compatibility metadata
- ✅ Unit tests, Lint, and CI baseline
- 🔄 Lifecycle and high-frequency Hook governance (in progress, per functional group)
- 🔄 Low-risk Kotlin-first migration (launcher Activities migrated, more to follow)
- ⏳ Restore APK output naming to `CustoMIUIzer-A13-r13.x.x.apk`
- ⏳ Real-device regression on reference ROMs

## License and Credits

Based on [Mikanoshi](https://github.com/Mikanoshi) and [MonwF](https://github.com/MonwF) CustoMIUIzer. The A13 independent fork is licensed under [GPL-3.0](LICENSE).