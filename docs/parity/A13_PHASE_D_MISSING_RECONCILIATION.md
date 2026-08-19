# A13 Phase D Missing Reconciliation (D-FINAL SWEEP)

Generic R3 ABSENCE_PROOF text is not used. Each record is produced from live A13 source searches.

CURRENT_MISSING_ROWS_AUDITED = 76

| A14_FEATURE_ID | A14_PREF_KEYS | FINAL_PARITY_STATE | A13_MATCH | RECLASSIFICATION_REASON |
|---|---|---|---|---|
| HideImeDismissButtonFeatureId | controls_hide_ime_dismiss_button | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| LauncherDockHeightFeatureId | launcher_dock_height | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_launcher_folderblur_disable | launcher_folderblur_disable | PARTIAL_PARITY | launcher_folderblur_opacity | A13 FolderBlurHook already owns folder blur via opacity; A14 adds a disable flag that preserves the stored opacity. |
| LauncherWallpaperColorModeFeatureId | launcher_wallpaper_colormode | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| AutoBrightnessAfterScreenOffFeatureId | system_autobrightness_reset_when_screenoff | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_btandtorch_ascard | system_cc_btandtorch_ascard | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_card_enabled_color | system_cc_card_enabled_color | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_card_enabled_color_custom | system_cc_card_enabled_color_custom | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_card_enabled_iconcolor_custom | system_cc_card_enabled_iconcolor_custom | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_card_enabled_primary_textcolor | system_cc_card_enabled_primary_textcolor | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_card_enabled_secondary_textcolor | system_cc_card_enabled_secondary_textcolor | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| CcClockCenterAlignFeatureId | system_cc_clock_centeralign | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_floatingtimetile | system_cc_floatingtimetile | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| LongClickTileOpenInFreeFormFeatureId | system_cc_freeform_when_longclick | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_hide_edit | system_cc_hide_edit | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_hide_profile_monitoring | system_cc_hide_profile_monitoring | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_slider_color_enable | system_cc_slider_color_enable | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_slider_icon_color | system_cc_slider_icon_color | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_slider_progress_color | system_cc_slider_progress_color | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_tile_enabled_color | system_cc_tile_enabled_color | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_tile_enabled_color_custom | system_cc_tile_enabled_color_custom | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_tile_enabled_color_usemonet | system_cc_tile_enabled_color_usemonet | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_cc_tile_enabled_iconcolor_custom | system_cc_tile_enabled_iconcolor_custom | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_charginginfo_fontsize | system_charginginfo_fontsize | PARTIAL_PARITY | system_charginginfo,system_charginginfo_view | A13 lockscreen charging-info family exists; A14 adds a font-size suboption on the same view. |
| NetSpeedStyleFeatureId | system_detailednetspeed_style | PARTIAL_PARITY | system_detailednetspeed,system_detailednetspeed_fakedualrow | A14 style selector supersedes A13 detailed/fakedualrow toggles on the same netspeed hook family. |
| DisableWindowBlursFeatureId | system_disable_window_blurs | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_drawer_date_centeralign | system_drawer_date_centeralign | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_drawer_date_fontsize | system_drawer_date_fontsize | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_drawer_dateformat | system_drawer_dateformat | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_drawer_hidedate | system_drawer_hidedate | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| HideNoNotificationsFeatureId | system_drawer_remove_emptynotify | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| ForceDarkAllAppsFeatureId | system_force_darken_allapps | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_hidestatusbar_whenscreenrecord | system_hidestatusbar_whenscreenrecord | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| DisableKeyguardEditorFeatureId | system_lockscreen_disable_edit | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_netspeed_boldfont | system_netspeed_boldfont | PRESENT_A13_VARIANT | system_netspeed_bold | Same user capability: bold network-speed typeface. A14 renamed the key. |
| A14_UI_system_netspeed_use_clock_style | system_netspeed_use_clock_style | PARTIAL_PARITY | system_netspeed_bold,system_netspeed_fontsize | A13 already customizes netspeed typeface/size; A14 adds match-clock-style on the same helper. |
| DisableFoldNotificationsFeatureId | system_notif_disable_fold | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| DisableFakeClockAnimFeatureId | system_qs_disable_fakeclock_anim | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| LauncherRecentsCardStyleFeatureId | system_recents_card_style | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| StatusBarContentGeometryFeatureId | system_statusbar_content_vertical_offset | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_dualrows_left_ratio | system_statusbar_dualrows_left_ratio | PARTIAL_PARITY | system_statusbar_dualrows,system_statusbar_dualrows_firstrow_horizmargin | A13 dual-row status bar exists with first-row padding; A14 adds left-width ratio. |
| A14_UI_system_statusbar_enable_weather_param | system_statusbar_enable_weather_param | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_icons_atleft_onkeyguard | system_statusbar_icons_atleft_onkeyguard | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| StatusBarDigitalSignalFeatureId | system_statusbar_mobile_digital_signal | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_align | system_statusbar_mobile_digital_signal_align | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_bold | system_statusbar_mobile_digital_signal_bold | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_fontsize | system_statusbar_mobile_digital_signal_fontsize | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_hideunit | system_statusbar_mobile_digital_signal_hideunit | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_in2rows | system_statusbar_mobile_digital_signal_in2rows | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_leftmargin | system_statusbar_mobile_digital_signal_leftmargin | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_rightmargin | system_statusbar_mobile_digital_signal_rightmargin | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbar_mobile_digital_signal_verticaloffset | system_statusbar_mobile_digital_signal_verticaloffset | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbarcontrols_dt_left | system_statusbarcontrols_dt_left | PARTIAL_PARITY | system_statusbarcontrols_dt | A13 has one status-bar double-tap action; A14 splits left-corner double-tap. |
| A14_UI_system_statusbarcontrols_dt_right | system_statusbarcontrols_dt_right | PARTIAL_PARITY | system_statusbarcontrols_dt | A13 has one status-bar double-tap action; A14 splits right-corner double-tap. |
| A14_UI_system_statusbaricons_bluetoothicn | system_statusbaricons_bluetoothicn | PRESENT_A13_VARIANT | system_statusbaricons_bluetooth | A13 HideIconsBluetoothHook option 3 already always-hides the bluetooth icon. |
| HidePrivacyIndicatorFeatureId | system_statusbaricons_privacy_prompt | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_statusbaricons_wireless_headset | system_statusbaricons_wireless_headset | PARTIAL_PARITY | system_statusbaricons_headset | A13 hides the headset slot; A14 adds a separate wireless_headset slot on the same hide-icons path. |
| A14_UI_system_strong_toast_island_offset | system_strong_toast_island_offset | HOLD_EVIDENCE | dynamic_island | Dynamic Island helper preference; product policy forbids extra DI gaps. Keep HOLD_EVIDENCE, do not port. |
| StrongToastPresentationFeatureId | system_strong_toast_mode | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| UsbDefaultFunctionFeatureId | system_usb_default_function | PARTIAL_PARITY | system_defaultusb,system_defaultusb_unsecure | A14 renamed key; A13 already owns USB default via USBConfigHook/USBConfigSettingsHook. |
| A14_UI_system_volume_hide_dnd_shortcut | system_volume_hide_dnd_shortcut | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_volume_hide_mute_shortcut | system_volume_hide_mute_shortcut | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_volume_mode_button_background_color | system_volume_mode_button_background_color | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_volume_mode_button_colors | system_volume_mode_button_colors | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_system_volume_mode_button_icon_color | system_volume_mode_button_icon_color | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_block_location_permission_prompts | various_block_location_permission_prompts | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_block_notification_permission_prompts | various_block_notification_permission_prompts | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_disable_miui_daemon | various_disable_miui_daemon | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| SecurityCenterPersistPrivacyThumbnailBlurFeatureId | various_disable_reset_recents_privacy_blur | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_disable_update_services | various_disable_update_services | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_disable_xiaomi_analytics | various_disable_xiaomi_analytics | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| SecurityCenterHideReportButtonFeatureId | various_hide_report_ondetails | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| PackageInstallerPurifyFeatureId | various_installer_purify | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_remove_security_center_antivirus | various_remove_security_center_antivirus | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_trim_miui_daemon_network | various_trim_miui_daemon_network | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |
| A14_UI_various_trim_security_center_marketing | various_trim_security_center_marketing | MISSING_IN_A13 | ABSENT | No A13 equivalent after feature-specific source review. |

