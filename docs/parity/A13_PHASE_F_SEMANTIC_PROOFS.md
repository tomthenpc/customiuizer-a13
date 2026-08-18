# A13 Phase F-R4 Semantic Proofs

Automatic PRESENT requires normalized body IDENTICAL, the same relevant preference keys,
and compatible installer ownership (BODY_RELATION=IDENTICAL).
Non-identical owners require an explicit reviewed manifest (BODY_RELATION=REVIEWED_VARIANT)
with filled difference fields and KEY_OWNERSHIP_EVIDENCE.
Prefix, ranked-first, same-XML, and same-basename-alone never assign semantic ownership.
SequenceMatcher ratio never authorizes PRESENT.
Same-key reads or a visible row in both XML files alone are IMPLEMENTATION_PRESENCE.

## PROOF_ACTION_SLOT_controls_backlong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_backlong`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_backlong,controls_backlong_action`
- VALUE_DOMAIN: action picker; stored as controls_backlong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_backlong_action`. The visible `controls_backlong` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_backlong: companion persisted key controls_backlong_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_backlong_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_backlong_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_FINGERPRINT_HAPTIC_FAILURE

- PROOF_ID: `PROOF_OG_FINGERPRINT_HAPTIC_FAILURE`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `FingerprintHapticFailureHook`
- A14_INSTALLER: `installer condition controls_fingerprintfailure`
- A14_HOOK_TARGETS: `com.android.server.biometrics.sensors.AcquisitionClient#vibrateError`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `FingerprintHapticFailureHook`
- A13_INSTALLER: `installers/SystemServerInstaller.java if controls_fingerprintfailure`
- A13_HOOK_TARGETS: `com.android.server.biometrics.sensors.AcquisitionClient#vibrateError`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_fingerprintfailure`
- VALUE_DOMAIN: boolean; default false
- DEFAULT_SEMANTICS: off keeps ROM error vibration
- RESULT/ARGUMENT_BEHAVIOR: Skip AcquisitionClient.vibrateError entirely so fingerprint failure does not vibrate.
- API33_VARIANT_REASON: A14 intercept sets skipped=true result=null and never proceeds. A13 before returnAndSkip(null). Same skip of vibrateError.
- DIFF_SUMMARY: Same member vibrateError skipped unconditionally once the installer gate is on. Callback adapter only.
- VALUE_DEFAULT_COMPARISON: Boolean gate on both trees; no extra failure-haptic modes.
- HOOK_TARGET_COMPARISON: Same AcquisitionClient.vibrateError.
- CALLBACK_SEMANTICS_COMPARISON: A14 intercept skip vs A13 before returnAndSkip; original not called.
- ARG_RESULT_COMPARISON: Skipped result null; no argument rewrite.
- A14_ONLY_BRANCHES: none for this key
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Toggle on means no error haptic on fingerprint failure. Same skipped ROM method.
- KEY_OWNERSHIP_EVIDENCE: controls_fingerprintfailure: INSTALLER_CALLEE → FingerprintHapticFailureHook on both trees
- A14_KEY_OWNER_REFERENCE: Controls.kt::FingerprintHapticFailureHook INSTALLER_CALLEE
- A13_KEY_OWNER_REFERENCE: Controls.kt::FingerprintHapticFailureHook INSTALLER_CALLEE via SystemServerInstaller.java
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_FINGERPRINT_SCREEN_ON

- PROOF_ID: `PROOF_OG_FINGERPRINT_SCREEN_ON`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `FingerprintScreenOnHook`
- A14_INSTALLER: `installer condition controls_fingerprintscreen`
- A14_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `FingerprintScreenOnHook`
- A13_INSTALLER: `installers/SystemServerInstaller.java if controls_fingerprintscreen`
- A13_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_fingerprintscreen`
- VALUE_DOMAIN: boolean; default false
- DEFAULT_SEMANTICS: off keeps ROM screen-off fingerprint failure behavior
- RESULT/ARGUMENT_BEHAVIOR: After onAuthenticated, if authentication failed and PowerManager is not interactive, send WakeUp. Success path does not wake. Authentication result is unchanged.
- API33_VARIANT_REASON: A14 intercept proceeds once then applies the wake side-effect. A13 after applies the same mAuthSuccess / isInteractive tests and WakeUp. No extra A14 wake condition.
- DIFF_SUMMARY: Shared: onAuthenticated; skip wake on success or if already interactive; WakeUp on failed auth while screen off. Adapter: intercept vs after.
- VALUE_DEFAULT_COMPARISON: Boolean installer gate on both trees.
- HOOK_TARGET_COMPARISON: Same AuthenticationClient.onAuthenticated.
- CALLBACK_SEMANTICS_COMPARISON: A14 proceed-once then side-effect equals A13 after side-effect.
- ARG_RESULT_COMPARISON: Host result unchanged; WakeUp is a GlobalActions side-effect.
- A14_ONLY_BRANCHES: none for this key
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Failed fingerprint while the screen is off wakes the device. Same tests and action.
- KEY_OWNERSHIP_EVIDENCE: controls_fingerprintscreen: INSTALLER_CALLEE → FingerprintScreenOnHook on both trees
- A14_KEY_OWNER_REFERENCE: Controls.kt::FingerprintScreenOnHook INSTALLER_CALLEE
- A13_KEY_OWNER_REFERENCE: Controls.kt::FingerprintScreenOnHook INSTALLER_CALLEE via SystemServerInstaller.java
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_FINGERPRINT_HAPTIC_SUCCESS

