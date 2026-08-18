# A13 Phase F HOLD_EVIDENCE

HOLD_EVIDENCE_COUNT = 67
DEAD_UPSTREAM_PATH_COUNT = 24

Each HOLD records the unresolved question, ROM/process, safe default, required device evidence, and why production is forbidden.

## launcher_wallpaper_colormode

- unresolved_question: Launcher wallpaper color-mode API on MIUI 14 Home vs HyperOS 1
- affected_rom_process: com.miui.home
- safe_default: ROM wallpaper coloring
- required_device_evidence: DeviceConfig/wallpaper color-mode field names
- why_forbidden: No A13 counterpart; speculative GlobalLauncher/DeviceConfig writes are forbidden.

## system_autobrightness_reset_when_screenoff

- unresolved_question: DisplayPowerController.setScreenState(int,boolean) intercept vs existing A13 initialize hook
- affected_rom_process: android / system_server
- safe_default: stock auto-brightness
- required_device_evidence: setScreenState signature and chain.proceed interaction on MIUI 14
- why_forbidden: A13 already hooks DisplayPowerController.initialize. Adding an intercept/chain.proceed path is a boot-safety choice that is not unique statically.

## system_cc_btandtorch_ascard

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_color_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_iconcolor_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_primary_textcolor

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_card_enabled_secondary_textcolor

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_clock_centeralign

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_floatingtimetile

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_freeform_when_longclick

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_hide_edit

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_hide_profile_monitoring

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_slider_color_enable

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_slider_icon_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_slider_progress_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_tile_enabled_color

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_tile_enabled_color_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_tile_enabled_color_usemonet

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_cc_tile_enabled_iconcolor_custom

- unresolved_question: miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers
- affected_rom_process: com.android.systemui + miui.systemui.plugin
- safe_default: off / ROM default
- required_device_evidence: MIUI 14 vs HyperOS 1 Control Center plugin class dump
- why_forbidden: A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.

## system_detailednetspeed_style

- unresolved_question: Preference-backed behavior; semantic proof pending.
- affected_rom_process: SYSTEM_UI
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: Detailed netspeed and fake dual-row already exist; selector migration is not statically safe.

## system_disable_window_blurs

- unresolved_question: Whether MIUI 14 system_server BlurController matches AOSP getBlurDisabledSetting
- affected_rom_process: android / system_server
- safe_default: ROM blur policy
- required_device_evidence: system_server BlurController members on MIUI 14 and HyperOS 1 A13
- why_forbidden: system_server boot path; A14 also adds a live PreferenceObserver. A13 catalog/contract wiring plus overlay ROM variants cannot be proven statically.

## system_drawer_date_centeralign

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_forbidden: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_date_fontsize

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_forbidden: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_dateformat

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_forbidden: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_hidedate

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_forbidden: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_drawer_remove_emptynotify

- unresolved_question: Notification shade date view identity on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock shade date
- required_device_evidence: Notification header/date view hierarchy
- why_forbidden: A14 drawer-date hooks target shade header classes not proven on MIUI 14.

## system_force_darken_allapps

- unresolved_question: ForceDarkAppListProvider/Manager presence vs A13 NoDarkForceHook
- affected_rom_process: android / system_server
- safe_default: stock per-app dark list
- required_device_evidence: ForceDark* class dump; interaction with system_nodarkforce
- why_forbidden: A14 uses ForceDarkAppList* ; A13 already owns the opposite NoDarkForceHook. Parallel implementation is forbidden.

## system_lockscreen_disable_edit

- unresolved_question: Keyguard editor entry class on MIUI 14 vs HyperOS 1
- affected_rom_process: com.android.systemui
- safe_default: stock keyguard editor
- required_device_evidence: Lockscreen editor activity dump
- why_forbidden: A14 disables a HyperOS keyguard editor not proven on MIUI 14.

## system_notif_disable_fold

- unresolved_question: MIUI fold-notification controller on A13
- affected_rom_process: com.android.systemui
- safe_default: stock fold notifications
- required_device_evidence: Notification fold policy class dump
- why_forbidden: No A13 fold-notification hook family.

## system_qs_disable_fakeclock_anim

- unresolved_question: Fake-clock animation owner on MIUI 14 QS/CC
- affected_rom_process: com.android.systemui / miui.systemui.plugin
- safe_default: stock fake clock
- required_device_evidence: QS clock animator class dump
- why_forbidden: A14 fake-clock hook is plugin-CC specific.

## system_recents_card_style

- unresolved_question: Recents card style controller class on MIUI 14 Home
- affected_rom_process: com.miui.home / com.android.systemui
- safe_default: stock recents cards
- required_device_evidence: Recents container class dump
- why_forbidden: A14 recents card-style is a new view path, not an upgrade of A13 recents blur.

## system_statusbar_clock_align

- unresolved_question: A14 production reads this key.
- affected_rom_process: SYSTEM_UI
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: A13 has the UI key but no production read; hook ownership is unresolved.

## system_statusbar_clock_bold

- unresolved_question: A14 production reads this key.
- affected_rom_process: SYSTEM_UI
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: A13 has the UI key but no production read; hook ownership is unresolved.

