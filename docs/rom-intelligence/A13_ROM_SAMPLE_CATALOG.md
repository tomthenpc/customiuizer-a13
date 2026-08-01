# A13 ROM Sample Catalog

Template for tracking local ROM samples.  Do not commit real ROM blobs.

## Columns

| Field | Meaning |
|-------|---------|
| `sampleId` | Short / stable identifier for the sample. |
| `device` | Device marketing name. |
| `codename` | Device codename. |
| `Android` | Android version (e.g. `13`). |
| `SDK` | `SDK_INT` (e.g. `33`). |
| `MIUI/HyperOS` | `MIUI 14` / `HyperOS 1.x` and build. |
| `build fingerprint` | Full `ro.build.fingerprint`. |
| `package` | APK package name, or `BOOTCLASSPATH` for framework jars. |
| `process` | Process where the class is loaded at runtime. |
| `versionName` | APK `versionName`; for framework jars use firmware version. |
| `versionCode` | APK `versionCode`; for framework jars use firmware build number. |
| `source filename` | Relative path inside `local-rom-samples/`. |
| `SHA-256` | SHA-256 of the file. |
| `sample type` | `APK` / `JAR` / `unknown`. |
| `collection date` | ISO-8601 date when the sample was collected. |
| `verification status` | `COMPILE_STUB` / `LOCAL_ROM_SAMPLE` / `DEVICE_EXTRACTED` / `UPSTREAM_REFERENCE` / `UNVERIFIED`. |

## Catalog

| sampleId | device | codename | Android | SDK | MIUI/HyperOS | build fingerprint | package | process | versionName | versionCode | source filename | SHA-256 | sample type | collection date | verification status |
|----------|--------|----------|---------|-----|--------------|-------------------|---------|---------|-------------|-------------|-----------------|---------|-------------|-----------------|---------------------|
| `framework-stub` | *placeholder* | *placeholder* | *placeholder* | *placeholder* | *placeholder* | *placeholder* | `BOOTCLASSPATH` | `system_server` | *placeholder* | *placeholder* | `app/lib/framework.jar` | `COMPILE_STUB` | `JAR` | *placeholder* | `COMPILE_STUB` |
| `miuisystem-stub` | *placeholder* | *placeholder* | *placeholder* | *placeholder* | *placeholder* | *placeholder* | `com.miui.home` / `com.android.systemui` | *placeholder* | *placeholder* | *placeholder* | `app/lib/miuisystem.jar` | `COMPILE_STUB` | `JAR` | *placeholder* | `COMPILE_STUB` |