- PROOF_ID: `PROOF_OG_FINGERPRINT_HAPTIC_SUCCESS`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `FingerprintHapticSuccessHook`
- A14_INSTALLER: `installer condition controls_fingerprintsuccess > 1`
- A14_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `FingerprintHapticSuccessHook`
- A13_INSTALLER: `installers/SystemServerInstaller.java if controls_fingerprintsuccess > 1`
- A13_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_fingerprintsuccess,controls_fingerprintsuccess_ignore`
- VALUE_DOMAIN: string-int 1=ROM default, 2=light haptic, 3=strong haptic; controls_fingerprintsuccess_ignore is a boolean helper consumed by this same hook
- DEFAULT_SEMANTICS: default `1` keeps ROM success haptic; 2/3 replace it
- RESULT/ARGUMENT_BEHAVIOR: After AuthenticationClient.onAuthenticated, if mAuthSuccess: opt 2 light vibration, opt 3 strong vibration; ignoreSystem comes from controls_fingerprintsuccess_ignore. The authentication result is not rewritten.
- API33_VARIANT_REASON: A14 intercept always chain.proceed() once then runs the haptic side-effect. A13 after runs the same haptic side-effect. No skip of onAuthenticated.
- DIFF_SUMMARY: Shared: AuthenticationClient.onAuthenticated; mAuthSuccess gate; getString(controls_fingerprintsuccess) 2/3 haptic; ignore boolean. Differ: A14 intercept+proceed then haptic vs A13 after haptic. A14 uses toInt(); A13 uses toIntOrNull()?:1. Unknown values keep default 1 on A13; A14 toInt() can throw and is caught.
- VALUE_DEFAULT_COMPARISON: Both default the visible list to 1 (keep ROM). The ignore helper is consumed by this same hook, not by system_vibration/toast owners.
- HOOK_TARGET_COMPARISON: Same AuthenticationClient.onAuthenticated member.
- CALLBACK_SEMANTICS_COMPARISON: A14 proceed-once then side-effect equals A13 after side-effect; neither returnAndSkip.
- ARG_RESULT_COMPARISON: Host return value unchanged; only vibrator side-effect after success.
- A14_ONLY_BRANCHES: none that add a fourth haptic mode; intercept wrapper only
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user-visible control is success-haptic strength. Both trees apply light/strong vibration only on authenticated success and leave ROM behavior at value 1.
- KEY_OWNERSHIP_EVIDENCE: controls_fingerprintsuccess: LITERAL_READ + INSTALLER_CALLEE in FingerprintHapticSuccessHook; controls_fingerprintsuccess_ignore: LITERAL_READ in the same hook (ignoreSystem). system_blocktoasts / system_nolightuponcharges / system_vibration are not consumed here.
- A14_KEY_OWNER_REFERENCE: Controls.kt::FingerprintHapticSuccessHook LITERAL_READ controls_fingerprintsuccess,controls_fingerprintsuccess_ignore
- A13_KEY_OWNER_REFERENCE: Controls.kt::FingerprintHapticSuccessHook LITERAL_READ + SystemServerInstaller INSTALLER_CALLEE
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_NO_FINGERPRINT_WAKE

- PROOF_ID: `PROOF_OG_NO_FINGERPRINT_WAKE`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `NoFingerprintWakeHook`
- A14_INSTALLER: `A14 SystemServer feature / installer condition controls_fingerprintwake`
- A14_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `NoFingerprintWakeHook`
- A13_INSTALLER: `installers/SystemServerInstaller.java if controls_fingerprintwake`
- A13_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_fingerprintwake`
- VALUE_DOMAIN: boolean; default false; installer gate only (hook body has no pref read)
- DEFAULT_SEMANTICS: off keeps ROM back-fingerprint wake-when-screen-off
- RESULT/ARGUMENT_BEHAVIOR: When the hooked method's screen-on boolean is false, skip the original processBackFingerprintDpcenterEvent so a back-fingerprint tap does not wake the device. When screen-on is true, the original method runs.
- API33_VARIANT_REASON: A14 intercept: if !isScreenOn skip with null and do not chain.proceed(); else one proceed. A13 before: if !isScreenOn returnAndSkip(null). Same member, same screen-on test, same skip.
- DIFF_SUMMARY: Shared: MiuiPhoneWindowManager.processBackFingerprintDpcenterEvent(KeyEvent, boolean); skip original when arg1 is false. Differ: A14 intercept/proceed-once vs A13 before skip. No extra A14 user-visible branch on controls_fingerprintwake.
- VALUE_DEFAULT_COMPARISON: Both use a boolean installer gate; neither introduces extra modes.
- HOOK_TARGET_COMPARISON: Same class and member on both trees.
- CALLBACK_SEMANTICS_COMPARISON: A14 intercept skip-or-proceed-once maps to A13 before returnAndSkip; the original is not invoked on the skipped path on either tree.
- ARG_RESULT_COMPARISON: Skipped result is null; the KeyEvent is not rewritten.
- A14_ONLY_BRANCHES: none for this key; intercept scaffolding only
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: User contract is: with the toggle on, a back-fingerprint press while the screen is off does not wake the device. That is the same skip on the same ROM method.
- KEY_OWNERSHIP_EVIDENCE: controls_fingerprintwake: INSTALLER_CALLEE on both trees (SystemServerInstaller / A14 installer condition → NoFingerprintWakeHook)
- A14_KEY_OWNER_REFERENCE: Controls.kt::NoFingerprintWakeHook INSTALLER_CALLEE controls_fingerprintwake
- A13_KEY_OWNER_REFERENCE: Controls.kt::NoFingerprintWakeHook INSTALLER_CALLEE via SystemServerInstaller.java
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_fsg_assist_left

