# A13_PHASE_E_UNATTENDED_COMPLETION_REPORT

PHASE_D_SWEEP_BASE_SHA = 48729a53796b478bfa97512ade2963f82310619c
PHASE_D_FINAL_FREEZE_SHA = 1c18972
A14_PINNED_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa

UNATTENDED_MODE = ON
FAST_FORWARD = ON
PHASE_F_STARTED = NO

This report SHA is not recorded here. LOCAL_HEAD below is the last production/test commit before this document.

---

## D-FINAL semantic absence sweep

MISSING_ROWS_SEMANTICALLY_AUDITED = 75 remaining MISSING candidates (77 including later-reclassified backup)
FALSE_MISSING_RECLASSIFIED_TOTAL = 11
PRESENT_A13_VARIANT_RECLASSIFIED = 2
PARTIAL_PARITY_RECLASSIFIED = 9 feature aliases + backup after E1
TRUE_MISSING_FINAL = 65 matrix MISSING_IN_A13 rows at freeze (1 of which HOLD)
PARTIAL_FINAL = 10
HOLD_EVIDENCE_FINAL = 1 at freeze (`system_strong_toast_island_offset`)
INSUFFICIENT_EVIDENCE = 403 (untouched)
INTENTIONAL_EXCLUDED = 1 (Dynamic Island)

403 INSUFFICIENT_EVIDENCE rows were not rebuilt. Phase A/B/C architecture was not revisited.

### False-missing reclassifications (no new parallel features)

| A14 key | Result | A13 counterpart |
|---|---|---|
| system_netspeed_boldfont | PRESENT_A13_VARIANT | system_netspeed_bold |
| system_statusbaricons_bluetoothicn | PRESENT_A13_VARIANT | system_statusbaricons_bluetooth option 3 |
| launcher_folderblur_disable | PARTIAL / UPGRADE | launcher_folderblur_opacity + FolderBlurHook |
| system_netspeed_use_clock_style | PARTIAL / UPGRADE | NetSpeedTypefaceHelper |
| system_statusbarcontrols_dt_left / _dt_right | PARTIAL / UPGRADE | system_statusbarcontrols_dt |
| system_charginginfo_fontsize | PARTIAL / UPGRADE | system_charginginfo family |
| system_statusbar_dualrows_left_ratio | PARTIAL / UPGRADE | system_statusbar_dualrows |
| system_statusbaricons_wireless_headset | PARTIAL / UPGRADE | headset slot hide |
| system_usb_default_function | PARTIAL / UPGRADE | system_defaultusb |
| system_detailednetspeed_style | PARTIAL / UPGRADE | detailednetspeed family kept |
| infra.backup_restore | PARTIAL / UPGRADE | after E1 V2 backup |

---

## Batch completion

E1_COMPLETED = YES
E2_COMPLETED = YES (count became 0 after routing; no manufactured work)
E3_COMPLETED = PARTIAL
E4_COMPLETED = PARTIAL
E5_COMPLETED = PARTIAL

E3/E4/E5 remaining proven gaps are carried to Phase F. They were not implemented when preflight showed ROM/API33 risk, missing A14 production hooks, or a required parallel path.

---

## FEATURES_PORTED

- `controls_hide_ime_dismiss_button` — NavigationBarView.updateNavButtonIcons after-hook; gestural IME back-alt only
- `launcher_dock_height` — DeviceConfig.calcHotSeatsHeight when > 60dp
- `various_installer_purify` — packageinstaller SharedPreferences/Settings gates + optional SafeMode tip hide
- `various_hide_report_ondetails` — ApplicationsDetailsActivity menu item 4

## EXISTING_A13_FEATURES_UPGRADED

- Backup/restore V2 typed CUI2 format, CRC, bounds, restricted legacy decoder, rollback, locale/launcher reconcile, app-selection sanitation (`r13bak_` prefix)
- Folder blur disable flag that preserves stored opacity
- Wireless headset status-bar slot hide
- Lock-screen charging-info font size
- Network-speed clock TextAppearance
- Dual-row left/right weights for no-cutout devices
- USB default: map A14 numeric/charging values onto existing `USBConfigHook` / `system_defaultusb`

## FALSE_GAPS_REJECTED

11 D-FINAL false-missing reclassifications. No second USB subsystem. No second backup format. No Dynamic Island. `various_miuiinstaller` was not treated as purify.

---

## USB_DEFAULT_RESULT

UPGRADE_EXISTING_A13.

