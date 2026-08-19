# Issue #1 Launcher Folder Regression Evidence

## Stage E2 status

- `STAGE_E2 = PASS` for static evidence closure
- `PRODUCTION_AUTHORIZATION = NO`
- `PRODUCTION_CHANGED = false`
- Evidence type: exact public APK static analysis only (`ROM evidence != DEVICE evidence`).

| Gate field | Value |
|---|---|
| `STATIC_ABI` | `VERIFIED` |
| `STATIC_LIFECYCLE_CORRECTIVE` | `SUPPORTED` |
| `PRODUCTION_CORRECTIVE_REQUIRED` | `NO` |
| `DEVICE_VERIFIED` | `NO` |
| `RUNTIME_RESOLUTION_CLAIM` | `NO` |
| `ISSUE_1_STATIC_STATE` | `61C8868_CORRECTIVE_SUPPORTED_ON_EXACT_LAUNCHER` |
| `STATIC_CLASSIFICATION` | `CORRECTIVE_ALREADY_PRESENT_AND_STATICALLY_SUPPORTED` |
| `RUNTIME_CLASSIFICATION` | `UNVERIFIED` |

No production changes are made; runtime proof requires device validation.

## Exact artifact

| Field | Value |
|---|---|
| `PACKAGE` | `com.miui.home` |
| `VERSION_NAME` | `RELEASE-4.39.14.8060-04191512` |
| `VERSION_CODE` | `439148060` |
| `APK_SIZE` | `24782235` |
| `APK_SHA256` | `b507f1cbf2d8fbc445398a2402ff9dd3f22580265b0ef9de07b4b37889b3384b` |
| `CERT_SHA256` | `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025` |
| `SIGNATURE_VARIANT` | `7b6d` |
| `ARTIFACT_SOURCE` | MemeOS Updates public mirror (APKMemeOS.com download link) |
| `EXACT_VERSION` | `YES` (size, SHA-256, certificate, version name and version code all match user-supplied metadata) |

The APK was downloaded to the external evidence cache and verified. It is **not** tracked in the repository.

## History correction

| Item | Value |
|---|---|
| `FEATURE_PRESENT_AT_INITIAL_REPOSITORY_BASELINE` | `YES` |
| `INITIAL_REPOSITORY_BASELINE_SHA` | `c696d33b071ff367e1ebb7c9b5777cd4a3b0a37c` |
| `REAL_FEATURE_INTRODUCTION_SHA` | `UNRESOLVED_PRE_REPOSITORY_HISTORY` |
| `c81ea42_CLASSIFICATION` | `JAVA_TO_KOTLIN_SPLIT_MIGRATION` |
| `51a0e78_CLASSIFICATION` | `NAMESPACE_RENAME` |
| `ISSUE_CREATED` | `2026-07-30` (user-supplied; not from git history) |
| `61c8868` | `2026-07-31` |
| `44b4c4c_POSTDATES_ISSUE` | `YES` |
| `31e48bd_POSTDATES_ISSUE` | `YES` |
| `INSTALL_GATE_CAUSAL_TO_ORIGINAL_ISSUE` | `NO / UNRESOLVED` |

`c696d33b071ff367e1ebb7c9b5777cd4a3b0a37c` (`2026-07-27`, "Initial baseline: r13.1.2") is the root commit of this repository. Its `mods/Launcher.java` already contains the `FolderColumnsHook` implementation: `mContent` cast to `GridView`, `numColumns` set, `MATCH_PARENT` width, `mBackgroundView` padding divided by 3, and the `mFakeIcon` `mContent.getTop() + mContent.getWidth()` geometry. Therefore the feature is present at the initial repository baseline; the real first introduction is outside the current git history.

`c81ea42` (`2026-07-29`, "K3: split and migrate mods/Launcher.java to Kotlin framework") only split the existing `Launcher.java` into multiple Kotlin files, including `LauncherFolderHooks.kt`. It is a Java-to-Kotlin migration, not the feature introduction.

`51a0e78` is the A13 namespace migration (`name.monwf.customiuizer` → `tv.withaibuild.customiuizer`) and did not introduce the feature.