## Detailed Audit Records

- **A14_FEATURE_ID**: `HideImeDismissButtonFeatureId`
  - A14_PREF_KEYS: `controls_hide_ime_dismiss_button`
  - A14_BEHAVIOR: Hides gesture-navigation IME dismiss affordance.
  - A14_REFERENCE: `mods/utils/feature/SystemUiFeatures.kt::HideImeDismissButtonFeatureId`
  - A13_SEARCH_TERMS: `controls_hide_ime_dismiss_button; HideImeDismissButtonFeatureId; Hide IME Dismiss Button`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `controls_hide_ime_dismiss_button`: no match
- feature id `HideImeDismissButtonFeatureId`: no match
- title `Hide IME Dismiss Button`: no match
- token 'dismiss' `dismiss`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `LauncherDockHeightFeatureId`
  - A14_PREF_KEYS: `launcher_dock_height`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/LauncherPackageReadyFeatures.kt`
  - A13_SEARCH_TERMS: `launcher_dock_height; LauncherDockHeightFeatureId; Launcher Dock Height`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `launcher_dock_height`: no match
- feature id `LauncherDockHeightFeatureId`: no match
- title `Launcher Dock Height`: no match
- token 'height' `height`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: launcher_dock_bottommargin, launcher_dock_topmargin
- nearest candidate `launcher_dock_topmargin / launcher_dock_bottommargin` inspected and rejected because those hooks change dock margins in LauncherLayoutHooks, not hotseat/dock height

- **A14_FEATURE_ID**: `A14_UI_launcher_folderblur_disable`
  - A14_PREF_KEYS: `launcher_folderblur_disable`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `launcher_folderblur_disable; A14_UI_launcher_folderblur_disable; Disable folder background blur; launcher_folderblur_opacity`
  - A13_MATCH: `launcher_folderblur_opacity`
  - A13_REFERENCE: `mods/LauncherFolderHooks.kt::FolderBlurHook; installers/LauncherInstaller.java`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A13 FolderBlurHook already owns folder blur via opacity; A14 adds a disable flag that preserves the stored opacity.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `launcher_folderblur_disable` has no identical A13 key
- matched A13 `launcher_folderblur_opacity` at mods/LauncherFolderHooks.kt::FolderBlurHook; installers/LauncherInstaller.java
- A14 materially extends existing A13 semantics: A13 FolderBlurHook already owns folder blur via opacity; A14 adds a disable flag that preserves the stored opacity.

- **A14_FEATURE_ID**: `LauncherWallpaperColorModeFeatureId`
  - A14_PREF_KEYS: `launcher_wallpaper_colormode`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/LauncherPostAttachFeatures.kt`
  - A13_SEARCH_TERMS: `launcher_wallpaper_colormode; LauncherWallpaperColorModeFeatureId; Launcher Wallpaper Color Mode`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `launcher_wallpaper_colormode`: no match
- feature id `LauncherWallpaperColorModeFeatureId`: no match
- title `Launcher Wallpaper Color Mode`: no match
- token 'wallpaper' `wallpaper`: hits app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/WallpaperInstaller.java
- token 'colormode' `colormode`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt, app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `AutoBrightnessAfterScreenOffFeatureId`
  - A14_PREF_KEYS: `system_autobrightness_reset_when_screenoff`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
  - A13_SEARCH_TERMS: `system_autobrightness_reset_when_screenoff; AutoBrightnessAfterScreenOffFeatureId; Auto Brightness After Screen Off`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_autobrightness_reset_when_screenoff`: no match
- feature id `AutoBrightnessAfterScreenOffFeatureId`: no match
- title `Auto Brightness After Screen Off`: no match
- token 'autobrightness' `autobrightness`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceSchema.kt
- token 'reset' `reset`: hits app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt, app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt
- token 'screenoff' `screenoff`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/utils/StepCounterController.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_autobrightness, system_autobrightness_cat, system_autobrightness_limitmax, system_autobrightness_limitmin
- nearest candidate `system_autobrightness / system_autobrightness_min/max` inspected and rejected because A13 AutoBrightness hooks clamp range; they do not reset brightness after screen-off

- **A14_FEATURE_ID**: `A14_UI_system_cc_btandtorch_ascard`
  - A14_PREF_KEYS: `system_cc_btandtorch_ascard`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_btandtorch_ascard; A14_UI_system_cc_btandtorch_ascard; Display bluetooth and flashlight tiles as cards`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_btandtorch_ascard`: no match
- feature id `A14_UI_system_cc_btandtorch_ascard`: no match
- title `Display bluetooth and flashlight tiles as cards`: no match
- token 'btandtorch' `btandtorch`: no match
- token 'ascard' `ascard`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_btandtorch_ascard` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_card_enabled_color`
  - A14_PREF_KEYS: `system_cc_card_enabled_color`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_card_enabled_color; A14_UI_system_cc_card_enabled_color; Enabled card tile`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_card_enabled_color`: no match
- feature id `A14_UI_system_cc_card_enabled_color`: no match
- title `Enabled card tile`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_card_enabled_color` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_card_enabled_color_custom`
  - A14_PREF_KEYS: `system_cc_card_enabled_color_custom`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_card_enabled_color_custom; A14_UI_system_cc_card_enabled_color_custom; Background color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_card_enabled_color_custom`: no match
- feature id `A14_UI_system_cc_card_enabled_color_custom`: no match
- title `Background color`: hits app/src/main/res/values/strings.xml
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_card_enabled_color_custom` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_card_enabled_iconcolor_custom`
  - A14_PREF_KEYS: `system_cc_card_enabled_iconcolor_custom`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_card_enabled_iconcolor_custom; A14_UI_system_cc_card_enabled_iconcolor_custom; Icon color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_card_enabled_iconcolor_custom`: no match
- feature id `A14_UI_system_cc_card_enabled_iconcolor_custom`: no match
- title `Icon color`: no match
- token 'iconcolor' `iconcolor`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_card_enabled_iconcolor_custom` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_card_enabled_primary_textcolor`
  - A14_PREF_KEYS: `system_cc_card_enabled_primary_textcolor`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_card_enabled_primary_textcolor; A14_UI_system_cc_card_enabled_primary_textcolor; Primary text color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_card_enabled_primary_textcolor`: no match
- feature id `A14_UI_system_cc_card_enabled_primary_textcolor`: no match
- title `Primary text color`: no match
- token 'primary' `primary`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/DropDownPreferenceEx.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/ListPreferenceEx.kt
- token 'textcolor' `textcolor`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/DropDownPreferenceEx.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_card_enabled_primary_textcolor` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_card_enabled_secondary_textcolor`
  - A14_PREF_KEYS: `system_cc_card_enabled_secondary_textcolor`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_card_enabled_secondary_textcolor; A14_UI_system_cc_card_enabled_secondary_textcolor; Secondary text color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_card_enabled_secondary_textcolor`: no match
- feature id `A14_UI_system_cc_card_enabled_secondary_textcolor`: no match
- title `Secondary text color`: no match
- token 'secondary' `secondary`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/DropDownPreferenceEx.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/ListPreferenceEx.kt
- token 'textcolor' `textcolor`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/DropDownPreferenceEx.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_card_enabled_secondary_textcolor` behavior

