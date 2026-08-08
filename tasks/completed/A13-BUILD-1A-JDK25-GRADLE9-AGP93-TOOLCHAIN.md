# A13-BUILD-1A — JDK25 / Gradle 9.6.1 / AGP 9.3.1 Toolchain Parity Migration

## Task

BUILD TOOLCHAIN migration only. No production code changes, no feature changes,
no P2-1/P3, no module split, no Kotlinization, no hook optimization, no lifecycle
fix, no libxposed upgrade, no compileSdk/targetSdk upgrade.

## Base

- Stable base SHA: `a93ef570f25e1d2b2c0f1be27091f6cc04450a9f`
- Branch: `devin/a13-jdk25-build-toolchain`
- Reference: `customiuizer-a14/devin/a14-jdk25-build-toolchain`

## Target

| Item | Value |
|---|---|
| Gradle | 9.6.1 |
| AGP | 9.3.1 |
| Build/Daemon JVM | 25 |
| Java toolchain | 25 |
| Android Java bytecode | 17 |
| Built-in Kotlin | enabled |
| Kotlin BOM | 2.3.21 |

## Frozen A13 identity

- namespace = `tv.withaibuild.customiuizer`
- applicationId = `tv.withaibuild.customiuizer.r13`
- compileSdk = 36
- minSdk = 33
- targetSdk = 34
- versionCode = 135
- versionName = `r13.10.1`
- ABI = `arm64-v8a`
- libxposed-api = 101.0.1
- libxposed-service = 101.0.0

## APK naming

- debug = `CustoMIUIzer-A13-r13.10.1-debug.apk`
- release = `CustoMIUIzer-A13-r13.10.1.apk`
- develop = current existing semantics

## Signing

- A13 signing model frozen: `customiuizerA13KeystoreProperties`
- Cert SHA-256: `15CE32F03E4D8E62DF9390F77431862E59BF2CF95CD5A72F0C7330CDFCCA2934`

## Verification Results

- Gradle 9.6.1, Launcher JVM 25.0.4, Daemon JVM 25
- AGP 9.3.1, Built-in Kotlin enabled
- Java toolchain 25, Java bytecode major version 61 (Java 17)
- Kotlin stdlib resolved to 2.3.21 (single version, no conflicts)
- applicationVariants = 0 in production/build config
- resConfigs = 0 in production/build config
- kotlin-android plugin = 0 in production/build config
- Configuration cache reused on second run
- Dependency verification strict: PASS
- Debug compile: PASS
- Debug unit tests: PASS
- Release compile: PASS
- lintRelease: PASS
- lintVitalRelease: PASS
- minifyReleaseWithR8: PASS
- analyzeReleaseR8Config: PASS
- Python tests: 1077 OK (skipped=2)
- memory lifecycle verify: PASS
- source hazard scan: PASS
- hook cost scan: PASS
- verify.py full: PASS
- Production diff (java/res): empty

## Additional gradle.properties

- `android.r8.optimizedResourceShrinking=false` added because AGP 9 requires
  `android.nonFinalResIds=true` for optimized resource shrinking, but A13
  intentionally keeps `android.nonFinalResIds=false`. This is the AGP-recommended
  alternative.

## Status

BUILD-1A = TOOLCHAIN_MIGRATION_ACCEPTED_PENDING_EXACT_HEAD_ARTIFACT
