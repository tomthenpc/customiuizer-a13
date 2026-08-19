# CustoMIUIzer A13

[简体中文](README.md) | English

CustoMIUIzer A13 customizes the system UI and interactions on **MIUI 14 / Android 13**, with capability-based compatibility paths for **HyperOS 1 / Android 13**. It uses an independent package, release line, and modern libxposed API.

> **Maintenance status**
>
> Active development has ended as the maintainer has moved to other systems.
> The project remains available as a stable maintenance build for MIUI 14 / Android 13.
> Future updates, if any, will be limited to critical fixes, necessary compatibility work, or actual maintainer needs.

- Current version: `r13.12.2` (versionCode `140`)
- Application ID: `tv.withaibuild.customiuizer.r13`
- Source repository: <https://github.com/tomthenpc/customiuizer-a13>
- User downloads: <https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases>
- Deployed framework: LSPosed / Vector

## Core Features

- Status-bar clock, date, temperature, network speed, battery, signal, and icon layout;
- Control center, notifications, volume, brightness, lock screen, media, and charging UI;
- Launcher icons, folders, Dock, recents, gestures, and animations;
- Navigation keys, button actions, power menu, floating windows, multi-window behavior, installer, sharing, and app permissions.

## Compatibility

| Item | Supported range |
| --- | --- |
| Primary system | MIUI 14 / Android 13 |
| Capability-detected target | HyperOS 1 / Android 13; feature availability depends on the ROM and system-app versions |
| SDK | minSdk 33 / targetSdk 34 |
| ABI | `arm64-v8a` |
| Xposed framework | LSPosed / Vector |
| Module metadata | `minApiVersion=101`, `targetApiVersion=102`, `staticScope=false` |

Android 14 and later are not supported. Do not enable this module together with upstream or another CustoMIUIzer-derived module.

Known deployed baseline: Redmi Note 11T Pro (`xaga`), MIUI `V14.0.10.0.TLOINXM`.

## Runtime Architecture

- SystemUI, Launcher, `system_server`, and regular-app entry points are routed to process-specific Installers, avoiding unrelated installation paths in the wrong process;
- Features use stable identities, process scopes, install phases, and install-once state; disabled or incompatible features skip unrelated registrations and object creation;
- ROM Contracts, Resolvers, and Installers share the resolved target, while missing targets safely skip only the affected feature;
- Receiver, Observer, View, and controller lifecycles are owner-bound with replacement, stale-state, and release paths;
- Ordinary ROM, reflection, and callback failures remain isolated, while `OutOfMemoryError`, `ThreadDeath`, and `VirtualMachineError` are not disguised as compatibility failures;
- DeviceInfo and Launcher hot paths reduce repeated Binder calls, reflection, I/O, configuration reads, and temporary objects.

See [CHANGELOG_EN.md](CHANGELOG_EN.md) for release changes and [AGENTS.md](AGENTS.md), [ARCHITECTURE.md](ARCHITECTURE.md), [COMPATIBILITY.md](COMPATIBILITY.md), and [docs/A13_PARITY.md](docs/A13_PARITY.md) for engineering rules, architecture, and compatibility.

`r13.12.2` supersedes `r13.12.0` / `r13.12.1`. This final release adds USB default mode, installer purify, Launcher dock height, hiding the IME dismiss button, status-bar visual options, folder-blur disable, Backup V2 with legacy restore, BatteryIndicator custom colors, and dim-ratio adjustment, and fixes MultiAction / gesture persistence, spinner OOB, Launcher restart scope, USB replug latch, app/shortcut/Activity result relay, and final MultiAction result delivery. See [CHANGELOG_EN.md](CHANGELOG_EN.md).

## Build and verification

```bash
python tools/verify.py full
```

## Support and contact

If this project helps you, you can support continued development via [PayPal](https://paypal.me/Jinjitv).

- Source repository: <https://github.com/tomthenpc/customiuizer-a13>
- User downloads: <https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases>

## Development notes

- Stability and behavior preservation come first; compatibility logic stays at ROM / ClassLoader boundaries;
- Hot-path hooks avoid temporary arrays, collections, regex, formatting, repeated reflection, and remote preference reads;
- Java-to-Kotlin migrations are behavior-equivalent and gated by tests;
- `MainModule.java`, `XposedHelpers.java`, and `MemberUtilsX.java` remain the JVM / framework boundary;
- Fine-grained history lives in Git commits and tags; release changes are in [CHANGELOG_EN.md](CHANGELOG_EN.md).

Distributed under GPL-3.0. Derived from Mikanoshi/CustoMIUIzer and informed by MonwF/customiuizer's Android 13 work.