- **A14_FEATURE_ID**: `CcClockCenterAlignFeatureId`
  - A14_PREF_KEYS: `system_cc_clock_centeralign`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_cc_clock_centeralign; CcClockCenterAlignFeatureId; CC Clock Center Align`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_clock_centeralign`: no match
- feature id `CcClockCenterAlignFeatureId`: no match
- title `CC Clock Center Align`: no match
- token 'clock' `clock`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java
- token 'centeralign' `centeralign`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize, system_cc_clock_topmargin_indrawer
- nearest A13 candidate `system_cc_clock_customformat` inspected and rejected because it does not implement `system_cc_clock_centeralign` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_floatingtimetile`
  - A14_PREF_KEYS: `system_cc_floatingtimetile`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_floatingtimetile; A14_UI_system_cc_floatingtimetile; Show floating time tile`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_floatingtimetile`: no match
- feature id `A14_UI_system_cc_floatingtimetile`: no match
- title `Show floating time tile`: no match
- token 'floatingtimetile' `floatingtimetile`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_floatingtimetile` behavior

- **A14_FEATURE_ID**: `LongClickTileOpenInFreeFormFeatureId`
  - A14_PREF_KEYS: `system_cc_freeform_when_longclick`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_cc_freeform_when_longclick; LongClickTileOpenInFreeFormFeatureId; Long Click Tile Open In Free Form`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_freeform_when_longclick`: no match
- feature id `LongClickTileOpenInFreeFormFeatureId`: no match
- title `Long Click Tile Open In Free Form`: no match
- token 'freeform' `freeform`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java
- token 'longclick' `longclick`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest candidate `Control Center tile click hooks` inspected and rejected because A13 CC hooks cover volume/theme/clock, not long-click-tile-open-in-freeform

- **A14_FEATURE_ID**: `A14_UI_system_cc_hide_edit`
  - A14_PREF_KEYS: `system_cc_hide_edit`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_hide_edit; A14_UI_system_cc_hide_edit; Hide edit button`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_hide_edit`: no match
- feature id `A14_UI_system_cc_hide_edit`: no match
- title `Hide edit button`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_hide_shortcuticons, system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width
- nearest A13 candidate `system_cc_hide_shortcuticons` inspected and rejected because it does not implement `system_cc_hide_edit` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_hide_profile_monitoring`
  - A14_PREF_KEYS: `system_cc_hide_profile_monitoring`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_hide_profile_monitoring; A14_UI_system_cc_hide_profile_monitoring; Hide security tips`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_hide_profile_monitoring`: no match
- feature id `A14_UI_system_cc_hide_profile_monitoring`: no match
- title `Hide security tips`: no match
- token 'profile' `profile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/compat/RomEnvironment.kt
- token 'monitoring' `monitoring`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_hide_shortcuticons, system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width
- nearest A13 candidate `system_cc_hide_shortcuticons` inspected and rejected because it does not implement `system_cc_hide_profile_monitoring` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_slider_color_enable`
  - A14_PREF_KEYS: `system_cc_slider_color_enable`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_slider_color_enable; A14_UI_system_cc_slider_color_enable; Toggle slider`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_slider_color_enable`: no match
- feature id `A14_UI_system_cc_slider_color_enable`: no match
- title `Toggle slider`: no match
- token 'slider' `slider`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/res/xml/prefs_system.xml
- token 'enable' `enable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_slider_color_enable` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_slider_icon_color`
  - A14_PREF_KEYS: `system_cc_slider_icon_color`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_slider_icon_color; A14_UI_system_cc_slider_icon_color; Icon color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_slider_icon_color`: no match
- feature id `A14_UI_system_cc_slider_icon_color`: no match
- title `Icon color`: no match
- token 'slider' `slider`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/res/xml/prefs_system.xml
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_slider_icon_color` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_slider_progress_color`
  - A14_PREF_KEYS: `system_cc_slider_progress_color`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_slider_progress_color; A14_UI_system_cc_slider_progress_color; Progress color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_slider_progress_color`: no match
- feature id `A14_UI_system_cc_slider_progress_color`: no match
- title `Progress color`: no match
- token 'slider' `slider`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/res/xml/prefs_system.xml
- token 'progress' `progress`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/SeekBarPreference.kt, app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width, system_cc_clock_fontsize
- nearest A13 candidate `system_cc_bluetooth_tile_style` inspected and rejected because it does not implement `system_cc_slider_progress_color` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_tile_enabled_color`
  - A14_PREF_KEYS: `system_cc_tile_enabled_color`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_tile_enabled_color; A14_UI_system_cc_tile_enabled_color; Enabled tile`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_tile_enabled_color`: no match
- feature id `A14_UI_system_cc_tile_enabled_color`: no match
- title `Enabled tile`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_tile_roundedrect, system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width
- nearest A13 candidate `system_cc_tile_roundedrect` inspected and rejected because it does not implement `system_cc_tile_enabled_color` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_tile_enabled_color_custom`
  - A14_PREF_KEYS: `system_cc_tile_enabled_color_custom`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_tile_enabled_color_custom; A14_UI_system_cc_tile_enabled_color_custom; Background color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_tile_enabled_color_custom`: no match
- feature id `A14_UI_system_cc_tile_enabled_color_custom`: no match
- title `Background color`: hits app/src/main/res/values/strings.xml
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_tile_roundedrect, system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width
- nearest A13 candidate `system_cc_tile_roundedrect` inspected and rejected because it does not implement `system_cc_tile_enabled_color_custom` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_tile_enabled_color_usemonet`
  - A14_PREF_KEYS: `system_cc_tile_enabled_color_usemonet`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_tile_enabled_color_usemonet; A14_UI_system_cc_tile_enabled_color_usemonet; Dynamic color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_tile_enabled_color_usemonet`: no match
- feature id `A14_UI_system_cc_tile_enabled_color_usemonet`: no match
- title `Dynamic color`: hits app/src/main/res/values/strings.xml
- token 'usemonet' `usemonet`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/res/xml/prefs_system_statusbar_clock.xml
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_tile_roundedrect, system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width
- nearest A13 candidate `system_cc_tile_roundedrect` inspected and rejected because it does not implement `system_cc_tile_enabled_color_usemonet` behavior

- **A14_FEATURE_ID**: `A14_UI_system_cc_tile_enabled_iconcolor_custom`
  - A14_PREF_KEYS: `system_cc_tile_enabled_iconcolor_custom`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_cc_tile_enabled_iconcolor_custom; A14_UI_system_cc_tile_enabled_iconcolor_custom; Icon color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_cc_tile_enabled_iconcolor_custom`: no match
- feature id `A14_UI_system_cc_tile_enabled_iconcolor_custom`: no match
- title `Icon color`: no match
- token 'iconcolor' `iconcolor`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_cc_tile_roundedrect, system_cc_bluetooth_tile_style, system_cc_clock_customformat, system_cc_clock_fixedcontent_width
- nearest A13 candidate `system_cc_tile_roundedrect` inspected and rejected because it does not implement `system_cc_tile_enabled_iconcolor_custom` behavior

- **A14_FEATURE_ID**: `A14_UI_system_charginginfo_fontsize`
  - A14_PREF_KEYS: `system_charginginfo_fontsize`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_charginginfo_fontsize; A14_UI_system_charginginfo_fontsize; Lock screen charging text size; system_charginginfo; system_charginginfo_view`
  - A13_MATCH: `system_charginginfo,system_charginginfo_view`
  - A13_REFERENCE: `mods/SystemChargingAndWallpaperHooks.kt; res/xml/prefs_system_charginginfo.xml`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A13 lockscreen charging-info family exists; A14 adds a font-size suboption on the same view.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_charginginfo_fontsize` has no identical A13 key
