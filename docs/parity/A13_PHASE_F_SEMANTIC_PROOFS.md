# A13 Phase F-R3 Semantic Proofs

Automatic PRESENT requires normalized body IDENTICAL, the same relevant preference keys,
and compatible installer ownership (BODY_RELATION=IDENTICAL).
Non-identical owners require an owner-group reviewed manifest (BODY_RELATION=REVIEWED_VARIANT)
with filled difference fields. SequenceMatcher ratio never authorizes PRESENT.
Same-key reads alone are IMPLEMENTATION_PRESENCE.

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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_NoFingerprintWakeHook__NoFingerprintWakeHook

- PROOF_ID: `PROOF_OG_Controls_kt_NoFingerprintWakeHook__NoFingerprintWakeHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `NoFingerprintWakeHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `NoFingerprintWakeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_fingerprintfailure,controls_fingerprintscreen,controls_fingerprintwake,controls_powerflash,controls_volumedowndt_torch`
- VALUE_DOMAIN: owner-group keys for NoFingerprintWakeHook: controls_fingerprintfailure,controls_fingerprintscreen,controls_fingerprintwake,controls_powerflash,controls_volumedowndt_torch
- DEFAULT_SEMANTICS: `controls_fingerprintfailure` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Controls.kt::NoFingerprintWakeHook` / `Controls.kt::NoFingerprintWakeHook`: A13 already implements the exclusive keys controls_fingerprintfailure,controls_fingerprintscreen,controls_fingerprintwake,controls_powerflash,controls_volumedowndt_torch. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::NoFingerprintWakeHook` (hook, phases=intercept) vs A13 `Controls.kt::NoFingerprintWakeHook` (hook, phases=before). Shared methods=['processBackFingerprintDpcenterEvent']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_fingerprintfailure` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent; A13=com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent; shared_methods=['processBackFingerprintDpcenterEvent']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::NoFingerprintWakeHook` / `Controls.kt::NoFingerprintWakeHook`: A13 already implements the exclusive keys controls_fingerprintfailure,controls_fingerprintscreen,controls_fingerprintwake,controls_powerflash,controls_volumedowndt_torch. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_FingerprintHapticSuccessHook__FingerprintHapticSuccessHook

- PROOF_ID: `PROOF_OG_Controls_kt_FingerprintHapticSuccessHook__FingerprintHapticSuccessHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `FingerprintHapticSuccessHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `FingerprintHapticSuccessHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_fingerprintsuccess,system_blocktoasts,system_nolightuponcharges,system_vibration`
- VALUE_DOMAIN: owner-group keys for FingerprintHapticSuccessHook: controls_fingerprintsuccess,system_blocktoasts,system_nolightuponcharges,system_vibration
- DEFAULT_SEMANTICS: `controls_fingerprintsuccess` A14 default="1"; A13 default="1"
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Controls.kt::FingerprintHapticSuccessHook` / `Controls.kt::FingerprintHapticSuccessHook`: A13 already implements the exclusive keys controls_fingerprintsuccess,system_blocktoasts,system_nolightuponcharges,system_vibration. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::FingerprintHapticSuccessHook` (hook, phases=intercept) vs A13 `Controls.kt::FingerprintHapticSuccessHook` (hook, phases=after). Shared methods=['onAuthenticated']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_fingerprintsuccess` A14="1" A13="1". No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated; A13=com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated; shared_methods=['onAuthenticated']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::FingerprintHapticSuccessHook` / `Controls.kt::FingerprintHapticSuccessHook`: A13 already implements the exclusive keys controls_fingerprintsuccess,system_blocktoasts,system_nolightuponcharges,system_vibration. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_FingerprintScreenOnHook__FingerprintScreenOnHook

- PROOF_ID: `PROOF_OG_Controls_kt_FingerprintScreenOnHook__FingerprintScreenOnHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `FingerprintScreenOnHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `FingerprintScreenOnHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_fingerprintsuccess_ignore`
- VALUE_DOMAIN: owner-group keys for FingerprintScreenOnHook: controls_fingerprintsuccess_ignore
- DEFAULT_SEMANTICS: `controls_fingerprintsuccess_ignore` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Controls.kt::FingerprintScreenOnHook` / `Controls.kt::FingerprintScreenOnHook`: A13 already implements the exclusive keys controls_fingerprintsuccess_ignore. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::FingerprintScreenOnHook` (hook, phases=intercept) vs A13 `Controls.kt::FingerprintScreenOnHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_fingerprintsuccess_ignore` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::FingerprintScreenOnHook` / `Controls.kt::FingerprintScreenOnHook`: A13 already implements the exclusive keys controls_fingerprintsuccess_ignore. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_BackGestureAreaHeightHook__BackGestureAreaHeightHook

- PROOF_ID: `PROOF_OG_Controls_kt_BackGestureAreaHeightHook__BackGestureAreaHeightHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `BackGestureAreaHeightHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#getGestureStubWindowParam`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `BackGestureAreaHeightHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#getGestureStubWindowParam`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `controls_fsg_coverage,system_recents_blur`
- VALUE_DOMAIN: owner-group keys for BackGestureAreaHeightHook: controls_fsg_coverage,system_recents_blur
- DEFAULT_SEMANTICS: `controls_fsg_coverage` A14 default=60; A13 default=60
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 setResult[lp]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Controls.kt::BackGestureAreaHeightHook` / `Controls.kt::BackGestureAreaHeightHook`: A13 already implements the exclusive keys controls_fsg_coverage,system_recents_blur. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::BackGestureAreaHeightHook` (hook, phases=intercept) vs A13 `Controls.kt::BackGestureAreaHeightHook` (hook, phases=after). Shared methods=['getGestureStubWindowParam']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_fsg_coverage` A14=60 A13=60. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.recents.GestureStubView#getGestureStubWindowParam; A13=com.miui.home.recents.GestureStubView#getGestureStubWindowParam; shared_methods=['getGestureStubWindowParam']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 setResult[lp]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::BackGestureAreaHeightHook` / `Controls.kt::BackGestureAreaHeightHook`: A13 already implements the exclusive keys controls_fsg_coverage,system_recents_blur. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherGestureHooks_kt_SwipeAndStopActionHook__SwipeAndStopActionHook

- PROOF_ID: `PROOF_OG_LauncherGestureHooks_kt_SwipeAndStopActionHook__SwipeAndStopActionHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A14_SYMBOL: `SwipeAndStopActionHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.recents.GestureBackArrowView#setReadyFinish,com.miui.home.recents.GestureStubView\$3#onSwipeStop,com.miui.home.recents.GestureStubView#getNextTask,#disableQuickSwitch,#isDisableQuickSwitch`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A13_SYMBOL: `SwipeAndStopActionHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt`
- A13_HOOK_TARGETS: `com.miui.home.recents.GestureBackArrowView#setReadyFinish,com.miui.home.recents.GestureStubView#disableQuickSwitch,com.miui.home.recents.GestureStubView#isDisableQuickSwitch,com.miui.home.recents.GestureStubView#getNextTask,#vibrate`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `controls_fsg_swipeandstop,controls_fsg_swipeandstop_disablevibrate,launcher_closedrawer,launcher_horizwidgetmargin`
- VALUE_DOMAIN: owner-group keys for SwipeAndStopActionHook: controls_fsg_swipeandstop,controls_fsg_swipeandstop_disablevibrate,launcher_closedrawer,launcher_horizwidgetmargin
- DEFAULT_SEMANTICS: `controls_fsg_swipeandstop` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 chain.proceed[chain.proceed()]; A13 returnAndSkip[XposedHelpers.newInstance(task,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before,after`. Owner-group review of `LauncherGestureHooks.kt::SwipeAndStopActionHook` / `LauncherGestureHooks.kt::SwipeAndStopActionHook`: A13 already implements the exclusive keys controls_fsg_swipeandstop,controls_fsg_swipeandstop_disablevibrate,launcher_closedrawer,launcher_horizwidgetmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherGestureHooks.kt::SwipeAndStopActionHook` (hook, phases=intercept) vs A13 `LauncherGestureHooks.kt::SwipeAndStopActionHook` (hook, phases=before,after). Shared methods=['disableQuickSwitch', 'getNextTask', 'isDisableQuickSwitch', 'setReadyFinish']; A14-only members=['#disableQuickSwitch', '#isDisableQuickSwitch', 'com.miui.home.recents.GestureStubView\\$3#onSwipeStop']; A13-only members=['#vibrate', 'com.miui.home.recents.GestureStubView#disableQuickSwitch', 'com.miui.home.recents.GestureStubView#isDisableQuickSwitch'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_fsg_swipeandstop` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.recents.GestureBackArrowView#setReadyFinish,com.miui.home.recents.GestureStubView\$3#onSwipeStop,com.miui.home.recents.GestureStubView#getNextTask,#disableQuickSwitch,#isDisableQuickSwitch; A13=com.miui.home.recents.GestureBackArrowView#setReadyFinish,com.miui.home.recents.GestureStubView#disableQuickSwitch,com.miui.home.recents.GestureStubView#isDisableQuickSwitch,com.miui.home.recents.GestureStubView#getNextTask,#vibrate; shared_methods=['disableQuickSwitch', 'getNextTask', 'isDisableQuickSwitch', 'setReadyFinish']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 chain.proceed[chain.proceed()]; A13 returnAndSkip[XposedHelpers.newInstance(task,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#disableQuickSwitch', '#isDisableQuickSwitch', 'com.miui.home.recents.GestureStubView\\$3#onSwipeStop']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherGestureHooks.kt::SwipeAndStopActionHook` / `LauncherGestureHooks.kt::SwipeAndStopActionHook`: A13 already implements the exclusive keys controls_fsg_swipeandstop,controls_fsg_swipeandstop_disablevibrate,launcher_closedrawer,launcher_horizwidgetmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_HideNavBarHook__HideNavBarHook

- PROOF_ID: `PROOF_OG_Controls_kt_HideNavBarHook__HideNavBarHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `HideNavBarHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `com.android.systemui.navigationbar.NavigationBarController#createNavigationBar,#setWindowState,com.android.systemui.recents.OverviewProxyService#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `HideNavBarHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NavigationModeControllerExt#hideNavigationBar,com.android.systemui.navigationbar.NavigationBarController#createNavigationBar,com.android.systemui.statusbar.phone.MiuiDockIndicatorService#onNavigationModeChanged`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_fsg_width,controls_nonavbar`
- VALUE_DOMAIN: owner-group keys for HideNavBarHook: controls_fsg_width,controls_nonavbar
- DEFAULT_SEMANTICS: `controls_fsg_width` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Controls.kt::HideNavBarHook` / `Controls.kt::HideNavBarHook`: A13 already implements the exclusive keys controls_fsg_width,controls_nonavbar. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::HideNavBarHook` (hook, phases=intercept) vs A13 `Controls.kt::HideNavBarHook` (hook, phases=before). Shared methods=['createNavigationBar']; A14-only members=['#setWindowState', 'com.android.systemui.recents.OverviewProxyService#<init>']; A13-only members=['com.android.systemui.statusbar.phone.MiuiDockIndicatorService#onNavigationModeChanged', 'com.android.systemui.statusbar.phone.NavigationModeControllerExt#hideNavigationBar'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_fsg_width` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.navigationbar.NavigationBarController#createNavigationBar,#setWindowState,com.android.systemui.recents.OverviewProxyService#<init>; A13=com.android.systemui.statusbar.phone.NavigationModeControllerExt#hideNavigationBar,com.android.systemui.navigationbar.NavigationBarController#createNavigationBar,com.android.systemui.statusbar.phone.MiuiDockIndicatorService#onNavigationModeChanged; shared_methods=['createNavigationBar']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#setWindowState', 'com.android.systemui.recents.OverviewProxyService#<init>']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::HideNavBarHook` / `Controls.kt::HideNavBarHook`: A13 already implements the exclusive keys controls_fsg_width,controls_nonavbar. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_AudioVisualizerHook__AudioVisualizerHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_AudioVisualizerHook__AudioVisualizerHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `AudioVisualizerHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.shade.MiuiNotificationPanelViewController#onViewAttachedToWindow,com.android.systemui.statusbar.phone.CentralSurfacesImpl#start,com.android.systemui.statusbar.phone.CentralSurfacesImpl#updateDozingState,com.android.systemui.statusbar.policy.KeyguardStateControllerImpl#notifyKeyguardState,com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,#onScreenTurnedOff,#onScreenTurnedOn`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `AudioVisualizerHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOff,com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOn,com.android.systemui.statusbar.policy.KeyguardStateControllerImpl#notifyKeyguardState,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `controls_hidenavbar_whenscreenshot,system_visualizer`
- VALUE_DOMAIN: owner-group keys for AudioVisualizerHook: controls_hidenavbar_whenscreenshot,system_visualizer
- DEFAULT_SEMANTICS: `controls_hidenavbar_whenscreenshot` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemAudioHooks.kt::AudioVisualizerHook` / `SystemAudioAndVisualAndMoreHooks.kt::AudioVisualizerHook`: A13 already implements the exclusive keys controls_hidenavbar_whenscreenshot,system_visualizer. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemAudioHooks.kt::AudioVisualizerHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::AudioVisualizerHook` (hook, phases=after,before). Shared methods=['notifyKeyguardState', 'onScreenTurnedOff', 'onScreenTurnedOn', 'updateMediaMetaData']; A14-only members=['#onScreenTurnedOff', '#onScreenTurnedOn', 'com.android.systemui.shade.MiuiNotificationPanelViewController#onViewAttachedToWindow', 'com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded', 'com.android.systemui.statusbar.phone.CentralSurfacesImpl#start', 'com.android.systemui.statusbar.phone.CentralSurfacesImpl#updateDozingState']; A13-only members=['com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOff', 'com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOn'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_hidenavbar_whenscreenshot` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.shade.MiuiNotificationPanelViewController#onViewAttachedToWindow,com.android.systemui.statusbar.phone.CentralSurfacesImpl#start,com.android.systemui.statusbar.phone.CentralSurfacesImpl#updateDozingState,com.android.systemui.statusbar.policy.KeyguardStateControllerImpl#notifyKeyguardState,com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,#onScreenTurnedOff,#onScreenTurnedOn; A13=com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOff,com.android.systemui.statusbar.phone.ScrimController#onScreenTurnedOn,com.android.systemui.statusbar.policy.KeyguardStateControllerImpl#notifyKeyguardState,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData; shared_methods=['notifyKeyguardState', 'onScreenTurnedOff', 'onScreenTurnedOn', 'updateMediaMetaData']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#onScreenTurnedOff', '#onScreenTurnedOn', 'com.android.systemui.shade.MiuiNotificationPanelViewController#onViewAttachedToWindow', 'com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded', 'com.android.systemui.statusbar.phone.CentralSurfacesImpl#start', 'com.android.systemui.statusbar.phone.CentralSurfacesImpl#updateDozingState']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemAudioHooks.kt::AudioVisualizerHook` / `SystemAudioAndVisualAndMoreHooks.kt::AudioVisualizerHook`: A13 already implements the exclusive keys controls_hidenavbar_whenscreenshot,system_visualizer. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemServerInstaller_java_needGlobalActions__installHook

- PROOF_ID: `PROOF_OG_SystemServerInstaller_java_needGlobalActions__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/GenericAppFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/GenericAppFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_SYMBOL: `needGlobalActions`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `controls_mediaplayer_apps`
- VALUE_DOMAIN: owner-group keys for needGlobalActions: controls_mediaplayer_apps
- DEFAULT_SEMANTICS: `controls_mediaplayer_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `GenericAppFeatures.kt::installHook` / `SystemServerInstaller.java::needGlobalActions`: A13 already implements the exclusive keys controls_mediaplayer_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `GenericAppFeatures.kt::installHook` (spec, phases=unknown) vs A13 `SystemServerInstaller.java::needGlobalActions` (installer, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_mediaplayer_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `GenericAppFeatures.kt::installHook` / `SystemServerInstaller.java::needGlobalActions`: A13 already implements the exclusive keys controls_mediaplayer_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_reposNavBarButtons__reposNavBarButtons

- PROOF_ID: `PROOF_OG_Controls_kt_reposNavBarButtons__reposNavBarButtons`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `reposNavBarButtons`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `reposNavBarButtons`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `controls_navbarmargin`
- VALUE_DOMAIN: owner-group keys for reposNavBarButtons: controls_navbarmargin
- DEFAULT_SEMANTICS: `controls_navbarmargin` A14 default=0; A13 default=0
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `Controls.kt::reposNavBarButtons` / `Controls.kt::reposNavBarButtons`: A13 already implements the exclusive keys controls_navbarmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::reposNavBarButtons` (hook, phases=unknown) vs A13 `Controls.kt::reposNavBarButtons` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_navbarmargin` A14=0 A13=0. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::reposNavBarButtons` / `Controls.kt::reposNavBarButtons`: A13 already implements the exclusive keys controls_navbarmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_GboardPaddingHook__GboardPaddingHook

- PROOF_ID: `PROOF_OG_Various_kt_GboardPaddingHook__GboardPaddingHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `GboardPaddingHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `GboardPaddingHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/InputMethodInstaller.java`
- A13_HOOK_TARGETS: `#get`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_nonavbar_fix_inputmethod,various_gboardpadding_land,various_gboardpadding_port`
- VALUE_DOMAIN: owner-group keys for GboardPaddingHook: controls_nonavbar_fix_inputmethod,various_gboardpadding_land,various_gboardpadding_port
- DEFAULT_SEMANTICS: `controls_nonavbar_fix_inputmethod` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[opt.toString(,opt.toString(]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Various.kt::GboardPaddingHook` / `Various.kt::GboardPaddingHook`: A13 already implements the exclusive keys controls_nonavbar_fix_inputmethod,various_gboardpadding_land,various_gboardpadding_port. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::GboardPaddingHook` (hook, phases=intercept) vs A13 `Various.kt::GboardPaddingHook` (hook, phases=before). Shared methods=none; A14-only members=none; A13-only members=['#get'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_nonavbar_fix_inputmethod` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=#get; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[opt.toString(,opt.toString(]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::GboardPaddingHook` / `Various.kt::GboardPaddingHook`: A13 already implements the exclusive keys controls_nonavbar_fix_inputmethod,various_gboardpadding_land,various_gboardpadding_port. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_PowerKeyHook__PowerKeyHook

- PROOF_ID: `PROOF_OG_Controls_kt_PowerKeyHook__PowerKeyHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `PowerKeyHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `PowerKeyHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing`
- A13_CALLBACK_PHASE: `after,before,intercept`
- PREFERENCE_KEYS: `controls_powerflash_delay,system_lswallpaper`
- VALUE_DOMAIN: owner-group keys for PowerKeyHook: controls_powerflash_delay,system_lswallpaper
- DEFAULT_SEMANTICS: `controls_powerflash_delay` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[0,0]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before,intercept`. Owner-group review of `Controls.kt::PowerKeyHook` / `Controls.kt::PowerKeyHook`: A13 already implements the exclusive keys controls_powerflash_delay,system_lswallpaper. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::PowerKeyHook` (hook, phases=intercept) vs A13 `Controls.kt::PowerKeyHook` (hook, phases=after,before,intercept). Shared methods=['init', 'interceptKeyBeforeQueueing']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_powerflash_delay` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing; A13=com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing; shared_methods=['init', 'interceptKeyBeforeQueueing']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before,intercept. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[0,0]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::PowerKeyHook` / `Controls.kt::PowerKeyHook`: A13 already implements the exclusive keys controls_powerflash_delay,system_lswallpaper. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_XML_controls

- PROOF_ID: `PROOF_OG_XML_controls`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/res/xml/prefs_controls.xml`
- A14_SYMBOL: `CheckBoxPreferenceEx`
- A14_INSTALLER: `Settings module UI`
- A14_HOOK_TARGETS: `(xml/snapshot family; no exclusive hook symbol)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/res/xml/prefs_controls.xml`
- A13_SYMBOL: `CheckBoxPreferenceEx`
- A13_INSTALLER: `Settings module UI`
- A13_HOOK_TARGETS: `(xml/snapshot family; consumed by A13 style/hook path)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `controls_volumecursor`
- VALUE_DOMAIN: XML family prefs_controls.xml
- DEFAULT_SEMANTICS: Both trees persist `controls_volumecursor` and sibling style keys in prefs_controls.xml
- RESULT/ARGUMENT_BEHAVIOR: No opposite host setResult on this XML family; values are PrefMap style fields.
- API33_VARIANT_REASON: Owner-group review of XML family `prefs_controls.xml`: both trees persist 1 user-visible keys including `controls_volumecursor`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- DIFF_SUMMARY: A14 `CheckBoxPreferenceEx` vs A13 `CheckBoxPreferenceEx` in `prefs_controls.xml`. Literal-key scanner miss on snapshot fields is not a missing capability.
- VALUE_DEFAULT_COMPARISON: Same preference keys in `prefs_controls.xml` on both trees.
- HOOK_TARGET_COMPARISON: No exclusive SystemUI member on the XML row; consumption is the family style hook.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the XML widget; click/persist is settings-owned.
- ARG_RESULT_COMPARISON: No host setResult for the XML row itself.
- A14_ONLY_BRANCHES: none on these exclusive XML-family keys
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of XML family `prefs_controls.xml`: both trees persist 1 user-visible keys including `controls_volumecursor`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_VolumeCursorHook__VolumeCursorHook

- PROOF_ID: `PROOF_OG_Controls_kt_VolumeCursorHook__VolumeCursorHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `VolumeCursorHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `android.inputmethodservice.InputMethodService#onKeyDown,android.inputmethodservice.InputMethodService#onKeyUp`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `VolumeCursorHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_HOOK_TARGETS: `android.inputmethodservice.InputMethodService#onKeyDown,android.inputmethodservice.InputMethodService#onKeyUp`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_volumecursor_apps,controls_volumecursor_reverse`
- VALUE_DOMAIN: owner-group keys for VolumeCursorHook: controls_volumecursor_apps,controls_volumecursor_reverse
- DEFAULT_SEMANTICS: `controls_volumecursor_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[true,null,true,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[true,true]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Controls.kt::VolumeCursorHook` / `Controls.kt::VolumeCursorHook`: A13 already implements the exclusive keys controls_volumecursor_apps,controls_volumecursor_reverse. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::VolumeCursorHook` (hook, phases=intercept) vs A13 `Controls.kt::VolumeCursorHook` (hook, phases=before). Shared methods=['onKeyDown', 'onKeyUp']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_volumecursor_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=android.inputmethodservice.InputMethodService#onKeyDown,android.inputmethodservice.InputMethodService#onKeyUp; A13=android.inputmethodservice.InputMethodService#onKeyDown,android.inputmethodservice.InputMethodService#onKeyUp; shared_methods=['onKeyDown', 'onKeyUp']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[true,null,true,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[true,true]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::VolumeCursorHook` / `Controls.kt::VolumeCursorHook`: A13 already implements the exclusive keys controls_volumecursor_apps,controls_volumecursor_reverse. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Controls_kt_VolumeMediaPlayerHook__VolumeMediaPlayerHook

- PROOF_ID: `PROOF_OG_Controls_kt_VolumeMediaPlayerHook__VolumeMediaPlayerHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `VolumeMediaPlayerHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `#pause`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `VolumeMediaPlayerHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `#pause`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `controls_volumemedia_down,controls_volumemedia_up`
- VALUE_DOMAIN: owner-group keys for VolumeMediaPlayerHook: controls_volumemedia_down,controls_volumemedia_up
- DEFAULT_SEMANTICS: `controls_volumemedia_down` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Controls.kt::VolumeMediaPlayerHook` / `Controls.kt::VolumeMediaPlayerHook`: A13 already implements the exclusive keys controls_volumemedia_down,controls_volumemedia_up. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Controls.kt::VolumeMediaPlayerHook` (hook, phases=intercept) vs A13 `Controls.kt::VolumeMediaPlayerHook` (hook, phases=before). Shared methods=['pause']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_volumemedia_down` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=#pause; A13=#pause; shared_methods=['pause']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Controls.kt::VolumeMediaPlayerHook` / `Controls.kt::VolumeMediaPlayerHook`: A13 already implements the exclusive keys controls_volumemedia_down,controls_volumemedia_up. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_GlobalActions_kt_sendDownUpKeyEvent__sendDownUpKeyEvent

- PROOF_ID: `PROOF_OG_GlobalActions_kt_sendDownUpKeyEvent__sendDownUpKeyEvent`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A14_SYMBOL: `sendDownUpKeyEvent`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A13_SYMBOL: `sendDownUpKeyEvent`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `controls_volumemedia_vibrate,controls_volumemedia_vibrate_ignore`
- VALUE_DOMAIN: owner-group keys for sendDownUpKeyEvent: controls_volumemedia_vibrate,controls_volumemedia_vibrate_ignore
- DEFAULT_SEMANTICS: `controls_volumemedia_vibrate` A14 default=true; A13 default=true
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `GlobalActions.kt::sendDownUpKeyEvent` / `GlobalActions.kt::sendDownUpKeyEvent`: A13 already implements the exclusive keys controls_volumemedia_vibrate,controls_volumemedia_vibrate_ignore. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `GlobalActions.kt::sendDownUpKeyEvent` (hook, phases=unknown) vs A13 `GlobalActions.kt::sendDownUpKeyEvent` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `controls_volumemedia_vibrate` A14=true A13=true. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `GlobalActions.kt::sendDownUpKeyEvent` / `GlobalActions.kt::sendDownUpKeyEvent`: A13 already implements the exclusive keys controls_volumemedia_vibrate,controls_volumemedia_vibrate_ignore. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherFolderHooks_kt_FolderColumnsHook__FolderColumnsHook

- PROOF_ID: `PROOF_OG_LauncherFolderHooks_kt_FolderColumnsHook__FolderColumnsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `FolderColumnsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.Folder#onFinishInflate,com.miui.home.launcher.Folder#resetViewsLayoutParams,com.miui.home.launcher.Folder#onLayout`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `FolderColumnsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Folder#onFinishInflate,com.miui.home.launcher.Folder#onLayout,com.miui.home.launcher.Folder#resetViewsLayoutParams`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `launcher_closefolders,launcher_folderspace`
- VALUE_DOMAIN: owner-group keys for FolderColumnsHook: launcher_closefolders,launcher_folderspace
- DEFAULT_SEMANTICS: `launcher_closefolders` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `LauncherFolderHooks.kt::FolderColumnsHook` / `LauncherFolderHooks.kt::FolderColumnsHook`: A13 already implements the exclusive keys launcher_closefolders,launcher_folderspace. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherFolderHooks.kt::FolderColumnsHook` (hook, phases=intercept) vs A13 `LauncherFolderHooks.kt::FolderColumnsHook` (hook, phases=after,before). Shared methods=['onFinishInflate', 'onLayout', 'resetViewsLayoutParams']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_closefolders` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.Folder#onFinishInflate,com.miui.home.launcher.Folder#resetViewsLayoutParams,com.miui.home.launcher.Folder#onLayout; A13=com.miui.home.launcher.Folder#onFinishInflate,com.miui.home.launcher.Folder#onLayout,com.miui.home.launcher.Folder#resetViewsLayoutParams; shared_methods=['onFinishInflate', 'onLayout', 'resetViewsLayoutParams']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherFolderHooks.kt::FolderColumnsHook` / `LauncherFolderHooks.kt::FolderColumnsHook`: A13 already implements the exclusive keys launcher_closefolders,launcher_folderspace. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherIconHooks_kt_TitleShadowHook__TitleShadowHook

- PROOF_ID: `PROOF_OG_LauncherIconHooks_kt_TitleShadowHook__TitleShadowHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `TitleShadowHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.WallpaperUtils#getIconTitleShadowColor,com.miui.home.launcher.WallpaperUtils#getTitleShadowColor`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_SYMBOL: `TitleShadowHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.WallpaperUtils#getIconTitleShadowColor,com.miui.home.launcher.WallpaperUtils#getTitleShadowColor`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_darkershadow,launcher_renameapps,launcher_titletopmargin`
- VALUE_DOMAIN: owner-group keys for TitleShadowHook: launcher_darkershadow,launcher_renameapps,launcher_titletopmargin
- DEFAULT_SEMANTICS: `launcher_darkershadow` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 setResult[Color.argb(Math.round(Color.alpha(color,Color.argb(Math.round(Color.alpha(color]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `LauncherIconHooks.kt::TitleShadowHook` / `LauncherIconHooks.kt::TitleShadowHook`: A13 already implements the exclusive keys launcher_darkershadow,launcher_renameapps,launcher_titletopmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherIconHooks.kt::TitleShadowHook` (hook, phases=intercept) vs A13 `LauncherIconHooks.kt::TitleShadowHook` (hook, phases=after). Shared methods=['getIconTitleShadowColor', 'getTitleShadowColor']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_darkershadow` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.WallpaperUtils#getIconTitleShadowColor,com.miui.home.launcher.WallpaperUtils#getTitleShadowColor; A13=com.miui.home.launcher.WallpaperUtils#getIconTitleShadowColor,com.miui.home.launcher.WallpaperUtils#getTitleShadowColor; shared_methods=['getIconTitleShadowColor', 'getTitleShadowColor']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 setResult[Color.argb(Math.round(Color.alpha(color,Color.argb(Math.round(Color.alpha(color]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherIconHooks.kt::TitleShadowHook` / `LauncherIconHooks.kt::TitleShadowHook`: A13 already implements the exclusive keys launcher_darkershadow,launcher_renameapps,launcher_titletopmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherSystemHooks_kt_DisableLauncherLogHook__DisableLauncherLogHook

- PROOF_ID: `PROOF_OG_LauncherSystemHooks_kt_DisableLauncherLogHook__DisableLauncherLogHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `DisableLauncherLogHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.AnalyticalDataCollectorJobService#onStartJob,com.miui.home.launcher.AnalyticalDataCollector#canTrackLaunchAppEvent`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `DisableLauncherLogHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.AnalyticalDataCollectorJobService#onStartJob,com.miui.home.launcher.AnalyticalDataCollector#canTrackLaunchAppEvent`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_disable_log,launcher_indicator_topmargin,launcher_unlockgrids`
- VALUE_DOMAIN: owner-group keys for DisableLauncherLogHook: launcher_disable_log,launcher_indicator_topmargin,launcher_unlockgrids
- DEFAULT_SEMANTICS: `launcher_disable_log` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `Launcher.kt::DisableLauncherLogHook` / `LauncherSystemHooks.kt::DisableLauncherLogHook`: A13 already implements the exclusive keys launcher_disable_log,launcher_indicator_topmargin,launcher_unlockgrids. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Launcher.kt::DisableLauncherLogHook` (hook, phases=unknown) vs A13 `LauncherSystemHooks.kt::DisableLauncherLogHook` (hook, phases=unknown). Shared methods=['canTrackLaunchAppEvent', 'onStartJob']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_disable_log` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.AnalyticalDataCollectorJobService#onStartJob,com.miui.home.launcher.AnalyticalDataCollector#canTrackLaunchAppEvent; A13=com.miui.home.launcher.AnalyticalDataCollectorJobService#onStartJob,com.miui.home.launcher.AnalyticalDataCollector#canTrackLaunchAppEvent; shared_methods=['canTrackLaunchAppEvent', 'onStartJob']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Launcher.kt::DisableLauncherLogHook` / `LauncherSystemHooks.kt::DisableLauncherLogHook`: A13 already implements the exclusive keys launcher_disable_log,launcher_indicator_topmargin,launcher_unlockgrids. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_MultiWindowPlusHook__MultiWindowPlusHook

- PROOF_ID: `PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_MultiWindowPlusHook__MultiWindowPlusHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `MultiWindowPlusHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `#updateResizeBlackList,#getSplitScreenBlackListFromXml,#inResizeBlackList`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `MultiWindowPlusHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `#updateResizeBlackList,#getSplitScreenBlackListFromXml,#inResizeBlackList`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_disable_wallpaperscale,system_recents_disable_wallpaperscale,system_recents_hide_statusbar`
- VALUE_DOMAIN: owner-group keys for MultiWindowPlusHook: launcher_disable_wallpaperscale,system_recents_disable_wallpaperscale,system_recents_hide_statusbar
- DEFAULT_SEMANTICS: `launcher_disable_wallpaperscale` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemWindowHooks.kt::MultiWindowPlusHook` / `SystemFreeformAndMultiWindowHooks.kt::MultiWindowPlusHook`: A13 already implements the exclusive keys launcher_disable_wallpaperscale,system_recents_disable_wallpaperscale,system_recents_hide_statusbar. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::MultiWindowPlusHook` (hook, phases=unknown) vs A13 `SystemFreeformAndMultiWindowHooks.kt::MultiWindowPlusHook` (hook, phases=unknown). Shared methods=['getSplitScreenBlackListFromXml', 'inResizeBlackList', 'updateResizeBlackList']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_disable_wallpaperscale` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=#updateResizeBlackList,#getSplitScreenBlackListFromXml,#inResizeBlackList; A13=#updateResizeBlackList,#getSplitScreenBlackListFromXml,#inResizeBlackList; shared_methods=['getSplitScreenBlackListFromXml', 'inResizeBlackList', 'updateResizeBlackList']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::MultiWindowPlusHook` / `SystemFreeformAndMultiWindowHooks.kt::MultiWindowPlusHook`: A13 already implements the exclusive keys launcher_disable_wallpaperscale,system_recents_disable_wallpaperscale,system_recents_hide_statusbar. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherLayoutHooks_kt_DockMarginBottomHook__DockMarginBottomHook

- PROOF_ID: `PROOF_OG_LauncherLayoutHooks_kt_DockMarginBottomHook__DockMarginBottomHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `DockMarginBottomHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `DockMarginBottomHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_dock_bottommargin,launcher_dock_topmargin,launcher_horizmargin,launcher_indicatorheight,launcher_topmargin`
- VALUE_DOMAIN: owner-group keys for DockMarginBottomHook: launcher_dock_bottommargin,launcher_dock_topmargin,launcher_horizmargin,launcher_indicatorheight,launcher_topmargin
- DEFAULT_SEMANTICS: `launcher_dock_bottommargin` A14 default=0; A13 default=0
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `LauncherLayoutHooks.kt::DockMarginBottomHook` / `LauncherLayoutHooks.kt::DockMarginBottomHook`: A13 already implements the exclusive keys launcher_dock_bottommargin,launcher_dock_topmargin,launcher_horizmargin,launcher_indicatorheight,launcher_topmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherLayoutHooks.kt::DockMarginBottomHook` (hook, phases=intercept) vs A13 `LauncherLayoutHooks.kt::DockMarginBottomHook` (hook, phases=before). Shared methods=['calcHotSeatsMarginBottom']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_dock_bottommargin` A14=0 A13=0. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom; A13=com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom; shared_methods=['calcHotSeatsMarginBottom']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherLayoutHooks.kt::DockMarginBottomHook` / `LauncherLayoutHooks.kt::DockMarginBottomHook`: A13 already implements the exclusive keys launcher_dock_bottommargin,launcher_dock_topmargin,launcher_horizmargin,launcher_indicatorheight,launcher_topmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_XML_launcher

- PROOF_ID: `PROOF_OG_XML_launcher`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/res/xml/prefs_launcher.xml`
- A14_SYMBOL: `CheckBoxPreferenceEx`
- A14_INSTALLER: `Settings module UI`
- A14_HOOK_TARGETS: `(xml/snapshot family; no exclusive hook symbol)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/res/xml/prefs_launcher.xml`
- A13_SYMBOL: `CheckBoxPreferenceEx`
- A13_INSTALLER: `Settings module UI`
- A13_HOOK_TARGETS: `(xml/snapshot family; consumed by A13 style/hook path)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `launcher_docktitles`
- VALUE_DOMAIN: XML family prefs_launcher.xml
- DEFAULT_SEMANTICS: Both trees persist `launcher_docktitles` and sibling style keys in prefs_launcher.xml
- RESULT/ARGUMENT_BEHAVIOR: No opposite host setResult on this XML family; values are PrefMap style fields.
- API33_VARIANT_REASON: Owner-group review of XML family `prefs_launcher.xml`: both trees persist 1 user-visible keys including `launcher_docktitles`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- DIFF_SUMMARY: A14 `CheckBoxPreferenceEx` vs A13 `CheckBoxPreferenceEx` in `prefs_launcher.xml`. Literal-key scanner miss on snapshot fields is not a missing capability.
- VALUE_DEFAULT_COMPARISON: Same preference keys in `prefs_launcher.xml` on both trees.
- HOOK_TARGET_COMPARISON: No exclusive SystemUI member on the XML row; consumption is the family style hook.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the XML widget; click/persist is settings-owned.
- ARG_RESULT_COMPARISON: No host setResult for the XML row itself.
- A14_ONLY_BRANCHES: none on these exclusive XML-family keys
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of XML family `prefs_launcher.xml`: both trees persist 1 user-visible keys including `launcher_docktitles`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherFolderHooks_kt_PrivacyFolderHook__PrivacyFolderHook

- PROOF_ID: `PROOF_OG_LauncherFolderHooks_kt_PrivacyFolderHook__PrivacyFolderHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `PrivacyFolderHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#registerBroadcastReceivers,com.miui.home.launcher.Launcher#startSecurityHide,com.miui.home.launcher.Launcher#onDestroy`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `PrivacyFolderHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Launcher#registerBroadcastReceivers,com.miui.home.launcher.Launcher#startSecurityHide`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `launcher_fixanim,launcher_spread`
- VALUE_DOMAIN: owner-group keys for PrivacyFolderHook: launcher_fixanim,launcher_spread
- DEFAULT_SEMANTICS: `launcher_fixanim` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `LauncherFolderHooks.kt::PrivacyFolderHook` / `LauncherFolderHooks.kt::PrivacyFolderHook`: A13 already implements the exclusive keys launcher_fixanim,launcher_spread. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherFolderHooks.kt::PrivacyFolderHook` (hook, phases=intercept) vs A13 `LauncherFolderHooks.kt::PrivacyFolderHook` (hook, phases=after,before). Shared methods=['registerBroadcastReceivers', 'startSecurityHide']; A14-only members=['com.miui.home.launcher.Launcher#onDestroy']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_fixanim` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.Launcher#registerBroadcastReceivers,com.miui.home.launcher.Launcher#startSecurityHide,com.miui.home.launcher.Launcher#onDestroy; A13=com.miui.home.launcher.Launcher#registerBroadcastReceivers,com.miui.home.launcher.Launcher#startSecurityHide; shared_methods=['registerBroadcastReceivers', 'startSecurityHide']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.miui.home.launcher.Launcher#onDestroy']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherFolderHooks.kt::PrivacyFolderHook` / `LauncherFolderHooks.kt::PrivacyFolderHook`: A13 already implements the exclusive keys launcher_fixanim,launcher_spread. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherLayoutHooks_kt_MaxHotseatIconsCountHook__MaxHotseatIconsCountHook

- PROOF_ID: `PROOF_OG_LauncherLayoutHooks_kt_MaxHotseatIconsCountHook__MaxHotseatIconsCountHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `MaxHotseatIconsCountHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#getHotseatMaxCount`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `MaxHotseatIconsCountHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_fixlaunch`
- VALUE_DOMAIN: owner-group keys for MaxHotseatIconsCountHook: launcher_fixlaunch
- DEFAULT_SEMANTICS: `launcher_fixlaunch` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `LauncherLayoutHooks.kt::MaxHotseatIconsCountHook` / `LauncherLayoutHooks.kt::MaxHotseatIconsCountHook`: A13 already implements the exclusive keys launcher_fixlaunch. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherLayoutHooks.kt::MaxHotseatIconsCountHook` (hook, phases=unknown) vs A13 `LauncherLayoutHooks.kt::MaxHotseatIconsCountHook` (hook, phases=unknown). Shared methods=none; A14-only members=['com.miui.home.launcher.DeviceConfig#getHotseatMaxCount']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_fixlaunch` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.DeviceConfig#getHotseatMaxCount; A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.miui.home.launcher.DeviceConfig#getHotseatMaxCount']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherLayoutHooks.kt::MaxHotseatIconsCountHook` / `LauncherLayoutHooks.kt::MaxHotseatIconsCountHook`: A13 already implements the exclusive keys launcher_fixlaunch. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherIconHooks_kt_TitleFontSizeHook__TitleFontSizeHook

- PROOF_ID: `PROOF_OG_LauncherIconHooks_kt_TitleFontSizeHook__TitleFontSizeHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `TitleFontSizeHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ShortcutIcon#fromXml,com.miui.home.launcher.ShortcutIcon#createShortcutIcon,com.miui.home.launcher.common.Utilities#adaptTitleStyleToWallpaper`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_SYMBOL: `TitleFontSizeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ItemIcon#setTitleColorMode,com.miui.home.launcher.ShortcutIcon#fromXml,com.miui.home.launcher.ShortcutIcon#createShortcutIcon,com.miui.home.launcher.common.Utilities#adaptTitleStyleToWallpaper`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_folder_cols,launcher_iconscale`
- VALUE_DOMAIN: owner-group keys for TitleFontSizeHook: launcher_folder_cols,launcher_iconscale
- DEFAULT_SEMANTICS: `launcher_folder_cols` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `LauncherIconHooks.kt::TitleFontSizeHook` / `LauncherIconHooks.kt::TitleFontSizeHook`: A13 already implements the exclusive keys launcher_folder_cols,launcher_iconscale. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherIconHooks.kt::TitleFontSizeHook` (hook, phases=intercept) vs A13 `LauncherIconHooks.kt::TitleFontSizeHook` (hook, phases=after). Shared methods=['adaptTitleStyleToWallpaper', 'createShortcutIcon', 'fromXml', 'onFinishInflate']; A14-only members=none; A13-only members=['com.miui.home.launcher.ItemIcon#setTitleColorMode'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_folder_cols` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ShortcutIcon#fromXml,com.miui.home.launcher.ShortcutIcon#createShortcutIcon,com.miui.home.launcher.common.Utilities#adaptTitleStyleToWallpaper; A13=com.miui.home.launcher.ItemIcon#onFinishInflate,com.miui.home.launcher.ItemIcon#setTitleColorMode,com.miui.home.launcher.ShortcutIcon#fromXml,com.miui.home.launcher.ShortcutIcon#createShortcutIcon,com.miui.home.launcher.common.Utilities#adaptTitleStyleToWallpaper; shared_methods=['adaptTitleStyleToWallpaper', 'createShortcutIcon', 'fromXml', 'onFinishInflate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherIconHooks.kt::TitleFontSizeHook` / `LauncherIconHooks.kt::TitleFontSizeHook`: A13 already implements the exclusive keys launcher_folder_cols,launcher_iconscale. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PREFERENCE_KEYS: `launcher_folderwidth,launcher_folderspace`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: shared defaults (no explicit default literal)
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals
- API33_VARIANT_REASON: Normalized owner bodies are identical; same relevant keys; compatible installer ownership.
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_OG_LauncherFolderHooks_kt_FolderBlurHook__FolderBlurHook

- PROOF_ID: `PROOF_OG_LauncherFolderHooks_kt_FolderBlurHook__FolderBlurHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `FolderBlurHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#cancelShortcutMenu,#fastBlurWhenOpenOrCloseFolder,#getLauncherBlur`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `FolderBlurHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.FolderCling#open,com.miui.home.launcher.FolderCling#close,com.miui.home.launcher.Launcher#cancelShortcutMenu,#getLauncherBlur`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `launcher_hideseekpoints,launcher_privacyapps_gest,system_hidefromrecents`
- VALUE_DOMAIN: owner-group keys for FolderBlurHook: launcher_hideseekpoints,launcher_privacyapps_gest,system_hidefromrecents
- DEFAULT_SEMANTICS: `launcher_hideseekpoints` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[blurRatio]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before,after`. Owner-group review of `LauncherFolderHooks.kt::FolderBlurHook` / `LauncherFolderHooks.kt::FolderBlurHook`: A13 already implements the exclusive keys launcher_hideseekpoints,launcher_privacyapps_gest,system_hidefromrecents. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherFolderHooks.kt::FolderBlurHook` (hook, phases=intercept) vs A13 `LauncherFolderHooks.kt::FolderBlurHook` (hook, phases=before,after). Shared methods=['cancelShortcutMenu', 'getLauncherBlur']; A14-only members=['#fastBlurWhenOpenOrCloseFolder']; A13-only members=['com.miui.home.launcher.FolderCling#close', 'com.miui.home.launcher.FolderCling#open'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_hideseekpoints` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.Launcher#cancelShortcutMenu,#fastBlurWhenOpenOrCloseFolder,#getLauncherBlur; A13=com.miui.home.launcher.FolderCling#open,com.miui.home.launcher.FolderCling#close,com.miui.home.launcher.Launcher#cancelShortcutMenu,#getLauncherBlur; shared_methods=['cancelShortcutMenu', 'getLauncherBlur']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[blurRatio]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#fastBlurWhenOpenOrCloseFolder']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherFolderHooks.kt::FolderBlurHook` / `LauncherFolderHooks.kt::FolderBlurHook`: A13 already implements the exclusive keys launcher_hideseekpoints,launcher_privacyapps_gest,system_hidefromrecents. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherLayoutHooks_kt_InfiniteScrollHook__InfiniteScrollHook

- PROOF_ID: `PROOF_OG_LauncherLayoutHooks_kt_InfiniteScrollHook__InfiniteScrollHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `InfiniteScrollHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.ScreenView#getSnapToScreenIndex,com.miui.home.launcher.ScreenView#getSnapUnitIndex`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `InfiniteScrollHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_HOOK_TARGETS: `com.miui.home.launcher.ScreenView#getSnapToScreenIndex,com.miui.home.launcher.ScreenView#getSnapUnitIndex`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_hideseekpoints_edit,launcher_infinitescroll`
- VALUE_DOMAIN: owner-group keys for InfiniteScrollHook: launcher_hideseekpoints_edit,launcher_infinitescroll
- DEFAULT_SEMANTICS: `launcher_hideseekpoints_edit` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 setResult[screenCount,0,screenCount,0]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `LauncherLayoutHooks.kt::InfiniteScrollHook` / `LauncherLayoutHooks.kt::InfiniteScrollHook`: A13 already implements the exclusive keys launcher_hideseekpoints_edit,launcher_infinitescroll. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherLayoutHooks.kt::InfiniteScrollHook` (hook, phases=intercept) vs A13 `LauncherLayoutHooks.kt::InfiniteScrollHook` (hook, phases=after). Shared methods=['getSnapToScreenIndex', 'getSnapUnitIndex']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_hideseekpoints_edit` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.ScreenView#getSnapToScreenIndex,com.miui.home.launcher.ScreenView#getSnapUnitIndex; A13=com.miui.home.launcher.ScreenView#getSnapToScreenIndex,com.miui.home.launcher.ScreenView#getSnapUnitIndex; shared_methods=['getSnapToScreenIndex', 'getSnapUnitIndex']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 setResult[screenCount,0,screenCount,0]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherLayoutHooks.kt::InfiniteScrollHook` / `LauncherLayoutHooks.kt::InfiniteScrollHook`: A13 already implements the exclusive keys launcher_hideseekpoints_edit,launcher_infinitescroll. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherSystemHooks_kt_FixAppInfoLaunchHook__FixAppInfoLaunchHook

- PROOF_ID: `PROOF_OG_LauncherSystemHooks_kt_FixAppInfoLaunchHook__FixAppInfoLaunchHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `FixAppInfoLaunchHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.shortcuts.ShortcutMenuManager#startAppDetailsActivity`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `FixAppInfoLaunchHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.util.Utilities#startDetailsActivityForInfo,com.miui.home.launcher.shortcuts.ShortcutMenuManager#startAppDetailsActivity`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_hidetitles`
- VALUE_DOMAIN: owner-group keys for FixAppInfoLaunchHook: launcher_hidetitles
- DEFAULT_SEMANTICS: `launcher_hidetitles` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[true,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Launcher.kt::FixAppInfoLaunchHook` / `LauncherSystemHooks.kt::FixAppInfoLaunchHook`: A13 already implements the exclusive keys launcher_hidetitles. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Launcher.kt::FixAppInfoLaunchHook` (hook, phases=intercept) vs A13 `LauncherSystemHooks.kt::FixAppInfoLaunchHook` (hook, phases=before). Shared methods=['startAppDetailsActivity']; A14-only members=none; A13-only members=['com.miui.home.launcher.util.Utilities#startDetailsActivityForInfo'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_hidetitles` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.shortcuts.ShortcutMenuManager#startAppDetailsActivity; A13=com.miui.home.launcher.util.Utilities#startDetailsActivityForInfo,com.miui.home.launcher.shortcuts.ShortcutMenuManager#startAppDetailsActivity; shared_methods=['startAppDetailsActivity']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[true,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Launcher.kt::FixAppInfoLaunchHook` / `LauncherSystemHooks.kt::FixAppInfoLaunchHook`: A13 already implements the exclusive keys launcher_hidetitles. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherSystemHooks_kt_NoClockHideHook__NoClockHideHook

- PROOF_ID: `PROOF_OG_LauncherSystemHooks_kt_NoClockHideHook__NoClockHideHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `NoClockHideHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#updateStatusBarClock`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `NoClockHideHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Launcher#updateStatusBarClock`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_noclockhide`
- VALUE_DOMAIN: owner-group keys for NoClockHideHook: launcher_noclockhide
- DEFAULT_SEMANTICS: `launcher_noclockhide` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `LauncherIconHooks.kt::NoClockHideHook` / `LauncherSystemHooks.kt::NoClockHideHook`: A13 already implements the exclusive keys launcher_noclockhide. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherIconHooks.kt::NoClockHideHook` (hook, phases=unknown) vs A13 `LauncherSystemHooks.kt::NoClockHideHook` (hook, phases=unknown). Shared methods=['updateStatusBarClock']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_noclockhide` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.Launcher#updateStatusBarClock; A13=com.miui.home.launcher.Launcher#updateStatusBarClock; shared_methods=['updateStatusBarClock']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherIconHooks.kt::NoClockHideHook` / `LauncherSystemHooks.kt::NoClockHideHook`: A13 already implements the exclusive keys launcher_noclockhide. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherSystemHooks_kt_CloseDrawerOnLaunchHook__CloseDrawerOnLaunchHook

- PROOF_ID: `PROOF_OG_LauncherSystemHooks_kt_CloseDrawerOnLaunchHook__CloseDrawerOnLaunchHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `CloseDrawerOnLaunchHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.allapps.category.fragment.AppsListFragment#onClick,com.miui.home.launcher.allapps.category.fragment.RecommendCategoryAppListFragment#onClick`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `CloseDrawerOnLaunchHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.allapps.category.fragment.AppsListFragment#onClick,com.miui.home.launcher.allapps.category.fragment.RecommendCategoryAppListFragment#onClick`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_nounlockanim,launcher_nozoomanim,launcher_oldlaunchanim`
- VALUE_DOMAIN: owner-group keys for CloseDrawerOnLaunchHook: launcher_nounlockanim,launcher_nozoomanim,launcher_oldlaunchanim
- DEFAULT_SEMANTICS: `launcher_nounlockanim` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `LauncherFolderHooks.kt::CloseDrawerOnLaunchHook` / `LauncherSystemHooks.kt::CloseDrawerOnLaunchHook`: A13 already implements the exclusive keys launcher_nounlockanim,launcher_nozoomanim,launcher_oldlaunchanim. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherFolderHooks.kt::CloseDrawerOnLaunchHook` (hook, phases=intercept) vs A13 `LauncherSystemHooks.kt::CloseDrawerOnLaunchHook` (hook, phases=before). Shared methods=['onClick']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_nounlockanim` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.allapps.category.fragment.AppsListFragment#onClick,com.miui.home.launcher.allapps.category.fragment.RecommendCategoryAppListFragment#onClick; A13=com.miui.home.launcher.allapps.category.fragment.AppsListFragment#onClick,com.miui.home.launcher.allapps.category.fragment.RecommendCategoryAppListFragment#onClick; shared_methods=['onClick']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherFolderHooks.kt::CloseDrawerOnLaunchHook` / `LauncherSystemHooks.kt::CloseDrawerOnLaunchHook`: A13 already implements the exclusive keys launcher_nounlockanim,launcher_nozoomanim,launcher_oldlaunchanim. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherLayoutHooks_kt_NoWidgetOnlyHook__NoWidgetOnlyHook

- PROOF_ID: `PROOF_OG_LauncherLayoutHooks_kt_NoWidgetOnlyHook__NoWidgetOnlyHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `NoWidgetOnlyHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.CellLayout#setScreenType`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `NoWidgetOnlyHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.CellLayout#setScreenType`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_nowidgetonly`
- VALUE_DOMAIN: owner-group keys for NoWidgetOnlyHook: launcher_nowidgetonly
- DEFAULT_SEMANTICS: `launcher_nowidgetonly` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `LauncherLayoutHooks.kt::NoWidgetOnlyHook` / `LauncherLayoutHooks.kt::NoWidgetOnlyHook`: A13 already implements the exclusive keys launcher_nowidgetonly. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherLayoutHooks.kt::NoWidgetOnlyHook` (hook, phases=intercept) vs A13 `LauncherLayoutHooks.kt::NoWidgetOnlyHook` (hook, phases=before). Shared methods=['setScreenType']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_nowidgetonly` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.CellLayout#setScreenType; A13=com.miui.home.launcher.CellLayout#setScreenType; shared_methods=['setScreenType']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherLayoutHooks.kt::NoWidgetOnlyHook` / `LauncherLayoutHooks.kt::NoWidgetOnlyHook`: A13 already implements the exclusive keys launcher_nowidgetonly. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Launcher_kt_onActivityCreated__onActivityCreated

- PROOF_ID: `PROOF_OG_Launcher_kt_onActivityCreated__onActivityCreated`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Launcher.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_privacyapps_list`
- VALUE_DOMAIN: owner-group keys for onActivityCreated: launcher_privacyapps_list
- DEFAULT_SEMANTICS: `launcher_privacyapps_list` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `Launcher.kt::onActivityCreated` / `Launcher.kt::onActivityCreated`: A13 already implements the exclusive keys launcher_privacyapps_list. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Launcher.kt::onActivityCreated` (hook, phases=unknown) vs A13 `Launcher.kt::onActivityCreated` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_privacyapps_list` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Launcher.kt::onActivityCreated` / `Launcher.kt::onActivityCreated`: A13 already implements the exclusive keys launcher_privacyapps_list. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherIconHooks_kt_RenameShortcutsHook__RenameShortcutsHook

- PROOF_ID: `PROOF_OG_LauncherIconHooks_kt_RenameShortcutsHook__RenameShortcutsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `RenameShortcutsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.Launcher#onCreate,com.miui.home.launcher.Launcher#onDestroy,com.miui.home.launcher.ShortcutInfo#loadToggleInfo,com.miui.home.launcher.ShortcutInfo#setLabelAndUpdateDB,com.miui.home.launcher.ShortcutInfo#load,com.miui.home.launcher.ShortcutInfo#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_SYMBOL: `RenameShortcutsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_HOOK_TARGETS: `com.miui.home.launcher.Launcher#onCreate,com.miui.home.launcher.ShortcutInfo#loadToggleInfo,com.miui.home.launcher.ShortcutInfo#setLabelAndUpdateDB,com.miui.home.launcher.ShortcutInfo#load,com.miui.home.launcher.ShortcutInfo#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_renameapps_list,launcher_titlefontsize`
- VALUE_DOMAIN: owner-group keys for RenameShortcutsHook: launcher_renameapps_list,launcher_titlefontsize
- DEFAULT_SEMANTICS: `launcher_renameapps_list` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `LauncherIconHooks.kt::RenameShortcutsHook` / `LauncherIconHooks.kt::RenameShortcutsHook`: A13 already implements the exclusive keys launcher_renameapps_list,launcher_titlefontsize. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherIconHooks.kt::RenameShortcutsHook` (hook, phases=intercept) vs A13 `LauncherIconHooks.kt::RenameShortcutsHook` (hook, phases=after). Shared methods=['<init>', 'load', 'loadToggleInfo', 'onCreate', 'setLabelAndUpdateDB']; A14-only members=['com.miui.home.launcher.Launcher#onDestroy']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_renameapps_list` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.Launcher#onCreate,com.miui.home.launcher.Launcher#onDestroy,com.miui.home.launcher.ShortcutInfo#loadToggleInfo,com.miui.home.launcher.ShortcutInfo#setLabelAndUpdateDB,com.miui.home.launcher.ShortcutInfo#load,com.miui.home.launcher.ShortcutInfo#<init>; A13=com.miui.home.launcher.Launcher#onCreate,com.miui.home.launcher.ShortcutInfo#loadToggleInfo,com.miui.home.launcher.ShortcutInfo#setLabelAndUpdateDB,com.miui.home.launcher.ShortcutInfo#load,com.miui.home.launcher.ShortcutInfo#<init>; shared_methods=['<init>', 'load', 'loadToggleInfo', 'onCreate', 'setLabelAndUpdateDB']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.miui.home.launcher.Launcher#onDestroy']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherIconHooks.kt::RenameShortcutsHook` / `LauncherIconHooks.kt::RenameShortcutsHook`: A13 already implements the exclusive keys launcher_renameapps_list,launcher_titlefontsize. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherAnimationHooks_kt_RecentsBlurRatioHook__RecentsBlurRatioHook

- PROOF_ID: `PROOF_OG_LauncherAnimationHooks_kt_RecentsBlurRatioHook__RecentsBlurRatioHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `RecentsBlurRatioHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_HOOK_TARGETS: `#fastBlurWhenEnterRecents,#fastBlurWhenGestureResetTaskView,#fastBlur`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
- A13_SYMBOL: `RecentsBlurRatioHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `#fastBlurWhenEnterRecents,#fastBlurWhenGestureResetTaskView,#fastBlur`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_sensorportrait,launcher_unlockhotseat`
- VALUE_DOMAIN: owner-group keys for RecentsBlurRatioHook: launcher_sensorportrait,launcher_unlockhotseat
- DEFAULT_SEMANTICS: `launcher_sensorportrait` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Launcher.kt::RecentsBlurRatioHook` / `LauncherAnimationHooks.kt::RecentsBlurRatioHook`: A13 already implements the exclusive keys launcher_sensorportrait,launcher_unlockhotseat. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Launcher.kt::RecentsBlurRatioHook` (hook, phases=intercept) vs A13 `LauncherAnimationHooks.kt::RecentsBlurRatioHook` (hook, phases=before). Shared methods=['fastBlur', 'fastBlurWhenEnterRecents', 'fastBlurWhenGestureResetTaskView']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `launcher_sensorportrait` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=#fastBlurWhenEnterRecents,#fastBlurWhenGestureResetTaskView,#fastBlur; A13=#fastBlurWhenEnterRecents,#fastBlurWhenGestureResetTaskView,#fastBlur; shared_methods=['fastBlur', 'fastBlurWhenEnterRecents', 'fastBlurWhenGestureResetTaskView']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Launcher.kt::RecentsBlurRatioHook` / `LauncherAnimationHooks.kt::RecentsBlurRatioHook`: A13 already implements the exclusive keys launcher_sensorportrait,launcher_unlockhotseat. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_BackupRestore_kt_performRestore__performRestore

- PROOF_ID: `PROOF_OG_BackupRestore_kt_performRestore__performRestore`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt`
- A14_SYMBOL: `performRestore`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt`
- A13_SYMBOL: `performRestore`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BackupRestore.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `miuizer_launchericon`
- VALUE_DOMAIN: owner-group keys for performRestore: miuizer_launchericon
- DEFAULT_SEMANTICS: `miuizer_launchericon` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `BackupRestore.kt::performRestore` / `BackupRestore.kt::performRestore`: A13 already implements the exclusive keys miuizer_launchericon. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `BackupRestore.kt::performRestore` (hook, phases=unknown) vs A13 `BackupRestore.kt::performRestore` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `miuizer_launchericon` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `BackupRestore.kt::performRestore` / `BackupRestore.kt::performRestore`: A13 already implements the exclusive keys miuizer_launchericon. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_SystemNotificationMoreHooks_kt_DisableAnyNotificationBlockHook__DisableAnyNotificationBlockHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_DisableAnyNotificationBlockHook__DisableAnyNotificationBlockHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `DisableAnyNotificationBlockHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_HOOK_TARGETS: `android.app.NotificationChannel#isBlockable,android.app.NotificationChannel#setBlockable`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `DisableAnyNotificationBlockHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java`
- A13_HOOK_TARGETS: `android.app.NotificationChannel#isBlockable,android.app.NotificationChannel#setBlockable`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `miuizer_settingsiconpos,system_disableanynotif`
- VALUE_DOMAIN: owner-group keys for DisableAnyNotificationBlockHook: miuizer_settingsiconpos,system_disableanynotif
- DEFAULT_SEMANTICS: `miuizer_settingsiconpos` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemNotificationHooks.kt::DisableAnyNotificationBlockHook` / `SystemNotificationMoreHooks.kt::DisableAnyNotificationBlockHook`: A13 already implements the exclusive keys miuizer_settingsiconpos,system_disableanynotif. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemNotificationHooks.kt::DisableAnyNotificationBlockHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::DisableAnyNotificationBlockHook` (hook, phases=before). Shared methods=['isBlockable', 'setBlockable']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `miuizer_settingsiconpos` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=android.app.NotificationChannel#isBlockable,android.app.NotificationChannel#setBlockable; A13=android.app.NotificationChannel#isBlockable,android.app.NotificationChannel#setBlockable; shared_methods=['isBlockable', 'setBlockable']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemNotificationHooks.kt::DisableAnyNotificationBlockHook` / `SystemNotificationMoreHooks.kt::DisableAnyNotificationBlockHook`: A13 already implements the exclusive keys miuizer_settingsiconpos,system_disableanynotif. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_StatusBarIconsPositionAdjustHook__StatusBarIconsPositionAdjustHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_StatusBarIconsPositionAdjustHook__StatusBarIconsPositionAdjustHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `StatusBarIconsPositionAdjustHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#updateStatusBarVisibilities,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#miuiOnAttachedToWindow`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `StatusBarIconsPositionAdjustHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility,com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility,com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIcon,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation,com.android.systemui.statusbar.policy.NetworkSpeedController#setDripNetworkSpeedView,com.android.systemui.statusbar.views.NetworkSpeedView#setVisibilityByController,com.android.systemui.statusbar.phone.StatusBarSignalPolicy#<init>`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_4gtolte,system_lockscreenshortcuts,system_statusbar_alarm_atleft,system_statusbar_alarm_atright,system_statusbar_btbattery_atright,system_statusbar_dnd_atleft,system_statusbar_gps_atleft,system_statusbar_headset_atright,system_statusbar_mobile_showname,system_statusbar_netspeed_atleft,system_statusbar_netspeed_atsecondrow,system_statusbar_nfc_atright,system_statusbar_sound_atleft,system_statusbar_vpn_atright,system_statusbaricons_swap_wifi_mobile,system_statusbaricons_wifi_mobile_atleft`
- VALUE_DOMAIN: owner-group keys for StatusBarIconsPositionAdjustHook: system_4gtolte,system_lockscreenshortcuts,system_statusbar_alarm_atleft,system_statusbar_alarm_atright,system_statusbar_btbattery_atright,system_statusbar_dnd_atleft,system_statusbar_gps_atleft,system_statusbar_headset_atright
- DEFAULT_SEMANTICS: `system_4gtolte` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[null,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before,after` vs A13 phase `before,after`. Owner-group review of `SystemUIStatusBarHooks.kt::StatusBarIconsPositionAdjustHook` / `SystemUIStatusBarHooks.kt::StatusBarIconsPositionAdjustHook`: A13 already implements the exclusive keys system_4gtolte,system_lockscreenshortcuts,system_statusbar_alarm_atleft,system_statusbar_alarm_atright,system_statusbar_btbattery_atright,system_statusbar_dnd_atleft,system_statusbar_gps_atleft,system_statusbar_headset_atright,system_statusbar_mobile_showname,system_statusbar_netspeed_atleft,system_statusbar_netspeed_atsecondrow,system_statusbar_nfc_atright,system_statusbar_sound_atleft,system_statusbar_vpn_atright,system_statusbaricons_swap_wifi_mobile,system_statusbaricons_wifi_mobile_atleft. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbar_batterytempandcurrent', 'system_statusbar_batterytempandcurrent_atright', 'system_statusbar_showdevicetemperature', 'system_statusbar_showdevicetemperature_atright'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::StatusBarIconsPositionAdjustHook` (hook, phases=before,after) vs A13 `SystemUIStatusBarHooks.kt::StatusBarIconsPositionAdjustHook` (hook, phases=before,after). Shared methods=none; A14-only members=['com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#updateStatusBarVisibilities', 'com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#miuiOnAttachedToWindow', 'com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow']; A13-only members=['com.android.systemui.SystemUIApplication#onCreate', 'com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated', 'com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility', 'com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation', 'com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIcon', 'com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility', 'com.android.systemui.statusbar.phone.StatusBarSignalPolicy#<init>', 'com.android.systemui.statusbar.policy.NetworkSpeedController#setDripNetworkSpeedView', 'com.android.systemui.statusbar.views.NetworkSpeedView#setVisibilityByController'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_4gtolte` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#updateStatusBarVisibilities,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#miuiOnAttachedToWindow; A13=com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility,com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility,com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIcon,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation,com.android.systemui.statusbar.policy.NetworkSpeedController#setDripNetworkSpeedView,com.android.systemui.statusbar.views.NetworkSpeedView#setVisibilityByController,com.android.systemui.statusbar.phone.StatusBarSignalPolicy#<init>; shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before,after; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[null,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_statusbar_batterytempandcurrent', 'system_statusbar_batterytempandcurrent_atright', 'system_statusbar_showdevicetemperature', 'system_statusbar_showdevicetemperature_atright']; A14-only hook members=['com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#updateStatusBarVisibilities', 'com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#miuiOnAttachedToWindow', 'com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::StatusBarIconsPositionAdjustHook` / `SystemUIStatusBarHooks.kt::StatusBarIconsPositionAdjustHook`: A13 already implements the exclusive keys system_4gtolte,system_lockscreenshortcuts,system_statusbar_alarm_atleft,system_statusbar_alarm_atright,system_statusbar_btbattery_atright,system_statusbar_dnd_atleft,system_statusbar_gps_atleft,system_statusbar_headset_atright,system_statusbar_mobile_showname,system_statusbar_netspeed_atleft,system_statusbar_netspeed_atsecondrow,system_statusbar_nfc_atright,system_statusbar_sound_atleft,system_statusbar_vpn_atright,system_statusbaricons_swap_wifi_mobile,system_statusbaricons_wifi_mobile_atleft. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbar_batterytempandcurrent', 'system_statusbar_batterytempandcurrent_atright', 'system_statusbar_showdevicetemperature', 'system_statusbar_showdevicetemperature_atright'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_ShowNotificationsAfterUnlockHook__ShowNotificationsAfterUnlockHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_ShowNotificationsAfterUnlockHook__ShowNotificationsAfterUnlockHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `ShowNotificationsAfterUnlockHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.interruption.KeyguardNotificationVisibilityProviderImpl#shouldHideNotification`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `ShowNotificationsAfterUnlockHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.ExpandedNotification#hasShownAfterUnlock,com.android.systemui.statusbar.notification.ExpandedNotification#setHasShownAfterUnlock,com.android.systemui.statusbar.notification.MiuiNotificationCompat#isKeptOnKeyguard`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_albumartonlock,system_betterpopups_center,system_expandheadups,system_notifafterunlock`
- VALUE_DOMAIN: owner-group keys for ShowNotificationsAfterUnlockHook: system_albumartonlock,system_betterpopups_center,system_expandheadups,system_notifafterunlock
- DEFAULT_SEMANTICS: `system_albumartonlock` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::ShowNotificationsAfterUnlockHook` / `SystemNotificationMoreHooks.kt::ShowNotificationsAfterUnlockHook`: A13 already implements the exclusive keys system_albumartonlock,system_betterpopups_center,system_expandheadups,system_notifafterunlock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::ShowNotificationsAfterUnlockHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::ShowNotificationsAfterUnlockHook` (hook, phases=after). Shared methods=none; A14-only members=['com.android.systemui.statusbar.notification.interruption.KeyguardNotificationVisibilityProviderImpl#shouldHideNotification']; A13-only members=['com.android.systemui.statusbar.notification.ExpandedNotification#hasShownAfterUnlock', 'com.android.systemui.statusbar.notification.ExpandedNotification#setHasShownAfterUnlock', 'com.android.systemui.statusbar.notification.MiuiNotificationCompat#isKeptOnKeyguard'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_albumartonlock` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.interruption.KeyguardNotificationVisibilityProviderImpl#shouldHideNotification; A13=com.android.systemui.statusbar.notification.ExpandedNotification#hasShownAfterUnlock,com.android.systemui.statusbar.notification.ExpandedNotification#setHasShownAfterUnlock,com.android.systemui.statusbar.notification.MiuiNotificationCompat#isKeptOnKeyguard; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.notification.interruption.KeyguardNotificationVisibilityProviderImpl#shouldHideNotification']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::ShowNotificationsAfterUnlockHook` / `SystemNotificationMoreHooks.kt::ShowNotificationsAfterUnlockHook`: A13 already implements the exclusive keys system_albumartonlock,system_betterpopups_center,system_expandheadups,system_notifafterunlock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUILockScreenHooks_kt_LockScreenAlbumArtHook__LockScreenAlbumArtHook

- PROOF_ID: `PROOF_OG_SystemUILockScreenHooks_kt_LockScreenAlbumArtHook__LockScreenAlbumArtHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `LockScreenAlbumArtHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,com.android.systemui.statusbar.NotificationMediaManager#clearCurrentMediaNotification,#updateThemeBackground,#updateThemeBackgroundVisibility,#linkageViewAnim`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `LockScreenAlbumArtHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackground,com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackgroundVisibility,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,com.android.systemui.statusbar.NotificationMediaManager#clearCurrentMediaNotification,com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_albumartonlock_blur,system_albumartonlock_gray,system_albumartonlock_scale`
- VALUE_DOMAIN: owner-group keys for LockScreenAlbumArtHook: system_albumartonlock_blur,system_albumartonlock_gray,system_albumartonlock_scale
- DEFAULT_SEMANTICS: `system_albumartonlock_blur` A14 default=n/a; A13 default=0
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after,before` vs A13 phase `after,before`. Owner-group review of `SystemUILockScreenHooks.kt::LockScreenAlbumArtHook` / `SystemUILockScreenHooks.kt::LockScreenAlbumArtHook`: A13 already implements the exclusive keys system_albumartonlock_blur,system_albumartonlock_gray,system_albumartonlock_scale. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUILockScreenHooks.kt::LockScreenAlbumArtHook` (hook, phases=after,before) vs A13 `SystemUILockScreenHooks.kt::LockScreenAlbumArtHook` (hook, phases=after,before). Shared methods=['clearCurrentMediaNotification', 'updateMediaMetaData', 'updateThemeBackground', 'updateThemeBackgroundVisibility']; A14-only members=['#linkageViewAnim', '#updateThemeBackground', '#updateThemeBackgroundVisibility']; A13-only members=['com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#<init>', 'com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackground', 'com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackgroundVisibility'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_albumartonlock_blur` A14=n/a A13=0. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,com.android.systemui.statusbar.NotificationMediaManager#clearCurrentMediaNotification,#updateThemeBackground,#updateThemeBackgroundVisibility,#linkageViewAnim; A13=com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackground,com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateThemeBackgroundVisibility,com.android.systemui.statusbar.NotificationMediaManager#updateMediaMetaData,com.android.systemui.statusbar.NotificationMediaManager#clearCurrentMediaNotification,com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#<init>; shared_methods=['clearCurrentMediaNotification', 'updateMediaMetaData', 'updateThemeBackground', 'updateThemeBackgroundVisibility']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after,before; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#linkageViewAnim', '#updateThemeBackground', '#updateThemeBackgroundVisibility']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUILockScreenHooks.kt::LockScreenAlbumArtHook` / `SystemUILockScreenHooks.kt::LockScreenAlbumArtHook`: A13 already implements the exclusive keys system_albumartonlock_blur,system_albumartonlock_gray,system_albumartonlock_scale. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_StatusBarGesturesHook__StatusBarGesturesHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_StatusBarGesturesHook__StatusBarGesturesHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `StatusBarGesturesHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.PhoneStatusBarView#onInterceptTouchEvent,com.android.systemui.statusbar.phone.PhoneStatusBarView#onTouchEvent,com.android.systemui.statusbar.phone.PhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.phone.PhoneStatusBarView#onDetachedFromWindow`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `StatusBarGesturesHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_allownotiffloat,system_allownotifonkeyguard,system_lsalarm,system_statusbarcontrols`
- VALUE_DOMAIN: owner-group keys for StatusBarGesturesHook: system_allownotiffloat,system_allownotifonkeyguard,system_lsalarm,system_statusbarcontrols
- DEFAULT_SEMANTICS: `system_allownotiffloat` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `before` vs A13 phase `before,after`. Owner-group review of `SystemUIControlCenterHooks.kt::StatusBarGesturesHook` / `SystemUIControlCenterHooks.kt::StatusBarGesturesHook`: A13 already implements the exclusive keys system_allownotiffloat,system_allownotifonkeyguard,system_lsalarm,system_statusbarcontrols. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::StatusBarGesturesHook` (hook, phases=before) vs A13 `SystemUIControlCenterHooks.kt::StatusBarGesturesHook` (hook, phases=before,after). Shared methods=none; A14-only members=['com.android.systemui.statusbar.phone.PhoneStatusBarView#onAttachedToWindow', 'com.android.systemui.statusbar.phone.PhoneStatusBarView#onDetachedFromWindow', 'com.android.systemui.statusbar.phone.PhoneStatusBarView#onInterceptTouchEvent', 'com.android.systemui.statusbar.phone.PhoneStatusBarView#onTouchEvent']; A13-only members=['miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_allownotiffloat` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.PhoneStatusBarView#onInterceptTouchEvent,com.android.systemui.statusbar.phone.PhoneStatusBarView#onTouchEvent,com.android.systemui.statusbar.phone.PhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.phone.PhoneStatusBarView#onDetachedFromWindow; A13=miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.phone.PhoneStatusBarView#onAttachedToWindow', 'com.android.systemui.statusbar.phone.PhoneStatusBarView#onDetachedFromWindow', 'com.android.systemui.statusbar.phone.PhoneStatusBarView#onInterceptTouchEvent', 'com.android.systemui.statusbar.phone.PhoneStatusBarView#onTouchEvent']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::StatusBarGesturesHook` / `SystemUIControlCenterHooks.kt::StatusBarGesturesHook`: A13 already implements the exclusive keys system_allownotiffloat,system_allownotifonkeyguard,system_lsalarm,system_statusbarcontrols. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_AllRotationsHook__AllRotationsHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_AllRotationsHook__AllRotationsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `AllRotationsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.wm.DisplayRotation#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `AllRotationsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.DisplayRotation#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_allrotations2`
- VALUE_DOMAIN: owner-group keys for AllRotationsHook: system_allrotations2
- DEFAULT_SEMANTICS: `system_allrotations2` A14 default=1; A13 default=1
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemWindowHooks.kt::AllRotationsHook` / `SystemAudioAndVisualAndMoreHooks.kt::AllRotationsHook`: A13 already implements the exclusive keys system_allrotations2. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::AllRotationsHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::AllRotationsHook` (hook, phases=after). Shared methods=['<init>']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_allrotations2` A14=1 A13=1. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.wm.DisplayRotation#<init>; A13=com.android.server.wm.DisplayRotation#<init>; shared_methods=['<init>']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::AllRotationsHook` / `SystemAudioAndVisualAndMoreHooks.kt::AllRotationsHook`: A13 already implements the exclusive keys system_allrotations2. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_System_kt_onActivityCreated__onActivityCreated

- PROOF_ID: `PROOF_OG_System_kt_onActivityCreated__onActivityCreated`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_animationscale_animator,system_animationscale_transition,system_animationscale_window,system_applock_list,system_cleanopenwith_test,system_cleanshare_test,system_credentials`
- VALUE_DOMAIN: owner-group keys for onActivityCreated: system_animationscale_animator,system_animationscale_transition,system_animationscale_window,system_applock_list,system_cleanopenwith_test,system_cleanshare_test,system_credentials
- DEFAULT_SEMANTICS: `system_animationscale_animator` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System.kt::onActivityCreated` / `System.kt::onActivityCreated`: A13 already implements the exclusive keys system_animationscale_animator,system_animationscale_transition,system_animationscale_window,system_applock_list,system_cleanopenwith_test,system_cleanshare_test,system_credentials. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_tile_style_cat', 'system_lockscreenshortcuts_right', 'system_statusbarcontrols_dt', 'system_statusbarcontrols_dt_left', 'system_statusbarcontrols_dt_right', 'system_statusbarcontrols_longpress', 'system_strong_toast_island_offset', 'system_strong_toast_mode'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::onActivityCreated` (hook, phases=unknown) vs A13 `System.kt::onActivityCreated` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_animationscale_animator` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_tile_style_cat', 'system_lockscreenshortcuts_right', 'system_statusbarcontrols_dt', 'system_statusbarcontrols_dt_left', 'system_statusbarcontrols_dt_right', 'system_statusbarcontrols_longpress', 'system_strong_toast_island_offset', 'system_strong_toast_mode']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::onActivityCreated` / `System.kt::onActivityCreated`: A13 already implements the exclusive keys system_animationscale_animator,system_animationscale_transition,system_animationscale_window,system_applock_list,system_cleanopenwith_test,system_cleanshare_test,system_credentials. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_tile_style_cat', 'system_lockscreenshortcuts_right', 'system_statusbarcontrols_dt', 'system_statusbarcontrols_dt_left', 'system_statusbarcontrols_dt_right', 'system_statusbarcontrols_longpress', 'system_strong_toast_island_offset', 'system_strong_toast_mode'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSecurityAndSystemHooks_kt_NoSignatureVerifyServiceHook__NoSignatureVerifyServiceHook

- PROOF_ID: `PROOF_OG_SystemSecurityAndSystemHooks_kt_NoSignatureVerifyServiceHook__NoSignatureVerifyServiceHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `NoSignatureVerifyServiceHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_HOOK_TARGETS: `android.util.jar.StrictJarVerifier#verifyMessageDigest,android.util.jar.StrictJarVerifier#verify,com.android.server.pm.PackageManagerServiceUtils#verifySignatures,com.android.server.pm.InstallPackageHelper#doesSignatureMatchForPermissions,com.android.server.pm.InstallPackageHelper#cannotInstallWithBadPermissionGroups,com.android.server.pm.permission.PermissionManagerServiceImpl#shouldGrantPermissionBySignature,android.content.pm.ApplicationInfo#isSignedWithPlatformKey,#checkCapability,android.util.jar.StrictJarVerifier#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `NoSignatureVerifyServiceHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `android.util.jar.StrictJarVerifier#verifyMessageDigest,android.util.jar.StrictJarVerifier#verify,com.android.server.pm.PackageManagerServiceUtils#verifySignatures,com.android.server.pm.InstallPackageHelper#doesSignatureMatchForPermissions,com.android.server.pm.InstallPackageHelper#cannotInstallWithBadPermissionGroups,com.android.server.pm.permission.PermissionManagerServiceImpl#shouldGrantPermissionBySignature,#checkCapability,android.util.jar.StrictJarVerifier#<init>`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_apksign`
- VALUE_DOMAIN: owner-group keys for NoSignatureVerifyServiceHook: system_apksign
- DEFAULT_SEMANTICS: `system_apksign` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[true,null,null,true]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[false,true,true,true]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before,after`. Owner-group review of `SystemSecurityHooks.kt::NoSignatureVerifyServiceHook` / `SystemSecurityAndSystemHooks.kt::NoSignatureVerifyServiceHook`: A13 already implements the exclusive keys system_apksign. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemSecurityHooks.kt::NoSignatureVerifyServiceHook` (hook, phases=intercept) vs A13 `SystemSecurityAndSystemHooks.kt::NoSignatureVerifyServiceHook` (hook, phases=before,after). Shared methods=['<init>', 'cannotInstallWithBadPermissionGroups', 'checkCapability', 'doesSignatureMatchForPermissions', 'shouldGrantPermissionBySignature', 'verify', 'verifyMessageDigest', 'verifySignatures']; A14-only members=['android.content.pm.ApplicationInfo#isSignedWithPlatformKey']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_apksign` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=android.util.jar.StrictJarVerifier#verifyMessageDigest,android.util.jar.StrictJarVerifier#verify,com.android.server.pm.PackageManagerServiceUtils#verifySignatures,com.android.server.pm.InstallPackageHelper#doesSignatureMatchForPermissions,com.android.server.pm.InstallPackageHelper#cannotInstallWithBadPermissionGroups,com.android.server.pm.permission.PermissionManagerServiceImpl#shouldGrantPermissionBySignature,android.content.pm.ApplicationInfo#isSignedWithPlatformKey,#checkCapability,android.util.jar.StrictJarVerifier#<init>; A13=android.util.jar.StrictJarVerifier#verifyMessageDigest,android.util.jar.StrictJarVerifier#verify,com.android.server.pm.PackageManagerServiceUtils#verifySignatures,com.android.server.pm.InstallPackageHelper#doesSignatureMatchForPermissions,com.android.server.pm.InstallPackageHelper#cannotInstallWithBadPermissionGroups,com.android.server.pm.permission.PermissionManagerServiceImpl#shouldGrantPermissionBySignature,#checkCapability,android.util.jar.StrictJarVerifier#<init>; shared_methods=['<init>', 'cannotInstallWithBadPermissionGroups', 'checkCapability', 'doesSignatureMatchForPermissions', 'shouldGrantPermissionBySignature', 'verify', 'verifyMessageDigest', 'verifySignatures']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[true,null,null,true]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[false,true,true,true]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['android.content.pm.ApplicationInfo#isSignedWithPlatformKey']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemSecurityHooks.kt::NoSignatureVerifyServiceHook` / `SystemSecurityAndSystemHooks.kt::NoSignatureVerifyServiceHook`: A13 already implements the exclusive keys system_apksign. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemLockScreenMoreHooks_kt_AppLockHook__AppLockHook

- PROOF_ID: `PROOF_OG_SystemLockScreenMoreHooks_kt_AppLockHook__AppLockHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `AppLockHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.miui.server.SecurityManagerService#removeAccessControlPassLocked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `AppLockHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.miui.server.SecurityManagerService#removeAccessControlPassLocked`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_applock`
- VALUE_DOMAIN: owner-group keys for AppLockHook: system_applock
- DEFAULT_SEMANTICS: `system_applock` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemLockScreenHooks.kt::AppLockHook` / `SystemLockScreenMoreHooks.kt::AppLockHook`: A13 already implements the exclusive keys system_applock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::AppLockHook` (hook, phases=intercept) vs A13 `SystemLockScreenMoreHooks.kt::AppLockHook` (hook, phases=before). Shared methods=['removeAccessControlPassLocked']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_applock` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.server.SecurityManagerService#removeAccessControlPassLocked; A13=com.miui.server.SecurityManagerService#removeAccessControlPassLocked; shared_methods=['removeAccessControlPassLocked']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::AppLockHook` / `SystemLockScreenMoreHooks.kt::AppLockHook`: A13 already implements the exclusive keys system_applock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_AppsDefaultSortHook__AppsDefaultSortHook

- PROOF_ID: `PROOF_OG_Various_kt_AppsDefaultSortHook__AppsDefaultSortHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AppsDefaultSortHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.miui.appmanager.AppManagerMainActivity#onCreate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AppsDefaultSortHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.appmanager.AppManagerMainActivity#onCreate`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_applock_scramblepin,various_appsort`
- VALUE_DOMAIN: owner-group keys for AppsDefaultSortHook: system_applock_scramblepin,various_appsort
- DEFAULT_SEMANTICS: `system_applock_scramblepin` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Various.kt::AppsDefaultSortHook` / `Various.kt::AppsDefaultSortHook`: A13 already implements the exclusive keys system_applock_scramblepin,various_appsort. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::AppsDefaultSortHook` (hook, phases=intercept) vs A13 `Various.kt::AppsDefaultSortHook` (hook, phases=before). Shared methods=['onCreate']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_applock_scramblepin` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.appmanager.AppManagerMainActivity#onCreate; A13=com.miui.appmanager.AppManagerMainActivity#onCreate; shared_methods=['onCreate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::AppsDefaultSortHook` / `Various.kt::AppsDefaultSortHook`: A13 already implements the exclusive keys system_applock_scramblepin,various_appsort. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_NoCallInterruptionHook__NoCallInterruptionHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_NoCallInterruptionHook__NoCallInterruptionHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `NoCallInterruptionHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.audio.AudioService#requestAudioFocus,com.android.server.TelephonyRegistry#notifyCallState,com.android.server.TelephonyRegistry#notifyCallStateForPhoneId`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `NoCallInterruptionHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.audio.AudioService#requestAudioFocus,com.android.server.TelephonyRegistry#notifyCallState,com.android.server.TelephonyRegistry#notifyCallStateForPhoneId`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_applock_skip,system_ignorecalls,system_ignorecalls_apps`
- VALUE_DOMAIN: owner-group keys for NoCallInterruptionHook: system_applock_skip,system_ignorecalls,system_ignorecalls_apps
- DEFAULT_SEMANTICS: `system_applock_skip` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[1]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before,after`. Owner-group review of `SystemAudioHooks.kt::NoCallInterruptionHook` / `SystemAudioAndVisualAndMoreHooks.kt::NoCallInterruptionHook`: A13 already implements the exclusive keys system_applock_skip,system_ignorecalls,system_ignorecalls_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemAudioHooks.kt::NoCallInterruptionHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::NoCallInterruptionHook` (hook, phases=before,after). Shared methods=['notifyCallState', 'notifyCallStateForPhoneId', 'requestAudioFocus']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_applock_skip` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.audio.AudioService#requestAudioFocus,com.android.server.TelephonyRegistry#notifyCallState,com.android.server.TelephonyRegistry#notifyCallStateForPhoneId; A13=com.android.server.audio.AudioService#requestAudioFocus,com.android.server.TelephonyRegistry#notifyCallState,com.android.server.TelephonyRegistry#notifyCallStateForPhoneId; shared_methods=['notifyCallState', 'notifyCallStateForPhoneId', 'requestAudioFocus']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[1]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemAudioHooks.kt::NoCallInterruptionHook` / `SystemAudioAndVisualAndMoreHooks.kt::NoCallInterruptionHook`: A13 already implements the exclusive keys system_applock_skip,system_ignorecalls,system_ignorecalls_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemLockScreenMoreHooks_kt_SkipAppLockHook__SkipAppLockHook

- PROOF_ID: `PROOF_OG_SystemLockScreenMoreHooks_kt_SkipAppLockHook__SkipAppLockHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `SkipAppLockHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.miui.server.AccessController#skipActivity`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `SkipAppLockHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_HOOK_TARGETS: `com.miui.server.AccessController#skipActivity`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_applock_skip_activities`
- VALUE_DOMAIN: owner-group keys for SkipAppLockHook: system_applock_skip_activities
- DEFAULT_SEMANTICS: `system_applock_skip_activities` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 setResult[true]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::SkipAppLockHook` / `SystemLockScreenMoreHooks.kt::SkipAppLockHook`: A13 already implements the exclusive keys system_applock_skip_activities. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::SkipAppLockHook` (hook, phases=intercept) vs A13 `SystemLockScreenMoreHooks.kt::SkipAppLockHook` (hook, phases=after). Shared methods=['skipActivity']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_applock_skip_activities` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.server.AccessController#skipActivity; A13=com.miui.server.AccessController#skipActivity; shared_methods=['skipActivity']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 setResult[true]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::SkipAppLockHook` / `SystemLockScreenMoreHooks.kt::SkipAppLockHook`: A13 already implements the exclusive keys system_applock_skip_activities. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemLockScreenMoreHooks_kt_AppLockTimeoutHook__AppLockTimeoutHook

- PROOF_ID: `PROOF_OG_SystemLockScreenMoreHooks_kt_AppLockTimeoutHook__AppLockTimeoutHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `AppLockTimeoutHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.miui.server.SecurityManagerService#addAccessControlPassForUser,com.miui.server.SecurityManagerService#checkAccessControlPassLocked,com.miui.server.SecurityManagerService#activityResume`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `AppLockTimeoutHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.miui.server.SecurityManagerService#addAccessControlPassForUser,com.miui.server.SecurityManagerService#checkAccessControlPassLocked`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_applock_timeout`
- VALUE_DOMAIN: owner-group keys for AppLockTimeoutHook: system_applock_timeout
- DEFAULT_SEMANTICS: `system_applock_timeout` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `unknown`. Owner-group review of `SystemLockScreenHooks.kt::AppLockTimeoutHook` / `SystemLockScreenMoreHooks.kt::AppLockTimeoutHook`: A13 already implements the exclusive keys system_applock_timeout. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::AppLockTimeoutHook` (hook, phases=intercept) vs A13 `SystemLockScreenMoreHooks.kt::AppLockTimeoutHook` (hook, phases=unknown). Shared methods=['addAccessControlPassForUser', 'checkAccessControlPassLocked']; A14-only members=['com.miui.server.SecurityManagerService#activityResume']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_applock_timeout` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.server.SecurityManagerService#addAccessControlPassForUser,com.miui.server.SecurityManagerService#checkAccessControlPassLocked,com.miui.server.SecurityManagerService#activityResume; A13=com.miui.server.SecurityManagerService#addAccessControlPassForUser,com.miui.server.SecurityManagerService#checkAccessControlPassLocked; shared_methods=['addAccessControlPassForUser', 'checkAccessControlPassLocked']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.miui.server.SecurityManagerService#activityResume']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::AppLockTimeoutHook` / `SystemLockScreenMoreHooks.kt::AppLockTimeoutHook`: A13 already implements the exclusive keys system_applock_timeout. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_XML_system_autobrightness

- PROOF_ID: `PROOF_OG_XML_system_autobrightness`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/res/xml/prefs_system_autobrightness.xml`
- A14_SYMBOL: `CheckBoxPreferenceEx`
- A14_INSTALLER: `Settings module UI`
- A14_HOOK_TARGETS: `(xml/snapshot family; no exclusive hook symbol)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/res/xml/prefs_system_autobrightness.xml`
- A13_SYMBOL: `CheckBoxPreferenceEx`
- A13_INSTALLER: `Settings module UI`
- A13_HOOK_TARGETS: `(xml/snapshot family; consumed by A13 style/hook path)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `system_autobrightness`
- VALUE_DOMAIN: XML family prefs_system_autobrightness.xml
- DEFAULT_SEMANTICS: Both trees persist `system_autobrightness` and sibling style keys in prefs_system_autobrightness.xml
- RESULT/ARGUMENT_BEHAVIOR: No opposite host setResult on this XML family; values are PrefMap style fields.
- API33_VARIANT_REASON: Owner-group review of XML family `prefs_system_autobrightness.xml`: both trees persist 1 user-visible keys including `system_autobrightness`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- DIFF_SUMMARY: A14 `CheckBoxPreferenceEx` vs A13 `CheckBoxPreferenceEx` in `prefs_system_autobrightness.xml`. Literal-key scanner miss on snapshot fields is not a missing capability.
- VALUE_DEFAULT_COMPARISON: Same preference keys in `prefs_system_autobrightness.xml` on both trees.
- HOOK_TARGET_COMPARISON: No exclusive SystemUI member on the XML row; consumption is the family style hook.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the XML widget; click/persist is settings-owned.
- ARG_RESULT_COMPARISON: No host setResult for the XML row itself.
- A14_ONLY_BRANCHES: none on these exclusive XML-family keys
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of XML family `prefs_system_autobrightness.xml`: both trees persist 1 user-visible keys including `system_autobrightness`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemDisplayAndWindowHooks_kt_constrainValue__refreshAutoBrightnessRangeSnapshot

- PROOF_ID: `PROOF_OG_SystemDisplayAndWindowHooks_kt_constrainValue__refreshAutoBrightnessRangeSnapshot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `refreshAutoBrightnessRangeSnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `constrainValue`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_autobrightness_limitmax,system_autobrightness_limitmin,system_autobrightness_max,system_autobrightness_min`
- VALUE_DOMAIN: owner-group keys for constrainValue: system_autobrightness_limitmax,system_autobrightness_limitmin,system_autobrightness_max,system_autobrightness_min
- DEFAULT_SEMANTICS: `system_autobrightness_limitmax` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemDisplayHooks.kt::refreshAutoBrightnessRangeSnapshot` / `SystemDisplayAndWindowHooks.kt::constrainValue`: A13 already implements the exclusive keys system_autobrightness_limitmax,system_autobrightness_limitmin,system_autobrightness_max,system_autobrightness_min. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemDisplayHooks.kt::refreshAutoBrightnessRangeSnapshot` (hook, phases=unknown) vs A13 `SystemDisplayAndWindowHooks.kt::constrainValue` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_autobrightness_limitmax` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemDisplayHooks.kt::refreshAutoBrightnessRangeSnapshot` / `SystemDisplayAndWindowHooks.kt::constrainValue`: A13 already implements the exclusive keys system_autobrightness_limitmax,system_autobrightness_limitmin,system_autobrightness_max,system_autobrightness_min. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIBatteryHooks_kt_BatteryIndicatorHook__BatteryIndicatorHook

- PROOF_ID: `PROOF_OG_SystemUIBatteryHooks_kt_BatteryIndicatorHook__BatteryIndicatorHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A14_SYMBOL: `BatteryIndicatorHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded,com.android.systemui.statusbar.phone.NotificationIconAreaController#onDarkChanged,com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl#fireBatteryLevelChanged,com.android.systemui.statusbar.policy.BatteryControllerImpl#firePowerSaveChanged`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A13_SYMBOL: `BatteryIndicatorHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationIconAreaController#onDarkChanged,com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl#fireBatteryLevelChanged,com.android.systemui.statusbar.policy.BatteryControllerImpl#firePowerSaveChanged`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_batteryindicator`
- VALUE_DOMAIN: owner-group keys for BatteryIndicatorHook: system_batteryindicator
- DEFAULT_SEMANTICS: `system_batteryindicator` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after` vs A13 phase `after`. Owner-group review of `SystemUIBatteryHooks.kt::BatteryIndicatorHook` / `SystemUIBatteryHooks.kt::BatteryIndicatorHook`: A13 already implements the exclusive keys system_batteryindicator. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIBatteryHooks.kt::BatteryIndicatorHook` (hook, phases=after) vs A13 `SystemUIBatteryHooks.kt::BatteryIndicatorHook` (hook, phases=after). Shared methods=['fireBatteryLevelChanged', 'firePowerSaveChanged', 'onDarkChanged']; A14-only members=['com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_batteryindicator` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded,com.android.systemui.statusbar.phone.NotificationIconAreaController#onDarkChanged,com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl#fireBatteryLevelChanged,com.android.systemui.statusbar.policy.BatteryControllerImpl#firePowerSaveChanged; A13=com.android.systemui.statusbar.phone.NotificationIconAreaController#onDarkChanged,com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl#fireBatteryLevelChanged,com.android.systemui.statusbar.policy.BatteryControllerImpl#firePowerSaveChanged; shared_methods=['fireBatteryLevelChanged', 'firePowerSaveChanged', 'onDarkChanged']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.shade.MiuiNotificationPanelViewController#updatePanelExpanded']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIBatteryHooks.kt::BatteryIndicatorHook` / `SystemUIBatteryHooks.kt::BatteryIndicatorHook`: A13 already implements the exclusive keys system_batteryindicator. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_BatteryIndicator_kt_updateParameters__updateParameters

- PROOF_ID: `PROOF_OG_BatteryIndicator_kt_updateParameters__updateParameters`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_SYMBOL: `updateParameters`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A13_SYMBOL: `updateParameters`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_batteryindicator_align,system_batteryindicator_centered,system_batteryindicator_color,system_batteryindicator_glow,system_batteryindicator_height,system_batteryindicator_limitvis,system_batteryindicator_lowlevel,system_batteryindicator_padding,system_batteryindicator_rounded,system_batteryindicator_transp`
- VALUE_DOMAIN: owner-group keys for updateParameters: system_batteryindicator_align,system_batteryindicator_centered,system_batteryindicator_color,system_batteryindicator_glow,system_batteryindicator_height,system_batteryindicator_limitvis,system_batteryindicator_lowlevel,system_batteryindicator_padding
- DEFAULT_SEMANTICS: `system_batteryindicator_align` A14 default=1; A13 default=1
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `BatteryIndicator.kt::updateParameters` / `BatteryIndicator.kt::updateParameters`: A13 already implements the exclusive keys system_batteryindicator_align,system_batteryindicator_centered,system_batteryindicator_color,system_batteryindicator_glow,system_batteryindicator_height,system_batteryindicator_limitvis,system_batteryindicator_lowlevel,system_batteryindicator_padding,system_batteryindicator_rounded,system_batteryindicator_transp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_batteryindicator_colorval1', 'system_batteryindicator_colorval2', 'system_batteryindicator_colorval3', 'system_batteryindicator_colorval4'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `BatteryIndicator.kt::updateParameters` (hook, phases=unknown) vs A13 `BatteryIndicator.kt::updateParameters` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_batteryindicator_align` A14=1 A13=1. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_batteryindicator_colorval1', 'system_batteryindicator_colorval2', 'system_batteryindicator_colorval3', 'system_batteryindicator_colorval4']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `BatteryIndicator.kt::updateParameters` / `BatteryIndicator.kt::updateParameters`: A13 already implements the exclusive keys system_batteryindicator_align,system_batteryindicator_centered,system_batteryindicator_color,system_batteryindicator_glow,system_batteryindicator_height,system_batteryindicator_limitvis,system_batteryindicator_lowlevel,system_batteryindicator_padding,system_batteryindicator_rounded,system_batteryindicator_transp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_batteryindicator_colorval1', 'system_batteryindicator_colorval2', 'system_batteryindicator_colorval3', 'system_batteryindicator_colorval4'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_System_BatteryIndicator_kt_onActivityCreated__updateParameters

- PROOF_ID: `PROOF_OG_System_BatteryIndicator_kt_onActivityCreated__updateParameters`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_SYMBOL: `updateParameters`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_batteryindicator_colorval1,system_batteryindicator_colorval2,system_batteryindicator_colorval3,system_batteryindicator_colorval4`
- VALUE_DOMAIN: owner-group keys for onActivityCreated: system_batteryindicator_colorval1,system_batteryindicator_colorval2,system_batteryindicator_colorval3,system_batteryindicator_colorval4
- DEFAULT_SEMANTICS: `system_batteryindicator_colorval1` A14 default=Color.GREEN; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=False; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `BatteryIndicator.kt::updateParameters` / `System_BatteryIndicator.kt::onActivityCreated`: A13 already implements the exclusive keys system_batteryindicator_colorval1,system_batteryindicator_colorval2,system_batteryindicator_colorval3,system_batteryindicator_colorval4. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_batteryindicator', 'system_batteryindicator_align', 'system_batteryindicator_centered', 'system_batteryindicator_glow', 'system_batteryindicator_height', 'system_batteryindicator_limitvis', 'system_batteryindicator_lowlevel', 'system_batteryindicator_padding', 'system_batteryindicator_rounded', 'system_batteryindicator_transp'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `BatteryIndicator.kt::updateParameters` (hook, phases=unknown) vs A13 `System_BatteryIndicator.kt::onActivityCreated` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_batteryindicator_colorval1` A14=Color.GREEN A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_batteryindicator', 'system_batteryindicator_align', 'system_batteryindicator_centered', 'system_batteryindicator_glow', 'system_batteryindicator_height', 'system_batteryindicator_limitvis', 'system_batteryindicator_lowlevel', 'system_batteryindicator_padding', 'system_batteryindicator_rounded', 'system_batteryindicator_transp']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `BatteryIndicator.kt::updateParameters` / `System_BatteryIndicator.kt::onActivityCreated`: A13 already implements the exclusive keys system_batteryindicator_colorval1,system_batteryindicator_colorval2,system_batteryindicator_colorval3,system_batteryindicator_colorval4. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_batteryindicator', 'system_batteryindicator_align', 'system_batteryindicator_centered', 'system_batteryindicator_glow', 'system_batteryindicator_height', 'system_batteryindicator_limitvis', 'system_batteryindicator_lowlevel', 'system_batteryindicator_padding', 'system_batteryindicator_rounded', 'system_batteryindicator_transp'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_System_BatteryIndicator_kt_onActivityCreated__onActivityCreated

- PROOF_ID: `PROOF_OG_System_BatteryIndicator_kt_onActivityCreated__onActivityCreated`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_BatteryIndicator.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_batteryindicator_test`
- VALUE_DOMAIN: owner-group keys for onActivityCreated: system_batteryindicator_test
- DEFAULT_SEMANTICS: `system_batteryindicator_test` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System_BatteryIndicator.kt::onActivityCreated` / `System_BatteryIndicator.kt::onActivityCreated`: A13 already implements the exclusive keys system_batteryindicator_test. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System_BatteryIndicator.kt::onActivityCreated` (hook, phases=unknown) vs A13 `System_BatteryIndicator.kt::onActivityCreated` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_batteryindicator_test` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System_BatteryIndicator.kt::onActivityCreated` / `System_BatteryIndicator.kt::onActivityCreated`: A13 already implements the exclusive keys system_batteryindicator_test. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUINotificationHooks_kt_DisableHeadsUpWhenMuteHook__DisableHeadsUpWhenMuteHook

- PROOF_ID: `PROOF_OG_SystemUINotificationHooks_kt_DisableHeadsUpWhenMuteHook__DisableHeadsUpWhenMuteHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `DisableHeadsUpWhenMuteHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl#canAlertAwakeCommon,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#updateVolumeZen`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `DisableHeadsUpWhenMuteHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.interruption.MiuiNotificationInterruptStateProviderImpl#shouldPeek,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#updateVolumeZen`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_betterpopups_allowfloat,system_betterpopups_disablewhenmute`
- VALUE_DOMAIN: owner-group keys for DisableHeadsUpWhenMuteHook: system_betterpopups_allowfloat,system_betterpopups_disablewhenmute
- DEFAULT_SEMANTICS: `system_betterpopups_allowfloat` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[false]; A13 setResult[false]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before,after` vs A13 phase `after`. Owner-group review of `SystemUINotificationHooks.kt::DisableHeadsUpWhenMuteHook` / `SystemUINotificationHooks.kt::DisableHeadsUpWhenMuteHook`: A13 already implements the exclusive keys system_betterpopups_allowfloat,system_betterpopups_disablewhenmute. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUINotificationHooks.kt::DisableHeadsUpWhenMuteHook` (hook, phases=before,after) vs A13 `SystemUINotificationHooks.kt::DisableHeadsUpWhenMuteHook` (hook, phases=after). Shared methods=['updateVolumeZen']; A14-only members=['com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl#canAlertAwakeCommon']; A13-only members=['com.android.systemui.statusbar.notification.interruption.MiuiNotificationInterruptStateProviderImpl#shouldPeek'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_betterpopups_allowfloat` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl#canAlertAwakeCommon,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#updateVolumeZen; A13=com.android.systemui.statusbar.notification.interruption.MiuiNotificationInterruptStateProviderImpl#shouldPeek,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy#updateVolumeZen; shared_methods=['updateVolumeZen']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before,after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[false]; A13 setResult[false]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl#canAlertAwakeCommon']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUINotificationHooks.kt::DisableHeadsUpWhenMuteHook` / `SystemUINotificationHooks.kt::DisableHeadsUpWhenMuteHook`: A13 already implements the exclusive keys system_betterpopups_allowfloat,system_betterpopups_disablewhenmute. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_BetterPopupsAllowFloatHook__BetterPopupsAllowFloatHook

- PROOF_ID: `PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_BetterPopupsAllowFloatHook__BetterPopupsAllowFloatHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `BetterPopupsAllowFloatHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateMiniWindowBar`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `BetterPopupsAllowFloatHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.NotificationSettingsManager#canSlide,com.android.systemui.statusbar.notification.policy.MiniWindowPolicy#canSlidePackage`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_betterpopups_allowfloat_apps`
- VALUE_DOMAIN: owner-group keys for BetterPopupsAllowFloatHook: system_betterpopups_allowfloat_apps
- DEFAULT_SEMANTICS: `system_betterpopups_allowfloat_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[true]; setResult[true,false]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemWindowHooks.kt::BetterPopupsAllowFloatHook` / `SystemFreeformAndMultiWindowHooks.kt::BetterPopupsAllowFloatHook`: A13 already implements the exclusive keys system_betterpopups_allowfloat_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::BetterPopupsAllowFloatHook` (hook, phases=intercept) vs A13 `SystemFreeformAndMultiWindowHooks.kt::BetterPopupsAllowFloatHook` (hook, phases=after,before). Shared methods=none; A14-only members=['com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateMiniWindowBar']; A13-only members=['com.android.systemui.statusbar.notification.NotificationSettingsManager#canSlide', 'com.android.systemui.statusbar.notification.policy.MiniWindowPolicy#canSlidePackage'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_betterpopups_allowfloat_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateMiniWindowBar; A13=com.android.systemui.statusbar.notification.NotificationSettingsManager#canSlide,com.android.systemui.statusbar.notification.policy.MiniWindowPolicy#canSlidePackage; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[true]; setResult[true,false]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateMiniWindowBar']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::BetterPopupsAllowFloatHook` / `SystemFreeformAndMultiWindowHooks.kt::BetterPopupsAllowFloatHook`: A13 already implements the exclusive keys system_betterpopups_allowfloat_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_AutoDismissExpandedPopupsHook__AutoDismissExpandedPopupsHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_AutoDismissExpandedPopupsHook__AutoDismissExpandedPopupsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `AutoDismissExpandedPopupsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.HeadsUpManagerPhone\$HeadsUpEntryPhone#updateEntry,com.android.systemui.statusbar.phone.StatusBarNotificationPresenter#onExpandClicked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `AutoDismissExpandedPopupsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.HeadsUpManagerPhone\$HeadsUpEntryPhone#setExpanded`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_betterpopups_autoclose_expanded,system_maxsbicons,system_statusbaricons_mute,system_statusbaricons_privacy,system_statusbaricons_record,system_statusbaricons_speaker`
- VALUE_DOMAIN: owner-group keys for AutoDismissExpandedPopupsHook: system_betterpopups_autoclose_expanded,system_maxsbicons,system_statusbaricons_mute,system_statusbaricons_privacy,system_statusbaricons_record,system_statusbaricons_speaker
- DEFAULT_SEMANTICS: `system_betterpopups_autoclose_expanded` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemNotificationHooks.kt::AutoDismissExpandedPopupsHook` / `SystemNotificationMoreHooks.kt::AutoDismissExpandedPopupsHook`: A13 already implements the exclusive keys system_betterpopups_autoclose_expanded,system_maxsbicons,system_statusbaricons_mute,system_statusbaricons_privacy,system_statusbaricons_record,system_statusbaricons_speaker. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemNotificationHooks.kt::AutoDismissExpandedPopupsHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::AutoDismissExpandedPopupsHook` (hook, phases=before). Shared methods=none; A14-only members=['com.android.systemui.statusbar.phone.HeadsUpManagerPhone\\$HeadsUpEntryPhone#updateEntry', 'com.android.systemui.statusbar.phone.StatusBarNotificationPresenter#onExpandClicked']; A13-only members=['com.android.systemui.statusbar.phone.HeadsUpManagerPhone\\$HeadsUpEntryPhone#setExpanded'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_betterpopups_autoclose_expanded` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.HeadsUpManagerPhone\$HeadsUpEntryPhone#updateEntry,com.android.systemui.statusbar.phone.StatusBarNotificationPresenter#onExpandClicked; A13=com.android.systemui.statusbar.phone.HeadsUpManagerPhone\$HeadsUpEntryPhone#setExpanded; shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.phone.HeadsUpManagerPhone\\$HeadsUpEntryPhone#updateEntry', 'com.android.systemui.statusbar.phone.StatusBarNotificationPresenter#onExpandClicked']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemNotificationHooks.kt::AutoDismissExpandedPopupsHook` / `SystemNotificationMoreHooks.kt::AutoDismissExpandedPopupsHook`: A13 already implements the exclusive keys system_betterpopups_autoclose_expanded,system_maxsbicons,system_statusbaricons_mute,system_statusbaricons_privacy,system_statusbaricons_record,system_statusbaricons_speaker. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemLockScreenHooks_kt_ScramblePINHook__ScramblePINHook

- PROOF_ID: `PROOF_OG_SystemLockScreenHooks_kt_ScramblePINHook__ScramblePINHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `ScramblePINHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.keyguard.KeyguardPINView#onFinishInflate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A13_SYMBOL: `ScramblePINHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.keyguard.KeyguardPINView#onFinishInflate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_betterpopups_delay,system_betterpopups_nohide`
- VALUE_DOMAIN: owner-group keys for ScramblePINHook: system_betterpopups_delay,system_betterpopups_nohide
- DEFAULT_SEMANTICS: `system_betterpopups_delay` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::ScramblePINHook` / `SystemLockScreenHooks.kt::ScramblePINHook`: A13 already implements the exclusive keys system_betterpopups_delay,system_betterpopups_nohide. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::ScramblePINHook` (hook, phases=intercept) vs A13 `SystemLockScreenHooks.kt::ScramblePINHook` (hook, phases=after). Shared methods=['onFinishInflate']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_betterpopups_delay` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.keyguard.KeyguardPINView#onFinishInflate; A13=com.android.keyguard.KeyguardPINView#onFinishInflate; shared_methods=['onFinishInflate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::ScramblePINHook` / `SystemLockScreenHooks.kt::ScramblePINHook`: A13 already implements the exclusive keys system_betterpopups_delay,system_betterpopups_nohide. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarAndClockHooks_kt_checkToast__checkToast

- PROOF_ID: `PROOF_OG_SystemStatusBarAndClockHooks_kt_checkToast__checkToast`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `checkToast`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt`
- A13_SYMBOL: `checkToast`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_blocktoasts_apps`
- VALUE_DOMAIN: owner-group keys for checkToast: system_blocktoasts_apps
- DEFAULT_SEMANTICS: `system_blocktoasts_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System.kt::checkToast` / `SystemStatusBarAndClockHooks.kt::checkToast`: A13 already implements the exclusive keys system_blocktoasts_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::checkToast` (hook, phases=unknown) vs A13 `SystemStatusBarAndClockHooks.kt::checkToast` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_blocktoasts_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::checkToast` / `SystemStatusBarAndClockHooks.kt::checkToast`: A13 already implements the exclusive keys system_blocktoasts_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationAndShareHooks_kt_QSHapticHook__QSHapticHook

- PROOF_ID: `PROOF_OG_SystemNotificationAndShareHooks_kt_QSHapticHook__QSHapticHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `QSHapticHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt`
- A13_SYMBOL: `QSHapticHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSFactoryImpl#createTileInternal`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_calendar_app,system_clock_app,system_qshaptics,system_qshaptics_ignore,system_shortcut_app,system_statusbaricons_alarmn`
- VALUE_DOMAIN: owner-group keys for QSHapticHook: system_calendar_app,system_clock_app,system_qshaptics,system_qshaptics_ignore,system_shortcut_app,system_statusbaricons_alarmn
- DEFAULT_SEMANTICS: `system_calendar_app` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemAudioHooks.kt::QSHapticHook` / `SystemNotificationAndShareHooks.kt::QSHapticHook`: A13 already implements the exclusive keys system_calendar_app,system_clock_app,system_qshaptics,system_qshaptics_ignore,system_shortcut_app,system_statusbaricons_alarmn. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemAudioHooks.kt::QSHapticHook` (hook, phases=intercept) vs A13 `SystemNotificationAndShareHooks.kt::QSHapticHook` (hook, phases=after). Shared methods=none; A14-only members=['com.android.systemui.qs.tileimpl.QSTileImpl#click']; A13-only members=['com.android.systemui.qs.tileimpl.QSFactoryImpl#createTileInternal'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_calendar_app` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.qs.tileimpl.QSTileImpl#click; A13=com.android.systemui.qs.tileimpl.QSFactoryImpl#createTileInternal; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.qs.tileimpl.QSTileImpl#click']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemAudioHooks.kt::QSHapticHook` / `SystemNotificationAndShareHooks.kt::QSHapticHook`: A13 already implements the exclusive keys system_calendar_app,system_clock_app,system_qshaptics,system_qshaptics_ignore,system_shortcut_app,system_statusbaricons_alarmn. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook__buildClockStyleSnapshot

- PROOF_ID: `PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook__buildClockStyleSnapshot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `buildClockStyleSnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `StatusBarClockTweakHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange,com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#setClockVisibility,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.qs.MiuiNotificationHeaderView#updateResources,com.android.systemui.qs.MiuiQSHeaderView#updateResources,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_cc_clock_customformat,system_statusbar_clock_24hour_format,system_statusbar_clock_customformat,system_statusbar_clock_customformat_enable,system_statusbar_clock_leadingzero,system_statusbar_clock_show_ampm,system_statusbar_clock_show_seconds`
- VALUE_DOMAIN: owner-group keys for StatusBarClockTweakHook: system_cc_clock_customformat,system_statusbar_clock_24hour_format,system_statusbar_clock_customformat,system_statusbar_clock_customformat_enable,system_statusbar_clock_leadingzero,system_statusbar_clock_show_ampm,system_statusbar_clock_show_seconds
- DEFAULT_SEMANTICS: `system_cc_clock_customformat` A14 default=""; A13 default=""
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[null,null,null]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `after,before`. Owner-group review of `SystemClockHooks.kt::buildClockStyleSnapshot` / `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook`: A13 already implements the exclusive keys system_cc_clock_customformat,system_statusbar_clock_24hour_format,system_statusbar_clock_customformat,system_statusbar_clock_customformat_enable,system_statusbar_clock_leadingzero,system_statusbar_clock_show_ampm,system_statusbar_clock_show_seconds. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_clock_customformat_enable', 'system_drawer_dateformat', 'system_statusbar_clock_align', 'system_statusbar_clock_bold', 'system_statusbar_clock_chip', 'system_statusbar_clock_chip_customtextcolor', 'system_statusbar_clock_chip_endcolor', 'system_statusbar_clock_chip_horizpadding', 'system_statusbar_clock_chip_orientation_vertical', 'system_statusbar_clock_chip_radius', 'system_statusbar_clock_chip_startcolor', 'system_statusbar_clock_chip_textcolor', 'system_statusbar_clock_chip_usemonet', 'system_statusbar_clock_chip_verticalpadding', 'system_statusbar_clock_fixedcontent_width', 'system_statusbar_clock_fontsize', 'system_statusbar_clock_leftmargin', 'system_statusbar_clock_rightmargin', 'system_statusbar_clock_verticaloffset', 'system_statusbar_enable_weather_param'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemClockHooks.kt::buildClockStyleSnapshot` (hook, phases=unknown) vs A13 `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook` (hook, phases=after,before). Shared methods=none; A14-only members=none; A13-only members=['com.android.systemui.qs.MiuiNotificationHeaderView#updateResources', 'com.android.systemui.qs.MiuiQSHeaderView#updateResources', 'com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow', 'com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>', 'com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange', 'com.android.systemui.statusbar.views.MiuiClock#<init>', 'com.android.systemui.statusbar.views.MiuiClock#setClockVisibility', 'com.android.systemui.statusbar.views.MiuiClock#updateTime'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_clock_customformat` A14="" A13="". No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange,com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#setClockVisibility,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.qs.MiuiNotificationHeaderView#updateResources,com.android.systemui.qs.MiuiQSHeaderView#updateResources,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[null,null,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_clock_customformat_enable', 'system_drawer_dateformat', 'system_statusbar_clock_align', 'system_statusbar_clock_bold', 'system_statusbar_clock_chip', 'system_statusbar_clock_chip_customtextcolor', 'system_statusbar_clock_chip_endcolor', 'system_statusbar_clock_chip_horizpadding', 'system_statusbar_clock_chip_orientation_vertical', 'system_statusbar_clock_chip_radius', 'system_statusbar_clock_chip_startcolor', 'system_statusbar_clock_chip_textcolor', 'system_statusbar_clock_chip_usemonet', 'system_statusbar_clock_chip_verticalpadding', 'system_statusbar_clock_fixedcontent_width', 'system_statusbar_clock_fontsize', 'system_statusbar_clock_leftmargin', 'system_statusbar_clock_rightmargin', 'system_statusbar_clock_verticaloffset', 'system_statusbar_enable_weather_param']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemClockHooks.kt::buildClockStyleSnapshot` / `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook`: A13 already implements the exclusive keys system_cc_clock_customformat,system_statusbar_clock_24hour_format,system_statusbar_clock_customformat,system_statusbar_clock_customformat_enable,system_statusbar_clock_leadingzero,system_statusbar_clock_show_ampm,system_statusbar_clock_show_seconds. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_clock_customformat_enable', 'system_drawer_dateformat', 'system_statusbar_clock_align', 'system_statusbar_clock_bold', 'system_statusbar_clock_chip', 'system_statusbar_clock_chip_customtextcolor', 'system_statusbar_clock_chip_endcolor', 'system_statusbar_clock_chip_horizpadding', 'system_statusbar_clock_chip_orientation_vertical', 'system_statusbar_clock_chip_radius', 'system_statusbar_clock_chip_startcolor', 'system_statusbar_clock_chip_textcolor', 'system_statusbar_clock_chip_usemonet', 'system_statusbar_clock_chip_verticalpadding', 'system_statusbar_clock_fixedcontent_width', 'system_statusbar_clock_fontsize', 'system_statusbar_clock_leftmargin', 'system_statusbar_clock_rightmargin', 'system_statusbar_clock_verticaloffset', 'system_statusbar_enable_weather_param'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook__CCClockTweakHook

- PROOF_ID: `PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook__CCClockTweakHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `CCClockTweakHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.qs.MiuiNotificationHeaderView#updateResources`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `StatusBarClockTweakHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange,com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#setClockVisibility,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.qs.MiuiNotificationHeaderView#updateResources,com.android.systemui.qs.MiuiQSHeaderView#updateResources,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_cc_clock_fontsize,system_cc_clock_verticaloffset`
- VALUE_DOMAIN: owner-group keys for StatusBarClockTweakHook: system_cc_clock_fontsize,system_cc_clock_verticaloffset
- DEFAULT_SEMANTICS: `system_cc_clock_fontsize` A14 default=9; A13 default=9
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null,null,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemClockHooks.kt::CCClockTweakHook` / `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook`: A13 already implements the exclusive keys system_cc_clock_fontsize,system_cc_clock_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_qs_force_systemfonts'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemClockHooks.kt::CCClockTweakHook` (hook, phases=intercept) vs A13 `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook` (hook, phases=after,before). Shared methods=['updateResources']; A14-only members=none; A13-only members=['com.android.systemui.qs.MiuiQSHeaderView#updateResources', 'com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow', 'com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>', 'com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange', 'com.android.systemui.statusbar.views.MiuiClock#<init>', 'com.android.systemui.statusbar.views.MiuiClock#setClockVisibility', 'com.android.systemui.statusbar.views.MiuiClock#updateTime'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_clock_fontsize` A14=9 A13=9. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.qs.MiuiNotificationHeaderView#updateResources; A13=com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange,com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#setClockVisibility,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.qs.MiuiNotificationHeaderView#updateResources,com.android.systemui.qs.MiuiQSHeaderView#updateResources,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>; shared_methods=['updateResources']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null,null,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_qs_force_systemfonts']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemClockHooks.kt::CCClockTweakHook` / `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook`: A13 already implements the exclusive keys system_cc_clock_fontsize,system_cc_clock_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_qs_force_systemfonts'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook__StatusBarClockTweakHook

- PROOF_ID: `PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_StatusBarClockTweakHook__StatusBarClockTweakHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `StatusBarClockTweakHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.views.MiuiClock#onDarkChanged,com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `StatusBarClockTweakHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange,com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#setClockVisibility,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.qs.MiuiNotificationHeaderView#updateResources,com.android.systemui.qs.MiuiQSHeaderView#updateResources,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_cc_clocktweak,system_cc_dateformat,system_cc_hidedate,system_statusbar_clocktweak`
- VALUE_DOMAIN: owner-group keys for StatusBarClockTweakHook: system_cc_clocktweak,system_cc_dateformat,system_cc_hidedate,system_statusbar_clocktweak
- DEFAULT_SEMANTICS: `system_cc_clocktweak` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemClockHooks.kt::StatusBarClockTweakHook` / `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook`: A13 already implements the exclusive keys system_cc_clocktweak,system_cc_dateformat,system_cc_hidedate,system_statusbar_clocktweak. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_drawer_hidedate', 'system_statusbar_enable_weather_param', 'system_statusbaricons_clock'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemClockHooks.kt::StatusBarClockTweakHook` (hook, phases=intercept) vs A13 `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook` (hook, phases=after,before). Shared methods=['<init>', 'onAttachedToWindow', 'updateTime']; A14-only members=['com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState', 'com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow', 'com.android.systemui.statusbar.views.MiuiClock#onDarkChanged', 'com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime']; A13-only members=['com.android.systemui.qs.MiuiNotificationHeaderView#updateResources', 'com.android.systemui.qs.MiuiQSHeaderView#updateResources', 'com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange', 'com.android.systemui.statusbar.views.MiuiClock#setClockVisibility'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_clocktweak` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.views.MiuiClock#onDarkChanged,com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>; A13=com.android.systemui.statusbar.policy.MiuiStatusBarClockController#fireTimeChange,com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#setClockVisibility,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.qs.MiuiNotificationHeaderView#updateResources,com.android.systemui.qs.MiuiQSHeaderView#updateResources,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>; shared_methods=['<init>', 'onAttachedToWindow', 'updateTime']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_drawer_hidedate', 'system_statusbar_enable_weather_param', 'system_statusbaricons_clock']; A14-only hook members=['com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState', 'com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow', 'com.android.systemui.statusbar.views.MiuiClock#onDarkChanged', 'com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemClockHooks.kt::StatusBarClockTweakHook` / `SystemStatusBarClockAndMoreHooks.kt::StatusBarClockTweakHook`: A13 already implements the exclusive keys system_cc_clocktweak,system_cc_dateformat,system_cc_hidedate,system_statusbar_clocktweak. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_drawer_hidedate', 'system_statusbar_enable_weather_param', 'system_statusbaricons_clock'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_ExpandNotificationsHook__ExpandNotificationsHook

- PROOF_ID: `PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_ExpandNotificationsHook__ExpandNotificationsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `ExpandNotificationsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `ExpandNotificationsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_cc_collapse_after_clicked`
- VALUE_DOMAIN: owner-group keys for ExpandNotificationsHook: system_cc_collapse_after_clicked
- DEFAULT_SEMANTICS: `system_cc_collapse_after_clicked` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `before`. Owner-group review of `SystemNotificationHooks.kt::ExpandNotificationsHook` / `SystemStatusBarClockAndMoreHooks.kt::ExpandNotificationsHook`: A13 already implements the exclusive keys system_cc_collapse_after_clicked. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemNotificationHooks.kt::ExpandNotificationsHook` (hook, phases=unknown) vs A13 `SystemStatusBarClockAndMoreHooks.kt::ExpandNotificationsHook` (hook, phases=before). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_collapse_after_clicked` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemNotificationHooks.kt::ExpandNotificationsHook` / `SystemStatusBarClockAndMoreHooks.kt::ExpandNotificationsHook`: A13 already implements the exclusive keys system_cc_collapse_after_clicked. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_initialValue__setupSystemUiResources

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_initialValue__setupSystemUiResources`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/SystemUiResourceBootstrap.kt`
- A14_SYMBOL: `setupSystemUiResources`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/SystemUiResourceBootstrap.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `initialValue`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_cc_enable_style_switch,system_statusbar_iconsize,system_statusbar_topmargin_val`
- VALUE_DOMAIN: owner-group keys for initialValue: system_cc_enable_style_switch,system_statusbar_iconsize,system_statusbar_topmargin_val
- DEFAULT_SEMANTICS: `system_cc_enable_style_switch` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemUiResourceBootstrap.kt::setupSystemUiResources` / `SystemUIStatusBarHooks.kt::initialValue`: A13 already implements the exclusive keys system_cc_enable_style_switch,system_statusbar_iconsize,system_statusbar_topmargin_val. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_drawer_date_fontsize', 'system_drawer_hidedate', 'system_lstimeout', 'system_taptounlock'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUiResourceBootstrap.kt::setupSystemUiResources` (hook, phases=unknown) vs A13 `SystemUIStatusBarHooks.kt::initialValue` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_enable_style_switch` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_drawer_date_fontsize', 'system_drawer_hidedate', 'system_lstimeout', 'system_taptounlock']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUiResourceBootstrap.kt::setupSystemUiResources` / `SystemUIStatusBarHooks.kt::initialValue`: A13 already implements the exclusive keys system_cc_enable_style_switch,system_statusbar_iconsize,system_statusbar_topmargin_val. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_drawer_date_fontsize', 'system_drawer_hidedate', 'system_lstimeout', 'system_taptounlock'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIMonitorAndTileHooks_kt_AddCustomTileHook__AddCustomTileHook

- PROOF_ID: `PROOF_OG_SystemUIMonitorAndTileHooks_kt_AddCustomTileHook__AddCustomTileHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt`
- A14_SYMBOL: `AddCustomTileHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.qs.tileimpl.MiuiQSFactory#createTile`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt`
- A13_SYMBOL: `AddCustomTileHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_cc_fpstile,system_fivegtile`
- VALUE_DOMAIN: owner-group keys for AddCustomTileHook: system_cc_fpstile,system_fivegtile
- DEFAULT_SEMANTICS: `system_cc_fpstile` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[tile,enable5G,enableFps,enableFloatingTime]; A13 returnAndSkip[tile,enable5G && TelephonyManager.getDefault(,enableFps,false]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after,before` vs A13 phase `after,before`. Owner-group review of `SystemUIMonitorAndTileHooks.kt::AddCustomTileHook` / `SystemUIMonitorAndTileHooks.kt::AddCustomTileHook`: A13 already implements the exclusive keys system_cc_fpstile,system_fivegtile. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_floatingtimetile'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIMonitorAndTileHooks.kt::AddCustomTileHook` (hook, phases=after,before) vs A13 `SystemUIMonitorAndTileHooks.kt::AddCustomTileHook` (hook, phases=after,before). Shared methods=['onCreate']; A14-only members=['com.android.systemui.qs.tileimpl.MiuiQSFactory#createTile']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_fpstile` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.qs.tileimpl.MiuiQSFactory#createTile; A13=com.android.systemui.SystemUIApplication#onCreate; shared_methods=['onCreate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after,before; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[tile,enable5G,enableFps,enableFloatingTime]; A13 returnAndSkip[tile,enable5G && TelephonyManager.getDefault(,enableFps,false]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_floatingtimetile']; A14-only hook members=['com.android.systemui.qs.tileimpl.MiuiQSFactory#createTile']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIMonitorAndTileHooks.kt::AddCustomTileHook` / `SystemUIMonitorAndTileHooks.kt::AddCustomTileHook`: A13 already implements the exclusive keys system_cc_fpstile,system_fivegtile. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_floatingtimetile'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUiInstaller_java_install__CCHeaderHook

- PROOF_ID: `PROOF_OG_SystemUiInstaller_java_install__CCHeaderHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `CCHeaderHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_SYMBOL: `install`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cc_hideoperator_delimiter,system_qs_hideoperator`
- VALUE_DOMAIN: owner-group keys for install: system_cc_hideoperator_delimiter,system_qs_hideoperator
- DEFAULT_SEMANTICS: `system_cc_hideoperator_delimiter` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `after` vs A13 phase `after`. Owner-group review of `SystemUIControlCenterHooks.kt::CCHeaderHook` / `SystemUiInstaller.java::install`: A13 already implements the exclusive keys system_cc_hideoperator_delimiter,system_qs_hideoperator. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::CCHeaderHook` (hook, phases=after) vs A13 `SystemUiInstaller.java::install` (installer, phases=after). Shared methods=none; A14-only members=['miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint']; A13-only members=['com.android.systemui.SystemUIApplication#onCreate'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_hideoperator_delimiter` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint; A13=com.android.systemui.SystemUIApplication#onCreate; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::CCHeaderHook` / `SystemUiInstaller.java::install`: A13 already implements the exclusive keys system_cc_hideoperator_delimiter,system_qs_hideoperator. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_ShowCCStepCountHook__CCHeaderHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_ShowCCStepCountHook__CCHeaderHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `CCHeaderHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `ShowCCStepCountHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.qs.MiuiNotificationHeaderView#themeChanged,com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar#updateHeaderColor`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cc_show_stepcount`
- VALUE_DOMAIN: owner-group keys for ShowCCStepCountHook: system_cc_show_stepcount
- DEFAULT_SEMANTICS: `system_cc_show_stepcount` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `after` vs A13 phase `after`. Owner-group review of `SystemUIControlCenterHooks.kt::CCHeaderHook` / `SystemUIControlCenterHooks.kt::ShowCCStepCountHook`: A13 already implements the exclusive keys system_cc_show_stepcount. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_hideoperator_delimiter', 'system_qs_hideoperator'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::CCHeaderHook` (hook, phases=after) vs A13 `SystemUIControlCenterHooks.kt::ShowCCStepCountHook` (hook, phases=after). Shared methods=none; A14-only members=['miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint']; A13-only members=['com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar#updateHeaderColor', 'com.android.systemui.qs.MiuiNotificationHeaderView#themeChanged'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_show_stepcount` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews,miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint; A13=com.android.systemui.qs.MiuiNotificationHeaderView#themeChanged,com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar#updateHeaderColor; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_hideoperator_delimiter', 'system_qs_hideoperator']; A14-only hook members=['miui.systemui.controlcenter.panel.main.header.StatusHeaderController#adjustCarrierOrPrompt', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#createStatusBarViews', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#onExpandChange', 'miui.systemui.controlcenter.panel.main.header.StatusHeaderController#updateConstraint']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::CCHeaderHook` / `SystemUIControlCenterHooks.kt::ShowCCStepCountHook`: A13 already implements the exclusive keys system_cc_show_stepcount. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_hideoperator_delimiter', 'system_qs_hideoperator'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_HideMobileNetworkIndicatorHook__HideMobileNetworkIndicatorHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_HideMobileNetworkIndicatorHook__HideMobileNetworkIndicatorHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HideMobileNetworkIndicatorHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarMobileView#applyMobileState,com.android.systemui.statusbar.StatusBarMobileView#updateState`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideMobileNetworkIndicatorHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cc_switch_qsandnotification,system_expandnotifs,system_mobiletypeicon,system_networkindicator_mobile,system_statusbar_mobiletype_show_wificonnected`
- VALUE_DOMAIN: owner-group keys for HideMobileNetworkIndicatorHook: system_cc_switch_qsandnotification,system_expandnotifs,system_mobiletypeicon,system_networkindicator_mobile,system_statusbar_mobiletype_show_wificonnected
- DEFAULT_SEMANTICS: `system_cc_switch_qsandnotification` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before,after` vs A13 phase `after`. Owner-group review of `SystemUIStatusBarHooks.kt::HideMobileNetworkIndicatorHook` / `SystemUIStatusBarHooks.kt::HideMobileNetworkIndicatorHook`: A13 already implements the exclusive keys system_cc_switch_qsandnotification,system_expandnotifs,system_mobiletypeicon,system_networkindicator_mobile,system_statusbar_mobiletype_show_wificonnected. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::HideMobileNetworkIndicatorHook` (hook, phases=before,after) vs A13 `SystemUIStatusBarHooks.kt::HideMobileNetworkIndicatorHook` (hook, phases=after). Shared methods=['updateState']; A14-only members=['com.android.systemui.statusbar.StatusBarMobileView#applyMobileState']; A13-only members=['com.android.systemui.statusbar.StatusBarMobileView#initViewState'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_switch_qsandnotification` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.StatusBarMobileView#applyMobileState,com.android.systemui.statusbar.StatusBarMobileView#updateState; A13=com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState; shared_methods=['updateState']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before,after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.StatusBarMobileView#applyMobileState']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::HideMobileNetworkIndicatorHook` / `SystemUIStatusBarHooks.kt::HideMobileNetworkIndicatorHook`: A13 already implements the exclusive keys system_cc_switch_qsandnotification,system_expandnotifs,system_mobiletypeicon,system_networkindicator_mobile,system_statusbar_mobiletype_show_wificonnected. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_ShowVolumePctHook__ShowVolumePctHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_ShowVolumePctHook__ShowVolumePctHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `ShowVolumePctHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener#onProgressChanged,#showVolumeDialogH,#dismissH`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `ShowVolumePctHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener#onProgressChanged,#showVolumeDialogH,#dismissH`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cc_tile_roundedrect,system_cc_volume_showpct,system_volumebar_blur_mtk,system_volumetimer`
- VALUE_DOMAIN: owner-group keys for ShowVolumePctHook: system_cc_tile_roundedrect,system_cc_volume_showpct,system_volumebar_blur_mtk,system_volumetimer
- DEFAULT_SEMANTICS: `system_cc_tile_roundedrect` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after` vs A13 phase `after`. Owner-group review of `SystemUIControlCenterHooks.kt::ShowVolumePctHook` / `SystemUIControlCenterHooks.kt::ShowVolumePctHook`: A13 already implements the exclusive keys system_cc_tile_roundedrect,system_cc_volume_showpct,system_volumebar_blur_mtk,system_volumetimer. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::ShowVolumePctHook` (hook, phases=after) vs A13 `SystemUIControlCenterHooks.kt::ShowVolumePctHook` (hook, phases=after). Shared methods=['dismissH', 'onProgressChanged', 'showVolumeDialogH']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cc_tile_roundedrect` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener#onProgressChanged,#showVolumeDialogH,#dismissH; A13=com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener#onProgressChanged,#showVolumeDialogH,#dismissH; shared_methods=['dismissH', 'onProgressChanged', 'showVolumeDialogH']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::ShowVolumePctHook` / `SystemUIControlCenterHooks.kt::ShowVolumePctHook`: A13 already implements the exclusive keys system_cc_tile_roundedrect,system_cc_volume_showpct,system_volumebar_blur_mtk,system_volumetimer. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_SystemCCGridHook__initControlCenter

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_SystemCCGridHook__initControlCenter`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `initControlCenter`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl#vibrateH`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `SystemCCGridHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,miui.systemui.controlcenter.qs.tileview.StandardTileView#createLabel,miui.systemui.controlcenter.qs.QSPager#distributeTiles`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_ccgridcolumns`
- VALUE_DOMAIN: owner-group keys for SystemCCGridHook: system_ccgridcolumns
- DEFAULT_SEMANTICS: `system_ccgridcolumns` A14 default=4; A13 default=4
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `after`. Owner-group review of `SystemUIControlCenterHooks.kt::initControlCenter` / `SystemUIControlCenterHooks.kt::SystemCCGridHook`: A13 already implements the exclusive keys system_ccgridcolumns. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_btandtorch_ascard', 'system_cc_card_enabled_color', 'system_cc_hide_edit', 'system_cc_hide_profile_monitoring', 'system_cc_hideoperator_delimiter', 'system_cc_show_stepcount', 'system_cc_slider_color_enable', 'system_cc_tile_enabled_color', 'system_cc_tile_roundedrect', 'system_cc_volume_showpct', 'system_nosilentvibrate', 'system_qs_hideoperator', 'system_volume_mode_button_colors', 'system_volumebar_blur_mtk', 'system_volumeblur_collapsed', 'system_volumeblur_expanded', 'system_volumedialogdelay_collapsed', 'system_volumedialogdelay_expanded', 'system_volumetimer'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::initControlCenter` (hook, phases=unknown) vs A13 `SystemUIControlCenterHooks.kt::SystemCCGridHook` (hook, phases=after). Shared methods=none; A14-only members=['com.android.systemui.miui.volume.MiuiVolumeDialogImpl#vibrateH']; A13-only members=['com.android.systemui.SystemUIApplication#onCreate', 'miui.systemui.controlcenter.qs.QSPager#distributeTiles', 'miui.systemui.controlcenter.qs.tileview.StandardTileView#createLabel'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_ccgridcolumns` A14=4 A13=4. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.miui.volume.MiuiVolumeDialogImpl#vibrateH; A13=com.android.systemui.SystemUIApplication#onCreate,miui.systemui.controlcenter.qs.tileview.StandardTileView#createLabel,miui.systemui.controlcenter.qs.QSPager#distributeTiles; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_btandtorch_ascard', 'system_cc_card_enabled_color', 'system_cc_hide_edit', 'system_cc_hide_profile_monitoring', 'system_cc_hideoperator_delimiter', 'system_cc_show_stepcount', 'system_cc_slider_color_enable', 'system_cc_tile_enabled_color', 'system_cc_tile_roundedrect', 'system_cc_volume_showpct', 'system_nosilentvibrate', 'system_qs_hideoperator', 'system_volume_mode_button_colors', 'system_volumebar_blur_mtk', 'system_volumeblur_collapsed', 'system_volumeblur_expanded', 'system_volumedialogdelay_collapsed', 'system_volumedialogdelay_expanded', 'system_volumetimer']; A14-only hook members=['com.android.systemui.miui.volume.MiuiVolumeDialogImpl#vibrateH']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::initControlCenter` / `SystemUIControlCenterHooks.kt::SystemCCGridHook`: A13 already implements the exclusive keys system_ccgridcolumns. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_btandtorch_ascard', 'system_cc_card_enabled_color', 'system_cc_hide_edit', 'system_cc_hide_profile_monitoring', 'system_cc_hideoperator_delimiter', 'system_cc_show_stepcount', 'system_cc_slider_color_enable', 'system_cc_tile_enabled_color', 'system_cc_tile_roundedrect', 'system_cc_volume_showpct', 'system_nosilentvibrate', 'system_qs_hideoperator', 'system_volume_mode_button_colors', 'system_volumebar_blur_mtk', 'system_volumeblur_collapsed', 'system_volumeblur_expanded', 'system_volumedialogdelay_collapsed', 'system_volumedialogdelay_expanded', 'system_volumetimer'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationPopupsHooks_kt_BetterPopupsHideDelayHook__BetterPopupsHideDelayHook

- PROOF_ID: `PROOF_OG_SystemNotificationPopupsHooks_kt_BetterPopupsHideDelayHook__BetterPopupsHideDelayHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `BetterPopupsHideDelayHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationPopupsHooks.kt`
- A13_SYMBOL: `BetterPopupsHideDelayHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.HeadsUpManager#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_chargeanimtime,system_drawer_blur,system_networkindicator_wifi`
- VALUE_DOMAIN: owner-group keys for BetterPopupsHideDelayHook: system_chargeanimtime,system_drawer_blur,system_networkindicator_wifi
- DEFAULT_SEMANTICS: `system_chargeanimtime` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemNotificationHooks.kt::BetterPopupsHideDelayHook` / `SystemNotificationPopupsHooks.kt::BetterPopupsHideDelayHook`: A13 already implements the exclusive keys system_chargeanimtime,system_drawer_blur,system_networkindicator_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemNotificationHooks.kt::BetterPopupsHideDelayHook` (hook, phases=intercept) vs A13 `SystemNotificationPopupsHooks.kt::BetterPopupsHideDelayHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=['com.android.systemui.statusbar.policy.HeadsUpManager#<init>'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_chargeanimtime` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.systemui.statusbar.policy.HeadsUpManager#<init>; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemNotificationHooks.kt::BetterPopupsHideDelayHook` / `SystemNotificationPopupsHooks.kt::BetterPopupsHideDelayHook`: A13 already implements the exclusive keys system_chargeanimtime,system_drawer_blur,system_networkindicator_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarMoreHooks_kt_HideIconsBattery1Hook__HideIconsBattery1Hook

- PROOF_ID: `PROOF_OG_SystemStatusBarMoreHooks_kt_HideIconsBattery1Hook__HideIconsBattery1Hook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_SYMBOL: `HideIconsBattery1Hook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `HideIconsBattery1Hook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiBatteryMeterView#initMiuiView`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_charginginfo,system_secureqs`
- VALUE_DOMAIN: owner-group keys for HideIconsBattery1Hook: system_charginginfo,system_secureqs
- DEFAULT_SEMANTICS: `system_charginginfo` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemStatusBarIconHooks.kt::HideIconsBattery1Hook` / `SystemStatusBarMoreHooks.kt::HideIconsBattery1Hook`: A13 already implements the exclusive keys system_charginginfo,system_secureqs. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemStatusBarIconHooks.kt::HideIconsBattery1Hook` (hook, phases=intercept) vs A13 `SystemStatusBarMoreHooks.kt::HideIconsBattery1Hook` (hook, phases=after). Shared methods=none; A14-only members=['com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll']; A13-only members=['com.android.systemui.statusbar.views.MiuiBatteryMeterView#initMiuiView'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_charginginfo` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll; A13=com.android.systemui.statusbar.views.MiuiBatteryMeterView#initMiuiView; shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemStatusBarIconHooks.kt::HideIconsBattery1Hook` / `SystemStatusBarMoreHooks.kt::HideIconsBattery1Hook`: A13 already implements the exclusive keys system_charginginfo,system_secureqs. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemChargingAndWallpaperHooks_kt_ChargingInfoHook__buildChargingInfoDetails

- PROOF_ID: `PROOF_OG_SystemChargingAndWallpaperHooks_kt_ChargingInfoHook__buildChargingInfoDetails`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `buildChargingInfoDetails`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt`
- A13_SYMBOL: `ChargingInfoHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt`
- A13_HOOK_TARGETS: `com.android.keyguard.charge.ChargeUtils#getChargingHintText,com.android.systemui.statusbar.phone.KeyguardIndicationTextView#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_charginginfo_current,system_charginginfo_temp,system_charginginfo_view,system_charginginfo_voltage,system_charginginfo_wattage`
- VALUE_DOMAIN: owner-group keys for ChargingInfoHook: system_charginginfo_current,system_charginginfo_temp,system_charginginfo_view,system_charginginfo_voltage,system_charginginfo_wattage
- DEFAULT_SEMANTICS: `system_charginginfo_current` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 setResult[hint + "\n" + info,hint + " · " + info,info + " · " + hint]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::buildChargingInfoDetails` / `SystemChargingAndWallpaperHooks.kt::ChargingInfoHook`: A13 already implements the exclusive keys system_charginginfo_current,system_charginginfo_temp,system_charginginfo_view,system_charginginfo_voltage,system_charginginfo_wattage. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_charginginfo'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::buildChargingInfoDetails` (hook, phases=unknown) vs A13 `SystemChargingAndWallpaperHooks.kt::ChargingInfoHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=['com.android.keyguard.charge.ChargeUtils#getChargingHintText', 'com.android.systemui.statusbar.phone.KeyguardIndicationTextView#<init>'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_charginginfo_current` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.keyguard.charge.ChargeUtils#getChargingHintText,com.android.systemui.statusbar.phone.KeyguardIndicationTextView#<init>; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 setResult[hint + "\n" + info,hint + " · " + info,info + " · " + hint]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_charginginfo']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::buildChargingInfoDetails` / `SystemChargingAndWallpaperHooks.kt::ChargingInfoHook`: A13 already implements the exclusive keys system_charginginfo_current,system_charginginfo_temp,system_charginginfo_view,system_charginginfo_voltage,system_charginginfo_wattage. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_charginginfo'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_SystemShareAndOpenWithHooks_kt_CleanOpenWithMenuHook__CleanOpenWithMenuHook

- PROOF_ID: `PROOF_OG_SystemShareAndOpenWithHooks_kt_CleanOpenWithMenuHook__CleanOpenWithMenuHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareMenuHooks.kt`
- A14_SYMBOL: `CleanOpenWithMenuHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareMenuHooks.kt`
- A14_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt`
- A13_SYMBOL: `CleanOpenWithMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java`
- A13_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cleanopenwith,system_cleanopenwith_apps,system_cleanshare`
- VALUE_DOMAIN: owner-group keys for CleanOpenWithMenuHook: system_cleanopenwith,system_cleanopenwith_apps,system_cleanshare
- DEFAULT_SEMANTICS: `system_cleanopenwith` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemShareMenuHooks.kt::CleanOpenWithMenuHook` / `SystemShareAndOpenWithHooks.kt::CleanOpenWithMenuHook`: A13 already implements the exclusive keys system_cleanopenwith,system_cleanopenwith_apps,system_cleanshare. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemShareMenuHooks.kt::CleanOpenWithMenuHook` (hook, phases=intercept) vs A13 `SystemShareAndOpenWithHooks.kt::CleanOpenWithMenuHook` (hook, phases=after). Shared methods=['run']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cleanopenwith` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; A13=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; shared_methods=['run']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemShareMenuHooks.kt::CleanOpenWithMenuHook` / `SystemShareAndOpenWithHooks.kt::CleanOpenWithMenuHook`: A13 already implements the exclusive keys system_cleanopenwith,system_cleanopenwith_apps,system_cleanshare. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemShareAndOpenWithHooks_kt_CleanShareMenuHook__CleanShareMenuHook

- PROOF_ID: `PROOF_OG_SystemShareAndOpenWithHooks_kt_CleanShareMenuHook__CleanShareMenuHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareMenuHooks.kt`
- A14_SYMBOL: `CleanShareMenuHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareMenuHooks.kt`
- A14_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt`
- A13_SYMBOL: `CleanShareMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt`
- A13_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cleanshare_apps`
- VALUE_DOMAIN: owner-group keys for CleanShareMenuHook: system_cleanshare_apps
- DEFAULT_SEMANTICS: `system_cleanshare_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemShareMenuHooks.kt::CleanShareMenuHook` / `SystemShareAndOpenWithHooks.kt::CleanShareMenuHook`: A13 already implements the exclusive keys system_cleanshare_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemShareMenuHooks.kt::CleanShareMenuHook` (hook, phases=intercept) vs A13 `SystemShareAndOpenWithHooks.kt::CleanShareMenuHook` (hook, phases=after). Shared methods=['run']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_cleanshare_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; A13=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; shared_methods=['run']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemShareMenuHooks.kt::CleanShareMenuHook` / `SystemShareAndOpenWithHooks.kt::CleanShareMenuHook`: A13 already implements the exclusive keys system_cleanshare_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ClearAllTasksHook__ClearAllTasksHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ClearAllTasksHook__ClearAllTasksHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ClearAllTasksHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `ClearAllTasksHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_clearalltasks`
- VALUE_DOMAIN: owner-group keys for ClearAllTasksHook: system_clearalltasks
- DEFAULT_SEMANTICS: `system_clearalltasks` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 setResult[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `System.kt::ClearAllTasksHook` / `SystemAudioAndVisualAndMoreHooks.kt::ClearAllTasksHook`: A13 already implements the exclusive keys system_clearalltasks. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::ClearAllTasksHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::ClearAllTasksHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_clearalltasks` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 setResult[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::ClearAllTasksHook` / `SystemAudioAndVisualAndMoreHooks.kt::ClearAllTasksHook`: A13 already implements the exclusive keys system_clearalltasks. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUINotificationHooks_kt_OpenNotifyInFloatingWindowHook__OpenNotifyInFloatingWindowHook

- PROOF_ID: `PROOF_OG_SystemUINotificationHooks_kt_OpenNotifyInFloatingWindowHook__OpenNotifyInFloatingWindowHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `OpenNotifyInFloatingWindowHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter#onNotificationClicked`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `OpenNotifyInFloatingWindowHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#startNotificationIntent`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_colorizenotifs,system_notify_openinfw_apps,system_notify_openinfw_in_whitelist,system_statusbar_dualsimin2rows`
- VALUE_DOMAIN: owner-group keys for OpenNotifyInFloatingWindowHook: system_colorizenotifs,system_notify_openinfw_apps,system_notify_openinfw_in_whitelist,system_statusbar_dualsimin2rows
- DEFAULT_SEMANTICS: `system_colorizenotifs` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `before,after` vs A13 phase `before`. Owner-group review of `SystemUINotificationHooks.kt::OpenNotifyInFloatingWindowHook` / `SystemUINotificationHooks.kt::OpenNotifyInFloatingWindowHook`: A13 already implements the exclusive keys system_colorizenotifs,system_notify_openinfw_apps,system_notify_openinfw_in_whitelist,system_statusbar_dualsimin2rows. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUINotificationHooks.kt::OpenNotifyInFloatingWindowHook` (hook, phases=before,after) vs A13 `SystemUINotificationHooks.kt::OpenNotifyInFloatingWindowHook` (hook, phases=before). Shared methods=none; A14-only members=['com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter#onNotificationClicked']; A13-only members=['#startNotificationIntent'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_colorizenotifs` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter#onNotificationClicked; A13=#startNotificationIntent; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before,after; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter#onNotificationClicked']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUINotificationHooks.kt::OpenNotifyInFloatingWindowHook` / `SystemUINotificationHooks.kt::OpenNotifyInFloatingWindowHook`: A13 already implements the exclusive keys system_colorizenotifs,system_notify_openinfw_apps,system_notify_openinfw_in_whitelist,system_statusbar_dualsimin2rows. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationAndShareHooks_kt_ColorizeNotificationCardHook__ColorizeNotificationCardHook

- PROOF_ID: `PROOF_OG_SystemNotificationAndShareHooks_kt_ColorizeNotificationCardHook__ColorizeNotificationCardHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemColorizeNotificationHooks.kt`
- A14_SYMBOL: `ColorizeNotificationCardHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemColorizeNotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateBlurBg,com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#onNotificationUpdated,com.android.systemui.statusbar.notification.row.NotificationBackgroundView#setTint,com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper#getCustomBackgroundColor,com.android.systemui.statusbar.notification.row.NotificationContentView#updateAllSingleLineViews,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#handle3thThemeColor,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createRemoteViews`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt`
- A13_SYMBOL: `ColorizeNotificationCardHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#updateNotificationColor,com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#onNotificationUpdated,com.android.systemui.statusbar.notification.row.NotificationBackgroundView#setTint,com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper#getCustomBackgroundColor,com.android.systemui.statusbar.notification.row.HybridGroupManager#bindFromNotificationWithStyle,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#handle3thThemeColor,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiContentView,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiExpandedView,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiPublicView`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_colorizenotifs_apps`
- VALUE_DOMAIN: owner-group keys for ColorizeNotificationCardHook: system_colorizenotifs_apps
- DEFAULT_SEMANTICS: `system_colorizenotifs_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null,XposedHelpers.getObjectField(param.thisObject, "mBackgroundColor",null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemColorizeNotificationHooks.kt::ColorizeNotificationCardHook` / `SystemNotificationAndShareHooks.kt::ColorizeNotificationCardHook`: A13 already implements the exclusive keys system_colorizenotifs_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemColorizeNotificationHooks.kt::ColorizeNotificationCardHook` (hook, phases=intercept) vs A13 `SystemNotificationAndShareHooks.kt::ColorizeNotificationCardHook` (hook, phases=after,before). Shared methods=['getCustomBackgroundColor', 'handle3thThemeColor', 'onNotificationUpdated', 'setTint']; A14-only members=['com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateBlurBg', 'com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createRemoteViews', 'com.android.systemui.statusbar.notification.row.NotificationContentView#updateAllSingleLineViews']; A13-only members=['com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#updateNotificationColor', 'com.android.systemui.statusbar.notification.row.HybridGroupManager#bindFromNotificationWithStyle', 'com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiContentView', 'com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiExpandedView', 'com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiPublicView'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_colorizenotifs_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateBlurBg,com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#onNotificationUpdated,com.android.systemui.statusbar.notification.row.NotificationBackgroundView#setTint,com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper#getCustomBackgroundColor,com.android.systemui.statusbar.notification.row.NotificationContentView#updateAllSingleLineViews,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#handle3thThemeColor,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createRemoteViews; A13=com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#updateNotificationColor,com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#onNotificationUpdated,com.android.systemui.statusbar.notification.row.NotificationBackgroundView#setTint,com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper#getCustomBackgroundColor,com.android.systemui.statusbar.notification.row.HybridGroupManager#bindFromNotificationWithStyle,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#handle3thThemeColor,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiContentView,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiExpandedView,com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createMiuiPublicView; shared_methods=['getCustomBackgroundColor', 'handle3thThemeColor', 'onNotificationUpdated', 'setTint']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null,XposedHelpers.getObjectField(param.thisObject, "mBackgroundColor",null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow#updateBlurBg', 'com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector#createRemoteViews', 'com.android.systemui.statusbar.notification.row.NotificationContentView#updateAllSingleLineViews']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemColorizeNotificationHooks.kt::ColorizeNotificationCardHook` / `SystemNotificationAndShareHooks.kt::ColorizeNotificationCardHook`: A13 already implements the exclusive keys system_colorizenotifs_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_TapToUnlockHook__TapToUnlockHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_TapToUnlockHook__TapToUnlockHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `TapToUnlockHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `#handleMiuiTouch`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `TapToUnlockHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.keyguard.injector.KeyguardPanelViewInjector#onTouchEvent,com.android.keyguard.injector.KeyguardPanelViewInjector#onInterceptTouchEvent`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_detailednetspeed_align,system_netspeed_fixedcontent_width,system_netspeed_fontsize,system_netspeed_leftmargin,system_netspeed_rightmargin,system_netspeed_verticaloffset,system_taptounlock`
- VALUE_DOMAIN: owner-group keys for TapToUnlockHook: system_detailednetspeed_align,system_netspeed_fixedcontent_width,system_netspeed_fontsize,system_netspeed_leftmargin,system_netspeed_rightmargin,system_netspeed_verticaloffset,system_taptounlock
- DEFAULT_SEMANTICS: `system_detailednetspeed_align` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::TapToUnlockHook` / `SystemAudioAndVisualAndMoreHooks.kt::TapToUnlockHook`: A13 already implements the exclusive keys system_detailednetspeed_align,system_netspeed_fixedcontent_width,system_netspeed_fontsize,system_netspeed_leftmargin,system_netspeed_rightmargin,system_netspeed_verticaloffset,system_taptounlock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::TapToUnlockHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::TapToUnlockHook` (hook, phases=after). Shared methods=none; A14-only members=['#handleMiuiTouch']; A13-only members=['com.android.keyguard.injector.KeyguardPanelViewInjector#onInterceptTouchEvent', 'com.android.keyguard.injector.KeyguardPanelViewInjector#onTouchEvent'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_detailednetspeed_align` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=#handleMiuiTouch; A13=com.android.keyguard.injector.KeyguardPanelViewInjector#onTouchEvent,com.android.keyguard.injector.KeyguardPanelViewInjector#onInterceptTouchEvent; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#handleMiuiTouch']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::TapToUnlockHook` / `SystemAudioAndVisualAndMoreHooks.kt::TapToUnlockHook`: A13 already implements the exclusive keys system_detailednetspeed_align,system_netspeed_fixedcontent_width,system_netspeed_fontsize,system_netspeed_leftmargin,system_netspeed_rightmargin,system_netspeed_verticaloffset,system_taptounlock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_DetailedNetSpeedHook__buildDetailedNetSpeedFormatSnapshot

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_DetailedNetSpeedHook__buildDetailedNetSpeedFormatSnapshot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildDetailedNetSpeedFormatSnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DetailedNetSpeedHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `#updateNetworkSpeed,#updateText`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_detailednetspeed_icon`
- VALUE_DOMAIN: owner-group keys for DetailedNetSpeedHook: system_detailednetspeed_icon
- DEFAULT_SEMANTICS: `system_detailednetspeed_icon` A14 default=2; A13 default=2
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `before`. Owner-group review of `SystemUIStatusBarHooks.kt::buildDetailedNetSpeedFormatSnapshot` / `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook`: A13 already implements the exclusive keys system_detailednetspeed_icon. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_detailednetspeed_style'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::buildDetailedNetSpeedFormatSnapshot` (hook, phases=unknown) vs A13 `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook` (hook, phases=before). Shared methods=none; A14-only members=none; A13-only members=['#updateNetworkSpeed', '#updateText'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_detailednetspeed_icon` A14=2 A13=2. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=#updateNetworkSpeed,#updateText; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_detailednetspeed_style']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::buildDetailedNetSpeedFormatSnapshot` / `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook`: A13 already implements the exclusive keys system_detailednetspeed_icon. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_detailednetspeed_style'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_FormatNetworkSpeedHook__buildDetailedNetSpeedFormatSnapshot

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_FormatNetworkSpeedHook__buildDetailedNetSpeedFormatSnapshot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildDetailedNetSpeedFormatSnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `FormatNetworkSpeedHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.NetworkSpeedController#formatSpeed`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_detailednetspeed_low,system_detailednetspeed_lowlevel,system_detailednetspeed_secunit`
- VALUE_DOMAIN: owner-group keys for FormatNetworkSpeedHook: system_detailednetspeed_low,system_detailednetspeed_lowlevel,system_detailednetspeed_secunit
- DEFAULT_SEMANTICS: `system_detailednetspeed_low` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[arrayOf("", "",""]; setResult[stripNetSpeedSuffix(speedText]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `before,after`. Owner-group review of `SystemUIStatusBarHooks.kt::buildDetailedNetSpeedFormatSnapshot` / `SystemUIStatusBarHooks.kt::FormatNetworkSpeedHook`: A13 already implements the exclusive keys system_detailednetspeed_low,system_detailednetspeed_lowlevel,system_detailednetspeed_secunit. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_detailednetspeed_icon', 'system_detailednetspeed_style'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::buildDetailedNetSpeedFormatSnapshot` (hook, phases=unknown) vs A13 `SystemUIStatusBarHooks.kt::FormatNetworkSpeedHook` (hook, phases=before,after). Shared methods=none; A14-only members=none; A13-only members=['com.android.systemui.statusbar.policy.NetworkSpeedController#formatSpeed'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_detailednetspeed_low` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.systemui.statusbar.policy.NetworkSpeedController#formatSpeed; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[arrayOf("", "",""]; setResult[stripNetSpeedSuffix(speedText]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_detailednetspeed_icon', 'system_detailednetspeed_style']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::buildDetailedNetSpeedFormatSnapshot` / `SystemUIStatusBarHooks.kt::FormatNetworkSpeedHook`: A13 already implements the exclusive keys system_detailednetspeed_low,system_detailednetspeed_lowlevel,system_detailednetspeed_secunit. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_detailednetspeed_icon', 'system_detailednetspeed_style'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ScreenDimTimeHook__ScreenDimTimeHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ScreenDimTimeHook__ScreenDimTimeHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `ScreenDimTimeHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.power.PowerManagerService#readConfigurationLocked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `ScreenDimTimeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.power.PowerManagerService#readConfigurationLocked,com.android.server.power.PowerManagerService#setStayOnSettingInternal`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_dimtime`
- VALUE_DOMAIN: owner-group keys for ScreenDimTimeHook: system_dimtime
- DEFAULT_SEMANTICS: `system_dimtime` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemDisplayHooks.kt::ScreenDimTimeHook` / `SystemAudioAndVisualAndMoreHooks.kt::ScreenDimTimeHook`: A13 already implements the exclusive keys system_dimtime. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_dimtime'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemDisplayHooks.kt::ScreenDimTimeHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::ScreenDimTimeHook` (hook, phases=after,before). Shared methods=['readConfigurationLocked']; A14-only members=none; A13-only members=['com.android.server.power.PowerManagerService#setStayOnSettingInternal'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_dimtime` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.power.PowerManagerService#readConfigurationLocked; A13=com.android.server.power.PowerManagerService#readConfigurationLocked,com.android.server.power.PowerManagerService#setStayOnSettingInternal; shared_methods=['readConfigurationLocked']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_dimtime']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemDisplayHooks.kt::ScreenDimTimeHook` / `SystemAudioAndVisualAndMoreHooks.kt::ScreenDimTimeHook`: A13 already implements the exclusive keys system_dimtime. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_dimtime'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSecurityAndSystemHooks_kt_DisableSystemIntegrityHook__DisableSystemIntegrityHook

- PROOF_ID: `PROOF_OG_SystemSecurityAndSystemHooks_kt_DisableSystemIntegrityHook__DisableSystemIntegrityHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `DisableSystemIntegrityHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_HOOK_TARGETS: `android.util.apk.ApkSignatureVerifier#getMinimumSignatureSchemeVersionForTargetSdk`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `DisableSystemIntegrityHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `android.util.apk.ApkSignatureVerifier#getMinimumSignatureSchemeVersionForTargetSdk`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_disableintegrity,system_forceclose`
- VALUE_DOMAIN: owner-group keys for DisableSystemIntegrityHook: system_disableintegrity,system_forceclose
- DEFAULT_SEMANTICS: `system_disableintegrity` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemSecurityHooks.kt::DisableSystemIntegrityHook` / `SystemSecurityAndSystemHooks.kt::DisableSystemIntegrityHook`: A13 already implements the exclusive keys system_disableintegrity,system_forceclose. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemSecurityHooks.kt::DisableSystemIntegrityHook` (hook, phases=unknown) vs A13 `SystemSecurityAndSystemHooks.kt::DisableSystemIntegrityHook` (hook, phases=unknown). Shared methods=['getMinimumSignatureSchemeVersionForTargetSdk']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_disableintegrity` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=android.util.apk.ApkSignatureVerifier#getMinimumSignatureSchemeVersionForTargetSdk; A13=android.util.apk.ApkSignatureVerifier#getMinimumSignatureSchemeVersionForTargetSdk; shared_methods=['getMinimumSignatureSchemeVersionForTargetSdk']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemSecurityHooks.kt::DisableSystemIntegrityHook` / `SystemSecurityAndSystemHooks.kt::DisableSystemIntegrityHook`: A13 already implements the exclusive keys system_disableintegrity,system_forceclose. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSecurityAndSystemHooks_kt_NoVersionCheckHook__NoVersionCheckHook

- PROOF_ID: `PROOF_OG_SystemSecurityAndSystemHooks_kt_NoVersionCheckHook__NoVersionCheckHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `NoVersionCheckHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.pm.PackageManagerServiceUtils#checkDowngrade`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `NoVersionCheckHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.pm.PackageManagerServiceUtils#checkDowngrade`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_downgrade`
- VALUE_DOMAIN: owner-group keys for NoVersionCheckHook: system_downgrade
- DEFAULT_SEMANTICS: `system_downgrade` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemSecurityHooks.kt::NoVersionCheckHook` / `SystemSecurityAndSystemHooks.kt::NoVersionCheckHook`: A13 already implements the exclusive keys system_downgrade. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemSecurityHooks.kt::NoVersionCheckHook` (hook, phases=unknown) vs A13 `SystemSecurityAndSystemHooks.kt::NoVersionCheckHook` (hook, phases=unknown). Shared methods=['checkDowngrade']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_downgrade` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.pm.PackageManagerServiceUtils#checkDowngrade; A13=com.android.server.pm.PackageManagerServiceUtils#checkDowngrade; shared_methods=['checkDowngrade']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemSecurityHooks.kt::NoVersionCheckHook` / `SystemSecurityAndSystemHooks.kt::NoVersionCheckHook`: A13 already implements the exclusive keys system_downgrade. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUINotificationHooks_kt_HideNoficationAccessIconHook__HideNoficationAccessIconHook

- PROOF_ID: `PROOF_OG_SystemUINotificationHooks_kt_HideNoficationAccessIconHook__HideNoficationAccessIconHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `HideNoficationAccessIconHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.qs.MiuiQSHeaderView#updateShortCutVisibility,com.android.systemui.qs.MiuiNotificationHeaderView#updateShortCutVisibility`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `HideNoficationAccessIconHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.qs.MiuiQSHeaderView#updateShortCutVisibility,com.android.systemui.qs.MiuiNotificationHeaderView#updateShortCutVisibility`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_drawer_removeshortcut`
- VALUE_DOMAIN: owner-group keys for HideNoficationAccessIconHook: system_drawer_removeshortcut
- DEFAULT_SEMANTICS: `system_drawer_removeshortcut` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before` vs A13 phase `before`. Owner-group review of `SystemUINotificationHooks.kt::HideNoficationAccessIconHook` / `SystemUINotificationHooks.kt::HideNoficationAccessIconHook`: A13 already implements the exclusive keys system_drawer_removeshortcut. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUINotificationHooks.kt::HideNoficationAccessIconHook` (hook, phases=before) vs A13 `SystemUINotificationHooks.kt::HideNoficationAccessIconHook` (hook, phases=before). Shared methods=['updateShortCutVisibility']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_drawer_removeshortcut` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.qs.MiuiQSHeaderView#updateShortCutVisibility,com.android.systemui.qs.MiuiNotificationHeaderView#updateShortCutVisibility; A13=com.android.systemui.qs.MiuiQSHeaderView#updateShortCutVisibility,com.android.systemui.qs.MiuiNotificationHeaderView#updateShortCutVisibility; shared_methods=['updateShortCutVisibility']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUINotificationHooks.kt::HideNoficationAccessIconHook` / `SystemUINotificationHooks.kt::HideNoficationAccessIconHook`: A13 already implements the exclusive keys system_drawer_removeshortcut. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemDisplayAndWindowHooks_kt_DoubleTapToSleepHook__DoubleTapToSleepHook

- PROOF_ID: `PROOF_OG_SystemDisplayAndWindowHooks_kt_DoubleTapToSleepHook__DoubleTapToSleepHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `DoubleTapToSleepHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.shade.NotificationsQuickSettingsContainer#onFinishInflate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `DoubleTapToSleepHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer#onFinishInflate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_dttosleep,system_scramblepin`
- VALUE_DOMAIN: owner-group keys for DoubleTapToSleepHook: system_dttosleep,system_scramblepin
- DEFAULT_SEMANTICS: `system_dttosleep` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::DoubleTapToSleepHook` / `SystemDisplayAndWindowHooks.kt::DoubleTapToSleepHook`: A13 already implements the exclusive keys system_dttosleep,system_scramblepin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::DoubleTapToSleepHook` (hook, phases=intercept) vs A13 `SystemDisplayAndWindowHooks.kt::DoubleTapToSleepHook` (hook, phases=after). Shared methods=['onFinishInflate']; A14-only members=['com.android.systemui.shade.NotificationsQuickSettingsContainer#onFinishInflate']; A13-only members=['com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer#onFinishInflate'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_dttosleep` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.shade.NotificationsQuickSettingsContainer#onFinishInflate; A13=com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer#onFinishInflate; shared_methods=['onFinishInflate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.shade.NotificationsQuickSettingsContainer#onFinishInflate']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::DoubleTapToSleepHook` / `SystemDisplayAndWindowHooks.kt::DoubleTapToSleepHook`: A13 already implements the exclusive keys system_dttosleep,system_scramblepin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_HideIconsHook__HideIconsHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_HideIconsHook__HideIconsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HideIconsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideIconsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility,com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_epm,system_statusbaricons_airplane,system_statusbaricons_ble_unlock,system_statusbaricons_btbattery,system_statusbaricons_dnd,system_statusbaricons_dualwifi,system_statusbaricons_gps,system_statusbaricons_headset,system_statusbaricons_hotspot,system_statusbaricons_nfc,system_statusbaricons_nosims,system_statusbaricons_profile,system_statusbaricons_secondspace,system_statusbaricons_sound,system_statusbaricons_vpn,system_statusbaricons_wifi`
- VALUE_DOMAIN: owner-group keys for HideIconsHook: system_epm,system_statusbaricons_airplane,system_statusbaricons_ble_unlock,system_statusbaricons_btbattery,system_statusbaricons_dnd,system_statusbaricons_dualwifi,system_statusbaricons_gps,system_statusbaricons_headset
- DEFAULT_SEMANTICS: `system_epm` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before` vs A13 phase `before`. Owner-group review of `SystemUIStatusBarHooks.kt::HideIconsHook` / `SystemUIStatusBarHooks.kt::HideIconsHook`: A13 already implements the exclusive keys system_epm,system_statusbaricons_airplane,system_statusbaricons_ble_unlock,system_statusbaricons_btbattery,system_statusbaricons_dnd,system_statusbaricons_dualwifi,system_statusbaricons_gps,system_statusbaricons_headset,system_statusbaricons_hotspot,system_statusbaricons_nfc,system_statusbaricons_nosims,system_statusbaricons_profile,system_statusbaricons_secondspace,system_statusbaricons_sound,system_statusbaricons_vpn,system_statusbaricons_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::HideIconsHook` (hook, phases=before) vs A13 `SystemUIStatusBarHooks.kt::HideIconsHook` (hook, phases=before). Shared methods=['setIconVisibility']; A14-only members=none; A13-only members=['com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_epm` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility; A13=com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setIconVisibility,com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl#setIconVisibility; shared_methods=['setIconVisibility']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::HideIconsHook` / `SystemUIStatusBarHooks.kt::HideIconsHook`: A13 already implements the exclusive keys system_epm,system_statusbaricons_airplane,system_statusbaricons_ble_unlock,system_statusbaricons_btbattery,system_statusbaricons_dnd,system_statusbaricons_dualwifi,system_statusbaricons_gps,system_statusbaricons_headset,system_statusbaricons_hotspot,system_statusbaricons_nfc,system_statusbaricons_nosims,system_statusbaricons_profile,system_statusbaricons_secondspace,system_statusbaricons_sound,system_statusbaricons_vpn,system_statusbaricons_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_ExpandHeadsUpHook__ExpandHeadsUpHook

- PROOF_ID: `PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_ExpandHeadsUpHook__ExpandHeadsUpHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `ExpandHeadsUpHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#setHeadsUp`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `ExpandHeadsUpHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#setHeadsUp`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_expandheadups_apps`
- VALUE_DOMAIN: owner-group keys for ExpandHeadsUpHook: system_expandheadups_apps
- DEFAULT_SEMANTICS: `system_expandheadups_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemNotificationHooks.kt::ExpandHeadsUpHook` / `SystemStatusBarClockAndMoreHooks.kt::ExpandHeadsUpHook`: A13 already implements the exclusive keys system_expandheadups_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemNotificationHooks.kt::ExpandHeadsUpHook` (hook, phases=intercept) vs A13 `SystemStatusBarClockAndMoreHooks.kt::ExpandHeadsUpHook` (hook, phases=after). Shared methods=['setHeadsUp']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_expandheadups_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#setHeadsUp; A13=com.android.systemui.statusbar.notification.row.ExpandableNotificationRow#setHeadsUp; shared_methods=['setHeadsUp']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemNotificationHooks.kt::ExpandHeadsUpHook` / `SystemStatusBarClockAndMoreHooks.kt::ExpandHeadsUpHook`: A13 already implements the exclusive keys system_expandheadups_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_ExpandNotificationsHook__processLegacy

- PROOF_ID: `PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_ExpandNotificationsHook__processLegacy`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/notificationautoexpand/NotificationAutoExpandEffect.kt`
- A14_SYMBOL: `processLegacy`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/notificationautoexpand/NotificationAutoExpandEffect.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `ExpandNotificationsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_expandnotifs_apps`
- VALUE_DOMAIN: owner-group keys for ExpandNotificationsHook: system_expandnotifs_apps
- DEFAULT_SEMANTICS: `system_expandnotifs_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `NotificationAutoExpandEffect.kt::processLegacy` / `SystemStatusBarClockAndMoreHooks.kt::ExpandNotificationsHook`: A13 already implements the exclusive keys system_expandnotifs_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `NotificationAutoExpandEffect.kt::processLegacy` (hook, phases=intercept) vs A13 `SystemStatusBarClockAndMoreHooks.kt::ExpandNotificationsHook` (hook, phases=before). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_expandnotifs_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `NotificationAutoExpandEffect.kt::processLegacy` / `SystemStatusBarClockAndMoreHooks.kt::ExpandNotificationsHook`: A13 already implements the exclusive keys system_expandnotifs_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_FirstVolumePressHook__FirstVolumePressHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_FirstVolumePressHook__FirstVolumePressHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `FirstVolumePressHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.audio.AudioService\$VolumeController#suppressAdjustment`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `FirstVolumePressHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.audio.AudioService\$VolumeController#suppressAdjustment`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_firstpress`
- VALUE_DOMAIN: owner-group keys for FirstVolumePressHook: system_firstpress
- DEFAULT_SEMANTICS: `system_firstpress` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,false]; chain.proceed[chain.proceed()]; A13 setResult[false]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemAudioHooks.kt::FirstVolumePressHook` / `SystemAudioAndVisualAndMoreHooks.kt::FirstVolumePressHook`: A13 already implements the exclusive keys system_firstpress. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemAudioHooks.kt::FirstVolumePressHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::FirstVolumePressHook` (hook, phases=after). Shared methods=['suppressAdjustment']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_firstpress` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.audio.AudioService\$VolumeController#suppressAdjustment; A13=com.android.server.audio.AudioService\$VolumeController#suppressAdjustment; shared_methods=['suppressAdjustment']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,false]; chain.proceed[chain.proceed()]; A13 setResult[false]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemAudioHooks.kt::FirstVolumePressHook` / `SystemAudioAndVisualAndMoreHooks.kt::FirstVolumePressHook`: A13 already implements the exclusive keys system_firstpress. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSecurityAndSystemHooks_kt_ForceCloseHook__ForceCloseHook

- PROOF_ID: `PROOF_OG_SystemSecurityAndSystemHooks_kt_ForceCloseHook__ForceCloseHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ForceCloseHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `com.android.server.policy.BaseMiuiPhoneWindowManager#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `ForceCloseHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_HOOK_TARGETS: `com.android.server.policy.BaseMiuiPhoneWindowManager#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_forceclose_apps`
- VALUE_DOMAIN: owner-group keys for ForceCloseHook: system_forceclose_apps
- DEFAULT_SEMANTICS: `system_forceclose_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `System.kt::ForceCloseHook` / `SystemSecurityAndSystemHooks.kt::ForceCloseHook`: A13 already implements the exclusive keys system_forceclose_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::ForceCloseHook` (hook, phases=intercept) vs A13 `SystemSecurityAndSystemHooks.kt::ForceCloseHook` (hook, phases=after). Shared methods=['<init>']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_forceclose_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.policy.BaseMiuiPhoneWindowManager#<init>; A13=com.android.server.policy.BaseMiuiPhoneWindowManager#<init>; shared_methods=['<init>']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::ForceCloseHook` / `SystemSecurityAndSystemHooks.kt::ForceCloseHook`: A13 already implements the exclusive keys system_forceclose_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_OpenAppInFreeFormHook__OpenAppInFreeFormHook

- PROOF_ID: `PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_OpenAppInFreeFormHook__OpenAppInFreeFormHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `OpenAppInFreeFormHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.wm.ActivityTaskManagerService#onSystemReady,com.miui.server.SecurityManagerService\$LocalService#checkGameBoosterPayPassAsUser,com.android.server.wm.ActivityStarterImpl#checkStartActivityByFreeForm`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `OpenAppInFreeFormHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.ActivityTaskManagerService#onSystemReady,com.android.server.wm.ActivityStarter#executeRequest`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_fw_forcein_actionsend,system_fw_forcein_actionsend_in_whitelist,system_screenshot_overlay`
- VALUE_DOMAIN: owner-group keys for OpenAppInFreeFormHook: system_fw_forcein_actionsend,system_fw_forcein_actionsend_in_whitelist,system_screenshot_overlay
- DEFAULT_SEMANTICS: `system_fw_forcein_actionsend` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemWindowHooks.kt::OpenAppInFreeFormHook` / `SystemFreeformAndMultiWindowHooks.kt::OpenAppInFreeFormHook`: A13 already implements the exclusive keys system_fw_forcein_actionsend,system_fw_forcein_actionsend_in_whitelist,system_screenshot_overlay. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::OpenAppInFreeFormHook` (hook, phases=intercept) vs A13 `SystemFreeformAndMultiWindowHooks.kt::OpenAppInFreeFormHook` (hook, phases=after,before). Shared methods=['onSystemReady']; A14-only members=['com.android.server.wm.ActivityStarterImpl#checkStartActivityByFreeForm', 'com.miui.server.SecurityManagerService\\$LocalService#checkGameBoosterPayPassAsUser']; A13-only members=['com.android.server.wm.ActivityStarter#executeRequest'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_fw_forcein_actionsend` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.wm.ActivityTaskManagerService#onSystemReady,com.miui.server.SecurityManagerService\$LocalService#checkGameBoosterPayPassAsUser,com.android.server.wm.ActivityStarterImpl#checkStartActivityByFreeForm; A13=com.android.server.wm.ActivityTaskManagerService#onSystemReady,com.android.server.wm.ActivityStarter#executeRequest; shared_methods=['onSystemReady']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.server.wm.ActivityStarterImpl#checkStartActivityByFreeForm', 'com.miui.server.SecurityManagerService\\$LocalService#checkGameBoosterPayPassAsUser']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::OpenAppInFreeFormHook` / `SystemFreeformAndMultiWindowHooks.kt::OpenAppInFreeFormHook`: A13 already implements the exclusive keys system_fw_forcein_actionsend,system_fw_forcein_actionsend_in_whitelist,system_screenshot_overlay. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_shouldOpenInFreeForm__shouldOpenInFreeForm

- PROOF_ID: `PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_shouldOpenInFreeForm__shouldOpenInFreeForm`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `shouldOpenInFreeForm`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `shouldOpenInFreeForm`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_fw_forcein_actionsend_apps`
- VALUE_DOMAIN: owner-group keys for shouldOpenInFreeForm: system_fw_forcein_actionsend_apps
- DEFAULT_SEMANTICS: `system_fw_forcein_actionsend_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemWindowHooks.kt::shouldOpenInFreeForm` / `SystemFreeformAndMultiWindowHooks.kt::shouldOpenInFreeForm`: A13 already implements the exclusive keys system_fw_forcein_actionsend_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_freeform_when_longclick'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::shouldOpenInFreeForm` (hook, phases=unknown) vs A13 `SystemFreeformAndMultiWindowHooks.kt::shouldOpenInFreeForm` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_fw_forcein_actionsend_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_freeform_when_longclick']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::shouldOpenInFreeForm` / `SystemFreeformAndMultiWindowHooks.kt::shouldOpenInFreeForm`: A13 already implements the exclusive keys system_fw_forcein_actionsend_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_freeform_when_longclick'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_HideSafeVolumeDlgHook__HideSafeVolumeDlgHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_HideSafeVolumeDlgHook__HideSafeVolumeDlgHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `HideSafeVolumeDlgHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.volume.VolumeUI#start`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `HideSafeVolumeDlgHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.volume.VolumeDialogControllerImpl#onShowSafetyWarningW`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_fw_noblacklist,system_notify_openinfw`
- VALUE_DOMAIN: owner-group keys for HideSafeVolumeDlgHook: system_fw_noblacklist,system_notify_openinfw
- DEFAULT_SEMANTICS: `system_fw_noblacklist` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `after` vs A13 phase `before`. Owner-group review of `SystemUIControlCenterHooks.kt::HideSafeVolumeDlgHook` / `SystemUIControlCenterHooks.kt::HideSafeVolumeDlgHook`: A13 already implements the exclusive keys system_fw_noblacklist,system_notify_openinfw. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::HideSafeVolumeDlgHook` (hook, phases=after) vs A13 `SystemUIControlCenterHooks.kt::HideSafeVolumeDlgHook` (hook, phases=before). Shared methods=none; A14-only members=['com.android.systemui.volume.VolumeUI#start']; A13-only members=['com.android.systemui.volume.VolumeDialogControllerImpl#onShowSafetyWarningW'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_fw_noblacklist` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.volume.VolumeUI#start; A13=com.android.systemui.volume.VolumeDialogControllerImpl#onShowSafetyWarningW; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.volume.VolumeUI#start']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::HideSafeVolumeDlgHook` / `SystemUIControlCenterHooks.kt::HideSafeVolumeDlgHook`: A13 already implements the exclusive keys system_fw_noblacklist,system_notify_openinfw. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_NoFloatingWindowBlacklistHook__NoFloatingWindowBlacklistHook

- PROOF_ID: `PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_NoFloatingWindowBlacklistHook__NoFloatingWindowBlacklistHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `NoFloatingWindowBlacklistHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.wm.MiuiFreeformUtilImpl#supportsFreeform`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `NoFloatingWindowBlacklistHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.MiuiFreeformServicesUtils#supportsFreeform`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_fw_splitscreen`
- VALUE_DOMAIN: owner-group keys for NoFloatingWindowBlacklistHook: system_fw_splitscreen
- DEFAULT_SEMANTICS: `system_fw_splitscreen` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[true]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `before`. Owner-group review of `SystemWindowHooks.kt::NoFloatingWindowBlacklistHook` / `SystemFreeformAndMultiWindowHooks.kt::NoFloatingWindowBlacklistHook`: A13 already implements the exclusive keys system_fw_splitscreen. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::NoFloatingWindowBlacklistHook` (hook, phases=unknown) vs A13 `SystemFreeformAndMultiWindowHooks.kt::NoFloatingWindowBlacklistHook` (hook, phases=before). Shared methods=['supportsFreeform']; A14-only members=['com.android.server.wm.MiuiFreeformUtilImpl#supportsFreeform']; A13-only members=['com.android.server.wm.MiuiFreeformServicesUtils#supportsFreeform'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_fw_splitscreen` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.wm.MiuiFreeformUtilImpl#supportsFreeform; A13=com.android.server.wm.MiuiFreeformServicesUtils#supportsFreeform; shared_methods=['supportsFreeform']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[true]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.server.wm.MiuiFreeformUtilImpl#supportsFreeform']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::NoFloatingWindowBlacklistHook` / `SystemFreeformAndMultiWindowHooks.kt::NoFloatingWindowBlacklistHook`: A13 already implements the exclusive keys system_fw_splitscreen. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_GalleryScreenshotPathHook__GalleryScreenshotPathHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_GalleryScreenshotPathHook__GalleryScreenshotPathHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `GalleryScreenshotPathHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `GalleryScreenshotPathHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/MediaInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_gallery_screenshots_path,system_screenshot`
- VALUE_DOMAIN: owner-group keys for GalleryScreenshotPathHook: system_gallery_screenshots_path,system_screenshot
- DEFAULT_SEMANTICS: `system_gallery_screenshots_path` A14 default=1; A13 default=1
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System.kt::GalleryScreenshotPathHook` / `SystemAudioAndVisualAndMoreHooks.kt::GalleryScreenshotPathHook`: A13 already implements the exclusive keys system_gallery_screenshots_path,system_screenshot. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::GalleryScreenshotPathHook` (hook, phases=unknown) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::GalleryScreenshotPathHook` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_gallery_screenshots_path` A14=1 A13=1. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::GalleryScreenshotPathHook` / `SystemAudioAndVisualAndMoreHooks.kt::GalleryScreenshotPathHook`: A13 already implements the exclusive keys system_gallery_screenshots_path,system_screenshot. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherSystemHooks_kt_HideFromRecentsHook__HideFromRecentsHook

- PROOF_ID: `PROOF_OG_LauncherSystemHooks_kt_HideFromRecentsHook__HideFromRecentsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_SYMBOL: `HideFromRecentsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Launcher.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_SYMBOL: `HideFromRecentsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt`
- A13_HOOK_TARGETS: `#needRemoveTask`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_hidefromrecents_apps`
- VALUE_DOMAIN: owner-group keys for HideFromRecentsHook: system_hidefromrecents_apps
- DEFAULT_SEMANTICS: `system_hidefromrecents_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 setResult[true]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Launcher.kt::HideFromRecentsHook` / `LauncherSystemHooks.kt::HideFromRecentsHook`: A13 already implements the exclusive keys system_hidefromrecents_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Launcher.kt::HideFromRecentsHook` (hook, phases=intercept) vs A13 `LauncherSystemHooks.kt::HideFromRecentsHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=['#needRemoveTask'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_hidefromrecents_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=#needRemoveTask; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 setResult[true]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Launcher.kt::HideFromRecentsHook` / `LauncherSystemHooks.kt::HideFromRecentsHook`: A13 already implements the exclusive keys system_hidefromrecents_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_PrivacyAppsLayoutHook__PrivacyAppsLayoutHook

- PROOF_ID: `PROOF_OG_Various_kt_PrivacyAppsLayoutHook__PrivacyAppsLayoutHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `PrivacyAppsLayoutHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.miui.privacyapps.ui.PrivacyAppsActivity#onCreate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `PrivacyAppsLayoutHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.privacyapps.ui.PrivacyAppsActivity#onCreate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_hidelowbatwarn,various_disable_dock_suggest,various_enable_expand_sidebar,various_privacyapps_column_nums4`
- VALUE_DOMAIN: owner-group keys for PrivacyAppsLayoutHook: system_hidelowbatwarn,various_disable_dock_suggest,various_enable_expand_sidebar,various_privacyapps_column_nums4
- DEFAULT_SEMANTICS: `system_hidelowbatwarn` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Various.kt::PrivacyAppsLayoutHook` / `Various.kt::PrivacyAppsLayoutHook`: A13 already implements the exclusive keys system_hidelowbatwarn,various_disable_dock_suggest,various_enable_expand_sidebar,various_privacyapps_column_nums4. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::PrivacyAppsLayoutHook` (hook, phases=intercept) vs A13 `Various.kt::PrivacyAppsLayoutHook` (hook, phases=after). Shared methods=['onCreate']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_hidelowbatwarn` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.privacyapps.ui.PrivacyAppsActivity#onCreate; A13=com.miui.privacyapps.ui.PrivacyAppsActivity#onCreate; shared_methods=['onCreate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::PrivacyAppsLayoutHook` / `Various.kt::PrivacyAppsLayoutHook`: A13 already implements the exclusive keys system_hidelowbatwarn,various_disable_dock_suggest,various_enable_expand_sidebar,various_privacyapps_column_nums4. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_ForceClockUseSystemFontsHook__ForceClockUseSystemFontsHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_ForceClockUseSystemFontsHook__ForceClockUseSystemFontsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `ForceClockUseSystemFontsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.miui.clock.MiuiBaseClock#updateViewsTextSize,com.miui.clock.MiuiLeftTopLargeClock#onLanguageChanged`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `ForceClockUseSystemFontsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.miui.clock.MiuiBaseClock#updateViewsTextSize,com.miui.clock.MiuiLeftTopLargeClock#onLanguageChanged`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_hidelsclock,system_hidelsstatusbar,system_showpct,system_statusbar_horizmargin`
- VALUE_DOMAIN: owner-group keys for ForceClockUseSystemFontsHook: system_hidelsclock,system_hidelsstatusbar,system_showpct,system_statusbar_horizmargin
- DEFAULT_SEMANTICS: `system_hidelsclock` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after` vs A13 phase `after`. Owner-group review of `SystemUIStatusBarHooks.kt::ForceClockUseSystemFontsHook` / `SystemUIStatusBarHooks.kt::ForceClockUseSystemFontsHook`: A13 already implements the exclusive keys system_hidelsclock,system_hidelsstatusbar,system_showpct,system_statusbar_horizmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::ForceClockUseSystemFontsHook` (hook, phases=after) vs A13 `SystemUIStatusBarHooks.kt::ForceClockUseSystemFontsHook` (hook, phases=after). Shared methods=['onLanguageChanged', 'updateViewsTextSize']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_hidelsclock` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.clock.MiuiBaseClock#updateViewsTextSize,com.miui.clock.MiuiLeftTopLargeClock#onLanguageChanged; A13=com.miui.clock.MiuiBaseClock#updateViewsTextSize,com.miui.clock.MiuiLeftTopLargeClock#onLanguageChanged; shared_methods=['onLanguageChanged', 'updateViewsTextSize']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::ForceClockUseSystemFontsHook` / `SystemUIStatusBarHooks.kt::ForceClockUseSystemFontsHook`: A13 already implements the exclusive keys system_hidelsclock,system_hidelsstatusbar,system_showpct,system_statusbar_horizmargin. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_AllowAllFloatHook__AllowAllFloatHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_AllowAllFloatHook__AllowAllFloatHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `AllowAllFloatHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.ExpandedNotification#isEnableFloat,com.android.systemui.statusbar.notification.NotificationSettingsManager#canFloat`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `AllowAllFloatHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.MiuiNotificationCompat#isEnableFloat`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_hidelshint`
- VALUE_DOMAIN: owner-group keys for AllowAllFloatHook: system_hidelshint
- DEFAULT_SEMANTICS: `system_hidelshint` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `before`. Owner-group review of `SystemWindowHooks.kt::AllowAllFloatHook` / `SystemAudioAndVisualAndMoreHooks.kt::AllowAllFloatHook`: A13 already implements the exclusive keys system_hidelshint. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::AllowAllFloatHook` (hook, phases=unknown) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::AllowAllFloatHook` (hook, phases=before). Shared methods=['isEnableFloat']; A14-only members=['com.android.systemui.statusbar.notification.ExpandedNotification#isEnableFloat', 'com.android.systemui.statusbar.notification.NotificationSettingsManager#canFloat']; A13-only members=['com.android.systemui.statusbar.notification.MiuiNotificationCompat#isEnableFloat'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_hidelshint` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.ExpandedNotification#isEnableFloat,com.android.systemui.statusbar.notification.NotificationSettingsManager#canFloat; A13=com.android.systemui.statusbar.notification.MiuiNotificationCompat#isEnableFloat; shared_methods=['isEnableFloat']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.notification.ExpandedNotification#isEnableFloat', 'com.android.systemui.statusbar.notification.NotificationSettingsManager#canFloat']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::AllowAllFloatHook` / `SystemAudioAndVisualAndMoreHooks.kt::AllowAllFloatHook`: A13 already implements the exclusive keys system_hidelshint. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemDisplayAndWindowHooks_kt_HideProximityWarningHook__HideProximityWarningHook

- PROOF_ID: `PROOF_OG_SystemDisplayAndWindowHooks_kt_HideProximityWarningHook__HideProximityWarningHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `HideProximityWarningHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `com.android.server.policy.MiuiScreenOnProximityLock#showHint,com.android.server.policy.MiuiScreenOnProximityLock#prepareHintWindow`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `HideProximityWarningHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.MiuiScreenOnProximityLock#showHint,com.android.server.policy.MiuiScreenOnProximityLock#prepareHintWindow`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_hideproxywarn`
- VALUE_DOMAIN: owner-group keys for HideProximityWarningHook: system_hideproxywarn
- DEFAULT_SEMANTICS: `system_hideproxywarn` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System.kt::HideProximityWarningHook` / `SystemDisplayAndWindowHooks.kt::HideProximityWarningHook`: A13 already implements the exclusive keys system_hideproxywarn. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::HideProximityWarningHook` (hook, phases=unknown) vs A13 `SystemDisplayAndWindowHooks.kt::HideProximityWarningHook` (hook, phases=unknown). Shared methods=['prepareHintWindow', 'showHint']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_hideproxywarn` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.policy.MiuiScreenOnProximityLock#showHint,com.android.server.policy.MiuiScreenOnProximityLock#prepareHintWindow; A13=com.android.server.policy.MiuiScreenOnProximityLock#showHint,com.android.server.policy.MiuiScreenOnProximityLock#prepareHintWindow; shared_methods=['prepareHintWindow', 'showHint']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::HideProximityWarningHook` / `SystemDisplayAndWindowHooks.kt::HideProximityWarningHook`: A13 already implements the exclusive keys system_hideproxywarn. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_BatteryIndicator_kt_registerCallbacks__init

- PROOF_ID: `PROOF_OG_BatteryIndicator_kt_registerCallbacks__init`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_SYMBOL: `init`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A13_SYMBOL: `registerCallbacks`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_hidestatusbar_whenscreenshot`
- VALUE_DOMAIN: owner-group keys for registerCallbacks: system_hidestatusbar_whenscreenshot
- DEFAULT_SEMANTICS: `system_hidestatusbar_whenscreenshot` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `BatteryIndicator.kt::init` / `BatteryIndicator.kt::registerCallbacks`: A13 already implements the exclusive keys system_hidestatusbar_whenscreenshot. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `BatteryIndicator.kt::init` (hook, phases=unknown) vs A13 `BatteryIndicator.kt::registerCallbacks` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_hidestatusbar_whenscreenshot` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `BatteryIndicator.kt::init` / `BatteryIndicator.kt::registerCallbacks`: A13 already implements the exclusive keys system_hidestatusbar_whenscreenshot. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_Disable72hStrongAuthHook__Disable72hStrongAuthHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_Disable72hStrongAuthHook__Disable72hStrongAuthHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `Disable72hStrongAuthHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.locksettings.LockSettingsStrongAuth#rescheduleStrongAuthTimeoutAlarm`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `Disable72hStrongAuthHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.locksettings.LockSettingsStrongAuth#rescheduleStrongAuthTimeoutAlarm`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_lockscreen_disable_strongauth_72h`
- VALUE_DOMAIN: owner-group keys for Disable72hStrongAuthHook: system_lockscreen_disable_strongauth_72h
- DEFAULT_SEMANTICS: `system_lockscreen_disable_strongauth_72h` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemLockScreenHooks.kt::Disable72hStrongAuthHook` / `SystemNotificationMoreHooks.kt::Disable72hStrongAuthHook`: A13 already implements the exclusive keys system_lockscreen_disable_strongauth_72h. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::Disable72hStrongAuthHook` (hook, phases=unknown) vs A13 `SystemNotificationMoreHooks.kt::Disable72hStrongAuthHook` (hook, phases=unknown). Shared methods=['rescheduleStrongAuthTimeoutAlarm']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_lockscreen_disable_strongauth_72h` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.locksettings.LockSettingsStrongAuth#rescheduleStrongAuthTimeoutAlarm; A13=com.android.server.locksettings.LockSettingsStrongAuth#rescheduleStrongAuthTimeoutAlarm; shared_methods=['rescheduleStrongAuthTimeoutAlarm']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::Disable72hStrongAuthHook` / `SystemNotificationMoreHooks.kt::Disable72hStrongAuthHook`: A13 already implements the exclusive keys system_lockscreen_disable_strongauth_72h. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUILockScreenHooks_kt_HideLockscreenZenModeHook__HideLockscreenZenModeHook

- PROOF_ID: `PROOF_OG_SystemUILockScreenHooks_kt_HideLockscreenZenModeHook__HideLockscreenZenModeHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `HideLockscreenZenModeHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.zen.ZenModeViewController#updateVisibility`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `HideLockscreenZenModeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.notification.zen.ZenModeViewController#shouldBeVisible`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_lockscreen_hidezenmode,system_nosafevolume`
- VALUE_DOMAIN: owner-group keys for HideLockscreenZenModeHook: system_lockscreen_hidezenmode,system_nosafevolume
- DEFAULT_SEMANTICS: `system_lockscreen_hidezenmode` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before,after` vs A13 phase `unknown`. Owner-group review of `SystemUILockScreenHooks.kt::HideLockscreenZenModeHook` / `SystemUILockScreenHooks.kt::HideLockscreenZenModeHook`: A13 already implements the exclusive keys system_lockscreen_hidezenmode,system_nosafevolume. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUILockScreenHooks.kt::HideLockscreenZenModeHook` (hook, phases=before,after) vs A13 `SystemUILockScreenHooks.kt::HideLockscreenZenModeHook` (hook, phases=unknown). Shared methods=none; A14-only members=['com.android.systemui.statusbar.notification.zen.ZenModeViewController#updateVisibility']; A13-only members=['com.android.systemui.statusbar.notification.zen.ZenModeViewController#shouldBeVisible'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_lockscreen_hidezenmode` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.zen.ZenModeViewController#updateVisibility; A13=com.android.systemui.statusbar.notification.zen.ZenModeViewController#shouldBeVisible; shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before,after; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.notification.zen.ZenModeViewController#updateVisibility']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUILockScreenHooks.kt::HideLockscreenZenModeHook` / `SystemUILockScreenHooks.kt::HideLockscreenZenModeHook`: A13 already implements the exclusive keys system_lockscreen_hidezenmode,system_nosafevolume. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUILockScreenHooks_kt_LockScreenShortcutHook__LockScreenShortcutHook

- PROOF_ID: `PROOF_OG_SystemUILockScreenHooks_kt_LockScreenShortcutHook__LockScreenShortcutHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `LockScreenShortcutHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.keyguard.injector.KeyguardBottomAreaInjector#updateLeftIcon,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightIcon,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightAffordanceViewLayoutVisibility,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateIcons,com.android.keyguard.KeyguardMoveHelper#setTranslation,com.android.keyguard.KeyguardMoveHelper#endMotion,com.android.keyguard.KeyguardMoveRightController#onTouchDown,com.android.keyguard.KeyguardMoveRightController#onTouchMove,com.android.keyguard.injector.KeyguardBottomAreaInjector#<init>`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `LockScreenShortcutHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultLeftButton#getIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultRightButton#getIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#initTipsView,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onFinishInflate,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#updateLeftAffordanceIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onClick,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#launchCamera,com.android.keyguard.MiuiKeyguardCameraView#setDarkStyle,com.android.keyguard.MiuiKeyguardCameraView#updatePreView,com.android.keyguard.MiuiKeyguardCameraView#setPreviewImageDrawable,com.android.keyguard.MiuiKeyguardCameraView#handleMoveDistanceChanged,com.android.keyguard.MiuiKeyguardCameraView#startFullScreenAnim,com.android.keyguard.KeyguardMoveHelper#setTranslation,com.android.keyguard.KeyguardMoveHelper#fling`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_lockscreenshortcuts_left_off,system_lockscreenshortcuts_left_tapaction,system_lockscreenshortcuts_right,system_lockscreenshortcuts_right_off`
- VALUE_DOMAIN: owner-group keys for LockScreenShortcutHook: system_lockscreenshortcuts_left_off,system_lockscreenshortcuts_left_tapaction,system_lockscreenshortcuts_right,system_lockscreenshortcuts_right_off
- DEFAULT_SEMANTICS: `system_lockscreenshortcuts_left_off` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[null,null,null,null]; setResult[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after,before` vs A13 phase `after,before`. Owner-group review of `SystemUILockScreenHooks.kt::LockScreenShortcutHook` / `SystemUILockScreenHooks.kt::LockScreenShortcutHook`: A13 already implements the exclusive keys system_lockscreenshortcuts_left_off,system_lockscreenshortcuts_left_tapaction,system_lockscreenshortcuts_right,system_lockscreenshortcuts_right_off. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUILockScreenHooks.kt::LockScreenShortcutHook` (hook, phases=after,before) vs A13 `SystemUILockScreenHooks.kt::LockScreenShortcutHook` (hook, phases=after,before). Shared methods=['setTranslation']; A14-only members=['com.android.keyguard.KeyguardMoveHelper#endMotion', 'com.android.keyguard.KeyguardMoveRightController#onTouchDown', 'com.android.keyguard.KeyguardMoveRightController#onTouchMove', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#<init>', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateIcons', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateLeftIcon', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightAffordanceViewLayoutVisibility', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightIcon']; A13-only members=['com.android.keyguard.KeyguardMoveHelper#fling', 'com.android.keyguard.MiuiKeyguardCameraView#handleMoveDistanceChanged', 'com.android.keyguard.MiuiKeyguardCameraView#setDarkStyle', 'com.android.keyguard.MiuiKeyguardCameraView#setPreviewImageDrawable', 'com.android.keyguard.MiuiKeyguardCameraView#startFullScreenAnim', 'com.android.keyguard.MiuiKeyguardCameraView#updatePreView', 'com.android.systemui.statusbar.phone.KeyguardBottomAreaView#initTipsView', 'com.android.systemui.statusbar.phone.KeyguardBottomAreaView#launchCamera', 'com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onClick', 'com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onFinishInflate', 'com.android.systemui.statusbar.phone.KeyguardBottomAreaView#updateLeftAffordanceIcon', 'com.android.systemui.statusbar.phone.KeyguardBottomAreaView\\$MiuiDefaultLeftButton#getIcon', 'com.android.systemui.statusbar.phone.KeyguardBottomAreaView\\$MiuiDefaultRightButton#getIcon'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_lockscreenshortcuts_left_off` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.keyguard.injector.KeyguardBottomAreaInjector#updateLeftIcon,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightIcon,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightAffordanceViewLayoutVisibility,com.android.keyguard.injector.KeyguardBottomAreaInjector#updateIcons,com.android.keyguard.KeyguardMoveHelper#setTranslation,com.android.keyguard.KeyguardMoveHelper#endMotion,com.android.keyguard.KeyguardMoveRightController#onTouchDown,com.android.keyguard.KeyguardMoveRightController#onTouchMove,com.android.keyguard.injector.KeyguardBottomAreaInjector#<init>; A13=com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultLeftButton#getIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultRightButton#getIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#initTipsView,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onFinishInflate,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#updateLeftAffordanceIcon,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#onClick,com.android.systemui.statusbar.phone.KeyguardBottomAreaView#launchCamera,com.android.keyguard.MiuiKeyguardCameraView#setDarkStyle,com.android.keyguard.MiuiKeyguardCameraView#updatePreView,com.android.keyguard.MiuiKeyguardCameraView#setPreviewImageDrawable,com.android.keyguard.MiuiKeyguardCameraView#handleMoveDistanceChanged,com.android.keyguard.MiuiKeyguardCameraView#startFullScreenAnim,com.android.keyguard.KeyguardMoveHelper#setTranslation,com.android.keyguard.KeyguardMoveHelper#fling; shared_methods=['setTranslation']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after,before; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[null,null,null,null]; setResult[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.keyguard.KeyguardMoveHelper#endMotion', 'com.android.keyguard.KeyguardMoveRightController#onTouchDown', 'com.android.keyguard.KeyguardMoveRightController#onTouchMove', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#<init>', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateIcons', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateLeftIcon', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightAffordanceViewLayoutVisibility', 'com.android.keyguard.injector.KeyguardBottomAreaInjector#updateRightIcon']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUILockScreenHooks.kt::LockScreenShortcutHook` / `SystemUILockScreenHooks.kt::LockScreenShortcutHook`: A13 already implements the exclusive keys system_lockscreenshortcuts_left_off,system_lockscreenshortcuts_left_tapaction,system_lockscreenshortcuts_right,system_lockscreenshortcuts_right_off. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_hookUpdateTime__hookUpdateTime

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_hookUpdateTime__hookUpdateTime`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `hookUpdateTime`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `hookUpdateTime`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_lsalarm_all`
- VALUE_DOMAIN: owner-group keys for hookUpdateTime: system_lsalarm_all
- DEFAULT_SEMANTICS: `system_lsalarm_all` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemLockScreenHooks.kt::hookUpdateTime` / `SystemAudioAndVisualAndMoreHooks.kt::hookUpdateTime`: A13 already implements the exclusive keys system_lsalarm_all. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::hookUpdateTime` (hook, phases=unknown) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::hookUpdateTime` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_lsalarm_all` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::hookUpdateTime` / `SystemAudioAndVisualAndMoreHooks.kt::hookUpdateTime`: A13 already implements the exclusive keys system_lsalarm_all. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_LockScreenAlarmHook__LockScreenAlarmHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_LockScreenAlarmHook__LockScreenAlarmHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `LockScreenAlarmHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.KeyguardIndicationController#setIndicationArea,com.android.systemui.statusbar.KeyguardIndicationController#updateDeviceEntryIndication,com.android.keyguard.injector.KeyguardBottomAreaInjector#handleBottomButtonClickedAnimation`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `LockScreenAlarmHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.keyguard.clock.MiuiKeyguardSingleClock#updateTime,com.android.keyguard.clock.MiuiKeyguardDualClock#updateTime`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_lsalarm_format`
- VALUE_DOMAIN: owner-group keys for LockScreenAlarmHook: system_lsalarm_format
- DEFAULT_SEMANTICS: `system_lsalarm_format` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::LockScreenAlarmHook` / `SystemAudioAndVisualAndMoreHooks.kt::LockScreenAlarmHook`: A13 already implements the exclusive keys system_lsalarm_format. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::LockScreenAlarmHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::LockScreenAlarmHook` (hook, phases=after). Shared methods=none; A14-only members=['com.android.keyguard.injector.KeyguardBottomAreaInjector#handleBottomButtonClickedAnimation', 'com.android.systemui.statusbar.KeyguardIndicationController#setIndicationArea', 'com.android.systemui.statusbar.KeyguardIndicationController#updateDeviceEntryIndication']; A13-only members=['com.android.keyguard.clock.MiuiKeyguardDualClock#updateTime', 'com.android.keyguard.clock.MiuiKeyguardSingleClock#updateTime'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_lsalarm_format` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.KeyguardIndicationController#setIndicationArea,com.android.systemui.statusbar.KeyguardIndicationController#updateDeviceEntryIndication,com.android.keyguard.injector.KeyguardBottomAreaInjector#handleBottomButtonClickedAnimation; A13=com.android.keyguard.clock.MiuiKeyguardSingleClock#updateTime,com.android.keyguard.clock.MiuiKeyguardDualClock#updateTime; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.keyguard.injector.KeyguardBottomAreaInjector#handleBottomButtonClickedAnimation', 'com.android.systemui.statusbar.KeyguardIndicationController#setIndicationArea', 'com.android.systemui.statusbar.KeyguardIndicationController#updateDeviceEntryIndication']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::LockScreenAlarmHook` / `SystemAudioAndVisualAndMoreHooks.kt::LockScreenAlarmHook`: A13 already implements the exclusive keys system_lsalarm_format. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_LockScreenTimeoutHook__setupSystemUiResources

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_LockScreenTimeoutHook__setupSystemUiResources`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/SystemUiResourceBootstrap.kt`
- A14_SYMBOL: `setupSystemUiResources`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/SystemUiResourceBootstrap.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `LockScreenTimeoutHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationShadeWindowControllerImpl#applyUserActivityTimeout`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_lstimeout`
- VALUE_DOMAIN: owner-group keys for LockScreenTimeoutHook: system_lstimeout
- DEFAULT_SEMANTICS: `system_lstimeout` A14 default=3; A13 default=3
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `after`. Owner-group review of `SystemUiResourceBootstrap.kt::setupSystemUiResources` / `SystemAudioAndVisualAndMoreHooks.kt::LockScreenTimeoutHook`: A13 already implements the exclusive keys system_lstimeout. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_enable_style_switch', 'system_cc_show_stepcount', 'system_drawer_date_fontsize', 'system_drawer_hidedate', 'system_statusbar_horizmargin', 'system_statusbar_iconsize', 'system_statusbar_topmargin', 'system_statusbar_topmargin_val', 'system_taptounlock', 'system_volumetimer'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUiResourceBootstrap.kt::setupSystemUiResources` (hook, phases=unknown) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::LockScreenTimeoutHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=['com.android.systemui.statusbar.phone.NotificationShadeWindowControllerImpl#applyUserActivityTimeout'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_lstimeout` A14=3 A13=3. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.systemui.statusbar.phone.NotificationShadeWindowControllerImpl#applyUserActivityTimeout; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_enable_style_switch', 'system_cc_show_stepcount', 'system_drawer_date_fontsize', 'system_drawer_hidedate', 'system_statusbar_horizmargin', 'system_statusbar_iconsize', 'system_statusbar_topmargin', 'system_statusbar_topmargin_val', 'system_taptounlock', 'system_volumetimer']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUiResourceBootstrap.kt::setupSystemUiResources` / `SystemAudioAndVisualAndMoreHooks.kt::LockScreenTimeoutHook`: A13 already implements the exclusive keys system_lstimeout. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_enable_style_switch', 'system_cc_show_stepcount', 'system_drawer_date_fontsize', 'system_drawer_hidedate', 'system_statusbar_horizmargin', 'system_statusbar_iconsize', 'system_statusbar_topmargin', 'system_statusbar_topmargin_val', 'system_taptounlock', 'system_volumetimer'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_MaxNotificationIconsHook__MaxNotificationIconsHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_MaxNotificationIconsHook__MaxNotificationIconsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `MaxNotificationIconsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationIconContainer#resetViewStates`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `MaxNotificationIconsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.NotificationIconContainer#miuiShowNotificationIcons`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_minimalnotifview,system_notifchannelsettings`
- VALUE_DOMAIN: owner-group keys for MaxNotificationIconsHook: system_minimalnotifview,system_notifchannelsettings
- DEFAULT_SEMANTICS: `system_minimalnotifview` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemNotificationHooks.kt::MaxNotificationIconsHook` / `SystemNotificationMoreHooks.kt::MaxNotificationIconsHook`: A13 already implements the exclusive keys system_minimalnotifview,system_notifchannelsettings. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemNotificationHooks.kt::MaxNotificationIconsHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::MaxNotificationIconsHook` (hook, phases=before). Shared methods=none; A14-only members=['com.android.systemui.statusbar.phone.NotificationIconContainer#resetViewStates']; A13-only members=['com.android.systemui.statusbar.phone.NotificationIconContainer#miuiShowNotificationIcons'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_minimalnotifview` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.NotificationIconContainer#resetViewStates; A13=com.android.systemui.statusbar.phone.NotificationIconContainer#miuiShowNotificationIcons; shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.phone.NotificationIconContainer#resetViewStates']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemNotificationHooks.kt::MaxNotificationIconsHook` / `SystemNotificationMoreHooks.kt::MaxNotificationIconsHook`: A13 already implements the exclusive keys system_minimalnotifview,system_notifchannelsettings. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUILockScreenHooks_kt_SecureQSTilesHook__SecureQSTilesHook

- PROOF_ID: `PROOF_OG_SystemUILockScreenHooks_kt_SecureQSTilesHook__SecureQSTilesHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `SecureQSTilesHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click,com.android.systemui.qs.tileimpl.QSTileImpl#longClick,com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `SecureQSTilesHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#createTileInternal,#handleClick`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_morenotif,system_secureqs_keepopened`
- VALUE_DOMAIN: owner-group keys for SecureQSTilesHook: system_morenotif,system_secureqs_keepopened
- DEFAULT_SEMANTICS: `system_morenotif` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `before` vs A13 phase `after,before`. Owner-group review of `SystemUIControlCenterHooks.kt::SecureQSTilesHook` / `SystemUILockScreenHooks.kt::SecureQSTilesHook`: A13 already implements the exclusive keys system_morenotif,system_secureqs_keepopened. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_secureqs_airplane', 'system_secureqs_bt', 'system_secureqs_custom', 'system_secureqs_hotspot', 'system_secureqs_location', 'system_secureqs_mobiledata', 'system_secureqs_nfc', 'system_secureqs_sync', 'system_secureqs_wifi'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::SecureQSTilesHook` (hook, phases=before) vs A13 `SystemUILockScreenHooks.kt::SecureQSTilesHook` (hook, phases=after,before). Shared methods=none; A14-only members=['com.android.systemui.qs.tileimpl.QSTileImpl#click', 'com.android.systemui.qs.tileimpl.QSTileImpl#longClick', 'com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick']; A13-only members=['#createTileInternal', '#handleClick'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_morenotif` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.qs.tileimpl.QSTileImpl#click,com.android.systemui.qs.tileimpl.QSTileImpl#longClick,com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick; A13=#createTileInternal,#handleClick; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_secureqs_airplane', 'system_secureqs_bt', 'system_secureqs_custom', 'system_secureqs_hotspot', 'system_secureqs_location', 'system_secureqs_mobiledata', 'system_secureqs_nfc', 'system_secureqs_sync', 'system_secureqs_wifi']; A14-only hook members=['com.android.systemui.qs.tileimpl.QSTileImpl#click', 'com.android.systemui.qs.tileimpl.QSTileImpl#longClick', 'com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::SecureQSTilesHook` / `SystemUILockScreenHooks.kt::SecureQSTilesHook`: A13 already implements the exclusive keys system_morenotif,system_secureqs_keepopened. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_secureqs_airplane', 'system_secureqs_bt', 'system_secureqs_custom', 'system_secureqs_hotspot', 'system_secureqs_location', 'system_secureqs_mobiledata', 'system_secureqs_nfc', 'system_secureqs_sync', 'system_secureqs_wifi'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarMoreHooks_kt_DisplayWifiStandardHook__DisplayWifiStandardHook

- PROOF_ID: `PROOF_OG_SystemStatusBarMoreHooks_kt_DisplayWifiStandardHook__DisplayWifiStandardHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_SYMBOL: `DisplayWifiStandardHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarIconHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarWifiView#applyWifiState`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt`
- A13_SYMBOL: `DisplayWifiStandardHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarWifiView#applyWifiState`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_mutevisiblenotif,system_statusbaricons_battery1,system_statusbaricons_battery2,system_statusbaricons_battery3,system_statusbaricons_battery4,system_statusbaricons_wifistandard`
- VALUE_DOMAIN: owner-group keys for DisplayWifiStandardHook: system_mutevisiblenotif,system_statusbaricons_battery1,system_statusbaricons_battery2,system_statusbaricons_battery3,system_statusbaricons_battery4,system_statusbaricons_wifistandard
- DEFAULT_SEMANTICS: `system_mutevisiblenotif` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemStatusBarIconHooks.kt::DisplayWifiStandardHook` / `SystemStatusBarMoreHooks.kt::DisplayWifiStandardHook`: A13 already implements the exclusive keys system_mutevisiblenotif,system_statusbaricons_battery1,system_statusbaricons_battery2,system_statusbaricons_battery3,system_statusbaricons_battery4,system_statusbaricons_wifistandard. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemStatusBarIconHooks.kt::DisplayWifiStandardHook` (hook, phases=intercept) vs A13 `SystemStatusBarMoreHooks.kt::DisplayWifiStandardHook` (hook, phases=before). Shared methods=['applyWifiState']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_mutevisiblenotif` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.StatusBarWifiView#applyWifiState; A13=com.android.systemui.statusbar.StatusBarWifiView#applyWifiState; shared_methods=['applyWifiState']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemStatusBarIconHooks.kt::DisplayWifiStandardHook` / `SystemStatusBarMoreHooks.kt::DisplayWifiStandardHook`: A13 already implements the exclusive keys system_mutevisiblenotif,system_statusbaricons_battery1,system_statusbaricons_battery2,system_statusbaricons_battery3,system_statusbaricons_battery4,system_statusbaricons_wifistandard. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_SystemUIStatusBarHooks_kt_initNetSpeedStyle__buildNetSpeedTextStyleSnapshot

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_initNetSpeedStyle__buildNetSpeedTextStyleSnapshot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildNetSpeedTextStyleSnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `initNetSpeedStyle`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_netspeed_rowspacing`
- VALUE_DOMAIN: owner-group keys for initNetSpeedStyle: system_netspeed_rowspacing
- DEFAULT_SEMANTICS: `system_netspeed_rowspacing` A14 default=100; A13 default=100
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemUIStatusBarHooks.kt::buildNetSpeedTextStyleSnapshot` / `SystemUIStatusBarHooks.kt::initNetSpeedStyle`: A13 already implements the exclusive keys system_netspeed_rowspacing. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_detailednetspeed_style', 'system_netspeed_boldfont', 'system_netspeed_fixedcontent_width'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::buildNetSpeedTextStyleSnapshot` (hook, phases=unknown) vs A13 `SystemUIStatusBarHooks.kt::initNetSpeedStyle` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_netspeed_rowspacing` A14=100 A13=100. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_detailednetspeed_style', 'system_netspeed_boldfont', 'system_netspeed_fixedcontent_width']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::buildNetSpeedTextStyleSnapshot` / `SystemUIStatusBarHooks.kt::initNetSpeedStyle`: A13 already implements the exclusive keys system_netspeed_rowspacing. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_detailednetspeed_style', 'system_netspeed_boldfont', 'system_netspeed_fixedcontent_width'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_SystemUIStatusBarHooks_kt_DetailedNetSpeedHook__DetailedNetSpeedHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_DetailedNetSpeedHook__DetailedNetSpeedHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `DetailedNetSpeedHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `#handleMessage,#updateText`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DetailedNetSpeedHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#updateNetworkSpeed,#updateText`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_netspeedinterval`
- VALUE_DOMAIN: owner-group keys for DetailedNetSpeedHook: system_netspeedinterval
- DEFAULT_SEMANTICS: `system_netspeedinterval` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before` vs A13 phase `before`. Owner-group review of `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook` / `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook`: A13 already implements the exclusive keys system_netspeedinterval. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook` (hook, phases=before) vs A13 `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook` (hook, phases=before). Shared methods=['updateText']; A14-only members=['#handleMessage']; A13-only members=['#updateNetworkSpeed'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_netspeedinterval` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=#handleMessage,#updateText; A13=#updateNetworkSpeed,#updateText; shared_methods=['updateText']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#handleMessage']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook` / `SystemUIStatusBarHooks.kt::DetailedNetSpeedHook`: A13 already implements the exclusive keys system_netspeedinterval. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_NoDuckingHook__NoDuckingHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_NoDuckingHook__NoDuckingHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `NoDuckingHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.audio.FocusRequester#handleFocusLoss`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `NoDuckingHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.audio.FocusRequester#handleFocusLoss`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_noducking,system_orientationlock`
- VALUE_DOMAIN: owner-group keys for NoDuckingHook: system_noducking,system_orientationlock
- DEFAULT_SEMANTICS: `system_noducking` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemAudioHooks.kt::NoDuckingHook` / `SystemNotificationMoreHooks.kt::NoDuckingHook`: A13 already implements the exclusive keys system_noducking,system_orientationlock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemAudioHooks.kt::NoDuckingHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::NoDuckingHook` (hook, phases=before). Shared methods=['handleFocusLoss']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_noducking` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.audio.FocusRequester#handleFocusLoss; A13=com.android.server.audio.FocusRequester#handleFocusLoss; shared_methods=['handleFocusLoss']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemAudioHooks.kt::NoDuckingHook` / `SystemNotificationMoreHooks.kt::NoDuckingHook`: A13 already implements the exclusive keys system_noducking,system_orientationlock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarAndClockHooks_kt_StatusBarBackgroundCompatHook__StatusBarBackgroundCompatHook

- PROOF_ID: `PROOF_OG_SystemStatusBarAndClockHooks_kt_StatusBarBackgroundCompatHook__StatusBarBackgroundCompatHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarBackgroundHooks.kt`
- A14_SYMBOL: `StatusBarBackgroundCompatHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarBackgroundHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt`
- A13_SYMBOL: `StatusBarBackgroundCompatHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_nooverscroll,system_statusbarcolor`
- VALUE_DOMAIN: owner-group keys for StatusBarBackgroundCompatHook: system_nooverscroll,system_statusbarcolor
- DEFAULT_SEMANTICS: `system_nooverscroll` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemStatusBarBackgroundHooks.kt::StatusBarBackgroundCompatHook` / `SystemStatusBarAndClockHooks.kt::StatusBarBackgroundCompatHook`: A13 already implements the exclusive keys system_nooverscroll,system_statusbarcolor. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemStatusBarBackgroundHooks.kt::StatusBarBackgroundCompatHook` (hook, phases=intercept) vs A13 `SystemStatusBarAndClockHooks.kt::StatusBarBackgroundCompatHook` (hook, phases=before). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_nooverscroll` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemStatusBarBackgroundHooks.kt::StatusBarBackgroundCompatHook` / `SystemStatusBarAndClockHooks.kt::StatusBarBackgroundCompatHook`: A13 already implements the exclusive keys system_nooverscroll,system_statusbarcolor. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_PreferenceLoadRegistry_kt_shouldLoad__onPackageReady

- PROOF_ID: `PROOF_OG_PreferenceLoadRegistry_kt_shouldLoad__onPackageReady`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/MainModule.java`
- A14_SYMBOL: `onPackageReady`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/MainModule.java`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceLoadRegistry.kt`
- A13_SYMBOL: `shouldLoad`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_nooverscroll_apps`
- VALUE_DOMAIN: owner-group keys for shouldLoad: system_nooverscroll_apps
- DEFAULT_SEMANTICS: `system_nooverscroll_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=False; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `MainModule.java::onPackageReady` / `PreferenceLoadRegistry.kt::shouldLoad`: A13 already implements the exclusive keys system_nooverscroll_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `MainModule.java::onPackageReady` (hook, phases=unknown) vs A13 `PreferenceLoadRegistry.kt::shouldLoad` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_nooverscroll_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `MainModule.java::onPackageReady` / `PreferenceLoadRegistry.kt::shouldLoad`: A13 already implements the exclusive keys system_nooverscroll_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemLockScreenHooks_kt_NoPasswordHook__NoPasswordHook

- PROOF_ID: `PROOF_OG_SystemLockScreenHooks_kt_NoPasswordHook__NoPasswordHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `NoPasswordHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A13_SYMBOL: `NoPasswordHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_nopassword`
- VALUE_DOMAIN: owner-group keys for NoPasswordHook: system_nopassword
- DEFAULT_SEMANTICS: `system_nopassword` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemLockScreenHooks.kt::NoPasswordHook` / `SystemLockScreenHooks.kt::NoPasswordHook`: A13 already implements the exclusive keys system_nopassword. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::NoPasswordHook` (hook, phases=unknown) vs A13 `SystemLockScreenHooks.kt::NoPasswordHook` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_nopassword` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::NoPasswordHook` / `SystemLockScreenHooks.kt::NoPasswordHook`: A13 already implements the exclusive keys system_nopassword. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemLockScreenMoreHooks_kt_NoScreenLockHook__NoScreenLockHook

- PROOF_ID: `PROOF_OG_SystemLockScreenMoreHooks_kt_NoScreenLockHook__NoScreenLockHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `NoScreenLockHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.keyguard.KeyguardViewMediator#handleKeyguardDone,com.android.keyguard.KeyguardUpdateMonitor#onFingerprintAuthenticated,com.android.keyguard.KeyguardSecurityContainerController#onInit,com.android.systemui.keyguard.KeyguardViewMediator#doKeyguardLocked,com.android.systemui.keyguard.KeyguardViewMediator#setupLocked,com.android.keyguard.KeyguardSecurityModel#getSecurityMode,com.android.systemui.statusbar.policy.BluetoothControllerImpl#updateConnected,com.android.systemui.statusbar.policy.BluetoothControllerImpl#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `NoScreenLockHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.keyguard.KeyguardViewMediator#handleKeyguardDone,com.android.keyguard.KeyguardUpdateMonitor#onFingerprintAuthenticated,com.android.systemui.keyguard.KeyguardViewMediator#doKeyguardLocked,com.android.systemui.keyguard.KeyguardViewMediator#setupLocked,com.android.keyguard.KeyguardSecurityModel#getSecurityMode,com.android.systemui.statusbar.policy.BluetoothControllerImpl#updateConnected,#startFaceUnlock,com.android.systemui.keyguard.KeyguardSecurityContainerController#<init>,com.android.systemui.statusbar.policy.BluetoothControllerImpl#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_noscreenlock,system_noscreenlock_act,system_noscreenlock_skip`
- VALUE_DOMAIN: owner-group keys for NoScreenLockHook: system_noscreenlock,system_noscreenlock_act,system_noscreenlock_skip
- DEFAULT_SEMANTICS: `system_noscreenlock` A14 default=1; A13 default=1
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null]; setResult[securityModeNone,false]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemLockScreenHooks.kt::NoScreenLockHook` / `SystemLockScreenMoreHooks.kt::NoScreenLockHook`: A13 already implements the exclusive keys system_noscreenlock,system_noscreenlock_act,system_noscreenlock_skip. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::NoScreenLockHook` (hook, phases=intercept) vs A13 `SystemLockScreenMoreHooks.kt::NoScreenLockHook` (hook, phases=after,before). Shared methods=['<init>', 'doKeyguardLocked', 'getSecurityMode', 'handleKeyguardDone', 'onFingerprintAuthenticated', 'setupLocked', 'updateConnected']; A14-only members=['com.android.keyguard.KeyguardSecurityContainerController#onInit']; A13-only members=['#startFaceUnlock', 'com.android.systemui.keyguard.KeyguardSecurityContainerController#<init>'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_noscreenlock` A14=1 A13=1. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.keyguard.KeyguardViewMediator#handleKeyguardDone,com.android.keyguard.KeyguardUpdateMonitor#onFingerprintAuthenticated,com.android.keyguard.KeyguardSecurityContainerController#onInit,com.android.systemui.keyguard.KeyguardViewMediator#doKeyguardLocked,com.android.systemui.keyguard.KeyguardViewMediator#setupLocked,com.android.keyguard.KeyguardSecurityModel#getSecurityMode,com.android.systemui.statusbar.policy.BluetoothControllerImpl#updateConnected,com.android.systemui.statusbar.policy.BluetoothControllerImpl#<init>; A13=com.android.systemui.keyguard.KeyguardViewMediator#handleKeyguardDone,com.android.keyguard.KeyguardUpdateMonitor#onFingerprintAuthenticated,com.android.systemui.keyguard.KeyguardViewMediator#doKeyguardLocked,com.android.systemui.keyguard.KeyguardViewMediator#setupLocked,com.android.keyguard.KeyguardSecurityModel#getSecurityMode,com.android.systemui.statusbar.policy.BluetoothControllerImpl#updateConnected,#startFaceUnlock,com.android.systemui.keyguard.KeyguardSecurityContainerController#<init>,com.android.systemui.statusbar.policy.BluetoothControllerImpl#<init>; shared_methods=['<init>', 'doKeyguardLocked', 'getSecurityMode', 'handleKeyguardDone', 'onFingerprintAuthenticated', 'setupLocked', 'updateConnected']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[null,null]; setResult[securityModeNone,false]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.keyguard.KeyguardSecurityContainerController#onInit']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::NoScreenLockHook` / `SystemLockScreenMoreHooks.kt::NoScreenLockHook`: A13 already implements the exclusive keys system_noscreenlock,system_noscreenlock_act,system_noscreenlock_skip. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_System_NoScreenLock_kt_openBtNetworks__openBtNetworks

- PROOF_ID: `PROOF_OG_System_NoScreenLock_kt_openBtNetworks__openBtNetworks`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A14_SYMBOL: `openBtNetworks`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A13_SYMBOL: `openBtNetworks`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_noscreenlock_bt`
- VALUE_DOMAIN: owner-group keys for openBtNetworks: system_noscreenlock_bt
- DEFAULT_SEMANTICS: `system_noscreenlock_bt` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System_NoScreenLock.kt::openBtNetworks` / `System_NoScreenLock.kt::openBtNetworks`: A13 already implements the exclusive keys system_noscreenlock_bt. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System_NoScreenLock.kt::openBtNetworks` (hook, phases=unknown) vs A13 `System_NoScreenLock.kt::openBtNetworks` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_noscreenlock_bt` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System_NoScreenLock.kt::openBtNetworks` / `System_NoScreenLock.kt::openBtNetworks`: A13 already implements the exclusive keys system_noscreenlock_bt. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
- PROOF_CONCLUSION: `PRESENT_EQUIVALENT`

## PROOF_OG_System_NoScreenLock_kt_openWifiNetworks__openWifiNetworks

- PROOF_ID: `PROOF_OG_System_NoScreenLock_kt_openWifiNetworks__openWifiNetworks`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A14_SYMBOL: `openWifiNetworks`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A13_SYMBOL: `openWifiNetworks`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_NoScreenLock.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_noscreenlock_wifi`
- VALUE_DOMAIN: owner-group keys for openWifiNetworks: system_noscreenlock_wifi
- DEFAULT_SEMANTICS: `system_noscreenlock_wifi` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System_NoScreenLock.kt::openWifiNetworks` / `System_NoScreenLock.kt::openWifiNetworks`: A13 already implements the exclusive keys system_noscreenlock_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System_NoScreenLock.kt::openWifiNetworks` (hook, phases=unknown) vs A13 `System_NoScreenLock.kt::openWifiNetworks` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_noscreenlock_wifi` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System_NoScreenLock.kt::openWifiNetworks` / `System_NoScreenLock.kt::openWifiNetworks`: A13 already implements the exclusive keys system_noscreenlock_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_BlurVolumeDialogBackgroundHook__BlurVolumeDialogBackgroundHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_BlurVolumeDialogBackgroundHook__BlurVolumeDialogBackgroundHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `BlurVolumeDialogBackgroundHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl#updateDialogWindowH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#showH`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `BlurVolumeDialogBackgroundHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl#updateDialogWindowH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#showH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#initDialog`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_nosilentvibrate,system_volumeblur_collapsed,system_volumeblur_expanded,system_volumedialogdelay_collapsed,system_volumedialogdelay_expanded`
- VALUE_DOMAIN: owner-group keys for BlurVolumeDialogBackgroundHook: system_nosilentvibrate,system_volumeblur_collapsed,system_volumeblur_expanded,system_volumedialogdelay_collapsed,system_volumedialogdelay_expanded
- DEFAULT_SEMANTICS: `system_nosilentvibrate` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after` vs A13 phase `after,before`. Owner-group review of `SystemUIControlCenterHooks.kt::BlurVolumeDialogBackgroundHook` / `SystemUIControlCenterHooks.kt::BlurVolumeDialogBackgroundHook`: A13 already implements the exclusive keys system_nosilentvibrate,system_volumeblur_collapsed,system_volumeblur_expanded,system_volumedialogdelay_collapsed,system_volumedialogdelay_expanded. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::BlurVolumeDialogBackgroundHook` (hook, phases=after) vs A13 `SystemUIControlCenterHooks.kt::BlurVolumeDialogBackgroundHook` (hook, phases=after,before). Shared methods=['showH', 'updateDialogWindowH']; A14-only members=none; A13-only members=['com.android.systemui.miui.volume.MiuiVolumeDialogImpl#initDialog'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_nosilentvibrate` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.miui.volume.MiuiVolumeDialogImpl#updateDialogWindowH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#showH; A13=com.android.systemui.miui.volume.MiuiVolumeDialogImpl#updateDialogWindowH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#showH,com.android.systemui.miui.volume.MiuiVolumeDialogImpl#initDialog; shared_methods=['showH', 'updateDialogWindowH']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::BlurVolumeDialogBackgroundHook` / `SystemUIControlCenterHooks.kt::BlurVolumeDialogBackgroundHook`: A13 already implements the exclusive keys system_nosilentvibrate,system_volumeblur_collapsed,system_volumeblur_expanded,system_volumedialogdelay_collapsed,system_volumedialogdelay_expanded. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSecurityAndSystemHooks_kt_NoSOSHook__NoSOSHook

- PROOF_ID: `PROOF_OG_SystemSecurityAndSystemHooks_kt_NoSOSHook__NoSOSHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `NoSOSHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.keyguard.EmergencyButtonController#updateEmergencyCallButton`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `NoSOSHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.keyguard.EmergencyButton#updateEmergencyCallButton`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_nosos`
- VALUE_DOMAIN: owner-group keys for NoSOSHook: system_nosos
- DEFAULT_SEMANTICS: `system_nosos` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemLockScreenHooks.kt::NoSOSHook` / `SystemSecurityAndSystemHooks.kt::NoSOSHook`: A13 already implements the exclusive keys system_nosos. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::NoSOSHook` (hook, phases=intercept) vs A13 `SystemSecurityAndSystemHooks.kt::NoSOSHook` (hook, phases=after). Shared methods=['updateEmergencyCallButton']; A14-only members=['com.android.keyguard.EmergencyButtonController#updateEmergencyCallButton']; A13-only members=['com.android.keyguard.EmergencyButton#updateEmergencyCallButton'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_nosos` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.keyguard.EmergencyButtonController#updateEmergencyCallButton; A13=com.android.keyguard.EmergencyButton#updateEmergencyCallButton; shared_methods=['updateEmergencyCallButton']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.keyguard.EmergencyButtonController#updateEmergencyCallButton']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::NoSOSHook` / `SystemSecurityAndSystemHooks.kt::NoSOSHook`: A13 already implements the exclusive keys system_nosos. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSettingsAndConnectivityHooks_kt_ViewWifiPasswordHook__ViewWifiPasswordHook

- PROOF_ID: `PROOF_OG_SystemSettingsAndConnectivityHooks_kt_ViewWifiPasswordHook__ViewWifiPasswordHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ViewWifiPasswordHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `com.android.settings.wifi.SavedAccessPointPreference#onBindViewHolder,miuix.appcompat.app.AlertDialog\$Builder#setTitle,miuix.appcompat.app.AlertDialog\$Builder#setMessage,miuix.appcompat.app.AlertDialog#onCreate,com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings#showDeleteDialog`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSettingsAndConnectivityHooks.kt`
- A13_SYMBOL: `ViewWifiPasswordHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java`
- A13_HOOK_TARGETS: `com.android.settings.wifi.SavedAccessPointPreference#onBindViewHolder,miuix.appcompat.app.AlertDialog\$Builder#setTitle,miuix.appcompat.app.AlertDialog\$Builder#setMessage,miuix.appcompat.app.AlertDialog#onCreate,com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings#showDeleteDialog`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_notifimportance,system_wifipassword`
- VALUE_DOMAIN: owner-group keys for ViewWifiPasswordHook: system_notifimportance,system_wifipassword
- DEFAULT_SEMANTICS: `system_notifimportance` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `System.kt::ViewWifiPasswordHook` / `SystemSettingsAndConnectivityHooks.kt::ViewWifiPasswordHook`: A13 already implements the exclusive keys system_notifimportance,system_wifipassword. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::ViewWifiPasswordHook` (hook, phases=intercept) vs A13 `SystemSettingsAndConnectivityHooks.kt::ViewWifiPasswordHook` (hook, phases=after,before). Shared methods=['onBindViewHolder', 'onCreate', 'setMessage', 'setTitle', 'showDeleteDialog']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_notifimportance` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.settings.wifi.SavedAccessPointPreference#onBindViewHolder,miuix.appcompat.app.AlertDialog\$Builder#setTitle,miuix.appcompat.app.AlertDialog\$Builder#setMessage,miuix.appcompat.app.AlertDialog#onCreate,com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings#showDeleteDialog; A13=com.android.settings.wifi.SavedAccessPointPreference#onBindViewHolder,miuix.appcompat.app.AlertDialog\$Builder#setTitle,miuix.appcompat.app.AlertDialog\$Builder#setMessage,miuix.appcompat.app.AlertDialog#onCreate,com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings#showDeleteDialog; shared_methods=['onBindViewHolder', 'onCreate', 'setMessage', 'setTitle', 'showDeleteDialog']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::ViewWifiPasswordHook` / `SystemSettingsAndConnectivityHooks.kt::ViewWifiPasswordHook`: A13 already implements the exclusive keys system_notifimportance,system_wifipassword. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_NotificationRowMenuHook__NotificationRowMenuHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_NotificationRowMenuHook__NotificationRowMenuHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_SYMBOL: `NotificationRowMenuHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow#createMenuViews`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `NotificationRowMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `#createMenuViews`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_notifrowmenu`
- VALUE_DOMAIN: owner-group keys for NotificationRowMenuHook: system_notifrowmenu
- DEFAULT_SEMANTICS: `system_notifrowmenu` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemNotificationHooks.kt::NotificationRowMenuHook` / `SystemNotificationMoreHooks.kt::NotificationRowMenuHook`: A13 already implements the exclusive keys system_notifrowmenu. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemNotificationHooks.kt::NotificationRowMenuHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::NotificationRowMenuHook` (hook, phases=after). Shared methods=['createMenuViews']; A14-only members=['com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow#createMenuViews']; A13-only members=['#createMenuViews'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_notifrowmenu` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow#createMenuViews; A13=#createMenuViews; shared_methods=['createMenuViews']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow#createMenuViews']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemNotificationHooks.kt::NotificationRowMenuHook` / `SystemNotificationMoreHooks.kt::NotificationRowMenuHook`: A13 already implements the exclusive keys system_notifrowmenu. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_WallpaperScaleLevelHook__WallpaperScaleLevelHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_WallpaperScaleLevelHook__WallpaperScaleLevelHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `WallpaperScaleLevelHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.wm.WallpaperController#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `WallpaperScaleLevelHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.WallpaperController#<init>`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_other_wallpaper_scale`
- VALUE_DOMAIN: owner-group keys for WallpaperScaleLevelHook: system_other_wallpaper_scale
- DEFAULT_SEMANTICS: `system_other_wallpaper_scale` A14 default=n/a; A13 default=6
- RESULT/ARGUMENT_BEHAVIOR: A14 chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemDisplayHooks.kt::WallpaperScaleLevelHook` / `SystemNotificationMoreHooks.kt::WallpaperScaleLevelHook`: A13 already implements the exclusive keys system_other_wallpaper_scale. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemDisplayHooks.kt::WallpaperScaleLevelHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::WallpaperScaleLevelHook` (hook, phases=after). Shared methods=['<init>']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_other_wallpaper_scale` A14=n/a A13=6. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.wm.WallpaperController#<init>; A13=com.android.server.wm.WallpaperController#<init>; shared_methods=['<init>']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemDisplayHooks.kt::WallpaperScaleLevelHook` / `SystemNotificationMoreHooks.kt::WallpaperScaleLevelHook`: A13 already implements the exclusive keys system_other_wallpaper_scale. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_MIUIVolumeDialogHook__CCClockTweakHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_MIUIVolumeDialogHook__CCClockTweakHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `CCClockTweakHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.qs.MiuiNotificationHeaderView#updateResources`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `MIUIVolumeDialogHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.miui.volume.MiuiVolumeDialogImpl#vibrateH,miui.systemui.util.SystemUIResourcesHelperImpl#getBoolean`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_qs_force_systemfonts`
- VALUE_DOMAIN: owner-group keys for MIUIVolumeDialogHook: system_qs_force_systemfonts
- DEFAULT_SEMANTICS: `system_qs_force_systemfonts` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[java.lang.Boolean.TRUE]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemClockHooks.kt::CCClockTweakHook` / `SystemUIControlCenterHooks.kt::MIUIVolumeDialogHook`: A13 already implements the exclusive keys system_qs_force_systemfonts. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_clock_fontsize', 'system_cc_clock_verticaloffset'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemClockHooks.kt::CCClockTweakHook` (hook, phases=intercept) vs A13 `SystemUIControlCenterHooks.kt::MIUIVolumeDialogHook` (hook, phases=after,before). Shared methods=none; A14-only members=['com.android.systemui.qs.MiuiNotificationHeaderView#updateResources']; A13-only members=['com.android.systemui.miui.volume.MiuiVolumeDialogImpl#vibrateH', 'miui.systemui.util.SystemUIResourcesHelperImpl#getBoolean'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_qs_force_systemfonts` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.qs.MiuiNotificationHeaderView#updateResources; A13=com.android.systemui.miui.volume.MiuiVolumeDialogImpl#vibrateH,miui.systemui.util.SystemUIResourcesHelperImpl#getBoolean; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[java.lang.Boolean.TRUE]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_clock_fontsize', 'system_cc_clock_verticaloffset']; A14-only hook members=['com.android.systemui.qs.MiuiNotificationHeaderView#updateResources']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemClockHooks.kt::CCClockTweakHook` / `SystemUIControlCenterHooks.kt::MIUIVolumeDialogHook`: A13 already implements the exclusive keys system_qs_force_systemfonts. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_clock_fontsize', 'system_cc_clock_verticaloffset'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSecurityAndSystemHooks_kt_RemoveActStartConfirmHook__RemoveActStartConfirmHook

- PROOF_ID: `PROOF_OG_SystemSecurityAndSystemHooks_kt_RemoveActStartConfirmHook__RemoveActStartConfirmHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `RemoveActStartConfirmHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_HOOK_TARGETS: `com.miui.server.SecurityManagerService\$LocalService#checkAllowStartActivity`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `RemoveActStartConfirmHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_remove_startactconfirm`
- VALUE_DOMAIN: owner-group keys for RemoveActStartConfirmHook: system_remove_startactconfirm
- DEFAULT_SEMANTICS: `system_remove_startactconfirm` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemSecurityHooks.kt::RemoveActStartConfirmHook` / `SystemSecurityAndSystemHooks.kt::RemoveActStartConfirmHook`: A13 already implements the exclusive keys system_remove_startactconfirm. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemSecurityHooks.kt::RemoveActStartConfirmHook` (hook, phases=unknown) vs A13 `SystemSecurityAndSystemHooks.kt::RemoveActStartConfirmHook` (hook, phases=unknown). Shared methods=none; A14-only members=['com.miui.server.SecurityManagerService\\$LocalService#checkAllowStartActivity']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_remove_startactconfirm` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.server.SecurityManagerService\$LocalService#checkAllowStartActivity; A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.miui.server.SecurityManagerService\\$LocalService#checkAllowStartActivity']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemSecurityHooks.kt::RemoveActStartConfirmHook` / `SystemSecurityAndSystemHooks.kt::RemoveActStartConfirmHook`: A13 already implements the exclusive keys system_remove_startactconfirm. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarAndClockHooks_kt_HideMemoryCleanHook__HideMemoryCleanHook

- PROOF_ID: `PROOF_OG_SystemStatusBarAndClockHooks_kt_HideMemoryCleanHook__HideMemoryCleanHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `HideMemoryCleanHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt`
- A13_SYMBOL: `HideMemoryCleanHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_removecleaner`
- VALUE_DOMAIN: owner-group keys for HideMemoryCleanHook: system_removecleaner
- DEFAULT_SEMANTICS: `system_removecleaner` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `System.kt::HideMemoryCleanHook` / `SystemStatusBarAndClockHooks.kt::HideMemoryCleanHook`: A13 already implements the exclusive keys system_removecleaner. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::HideMemoryCleanHook` (hook, phases=intercept) vs A13 `SystemStatusBarAndClockHooks.kt::HideMemoryCleanHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_removecleaner` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::HideMemoryCleanHook` / `SystemStatusBarAndClockHooks.kt::HideMemoryCleanHook`: A13 already implements the exclusive keys system_removecleaner. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUINotificationHooks_kt_HideDismissViewHook__HideDismissViewHook

- PROOF_ID: `PROOF_OG_SystemUINotificationHooks_kt_HideDismissViewHook__HideDismissViewHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_SYMBOL: `HideDismissViewHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.shade.MiuiNotificationPanelViewController#updateDismissView`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `HideDismissViewHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateDismissView`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_removedismiss`
- VALUE_DOMAIN: owner-group keys for HideDismissViewHook: system_removedismiss
- DEFAULT_SEMANTICS: `system_removedismiss` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before` vs A13 phase `before`. Owner-group review of `SystemUINotificationHooks.kt::HideDismissViewHook` / `SystemUINotificationHooks.kt::HideDismissViewHook`: A13 already implements the exclusive keys system_removedismiss. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUINotificationHooks.kt::HideDismissViewHook` (hook, phases=before) vs A13 `SystemUINotificationHooks.kt::HideDismissViewHook` (hook, phases=before). Shared methods=['updateDismissView']; A14-only members=['com.android.systemui.shade.MiuiNotificationPanelViewController#updateDismissView']; A13-only members=['com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateDismissView'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_removedismiss` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.shade.MiuiNotificationPanelViewController#updateDismissView; A13=com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController#updateDismissView; shared_methods=['updateDismissView']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.shade.MiuiNotificationPanelViewController#updateDismissView']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUINotificationHooks.kt::HideDismissViewHook` / `SystemUINotificationHooks.kt::HideDismissViewHook`: A13 already implements the exclusive keys system_removedismiss. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSecurityAndSystemHooks_kt_RemoveSecureHook__RemoveSecureHook

- PROOF_ID: `PROOF_OG_SystemSecurityAndSystemHooks_kt_RemoveSecureHook__RemoveSecureHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_SYMBOL: `RemoveSecureHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.wm.WindowState#isSecureLocked,com.android.server.wm.WindowSurfaceController#setSecure,com.android.server.wm.WindowManagerServiceImpl#notAllowCaptureDisplay,com.android.server.wm.WindowSurfaceController#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt`
- A13_SYMBOL: `RemoveSecureHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.wm.WindowState#isSecureLocked,com.android.server.wm.WindowSurfaceController#setSecure,com.android.server.wm.WindowSurfaceController#<init>`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_removesecure`
- VALUE_DOMAIN: owner-group keys for RemoveSecureHook: system_removesecure
- DEFAULT_SEMANTICS: `system_removesecure` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,false,null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `SystemSecurityHooks.kt::RemoveSecureHook` / `SystemSecurityAndSystemHooks.kt::RemoveSecureHook`: A13 already implements the exclusive keys system_removesecure. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemSecurityHooks.kt::RemoveSecureHook` (hook, phases=intercept) vs A13 `SystemSecurityAndSystemHooks.kt::RemoveSecureHook` (hook, phases=before). Shared methods=['<init>', 'isSecureLocked', 'setSecure']; A14-only members=['com.android.server.wm.WindowManagerServiceImpl#notAllowCaptureDisplay']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_removesecure` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.wm.WindowState#isSecureLocked,com.android.server.wm.WindowSurfaceController#setSecure,com.android.server.wm.WindowManagerServiceImpl#notAllowCaptureDisplay,com.android.server.wm.WindowSurfaceController#<init>; A13=com.android.server.wm.WindowState#isSecureLocked,com.android.server.wm.WindowSurfaceController#setSecure,com.android.server.wm.WindowSurfaceController#<init>; shared_methods=['<init>', 'isSecureLocked', 'setSecure']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,false,null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.server.wm.WindowManagerServiceImpl#notAllowCaptureDisplay']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemSecurityHooks.kt::RemoveSecureHook` / `SystemSecurityAndSystemHooks.kt::RemoveSecureHook`: A13 already implements the exclusive keys system_removesecure. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_LauncherLayoutHooks_kt_ResizableWidgetsHook__ResizableWidgetsHook

- PROOF_ID: `PROOF_OG_LauncherLayoutHooks_kt_ResizableWidgetsHook__ResizableWidgetsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `ResizableWidgetsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `android.appwidget.AppWidgetHostView#getAppWidgetInfo`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `ResizableWidgetsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `android.appwidget.AppWidgetHostView#getAppWidgetInfo`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_resizablewidgets`
- VALUE_DOMAIN: owner-group keys for ResizableWidgetsHook: system_resizablewidgets
- DEFAULT_SEMANTICS: `system_resizablewidgets` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 setResult[widgetInfo]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `LauncherLayoutHooks.kt::ResizableWidgetsHook` / `LauncherLayoutHooks.kt::ResizableWidgetsHook`: A13 already implements the exclusive keys system_resizablewidgets. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `LauncherLayoutHooks.kt::ResizableWidgetsHook` (hook, phases=intercept) vs A13 `LauncherLayoutHooks.kt::ResizableWidgetsHook` (hook, phases=after). Shared methods=['getAppWidgetInfo']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_resizablewidgets` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=android.appwidget.AppWidgetHostView#getAppWidgetInfo; A13=android.appwidget.AppWidgetHostView#getAppWidgetInfo; shared_methods=['getAppWidgetInfo']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 setResult[widgetInfo]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `LauncherLayoutHooks.kt::ResizableWidgetsHook` / `LauncherLayoutHooks.kt::ResizableWidgetsHook`: A13 already implements the exclusive keys system_resizablewidgets. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemDisplayAndWindowHooks_kt_ScreenAnimHook__ScreenAnimHook

- PROOF_ID: `PROOF_OG_SystemDisplayAndWindowHooks_kt_ScreenAnimHook__ScreenAnimHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_SYMBOL: `ScreenAnimHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.display.DisplayPowerController#initialize`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt`
- A13_SYMBOL: `ScreenAnimHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.display.DisplayPowerController#initialize`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_screenanim_duration`
- VALUE_DOMAIN: owner-group keys for ScreenAnimHook: system_screenanim_duration
- DEFAULT_SEMANTICS: `system_screenanim_duration` A14 default=n/a; A13 default=0
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before,after`. Owner-group review of `SystemDisplayHooks.kt::ScreenAnimHook` / `SystemDisplayAndWindowHooks.kt::ScreenAnimHook`: A13 already implements the exclusive keys system_screenanim_duration. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemDisplayHooks.kt::ScreenAnimHook` (hook, phases=intercept) vs A13 `SystemDisplayAndWindowHooks.kt::ScreenAnimHook` (hook, phases=before,after). Shared methods=['initialize']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_screenanim_duration` A14=n/a A13=0. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.display.DisplayPowerController#initialize; A13=com.android.server.display.DisplayPowerController#initialize; shared_methods=['initialize']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemDisplayHooks.kt::ScreenAnimHook` / `SystemDisplayAndWindowHooks.kt::ScreenAnimHook`: A13 already implements the exclusive keys system_screenanim_duration. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ToastTimeHook__ToastTimeHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ToastTimeHook__ToastTimeHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ToastTimeHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `com.android.server.notification.NotificationManagerService#showNextToastLocked`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `ToastTimeHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.server.notification.NotificationManagerService#showNextToastLocked`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_screenshot_format,system_screenshot_quality,system_toasttime`
- VALUE_DOMAIN: owner-group keys for ToastTimeHook: system_screenshot_format,system_screenshot_quality,system_toasttime
- DEFAULT_SEMANTICS: `system_screenshot_format` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `System.kt::ToastTimeHook` / `SystemAudioAndVisualAndMoreHooks.kt::ToastTimeHook`: A13 already implements the exclusive keys system_screenshot_format,system_screenshot_quality,system_toasttime. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::ToastTimeHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::ToastTimeHook` (hook, phases=after,before). Shared methods=['showNextToastLocked']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_screenshot_format` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.notification.NotificationManagerService#showNextToastLocked; A13=com.android.server.notification.NotificationManagerService#showNextToastLocked; shared_methods=['showNextToastLocked']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::ToastTimeHook` / `SystemAudioAndVisualAndMoreHooks.kt::ToastTimeHook`: A13 already implements the exclusive keys system_screenshot_format,system_screenshot_quality,system_toasttime. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_System_ScreenshotConfig_kt_onActivityResult__onActivityResult

- PROOF_ID: `PROOF_OG_System_ScreenshotConfig_kt_onActivityResult__onActivityResult`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_ScreenshotConfig.kt`
- A14_SYMBOL: `onActivityResult`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_ScreenshotConfig.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_ScreenshotConfig.kt`
- A13_SYMBOL: `onActivityResult`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System_ScreenshotConfig.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_screenshot_mypath`
- VALUE_DOMAIN: owner-group keys for onActivityResult: system_screenshot_mypath
- DEFAULT_SEMANTICS: `system_screenshot_mypath` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `System_ScreenshotConfig.kt::onActivityResult` / `System_ScreenshotConfig.kt::onActivityResult`: A13 already implements the exclusive keys system_screenshot_mypath. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System_ScreenshotConfig.kt::onActivityResult` (hook, phases=unknown) vs A13 `System_ScreenshotConfig.kt::onActivityResult` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_screenshot_mypath` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System_ScreenshotConfig.kt::onActivityResult` / `System_ScreenshotConfig.kt::onActivityResult`: A13 already implements the exclusive keys system_screenshot_mypath. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ScreenshotConfigHook__ScreenshotConfigHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_ScreenshotConfigHook__ScreenshotConfigHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `ScreenshotConfigHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `android.content.ContentResolver#update,android.content.ContentResolver#insert,android.graphics.Bitmap#compress`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `ScreenshotConfigHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_HOOK_TARGETS: `android.content.ContentResolver#update,android.content.ContentResolver#insert,com.miui.screenshot.MiuiScreenshotApplication#attachBaseContext,com.miui.screenshot.u0.f\$a#a,com.miui.screenshot.x0.e\$a#a,android.graphics.Bitmap#compress`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_screenshot_path`
- VALUE_DOMAIN: owner-group keys for ScreenshotConfigHook: system_screenshot_path
- DEFAULT_SEMANTICS: `system_screenshot_path` A14 default=1; A13 default=1
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before,after`. Owner-group review of `System.kt::ScreenshotConfigHook` / `SystemAudioAndVisualAndMoreHooks.kt::ScreenshotConfigHook`: A13 already implements the exclusive keys system_screenshot_path. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `System.kt::ScreenshotConfigHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::ScreenshotConfigHook` (hook, phases=before,after). Shared methods=['compress', 'insert', 'update']; A14-only members=none; A13-only members=['com.miui.screenshot.MiuiScreenshotApplication#attachBaseContext', 'com.miui.screenshot.u0.f\\$a#a', 'com.miui.screenshot.x0.e\\$a#a'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_screenshot_path` A14=1 A13=1. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=android.content.ContentResolver#update,android.content.ContentResolver#insert,android.graphics.Bitmap#compress; A13=android.content.ContentResolver#update,android.content.ContentResolver#insert,com.miui.screenshot.MiuiScreenshotApplication#attachBaseContext,com.miui.screenshot.u0.f\$a#a,com.miui.screenshot.x0.e\$a#a,android.graphics.Bitmap#compress; shared_methods=['compress', 'insert', 'update']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `System.kt::ScreenshotConfigHook` / `SystemAudioAndVisualAndMoreHooks.kt::ScreenshotConfigHook`: A13 already implements the exclusive keys system_screenshot_path. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemLockScreenHooks_kt_EnhancedSecurityHook__EnhancedSecurityHook

- PROOF_ID: `PROOF_OG_SystemLockScreenHooks_kt_EnhancedSecurityHook__EnhancedSecurityHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `EnhancedSecurityHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#interceptPowerKeyDown,com.android.server.policy.PhoneWindowManager#powerLongPress,com.android.server.policy.PhoneWindowManager#showGlobalActions,com.android.server.policy.PhoneWindowManager#showGlobalActionsInternal`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A13_SYMBOL: `EnhancedSecurityHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.policy.PhoneWindowManager#showGlobalActions,com.android.server.policy.PhoneWindowManager#showGlobalActionsInternal`
- A13_CALLBACK_PHASE: `after,before,intercept`
- PREFERENCE_KEYS: `system_securelock`
- VALUE_DOMAIN: owner-group keys for EnhancedSecurityHook: system_securelock
- DEFAULT_SEMANTICS: `system_securelock` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before,intercept`. Owner-group review of `SystemLockScreenHooks.kt::EnhancedSecurityHook` / `SystemLockScreenHooks.kt::EnhancedSecurityHook`: A13 already implements the exclusive keys system_securelock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemLockScreenHooks.kt::EnhancedSecurityHook` (hook, phases=intercept) vs A13 `SystemLockScreenHooks.kt::EnhancedSecurityHook` (hook, phases=after,before,intercept). Shared methods=['showGlobalActions', 'showGlobalActionsInternal']; A14-only members=['com.android.server.policy.PhoneWindowManager#interceptPowerKeyDown', 'com.android.server.policy.PhoneWindowManager#powerLongPress']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_securelock` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.policy.PhoneWindowManager#interceptPowerKeyDown,com.android.server.policy.PhoneWindowManager#powerLongPress,com.android.server.policy.PhoneWindowManager#showGlobalActions,com.android.server.policy.PhoneWindowManager#showGlobalActionsInternal; A13=com.android.server.policy.PhoneWindowManager#showGlobalActions,com.android.server.policy.PhoneWindowManager#showGlobalActionsInternal; shared_methods=['showGlobalActions', 'showGlobalActionsInternal']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before,intercept. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.server.policy.PhoneWindowManager#interceptPowerKeyDown', 'com.android.server.policy.PhoneWindowManager#powerLongPress']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemLockScreenHooks.kt::EnhancedSecurityHook` / `SystemLockScreenHooks.kt::EnhancedSecurityHook`: A13 already implements the exclusive keys system_securelock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUILockScreenHooks_kt_isSecureTile__SecureQSTilesHook

- PROOF_ID: `PROOF_OG_SystemUILockScreenHooks_kt_isSecureTile__SecureQSTilesHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `SecureQSTilesHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click,com.android.systemui.qs.tileimpl.QSTileImpl#longClick,com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `isSecureTile`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_secureqs_airplane,system_secureqs_bt,system_secureqs_custom,system_secureqs_hotspot,system_secureqs_location,system_secureqs_mobiledata,system_secureqs_nfc,system_secureqs_sync,system_secureqs_wifi`
- VALUE_DOMAIN: owner-group keys for isSecureTile: system_secureqs_airplane,system_secureqs_bt,system_secureqs_custom,system_secureqs_hotspot,system_secureqs_location,system_secureqs_mobiledata,system_secureqs_nfc,system_secureqs_sync
- DEFAULT_SEMANTICS: `system_secureqs_airplane` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[null]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `before` vs A13 phase `unknown`. Owner-group review of `SystemUIControlCenterHooks.kt::SecureQSTilesHook` / `SystemUILockScreenHooks.kt::isSecureTile`: A13 already implements the exclusive keys system_secureqs_airplane,system_secureqs_bt,system_secureqs_custom,system_secureqs_hotspot,system_secureqs_location,system_secureqs_mobiledata,system_secureqs_nfc,system_secureqs_sync,system_secureqs_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_secureqs_keepopened'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::SecureQSTilesHook` (hook, phases=before) vs A13 `SystemUILockScreenHooks.kt::isSecureTile` (hook, phases=unknown). Shared methods=none; A14-only members=['com.android.systemui.qs.tileimpl.QSTileImpl#click', 'com.android.systemui.qs.tileimpl.QSTileImpl#longClick', 'com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick']; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_secureqs_airplane` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.qs.tileimpl.QSTileImpl#click,com.android.systemui.qs.tileimpl.QSTileImpl#longClick,com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick; A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[null]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_secureqs_keepopened']; A14-only hook members=['com.android.systemui.qs.tileimpl.QSTileImpl#click', 'com.android.systemui.qs.tileimpl.QSTileImpl#longClick', 'com.android.systemui.qs.tileimpl.QSTileImpl#secondaryClick']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::SecureQSTilesHook` / `SystemUILockScreenHooks.kt::isSecureTile`: A13 already implements the exclusive keys system_secureqs_airplane,system_secureqs_bt,system_secureqs_custom,system_secureqs_hotspot,system_secureqs_location,system_secureqs_mobiledata,system_secureqs_nfc,system_secureqs_sync,system_secureqs_wifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_secureqs_keepopened'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_BrightnessPctHook__BrightnessPctHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_BrightnessPctHook__BrightnessPctHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `BrightnessPctHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStart,com.android.systemui.controlcenter.policy.MiuiBrightnessController#setToggleSliderBase,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStop,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onChanged,#onStartTrackingTouch`
- A14_CALLBACK_PHASE: `before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `BrightnessPctHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.policy.BrightnessMirrorController#showMirror,com.android.systemui.statusbar.policy.BrightnessMirrorController#hideMirror,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStart,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStop,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onChanged`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_showpct_top`
- VALUE_DOMAIN: owner-group keys for BrightnessPctHook: system_showpct_top
- DEFAULT_SEMANTICS: `system_showpct_top` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before,after` vs A13 phase `after,before`. Owner-group review of `SystemUIControlCenterHooks.kt::BrightnessPctHook` / `SystemUIControlCenterHooks.kt::BrightnessPctHook`: A13 already implements the exclusive keys system_showpct_top. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::BrightnessPctHook` (hook, phases=before,after) vs A13 `SystemUIControlCenterHooks.kt::BrightnessPctHook` (hook, phases=after,before). Shared methods=['onChanged', 'onStart', 'onStop']; A14-only members=['#onStartTrackingTouch', 'com.android.systemui.controlcenter.policy.MiuiBrightnessController#setToggleSliderBase']; A13-only members=['com.android.systemui.statusbar.policy.BrightnessMirrorController#hideMirror', 'com.android.systemui.statusbar.policy.BrightnessMirrorController#showMirror'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_showpct_top` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStart,com.android.systemui.controlcenter.policy.MiuiBrightnessController#setToggleSliderBase,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStop,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onChanged,#onStartTrackingTouch; A13=com.android.systemui.statusbar.policy.BrightnessMirrorController#showMirror,com.android.systemui.statusbar.policy.BrightnessMirrorController#hideMirror,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStart,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onStop,com.android.systemui.controlcenter.policy.MiuiBrightnessController#onChanged; shared_methods=['onChanged', 'onStart', 'onStop']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before,after; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['#onStartTrackingTouch', 'com.android.systemui.controlcenter.policy.MiuiBrightnessController#setToggleSliderBase']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::BrightnessPctHook` / `SystemUIControlCenterHooks.kt::BrightnessPctHook`: A13 already implements the exclusive keys system_showpct_top. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUILockScreenHooks_kt_LockScreenTopMarginHook__LockScreenTopMarginHook

- PROOF_ID: `PROOF_OG_SystemUILockScreenHooks_kt_LockScreenTopMarginHook__LockScreenTopMarginHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `LockScreenTopMarginHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#updateViewStatusBarPaddingTop,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#onFinishInflate`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A13_SYMBOL: `LockScreenTopMarginHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#updateViewStatusBarPaddingTop,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#onFinishInflate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbar_batterystyle,system_statusbar_batterytempandcurrent,system_statusbar_clock_position,system_statusbar_dualrows,system_statusbar_showdevicetemperature,system_statusbar_topmargin,system_statusbar_topmargin_unset_lockscreen`
- VALUE_DOMAIN: owner-group keys for LockScreenTopMarginHook: system_statusbar_batterystyle,system_statusbar_batterytempandcurrent,system_statusbar_clock_position,system_statusbar_dualrows,system_statusbar_showdevicetemperature,system_statusbar_topmargin,system_statusbar_topmargin_unset_lockscreen
- DEFAULT_SEMANTICS: `system_statusbar_batterystyle` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after,before` vs A13 phase `after,before`. Owner-group review of `SystemUILockScreenHooks.kt::LockScreenTopMarginHook` / `SystemUILockScreenHooks.kt::LockScreenTopMarginHook`: A13 already implements the exclusive keys system_statusbar_batterystyle,system_statusbar_batterytempandcurrent,system_statusbar_clock_position,system_statusbar_dualrows,system_statusbar_showdevicetemperature,system_statusbar_topmargin,system_statusbar_topmargin_unset_lockscreen. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUILockScreenHooks.kt::LockScreenTopMarginHook` (hook, phases=after,before) vs A13 `SystemUILockScreenHooks.kt::LockScreenTopMarginHook` (hook, phases=after,before). Shared methods=['onCreate', 'onFinishInflate', 'updateViewStatusBarPaddingTop']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_batterystyle` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#updateViewStatusBarPaddingTop,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#onFinishInflate; A13=com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#updateViewStatusBarPaddingTop,com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView#onFinishInflate; shared_methods=['onCreate', 'onFinishInflate', 'updateViewStatusBarPaddingTop']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after,before; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[null]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUILockScreenHooks.kt::LockScreenTopMarginHook` / `SystemUILockScreenHooks.kt::LockScreenTopMarginHook`: A13 already implements the exclusive keys system_statusbar_batterystyle,system_statusbar_batterytempandcurrent,system_statusbar_clock_position,system_statusbar_dualrows,system_statusbar_showdevicetemperature,system_statusbar_topmargin,system_statusbar_topmargin_unset_lockscreen. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIBatteryHooks_kt_StatusBarStyleBatteryIconHook__installHook

- PROOF_ID: `PROOF_OG_SystemUIBatteryHooks_kt_StatusBarStyleBatteryIconHook__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A13_SYMBOL: `StatusBarStyleBatteryIconHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbar_batterystyle_bold,system_statusbar_batterystyle_fontsize,system_statusbar_batterystyle_leftmargin,system_statusbar_batterystyle_mark_fontsize,system_statusbar_batterystyle_mark_verticaloffset,system_statusbar_batterystyle_rightmargin,system_statusbar_batterystyle_verticaloffset`
- VALUE_DOMAIN: owner-group keys for StatusBarStyleBatteryIconHook: system_statusbar_batterystyle_bold,system_statusbar_batterystyle_fontsize,system_statusbar_batterystyle_leftmargin,system_statusbar_batterystyle_mark_fontsize,system_statusbar_batterystyle_mark_verticaloffset,system_statusbar_batterystyle_rightmargin,system_statusbar_batterystyle_verticaloffset
- DEFAULT_SEMANTICS: `system_statusbar_batterystyle_bold` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `after`. Owner-group review of `SystemUiFeatures.kt::installHook` / `SystemUIBatteryHooks.kt::StatusBarStyleBatteryIconHook`: A13 already implements the exclusive keys system_statusbar_batterystyle_bold,system_statusbar_batterystyle_fontsize,system_statusbar_batterystyle_leftmargin,system_statusbar_batterystyle_mark_fontsize,system_statusbar_batterystyle_mark_verticaloffset,system_statusbar_batterystyle_rightmargin,system_statusbar_batterystyle_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbar_batterystyle'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUiFeatures.kt::installHook` (spec, phases=unknown) vs A13 `SystemUIBatteryHooks.kt::StatusBarStyleBatteryIconHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=['com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_batterystyle_bold` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.systemui.statusbar.views.MiuiBatteryMeterView#updateAll; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_statusbar_batterystyle']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUiFeatures.kt::installHook` / `SystemUIBatteryHooks.kt::StatusBarStyleBatteryIconHook`: A13 already implements the exclusive keys system_statusbar_batterystyle_bold,system_statusbar_batterystyle_fontsize,system_statusbar_batterystyle_leftmargin,system_statusbar_batterystyle_mark_fontsize,system_statusbar_batterystyle_mark_verticaloffset,system_statusbar_batterystyle_rightmargin,system_statusbar_batterystyle_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbar_batterystyle'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_MonitorDeviceInfoHook__DualRowsStatusbarHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_MonitorDeviceInfoHook__DualRowsStatusbarHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `DualRowsStatusbarHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `MonitorDeviceInfoHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarIconController\$IconManager#addHolder,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea,#getSlot,com.android.systemui.statusbar.policy.NetworkSpeedController#<init>`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbar_batterytempandcurrent_align,system_statusbar_batterytempandcurrent_atright,system_statusbar_batterytempandcurrent_bold,system_statusbar_batterytempandcurrent_fixedcontent_width,system_statusbar_batterytempandcurrent_fontsize,system_statusbar_batterytempandcurrent_leftmargin,system_statusbar_batterytempandcurrent_rightmargin,system_statusbar_batterytempandcurrent_verticaloffset,system_statusbar_showdevicetemperature_align,system_statusbar_showdevicetemperature_atright,system_statusbar_showdevicetemperature_bold,system_statusbar_showdevicetemperature_fixedcontent_width,system_statusbar_showdevicetemperature_fontsize,system_statusbar_showdevicetemperature_leftmargin,system_statusbar_showdevicetemperature_rightmargin,system_statusbar_showdevicetemperature_verticaloffset`
- VALUE_DOMAIN: owner-group keys for MonitorDeviceInfoHook: system_statusbar_batterytempandcurrent_align,system_statusbar_batterytempandcurrent_atright,system_statusbar_batterytempandcurrent_bold,system_statusbar_batterytempandcurrent_fixedcontent_width,system_statusbar_batterytempandcurrent_fontsize,system_statusbar_batterytempandcurrent_leftmargin,system_statusbar_batterytempandcurrent_rightmargin,system_statusbar_batterytempandcurrent_verticaloffset
- DEFAULT_SEMANTICS: `system_statusbar_batterytempandcurrent_align` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[iconView,getSlotNameByType(ti?.iconType ?: 0]
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `after` vs A13 phase `after,before`. Owner-group review of `SystemUIStatusBarHooks.kt::DualRowsStatusbarHook` / `SystemUIStatusBarHooks.kt::MonitorDeviceInfoHook`: A13 already implements the exclusive keys system_statusbar_batterytempandcurrent_align,system_statusbar_batterytempandcurrent_atright,system_statusbar_batterytempandcurrent_bold,system_statusbar_batterytempandcurrent_fixedcontent_width,system_statusbar_batterytempandcurrent_fontsize,system_statusbar_batterytempandcurrent_leftmargin,system_statusbar_batterytempandcurrent_rightmargin,system_statusbar_batterytempandcurrent_verticaloffset,system_statusbar_showdevicetemperature_align,system_statusbar_showdevicetemperature_atright,system_statusbar_showdevicetemperature_bold,system_statusbar_showdevicetemperature_fixedcontent_width,system_statusbar_showdevicetemperature_fontsize,system_statusbar_showdevicetemperature_leftmargin,system_statusbar_showdevicetemperature_rightmargin,system_statusbar_showdevicetemperature_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_icons', 'system_statusbar_dualrows_clock_span2rows', 'system_statusbar_dualrows_firstrow_horizmargin', 'system_statusbar_dualrows_firstrow_horizmargin_left', 'system_statusbar_dualrows_firstrow_horizmargin_right', 'system_statusbar_dualrows_left_ratio', 'system_statusbar_netspeed_atsecondrow'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::DualRowsStatusbarHook` (hook, phases=after) vs A13 `SystemUIStatusBarHooks.kt::MonitorDeviceInfoHook` (hook, phases=after,before). Shared methods=none; A14-only members=['com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate', 'com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation']; A13-only members=['#getSlot', 'com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea', 'com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated', 'com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea', 'com.android.systemui.statusbar.phone.StatusBarIconController\\$IconManager#addHolder', 'com.android.systemui.statusbar.policy.NetworkSpeedController#<init>'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_batterytempandcurrent_align` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation; A13=com.android.systemui.statusbar.phone.StatusBarIconController\$IconManager#addHolder,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#initMiuiViewsOnViewCreated,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea,#getSlot,com.android.systemui.statusbar.policy.NetworkSpeedController#<init>; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[iconView,getSlotNameByType(ti?.iconType ?: 0]
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_icons', 'system_statusbar_dualrows_clock_span2rows', 'system_statusbar_dualrows_firstrow_horizmargin', 'system_statusbar_dualrows_firstrow_horizmargin_left', 'system_statusbar_dualrows_firstrow_horizmargin_right', 'system_statusbar_dualrows_left_ratio', 'system_statusbar_netspeed_atsecondrow']; A14-only hook members=['com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate', 'com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::DualRowsStatusbarHook` / `SystemUIStatusBarHooks.kt::MonitorDeviceInfoHook`: A13 already implements the exclusive keys system_statusbar_batterytempandcurrent_align,system_statusbar_batterytempandcurrent_atright,system_statusbar_batterytempandcurrent_bold,system_statusbar_batterytempandcurrent_fixedcontent_width,system_statusbar_batterytempandcurrent_fontsize,system_statusbar_batterytempandcurrent_leftmargin,system_statusbar_batterytempandcurrent_rightmargin,system_statusbar_batterytempandcurrent_verticaloffset,system_statusbar_showdevicetemperature_align,system_statusbar_showdevicetemperature_atright,system_statusbar_showdevicetemperature_bold,system_statusbar_showdevicetemperature_fixedcontent_width,system_statusbar_showdevicetemperature_fontsize,system_statusbar_showdevicetemperature_leftmargin,system_statusbar_showdevicetemperature_rightmargin,system_statusbar_showdevicetemperature_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_icons', 'system_statusbar_dualrows_clock_span2rows', 'system_statusbar_dualrows_firstrow_horizmargin', 'system_statusbar_dualrows_firstrow_horizmargin_left', 'system_statusbar_dualrows_firstrow_horizmargin_right', 'system_statusbar_dualrows_left_ratio', 'system_statusbar_netspeed_atsecondrow'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_DeviceInfoMonitor_kt_readSnapshot__buildConfig

- PROOF_ID: `PROOF_OG_DeviceInfoMonitor_kt_readSnapshot__buildConfig`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt`
- A14_SYMBOL: `buildConfig`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt`
- A13_SYMBOL: `readSnapshot`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbar_batterytempandcurrent_content,system_statusbar_batterytempandcurrent_fixcurrentratio,system_statusbar_batterytempandcurrent_hideunit,system_statusbar_batterytempandcurrent_incharge,system_statusbar_batterytempandcurrent_positive,system_statusbar_batterytempandcurrent_reverseorder,system_statusbar_batterytempandcurrent_singlerow,system_statusbar_batterytempandcurrent_temp_decimal,system_statusbar_showdevicetemperature_content,system_statusbar_showdevicetemperature_hideunit,system_statusbar_showdevicetemperature_reverseorder,system_statusbar_showdevicetemperature_singlerow`
- VALUE_DOMAIN: owner-group keys for readSnapshot: system_statusbar_batterytempandcurrent_content,system_statusbar_batterytempandcurrent_fixcurrentratio,system_statusbar_batterytempandcurrent_hideunit,system_statusbar_batterytempandcurrent_incharge,system_statusbar_batterytempandcurrent_positive,system_statusbar_batterytempandcurrent_reverseorder,system_statusbar_batterytempandcurrent_singlerow,system_statusbar_batterytempandcurrent_temp_decimal
- DEFAULT_SEMANTICS: `system_statusbar_batterytempandcurrent_content` A14 default=1; A13 default=1
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `DeviceInfoMonitor.kt::buildConfig` / `DeviceInfoMonitor.kt::readSnapshot`: A13 already implements the exclusive keys system_statusbar_batterytempandcurrent_content,system_statusbar_batterytempandcurrent_fixcurrentratio,system_statusbar_batterytempandcurrent_hideunit,system_statusbar_batterytempandcurrent_incharge,system_statusbar_batterytempandcurrent_positive,system_statusbar_batterytempandcurrent_reverseorder,system_statusbar_batterytempandcurrent_singlerow,system_statusbar_batterytempandcurrent_temp_decimal,system_statusbar_showdevicetemperature_content,system_statusbar_showdevicetemperature_hideunit,system_statusbar_showdevicetemperature_reverseorder,system_statusbar_showdevicetemperature_singlerow. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbar_batterytempandcurrent', 'system_statusbar_batterytempandcurrent_atright', 'system_statusbar_dualrows', 'system_statusbar_showdevicetemperature', 'system_statusbar_showdevicetemperature_atright'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `DeviceInfoMonitor.kt::buildConfig` (hook, phases=unknown) vs A13 `DeviceInfoMonitor.kt::readSnapshot` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_batterytempandcurrent_content` A14=1 A13=1. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_statusbar_batterytempandcurrent', 'system_statusbar_batterytempandcurrent_atright', 'system_statusbar_dualrows', 'system_statusbar_showdevicetemperature', 'system_statusbar_showdevicetemperature_atright']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `DeviceInfoMonitor.kt::buildConfig` / `DeviceInfoMonitor.kt::readSnapshot`: A13 already implements the exclusive keys system_statusbar_batterytempandcurrent_content,system_statusbar_batterytempandcurrent_fixcurrentratio,system_statusbar_batterytempandcurrent_hideunit,system_statusbar_batterytempandcurrent_incharge,system_statusbar_batterytempandcurrent_positive,system_statusbar_batterytempandcurrent_reverseorder,system_statusbar_batterytempandcurrent_singlerow,system_statusbar_batterytempandcurrent_temp_decimal,system_statusbar_showdevicetemperature_content,system_statusbar_showdevicetemperature_hideunit,system_statusbar_showdevicetemperature_reverseorder,system_statusbar_showdevicetemperature_singlerow. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbar_batterytempandcurrent', 'system_statusbar_batterytempandcurrent_atright', 'system_statusbar_dualrows', 'system_statusbar_showdevicetemperature', 'system_statusbar_showdevicetemperature_atright'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_XML_system_statusbar_clock

- PROOF_ID: `PROOF_OG_XML_system_statusbar_clock`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/res/xml/prefs_system_statusbar_clock.xml`
- A14_SYMBOL: `ListPreferenceEx`
- A14_INSTALLER: `Settings module UI`
- A14_HOOK_TARGETS: `(xml/snapshot family; no exclusive hook symbol)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/res/xml/prefs_system_statusbar_clock.xml`
- A13_SYMBOL: `ListPreferenceEx`
- A13_INSTALLER: `Settings module UI`
- A13_HOOK_TARGETS: `(xml/snapshot family; consumed by A13 style/hook path)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `system_statusbar_clock_align,system_statusbar_clock_bold,system_statusbar_clock_fixedcontent_width`
- VALUE_DOMAIN: XML family prefs_system_statusbar_clock.xml
- DEFAULT_SEMANTICS: Both trees persist `system_statusbar_clock_align` and sibling style keys in prefs_system_statusbar_clock.xml
- RESULT/ARGUMENT_BEHAVIOR: No opposite host setResult on this XML family; values are PrefMap style fields.
- API33_VARIANT_REASON: Owner-group review of XML family `prefs_system_statusbar_clock.xml`: both trees persist 3 user-visible keys including `system_statusbar_clock_align`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- DIFF_SUMMARY: A14 `ListPreferenceEx` vs A13 `ListPreferenceEx` in `prefs_system_statusbar_clock.xml`. Literal-key scanner miss on snapshot fields is not a missing capability.
- VALUE_DEFAULT_COMPARISON: Same preference keys in `prefs_system_statusbar_clock.xml` on both trees.
- HOOK_TARGET_COMPARISON: No exclusive SystemUI member on the XML row; consumption is the family style hook.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the XML widget; click/persist is settings-owned.
- ARG_RESULT_COMPARISON: No host setResult for the XML row itself.
- A14_ONLY_BRANCHES: none on these exclusive XML-family keys
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of XML family `prefs_system_statusbar_clock.xml`: both trees persist 3 user-visible keys including `system_statusbar_clock_align`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_initClockStyle__buildClockStyleSnapshot

- PROOF_ID: `PROOF_OG_SystemStatusBarClockAndMoreHooks_kt_initClockStyle__buildClockStyleSnapshot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `buildClockStyleSnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_SYMBOL: `initClockStyle`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbar_clock_chip,system_statusbar_clock_chip_customtextcolor,system_statusbar_clock_chip_endcolor,system_statusbar_clock_chip_horizpadding,system_statusbar_clock_chip_orientation_vertical,system_statusbar_clock_chip_radius,system_statusbar_clock_chip_startcolor,system_statusbar_clock_chip_textcolor,system_statusbar_clock_chip_usemonet,system_statusbar_clock_chip_verticalpadding,system_statusbar_clock_fontsize,system_statusbar_clock_leftmargin,system_statusbar_clock_rightmargin,system_statusbar_clock_verticaloffset`
- VALUE_DOMAIN: owner-group keys for initClockStyle: system_statusbar_clock_chip,system_statusbar_clock_chip_customtextcolor,system_statusbar_clock_chip_endcolor,system_statusbar_clock_chip_horizpadding,system_statusbar_clock_chip_orientation_vertical,system_statusbar_clock_chip_radius,system_statusbar_clock_chip_startcolor,system_statusbar_clock_chip_textcolor
- DEFAULT_SEMANTICS: `system_statusbar_clock_chip` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemClockHooks.kt::buildClockStyleSnapshot` / `SystemStatusBarClockAndMoreHooks.kt::initClockStyle`: A13 already implements the exclusive keys system_statusbar_clock_chip,system_statusbar_clock_chip_customtextcolor,system_statusbar_clock_chip_endcolor,system_statusbar_clock_chip_horizpadding,system_statusbar_clock_chip_orientation_vertical,system_statusbar_clock_chip_radius,system_statusbar_clock_chip_startcolor,system_statusbar_clock_chip_textcolor,system_statusbar_clock_chip_usemonet,system_statusbar_clock_chip_verticalpadding,system_statusbar_clock_fontsize,system_statusbar_clock_leftmargin,system_statusbar_clock_rightmargin,system_statusbar_clock_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_clock_customformat', 'system_cc_clock_customformat_enable', 'system_cc_dateformat', 'system_drawer_dateformat', 'system_statusbar_clock_24hour_format', 'system_statusbar_clock_align', 'system_statusbar_clock_bold', 'system_statusbar_clock_customformat', 'system_statusbar_clock_customformat_enable', 'system_statusbar_clock_fixedcontent_width', 'system_statusbar_clock_leadingzero', 'system_statusbar_clock_show_ampm', 'system_statusbar_clock_show_seconds', 'system_statusbar_enable_weather_param'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemClockHooks.kt::buildClockStyleSnapshot` (hook, phases=unknown) vs A13 `SystemStatusBarClockAndMoreHooks.kt::initClockStyle` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_clock_chip` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_cc_clock_customformat', 'system_cc_clock_customformat_enable', 'system_cc_dateformat', 'system_drawer_dateformat', 'system_statusbar_clock_24hour_format', 'system_statusbar_clock_align', 'system_statusbar_clock_bold', 'system_statusbar_clock_customformat', 'system_statusbar_clock_customformat_enable', 'system_statusbar_clock_fixedcontent_width', 'system_statusbar_clock_leadingzero', 'system_statusbar_clock_show_ampm', 'system_statusbar_clock_show_seconds', 'system_statusbar_enable_weather_param']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemClockHooks.kt::buildClockStyleSnapshot` / `SystemStatusBarClockAndMoreHooks.kt::initClockStyle`: A13 already implements the exclusive keys system_statusbar_clock_chip,system_statusbar_clock_chip_customtextcolor,system_statusbar_clock_chip_endcolor,system_statusbar_clock_chip_horizpadding,system_statusbar_clock_chip_orientation_vertical,system_statusbar_clock_chip_radius,system_statusbar_clock_chip_startcolor,system_statusbar_clock_chip_textcolor,system_statusbar_clock_chip_usemonet,system_statusbar_clock_chip_verticalpadding,system_statusbar_clock_fontsize,system_statusbar_clock_leftmargin,system_statusbar_clock_rightmargin,system_statusbar_clock_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_cc_clock_customformat', 'system_cc_clock_customformat_enable', 'system_cc_dateformat', 'system_drawer_dateformat', 'system_statusbar_clock_24hour_format', 'system_statusbar_clock_align', 'system_statusbar_clock_bold', 'system_statusbar_clock_customformat', 'system_statusbar_clock_customformat_enable', 'system_statusbar_clock_fixedcontent_width', 'system_statusbar_clock_leadingzero', 'system_statusbar_clock_show_ampm', 'system_statusbar_clock_show_seconds', 'system_statusbar_enable_weather_param'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_DualRowStatusbarHook__DualRowsStatusbarHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_DualRowStatusbarHook__DualRowsStatusbarHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `DualRowsStatusbarHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DualRowStatusbarHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbar_dualrows_clock_span2rows,system_statusbar_dualrows_firstrow_horizmargin,system_statusbar_dualrows_firstrow_horizmargin_left,system_statusbar_dualrows_firstrow_horizmargin_right`
- VALUE_DOMAIN: owner-group keys for DualRowStatusbarHook: system_statusbar_dualrows_clock_span2rows,system_statusbar_dualrows_firstrow_horizmargin,system_statusbar_dualrows_firstrow_horizmargin_left,system_statusbar_dualrows_firstrow_horizmargin_right
- DEFAULT_SEMANTICS: `system_statusbar_dualrows_clock_span2rows` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after` vs A13 phase `after`. Owner-group review of `SystemUIStatusBarHooks.kt::DualRowsStatusbarHook` / `SystemUIStatusBarHooks.kt::DualRowStatusbarHook`: A13 already implements the exclusive keys system_statusbar_dualrows_clock_span2rows,system_statusbar_dualrows_firstrow_horizmargin,system_statusbar_dualrows_firstrow_horizmargin_left,system_statusbar_dualrows_firstrow_horizmargin_right. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::DualRowsStatusbarHook` (hook, phases=after) vs A13 `SystemUIStatusBarHooks.kt::DualRowStatusbarHook` (hook, phases=after). Shared methods=['onFinishInflate', 'updateCutoutLocation']; A14-only members=none; A13-only members=['com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea', 'com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_dualrows_clock_span2rows` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation; A13=com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateCutoutLocation,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#showSystemIconArea,com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#hideSystemIconArea; shared_methods=['onFinishInflate', 'updateCutoutLocation']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::DualRowsStatusbarHook` / `SystemUIStatusBarHooks.kt::DualRowStatusbarHook`: A13 already implements the exclusive keys system_statusbar_dualrows_clock_span2rows,system_statusbar_dualrows_firstrow_horizmargin,system_statusbar_dualrows_firstrow_horizmargin_left,system_statusbar_dualrows_firstrow_horizmargin_right. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

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

## PROOF_OG_SystemUIStatusBarHooks_kt_DualRowSignalHook__DualRowSignalHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_DualRowSignalHook__DualRowSignalHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `DualRowSignalHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setMobileIcons,com.android.systemui.statusbar.StatusBarMobileView#applyMobileState,com.android.systemui.statusbar.StatusBarMobileView#updateState,com.android.systemui.statusbar.StatusBarMobileView#applyDarknessInternal,com.android.systemui.statusbar.StatusBarMobileView#onDarkChanged,com.android.systemui.statusbar.StatusBarMobileView#setDripEnd`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `DualRowSignalHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.$ControllerImplName#setMobileIcons,com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState,com.android.systemui.statusbar.StatusBarMobileView#applyDarknessInternal,com.android.systemui.statusbar.StatusBarMobileView#init`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbar_dualsimin2rows_leftmargin,system_statusbar_dualsimin2rows_rightmargin,system_statusbar_dualsimin2rows_scale,system_statusbar_dualsimin2rows_style,system_statusbar_dualsimin2rows_verticaloffset,system_statusbar_mobiletype_single`
- VALUE_DOMAIN: owner-group keys for DualRowSignalHook: system_statusbar_dualsimin2rows_leftmargin,system_statusbar_dualsimin2rows_rightmargin,system_statusbar_dualsimin2rows_scale,system_statusbar_dualsimin2rows_style,system_statusbar_dualsimin2rows_verticaloffset,system_statusbar_mobiletype_single
- DEFAULT_SEMANTICS: `system_statusbar_dualsimin2rows_leftmargin` A14 default=0; A13 default=0
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after,before` vs A13 phase `after,before`. Owner-group review of `SystemUIStatusBarHooks.kt::DualRowSignalHook` / `SystemUIStatusBarHooks.kt::DualRowSignalHook`: A13 already implements the exclusive keys system_statusbar_dualsimin2rows_leftmargin,system_statusbar_dualsimin2rows_rightmargin,system_statusbar_dualsimin2rows_scale,system_statusbar_dualsimin2rows_style,system_statusbar_dualsimin2rows_verticaloffset,system_statusbar_mobiletype_single. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::DualRowSignalHook` (hook, phases=after,before) vs A13 `SystemUIStatusBarHooks.kt::DualRowSignalHook` (hook, phases=after,before). Shared methods=['applyDarknessInternal', 'onCreate', 'setMobileIcons', 'updateState']; A14-only members=['com.android.systemui.statusbar.StatusBarMobileView#applyMobileState', 'com.android.systemui.statusbar.StatusBarMobileView#onDarkChanged', 'com.android.systemui.statusbar.StatusBarMobileView#setDripEnd', 'com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setMobileIcons']; A13-only members=['com.android.systemui.statusbar.StatusBarMobileView#init', 'com.android.systemui.statusbar.StatusBarMobileView#initViewState', 'com.android.systemui.statusbar.phone.$ControllerImplName#setMobileIcons'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_dualsimin2rows_leftmargin` A14=0 A13=0. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setMobileIcons,com.android.systemui.statusbar.StatusBarMobileView#applyMobileState,com.android.systemui.statusbar.StatusBarMobileView#updateState,com.android.systemui.statusbar.StatusBarMobileView#applyDarknessInternal,com.android.systemui.statusbar.StatusBarMobileView#onDarkChanged,com.android.systemui.statusbar.StatusBarMobileView#setDripEnd; A13=com.android.systemui.SystemUIApplication#onCreate,com.android.systemui.statusbar.phone.$ControllerImplName#setMobileIcons,com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState,com.android.systemui.statusbar.StatusBarMobileView#applyDarknessInternal,com.android.systemui.statusbar.StatusBarMobileView#init; shared_methods=['applyDarknessInternal', 'onCreate', 'setMobileIcons', 'updateState']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after,before; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.statusbar.StatusBarMobileView#applyMobileState', 'com.android.systemui.statusbar.StatusBarMobileView#onDarkChanged', 'com.android.systemui.statusbar.StatusBarMobileView#setDripEnd', 'com.android.systemui.statusbar.phone.StatusBarIconControllerImpl#setMobileIcons']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::DualRowSignalHook` / `SystemUIStatusBarHooks.kt::DualRowSignalHook`: A13 already implements the exclusive keys system_statusbar_dualsimin2rows_leftmargin,system_statusbar_dualsimin2rows_rightmargin,system_statusbar_dualsimin2rows_scale,system_statusbar_dualsimin2rows_style,system_statusbar_dualsimin2rows_verticaloffset,system_statusbar_mobiletype_single. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_HideIconsVoWiFiHook__HideIconsVoWiFiHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_HideIconsVoWiFiHook__HideIconsVoWiFiHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HideIconsVoWiFiHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig#<init>`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideIconsVoWiFiHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig#getHideVowifi`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbar_horizmargin_left,system_statusbar_horizmargin_right,system_statusbar_mobiletype_single_bold,system_statusbar_mobiletype_single_fontsize,system_statusbaricons_roaming,system_statusbaricons_signal,system_statusbaricons_sim1,system_statusbaricons_sim2,system_statusbaricons_sim_nodata,system_statusbaricons_vowifi`
- VALUE_DOMAIN: owner-group keys for HideIconsVoWiFiHook: system_statusbar_horizmargin_left,system_statusbar_horizmargin_right,system_statusbar_mobiletype_single_bold,system_statusbar_mobiletype_single_fontsize,system_statusbaricons_roaming,system_statusbaricons_signal,system_statusbaricons_sim1,system_statusbaricons_sim2
- DEFAULT_SEMANTICS: `system_statusbar_horizmargin_left` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before` vs A13 phase `unknown`. Owner-group review of `SystemUIStatusBarHooks.kt::HideIconsVoWiFiHook` / `SystemUIStatusBarHooks.kt::HideIconsVoWiFiHook`: A13 already implements the exclusive keys system_statusbar_horizmargin_left,system_statusbar_horizmargin_right,system_statusbar_mobiletype_single_bold,system_statusbar_mobiletype_single_fontsize,system_statusbaricons_roaming,system_statusbaricons_signal,system_statusbaricons_sim1,system_statusbaricons_sim2,system_statusbaricons_sim_nodata,system_statusbaricons_vowifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::HideIconsVoWiFiHook` (hook, phases=before) vs A13 `SystemUIStatusBarHooks.kt::HideIconsVoWiFiHook` (hook, phases=unknown). Shared methods=none; A14-only members=['com.android.systemui.MiuiOperatorCustomizedPolicy\\$MiuiOperatorConfig#<init>']; A13-only members=['com.android.systemui.MiuiOperatorCustomizedPolicy\\$MiuiOperatorConfig#getHideVowifi'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_horizmargin_left` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig#<init>; A13=com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig#getHideVowifi; shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=['com.android.systemui.MiuiOperatorCustomizedPolicy\\$MiuiOperatorConfig#<init>']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::HideIconsVoWiFiHook` / `SystemUIStatusBarHooks.kt::HideIconsVoWiFiHook`: A13 already implements the exclusive keys system_statusbar_horizmargin_left,system_statusbar_horizmargin_right,system_statusbar_mobiletype_single_bold,system_statusbar_mobiletype_single_fontsize,system_statusbaricons_roaming,system_statusbaricons_signal,system_statusbaricons_sim1,system_statusbaricons_sim2,system_statusbaricons_sim_nodata,system_statusbaricons_vowifi. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_HorizMarginHook__HorizMarginHook

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_HorizMarginHook__HorizMarginHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `HorizMarginHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider#getStatusBarContentInsetsForCurrentRotation`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HorizMarginHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider#getStatusBarContentInsetsForCurrentRotation`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbar_mobiletype_single_atleft,system_statusbar_mobiletype_single_leftmargin,system_statusbar_mobiletype_single_rightmargin,system_statusbar_mobiletype_single_verticaloffset`
- VALUE_DOMAIN: owner-group keys for HorizMarginHook: system_statusbar_mobiletype_single_atleft,system_statusbar_mobiletype_single_leftmargin,system_statusbar_mobiletype_single_rightmargin,system_statusbar_mobiletype_single_verticaloffset
- DEFAULT_SEMANTICS: `system_statusbar_mobiletype_single_atleft` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 returnAndSkip[Pair(leftMarginPx, rightMarginPx]; A13 returnAndSkip[android.util.Pair(marginLeft.toInt(]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before` vs A13 phase `before`. Owner-group review of `SystemUIStatusBarHooks.kt::HorizMarginHook` / `SystemUIStatusBarHooks.kt::HorizMarginHook`: A13 already implements the exclusive keys system_statusbar_mobiletype_single_atleft,system_statusbar_mobiletype_single_leftmargin,system_statusbar_mobiletype_single_rightmargin,system_statusbar_mobiletype_single_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::HorizMarginHook` (hook, phases=before) vs A13 `SystemUIStatusBarHooks.kt::HorizMarginHook` (hook, phases=before). Shared methods=['getStatusBarContentInsetsForCurrentRotation']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbar_mobiletype_single_atleft` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider#getStatusBarContentInsetsForCurrentRotation; A13=com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider#getStatusBarContentInsetsForCurrentRotation; shared_methods=['getStatusBarContentInsetsForCurrentRotation']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 returnAndSkip[Pair(leftMarginPx, rightMarginPx]; A13 returnAndSkip[android.util.Pair(marginLeft.toInt(]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::HorizMarginHook` / `SystemUIStatusBarHooks.kt::HorizMarginHook`: A13 already implements the exclusive keys system_statusbar_mobiletype_single_atleft,system_statusbar_mobiletype_single_leftmargin,system_statusbar_mobiletype_single_rightmargin,system_statusbar_mobiletype_single_verticaloffset. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarAndClockHooks_kt_isIgnored__installHook

- PROOF_ID: `PROOF_OG_SystemStatusBarAndClockHooks_kt_isIgnored__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/GenericAppFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/GenericAppFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt`
- A13_SYMBOL: `isIgnored`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbarcolor_apps`
- VALUE_DOMAIN: owner-group keys for isIgnored: system_statusbarcolor_apps
- DEFAULT_SEMANTICS: `system_statusbarcolor_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `GenericAppFeatures.kt::installHook` / `SystemStatusBarAndClockHooks.kt::isIgnored`: A13 already implements the exclusive keys system_statusbarcolor_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbarcolor'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `GenericAppFeatures.kt::installHook` (spec, phases=unknown) vs A13 `SystemStatusBarAndClockHooks.kt::isIgnored` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbarcolor_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_statusbarcolor']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `GenericAppFeatures.kt::installHook` / `SystemStatusBarAndClockHooks.kt::isIgnored`: A13 already implements the exclusive keys system_statusbarcolor_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbarcolor'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_StatusBarGesturesHook__launch

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_StatusBarGesturesHook__launch`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/gesture/StatusBarGestureEffectExecutor.kt`
- A14_SYMBOL: `launch`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/gesture/StatusBarGestureEffectExecutor.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `StatusBarGesturesHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_HOOK_TARGETS: `miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_statusbarcontrols_dt,system_statusbarcontrols_longpress,system_statusbarcontrols_longpress_vibrate,system_statusbarcontrols_longpress_vibrate_ignoreoff`
- VALUE_DOMAIN: owner-group keys for StatusBarGesturesHook: system_statusbarcontrols_dt,system_statusbarcontrols_longpress,system_statusbarcontrols_longpress_vibrate,system_statusbarcontrols_longpress_vibrate_ignoreoff
- DEFAULT_SEMANTICS: `system_statusbarcontrols_dt` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `before,after`. Owner-group review of `StatusBarGestureEffectExecutor.kt::launch` / `SystemUIControlCenterHooks.kt::StatusBarGesturesHook`: A13 already implements the exclusive keys system_statusbarcontrols_dt,system_statusbarcontrols_longpress,system_statusbarcontrols_longpress_vibrate,system_statusbarcontrols_longpress_vibrate_ignoreoff. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbarcontrols_dt_left', 'system_statusbarcontrols_dt_right'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `StatusBarGestureEffectExecutor.kt::launch` (hook, phases=unknown) vs A13 `SystemUIControlCenterHooks.kt::StatusBarGesturesHook` (hook, phases=before,after). Shared methods=none; A14-only members=none; A13-only members=['miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbarcontrols_dt` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_statusbarcontrols_dt_left', 'system_statusbarcontrols_dt_right']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `StatusBarGestureEffectExecutor.kt::launch` / `SystemUIControlCenterHooks.kt::StatusBarGesturesHook`: A13 already implements the exclusive keys system_statusbarcontrols_dt,system_statusbarcontrols_longpress,system_statusbarcontrols_longpress_vibrate,system_statusbarcontrols_longpress_vibrate_ignoreoff. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbarcontrols_dt_left', 'system_statusbarcontrols_dt_right'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIControlCenterHooks_kt_StatusBarGesturesHook__installHook

- PROOF_ID: `PROOF_OG_SystemUIControlCenterHooks_kt_StatusBarGesturesHook__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `StatusBarGesturesHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_HOOK_TARGETS: `miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_statusbarcontrols_dual,system_statusbarcontrols_sens_bright,system_statusbarcontrols_sens_vol,system_statusbarcontrols_single`
- VALUE_DOMAIN: owner-group keys for StatusBarGesturesHook: system_statusbarcontrols_dual,system_statusbarcontrols_sens_bright,system_statusbarcontrols_sens_vol,system_statusbarcontrols_single
- DEFAULT_SEMANTICS: `system_statusbarcontrols_dual` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `before,after`. Owner-group review of `SystemUiFeatures.kt::installHook` / `SystemUIControlCenterHooks.kt::StatusBarGesturesHook`: A13 already implements the exclusive keys system_statusbarcontrols_dual,system_statusbarcontrols_sens_bright,system_statusbarcontrols_sens_vol,system_statusbarcontrols_single. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbarcontrols'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUiFeatures.kt::installHook` (spec, phases=unknown) vs A13 `SystemUIControlCenterHooks.kt::StatusBarGesturesHook` (hook, phases=before,after). Shared methods=none; A14-only members=none; A13-only members=['miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbarcontrols_dual` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl#handleMotionEvent; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_statusbarcontrols']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUiFeatures.kt::installHook` / `SystemUIControlCenterHooks.kt::StatusBarGesturesHook`: A13 already implements the exclusive keys system_statusbarcontrols_dual,system_statusbarcontrols_sens_bright,system_statusbarcontrols_sens_vol,system_statusbarcontrols_single. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbarcontrols'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemStatusBarAndClockHooks_kt_StatusBarHeightRes__installHook

- PROOF_ID: `PROOF_OG_SystemStatusBarAndClockHooks_kt_StatusBarHeightRes__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt`
- A13_SYMBOL: `StatusBarHeightRes`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbarheight`
- VALUE_DOMAIN: owner-group keys for StatusBarHeightRes: system_statusbarheight
- DEFAULT_SEMANTICS: `system_statusbarheight` A14 default=n/a; A13 default=19
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemServerFeatures.kt::installHook` / `SystemStatusBarAndClockHooks.kt::StatusBarHeightRes`: A13 already implements the exclusive keys system_statusbarheight. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['controls_backlong_action', 'controls_fingerprintfailure', 'controls_fingerprintscreen', 'controls_fingerprintsuccess', 'controls_fingerprintwake', 'controls_powerdt_action', 'controls_powerflash', 'controls_volumemedia_up', 'system_allrotations2', 'system_apksign', 'system_applock', 'system_applock_skip', 'system_applock_timeout', 'system_autobrightness', 'system_autobrightness_reset_when_screenoff', 'system_blocktoasts', 'system_cleanopenwith', 'system_cleanshare', 'system_clearalltasks', 'system_dimtime', 'system_disable_window_blurs', 'system_disableanynotif', 'system_disableintegrity', 'system_downgrade', 'system_firstpress', 'system_force_darken_allapps', 'system_forceclose', 'system_fw_noblacklist', 'system_fw_splitscreen', 'system_hideproxywarn', 'system_ignorecalls', 'system_lockscreen_disable_strongauth_72h', 'system_lswallpaper', 'system_noducking', 'system_nolightuponcharges', 'system_notify_openinfw', 'system_orientationlock', 'system_other_wallpaper_scale', 'system_remove_startactconfirm', 'system_removesecure', 'system_screenanim_duration', 'system_screenshot_overlay', 'system_securelock', 'system_toasttime', 'system_usb_default_function', 'system_vibration', 'system_vibration_amp', 'various_alarmcompat', 'various_allow_untrusted_touch', 'various_disable_access_devicelogs', 'various_disableapp'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemServerFeatures.kt::installHook` (spec, phases=unknown) vs A13 `SystemStatusBarAndClockHooks.kt::StatusBarHeightRes` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbarheight` A14=n/a A13=19. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['controls_backlong_action', 'controls_fingerprintfailure', 'controls_fingerprintscreen', 'controls_fingerprintsuccess', 'controls_fingerprintwake', 'controls_powerdt_action', 'controls_powerflash', 'controls_volumemedia_up', 'system_allrotations2', 'system_apksign', 'system_applock', 'system_applock_skip', 'system_applock_timeout', 'system_autobrightness', 'system_autobrightness_reset_when_screenoff', 'system_blocktoasts', 'system_cleanopenwith', 'system_cleanshare', 'system_clearalltasks', 'system_dimtime', 'system_disable_window_blurs', 'system_disableanynotif', 'system_disableintegrity', 'system_downgrade', 'system_firstpress', 'system_force_darken_allapps', 'system_forceclose', 'system_fw_noblacklist', 'system_fw_splitscreen', 'system_hideproxywarn', 'system_ignorecalls', 'system_lockscreen_disable_strongauth_72h', 'system_lswallpaper', 'system_noducking', 'system_nolightuponcharges', 'system_notify_openinfw', 'system_orientationlock', 'system_other_wallpaper_scale', 'system_remove_startactconfirm', 'system_removesecure', 'system_screenanim_duration', 'system_screenshot_overlay', 'system_securelock', 'system_toasttime', 'system_usb_default_function', 'system_vibration', 'system_vibration_amp', 'various_alarmcompat', 'various_allow_untrusted_touch', 'various_disable_access_devicelogs', 'various_disableapp']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemServerFeatures.kt::installHook` / `SystemStatusBarAndClockHooks.kt::StatusBarHeightRes`: A13 already implements the exclusive keys system_statusbarheight. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['controls_backlong_action', 'controls_fingerprintfailure', 'controls_fingerprintscreen', 'controls_fingerprintsuccess', 'controls_fingerprintwake', 'controls_powerdt_action', 'controls_powerflash', 'controls_volumemedia_up', 'system_allrotations2', 'system_apksign', 'system_applock', 'system_applock_skip', 'system_applock_timeout', 'system_autobrightness', 'system_autobrightness_reset_when_screenoff', 'system_blocktoasts', 'system_cleanopenwith', 'system_cleanshare', 'system_clearalltasks', 'system_dimtime', 'system_disable_window_blurs', 'system_disableanynotif', 'system_disableintegrity', 'system_downgrade', 'system_firstpress', 'system_force_darken_allapps', 'system_forceclose', 'system_fw_noblacklist', 'system_fw_splitscreen', 'system_hideproxywarn', 'system_ignorecalls', 'system_lockscreen_disable_strongauth_72h', 'system_lswallpaper', 'system_noducking', 'system_nolightuponcharges', 'system_notify_openinfw', 'system_orientationlock', 'system_other_wallpaper_scale', 'system_remove_startactconfirm', 'system_removesecure', 'system_screenanim_duration', 'system_screenshot_overlay', 'system_securelock', 'system_toasttime', 'system_usb_default_function', 'system_vibration', 'system_vibration_amp', 'various_alarmcompat', 'various_allow_untrusted_touch', 'various_disable_access_devicelogs', 'various_disableapp'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemSettingsAndConnectivityHooks_kt_CollapseCCAfterClickHook__CollapseCCAfterClickHook

- PROOF_ID: `PROOF_OG_SystemSettingsAndConnectivityHooks_kt_CollapseCCAfterClickHook__CollapseCCAfterClickHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `CollapseCCAfterClickHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click`
- A14_CALLBACK_PHASE: `after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemSettingsAndConnectivityHooks.kt`
- A13_SYMBOL: `CollapseCCAfterClickHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.qs.tileimpl.QSTileImpl#click`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbaricons_alarm`
- VALUE_DOMAIN: owner-group keys for CollapseCCAfterClickHook: system_statusbaricons_alarm
- DEFAULT_SEMANTICS: `system_statusbaricons_alarm` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `after` vs A13 phase `after`. Owner-group review of `SystemUIControlCenterHooks.kt::CollapseCCAfterClickHook` / `SystemSettingsAndConnectivityHooks.kt::CollapseCCAfterClickHook`: A13 already implements the exclusive keys system_statusbaricons_alarm. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIControlCenterHooks.kt::CollapseCCAfterClickHook` (hook, phases=after) vs A13 `SystemSettingsAndConnectivityHooks.kt::CollapseCCAfterClickHook` (hook, phases=after). Shared methods=['click']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbaricons_alarm` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.qs.tileimpl.QSTileImpl#click; A13=com.android.systemui.qs.tileimpl.QSTileImpl#click; shared_methods=['click']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIControlCenterHooks.kt::CollapseCCAfterClickHook` / `SystemSettingsAndConnectivityHooks.kt::CollapseCCAfterClickHook`: A13 already implements the exclusive keys system_statusbaricons_alarm. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_SystemUiInstaller_java_install__StatusBarClockTweakHook

- PROOF_ID: `PROOF_OG_SystemUiInstaller_java_install__StatusBarClockTweakHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_SYMBOL: `StatusBarClockTweakHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemClockHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.views.MiuiClock#onDarkChanged,com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_SYMBOL: `install`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.SystemUIApplication#onCreate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_statusbaricons_clock`
- VALUE_DOMAIN: owner-group keys for install: system_statusbaricons_clock
- DEFAULT_SEMANTICS: `system_statusbaricons_clock` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `SystemClockHooks.kt::StatusBarClockTweakHook` / `SystemUiInstaller.java::install`: A13 already implements the exclusive keys system_statusbaricons_clock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_drawer_hidedate', 'system_statusbar_clocktweak', 'system_statusbar_enable_weather_param'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemClockHooks.kt::StatusBarClockTweakHook` (hook, phases=intercept) vs A13 `SystemUiInstaller.java::install` (installer, phases=after). Shared methods=none; A14-only members=['com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow', 'com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState', 'com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>', 'com.android.systemui.statusbar.views.MiuiClock#<init>', 'com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow', 'com.android.systemui.statusbar.views.MiuiClock#onDarkChanged', 'com.android.systemui.statusbar.views.MiuiClock#updateTime', 'com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime']; A13-only members=['com.android.systemui.SystemUIApplication#onCreate'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbaricons_clock` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.views.MiuiClock#updateTime,com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime,com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow,com.android.systemui.statusbar.views.MiuiClock#onDarkChanged,com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState,com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>,com.android.systemui.statusbar.views.MiuiClock#<init>; A13=com.android.systemui.SystemUIApplication#onCreate; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_drawer_hidedate', 'system_statusbar_clocktweak', 'system_statusbar_enable_weather_param']; A14-only hook members=['com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow', 'com.android.systemui.statusbar.policy.FakeStatusBarClockController#initState', 'com.android.systemui.statusbar.policy.MiuiStatusBarClockController#<init>', 'com.android.systemui.statusbar.views.MiuiClock#<init>', 'com.android.systemui.statusbar.views.MiuiClock#onAttachedToWindow', 'com.android.systemui.statusbar.views.MiuiClock#onDarkChanged', 'com.android.systemui.statusbar.views.MiuiClock#updateTime', 'com.android.systemui.statusbar.views.MiuiStatusBarClock#updateTime']. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemClockHooks.kt::StatusBarClockTweakHook` / `SystemUiInstaller.java::install`: A13 already implements the exclusive keys system_statusbaricons_clock. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_drawer_hidedate', 'system_statusbar_clocktweak', 'system_statusbar_enable_weather_param'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUIStatusBarHooks_kt_HideIconsSignalHook__buildStatusBarIconVisibilitySnapshot

- PROOF_ID: `PROOF_OG_SystemUIStatusBarHooks_kt_HideIconsSignalHook__buildStatusBarIconVisibilitySnapshot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildStatusBarIconVisibilitySnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `HideIconsSignalHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbaricons_signal_wificonnected`
- VALUE_DOMAIN: owner-group keys for HideIconsSignalHook: system_statusbaricons_signal_wificonnected
- DEFAULT_SEMANTICS: `system_statusbaricons_signal_wificonnected` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `before`. Owner-group review of `SystemUIStatusBarHooks.kt::buildStatusBarIconVisibilitySnapshot` / `SystemUIStatusBarHooks.kt::HideIconsSignalHook`: A13 already implements the exclusive keys system_statusbaricons_signal_wificonnected. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbaricons_airplane', 'system_statusbaricons_alarm', 'system_statusbaricons_ble_unlock', 'system_statusbaricons_bluetoothicn', 'system_statusbaricons_btbattery', 'system_statusbaricons_dnd', 'system_statusbaricons_gps', 'system_statusbaricons_headset', 'system_statusbaricons_hotspot', 'system_statusbaricons_mute', 'system_statusbaricons_nfc', 'system_statusbaricons_nosims', 'system_statusbaricons_privacy', 'system_statusbaricons_profile', 'system_statusbaricons_record', 'system_statusbaricons_secondspace', 'system_statusbaricons_sound', 'system_statusbaricons_speaker', 'system_statusbaricons_vpn', 'system_statusbaricons_wifi', 'system_statusbaricons_wireless_headset'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUIStatusBarHooks.kt::buildStatusBarIconVisibilitySnapshot` (hook, phases=unknown) vs A13 `SystemUIStatusBarHooks.kt::HideIconsSignalHook` (hook, phases=before). Shared methods=none; A14-only members=none; A13-only members=['com.android.systemui.statusbar.StatusBarMobileView#initViewState', 'com.android.systemui.statusbar.StatusBarMobileView#updateState'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbaricons_signal_wificonnected` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.systemui.statusbar.StatusBarMobileView#initViewState,com.android.systemui.statusbar.StatusBarMobileView#updateState; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_statusbaricons_airplane', 'system_statusbaricons_alarm', 'system_statusbaricons_ble_unlock', 'system_statusbaricons_bluetoothicn', 'system_statusbaricons_btbattery', 'system_statusbaricons_dnd', 'system_statusbaricons_gps', 'system_statusbaricons_headset', 'system_statusbaricons_hotspot', 'system_statusbaricons_mute', 'system_statusbaricons_nfc', 'system_statusbaricons_nosims', 'system_statusbaricons_privacy', 'system_statusbaricons_profile', 'system_statusbaricons_record', 'system_statusbaricons_secondspace', 'system_statusbaricons_sound', 'system_statusbaricons_speaker', 'system_statusbaricons_vpn', 'system_statusbaricons_wifi', 'system_statusbaricons_wireless_headset']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUIStatusBarHooks.kt::buildStatusBarIconVisibilitySnapshot` / `SystemUIStatusBarHooks.kt::HideIconsSignalHook`: A13 already implements the exclusive keys system_statusbaricons_signal_wificonnected. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_statusbaricons_airplane', 'system_statusbaricons_alarm', 'system_statusbaricons_ble_unlock', 'system_statusbaricons_bluetoothicn', 'system_statusbaricons_btbattery', 'system_statusbaricons_dnd', 'system_statusbaricons_gps', 'system_statusbaricons_headset', 'system_statusbaricons_hotspot', 'system_statusbaricons_mute', 'system_statusbaricons_nfc', 'system_statusbaricons_nosims', 'system_statusbaricons_privacy', 'system_statusbaricons_profile', 'system_statusbaricons_record', 'system_statusbaricons_secondspace', 'system_statusbaricons_sound', 'system_statusbaricons_speaker', 'system_statusbaricons_vpn', 'system_statusbaricons_wifi', 'system_statusbaricons_wireless_headset'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_XML_system_statusbar_batterystyle

- PROOF_ID: `PROOF_OG_XML_system_statusbar_batterystyle`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/res/xml/prefs_system_statusbar_batterystyle.xml`
- A14_SYMBOL: `CheckBoxPreferenceEx`
- A14_INSTALLER: `Settings module UI`
- A14_HOOK_TARGETS: `(xml/snapshot family; no exclusive hook symbol)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/res/xml/prefs_system_statusbar_batterystyle.xml`
- A13_SYMBOL: `CheckBoxPreferenceEx`
- A13_INSTALLER: `Settings module UI`
- A13_HOOK_TARGETS: `(xml/snapshot family; consumed by A13 style/hook path)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `system_statusbaricons_swap_batteryicon_percentage`
- VALUE_DOMAIN: XML family prefs_system_statusbar_batterystyle.xml
- DEFAULT_SEMANTICS: Both trees persist `system_statusbaricons_swap_batteryicon_percentage` and sibling style keys in prefs_system_statusbar_batterystyle.xml
- RESULT/ARGUMENT_BEHAVIOR: No opposite host setResult on this XML family; values are PrefMap style fields.
- API33_VARIANT_REASON: Owner-group review of XML family `prefs_system_statusbar_batterystyle.xml`: both trees persist 1 user-visible keys including `system_statusbaricons_swap_batteryicon_percentage`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- DIFF_SUMMARY: A14 `CheckBoxPreferenceEx` vs A13 `CheckBoxPreferenceEx` in `prefs_system_statusbar_batterystyle.xml`. Literal-key scanner miss on snapshot fields is not a missing capability.
- VALUE_DEFAULT_COMPARISON: Same preference keys in `prefs_system_statusbar_batterystyle.xml` on both trees.
- HOOK_TARGET_COMPARISON: No exclusive SystemUI member on the XML row; consumption is the family style hook.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the XML widget; click/persist is settings-owned.
- ARG_RESULT_COMPARISON: No host setResult for the XML row itself.
- A14_ONLY_BRANCHES: none on these exclusive XML-family keys
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of XML family `prefs_system_statusbar_batterystyle.xml`: both trees persist 1 user-visible keys including `system_statusbaricons_swap_batteryicon_percentage`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemUINotificationHooks_kt_ReplaceShortcutAppHook__ReplaceShortcutAppHook

- PROOF_ID: `PROOF_OG_SystemUINotificationHooks_kt_ReplaceShortcutAppHook__ReplaceShortcutAppHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_SYMBOL: `ReplaceShortcutAppHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt`
- A14_HOOK_TARGETS: `com.miui.systemui.util.CommonUtil#startSettingsApp,com.miui.systemui.util.CommonUtil#startCalendarApp,com.miui.systemui.util.CommonUtil#startClockApp`
- A14_CALLBACK_PHASE: `before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt`
- A13_SYMBOL: `ReplaceShortcutAppHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.miui.systemui.util.CommonUtil#startSettingsApp,com.miui.systemui.util.CommonUtil#startCalendarApp,com.miui.systemui.util.CommonUtil#startClockApp`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `system_statusbaricons_volte`
- VALUE_DOMAIN: owner-group keys for ReplaceShortcutAppHook: system_statusbaricons_volte
- DEFAULT_SEMANTICS: `system_statusbaricons_volte` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `before` vs A13 phase `before`. Owner-group review of `SystemUILockScreenHooks.kt::ReplaceShortcutAppHook` / `SystemUINotificationHooks.kt::ReplaceShortcutAppHook`: A13 already implements the exclusive keys system_statusbaricons_volte. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUILockScreenHooks.kt::ReplaceShortcutAppHook` (hook, phases=before) vs A13 `SystemUINotificationHooks.kt::ReplaceShortcutAppHook` (hook, phases=before). Shared methods=['startCalendarApp', 'startClockApp', 'startSettingsApp']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_statusbaricons_volte` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.systemui.util.CommonUtil#startSettingsApp,com.miui.systemui.util.CommonUtil#startCalendarApp,com.miui.systemui.util.CommonUtil#startClockApp; A13=com.miui.systemui.util.CommonUtil#startSettingsApp,com.miui.systemui.util.CommonUtil#startCalendarApp,com.miui.systemui.util.CommonUtil#startClockApp; shared_methods=['startCalendarApp', 'startClockApp', 'startSettingsApp']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=before; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUILockScreenHooks.kt::ReplaceShortcutAppHook` / `SystemUINotificationHooks.kt::ReplaceShortcutAppHook`: A13 already implements the exclusive keys system_statusbaricons_volte. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_MuffledVibrationHook__MuffledVibrationHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_MuffledVibrationHook__MuffledVibrationHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `MuffledVibrationHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.VibratorService#doVibratorOn`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `MuffledVibrationHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.server.VibratorService#doVibratorOn`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_vibration_amp,system_vibration_amp_notif,system_vibration_amp_other,system_vibration_amp_ringer`
- VALUE_DOMAIN: owner-group keys for MuffledVibrationHook: system_vibration_amp,system_vibration_amp_notif,system_vibration_amp_other,system_vibration_amp_ringer
- DEFAULT_SEMANTICS: `system_vibration_amp` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before,after`. Owner-group review of `SystemAudioHooks.kt::MuffledVibrationHook` / `SystemAudioAndVisualAndMoreHooks.kt::MuffledVibrationHook`: A13 already implements the exclusive keys system_vibration_amp,system_vibration_amp_notif,system_vibration_amp_other,system_vibration_amp_ringer. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_vibration_amp_period_end_hour', 'system_vibration_amp_period_end_minute', 'system_vibration_amp_period_start_hour', 'system_vibration_amp_period_start_minute'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemAudioHooks.kt::MuffledVibrationHook` (hook, phases=intercept) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::MuffledVibrationHook` (hook, phases=before,after). Shared methods=['doVibratorOn']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_vibration_amp` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.VibratorService#doVibratorOn; A13=com.android.server.VibratorService#doVibratorOn; shared_methods=['doVibratorOn']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_vibration_amp_period_end_hour', 'system_vibration_amp_period_end_minute', 'system_vibration_amp_period_start_hour', 'system_vibration_amp_period_start_minute']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemAudioHooks.kt::MuffledVibrationHook` / `SystemAudioAndVisualAndMoreHooks.kt::MuffledVibrationHook`: A13 already implements the exclusive keys system_vibration_amp,system_vibration_amp_notif,system_vibration_amp_other,system_vibration_amp_ringer. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_vibration_amp_period_end_hour', 'system_vibration_amp_period_end_minute', 'system_vibration_amp_period_start_hour', 'system_vibration_amp_period_start_minute'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_MuffledVibrationHook__installHook

- PROOF_ID: `PROOF_OG_SystemAudioAndVisualAndMoreHooks_kt_MuffledVibrationHook__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_SYMBOL: `MuffledVibrationHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.server.VibratorService#doVibratorOn`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `system_vibration_amp_period_end,system_vibration_amp_period_start`
- VALUE_DOMAIN: owner-group keys for MuffledVibrationHook: system_vibration_amp_period_end,system_vibration_amp_period_start
- DEFAULT_SEMANTICS: `system_vibration_amp_period_end` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=False; ownership_compat=True; A14 phase `unknown` vs A13 phase `before,after`. Owner-group review of `SystemServerFeatures.kt::installHook` / `SystemAudioAndVisualAndMoreHooks.kt::MuffledVibrationHook`: A13 already implements the exclusive keys system_vibration_amp_period_end,system_vibration_amp_period_start. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_vibration_amp'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemServerFeatures.kt::installHook` (spec, phases=unknown) vs A13 `SystemAudioAndVisualAndMoreHooks.kt::MuffledVibrationHook` (hook, phases=before,after). Shared methods=none; A14-only members=none; A13-only members=['com.android.server.VibratorService#doVibratorOn'].
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_vibration_amp_period_end` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=com.android.server.VibratorService#doVibratorOn; shared_methods=n/a; hook_targets_compatible=False
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=before,after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_vibration_amp']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemServerFeatures.kt::installHook` / `SystemAudioAndVisualAndMoreHooks.kt::MuffledVibrationHook`: A13 already implements the exclusive keys system_vibration_amp_period_end,system_vibration_amp_period_start. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_vibration_amp'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemNotificationMoreHooks_kt_SelectiveVibrationHook__SelectiveVibrationHook

- PROOF_ID: `PROOF_OG_SystemNotificationMoreHooks_kt_SelectiveVibrationHook__SelectiveVibrationHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_SYMBOL: `SelectiveVibrationHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioHooks.kt`
- A14_HOOK_TARGETS: `com.android.server.vibrator.VibratorManagerService#systemReady,com.android.server.vibrator.VibratorManagerService#vibrate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_SYMBOL: `SelectiveVibrationHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt`
- A13_HOOK_TARGETS: `com.android.server.vibrator.VibratorManagerService#systemReady,com.android.server.vibrator.VibratorManagerService#vibrate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_vibration_apps`
- VALUE_DOMAIN: owner-group keys for SelectiveVibrationHook: system_vibration_apps
- DEFAULT_SEMANTICS: `system_vibration_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `SystemAudioHooks.kt::SelectiveVibrationHook` / `SystemNotificationMoreHooks.kt::SelectiveVibrationHook`: A13 already implements the exclusive keys system_vibration_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemAudioHooks.kt::SelectiveVibrationHook` (hook, phases=intercept) vs A13 `SystemNotificationMoreHooks.kt::SelectiveVibrationHook` (hook, phases=after,before). Shared methods=['systemReady', 'vibrate']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_vibration_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.vibrator.VibratorManagerService#systemReady,com.android.server.vibrator.VibratorManagerService#vibrate; A13=com.android.server.vibrator.VibratorManagerService#systemReady,com.android.server.vibrator.VibratorManagerService#vibrate; shared_methods=['systemReady', 'vibrate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemAudioHooks.kt::SelectiveVibrationHook` / `SystemNotificationMoreHooks.kt::SelectiveVibrationHook`: A13 already implements the exclusive keys system_vibration_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_AudioVisualizer_kt_shouldDisplayAudioVisualizer__handlePreferenceChanged

- PROOF_ID: `PROOF_OG_AudioVisualizer_kt_shouldDisplayAudioVisualizer__handlePreferenceChanged`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A14_SYMBOL: `handlePreferenceChanged`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A13_SYMBOL: `shouldDisplayAudioVisualizer`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_visualizer_animdur,system_visualizer_color,system_visualizer_colorval,system_visualizer_controller,system_visualizer_drawer,system_visualizer_dyntime,system_visualizer_glowlevel,system_visualizer_render,system_visualizer_style,system_visualizer_transp`
- VALUE_DOMAIN: owner-group keys for shouldDisplayAudioVisualizer: system_visualizer_animdur,system_visualizer_color,system_visualizer_colorval,system_visualizer_controller,system_visualizer_drawer,system_visualizer_dyntime,system_visualizer_glowlevel,system_visualizer_render
- DEFAULT_SEMANTICS: `system_visualizer_animdur` A14 default=65; A13 default=65
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `AudioVisualizer.kt::handlePreferenceChanged` / `AudioVisualizer.kt::shouldDisplayAudioVisualizer`: A13 already implements the exclusive keys system_visualizer_animdur,system_visualizer_color,system_visualizer_colorval,system_visualizer_controller,system_visualizer_drawer,system_visualizer_dyntime,system_visualizer_glowlevel,system_visualizer_render,system_visualizer_style,system_visualizer_transp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `AudioVisualizer.kt::handlePreferenceChanged` (hook, phases=unknown) vs A13 `AudioVisualizer.kt::shouldDisplayAudioVisualizer` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_visualizer_animdur` A14=65 A13=65. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `AudioVisualizer.kt::handlePreferenceChanged` / `AudioVisualizer.kt::shouldDisplayAudioVisualizer`: A13 already implements the exclusive keys system_visualizer_animdur,system_visualizer_color,system_visualizer_colorval,system_visualizer_controller,system_visualizer_drawer,system_visualizer_dyntime,system_visualizer_glowlevel,system_visualizer_render,system_visualizer_style,system_visualizer_transp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_AudioVisualizer_kt_shouldDisplayAudioVisualizer__installHook

- PROOF_ID: `PROOF_OG_AudioVisualizer_kt_shouldDisplayAudioVisualizer__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemUiFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A13_SYMBOL: `shouldDisplayAudioVisualizer`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/AudioVisualizer.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_visualizer_custom`
- VALUE_DOMAIN: owner-group keys for shouldDisplayAudioVisualizer: system_visualizer_custom
- DEFAULT_SEMANTICS: `system_visualizer_custom` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemUiFeatures.kt::installHook` / `AudioVisualizer.kt::shouldDisplayAudioVisualizer`: A13 already implements the exclusive keys system_visualizer_custom. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_visualizer'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemUiFeatures.kt::installHook` (spec, phases=unknown) vs A13 `AudioVisualizer.kt::shouldDisplayAudioVisualizer` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `system_visualizer_custom` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=['system_visualizer']; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemUiFeatures.kt::installHook` / `AudioVisualizer.kt::shouldDisplayAudioVisualizer`: A13 already implements the exclusive keys system_visualizer_custom. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['system_visualizer'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_AlarmCompatHook__AlarmCompatHook

- PROOF_ID: `PROOF_OG_Various_kt_AlarmCompatHook__AlarmCompatHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AlarmCompatHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AlarmCompatHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_alarmcompat`
- VALUE_DOMAIN: owner-group keys for AlarmCompatHook: various_alarmcompat
- DEFAULT_SEMANTICS: `various_alarmcompat` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Various.kt::AlarmCompatHook` / `Various.kt::AlarmCompatHook`: A13 already implements the exclusive keys various_alarmcompat. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::AlarmCompatHook` (hook, phases=intercept) vs A13 `Various.kt::AlarmCompatHook` (hook, phases=before). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_alarmcompat` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::AlarmCompatHook` / `Various.kt::AlarmCompatHook`: A13 already implements the exclusive keys various_alarmcompat. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_AlarmCompatServiceHook__AlarmCompatServiceHook

- PROOF_ID: `PROOF_OG_Various_kt_AlarmCompatServiceHook__AlarmCompatServiceHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AlarmCompatServiceHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.android.server.alarm.AlarmManagerService#onBootPhase,com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AlarmCompatServiceHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/GenericAppInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.alarm.AlarmManagerService#onBootPhase,com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `various_alarmcompat_apps`
- VALUE_DOMAIN: owner-group keys for AlarmCompatServiceHook: various_alarmcompat_apps
- DEFAULT_SEMANTICS: `various_alarmcompat_apps` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 setResult[if (time == 0L]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Various.kt::AlarmCompatServiceHook` / `Various.kt::AlarmCompatServiceHook`: A13 already implements the exclusive keys various_alarmcompat_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::AlarmCompatServiceHook` (hook, phases=intercept) vs A13 `Various.kt::AlarmCompatServiceHook` (hook, phases=after). Shared methods=['getNextAlarmClockImpl', 'onBootPhase']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_alarmcompat_apps` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.alarm.AlarmManagerService#onBootPhase,com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl; A13=com.android.server.alarm.AlarmManagerService#onBootPhase,com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl; shared_methods=['getNextAlarmClockImpl', 'onBootPhase']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 setResult[if (time == 0L]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::AlarmCompatServiceHook` / `Various.kt::AlarmCompatServiceHook`: A13 already implements the exclusive keys various_alarmcompat_apps. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_onActivityCreated__installHook

- PROOF_ID: `PROOF_OG_Various_kt_onActivityCreated__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `various_allow_untrusted_touch`
- VALUE_DOMAIN: owner-group keys for onActivityCreated: various_allow_untrusted_touch
- DEFAULT_SEMANTICS: `various_allow_untrusted_touch` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemServerFeatures.kt::installHook` / `Various.kt::onActivityCreated`: A13 already implements the exclusive keys various_allow_untrusted_touch. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemServerFeatures.kt::installHook` (spec, phases=unknown) vs A13 `Various.kt::onActivityCreated` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_allow_untrusted_touch` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemServerFeatures.kt::installHook` / `Various.kt::onActivityCreated`: A13 already implements the exclusive keys various_allow_untrusted_touch. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_AnswerCallInHeadUpHook__AnswerCallInHeadUpHook

- PROOF_ID: `PROOF_OG_Various_kt_AnswerCallInHeadUpHook__AnswerCallInHeadUpHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AnswerCallInHeadUpHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.android.incallui.InCallPresenter#answerIncomingCall`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AnswerCallInHeadUpHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PhoneInstaller.java`
- A13_HOOK_TARGETS: `com.android.incallui.InCallPresenter#answerIncomingCall`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_answerinheadup`
- VALUE_DOMAIN: owner-group keys for AnswerCallInHeadUpHook: various_answerinheadup
- DEFAULT_SEMANTICS: `various_answerinheadup` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Various.kt::AnswerCallInHeadUpHook` / `Various.kt::AnswerCallInHeadUpHook`: A13 already implements the exclusive keys various_answerinheadup. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::AnswerCallInHeadUpHook` (hook, phases=intercept) vs A13 `Various.kt::AnswerCallInHeadUpHook` (hook, phases=before). Shared methods=['answerIncomingCall']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_answerinheadup` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.incallui.InCallPresenter#answerIncomingCall; A13=com.android.incallui.InCallPresenter#answerIncomingCall; shared_methods=['answerIncomingCall']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::AnswerCallInHeadUpHook` / `Various.kt::AnswerCallInHeadUpHook`: A13 already implements the exclusive keys various_answerinheadup. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_HiddenFeatures_kt_onActivityCreated__onActivityCreated

- PROOF_ID: `PROOF_OG_Various_HiddenFeatures_kt_onActivityCreated__onActivityCreated`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt`
- A14_SYMBOL: `onActivityCreated`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt`
- A13_SYMBOL: `onActivityCreated`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/Various_HiddenFeatures.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `various_aospnotif,various_batteryoptimization,various_runningservices`
- VALUE_DOMAIN: owner-group keys for onActivityCreated: various_aospnotif,various_batteryoptimization,various_runningservices
- DEFAULT_SEMANTICS: `various_aospnotif` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `Various_HiddenFeatures.kt::onActivityCreated` / `Various_HiddenFeatures.kt::onActivityCreated`: A13 already implements the exclusive keys various_aospnotif,various_batteryoptimization,various_runningservices. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various_HiddenFeatures.kt::onActivityCreated` (spec, phases=unknown) vs A13 `Various_HiddenFeatures.kt::onActivityCreated` (spec, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_aospnotif` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various_HiddenFeatures.kt::onActivityCreated` / `Various_HiddenFeatures.kt::onActivityCreated`: A13 already implements the exclusive keys various_aospnotif,various_batteryoptimization,various_runningservices. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_XML_various

- PROOF_ID: `PROOF_OG_XML_various`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/res/xml/prefs_various.xml`
- A14_SYMBOL: `CheckBoxPreferenceEx`
- A14_INSTALLER: `Settings module UI`
- A14_HOOK_TARGETS: `(xml/snapshot family; no exclusive hook symbol)`
- A14_CALLBACK_PHASE: `n/a`
- A13_OWNER_PATH: `app/src/main/res/xml/prefs_various.xml`
- A13_SYMBOL: `CheckBoxPreferenceEx`
- A13_INSTALLER: `Settings module UI`
- A13_HOOK_TARGETS: `(xml/snapshot family; consumed by A13 style/hook path)`
- A13_CALLBACK_PHASE: `n/a`
- PREFERENCE_KEYS: `various_appdetails`
- VALUE_DOMAIN: XML family prefs_various.xml
- DEFAULT_SEMANTICS: Both trees persist `various_appdetails` and sibling style keys in prefs_various.xml
- RESULT/ARGUMENT_BEHAVIOR: No opposite host setResult on this XML family; values are PrefMap style fields.
- API33_VARIANT_REASON: Owner-group review of XML family `prefs_various.xml`: both trees persist 1 user-visible keys including `various_appdetails`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- DIFF_SUMMARY: A14 `CheckBoxPreferenceEx` vs A13 `CheckBoxPreferenceEx` in `prefs_various.xml`. Literal-key scanner miss on snapshot fields is not a missing capability.
- VALUE_DEFAULT_COMPARISON: Same preference keys in `prefs_various.xml` on both trees.
- HOOK_TARGET_COMPARISON: No exclusive SystemUI member on the XML row; consumption is the family style hook.
- CALLBACK_SEMANTICS_COMPARISON: No Xposed callback on the XML widget; click/persist is settings-owned.
- ARG_RESULT_COMPARISON: No host setResult for the XML row itself.
- A14_ONLY_BRANCHES: none on these exclusive XML-family keys
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of XML family `prefs_various.xml`: both trees persist 1 user-visible keys including `various_appdetails`. Scanner did not bind a 1:1 hook symbol (typical of A14 snapshot builders / generated style fields). A13 still has the product row and consumes the family in the matching style/hook path.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_InCallBrightnessHook__InCallBrightnessHook

- PROOF_ID: `PROOF_OG_Various_kt_InCallBrightnessHook__InCallBrightnessHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `InCallBrightnessHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.android.incallui.InCallActivity#onCreate`
- A14_CALLBACK_PHASE: `intercept,before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `InCallBrightnessHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PhoneInstaller.java`
- A13_HOOK_TARGETS: `com.android.incallui.InCallActivity#onCreate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `various_calluibright,various_calluibright_night,various_calluibright_night_end,various_calluibright_night_start,various_calluibright_type`
- VALUE_DOMAIN: owner-group keys for InCallBrightnessHook: various_calluibright,various_calluibright_night,various_calluibright_night_end,various_calluibright_night_start,various_calluibright_type
- DEFAULT_SEMANTICS: `various_calluibright` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept,before,after` vs A13 phase `after,before`. Owner-group review of `Various.kt::InCallBrightnessHook` / `Various.kt::InCallBrightnessHook`: A13 already implements the exclusive keys various_calluibright,various_calluibright_night,various_calluibright_night_end,various_calluibright_night_start,various_calluibright_type. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::InCallBrightnessHook` (hook, phases=intercept,before,after) vs A13 `Various.kt::InCallBrightnessHook` (hook, phases=after,before). Shared methods=['onCreate']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_calluibright` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.incallui.InCallActivity#onCreate; A13=com.android.incallui.InCallActivity#onCreate; shared_methods=['onCreate']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept,before,after; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::InCallBrightnessHook` / `Various.kt::InCallBrightnessHook`: A13 already implements the exclusive keys various_calluibright,various_calluibright_night,various_calluibright_night_end,various_calluibright_night_start,various_calluibright_type. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_AppInfoDuringMiuiInstallHook__AppInfoDuringMiuiInstallHook

- PROOF_ID: `PROOF_OG_Various_kt_AppInfoDuringMiuiInstallHook__AppInfoDuringMiuiInstallHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AppInfoDuringMiuiInstallHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AppInfoDuringMiuiInstallHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `various_calluibright_val,various_installappinfo,various_miuiinstaller`
- VALUE_DOMAIN: owner-group keys for AppInfoDuringMiuiInstallHook: various_calluibright_val,various_installappinfo,various_miuiinstaller
- DEFAULT_SEMANTICS: `various_calluibright_val` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Various.kt::AppInfoDuringMiuiInstallHook` / `Various.kt::AppInfoDuringMiuiInstallHook`: A13 already implements the exclusive keys various_calluibright_val,various_installappinfo,various_miuiinstaller. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::AppInfoDuringMiuiInstallHook` (hook, phases=intercept) vs A13 `Various.kt::AppInfoDuringMiuiInstallHook` (hook, phases=after). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_calluibright_val` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::AppInfoDuringMiuiInstallHook` / `Various.kt::AppInfoDuringMiuiInstallHook`: A13 already implements the exclusive keys various_calluibright_val,various_installappinfo,various_miuiinstaller. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemServerInstaller_java_install__installHook

- PROOF_ID: `PROOF_OG_SystemServerInstaller_java_install__installHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_SYMBOL: `installHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature/SystemServerFeatures.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_SYMBOL: `install`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `various_disable_access_devicelogs`
- VALUE_DOMAIN: owner-group keys for install: various_disable_access_devicelogs
- DEFAULT_SEMANTICS: `various_disable_access_devicelogs` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemServerFeatures.kt::installHook` / `SystemServerInstaller.java::install`: A13 already implements the exclusive keys various_disable_access_devicelogs. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemServerFeatures.kt::installHook` (spec, phases=unknown) vs A13 `SystemServerInstaller.java::install` (installer, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_disable_access_devicelogs` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemServerFeatures.kt::installHook` / `SystemServerInstaller.java::install`: A13 already implements the exclusive keys various_disable_access_devicelogs. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_DisableSideBarSuggestionHook__DisableSideBarSuggestionHook

- PROOF_ID: `PROOF_OG_SystemFreeformAndMultiWindowHooks_kt_DisableSideBarSuggestionHook__DisableSideBarSuggestionHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_SYMBOL: `DisableSideBarSuggestionHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemWindowHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt`
- A13_SYMBOL: `DisableSideBarSuggestionHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `various_disable_freeform_suggest_blacklist`
- VALUE_DOMAIN: owner-group keys for DisableSideBarSuggestionHook: various_disable_freeform_suggest_blacklist
- DEFAULT_SEMANTICS: `various_disable_freeform_suggest_blacklist` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `unknown` vs A13 phase `unknown`. Owner-group review of `SystemWindowHooks.kt::DisableSideBarSuggestionHook` / `SystemFreeformAndMultiWindowHooks.kt::DisableSideBarSuggestionHook`: A13 already implements the exclusive keys various_disable_freeform_suggest_blacklist. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `SystemWindowHooks.kt::DisableSideBarSuggestionHook` (hook, phases=unknown) vs A13 `SystemFreeformAndMultiWindowHooks.kt::DisableSideBarSuggestionHook` (hook, phases=unknown). Shared methods=none; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_disable_freeform_suggest_blacklist` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=(no host hook members); A13=(no host hook members); shared_methods=n/a; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `SystemWindowHooks.kt::DisableSideBarSuggestionHook` / `SystemFreeformAndMultiWindowHooks.kt::DisableSideBarSuggestionHook`: A13 already implements the exclusive keys various_disable_freeform_suggest_blacklist. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_AppsDisableServiceHook__AppsDisableServiceHook

- PROOF_ID: `PROOF_OG_Various_kt_AppsDisableServiceHook__AppsDisableServiceHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AppsDisableServiceHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.android.server.pm.PackageManagerServiceImpl#canBeDisabled`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AppsDisableServiceHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.android.server.pm.PackageManagerServiceImpl#canBeDisabled`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `various_disableapp`
- VALUE_DOMAIN: owner-group keys for AppsDisableServiceHook: various_disableapp
- DEFAULT_SEMANTICS: `various_disableapp` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `unknown`. Owner-group review of `Various.kt::AppsDisableServiceHook` / `Various.kt::AppsDisableServiceHook`: A13 already implements the exclusive keys various_disableapp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::AppsDisableServiceHook` (hook, phases=intercept) vs A13 `Various.kt::AppsDisableServiceHook` (hook, phases=unknown). Shared methods=['canBeDisabled']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_disableapp` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.server.pm.PackageManagerServiceImpl#canBeDisabled; A13=com.android.server.pm.PackageManagerServiceImpl#canBeDisabled; shared_methods=['canBeDisabled']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=unknown. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::AppsDisableServiceHook` / `Various.kt::AppsDisableServiceHook`: A13 already implements the exclusive keys various_disableapp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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

## PROOF_OG_Various_kt_PersistBatteryOptimizationHook__PersistBatteryOptimizationHook

- PROOF_ID: `PROOF_OG_Various_kt_PersistBatteryOptimizationHook__PersistBatteryOptimizationHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `PersistBatteryOptimizationHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.miui.powerkeeper.utils.CommonAdapter#addPowerSaveWhitelistApps,com.miui.powerkeeper.millet.MilletPolicy#dealSleepModeWhiteList,com.miui.powerkeeper.statemachine.ForceDozeController#restoreWhiteListAppsIfQuitForceIdle`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `PersistBatteryOptimizationHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PowerKeeperInstaller.java`
- A13_HOOK_TARGETS: `com.miui.powerkeeper.utils.CommonAdapter#addPowerSaveWhitelistApps,com.miui.powerkeeper.millet.MilletPolicy#dealSleepModeWhiteList,com.miui.powerkeeper.statemachine.ForceDozeController#restoreWhiteListAppsIfQuitForceIdle`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_persist_batteryoptimization,various_restrictapp`
- VALUE_DOMAIN: owner-group keys for PersistBatteryOptimizationHook: various_persist_batteryoptimization,various_restrictapp
- DEFAULT_SEMANTICS: `various_persist_batteryoptimization` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Various.kt::PersistBatteryOptimizationHook` / `Various.kt::PersistBatteryOptimizationHook`: A13 already implements the exclusive keys various_persist_batteryoptimization,various_restrictapp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::PersistBatteryOptimizationHook` (hook, phases=intercept) vs A13 `Various.kt::PersistBatteryOptimizationHook` (hook, phases=before). Shared methods=['addPowerSaveWhitelistApps', 'dealSleepModeWhiteList', 'restoreWhiteListAppsIfQuitForceIdle']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_persist_batteryoptimization` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.powerkeeper.utils.CommonAdapter#addPowerSaveWhitelistApps,com.miui.powerkeeper.millet.MilletPolicy#dealSleepModeWhiteList,com.miui.powerkeeper.statemachine.ForceDozeController#restoreWhiteListAppsIfQuitForceIdle; A13=com.miui.powerkeeper.utils.CommonAdapter#addPowerSaveWhitelistApps,com.miui.powerkeeper.millet.MilletPolicy#dealSleepModeWhiteList,com.miui.powerkeeper.statemachine.ForceDozeController#restoreWhiteListAppsIfQuitForceIdle; shared_methods=['addPowerSaveWhitelistApps', 'dealSleepModeWhiteList', 'restoreWhiteListAppsIfQuitForceIdle']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::PersistBatteryOptimizationHook` / `Various.kt::PersistBatteryOptimizationHook`: A13 already implements the exclusive keys various_persist_batteryoptimization,various_restrictapp. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_ShowTempInBatteryHook__ShowTempInBatteryHook

- PROOF_ID: `PROOF_OG_Various_kt_ShowTempInBatteryHook__ShowTempInBatteryHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `ShowTempInBatteryHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `#handleMessage`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `ShowTempInBatteryHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `#handleMessage`
- A13_CALLBACK_PHASE: `after,intercept`
- PREFERENCE_KEYS: `various_replace_defaultopen_with_openbydefault,various_show_battery_temperature,various_skip_securityscan`
- VALUE_DOMAIN: owner-group keys for ShowTempInBatteryHook: various_replace_defaultopen_with_openbydefault,various_show_battery_temperature,various_skip_securityscan
- DEFAULT_SEMANTICS: `various_replace_defaultopen_with_openbydefault` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,intercept`. Owner-group review of `Various.kt::ShowTempInBatteryHook` / `Various.kt::ShowTempInBatteryHook`: A13 already implements the exclusive keys various_replace_defaultopen_with_openbydefault,various_show_battery_temperature,various_skip_securityscan. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::ShowTempInBatteryHook` (hook, phases=intercept) vs A13 `Various.kt::ShowTempInBatteryHook` (hook, phases=after,intercept). Shared methods=['handleMessage']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_replace_defaultopen_with_openbydefault` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=#handleMessage; A13=#handleMessage; shared_methods=['handleMessage']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,intercept. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::ShowTempInBatteryHook` / `Various.kt::ShowTempInBatteryHook`: A13 already implements the exclusive keys various_replace_defaultopen_with_openbydefault,various_show_battery_temperature,various_skip_securityscan. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_ShowCallUIHook__ShowCallUIHook

- PROOF_ID: `PROOF_OG_Various_kt_ShowCallUIHook__ShowCallUIHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `ShowCallUIHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.android.incallui.InCallPresenter#startUi`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `ShowCallUIHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/PhoneInstaller.java`
- A13_HOOK_TARGETS: `com.android.incallui.InCallPresenter#startUi`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `various_showcallui`
- VALUE_DOMAIN: owner-group keys for ShowCallUIHook: various_showcallui
- DEFAULT_SEMANTICS: `various_showcallui` A14 default=0; A13 default=0
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 setResult[true]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after`. Owner-group review of `Various.kt::ShowCallUIHook` / `Various.kt::ShowCallUIHook`: A13 already implements the exclusive keys various_showcallui. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::ShowCallUIHook` (hook, phases=intercept) vs A13 `Various.kt::ShowCallUIHook` (hook, phases=after). Shared methods=['startUi']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_showcallui` A14=0 A13=0. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.android.incallui.InCallPresenter#startUi; A13=com.android.incallui.InCallPresenter#startUi; shared_methods=['startUi']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 setResult[true]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::ShowCallUIHook` / `Various.kt::ShowCallUIHook`: A13 already implements the exclusive keys various_showcallui. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_SkipSecurityScanHook__SkipSecurityScanHook

- PROOF_ID: `PROOF_OG_Various_kt_SkipSecurityScanHook__SkipSecurityScanHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `SkipSecurityScanHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.miui.securityscan.model.ModelFactory#produceSystemGroupModel,com.miui.securityscan.model.ModelFactory#produceManualGroupModel,com.miui.common.customview.ScoreTextView#setScore,com.miui.securityscan.ui.main.MainContentFrame#onClick`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `SkipSecurityScanHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `com.miui.securityscan.model.ModelFactory#produceSystemGroupModel,com.miui.securityscan.model.ModelFactory#produceManualGroupModel,com.miui.common.customview.ScoreTextView#setScore,com.miui.securityscan.ui.main.MainContentFrame#onClick`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `various_skip_interceptperm`
- VALUE_DOMAIN: owner-group keys for SkipSecurityScanHook: various_skip_interceptperm
- DEFAULT_SEMANTICS: `various_skip_interceptperm` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[ArrayList<Any>(,res]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `before`. Owner-group review of `Various.kt::SkipSecurityScanHook` / `Various.kt::SkipSecurityScanHook`: A13 already implements the exclusive keys various_skip_interceptperm. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::SkipSecurityScanHook` (hook, phases=intercept) vs A13 `Various.kt::SkipSecurityScanHook` (hook, phases=before). Shared methods=['onClick', 'produceManualGroupModel', 'produceSystemGroupModel', 'setScore']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_skip_interceptperm` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=com.miui.securityscan.model.ModelFactory#produceSystemGroupModel,com.miui.securityscan.model.ModelFactory#produceManualGroupModel,com.miui.common.customview.ScoreTextView#setScore,com.miui.securityscan.ui.main.MainContentFrame#onClick; A13=com.miui.securityscan.model.ModelFactory#produceSystemGroupModel,com.miui.securityscan.model.ModelFactory#produceManualGroupModel,com.miui.common.customview.ScoreTextView#setScore,com.miui.securityscan.ui.main.MainContentFrame#onClick; shared_methods=['onClick', 'produceManualGroupModel', 'produceSystemGroupModel', 'setScore']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[null,null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[ArrayList<Any>(,res]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::SkipSecurityScanHook` / `Various.kt::SkipSecurityScanHook`: A13 already implements the exclusive keys various_skip_interceptperm. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_OG_Various_kt_AddSideBarExpandReceiverHook__AddSideBarExpandReceiverHook

- PROOF_ID: `PROOF_OG_Various_kt_AddSideBarExpandReceiverHook__AddSideBarExpandReceiverHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `AddSideBarExpandReceiverHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `#onTouch,#draw,#onViewDetachedFromWindow`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `AddSideBarExpandReceiverHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_HOOK_TARGETS: `#onTouch,#draw,#onViewDetachedFromWindow`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `various_swipe_expand_sidebar`
- VALUE_DOMAIN: owner-group keys for AddSideBarExpandReceiverHook: various_swipe_expand_sidebar
- DEFAULT_SEMANTICS: `various_swipe_expand_sidebar` A14 default=n/a; A13 default=n/a
- RESULT/ARGUMENT_BEHAVIOR: A14 result_assign[false,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[false,null,null]
- API33_VARIANT_REASON: hook_compat=True; ownership_compat=True; A14 phase `intercept` vs A13 phase `after,before`. Owner-group review of `Various.kt::AddSideBarExpandReceiverHook` / `Various.kt::AddSideBarExpandReceiverHook`: A13 already implements the exclusive keys various_swipe_expand_sidebar. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
- DIFF_SUMMARY: A14 `Various.kt::AddSideBarExpandReceiverHook` (hook, phases=intercept) vs A13 `Various.kt::AddSideBarExpandReceiverHook` (hook, phases=after,before). Shared methods=['draw', 'onTouch', 'onViewDetachedFromWindow']; A14-only members=none; A13-only members=none.
- VALUE_DEFAULT_COMPARISON: Reviewed keys keep the same preference identifiers. Sample `various_swipe_expand_sidebar` A14=n/a A13=n/a. No extra List/SeekBar domain was proven for these exclusive keys.
- HOOK_TARGET_COMPARISON: A14=#onTouch,#draw,#onViewDetachedFromWindow; A13=#onTouch,#draw,#onViewDetachedFromWindow; shared_methods=['draw', 'onTouch', 'onViewDetachedFromWindow']; hook_targets_compatible=True
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before. intercept/proceed-once versus before returnAndSkip / after setResult is the API33 libxposed adapter when polarity is not inverted on this pair.
- ARG_RESULT_COMPARISON: A14 result_assign[false,null,null,null]; chain.proceed[chain.proceed(),chain.proceed(),chain.proceed(),chain.proceed()]; A13 returnAndSkip[false,null,null]
- A14_ONLY_BRANCHES: A14-only sibling keys=none; A14-only hook members=none. Those siblings are classified on their own rows; they are not extra modes of this group.
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Owner-group review of `Various.kt::AddSideBarExpandReceiverHook` / `Various.kt::AddSideBarExpandReceiverHook`: A13 already implements the exclusive keys various_swipe_expand_sidebar. Differences are API33 intercept/before-after translation, MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus A13 hook/settings consumption. Sibling A14 keys ['none'] are separate product rows, not extra modes of these keys.
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
