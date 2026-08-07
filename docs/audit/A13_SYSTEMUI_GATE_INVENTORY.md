# A13 SystemUI Gate Inventory

**Schema version:** 1.0
**Generated from:** app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java, app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt

This document records the raw conditions found in SystemUI startup gating.
It does not judge INSTALLER_ONLY, GATE_ONLY, DEFAULT_MISMATCH, etc.

## Category counts

| category | count |
|----------|-------|
| FEATURE_CATALOG_GATES | 13 |
| FEATURE_DISPATCH_CALLS | 5 |
| GLOBAL_ACTION_DOMAIN_RULES | 2 |
| INSTALL_CONDITIONS | 108 |
| PARTIAL | 0 |
| RESOURCE_PHASE_CONDITIONS | 2 |
| RESTART_GUARD | 1 |
| STARTUP_GATE_CONDITIONS | 119 |
| UNPARSED | 0 |

## Parse status counts

| parse_status | count |
|--------------|-------|
| PARSED | 250 |
| PARTIAL | 0 |
| UNPARSED | 0 |

## INSTALL_CONDITIONS (108)

### install_if_1
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 49-49
- phase: `PACKAGE_GUARD`
- parse_status: `PARSED`
- branch_kind: `IF`
- comparators: ['!=']
- boolean_operators: ['NOT']
- raw_expression: `!pkg.equals("com.android.systemui")`

### install_if_2
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 51-51
- phase: `PRE_RESTART_GUARD_RESOURCE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbarheight']
- accessors: ['getInt']
- default_values: [19]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_statusbarheight", 19) > 19`

### install_if_3
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 52-52
- phase: `PRE_RESTART_GUARD_RESOURCE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_navbarheight']
- accessors: ['getInt']
- default_values: [19]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("controls_navbarheight", 19) > 19`

### install_if_4
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 57-57
- phase: `PRE_RESTART_GUARD_INFRASTRUCTURE`
- parse_status: `PARSED`
- branch_kind: `IF`
- comparators: ['!=']
- raw_expression: `NetworkSpeedViewCls != null`

### install_if_5
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 64-64
- phase: `PRE_RESTART_GUARD_INFRASTRUCTURE`
- parse_status: `PARSED`
- branch_kind: `IF`
- boolean_operators: ['NOT']
- raw_expression: `!isHooked`

### install_if_6
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 74-74
- phase: `RESTART_GUARD`
- parse_status: `PARSED`
- branch_kind: `IF`
- raw_expression: `isWithinSystemUiRestartGuard(restartTime, currentTime)`

