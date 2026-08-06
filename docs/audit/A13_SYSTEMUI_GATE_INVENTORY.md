# A13 SystemUI Gate Inventory

This document records the raw conditions found in SystemUI startup gating.
It does not judge INSTALLER_ONLY, GATE_ONLY, DEFAULT_MISMATCH, etc.

Total entries: 256

| parse_status | count |
|--------------|-------|
| PARSED | 242 |
| PARTIAL | 0 |
| UNPARSED | 14 |

## INSTALL_CONDITIONS (108)

### install_if_1
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 3-3
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- boolean_operators: ['NOT']
- raw_expression: `!pkg.equals("com.android.systemui")`

### install_if_2
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 5-5
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- preference_keys: ['system_statusbarheight']
- accessors: ['getInt']
- default_values: [19]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_statusbarheight", 19) > 19`

### install_if_3
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 6-6
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- preference_keys: ['controls_navbarheight']
- accessors: ['getInt']
- default_values: [19]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("controls_navbarheight", 19) > 19`

### install_if_4
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 11-11
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- comparators: ['!=']
- raw_expression: `NetworkSpeedViewCls != null`

### install_if_5
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 18-18
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- boolean_operators: ['NOT']
- raw_expression: `!isHooked`

### install_if_6
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 28-28
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- raw_expression: `isWithinSystemUiRestartGuard(restartTime, currentTime)`

### install_if_7
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 34-36
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- preference_keys: ['various_showcallui', 'controls_volumecursor']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [0, None]
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0
                || MainModule.mPrefs.getBoolean("controls_volumecursor")`

### install_if_8
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 40-43
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideStatusBarBeforeScreenshot`
- preference_keys: ['system_fivegtile', 'system_cc_fpstile']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_fivegtile")
                || MainModule.mPrefs.getBoolean("system_cc_fpstile")`