Kept `system_defaultusb`, `USBConfigHook`, `USBConfigSettingsHook`.
A14 `UsbHandlerHal.setEnabledFunctions(JZI)` was not copied: that is an Android 14 HAL boundary, not an A13/API33-safe rewrite.
`UsbDefaultFunctionMapper` translates A14 modes `0/1/charging` → follow/charge, `2` → `mtp`, `3` → `ptp`.

STATIC_VERIFIED = YES
BUILD_VERIFIED = YES
DEVICE_VERIFIED = NO

## BACKUP_RESULT

UPGRADE_EXISTING_A13.

V2 typed format with version/CRC/bounds. Legacy ObjectOutputStream backups decode through a restricted decoder (no `ObjectInputStream`). Restore rolls back on commit failure. A14 USB/netspeed keys alias to A13 names. Filename prefix `r13bak_`.

STATIC_VERIFIED = YES
BUILD_VERIFIED = YES
DEVICE_VERIFIED = NO

---

## Held for Phase F (not implemented)

HOLD_EVIDENCE / high-risk remainder:

- `system_strong_toast_island_offset` — Dynamic Island helper; not a Phase E gap
- `system_hidestatusbar_whenscreenrecord` — A14 has UI/strings only, no production hook
- E3 remainder: CC tile/card/slider colors, CC hide-edit (HyperOS plugin distributor), digital signal family, drawer date family, dt left/right split, volume shortcut colors, strong-toast mode, recents card style, wallpaper color mode, privacy indicator
- E4 remainder: daemon/analytics/antivirus/marketing/location+notification prompt dismiss, recents privacy-blur persist
- E5 remainder: `system_disable_window_blurs`, `system_force_darken_allapps`, `system_autobrightness_reset_when_screenoff` (system_server; no speculative ROM fallback)

`system_detailednetspeed_style` stays PARTIAL on the existing detailed/fakedualrow toggles. Replacing them with an A14 list selector would migrate live A13 prefs and was deferred.

---

## Commits

PRODUCTION_COMMITS =
- 57c40f6 feat(settings): port A13 backup V2 typed format with legacy restore
- 9650800 feat(launcher): add folder blur disable without discarding opacity
- 3211dd3 feat(systemui): add E3 status bar visual upgrades
- 0a8b0bd feat(launcher,controls): add dock height and hide IME dismiss
- 415023e feat(system_server): map A14 USB default modes onto existing A13 hook
- 813f304 feat(various): add installer purify and hide app-details report

TEST_COMMITS =
- adb01b4 test(audit): refresh hook inventories after Phase E ports

Also on this branch after the D freeze (not Phase E feature ports):
- e1ba9b5 build: add Gradle 9 plugin classpath checksums required by dependency verification
- 1c18972 docs(parity): D-FINAL semantic absence sweep for remaining missing rows

---

## Validation

TARGETED_TESTS = YES
- BackupFormatV2Test / BackupRestoreTest / AppSelectionSanitizerTest
- FolderBlurDisableTest
- ChargingInfoFontSizeTest
- DualRowsLeftRatioTest
- NetSpeedClockStyleTest
- DockHeightOverrideTest
- HideImeDismissButtonTest
- UsbDefaultFunctionMapperTest
- PackageInstallerPurifyTest / HideReportButtonTest
- `python tools/verify.py fast --changed --tests customiuizer.mods` and `customiuizer`

FAST_GATES = YES (per production slice)
FULL_GATE = YES (`python tools/verify.py full` including lintDebug)
TOOLS_TESTS = YES (`python -m unittest discover -s tools/tests -p "test_*.py"` → 1283 tests, 2 skipped)
COMPILEALL = YES (`python -m compileall tools`)
DIFF_CHECK = YES (`git diff --check`)
DEPENDENCY_VERIFICATION = YES (not disabled; Gradle 9 plugin classpath checksums in e1ba9b5)

STATIC_VERIFIED = YES for implemented slices
BUILD_VERIFIED = YES for implemented slices
DEVICE_VERIFIED = NO
LOG_VERIFIED = NO

---

## Worktree

WORKTREE = CLEAN at last production/test commit
LOCAL_HEAD = adb01b4
REMOTE_HEAD = 48729a53796b478bfa97512ade2963f82310619c
BRANCH = devin/a13-foundation-parity-r13.11.1 (ahead of origin; not pushed)

No force-push, reset, rebase, main merge, tag, or release.

---

READY_FOR_CHATGPT_PHASE_E_FINAL_AUDIT
