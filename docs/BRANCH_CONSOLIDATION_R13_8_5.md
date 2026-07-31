# Branch Consolidation Ledger — A13 r13.8.5

**Baseline candidate:** `origin/fix/r13.8.3-ui-text-inheritance-and-about-wrap`

This ledger compares each tracked branch against the proposed release base. A branch is *contained* when it is an ancestor of the baseline. A branch is *patch-equivalent* when its changes are already reflected in the baseline (via cherry-pick deduplication, patch-id match, or final-tree analysis), even if the exact commits are not the same.

## Summary table

| Branch | Tip SHA | Merge Base | Ahead | Behind | Contained | Patch-Equivalent | Independent Commits | Independent Files | Final Treatment |
|---|---|---|---|---:|---:|---|---|---|---|---|
| `origin/fix/r13.8.3-ui-text-inheritance-and-about-wrap` | `b123099ef4df` | `b123099ef4df` | 0 | 0 | Yes | Yes | _none_ | _none_ | baseline |
| `local/backup/r13-k5-before-system-java-removal` | `8df0c3ded351` | `8df0c3ded351` | 0 | 127 | Yes | Yes | _none_ | _none_ | backup-only |
| `origin/codex/r13.7-ui-polish` | `66f22a7db00f` | `66f22a7db00f` | 0 | 57 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.7.1-maintenance-foundation` | `a483120b6dee` | `a483120b6dee` | 0 | 48 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-catalog-expansion-batch-1` | `8b0fb87654d3` | `8b0fb87654d3` | 0 | 26 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-catalog-expansion-batch-2` | `7514eb397467` | `7514eb397467` | 0 | 21 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-catalog-expansion-batch-3` | `b0fd3e910b92` | `b0fd3e910b92` | 0 | 15 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-catalog-final-review` | `e0263d98ee87` | `e0263d98ee87` | 0 | 13 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-install-evidence` | `5de26061a0c3` | `5de26061a0c3` | 0 | 33 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-install-evidence-correctness` | `fd85e44f1461` | `fd85e44f1461` | 0 | 31 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-issue-1-network-folder` | `80d3d6032b6c` | `80d3d6032b6c` | 0 | 5 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-maintenance-architecture` | `92bfaee5385e` | `92bfaee5385e` | 0 | 35 | Yes | Yes | _none_ | _none_ | contained |
| `origin/devin/r13.8-scope-and-stability` | `28e88ea7f203` | `28e88ea7f203` | 0 | 10 | Yes | Yes | _none_ | _none_ | contained |
| `origin/fix/r13.8-ui-text-inheritance-and-about-wrap` | `45ea706c8b00` | `66f22a7db00f` | 1 | 57 | No | Yes (superseded) | 45ea706 fix(ui): preserve text styling and wrap about attribution | app/src/main/res/layout/fragment_about_head.xml<br>tools/check-invariants.py | patch-equivalent (superseded) |
| `origin/main` | `66f22a7db00f` | `66f22a7db00f` | 0 | 57 | Yes | Yes | _none_ | _none_ | contained |

## Key conclusion

No branches require a merge. Every tracked branch is either already contained in the baseline or is patch-equivalent/superseded. The r13.8.5 consolidation target can be built directly from `origin/fix/r13.8.3-ui-text-inheritance-and-about-wrap` (`b123099ef4df`).

## Per-branch records

### `origin/fix/r13.8.3-ui-text-inheritance-and-about-wrap`

- **Tip SHA:** `b123099ef4df5f55feae3437ba2a90f762959bb2`
- **Merge base with baseline:** `b123099ef4df5f55feae3437ba2a90f762959bb2`
- **Ahead:** 0
- **Behind:** 0
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** baseline

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `local/backup/r13-k5-before-system-java-removal`

- **Tip SHA:** `8df0c3ded351c4cf2a0401a0a470d00103c2ad76`
- **Merge base with baseline:** `8df0c3ded351c4cf2a0401a0a470d00103c2ad76`
- **Ahead:** 0
- **Behind:** 127
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** backup-only

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/codex/r13.7-ui-polish`

- **Tip SHA:** `66f22a7db00f238d45d396e4daca78ffdc820ad1`
- **Merge base with baseline:** `66f22a7db00f238d45d396e4daca78ffdc820ad1`
- **Ahead:** 0
- **Behind:** 57
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.7.1-maintenance-foundation`

- **Tip SHA:** `a483120b6dee04a938b4f39247934cad95cadeea`
- **Merge base with baseline:** `a483120b6dee04a938b4f39247934cad95cadeea`
- **Ahead:** 0
- **Behind:** 48
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-catalog-expansion-batch-1`

- **Tip SHA:** `8b0fb87654d32a42f4212a3cd3f40f103f3d92c5`
- **Merge base with baseline:** `8b0fb87654d32a42f4212a3cd3f40f103f3d92c5`
- **Ahead:** 0
- **Behind:** 26
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-catalog-expansion-batch-2`

- **Tip SHA:** `7514eb397467ef9b31587c5fd1478308b87027f3`
- **Merge base with baseline:** `7514eb397467ef9b31587c5fd1478308b87027f3`
- **Ahead:** 0
- **Behind:** 21
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-catalog-expansion-batch-3`