- matched A13 `system_charginginfo,system_charginginfo_view` at mods/SystemChargingAndWallpaperHooks.kt; res/xml/prefs_system_charginginfo.xml
- A14 materially extends existing A13 semantics: A13 lockscreen charging-info family exists; A14 adds a font-size suboption on the same view.

- **A14_FEATURE_ID**: `NetSpeedStyleFeatureId`
  - A14_PREF_KEYS: `system_detailednetspeed_style`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_detailednetspeed_style; NetSpeedStyleFeatureId; Net Speed Style; system_detailednetspeed; system_detailednetspeed_fakedualrow`
  - A13_MATCH: `system_detailednetspeed,system_detailednetspeed_fakedualrow`
  - A13_REFERENCE: `res/xml/prefs_system_detailednetspeed.xml; mods/SystemUIStatusBarHooks.kt`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A14 style selector supersedes A13 detailed/fakedualrow toggles on the same netspeed hook family.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_detailednetspeed_style` has no identical A13 key
- matched A13 `system_detailednetspeed,system_detailednetspeed_fakedualrow` at res/xml/prefs_system_detailednetspeed.xml; mods/SystemUIStatusBarHooks.kt
- A14 materially extends existing A13 semantics: A14 style selector supersedes A13 detailed/fakedualrow toggles on the same netspeed hook family.

- **A14_FEATURE_ID**: `DisableWindowBlursFeatureId`
  - A14_PREF_KEYS: `system_disable_window_blurs`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
  - A13_SEARCH_TERMS: `system_disable_window_blurs; DisableWindowBlursFeatureId; Disable Window Blurs`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_disable_window_blurs`: no match
- feature id `DisableWindowBlursFeatureId`: no match
- title `Disable Window Blurs`: no match
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'window' `window`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
- token 'blurs' `blurs`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest candidate `system_recents_blur / folder blur` inspected and rejected because those are app-surface blur intensities, not system_server window-blur disable

- **A14_FEATURE_ID**: `A14_UI_system_drawer_date_centeralign`
  - A14_PREF_KEYS: `system_drawer_date_centeralign`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_drawer_date_centeralign; A14_UI_system_drawer_date_centeralign; Show centered`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_drawer_date_centeralign`: no match
- feature id `A14_UI_system_drawer_date_centeralign`: no match
- title `Show centered`: hits app/src/main/res/values/strings.xml
- token 'drawer' `drawer`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt
- token 'centeralign' `centeralign`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_drawer_blur, system_drawer_removeshortcut, system_drawer_show_stepcount
- nearest A13 candidate `system_drawer_blur` inspected and rejected because it does not implement `system_drawer_date_centeralign` behavior

- **A14_FEATURE_ID**: `A14_UI_system_drawer_date_fontsize`
  - A14_PREF_KEYS: `system_drawer_date_fontsize`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_drawer_date_fontsize; A14_UI_system_drawer_date_fontsize; Font size`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_drawer_date_fontsize`: no match
- feature id `A14_UI_system_drawer_date_fontsize`: no match
- title `Font size`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt, app/src/main/res/values/strings.xml
- token 'drawer' `drawer`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt
- token 'fontsize' `fontsize`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_drawer_blur, system_drawer_removeshortcut, system_drawer_show_stepcount
- nearest A13 candidate `system_drawer_blur` inspected and rejected because it does not implement `system_drawer_date_fontsize` behavior

- **A14_FEATURE_ID**: `A14_UI_system_drawer_dateformat`
  - A14_PREF_KEYS: `system_drawer_dateformat`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_drawer_dateformat; A14_UI_system_drawer_dateformat; Custom date format`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_drawer_dateformat`: no match
- feature id `A14_UI_system_drawer_dateformat`: no match
- title `Custom date format`: hits app/src/main/res/values/strings.xml
- token 'drawer' `drawer`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt
- token 'dateformat' `dateformat`: hits app/src/main/java/tv/withaibuild/customiuizer/AboutFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_drawer_blur, system_drawer_removeshortcut, system_drawer_show_stepcount
- nearest A13 candidate `system_drawer_blur` inspected and rejected because it does not implement `system_drawer_dateformat` behavior

- **A14_FEATURE_ID**: `A14_UI_system_drawer_hidedate`
  - A14_PREF_KEYS: `system_drawer_hidedate`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_drawer_hidedate; A14_UI_system_drawer_hidedate; Hide date`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_drawer_hidedate`: no match
- feature id `A14_UI_system_drawer_hidedate`: no match
- title `Hide date`: hits app/src/main/res/values/strings.xml
- token 'drawer' `drawer`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt
- token 'hidedate' `hidedate`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceSchema.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_drawer_blur, system_drawer_removeshortcut, system_drawer_show_stepcount
- nearest A13 candidate `system_drawer_blur` inspected and rejected because it does not implement `system_drawer_hidedate` behavior

- **A14_FEATURE_ID**: `HideNoNotificationsFeatureId`
  - A14_PREF_KEYS: `system_drawer_remove_emptynotify`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_drawer_remove_emptynotify; HideNoNotificationsFeatureId; Hide No Notifications`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_drawer_remove_emptynotify`: no match
- feature id `HideNoNotificationsFeatureId`: no match
- title `Hide No Notifications`: no match
- token 'drawer' `drawer`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt
- token 'remove' `remove`: hits app/src/main/java/tv/withaibuild/customiuizer/MainApplication.kt, app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt
- token 'emptynotify' `emptynotify`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_drawer_blur, system_drawer_removeshortcut, system_drawer_show_stepcount
- nearest A13 candidate `system_drawer_blur` inspected and rejected because it does not implement `system_drawer_remove_emptynotify` behavior

- **A14_FEATURE_ID**: `ForceDarkAllAppsFeatureId`
  - A14_PREF_KEYS: `system_force_darken_allapps`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
  - A13_SEARCH_TERMS: `system_force_darken_allapps; ForceDarkAllAppsFeatureId; Force Dark All Apps`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_force_darken_allapps`: no match
- feature id `ForceDarkAllAppsFeatureId`: no match
- title `Force Dark All Apps`: no match
- token 'force' `force`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt
- token 'darken' `darken`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt
- token 'allapps' `allapps`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest candidate `debug.hwui.force_dark write in SystemSecurityAndSystemHooks` inspected and rejected because A13 only forces the property false in one path; no all-apps force-dark feature

- **A14_FEATURE_ID**: `A14_UI_system_hidestatusbar_whenscreenrecord`
  - A14_PREF_KEYS: `system_hidestatusbar_whenscreenrecord`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_hidestatusbar_whenscreenrecord; A14_UI_system_hidestatusbar_whenscreenrecord; Hide status bar when recording screen`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_hidestatusbar_whenscreenrecord`: no match
