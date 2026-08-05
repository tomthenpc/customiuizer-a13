# CustoMIUIzer A13

[简体中文](README.md) | English

CustoMIUIzer A13 customizes Android 13 system UI and interactions on MIUI and HyperOS using an independent package, release line, and modern libxposed API.

## Core features

- Status bar: clock, date, temperature, network speed, battery, signal, and icon layout;
- System UI: control center, notifications, volume, brightness, lock screen, media, and charging information;
- Launcher: icons, folders, Dock, recents, gestures, and animations;
- System behavior: navigation keys, button actions, power menu, floating windows, installer, sharing, and app permissions.

## Compatibility

- MIUI 14 / Android 13: primary compatibility target;
- HyperOS 1 / Android 13: formal target selected through complete Contract/Resolver capability bundles without assuming MIUI 14 internals;
- `arm64-v8a`, applicationId `tv.withaibuild.customiuizer.r13`;
- libxposed `minApiVersion=101`, `targetApiVersion=102`;
- Android 14 and later are not supported.

Known device baseline: Redmi Note 11T Pro (`xaga`), MIUI `V14.0.10.0.TLOINXM`, and LSPosed 2.1.1. HyperOS 1 / Android 13 still requires complete ROM-specific LSPosed logs.

## Build and development

JDK 17, Android SDK, and Python 3 are required. Development gates:

```bash
python tools/verify.py full
python -m compileall tools
python -m unittest discover -s tools/tests -p "test_*.py"
```

Formal builds use an external A13-specific signing configuration. Never commit keys, passwords, tokens, APKs, or local signing files. Disabled features must create no background work; Hook hot paths stay allocation-light; receivers and observers are releasable; `OutOfMemoryError` is never swallowed.

- Current release: `r13.10.0` (versionCode `134`)
- User downloads: <https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases>
- Source: <https://github.com/tomthenpc/customiuizer-a13>

Distributed under GPL-3.0. Derived from Mikanoshi/CustoMIUIzer and informed by MonwF/customiuizer's Android 13 work.