- **Tip SHA:** `b0fd3e910b92feeac13482761ffdcdd479d64ce1`
- **Merge base with baseline:** `b0fd3e910b92feeac13482761ffdcdd479d64ce1`
- **Ahead:** 0
- **Behind:** 15
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-catalog-final-review`

- **Tip SHA:** `e0263d98ee87a05067cb1dffe79c2b7d9d012df0`
- **Merge base with baseline:** `e0263d98ee87a05067cb1dffe79c2b7d9d012df0`
- **Ahead:** 0
- **Behind:** 13
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-install-evidence`

- **Tip SHA:** `5de26061a0c37dc7f34981f1af643edd645fb393`
- **Merge base with baseline:** `5de26061a0c37dc7f34981f1af643edd645fb393`
- **Ahead:** 0
- **Behind:** 33
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-install-evidence-correctness`

- **Tip SHA:** `fd85e44f1461589d1f8a1a5937d7090560b1fe08`
- **Merge base with baseline:** `fd85e44f1461589d1f8a1a5937d7090560b1fe08`
- **Ahead:** 0
- **Behind:** 31
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-issue-1-network-folder`

- **Tip SHA:** `80d3d6032b6c8dc7057dbe42fb1639fcfb28c1f8`
- **Merge base with baseline:** `80d3d6032b6c8dc7057dbe42fb1639fcfb28c1f8`
- **Ahead:** 0
- **Behind:** 5
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-maintenance-architecture`

- **Tip SHA:** `92bfaee5385e60df673a4a7d38f598b81431074c`
- **Merge base with baseline:** `92bfaee5385e60df673a4a7d38f598b81431074c`
- **Ahead:** 0
- **Behind:** 35
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/devin/r13.8-scope-and-stability`

- **Tip SHA:** `28e88ea7f203d7236e4d456aaf7c11f3dd1632b8`
- **Merge base with baseline:** `28e88ea7f203d7236e4d456aaf7c11f3dd1632b8`
- **Ahead:** 0
- **Behind:** 10
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_

### `origin/fix/r13.8-ui-text-inheritance-and-about-wrap`

- **Tip SHA:** `45ea706c8b00d886b68e08f13755033a990b1ed7`
- **Merge base with baseline:** `66f22a7db00f238d45d396e4daca78ffdc820ad1`
- **Ahead:** 1
- **Behind:** 57
- **Contained:** No
- **Patch-equivalent:** Yes (superseded) — superseded: about layout patch-id is identical; check-invariants was rewritten into a stricter superset
- **Final treatment:** patch-equivalent (superseded)

#### Independent commits after cherry-pick
```
45ea706 fix(ui): preserve text styling and wrap about attribution
```

#### Independent files
- `app/src/main/res/layout/fragment_about_head.xml`
- `tools/check-invariants.py`

#### Changed files since merge-base
- `M` app/src/main/res/layout/fragment_about_head.xml
- `M` tools/check-invariants.py

#### File-level cumulative patch-id comparison
- `app/src/main/res/layout/fragment_about_head.xml`: branch PID `f471c2d68ddcfc86627b3152c8388e88b988f07e`, baseline PID `f471c2d68ddcfc86627b3152c8388e88b988f07e` — identical
- `tools/check-invariants.py`: branch PID `f5a4537937d1cbf1c5c6acd5d0760f813568e31d`, baseline PID `c3d4e3c4ac1d1aab3f4c990c1f3f564d10d7fbd1` — divergent

#### Final-tree / patch-id comparison with baseline

Cumulative patch-IDs of files changed by this branch (merge-base → branch tip vs merge-base → baseline tip):
- `app/src/main/res/layout/fragment_about_head.xml`: branch PID `f471c2d68ddcfc86627b3152c8388e88b988f07e`, baseline PID `f471c2d68ddcfc86627b3152c8388e88b988f07e` — identical
- `tools/check-invariants.py`: branch PID `f5a4537937d1cbf1c5c6acd5d0760f813568e31d`, baseline PID `c3d4e3c4ac1d1aab3f4c990c1f3f564d10d7fbd1` — divergent (baseline is a stricter superset)

`git diff --stat --no-renames merge-base..older`:
```
 app/src/main/res/layout/fragment_about_head.xml |  6 ----
 tools/check-invariants.py                       | 43 +++++++++++++++++++++++++
 2 files changed, 43 insertions(+), 6 deletions(-)
```

`git diff --no-renames --numstat baseline older` (tip-to-tip) for the changed files:
```
0	0	app/src/main/res/layout/fragment_about_head.xml  (identical at tips)
20	242	tools/check-invariants.py
```

Conclusion: The `fragment_about_head.xml` change is byte-for-byte identical in the baseline. `tools/check-invariants.py` diverges only because the baseline rewrote the rule into a stricter, more complete implementation. The older branch is therefore superseded; merging it would revert baseline improvements. No merge required.