- feature id `A14_UI_system_hidestatusbar_whenscreenrecord`: no match
- title `Hide status bar when recording screen`: no match
- token 'hidestatusbar' `hidestatusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIScreenshotHooks.kt
- token 'whenscreenrecord' `whenscreenrecord`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_hidestatusbar_whenscreenshot
- nearest candidate `system_hidestatusbar_whenscreenshot` inspected and rejected because existing hook is screenshot-only in BatteryIndicator; screen-record is a different trigger

- **A14_FEATURE_ID**: `DisableKeyguardEditorFeatureId`
  - A14_PREF_KEYS: `system_lockscreen_disable_edit`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_lockscreen_disable_edit; DisableKeyguardEditorFeatureId; Disable Keyguard Editor`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_lockscreen_disable_edit`: no match
- feature id `DisableKeyguardEditorFeatureId`: no match
- title `Disable Keyguard Editor`: no match
- token 'lockscreen' `lockscreen`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_lockscreen_disable_strongauth_72h, system_lockscreen_hidezenmode
- nearest A13 candidate `system_lockscreen_disable_strongauth_72h` inspected and rejected because it does not implement `system_lockscreen_disable_edit` behavior

- **A14_FEATURE_ID**: `A14_UI_system_netspeed_boldfont`
  - A14_PREF_KEYS: `system_netspeed_boldfont`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_netspeed_boldfont; A14_UI_system_netspeed_boldfont; Bold style; system_netspeed_bold`
  - A13_MATCH: `system_netspeed_bold`
  - A13_REFERENCE: `res/xml/prefs_system_detailednetspeed.xml; mods/SystemUIStatusBarHooks.kt::NetSpeedTypefaceHelper`
  - FINAL_PARITY_STATE: `PRESENT_A13_VARIANT`
  - RECLASSIFICATION_REASON: Same user capability: bold network-speed typeface. A14 renamed the key.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_netspeed_boldfont` has no identical A13 key
- matched A13 `system_netspeed_bold` at res/xml/prefs_system_detailednetspeed.xml; mods/SystemUIStatusBarHooks.kt::NetSpeedTypefaceHelper
- same user capability: Same user capability: bold network-speed typeface. A14 renamed the key.

- **A14_FEATURE_ID**: `A14_UI_system_netspeed_use_clock_style`
  - A14_PREF_KEYS: `system_netspeed_use_clock_style`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_netspeed_use_clock_style; A14_UI_system_netspeed_use_clock_style; Match status bar clock text style; system_netspeed_bold; system_netspeed_fontsize`
  - A13_MATCH: `system_netspeed_bold,system_netspeed_fontsize`
  - A13_REFERENCE: `mods/SystemUIStatusBarHooks.kt::NetSpeedTypefaceHelper`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A13 already customizes netspeed typeface/size; A14 adds match-clock-style on the same helper.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_netspeed_use_clock_style` has no identical A13 key
- matched A13 `system_netspeed_bold,system_netspeed_fontsize` at mods/SystemUIStatusBarHooks.kt::NetSpeedTypefaceHelper
- A14 materially extends existing A13 semantics: A13 already customizes netspeed typeface/size; A14 adds match-clock-style on the same helper.

- **A14_FEATURE_ID**: `DisableFoldNotificationsFeatureId`
  - A14_PREF_KEYS: `system_notif_disable_fold`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_notif_disable_fold; DisableFoldNotificationsFeatureId; Disable Fold Notifications`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_notif_disable_fold`: no match
- feature id `DisableFoldNotificationsFeatureId`: no match
- title `Disable Fold Notifications`: no match
- token 'notif' `notif`: hits app/src/main/java/tv/withaibuild/customiuizer/MainApplication.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `DisableFakeClockAnimFeatureId`
  - A14_PREF_KEYS: `system_qs_disable_fakeclock_anim`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_qs_disable_fakeclock_anim; DisableFakeClockAnimFeatureId; Disable Fake Clock Anim`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_qs_disable_fakeclock_anim`: no match
- feature id `DisableFakeClockAnimFeatureId`: no match
- title `Disable Fake Clock Anim`: no match
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'fakeclock' `fakeclock`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_qs_force_systemfonts, system_qs_hideoperator
- nearest A13 candidate `system_qs_force_systemfonts` inspected and rejected because it does not implement `system_qs_disable_fakeclock_anim` behavior

- **A14_FEATURE_ID**: `LauncherRecentsCardStyleFeatureId`
  - A14_PREF_KEYS: `system_recents_card_style`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/LauncherPostAttachFeatures.kt`
  - A13_SEARCH_TERMS: `system_recents_card_style; LauncherRecentsCardStyleFeatureId; Launcher Recents Card Style`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_recents_card_style`: no match
- feature id `LauncherRecentsCardStyleFeatureId`: no match
- title `Launcher Recents Card Style`: no match
- token 'recents' `recents`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt
- token 'style' `style`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/NetSpeedTypefaceHelper.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_recents_blur, system_recents_disable_wallpaperscale, system_recents_hide_statusbar
- nearest A13 candidate `system_recents_blur` inspected and rejected because it does not implement `system_recents_card_style` behavior

- **A14_FEATURE_ID**: `StatusBarContentGeometryFeatureId`
  - A14_PREF_KEYS: `system_statusbar_content_vertical_offset`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_statusbar_content_vertical_offset; StatusBarContentGeometryFeatureId; Status Bar Content Geometry`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_content_vertical_offset`: no match
- feature id `StatusBarContentGeometryFeatureId`: no match
- title `Status Bar Content Geometry`: no match
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'content' `content`: hits app/src/main/java/tv/withaibuild/customiuizer/AboutFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt, app/src/main/java/tv/withaibuild/customiuizer/CredentialsShortcut.kt, app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt
- token 'vertical' `vertical`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt
- token 'offset' `offset`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_alarm_atleft, system_statusbar_alarm_atright, system_statusbar_batterystyle, system_statusbar_batterystyle_bold
- nearest A13 candidate `system_statusbar_alarm_atleft` inspected and rejected because it does not implement `system_statusbar_content_vertical_offset` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_dualrows_left_ratio`
  - A14_PREF_KEYS: `system_statusbar_dualrows_left_ratio`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_dualrows_left_ratio; A14_UI_system_statusbar_dualrows_left_ratio; Set left width for device without notches; system_statusbar_dualrows; system_statusbar_dualrows_firstrow_horizmargin`
  - A13_MATCH: `system_statusbar_dualrows,system_statusbar_dualrows_firstrow_horizmargin`
  - A13_REFERENCE: `mods/SystemUIStatusBarHooks.kt; res/xml/prefs_system.xml`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A13 dual-row status bar exists with first-row padding; A14 adds left-width ratio.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_statusbar_dualrows_left_ratio` has no identical A13 key
