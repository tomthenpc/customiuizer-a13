# Issue #1 Launcher Folder Regression Evidence

## Issue subset handled in this batch

- Feature request: dual-row network-speed spacing → `DEFER`
- Regression: launcher folder width + folder spacing
- User launcher: `RELEASE-4.39.14.8060-04191512`

## User-visible semantics

| preference | default | title (en/zh-rCN) | user-visible meaning |
|---|---|---|---|
| `launcher_folderwidth` | `false` | "Use entire folder's width" / "使用整个文件夹的宽度" | Use entire folder width |
| `launcher_folderspace` | `false` | "Reduce side padding" / "减少边距" | Reduce folder side padding |
| `launcher_folder_cols` | `1` (range 1-6) | "Number of columns in folders" / "文件夹排列数量" | Custom folder column count |

## Production installer gate

`LauncherInstaller.handleLoadLauncher` installs the `folderColumns` feature only if:

```java
if (MainModule.mPrefs.getInt("launcher_folder_cols", 1) > 1)
    FeatureDispatcher.installById("folderColumns", launcherRuntime);
```

`MainModule.onPackageReady` calls `LauncherInstaller.installApplication` only when `LauncherInstaller.hasAnyLauncherApplicationFeature(mPrefs)` returns `true`.

`hasAnyLauncherApplicationFeature` (and `hasAnyLauncherStartupFeature`) does **not** include `launcher_folderwidth` or `launcher_folderspace`.

### Install gate matrix

| Setting state | `hasAnyLauncherApplicationFeature` | `folderColumns` installed |
|---|---|---|
| folderwidth only, `cols == 1` | false | **NOT INSTALLED** |
| folderspace only, `cols == 1` | false | **NOT INSTALLED** |
| cols only (`cols > 1`) | true | installed |
| cols > 3 + width + space | true | installed |

### UI gate

`Launcher.kt` disables:

- `pref_key_launcher_folderwidth` unless `folder_cols > 1`
- `pref_key_launcher_folderspace` unless `folder_cols > 3`

This means a user who enables `folderwidth` through the UI normally also has `folder_cols > 1`. However, stale preferences (e.g. set `cols > 1`, enable width/space, then restore `cols == 1` without clearing the checkboxes) can leave `folderwidth == true` with `cols == 1`. In that state the feature is silently not installed.

## Hook behavior

`LauncherFolderHooks.FolderColumnsHook` is the only code path that applies `folderwidth` and `folderspace`.

### `applyFolderWidth`

```kotlin
lp.width = ViewGroup.LayoutParams.MATCH_PARENT
```

Hooked in:
- `Folder.onFinishInflate` after
- `Folder.onLayout` before
- `Folder.resetViewsLayoutParams` after

### `folderspace`

```kotlin
mBackgroundView.setPadding(left / 3, top, right / 3, bottom)
```

Applied only when `cols > 3 && launcher_folderspace`.

### `mFakeIcon` geometry

```kotlin
mFakeIcon.layout(
    contentView.left,
    contentView.top,
    contentView.right,
    contentView.top + contentView.width
)
```

The bottom coordinate uses `contentView.width` instead of `contentView.height`. This was already present in the initial `LauncherFolderHooks.kt` commit `51a0e78` and has not changed. Classification: `UNRESOLVED` without launcher source/device evidence.

## Git archaeology

| Commit | Relevance |
|---|---|
| `51a0e78` | First `LauncherFolderHooks.kt` implementation; `cols > 3` gate for `folderspace` and `mFakeIcon` layout using `contentView.width` both present from this commit. |
| `e30294f` | Privacy-folder receiver registration refactored to stable owner; no folder-width/space logic change. |
| `61c8868` | `fix(launcher): preserve folder width across layout resets` — extracted `applyFolderWidth`, added `resetViewsLayoutParams` hook, cached original padding, made `mContent` a generic `View` for width. The `cols > 3` and `mFakeIcon` geometry remain unchanged. |
| `0ffa39a` | `perf(hotpath)` — `getArgs(0)` to `getArg(0)`; no behavior change. |
| `29ba593` | `perf(launcher): harden callbacks` — added `OutOfMemoryError` rethrow; no behavior change. |

The install gate `launcher_folder_cols > 1` was introduced in `P1B-1` (`44b4c4c`) when startup family predicates were added, and has never included `folderwidth` or `folderspace`.

## Launcher ABI / ROM evidence