## system_statusbar_clock_fixedcontent_width

- unresolved_question: A14 production reads this key.
- affected_rom_process: SYSTEM_UI
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: A13 has the UI key but no production read; hook ownership is unresolved.

## system_statusbar_content_vertical_offset

- unresolved_question: Status-bar content geometry owner on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock geometry
- required_device_evidence: Collapsed status bar layout dump
- why_forbidden: New geometry rewrite; high visual-regression risk without device proof.

## system_statusbar_enable_weather_param

- unresolved_question: Weather status-bar param API on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock weather
- required_device_evidence: Weather controller class dump
- why_forbidden: No A13 weather-param hook.

## system_statusbar_icons_atleft_onkeyguard

- unresolved_question: Keyguard status-bar icon gravity on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock keyguard icon gravity
- required_device_evidence: Keyguard status-bar layout dump
- why_forbidden: New keyguard-only icon placement path.

## system_statusbar_mobile_digital_signal

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_align

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_bold

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_fontsize

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_hideunit

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_in2rows

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_leftmargin

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_rightmargin

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbar_mobile_digital_signal_verticaloffset

- unresolved_question: Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14
- affected_rom_process: com.android.systemui
- safe_default: stock signal icon
- required_device_evidence: StatusBar layout dump on MIUI 14 and HyperOS 1 A13
- why_forbidden: A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.

## system_statusbarcontrols_dt_left

- unresolved_question: Preference-backed behavior; semantic proof pending.
- affected_rom_process: SYSTEM_UI
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: Single system_statusbarcontrols_dt handles the whole bar.

## system_statusbarcontrols_dt_right

- unresolved_question: Preference-backed behavior; semantic proof pending.
- affected_rom_process: SYSTEM_UI
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: Single system_statusbarcontrols_dt handles the whole bar.

## system_statusbaricons_privacy_prompt

- unresolved_question: Camera/mic privacy-indicator slot name on MIUI 14 SystemUI
- affected_rom_process: com.android.systemui
- safe_default: stock privacy indicator
- required_device_evidence: Status bar slot dump
- why_forbidden: A13 system_statusbaricons_privacy hides incognito/stealth, not the privacy chip.

## system_strong_toast_island_offset

- unresolved_question: Preference-backed behavior; semantic proof pending.
- affected_rom_process: SYSTEM_UI
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: No strong-toast/island implementation; offset is a DI helper, not a remaining product gap.

## system_strong_toast_mode

- unresolved_question: Status-capsule / strong-toast presenter class on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock toast/capsule
- required_device_evidence: Strong toast / island presenter dump
- why_forbidden: No A13 capsule path; island mode is DI-adjacent.

## system_volume_hide_dnd_shortcut

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_forbidden: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_hide_mute_shortcut

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_forbidden: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_mode_button_background_color

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_forbidden: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_mode_button_colors

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_forbidden: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## system_volume_mode_button_icon_color

- unresolved_question: MIUI volume dialog shortcut/button view IDs on A13
- affected_rom_process: com.android.systemui
- safe_default: stock volume dialog
- required_device_evidence: Volume dialog view dump
- why_forbidden: A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.

## various_block_location_permission_prompts

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_block_notification_permission_prompts

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_disable_miui_daemon

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_disable_reset_recents_privacy_blur

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_disable_update_services

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_disable_xiaomi_analytics

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_remove_security_center_antivirus

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_trim_miui_daemon_network

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## various_trim_security_center_marketing

- unresolved_question: ROM component/package names for daemon/analytics/antivirus/marketing/permission controller
- affected_rom_process: module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller
- safe_default: components remain enabled
- required_device_evidence: Package/component inventory on MIUI 14 and HyperOS 1
- why_forbidden: A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.

## warning

- unresolved_question: A14 production reads this key.
- affected_rom_process: UNKNOWN_DISCOVERY
- safe_default: feature remains off / ROM default
- required_device_evidence: A13 MIUI 14 and HyperOS 1 class/layout dump
- why_forbidden: A13 has the UI key but no production read; hook ownership is unresolved.

## Dead A14 UI paths

- system_hidestatusbar_whenscreenrecord
- system_statusbar_batterytempandcurrent_align
- system_statusbar_batterytempandcurrent_bold
- system_statusbar_batterytempandcurrent_fixedcontent_width
- system_statusbar_batterytempandcurrent_fontsize
- system_statusbar_batterytempandcurrent_leftmargin
- system_statusbar_batterytempandcurrent_rightmargin
- system_statusbar_batterytempandcurrent_verticaloffset
- system_statusbar_showdevicetemperature_align
- system_statusbar_showdevicetemperature_bold
- system_statusbar_showdevicetemperature_fixedcontent_width
- system_statusbar_showdevicetemperature_fontsize
- system_statusbar_showdevicetemperature_leftmargin
- system_statusbar_showdevicetemperature_rightmargin
- system_statusbar_showdevicetemperature_verticaloffset
- system_statusbarcontrols_dual
- system_statusbarcontrols_single
- system_vibration_amp_period_end
- system_vibration_amp_period_start
- various_aospnotiflog
- various_appusagestats
- various_calluibright_night_end
- various_calluibright_night_start
- various_memorystats
