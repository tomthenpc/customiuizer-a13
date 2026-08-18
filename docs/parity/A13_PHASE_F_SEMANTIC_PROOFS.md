# A13 Phase F-R1 Semantic Proofs

Owner manifests used to promote rows to PRESENT_EQUIVALENT or PRESENT_A13_VARIANT.
Same-key reads alone are IMPLEMENTATION_PRESENCE and never sufficient.

## PROOF_ACTION_SLOT_controls_backlong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_backlong`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_backlong,controls_backlong_action`
- VALUE_DOMAIN: action picker; stored as controls_backlong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_FingerprintHapticFailureHook

- PROOF_ID: `PROOF_FP_Controls_kt_FingerprintHapticFailureHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `FingerprintHapticFailureHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.biometrics.sensors.AcquisitionClient#vibrateError`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `FingerprintHapticFailureHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.biometrics.sensors.AcquisitionClient#vibrateError`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_fingerprintfailure`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FingerprintHapticFailureHook` vs `FingerprintHapticFailureHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_NoFingerprintWakeHook

- PROOF_ID: `PROOF_FP_Controls_kt_NoFingerprintWakeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `NoFingerprintWakeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `NoFingerprintWakeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_fingerprintscreen`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoFingerprintWakeHook` vs `NoFingerprintWakeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_FingerprintHapticSuccessHook

- PROOF_ID: `PROOF_FP_Controls_kt_FingerprintHapticSuccessHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `FingerprintHapticSuccessHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `FingerprintHapticSuccessHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_fingerprintsuccess,controls_fingerprintsuccess_ignore`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FingerprintHapticSuccessHook` vs `FingerprintHapticSuccessHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_fsg_assist_left

- PROOF_ID: `PROOF_ACTION_SLOT_controls_fsg_assist_left`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_fsg_assist_left,controls_fsg_assist_left_action`
- VALUE_DOMAIN: action picker; stored as controls_fsg_assist_left_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_fsg_assist_right

- PROOF_ID: `PROOF_ACTION_SLOT_controls_fsg_assist_right`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_fsg_assist_right,controls_fsg_assist_right_action`
- VALUE_DOMAIN: action picker; stored as controls_fsg_assist_right_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_BackGestureAreaHeightHook

- PROOF_ID: `PROOF_FP_Controls_kt_BackGestureAreaHeightHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `BackGestureAreaHeightHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#getGestureStubWindowParam`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `BackGestureAreaHeightHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#getGestureStubWindowParam`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_fsg_coverage`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BackGestureAreaHeightHook` vs `BackGestureAreaHeightHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherGestureHooks_kt_FSGesturesHook

- PROOF_ID: `PROOF_FP_LauncherGestureHooks_kt_FSGesturesHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A14_SYMBOL: `FSGesturesHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#usingFsGesture,com.miui.launcher.utils.MiuiSettingsUtils#getGlobalBoolean,com.miui.home.recents.GestureStubView#onTouchEvent,#createAndAddNavStubView,#updateFsgWindowState`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A13_SYMBOL: `FSGesturesHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#usingFsGesture,com.miui.home.recents.BaseRecentsImpl#createAndAddNavStubView,com.miui.home.recents.BaseRecentsImpl#updateFsgWindowState,com.miui.launcher.utils.MiuiSettingsUtils#getGlobalBoolean,com.miui.home.recents.GestureStubView#onTouchEvent`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `controls_fsg_horiz,controls_fsg_horiz_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FSGesturesHook` vs `FSGesturesHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_fsg_swipeandstop

- PROOF_ID: `PROOF_ACTION_SLOT_controls_fsg_swipeandstop`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_fsg_swipeandstop,controls_fsg_swipeandstop_action`
- VALUE_DOMAIN: action picker; stored as controls_fsg_swipeandstop_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherGestureHooks_kt_SwipeAndStopActionHook

- PROOF_ID: `PROOF_FP_LauncherGestureHooks_kt_SwipeAndStopActionHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A14_SYMBOL: `SwipeAndStopActionHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.recents.GestureBackArrowView#setReadyFinish,com.miui.home.recents.GestureStubView\$3#onSwipeStop,com.miui.home.recents.GestureStubView#getNextTask,#disableQuickSwitch,#isDisableQuickSwitch`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A13_SYMBOL: `SwipeAndStopActionHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.miui.home.recents.GestureBackArrowView#setReadyFinish,com.miui.home.recents.GestureStubView#disableQuickSwitch,com.miui.home.recents.GestureStubView#isDisableQuickSwitch,com.miui.home.recents.GestureStubView#getNextTask,#vibrate`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `controls_fsg_swipeandstop_disablevibrate,controls_fsg_swipeandstop`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `SwipeAndStopActionHook` vs `SwipeAndStopActionHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_BackGestureAreaWidthHook

- PROOF_ID: `PROOF_FP_Controls_kt_BackGestureAreaWidthHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `BackGestureAreaWidthHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#initScreenSizeAndDensity,com.miui.home.recents.GestureStubView#setSize`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `BackGestureAreaWidthHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#initScreenSizeAndDensity,com.miui.home.recents.GestureStubView#setSize`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `controls_fsg_width`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BackGestureAreaWidthHook` vs `BackGestureAreaWidthHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_HIDE_IME_DISMISS

- PROOF_ID: `PROOF_HIDE_IME_DISMISS`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `HideImeDismissButtonHook`
- A14_INSTALLER: `mods/utils/feature/SystemUiFeatures.kt`
- A14_HOOK_TARGETS: `com.android.systemui.navigationbar.NavigationBarView#updateNavButtonIcons`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `HideImeDismissButtonHook`
- A13_INSTALLER: `installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.navigationbar.NavigationBarView#updateNavButtonIcons`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_hide_ime_dismiss_button`
- VALUE_DOMAIN: boolean
- DEFAULT_SEMANTICS: false keeps stock IME dismiss
- RESULT/ARGUMENT_BEHAVIOR: after updateNavButtonIcons, set IME back-alt visibility INVISIBLE when gestural
- API33_VARIANT_REASON: Same NavigationBarView member; A13 uses installer boolean vs A14 FeatureSpec.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AudioVisualizerHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AudioVisualizerHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `AudioVisualizerHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.shade.MiuiNotificationPanelViewController#onViewAttachedToWindow,com.android.systemui.statusbar.phone.CentralSurfacesImpl#start,com.android.systemui.statusbar.phone.CentralSurfacesImpl#updateDozingState,com.android.systemui.statusbar.policy.KeyguardStateControllerImpl#notifyKeyguardState,com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,#onScreenTurnedOff,#onScreenTurnedOn`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `AudioVisualizerHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOff,com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOn,com.android.systemui.statusbar.policy.KeyguardStateControllerImpl#notifyKeyguardState,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `controls_hidenavbar_whenscreenshot`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AudioVisualizerHook` vs `AudioVisualizerHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_homelong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_homelong`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_homelong,controls_homelong_action`
- VALUE_DOMAIN: action picker; stored as controls_homelong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_menulong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_menulong`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_menulong,controls_menulong_action`
- VALUE_DOMAIN: action picker; stored as controls_menulong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarleft

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarleft`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarleft,controls_navbarleft_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarleft_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarleftlong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarleftlong`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarleftlong,controls_navbarleftlong_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarleftlong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_reposNavBarButtons

- PROOF_ID: `PROOF_FP_Controls_kt_reposNavBarButtons`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `reposNavBarButtons`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `reposNavBarButtons`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `controls_navbarmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `reposNavBarButtons` vs `reposNavBarButtons`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarright

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarright`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarright,controls_navbarright_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarright_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarrightlong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarrightlong`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarrightlong,controls_navbarrightlong_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarrightlong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_FixInputMethodBottomMarginHook

- PROOF_ID: `PROOF_FP_Various_kt_FixInputMethodBottomMarginHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `FixInputMethodBottomMarginHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#addMiuiBottomView,#updateGestureLineEnable`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `FixInputMethodBottomMarginHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java`
- A13_HOOK_TARGETS: `#addMiuiBottomView,#updateGestureLineEnable`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `controls_nonavbar_fix_inputmethod`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FixInputMethodBottomMarginHook` vs `FixInputMethodBottomMarginHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_powerdt

- PROOF_ID: `PROOF_ACTION_SLOT_controls_powerdt`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_powerdt,controls_powerdt_action`
- VALUE_DOMAIN: action picker; stored as controls_powerdt_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_PowerKeyHook

- PROOF_ID: `PROOF_FP_Controls_kt_PowerKeyHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `PowerKeyHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `PowerKeyHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A13_CALLBACK_PHASE: `after,before,intercept`
- PREFERENCE_KEYS: `controls_powerflash_delay`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `PowerKeyHook` vs `PowerKeyHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIMonitorAndTileHooks_kt_AddCustomTileHook

- PROOF_ID: `PROOF_FP_SystemUIMonitorAndTileHooks_kt_AddCustomTileHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt`
- A14_SYMBOL: `AddCustomTileHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.qs.tileimpl.MiuiQSFactory#createTile`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt`
- A13_SYMBOL: `AddCustomTileHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `controls_volumecursor,system_cc_fpstile,system_fivegtile`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AddCustomTileHook` vs `AddCustomTileHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_VolumeCursorHook

- PROOF_ID: `PROOF_FP_Controls_kt_VolumeCursorHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `VolumeCursorHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.inputmethodservice.InputMethodService#onKeyDown,android.inputmethodservice.InputMethodService#onKeyUp`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `VolumeCursorHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `android.inputmethodservice.InputMethodService#onKeyDown,android.inputmethodservice.InputMethodService#onKeyUp`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_volumecursor_reverse,controls_volumecursor_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `VolumeCursorHook` vs `VolumeCursorHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_PowerDoubleTapActionHook

- PROOF_ID: `PROOF_FP_Controls_kt_PowerDoubleTapActionHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `PowerDoubleTapActionHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.server.input.util.ShortCutActionsUtils#triggerFunction,com.android.server.policy.MiuiShortcutTriggerHelper#getDoubleVolumeDownKeyFunction,com.android.server.input.shortcut.singlekeyrule.VolumeDownKeyRule#isEnableLaunchCamera`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `PowerDoubleTapActionHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.MiuiKeyShortcutManager#getVolumeKeyLaunchCamera`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_volumedowndt_torch,controls_powerdt`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `PowerDoubleTapActionHook` vs `PowerDoubleTapActionHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_VolumeMediaButtonsHook

- PROOF_ID: `PROOF_FP_Controls_kt_VolumeMediaButtonsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `VolumeMediaButtonsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `VolumeMediaButtonsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A13_CALLBACK_PHASE: `before,intercept`
- PREFERENCE_KEYS: `controls_volumemedia_down`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `VolumeMediaButtonsHook` vs `VolumeMediaButtonsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_GlobalActions_kt_sendDownUpKeyEvent

- PROOF_ID: `PROOF_FP_GlobalActions_kt_sendDownUpKeyEvent`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A14_SYMBOL: `sendDownUpKeyEvent`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A13_SYMBOL: `sendDownUpKeyEvent`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `controls_volumemedia_vibrate,controls_volumemedia_vibrate_ignore`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `sendDownUpKeyEvent` vs `sendDownUpKeyEvent`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherFolderHooks_kt_CloseFolderOrDrawerOnLaunchShortcutMenuHook

- PROOF_ID: `PROOF_FP_LauncherFolderHooks_kt_CloseFolderOrDrawerOnLaunchShortcutMenuHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `CloseFolderOrDrawerOnLaunchShortcutMenuHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.shortcuts.AppShortcutMenuItem#getOnClickListener`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `CloseFolderOrDrawerOnLaunchShortcutMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.shortcuts.AppShortcutMenuItem#getOnClickListener`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_closedrawer,launcher_closefolders`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `CloseFolderOrDrawerOnLaunchShortcutMenuHook` vs `CloseFolderOrDrawerOnLaunchShortcutMenuHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_HideNavBarHook

- PROOF_ID: `PROOF_FP_Controls_kt_HideNavBarHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `HideNavBarHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.navigationbar.NavigationBarController#createNavigationBar,#setWindowState,com.android.systemui.recents.OverviewProxyService#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `HideNavBarHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NavigationModeControllerExt#hideNavigationBar,com.android.systemui.navigationbar.NavigationBarController#createNavigationBar,com.android.systemui.statusbar.phone.MiuiDockIndicatorService#onNavigationModeChanged`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_darkershadow`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideNavBarHook` vs `HideNavBarHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherSystemHooks_kt_DisableLauncherLogHook

