# CustoMIUIzer A13 Kotlin Refactor

[简体中文](README.md) | English

A Kotlin-refactored, independently maintained CustoMIUIzer build for **MIUI 14 / Android 13**.

This project uses MonwF/customiuizer v23.11.26 as the Android 13 functional reference, with an independent package name, release line, signing identity and modern libxposed API. It is not an official upstream release and does not support Android 14 or later.

## Current version

| Item | Value |
|---|---|
| Version | `r13.8.6` |
| versionCode | `131` |
| System | MIUI 14 / Android 13 (API 33) |
| ABI | `arm64-v8a` |
| Application ID | `tv.withaibuild.customiuizer.r13` |
| libxposed | `minApiVersion=101`, `targetApiVersion=102` |
| staticScope | `false` |
| APK | `CustoMIUIzer-A13-r13.8.6.apk` |
| APK SHA-256 | `ABF31CE311253AE863F7B2CEB87BF95140EE706EFF39ADA219033552B6FA7287` |
| Signing certificate SHA-256 | `C0EFF2DC4E662717195490DA78B12A984C6F2E6BD38ACF4EDAD14D53E3D22E70` |

The LSPosed user download page is at:

`Xposed-Modules-Repo/tv.withaibuild.customiuizer.r13`

## r13.8.6 highlights

* Merged the latest A13 maintenance, Catalog, compatibility diagnostics, scope and UI fixes into `main`;
* Completed the feature catalog, process targets, restart requirements and hook installation result recording;
* Strengthened hook target resolution, compatibility fallback, installation evidence and exception diagnostics;
* Optimized lifecycles for receivers, observers, step counter, device monitor and lock-screen album art;
* Reduced temporary objects and repeated computation in hot paths for status bar, notifications, network speed, battery, clock and Launcher;
* Preserved the system typeface family for status-bar network speed and added dual-row network speed line spacing adjustment;
* Fixed settings text style inheritance and About page text wrapping;
* Unified README, CHANGELOG, version metadata and release process.

See [CHANGELOG_EN.md](CHANGELOG_EN.md) for full changes.

## Compatibility

| Item | Value |
|---|---|
| System | MIUI 14 / Android 13 |
| Primary device | Redmi Note 11T Pro / Pro+ (`xaga`) |
| Reference ROMs | `V14.0.10.0.TLOINXM`, `V14.0.7.0.TLOCNXM` |
| Framework | LSPosed / Vector implementing libxposed API 101 or 102 |
| Android 14+ | Not supported |

Different ROM implementations of SystemUI, Launcher and system apps may vary; some features may need ROM-specific adaptation.

## Feature areas

* Status bar, battery, signal, network speed, clock, date and temperature;
* Control center, volume, brightness, notifications and system animations;
* Lock screen, charging info, media UI, shortcuts and album art;
* Launcher, recents, folders, icons, Dock and launcher gestures;
* Navigation bar, buttons, custom actions, power menu, freeform and Tasker;
* App permissions, installer, sharing, hidden apps and app lock behavior.

## Installation

1. Download the formal APK from the LSPosed distribution repository;
2. Install the APK;
3. Enable the module in LSPosed / Vector and confirm the recommended scope;
4. Open the module settings once;
5. Fully reboot the device.

Early builds signed with a different certificate cannot be upgraded in place. If a signature mismatch appears, back up your settings and uninstall the old build first.

## Building

JDK 17 and Android SDK API 36 are required.

```bash
./gradlew :app:assembleRelease
```

Release builds must use the repository-external formal signing configuration. Do not commit keystores, passwords, tokens or local build artifacts.

## Verification notes

`r13.8.6` has completed a formal Release APK build and the following basic checks:

* APK v2 signature;
* zipalign;
* applicationId, versionCode, versionName;
* libxposed module.prop, scope.list and java_init.list;
* APK SHA-256 and signing certificate verification.

This release did not run the full unit test suite, Lint, engineering audit or full-device functional regression. Build and APK verification do not prove that all features work on every MIUI 14 ROM.

## Feedback

When submitting an issue, please provide:

* Module version and APK source;
* Device and ROM version;
* System app versions such as SystemUI and Launcher;
* LSPosed / Vector version;
* Actual scope;
* Reproduction steps and complete logs.

## License and credits

The project is derived from Mikanoshi/CustoMIUIzer and references MonwF/customiuizer's Android 13 work, distributed under GPL-3.0.