- matched A13 `system_statusbar_dualrows,system_statusbar_dualrows_firstrow_horizmargin` at mods/SystemUIStatusBarHooks.kt; res/xml/prefs_system.xml
- A14 materially extends existing A13 semantics: A13 dual-row status bar exists with first-row padding; A14 adds left-width ratio.

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_enable_weather_param`
  - A14_PREF_KEYS: `system_statusbar_enable_weather_param`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_enable_weather_param; A14_UI_system_statusbar_enable_weather_param; Use tq to display weather when formatting`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_enable_weather_param`: no match
- feature id `A14_UI_system_statusbar_enable_weather_param`: no match
- title `Use tq to display weather when formatting`: no match
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'enable' `enable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java
- token 'weather' `weather`: no match
- token 'param' `param`: hits app/src/main/java/tv/withaibuild/customiuizer/AboutFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt, app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_alarm_atleft, system_statusbar_alarm_atright, system_statusbar_batterystyle, system_statusbar_batterystyle_bold
- nearest A13 candidate `system_statusbar_alarm_atleft` inspected and rejected because it does not implement `system_statusbar_enable_weather_param` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_icons_atleft_onkeyguard`
  - A14_PREF_KEYS: `system_statusbar_icons_atleft_onkeyguard`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_icons_atleft_onkeyguard; A14_UI_system_statusbar_icons_atleft_onkeyguard; Move to the left on the lock screen`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_icons_atleft_onkeyguard`: no match
- feature id `A14_UI_system_statusbar_icons_atleft_onkeyguard`: no match
- title `Move to the left on the lock screen`: no match
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'icons' `icons`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt
- token 'atleft' `atleft`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt, app/src/main/res/xml/prefs_system_statusbar_mobilesignal.xml, app/src/main/res/xml/prefs_system_statusbar_righticons.xml
- token 'onkeyguard' `onkeyguard`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_alarm_atleft, system_statusbar_alarm_atright, system_statusbar_batterystyle, system_statusbar_batterystyle_bold
- nearest A13 candidate `system_statusbar_alarm_atleft` inspected and rejected because it does not implement `system_statusbar_icons_atleft_onkeyguard` behavior

- **A14_FEATURE_ID**: `StatusBarDigitalSignalFeatureId`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal; StatusBarDigitalSignalFeatureId; Status Bar Digital Signal`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal`: no match
- feature id `StatusBarDigitalSignalFeatureId`: no match
- title `Status Bar Digital Signal`: no match
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_align`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_align`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_align; A14_UI_system_statusbar_mobile_digital_signal_align; Horizontal alignment`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_align`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_align`: no match
- title `Horizontal alignment`: hits app/src/main/res/values/strings.xml
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- token 'align' `align`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_align` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_bold`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_bold`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_bold; A14_UI_system_statusbar_mobile_digital_signal_bold; Bold style`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_bold`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_bold`: no match
- title `Bold style`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/NetSpeedTypefaceHelper.kt, app/src/main/res/values/strings.xml
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_bold` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_fontsize`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_fontsize`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_fontsize; A14_UI_system_statusbar_mobile_digital_signal_fontsize; Font size`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_fontsize`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_fontsize`: no match
- title `Font size`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt, app/src/main/res/values/strings.xml
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- token 'fontsize' `fontsize`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_fontsize` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_hideunit`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_hideunit`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_hideunit; A14_UI_system_statusbar_mobile_digital_signal_hideunit; Hide unit`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_hideunit`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_hideunit`: no match
- title `Hide unit`: hits app/src/main/res/values/strings.xml
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- token 'hideunit' `hideunit`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt, app/src/main/res/xml/prefs_system_statusbar_batterytempandcurrent.xml, app/src/main/res/xml/prefs_system_statusbar_showdevicetemperature.xml
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_hideunit` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_in2rows`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_in2rows`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_in2rows; A14_UI_system_statusbar_mobile_digital_signal_in2rows; Display as dual rows when phone has dual SIM cards`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_in2rows`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_in2rows`: no match
- title `Display as dual rows when phone has dual SIM cards`: no match
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- token 'in2rows' `in2rows`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt, app/src/main/res/xml/prefs_system.xml, app/src/main/res/xml/prefs_system_detailednetspeed.xml
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_in2rows` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_leftmargin`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_leftmargin`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_leftmargin; A14_UI_system_statusbar_mobile_digital_signal_leftmargin; Left margin`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_leftmargin`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_leftmargin`: no match
- title `Left margin`: hits app/src/main/res/values/strings.xml
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- token 'leftmargin' `leftmargin`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_leftmargin` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_rightmargin`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_rightmargin`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_rightmargin; A14_UI_system_statusbar_mobile_digital_signal_rightmargin; Right margin`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_rightmargin`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_rightmargin`: no match
- title `Right margin`: hits app/src/main/res/values/strings.xml
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- token 'rightmargin' `rightmargin`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_rightmargin` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbar_mobile_digital_signal_verticaloffset`
  - A14_PREF_KEYS: `system_statusbar_mobile_digital_signal_verticaloffset`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbar_mobile_digital_signal_verticaloffset; A14_UI_system_statusbar_mobile_digital_signal_verticaloffset; Vertical offset`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbar_mobile_digital_signal_verticaloffset`: no match
- feature id `A14_UI_system_statusbar_mobile_digital_signal_verticaloffset`: no match
- title `Vertical offset`: hits app/src/main/res/values/strings.xml
- token 'statusbar' `statusbar`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java
- token 'mobile' `mobile`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt
- token 'digital' `digital`: no match
- token 'signal' `signal`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt
- token 'verticaloffset' `verticaloffset`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbar_mobile_showname, system_statusbar_mobile_signal_cat, system_statusbar_alarm_atleft, system_statusbar_alarm_atright
- nearest A13 candidate `system_statusbar_mobile_showname` inspected and rejected because it does not implement `system_statusbar_mobile_digital_signal_verticaloffset` behavior

- **A14_FEATURE_ID**: `A14_UI_system_statusbarcontrols_dt_left`
  - A14_PREF_KEYS: `system_statusbarcontrols_dt_left`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbarcontrols_dt_left; A14_UI_system_statusbarcontrols_dt_left; Double tap left corner action; system_statusbarcontrols_dt`
  - A13_MATCH: `system_statusbarcontrols_dt`
  - A13_REFERENCE: `mods/SystemUIControlCenterHooks.kt; res/xml/prefs_system_statusbarcontrols.xml`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A13 has one status-bar double-tap action; A14 splits left-corner double-tap.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_statusbarcontrols_dt_left` has no identical A13 key
- matched A13 `system_statusbarcontrols_dt` at mods/SystemUIControlCenterHooks.kt; res/xml/prefs_system_statusbarcontrols.xml
- A14 materially extends existing A13 semantics: A13 has one status-bar double-tap action; A14 splits left-corner double-tap.

- **A14_FEATURE_ID**: `A14_UI_system_statusbarcontrols_dt_right`
  - A14_PREF_KEYS: `system_statusbarcontrols_dt_right`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbarcontrols_dt_right; A14_UI_system_statusbarcontrols_dt_right; Double tap right corner action; system_statusbarcontrols_dt`
  - A13_MATCH: `system_statusbarcontrols_dt`
  - A13_REFERENCE: `mods/SystemUIControlCenterHooks.kt; res/xml/prefs_system_statusbarcontrols.xml`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A13 has one status-bar double-tap action; A14 splits right-corner double-tap.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_statusbarcontrols_dt_right` has no identical A13 key
- matched A13 `system_statusbarcontrols_dt` at mods/SystemUIControlCenterHooks.kt; res/xml/prefs_system_statusbarcontrols.xml
- A14 materially extends existing A13 semantics: A13 has one status-bar double-tap action; A14 splits right-corner double-tap.

- **A14_FEATURE_ID**: `A14_UI_system_statusbaricons_bluetoothicn`
  - A14_PREF_KEYS: `system_statusbaricons_bluetoothicn`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbaricons_bluetoothicn; A14_UI_system_statusbaricons_bluetoothicn; Bluetooth; system_statusbaricons_bluetooth`
  - A13_MATCH: `system_statusbaricons_bluetooth`
  - A13_REFERENCE: `mods/SystemStatusBarMoreHooks.kt::HideIconsBluetoothHook`
  - FINAL_PARITY_STATE: `PRESENT_A13_VARIANT`
  - RECLASSIFICATION_REASON: A13 HideIconsBluetoothHook option 3 already always-hides the bluetooth icon.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_statusbaricons_bluetoothicn` has no identical A13 key
- matched A13 `system_statusbaricons_bluetooth` at mods/SystemStatusBarMoreHooks.kt::HideIconsBluetoothHook
- same user capability: A13 HideIconsBluetoothHook option 3 already always-hides the bluetooth icon.

- **A14_FEATURE_ID**: `HidePrivacyIndicatorFeatureId`
  - A14_PREF_KEYS: `system_statusbaricons_privacy_prompt`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_statusbaricons_privacy_prompt; HidePrivacyIndicatorFeatureId; Hide Privacy Indicator`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_statusbaricons_privacy_prompt`: no match
- feature id `HidePrivacyIndicatorFeatureId`: no match
- title `Hide Privacy Indicator`: no match
- token 'statusbaricons' `statusbaricons`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt
- token 'privacy' `privacy`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'prompt' `prompt`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: system_statusbaricons_privacy, system_statusbaricons_airplane, system_statusbaricons_alarm, system_statusbaricons_alarmn
- nearest candidate `system_statusbaricons_privacy` inspected and rejected because A13 privacy key hides incognito/stealth; privacy_prompt is the camera/mic privacy indicator

