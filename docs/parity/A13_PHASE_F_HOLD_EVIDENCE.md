# A13 Phase F-R1 HOLD_EVIDENCE

HOLD_EVIDENCE_COUNT = 90
DEAD_UPSTREAM_PATH_COUNT = 0
SOURCE_REVIEW_REQUIRED = 0 (not an accepted final state)

Each HOLD is ROM/device/runtime uncertainty. Static source review was completed in F-R1.

## launcher_wallpaper_colormode

- unresolved_question: Launcher wallpaper color-mode API on MIUI 14 Home vs HyperOS 1
- affected_rom_process: com.miui.home
- safe_default: ROM wallpaper coloring
- required_device_evidence: DeviceConfig/wallpaper color-mode field names
- why_static_source_cannot_decide: No A13 counterpart; speculative GlobalLauncher/DeviceConfig writes are forbidden.

## miuizer_locale

- unresolved_question: Same key `miuizer_locale` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.settings
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

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

## system_cc_hideoperator_delimiter

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

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

## system_ccgridcolumns

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

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

## system_lstimeout

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: android
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

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

## system_qs_force_systemfonts

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

## system_qs_hideoperator

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

## system_recents_card_style

- unresolved_question: Recents card style controller class on MIUI 14 Home
- affected_rom_process: com.miui.home / com.android.systemui
- safe_default: stock recents cards
- required_device_evidence: Recents container class dump
- why_static_source_cannot_decide: A14 recents card-style is a new view path, not an upgrade of A13 recents blur.

## system_statusbar_batterytempandcurrent_align

- unresolved_question: Same key `system_statusbar_batterytempandcurrent_align` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_batterytempandcurrent_atright

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: com.android.systemui
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

## system_statusbar_batterytempandcurrent_bold

- unresolved_question: Same key `system_statusbar_batterytempandcurrent_bold` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_batterytempandcurrent_fixedcontent_width

- unresolved_question: Same key `system_statusbar_batterytempandcurrent_fixedcontent_width` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_batterytempandcurrent_fontsize

- unresolved_question: Same key `system_statusbar_batterytempandcurrent_fontsize` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_batterytempandcurrent_leftmargin

- unresolved_question: Same key `system_statusbar_batterytempandcurrent_leftmargin` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_batterytempandcurrent_rightmargin

- unresolved_question: Same key `system_statusbar_batterytempandcurrent_rightmargin` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_batterytempandcurrent_verticaloffset

- unresolved_question: Same key `system_statusbar_batterytempandcurrent_verticaloffset` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_clock_align

- unresolved_question: Same key `system_statusbar_clock_align` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_clock_bold

- unresolved_question: Same key `system_statusbar_clock_bold` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_clock_fixedcontent_width

- unresolved_question: Same key `system_statusbar_clock_fixedcontent_width` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_content_vertical_offset

- unresolved_question: Status-bar content geometry owner on MIUI 14
- affected_rom_process: com.android.systemui
- safe_default: stock geometry
- required_device_evidence: Collapsed status bar layout dump
- why_static_source_cannot_decide: New geometry rewrite; high visual-regression risk without device proof.

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

## system_statusbar_showdevicetemperature_align

- unresolved_question: Same key `system_statusbar_showdevicetemperature_align` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_showdevicetemperature_bold

- unresolved_question: Same key `system_statusbar_showdevicetemperature_bold` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_showdevicetemperature_fixedcontent_width

- unresolved_question: Same key `system_statusbar_showdevicetemperature_fixedcontent_width` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_showdevicetemperature_fontsize

- unresolved_question: Same key `system_statusbar_showdevicetemperature_fontsize` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_showdevicetemperature_leftmargin

- unresolved_question: Same key `system_statusbar_showdevicetemperature_leftmargin` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_showdevicetemperature_rightmargin

- unresolved_question: Same key `system_statusbar_showdevicetemperature_rightmargin` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_statusbar_showdevicetemperature_verticaloffset

- unresolved_question: Same key `system_statusbar_showdevicetemperature_verticaloffset` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

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

## system_statusbarheight

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: android
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

## system_statusbaricons_clock

- unresolved_question: Same key `system_statusbaricons_clock` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

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

## system_vibration_amp_period_end

- unresolved_question: Same key `system_vibration_amp_period_end` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_vibration_amp_period_start

- unresolved_question: Same key `system_vibration_amp_period_start` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## system_visualizer_custom

- unresolved_question: Same key `system_visualizer_custom` is present on both trees without a verified owner proof.
- affected_rom_process: com.android.systemui
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

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

## various_allow_untrusted_touch

- unresolved_question: A14 and A13 both own this key but hook members/classes differ.
- affected_rom_process: android
- safe_default: feature off / ROM default
- required_device_evidence: Host class/member dump on MIUI 14
- why_static_source_cannot_decide: ROM dump required to know which member exists on MIUI 14 / HyperOS 1 A13.

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

## various_calluibright_night_end

- unresolved_question: Same key `various_calluibright_night_end` is present on both trees without a verified owner proof.
- affected_rom_process: android.system.package
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## various_calluibright_night_start

- unresolved_question: Same key `various_calluibright_night_start` is present on both trees without a verified owner proof.
- affected_rom_process: android.system.package
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

## various_disable_access_devicelogs

- unresolved_question: Same key `various_disable_access_devicelogs` is present on both trees without a verified owner proof.
- affected_rom_process: android
- safe_default: keep current A13 behavior
- required_device_evidence: Owner class/member dump comparing A14 vs MIUI 14 SystemUI/Home
- why_static_source_cannot_decide: Static analysis could not identify matching installer/hook members; ROM/process dump required.

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
