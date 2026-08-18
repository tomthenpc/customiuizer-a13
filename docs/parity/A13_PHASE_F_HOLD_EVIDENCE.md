# A13 Phase F-R4 HOLD_EVIDENCE

HOLD_EVIDENCE_COUNT = 62
DEAD_UPSTREAM_PATH_COUNT = 0
SOURCE_REVIEW_REQUIRED = 469

Final HOLD_EVIDENCE rows are ROM_DEVICE_HOLD only: ROM ABI, class/member, layout/view identity,
device behavior, or boot/system_server risk. Module-owned app logic is not parked here.
SOURCE_REVIEW_REQUIRED is not HOLD_EVIDENCE.

## launcher_wallpaper_colormode

- unresolved_question: Launcher wallpaper color-mode API on MIUI 14 Home vs HyperOS 1
- affected_rom_process: com.miui.home
- safe_default: ROM wallpaper coloring
- required_device_evidence: DeviceConfig/wallpaper color-mode field names
- why_static_source_cannot_decide: No A13 counterpart; speculative GlobalLauncher/DeviceConfig writes are forbidden.

## system_autobrightness_reset_when_screenoff

- unresolved_question: DisplayPowerController.setScreenState(int,boolean) intercept vs existing A13 initialize hook
- affected_rom_process: android / system_server
- safe_default: stock auto-brightness
- required_device_evidence: setScreenState signature and chain.proceed interaction on MIUI 14
- why_static_source_cannot_decide: A13 already hooks DisplayPowerController.initialize. Adding an intercept/chain.proceed path is a boot-safety choice that is not unique statically.

## system_cc_btandtorch_ascard

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_color_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_iconcolor_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_primary_textcolor

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_secondary_textcolor

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_clock_centeralign

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_floatingtimetile

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_freeform_when_longclick

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_hide_edit

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_hide_profile_monitoring

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_slider_color_enable

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_slider_icon_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_slider_progress_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_tile_enabled_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_tile_enabled_color_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_tile_enabled_iconcolor_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_static_source_cannot_decide: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_detailednetspeed_style

- unresolved_question: Replacing live A13 detailed/fakedualrow toggles with an A14 list selector would migrate stored prefs.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: Detailed netspeed and fake dual-row already exist; selector migration is not statically safe.

## system_disable_window_blurs

- unresolved_question: Whether MIUI 14 system_server BlurController matches AOSP getBlurDisabledSetting
- affected_rom_process: android / system_server
- safe_default: ROM blur policy
- required_device_evidence: system_server BlurController members on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: system_server boot path; A14 also adds a live PreferenceObserver. A13 catalog/contract wiring plus overlay ROM variants cannot be proven statically.

## system_drawer_date_centeralign

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_static_source_cannot_decide: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_date_fontsize

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_static_source_cannot_decide: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_dateformat

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_static_source_cannot_decide: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_hidedate

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_static_source_cannot_decide: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_remove_emptynotify

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_static_source_cannot_decide: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_force_darken_allapps

- unresolved_question: ForceDarkAppListProvider/Manager presence vs A13 NoDarkForceHook
- affected_rom_process: android / system_server
- safe_default: stock per-app dark list
- required_device_evidence: ForceDark* class dump; interaction with system_nodarkforce
- why_static_source_cannot_decide: A14 uses ForceDarkAppList* ; A13 already owns the opposite NoDarkForceHook. Parallel implementation is forbidden.

## system_lockscreen_disable_edit

- unresolved_question: Keyguard editor entry class on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock keyguard editor
- required_device_evidence: Lockscreen editor activity dump
- why_static_source_cannot_decide: A14 disables a HyperOS keyguard editor not proven on MIUI 14.

## system_notif_disable_fold

- unresolved_question: MIUI fold-notification controller on A13
- affected_rom_process: com.android.systemui
- safe_default: stock fold notifications
- required_device_evidence: Notification fold policy class dump
- why_static_source_cannot_decide: No A13 fold-notification hook family.

## system_qs_disable_fakeclock_anim

- unresolved_question: Fake-clock animation owner on MIUI 14 QS/CC
- affected_rom_process: com.android.systemui / miui.systemui.plugin
- safe_default: stock fake clock
- required_device_evidence: QS clock animator class dump
- why_static_source_cannot_decide: A14 fake-clock hook is plugin-CC specific.