The launcher startup/install gates (`44b4c4c` and `31e48bd`) were committed on `2026-08-06`, both after the issue date (`2026-07-30`). They therefore cannot be the original cause of the reported regression, although they remain a robustness gap: `hasAnyLauncherApplicationFeature` does not consider `launcher_folderwidth` or `launcher_folderspace` as independent install reasons.

## Exact launcher ABI

| Member | DEX evidence |
|---|---|
| `Folder` class | `Lcom/miui/home/launcher/Folder;` |
| `SUPERCLASS` | `Landroid/widget/LinearLayout;` |
| `onFinishInflate` | `()V` protected |
| `onLayout` | `(Z I I I I)V` protected |
| `resetViewsLayoutParams` | `()V` public |
| `mContent` | `Lcom/miui/home/launcher/FolderGridView;` (extends `Landroid/widget/GridView;`) |
| `mBackgroundView` | `Landroid/view/ViewGroup;` |
| `mFakeIcon` | `Landroid/widget/ImageView;` |

`FolderGridView` was also confirmed in the same DEX and its superclass is `Landroid/widget/GridView;`.

## Old implementation (pre-61c8868)

Pre-`61c8868`, the same logic was already present in the initial baseline `c696d33` (Java `Launcher.java`) and carried unchanged through `c81ea42` (Kotlin split):

```java
GridView mContent = (GridView) XposedHelpers.getObjectField(param.getThisObject(), "mContent");
mContent.setNumColumns(cols);
```

| Check | Result |
|---|---|
| `OLD_mContent_GRIDVIEW_CAST` | `MATCH` for the exact artifact (`mContent` is a `GridView` subclass) |
| `OLD_WIDTH_WRITE_PATH` | `mContent.layoutParams.width = MATCH_PARENT` only in `onFinishInflate(after)` |
| `OLD_PADDING_WRITE_PATH` | `mBackgroundView.setPadding(left/3, top, right/3, bottom)` only in `onFinishInflate(after)`, no original padding cache |

## 61c8868 corrective assessment

`61c8868` (`fix(launcher): preserve folder width across layout resets`) made four key changes:

1. Treats `mContent` as a generic `View` for width (`as? View`).
2. Extracts `applyFolderWidth()` and calls it in `onLayout(before)`.
3. Adds a `resetViewsLayoutParams(after)` hook that re-applies `MATCH_PARENT`.
4. Caches the original `mBackgroundView` left/right padding before dividing by 3.

| Check | Result |
|---|---|
| `GENERIC_VIEW_CHANGE` | `RELEVANT` — makes width application independent of `mContent` runtime type |
| `ONLAYOUT_BEFORE_REAPPLY` | `RELEVANT` — re-applies width before `super.onLayout` is called |
| `RESET_LAYOUTPARAMS_AFTER` | `RELEVANT` — the exact launcher `resetViewsLayoutParams` sets `mContent.getLayoutParams().width` to a fixed dimension resource; the after-hook overwrites it back to `MATCH_PARENT` |
| `ORIGINAL_PADDING_CACHE` | `RELEVANT` — no launcher call resets `mBackgroundView` padding, but the cache prevents repeated re-inflation from compounding the `/3` shrink |
| `61c8868_STATIC_RESULT` | `SUPPORTED` for the exact artifact |

### Why `resetViewsLayoutParams` matters

Decompilation of the exact artifact shows the launcher writes:

```java
this.mContent.setPadding(...resource ids...);
android.widget.FrameLayout$LayoutParams lp = (android.widget.FrameLayout$LayoutParams) this.mContent.getLayoutParams();
lp.width = this.getResources().getDimensionPixelSize(2131165625);  // fixed width
this.mContent.requestLayout();
```

Without the `resetViewsLayoutParams` hook, the launcher would reset `mContent` width to a fixed value after `onFinishInflate`. `61c8868` re-applies `MATCH_PARENT` immediately after this method returns.

## Current lifecycle