#### Raw `git log --left-right --cherry-pick --oneline --no-decorate` output
```
< b123099 test(ui): make text invariant failures meaningful
< 9853ad5 fix(ui): complete about wrapping and harden text invariants
< e919db2 chore(version): bump A13 debug build to r13.8.3
< a1b849a test(invariants): enforce preference style defaults
< e0d87c0 fix(ui): wrap about attribution on narrow screens
> 45ea706 fix(ui): preserve text styling and wrap about attribution
< 80d3d60 fix(i18n): complete A13 network speed row spacing translations
< e19c2e1 fix(systemui): preserve native network speed typeface
< 96b27f1 chore(debug): trace A13 network speed initialization
< 61c8868 fix(launcher): preserve folder width across layout resets
< 35428cd feat(systemui): add adjustable dual-row network speed spacing
< 28e88ea fix(debug): identify r13.8.0 test build in metadata and logs
< 874f758 chore(release): bump to r13.8.0 and add per-process load marker
< 81aec5b test(xposed): prevent system scope regression
< e0263d9 docs: add R13 catalog final review and MIUI 14 smoke test plan
< abfeed0 fix(catalog): align canary compatibility checks with contracts
< b0fd3e9 fix(catalog): preserve package-specific hotseat hook behavior
< 8016915 tools: extend catalog audits for batch-3
< 0f58e56 tests: add batch-3 catalog test coverage
< d322dc6 MainModule: migrate batch-3 features to FeatureCatalog
< 535f750 catalog: add batch-3 specs, contracts, schema and diagnostic IDs
< 96898f0 docs: plan catalog expansion batch 3
< 7514eb3 tools: extend catalog audits for batch-2
< 66eb134 tests: add batch-2 catalog test coverage
< 886a5fd MainModule: migrate batch-2 features to FeatureCatalog
< 67a9085 catalog: add batch-2 specs, contracts, schema and diagnostic IDs
< 89414f8 docs: plan catalog expansion batch 2
< 8b0fb87 tools: extend catalog audits for batch-1
< 1fb3dc7 tests: add batch-1 catalog test coverage
< 224b347 MainModule: migrate batch-1 features to FeatureCatalog
< 6b5524a catalog: add batch-1 feature specs, contracts, schema and diagnostic IDs
< ccdac04 docs: plan first catalog expansion batch
< fd85e44 ci: fetch full git history so baseline-ref audits can resolve
< 4c80c2c refactor(hook): unify contract model, evidence evaluator and session lifecycle
< 5de2606 fix(installer): do not count unselected fallback-group targets as failures
< a6037c3 feat(catalog): real hook install evidence with typed contracts and diagnostics
< 92bfaee Harden feature catalog, diagnostics and canary migration, and update tests/audits.
< 1ff4fa2 chore(cleanup): remove unused Helpers.isUPlus()
< aab22a7 feat(catalog): type-safe FeatureCatalog and StatusBarClockTweak closed-loop with compatibility probe
< c5f1176 feat(resolver): strengthen HookTargetResolver with candidate resolution and DiagnosticRecorder logging
< 82de337 feat(prefs): strengthen PreferenceSchema with type-safe constraints, RestartTarget and ownerFeature validation
< fcd5575 feat(types): add type-safe ProcessTarget, RestartTarget, CompatibilityState, FeatureRuntime, FeatureSpec, PreferenceConstraint
< c6cfae2 feat(diagnostics): strengthen DiagnosticRecorder with monotonic clock and severity-aware throttling
< 8ea2462 docs(audit): add broadcast/reboot/security and A14 mis-port audit report
< ebabdb9 Add PreferenceSchema, audit script and unit tests
< 817df9c feat(catalog): introduce FeatureCatalog with PackagePermissions and StatusBarClockTweak
< 2521880 Add HookTargetResolver for lightweight hook target caching
< fcefecb Add lightweight privacy-safe diagnostics skeleton
< a483120 chore(hooks): remove unused Helpers imports from hook modules
< 5392252 docs: record second-ticker weak-reference fix in cross-version ledger
< 3a291bf fix(lifecycle): break strong controller reference in statusbar second-ticker runnable
< 7ffdf91 docs: finalize cross-version ledger, release manifest and HookUtils/Helpers boundary comments
< 7cf3f1e refactor(lifecycle): make StepCounter and statusbar second-ticker state testable and add unit tests
< d85c52a docs: add release-manifest and r13.7.1 audit plan
< cbf2ad3 feat(lifecycle): optimize StepCounterController and statusbar second-hand lifecycle
< d70727d feat(hooks): extract lightweight utilities into HookUtils and redirect all mod call sites
< 5685761 docs: add CROSS_VERSION_PORTING_LEDGER for A13/A14 sync
```

### `origin/main`

- **Tip SHA:** `66f22a7db00f238d45d396e4daca78ffdc820ad1`
- **Merge base with baseline:** `66f22a7db00f238d45d396e4daca78ffdc820ad1`
- **Ahead:** 0
- **Behind:** 57
- **Contained:** Yes
- **Patch-equivalent:** Yes — contained (ancestor of baseline)
- **Final treatment:** contained

#### Independent commits after cherry-pick
```
(none)
```

#### Independent files
_none_

#### Changed files since merge-base
_none_