### install_if_9
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 49-49
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `networkIndicatorWifi`
- preference_keys: ['system_qsgridcolumns', 'system_qsgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [2, 1]
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("system_qsgridcolumns", 2) > 2 || MainModule.mPrefs.getInt("system_qsgridrows", 1) > 1`

### install_if_10
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 50-50
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `networkIndicatorWifi`
- preference_keys: ['system_qqsgridcolumns']
- accessors: ['getInt']
- default_values: [2]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_qqsgridcolumns", 2) > 2`

### install_if_11
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 51-51
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `networkIndicatorWifi`
- preference_keys: ['system_networkindicator_wifi']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_networkindicator_wifi")`

### install_if_12
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 53-53
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_drawer_blur']
- accessors: ['getInt']
- default_values: [100]
- comparators: ['<']
- raw_expression: `MainModule.mPrefs.getInt("system_drawer_blur", 100) < 100`

### install_if_13
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 54-54
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_chargeanimtime']
- accessors: ['getInt']
- default_values: [20]
- comparators: ['<']
- raw_expression: `MainModule.mPrefs.getInt("system_chargeanimtime", 20) < 20`

### install_if_14
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 55-55
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_betterpopups_delay', 'system_betterpopups_nohide']
- accessors: ['getInt', 'getBoolean']
- default_values: [0, None]
- comparators: ['>']
- boolean_operators: ['NOT', 'AND']
- raw_expression: `MainModule.mPrefs.getInt("system_betterpopups_delay", 0) > 0 && !MainModule.mPrefs.getBoolean("system_betterpopups_nohide")`

### install_if_15
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 56-56
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_netspeedinterval']
- accessors: ['getInt']
- default_values: [4]
- comparators: ['!=']
- raw_expression: `MainModule.mPrefs.getInt("system_netspeedinterval", 4) != 4`

### install_if_16
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 57-57
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_qsgridrows', 'system_qsnolabels']
- accessors: ['getInt', 'getBoolean']
- default_values: [1, None]
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("system_qsgridrows", 1) > 1 || MainModule.mPrefs.getBoolean("system_qsnolabels")`

### install_if_17
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 58-58
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_lstimeout']
- accessors: ['getInt']
- default_values: [3]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_lstimeout", 3) > 3`

### install_if_18
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 59-61
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['controls_fsg_assist_left_action', 'controls_fsg_assist_right_action']
- accessors: ['getInt', 'getInt']
- default_values: [1, 1]
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("controls_fsg_assist_left_action", 1) > 1
                || MainModule.mPrefs.getInt("controls_fsg_assist_right_action", 1) > 1`

### install_if_19
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 62-65
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['controls_navbarleft_action', 'controls_navbarleftlong_action', 'controls_navbarright_action', 'controls_navbarrightlong_action']
- accessors: ['getInt', 'getInt', 'getInt', 'getInt']
- default_values: [1, 1, 1, 1]
- comparators: ['>', '>', '>', '>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("controls_navbarleft_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarleftlong_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarright_action", 1) > 1 ||
                    MainModule.mPrefs.getInt("controls_navbarrightlong_action", 1) > 1`

### install_if_20
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 66-66
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_scramblepin']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_scramblepin")`

### install_if_21
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 67-67
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- preference_keys: ['system_dttosleep']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_dttosleep")`

### install_if_22
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 69-69
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- preference_keys: ['system_noscreenlock_act']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_noscreenlock_act")`

### install_if_23
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 70-73
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- preference_keys: ['system_detailednetspeed', 'system_detailednetspeed_fakedualrow']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['NOT', 'AND']
- raw_expression: `MainModule.mPrefs.getBoolean("system_detailednetspeed")
                && !MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")`

### install_if_24
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 74-74
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- preference_keys: ['system_albumartonlock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_albumartonlock")`

### install_if_25
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 75-75
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- preference_keys: ['system_expandheadups']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_expandheadups", 1) > 1`

### install_if_26
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 76-76
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- preference_keys: ['system_betterpopups_nohide']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_nohide")`

### install_if_27
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 77-77
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- preference_keys: ['system_betterpopups_swipedown']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_swipedown")`

### install_if_28
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 78-78
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- preference_keys: ['system_betterpopups_center']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_center")`

### install_if_29
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 80-80
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideDismissView`
- preference_keys: ['system_notifafterunlock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_notifafterunlock")`

### install_if_30
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 81-81
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideDismissView`
- preference_keys: ['system_notifrowmenu']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_notifrowmenu")`

### install_if_31
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 82-82
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideDismissView`
- preference_keys: ['system_compactnotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_compactnotif")`

### install_if_32
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 83-83
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideDismissView`
- preference_keys: ['system_removedismiss']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_removedismiss")`

### install_if_33
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 84-84
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideNavBarBeforeScreenshot`
- preference_keys: ['system_drawer_removeshortcut']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_drawer_removeshortcut")`

### install_if_34
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 85-85
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideNavBarBeforeScreenshot`
- preference_keys: ['controls_nonavbar']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("controls_nonavbar")`

### install_if_35
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 86-86
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideNavBarBeforeScreenshot`
- preference_keys: ['controls_hidenavbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("controls_hidenavbar_whenscreenshot")`

### install_if_36
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 87-87
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `batteryIndicator`
- preference_keys: ['controls_imebackalticon']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("controls_imebackalticon")`

### install_if_37
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 88-88
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `batteryIndicator`
- preference_keys: ['system_visualizer']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_visualizer")`

### install_if_38
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 89-103
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `batteryIndicator`
- preference_keys: ['system_nosilentvibrate', 'system_qs_force_systemfonts', 'system_volumetimer', 'system_qsnolabels', 'system_cc_volume_showpct', 'system_volumebar_blur_mtk', 'system_cc_hidedate', 'system_cc_hide_shortcuticons', 'system_cc_clocktweak', 'system_cc_tile_roundedrect', 'system_cc_bluetooth_tile_style', 'system_separatevolume', 'system_separatevolume_slider', 'system_volumedialogdelay_collapsed', 'system_volumedialogdelay_expanded', 'system_volumeblur_collapsed', 'system_volumeblur_expanded']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getStringAsInt', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getInt']
- default_values: [None, None, None, None, None, None, None, None, None, None, 1, None, None, 0, 0, 0, 0]
- comparators: ['>', '>', '>', '>', '>']
- boolean_operators: ['AND', 'OR']
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

### install_if_39
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 107-107
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_disableanynotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_disableanynotif")`

### install_if_40
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 108-108
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_lockscreenshortcuts']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_lockscreenshortcuts")`

### install_if_41
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 109-112
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_4gtolte', 'system_statusbar_mobiletype_single', 'system_statusbar_mobile_showname']
- accessors: ['getBoolean', 'getBoolean', 'getString']
- default_values: [None, None, '']
- boolean_operators: ['NOT', 'AND', 'OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_4gtolte")
                || (MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single") &&
                    !MainModule.mPrefs.getString("system_statusbar_mobile_showname", "").equals(""))`

### install_if_42
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 125-130
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_statusbar_netspeed_atleft', 'system_statusbar_dualrows', 'system_statusbar_netspeed_atsecondrow', 'system_statusbaricons_wifi_mobile_atleft', 'system_statusbaricons_swap_wifi_mobile']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None, None]
- boolean_operators: ['AND', 'OR']
- raw_expression: `moveRight || moveLeft
                || MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atleft")
                || (MainModule.mPrefs.getBoolean("system_statusbar_dualrows") && MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atsecondrow"))
                || MainModule.mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_swap_wifi_mobile")`

### install_if_43
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 133-133
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_statusbar_clock_position', 'system_statusbar_dualrows']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [1, None]
- comparators: ['>']
- boolean_operators: ['NOT', 'AND']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_statusbar_clock_position", 1) > 1 && !MainModule.mPrefs.getBoolean("system_statusbar_dualrows")`

### install_if_44
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 136-136
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_statusbar_batterystyle']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_batterystyle")`

### install_if_45
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 139-141
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_statusbar_batterytempandcurrent', 'system_statusbar_showdevicetemperature']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent")
                || MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature")`

### install_if_46
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 142-142
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_statusbar_topmargin', 'system_statusbar_topmargin_unset_lockscreen']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['AND']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_topmargin") && MainModule.mPrefs.getBoolean("system_statusbar_topmargin_unset_lockscreen")`

### install_if_47
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 143-143
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_statusbar_horizmargin']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_horizmargin")`

### install_if_48
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 144-144
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_showpct']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_showpct")`

### install_if_49
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 145-145
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_hidelsstatusbar']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_hidelsstatusbar")`

### install_if_50
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 146-146
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_hidelsclock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_hidelsclock")`

### install_if_51
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 147-147
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_ls_force_systemfonts']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_ls_force_systemfonts")`

### install_if_52
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 148-148
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- preference_keys: ['system_hidelshint']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_hidelshint")`

### install_if_53
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 149-149
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_allowdirectreply']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_allowdirectreply")`

### install_if_54
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 150-150
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_allownotifonkeyguard']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_allownotifonkeyguard")`

### install_if_55
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 151-151
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_allownotiffloat']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_allownotiffloat")`

### install_if_56
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 152-152
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_hideqs']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_hideqs")`

### install_if_57
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 153-153
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_lsalarm']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_lsalarm")`

### install_if_58
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 154-154
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_statusbarcontrols']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbarcontrols")`

### install_if_59
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 155-155
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- preference_keys: ['system_nonetspeedseparator']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_nonetspeedseparator")`

### install_if_60
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 156-156
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `hideIconsClock`
- preference_keys: ['system_statusbaricons_clock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_clock")`

### install_if_61
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 157-163
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `chargingInfo`
- preference_keys: ['system_detailednetspeed_fakedualrow', 'system_detailednetspeed', 'system_detailednetspeed_secunit', 'system_detailednetspeed_low']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None]
- boolean_operators: ['NOT', 'AND', 'OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || (!MainModule.mPrefs.getBoolean("system_detailednetspeed")
                    && (MainModule.mPrefs.getBoolean("system_detailednetspeed_secunit")
                        || MainModule.mPrefs.getBoolean("system_detailednetspeed_low")
                        )
                    )`

### install_if_62
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 166-176
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `chargingInfo`
- preference_keys: ['system_netspeed_fontsize', 'system_netspeed_verticaloffset', 'system_detailednetspeed', 'system_detailednetspeed_fakedualrow', 'system_netspeed_bold', 'system_netspeed_leftmargin', 'system_netspeed_fixedcontent_width', 'system_netspeed_rightmargin', 'system_detailednetspeed_align']
- accessors: ['getInt', 'getInt', 'getBoolean', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getStringAsInt']
- default_values: [13, 8, None, None, None, 0, 10, 0, 1]
- comparators: ['>', '!=', '>', '>', '>', '>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("system_netspeed_fontsize", 13) > 13
                || MainModule.mPrefs.getInt("system_netspeed_verticaloffset", 8) != 8
                || MainModule.mPrefs.getBoolean("system_detailednetspeed")
                || MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
                || MainModule.mPrefs.getBoolean("system_netspeed_bold")
                || MainModule.mPrefs.getInt("system_netspeed_leftmargin", 0) > 0
                || MainModule.mPrefs.getInt("system_netspeed_fixedcontent_width", 10) > 10
                || MainModule.mPrefs.getInt("system_netspeed_rightmargin", 0) > 0
                || MainModule.mPrefs.getStringAsInt("system_detailednetspeed_align", 1) > 1`

### install_if_63
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 179-179
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `chargingInfo`
- preference_keys: ['system_taptounlock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_taptounlock")`

### install_if_64
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 180-180
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `chargingInfo`
- preference_keys: ['system_nosos']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_nosos")`

### install_if_65
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 181-181
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `chargingInfo`
- preference_keys: ['system_morenotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_morenotif")`

### install_if_66
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 182-182
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `chargingInfo`
- preference_keys: ['system_charginginfo']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_charginginfo")`

### install_if_67
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 183-183
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `muteVisibleNotifications`
- preference_keys: ['system_secureqs']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_secureqs")`

### install_if_68
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 184-184
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- feature_id: `muteVisibleNotifications`
- preference_keys: ['system_mutevisiblenotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_mutevisiblenotif")`

### install_if_69
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 185-185
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_battery1']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_battery1")`

### install_if_70
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 186-189
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_battery3', 'system_statusbaricons_battery4', 'system_statusbaricons_battery2']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None]
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_battery3")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_battery4")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_battery2")`

### install_if_71
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 190-190
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_wifistandard']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_statusbaricons_wifistandard", 1) > 1`

### install_if_72
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 191-197
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_signal', 'system_statusbaricons_sim1', 'system_statusbaricons_sim2', 'system_statusbaricons_sim_nodata', 'system_statusbaricons_roaming', 'system_statusbaricons_volte']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None, None, None]
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_signal")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim1")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim2")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_sim_nodata")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_roaming")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_volte")`

### install_if_73
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 198-198
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_vowifi']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_vowifi")`

### install_if_74
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 199-199
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_alarm', 'system_statusbaricons_alarmn']
- accessors: ['getBoolean', 'getInt']
- default_values: [None, 0]
- comparators: ['>']
- boolean_operators: ['NOT', 'AND']
- raw_expression: `!MainModule.mPrefs.getBoolean("system_statusbaricons_alarm") && MainModule.mPrefs.getInt("system_statusbaricons_alarmn", 0) > 0`

### install_if_75
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 200-202
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_shortcut_app', 'system_calendar_app', 'system_clock_app']
- accessors: ['getString', 'getString', 'getString']
- default_values: ['', '', '']
- boolean_operators: ['NOT', 'OR']
- raw_expression: `!MainModule.mPrefs.getString("system_shortcut_app", "").equals("")
                || !MainModule.mPrefs.getString("system_calendar_app", "").equals("")
                || !MainModule.mPrefs.getString("system_clock_app", "").equals("")`

### install_if_76
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 203-203
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_qshaptics']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_qshaptics", 1) > 1`

### install_if_77
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 204-204
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_qs_hideoperator']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_qs_hideoperator")`

### install_if_78
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 205-205
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_hideoperator_delimiter']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_hideoperator_delimiter")`

### install_if_79
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 206-208
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_show_stepcount', 'system_drawer_show_stepcount']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_show_stepcount")
                || MainModule.mPrefs.getBoolean("system_drawer_show_stepcount")`

### install_if_80
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 209-209
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_disable_bluetooth_restrict']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_disable_bluetooth_restrict")`

### install_if_81
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 210-210
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_collapse_after_clicked']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_collapse_after_clicked")`

### install_if_82
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 211-211
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_switch_qsandnotification']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_cc_switch_qsandnotification")`

### install_if_83
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 212-212
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_expandnotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_expandnotifs", 1) > 1`

### install_if_84
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 213-213
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_inactivebrightness']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_inactivebrightness", 1) > 1`

### install_if_85
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 214-217
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_mobiletypeicon', 'system_networkindicator_mobile', 'system_statusbar_mobiletype_show_wificonnected']
- accessors: ['getStringAsInt', 'getBoolean', 'getBoolean']
- default_values: [1, None, None]
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_mobiletypeicon", 1) > 1
                || MainModule.mPrefs.getBoolean("system_networkindicator_mobile")
                || MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")`

### install_if_86
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 220-220
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_bluetooth']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_statusbaricons_bluetooth", 1) > 1`

### install_if_87
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 221-221
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_epm']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_epm")`

### install_if_88
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 241-241
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- raw_expression: `hideIconsActive`

### install_if_89
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 243-248
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_privacy', 'system_statusbaricons_mute', 'system_statusbaricons_speaker', 'system_statusbaricons_record']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None]
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbaricons_privacy")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_mute")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_speaker")
                || MainModule.mPrefs.getBoolean("system_statusbaricons_record")`

### install_if_90
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 249-249
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_messagingstylelines']
- accessors: ['getInt']
- default_values: [0]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getInt("system_messagingstylelines", 0) > 0`

### install_if_91
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 250-250
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_allowfloat']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")`

### install_if_92
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 251-251
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_autoclose_expanded']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_autoclose_expanded")`

### install_if_93
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 252-252
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_disablewhenmute']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_betterpopups_disablewhenmute")`

### install_if_94
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 253-253
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_securecontrolcenter']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_securecontrolcenter")`

### install_if_95
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 254-254
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_minimalnotifview']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_minimalnotifview")`

### install_if_96
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 255-255
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_notifchannelsettings']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_notifchannelsettings")`

### install_if_97
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 256-256
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_maxsbicons']
- accessors: ['getStringAsInt']
- default_values: [0]
- comparators: ['!=']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_maxsbicons", 0) != 0`

### install_if_98
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 257-257
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_mobiletype_single']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single")`

### install_if_99
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 260-260
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_dualsimin2rows']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_dualsimin2rows")`

### install_if_100
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 263-263
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_dualrows']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_statusbar_dualrows")`

### install_if_101
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 266-266
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_ccgridcolumns', 'system_ccgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [4, 4]
- comparators: ['>', '!=']
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getInt("system_ccgridcolumns", 4) > 4 || MainModule.mPrefs.getInt("system_ccgridrows", 4) != 4`

### install_if_102
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 267-267
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_colorizenotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `MainModule.mPrefs.getStringAsInt("system_colorizenotifs", 1) > 1`

### install_if_103
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 268-268
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_notify_openinfw']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_notify_openinfw")`

### install_if_104
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 269-269
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_fw_noblacklist']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_fw_noblacklist")`

### install_if_105
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 271-274
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_notify_openinfw', 'system_notifrowmenu', 'system_betterpopups_allowfloat']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None]
- boolean_operators: ['OR']
- raw_expression: `MainModule.mPrefs.getBoolean("system_notify_openinfw")
                || MainModule.mPrefs.getBoolean("system_notifrowmenu")
                || MainModule.mPrefs.getBoolean("system_betterpopups_allowfloat")`

### install_if_106
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 277-277
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_nosafevolume']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_nosafevolume")`

### install_if_107
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 280-280
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_lockscreen_hidezenmode']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_lockscreen_hidezenmode")`

### install_if_108
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 283-283
- phase: `PACKAGE_READY_RESOURCE`
- parse_status: `PARSED`
- preference_keys: ['system_nopassword']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `MainModule.mPrefs.getBoolean("system_nopassword")`

## STARTUP_GATE_CONDITIONS (119)

### hasAnySystemUiStartupFeature_if_1
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 2-2
- phase: `STARTUP_GATE`
- parse_status: `UNPARSED`
- raw_expression: `hasAnyGlobalAction(prefs)`

### hasAnySystemUiStartupFeature_if_2
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 3-3
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbarheight']
- accessors: ['getInt']
- default_values: [19]
- comparators: ['>']
- raw_expression: `prefs.getInt("system_statusbarheight", 19) > 19`

### hasAnySystemUiStartupFeature_if_3
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 4-4
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['controls_navbarheight']
- accessors: ['getInt']
- default_values: [19]
- comparators: ['>']
- raw_expression: `prefs.getInt("controls_navbarheight", 19) > 19`

### hasAnySystemUiStartupFeature_if_4
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 5-5
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['various_showcallui', 'controls_volumecursor']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [0, None]
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getStringAsInt("various_showcallui", 0) > 0 || prefs.getBoolean("controls_volumecursor")`

### hasAnySystemUiStartupFeature_if_5
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 6-6
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_fivegtile', 'system_cc_fpstile']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_fivegtile") || prefs.getBoolean("system_cc_fpstile")`

### hasAnySystemUiStartupFeature_if_6
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 7-7
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_qsgridcolumns', 'system_qsgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [2, 1]
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("system_qsgridcolumns", 2) > 2 || prefs.getInt("system_qsgridrows", 1) > 1`

### hasAnySystemUiStartupFeature_if_7
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 8-8
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_qqsgridcolumns']
- accessors: ['getInt']
- default_values: [2]
- comparators: ['>']
- raw_expression: `prefs.getInt("system_qqsgridcolumns", 2) > 2`

### hasAnySystemUiStartupFeature_if_8
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 9-9
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_networkindicator_wifi']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_networkindicator_wifi")`

### hasAnySystemUiStartupFeature_if_9
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 10-10
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_drawer_blur']
- accessors: ['getInt']
- default_values: [100]
- comparators: ['<']
- raw_expression: `prefs.getInt("system_drawer_blur", 100) < 100`

### hasAnySystemUiStartupFeature_if_10
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 11-11
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_chargeanimtime']
- accessors: ['getInt']
- default_values: [20]
- comparators: ['<']
- raw_expression: `prefs.getInt("system_chargeanimtime", 20) < 20`

### hasAnySystemUiStartupFeature_if_11
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 12-12
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_delay', 'system_betterpopups_nohide']
- accessors: ['getInt', 'getBoolean']
- default_values: [0, None]
- comparators: ['>']
- boolean_operators: ['NOT', 'AND']
- raw_expression: `prefs.getInt("system_betterpopups_delay", 0) > 0 && !prefs.getBoolean("system_betterpopups_nohide")`

### hasAnySystemUiStartupFeature_if_12
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 13-13
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_netspeedinterval']
- accessors: ['getInt']
- default_values: [4]
- comparators: ['!=']
- raw_expression: `prefs.getInt("system_netspeedinterval", 4) != 4`

### hasAnySystemUiStartupFeature_if_13
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 14-14
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_qsgridrows', 'system_qsnolabels']
- accessors: ['getInt', 'getBoolean']
- default_values: [1, None]
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("system_qsgridrows", 1) > 1 || prefs.getBoolean("system_qsnolabels")`

### hasAnySystemUiStartupFeature_if_14
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 15-15
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_lstimeout']
- accessors: ['getInt']
- default_values: [3]
- comparators: ['>']
- raw_expression: `prefs.getInt("system_lstimeout", 3) > 3`

### hasAnySystemUiStartupFeature_if_15
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 16-16
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['controls_fsg_assist_left_action', 'controls_fsg_assist_right_action']
- accessors: ['getInt', 'getInt']
- default_values: [1, 1]
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("controls_fsg_assist_left_action", 1) > 1 || prefs.getInt("controls_fsg_assist_right_action", 1) > 1`

### hasAnySystemUiStartupFeature_if_16
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 17-17
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['controls_navbarleft_action', 'controls_navbarleftlong_action', 'controls_navbarright_action', 'controls_navbarrightlong_action']
- accessors: ['getInt', 'getInt', 'getInt', 'getInt']
- default_values: [1, 1, 1, 1]
- comparators: ['>', '>', '>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("controls_navbarleft_action", 1) > 1 || prefs.getInt("controls_navbarleftlong_action", 1) > 1 || prefs.getInt("controls_navbarright_action", 1) > 1 || prefs.getInt("controls_navbarrightlong_action", 1) > 1`

### hasAnySystemUiStartupFeature_if_17
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 18-18
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_scramblepin']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_scramblepin")`

### hasAnySystemUiStartupFeature_if_18
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 19-19
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_dttosleep']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_dttosleep")`

### hasAnySystemUiStartupFeature_if_19
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 20-20
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_noscreenlock_act']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_noscreenlock_act")`

### hasAnySystemUiStartupFeature_if_20
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 21-21
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_detailednetspeed', 'system_detailednetspeed_fakedualrow']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['NOT', 'AND']
- raw_expression: `prefs.getBoolean("system_detailednetspeed") && !prefs.getBoolean("system_detailednetspeed_fakedualrow")`

### hasAnySystemUiStartupFeature_if_21
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 22-22
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_albumartonlock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_albumartonlock")`

### hasAnySystemUiStartupFeature_if_22
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 23-23
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_expandheadups']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_expandheadups", 1) > 1`

### hasAnySystemUiStartupFeature_if_23
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 24-24
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_nohide']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_betterpopups_nohide")`

### hasAnySystemUiStartupFeature_if_24
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 25-25
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_swipedown']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_betterpopups_swipedown")`

### hasAnySystemUiStartupFeature_if_25
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 26-26
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_center']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_betterpopups_center")`

### hasAnySystemUiStartupFeature_if_26
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 27-27
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_notifafterunlock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_notifafterunlock")`

### hasAnySystemUiStartupFeature_if_27
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 28-28
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_notifrowmenu']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_notifrowmenu")`

### hasAnySystemUiStartupFeature_if_28
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 29-29
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_compactnotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_compactnotif")`

### hasAnySystemUiStartupFeature_if_29
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 30-30
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_removedismiss']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_removedismiss")`

### hasAnySystemUiStartupFeature_if_30
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 31-31
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_drawer_removeshortcut']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_drawer_removeshortcut")`

### hasAnySystemUiStartupFeature_if_31
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 32-32
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['controls_nonavbar']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("controls_nonavbar")`

### hasAnySystemUiStartupFeature_if_32
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 33-33
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['controls_hidenavbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("controls_hidenavbar_whenscreenshot")`

### hasAnySystemUiStartupFeature_if_33
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 34-34
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['controls_imebackalticon']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("controls_imebackalticon")`

### hasAnySystemUiStartupFeature_if_34
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 35-35
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_visualizer']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_visualizer")`

### hasAnySystemUiStartupFeature_if_35
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 36-36
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_nosilentvibrate', 'system_qs_force_systemfonts', 'system_volumetimer', 'system_qsnolabels', 'system_cc_volume_showpct', 'system_volumebar_blur_mtk', 'system_cc_hidedate', 'system_cc_hide_shortcuticons', 'system_cc_clocktweak', 'system_cc_tile_roundedrect', 'system_cc_bluetooth_tile_style', 'system_separatevolume', 'system_separatevolume_slider', 'system_volumedialogdelay_collapsed', 'system_volumedialogdelay_expanded', 'system_volumeblur_collapsed', 'system_volumeblur_expanded']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getStringAsInt', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getInt']
- default_values: [None, None, None, None, None, None, None, None, None, None, 1, None, None, 0, 0, 0, 0]
- comparators: ['>', '>', '>', '>', '>']
- boolean_operators: ['AND', 'OR']
- raw_expression: `prefs.getBoolean("system_nosilentvibrate") || prefs.getBoolean("system_qs_force_systemfonts") || prefs.getBoolean("system_volumetimer") || prefs.getBoolean("system_qsnolabels") || prefs.getBoolean("system_cc_volume_showpct") || prefs.getBoolean("system_volumebar_blur_mtk") || prefs.getBoolean("system_cc_hidedate") || prefs.getBoolean("system_cc_hide_shortcuticons") || prefs.getBoolean("system_cc_clocktweak") || prefs.getBoolean("system_cc_tile_roundedrect") || prefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1 || (prefs.getBoolean("system_separatevolume") && prefs.getBoolean("system_separatevolume_slider")) || (prefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || prefs.getInt("system_volumedialogdelay_expanded", 0) > 0) || (prefs.getInt("system_volumeblur_collapsed", 0) > 0 || prefs.getInt("system_volumeblur_expanded", 0) > 0)`

### hasAnySystemUiStartupFeature_if_36
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 37-37
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_disableanynotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_disableanynotif")`

### hasAnySystemUiStartupFeature_if_37
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 38-38
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_lockscreenshortcuts']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_lockscreenshortcuts")`

### hasAnySystemUiStartupFeature_if_38
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 39-39
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_4gtolte', 'system_statusbar_mobiletype_single', 'system_statusbar_mobile_showname']
- accessors: ['getBoolean', 'getBoolean', 'getString']
- default_values: [None, None, '']
- boolean_operators: ['NOT', 'AND', 'OR']
- raw_expression: `prefs.getBoolean("system_4gtolte") || (prefs.getBoolean("system_statusbar_mobiletype_single") && !prefs.getString("system_statusbar_mobile_showname", "").equals(""))`

### hasAnySystemUiStartupFeature_if_39
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 40-40
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_netspeed_atright', 'system_statusbar_alarm_atright', 'system_statusbar_sound_atright', 'system_statusbar_dnd_atright', 'system_statusbar_nfc_atright', 'system_statusbar_btbattery_atright', 'system_statusbar_headset_atright', 'system_statusbar_vpn_atright', 'system_statusbar_alarm_atleft', 'system_statusbar_sound_atleft', 'system_statusbar_dnd_atleft', 'system_statusbar_gps_atleft', 'system_statusbar_netspeed_atleft', 'system_statusbar_dualrows', 'system_statusbar_netspeed_atsecondrow', 'system_statusbaricons_wifi_mobile_atleft', 'system_statusbaricons_swap_wifi_mobile']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None]
- boolean_operators: ['AND', 'OR']
- raw_expression: `(prefs.getBoolean("system_statusbar_netspeed_atright") || prefs.getBoolean("system_statusbar_alarm_atright") || prefs.getBoolean("system_statusbar_sound_atright") || prefs.getBoolean("system_statusbar_dnd_atright") || prefs.getBoolean("system_statusbar_nfc_atright") || prefs.getBoolean("system_statusbar_btbattery_atright") || prefs.getBoolean("system_statusbar_headset_atright") || prefs.getBoolean("system_statusbar_vpn_atright")) || (prefs.getBoolean("system_statusbar_alarm_atleft") || prefs.getBoolean("system_statusbar_sound_atleft") || prefs.getBoolean("system_statusbar_dnd_atleft") || prefs.getBoolean("system_statusbar_gps_atleft")) || prefs.getBoolean("system_statusbar_netspeed_atleft") || (prefs.getBoolean("system_statusbar_dualrows") && prefs.getBoolean("system_statusbar_netspeed_atsecondrow")) || prefs.getBoolean("system_statusbaricons_wifi_mobile_atleft") || prefs.getBoolean("system_statusbaricons_swap_wifi_mobile")`

### hasAnySystemUiStartupFeature_if_40
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 41-41
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_clock_position', 'system_statusbar_dualrows']
- accessors: ['getStringAsInt', 'getBoolean']
- default_values: [1, None]
- comparators: ['>']
- boolean_operators: ['NOT', 'AND']
- raw_expression: `prefs.getStringAsInt("system_statusbar_clock_position", 1) > 1 && !prefs.getBoolean("system_statusbar_dualrows")`

### hasAnySystemUiStartupFeature_if_41
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 42-42
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_batterystyle']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbar_batterystyle")`

### hasAnySystemUiStartupFeature_if_42
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 43-43
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_batterytempandcurrent', 'system_statusbar_showdevicetemperature']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_statusbar_batterytempandcurrent") || prefs.getBoolean("system_statusbar_showdevicetemperature")`

### hasAnySystemUiStartupFeature_if_43
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 44-44
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_topmargin', 'system_statusbar_topmargin_unset_lockscreen']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['AND']
- raw_expression: `prefs.getBoolean("system_statusbar_topmargin") && prefs.getBoolean("system_statusbar_topmargin_unset_lockscreen")`

### hasAnySystemUiStartupFeature_if_44
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 45-45
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_horizmargin']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbar_horizmargin")`

### hasAnySystemUiStartupFeature_if_45
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 46-46
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_showpct']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_showpct")`

### hasAnySystemUiStartupFeature_if_46
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 47-47
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_hidelsstatusbar']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_hidelsstatusbar")`

### hasAnySystemUiStartupFeature_if_47
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 48-48
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_hidelsclock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_hidelsclock")`

### hasAnySystemUiStartupFeature_if_48
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 49-49
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_ls_force_systemfonts']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_ls_force_systemfonts")`

### hasAnySystemUiStartupFeature_if_49
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 50-50
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_hidelshint']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_hidelshint")`

### hasAnySystemUiStartupFeature_if_50
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 51-51
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_allowdirectreply']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_allowdirectreply")`

### hasAnySystemUiStartupFeature_if_51
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 52-52
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_allownotifonkeyguard']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_allownotifonkeyguard")`

### hasAnySystemUiStartupFeature_if_52
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 53-53
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_allownotiffloat']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_allownotiffloat")`

### hasAnySystemUiStartupFeature_if_53
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 54-54
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_hideqs']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_hideqs")`

### hasAnySystemUiStartupFeature_if_54
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 55-55
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_lsalarm']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_lsalarm")`

### hasAnySystemUiStartupFeature_if_55
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 56-56
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbarcontrols']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbarcontrols")`

### hasAnySystemUiStartupFeature_if_56
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 57-57
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_nonetspeedseparator']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_nonetspeedseparator")`

### hasAnySystemUiStartupFeature_if_57
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 58-58
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_clock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbaricons_clock")`

### hasAnySystemUiStartupFeature_if_58
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 59-59
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_detailednetspeed_fakedualrow', 'system_detailednetspeed', 'system_detailednetspeed_secunit', 'system_detailednetspeed_low']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None]
- boolean_operators: ['NOT', 'AND', 'OR']
- raw_expression: `prefs.getBoolean("system_detailednetspeed_fakedualrow") || (!prefs.getBoolean("system_detailednetspeed") && (prefs.getBoolean("system_detailednetspeed_secunit") || prefs.getBoolean("system_detailednetspeed_low") ) )`

### hasAnySystemUiStartupFeature_if_59
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 60-60
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_netspeed_fontsize', 'system_netspeed_verticaloffset', 'system_detailednetspeed', 'system_detailednetspeed_fakedualrow', 'system_netspeed_bold', 'system_netspeed_leftmargin', 'system_netspeed_fixedcontent_width', 'system_netspeed_rightmargin', 'system_detailednetspeed_align']
- accessors: ['getInt', 'getInt', 'getBoolean', 'getBoolean', 'getBoolean', 'getInt', 'getInt', 'getInt', 'getStringAsInt']
- default_values: [13, 8, None, None, None, 0, 10, 0, 1]
- comparators: ['>', '!=', '>', '>', '>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("system_netspeed_fontsize", 13) > 13 || prefs.getInt("system_netspeed_verticaloffset", 8) != 8 || prefs.getBoolean("system_detailednetspeed") || prefs.getBoolean("system_detailednetspeed_fakedualrow") || prefs.getBoolean("system_netspeed_bold") || prefs.getInt("system_netspeed_leftmargin", 0) > 0 || prefs.getInt("system_netspeed_fixedcontent_width", 10) > 10 || prefs.getInt("system_netspeed_rightmargin", 0) > 0 || prefs.getStringAsInt("system_detailednetspeed_align", 1) > 1`

### hasAnySystemUiStartupFeature_if_60
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 61-61
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_taptounlock']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_taptounlock")`

### hasAnySystemUiStartupFeature_if_61
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 62-62
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_nosos']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_nosos")`

### hasAnySystemUiStartupFeature_if_62
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 63-63
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_morenotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_morenotif")`

### hasAnySystemUiStartupFeature_if_63
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 64-64
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_charginginfo']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_charginginfo")`

### hasAnySystemUiStartupFeature_if_64
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 65-65
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_secureqs']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_secureqs")`

### hasAnySystemUiStartupFeature_if_65
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 66-66
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_mutevisiblenotif']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_mutevisiblenotif")`

### hasAnySystemUiStartupFeature_if_66
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 67-67
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_battery1']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbaricons_battery1")`

### hasAnySystemUiStartupFeature_if_67
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 68-68
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_battery3', 'system_statusbaricons_battery4', 'system_statusbaricons_battery2']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None]
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_statusbaricons_battery3") || prefs.getBoolean("system_statusbaricons_battery4") || prefs.getBoolean("system_statusbaricons_battery2")`

### hasAnySystemUiStartupFeature_if_68
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 69-69
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_wifistandard']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_statusbaricons_wifistandard", 1) > 1`

### hasAnySystemUiStartupFeature_if_69
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 70-70
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_signal', 'system_statusbaricons_sim1', 'system_statusbaricons_sim2', 'system_statusbaricons_sim_nodata', 'system_statusbaricons_roaming', 'system_statusbaricons_volte']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None, None, None]
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_statusbaricons_signal") || prefs.getBoolean("system_statusbaricons_sim1") || prefs.getBoolean("system_statusbaricons_sim2") || prefs.getBoolean("system_statusbaricons_sim_nodata") || prefs.getBoolean("system_statusbaricons_roaming") || prefs.getBoolean("system_statusbaricons_volte")`

### hasAnySystemUiStartupFeature_if_70
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 71-71
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_vowifi']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbaricons_vowifi")`

### hasAnySystemUiStartupFeature_if_71
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 72-72
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_alarm', 'system_statusbaricons_alarmn']
- accessors: ['getBoolean', 'getInt']
- default_values: [None, 0]
- comparators: ['>']
- boolean_operators: ['NOT', 'AND']
- raw_expression: `!prefs.getBoolean("system_statusbaricons_alarm") && prefs.getInt("system_statusbaricons_alarmn", 0) > 0`

### hasAnySystemUiStartupFeature_if_72
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 73-73
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_shortcut_app', 'system_calendar_app', 'system_clock_app']
- accessors: ['getString', 'getString', 'getString']
- default_values: ['', '', '']
- boolean_operators: ['NOT', 'OR']
- raw_expression: `!prefs.getString("system_shortcut_app", "").equals("") || !prefs.getString("system_calendar_app", "").equals("") || !prefs.getString("system_clock_app", "").equals("")`

### hasAnySystemUiStartupFeature_if_73
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 74-74
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_qshaptics']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_qshaptics", 1) > 1`

### hasAnySystemUiStartupFeature_if_74
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 75-75
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_qs_hideoperator']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_qs_hideoperator")`

### hasAnySystemUiStartupFeature_if_75
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 76-76
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_hideoperator_delimiter']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_cc_hideoperator_delimiter")`

### hasAnySystemUiStartupFeature_if_76
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 77-77
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_show_stepcount', 'system_drawer_show_stepcount']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_cc_show_stepcount") || prefs.getBoolean("system_drawer_show_stepcount")`

### hasAnySystemUiStartupFeature_if_77
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 78-78
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_disable_bluetooth_restrict']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_cc_disable_bluetooth_restrict")`

### hasAnySystemUiStartupFeature_if_78
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 79-79
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_collapse_after_clicked']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_cc_collapse_after_clicked")`

### hasAnySystemUiStartupFeature_if_79
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 80-80
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_switch_qsandnotification']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_cc_switch_qsandnotification")`

### hasAnySystemUiStartupFeature_if_80
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 81-81
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_expandnotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_expandnotifs", 1) > 1`

### hasAnySystemUiStartupFeature_if_81
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 82-82
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_inactivebrightness']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_inactivebrightness", 1) > 1`

### hasAnySystemUiStartupFeature_if_82
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 83-83
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_mobiletypeicon', 'system_networkindicator_mobile', 'system_statusbar_mobiletype_show_wificonnected']
- accessors: ['getStringAsInt', 'getBoolean', 'getBoolean']
- default_values: [1, None, None]
- comparators: ['>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getStringAsInt("system_mobiletypeicon", 1) > 1 || prefs.getBoolean("system_networkindicator_mobile") || prefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")`

### hasAnySystemUiStartupFeature_if_83
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 84-84
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_bluetooth']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_statusbaricons_bluetooth", 1) > 1`

### hasAnySystemUiStartupFeature_if_84
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 85-85
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_epm']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_epm")`

### hasAnySystemUiStartupFeature_if_85
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 86-86
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_wifi', 'system_statusbaricons_dualwifi', 'system_statusbaricons_alarm', 'system_statusbaricons_profile', 'system_statusbaricons_sound', 'system_statusbaricons_dnd', 'system_statusbaricons_secondspace', 'system_statusbaricons_headset', 'system_statusbaricons_nfc', 'system_statusbaricons_vpn', 'system_statusbaricons_airplane', 'system_statusbaricons_hotspot', 'system_statusbaricons_nosims', 'system_statusbaricons_gps', 'system_statusbaricons_btbattery', 'system_statusbaricons_ble_unlock', 'system_statusbaricons_volte']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None]
- boolean_operators: ['OR']
- raw_expression: `(prefs.getBoolean("system_statusbaricons_wifi") || prefs.getBoolean("system_statusbaricons_dualwifi") || prefs.getBoolean("system_statusbaricons_alarm") || prefs.getBoolean("system_statusbaricons_profile") || prefs.getBoolean("system_statusbaricons_sound") || prefs.getBoolean("system_statusbaricons_dnd") || prefs.getBoolean("system_statusbaricons_secondspace") || prefs.getBoolean("system_statusbaricons_headset") || prefs.getBoolean("system_statusbaricons_nfc") || prefs.getBoolean("system_statusbaricons_vpn") || prefs.getBoolean("system_statusbaricons_airplane") || prefs.getBoolean("system_statusbaricons_hotspot") || prefs.getBoolean("system_statusbaricons_nosims") || prefs.getBoolean("system_statusbaricons_gps") || prefs.getBoolean("system_statusbaricons_btbattery") || prefs.getBoolean("system_statusbaricons_ble_unlock") || prefs.getBoolean("system_statusbaricons_volte"))`

### hasAnySystemUiStartupFeature_if_86
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 87-87
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_privacy', 'system_statusbaricons_mute', 'system_statusbaricons_speaker', 'system_statusbaricons_record']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None, None]
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_statusbaricons_privacy") || prefs.getBoolean("system_statusbaricons_mute") || prefs.getBoolean("system_statusbaricons_speaker") || prefs.getBoolean("system_statusbaricons_record")`

### hasAnySystemUiStartupFeature_if_87
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 88-88
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_messagingstylelines']
- accessors: ['getInt']
- default_values: [0]
- comparators: ['>']
- raw_expression: `prefs.getInt("system_messagingstylelines", 0) > 0`

### hasAnySystemUiStartupFeature_if_88
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 89-89
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_allowfloat']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_betterpopups_allowfloat")`

### hasAnySystemUiStartupFeature_if_89
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 90-90
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_autoclose_expanded']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_betterpopups_autoclose_expanded")`

### hasAnySystemUiStartupFeature_if_90
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 91-91
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_betterpopups_disablewhenmute']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_betterpopups_disablewhenmute")`

### hasAnySystemUiStartupFeature_if_91
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 92-92
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_securecontrolcenter']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_securecontrolcenter")`

### hasAnySystemUiStartupFeature_if_92
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 93-93
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_minimalnotifview']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_minimalnotifview")`

### hasAnySystemUiStartupFeature_if_93
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 94-94
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_notifchannelsettings']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_notifchannelsettings")`

### hasAnySystemUiStartupFeature_if_94
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 95-95
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_maxsbicons']
- accessors: ['getStringAsInt']
- default_values: [0]
- comparators: ['!=']
- raw_expression: `prefs.getStringAsInt("system_maxsbicons", 0) != 0`

### hasAnySystemUiStartupFeature_if_95
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 96-96
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_mobiletype_single']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbar_mobiletype_single")`

### hasAnySystemUiStartupFeature_if_96
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 97-97
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_dualsimin2rows']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbar_dualsimin2rows")`

### hasAnySystemUiStartupFeature_if_97
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 98-98
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_dualrows']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbar_dualrows")`

### hasAnySystemUiStartupFeature_if_98
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 99-99
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_ccgridcolumns', 'system_ccgridrows']
- accessors: ['getInt', 'getInt']
- default_values: [4, 4]
- comparators: ['>', '!=']
- boolean_operators: ['OR']
- raw_expression: `prefs.getInt("system_ccgridcolumns", 4) > 4 || prefs.getInt("system_ccgridrows", 4) != 4`

### hasAnySystemUiStartupFeature_if_99
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 100-100
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_colorizenotifs']
- accessors: ['getStringAsInt']
- default_values: [1]
- comparators: ['>']
- raw_expression: `prefs.getStringAsInt("system_colorizenotifs", 1) > 1`

### hasAnySystemUiStartupFeature_if_100
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 101-101
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_notify_openinfw']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_notify_openinfw")`

### hasAnySystemUiStartupFeature_if_101
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 102-102
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_fw_noblacklist']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_fw_noblacklist")`

### hasAnySystemUiStartupFeature_if_102
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 103-103
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_notify_openinfw', 'system_notifrowmenu', 'system_betterpopups_allowfloat']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean']
- default_values: [None, None, None]
- boolean_operators: ['OR']
- raw_expression: `prefs.getBoolean("system_notify_openinfw") || prefs.getBoolean("system_notifrowmenu") || prefs.getBoolean("system_betterpopups_allowfloat")`

### hasAnySystemUiStartupFeature_if_103
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 104-104
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_nosafevolume']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_nosafevolume")`

### hasAnySystemUiStartupFeature_if_104
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 105-105
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_lockscreen_hidezenmode']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_lockscreen_hidezenmode")`

### hasAnySystemUiStartupFeature_if_105
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 106-106
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_nopassword']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_nopassword")`

### hasAnySystemUiStartupFeature_if_106
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 107-107
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_topmargin']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_statusbar_topmargin")`

### hasAnySystemUiStartupFeature_if_107
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 108-108
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_enable_style_switch']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_cc_enable_style_switch")`

### hasAnySystemUiStartupFeature_if_108
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 109-109
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_qs_force_systemfonts']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_qs_force_systemfonts")`

### hasAnySystemUiStartupFeature_if_109
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 110-110
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_detailednetspeed_fakedualrow']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_detailednetspeed_fakedualrow")`

### hasAnySystemUiStartupFeature_if_110
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 111-111
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_volumetimer']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_volumetimer")`

### hasAnySystemUiStartupFeature_if_111
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 112-112
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_tile_roundedrect']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_cc_tile_roundedrect")`

### hasAnySystemUiStartupFeature_if_112
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 113-113
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_iconsize']
- accessors: ['getInt']
- default_values: [6]
- comparators: ['>']
- raw_expression: `(prefs.getInt("system_statusbar_iconsize", 6)) > 6`

### hasAnySystemUiStartupFeature_if_113
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 114-114
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_cc_show_stepcount']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_cc_show_stepcount")`

### hasAnySystemUiStartupFeature_if_114
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 115-115
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbaricons_swap_wifi_mobile', 'system_statusbaricons_wifi_mobile_atleft']
- accessors: ['getBoolean', 'getBoolean']
- default_values: [None, None]
- boolean_operators: ['OR']
- raw_expression: `(prefs.getBoolean("system_statusbaricons_swap_wifi_mobile")) || (prefs.getBoolean("system_statusbaricons_wifi_mobile_atleft"))`

### hasAnySystemUiStartupFeature_if_115
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 116-116
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_screenshot_overlay']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_screenshot_overlay")`

### hasAnySystemUiStartupFeature_if_116
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 117-117
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_hidestatusbar_whenscreenshot']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_hidestatusbar_whenscreenshot")`

### hasAnySystemUiStartupFeature_if_117
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 118-121
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_statusbar_clocktweak', 'system_cc_clocktweak', 'system_cc_hidedate', 'system_cc_dateformat']
- accessors: ['getBoolean', 'getBoolean', 'getBoolean', 'getString']
- default_values: [None, None, None, '']
- boolean_operators: ['NOT', 'OR', 'isEmpty']
- raw_expression: `prefs.getBoolean("system_statusbar_clocktweak") ||
                prefs.getBoolean("system_cc_clocktweak") ||
                prefs.getBoolean("system_cc_hidedate") ||
                !prefs.getString("system_cc_dateformat", "").isEmpty()`

### hasAnySystemUiStartupFeature_if_118
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 122-122
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_hidemoreicon']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_hidemoreicon")`

### hasAnySystemUiStartupFeature_if_119
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnySystemUiStartupFeature`
- lines: 123-123
- phase: `STARTUP_GATE`
- parse_status: `PARSED`
- preference_keys: ['system_batteryindicator']
- accessors: ['getBoolean']
- default_values: [None]
- raw_expression: `prefs.getBoolean("system_batteryindicator")`

## GLOBAL_ACTION_DOMAIN_RULES (2)

### hasAnyGlobalAction_if_1
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnyGlobalAction`
- lines: 5-5
- phase: `GLOBAL_ACTION_DYNAMIC_SCAN`
- parse_status: `PARSED`
- comparators: ['>']
- boolean_operators: ['AND']
- raw_expression: `isSystemUiGlobalActionKey(key) && value instanceof Integer && (Integer) value > 1`

### hasAnyGlobalAction_if_2
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `hasAnyGlobalAction`
- lines: 9-9
- phase: `GLOBAL_ACTION_DYNAMIC_SCAN`
- parse_status: `PARSED`
- preference_keys: ['controls_volumemedia_up', 'controls_volumemedia_down']
- accessors: ['getStringAsInt', 'getStringAsInt']
- default_values: [0, 0]
- comparators: ['>', '>']
- boolean_operators: ['OR']
- raw_expression: `prefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || prefs.getStringAsInt("controls_volumemedia_down", 0) > 0`

## FEATURE_DISPATCH_CALLS (13)

### install_installById_1
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 38-38
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `tempHideOverlaySystemUI`
- raw_expression: `FeatureDispatcher.installById("tempHideOverlaySystemUI", systemuiRuntime)`

### install_installById_2
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 47-47
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `hideStatusBarBeforeScreenshot`
- raw_expression: `FeatureDispatcher.installById("hideStatusBarBeforeScreenshot", systemuiRuntime)`

### install_installById_3
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 51-51
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `networkIndicatorWifi`
- raw_expression: `FeatureDispatcher.installById("networkIndicatorWifi", systemuiRuntime)`

### install_installById_4
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 68-68
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `statusBarClockTweak`
- raw_expression: `FeatureDispatcher.installById("statusBarClockTweak", systemuiRuntime)`

### install_installById_5
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 79-79
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `noMoreIcon`
- raw_expression: `FeatureDispatcher.installById("noMoreIcon", systemuiRuntime)`

### install_installById_6
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 83-83
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `hideDismissView`
- raw_expression: `FeatureDispatcher.installById("hideDismissView", systemuiRuntime)`

### install_installById_7
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 86-86
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `hideNavBarBeforeScreenshot`
- raw_expression: `FeatureDispatcher.installById("hideNavBarBeforeScreenshot", systemuiRuntime)`

### install_installById_8
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 106-106
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `batteryIndicator`
- raw_expression: `FeatureDispatcher.installById("batteryIndicator", systemuiRuntime)`

### install_installById_9
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 148-148
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `hideLockScreenHint`
- raw_expression: `FeatureDispatcher.installById("hideLockScreenHint", systemuiRuntime)`

### install_installById_10
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 155-155
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `noNetworkSpeedSeparator`
- raw_expression: `FeatureDispatcher.installById("noNetworkSpeedSeparator", systemuiRuntime)`

### install_installById_11
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 156-156
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `hideIconsClock`
- raw_expression: `FeatureDispatcher.installById("hideIconsClock", systemuiRuntime)`

### install_installById_12
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 182-182
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `chargingInfo`
- raw_expression: `FeatureDispatcher.installById("chargingInfo", systemuiRuntime)`

### install_installById_13
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `install`
- lines: 184-184
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `PARSED`
- feature_id: `muteVisibleNotifications`
- raw_expression: `FeatureDispatcher.installById("muteVisibleNotifications", systemuiRuntime)`

## FEATURE_CATALOG_GATES (13)

### FeatureCatalog_statusBarClockTweak
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 95-162
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `statusBarClockTweak`
- raw_expression: `FeatureSpec(
            id = "statusBarClockTweak",
            diagnosticId = DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf(
                "system_statusbar_clocktweak",
                "system_cc_clocktweak",
                "system_cc_hidedate",
                "system_cc_dateformat"
            ),
            condition = { prefs ->
                prefs.getBoolean("system_statusbar_clocktweak") ||
                prefs.getBoolean("system_cc_clocktweak") ||
                prefs.getBoolean("system_cc_hidedate") ||
                prefs.getString("system_cc_dateformat", "").isNotEmpty()
            },
            compatibilityPolicy = CompatibilityPolicy.CUSTOM,
            compatibilityCheck = { runtime ->
                val contract = statusBarClockTweakContract(runtime.prefs)
                val (compat, result) = runtime.resolver.evaluateContract(
                    contract,
                    DiagnosticIds.STATUSBAR_CLOCK_TWEAK
                )
                CompatibilityResult(
                    compat,
                    result.reasonCode,
                    result.detail,
                    result.copy(resolvedContract = contract)
                )
            },
            installer = { runtime, compatResult ->
                val contract = compatResult.resolvedContract
                    ?: statusBarClockTweakContract(runtime.prefs)
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = contract,
                    diagnosticId = DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "statusBarClockTweak session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.PARTIAL
        )`

### FeatureCatalog_noMoreIcon
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 270-315
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `noMoreIcon`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CanaryContracts.noMoreIcon,
            id = "noMoreIcon",
            diagnosticId = DiagnosticIds.NO_MORE_ICON,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidemoreicon"),
            condition = { prefs ->
                prefs.getBoolean("system_hidemoreicon", false)
            },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.noMoreIcon,
                    diagnosticId = DiagnosticIds.NO_MORE_ICON,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    SystemNotificationMoreHooks.NoMoreIconHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "noMoreIcon session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_batteryIndicator
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 316-361
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `batteryIndicator`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CanaryContracts.batteryIndicator,
            id = "batteryIndicator",
            diagnosticId = DiagnosticIds.BATTERY_INDICATOR,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_batteryindicator"),
            condition = { prefs ->
                prefs.getBoolean("system_batteryindicator", false)
            },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.batteryIndicator,
                    diagnosticId = DiagnosticIds.BATTERY_INDICATOR,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    SystemUIBatteryHooks.BatteryIndicatorHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "batteryIndicator session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_networkIndicatorWifi
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 568-594
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `networkIndicatorWifi`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.networkIndicatorWifi,
            id = "networkIndicatorWifi",
            diagnosticId = DiagnosticIds.NETWORK_INDICATOR_WIFI,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_networkindicator_wifi"),
            condition = { prefs ->
                prefs.getBoolean("system_networkindicator_wifi", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.networkIndicatorWifi,
                    diagnosticId = DiagnosticIds.NETWORK_INDICATOR_WIFI
                ) {
                    SystemStatusBarMoreHooks.NetworkIndicatorWifi(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_muteVisibleNotifications
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 595-621
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `muteVisibleNotifications`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.muteVisibleNotifications,
            id = "muteVisibleNotifications",
            diagnosticId = DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_mutevisiblenotif"),
            condition = { prefs ->
                prefs.getBoolean("system_mutevisiblenotif", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.muteVisibleNotifications,
                    diagnosticId = DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS
                ) {
                    SystemNotificationMoreHooks.MuteVisibleNotificationsHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_hideDismissView
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 733-759
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `hideDismissView`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideDismissView,
            id = "hideDismissView",
            diagnosticId = DiagnosticIds.HIDE_DISMISS_VIEW,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_removedismiss"),
            condition = { prefs ->
                prefs.getBoolean("system_removedismiss", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideDismissView,
                    diagnosticId = DiagnosticIds.HIDE_DISMISS_VIEW
                ) {
                    SystemUINotificationHooks.HideDismissViewHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_hideLockScreenHint
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 760-786
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `hideLockScreenHint`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideLockScreenHint,
            id = "hideLockScreenHint",
            diagnosticId = DiagnosticIds.HIDE_LOCK_SCREEN_HINT,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidelshint"),
            condition = { prefs ->
                prefs.getBoolean("system_hidelshint", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideLockScreenHint,
                    diagnosticId = DiagnosticIds.HIDE_LOCK_SCREEN_HINT
                ) {
                    SystemLockScreenMoreHooks.HideLockScreenHintHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_noNetworkSpeedSeparator
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 898-924
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `noNetworkSpeedSeparator`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noNetworkSpeedSeparator,
            id = "noNetworkSpeedSeparator",
            diagnosticId = DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_nonetspeedseparator"),
            condition = { prefs ->
                prefs.getBoolean("system_nonetspeedseparator", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noNetworkSpeedSeparator,
                    diagnosticId = DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR
                ) {
                    SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_hideIconsClock
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 925-951
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `hideIconsClock`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideIconsClock,
            id = "hideIconsClock",
            diagnosticId = DiagnosticIds.HIDE_ICONS_CLOCK,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_statusbaricons_clock"),
            condition = { prefs ->
                prefs.getBoolean("system_statusbaricons_clock", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideIconsClock,
                    diagnosticId = DiagnosticIds.HIDE_ICONS_CLOCK
                ) {
                    SystemUIStatusBarHooks.HideIconsClockHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_tempHideOverlaySystemUI
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 981-1007
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `tempHideOverlaySystemUI`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.tempHideOverlaySystemUI,
            id = "tempHideOverlaySystemUI",
            diagnosticId = DiagnosticIds.TEMP_HIDE_OVERLAY_SYSTEMUI,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_screenshot_overlay"),
            condition = { prefs ->
                prefs.getBoolean("system_screenshot_overlay", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.tempHideOverlaySystemUI,
                    diagnosticId = DiagnosticIds.TEMP_HIDE_OVERLAY_SYSTEMUI
                ) {
                    SystemUIScreenshotHooks.TempHideOverlaySystemUIHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_hideStatusBarBeforeScreenshot
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 1008-1034
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `hideStatusBarBeforeScreenshot`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideStatusBarBeforeScreenshot,
            id = "hideStatusBarBeforeScreenshot",
            diagnosticId = DiagnosticIds.HIDE_STATUS_BAR_BEFORE_SCREENSHOT,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidestatusbar_whenscreenshot"),
            condition = { prefs ->
                prefs.getBoolean("system_hidestatusbar_whenscreenshot", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideStatusBarBeforeScreenshot,
                    diagnosticId = DiagnosticIds.HIDE_STATUS_BAR_BEFORE_SCREENSHOT
                ) {
                    SystemUIScreenshotHooks.HideStatusBarBeforeScreenshotHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_hideNavBarBeforeScreenshot
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 1035-1061
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `hideNavBarBeforeScreenshot`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideNavBarBeforeScreenshot,
            id = "hideNavBarBeforeScreenshot",
            diagnosticId = DiagnosticIds.HIDE_NAV_BAR_BEFORE_SCREENSHOT,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("controls_hidenavbar_whenscreenshot"),
            condition = { prefs ->
                prefs.getBoolean("controls_hidenavbar_whenscreenshot", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideNavBarBeforeScreenshot,
                    diagnosticId = DiagnosticIds.HIDE_NAV_BAR_BEFORE_SCREENSHOT
                ) {
                    SystemUIScreenshotHooks.HideNavBarBeforeScreenshotHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

### FeatureCatalog_chargingInfo
- source_file: `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`
- source_method: `FeatureCatalog`
- lines: 1172-1205
- phase: `FEATURE_DISPATCHER_INTERNAL_GATE`
- parse_status: `UNPARSED`
- feature_id: `chargingInfo`
- raw_expression: `FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.chargingInfo,
            id = "chargingInfo",
            diagnosticId = DiagnosticIds.CHARGING_INFO,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf(
                "system_charginginfo",
                "system_charginginfo_current",
                "system_charginginfo_voltage",
                "system_charginginfo_wattage",
                "system_charginginfo_temp",
                "system_charginginfo_view"
            ),
            condition = { prefs ->
                prefs.getBoolean("system_charginginfo", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.chargingInfo,
                    diagnosticId = DiagnosticIds.CHARGING_INFO
                ) {
                    SystemChargingAndWallpaperHooks.ChargingInfoHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )`

## RESOURCE_PHASE_CONDITIONS (0)

## RESTART_GUARD (1)

### isWithinSystemUiRestartGuard_return
- source_file: `C:\Users\tv\Downloads\Peengeek\customiuizer-a13-forDevin\app\src\main\java\tv\withaibuild\customiuizer\installers\SystemUiInstaller.java`
- source_method: `isWithinSystemUiRestartGuard`
- lines: 74-74
- phase: `RESTART_GUARD`
- parse_status: `PARSED`
- comparators: ['<']
- raw_expression: `currentTime - restartTime < 10000`

## UNPARSED (0)