- PROOF_ID: `PROOF_FP_LauncherSystemHooks_kt_DisableLauncherLogHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `DisableLauncherLogHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.AnalyticalDataCollectorJobService#onStartJob,com.miui.home.launcher.AnalyticalDataCollector#canTrackLaunchAppEvent`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `DisableLauncherLogHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.AnalyticalDataCollectorJobService#onStartJob,com.miui.home.launcher.AnalyticalDataCollector#canTrackLaunchAppEvent`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_disable_log`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DisableLauncherLogHook` vs `DisableLauncherLogHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherSystemHooks_kt_HideStatusBarInRecentsHook

- PROOF_ID: `PROOF_FP_LauncherSystemHooks_kt_HideStatusBarInRecentsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `HideStatusBarInRecentsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.common.DeviceLevelUtils#isHideStatusBarWhenEnterRecents,com.miui.home.launcher.DeviceConfig#keepStatusBarShowingForBetterPerformance`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `HideStatusBarInRecentsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.common.DeviceLevelUtils#isHideStatusBarWhenEnterRecents,com.miui.home.launcher.DeviceConfig#keepStatusBarShowingForBetterPerformance`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_disable_wallpaperscale`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Normalized owner body identical.
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_FP_LauncherLayoutHooks_kt_DockMarginBottomHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_DockMarginBottomHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `DockMarginBottomHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `DockMarginBottomHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_dock_bottommargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DockMarginBottomHook` vs `DockMarginBottomHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_LAUNCHER_DOCK_HEIGHT

- PROOF_ID: `PROOF_LAUNCHER_DOCK_HEIGHT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `DockHeightHook`
- A14_INSTALLER: `mods/utils/feature/LauncherPackageReadyFeatures.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `DockHeightHook`
- A13_INSTALLER: `installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_dock_height`
- VALUE_DOMAIN: int dp, default 60
- DEFAULT_SEMANTICS: <=60 keeps ROM hotseat height
- RESULT/ARGUMENT_BEHAVIOR: before-hook returnAndSkip dp2px(dockHeight)
- API33_VARIANT_REASON: Same DeviceConfig.calcHotSeatsHeight member on MIUI Home.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_DockMarginTopHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_DockMarginTopHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `DockMarginTopHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginTop`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `DockMarginTopHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginTop`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_dock_topmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DockMarginTopHook` vs `DockMarginTopHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_doubletap

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_doubletap`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `action dispatcher`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `action dispatcher`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_doubletap,launcher_doubletap_action`
- VALUE_DOMAIN: action picker; stored as launcher_doubletap_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key launches picker; _action int selects GlobalActions handler
- API33_VARIANT_REASON: A13 and A14 share the action-slot + _action value domain.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherAnimationHooks_kt_FixAnimHook

- PROOF_ID: `PROOF_FP_LauncherAnimationHooks_kt_FixAnimHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
- A14_SYMBOL: `FixAnimHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.animate.SpringAnimator#getSpringForce,com.miui.home.recents.util.RectFSpringAnim#initAllAnimations`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
- A13_SYMBOL: `FixAnimHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.animate.SpringAnimator#getSpringForce,com.miui.home.recents.util.RectFSpringAnim#initAllAnimations`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_fixanim`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FixAnimHook` vs `FixAnimHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherSystemHooks_kt_FixAppInfoLaunchHook

- PROOF_ID: `PROOF_FP_LauncherSystemHooks_kt_FixAppInfoLaunchHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `FixAppInfoLaunchHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.shortcuts.ShortcutMenuManager#startAppDetailsActivity`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `FixAppInfoLaunchHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.util.Utilities#startDetailsActivityForInfo,com.miui.home.launcher.shortcuts.ShortcutMenuManager#startAppDetailsActivity`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_fixlaunch`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FixAppInfoLaunchHook` vs `FixAppInfoLaunchHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherFolderHooks_kt_FolderColumnsHook

- PROOF_ID: `PROOF_FP_LauncherFolderHooks_kt_FolderColumnsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `FolderColumnsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.Folder#onFinishInflate,com.miui.home.launcher.Folder#resetViewsLayoutParams,com.miui.home.launcher.Folder#onLayout`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `FolderColumnsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Folder#onFinishInflate,com.miui.home.launcher.Folder#onLayout,com.miui.home.launcher.Folder#resetViewsLayoutParams`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `launcher_folder_cols,launcher_folderspace`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FolderColumnsHook` vs `FolderColumnsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FOLDER_BLUR_DISABLE

- PROOF_ID: `PROOF_FOLDER_BLUR_DISABLE`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `FolderBlurHook`
- A14_INSTALLER: `mods/utils/feature/LauncherPostAttachFeatures.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.common.BlurUtils#getLauncherBlur; FolderCling#open`
- A14_CALLBACK_PHASE: `before/after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `FolderBlurHook`
- A13_INSTALLER: `installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.common.BlurUtils#getLauncherBlur; FolderCling#open`
- A13_CALLBACK_PHASE: `before/after`
- PREFERENCE_KEYS: `launcher_folderblur_disable,launcher_folderblur_opacity`
- VALUE_DOMAIN: boolean disable + int opacity
- DEFAULT_SEMANTICS: disable=false uses opacity overlay; disable=true forces clear background
- RESULT/ARGUMENT_BEHAVIOR: resolveFolderBlurRatio(disable, opacity) skipped into getLauncherBlur
- API33_VARIANT_REASON: A13 FolderBlurHook gained the A14 disable flag without replacing opacity storage.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Launcher_kt_onActivityCreated

- PROOF_ID: `PROOF_FP_Launcher_kt_onActivityCreated`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_folderwidth,launcher_cat_folders,launcher_cat_gestures,launcher_cat_privacyapps,launcher_cat_titles,launcher_doubletap,launcher_folder_cols,launcher_folderspace,launcher_pinch,launcher_privacyapps_list,launcher_renameapps_list,launcher_shake,launcher_spread,launcher_swipedown,launcher_swipedown2,launcher_swipeleft,launcher_swiperight,launcher_swipeup,launcher_swipeup2`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `onActivityCreated` vs `onActivityCreated`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherFolderHooks_kt_FolderBlurHook

- PROOF_ID: `PROOF_FP_LauncherFolderHooks_kt_FolderBlurHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `FolderBlurHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#cancelShortcutMenu,#fastBlurWhenOpenOrCloseFolder,#getLauncherBlur`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `FolderBlurHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.FolderCling#open,com.miui.home.launcher.FolderCling#close,com.miui.home.launcher.Launcher#cancelShortcutMenu,#getLauncherBlur`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `launcher_hideseekpoints`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FolderBlurHook` vs `FolderBlurHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_InfiniteScrollHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_InfiniteScrollHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `InfiniteScrollHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.ScreenView#getSnapToScreenIndex,com.miui.home.launcher.ScreenView#getSnapUnitIndex`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `InfiniteScrollHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.miui.home.launcher.ScreenView#getSnapToScreenIndex,com.miui.home.launcher.ScreenView#getSnapUnitIndex`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_hideseekpoints_edit`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `InfiniteScrollHook` vs `InfiniteScrollHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_HorizontalWidgetSpacingHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_HorizontalWidgetSpacingHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `HorizontalWidgetSpacingHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#getMiuiWidgetSizeSpec,com.miui.home.launcher.MIUIWidgetUtil#getMiuiWidgetPadding`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `HorizontalWidgetSpacingHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#getMiuiWidgetSizeSpec,com.miui.home.launcher.MIUIWidgetUtil#getMiuiWidgetPadding`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_horizwidgetmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HorizontalWidgetSpacingHook` vs `HorizontalWidgetSpacingHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherIconHooks_kt_IconScaleHook

- PROOF_ID: `PROOF_FP_LauncherIconHooks_kt_IconScaleHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `IconScaleHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.ShortcutIcon#restoreToInitState,com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ItemIcon#getIconLocation,com.miui.home.launcher.gadget.ClearButton#onCreate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_SYMBOL: `IconScaleHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.ShortcutIcon#restoreToInitState,com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ItemIcon#getIconLocation,com.miui.home.launcher.gadget.ClearButton#onCreate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_iconscale`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `IconScaleHook` vs `IconScaleHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_IndicatorMarginTopHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_IndicatorMarginTopHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `IndicatorMarginTopHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.util.DimenUtils1X#getDimensionPixelSize`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `IndicatorMarginTopHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.util.DimenUtils1X#getDimensionPixelSize`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_indicator_topmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `IndicatorMarginTopHook` vs `IndicatorMarginTopHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_DockHeightHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_DockHeightHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `DockHeightHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `DockHeightHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_indicatorheight,launcher_dock_height`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DockHeightHook` vs `DockHeightHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherSystemHooks_kt_NoClockHideHook

- PROOF_ID: `PROOF_FP_LauncherSystemHooks_kt_NoClockHideHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `NoClockHideHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#updateStatusBarClock`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `NoClockHideHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Launcher#updateStatusBarClock`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_noclockhide`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoClockHideHook` vs `NoClockHideHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherSystemHooks_kt_CloseDrawerOnLaunchHook

- PROOF_ID: `PROOF_FP_LauncherSystemHooks_kt_CloseDrawerOnLaunchHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `CloseDrawerOnLaunchHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.allapps.category.fragment.AppsListFragment#onClick,com.miui.home.launcher.allapps.category.fragment.RecommendCategoryAppListFragment#onClick`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `CloseDrawerOnLaunchHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.allapps.category.fragment.AppsListFragment#onClick,com.miui.home.launcher.allapps.category.fragment.RecommendCategoryAppListFragment#onClick`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_nounlockanim`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `CloseDrawerOnLaunchHook` vs `CloseDrawerOnLaunchHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_NoWidgetOnlyHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_NoWidgetOnlyHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `NoWidgetOnlyHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.CellLayout#setScreenType`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `NoWidgetOnlyHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.CellLayout#setScreenType`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_nowidgetonly`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoWidgetOnlyHook` vs `NoWidgetOnlyHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_AssistGestureActionHook

- PROOF_ID: `PROOF_FP_Controls_kt_AssistGestureActionHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `AssistGestureActionHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.assist.AssistManager#startAssist,com.android.systemui.assist.ui.DefaultUiController#logInvocationProgressMetrics`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `AssistGestureActionHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.assist.AssistManager#startAssist,com.android.systemui.assist.ui.DefaultUiController#logInvocationProgressMetrics`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_oldlaunchanim`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AssistGestureActionHook` vs `AssistGestureActionHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherFolderHooks_kt_PrivacyFolderHook

- PROOF_ID: `PROOF_FP_LauncherFolderHooks_kt_PrivacyFolderHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `PrivacyFolderHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#registerBroadcastReceivers,com.miui.home.launcher.Launcher#startSecurityHide,com.miui.home.launcher.Launcher#onDestroy`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `PrivacyFolderHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Launcher#registerBroadcastReceivers,com.miui.home.launcher.Launcher#startSecurityHide`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `launcher_privacyapps_gest,launcher_spread`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `PrivacyFolderHook` vs `PrivacyFolderHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherFolderHooks_kt_CloseFolderOnLaunchHook

- PROOF_ID: `PROOF_FP_LauncherFolderHooks_kt_CloseFolderOnLaunchHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `CloseFolderOnLaunchHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#launch`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `CloseFolderOnLaunchHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Launcher#launch`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_sensorportrait,launcher_closefolders`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `CloseFolderOnLaunchHook` vs `CloseFolderOnLaunchHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherIconHooks_kt_TitleFontSizeHook

- PROOF_ID: `PROOF_FP_LauncherIconHooks_kt_TitleFontSizeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `TitleFontSizeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ShortcutIcon#fromXml,com.miui.home.launcher.ShortcutIcon#createShortcutIcon,com.miui.home.launcher.common.Utilities#adaptTitleStyleToWallpaper`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_SYMBOL: `TitleFontSizeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ItemIcon#setTitleColorMode,com.miui.home.launcher.ShortcutIcon#fromXml,com.miui.home.launcher.ShortcutIcon#createShortcutIcon,com.miui.home.launcher.common.Utilities#adaptTitleStyleToWallpaper`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_titlefontsize`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `TitleFontSizeHook` vs `TitleFontSizeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherIconHooks_kt_TitleTopMarginHook

