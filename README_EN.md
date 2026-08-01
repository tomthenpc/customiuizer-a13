# CustoMIUIzer A13

[简体中文](README.md) | English

CustoMIUIzer A13 is an Android 13 system UI and interaction customization module for MIUI and HyperOS. It maintains the historical CustoMIUIzer feature semantics with an independent package, release line, and modern libxposed API.

## Core features

- Status bar: clock, date, temperature, network speed, battery, signal, and icon layout;
- System UI: control center, notifications, volume, brightness, lock screen, media, and charging information;
- Launcher: icons, folders, Dock, recents, gestures, and animations;
- System behavior: navigation keys, button actions, power menu, floating windows, installer, sharing, and app permissions.

## Compatibility

- MIUI 14 / Android 13: primary compatibility target;
- HyperOS 1 / Android 13: formal compatibility target, guarded by Contract/Resolver capability checks without assuming MIUI 14 internals;
- ABI: `arm64-v8a`;
- applicationId: `tv.withaibuild.customiuizer.r13`;
- libxposed: `minApiVersion=101`, `targetApiVersion=102`;
- Android 14 and later are outside this project's scope.

The known device baseline is Redmi Note 11T Pro (`xaga`), MIUI `V14.0.10.0.TLOINXM`, and LSPosed 2.1.1. HyperOS 1 / Android 13 candidates must pass capability checks and still require detailed LSPosed-log validation; static verification is not a complete on-device regression.

## Build and verification

JDK 17, Android SDK, and Python 3 are required. Run the regular development gates with:

```bash
python tools/verify.py full
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
```

Formal Release builds use an external A13-specific signing configuration:

```bash
./gradlew :app:assembleRelease
```

Never commit keystores, passwords, tokens, APKs, or local signing configuration.

## Source and development

- Source: <https://github.com/tomthenpc/customiuizer-a13>
- User downloads: <https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases>
- Current release: `r13.9.1` (versionCode `132`)
- Development rules: disabled features create no background work; hot Hooks avoid repeated reflection, blocking, and temporary allocations; receivers and observers are idempotent and releasable; ordinary failures are isolated while `OutOfMemoryError` is rethrown.

Compatibility reports should include device, ROM, SystemUI/Launcher versions, framework version, actual scope, reproduction steps, and complete LSPosed logs.

Distributed under GPL-3.0. Derived from Mikanoshi/CustoMIUIzer and informed by MonwF/customiuizer's Android 13 work.
