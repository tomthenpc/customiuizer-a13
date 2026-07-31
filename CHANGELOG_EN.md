# Changelog

## r13.8.5 (A13 branch consolidation and locally-signed candidate)

### Branch consolidation

- Used `fix/r13.8.3-ui-text-inheritance-and-about-wrap` as the candidate baseline; verified it already contains the effective commits from `devin/r13.7.1-maintenance-foundation` through `devin/r13.8-scope-and-stability`, the Catalog expansion batches, install-evidence work, and the `fix/r13.8-ui` semantics;
- recorded `fix/r13.8-ui-text-inheritance-and-about-wrap` as `patch-equivalent/obsolete` because its about-wrapping and text-style-invariant rules are fully covered by later `r13.8.3` commits; not merged again;
- all other branches are ancestors or patch-equivalent, requiring no extra merge commits.

### Fixes and cleanup

- Unified the active version as `r13.8.5` in `release-manifest.json`, README, CHANGELOG, build docs, and test docs;
- Fixed `release-manifest.json` `compileSdk=34` to `36` to match the Gradle configuration;
- Replaced/updated the stale `r13.7.1` draft and noted the build is a locally-signed candidate pending device/LSPosed validation;
- Clarified the `certificateDnNote` A14 label is a historical certificate name kept for A13 upgrade compatibility; the signing key was not changed;
- Updated CI to trigger on `release/**` and pinned the `audit-system-migration.py` baseline to a stable SHA instead of the unresolvable `backup/r13-k5-before-system-java-removal`.

### Verification

- `check-invariants`, `audit-system-migration`, `audit-architecture`, `audit-prefs`, `audit-canary-sequence`, and `audit-catalog-contracts` all pass;
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:lintRelease`, `:app:lintVitalRelease`, `:app:assembleDebug`, and `:app:minifyReleaseWithR8` results will be filled in after the build;
- `CustoMIUIzer-A13-r13.8.5.apk` is a locally-signed candidate pending real-device and LSPosed log validation.

## r13.7.0 (A13 engineering-parity formal release)

### Runtime stability

- Established a common exception boundary for receivers, content observers, listeners, handlers, and runnables registered from Hooks, preventing a single compatibility failure from escaping into the host process.
- Bound dynamic callbacks to stable owners and registration keys, including replacement, idempotent removal, owner destruction, and stale weak-reference cleanup.
- Fixed permanently cached empty RemotePreferences snapshots, premature listener-registration state, and preference-mirror initialization order.
- Replaced additional-instance-field storage with weak identity keys to avoid retaining Hook targets.
- Restored migrated Launcher loop-exit semantics and rate-limited repeated compatibility-failure logs.

### Performance and lifecycle

- Read Hook arguments through `getArg()` / `chain.args` and avoided argument-array copies when no mutation is required.
- Suspended device monitoring with the screen off and added generation control plus bounded backoff; corrected fixed-slot configuration and data dependencies.
- Added bounded application-icon loading with in-flight deduplication, weak ImageView waiters, and a byte-sized LRU.
- Precomputed settings search data, rejected stale generations, and moved adapters to main-thread-owned replacement lists.
- Moved AudioVisualizer to one frame scheduler and latest-wins Palette processing, releasing observers, frame callbacks, and work on detach.
- Added a bounded single-worker album-art pipeline with target downsampling, a byte cache, and screen/owner cancellation; fixed source-token collisions.

### Engineering and compatibility

- Unified the source namespace as `tv.withaibuild.customiuizer` while retaining application ID `tv.withaibuild.customiuizer.r13`.
- Completed the System, SystemUI, and Launcher domain split and Kotlin migration, then removed compatibility facades without duplicating implementations.
- The migration audit resolves 124/124 baseline System entries, 119 direct call sites, and zero remaining facade calls.
- Retained MIUI 14 / Android 13, `arm64-v8a`, `minSdk=33`, and `targetSdk=34`.
- Retained libxposed `minApiVersion=101`, `targetApiVersion=102`, and `staticScope=false`, with no Legacy Xposed API.

### Verification

- LSPosed 2.1.1 (7790) evidence covered module scope in SystemUI, Launcher, and system processes, with no module-causal crash, ANR, Fatal, Hook/reflection failure, or repeated exception.
- All three bundled ANRs belong to YouTube `TIME_SET` broadcasts. The 17 tombstones belong to audio services, AyuGram, or `libksud`; none contain module classes or causal core-process evidence.
- Unit tests, runtime invariants, System migration audit, all three Lint variants, Debug/Release, R8, resource shrinking, zip alignment, and formal v2 signing are release gates.

### Verification boundary

- Logs and static gates cannot prove every feature on every MIUI 14 variant.
- Device monitoring, AudioVisualizer, and lock-screen album art remain subjects for continued cross-ROM regression. Disable an affected feature and provide complete logs for any compatibility failure.

## Earlier releases

The complete historical changelog is preserved in Chinese in [CHANGELOG.md](CHANGELOG.md). This English file is the synchronized release record beginning with `r13.7.0`.
