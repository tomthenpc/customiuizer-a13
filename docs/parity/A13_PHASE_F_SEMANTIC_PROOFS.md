# A13 Phase F-R2 Semantic Proofs

Automatic PRESENT requires normalized body IDENTICAL, the same relevant preference keys,
and compatible installer ownership (BODY_RELATION=IDENTICAL).
Non-identical owners require an explicit reviewed manifest (BODY_RELATION=REVIEWED_VARIANT)
with filled difference fields. Same-key reads alone are IMPLEMENTATION_PRESENCE.

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

## PROOF_REVIEWED_Controls_kt_FingerprintHapticSuccessHook

- PROOF_ID: `PROOF_REVIEWED_Controls_kt_FingerprintHapticSuccessHook`
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
- PREFERENCE_KEYS: `controls_fingerprintsuccess`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `"1"`; A13 default `"1"`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `controls_fingerprintsuccess` and rewrite host members ['onAuthenticated']. Scaffolding-stripped body ratio=0.624. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.581; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { val mAuthSuccess = `; replace: A14`Interface.Chain): Any? { var result: Any? var throwable: ` A13`Helpers.getBooleanField(param.get`; delete: A14`hrowable? = null try { result = chain.proceed() } catch (t: Throwable) { throwable = t res` A13``; insert: A14`` A13`(), "mAuthSuccess") if (!mAuthSuccess) return val mContext`; replace: A14`chain.this` A13`XposedHelpers.get`
- VALUE_DEFAULT_COMPARISON: A14 `controls_fingerprintsuccess` default="1"; A13 default="1"
- HOOK_TARGET_COMPARISON: A14=com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated; A13=com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated; shared_methods=['onAuthenticated']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `controls_fingerprintsuccess` and rewrite host members ['onAuthenticated']. Scaffolding-stripped body ratio=0.624. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_Controls_kt_BackGestureAreaWidthHook

- PROOF_ID: `PROOF_REVIEWED_Controls_kt_BackGestureAreaWidthHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_SYMBOL: `BackGestureAreaWidthHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A14_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#initScreenSizeAndDensity,com.miui.home.recents.GestureStubView#setSize`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt`
- A13_SYMBOL: `BackGestureAreaWidthHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.recents.GestureStubView#initScreenSizeAndDensity,com.miui.home.recents.GestureStubView#setSize`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `controls_fsg_width`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `100`; A13 default `100`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null,null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `controls_fsg_width` and rewrite host members ['initScreenSizeAndDensity', 'setSize']. Scaffolding-stripped body ratio=0.624. Callback delta is A14 `intercept` vs A13 `after,before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.406; insert: A14`` A13`Silently`; replace: A14`intercept(chain` A13`after(param`; replace: A14`XposedInterface.` A13`AfterHook`; replace: A14`hain): Any?` A13`allback)`; delete: A14` var result: Any? var throwable: Throwable? = null try { result = chain.proceed() } catch ` A13``; replace: A14`{ return` A13`return var mGestureStubDefaultSize =`
- VALUE_DEFAULT_COMPARISON: A14 `controls_fsg_width` default=100; A13 default=100
- HOOK_TARGET_COMPARISON: A14=com.miui.home.recents.GestureStubView#initScreenSizeAndDensity,com.miui.home.recents.GestureStubView#setSize; A13=com.miui.home.recents.GestureStubView#initScreenSizeAndDensity,com.miui.home.recents.GestureStubView#setSize; shared_methods=['initScreenSizeAndDensity', 'setSize']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `controls_fsg_width` and rewrite host members ['initScreenSizeAndDensity', 'setSize']. Scaffolding-stripped body ratio=0.624. Callback delta is A14 `intercept` vs A13 `after,before`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_Controls_kt_reposNavBarButtons

- PROOF_ID: `PROOF_REVIEWED_Controls_kt_reposNavBarButtons`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `controls_navbarmargin` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.918. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.918; replace: A14`!!.rotation` A13`?.rotation ?: Surface.ROTATION_0`; insert: A14`` A13`?`; insert: A14`` A13`?`; insert: A14`` A13`?`; insert: A14`` A13`?`; replace: A14`r` A13`l`
- VALUE_DEFAULT_COMPARISON: A14 `controls_navbarmargin` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `controls_navbarmargin` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.918. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_Controls_kt_PowerKeyHook

- PROOF_ID: `PROOF_REVIEWED_Controls_kt_PowerKeyHook`
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
- PREFERENCE_KEYS: `controls_powerflash_delay`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()] | returnAndSkip[0,0]
- API33_VARIANT_REASON: Both owners read `controls_powerflash_delay` and rewrite host members ['init', 'interceptKeyBeforeQueueing']. Scaffolding-stripped body ratio=0.660. Callback delta is A14 `intercept` vs A13 `after,before,intercept`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.592; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { val mContext = `; replace: A14`Interface.Chain): Any? { var result: Any? var throwable: Throwable? = null try { result = ` A13`Helpers.get`; replace: A14` = chain.t` A13`Field(param.getT`; replace: A14` val ` A13`(), "`; replace: A14` = XposedHelpers.get` A13`") as? Context ?: return if (sScreen`
- VALUE_DEFAULT_COMPARISON: A14 `controls_powerflash_delay` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing; A13=com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing; shared_methods=['init', 'interceptKeyBeforeQueueing']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after,before,intercept; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 returnAndSkip[0,0]
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `controls_powerflash_delay` and rewrite host members ['init', 'interceptKeyBeforeQueueing']. Scaffolding-stripped body ratio=0.660. Callback delta is A14 `intercept` vs A13 `after,before,intercept`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_GlobalActions_kt_sendDownUpKeyEvent

- PROOF_ID: `PROOF_REVIEWED_GlobalActions_kt_sendDownUpKeyEvent`
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
- PREFERENCE_KEYS: `controls_volumemedia_vibrate`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `true`; A13 default `true`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `controls_volumemedia_vibrate` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.948. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.948; insert: A14`` A13` if (mContext == null) return`; insert: A14`` A13`?`; insert: A14`` A13` ?: return`; delete: A14`{ ` A13``; delete: A14` }` A13``
- VALUE_DEFAULT_COMPARISON: A14 `controls_volumemedia_vibrate` default=true; A13 default=true
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `controls_volumemedia_vibrate` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.948. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_LauncherFolderHooks_kt_CloseFolderOrDrawerOnLaunchShortcutMenuHook

- PROOF_ID: `PROOF_REVIEWED_LauncherFolderHooks_kt_CloseFolderOrDrawerOnLaunchShortcutMenuHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_SYMBOL: `CloseFolderOrDrawerOnLaunchShortcutMenuHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.shortcuts.AppShortcutMenuItem#getOnClickListener`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt`
- A13_SYMBOL: `CloseFolderOrDrawerOnLaunchShortcutMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.shortcuts.AppShortcutMenuItem#getOnClickListener`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_closedrawer`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | setResult[View.OnClickListener { view -> listener.onClick(view]
- API33_VARIANT_REASON: Both owners read `launcher_closedrawer` and rewrite host members ['getOnClickListener']. Scaffolding-stripped body ratio=0.734. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.691; replace: A14`intercept(chain` A13`after(param`; replace: A14`XposedInterface.Chain)` A13`AfterHookCallback) { val listener = param.getResult() as? View.OnClickListener ?`; replace: A14`Any?` A13`return param.setResult(View.OnClickListener`; replace: A14`ar result: Any? = null ` A13`iew -> listener.onClick(`; replace: A14`ar thro` A13`ie`; delete: A14`able: Throwable? = null try { result = chain.proceed() } catch (t: Throwable) { throwable ` A13``
- VALUE_DEFAULT_COMPARISON: A14 `launcher_closedrawer` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.shortcuts.AppShortcutMenuItem#getOnClickListener; A13=com.miui.home.launcher.shortcuts.AppShortcutMenuItem#getOnClickListener; shared_methods=['getOnClickListener']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 setResult[View.OnClickListener { view -> listener.onClick(view]
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_closedrawer` and rewrite host members ['getOnClickListener']. Scaffolding-stripped body ratio=0.734. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_LauncherAnimationHooks_kt_DisableLauncherWallpaperScale

- PROOF_ID: `PROOF_REVIEWED_LauncherAnimationHooks_kt_DisableLauncherWallpaperScale`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
- A14_SYMBOL: `DisableLauncherWallpaperScale`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.recents.DimLayer#isSupportDim,com.miui.home.recents.OverviewState#onStateEnabled`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
- A13_SYMBOL: `DisableLauncherWallpaperScale`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.recents.DimLayer#isSupportDim,com.miui.home.recents.OverviewState#onStateEnabled`
- A13_CALLBACK_PHASE: `before,after`
- PREFERENCE_KEYS: `launcher_disable_wallpaperscale`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `launcher_disable_wallpaperscale` and rewrite host members ['isSupportDim', 'onStateEnabled']. Scaffolding-stripped body ratio=0.680. Callback delta is A14 `intercept` vs A13 `before,after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.638; replace: A14`W` A13`w`; insert: A14`` A13`if (wallpaperZoomManagerKtClass != null) { try { `; replace: A14`W` A13`w`; insert: A14`` A13`} catch (t: Throwable) { RuntimeFatality.throwIfFatal(t) XposedHelpers.log("DisableLaunche`; replace: A14`intercept(chain` A13`before(param`; insert: A14`` A13`BeforeHookCallback) { if (wallpaperZoomManagerKtClass == null) return try { `
- VALUE_DEFAULT_COMPARISON: A14 `launcher_disable_wallpaperscale` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=com.miui.home.recents.DimLayer#isSupportDim,com.miui.home.recents.OverviewState#onStateEnabled; A13=com.miui.home.recents.DimLayer#isSupportDim,com.miui.home.recents.OverviewState#onStateEnabled; shared_methods=['isSupportDim', 'onStateEnabled']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before,after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_disable_wallpaperscale` and rewrite host members ['isSupportDim', 'onStateEnabled']. Scaffolding-stripped body ratio=0.680. Callback delta is A14 `intercept` vs A13 `before,after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_LauncherLayoutHooks_kt_DockMarginBottomHook

- PROOF_ID: `PROOF_REVIEWED_LauncherLayoutHooks_kt_DockMarginBottomHook`
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
- PREFERENCE_KEYS: `launcher_dock_bottommargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- API33_VARIANT_REASON: Both owners read `launcher_dock_bottommargin` and rewrite host members ['calcHotSeatsMarginBottom']. Scaffolding-stripped body ratio=0.755. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.622; delete: A14`!!` A13``; delete: A14`!!` A13``; replace: A14`intercept(chain: XposedInterface.` A13`before(param: BeforeHook`; replace: A14`hain): ` A13`allback) { param.return`; replace: A14`y? { var skipped = false var result: Any? = null var throwable: Throwable? = null try { sk` A13`dSkip(`; replace: A14` throwable = null` A13`)`
- VALUE_DEFAULT_COMPARISON: A14 `launcher_dock_bottommargin` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom; A13=com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginBottom; shared_methods=['calcHotSeatsMarginBottom']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_dock_bottommargin` and rewrite host members ['calcHotSeatsMarginBottom']. Scaffolding-stripped body ratio=0.755. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_LauncherLayoutHooks_kt_DockMarginTopHook

- PROOF_ID: `PROOF_REVIEWED_LauncherLayoutHooks_kt_DockMarginTopHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `DockMarginTopHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginTop`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `DockMarginTopHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginTop`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_dock_topmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- API33_VARIANT_REASON: Both owners read `launcher_dock_topmargin` and rewrite host members ['calcHotSeatsMarginTop']. Scaffolding-stripped body ratio=0.734. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.596; delete: A14`!!` A13``; replace: A14`intercept(chain: XposedInterface.` A13`before(param: BeforeHook`; replace: A14`hain): ` A13`allback) { param.return`; replace: A14`y? { var skipped = false var result: Any? = null var throwable: Throwable? = null try { sk` A13`dSkip(`; replace: A14` throwable = null` A13`)`; delete: A14`catch (t: Throwable) { throwable = t result = null ` A13``
- VALUE_DEFAULT_COMPARISON: A14 `launcher_dock_topmargin` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginTop; A13=com.miui.home.launcher.DeviceConfig#calcHotSeatsMarginTop; shared_methods=['calcHotSeatsMarginTop']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_dock_topmargin` and rewrite host members ['calcHotSeatsMarginTop']. Scaffolding-stripped body ratio=0.734. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_LauncherLayoutHooks_kt_HideSeekPointsHook

- PROOF_ID: `PROOF_REVIEWED_LauncherLayoutHooks_kt_HideSeekPointsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `HideSeekPointsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.pageindicators.AllAppsIndicator#shouldHide,com.miui.home.launcher.pageindicators.AllAppsIndicator#hideAllAppsArrow`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `HideSeekPointsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_HOOK_TARGETS: `com.miui.home.launcher.pageindicators.AllAppsIndicator#shouldHide,com.miui.home.launcher.pageindicators.AllAppsIndicator#hideAllAppsArrow`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_hideseekpoints_edit`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `launcher_hideseekpoints_edit` and rewrite host members ['hideAllAppsArrow', 'shouldHide']. Scaffolding-stripped body ratio=0.659. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.625; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { val mLauncher = `; replace: A14`Interface.Chain): Any? { var result: Any? = null var throwable: Throwable? = null try { re` A13`Helpers.get`; replace: A14` = chain` A13`Field(param`; insert: A14`` A13`, "mLauncher") if (mLauncher == null) return`; replace: A14`mLauncher` A13`workspace`
- VALUE_DEFAULT_COMPARISON: A14 `launcher_hideseekpoints_edit` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.pageindicators.AllAppsIndicator#shouldHide,com.miui.home.launcher.pageindicators.AllAppsIndicator#hideAllAppsArrow; A13=com.miui.home.launcher.pageindicators.AllAppsIndicator#shouldHide,com.miui.home.launcher.pageindicators.AllAppsIndicator#hideAllAppsArrow; shared_methods=['hideAllAppsArrow', 'shouldHide']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_hideseekpoints_edit` and rewrite host members ['hideAllAppsArrow', 'shouldHide']. Scaffolding-stripped body ratio=0.659. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_LauncherLayoutHooks_kt_HorizontalSpacingRes

- PROOF_ID: `PROOF_REVIEWED_LauncherLayoutHooks_kt_HorizontalSpacingRes`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `HorizontalSpacingRes`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `HorizontalSpacingRes`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_horizmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `launcher_horizmargin` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.417. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.417; replace: A14`r` A13`getR`; replace: A14`.setT` A13`().setDensityReplacement("com.miui.`; replace: A14`emeValue` A13`ome", "dimen", "workspace_cell_padding_side", opt.toFloat()) MainModule.getResHooks().setD`; replace: A14`", opt) MainModule.r` A13`_no_word", opt.toFloat()) MainModule.getR`; replace: A14`.setT` A13`().setDensityReplacement("com.miui.`; replace: A14`emeValueReplacement("com.miui.` A13`ome", "dimen", "workspace_cell_padding_side_rotatable", opt.toFloat()) MainModule.getResHo`
- VALUE_DEFAULT_COMPARISON: A14 `launcher_horizmargin` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_horizmargin` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.417. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_LauncherLayoutHooks_kt_IndicatorMarginTopHook

- PROOF_ID: `PROOF_REVIEWED_LauncherLayoutHooks_kt_IndicatorMarginTopHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `IndicatorMarginTopHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.util.DimenUtils1X#getDimensionPixelSize`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `IndicatorMarginTopHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.util.DimenUtils1X#getDimensionPixelSize`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_indicator_topmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- API33_VARIANT_REASON: Both owners read `launcher_indicator_topmargin` and rewrite host members ['getDimensionPixelSize']. Scaffolding-stripped body ratio=0.647. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.546; replace: A14`r` A13`getR`; replace: A14`.setT` A13`().setDensityReplacement("com.miui.`; replace: A14`emeValue` A13`ome", "dimen", "slide_bar_margin_top", opt.toFloat()) MainModule.get`; replace: A14`placement("com.miui.` A13`sHooks().setDensityReplacement("com.mi.android.globallaunc`; replace: A14`ome` A13`er`; insert: A14`` A13`.toFloat()`
- VALUE_DEFAULT_COMPARISON: A14 `launcher_indicator_topmargin` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.util.DimenUtils1X#getDimensionPixelSize; A13=com.miui.home.launcher.util.DimenUtils1X#getDimensionPixelSize; shared_methods=['getDimensionPixelSize']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_indicator_topmargin` and rewrite host members ['getDimensionPixelSize']. Scaffolding-stripped body ratio=0.647. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_LauncherLayoutHooks_kt_IndicatorHeightRes

- PROOF_ID: `PROOF_REVIEWED_LauncherLayoutHooks_kt_IndicatorHeightRes`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `IndicatorHeightRes`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `IndicatorHeightRes`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `launcher_indicatorheight`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `9`; A13 default `9`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `launcher_indicatorheight` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.644. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.644; replace: A14`r` A13`getR`; replace: A14`.setThemeValue` A13`().setDensity`; insert: A14`` A13`.toFloat()) MainModule.getResHooks().setDensityReplacement("com.mi.android.globallauncher"`
- VALUE_DEFAULT_COMPARISON: A14 `launcher_indicatorheight` default=9; A13 default=9
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_indicatorheight` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.644. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_LauncherIconHooks_kt_TitleTopMarginHook

- PROOF_ID: `PROOF_REVIEWED_LauncherIconHooks_kt_TitleTopMarginHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_SYMBOL: `TitleTopMarginHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt`
- A13_SYMBOL: `TitleTopMarginHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.ItemIcon#onFinishInflate`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `launcher_titletopmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `launcher_titletopmargin` and rewrite host members ['onFinishInflate']. Scaffolding-stripped body ratio=0.792. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.732; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { val mTitleContainer = `; replace: A14`Interface.Chain): Any? { var result: Any? = null var throwable: Throwable? = null try { re` A13`Helpers.get`; replace: A14` = chain` A13`Field(param`; delete: A14` val mTitleContainer = XposedHelpers.getObjectField(thisObject` A13``; replace: A14`if (mTitleContainer == null) { return XposedHelpers.throwOrReturn(throwable, result) }` A13`?: return`
- VALUE_DEFAULT_COMPARISON: A14 `launcher_titletopmargin` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.ItemIcon#onFinishInflate; A13=com.miui.home.launcher.ItemIcon#onFinishInflate; shared_methods=['onFinishInflate']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_titletopmargin` and rewrite host members ['onFinishInflate']. Scaffolding-stripped body ratio=0.792. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_LauncherLayoutHooks_kt_WorkspaceCellPaddingTopHook

- PROOF_ID: `PROOF_REVIEWED_LauncherLayoutHooks_kt_WorkspaceCellPaddingTopHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_SYMBOL: `WorkspaceCellPaddingTopHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A14_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#getWorkspaceCellPaddingTop`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt`
- A13_SYMBOL: `WorkspaceCellPaddingTopHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
- A13_HOOK_TARGETS: `com.miui.home.launcher.DeviceConfig#getWorkspaceCellPaddingTop`
- A13_CALLBACK_PHASE: `before`
- PREFERENCE_KEYS: `launcher_topmargin`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- API33_VARIANT_REASON: Both owners read `launcher_topmargin` and rewrite host members ['getWorkspaceCellPaddingTop']. Scaffolding-stripped body ratio=0.811. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.692; replace: A14`intercept(chain` A13`before(param`; replace: A14`XposedInterface.` A13`BeforeHook`; replace: A14`hain): ` A13`allback) { param.return`; replace: A14`y? { var skipped = false var result: Any? = null var throwable: Throwable? = null try { sk` A13`dSkip(`; replace: A14` throwable = null } catch (t: Throwable) { throwable = t result = null } if (skipped) { re` A13`)`
- VALUE_DEFAULT_COMPARISON: A14 `launcher_topmargin` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=com.miui.home.launcher.DeviceConfig#getWorkspaceCellPaddingTop; A13=com.miui.home.launcher.DeviceConfig#getWorkspaceCellPaddingTop; shared_methods=['getWorkspaceCellPaddingTop']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 returnAndSkip[Math.round(HookUtils.dp2px(opt.toFloat(]
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `launcher_topmargin` and rewrite host members ['getWorkspaceCellPaddingTop']. Scaffolding-stripped body ratio=0.811. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_BackupRestore_kt_performRestore

- PROOF_ID: `PROOF_REVIEWED_BackupRestore_kt_performRestore`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `miuizer_launchericon` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.157. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.157; insert: A14`` A13`rethrowIfFatal(t) return@use RestoreResult( status = Status.FAILURE, commitSucceeded = fal`; insert: A14`` A13` val rawRoot = try { decodeBackup(bytes) } catch (oom: OutOfMemoryError) { throw oom } cat`
- VALUE_DEFAULT_COMPARISON: A14 `miuizer_launchericon` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `miuizer_launchericon` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.157. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_GlobalActions_kt_miuizerSettingsHook

- PROOF_ID: `PROOF_REVIEWED_GlobalActions_kt_miuizerSettingsHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A14_SYMBOL: `miuizerSettingsHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A14_HOOK_TARGETS: `com.android.settings.MiuiSettings#updateHeaderList,com.android.settings.MiuiSettings\$HeaderAdapter#setIcon`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/GlobalActions.kt`
- A13_SYMBOL: `miuizerSettingsHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SettingsInstaller.java`
- A13_HOOK_TARGETS: `com.android.settings.MiuiSettings#updateHeaderList,com.android.settings.MiuiSettings\$HeaderAdapter#setIcon`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `miuizer_settingsiconpos`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `miuizer_settingsiconpos` and rewrite host members ['setIcon', 'updateHeaderList']. Scaffolding-stripped body ratio=0.850. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.782; replace: A14`r` A13`getR`; replace: A14`.addFake` A13`().add`; delete: A14`, "drawable"` A13``; replace: A14`intercept(chain` A13`after(param`; replace: A14`XposedInter` A13`A`; replace: A14`ace.` A13`terHook`
- VALUE_DEFAULT_COMPARISON: A14 `miuizer_settingsiconpos` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=com.android.settings.MiuiSettings#updateHeaderList,com.android.settings.MiuiSettings\$HeaderAdapter#setIcon; A13=com.android.settings.MiuiSettings#updateHeaderList,com.android.settings.MiuiSettings\$HeaderAdapter#setIcon; shared_methods=['setIcon', 'updateHeaderList']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null,null]; chain.proceed[chain.proceed(),chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `miuizer_settingsiconpos` and rewrite host members ['setIcon', 'updateHeaderList']. Scaffolding-stripped body ratio=0.850. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemAudioAndVisualAndMoreHooks_kt_AllRotationsHook

- PROOF_ID: `PROOF_REVIEWED_SystemAudioAndVisualAndMoreHooks_kt_AllRotationsHook`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_allrotations2` and rewrite host members ['<init>']. Scaffolding-stripped body ratio=0.626. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.572; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { `; insert: A14`` A13`Helpers.set`; replace: A14`erface.Chain): Any? { var result: Any? var throwable: Throwable? = null try { result = cha` A13`Field(param.`; delete: A14` = chain.thisObject XposedHelpers.setIntField(thisObject` A13``; delete: A14`catch (t: Throwable) { XposedHelpers.log(t) ` A13``
- VALUE_DEFAULT_COMPARISON: A14 `system_allrotations2` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=com.android.server.wm.DisplayRotation#<init>; A13=com.android.server.wm.DisplayRotation#<init>; shared_methods=['<init>']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_allrotations2` and rewrite host members ['<init>']. Scaffolding-stripped body ratio=0.626. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemLockScreenMoreHooks_kt_checkLastCheck

- PROOF_ID: `PROOF_REVIEWED_SystemLockScreenMoreHooks_kt_checkLastCheck`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_SYMBOL: `checkLastCheck`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt`
- A13_SYMBOL: `checkLastCheck`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_applock_timeout`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_applock_timeout` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.929. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.929; insert: A14`` A13`?`; insert: A14`` A13` `; replace: A14` if (mAccessControlLastCheckSaved == null)` A13`:`; insert: A14`` A13`?`; insert: A14`` A13`?: return `; replace: A14`pair` A13`(pkg, time)`
- VALUE_DEFAULT_COMPARISON: A14 `system_applock_timeout` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_applock_timeout` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.929. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_BatteryIndicator_kt_updateParameters

- PROOF_ID: `PROOF_REVIEWED_BatteryIndicator_kt_updateParameters`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_SYMBOL: `updateParameters`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt`
- A13_SYMBOL: `updateParameters`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_batteryindicator`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_batteryindicator` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.754. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.754; delete: A14` mFullColor = MainModule.mPrefs.getInt("system_batteryindicator_colorval1", Color.GREEN) m` A13``; insert: A14`` A13`?`; insert: A14`` A13` ?: return`; insert: A14`` A13`this.`; insert: A14`` A13`Math.round`; replace: A14`.round` A13` } catch (oom: OutOfMemoryError) { throw oom } catch (ignored: `
- VALUE_DEFAULT_COMPARISON: A14 `system_batteryindicator` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=['system_batteryindicator_colorval1', 'system_batteryindicator_colorval2', 'system_batteryindicator_colorval3', 'system_batteryindicator_colorval4']; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_batteryindicator` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.754. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemStatusBarAndClockHooks_kt_checkToast

- PROOF_ID: `PROOF_REVIEWED_SystemStatusBarAndClockHooks_kt_checkToast`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_SYMBOL: `checkToast`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt`
- A13_SYMBOL: `checkToast`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_blocktoasts`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_blocktoasts` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.812. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.812; insert: A14`` A13` return`; insert: A14`` A13` != null && selectedApps`; delete: A14`return (` A13``; replace: A14`) || (` A13` || `; delete: A14`)` A13``; insert: A14`` A13`if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t `
- VALUE_DEFAULT_COMPARISON: A14 `system_blocktoasts` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_blocktoasts` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.812. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_System_kt_onActivityResult

- PROOF_ID: `PROOF_REVIEWED_System_kt_onActivityResult`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A14_SYMBOL: `onActivityResult`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/subs/System.kt`
- A13_SYMBOL: `onActivityResult`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_calendar_app`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_calendar_app` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.619. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.619; replace: A14`i` A13`key?.let { val user = data?.getIntExtra("user", 0) ?: 0 AppHelper.appPre`; replace: A14` (key != null) { AppHelper.appPrefs!!.edit() ` A13`s?.edit()?`; replace: A14`key` A13`it`; replace: A14` ` A13`?`; replace: A14`key + "` A13`"${it}`; replace: A14`data?.getIntExtra("user"` A13`user)?.apply() } } super.onActivityResult(requestCode`
- VALUE_DEFAULT_COMPARISON: A14 `system_calendar_app` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_calendar_app` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.619. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_SystemShareAndOpenWithHooks_kt_CleanOpenWithMenuHook

- PROOF_ID: `PROOF_REVIEWED_SystemShareAndOpenWithHooks_kt_CleanOpenWithMenuHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareMenuHooks.kt`
- A14_SYMBOL: `CleanOpenWithMenuHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareMenuHooks.kt`
- A14_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A14_CALLBACK_PHASE: `intercept`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt`
- A13_SYMBOL: `CleanOpenWithMenuHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt`
- A13_HOOK_TARGETS: `miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run`
- A13_CALLBACK_PHASE: `after`
- PREFERENCE_KEYS: `system_cleanopenwith_apps`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_cleanopenwith_apps` and rewrite host members ['run']. Scaffolding-stripped body ratio=0.758. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.696; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { val mOriginalIntent = `; replace: A14`Interface.Chain): Any? { var result: Any? var throwable: Throwable? = null try { result = ` A13`Helpers.get`; replace: A14` = chain` A13`Field(param`; replace: A14` val ` A13`, "`; insert: A14`` A13`") as? Intent ?: return val action`
- VALUE_DEFAULT_COMPARISON: A14 `system_cleanopenwith_apps` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; A13=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; shared_methods=['run']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_cleanopenwith_apps` and rewrite host members ['run']. Scaffolding-stripped body ratio=0.758. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemShareAndOpenWithHooks_kt_CleanShareMenuHook

- PROOF_ID: `PROOF_REVIEWED_SystemShareAndOpenWithHooks_kt_CleanShareMenuHook`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_cleanshare_apps` and rewrite host members ['run']. Scaffolding-stripped body ratio=0.675. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.612; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { val mOriginalIntent = `; replace: A14`Interface.Chain): Any? { var result: Any? var throwable: Throwable? = null try { result = ` A13`Helpers.get`; replace: A14` = chain` A13`Field(param`; delete: A14` val mOriginalIntent = XposedHelpers.getObjectField(thisObject` A13``; delete: A14` Intent` A13``
- VALUE_DEFAULT_COMPARISON: A14 `system_cleanshare_apps` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; A13=miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner#run; shared_methods=['run']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_cleanshare_apps` and rewrite host members ['run']. Scaffolding-stripped body ratio=0.675. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemUIStatusBarHooks_kt_initNetSpeedStyle

- PROOF_ID: `PROOF_REVIEWED_SystemUIStatusBarHooks_kt_initNetSpeedStyle`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildNetSpeedTextStyleSnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `initNetSpeedStyle`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_detailednetspeed_align`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_detailednetspeed_align` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.113. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.113; replace: A14`return ` A13`if (meter.getTag(netSpeedStyleAppliedTag) == true) return val isFirst = !init`; insert: A14`` A13`StyleLogged if (isFirst) { initNetSpeedStyleLogged = true XposedHelpers.log("CustoMIUIzer `; replace: A14`StyleSnaps` A13`Views(meter) ?: return val iconTextView = `; replace: A14`t( id = idGenerator.incrementAndGet(), speedStyle = prefs.getStringAsInt("system_detailedn` A13`lder.num`; replace: A14`old = p` A13`erView val dualRow = MainModule.mP`; replace: A14`netspeed_boldfont"),` A13`detailednetspeed") || MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow") `
- VALUE_DEFAULT_COMPARISON: A14 `system_detailednetspeed_align` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=['system_detailednetspeed_style', 'system_netspeed_boldfont', 'system_netspeed_fixedcontent_width']; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_detailednetspeed_align` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.113. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemSecurityAndSystemHooks_kt_ForceCloseHook

- PROOF_ID: `PROOF_REVIEWED_SystemSecurityAndSystemHooks_kt_ForceCloseHook`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_forceclose_apps` and rewrite host members ['<init>']. Scaffolding-stripped body ratio=0.760. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.714; insert: A14`` A13`@Suppress("UNCHECKED_CAST") `; replace: A14`intercept(chain` A13`after(param`; insert: A14`` A13`AfterHookCallback) { val mSystemKeyPackages = `; replace: A14`Interface.Chain): Any? { var result: Any? var throwable: Throwable? = null try { result = ` A13`Helpers.get`; replace: A14` = chain` A13`Field(param`; replace: A14` val mSystemKeyPackages = Xposed` A13`, "mSystemKeyPackages") as? `
- VALUE_DEFAULT_COMPARISON: A14 `system_forceclose_apps` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=com.android.server.policy.BaseMiuiPhoneWindowManager#<init>; A13=com.android.server.policy.BaseMiuiPhoneWindowManager#<init>; shared_methods=['<init>']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_forceclose_apps` and rewrite host members ['<init>']. Scaffolding-stripped body ratio=0.760. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemAudioAndVisualAndMoreHooks_kt_GalleryScreenshotPathHook

- PROOF_ID: `PROOF_REVIEWED_SystemAudioAndVisualAndMoreHooks_kt_GalleryScreenshotPathHook`
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
- PREFERENCE_KEYS: `system_gallery_screenshots_path`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_gallery_screenshots_path` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.822. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.822; insert: A14`` A13`try { `; replace: A14`(` A13`IfExists( `; replace: A14`)` A13` ) ?: return`; replace: A14`r` A13`l`; replace: A14`"" if (folder ==` A13`when (folder) {`; replace: A14`) { ssPath =` A13` ->`
- VALUE_DEFAULT_COMPARISON: A14 `system_gallery_screenshots_path` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_gallery_screenshots_path` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.822. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_BatteryIndicator_kt_registerCallbacks

- PROOF_ID: `PROOF_REVIEWED_BatteryIndicator_kt_registerCallbacks`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_hidestatusbar_whenscreenshot` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.510. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.510; replace: A14`mStatusBar = statusBar try { val sha` A13`ModuleHel`; replace: A14` = Sha` A13`r.observePreferenceChange("systemui.batteryIndicator", this, `; replace: A14`eDrawable() val paint = shape.paint paint.style = Paint.Style.FILL paint.isAntiAlias = tru` A13`reference`; replace: A14`utOfMemoryError) { throw oom } catch (t: Throwable) { XposedHelpers.log(t) } updateParamet` A13`bserver`; delete: A14`if (broadcastReceiver == null) { broadcastReceiver = ` A13``; replace: A14`RECE` A13`"systemui.battery`
- VALUE_DEFAULT_COMPARISON: A14 `system_hidestatusbar_whenscreenshot` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_hidestatusbar_whenscreenshot` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.510. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_System_NoScreenLock_kt_openBtNetworks

- PROOF_ID: `PROOF_REVIEWED_System_NoScreenLock_kt_openBtNetworks`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_noscreenlock_bt` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.854. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.854; replace: A14`.apply { ` A13` args.`; delete: A14` }` A13``; delete: A14`App` A13``; insert: A14`` A13`s`; insert: A14`` A13`Helpers.`; replace: A14`pp` A13`ctionBarType.`
- VALUE_DEFAULT_COMPARISON: A14 `system_noscreenlock_bt` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_noscreenlock_bt` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.854. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_System_NoScreenLock_kt_openWifiNetworks

- PROOF_ID: `PROOF_REVIEWED_System_NoScreenLock_kt_openWifiNetworks`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_noscreenlock_wifi` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.946. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.946; replace: A14`.apply { ` A13` args.`; delete: A14` }` A13``; delete: A14`App` A13``; insert: A14`` A13`s`; delete: A14`App` A13``; insert: A14`` A13`s`
- VALUE_DEFAULT_COMPARISON: A14 `system_noscreenlock_wifi` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_noscreenlock_wifi` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.946. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_System_ScreenshotConfig_kt_onActivityResult

- PROOF_ID: `PROOF_REVIEWED_System_ScreenshotConfig_kt_onActivityResult`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_screenshot_mypath` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.336. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.336; replace: A14`r` A13`l`; replace: A14`a` A13`requireA`; insert: A14`` A13`(), data?.data)`; replace: A14`return` A13`"" findPreference<Preference>("pref_key_system_screenshot_mypath")?.summary = dir AppHelpe`; replace: A14`ata?.data) if (dir == null) dir = "" findPreference<Preference>("pref_key_system_screensho` A13`ir)?.apply() } super.on`; replace: A14`ppHelper.appPrefs!!.edit().putString("pref_key_system_screenshot_mypath"` A13`ctivityResult(requestCode`
- VALUE_DEFAULT_COMPARISON: A14 `system_screenshot_mypath` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_screenshot_mypath` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.336. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemUIControlCenterHooks_kt_initPct

- PROOF_ID: `PROOF_REVIEWED_SystemUIControlCenterHooks_kt_initPct`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_SYMBOL: `applyPctTopMargin`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_SYMBOL: `initPct`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_showpct_top`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `28`; A13 default `28`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_showpct_top` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.133. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.133; replace: A14`lp = (pct.layoutParams as?` A13`res = context.resources if (mPct == null) { mPct = TextView(container.context) mPct!!.setT`; replace: A14`) ?: return` A13`(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT) lp.topMargi`; replace: A14`lp.topMargin = i` A13`mPct!!.setPadding(Math.round(20 * density), Math.round(10 * density), Math.round(18 * dens`; replace: A14` (source == P` A13`ace_variant, context.theme)) mPct!!.background = Resources`; replace: A14`T` A13`ompat.getDrawable(modRes, R.drawable.input`; replace: A14`S` A13`background, context.theme) } catch (err: Throwable) { if (err is `
- VALUE_DEFAULT_COMPARISON: A14 `system_showpct_top` default=28; A13 default=28
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_showpct_top` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.133. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_DeviceInfoMonitor_kt_readSnapshot

- PROOF_ID: `PROOF_REVIEWED_DeviceInfoMonitor_kt_readSnapshot`
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
- PREFERENCE_KEYS: `system_statusbar_batterytempandcurrent_content`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_statusbar_batterytempandcurrent_content` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.366. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.366; replace: A14`val` A13`return Snapshot(`; replace: A14`mPrefs.get` A13`show`; replace: A14`oolean("system_statusbar_batterytempandcurrent") val` A13`atteryDetail,`; delete: A14`mPrefs.getBoolean("system_statusbar_showdevicetemperature") val placement = resolveDeviceI` A13``; delete: A14` = showDeviceTemp, dualRows = mPrefs.getBoolean("system_statusbar_dualrows"), batteryAtRig` A13``; replace: A14`mP` A13`p`
- VALUE_DEFAULT_COMPARISON: A14 `system_statusbar_batterytempandcurrent_content` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=['system_statusbar_batterytempandcurrent', 'system_statusbar_batterytempandcurrent_atright', 'system_statusbar_dualrows', 'system_statusbar_showdevicetemperature', 'system_statusbar_showdevicetemperature_atright']; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_statusbar_batterytempandcurrent_content` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.366. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_SystemUIStatusBarHooks_kt_StatusBarClockPositionHook

- PROOF_ID: `PROOF_REVIEWED_SystemUIStatusBarHooks_kt_StatusBarClockPositionHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `StatusBarClockPositionHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.PhoneStatusBarView#updateLayoutForCutout,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateNotificationIconAreaInnnerParent`
- A14_CALLBACK_PHASE: `after,before`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `StatusBarClockPositionHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.PhoneStatusBarView#updateLayoutForCutout,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateNotificationIconAreaInnnerParent`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `system_statusbar_clock_position`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_statusbar_clock_position` and rewrite host members ['onFinishInflate', 'updateLayoutForCutout', 'updateNotificationIconAreaInnnerParent']. Scaffolding-stripped body ratio=0.652. Callback delta is A14 `after,before` vs A13 `after,before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.627; insert: A14`` A13`?`; replace: A14`if (XposedHelpers.getAdditionalInstanceField(` A13`?: return val mContext = `; replace: A14`, "clockPositionInitialized") != null) return` A13`.context`; insert: A14`` A13`res = `; replace: A14` = sbView.context` A13`.resources`; insert: A14`` A13`Miui`
- VALUE_DEFAULT_COMPARISON: A14 `system_statusbar_clock_position` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.PhoneStatusBarView#updateLayoutForCutout,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateNotificationIconAreaInnnerParent; A13=com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onFinishInflate,com.android.systemui.statusbar.phone.PhoneStatusBarView#updateLayoutForCutout,com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#updateNotificationIconAreaInnnerParent; shared_methods=['onFinishInflate', 'updateLayoutForCutout', 'updateNotificationIconAreaInnnerParent']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=after,before; A13 phases=after,before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_statusbar_clock_position` and rewrite host members ['onFinishInflate', 'updateLayoutForCutout', 'updateNotificationIconAreaInnnerParent']. Scaffolding-stripped body ratio=0.652. Callback delta is A14 `after,before` vs A13 `after,before`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_SystemUIStatusBarHooks_kt_checkSlot

- PROOF_ID: `PROOF_REVIEWED_SystemUIStatusBarHooks_kt_checkSlot`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_SYMBOL: `buildStatusBarIconVisibilitySnapshot`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
- A13_SYMBOL: `checkSlot`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `system_statusbaricons_airplane`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_statusbaricons_airplane` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.034. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.034; replace: A14`Status` A13`try { ("headset" == slotName && MainModule.mPrefs.get`; replace: A14`arIconVisibilitySna` A13`oolean("system_statusbaricons_headset")) || ("wireless_headset" == slotName && MainModule.`; replace: A14`shot( id = idGenerator.incrementAndGet(), hideHeadset = ` A13`rofile" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_`; replace: A14`efs.getBoolean("system_statusbaricons_headset"), hideSound = prefs.getBoolean("system_stat` A13`ofile")) || ("`; replace: A14`), ` A13` == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_vpn")) || ("airplane" `; replace: A14`ideAir` A13`ots`
- VALUE_DEFAULT_COMPARISON: A14 `system_statusbaricons_airplane` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=['system_statusbaricons_bluetoothicn', 'system_statusbaricons_mute', 'system_statusbaricons_privacy', 'system_statusbaricons_record', 'system_statusbaricons_roaming', 'system_statusbaricons_signal', 'system_statusbaricons_signal_wificonnected', 'system_statusbaricons_sim1', 'system_statusbaricons_sim2', 'system_statusbaricons_sim_nodata', 'system_statusbaricons_speaker']; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_statusbaricons_airplane` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.034. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_SystemStatusBarMoreHooks_kt_DisplayWifiStandardHook

- PROOF_ID: `PROOF_REVIEWED_SystemStatusBarMoreHooks_kt_DisplayWifiStandardHook`
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
- PREFERENCE_KEYS: `system_statusbaricons_wifistandard`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_statusbaricons_wifistandard` and rewrite host members ['applyWifiState']. Scaffolding-stripped body ratio=0.813. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.686; replace: A14`intercept(chain` A13`before(param`; replace: A14`XposedInterface.` A13`BeforeHook`; replace: A14`hain): Any?` A13`allback)`; replace: A14`r result: Any? = null var throwable: Throwable? = null try { val wifiState = chain` A13`l wifiState = param`; replace: A14`{ return` A13`return val wifiStandard =`; replace: A14`proceed` A13`get`
- VALUE_DEFAULT_COMPARISON: A14 `system_statusbaricons_wifistandard` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=com.android.systemui.statusbar.StatusBarWifiView#applyWifiState; A13=com.android.systemui.statusbar.StatusBarWifiView#applyWifiState; shared_methods=['applyWifiState']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_statusbaricons_wifistandard` and rewrite host members ['applyWifiState']. Scaffolding-stripped body ratio=0.813. Callback delta is A14 `intercept` vs A13 `before`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_AudioVisualizer_kt_shouldDisplayAudioVisualizer

- PROOF_ID: `PROOF_REVIEWED_AudioVisualizer_kt_shouldDisplayAudioVisualizer`
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
- PREFERENCE_KEYS: `system_visualizer_animdur`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `65`; A13 default `65`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `system_visualizer_animdur` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.094. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.094; replace: A14`if (detached) return@` A13`enum class BarStyle { DUMMY, SOLID, SOLID_ROUNDED, DASHED, CIRCLES, LINE } enum class Colo`; replace: A14`uarded when (` A13`er() private fun createWor`; replace: A14`y)` A13`r(role: String): ThreadPoolExecutor = ThreadPoolExecutor( 1, 1, 15L, TimeUnit.SECONDS, Lin`; replace: A14`"system_visualizer_animdur"` A13`runnable`; insert: A14`` A13`Thread( { Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND) runnable.run() }, `; insert: A14`` A13`} `
- VALUE_DEFAULT_COMPARISON: A14 `system_visualizer_animdur` default=65; A13 default=65
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `system_visualizer_animdur` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.094. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_Various_kt_checkBundle

- PROOF_ID: `PROOF_REVIEWED_Various_kt_checkBundle`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `checkBundle`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `(no host hook members)`
- A14_CALLBACK_PHASE: `unknown`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `checkBundle`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/installers/SecurityCenterInstaller.java`
- A13_HOOK_TARGETS: `(no host hook members)`
- A13_CALLBACK_PHASE: `unknown`
- PREFERENCE_KEYS: `various_appsort`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `1`; A13 default `1`
- RESULT/ARGUMENT_BEHAVIOR: no result/argument rewrite literals | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `various_appsort` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.928. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.928; replace: A14`l b` A13`r newBundle`; replace: A14`b` A13`newBundle`; replace: A14`b` A13`newBundle`; replace: A14`b` A13`newBundle`
- VALUE_DEFAULT_COMPARISON: A14 `various_appsort` default=1; A13 default=1
- HOOK_TARGET_COMPARISON: A14=none; A13=none; shared_methods=n/a
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=unknown; A13 phases=unknown; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 no result/argument rewrite literals; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `various_appsort` in module/settings code with no ROM member dump required. Scaffolding-stripped body ratio=0.928. Callback delta is A14 `unknown` vs A13 `unknown`. No opposite setResult/returnAndSkip polarity on this pair.
- PROOF_CONCLUSION: `PRESENT_A13_VARIANT`

## PROOF_REVIEWED_Various_kt_InCallBrightnessHook

- PROOF_ID: `PROOF_REVIEWED_Various_kt_InCallBrightnessHook`
- BODY_RELATION: `REVIEWED_VARIANT`
- A14_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_SYMBOL: `InCallBrightnessHook`
- A14_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A14_HOOK_TARGETS: `com.android.incallui.InCallActivity#onCreate`
- A14_CALLBACK_PHASE: `intercept,before,after`
- A13_OWNER_PATH: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_SYMBOL: `InCallBrightnessHook`
- A13_INSTALLER: `app/src/main/java/tv/withaibuild/customiuizer/mods/Various.kt`
- A13_HOOK_TARGETS: `com.android.incallui.InCallActivity#onCreate`
- A13_CALLBACK_PHASE: `after,before`
- PREFERENCE_KEYS: `various_calluibright_night`
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `n/a`; A13 default `n/a`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null]; chain.proceed[chain.proceed()] | no result/argument rewrite literals
- API33_VARIANT_REASON: Both owners read `various_calluibright_night` and rewrite host members ['onCreate']. Scaffolding-stripped body ratio=0.691. Callback delta is A14 `intercept,before,after` vs A13 `after,before`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.585; replace: A14`intercept(chain` A13`after(param`; replace: A14`XposedInterface.` A13`AfterHook`; replace: A14`hain): Any?` A13`allback)`; replace: A14`r result: Any? var throwable: Throwable? = null try { result = chain.proceed() } catch (t:` A13`l act = param.`; replace: A14`= chain.thisObject val act = thisObject as` A13`as?`; replace: A14`val` A13`?: return var`
- VALUE_DEFAULT_COMPARISON: A14 `various_calluibright_night` default=n/a; A13 default=n/a
- HOOK_TARGET_COMPARISON: A14=com.android.incallui.InCallActivity#onCreate; A13=com.android.incallui.InCallActivity#onCreate; shared_methods=['onCreate']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept,before,after; A13 phases=after,before; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null]; chain.proceed[chain.proceed()]; A13 no result/argument rewrite literals
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `various_calluibright_night` and rewrite host members ['onCreate']. Scaffolding-stripped body ratio=0.691. Callback delta is A14 `intercept,before,after` vs A13 `after,before`. No opposite setResult/returnAndSkip polarity on this pair.
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

## PROOF_REVIEWED_Various_kt_ShowCallUIHook

- PROOF_ID: `PROOF_REVIEWED_Various_kt_ShowCallUIHook`
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
- VALUE_DOMAIN: owner-local preference domain
- DEFAULT_SEMANTICS: A14 default `0`; A13 default `0`
- RESULT/ARGUMENT_BEHAVIOR: result_assign[null,true]; chain.proceed[chain.proceed()] | setResult[true]
- API33_VARIANT_REASON: Both owners read `various_showcallui` and rewrite host members ['startUi']. Scaffolding-stripped body ratio=0.812. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
- DIFF_SUMMARY: normalized_ratio=0.615; replace: A14`intercept(chain` A13`after(param`; replace: A14`XposedInterface.` A13`AfterHook`; replace: A14`hain): Any` A13`allback) { if ((param.result as`; replace: A14`{ var result: Any? var throwable: Throwable? = null try { result = chain.proceed() } catch` A13`Boolean `; replace: A14`(result as Boolean` A13`= true`; replace: A14`chain.getArg(` A13`param.args[`
- VALUE_DEFAULT_COMPARISON: A14 `various_showcallui` default=0; A13 default=0
- HOOK_TARGET_COMPARISON: A14=com.android.incallui.InCallPresenter#startUi; A13=com.android.incallui.InCallPresenter#startUi; shared_methods=['startUi']
- CALLBACK_SEMANTICS_COMPARISON: A14 phases=intercept; A13 phases=after; intercept/proceed vs before/after is an API33 libxposed translation when polarity matches
- ARG_RESULT_COMPARISON: A14 result_assign[null,true]; chain.proceed[chain.proceed()]; A13 setResult[true]
- A14_ONLY_BRANCHES: extra_keys=none; extra_hook_targets=none
- WHY_USER_BEHAVIOR_IS_EQUIVALENT: Both owners read `various_showcallui` and rewrite host members ['startUi']. Scaffolding-stripped body ratio=0.812. Callback delta is A14 `intercept` vs A13 `after`. No opposite setResult/returnAndSkip polarity on this pair.
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
