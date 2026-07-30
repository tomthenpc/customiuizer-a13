# CustoMIUIzer A13

[简体中文](README.md) | English

A Kotlin-refactored, independently maintained CustoMIUIzer build for
**MIUI 14 / Android 13**.

The project uses [MonwF/customiuizer v23.11.26](https://github.com/MonwF/customiuizer)
as its Android 13 functional reference, with a separate package name, release line, signing
identity, and modern libxposed API integration. Kotlin is the primary implementation language,
while audited, stable Java/JVM boundaries remain. It is not an official upstream release and
does not support Android 14 or later, or other major MIUI versions. User-facing installation and
downloads are hosted in
[Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13](https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13).

## r13.7.0

`r13.7.0` is the first formal release after the A13 engineering parity and stability program:

- unified the source namespace under `tv.withaibuild.customiuizer` while retaining application ID `tv.withaibuild.customiuizer.r13`;
- split and migrated the large System, SystemUI, and Launcher implementations, with migration audits protecting Hook order, arguments, and invocation counts;
- added module-owned exception boundaries and lifecycle ownership for deferred receivers, observers, listeners, handlers, and runnables;
- fixed RemotePreferences initialization, listener registration, weak-reference cleanup, and additional-instance-field lifetime handling;
- bounded queues, caches, and invalidation for device monitoring, application icons, settings search, AudioVisualizer, and lock-screen album art;
- retained libxposed API 101 as the minimum runtime baseline while publishing API 102 metadata.

See [CHANGELOG_EN.md](CHANGELOG_EN.md) for details; the Chinese changelog is [CHANGELOG.md](CHANGELOG.md).

## Compatibility

| Item | Value |
|---|---|
| System | MIUI 14 / Android 13 (API 33) |
| Primary device | Redmi Note 11T Pro / Pro+ (`xaga`) |
| Reference ROMs | `V14.0.10.0.TLOINXM`, `V14.0.7.0.TLOCNXM` |
| ABI | `arm64-v8a` |
| Application ID | `tv.withaibuild.customiuizer.r13` |
| libxposed | `minApiVersion=101`, `targetApiVersion=102`, `staticScope=false` |
| Recommended framework | LSPosed 2.x / Vector 2.x |

Other MIUI 14 / Android 13 builds may work, but SystemUI, Launcher, and system-app signatures differ between ROMs. Android 14 and later are outside this repository's support scope.

## Main feature areas

- status bar, battery, signal, network speed, clock, date, and temperature;
- control center, volume, brightness, notifications, and system animations;
- lock screen, charging information, media UI, shortcuts, and album art;
- launcher, recents, folders, icons, dock, drawer, and launcher gestures;
- navigation, buttons, custom actions, power menu, freeform, and Tasker;
- permissions, installer, sharing, hidden applications, app lock, and other MIUI behaviors.

Actual availability depends on the device, MIUI build, system-app versions, and enabled scope.

## Installation

1. Download the formal APK from the [LSPosed distribution repository](https://github.com/Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13/releases).
2. Install it and enable the recommended scope in LSPosed / Vector.
3. Open module settings once, then fully reboot the device.
4. Enable and verify features by group. If a problem appears, disable the affected feature before collecting LSPosed logs.

Early A13 builds signed with a different certificate cannot be upgraded in place. Back up settings and uninstall the old build if Android reports a signature mismatch.

## Building

JDK 17 and Android SDK API 36 are required. The project does not pin `buildToolsVersion`; the current AGP and local SDK select a compatible Build Tools installation.

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release and Develop builds require repository-external `../keystore.properties` pointing to the formal A13 signing key. Packaging fails explicitly when formal signing is unavailable and never falls back to the Debug certificate.

Recommended local gates:

```bash
python tools/check-invariants.py
python tools/audit-system-migration.py --baseline-ref backup/r13-k5-before-system-java-removal
./gradlew clean :app:test :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease
./gradlew :app:lintVitalRelease --rerun-tasks
```

Release builds use R8 and resource shrinking. Formal publication also verifies zip alignment, v2 signing, the certificate, APK and Xposed metadata, the Legacy Xposed API boundary, and the final SHA-256.

## Engineering boundaries

- A13 is the independent runtime baseline. A14 is an engineering-method reference only and cannot supply A13 Hook targets or ROM facts.
- Stable Java/JVM boundaries remain in place; Kotlin coverage is not a release criterion.
- Builds, Lint, R8, and signing are static evidence and do not replace device/ROM validation.
- Legacy Xposed API, Hot Reload, hook IDs, and atomic replacement remain disabled.

## Feedback

Open an issue in this repository with the module version, device and ROM, system-app versions, LSPosed/Vector version, actual scope, reproduction steps, and complete logs. A package-name mention alone is not module causality; include a Hook failure, module stack, or crash context.

## License and credits

Based on CustoMIUIzer by [Mikanoshi](https://github.com/Mikanoshi) and [MonwF](https://github.com/MonwF), licensed under [GPL-3.0](LICENSE).