- PROOF_ID: `PROOF_ACTION_SLOT_controls_fsg_assist_left`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_fsg_assist_left,controls_fsg_assist_left_action`
- VALUE_DOMAIN: action picker; stored as controls_fsg_assist_left_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_fsg_assist_left_action`. The visible `controls_fsg_assist_left` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_fsg_assist_left: companion persisted key controls_fsg_assist_left_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_fsg_assist_left_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_fsg_assist_left_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_fsg_assist_right

- PROOF_ID: `PROOF_ACTION_SLOT_controls_fsg_assist_right`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_fsg_assist_right,controls_fsg_assist_right_action`
- VALUE_DOMAIN: action picker; stored as controls_fsg_assist_right_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_fsg_assist_right_action`. The visible `controls_fsg_assist_right` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_fsg_assist_right: companion persisted key controls_fsg_assist_right_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_fsg_assist_right_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_fsg_assist_right_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FSG_HORIZ

- PROOF_ID: `PROOF_FSG_HORIZ`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A14_SYMBOL: `FSGesturesHook`
- A14_INSTALLER: `ForceFsgNavBarCallerScope + Launcher installer / A14 launcher features`
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#usingFsGesture,com.miui.home.recents.BaseRecentsImpl#createAndAddNavStubView,com.miui.home.recents.BaseRecentsImpl#updateFsgWindowState,com.miui.launcher.utils.MiuiSettingsUtils#getGlobalBoolean,com.miui.home.recents.GestureStubView#onTouchEvent,BaseRecentsImpl#lambda$showBackStubWindow,BaseRecentsImpl#lambda$updateFsgWindowVisibilityState`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A13_SYMBOL: `FSGesturesHook`
- A13_INSTALLER: `installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#usingFsGesture,com.miui.home.recents.BaseRecentsImpl#createAndAddNavStubView,com.miui.home.recents.BaseRecentsImpl#updateFsgWindowState,com.miui.launcher.utils.MiuiSettingsUtils#getGlobalBoolean,com.miui.home.recents.GestureStubView#onTouchEvent`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `controls_fsg_horiz,controls_fsg_horiz_apps`
- VALUE_DOMAIN: boolean force-FSG + StringSet skip-apps
- DEFAULT_SEMANTICS: controls_fsg_horiz default false; controls_fsg_horiz_apps default empty set
- RESULT/ARGUMENT_BEHAVIOR: usingFsGesture constant true; createAndAddNavStubView skipped when REAL_FORCE_FSG_NAV_BAR is false; updateFsgWindowState removes mNavStubView when not fsg; getGlobalBoolean(force_fsg_nav_bar) stashes the real result then reports true for BaseRecents callers; GestureStubView.onTouchEvent skipped for packages in controls_fsg_horiz_apps
- API33_VARIANT_REASON: A13 identifies BaseRecentsImpl callers of getGlobalBoolean by walking Thread.currentThread().stackTrace for class com.miui.home.recents.BaseRecentsImpl. A14 uses ForceFsgNavBarCallerScope ThreadLocal around three verified HyperOS members. On API33/MIUI 14 the lambda names are not required; the stack-trace class filter preserves force-FSG plus per-app skip without those members.
- DIFF_SUMMARY: Shared: DeviceConfig.usingFsGesture=true, createAndAddNavStubView skip, updateFsgWindowState stub removal, GestureStubView skip-apps. Differ: A14 intercept/chain.proceed + ForceFsgNavBarCallerScope ThreadLocal on updateFsgWindowState and two BaseRecentsImpl lambdas; A13 before/after + stack-trace class scan in getGlobalBoolean.
- VALUE_DEFAULT_COMPARISON: Both treat controls_fsg_horiz as the enable gate and controls_fsg_horiz_apps as the skip package set; neither inverts the boolean or replaces the StringSet with a whitelist.
- HOOK_TARGET_COMPARISON: Shared members: usingFsGesture, createAndAddNavStubView, updateFsgWindowState, getGlobalBoolean, GestureStubView.onTouchEvent. A14-only: lambda$showBackStubWindow$*$BaseRecentsImpl(boolean) and lambda$updateFsgWindowVisibilityState$*$BaseRecentsImpl(boolean, String).
- CALLBACK_SEMANTICS_COMPARISON: A14: intercept with one chain.proceed() on the unskipped path. A13: before returnAndSkip for createAndAddNavStubView/GestureStubView; after setResult(true) for getGlobalBoolean. proceed-once vs skip/setResult maps to the same skip-or-force-true user path.
- ARG_RESULT_COMPARISON: Both stash REAL_FORCE_FSG_NAV_BAR from the real getGlobalBoolean result then report true to BaseRecents; both returnAndSkip(false) on GestureStubView ACTION_DOWN when the foreground package is in controls_fsg_horiz_apps; neither rewrites the MotionEvent.
- A14_ONLY_BRANCHES: ForceFsgNavBarCallerScope fail-closed install of three verified BaseRecentsImpl callers; lambda$showBackStubWindow and lambda$updateFsgWindowVisibilityState. A13 does not hook those lambdas.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: User-visible contract is force full-screen gestures plus disable horizontal FSG in selected apps. A13's stack-trace BaseRecentsImpl filter is the API33-compatible caller scope: it does not depend on HyperOS-only lambda names that MIUI 14 Home may lack. Extra A14 caller wrappers are robustness, not a second toggle.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_fsg_swipeandstop