### install_if_7
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 80-82
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['various_showcallui', 'controls_volumecursor']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [0, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0
                || MainModule.mPrefs.getBoolean("controls_volumecursor")`
- normalized_expression: `MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0 || MainModule.mPrefs.getBoolean("controls_volumecursor")`

### install_if_8
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 86-89
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_fivegtile', 'system_cc_fpstile']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_fivegtile")
                || MainModule.mPrefs.getBoolean("system_cc_fpstile")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_fivegtile") || MainModule.mPrefs.getBoolean("system_cc_fpstile")`

### install_if_9
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 95-95
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qsgridcolumns', 'system_qsgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [2, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("system_qsgridcolumns", 2) > 2 || MainModule.mPrefs.getInt("system_qsgridrows", 1) > 1`

### install_if_10
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 96-96
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qqsgridcolumns']
- accessors: ['getInt']
- default_values: [2]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_qqsgridcolumns", 2) > 2`

### install_if_11
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 97-97
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `networkIndicatorWifi`
- preference_keys: ['system_networkindicator_wifi']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_networkindicator_wifi")`

### install_if_12
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 99-99
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_drawer_blur']
- accessors: ['getInt']
- default_values: [100]
- default_kinds: ['EXPLICIT']
- comparators: ['<']
- raw_expression: `MainModule.mPrefs.getInt("system_drawer_blur", 100) < 100`

### install_if_13
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 100-100
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_chargeanimtime']
- accessors: ['getInt']
- default_values: [20]
- default_kinds: ['EXPLICIT']
- comparators: ['<']
- raw_expression: `MainModule.mPrefs.getInt("system_chargeanimtime", 20) < 20`

### install_if_14
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 101-101
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_delay', 'system_betterpopups_nohide']
- accessors: ['getInt', 'getBoolean']
- default_values: [0, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `MainModule.mPrefs.getInt("system_betterpopups_delay", 0) > 0 && !MainModule.mPrefs.getBoolean("system_betterpopups_nohide")`

### install_if_15
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 102-102
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_netspeedinterval']
- accessors: ['getInt']
- default_values: [4]
- default_kinds: ['EXPLICIT']
- comparators: ['!=']
- raw_expression: `MainModule.mPrefs.getInt("system_netspeedinterval", 4) != 4`

### install_if_16
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 103-103
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qsgridrows', 'system_qsnolabels']
- accessors: ['getInt', 'getBoolean']
- default_values: [1, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("system_qsgridrows", 1) > 1 || MainModule.mPrefs.getBoolean("system_qsnolabels")`

### install_if_17
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 104-104
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lstimeout']
- accessors: ['getInt']
- default_values: [3]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_lstimeout", 3) > 3`

### install_if_18
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 105-107
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_fsg_assist_left_action', 'controls_fsg_assist_right_action']
- accessors: ['getInt', 'getInt']
- default_values: [1, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("controls_fsg_assist_left_action", 1) > 1
                || MainModule.mPrefs.getInt("controls_fsg_assist_right_action", 1) > 1`
- normalized_expression: `MainModule.mPrefs.getInt("controls_fsg_assist_left_action", 1) > 1 || MainModule.mPrefs.getInt("controls_fsg_assist_right_action", 1) > 1`

### install_if_19
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 108-111
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_navbarleft_action', 'controls_navbarleftlong_action', 'controls_navbarright_action', 'controls_navbarrightlong_action']
- accessors: ['getInt', 'getInt', 'getInt', 'getInt']
- default_values: [1, 1, 1, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>', '>', '>']
- boolean_operators: ['OR', 'OR', 'OR']
- raw_expression: `MainModule.mPrefs.getInt("controls_navbarleft_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarleftlong_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarright_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarrightlong_action", 1) > 1`
- normalized_expression: `MainModule.mPrefs.getInt("controls_navbarleft_action", 1) > 1 || MainModule.mPrefs.getInt("controls_navbarleftlong_action", 1) > 1 || MainModule.mPrefs.getInt("controls_navbarright_action", 1) > 1 || MainModule.mPrefs.getInt("controls_navbarrightlong_action", 1) > 1`

### install_if_20
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 112-112
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_scramblepin']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_scramblepin")`

### install_if_21
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 113-113
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_dttosleep']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_dttosleep")`

### install_if_22
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 115-115
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_noscreenlock_act']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_noscreenlock_act")`

### install_if_23
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 116-119
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_detailednetspeed', 'system_detailednetspeed_fakedualrow']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_detailednetspeed")
                && !MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_detailednetspeed") && !MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")`

### install_if_24
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 120-120
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_albumartonlock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_albumartonlock")`

### install_if_25
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 121-121
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_expandheadups']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_expandheadups", 1) > 1`

### install_if_26
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 122-122
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_nohide']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_nohide")`

### install_if_27
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 123-123
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_swipedown']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_swipedown")`

### install_if_28
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 124-124
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_center']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_center")`

### install_if_29
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 126-126
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notifafterunlock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_notifafterunlock")`

### install_if_30
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 127-127
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notifrowmenu']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_notifrowmenu")`

### install_if_31
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 128-128
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_compactnotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_compactnotif")`

### install_if_32
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 129-129
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideDismissView`
- preference_keys: ['system_removedismiss']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_removedismiss")`

### install_if_33
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 130-130
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_drawer_removeshortcut']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_drawer_removeshortcut")`

### install_if_34
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 131-131
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_nonavbar']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("controls_nonavbar")`

### install_if_34_else_if_35
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 132-132
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- nesting_depth: 1
- parent_condition_id: `install_if_34`
- branch_kind: `ELSE_IF`
- feature_id: `hideNavBarBeforeScreenshot`
- preference_keys: ['controls_hidenavbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("controls_hidenavbar_whenscreenshot")`

### install_if_36
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 133-133
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_imebackalticon']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("controls_imebackalticon")`

### install_if_37
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 134-134
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_visualizer']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_visualizer")`

### install_if_38
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 135-149
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nosilentvibrate', 'system_qs_force_systemfonts', 'system_volumetimer', 'system_qsnolabels', 'system_cc_volume_showpct', 'system_volumebar_blur_mtk', 'system_cc_hidedate', 'system_cc_hide_shortcuticons', 'system_cc_clocktweak', 'system_cc_tile_roundedrect', 'system_cc_bluetooth_tile_style', 'system_separatevolume', 'system_separatevolume_slider', 'system_volumedialogdelay_collapsed', 'system_volumedialogdelay_expanded', 'system_volumeblur_collapsed', 'system_volumeblur_expanded']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getStringAsInt', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getInt']
- default_values: [False, False, False, False, False, False, False, False, False, False, 1, False, False, 0, 0, 0, 0]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>', '>', '>', '>']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_nosilentvibrate")
                || MainModule.mPrefs.getBoolean("system_qs_force_systemfonts")
                || MainModule.mPrefs.getBoolean("system_volumetimer")
                || MainModule.mPrefs.getBoolean("system_qsnolabels")
                || MainModule.mPrefs.getBoolean("system_cc_volume_showpct")
                || MainModule.mPrefs.getBoolean("system_volumebar_blur_mtk")
                || MainModule.mPrefs.getBoolean("system_cc_hidedate")
                || MainModule.mPrefs.getBoolean("system_cc_hide_shortcuticons")
                || MainModule.mPrefs.getBoolean("system_cc_clocktweak")
                || MainModule.mPrefs.getBoolean("system_cc_tile_roundedrect")
                || MainModule.mPrefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1
                || (MainModule.mPrefs.getBoolean("system_separatevolume") && MainModule.mPrefs.getBoolean("system_separatevolume_slider"))
                || (MainModule.mPrefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumedialogdelay_expanded", 0) > 0)
                || (MainModule.mPrefs.getInt("system_volumeblur_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumeblur_expanded", 0) > 0)`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_nosilentvibrate") || MainModule.mPrefs.getBoolean("system_qs_force_systemfonts") || MainModule.mPrefs.getBoolean("system_volumetimer") || MainModule.mPrefs.getBoolean("system_qsnolabels") || MainModule.mPrefs.getBoolean("system_cc_volume_showpct") || MainModule.mPrefs.getBoolean("system_volumebar_blur_mtk") || MainModule.mPrefs.getBoolean("system_cc_hidedate") || MainModule.mPrefs.getBoolean("system_cc_hide_shortcuticons") || MainModule.mPrefs.getBoolean("system_cc_clocktweak") || MainModule.mPrefs.getBoolean("system_cc_tile_roundedrect") || MainModule.mPrefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1 || (MainModule.mPrefs.getBoolean("system_separatevolume") && MainModule.mPrefs.getBoolean("system_separatevolume_slider")) || (MainModule.mPrefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumedialogdelay_expanded", 0) > 0) || (MainModule.mPrefs.getInt("system_volumeblur_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumeblur_expanded", 0) > 0)`

### install_if_39
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 153-153
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_disableanynotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_disableanynotif")`

### install_if_40
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 154-154
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lockscreenshortcuts']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_lockscreenshortcuts")`

### install_if_41
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 155-158
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_4gtolte', 'system_statusbar_mobiletype_single', 'system_statusbar_mobile_showname']
- accessors: ['getBoolean', 'getBoolean', 'getString']
- default_values: [False, False, '']
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT']
- comparators: ['==']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_4gtolte")
                || (MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single") &&
                    !MainModule.mPrefs.getString("system_statusbar_mobile_showname", "").equals(""))`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_4gtolte") || (MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single") && !MainModule.mPrefs.getString("system_statusbar_mobile_showname", "").equals(""))`

### install_if_42
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 171-176
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_netspeed_atleft', 'system_statusbar_dualrows', 'system_statusbar_netspeed_atsecondrow', 'system_statusbaricons_wifi_mobile_atleft', 'system_statusbaricons_swap_wifi_mobile']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `moveRight || moveLeft
                || MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atleft")
                || (MainModule.mPrefs.getBoolean("system_statusbar_dualrows") && MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atsecondrow"))
                || MainModule.mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_swap_wifi_mobile")`
- normalized_expression: `moveRight || moveLeft || MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atleft") || (MainModule.mPrefs.getBoolean("system_statusbar_dualrows") && MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atsecondrow")) || MainModule.mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft") || MainModule.mPrefs.getBoolean("system_statusbaricons_swap_wifi_mobile")`

### install_if_43
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 179-179
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_clock_position', 'system_statusbar_dualrows']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [1, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_statusbar_clock_position", 1) > 1 && !MainModule.mPrefs.getBoolean("system_statusbar_dualrows")`

### install_if_44
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 182-182
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_batterystyle']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_batterystyle")`

### install_if_45
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 185-187
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_batterytempandcurrent', 'system_statusbar_showdevicetemperature']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent")
                || MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent") || MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature")`

### install_if_46
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 188-188
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_topmargin', 'system_statusbar_topmargin_unset_lockscreen']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['AND']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_topmargin") && MainModule.mPrefs.getBoolean("system_statusbar_topmargin_unset_lockscreen")`

### install_if_47
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 189-189
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_horizmargin']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_horizmargin")`

### install_if_48
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 190-190
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_showpct']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_showpct")`

### install_if_49
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 191-191
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hidelsstatusbar']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_hidelsstatusbar")`

### install_if_50
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 192-192
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hidelsclock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_hidelsclock")`

### install_if_51
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 193-193
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_ls_force_systemfonts']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_ls_force_systemfonts")`

### install_if_52
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 194-194
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_hidelshint']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_hidelshint")`

### install_if_53
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 195-195
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_allowdirectreply']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_allowdirectreply")`

### install_if_54
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 196-196
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_allownotifonkeyguard']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_allownotifonkeyguard")`

### install_if_55
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 197-197
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_allownotiffloat']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_allownotiffloat")`

### install_if_56
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 198-198
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hideqs']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_hideqs")`

### install_if_57
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 199-199
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lsalarm']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_lsalarm")`

### install_if_58
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 200-200
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbarcontrols']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbarcontrols")`

### install_if_59
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 201-201
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_nonetspeedseparator']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_nonetspeedseparator")`

### install_if_60
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 202-202
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideIconsClock`
- preference_keys: ['system_statusbaricons_clock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_clock")`

### install_if_61
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 203-209
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_detailednetspeed_fakedualrow', 'system_detailednetspeed', 'system_detailednetspeed_secunit', 'system_detailednetspeed_low']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'NOT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || (!MainModule.mPrefs.getBoolean("system_detailednetspeed")
                    && (MainModule.mPrefs.getBoolean("system_detailednetspeed_secunit")
                        || MainModule.mPrefs.getBoolean("system_detailednetspeed_low")
                        )
                    )`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow") || (!MainModule.mPrefs.getBoolean("system_detailednetspeed") && (MainModule.mPrefs.getBoolean("system_detailednetspeed_secunit") || MainModule.mPrefs.getBoolean("system_detailednetspeed_low") ) )`

### install_if_62
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 212-222
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_netspeed_fontsize', 'system_netspeed_verticaloffset', 'system_detailednetspeed', 'system_detailednetspeed_fakedualrow', 'system_netspeed_bold', 'system_netspeed_leftmargin', 'system_netspeed_fixedcontent_width', 'system_netspeed_rightmargin', 'system_detailednetspeed_align']
- accessors: ['getInt', 'getInt', 'getBoolean', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getStringAsInt']
- default_values: [13, 8, False, False, False, 0, 10, 0, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['>', '!=', '>', '>', '>', '>']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `MainModule.mPrefs.getInt("system_netspeed_fontsize", 13) > 13
                || MainModule.mPrefs.getInt("system_netspeed_verticaloffset", 8) != 8
                || MainModule.mPrefs.getBoolean("system_detailednetspeed")
                || MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || MainModule.mPrefs.getBoolean("system_netspeed_bold")
                || MainModule.mPrefs.getInt("system_netspeed_leftmargin", 0) > 0
                || MainModule.mPrefs.getInt("system_netspeed_fixedcontent_width", 10) > 10
                || MainModule.mPrefs.getInt("system_netspeed_rightmargin", 0) > 0
                || MainModule.mPrefs.getStringAsInt("system_detailednetspeed_align", 1) > 1`
- normalized_expression: `MainModule.mPrefs.getInt("system_netspeed_fontsize", 13) > 13 || MainModule.mPrefs.getInt("system_netspeed_verticaloffset", 8) != 8 || MainModule.mPrefs.getBoolean("system_detailednetspeed") || MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow") || MainModule.mPrefs.getBoolean("system_netspeed_bold") || MainModule.mPrefs.getInt("system_netspeed_leftmargin", 0) > 0 || MainModule.mPrefs.getInt("system_netspeed_fixedcontent_width", 10) > 10 || MainModule.mPrefs.getInt("system_netspeed_rightmargin", 0) > 0 || MainModule.mPrefs.getStringAsInt("system_detailednetspeed_align", 1) > 1`

### install_if_63
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 225-225
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_taptounlock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_taptounlock")`

### install_if_64
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 226-226
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nosos']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_nosos")`

### install_if_65
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 227-227
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_morenotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_morenotif")`

### install_if_66
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 228-228
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `chargingInfo`
- preference_keys: ['system_charginginfo']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_charginginfo")`

### install_if_67
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 229-229
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_secureqs']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_secureqs")`

### install_if_68
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 230-230
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `muteVisibleNotifications`
- preference_keys: ['system_mutevisiblenotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_mutevisiblenotif")`

### install_if_69
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 231-231
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_battery1']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_battery1")`

### install_if_70
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 232-235
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_battery3', 'system_statusbaricons_battery4', 'system_statusbaricons_battery2']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_battery3")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_battery4")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_battery2")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_battery3") || MainModule.mPrefs.getBoolean("system_statusbaricons_battery4") || MainModule.mPrefs.getBoolean("system_statusbaricons_battery2")`

### install_if_71
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 236-236
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_wifistandard']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_statusbaricons_wifistandard", 1) > 1`

### install_if_72
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 237-243
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_signal', 'system_statusbaricons_sim1', 'system_statusbaricons_sim2', 'system_statusbaricons_sim_nodata', 'system_statusbaricons_roaming', 'system_statusbaricons_volte']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_signal")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim1")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim2")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim_nodata")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_roaming")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_volte")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_signal") || MainModule.mPrefs.getBoolean("system_statusbaricons_sim1") || MainModule.mPrefs.getBoolean("system_statusbaricons_sim2") || MainModule.mPrefs.getBoolean("system_statusbaricons_sim_nodata") || MainModule.mPrefs.getBoolean("system_statusbaricons_roaming") || MainModule.mPrefs.getBoolean("system_statusbaricons_volte")`

### install_if_73
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 244-244
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_vowifi']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_vowifi")`

### install_if_74
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 245-245
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_alarm', 'system_statusbaricons_alarmn']
- accessors: ['getBoolean', 'getInt']
- default_values: [False, 0]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT']
- comparators: ['>']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `!MainModule.mPrefs.getBoolean("system_statusbaricons_alarm") && MainModule.mPrefs.getInt("system_statusbaricons_alarmn", 0) > 0`

### install_if_75
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 246-248
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_shortcut_app', 'system_calendar_app', 'system_clock_app']
- accessors: ['getString', 'getString', 'getString']
- default_values: ['', '', '']
- default_kinds: ['EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['!=', '!=', '!=']
- boolean_operators: ['OR', 'OR', 'NOT', 'NOT', 'NOT']
- raw_expression: `!MainModule.mPrefs.getString("system_shortcut_app", "").equals("")
                || !MainModule.mPrefs.getString("system_calendar_app", "").equals("")
                || !MainModule.mPrefs.getString("system_clock_app", "").equals("")`
- normalized_expression: `!MainModule.mPrefs.getString("system_shortcut_app", "").equals("") || !MainModule.mPrefs.getString("system_calendar_app", "").equals("") || !MainModule.mPrefs.getString("system_clock_app", "").equals("")`

### install_if_76
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 249-249
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qshaptics']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_qshaptics", 1) > 1`

### install_if_77
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 250-250
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qs_hideoperator']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_qs_hideoperator")`

### install_if_78
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 251-251
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_hideoperator_delimiter']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_hideoperator_delimiter")`

### install_if_79
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 252-254
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_show_stepcount', 'system_drawer_show_stepcount']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_show_stepcount")
                || MainModule.mPrefs.getBoolean("system_drawer_show_stepcount")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_cc_show_stepcount") || MainModule.mPrefs.getBoolean("system_drawer_show_stepcount")`

### install_if_80
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 255-255
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_disable_bluetooth_restrict']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_disable_bluetooth_restrict")`

### install_if_81
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 256-256
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_collapse_after_clicked']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_collapse_after_clicked")`

### install_if_82
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 257-257
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_switch_qsandnotification']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_switch_qsandnotification")`

### install_if_83
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 258-258
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_expandnotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_expandnotifs", 1) > 1`

### install_if_84
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 259-259
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_inactivebrightness']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_inactivebrightness", 1) > 1`

### install_if_85
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 260-263
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_mobiletypeicon', 'system_networkindicator_mobile', 'system_statusbar_mobiletype_show_wificonnected']
- accessors: ['getStringAsInt', 'getBoolean', 'getBoolean']
- default_values: [1, False, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['OR', 'OR']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_mobiletypeicon", 1) > 1
                || MainModule.mPrefs.getBoolean("system_networkindicator_mobile")
                || MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")`
- normalized_expression: `MainModule.mPrefs.getStringAsInt("system_mobiletypeicon", 1) > 1 || MainModule.mPrefs.getBoolean("system_networkindicator_mobile") || MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")`

### install_if_86
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 266-266
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_bluetooth']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_statusbaricons_bluetooth", 1) > 1`

### install_if_87
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 267-267
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_epm']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_epm")`

### install_if_88
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 287-287
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- raw_expression: `hideIconsActive`

### install_if_89
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 289-294
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_privacy', 'system_statusbaricons_mute', 'system_statusbaricons_speaker', 'system_statusbaricons_record']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR', 'OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_privacy")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_mute")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_speaker")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_record")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_privacy") || MainModule.mPrefs.getBoolean("system_statusbaricons_mute") || MainModule.mPrefs.getBoolean("system_statusbaricons_speaker") || MainModule.mPrefs.getBoolean("system_statusbaricons_record")`

### install_if_90
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 295-295
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_messagingstylelines']
- accessors: ['getInt']
- default_values: [0]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_messagingstylelines", 0) > 0`

### install_if_91
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 296-296
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_allowfloat']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")`

### install_if_92
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 297-297
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_autoclose_expanded']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_autoclose_expanded")`

### install_if_93
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 298-298
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_disablewhenmute']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_disablewhenmute")`

### install_if_94
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 299-299
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_securecontrolcenter']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_securecontrolcenter")`

### install_if_95
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 300-300
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_minimalnotifview']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_minimalnotifview")`

### install_if_96
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 301-301
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notifchannelsettings']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_notifchannelsettings")`

### install_if_97
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 302-302
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_maxsbicons']
- accessors: ['getStringAsInt']
- default_values: [0]
- default_kinds: ['EXPLICIT']
- comparators: ['!=']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_maxsbicons", 0) != 0`

### install_if_98
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 303-303
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_mobiletype_single']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single")`

### install_if_99
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 306-306
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_dualsimin2rows']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_dualsimin2rows")`

### install_if_100
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 309-309
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_dualrows']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_dualrows")`

### install_if_101
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 312-312
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_ccgridcolumns', 'system_ccgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [4, 4]
- default_kinds: ['EXPLICIT', 'EXPLICIT']
- comparators: ['>', '!=']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("system_ccgridcolumns", 4) > 4 || MainModule.mPrefs.getInt("system_ccgridrows", 4) != 4`

### install_if_102
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 313-313
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_colorizenotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_colorizenotifs", 1) > 1`

### install_if_103
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 314-314
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notify_openinfw']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_notify_openinfw")`

### install_if_104
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 315-315
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_fw_noblacklist']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_fw_noblacklist")`

### install_if_105
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 317-320
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notify_openinfw', 'system_notifrowmenu', 'system_betterpopups_allowfloat']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_notify_openinfw")
                || MainModule.mPrefs.getBoolean("system_notifrowmenu")
                || MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")`
- normalized_expression: `MainModule.mPrefs.getBoolean("system_notify_openinfw") || MainModule.mPrefs.getBoolean("system_notifrowmenu") || MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")`

### install_if_106
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 323-323
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nosafevolume']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_nosafevolume")`

### install_if_107
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 326-326
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lockscreen_hidezenmode']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_lockscreen_hidezenmode")`

### install_if_108
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 329-329
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nopassword']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `MainModule.mPrefs.getBoolean("system_nopassword")`

## STARTUP_GATE_CONDITIONS (119)

### hasAnySystemUiStartupFeature_if_1
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 334-334
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- raw_expression: `hasAnyGlobalAction(prefs)`

### hasAnySystemUiStartupFeature_if_2
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 335-335
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbarheight']
- accessors: ['getInt']
- default_values: [19]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getInt("system_statusbarheight", 19) > 19`

### hasAnySystemUiStartupFeature_if_3
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 336-336
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_navbarheight']
- accessors: ['getInt']
- default_values: [19]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getInt("controls_navbarheight", 19) > 19`

### hasAnySystemUiStartupFeature_if_4
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 337-337
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['various_showcallui', 'controls_volumecursor']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [0, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getStringAsInt("various_showcallui", 0) > 0 || prefs.getBoolean("controls_volumecursor")`

### hasAnySystemUiStartupFeature_if_5
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 338-338
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_fivegtile', 'system_cc_fpstile']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_fivegtile") || prefs.getBoolean("system_cc_fpstile")`

### hasAnySystemUiStartupFeature_if_6
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 339-339
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qsgridcolumns', 'system_qsgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [2, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("system_qsgridcolumns", 2) > 2 || prefs.getInt("system_qsgridrows", 1) > 1`

### hasAnySystemUiStartupFeature_if_7
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 340-340
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qqsgridcolumns']
- accessors: ['getInt']
- default_values: [2]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getInt("system_qqsgridcolumns", 2) > 2`

### hasAnySystemUiStartupFeature_if_8
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 341-341
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_networkindicator_wifi']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_networkindicator_wifi")`

### hasAnySystemUiStartupFeature_if_9
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 342-342
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_drawer_blur']
- accessors: ['getInt']
- default_values: [100]
- default_kinds: ['EXPLICIT']
- comparators: ['<']
- raw_expression: `prefs.getInt("system_drawer_blur", 100) < 100`

### hasAnySystemUiStartupFeature_if_10
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 343-343
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_chargeanimtime']
- accessors: ['getInt']
- default_values: [20]
- default_kinds: ['EXPLICIT']
- comparators: ['<']
- raw_expression: `prefs.getInt("system_chargeanimtime", 20) < 20`

### hasAnySystemUiStartupFeature_if_11
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 344-344
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_delay', 'system_betterpopups_nohide']
- accessors: ['getInt', 'getBoolean']
- default_values: [0, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `prefs.getInt("system_betterpopups_delay", 0) > 0 && !prefs.getBoolean("system_betterpopups_nohide")`

### hasAnySystemUiStartupFeature_if_12
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 345-345
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_netspeedinterval']
- accessors: ['getInt']
- default_values: [4]
- default_kinds: ['EXPLICIT']
- comparators: ['!=']
- raw_expression: `prefs.getInt("system_netspeedinterval", 4) != 4`

### hasAnySystemUiStartupFeature_if_13
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 346-346
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qsgridrows', 'system_qsnolabels']
- accessors: ['getInt', 'getBoolean']
- default_values: [1, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("system_qsgridrows", 1) > 1 || prefs.getBoolean("system_qsnolabels")`

### hasAnySystemUiStartupFeature_if_14
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 347-347
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lstimeout']
- accessors: ['getInt']
- default_values: [3]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getInt("system_lstimeout", 3) > 3`

### hasAnySystemUiStartupFeature_if_15
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 348-348
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_fsg_assist_left_action', 'controls_fsg_assist_right_action']
- accessors: ['getInt', 'getInt']
- default_values: [1, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("controls_fsg_assist_left_action", 1) > 1 || prefs.getInt("controls_fsg_assist_right_action", 1) > 1`

### hasAnySystemUiStartupFeature_if_16
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 349-349
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_navbarleft_action', 'controls_navbarleftlong_action', 'controls_navbarright_action', 'controls_navbarrightlong_action']
- accessors: ['getInt', 'getInt', 'getInt', 'getInt']
- default_values: [1, 1, 1, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>', '>', '>']
- boolean_operators: ['OR', 'OR', 'OR']
- raw_expression: `prefs.getInt("controls_navbarleft_action", 1) > 1 || prefs.getInt("controls_navbarleftlong_action", 1) > 1 || prefs.getInt("controls_navbarright_action", 1) > 1 || prefs.getInt("controls_navbarrightlong_action", 1) > 1`

### hasAnySystemUiStartupFeature_if_17
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 350-350
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_scramblepin']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_scramblepin")`

### hasAnySystemUiStartupFeature_if_18
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 351-351
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_dttosleep']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_dttosleep")`

### hasAnySystemUiStartupFeature_if_19
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 352-352
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_noscreenlock_act']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_noscreenlock_act")`

### hasAnySystemUiStartupFeature_if_20
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 353-353
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_detailednetspeed', 'system_detailednetspeed_fakedualrow']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `prefs.getBoolean("system_detailednetspeed") && !prefs.getBoolean("system_detailednetspeed_fakedualrow")`

### hasAnySystemUiStartupFeature_if_21
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 354-354
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_albumartonlock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_albumartonlock")`

### hasAnySystemUiStartupFeature_if_22
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 355-355
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_expandheadups']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_expandheadups", 1) > 1`

### hasAnySystemUiStartupFeature_if_23
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 356-356
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_nohide']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_betterpopups_nohide")`

### hasAnySystemUiStartupFeature_if_24
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 357-357
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_swipedown']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_betterpopups_swipedown")`

### hasAnySystemUiStartupFeature_if_25
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 358-358
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_center']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_betterpopups_center")`

### hasAnySystemUiStartupFeature_if_26
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 359-359
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notifafterunlock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_notifafterunlock")`

### hasAnySystemUiStartupFeature_if_27
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 360-360
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notifrowmenu']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_notifrowmenu")`

### hasAnySystemUiStartupFeature_if_28
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 361-361
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_compactnotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_compactnotif")`

### hasAnySystemUiStartupFeature_if_29
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 362-362
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_removedismiss']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_removedismiss")`

### hasAnySystemUiStartupFeature_if_30
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 363-363
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_drawer_removeshortcut']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_drawer_removeshortcut")`

### hasAnySystemUiStartupFeature_if_31
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 364-364
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_nonavbar']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("controls_nonavbar")`

### hasAnySystemUiStartupFeature_if_32
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 365-365
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_hidenavbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("controls_hidenavbar_whenscreenshot")`

### hasAnySystemUiStartupFeature_if_33
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 366-366
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_imebackalticon']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("controls_imebackalticon")`

### hasAnySystemUiStartupFeature_if_34
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 367-367
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_visualizer']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_visualizer")`

### hasAnySystemUiStartupFeature_if_35
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 368-368
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nosilentvibrate', 'system_qs_force_systemfonts', 'system_volumetimer', 'system_qsnolabels', 'system_cc_volume_showpct', 'system_volumebar_blur_mtk', 'system_cc_hidedate', 'system_cc_hide_shortcuticons', 'system_cc_clocktweak', 'system_cc_tile_roundedrect', 'system_cc_bluetooth_tile_style', 'system_separatevolume', 'system_separatevolume_slider', 'system_volumedialogdelay_collapsed', 'system_volumedialogdelay_expanded', 'system_volumeblur_collapsed', 'system_volumeblur_expanded']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getStringAsInt', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getInt']
- default_values: [False, False, False, False, False, False, False, False, False, False, 1, False, False, 0, 0, 0, 0]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>', '>', '>', '>']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `prefs.getBoolean("system_nosilentvibrate") || prefs.getBoolean("system_qs_force_systemfonts") || prefs.getBoolean("system_volumetimer") || prefs.getBoolean("system_qsnolabels") || prefs.getBoolean("system_cc_volume_showpct") || prefs.getBoolean("system_volumebar_blur_mtk") || prefs.getBoolean("system_cc_hidedate") || prefs.getBoolean("system_cc_hide_shortcuticons") || prefs.getBoolean("system_cc_clocktweak") || prefs.getBoolean("system_cc_tile_roundedrect") || prefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1 || (prefs.getBoolean("system_separatevolume") && prefs.getBoolean("system_separatevolume_slider")) || (prefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || prefs.getInt("system_volumedialogdelay_expanded", 0) > 0) || (prefs.getInt("system_volumeblur_collapsed", 0) > 0 || prefs.getInt("system_volumeblur_expanded", 0) > 0)`

### hasAnySystemUiStartupFeature_if_36
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 369-369
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_disableanynotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_disableanynotif")`

### hasAnySystemUiStartupFeature_if_37
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 370-370
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lockscreenshortcuts']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_lockscreenshortcuts")`

### hasAnySystemUiStartupFeature_if_38
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 371-371
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_4gtolte', 'system_statusbar_mobiletype_single', 'system_statusbar_mobile_showname']
- accessors: ['getBoolean', 'getBoolean', 'getString']
- default_values: [False, False, '']
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT']
- comparators: ['==']
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_4gtolte") || (prefs.getBoolean("system_statusbar_mobiletype_single") && !prefs.getString("system_statusbar_mobile_showname", "").equals(""))`

### hasAnySystemUiStartupFeature_if_39
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 372-372
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_netspeed_atright', 'system_statusbar_alarm_atright', 'system_statusbar_sound_atright', 'system_statusbar_dnd_atright', 'system_statusbar_nfc_atright', 'system_statusbar_btbattery_atright', 'system_statusbar_headset_atright', 'system_statusbar_vpn_atright', 'system_statusbar_alarm_atleft', 'system_statusbar_sound_atleft', 'system_statusbar_dnd_atleft', 'system_statusbar_gps_atleft', 'system_statusbar_netspeed_atleft', 'system_statusbar_dualrows', 'system_statusbar_netspeed_atsecondrow', 'system_statusbaricons_wifi_mobile_atleft', 'system_statusbaricons_swap_wifi_mobile']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False, False, False, False, False, False, False, False, False, False, False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `(prefs.getBoolean("system_statusbar_netspeed_atright") || prefs.getBoolean("system_statusbar_alarm_atright") || prefs.getBoolean("system_statusbar_sound_atright") || prefs.getBoolean("system_statusbar_dnd_atright") || prefs.getBoolean("system_statusbar_nfc_atright") || prefs.getBoolean("system_statusbar_btbattery_atright") || prefs.getBoolean("system_statusbar_headset_atright") || prefs.getBoolean("system_statusbar_vpn_atright")) || (prefs.getBoolean("system_statusbar_alarm_atleft") || prefs.getBoolean("system_statusbar_sound_atleft") || prefs.getBoolean("system_statusbar_dnd_atleft") || prefs.getBoolean("system_statusbar_gps_atleft")) || prefs.getBoolean("system_statusbar_netspeed_atleft") || (prefs.getBoolean("system_statusbar_dualrows") && prefs.getBoolean("system_statusbar_netspeed_atsecondrow")) || prefs.getBoolean("system_statusbaricons_wifi_mobile_atleft") || prefs.getBoolean("system_statusbaricons_swap_wifi_mobile")`

### hasAnySystemUiStartupFeature_if_40
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 373-373
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_clock_position', 'system_statusbar_dualrows']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [1, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `prefs.getStringAsInt("system_statusbar_clock_position", 1) > 1 && !prefs.getBoolean("system_statusbar_dualrows")`

### hasAnySystemUiStartupFeature_if_41
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 374-374
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_batterystyle']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbar_batterystyle")`

### hasAnySystemUiStartupFeature_if_42
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 375-375
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_batterytempandcurrent', 'system_statusbar_showdevicetemperature']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_statusbar_batterytempandcurrent") || prefs.getBoolean("system_statusbar_showdevicetemperature")`

### hasAnySystemUiStartupFeature_if_43
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 376-376
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_topmargin', 'system_statusbar_topmargin_unset_lockscreen']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['AND']
- raw_expression: `prefs.getBoolean("system_statusbar_topmargin") && prefs.getBoolean("system_statusbar_topmargin_unset_lockscreen")`

### hasAnySystemUiStartupFeature_if_44
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 377-377
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_horizmargin']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbar_horizmargin")`

### hasAnySystemUiStartupFeature_if_45
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 378-378
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_showpct']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_showpct")`

### hasAnySystemUiStartupFeature_if_46
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 379-379
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hidelsstatusbar']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_hidelsstatusbar")`

### hasAnySystemUiStartupFeature_if_47
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 380-380
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hidelsclock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_hidelsclock")`

### hasAnySystemUiStartupFeature_if_48
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 381-381
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_ls_force_systemfonts']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_ls_force_systemfonts")`

### hasAnySystemUiStartupFeature_if_49
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 382-382
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hidelshint']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_hidelshint")`

### hasAnySystemUiStartupFeature_if_50
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 383-383
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_allowdirectreply']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_allowdirectreply")`

### hasAnySystemUiStartupFeature_if_51
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 384-384
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_allownotifonkeyguard']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_allownotifonkeyguard")`

### hasAnySystemUiStartupFeature_if_52
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 385-385
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_allownotiffloat']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_allownotiffloat")`

### hasAnySystemUiStartupFeature_if_53
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 386-386
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hideqs']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_hideqs")`

### hasAnySystemUiStartupFeature_if_54
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 387-387
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lsalarm']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_lsalarm")`

### hasAnySystemUiStartupFeature_if_55
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 388-388
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbarcontrols']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbarcontrols")`

### hasAnySystemUiStartupFeature_if_56
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 389-389
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nonetspeedseparator']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_nonetspeedseparator")`

### hasAnySystemUiStartupFeature_if_57
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 390-390
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_clock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbaricons_clock")`

### hasAnySystemUiStartupFeature_if_58
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 391-391
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_detailednetspeed_fakedualrow', 'system_detailednetspeed', 'system_detailednetspeed_secunit', 'system_detailednetspeed_low']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'NOT']
- raw_expression: `prefs.getBoolean("system_detailednetspeed_fakedualrow") || (!prefs.getBoolean("system_detailednetspeed") && (prefs.getBoolean("system_detailednetspeed_secunit") || prefs.getBoolean("system_detailednetspeed_low") ) )`

### hasAnySystemUiStartupFeature_if_59
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 392-392
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_netspeed_fontsize', 'system_netspeed_verticaloffset', 'system_detailednetspeed', 'system_detailednetspeed_fakedualrow', 'system_netspeed_bold', 'system_netspeed_leftmargin', 'system_netspeed_fixedcontent_width', 'system_netspeed_rightmargin', 'system_detailednetspeed_align']
- accessors: ['getInt', 'getInt', 'getBoolean', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getStringAsInt']
- default_values: [13, 8, False, False, False, 0, 10, 0, 1]
- default_kinds: ['EXPLICIT', 'EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['>', '!=', '>', '>', '>', '>']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `prefs.getInt("system_netspeed_fontsize", 13) > 13 || prefs.getInt("system_netspeed_verticaloffset", 8) != 8 || prefs.getBoolean("system_detailednetspeed") || prefs.getBoolean("system_detailednetspeed_fakedualrow") || prefs.getBoolean("system_netspeed_bold") || prefs.getInt("system_netspeed_leftmargin", 0) > 0 || prefs.getInt("system_netspeed_fixedcontent_width", 10) > 10 || prefs.getInt("system_netspeed_rightmargin", 0) > 0 || prefs.getStringAsInt("system_detailednetspeed_align", 1) > 1`

### hasAnySystemUiStartupFeature_if_60
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 393-393
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_taptounlock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_taptounlock")`

### hasAnySystemUiStartupFeature_if_61
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 394-394
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nosos']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_nosos")`

### hasAnySystemUiStartupFeature_if_62
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 395-395
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_morenotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_morenotif")`

### hasAnySystemUiStartupFeature_if_63
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 396-396
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_charginginfo']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_charginginfo")`

### hasAnySystemUiStartupFeature_if_64
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 397-397
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_secureqs']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_secureqs")`

### hasAnySystemUiStartupFeature_if_65
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 398-398
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_mutevisiblenotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_mutevisiblenotif")`

### hasAnySystemUiStartupFeature_if_66
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 399-399
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_battery1']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbaricons_battery1")`

### hasAnySystemUiStartupFeature_if_67
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 400-400
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_battery3', 'system_statusbaricons_battery4', 'system_statusbaricons_battery2']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR']
- raw_expression: `prefs.getBoolean("system_statusbaricons_battery3") || prefs.getBoolean("system_statusbaricons_battery4") || prefs.getBoolean("system_statusbaricons_battery2")`

### hasAnySystemUiStartupFeature_if_68
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 401-401
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_wifistandard']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_statusbaricons_wifistandard", 1) > 1`

### hasAnySystemUiStartupFeature_if_69
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 402-402
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_signal', 'system_statusbaricons_sim1', 'system_statusbaricons_sim2', 'system_statusbaricons_sim_nodata', 'system_statusbaricons_roaming', 'system_statusbaricons_volte']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR', 'OR', 'OR', 'OR']
- raw_expression: `prefs.getBoolean("system_statusbaricons_signal") || prefs.getBoolean("system_statusbaricons_sim1") || prefs.getBoolean("system_statusbaricons_sim2") || prefs.getBoolean("system_statusbaricons_sim_nodata") || prefs.getBoolean("system_statusbaricons_roaming") || prefs.getBoolean("system_statusbaricons_volte")`

### hasAnySystemUiStartupFeature_if_70
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 403-403
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_vowifi']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbaricons_vowifi")`

### hasAnySystemUiStartupFeature_if_71
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 404-404
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_alarm', 'system_statusbaricons_alarmn']
- accessors: ['getBoolean', 'getInt']
- default_values: [False, 0]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT']
- comparators: ['>']
- boolean_operators: ['AND', 'NOT']
- raw_expression: `!prefs.getBoolean("system_statusbaricons_alarm") && prefs.getInt("system_statusbaricons_alarmn", 0) > 0`

### hasAnySystemUiStartupFeature_if_72
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 405-405
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_shortcut_app', 'system_calendar_app', 'system_clock_app']
- accessors: ['getString', 'getString', 'getString']
- default_values: ['', '', '']
- default_kinds: ['EXPLICIT', 'EXPLICIT', 'EXPLICIT']
- comparators: ['!=', '!=', '!=']
- boolean_operators: ['OR', 'OR', 'NOT', 'NOT', 'NOT']
- raw_expression: `!prefs.getString("system_shortcut_app", "").equals("") || !prefs.getString("system_calendar_app", "").equals("") || !prefs.getString("system_clock_app", "").equals("")`

### hasAnySystemUiStartupFeature_if_73
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 406-406
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qshaptics']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_qshaptics", 1) > 1`

### hasAnySystemUiStartupFeature_if_74
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 407-407
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qs_hideoperator']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_qs_hideoperator")`

### hasAnySystemUiStartupFeature_if_75
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 408-408
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_hideoperator_delimiter']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_cc_hideoperator_delimiter")`

### hasAnySystemUiStartupFeature_if_76
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 409-409
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_show_stepcount', 'system_drawer_show_stepcount']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_cc_show_stepcount") || prefs.getBoolean("system_drawer_show_stepcount")`

### hasAnySystemUiStartupFeature_if_77
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 410-410
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_disable_bluetooth_restrict']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_cc_disable_bluetooth_restrict")`

### hasAnySystemUiStartupFeature_if_78
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 411-411
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_collapse_after_clicked']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_cc_collapse_after_clicked")`

### hasAnySystemUiStartupFeature_if_79
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 412-412
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_switch_qsandnotification']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_cc_switch_qsandnotification")`

### hasAnySystemUiStartupFeature_if_80
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 413-413
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_expandnotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_expandnotifs", 1) > 1`

### hasAnySystemUiStartupFeature_if_81
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 414-414
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_inactivebrightness']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_inactivebrightness", 1) > 1`

### hasAnySystemUiStartupFeature_if_82
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 415-415
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_mobiletypeicon', 'system_networkindicator_mobile', 'system_statusbar_mobiletype_show_wificonnected']
- accessors: ['getStringAsInt', 'getBoolean', 'getBoolean']
- default_values: [1, False, False]
- default_kinds: ['EXPLICIT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- comparators: ['>']
- boolean_operators: ['OR', 'OR']
- raw_expression: `prefs.getStringAsInt("system_mobiletypeicon", 1) > 1 || prefs.getBoolean("system_networkindicator_mobile") || prefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")`

### hasAnySystemUiStartupFeature_if_83
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 416-416
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_bluetooth']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_statusbaricons_bluetooth", 1) > 1`

### hasAnySystemUiStartupFeature_if_84
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 417-417
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_epm']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_epm")`

### hasAnySystemUiStartupFeature_if_85
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 418-418
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_wifi', 'system_statusbaricons_dualwifi', 'system_statusbaricons_alarm', 'system_statusbaricons_profile', 'system_statusbaricons_sound', 'system_statusbaricons_dnd', 'system_statusbaricons_secondspace', 'system_statusbaricons_headset', 'system_statusbaricons_nfc', 'system_statusbaricons_vpn', 'system_statusbaricons_airplane', 'system_statusbaricons_hotspot', 'system_statusbaricons_nosims', 'system_statusbaricons_gps', 'system_statusbaricons_btbattery', 'system_statusbaricons_ble_unlock', 'system_statusbaricons_volte']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False, False, False, False, False, False, False, False, False, False, False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `(prefs.getBoolean("system_statusbaricons_wifi") || prefs.getBoolean("system_statusbaricons_dualwifi") || prefs.getBoolean("system_statusbaricons_alarm") || prefs.getBoolean("system_statusbaricons_profile") || prefs.getBoolean("system_statusbaricons_sound") || prefs.getBoolean("system_statusbaricons_dnd") || prefs.getBoolean("system_statusbaricons_secondspace") || prefs.getBoolean("system_statusbaricons_headset") || prefs.getBoolean("system_statusbaricons_nfc") || prefs.getBoolean("system_statusbaricons_vpn") || prefs.getBoolean("system_statusbaricons_airplane") || prefs.getBoolean("system_statusbaricons_hotspot") || prefs.getBoolean("system_statusbaricons_nosims") || prefs.getBoolean("system_statusbaricons_gps") || prefs.getBoolean("system_statusbaricons_btbattery") || prefs.getBoolean("system_statusbaricons_ble_unlock") || prefs.getBoolean("system_statusbaricons_volte"))`

### hasAnySystemUiStartupFeature_if_86
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 419-419
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_privacy', 'system_statusbaricons_mute', 'system_statusbaricons_speaker', 'system_statusbaricons_record']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR', 'OR']
- raw_expression: `prefs.getBoolean("system_statusbaricons_privacy") || prefs.getBoolean("system_statusbaricons_mute") || prefs.getBoolean("system_statusbaricons_speaker") || prefs.getBoolean("system_statusbaricons_record")`

### hasAnySystemUiStartupFeature_if_87
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 420-420
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_messagingstylelines']
- accessors: ['getInt']
- default_values: [0]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getInt("system_messagingstylelines", 0) > 0`

### hasAnySystemUiStartupFeature_if_88
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 421-421
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_allowfloat']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_betterpopups_allowfloat")`

### hasAnySystemUiStartupFeature_if_89
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 422-422
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_autoclose_expanded']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_betterpopups_autoclose_expanded")`

### hasAnySystemUiStartupFeature_if_90
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 423-423
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_betterpopups_disablewhenmute']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_betterpopups_disablewhenmute")`

### hasAnySystemUiStartupFeature_if_91
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 424-424
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_securecontrolcenter']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_securecontrolcenter")`

### hasAnySystemUiStartupFeature_if_92
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 425-425
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_minimalnotifview']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_minimalnotifview")`

### hasAnySystemUiStartupFeature_if_93
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 426-426
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notifchannelsettings']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_notifchannelsettings")`

### hasAnySystemUiStartupFeature_if_94
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 427-427
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_maxsbicons']
- accessors: ['getStringAsInt']
- default_values: [0]
- default_kinds: ['EXPLICIT']
- comparators: ['!=']
- raw_expression: `prefs.getStringAsInt("system_maxsbicons", 0) != 0`

### hasAnySystemUiStartupFeature_if_95
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 428-428
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_mobiletype_single']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbar_mobiletype_single")`

### hasAnySystemUiStartupFeature_if_96
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 429-429
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_dualsimin2rows']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbar_dualsimin2rows")`

### hasAnySystemUiStartupFeature_if_97
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 430-430
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_dualrows']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbar_dualrows")`

### hasAnySystemUiStartupFeature_if_98
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 431-431
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_ccgridcolumns', 'system_ccgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [4, 4]
- default_kinds: ['EXPLICIT', 'EXPLICIT']
- comparators: ['>', '!=']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("system_ccgridcolumns", 4) > 4 || prefs.getInt("system_ccgridrows", 4) != 4`

### hasAnySystemUiStartupFeature_if_99
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 432-432
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_colorizenotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_colorizenotifs", 1) > 1`

### hasAnySystemUiStartupFeature_if_100
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 433-433
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notify_openinfw']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_notify_openinfw")`

### hasAnySystemUiStartupFeature_if_101
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 434-434
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_fw_noblacklist']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_fw_noblacklist")`

### hasAnySystemUiStartupFeature_if_102
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 435-435
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_notify_openinfw', 'system_notifrowmenu', 'system_betterpopups_allowfloat']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [False, False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR', 'OR']
- raw_expression: `prefs.getBoolean("system_notify_openinfw") || prefs.getBoolean("system_notifrowmenu") || prefs.getBoolean("system_betterpopups_allowfloat")`

### hasAnySystemUiStartupFeature_if_103
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 436-436
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nosafevolume']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_nosafevolume")`

### hasAnySystemUiStartupFeature_if_104
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 437-437
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_lockscreen_hidezenmode']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_lockscreen_hidezenmode")`

### hasAnySystemUiStartupFeature_if_105
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 438-438
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_nopassword']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_nopassword")`

### hasAnySystemUiStartupFeature_if_106
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 439-439
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_topmargin']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_statusbar_topmargin")`

### hasAnySystemUiStartupFeature_if_107
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 440-440
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_enable_style_switch']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_cc_enable_style_switch")`

### hasAnySystemUiStartupFeature_if_108
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 441-441
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_qs_force_systemfonts']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_qs_force_systemfonts")`

### hasAnySystemUiStartupFeature_if_109
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 442-442
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_detailednetspeed_fakedualrow']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_detailednetspeed_fakedualrow")`

### hasAnySystemUiStartupFeature_if_110
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 443-443
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_volumetimer']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_volumetimer")`

### hasAnySystemUiStartupFeature_if_111
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 444-444
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_tile_roundedrect']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_cc_tile_roundedrect")`

### hasAnySystemUiStartupFeature_if_112
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 445-445
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_iconsize']
- accessors: ['getInt']
- default_values: [6]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `(prefs.getInt("system_statusbar_iconsize", 6)) > 6`

### hasAnySystemUiStartupFeature_if_113
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 446-446
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_cc_show_stepcount']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_cc_show_stepcount")`

### hasAnySystemUiStartupFeature_if_114
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 447-447
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbaricons_swap_wifi_mobile', 'system_statusbaricons_wifi_mobile_atleft']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [False, False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT']
- boolean_operators: ['OR']
- raw_expression: `(prefs.getBoolean("system_statusbaricons_swap_wifi_mobile")) || (prefs.getBoolean("system_statusbaricons_wifi_mobile_atleft"))`

### hasAnySystemUiStartupFeature_if_115
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 448-448
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_screenshot_overlay']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_screenshot_overlay")`

### hasAnySystemUiStartupFeature_if_116
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 449-449
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hidestatusbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_hidestatusbar_whenscreenshot")`

### hasAnySystemUiStartupFeature_if_117
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 450-453
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbar_clocktweak', 'system_cc_clocktweak', 'system_cc_hidedate', 'system_cc_dateformat']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getString']
- default_values: [False, False, False, '']
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT']
- boolean_operators: ['OR', 'OR', 'OR', 'NOT', 'notIsEmpty']
- raw_expression: `prefs.getBoolean("system_statusbar_clocktweak") ||
                prefs.getBoolean("system_cc_clocktweak") ||
                prefs.getBoolean("system_cc_hidedate") ||
                !prefs.getString("system_cc_dateformat", "").isEmpty()`
- normalized_expression: `prefs.getBoolean("system_statusbar_clocktweak") || prefs.getBoolean("system_cc_clocktweak") || prefs.getBoolean("system_cc_hidedate") || !prefs.getString("system_cc_dateformat", "").isEmpty()`

### hasAnySystemUiStartupFeature_if_118
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 454-454
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_hidemoreicon']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_hidemoreicon")`

### hasAnySystemUiStartupFeature_if_119
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 455-455
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_batteryindicator']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT']
- raw_expression: `prefs.getBoolean("system_batteryindicator")`

## GLOBAL_ACTION_DOMAIN_RULES (2)

### hasAnyGlobalAction_if_1
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnyGlobalAction`
- lines: 463-463
- phase: `GLOBAL_ACTION_DOMAIN`
- parse_status: `PARSED`
- branch_kind: `IF`
- comparators: ['>']
- boolean_operators: ['AND', 'AND']
- raw_expression: `isSystemUiGlobalActionKey(key) && value instanceof Integer && (Integer) value > 1`

### hasAnyGlobalAction_if_2
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `hasAnyGlobalAction`
- lines: 467-467
- phase: `GLOBAL_ACTION_DOMAIN`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_volumemedia_up', 'controls_volumemedia_down']
- accessors: ['getStringAsInt', 'getStringAsInt']
- default_values: [0, 0]
- default_kinds: ['EXPLICIT', 'EXPLICIT']
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || prefs.getStringAsInt("controls_volumemedia_down", 0) > 0`

## FEATURE_DISPATCH_CALLS (5)

### install_installById_1
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 84-84
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `tempHideOverlaySystemUI`
- raw_expression: `FeatureDispatcher.installById("tempHideOverlaySystemUI", systemuiRuntime)`

### install_installById_2
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 93-93
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideStatusBarBeforeScreenshot`
- raw_expression: `FeatureDispatcher.installById("hideStatusBarBeforeScreenshot", systemuiRuntime)`

### install_installById_3
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 114-114
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `statusBarClockTweak`
- raw_expression: `FeatureDispatcher.installById("statusBarClockTweak", systemuiRuntime)`

### install_installById_4
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 125-125
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `noMoreIcon`
- raw_expression: `FeatureDispatcher.installById("noMoreIcon", systemuiRuntime)`

### install_installById_5
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 152-152
- phase: `POST_RESTART_GUARD_RUNTIME`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `batteryIndicator`
- raw_expression: `FeatureDispatcher.installById("batteryIndicator", systemuiRuntime)`

## FEATURE_CATALOG_GATES (13)

### FeatureCatalog_statusBarClockTweak
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 102-169
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `statusBarClockTweak`
- declared_preference_keys: ['system_cc_clocktweak', 'system_cc_dateformat', 'system_cc_hidedate', 'system_statusbar_clocktweak']
- condition_preference_keys: ['system_statusbar_clocktweak', 'system_cc_clocktweak', 'system_cc_hidedate', 'system_cc_dateformat']
- preference_keys: ['system_statusbar_clocktweak', 'system_cc_clocktweak', 'system_cc_hidedate', 'system_cc_dateformat']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getString']
- default_values: [False, False, False, '']
- default_kinds: ['IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'IMPLICIT_PREFMAP_DEFAULT', 'EXPLICIT']
- boolean_operators: ['OR', 'OR', 'OR', 'notIsEmpty']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_statusbar_clocktweak") ||
                prefs.getBoolean("system_cc_clocktweak") ||
                prefs.getBoolean("system_cc_hidedate") ||
                prefs.getString("system_cc_dateformat", "").isNotEmpty()
            }`
- normalized_expression: `prefs.getBoolean("system_statusbar_clocktweak") || prefs.getBoolean("system_cc_clocktweak") || prefs.getBoolean("system_cc_hidedate") || prefs.getString("system_cc_dateformat", "").isNotEmpty()`

### FeatureCatalog_noMoreIcon
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 277-322
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `noMoreIcon`
- declared_preference_keys: ['system_hidemoreicon']
- condition_preference_keys: ['system_hidemoreicon']
- preference_keys: ['system_hidemoreicon']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_hidemoreicon", false)
            }`
- normalized_expression: `prefs.getBoolean("system_hidemoreicon", false)`

### FeatureCatalog_batteryIndicator
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 323-368
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `batteryIndicator`
- declared_preference_keys: ['system_batteryindicator']
- condition_preference_keys: ['system_batteryindicator']
- preference_keys: ['system_batteryindicator']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_batteryindicator", false)
            }`
- normalized_expression: `prefs.getBoolean("system_batteryindicator", false)`

### FeatureCatalog_networkIndicatorWifi
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 575-601
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `networkIndicatorWifi`
- declared_preference_keys: ['system_networkindicator_wifi']
- condition_preference_keys: ['system_networkindicator_wifi']
- preference_keys: ['system_networkindicator_wifi']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_networkindicator_wifi", false)
            }`
- normalized_expression: `prefs.getBoolean("system_networkindicator_wifi", false)`

### FeatureCatalog_muteVisibleNotifications
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 602-628
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `muteVisibleNotifications`
- declared_preference_keys: ['system_mutevisiblenotif']
- condition_preference_keys: ['system_mutevisiblenotif']
- preference_keys: ['system_mutevisiblenotif']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_mutevisiblenotif", false)
            }`
- normalized_expression: `prefs.getBoolean("system_mutevisiblenotif", false)`

### FeatureCatalog_hideDismissView
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 740-766
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideDismissView`
- declared_preference_keys: ['system_removedismiss']
- condition_preference_keys: ['system_removedismiss']
- preference_keys: ['system_removedismiss']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_removedismiss", false)
            }`
- normalized_expression: `prefs.getBoolean("system_removedismiss", false)`

### FeatureCatalog_hideLockScreenHint
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 767-793
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideLockScreenHint`
- declared_preference_keys: ['system_hidelshint']
- condition_preference_keys: ['system_hidelshint']
- preference_keys: ['system_hidelshint']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_hidelshint", false)
            }`
- normalized_expression: `prefs.getBoolean("system_hidelshint", false)`

### FeatureCatalog_noNetworkSpeedSeparator
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 905-931
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `noNetworkSpeedSeparator`
- declared_preference_keys: ['system_nonetspeedseparator']
- condition_preference_keys: ['system_nonetspeedseparator']
- preference_keys: ['system_nonetspeedseparator']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_nonetspeedseparator", false)
            }`
- normalized_expression: `prefs.getBoolean("system_nonetspeedseparator", false)`

### FeatureCatalog_hideIconsClock
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 932-958
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideIconsClock`
- declared_preference_keys: ['system_statusbaricons_clock']
- condition_preference_keys: ['system_statusbaricons_clock']
- preference_keys: ['system_statusbaricons_clock']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_statusbaricons_clock", false)
            }`
- normalized_expression: `prefs.getBoolean("system_statusbaricons_clock", false)`

### FeatureCatalog_tempHideOverlaySystemUI
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 988-1014
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `tempHideOverlaySystemUI`
- declared_preference_keys: ['system_screenshot_overlay']
- condition_preference_keys: ['system_screenshot_overlay']
- preference_keys: ['system_screenshot_overlay']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_screenshot_overlay", false)
            }`
- normalized_expression: `prefs.getBoolean("system_screenshot_overlay", false)`

### FeatureCatalog_hideStatusBarBeforeScreenshot
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 1015-1041
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideStatusBarBeforeScreenshot`
- declared_preference_keys: ['system_hidestatusbar_whenscreenshot']
- condition_preference_keys: ['system_hidestatusbar_whenscreenshot']
- preference_keys: ['system_hidestatusbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_hidestatusbar_whenscreenshot", false)
            }`
- normalized_expression: `prefs.getBoolean("system_hidestatusbar_whenscreenshot", false)`

### FeatureCatalog_hideNavBarBeforeScreenshot
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 1042-1068
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `hideNavBarBeforeScreenshot`
- declared_preference_keys: ['controls_hidenavbar_whenscreenshot']
- condition_preference_keys: ['controls_hidenavbar_whenscreenshot']
- preference_keys: ['controls_hidenavbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("controls_hidenavbar_whenscreenshot", false)
            }`
- normalized_expression: `prefs.getBoolean("controls_hidenavbar_whenscreenshot", false)`

### FeatureCatalog_chargingInfo
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 1179-1212
- phase: `FEATURE_CATALOG_GATE`
- parse_status: `PARSED`
- branch_kind: `IF`
- feature_id: `chargingInfo`
- declared_preference_keys: ['system_charginginfo', 'system_charginginfo_current', 'system_charginginfo_temp', 'system_charginginfo_view', 'system_charginginfo_voltage', 'system_charginginfo_wattage']
- condition_preference_keys: ['system_charginginfo']
- preference_key_difference: ['system_charginginfo_current', 'system_charginginfo_temp', 'system_charginginfo_view', 'system_charginginfo_voltage', 'system_charginginfo_wattage']
- preference_keys: ['system_charginginfo']
- accessors: ['getBoolean']
- default_values: [False]
- default_kinds: ['EXPLICIT']
- raw_expression: `{ prefs ->
                prefs.getBoolean("system_charginginfo", false)
            }`
- normalized_expression: `prefs.getBoolean("system_charginginfo", false)`

## RESOURCE_PHASE_CONDITIONS (2)

### install_if_2
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 51-51
- phase: `PRE_RESTART_GUARD_RESOURCE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['system_statusbarheight']
- accessors: ['getInt']
- default_values: [19]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_statusbarheight", 19) > 19`

### install_if_3
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `install`
- lines: 52-52
- phase: `PRE_RESTART_GUARD_RESOURCE`
- parse_status: `PARSED`
- branch_kind: `IF`
- preference_keys: ['controls_navbarheight']
- accessors: ['getInt']
- default_values: [19]
- default_kinds: ['EXPLICIT']
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("controls_navbarheight", 19) > 19`

## RESTART_GUARD (1)

### isWithinSystemUiRestartGuard_predicate
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- source_method: `isWithinSystemUiRestartGuard`
- lines: 493-493
- phase: `RESTART_GUARD`
- parse_status: `PARSED`
- branch_kind: `IF`
- comparators: ['<']
- raw_expression: `currentTime - restartTime < 10000`

## UNPARSED (0)

## PARTIAL (0)