- **A14_FEATURE_ID**: `A14_UI_system_statusbaricons_wireless_headset`
  - A14_PREF_KEYS: `system_statusbaricons_wireless_headset`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_statusbaricons_wireless_headset; A14_UI_system_statusbaricons_wireless_headset; Wireless headset; system_statusbaricons_headset`
  - A13_MATCH: `system_statusbaricons_headset`
  - A13_REFERENCE: `mods/SystemUIStatusBarHooks.kt; res/xml/prefs_system_hideicons.xml`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A13 hides the headset slot; A14 adds a separate wireless_headset slot on the same hide-icons path.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_statusbaricons_wireless_headset` has no identical A13 key
- matched A13 `system_statusbaricons_headset` at mods/SystemUIStatusBarHooks.kt; res/xml/prefs_system_hideicons.xml
- A14 materially extends existing A13 semantics: A13 hides the headset slot; A14 adds a separate wireless_headset slot on the same hide-icons path.

- **A14_FEATURE_ID**: `A14_UI_system_strong_toast_island_offset`
  - A14_PREF_KEYS: `system_strong_toast_island_offset`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_strong_toast_island_offset; A14_UI_system_strong_toast_island_offset; Dynamic Island vertical position; dynamic_island`
  - A13_MATCH: `dynamic_island`
  - A13_REFERENCE: `ABSENT (Dynamic Island excluded)`
  - FINAL_PARITY_STATE: `HOLD_EVIDENCE`
  - RECLASSIFICATION_REASON: Dynamic Island helper preference; product policy forbids extra DI gaps. Keep HOLD_EVIDENCE, do not port.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_strong_toast_island_offset`: no A13 implementation
- classified HOLD because Dynamic Island helper preference; product policy forbids extra DI gaps. Keep HOLD_EVIDENCE, do not port.

- **A14_FEATURE_ID**: `StrongToastPresentationFeatureId`
  - A14_PREF_KEYS: `system_strong_toast_mode`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
  - A13_SEARCH_TERMS: `system_strong_toast_mode; StrongToastPresentationFeatureId; Strong Toast Presentation`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_strong_toast_mode`: no match
- feature id `StrongToastPresentationFeatureId`: no match
- title `Strong Toast Presentation`: no match
- token 'strong' `strong`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt
- token 'toast' `toast`: hits app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt, app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest candidate `system_notif_disable_strong_toast (absent)` inspected and rejected because A13 has no status-capsule / strong-toast presentation path; island mode is DI-excluded

- **A14_FEATURE_ID**: `UsbDefaultFunctionFeatureId`
  - A14_PREF_KEYS: `system_usb_default_function`
  - A14_BEHAVIOR: Sets default USB function behavior (follow system/charge/MTP/PTP) via SystemUsbDefaultHooks.
  - A14_REFERENCE: `mods/utils/feature/SystemServerFeatures.kt::UsbDefaultFunctionFeatureId`
  - A13_SEARCH_TERMS: `system_usb_default_function; UsbDefaultFunctionFeatureId; USB Default Function; system_defaultusb; system_defaultusb_unsecure`
  - A13_MATCH: `system_defaultusb,system_defaultusb_unsecure`
  - A13_REFERENCE: `mods/SystemSettingsMoreHooks.kt::USBConfigHook,USBConfigSettingsHook`
  - FINAL_PARITY_STATE: `PARTIAL_PARITY`
  - RECLASSIFICATION_REASON: A14 renamed key; A13 already owns USB default via USBConfigHook/USBConfigSettingsHook.
  - ABSENCE_PROOF: A13_SEARCHED =
- A14 key `system_usb_default_function` has no identical A13 key
- matched A13 `system_defaultusb,system_defaultusb_unsecure` at mods/SystemSettingsMoreHooks.kt::USBConfigHook,USBConfigSettingsHook
- A14 materially extends existing A13 semantics: A14 renamed key; A13 already owns USB default via USBConfigHook/USBConfigSettingsHook.

- **A14_FEATURE_ID**: `A14_UI_system_volume_hide_dnd_shortcut`
  - A14_PREF_KEYS: `system_volume_hide_dnd_shortcut`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_volume_hide_dnd_shortcut; A14_UI_system_volume_hide_dnd_shortcut; Hide DND shortcut`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_volume_hide_dnd_shortcut`: no match
- feature id `A14_UI_system_volume_hide_dnd_shortcut`: no match
- title `Hide DND shortcut`: no match
- token 'volume' `volume`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
- token 'shortcut' `shortcut`: hits app/src/main/java/tv/withaibuild/customiuizer/CredentialsShortcut.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest candidate `MIUIVolumeDialogHook` inspected and rejected because A13 volume hook covers autohide/blur, not DND shortcut visibility

- **A14_FEATURE_ID**: `A14_UI_system_volume_hide_mute_shortcut`
  - A14_PREF_KEYS: `system_volume_hide_mute_shortcut`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_volume_hide_mute_shortcut; A14_UI_system_volume_hide_mute_shortcut; Hide Mute shortcut`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_volume_hide_mute_shortcut`: no match
- feature id `A14_UI_system_volume_hide_mute_shortcut`: no match
- title `Hide Mute shortcut`: no match
- token 'volume' `volume`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
- token 'shortcut' `shortcut`: hits app/src/main/java/tv/withaibuild/customiuizer/CredentialsShortcut.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest candidate `MIUIVolumeDialogHook` inspected and rejected because A13 volume hook covers autohide/blur, not mute shortcut visibility

- **A14_FEATURE_ID**: `A14_UI_system_volume_mode_button_background_color`
  - A14_PREF_KEYS: `system_volume_mode_button_background_color`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_volume_mode_button_background_color; A14_UI_system_volume_mode_button_background_color; Background color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_volume_mode_button_background_color`: no match
- feature id `A14_UI_system_volume_mode_button_background_color`: no match
- title `Background color`: hits app/src/main/res/values/strings.xml
- token 'volume' `volume`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
- token 'background' `background`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `A14_UI_system_volume_mode_button_colors`
  - A14_PREF_KEYS: `system_volume_mode_button_colors`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_volume_mode_button_colors; A14_UI_system_volume_mode_button_colors; Silent and DND shortcut colors`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_volume_mode_button_colors`: no match
- feature id `A14_UI_system_volume_mode_button_colors`: no match
- title `Silent and DND shortcut colors`: no match
- token 'volume' `volume`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
- token 'colors' `colors`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `A14_UI_system_volume_mode_button_icon_color`
  - A14_PREF_KEYS: `system_volume_mode_button_icon_color`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `system_volume_mode_button_icon_color; A14_UI_system_volume_mode_button_icon_color; Icon color`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `system_volume_mode_button_icon_color`: no match
- feature id `A14_UI_system_volume_mode_button_icon_color`: no match
- title `Icon color`: no match
- token 'volume' `volume`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `A14_UI_various_block_location_permission_prompts`
  - A14_PREF_KEYS: `various_block_location_permission_prompts`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_block_location_permission_prompts; A14_UI_various_block_location_permission_prompts; Dismiss location permission prompts`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_block_location_permission_prompts`: no match
- feature id `A14_UI_various_block_location_permission_prompts`: no match
- title `Dismiss location permission prompts`: no match
- token 'block' `block`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt
- token 'location' `location`: hits app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt
- token 'permission' `permission`: hits app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt
- token 'prompts' `prompts`: no match
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `A14_UI_various_block_notification_permission_prompts`
  - A14_PREF_KEYS: `various_block_notification_permission_prompts`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_block_notification_permission_prompts; A14_UI_various_block_notification_permission_prompts; Dismiss notification permission prompts`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_block_notification_permission_prompts`: no match
- feature id `A14_UI_various_block_notification_permission_prompts`: no match
- title `Dismiss notification permission prompts`: no match
- token 'block' `block`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt
- token 'notification' `notification`: hits app/src/main/java/tv/withaibuild/customiuizer/MainApplication.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java
- token 'permission' `permission`: hits app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt
- token 'prompts' `prompts`: no match
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `A14_UI_various_disable_miui_daemon`
  - A14_PREF_KEYS: `various_disable_miui_daemon`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_disable_miui_daemon; A14_UI_various_disable_miui_daemon; Force-disable System Quality Service`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_disable_miui_daemon`: no match