- PROOF_ID: `PROOF_FP_LauncherIconHooks_kt_TitleTopMarginHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `TitleTopMarginHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_SYMBOL: `TitleTopMarginHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_titletopmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `TitleTopMarginHook` vs `TitleTopMarginHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_WorkspaceCellPaddingTopHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_WorkspaceCellPaddingTopHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `WorkspaceCellPaddingTopHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#getWorkspaceCellPaddingTop`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `WorkspaceCellPaddingTopHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#getWorkspaceCellPaddingTop`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_topmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `WorkspaceCellPaddingTopHook` vs `WorkspaceCellPaddingTopHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_MainFragment_kt_onActivityCreated

- PROOF_ID: `PROOF_FP_MainFragment_kt_onActivityCreated`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/MainFragment.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `miuizer_launchericon`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `onActivityCreated` vs `onActivityCreated`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_GlobalActions_kt_miuizerSettingsHook

- PROOF_ID: `PROOF_FP_GlobalActions_kt_miuizerSettingsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A14_SYMBOL: `miuizerSettingsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.settings.MiuiSettings#updateHeaderList,com.android.settings.MiuiSettings\$HeaderAdapter#setIcon`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A13_SYMBOL: `miuizerSettingsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java`
- A13_HOOK_TARGETS: `com.android.settings.MiuiSettings#updateHeaderList,com.android.settings.MiuiSettings\$HeaderAdapter#setIcon`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `miuizer_settingsiconpos,launcher_settings`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `miuizerSettingsHook` vs `miuizerSettingsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarMoreHooks_kt_MobileNetworkTypeHook

- PROOF_ID: `PROOF_FP_SystemStatusBarMoreHooks_kt_MobileNetworkTypeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `MobileNetworkTypeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.connectivity.MobileSignalController#getMobileTypeName`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `MobileNetworkTypeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_4gtolte,system_statusbar_mobile_showname`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `MobileNetworkTypeHook` vs `MobileNetworkTypeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_BetterPopupsCenteredHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_BetterPopupsCenteredHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `BetterPopupsCenteredHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.policy.HeadsUpManagerInjector#miuiHeadsUpInset`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `BetterPopupsCenteredHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.HeadsUpManagerInjector#miuiHeadsUpInset`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_albumartonlock`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BetterPopupsCenteredHook` vs `BetterPopupsCenteredHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUILockScreenHooks_kt_LockScreenAlbumArtHook

- PROOF_ID: `PROOF_FP_SystemUILockScreenHooks_kt_LockScreenAlbumArtHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `LockScreenAlbumArtHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,com.android.systemui.statusbar.NotificationMediaManager#clearCurrentMediaNotification,#updateThemeBackground,#updateThemeBackgroundVisibility,#linkageViewAnim`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `LockScreenAlbumArtHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackground,com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackgroundVisibility,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,com.android.systemui.statusbar.NotificationMediaManager#clearCurrentMediaNotification,com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_albumartonlock_blur,system_albumartonlock_gray,system_albumartonlock_scale`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `LockScreenAlbumArtHook` vs `LockScreenAlbumArtHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AllowAllFloatHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AllowAllFloatHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `AllowAllFloatHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.ExpandedNotification#isEnableFloat,com.android.systemui.statusbar.notification.NotificationSettingsManager#canFloat`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `AllowAllFloatHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.MiuiNotificationCompat#isEnableFloat`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_allownotiffloat`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AllowAllFloatHook` vs `AllowAllFloatHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AllRotationsHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AllRotationsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `AllRotationsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.wm.DisplayRotation#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `AllRotationsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.DisplayRotation#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_allrotations2`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AllRotationsHook` vs `AllRotationsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_System_kt_onActivityCreated

- PROOF_ID: `PROOF_FP_System_kt_onActivityCreated`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_animationscale_animator,system_albumartonlock_cat,system_animationscale_transition,system_animationscale_window,system_applock_list,system_applock_skip_activities,system_autobrightness_cat,system_batteryindicator_cat,system_betterpopups_allowfloat_apps,system_blocktoasts_apps,system_calendar_app,system_cat_applock,system_cat_audio,system_cat_betterpopups,system_cat_drawer,system_cat_floatingwindows,system_cat_lockscreen,system_cat_notifications,system_cat_other,system_cat_qs,system_cat_recents,system_cat_screen,system_cat_statusbar,system_cat_toasts,system_cat_vibration,system_cc_clocktweak_cat,system_cc_switch_qsandnotification,system_charginginfo_cat,system_cleanopenwith_apps,system_cleanopenwith_test,system_cleanshare_apps,system_cleanshare_test,system_clock_app,system_colorizenotifs_apps,system_credentials,system_detailednetspeed_cat,system_expandheadups_apps,system_expandnotifs_apps,system_forceclose_apps,system_fw_forcein_actionsend_apps,system_hidefromrecents_apps,system_ignorecalls_apps,system_lockscreenshortcuts_cat,system_lsalarm_cat,system_nooverscroll_apps,system_nopassword,system_noscreenlock_cat,system_notify_openinfw_apps,system_orientationlock,system_screenshot_cat,system_secureqs_cat,system_shortcut_app,system_statusbar_batterystyle_cat,system_statusbar_batterytempandcurrent_cat,system_statusbar_clocktweak_cat,system_statusbar_mobile_signal_cat,system_statusbarcolor_apps,system_statusbarcontrols_cat,system_statusbaricons_atright_cat,system_statusbaricons_cat,system_vibration_amp_cat,system_vibration_apps,system_visualizer_cat`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `onActivityCreated` vs `onActivityCreated`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemSecurityAndSystemHooks_kt_NoSignatureVerifyServiceHook

- PROOF_ID: `PROOF_FP_SystemSecurityAndSystemHooks_kt_NoSignatureVerifyServiceHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `NoSignatureVerifyServiceHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.util.jar.StrictJarVerifier#verifyMessageDigest,android.util.jar.StrictJarVerifier#verify,com.android.server.pm.PackageManagerServiceUtils#verifySignatures,com.android.server.pm.InstallPackageHelper#doesSignatureMatchForPermissions,com.android.server.pm.InstallPackageHelper#cannotInstallWithBadPermissionGroups,com.android.server.pm.permission.PermissionManagerServiceImpl#shouldGrantPermissionBySignature,android.content.pm.ApplicationInfo#isSignedWithPlatformKey,#checkCapability,android.util.jar.StrictJarVerifier#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `NoSignatureVerifyServiceHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `android.util.jar.StrictJarVerifier#verifyMessageDigest,android.util.jar.StrictJarVerifier#verify,com.android.server.pm.PackageManagerServiceUtils#verifySignatures,com.android.server.pm.InstallPackageHelper#doesSignatureMatchForPermissions,com.android.server.pm.InstallPackageHelper#cannotInstallWithBadPermissionGroups,com.android.server.pm.permission.PermissionManagerServiceImpl#shouldGrantPermissionBySignature,#checkCapability,android.util.jar.StrictJarVerifier#<init>`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_apksign`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoSignatureVerifyServiceHook` vs `NoSignatureVerifyServiceHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_AlarmCompatServiceHook

- PROOF_ID: `PROOF_FP_Various_kt_AlarmCompatServiceHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AlarmCompatServiceHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.alarm.AlarmManagerService#onBootPhase,com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AlarmCompatServiceHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.alarm.AlarmManagerService#onBootPhase,com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_applock,various_alarmcompat_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AlarmCompatServiceHook` vs `AlarmCompatServiceHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_AppsDefaultSortHook

- PROOF_ID: `PROOF_FP_Various_kt_AppsDefaultSortHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AppsDefaultSortHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.appmanager.AppManagerMainActivity#onCreate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AppsDefaultSortHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.appmanager.AppManagerMainActivity#onCreate`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_applock_scramblepin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AppsDefaultSortHook` vs `AppsDefaultSortHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemLockScreenMoreHooks_kt_AppLockTimeoutHook

- PROOF_ID: `PROOF_FP_SystemLockScreenMoreHooks_kt_AppLockTimeoutHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `AppLockTimeoutHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.server.SecurityManagerService#addAccessControlPassForUser,com.miui.server.SecurityManagerService#checkAccessControlPassLocked,com.miui.server.SecurityManagerService#activityResume`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `AppLockTimeoutHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.miui.server.SecurityManagerService#addAccessControlPassForUser,com.miui.server.SecurityManagerService#checkAccessControlPassLocked`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_applock_timeout`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AppLockTimeoutHook` vs `AppLockTimeoutHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_AutoBrightnessRangeHook

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_AutoBrightnessRangeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `AutoBrightnessRangeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.display.AutomaticBrightnessController#clampScreenBrightness,com.android.server.display.DisplayPowerController#clampScreenBrightness,com.android.server.display.AutomaticBrightnessController#<init>,com.android.server.display.DisplayPowerController#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `AutoBrightnessRangeHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_autobrightness`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AutoBrightnessRangeHook` vs `AutoBrightnessRangeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_constrainValue

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_constrainValue`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `refreshAutoBrightnessRangeSnapshot`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `constrainValue`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_autobrightness_limitmax,system_autobrightness_limitmin,system_autobrightness_max,system_autobrightness_min`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `refreshAutoBrightnessRangeSnapshot` vs `constrainValue`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIBatteryHooks_kt_BatteryIndicatorHook

- PROOF_ID: `PROOF_FP_SystemUIBatteryHooks_kt_BatteryIndicatorHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A14_SYMBOL: `BatteryIndicatorHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded,com.android.systemui.statusbar.phone.NotificationIconAreaController#onDarkChanged,com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl#fireBatteryLevelChanged,com.android.systemui.statusbar.policy.BatteryControllerImpl#firePowerSaveChanged`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A13_SYMBOL: `BatteryIndicatorHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationIconAreaController#onDarkChanged,com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl#fireBatteryLevelChanged,com.android.systemui.statusbar.policy.BatteryControllerImpl#firePowerSaveChanged`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_batteryindicator`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BatteryIndicatorHook` vs `BatteryIndicatorHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_BatteryIndicator_kt_updateParameters

- PROOF_ID: `PROOF_FP_BatteryIndicator_kt_updateParameters`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_SYMBOL: `updateParameters`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A13_SYMBOL: `updateParameters`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_batteryindicator_align,system_batteryindicator,system_batteryindicator_centered,system_batteryindicator_color,system_batteryindicator_glow,system_batteryindicator_height,system_batteryindicator_limitvis,system_batteryindicator_lowlevel,system_batteryindicator_padding,system_batteryindicator_rounded,system_batteryindicator_transp`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `updateParameters` vs `updateParameters`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_System_BatteryIndicator_kt_onActivityCreated

- PROOF_ID: `PROOF_FP_System_BatteryIndicator_kt_onActivityCreated`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_batteryindicator_colorval1,system_batteryindicator_color,system_batteryindicator_colorval2,system_batteryindicator_colorval3,system_batteryindicator_colorval4,system_batteryindicator_test`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `onActivityCreated` vs `onActivityCreated`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_AutoDismissExpandedPopupsHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_AutoDismissExpandedPopupsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `AutoDismissExpandedPopupsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.HeadsUpManagerPhone\$HeadsUpEntryPhone#updateEntry,com.android.systemui.statusbar.phone.StatusBarNotificationPresenter#onExpandClicked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `AutoDismissExpandedPopupsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.HeadsUpManagerPhone\$HeadsUpEntryPhone#setExpanded`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_betterpopups_allowfloat`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AutoDismissExpandedPopupsHook` vs `AutoDismissExpandedPopupsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationPopupsHooks_kt_BetterPopupsHideDelayHook

- PROOF_ID: `PROOF_FP_SystemNotificationPopupsHooks_kt_BetterPopupsHideDelayHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `BetterPopupsHideDelayHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationPopupsHooks.kt`
- A13_SYMBOL: `BetterPopupsHideDelayHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.HeadsUpManager#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_betterpopups_delay`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BetterPopupsHideDelayHook` vs `BetterPopupsHideDelayHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUINotificationHooks_kt_DisableHeadsUpWhenMuteHook