- PROOF_ID: `PROOF_ACTION_SLOT_controls_fsg_swipeandstop`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_fsg_swipeandstop,controls_fsg_swipeandstop_action`
- VALUE_DOMAIN: action picker; stored as controls_fsg_swipeandstop_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_fsg_swipeandstop_action`. The visible `controls_fsg_swipeandstop` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_fsg_swipeandstop: companion persisted key controls_fsg_swipeandstop_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_fsg_swipeandstop_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_fsg_swipeandstop_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_HIDE_IME_DISMISS

- PROOF_ID: `PROOF_HIDE_IME_DISMISS`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: Same NavigationBarView member; A13 uses installer boolean vs A14 FeatureSpec.
- VALUE_DEFAULT_COMPARISON: false keeps stock IME dismiss
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.navigationbar.NavigationBarView#updateNavButtonIcons; A13=com.android.systemui.navigationbar.NavigationBarView#updateNavButtonIcons
- CALLBACK_SEMANTICS_COMPARISON: A14=after; A13=after
- ARG_RESULT_COMPARISON: after updateNavButtonIcons, set IME back-alt visibility INVISIBLE when gestural
- A14_ONLY_BRANCHES: Same NavigationBarView member; A13 uses installer boolean vs A14 FeatureSpec.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: after updateNavButtonIcons, set IME back-alt visibility INVISIBLE when gestural. Same NavigationBarView member; A13 uses installer boolean vs A14 FeatureSpec.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_homelong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_homelong`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_homelong,controls_homelong_action`
- VALUE_DOMAIN: action picker; stored as controls_homelong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_homelong_action`. The visible `controls_homelong` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_homelong: companion persisted key controls_homelong_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_homelong_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_homelong_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_menulong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_menulong`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_menulong,controls_menulong_action`
- VALUE_DOMAIN: action picker; stored as controls_menulong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_menulong_action`. The visible `controls_menulong` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_menulong: companion persisted key controls_menulong_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_menulong_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_menulong_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarleft

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarleft`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarleft,controls_navbarleft_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarleft_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_navbarleft_action`. The visible `controls_navbarleft` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_navbarleft: companion persisted key controls_navbarleft_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarleft_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarleft_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarleftlong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarleftlong`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarleftlong,controls_navbarleftlong_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarleftlong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_navbarleftlong_action`. The visible `controls_navbarleftlong` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_navbarleftlong: companion persisted key controls_navbarleftlong_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarleftlong_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarleftlong_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarright

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarright`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarright,controls_navbarright_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarright_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_navbarright_action`. The visible `controls_navbarright` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_navbarright: companion persisted key controls_navbarright_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarright_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarright_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_navbarrightlong

- PROOF_ID: `PROOF_ACTION_SLOT_controls_navbarrightlong`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_navbarrightlong,controls_navbarrightlong_action`
- VALUE_DOMAIN: action picker; stored as controls_navbarrightlong_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_navbarrightlong_action`. The visible `controls_navbarrightlong` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_navbarrightlong: companion persisted key controls_navbarrightlong_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarrightlong_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_navbarrightlong_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_controls_powerdt

- PROOF_ID: `PROOF_ACTION_SLOT_controls_powerdt`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_powerdt,controls_powerdt_action`
- VALUE_DOMAIN: action picker; stored as controls_powerdt_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `controls_powerdt_action`. The visible `controls_powerdt` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: controls_powerdt: companion persisted key controls_powerdt_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_powerdt_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion controls_powerdt_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_POWER_KEY_FLASH

- PROOF_ID: `PROOF_OG_POWER_KEY_FLASH`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `PowerKeyHook`
- A14_INSTALLER: `installer condition controls_powerflash`
- A14_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A14_CALLBACK_PHASE: `intercept,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `PowerKeyHook`
- A13_INSTALLER: `installers/SystemServerInstaller.java if controls_powerflash`
- A13_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `controls_powerflash,controls_powerflash_delay`
- VALUE_DOMAIN: boolean; default false
- DEFAULT_SEMANTICS: off keeps ROM power-key behavior
- RESULT/ARGUMENT_BEHAVIOR: When the screen is off, KEYCODE_POWER down starts a long-press timer; long-press toggles torch and holds a wake lock. Short press wakes and turns torch off. controls_powerflash_delay triples ViewConfiguration.getLongPressTimeout when true. Volume-down torch is a different installer key.
- API33_VARIANT_REASON: A14 intercept: ACTION_DOWN skip with result 0 and never proceeds; ACTION_UP skip 0 after wake/torch-off. A13 before: returnAndSkip(0) on the same paths. A14 registers the SCREEN_ON receiver through registerModuleReceiver; A13 uses Context.registerReceiver with explicit unregister of the previous owner.
- DIFF_SUMMARY: Shared: PhoneWindowManager.init SCREEN_ON receiver; MiuiPhoneWindowManager.interceptKeyBeforeQueueing KEYCODE_POWER; FLAG_VIRTUAL_HARD_KEY / FLAG_FROM_SYSTEM filters; isInteractive early return; controls_powerflash_delay long-press timeout. Differ: A14 intercept skip-0 vs A13 before returnAndSkip(0); A14 guarded inline long-press runnable vs A13 mPowerLongPressRunnable; A14 registerModuleReceiver vs A13 registerReceiver.
- VALUE_DEFAULT_COMPARISON: controls_powerflash boolean installer gate; controls_powerflash_delay boolean default false on both trees.
- HOOK_TARGET_COMPARISON: PhoneWindowManager.init and MiuiPhoneWindowManager.interceptKeyBeforeQueueing on both trees.
- CALLBACK_SEMANTICS_COMPARISON: A14 intercept skip-or-proceed vs A13 before returnAndSkip; init side-effect is after-equivalent (A14 proceeds once then registers).
- ARG_RESULT_COMPARISON: Skipped queue result is 0; KeyEvent is not rewritten. Torch/wake are side-effects.
- A14_ONLY_BRANCHES: receiver helper name torchScreenOnReceiver; no extra user-visible flashlight mode; does not consume fingerprint/toast/vibration keys
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: User contract is power-key flashlight while the screen is off, with an optional longer delay. Same filters, same torch/wake side-effects, same delay key.
- KEY_OWNERSHIP_EVIDENCE: controls_powerflash: INSTALLER_CALLEE → PowerKeyHook; controls_powerflash_delay: LITERAL_READ inside PowerKeyHook. controls_volumedowndt_torch is not consumed here.
- A14_KEY_OWNER_REFERENCE: Controls.kt::PowerKeyHook INSTALLER_CALLEE controls_powerflash; LITERAL_READ controls_powerflash_delay
- A13_KEY_OWNER_REFERENCE: Controls.kt::PowerKeyHook INSTALLER_CALLEE via SystemServerInstaller.java; LITERAL_READ controls_powerflash_delay
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_LAUNCHER_DOCK_HEIGHT

