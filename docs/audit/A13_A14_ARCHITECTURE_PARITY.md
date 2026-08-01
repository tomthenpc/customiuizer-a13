# A13 / A14 Architecture Parity

Scope: A13 `devin/a13-rom-intelligence-audit` vs A14 `devin/a14-runtime-hardening`. A14 is read-only reference.

| Component | A14 Location | A13 Location | Status | Gap / Action |
|---|---|---|---|---|
| ProcessScope | `mods/utils/ProcessScope.kt` | `mods/utils/ProcessScope.kt` | ALIGNED | A13 enum and `ProcessScopes` cover 18 scopes, matching A14 semantics. |
| ProcessRouter | `mods/utils/ProcessRouter.kt` | `mods/utils/ProcessScopes.kt` | ALIGNED | `ProcessScopes.resolve` is the A13 pure-function equivalent. |
| MainModule process routing | `MainModule.java` uses `ProcessScope.isInstallable` | `MainModule.java` uses `ProcessScopes.isRejected`/`ProcessScope` | PARTIAL | `MainModule` now uses `ProcessScope` for input method / launcher / systemui / settings / security center / power keeper / wallpaper / media / phone. AlarmCompat and generic app attach still remain. |
| InputMethodInstaller | `installers/InputMethodInstaller.kt` (via registry) | `installers/InputMethodInstaller.java` | ALIGNED | A13 installer extracted from `MainModule` and owns all input method hooks. |
| SystemServerInstaller | `mods/utils/SystemServerInstaller.kt` | `installers/SystemServerInstaller.java` | ALIGNED | Java equivalent, installs package permissions. |
| SystemUiInstaller | `installers/SystemUiInstaller.kt` | `installers/SystemUiInstaller.java` | ALIGNED | Java equivalent. |
| LauncherInstaller | `installers/LauncherInstaller.kt` | `installers/LauncherInstaller.java` | ALIGNED | Java equivalent. |
| SettingsInstaller | `installers/SettingsInstaller.kt` | `installers/SettingsInstaller.java` | ALIGNED | Migrated out of `PackageInstallerRouter`. |
| SecurityCenterInstaller | `installers/SecurityCenterInstaller.kt` | `installers/SecurityCenterInstaller.java` | ALIGNED | Migrated out of `PackageInstallerRouter`. |
| PowerKeeperInstaller | `installers/PowerKeeperInstaller.kt` | `installers/PowerKeeperInstaller.java` | ALIGNED | Migrated out of `PackageInstallerRouter`. |
| WallpaperInstaller | `installers/WallpaperInstaller.kt` | `installers/WallpaperInstaller.java` | ALIGNED | Migrated out of `PackageInstallerRouter`. |
| MediaInstaller | `installers/MediaInstaller.kt` | `installers/MediaInstaller.java` | ALIGNED | Migrated out of `PackageInstallerRouter`. |
| PhoneInstaller | `installers/PhoneInstaller.kt` | `installers/PhoneInstaller.java` | ALIGNED | Migrated out of `PackageInstallerRouter`. |
| PackageInstallerRouter | `installers/PackageInstallerRouter.kt` | `installers/PackageInstallerRouter.java` | ALIGNED | A13 version is the legacy router; A14 uses it too for package installer features. |
| GenericAppInstaller | `installers/GenericAppInstaller.kt` | `MainModule.onPackageReady` (inline) | PARTIAL | A13 generic app hooks still inline; A14 has a dedicated installer. |
| FeatureId | `mods/utils/FeatureId.kt` | `mods/utils/FeatureId.kt` | ALIGNED | A13 model added. |
| FeatureState | `mods/utils/FeatureState.kt` | `mods/utils/FeatureState.kt` | ALIGNED | A13 model added. |
| FeatureInstallResult | `mods/utils/FeatureInstallResult.kt` | `mods/utils/FeatureInstallResult.kt` | ALIGNED | A13 model added. |
| FeatureInstallState | `mods/utils/FeatureInstallState.kt` | `mods/utils/FeatureInstallState.kt` | ALIGNED | A13 model added. |
| FeatureSpec | `mods/utils/FeatureSpec.kt` (interface) | `mods/catalog/FeatureSpec.kt` (data class) | PARTIAL | A13 catalog `FeatureSpec` exists but is data class; A14 is an interface. Need unify. |
| FeatureDefinition | `mods/utils/FeatureDefinition.kt` | `mods/catalog/FeatureSpec` + installers | MISSING | A13 has no `FeatureDefinition` abstraction. |
| FeatureInstallRegistry | `mods/utils/FeatureInstallRegistry.kt` | `mods/catalog/FeatureDispatcher.kt` (object, returns Boolean) | PARTIAL | `FeatureDispatcher` returns `Boolean`. A13 has `FeatureLifecycle`/`FeatureInstallResult` models but not a real registry. |
| InstallPhase | `mods/utils/InstallPhase.kt` | Implicit in installers / catalog | MISSING | A13 has no `InstallPhase` enum. |
| FeatureTarget | `mods/utils/FeatureTarget.kt` | `mods/catalog/ProcessTarget.kt` | PARTIAL | `ProcessTarget` sealed class serves similar purpose but naming differs. |
| PreferenceBootstrap | `utils/PreferenceBootstrap.kt` | `utils/PreferenceBootstrap.java` | ALIGNED | Java equivalent, single process bootstrap. |
| FeatureTarget | `mods/utils/FeatureTarget.kt` | `mods/catalog/ProcessTarget.kt` | PARTIAL | A13 `ProcessTarget` sealed class vs A14 `FeatureTarget` enum. |
| Contract / Target resolver | `mods/utils/Contract.kt`, `mods/utils/TargetResolver.kt` | `mods/catalog/CanaryContracts.kt` | PARTIAL | A13 has static Canary/Catalog contracts but no active resolver. |
| ReflectionCache | `mods/utils/ReflectionCache.kt` | `mods/utils/ReflectionCache.java` | ALIGNED | Java equivalent with safe lifecycle. |
| Diagnostics | `mods/utils/HookDiagnostics.kt` | `mods/diagnostics/DiagnosticRecorder.kt` | PARTIAL | A13 has `DiagnosticRecorder` but not unified with `InstallResult`. |
| Invariant checks | `tools/check-invariants.py` | `tools/check-invariants.py` + new static tests | PARTIAL | A13 checks process matrix and contracts; architecture invariants partially extended. |
| Feature Inventory | generated | `docs/rom-intelligence/A13_TARGET_MATRIX.md` | MISSING | A13 has manual matrix; A14 generates inventory. A13 `FeatureInventory` generator pending. |

## Overall Status

- Phase 1 (ProcessScope / MainModule routing) largely complete.
- Phase 2 (FeatureInstallState / FeatureInstallResult / FeatureId) model layer added.
- Remaining `MISSING` / `PARTIAL` are the per-package installer split and Feature registry/Spec unification.
