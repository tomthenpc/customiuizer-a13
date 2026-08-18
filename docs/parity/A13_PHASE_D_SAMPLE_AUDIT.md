# A13 Phase D-R2 Sample Audit

```text
SOURCE_MATRIX = docs/parity/A13_A14_FEATURE_MATRIX.csv
METHOD = deterministic seeded sampling + full-set inclusion where required
```

## PRESENT rows (10)

- `A14_UI_system_statusbar_headset_atright` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | host/process `SYSTEM_UI/com.android.systemui` | result `PRESENT_A13_VARIANT` | proof `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_dualsimin2rows_style` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_clock_chip_usemonet` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_topmargin_unset_lockscreen` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_clock_chip_radius` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_clock_fontsize` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_clock_leftmargin` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_clock_chip` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `A14_UI_system_statusbar_batterytempandcurrent_positive` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`
- `StatusBarClockPositionFeatureId` | A14 `mods/SystemUIStatusBarHooks.kt` | A13 `mods/SystemUIStatusBarHooks.kt` | `SYSTEM_UI/com.android.systemui` | `PRESENT_A13_VARIANT` | `PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS`

## MISSING rows (10)

- `A14_UI_system_statusbar_mobile_digital_signal_leftmargin` | A14 `inferred-from-ui-topology` | A13 `ABSENT` | `SYSTEM_UI/com.android.systemui` | A14 has digital-signal margin control; A13 absent | `MISSING_IN_A13`
- `A14_UI_various_disable_miui_daemon` | A14 `inferred-from-ui-topology` | A13 `ABSENT` | `SECURITY_CENTER/com.miui.securitycenter` | daemon-disable semantics absent in A13 | `MISSING_IN_A13`
- `SecurityCenterPersistPrivacyThumbnailBlurFeatureId` | A14 `mods/utils/feature/SecurityCenterFeatures.kt` | A13 `ABSENT` | `SYSTEM_PACKAGE/android.system.package` | recents/privacy persistence absent | `MISSING_IN_A13`
- `CcClockCenterAlignFeatureId` | A14 `mods/utils/feature/SystemUiFeatures.kt` | A13 `ABSENT` | `SYSTEM_UI/com.android.systemui` | CC center alignment absent | `MISSING_IN_A13`
- `A14_UI_system_volume_mode_button_background_color` | A14 `inferred-from-ui-topology` | A13 `ABSENT` | `SYSTEM_UI/com.android.systemui` | volume mode background color absent | `MISSING_IN_A13`
- `DisableWindowBlursFeatureId` | A14 `mods/utils/feature/SystemServerFeatures.kt` | A13 `ABSENT` | `SYSTEM_SERVER/android` | A14 exposes window-blur global behavior toggle; A13 absent | `MISSING_IN_A13`
- `A14_UI_system_statusbarcontrols_dt_right` | A14 `inferred-from-ui-topology` | A13 `ABSENT` | `SYSTEM_UI/com.android.systemui` | right double-tap statusbar control absent | `MISSING_IN_A13`
- `A14_UI_system_drawer_date_fontsize` | A14 `inferred-from-ui-topology` | A13 `ABSENT` | `SYSTEM_UI/com.android.systemui` | notification drawer date font-size control absent | `MISSING_IN_A13`
- `A14_UI_system_statusbar_mobile_digital_signal_hideunit` | A14 `inferred-from-ui-topology` | A13 `ABSENT` | `SYSTEM_UI/com.android.systemui` | digital signal unit hiding absent | `MISSING_IN_A13`
- `infra.backup_restore` | A14 `utils/BackupFormatV2.kt, utils/BackupRestore.kt` | A13 `PreferenceFragmentBase.kt` | `SETTINGS/com.android.settings` | typed V2 backup semantics missing in A13 | `MISSING_IN_A13` | proof `PROOF_INFRA_BACKUP_RESTORE`

## PARTIAL rows (all)

- none (`0`)

## E1 rows (all)

- `infra.backup_restore` | host/process `SETTINGS/com.android.settings` | A14 typed V2 backup/restore vs A13 legacy serialization | result `MISSING_IN_A13` | proof `PROOF_INFRA_BACKUP_RESTORE`

## E5 rows (all)

- `AutoBrightnessAfterScreenOffFeatureId` | A14 `mods/utils/feature/SystemServerFeatures.kt` | A13 `ABSENT` | `SYSTEM_SERVER/android` | `MISSING_IN_A13`
- `DisableWindowBlursFeatureId` | A14 `mods/utils/feature/SystemServerFeatures.kt` | A13 `ABSENT` | `SYSTEM_SERVER/android` | `MISSING_IN_A13`
- `ForceDarkAllAppsFeatureId` | A14 `mods/utils/feature/SystemServerFeatures.kt` | A13 `ABSENT` | `SYSTEM_SERVER/android` | `MISSING_IN_A13`
- `UsbDefaultFunctionFeatureId` | A14 `mods/utils/feature/SystemServerFeatures.kt::UsbDefaultFunctionFeatureId` | A13 `ABSENT` | `SYSTEM_SERVER/android` | `MISSING_IN_A13` | proof `PROOF_USB_DEFAULT_PURPOSE_A14_ONLY`

## E3 representative rows (10)

- `A14_UI_system_volume_mode_button_colors` | `SYSTEM_UI/com.android.systemui` | A14 volume mode color controls; A13 absent | `MISSING_IN_A13`
- `A14_UI_system_statusbar_mobile_digital_signal_leftmargin` | `SYSTEM_UI/com.android.systemui` | A14 digital signal layout tuning; A13 absent | `MISSING_IN_A13`
- `A14_UI_system_cc_hide_edit` | `SYSTEM_UI/com.android.systemui` | A14 CC edit suppression toggle; A13 absent | `MISSING_IN_A13`
- `A14_UI_system_cc_card_enabled_secondary_textcolor` | `SYSTEM_UI/com.android.systemui` | A14 CC secondary text color control; A13 absent | `MISSING_IN_A13`
- `DisableKeyguardEditorFeatureId` | `SYSTEM_UI/com.android.systemui` | lockscreen editor disable branch absent | `MISSING_IN_A13`
- `A14_UI_system_statusbar_mobile_digital_signal_in2rows` | `SYSTEM_UI/com.android.systemui` | two-row digital signal style absent | `MISSING_IN_A13`
- `A14_UI_system_cc_floatingtimetile` | `SYSTEM_UI/com.android.systemui` | floating time tile behavior absent | `MISSING_IN_A13`
- `A14_UI_system_statusbar_icons_atleft_onkeyguard` | `SYSTEM_UI/com.android.systemui` | icon-side-on-keyguard behavior absent | `MISSING_IN_A13`
- `A14_UI_system_cc_tile_enabled_iconcolor_custom` | `SYSTEM_UI/com.android.systemui` | custom icon color enabled-state absent | `MISSING_IN_A13`
- `A14_UI_system_cc_card_enabled_color` | `SYSTEM_UI/com.android.systemui` | card enabled color feature absent | `MISSING_IN_A13`

## E4 representative rows (all 9 available)

- `A14_UI_various_block_location_permission_prompts` | `SECURITY_CENTER/com.miui.securitycenter` | dismiss location permission prompt behavior absent | `MISSING_IN_A13`
- `A14_UI_various_block_notification_permission_prompts` | `SECURITY_CENTER/com.miui.securitycenter` | dismiss notification permission prompt behavior absent | `MISSING_IN_A13`
- `A14_UI_various_disable_miui_daemon` | `SECURITY_CENTER/com.miui.securitycenter` | MIUI daemon disable controls absent | `MISSING_IN_A13`
- `SecurityCenterPersistPrivacyThumbnailBlurFeatureId` | `SYSTEM_PACKAGE/android.system.package` | privacy thumbnail blur persistence path absent | `MISSING_IN_A13`
- `A14_UI_various_disable_update_services` | `SECURITY_CENTER/com.miui.securitycenter` | updater-service controls absent | `MISSING_IN_A13`
- `A14_UI_various_disable_xiaomi_analytics` | `SECURITY_CENTER/com.miui.securitycenter` | analytics controls absent | `MISSING_IN_A13`
- `A14_UI_various_remove_security_center_antivirus` | `SECURITY_CENTER/com.miui.securitycenter` | antivirus entry/reminder removal absent | `MISSING_IN_A13`
- `A14_UI_various_trim_miui_daemon_network` | `SECURITY_CENTER/com.miui.securitycenter` | daemon network trim controls absent | `MISSING_IN_A13`
- `A14_UI_various_trim_security_center_marketing` | `SECURITY_CENTER/com.miui.securitycenter` | marketing component trim controls absent | `MISSING_IN_A13`

## HOLD_EVIDENCE P0/P1 rows (all)

- `A14_UI_system_charginginfo_fontsize` | host/process unresolved | result `MISSING_IN_A13` | hold reason: unresolved implementation ownership/classloader
- `A14_UI_system_netspeed_boldfont` | host/process unresolved | result `MISSING_IN_A13` | hold reason: unresolved implementation ownership/classloader
- `A14_UI_system_netspeed_use_clock_style` | host/process unresolved | result `MISSING_IN_A13` | hold reason: unresolved implementation ownership/classloader
- `A14_UI_system_strong_toast_island_offset` | host/process unresolved | result `MISSING_IN_A13` | hold reason: unresolved implementation ownership/classloader

```text
SAMPLE_AUDIT_RESULT = PASS
```