- feature id `A14_UI_various_disable_miui_daemon`: no match
- title `Force-disable System Quality Service`: no match
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'daemon' `daemon`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: various_disable_access_devicelogs, various_disable_dock_suggest, various_disable_freeform_suggest_blacklist
- nearest A13 candidate `various_disable_access_devicelogs` inspected and rejected because it does not implement `various_disable_miui_daemon` behavior

- **A14_FEATURE_ID**: `SecurityCenterPersistPrivacyThumbnailBlurFeatureId`
  - A14_PREF_KEYS: `various_disable_reset_recents_privacy_blur`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SecurityCenterFeatures.kt`
  - A13_SEARCH_TERMS: `various_disable_reset_recents_privacy_blur; SecurityCenterPersistPrivacyThumbnailBlurFeatureId; Security Center Persist Privacy Thumbnail Blur`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_disable_reset_recents_privacy_blur`: no match
- feature id `SecurityCenterPersistPrivacyThumbnailBlurFeatureId`: no match
- title `Security Center Persist Privacy Thumbnail Blur`: no match
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'reset' `reset`: hits app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt, app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt
- token 'recents' `recents`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt
- token 'privacy' `privacy`: hits app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: various_disable_access_devicelogs, various_disable_dock_suggest, various_disable_freeform_suggest_blacklist
- nearest candidate `system_recents_blur` inspected and rejected because RecentsBlurRatioHook is recents background blur intensity, not privacy-thumbnail persist

- **A14_FEATURE_ID**: `A14_UI_various_disable_update_services`
  - A14_PREF_KEYS: `various_disable_update_services`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_disable_update_services; A14_UI_various_disable_update_services; Disable Xiaomi updater services`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_disable_update_services`: no match
- feature id `A14_UI_various_disable_update_services`: no match
- title `Disable Xiaomi updater services`: no match
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'update' `update`: hits app/src/main/java/tv/withaibuild/customiuizer/PrefsProvider.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt
- token 'services' `services`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt, app/src/main/java/tv/withaibuild/customiuizer/subs/WiFiList.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/CatalogContracts.kt
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: various_disable_access_devicelogs, various_disable_dock_suggest, various_disable_freeform_suggest_blacklist
- nearest A13 candidate `various_disable_access_devicelogs` inspected and rejected because it does not implement `various_disable_update_services` behavior

- **A14_FEATURE_ID**: `A14_UI_various_disable_xiaomi_analytics`
  - A14_PREF_KEYS: `various_disable_xiaomi_analytics`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_disable_xiaomi_analytics; A14_UI_various_disable_xiaomi_analytics; Disable Xiaomi analytics and system ads`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_disable_xiaomi_analytics`: no match
- feature id `A14_UI_various_disable_xiaomi_analytics`: no match
- title `Disable Xiaomi analytics and system ads`: no match
- token 'disable' `disable`: hits app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'xiaomi' `xiaomi`: hits app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt
- token 'analytics' `analytics`: hits app/src/main/res/values/strings.xml
- FeatureCatalog/installer/schema: no owner for this key
- nearest A13 keys: various_disable_access_devicelogs, various_disable_dock_suggest, various_disable_freeform_suggest_blacklist
- nearest A13 candidate `various_disable_access_devicelogs` inspected and rejected because it does not implement `various_disable_xiaomi_analytics` behavior

- **A14_FEATURE_ID**: `SecurityCenterHideReportButtonFeatureId`
  - A14_PREF_KEYS: `various_hide_report_ondetails`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SecurityCenterFeatures.kt`
  - A13_SEARCH_TERMS: `various_hide_report_ondetails; SecurityCenterHideReportButtonFeatureId; Security Center Hide Report Button`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_hide_report_ondetails`: no match
- feature id `SecurityCenterHideReportButtonFeatureId`: no match
- title `Security Center Hide Report Button`: no match
- token 'report' `report`: hits app/src/main/java/tv/withaibuild/customiuizer/MainActivity.kt, app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt
- token 'ondetails' `ondetails`: no match
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `PackageInstallerPurifyFeatureId`
  - A14_PREF_KEYS: `various_installer_purify`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `C:/Users/tv/Downloads/Peengeek/customiuizer-a14-forDevin/app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/PackageInstallerFeatures.kt`
  - A13_SEARCH_TERMS: `various_installer_purify; PackageInstallerPurifyFeatureId; Package Installer Purify`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_installer_purify`: no match
- feature id `PackageInstallerPurifyFeatureId`: no match
- title `Package Installer Purify`: no match
- token 'installer' `installer`: hits app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java
- token 'purify' `purify`: no match
- FeatureCatalog/installer/schema: no owner for this key
- nearest candidate `various_miuiinstaller` inspected and rejected because MiuiPackageInstallerHook forces the MIUI installer; purify removes installer UI clutter

- **A14_FEATURE_ID**: `A14_UI_various_remove_security_center_antivirus`
  - A14_PREF_KEYS: `various_remove_security_center_antivirus`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_remove_security_center_antivirus; A14_UI_various_remove_security_center_antivirus; Remove Security Center antivirus and reminders`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_remove_security_center_antivirus`: no match
- feature id `A14_UI_various_remove_security_center_antivirus`: no match
- title `Remove Security Center antivirus and reminders`: no match
- token 'remove' `remove`: hits app/src/main/java/tv/withaibuild/customiuizer/MainApplication.kt, app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragmentWithSearch.kt
- token 'security' `security`: hits app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt
- token 'center' `center`: hits app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'antivirus' `antivirus`: no match
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `A14_UI_various_trim_miui_daemon_network`
  - A14_PREF_KEYS: `various_trim_miui_daemon_network`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_trim_miui_daemon_network; A14_UI_various_trim_miui_daemon_network; Trim Daemon cloud control and uploads`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_trim_miui_daemon_network`: no match
- feature id `A14_UI_various_trim_miui_daemon_network`: no match
- title `Trim Daemon cloud control and uploads`: no match
- token 'daemon' `daemon`: no match
- token 'network' `network`: hits app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/NetSpeedTypefaceHelper.kt, app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt
- FeatureCatalog/installer/schema: no owner for this key

- **A14_FEATURE_ID**: `A14_UI_various_trim_security_center_marketing`
  - A14_PREF_KEYS: `various_trim_security_center_marketing`
  - A14_BEHAVIOR: Preference-backed behavior; semantic proof pending.
  - A14_REFERENCE: `inferred-from-ui-topology`
  - A13_SEARCH_TERMS: `various_trim_security_center_marketing; A14_UI_various_trim_security_center_marketing; Trim Security Center marketing startup`
  - A13_MATCH: `ABSENT`
  - A13_REFERENCE: `ABSENT`
  - FINAL_PARITY_STATE: `MISSING_IN_A13`
  - RECLASSIFICATION_REASON: No A13 equivalent after feature-specific source review.
  - ABSENCE_PROOF: A13_SEARCHED =
- key `various_trim_security_center_marketing`: no match
- feature id `A14_UI_various_trim_security_center_marketing`: no match
- title `Trim Security Center marketing startup`: no match
- token 'security' `security`: hits app/src/main/java/tv/withaibuild/customiuizer/Credentials.kt, app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt
- token 'center' `center`: hits app/src/main/java/tv/withaibuild/customiuizer/MainModule.java, app/src/main/java/tv/withaibuild/customiuizer/PreferenceFragmentBase.kt, app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt, app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java
- token 'marketing' `marketing`: no match
- FeatureCatalog/installer/schema: no owner for this key