- ROM corpus: HyperOS 1.0.10.0 `veux` available
- Attempted extraction of `MiuiHome.apk` from `product_a`, `system_a`, `system_ext_a`: not found at `/priv-app/MiuiHome/MiuiHome.apk` or `/app/MiuiHome/MiuiHome.apk`
- `system_a` and `product_a` are non-EROFS/EXT4; directory listing via `ext4` module is possible but `MiuiHome` path could not be resolved before resource limits
- `ROM_EVIDENCE_LEVEL = UNRESOLVED`

## Hypotheses

### H1 — Install gate regression

`hasAnyLauncherApplicationFeature` and `handleLoadLauncher` should recognize `launcher_folderwidth` and `launcher_folderspace` as independent reasons to install `FolderColumnsHook`, because the UI allows them only after `folder_cols` is set but the installer gate does not persist that relationship if the user later resets `folder_cols` to 1.

- Evidence: source code; UI gate in `Launcher.kt`; install gate in `LauncherInstaller.java`.
- Counter-evidence: in normal UI flow `cols > 1` is required to enable `folderwidth`, so the gate would usually be satisfied.
- Confidence: HIGH for the gate being incomplete; MEDIUM for it explaining this specific user report.

### H2 — `cols > 3` semantic gate for `folderspace`

`folderspace` only applies when `cols > 3`. The UI enforces the same threshold, so this is consistent. If the user has `folder_cols` between 2 and 3 and expects `folderspace` to work, the UI should have prevented enabling it, so this is unlikely the primary regression.

- Evidence: code and UI both require `cols > 3`.
- Counter-evidence: not a mismatch between UI and production.
- Confidence: LOW as primary cause.

### H3 — Launcher version ABI variant

The user launcher `RELEASE-4.39.14.8060-04191512` may have a different `Folder` class layout, field types, or lifecycle. The A13 `CatalogContracts.folderColumns` only requires `onFinishInflate` and `onLayout`; `resetViewsLayoutParams` is optional. If the launcher does not declare these methods or `mContent`/`mBackgroundView` types do not match the casts, the feature silently returns.

- Evidence: launcher version string different from base corpus; no ROM APK extracted.
- Counter-evidence: code uses safe `as?` casts, so no crash implies method/field exists but may be wrong type.
- Confidence: UNRESOLVED pending launcher DEX.

### H4 — `mFakeIcon` geometry candidate

The `mFakeIcon` bottom coordinate uses `contentView.width` instead of `contentView.height`. This may be intentional (square icon) or a latent bug. Without launcher source it cannot be tied to the reported width/padding regression.

- Evidence: source code.
- Counter-evidence: unchanged since first implementation; likely intentional square placeholder.
- Confidence: UNRESOLVED.

## Primary classification

`MULTIPLE`:

1. `INSTALL_GATE_REGRESSION` — `hasAnyLauncherApplicationFeature` must include `launcher_folderwidth`/`launcher_folderspace`.
2. `LAUNCHER_VERSION_VARIANT` / `UNRESOLVED` — launcher `RELEASE-4.39.14.8060-04191512` ABI not statically verified.

## Missing test coverage

- `hasAnyLauncherStartupFeature_folderWidthOnly`
- `hasAnyLauncherStartupFeature_folderSpaceOnly`
- `folderColumns_installed_withFolderWidthOnly`
- `folderColumns_installed_withFolderSpaceOnly`
- `FolderColumnsHook_applyFolderWidth`
- `FolderColumnsHook_reduceSidePadding`
- `FolderColumnsHook_mFakeIconGeometry`
- `FolderColumnsHook_onLayoutLifecycle`
- `LauncherFolderHooks` fixtures with `mContent`, `mBackgroundView`, `mFakeIcon`

## Next minimal production corrective

Update `LauncherInstaller.java`:

- Add `launcher_folderwidth` and `launcher_folderspace` to `hasAnyLauncherApplicationFeature`.
- Add `launcher_folderwidth` and `launcher_folderspace` to the install condition in `handleLoadLauncher` so `FolderColumnsHook` is installed when either is enabled.

Expected changed files:

- `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`

Expected test files to add/update:

- `app/src/test/java/tv/withaibuild/customiuizer/installers/LauncherInstallerTest.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooksTest.kt` (new)
- `app/src/test/java/com/miui/home/launcher/Folder.kt` (expand fixture)

## Device validation

- `DEVICE_VALIDATION_REQUIRED = YES` before claiming the fix resolves the user report, because the primary evidence is source-archaeological and the launcher `RELEASE-4.39.14.8060-04191512` is not in the local ROM corpus.