- PROOF_ID: `PROOF_FP_SystemUINotificationHooks_kt_DisableHeadsUpWhenMuteHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `DisableHeadsUpWhenMuteHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl#canAlertAwakeCommon,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#updateVolumeZen`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `DisableHeadsUpWhenMuteHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.interruption.MiuiNotificationInterruptStateProviderImpl#shouldPeek,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#updateVolumeZen`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_betterpopups_disablewhenmute`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DisableHeadsUpWhenMuteHook` vs `DisableHeadsUpWhenMuteHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook

- PROOF_ID: `PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `buildClockStyleSnapshot`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `StatusBarClockTweakHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange,com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#setClockVisibility,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.qs.MiuiNotificationHeaderView#updateResources,com.android.systemui.qs.MiuiQSHeaderView#updateResources,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_cc_clock_customformat,system_cc_dateformat,system_statusbar_clock_24hour_format,system_statusbar_clock_customformat,system_statusbar_clock_customformat_enable,system_statusbar_clock_leadingzero,system_statusbar_clock_show_ampm,system_statusbar_clock_show_seconds`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `buildClockStyleSnapshot` vs `StatusBarClockTweakHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemSettingsAndConnectivityHooks_kt_CollapseCCAfterClickHook

- PROOF_ID: `PROOF_FP_SystemSettingsAndConnectivityHooks_kt_CollapseCCAfterClickHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `CollapseCCAfterClickHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSettingsAndConnectivityHooks.kt`
- A13_SYMBOL: `CollapseCCAfterClickHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cc_collapse_after_clicked`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `CollapseCCAfterClickHook` vs `CollapseCCAfterClickHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_initialValue

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_initialValue`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/SystemUiResourceBootstrap.kt`
- A14_SYMBOL: `setupSystemUiResources`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `initialValue`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_cc_enable_style_switch,system_cc_show_stepcount,system_statusbar_horizmargin,system_statusbar_iconsize,system_statusbar_topmargin,system_statusbar_topmargin_val,system_volumetimer`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `setupSystemUiResources` vs `initialValue`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIControlCenterHooks_kt_CCTileCornerHook

- PROOF_ID: `PROOF_FP_SystemUIControlCenterHooks_kt_CCTileCornerHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `CCTileCornerHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `miui.systemui.controlcenter.qs.tileview.QSTileItemIconView#getCornerRadius,miui.systemui.controlcenter.qs.tileview.QSTileItemIconView#getDisabledBackgroundDrawable,miui.systemui.controlcenter.qs.tileview.QSTileItemIconView#getActiveBackgroundDrawable`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `CCTileCornerHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `miui.systemui.controlcenter.qs.tileview.ExpandableIconView#setCornerRadius,miui.systemui.dagger.PluginComponentFactory#create,android.content.res.Resources#getDrawable,android.content.res.Resources.Theme#getDrawable`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_cc_tile_roundedrect`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `CCTileCornerHook` vs `CCTileCornerHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIControlCenterHooks_kt_ShowVolumePctHook

- PROOF_ID: `PROOF_FP_SystemUIControlCenterHooks_kt_ShowVolumePctHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `ShowVolumePctHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener#onProgressChanged,#showVolumeDialogH,#dismissH`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `ShowVolumePctHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener#onProgressChanged,#showVolumeDialogH,#dismissH`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cc_volume_showpct`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ShowVolumePctHook` vs `ShowVolumePctHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_ChargeAnimationHook

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_ChargeAnimationHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `ChargeAnimationHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.charge.container.MiuiChargeAnimationView#getAnimationDuration`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `ChargeAnimationHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#showWirelessChargeAnimation,#showRapidChargeAnimation,#showWirelessRapidChargeAnimation`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_chargeanimtime`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ChargeAnimationHook` vs `ChargeAnimationHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemChargingAndWallpaperHooks_kt_ChargingInfoHook

- PROOF_ID: `PROOF_FP_SystemChargingAndWallpaperHooks_kt_ChargingInfoHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `ChargingInfoHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.charge.ChargeUtils#getChargingHintText,com.android.systemui.statusbar.phone.KeyguardIndicationTextView#onFinishInflate,com.android.systemui.statusbar.phone.KeyguardIndicationTextView#setNextIndication`
- A14_CALLBACK_PHASE: `after,intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt`
- A13_SYMBOL: `ChargingInfoHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.keyguard.charge.ChargeUtils#getChargingHintText,com.android.systemui.statusbar.phone.KeyguardIndicationTextView#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_charginginfo`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ChargingInfoHook` vs `ChargingInfoHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_CHARGINGINFO_FONTSIZE

- PROOF_ID: `PROOF_CHARGINGINFO_FONTSIZE`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt`
- A14_SYMBOL: `ChargingInfoHook`
- A14_INSTALLER: `SystemUi feature catalog`
- A14_HOOK_TARGETS: `KeyguardIndicationTextView#<init>`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt`
- A13_SYMBOL: `ChargingInfoHook`
- A13_INSTALLER: `mods/catalog/FeatureCatalog.kt + SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.KeyguardIndicationTextView#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_charginginfo_fontsize`
- VALUE_DOMAIN: int sp, default 16
- DEFAULT_SEMANTICS: default keeps system text size
- RESULT/ARGUMENT_BEHAVIOR: setTextSize(COMPLEX_UNIT_SP) when resolveChargingInfoFontSizeSp non-null
- API33_VARIANT_REASON: Same KeyguardIndicationTextView constructor hook; fontsize is an A13-owned suboption.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemShareAndOpenWithHooks_kt_CleanOpenWithMenuHook

- PROOF_ID: `PROOF_FP_SystemShareAndOpenWithHooks_kt_CleanOpenWithMenuHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareMenuHooks.kt`
- A14_SYMBOL: `CleanOpenWithMenuHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt`
- A13_SYMBOL: `CleanOpenWithMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java`
- A13_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cleanopenwith,system_cleanopenwith_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `CleanOpenWithMenuHook` vs `CleanOpenWithMenuHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationAndShareHooks_kt_ColorizeNotificationCardHook

- PROOF_ID: `PROOF_FP_SystemNotificationAndShareHooks_kt_ColorizeNotificationCardHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemColorizeNotificationHooks.kt`
- A14_SYMBOL: `ColorizeNotificationCardHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateBlurBg,com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#onNotificationUpdated,com.android.systemui.statusbar.notification.row.NotificationBackgroundView#setTint,com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper#getCustomBackgroundColor,com.android.systemui.statusbar.notification.row.NotificationContentView#updateAllSingleLineViews,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#handle3thThemeColor,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createRemoteViews`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt`
- A13_SYMBOL: `ColorizeNotificationCardHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#updateNotificationColor,com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#onNotificationUpdated,com.android.systemui.statusbar.notification.row.NotificationBackgroundView#setTint,com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper#getCustomBackgroundColor,com.android.systemui.statusbar.notification.row.HybridGroupManager#bindFromNotificationWithStyle,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#handle3thThemeColor,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiContentView,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiExpandedView,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiPublicView`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_colorizenotifs,system_colorizenotifs_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ColorizeNotificationCardHook` vs `ColorizeNotificationCardHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_NetSpeedStyleHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_NetSpeedStyleHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `NetSpeedStyleHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.widget.TextView#setTextAppearance,com.android.systemui.statusbar.views.NetworkSpeedView#setNetworkSpeed,com.android.systemui.statusbar.views.NetworkSpeedView#onFinishInflate`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `NetSpeedStyleHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#setNetworkSpeed`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_detailednetspeed_align`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NetSpeedStyleHook` vs `NetSpeedStyleHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_DetailedNetSpeedHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_DetailedNetSpeedHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildDetailedNetSpeedFormatSnapshot`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DetailedNetSpeedHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `#updateNetworkSpeed,#updateText`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_detailednetspeed_icon,system_detailednetspeed_low,system_detailednetspeed_lowlevel,system_detailednetspeed_secunit`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `buildDetailedNetSpeedFormatSnapshot` vs `DetailedNetSpeedHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_ScreenDimTimeHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_ScreenDimTimeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `ScreenDimTimeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.power.PowerManagerService#readConfigurationLocked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `ScreenDimTimeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.power.PowerManagerService#readConfigurationLocked,com.android.server.power.PowerManagerService#setStayOnSettingInternal`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_dimtime`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ScreenDimTimeHook` vs `ScreenDimTimeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_DisableAnyNotificationBlockHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_DisableAnyNotificationBlockHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `DisableAnyNotificationBlockHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.app.NotificationChannel#isBlockable,android.app.NotificationChannel#setBlockable`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `DisableAnyNotificationBlockHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java`
- A13_HOOK_TARGETS: `android.app.NotificationChannel#isBlockable,android.app.NotificationChannel#setBlockable`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_disableanynotif`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DisableAnyNotificationBlockHook` vs `DisableAnyNotificationBlockHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemSecurityAndSystemHooks_kt_DisableSystemIntegrityHook

- PROOF_ID: `PROOF_FP_SystemSecurityAndSystemHooks_kt_DisableSystemIntegrityHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `DisableSystemIntegrityHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.util.apk.ApkSignatureVerifier#getMinimumSignatureSchemeVersionForTargetSdk`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `DisableSystemIntegrityHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `android.util.apk.ApkSignatureVerifier#getMinimumSignatureSchemeVersionForTargetSdk`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_disableintegrity`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DisableSystemIntegrityHook` vs `DisableSystemIntegrityHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemSecurityAndSystemHooks_kt_NoVersionCheckHook

- PROOF_ID: `PROOF_FP_SystemSecurityAndSystemHooks_kt_NoVersionCheckHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `NoVersionCheckHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.pm.PackageManagerServiceUtils#checkDowngrade`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `NoVersionCheckHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.pm.PackageManagerServiceUtils#checkDowngrade`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_downgrade`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Normalized owner body identical.
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_DrawerBlurRatioHook

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_DrawerBlurRatioHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `DrawerBlurRatioHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.NotificationShadeDepthController\$updateBlurCallback\$1#doFrame,com.android.systemui.statusbar.policy.BlurUtilsExt#applyBlur,com.android.systemui.controlcenter.phone.ControlPanelWindowManager#setBlurRatio`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `DrawerBlurRatioHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.NotificationShadeDepthController\$updateBlurCallback\$1#doFrame,com.android.systemui.statusbar.policy.BlurUtilsExt#applyBlurByRadius,com.android.systemui.controlcenter.phone.ControlPanelWindowManager#setBlurRatio,com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_drawer_blur`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DrawerBlurRatioHook` vs `DrawerBlurRatioHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Controls_kt_HideImeDismissButtonHook

- PROOF_ID: `PROOF_FP_Controls_kt_HideImeDismissButtonHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `HideImeDismissButtonHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#updateNavButtonIcons`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `HideImeDismissButtonHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#updateNavButtonIcons`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_drawer_removeshortcut`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideImeDismissButtonHook` vs `HideImeDismissButtonHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_DoubleTapToSleepHook

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_DoubleTapToSleepHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `DoubleTapToSleepHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.shade.NotificationsQuickSettingsContainer#onFinishInflate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `DoubleTapToSleepHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer#onFinishInflate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_dttosleep`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DoubleTapToSleepHook` vs `DoubleTapToSleepHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUINotificationHooks_kt_ExtendedPowerMenuHook

- PROOF_ID: `PROOF_FP_SystemUINotificationHooks_kt_ExtendedPowerMenuHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUI.kt`
- A14_SYMBOL: `ExtendedPowerMenuHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.globalactions.GlobalActionsDialogLite#createActionItems,com.android.systemui.plugins.PluginEnablerImpl#isEnabled,#onPress`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `ExtendedPowerMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.miui.maml.ScreenElementRoot#issueExternCommand,com.android.systemui.plugins.PluginEnablerImpl#isEnabled`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_epm`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ExtendedPowerMenuHook` vs `ExtendedPowerMenuHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_ExpandHeadsUpHook

- PROOF_ID: `PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_ExpandHeadsUpHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `ExpandHeadsUpHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#setHeadsUp`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `ExpandHeadsUpHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#setHeadsUp`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_expandheadups,system_expandheadups_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ExpandHeadsUpHook` vs `ExpandHeadsUpHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_ExpandNotificationsHook