- PROOF_ID: `PROOF_LAUNCHER_DOCK_HEIGHT`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: Same DeviceConfig.calcHotSeatsHeight member on MIUI Home.
- VALUE_DEFAULT_COMPARISON: <=60 keeps ROM hotseat height
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight; A13=com.miui.home.launcher.DeviceConfig#calcHotSeatsHeight
- CALLBACK_SEMANTICS_COMPARISON: A14=before; A13=before
- ARG_RESULT_COMPARISON: before-hook returnAndSkip dp2px(dockHeight)
- A14_ONLY_BRANCHES: Same DeviceConfig.calcHotSeatsHeight member on MIUI Home.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: before-hook returnAndSkip dp2px(dockHeight). Same DeviceConfig.calcHotSeatsHeight member on MIUI Home.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_doubletap

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_doubletap`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_doubletap,launcher_doubletap_action`
- VALUE_DOMAIN: action picker; stored as launcher_doubletap_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_doubletap_action`. The visible `launcher_doubletap` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_doubletap: companion persisted key launcher_doubletap_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_doubletap_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_doubletap_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FOLDER_BLUR_DISABLE

- PROOF_ID: `PROOF_FOLDER_BLUR_DISABLE`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: A13 FolderBlurHook gained the A14 disable flag without replacing opacity storage.
- VALUE_DEFAULT_COMPARISON: disable=false uses opacity overlay; disable=true forces clear background
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.common.BlurUtils#getLauncherBlur; FolderCling#open; A13=com.miui.home.launcher.common.BlurUtils#getLauncherBlur; FolderCling#open
- CALLBACK_SEMANTICS_COMPARISON: A14=before/after; A13=before/after
- ARG_RESULT_COMPARISON: resolveFolderBlurRatio(disable, opacity) skipped into getLauncherBlur
- A14_ONLY_BRANCHES: A13 FolderBlurHook gained the A14 disable flag without replacing opacity storage.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: resolveFolderBlurRatio(disable, opacity) skipped into getLauncherBlur. A13 FolderBlurHook gained the A14 disable flag without replacing opacity storage.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_Launcher_kt_onStopTrackingTouch

- PROOF_ID: `PROOF_FP_Launcher_kt_onStopTrackingTouch`
- BODY_RELATION: `IDENTICAL`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A14_SYMBOL: `onStopTrackingTouch`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A13_SYMBOL: `onStopTrackingTouch`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_folderwidth`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: shared defaults (no explicit default literal)
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals
- API33_VARIANT_REASON: Normalized owner bodies are identical; same relevant keys; compatible installer ownership.
- KEY_OWNERSHIP_EVIDENCE: launcher_folderwidth: LITERAL_READ in both owner bodies
- A14_KEY_OWNER_REFERENCE: app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt::onStopTrackingTouch LITERAL_READ launcher_folderwidth
- A13_KEY_OWNER_REFERENCE: app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt::onStopTrackingTouch LITERAL_READ launcher_folderwidth
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_ACTION_SLOT_launcher_pinch

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_pinch`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_pinch,launcher_pinch_action`
- VALUE_DOMAIN: action picker; stored as launcher_pinch_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_pinch_action`. The visible `launcher_pinch` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_pinch: companion persisted key launcher_pinch_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_pinch_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_pinch_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_shake

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_shake`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_shake,launcher_shake_action`
- VALUE_DOMAIN: action picker; stored as launcher_shake_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_shake_action`. The visible `launcher_shake` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_shake: companion persisted key launcher_shake_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_shake_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_shake_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_spread

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_spread`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_spread,launcher_spread_action`
- VALUE_DOMAIN: action picker; stored as launcher_spread_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_spread_action`. The visible `launcher_spread` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_spread: companion persisted key launcher_spread_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_spread_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_spread_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_swipedown

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_swipedown`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_swipedown,launcher_swipedown_action`
- VALUE_DOMAIN: action picker; stored as launcher_swipedown_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_swipedown_action`. The visible `launcher_swipedown` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_swipedown: companion persisted key launcher_swipedown_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipedown_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipedown_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_swipedown2

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_swipedown2`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_swipedown2,launcher_swipedown2_action`
- VALUE_DOMAIN: action picker; stored as launcher_swipedown2_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_swipedown2_action`. The visible `launcher_swipedown2` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_swipedown2: companion persisted key launcher_swipedown2_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipedown2_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipedown2_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_swipeleft

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_swipeleft`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_swipeleft,launcher_swipeleft_action`
- VALUE_DOMAIN: action picker; stored as launcher_swipeleft_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_swipeleft_action`. The visible `launcher_swipeleft` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_swipeleft: companion persisted key launcher_swipeleft_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipeleft_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipeleft_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_swiperight

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_swiperight`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_swiperight,launcher_swiperight_action`
- VALUE_DOMAIN: action picker; stored as launcher_swiperight_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_swiperight_action`. The visible `launcher_swiperight` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_swiperight: companion persisted key launcher_swiperight_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swiperight_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swiperight_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_swipeup

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_swipeup`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_swipeup,launcher_swipeup_action`
- VALUE_DOMAIN: action picker; stored as launcher_swipeup_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_swipeup_action`. The visible `launcher_swipeup` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_swipeup: companion persisted key launcher_swipeup_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipeup_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipeup_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_ACTION_SLOT_launcher_swipeup2