## system_recents_card_style

- unresolved_question: Recents card style controller class on MIUI 14 Home
- affected_rom_process: com.miui.home / com.android.systemui
- safe_default: stock recents cards
- required_device_evidence: Recents container class dump
- why_static_source_cannot_decide: A14 recents card-style is a new view path, not an upgrade of A13 recents blur.

## system_statusbar_content_vertical_offset

- unresolved_question: Status-bar content geometry owner on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock geometry
- required_device_evidence: Collapsed status bar layout dump
- why_static_source_cannot_decide: New geometry rewrite; high visual-regression risk without device proof.

## system_statusbar_enable_weather_param

- unresolved_question: Weather status-bar param API on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock weather
- required_device_evidence: Weather controller class dump
- why_static_source_cannot_decide: No A13 weather-param hook.

## system_statusbar_mobile_digital_signal

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_align

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_bold

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_fontsize

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_hideunit

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_in2rows

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_leftmargin

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_rightmargin

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_verticaloffset

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_static_source_cannot_decide: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbarcontrols_dt_left

- unresolved_question: Left/right hit-testing needs device geometry; A13 already has one whole-bar double-tap action.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: Single system_statusbarcontrols_dt handles the whole bar.

## system_statusbarcontrols_dt_right

- unresolved_question: Left/right hit-testing needs device geometry; A13 already has one whole-bar double-tap action.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: Single system_statusbarcontrols_dt handles the whole bar.

## system_statusbaricons_privacy_prompt

- unresolved_question: Camera/mic privacy-indicator slot name on MIUI 14 SystemUI
- affected_rom_process: com.android.systemui
- safe_default: stock privacy indicator
- required_device_evidence: Status bar slot dump
- why_static_source_cannot_decide: A13 system_statusbaricons_privacy hides incognito/stealth, not the privacy chip.

## system_strong_toast_mode

- unresolved_question: Status-capsule / strong-toast presenter class on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock toast/capsule
- required_device_evidence: Strong toast / island presenter dump
- why_static_source_cannot_decide: No A13 capsule path; island mode is DI-adjacent.

## system_volume_hide_dnd_shortcut

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_static_source_cannot_decide: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_hide_mute_shortcut

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_static_source_cannot_decide: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_mode_button_background_color

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_static_source_cannot_decide: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_mode_button_colors

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_static_source_cannot_decide: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_mode_button_icon_color

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_static_source_cannot_decide: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## various_block_location_permission_prompts

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_block_notification_permission_prompts

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_clear_update_state

- unresolved_question: Whether MIUI 14 Settings.Global miui_new_version/miui_update_ready plus com.android.updater clearApplicationUserData match the HyperOS updater-state cache
- affected_rom_process: android / com.android.updater
- safe_default: no one-shot action; ROM updater reminder unchanged
- required_device_evidence: MIUI 14 updater package dump, Settings.Global key names, and ActivityManager.clearApplicationUserData 4-arg result on API33
- why_static_source_cannot_decide: A14 one-shot lives in the HyperOS updater-services bridge (same system_server owner as various_disable_update_services, already HOLD). Product copy and Global keys are HyperOS-branded. Privileged system_server data wipe cannot be decided from A13 source.

## various_disable_defraud_apps_detect

- unresolved_question: Whether MIUI 14 com.miui.guardprovider contains AntiDefraudAppManager / getUnSystemAppList strings used by A14 DexKit
- affected_rom_process: com.miui.guardprovider
- safe_default: feature off / ROM fraud-app scan unchanged
- required_device_evidence: GuardProvider DEX dump on MIUI 14 and HyperOS 1 A13 showing the DexKit string pair
- why_static_source_cannot_decide: A14 DisableDefraudAppsCheck is DexKit-only against GuardProvider. No A13 owner, installer, or fixed class/member. Fail-open would hide a dead toggle.

## various_disable_miui_daemon

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_disable_reset_recents_privacy_blur

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_disable_update_services

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_disable_xiaomi_analytics

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_remove_security_center_antivirus

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_trim_miui_daemon_network

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_trim_security_center_marketing

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_static_source_cannot_decide: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.