- PROOF_ID: `PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_ExpandNotificationsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `ExpandNotificationsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `ExpandNotificationsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_expandnotifs`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ExpandNotificationsHook` vs `ExpandNotificationsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_FirstVolumePressHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_FirstVolumePressHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `FirstVolumePressHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.audio.AudioService\$VolumeController#suppressAdjustment`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `FirstVolumePressHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.audio.AudioService\$VolumeController#suppressAdjustment`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_firstpress`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `FirstVolumePressHook` vs `FirstVolumePressHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemFreeformAndMultiWindowHooks_kt_OpenAppInFreeFormHook

- PROOF_ID: `PROOF_FP_SystemFreeformAndMultiWindowHooks_kt_OpenAppInFreeFormHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `OpenAppInFreeFormHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.wm.ActivityTaskManagerService#onSystemReady,com.miui.server.SecurityManagerService\$LocalService#checkGameBoosterPayPassAsUser,com.android.server.wm.ActivityStarterImpl#checkStartActivityByFreeForm`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `OpenAppInFreeFormHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.ActivityTaskManagerService#onSystemReady,com.android.server.wm.ActivityStarter#executeRequest`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_fw_forcein_actionsend`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `OpenAppInFreeFormHook` vs `OpenAppInFreeFormHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUILockScreenHooks_kt_HideLockscreenZenModeHook

- PROOF_ID: `PROOF_FP_SystemUILockScreenHooks_kt_HideLockscreenZenModeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `HideLockscreenZenModeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.zen.ZenModeViewController#updateVisibility`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `HideLockscreenZenModeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.zen.ZenModeViewController#shouldBeVisible`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_fw_noblacklist`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideLockscreenZenModeHook` vs `HideLockscreenZenModeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_GalleryScreenshotPathHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_GalleryScreenshotPathHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `GalleryScreenshotPathHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `GalleryScreenshotPathHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/MediaInstaller.java`
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_gallery_screenshots_path`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `GalleryScreenshotPathHook` vs `GalleryScreenshotPathHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_PrivacyAppsLayoutHook

- PROOF_ID: `PROOF_FP_Various_kt_PrivacyAppsLayoutHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `PrivacyAppsLayoutHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.privacyapps.ui.PrivacyAppsActivity#onCreate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `PrivacyAppsLayoutHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.privacyapps.ui.PrivacyAppsActivity#onCreate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_hidelowbatwarn`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `PrivacyAppsLayoutHook` vs `PrivacyAppsLayoutHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AllowAllKeyguardHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_AllowAllKeyguardHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `AllowAllKeyguardHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.ExpandedNotification#isEnableKeyguard,com.android.systemui.statusbar.notification.NotificationSettingsManager#canShowOnKeyguard`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `AllowAllKeyguardHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.MiuiNotificationCompat#isEnableKeyguard`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_hidelsstatusbar`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AllowAllKeyguardHook` vs `AllowAllKeyguardHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_HideProximityWarningHook

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_HideProximityWarningHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `HideProximityWarningHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.policy.MiuiScreenOnProximityLock#showHint,com.android.server.policy.MiuiScreenOnProximityLock#prepareHintWindow`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `HideProximityWarningHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.MiuiScreenOnProximityLock#showHint,com.android.server.policy.MiuiScreenOnProximityLock#prepareHintWindow`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_hideproxywarn`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Normalized owner body identical.
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_FP_BatteryIndicator_kt_registerCallbacks

- PROOF_ID: `PROOF_FP_BatteryIndicator_kt_registerCallbacks`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_SYMBOL: `init`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A13_SYMBOL: `registerCallbacks`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_hidestatusbar_whenscreenshot`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `init` vs `registerCallbacks`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUILockScreenHooks_kt_LockScreenShortcutHook

- PROOF_ID: `PROOF_FP_SystemUILockScreenHooks_kt_LockScreenShortcutHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `LockScreenShortcutHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.keyguard.injector.KeyguardBottomAreaInjector#updateLeftIcon,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightIcon,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightAffordanceViewLayoutVisibility,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateIcons,com.android.keyguard.KeyguardMoveHelper#setTranslation,com.android.keyguard.KeyguardMoveHelper#endMotion,com.android.keyguard.KeyguardMoveRightController#onTouchDown,com.android.keyguard.KeyguardMoveRightController#onTouchMove,com.android.keyguard.injector.KeyguardBottomAreaInjector#<init>`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `LockScreenShortcutHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultLeftButton#getIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultRightButton#getIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#initTipsView,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onFinishInflate,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#updateLeftAffordanceIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onClick,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#launchCamera,com.android.keyguard.MiuiKeyguardCameraView#setDarkStyle,com.android.keyguard.MiuiKeyguardCameraView#updatePreView,com.android.keyguard.MiuiKeyguardCameraView#setPreviewImageDrawable,com.android.keyguard.MiuiKeyguardCameraView#handleMoveDistanceChanged,com.android.keyguard.MiuiKeyguardCameraView#startFullScreenAnim,com.android.keyguard.KeyguardMoveHelper#setTranslation,com.android.keyguard.KeyguardMoveHelper#fling`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_lockscreenshortcuts,system_lockscreenshortcuts_left_off,system_lockscreenshortcuts_left_tapaction,system_lockscreenshortcuts_right,system_lockscreenshortcuts_right_action,system_lockscreenshortcuts_right_off`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `LockScreenShortcutHook` vs `LockScreenShortcutHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_LockScreenAlarmHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_LockScreenAlarmHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `LockScreenAlarmHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.KeyguardIndicationController#setIndicationArea,com.android.systemui.statusbar.KeyguardIndicationController#updateDeviceEntryIndication,com.android.keyguard.injector.KeyguardBottomAreaInjector#handleBottomButtonClickedAnimation`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `LockScreenAlarmHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.keyguard.clock.MiuiKeyguardSingleClock#updateTime,com.android.keyguard.clock.MiuiKeyguardDualClock#updateTime`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_lsalarm`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `LockScreenAlarmHook` vs `LockScreenAlarmHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_hookUpdateTime

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_hookUpdateTime`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `hookUpdateTime`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `hookUpdateTime`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_lsalarm_all,system_lsalarm_format`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `hookUpdateTime` vs `hookUpdateTime`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_MaxNotificationIconsHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_MaxNotificationIconsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `MaxNotificationIconsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationIconContainer#resetViewStates`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `MaxNotificationIconsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationIconContainer#miuiShowNotificationIcons`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_maxsbicons`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `MaxNotificationIconsHook` vs `MaxNotificationIconsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_HideMobileNetworkIndicatorHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_HideMobileNetworkIndicatorHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HideMobileNetworkIndicatorHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarMobileView#applyMobileState,com.android.systemui.statusbar.StatusBarMobileView#updateState`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideMobileNetworkIndicatorHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_mobiletypeicon,system_networkindicator_mobile,system_statusbar_mobiletype_show_wificonnected,system_statusbar_mobiletype_single`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideMobileNetworkIndicatorHook` vs `HideMobileNetworkIndicatorHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUILockScreenHooks_kt_SecureQSTilesHook

- PROOF_ID: `PROOF_FP_SystemUILockScreenHooks_kt_SecureQSTilesHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `SecureQSTilesHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click,com.android.systemui.qs.tileimpl.QSTileImpl#longClick,com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `SecureQSTilesHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#createTileInternal,#handleClick`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_morenotif,system_secureqs_keepopened`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `SecureQSTilesHook` vs `SecureQSTilesHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarMoreHooks_kt_DisplayWifiStandardHook

- PROOF_ID: `PROOF_FP_SystemStatusBarMoreHooks_kt_DisplayWifiStandardHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_SYMBOL: `DisplayWifiStandardHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarWifiView#applyWifiState`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `DisplayWifiStandardHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarWifiView#applyWifiState`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_mutevisiblenotif,system_statusbaricons_wifistandard`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DisplayWifiStandardHook` vs `DisplayWifiStandardHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_NETSPEED_BOLDFONT_RENAME

- PROOF_ID: `PROOF_NETSPEED_BOLDFONT_RENAME`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `NetSpeedTypefaceHelper`
- A14_INSTALLER: `A14 SystemUiFeatures / netspeed style`
- A14_HOOK_TARGETS: `network speed text typeface path`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `NetSpeedTypefaceHelper`
- A13_INSTALLER: `installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `network speed text typeface path`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_netspeed_boldfont,system_netspeed_bold`
- VALUE_DOMAIN: boolean bold typeface
- DEFAULT_SEMANTICS: false = stock weight
- RESULT/ARGUMENT_BEHAVIOR: A13 key system_netspeed_bold drives the same typeface helper as A14 boldfont
- API33_VARIANT_REASON: Capability-preserving key rename; not a second netspeed product.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_initNetSpeedStyle

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_initNetSpeedStyle`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildNetSpeedTextStyleSnapshot`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `initNetSpeedStyle`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_netspeed_rowspacing,system_detailednetspeed_align,system_netspeed_fontsize,system_netspeed_leftmargin,system_netspeed_rightmargin,system_netspeed_verticaloffset`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `buildNetSpeedTextStyleSnapshot` vs `initNetSpeedStyle`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_NETSPEED_CLOCK_STYLE

- PROOF_ID: `PROOF_NETSPEED_CLOCK_STYLE`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `initNetSpeedStyle`
- A14_INSTALLER: `installers/SystemUiInstaller.java / A14 SystemUiFeatures`
- A14_HOOK_TARGETS: `status bar NetworkSpeedView / meter style path`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `initNetSpeedStyle`
- A13_INSTALLER: `installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `status bar NetworkSpeedView / meter style path`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_netspeed_use_clock_style`
- VALUE_DOMAIN: boolean
- DEFAULT_SEMANTICS: false keeps netspeed typeface; true copies status-bar clock appearance
- RESULT/ARGUMENT_BEHAVIOR: applyStatusBarClockTextAppearance on netspeed text views
- API33_VARIANT_REASON: A13 NetSpeedTypefaceHelper gained clock-style copy; same status-bar meter owner.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_NetSpeedIntervalHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_NetSpeedIntervalHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `NetSpeedIntervalHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#handleMessage`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `NetSpeedIntervalHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.NetworkSpeedController#postUpdateNetworkSpeedDelay`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_netspeedinterval`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NetSpeedIntervalHook` vs `NetSpeedIntervalHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_NoDuckingHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_NoDuckingHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `NoDuckingHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.audio.FocusRequester#handleFocusLoss`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `NoDuckingHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.audio.FocusRequester#handleFocusLoss`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_noducking`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoDuckingHook` vs `NoDuckingHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_NoLightUpOnChargeHook

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_NoLightUpOnChargeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `NoLightUpOnChargeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.power.PowerManagerService#wakePowerGroupLocked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `NoLightUpOnChargeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_nolightuponcharges`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoLightUpOnChargeHook` vs `NoLightUpOnChargeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_NoOverscrollAppHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_NoOverscrollAppHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `NoOverscrollAppHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.widget.AbsListView#initAbsListView,#setSpringBackEnable,#setSpringEnabled`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `NoOverscrollAppHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `android.widget.AbsListView#initAbsListView,#setSpringBackEnable,#setSpringEnabled`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_nooverscroll`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoOverscrollAppHook` vs `NoOverscrollAppHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemLockScreenMoreHooks_kt_NoScreenLockHook

- PROOF_ID: `PROOF_FP_SystemLockScreenMoreHooks_kt_NoScreenLockHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `NoScreenLockHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.keyguard.KeyguardViewMediator#handleKeyguardDone,com.android.keyguard.KeyguardUpdateMonitor#onFingerprintAuthenticated,com.android.keyguard.KeyguardSecurityContainerController#onInit,com.android.systemui.keyguard.KeyguardViewMediator#doKeyguardLocked,com.android.systemui.keyguard.KeyguardViewMediator#setupLocked,com.android.keyguard.KeyguardSecurityModel#getSecurityMode,com.android.systemui.statusbar.policy.BluetoothControllerImpl#updateConnected,com.android.systemui.statusbar.policy.BluetoothControllerImpl#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `NoScreenLockHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.keyguard.KeyguardViewMediator#handleKeyguardDone,com.android.keyguard.KeyguardUpdateMonitor#onFingerprintAuthenticated,com.android.systemui.keyguard.KeyguardViewMediator#doKeyguardLocked,com.android.systemui.keyguard.KeyguardViewMediator#setupLocked,com.android.keyguard.KeyguardSecurityModel#getSecurityMode,com.android.systemui.statusbar.policy.BluetoothControllerImpl#updateConnected,#startFaceUnlock,com.android.systemui.keyguard.KeyguardSecurityContainerController#<init>,com.android.systemui.statusbar.policy.BluetoothControllerImpl#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_noscreenlock,system_noscreenlock_force,system_noscreenlock_skip`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoScreenLockHook` vs `NoScreenLockHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_System_NoScreenLock_kt_onActivityCreated

