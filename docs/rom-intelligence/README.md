# ROM Intelligence Workflow

This directory holds the offline ROM target intelligence infrastructure for
CustoMIUIzer A13.  It is used for cataloging, diffing, and cross-ROM target
mapping without building an APK, running ADB, or uploading files.

## Directory Layout

```
repo-root/
├── local-rom-samples/          # Ignored by git.  Place your own local samples here.
├── build/rom-intelligence/     # Ignored by git.  Inventory/diff reports generated here.
├── tools/
│   ├── rom_inventory.py        # Scan a directory and create a sample inventory.
│   └── rom_target_diff.py      # Diff two APK/JAR samples and report class/member changes.
└── docs/rom-intelligence/
    ├── README.md                       # This file.
    ├── A13_ROM_SAMPLE_CATALOG.md       # Catalog of collected and reference samples.
    ├── A13_SAMPLE_ACQUISITION.md       # How to collect samples manually (no ADB automation).
    └── A13_TARGET_MATRIX.md            # Hook target matrix from Canary/Catalog contracts.
```

## Workflow

1. **Collect** local samples into `local-rom-samples/` (see `A13_SAMPLE_ACQUISITION.md`).
2. **Catalog** them with `tools/rom_inventory.py -d local-rom-samples/ -f csv`.
3. **Compare** important pairs with `tools/rom_target_diff.py left.jar right.jar`.
4. **Update** `A13_TARGET_MATRIX.md` after verification, changing `STATIC_RESOLVED`
   to `DEVICE_HOOK_VERIFIED` or `DEVICE_BEHAVIOR_VERIFIED` when real evidence exists.

## Verification Statuses

### Catalog / inventory

- `COMPILE_STUB` — build artifacts or placeholders used only for compilation checks.
- `LOCAL_ROM_SAMPLE` — extracted from a local ROM package kept by the user.
- `DEVICE_EXTRACTED` — manually copied from a user device (no automated ADB).
- `UPSTREAM_REFERENCE` — a reference blob from an upstream source, not yet verified locally.
- `UNVERIFIED` — default; no verification performed.

### Target matrix

- `STATIC_RESOLVED` — target present in the static A13 contract source.
- `DEVICE_HOOK_VERIFIED` — hook installed and confirmed by device logs.
- `DEVICE_BEHAVIOR_VERIFIED` — behavior change confirmed on device.
- `CANDIDATE` — a plausible but unproven target.
- `MISSING` — target is absent from the sampled ROM.
- `INCOMPATIBLE` — target exists but cannot be used on this ROM.

## Rules

- No ADB, no APK build, no assemble/package/bundle/install/sign/publish.
- No real ROM blobs or samples are committed to git.
- Do not download ROM files from the internet.
- Do not connect to phones or run device automation.
