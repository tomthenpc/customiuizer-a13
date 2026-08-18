# A13 Phase D-FINAL Semantic Absence Sweep

PHASE_D_SWEEP_BASE_SHA = 48729a53796b478bfa97512ade2963f82310619c
A14_PINNED_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa

This sweep does not rebuild Phase D. The 403 INSUFFICIENT_EVIDENCE rows are untouched.
Each of the remaining MISSING_IN_A13 candidates was searched in A13 production
sources (preference key, feature id, titles, behavior tokens, catalog, installers,
nearest legacy keys) and classified from those hits.

## Counts after D-FINAL

```
MISSING_ROWS_SEMANTICALLY_AUDITED = 77 originally; 76 remain in the missing-candidate list after backup left MISSING
FALSE_MISSING_RECLASSIFIED_TOTAL = 11
PRESENT_A13_VARIANT_RECLASSIFIED = 2
PARTIAL_PARITY_RECLASSIFIED = 9 feature aliases + backup upgraded to PARTIAL after E1
TRUE_MISSING_FINAL = 65 matrix MISSING_IN_A13 rows (1 of which is HOLD)
PARTIAL_FINAL = 10
HOLD_EVIDENCE_FINAL = 1 (system_strong_toast_island_offset, Dynamic Island helper)
INSUFFICIENT_EVIDENCE = 403 (unchanged)
INTENTIONAL_EXCLUDED = 1 (Dynamic Island)
E1=1 E2=0 E3=58 E4=11 E5=4 PHASE_E_READY_GAPS=74
```

## False-missing reclassifications

| A14 key | Result | A13 counterpart |
|---|---|---|
| system_netspeed_boldfont | PRESENT_A13_VARIANT | system_netspeed_bold |
| system_statusbaricons_bluetoothicn | PRESENT_A13_VARIANT | system_statusbaricons_bluetooth option 3 |
| launcher_folderblur_disable | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | launcher_folderblur_opacity + FolderBlurHook |
| system_netspeed_use_clock_style | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | NetSpeedTypefaceHelper |
| system_statusbarcontrols_dt_left | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | system_statusbarcontrols_dt |
| system_statusbarcontrols_dt_right | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | system_statusbarcontrols_dt |
| system_charginginfo_fontsize | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | system_charginginfo family |
| system_statusbar_dualrows_left_ratio | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | system_statusbar_dualrows |
| system_statusbaricons_wireless_headset | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | system_statusbaricons_headset |
| system_usb_default_function | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | system_defaultusb (kept) |
| system_detailednetspeed_style | PARTIAL_PARITY / UPGRADE_EXISTING_A13 | detailednetspeed family (kept) |

## Inspected and rejected near-misses (remain MISSING)

- launcher_dock_height vs dock top/bottom margin
- various_installer_purify vs various_miuiinstaller
- various_disable_reset_recents_privacy_blur vs system_recents_blur
- system_statusbaricons_privacy_prompt vs system_statusbaricons_privacy
- system_hidestatusbar_whenscreenrecord vs whenscreenshot
- system_autobrightness_reset_when_screenoff vs autobrightness range
- system_cc_freeform_when_longclick vs existing CC hooks
- volume DND/mute shortcut hide vs MIUIVolumeDialogHook autohide/blur
- system_disable_window_blurs vs recents/folder blur
- system_force_darken_allapps vs a one-off force_dark=false write

## Phase E entry (after sweep)

Ready gaps are MISSING_IN_A13 or PARTIAL_PARITY with resolved host/process,
except HOLD_EVIDENCE rows.

USB default remains UPGRADE_EXISTING_A13 on the existing USBConfigHook path.
Backup remains the E1 gap: upgrade PreferenceFragmentBase off ObjectOutputStream.