- PROOF_ID: `PROOF_FP_System_NoScreenLock_kt_onActivityCreated`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_noscreenlock_bt,system_noscreenlock,system_noscreenlock_req,system_noscreenlock_wifi`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `onActivityCreated` vs `onActivityCreated`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIControlCenterHooks_kt_BlurMTKVolumeBarHook

- PROOF_ID: `PROOF_FP_SystemUIControlCenterHooks_kt_BlurMTKVolumeBarHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `BlurMTKVolumeBarHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.miui.volume.Util#isSupportBlurS`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `BlurMTKVolumeBarHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.miui.volume.Util#isSupportBlurS`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_nosilentvibrate`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Normalized owner body identical.
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_FP_SystemSecurityAndSystemHooks_kt_NoSOSHook

- PROOF_ID: `PROOF_FP_SystemSecurityAndSystemHooks_kt_NoSOSHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `NoSOSHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.keyguard.EmergencyButtonController#updateEmergencyCallButton`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `NoSOSHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.keyguard.EmergencyButton#updateEmergencyCallButton`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_nosos`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NoSOSHook` vs `NoSOSHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUINotificationHooks_kt_HideNoficationAccessIconHook

- PROOF_ID: `PROOF_FP_SystemUINotificationHooks_kt_HideNoficationAccessIconHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `HideNoficationAccessIconHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.qs.MiuiQSHeaderView#updateShortCutVisibility,com.android.systemui.qs.MiuiNotificationHeaderView#updateShortCutVisibility`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `HideNoficationAccessIconHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.qs.MiuiQSHeaderView#updateShortCutVisibility,com.android.systemui.qs.MiuiNotificationHeaderView#updateShortCutVisibility`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_notifafterunlock`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideNoficationAccessIconHook` vs `HideNoficationAccessIconHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_NotificationImportanceHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_NotificationImportanceHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `NotificationImportanceHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.settings.notification.BaseNotificationSettings#setPrefVisible,com.android.settings.notification.ChannelNotificationSettings#setupChannelDefaultPrefs`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `NotificationImportanceHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java`
- A13_HOOK_TARGETS: `com.android.settings.notification.BaseNotificationSettings#setPrefVisible,com.android.settings.notification.ChannelNotificationSettings#setupChannelDefaultPrefs`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_notifimportance`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `NotificationImportanceHook` vs `NotificationImportanceHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUINotificationHooks_kt_OpenNotifyInFloatingWindowHook

- PROOF_ID: `PROOF_FP_SystemUINotificationHooks_kt_OpenNotifyInFloatingWindowHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `OpenNotifyInFloatingWindowHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter#onNotificationClicked`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `OpenNotifyInFloatingWindowHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `#startNotificationIntent`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_notify_openinfw,system_notify_openinfw_apps,system_notify_openinfw_in_whitelist`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `OpenNotifyInFloatingWindowHook` vs `OpenNotifyInFloatingWindowHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_WallpaperScaleLevelHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_WallpaperScaleLevelHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `WallpaperScaleLevelHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.wm.WallpaperController#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `WallpaperScaleLevelHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.WallpaperController#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_other_wallpaper_scale`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `WallpaperScaleLevelHook` vs `WallpaperScaleLevelHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationAndShareHooks_kt_QSHapticHook

- PROOF_ID: `PROOF_FP_SystemNotificationAndShareHooks_kt_QSHapticHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `QSHapticHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt`
- A13_SYMBOL: `QSHapticHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSFactoryImpl#createTileInternal`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_qshaptics,system_qshaptics_ignore`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `QSHapticHook` vs `QSHapticHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherAnimationHooks_kt_RecentsBlurRatioHook

- PROOF_ID: `PROOF_FP_LauncherAnimationHooks_kt_RecentsBlurRatioHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `RecentsBlurRatioHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#fastBlurWhenEnterRecents,#fastBlurWhenGestureResetTaskView,#fastBlur`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
- A13_SYMBOL: `RecentsBlurRatioHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `#fastBlurWhenEnterRecents,#fastBlurWhenGestureResetTaskView,#fastBlur`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_recents_blur`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `RecentsBlurRatioHook` vs `RecentsBlurRatioHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemSecurityAndSystemHooks_kt_RemoveActStartConfirmHook

- PROOF_ID: `PROOF_FP_SystemSecurityAndSystemHooks_kt_RemoveActStartConfirmHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `RemoveActStartConfirmHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.server.SecurityManagerService\$LocalService#checkAllowStartActivity`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `RemoveActStartConfirmHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_remove_startactconfirm`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `RemoveActStartConfirmHook` vs `RemoveActStartConfirmHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUINotificationHooks_kt_HideDismissViewHook

- PROOF_ID: `PROOF_FP_SystemUINotificationHooks_kt_HideDismissViewHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `HideDismissViewHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.shade.MiuiNotificationPanelViewController#updateDismissView`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `HideDismissViewHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateDismissView`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_removedismiss`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideDismissViewHook` vs `HideDismissViewHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemSecurityAndSystemHooks_kt_RemoveSecureHook

- PROOF_ID: `PROOF_FP_SystemSecurityAndSystemHooks_kt_RemoveSecureHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `RemoveSecureHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.wm.WindowState#isSecureLocked,com.android.server.wm.WindowSurfaceController#setSecure,com.android.server.wm.WindowManagerServiceImpl#notAllowCaptureDisplay,com.android.server.wm.WindowSurfaceController#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `RemoveSecureHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.WindowState#isSecureLocked,com.android.server.wm.WindowSurfaceController#setSecure,com.android.server.wm.WindowSurfaceController#<init>`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_removesecure`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `RemoveSecureHook` vs `RemoveSecureHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_LauncherLayoutHooks_kt_ResizableWidgetsHook

- PROOF_ID: `PROOF_FP_LauncherLayoutHooks_kt_ResizableWidgetsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `ResizableWidgetsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.appwidget.AppWidgetHostView#getAppWidgetInfo`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `ResizableWidgetsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `android.appwidget.AppWidgetHostView#getAppWidgetInfo`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_resizablewidgets`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ResizableWidgetsHook` vs `ResizableWidgetsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemDisplayAndWindowHooks_kt_ScreenAnimHook

- PROOF_ID: `PROOF_FP_SystemDisplayAndWindowHooks_kt_ScreenAnimHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `ScreenAnimHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.display.DisplayPowerController#initialize`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `ScreenAnimHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.display.DisplayPowerController#initialize`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_screenanim_duration`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ScreenAnimHook` vs `ScreenAnimHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_ScreenshotConfigHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_ScreenshotConfigHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ScreenshotConfigHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.content.ContentResolver#update,android.content.ContentResolver#insert,android.graphics.Bitmap#compress`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `ScreenshotConfigHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/MediaInstaller.java`
- A13_HOOK_TARGETS: `android.content.ContentResolver#update,android.content.ContentResolver#insert,com.miui.screenshot.MiuiScreenshotApplication#attachBaseContext,com.miui.screenshot.u0.f\$a#a,com.miui.screenshot.x0.e\$a#a,android.graphics.Bitmap#compress`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_screenshot,system_screenshot_format,system_screenshot_mypath,system_screenshot_path,system_screenshot_quality`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ScreenshotConfigHook` vs `ScreenshotConfigHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemLockScreenHooks_kt_EnhancedSecurityHook

- PROOF_ID: `PROOF_FP_SystemLockScreenHooks_kt_EnhancedSecurityHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `EnhancedSecurityHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#interceptPowerKeyDown,com.android.server.policy.PhoneWindowManager#powerLongPress,com.android.server.policy.PhoneWindowManager#showGlobalActions,com.android.server.policy.PhoneWindowManager#showGlobalActionsInternal`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A13_SYMBOL: `EnhancedSecurityHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#showGlobalActions,com.android.server.policy.PhoneWindowManager#showGlobalActionsInternal`
- A13_CALLBACK_PHASE: `after,before,intercept`
- PREFERENCE_KEYS: `system_securelock`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `EnhancedSecurityHook` vs `EnhancedSecurityHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarMoreHooks_kt_HideIconsBattery1Hook

- PROOF_ID: `PROOF_FP_SystemStatusBarMoreHooks_kt_HideIconsBattery1Hook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_SYMBOL: `HideIconsBattery1Hook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `HideIconsBattery1Hook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiBatteryMeterView#initMiuiView`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_secureqs`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideIconsBattery1Hook` vs `HideIconsBattery1Hook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUILockScreenHooks_kt_isSecureTile

- PROOF_ID: `PROOF_FP_SystemUILockScreenHooks_kt_isSecureTile`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `SecureQSTilesHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click,com.android.systemui.qs.tileimpl.QSTileImpl#longClick,com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `isSecureTile`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_secureqs_airplane,system_secureqs_bt,system_secureqs_custom,system_secureqs_hotspot,system_secureqs_location,system_secureqs_mobiledata,system_secureqs_nfc,system_secureqs_sync,system_secureqs_wifi`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `SecureQSTilesHook` vs `isSecureTile`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIControlCenterHooks_kt_BrightnessPctHook

- PROOF_ID: `PROOF_FP_SystemUIControlCenterHooks_kt_BrightnessPctHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `BrightnessPctHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStart,com.android.systemui.controlcenter.policy.MiuiBrightnessController#setToggleSliderBase,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStop,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onChanged,#onStartTrackingTouch`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `BrightnessPctHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.BrightnessMirrorController#showMirror,com.android.systemui.statusbar.policy.BrightnessMirrorController#hideMirror,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStart,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStop,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onChanged`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_showpct`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BrightnessPctHook` vs `BrightnessPctHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_StatusBarIconsPositionAdjustHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_StatusBarIconsPositionAdjustHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `StatusBarIconsPositionAdjustHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#updateStatusBarVisibilities,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#miuiOnAttachedToWindow`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `StatusBarIconsPositionAdjustHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility,com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility,com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIcon,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation,com.android.systemui.statusbar.policy.NetworkSpeedController#setDripNetworkSpeedView,com.android.systemui.statusbar.views.NetworkSpeedView#setVisibilityByController,com.android.systemui.statusbar.phone.StatusBarSignalPolicy#<init>`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_statusbar_alarm_atleft,system_statusbar_alarm_atright,system_statusbar_btbattery_atright,system_statusbar_dnd_atleft,system_statusbar_dualrows,system_statusbar_gps_atleft,system_statusbar_headset_atright,system_statusbar_netspeed_atleft,system_statusbar_netspeed_atsecondrow,system_statusbar_nfc_atright,system_statusbar_sound_atleft,system_statusbar_vpn_atright,system_statusbaricons_swap_wifi_mobile,system_statusbaricons_wifi_mobile_atleft`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `StatusBarIconsPositionAdjustHook` vs `StatusBarIconsPositionAdjustHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIBatteryHooks_kt_StatusBarStyleBatteryIconHook

- PROOF_ID: `PROOF_FP_SystemUIBatteryHooks_kt_StatusBarStyleBatteryIconHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A14_SYMBOL: `StatusBarStyleBatteryIconHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A13_SYMBOL: `StatusBarStyleBatteryIconHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbar_batterystyle_bold`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `StatusBarStyleBatteryIconHook` vs `StatusBarStyleBatteryIconHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_MonitorDeviceInfoHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_MonitorDeviceInfoHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt`
- A14_SYMBOL: `MonitorDeviceInfoHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `MonitorDeviceInfoHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconController\$IconManager#addHolder,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea,#getSlot,com.android.systemui.statusbar.policy.NetworkSpeedController#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbar_batterytempandcurrent`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `MonitorDeviceInfoHook` vs `MonitorDeviceInfoHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_DeviceInfoMonitor_kt_readSnapshot

