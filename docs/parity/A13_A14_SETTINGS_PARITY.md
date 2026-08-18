# A13/A14 Settings & Product Experience Parity (Phase D4)

```text
SCOPE = settings UX / search / restart UX / language-about / backup-restore
PRODUCTION_CHANGED = NO
```

## Settings structure and navigation

- **Home/grouping**: both trees keep `system`, `launcher`, `controls`, `various` top-level families; A14 has extra regrouping under control-center theme style and status-bar typography options.
- **Secondary-page wiring**: both use `SubFragment` + XML-driven category pages; A14 has more deep-link granularity.
- **Searchability**: both maintain search/navigation infrastructure; A14 exposes broader key surface (higher searchable key count).
- **Lazy loading**: both avoid eager hook install from settings UI; install remains process/phase driven in module runtime.

Classification:

```text
SETTINGS_PARITY_RESULT = PARTIAL_PARITY
SEARCH_PARITY_RESULT = PRESENT_A13_VARIANT (A14 wider index)
```

## Restart UX and restart target semantics

- A13 uses menu actions (`restartlauncher`, `restartsystemui`, `restartsecuritycenter`, `softreboot`) via broadcast-based triggers.
- A13 typed catalog has `RestartTarget`, but legacy paths still coexist.
- A14 has more centralized restart-page policy and stronger feature-level restart metadata integration.

Classification:

```text
RESTART_PARITY_RESULT = PARTIAL_PARITY
```

## Language and About

- Both have `AppLocaleController` and `AboutFragment`.
- A13 locale controller supports A13 per-app locale application and reconcile marker invalidation, but A14 line is more tightly integrated with backup/restore contract flow.
- About/settings entrypoint parity exists, but A14 integration depth is higher.

Classification:

```text
LANGUAGE_ABOUT_RESULT = PRESENT_A13_VARIANT
```

## Backup and restore parity

Observed architectural difference:

- **A14**: typed backup pipeline (`BackupFormatV2`, `BackupRestore`, migration filters, bounds/CRC, restricted legacy decoder, rollback semantics, launcher reconcile path).
- **A13**: direct Java serialization via `ObjectOutputStream`/`ObjectInputStream` inside `PreferenceFragmentBase`, no typed schema versioning or CRC integrity model, weaker rollback/integrity controls.

Classification:

```text
BACKUP_PARITY_RESULT = MISSING_IN_A13
```

## D4 discrepancy accounting

```text
UI_WITHOUT_IMPLEMENTATION = 27
IMPLEMENTATION_WITHOUT_UI = 56
SEARCH_WITHOUT_DESTINATION = included in UI_WITHOUT_IMPLEMENTATION candidates
DUPLICATE_SEMANTIC_ENTRY = needs Phase E cleanup pass
```

Phase D policy applied: documented only, no production corrective.

