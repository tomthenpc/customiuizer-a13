# A13 Sample Acquisition

This document describes how to populate `local-rom-samples/` with real ROM
files **without using ADB automation** and **without downloading ROM files from
the internet**.  All samples must come from your own local collection or device.

## What to collect

Focus on APKs and JARs that contain the classes listed in
`A13_TARGET_MATRIX.md`:

- `framework.jar` / `services.jar` / `systemui.jar` (system_server / SystemUI)
- `miuiui.jar` / `MiuiSystemUI.apk` / `MiuiHome.apk` (MIUI/HyperOS custom layers)
- `Settings.apk`, `framework-res.apk` (reference resources)

## Safe local sources

1. **Local firmware archives you already own.**  Official full ROM zips can be
   extracted with `payload-dumper-go` or similar tools to obtain `/system` files.
2. **TWRP or recovery backups.**  Mount the device storage manually and copy
   `/system/framework` or `/system/app` files to a PC via USB mass storage or an
   SD card.
3. **Rooted device file manager.**  On a rooted device, use a terminal or file
   manager to copy target files to an accessible folder, then transfer them to
   the PC using normal file transfer (not ADB scripts).

## Prohibited

- ADB pull / push / shell scripts
- Automated device collection
- Downloading ROM files from the internet
- Committing the raw samples to git (`*.rom-sample` and `local-rom-samples/` are ignored)

## Directory convention

Place samples under `local-rom-samples/` using a layout that makes the origin
clear:

```
local-rom-samples/
├── miui14-a13-<device>-<build>/
│   ├── framework.jar
│   ├── services.jar
│   └── MiuiSystemUI.apk
└── hyperos1-a13-<device>-<build>/
    ├── framework.jar
    ├── services.jar
    └── MiuiSystemUI.apk
```

After copying files, run:

```bash
python tools/rom_inventory.py -d local-rom-samples/ -f csv
```

Then paste the generated row into `docs/rom-intelligence/A13_ROM_SAMPLE_CATALOG.md`.