- PROOF_ID: `PROOF_FP_DeviceInfoMonitor_kt_readSnapshot`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt`
- A14_SYMBOL: `buildConfig`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt`
- A13_SYMBOL: `readSnapshot`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbar_batterytempandcurrent_content,system_statusbar_batterytempandcurrent_fixcurrentratio,system_statusbar_batterytempandcurrent_hideunit,system_statusbar_batterytempandcurrent_incharge,system_statusbar_batterytempandcurrent_positive,system_statusbar_batterytempandcurrent_reverseorder,system_statusbar_batterytempandcurrent_singlerow,system_statusbar_batterytempandcurrent_temp_decimal,system_statusbar_showdevicetemperature_content,system_statusbar_showdevicetemperature_hideunit,system_statusbar_showdevicetemperature_reverseorder,system_statusbar_showdevicetemperature_singlerow`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `buildConfig` vs `readSnapshot`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_initClockStyle

- PROOF_ID: `PROOF_FP_SystemStatusBarClockAndMoreHooks_kt_initClockStyle`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `buildClockStyleSnapshot`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `initClockStyle`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbar_clock_chip,system_statusbar_clock_chip_customtextcolor,system_statusbar_clock_chip_endcolor,system_statusbar_clock_chip_horizpadding,system_statusbar_clock_chip_orientation_vertical,system_statusbar_clock_chip_radius,system_statusbar_clock_chip_startcolor,system_statusbar_clock_chip_textcolor,system_statusbar_clock_chip_usemonet,system_statusbar_clock_chip_verticalpadding,system_statusbar_clock_fontsize,system_statusbar_clock_leftmargin,system_statusbar_clock_rightmargin,system_statusbar_clock_verticaloffset`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `buildClockStyleSnapshot` vs `initClockStyle`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_StatusBarClockPositionHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_StatusBarClockPositionHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `StatusBarClockPositionHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.PhoneStatusBarView#updateLayoutForCutout,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateNotificationIconAreaInnnerParent`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `StatusBarClockPositionHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.PhoneStatusBarView#updateLayoutForCutout,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateNotificationIconAreaInnnerParent`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbar_clock_position`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `StatusBarClockPositionHook` vs `StatusBarClockPositionHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_DualRowStatusbarHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_DualRowStatusbarHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `DualRowsStatusbarHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DualRowStatusbarHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbar_dualrows_clock_span2rows,system_icons,system_statusbar_batterytempandcurrent,system_statusbar_batterytempandcurrent_atright,system_statusbar_dualrows_firstrow_horizmargin,system_statusbar_dualrows_firstrow_horizmargin_left,system_statusbar_dualrows_firstrow_horizmargin_right,system_statusbar_dualrows_left_ratio,system_statusbar_netspeed_atsecondrow,system_statusbar_showdevicetemperature,system_statusbar_showdevicetemperature_atright`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DualRowsStatusbarHook` vs `DualRowStatusbarHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_DUALROWS_LEFT_RATIO

- PROOF_ID: `PROOF_DUALROWS_LEFT_RATIO`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `DualRowsHook`
- A14_INSTALLER: `SystemUi installer / A14 features`
- A14_HOOK_TARGETS: `MiuiPhoneStatusBarView#updateCutoutLocation`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DualRowStatusbarHook`
- A13_INSTALLER: `installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbar_dualrows_left_ratio`
- VALUE_DOMAIN: int ratio, default 4
- DEFAULT_SEMANTICS: default split; custom ratio on no-cutout type 0
- RESULT/ARGUMENT_BEHAVIOR: left/right LinearLayout weights from resolveDualRowsCutoutWeights
- API33_VARIANT_REASON: Same MiuiPhoneStatusBarView.updateCutoutLocation owner on A13 SystemUI.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_DualRowSignalHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_DualRowSignalHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `DualRowSignalHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setMobileIcons,com.android.systemui.statusbar.StatusBarMobileView#applyMobileState,com.android.systemui.statusbar.StatusBarMobileView#updateState,com.android.systemui.statusbar.StatusBarMobileView#applyDarknessInternal,com.android.systemui.statusbar.StatusBarMobileView#onDarkChanged,com.android.systemui.statusbar.StatusBarMobileView#setDripEnd`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DualRowSignalHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.$ControllerImplName#setMobileIcons,com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState,com.android.systemui.statusbar.StatusBarMobileView#applyDarknessInternal,com.android.systemui.statusbar.StatusBarMobileView#init`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbar_dualsimin2rows_leftmargin,system_statusbar_dualsimin2rows_rightmargin,system_statusbar_dualsimin2rows_scale,system_statusbar_dualsimin2rows_style,system_statusbar_dualsimin2rows_verticaloffset,system_statusbar_mobiletype_single`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `DualRowSignalHook` vs `DualRowSignalHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_HideIconsVoWiFiHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_HideIconsVoWiFiHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HideIconsVoWiFiHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig#<init>`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideIconsVoWiFiHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig#getHideVowifi`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbar_horizmargin_left`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideIconsVoWiFiHook` vs `HideIconsVoWiFiHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_HorizMarginHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_HorizMarginHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HorizMarginHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider#getStatusBarContentInsetsForCurrentRotation`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HorizMarginHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider#getStatusBarContentInsetsForCurrentRotation`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbar_mobiletype_single_atleft,system_statusbar_horizmargin_left,system_statusbar_horizmargin_right`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HorizMarginHook` vs `HorizMarginHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_System_StatusbarControls_kt_onActivityCreated

- PROOF_ID: `PROOF_FP_System_StatusbarControls_kt_onActivityCreated`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_StatusbarControls.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbarcontrols_dt,system_statusbarcontrols_longpress`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `onActivityCreated` vs `onActivityCreated`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIControlCenterHooks_kt_StatusBarGesturesHook

- PROOF_ID: `PROOF_FP_SystemUIControlCenterHooks_kt_StatusBarGesturesHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `StatusBarGesturesHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.PhoneStatusBarView#onInterceptTouchEvent,com.android.systemui.statusbar.phone.PhoneStatusBarView#onTouchEvent,com.android.systemui.statusbar.phone.PhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.phone.PhoneStatusBarView#onDetachedFromWindow`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `StatusBarGesturesHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_statusbarcontrols_dual`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `StatusBarGesturesHook` vs `StatusBarGesturesHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemFreeformAndMultiWindowHooks_kt_BetterPopupsAllowFloatHook

- PROOF_ID: `PROOF_FP_SystemFreeformAndMultiWindowHooks_kt_BetterPopupsAllowFloatHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `BetterPopupsAllowFloatHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateMiniWindowBar`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `BetterPopupsAllowFloatHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.NotificationSettingsManager#canSlide,com.android.systemui.statusbar.notification.policy.MiniWindowPolicy#canSlidePackage`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbaricons_airplane,system_betterpopups_allowfloat_apps,system_betterpopups_allowfloat_apps_black`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BetterPopupsAllowFloatHook` vs `BetterPopupsAllowFloatHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarMoreHooks_kt_HideIconsBattery2Hook

- PROOF_ID: `PROOF_FP_SystemStatusBarMoreHooks_kt_HideIconsBattery2Hook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_SYMBOL: `HideIconsBattery2Hook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.KeyguardStatusBarView#onFinishInflate,com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateChargeAndText`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `HideIconsBattery2Hook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.KeyguardStatusBarView#onFinishInflate,com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateChargeAndText`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbaricons_battery2,system_statusbaricons_battery3,system_statusbaricons_battery4`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideIconsBattery2Hook` vs `HideIconsBattery2Hook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_BT_ICON_ALWAYS_HIDE

- PROOF_ID: `PROOF_BT_ICON_ALWAYS_HIDE`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A14_SYMBOL: `HideIconsBluetoothHook`
- A14_INSTALLER: `A14 SystemUi features`
- A14_HOOK_TARGETS: `bluetooth status-bar icon visibility`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `HideIconsBluetoothHook`
- A13_INSTALLER: `installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `bluetooth status-bar icon visibility`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbaricons_bluetoothicn,system_statusbaricons_bluetooth`
- VALUE_DOMAIN: A14 boolean vs A13 list option 3 = always hide
- DEFAULT_SEMANTICS: stock bluetooth icon visible
- RESULT/ARGUMENT_BEHAVIOR: A13 option 3 forces bluetooth icon visibility false
- API33_VARIANT_REASON: A13 already exposes always-hide as bluetooth=3; A14 split a dedicated key.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_HideIconsHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_HideIconsHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HideIconsHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideIconsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility,com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbaricons_dualwifi`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideIconsHook` vs `HideIconsHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIStatusBarHooks_kt_HideIconsSignalHook

- PROOF_ID: `PROOF_FP_SystemUIStatusBarHooks_kt_HideIconsSignalHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HideIconsSignalHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarMobileView#applyMobileState,com.android.systemui.statusbar.StatusBarMobileView#updateState`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideIconsSignalHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbaricons_roaming`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideIconsSignalHook` vs `HideIconsSignalHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemStatusBarMoreHooks_kt_HideIconsSelectiveAlarmHook

- PROOF_ID: `PROOF_FP_SystemStatusBarMoreHooks_kt_HideIconsSelectiveAlarmHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_SYMBOL: `HideIconsSelectiveAlarmHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#onAlarmChanged,#onNextAlarmChanged,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#<init>`
- A14_CALLBACK_PHASE: `after,intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `HideIconsSelectiveAlarmHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.PhoneStatusBarPolicy#updateAlarm,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#onMiuiAlarmChanged,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbaricons_vowifi`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `HideIconsSelectiveAlarmHook` vs `HideIconsSelectiveAlarmHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_WIRELESS_HEADSET_SLOT

- PROOF_ID: `PROOF_WIRELESS_HEADSET_SLOT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `checkSlot`
- A14_INSTALLER: `installers/SystemUiInstaller.java / A14 features`
- A14_HOOK_TARGETS: `status bar icon slot hide path`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `checkSlot`
- A13_INSTALLER: `installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `status bar icon slot hide path`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbaricons_wireless_headset`
- VALUE_DOMAIN: boolean
- DEFAULT_SEMANTICS: false keeps wireless_headset slot
- RESULT/ARGUMENT_BEHAVIOR: checkSlot('wireless_headset') hides the slot when enabled
- API33_VARIANT_REASON: A13 hide-icons path already owned headset; wireless_headset is an extra slot name on the same function.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_ToastTimeHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_ToastTimeHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ToastTimeHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.notification.NotificationManagerService#showNextToastLocked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `ToastTimeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.notification.NotificationManagerService#showNextToastLocked`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_toasttime`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ToastTimeHook` vs `ToastTimeHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_USB_DEFAULT_R1_LATCH

- PROOF_ID: `PROOF_USB_DEFAULT_R1_LATCH`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_SYMBOL: `UsbDefaultFunctionFeature`
- A14_INSTALLER: `SystemServerFeatures.kt::UsbDefaultFunctionFeatureId`
- A14_HOOK_TARGETS: `UsbDeviceManager / HAL setEnabledFunctions`
- A14_CALLBACK_PHASE: `after/intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt`
- A13_SYMBOL: `USBConfigHook`
- A13_INSTALLER: `installers/SystemServerInstaller.java + SettingsInstaller.java`
- A13_HOOK_TARGETS: `UsbDeviceManager.setCurrentFunction; UsbConnectLatch rising-edge`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_usb_default_function,system_defaultusb,system_defaultusb_unsecure`
- VALUE_DOMAIN: follow-system/charge/MTP/PTP mapped onto A13 function strings
- DEFAULT_SEMANTICS: none = follow system; unsecure latch ignored when none
- RESULT/ARGUMENT_BEHAVIOR: A13 setCurrentFunction; disconnect clears UsbConnectLatch
- API33_VARIANT_REASON: A14 HAL setEnabledFunctions(JZI) is not copied; A13 owns setCurrentFunction + R1 latch.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemNotificationMoreHooks_kt_SelectiveVibrationHook

- PROOF_ID: `PROOF_FP_SystemNotificationMoreHooks_kt_SelectiveVibrationHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `SelectiveVibrationHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.vibrator.VibratorManagerService#systemReady,com.android.server.vibrator.VibratorManagerService#vibrate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `SelectiveVibrationHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.vibrator.VibratorManagerService#systemReady,com.android.server.vibrator.VibratorManagerService#vibrate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_vibration,system_vibration_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `SelectiveVibrationHook` vs `SelectiveVibrationHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_MuffledVibrationHook