| Aspect | Static finding |
|---|---|
| `WIDTH_WRITERS` | Launcher `resetViewsLayoutParams` sets `mContent` layout width to a fixed dimen; module `applyFolderWidth` sets it to `MATCH_PARENT` |
| `WIDTH_OVERWRITE_AFTER_MODULE` | `resetViewsLayoutParams` runs, then the module after-hook overwrites it to `MATCH_PARENT`; `onLayout` itself does not write width, but the module before-hook re-applies `MATCH_PARENT` before `super.onLayout` |
| `PADDING_WRITERS` | Launcher `resetViewsLayoutParams` sets `mContent` padding; module sets `mBackgroundView` padding |
| `PADDING_OVERWRITE_AFTER_MODULE` | Launcher does not modify `mBackgroundView` padding, so module padding persists; launcher does overwrite `mContent` padding, but the module does not touch `mContent` padding |
| `VISUAL_SPACING_TARGET` | `mBackgroundView` is the module's spacing target; `mContent` padding/horizontalSpacing/verticalSpacing are launcher-managed and are not reduced by the module |

## `mFakeIcon` geometry

The module positions `mFakeIcon` as:

```kotlin
mFakeIcon.layout(contentView.left, contentView.top, contentView.right, contentView.top + contentView.width)
```

The exact launcher `onLayout` positions it as:

```java
mFakeIcon.layout(
    mContent.getLeft()  (+/- fold padding adjustments),
    mContent.getTop(),
    mContent.getRight() (+/- fold padding adjustments),
    mContent.getTop() + mContentRect.width()
);
```

| Check | Result |
|---|---|
| `GEOMETRY` | `EXPECTED` for non-fold devices; `BUG_CANDIDATE` only when `DeviceConfig.isInFoldLargeScreen(context)` is true, because the module does not subtract `mContent` left/right padding |
| `ISSUE_1_RELEVANCE` | `NO` — `mFakeIcon` is the opening/closing animation placeholder, not the visible folder grid width or spacing |

## Installer classification

| Field | Value |
|---|---|
| `ROBUSTNESS_GAP` | `STATIC_VERIFIED` |
| `ISSUE_1_CAUSAL` | `NO_EVIDENCE` — the install gate is incomplete but post-dates the issue and cannot explain the original regression |
| `PRODUCTION_ACTION_THIS_BATCH` | `NONE` |

## Issue #4

See `A13-Issue-4-scope.md`. Classification remains `OUT_OF_SCOPE_ROM_GENERATION`; no A13 or A14 action; no route to A14 audit; permanently parked.

## Root cause classification (Stage E2)

| Field | Value |
|---|---|
| `STATIC_CLASSIFICATION` | `CORRECTIVE_ALREADY_PRESENT_AND_STATICALLY_SUPPORTED` |
| `RUNTIME_CLASSIFICATION` | `UNVERIFIED` |
| `FIRST_STATIC_BREAKPOINT` | None identified for the exact artifact |
| `NEXT_MINIMAL_PRODUCTION_CORRECTIVE` | None required for the exact launcher version; installer gate remains a non-causal robustness gap and is not changed in this batch |
| `PRODUCTION_AUTHORIZATION_REQUEST` | `NO` |
| `DEVICE_VALIDATION_REQUIRED` | `YES` before claiming runtime resolution on the user's device |

`61c8868` is already present in the branch and the exact launcher ABI statically supports the corrective. This does not constitute runtime proof on the user's device.

## Verification

Run during Stage E2:

| Command | Result |
|---|---|
| `python tools/verify.py fast --changed` | `OK` |
| `python tools/verify.py full` | `OK` (compileDebugKotlin, compileDebugJavaWithJavac, testDebugUnitTest, lintDebug) |
| `python -m compileall tools` | `OK` |
| `python -m unittest discover -s tools/tests -p "test_*.py"` | `OK (1267 tests, skipped=2)` |
| `git diff --check` | `OK` |

`PRODUCTION_CHANGED = false`

`LOCKED_XAGA_RAW_SUPER = DELETED` (the previously locked `C:\Home\xiaomi\rom\A13\_analysis_work\xaga_raw_super.img` was confirmed unlocked and removed)

## Derived metadata

- `docs/rom-intelligence/A13_dex_launcher_4.39.14.8060.json` — bounded DEX summary; no APK, DEX, smali, or JADX dump.