- PROOF_ID: `PROOF_ACTION_SLOT_launcher_swipeup2`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `mods/utils/GlobalActionConfig.kt / action picker`
- A14_SYMBOL: `handleAction/handleNavBarAction`
- A14_INSTALLER: `SystemUiInstaller / LauncherInstaller / SystemServerInstaller`
- A14_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt`
- A13_SYMBOL: `handleAction/handleNavBarAction`
- A13_INSTALLER: `installers/*Installer.java`
- A13_HOOK_TARGETS: `(no ROM member; GlobalActions dispatcher)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_swipeup2,launcher_swipeup2_action`
- VALUE_DOMAIN: action picker; stored as launcher_swipeup2_action
- DEFAULT_SEMANTICS: action=1 keeps ROM default
- RESULT/ARGUMENT_BEHAVIOR: UI key opens the action picker; the int in _action selects handleAction
- API33_VARIANT_REASON: A13 and A14 share the visible picker row plus the companion _action int domain.
- DIFF_SUMMARY: Both trees persist the selected action id in `launcher_swipeup2_action`. The visible `launcher_swipeup2` row is the picker, not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees.
- VALUE_DEFAULT_COMPARISON: Both default the stored action id to 1 (keep ROM handler).
- HOOK_TARGET_COMPARISON: No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the picker row; click opens the action selector.
- ARG_RESULT_COMPARISON: No setResult on this row. The stored int is later dispatched by handleAction.
- A14_ONLY_BRANCHES: none for the slot row itself
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user configures the same action picker; the companion _action integer is consumed by the shared GlobalActions dispatcher on both trees.
- KEY_OWNERSHIP_EVIDENCE: launcher_swipeup2: companion persisted key launcher_swipeup2_action is read by handleAction/handleNavBarAction on both trees
- A14_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipeup2_action
- A13_KEY_OWNER_REFERENCE: GlobalActions handleAction companion launcher_swipeup2_action
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_MIUIZER_LOCALE

- PROOF_ID: `PROOF_MIUIZER_LOCALE`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/AppLocaleController.kt`
- A14_SYMBOL: `AppLocaleController`
- A14_INSTALLER: `Settings app / MainApplication apply()`
- A14_HOOK_TARGETS: `(settings app, no host hook)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/AppLocaleController.kt`
- A13_SYMBOL: `AppLocaleController`
- A13_INSTALLER: `AboutFragment.setupLocalePreference + MainApplication.apply()`
- A13_HOOK_TARGETS: `(settings app, no host hook)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `miuizer_locale`
- VALUE_DOMAIN: locale tag: auto|en|zh-CN|zh-TW|ru-RU|ja-JP|vi-VN|cs-CZ|pt-BR|tr-TR|es-ES
- DEFAULT_SEMANTICS: default `auto`; unknown/legacy `1` normalize to auto
- RESULT/ARGUMENT_BEHAVIOR: Persists pref_key_miuizer_locale; apply() writes LocaleManager.applicationLocales or clears on auto; pref_key_miuizer_locale_applied is a derived fast-path marker, not a second user setting
- API33_VARIANT_REASON: Both trees own AppLocaleController on API33 LocaleManager. A13 ListPreferenceEx lives on About; A14 row is on prefs_main.xml. Screen placement does not change the persisted tag or apply() contract.
- DIFF_SUMMARY: Shared: same LOCALE_PREF_KEY / APPLIED_LOCALE_PREF_KEY, same SUPPORTED_LOCALE_TAGS, auto fast-path, LocaleManager.applicationLocales. A14 adds setUserLocale commit rollback, Locale.setDefault, AppLocaleGateway test seam, FatalErrors. A13 keeps optional Context apply(), applicationLocaleApplier/Provider hooks, getLocaleContext no-op.
- VALUE_DEFAULT_COMPARISON: Both default getString(pref_key_miuizer_locale, auto) and normalize unknown tags to auto.
- HOOK_TARGET_COMPARISON: Neither side hooks SystemUI/Home; this is module Settings/app locale only.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback. Change is persist + process restart / next apply().
- ARG_RESULT_COMPARISON: No host setResult. Framework write is LocaleManager.applicationLocales = tag list or empty for auto.
- A14_ONLY_BRANCHES: setUserLocale rollback on failed commit; Locale.setDefault; AppLocaleGateway.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: The user-visible control is the same language list persisted in pref_key_miuizer_locale and applied through Android 13 LocaleManager. No SystemUI/Home dump is required to decide this row.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_CHARGINGINFO_FONTSIZE

- PROOF_ID: `PROOF_CHARGINGINFO_FONTSIZE`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: Same KeyguardIndicationTextView constructor hook; fontsize is an A13-owned suboption.
- VALUE_DEFAULT_COMPARISON: default keeps system text size
- HOOK_TARGET_COMPARISON: A14=KeyguardIndicationTextView#<init>; A13=com.android.systemui.statusbar.phone.KeyguardIndicationTextView#<init>
- CALLBACK_SEMANTICS_COMPARISON: A14=after; A13=after
- ARG_RESULT_COMPARISON: setTextSize(COMPLEX_UNIT_SP) when resolveChargingInfoFontSizeSp non-null
- A14_ONLY_BRANCHES: Same KeyguardIndicationTextView constructor hook; fontsize is an A13-owned suboption.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: setTextSize(COMPLEX_UNIT_SP) when resolveChargingInfoFontSizeSp non-null. Same KeyguardIndicationTextView constructor hook; fontsize is an A13-owned suboption.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_NETSPEED_BOLDFONT_RENAME

- PROOF_ID: `PROOF_NETSPEED_BOLDFONT_RENAME`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: Capability-preserving key rename; not a second netspeed product.
- VALUE_DEFAULT_COMPARISON: false = stock weight
- HOOK_TARGET_COMPARISON: A14=network speed text typeface path; A13=network speed text typeface path
- CALLBACK_SEMANTICS_COMPARISON: A14=after; A13=after
- ARG_RESULT_COMPARISON: A13 key system_netspeed_bold drives the same typeface helper as A14 boldfont
- A14_ONLY_BRANCHES: Capability-preserving key rename; not a second netspeed product.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: A13 key system_netspeed_bold drives the same typeface helper as A14 boldfont. Capability-preserving key rename; not a second netspeed product.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_NETSPEED_CLOCK_STYLE

- PROOF_ID: `PROOF_NETSPEED_CLOCK_STYLE`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: A13 NetSpeedTypefaceHelper gained clock-style copy; same status-bar meter owner.
- VALUE_DEFAULT_COMPARISON: false keeps netspeed typeface; true copies status-bar clock appearance
- HOOK_TARGET_COMPARISON: A14=status bar NetworkSpeedView / meter style path; A13=status bar NetworkSpeedView / meter style path
- CALLBACK_SEMANTICS_COMPARISON: A14=after; A13=after
- ARG_RESULT_COMPARISON: applyStatusBarClockTextAppearance on netspeed text views
- A14_ONLY_BRANCHES: A13 NetSpeedTypefaceHelper gained clock-style copy; same status-bar meter owner.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: applyStatusBarClockTextAppearance on netspeed text views. A13 NetSpeedTypefaceHelper gained clock-style copy; same status-bar meter owner.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_FP_SystemLockScreenMoreHooks_kt_isAuthOnce

- PROOF_ID: `PROOF_FP_SystemLockScreenMoreHooks_kt_isAuthOnce`
- BODY_RELATION: `IDENTICAL`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `isAuthOnce`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `isAuthOnce`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_noscreenlock_req`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: shared defaults 1
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals
- API33_VARIANT_REASON: Normalized owner bodies are identical; same relevant keys; compatible installer ownership.
- KEY_OWNERSHIP_EVIDENCE: system_noscreenlock_req: LITERAL_READ in both owner bodies
- A14_KEY_OWNER_REFERENCE: app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt::isAuthOnce LITERAL_READ system_noscreenlock_req
- A13_KEY_OWNER_REFERENCE: app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt::isAuthOnce LITERAL_READ system_noscreenlock_req
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_DUALROWS_LEFT_RATIO

- PROOF_ID: `PROOF_DUALROWS_LEFT_RATIO`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: Same MiuiPhoneStatusBarView.updateCutoutLocation owner on A13 SystemUI.
- VALUE_DEFAULT_COMPARISON: default split; custom ratio on no-cutout type 0
- HOOK_TARGET_COMPARISON: A14=MiuiPhoneStatusBarView#updateCutoutLocation; A13=com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation
- CALLBACK_SEMANTICS_COMPARISON: A14=after; A13=after
- ARG_RESULT_COMPARISON: left/right LinearLayout weights from resolveDualRowsCutoutWeights
- A14_ONLY_BRANCHES: Same MiuiPhoneStatusBarView.updateCutoutLocation owner on A13 SystemUI.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: left/right LinearLayout weights from resolveDualRowsCutoutWeights. Same MiuiPhoneStatusBarView.updateCutoutLocation owner on A13 SystemUI.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_BT_ICON_ALWAYS_HIDE

- PROOF_ID: `PROOF_BT_ICON_ALWAYS_HIDE`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: A13 already exposes always-hide as bluetooth=3; A14 split a dedicated key.
- VALUE_DEFAULT_COMPARISON: stock bluetooth icon visible
- HOOK_TARGET_COMPARISON: A14=bluetooth status-bar icon visibility; A13=bluetooth status-bar icon visibility
- CALLBACK_SEMANTICS_COMPARISON: A14=before; A13=before
- ARG_RESULT_COMPARISON: A13 option 3 forces bluetooth icon visibility false
- A14_ONLY_BRANCHES: A13 already exposes always-hide as bluetooth=3; A14 split a dedicated key.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: A13 option 3 forces bluetooth icon visibility false. A13 already exposes always-hide as bluetooth=3; A14 split a dedicated key.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_WIRELESS_HEADSET_SLOT

- PROOF_ID: `PROOF_WIRELESS_HEADSET_SLOT`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: A13 hide-icons path already owned headset; wireless_headset is an extra slot name on the same function.
- VALUE_DEFAULT_COMPARISON: false keeps wireless_headset slot
- HOOK_TARGET_COMPARISON: A14=status bar icon slot hide path; A13=status bar icon slot hide path
- CALLBACK_SEMANTICS_COMPARISON: A14=before; A13=before
- ARG_RESULT_COMPARISON: checkSlot('wireless_headset') hides the slot when enabled
- A14_ONLY_BRANCHES: A13 hide-icons path already owned headset; wireless_headset is an extra slot name on the same function.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: checkSlot('wireless_headset') hides the slot when enabled. A13 hide-icons path already owned headset; wireless_headset is an extra slot name on the same function.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_USB_DEFAULT_R1_LATCH

- PROOF_ID: `PROOF_USB_DEFAULT_R1_LATCH`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: A14 HAL setEnabledFunctions(JZI) is not copied; A13 owns setCurrentFunction + R1 latch.
- VALUE_DEFAULT_COMPARISON: none = follow system; unsecure latch ignored when none
- HOOK_TARGET_COMPARISON: A14=UsbDeviceManager / HAL setEnabledFunctions; A13=UsbDeviceManager.setCurrentFunction; UsbConnectLatch rising-edge
- CALLBACK_SEMANTICS_COMPARISON: A14=after/intercept; A13=after
- ARG_RESULT_COMPARISON: A13 setCurrentFunction; disconnect clears UsbConnectLatch
- A14_ONLY_BRANCHES: A14 HAL setEnabledFunctions(JZI) is not copied; A13 owns setCurrentFunction + R1 latch.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: A13 setCurrentFunction; disconnect clears UsbConnectLatch. A14 HAL setEnabledFunctions(JZI) is not copied; A13 owns setCurrentFunction + R1 latch.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_HIDE_REPORT

- PROOF_ID: `PROOF_HIDE_REPORT`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: Same ApplicationsDetailsActivity menu owner; A13 SecurityCenter installer.
- VALUE_DEFAULT_COMPARISON: false keeps report menu item
- HOOK_TARGET_COMPARISON: A14=com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu; A13=com.miui.appmanager.ApplicationsDetailsActivity#onCreateOptionsMenu
- CALLBACK_SEMANTICS_COMPARISON: A14=after; A13=after
- ARG_RESULT_COMPARISON: after onCreateOptionsMenu, itemId 4 setVisible(false)
- A14_ONLY_BRANCHES: Same ApplicationsDetailsActivity menu owner; A13 SecurityCenter installer.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: after onCreateOptionsMenu, itemId 4 setVisible(false). Same ApplicationsDetailsActivity menu owner; A13 SecurityCenter installer.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_PACKAGEINSTALLER_PURIFY

- PROOF_ID: `PROOF_PACKAGEINSTALLER_PURIFY`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: Same Various.PurePackageInstallerHook; A13 router vs A14 FeatureSpec.
- VALUE_DEFAULT_COMPARISON: false keeps installer ads/recommend/verify UI
- HOOK_TARGET_COMPARISON: A14=MIUI package installer preference/settings members; A13=MIUI package installer preference/settings members
- CALLBACK_SEMANTICS_COMPARISON: A14=before/after; A13=before/after
- ARG_RESULT_COMPARISON: purifiedInstallerBoolean/SystemInt/SecureInt rewrite installer prefs
- A14_ONLY_BRANCHES: Same Various.PurePackageInstallerHook; A13 router vs A14 FeatureSpec.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: purifiedInstallerBoolean/SystemInt/SecureInt rewrite installer prefs. Same Various.PurePackageInstallerHook; A13 router vs A14 FeatureSpec.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_BACKUP_V2

- PROOF_ID: `PROOF_BACKUP_V2`
- BODY_RELATION: `REVIEWED_VARIANT`
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
- DIFF_SUMMARY: A13 V2 contract matches A14 M2 typed backup; no API34 host types.
- VALUE_DEFAULT_COMPARISON: encode V2; restore auto-detects V2 vs legacy
- HOOK_TARGET_COMPARISON: A14=(settings app, no host hook); A13=(settings app, no host hook)
- CALLBACK_SEMANTICS_COMPARISON: A14=n/a; A13=n/a
- ARG_RESULT_COMPARISON: CRC/size bounds; rollback on commit failure
- A14_ONLY_BRANCHES: A13 V2 contract matches A14 M2 typed backup; no API34 host types.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: CRC/size bounds; rollback on commit failure. A13 V2 contract matches A14 M2 typed backup; no API34 host types.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`