- PROOF_ID: `PROOF_FP_SystemAudioAndVisualAndMoreHooks_kt_MuffledVibrationHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `MuffledVibrationHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.server.VibratorService#doVibratorOn`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `MuffledVibrationHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.server.VibratorService#doVibratorOn`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_vibration_amp,system_vibration_amp_notif,system_vibration_amp_other,system_vibration_amp_ringer`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `MuffledVibrationHook` vs `MuffledVibrationHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_AudioVisualizer_kt_shouldDisplayAudioVisualizer

- PROOF_ID: `PROOF_FP_AudioVisualizer_kt_shouldDisplayAudioVisualizer`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A14_SYMBOL: `handlePreferenceChanged`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A13_SYMBOL: `shouldDisplayAudioVisualizer`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_visualizer_animdur,system_visualizer_color,system_visualizer_colorval,system_visualizer_controller,system_visualizer_drawer,system_visualizer_dyntime,system_visualizer_glowlevel,system_visualizer_render,system_visualizer_style,system_visualizer_transp`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `handlePreferenceChanged` vs `shouldDisplayAudioVisualizer`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIControlCenterHooks_kt_BlurVolumeDialogBackgroundHook

- PROOF_ID: `PROOF_FP_SystemUIControlCenterHooks_kt_BlurVolumeDialogBackgroundHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `BlurVolumeDialogBackgroundHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl#updateDialogWindowH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#showH`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `BlurVolumeDialogBackgroundHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl#updateDialogWindowH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#showH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#initDialog`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_volumeblur_collapsed`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `BlurVolumeDialogBackgroundHook` vs `BlurVolumeDialogBackgroundHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemUIControlCenterHooks_kt_VolumeDialogAutohideDelayHook

- PROOF_ID: `PROOF_FP_SystemUIControlCenterHooks_kt_VolumeDialogAutohideDelayHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `VolumeDialogAutohideDelayHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `VolumeDialogAutohideDelayHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl#computeTimeoutH`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_volumedialogdelay_collapsed`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `VolumeDialogAutohideDelayHook` vs `VolumeDialogAutohideDelayHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemSettingsAndConnectivityHooks_kt_ViewWifiPasswordHook

- PROOF_ID: `PROOF_FP_SystemSettingsAndConnectivityHooks_kt_ViewWifiPasswordHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ViewWifiPasswordHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.settings.wifi.SavedAccessPointPreference#onBindViewHolder,miuix.appcompat.app.AlertDialog\$Builder#setTitle,miuix.appcompat.app.AlertDialog\$Builder#setMessage,miuix.appcompat.app.AlertDialog#onCreate,com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings#showDeleteDialog`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSettingsAndConnectivityHooks.kt`
- A13_SYMBOL: `ViewWifiPasswordHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java`
- A13_HOOK_TARGETS: `com.android.settings.wifi.SavedAccessPointPreference#onBindViewHolder,miuix.appcompat.app.AlertDialog\$Builder#setTitle,miuix.appcompat.app.AlertDialog\$Builder#setMessage,miuix.appcompat.app.AlertDialog#onCreate,com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings#showDeleteDialog`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_wifipassword,system_wifi_password_dlgtitle,system_wifipassword_btn_title`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ViewWifiPasswordHook` vs `ViewWifiPasswordHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_AnswerCallInHeadUpHook

- PROOF_ID: `PROOF_FP_Various_kt_AnswerCallInHeadUpHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AnswerCallInHeadUpHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.incallui.InCallPresenter#answerIncomingCall`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AnswerCallInHeadUpHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PhoneInstaller.java`
- A13_HOOK_TARGETS: `com.android.incallui.InCallPresenter#answerIncomingCall`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_answerinheadup`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AnswerCallInHeadUpHook` vs `AnswerCallInHeadUpHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_HiddenFeatures_kt_onActivityCreated

- PROOF_ID: `PROOF_FP_Various_HiddenFeatures_kt_onActivityCreated`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt`
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `(owner body / installer callee)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `various_aospnotif,various_batteryoptimization,various_runningservices`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `onActivityCreated` vs `onActivityCreated`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_AppInfoHook

- PROOF_ID: `PROOF_FP_Various_kt_AppInfoHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AppInfoHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#onCreate,#onPreferenceTreeClick`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AppInfoHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `#onCreate`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `various_appdetails`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AppInfoHook` vs `AppInfoHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_InCallBrightnessHook

- PROOF_ID: `PROOF_FP_Various_kt_InCallBrightnessHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `InCallBrightnessHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.incallui.InCallActivity#onCreate`
- A14_CALLBACK_PHASE: `intercept,before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `InCallBrightnessHook`
- A13_INSTALLER: ``
- A13_HOOK_TARGETS: `com.android.incallui.InCallActivity#onCreate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `various_calluibright_night,various_calluibright_type,various_calluibright_val`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `InCallBrightnessHook` vs `InCallBrightnessHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_AddSideBarExpandReceiverHook

- PROOF_ID: `PROOF_FP_Various_kt_AddSideBarExpandReceiverHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AddSideBarExpandReceiverHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#onTouch,#draw,#onViewDetachedFromWindow`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AddSideBarExpandReceiverHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `#onTouch,#draw,#onViewDetachedFromWindow`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `various_disable_dock_suggest,various_swipe_expand_sidebar`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AddSideBarExpandReceiverHook` vs `AddSideBarExpandReceiverHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_AppsDisableHook

- PROOF_ID: `PROOF_FP_Various_kt_AppsDisableHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AppsDisableHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu,com.miui.appmanager.ApplicationsDetailsActivity#onOptionsItemSelected`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AppsDisableHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu,com.miui.appmanager.ApplicationsDetailsActivity#onOptionsItemSelected`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `various_disableapp`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `AppsDisableHook` vs `AppsDisableHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_GboardPaddingHook

- PROOF_ID: `PROOF_FP_Various_kt_GboardPaddingHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `GboardPaddingHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `(owner body / installer callee)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `GboardPaddingHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java`
- A13_HOOK_TARGETS: `#get`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_gboardpadding_land,various_gboardpadding_port`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `GboardPaddingHook` vs `GboardPaddingHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_HIDE_REPORT

- PROOF_ID: `PROOF_HIDE_REPORT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `HideReportButtonHook`
- A14_INSTALLER: `mods/utils/feature/SecurityCenterFeatures.kt`
- A14_HOOK_TARGETS: `com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `HideReportButtonHook`
- A13_INSTALLER: `installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `various_hide_report_ondetails`
- VALUE_DOMAIN: boolean
- DEFAULT_SEMANTICS: false keeps report menu item
- RESULT/ARGUMENT_BEHAVIOR: after onCreateOptionsMenu, itemId 4 setVisible(false)
- API33_VARIANT_REASON: Same ApplicationsDetailsActivity menu owner; A13 SecurityCenter installer.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_PurePackageInstallerHook

- PROOF_ID: `PROOF_FP_Various_kt_PurePackageInstallerHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `PurePackageInstallerHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `android.app.SharedPreferencesImpl#getBoolean,com.miui.packageInstaller.ui.listcomponets.SafeModeTipViewObject\$ViewHolder#updateSuggestionMsgState`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `PurePackageInstallerHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PackageInstallerRouter.java`
- A13_HOOK_TARGETS: `android.app.SharedPreferencesImpl#getBoolean,com.miui.packageInstaller.ui.listcomponets.SafeModeTipViewObject\$ViewHolder#updateSuggestionMsgState`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `various_installappinfo`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `PurePackageInstallerHook` vs `PurePackageInstallerHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_PACKAGEINSTALLER_PURIFY

- PROOF_ID: `PROOF_PACKAGEINSTALLER_PURIFY`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `PurePackageInstallerHook`
- A14_INSTALLER: `mods/utils/feature/PackageInstallerFeatures.kt`
- A14_HOOK_TARGETS: `MIUI package installer preference/settings members`
- A14_CALLBACK_PHASE: `before/after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `PurePackageInstallerHook`
- A13_INSTALLER: `installers/PackageInstallerRouter.java`
- A13_HOOK_TARGETS: `MIUI package installer preference/settings members`
- A13_CALLBACK_PHASE: `before/after`
- PREFERENCE_KEYS: `various_installer_purify`
- VALUE_DOMAIN: boolean
- DEFAULT_SEMANTICS: false keeps installer ads/recommend/verify UI
- RESULT/ARGUMENT_BEHAVIOR: purifiedInstallerBoolean/SystemInt/SecureInt rewrite installer prefs
- API33_VARIANT_REASON: Same Various.PurePackageInstallerHook; A13 router vs A14 FeatureSpec.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_MiuiPackageInstallerHook

- PROOF_ID: `PROOF_FP_Various_kt_MiuiPackageInstallerHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `MiuiPackageInstallerHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.packageInstaller.InstallStart#getCallingPackage`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `MiuiPackageInstallerHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PackageInstallerRouter.java`
- A13_HOOK_TARGETS: `android.os.SystemProperties#getBoolean,com.miui.packageInstaller.InstallStart#getCallingPackage`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_miuiinstaller`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `MiuiPackageInstallerHook` vs `MiuiPackageInstallerHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_PersistBatteryOptimizationHook

- PROOF_ID: `PROOF_FP_Various_kt_PersistBatteryOptimizationHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `PersistBatteryOptimizationHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.powerkeeper.utils.CommonAdapter#addPowerSaveWhitelistApps,com.miui.powerkeeper.millet.MilletPolicy#dealSleepModeWhiteList,com.miui.powerkeeper.statemachine.ForceDozeController#restoreWhiteListAppsIfQuitForceIdle`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `PersistBatteryOptimizationHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PowerKeeperInstaller.java`
- A13_HOOK_TARGETS: `com.miui.powerkeeper.utils.CommonAdapter#addPowerSaveWhitelistApps,com.miui.powerkeeper.millet.MilletPolicy#dealSleepModeWhiteList,com.miui.powerkeeper.statemachine.ForceDozeController#restoreWhiteListAppsIfQuitForceIdle`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_persist_batteryoptimization`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `PersistBatteryOptimizationHook` vs `PersistBatteryOptimizationHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_OpenByDefaultHook

- PROOF_ID: `PROOF_FP_Various_kt_OpenByDefaultHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `OpenByDefaultHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.miui.appmanager.ApplicationsDetailsActivity#initView`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `OpenByDefaultHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.appmanager.ApplicationsDetailsActivity#initView,com.miui.appmanager.ApplicationsDetailsActivity#onClick`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_replace_defaultopen_with_openbydefault`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `OpenByDefaultHook` vs `OpenByDefaultHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_ShowCallUIHook

- PROOF_ID: `PROOF_FP_Various_kt_ShowCallUIHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `ShowCallUIHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `com.android.incallui.InCallPresenter#startUi`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `ShowCallUIHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PhoneInstaller.java`
- A13_HOOK_TARGETS: `com.android.incallui.InCallPresenter#startUi`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `various_showcallui`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ShowCallUIHook` vs `ShowCallUIHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Various_kt_ShowTempInBatteryHook

- PROOF_ID: `PROOF_FP_Various_kt_ShowTempInBatteryHook`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `ShowTempInBatteryHook`
- A14_INSTALLER: ``
- A14_HOOK_TARGETS: `#handleMessage`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `ShowTempInBatteryHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `#handleMessage`
- A13_CALLBACK_PHASE: `after,intercept`
- PREFERENCE_KEYS: `various_skip_securityscan`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: same owner default path unless a key-specific override exists
- RESULT/ARGUMENT_BEHAVIOR: owner hook result/argument rewrite as in matched bodies
- API33_VARIANT_REASON: Identified owners `ShowTempInBatteryHook` vs `ShowTempInBatteryHook`; installer/hook members match or API33 variant in the same capability path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_BACKUP_V2

- PROOF_ID: `PROOF_BACKUP_V2`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupFormatV2.kt`
- A14_SYMBOL: `BackupFormatV2`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt`
- A14_HOOK_TARGETS: `(settings app, no host hook)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupFormatV2.kt`
- A13_SYMBOL: `BackupFormatV2`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt`
- A13_HOOK_TARGETS: `(settings app, no host hook)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: ``
- VALUE_DOMAIN: typed backup entries / CUI2
- DEFAULT_SEMANTICS: encode V2; restore auto-detects V2 vs legacy
- RESULT/ARGUMENT_BEHAVIOR: CRC/size bounds; rollback on commit failure
- API33_VARIANT_REASON: A13 V2 contract matches A14 M2 typed backup; no API34 host types.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`
