package tv.withaibuild.customiuizer.mods.catalog

import android.content.Context
import android.content.Intent
import tv.withaibuild.customiuizer.mods.utils.AnyOfRequirement
import tv.withaibuild.customiuizer.mods.utils.Criticality
import tv.withaibuild.customiuizer.mods.utils.HookOperation
import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.mods.utils.HookTargetSpec
import tv.withaibuild.customiuizer.mods.utils.SingleTargetRequirement

/**
 * Typed target contracts for the first catalog expansion batch.
 *
 * Each contract is derived from the real `ModuleHelper` calls in the legacy
 * installer. They are used by [tv.withaibuild.customiuizer.mods.utils.HookTargetResolver]
 * for compatibility probing and by [tv.withaibuild.customiuizer.mods.utils.HookInstaller]
 * for real install evidence.
 */
object CatalogContracts {

    private val INT = Int::class.javaPrimitiveType!!
    private val BOOLEAN = Boolean::class.javaPrimitiveType!!
    private val LONG = Long::class.javaPrimitiveType!!
    private val STRING = String::class.java

    val screenDimTime: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "screenDimTime",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PowerManagerService.readConfigurationLocked",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.power.PowerManagerService",
                    memberName = "readConfigurationLocked",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PowerManagerService.setStayOnSettingInternal",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.power.PowerManagerService",
                    memberName = "setStayOnSettingInternal",
                    parameterTypes = listOf(INT)
                )
            )
        )
    )
    }

    val firstVolumePress: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "firstVolumePress",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AudioService.VolumeController.suppressAdjustment",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.audio.AudioService\$VolumeController",
                    memberName = "suppressAdjustment",
                    parameterTypes = listOf(INT, INT, BOOLEAN)
                )
            )
        )
    )
    }

    val networkIndicatorWifi: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "networkIndicatorWifi",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "StatusBarWifiView.applyWifiState",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.systemui.statusbar.StatusBarWifiView",
                    memberName = "applyWifiState"
                )
            )
        )
    )
    }

    val muteVisibleNotifications: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "muteVisibleNotifications",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NotificationAlertController.buzzBeepBlink",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.systemui.statusbar.notification.policy.NotificationAlertController",
                    memberName = "buzzBeepBlink"
                )
            )
        )
    )
    }

    val hideLauncherTitles: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "hideLauncherTitles",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ItemIcon.onFinishInflate",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.home.launcher.ItemIcon",
                    memberName = "onFinishInflate",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val fixAppInfoLaunch: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "fixAppInfoLaunch",
        requirements = listOf(
            AnyOfRequirement(
                id = "fixAppInfoLaunch.launcher",
                criticality = Criticality.REQUIRED,
                candidates = listOf(
                    HookTargetSpec(
                        id = "ShortcutMenuManager.startAppDetailsActivity",
                        operation = HookOperation.ALL_METHODS_BY_NAME,
                        className = "com.miui.home.launcher.shortcuts.ShortcutMenuManager",
                        memberName = "startAppDetailsActivity"
                    ),
                    HookTargetSpec(
                        id = "Utilities.startDetailsActivityForInfo",
                        operation = HookOperation.ALL_METHODS_BY_NAME,
                        className = "com.miui.home.launcher.util.Utilities",
                        memberName = "startDetailsActivityForInfo"
                    )
                )
            )
        )
    )
    }

    // Catalog expansion batch 2: system_server
    val volumeSteps: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "volumeSteps",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AudioService.createStreamStates",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.audio.AudioService",
                    memberName = "createStreamStates",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val toastTime: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "toastTime",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NotificationManagerService.showNextToastLocked",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.notification.NotificationManagerService",
                    memberName = "showNextToastLocked",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val hideProximityWarning: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "hideProximityWarning",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiScreenOnProximityLock.showHint",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.policy.MiuiScreenOnProximityLock",
                    memberName = "showHint",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiScreenOnProximityLock.prepareHintWindow",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.policy.MiuiScreenOnProximityLock",
                    memberName = "prepareHintWindow",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val clearAllTasks: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "clearAllTasks",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "WindowProcessUtils.getPerceptibleRecentAppList",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.wm.WindowProcessUtils",
                    memberName = "getPerceptibleRecentAppList"
                )
            )
        )
    )
    }

    // Catalog expansion batch 2: SystemUI
    val hideDismissView: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "hideDismissView",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiNotificationPanelViewController.updateDismissView",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController",
                    memberName = "updateDismissView",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val hideLockScreenHint: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "hideLockScreenHint",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "KeyguardIndicationRotateTextViewController.hasIndicationsExceptResting",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController",
                    memberName = "hasIndicationsExceptResting",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    // Catalog expansion batch 2: Launcher
    val folderColumns: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "folderColumns",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "Folder.onFinishInflate",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.home.launcher.Folder",
                    memberName = "onFinishInflate",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "Folder.onLayout",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.miui.home.launcher.Folder",
                    memberName = "onLayout"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "Folder.resetViewsLayoutParams",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.home.launcher.Folder",
                    memberName = "resetViewsLayoutParams",
                    parameterTypes = emptyList()
                ),
                criticality = Criticality.OPTIONAL
            )
        )
    )
    }

    val titleTopMargin: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "titleTopMargin",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ItemIcon.onFinishInflate.titleTopMargin",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.home.launcher.ItemIcon",
                    memberName = "onFinishInflate",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    // Catalog expansion batch 3: system_server
    val noLightUpOnCharge: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noLightUpOnCharge",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PowerManagerService.wakePowerGroupLocked",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.power.PowerManagerService",
                    memberName = "wakePowerGroupLocked"
                )
            )
        )
    )
    }

    val allRotations: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "allRotations",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "DisplayRotation.<init>",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.server.wm.DisplayRotation",
                    memberName = "<init>"
                )
            )
        )
    )
    }

    // Catalog expansion batch 3: SystemUI
    val noNetworkSpeedSeparator: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noNetworkSpeedSeparator",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NetworkSpeedSplitter.onClockVisibilityChanged",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.views.NetworkSpeedSplitter",
                    memberName = "onClockVisibilityChanged",
                    parameterTypes = listOf(INT)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NetworkSpeedSplitter.onNetworkSpeedVisibilityChanged",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.views.NetworkSpeedSplitter",
                    memberName = "onNetworkSpeedVisibilityChanged",
                    parameterTypes = listOf(INT)
                )
            )
        )
    )
    }

    val hideIconsClock: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "hideIconsClock",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiCollapsedStatusBarFragment.showClock",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment",
                    memberName = "showClock",
                    parameterTypes = listOf(BOOLEAN)
                )
            )
        )
    )
    }

    // Catalog expansion batch 3: Launcher
    val noUnlockAnimation: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noUnlockAnimation",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiSettingsUtils.isSystemAnimationOpen",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.miui.launcher.utils.MiuiSettingsUtils",
                    memberName = "isSystemAnimationOpen"
                )
            )
        )
    )
    }

    // Catalog expansion batch 4: SystemUI screenshot
    val tempHideOverlaySystemUI: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "tempHideOverlaySystemUI",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PipTaskOrganizer.onTaskAppeared",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.wm.shell.pip.PipTaskOrganizer",
                    memberName = "onTaskAppeared"
                )
            )
        )
    )
    }

    val hideStatusBarBeforeScreenshot: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "hideStatusBarBeforeScreenshot",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiCollapsedStatusBarFragment.initMiuiViewsOnViewCreated",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment",
                    memberName = "initMiuiViewsOnViewCreated"
                )
            )
        )
    )
    }

    val hideNavBarBeforeScreenshot: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "hideNavBarBeforeScreenshot",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NavigationBar.onInit",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.systemui.navigationbar.NavigationBar",
                    memberName = "onInit"
                )
            )
        )
    )
    }

    val cleanShareMenu: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "cleanShareMenu",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ResolverActivityRunner.run",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner",
                    memberName = "run"
                )
            )
        )
    )
    }

    val cleanShareMenuService: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "cleanShareMenuService",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ComputerEngine.queryIntentActivitiesInternal",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.ComputerEngine",
                    memberName = "queryIntentActivitiesInternal"
                )
            )
        )
    )
    }

    val cleanOpenWithMenu: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "cleanOpenWithMenu",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ResolverActivityRunner.run",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner",
                    memberName = "run"
                )
            )
        )
    )
    }

    val cleanOpenWithMenuService: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "cleanOpenWithMenuService",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ComputerEngine.queryIntentActivitiesInternal",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.ComputerEngine",
                    memberName = "queryIntentActivitiesInternal"
                )
            )
        )
    )
    }

    val chargingInfo: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "chargingInfo",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ChargeUtils.getChargingHintText",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.keyguard.charge.ChargeUtils",
                    memberName = "getChargingHintText"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "KeyguardIndicationTextView.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.systemui.statusbar.phone.KeyguardIndicationTextView"
                )
            )
        )
    )
    }

    val setLockscreenWallpaper: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "setLockscreenWallpaper",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "WallpaperManagerService.setWallpaper",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.wallpaper.WallpaperManagerService",
                    memberName = "setWallpaper"
                )
            )
        )
    )
    }

    val noVersionCheck: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noVersionCheck",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PackageManagerServiceUtils.checkDowngrade",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.PackageManagerServiceUtils",
                    memberName = "checkDowngrade"
                )
            )
        )
    )
    }

    val removeActStartConfirm: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "removeActStartConfirm",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "SecurityManagerService.checkAllowStartActivity",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.miui.server.SecurityManagerService",
                    memberName = "checkAllowStartActivity"
                )
            )
        )
    )
    }

    val forceClose: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "forceClose",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "BaseMiuiPhoneWindowManager.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.server.policy.BaseMiuiPhoneWindowManager"
                )
            )
        )
    )
    }

    val disableSystemIntegrity: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "disableSystemIntegrity",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ApkSignatureVerifier.getMinimumSignatureSchemeVersionForTargetSdk",
                    operation = HookOperation.EXACT_METHOD,
                    className = "android.util.apk.ApkSignatureVerifier",
                    memberName = "getMinimumSignatureSchemeVersionForTargetSdk",
                    parameterTypes = listOf(INT)
                )
            )
        )
    )
    }

    val orientationLock: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "orientationLock",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "DisplayRotation.rotationForOrientation",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.wm.DisplayRotation",
                    memberName = "rotationForOrientation"
                )
            )
        )
    )
    }

    val noDucking: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noDucking",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "FocusRequester.handleFocusLoss",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.audio.FocusRequester",
                    memberName = "handleFocusLoss"
                )
            )
        )
    )
    }

    val disable72hStrongAuth: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "disable72hStrongAuth",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "LockSettingsStrongAuth.rescheduleStrongAuthTimeoutAlarm",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.locksettings.LockSettingsStrongAuth",
                    memberName = "rescheduleStrongAuthTimeoutAlarm",
                    parameterTypes = listOf(LONG, INT)
                )
            )
        )
    )
    }

    val disableAnyNotificationBlock: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "disableAnyNotificationBlock",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NotificationChannel.isBlockable",
                    operation = HookOperation.EXACT_METHOD,
                    className = "android.app.NotificationChannel",
                    memberName = "isBlockable",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NotificationChannel.setBlockable",
                    operation = HookOperation.EXACT_METHOD,
                    className = "android.app.NotificationChannel",
                    memberName = "setBlockable",
                    parameterTypes = listOf(BOOLEAN)
                )
            )
        )
    )
    }

    val enhancedSecurity: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "enhancedSecurity",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PhoneWindowManager.interceptPowerKeyDown",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.policy.PhoneWindowManager",
                    memberName = "interceptPowerKeyDown"
                ),
                criticality = Criticality.OPTIONAL
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PhoneWindowManager.powerLongPress",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.policy.PhoneWindowManager",
                    memberName = "powerLongPress"
                ),
                criticality = Criticality.OPTIONAL
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PhoneWindowManager.showGlobalActions",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.policy.PhoneWindowManager",
                    memberName = "showGlobalActions",
                    parameterTypes = emptyList()
                ),
                criticality = Criticality.OPTIONAL
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PhoneWindowManager.showGlobalActionsInternal",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.policy.PhoneWindowManager",
                    memberName = "showGlobalActionsInternal",
                    parameterTypes = emptyList()
                ),
                criticality = Criticality.OPTIONAL
            )
        )
    )
    }

    val appLock: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "appLock",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "SecurityManagerService.removeAccessControlPassLocked",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.miui.server.SecurityManagerService",
                    memberName = "removeAccessControlPassLocked"
                )
            )
        )
    )
    }

    val skipAppLock: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "skipAppLock",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AccessController.skipActivity",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.miui.server.AccessController",
                    memberName = "skipActivity"
                )
            )
        )
    )
    }

    val noCallInterruption: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noCallInterruption",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AudioService.requestAudioFocus",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.audio.AudioService",
                    memberName = "requestAudioFocus"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "TelephonyRegistry.notifyCallState",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.TelephonyRegistry",
                    memberName = "notifyCallState",
                    parameterTypes = listOf(INT, STRING)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "TelephonyRegistry.notifyCallStateForPhoneId",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.TelephonyRegistry",
                    memberName = "notifyCallStateForPhoneId",
                    parameterTypes = listOf(INT, INT, INT, STRING)
                )
            )
        )
    )
    }

    val removeSecure: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "removeSecure",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "WindowState.isSecureLocked",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.wm.WindowState",
                    memberName = "isSecureLocked",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "WindowSurfaceController.setSecure",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.wm.WindowSurfaceController",
                    memberName = "setSecure",
                    parameterTypes = listOf(BOOLEAN)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "WindowSurfaceController.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.server.wm.WindowSurfaceController"
                )
            )
        )
    )
    }

    val noSignatureVerify: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noSignatureVerify",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "SigningDetails.checkCapability",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "android.content.pm.SigningDetails",
                    memberName = "checkCapability"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "StrictJarVerifier.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "android.util.jar.StrictJarVerifier"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "StrictJarVerifier.verifyMessageDigest",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "android.util.jar.StrictJarVerifier",
                    memberName = "verifyMessageDigest"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "StrictJarVerifier.verify",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "android.util.jar.StrictJarVerifier",
                    memberName = "verify"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PackageManagerServiceUtils.verifySignatures",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.PackageManagerServiceUtils",
                    memberName = "verifySignatures"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "InstallPackageHelper.doesSignatureMatchForPermissions",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.InstallPackageHelper",
                    memberName = "doesSignatureMatchForPermissions"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "InstallPackageHelper.cannotInstallWithBadPermissionGroups",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.InstallPackageHelper",
                    memberName = "cannotInstallWithBadPermissionGroups"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PermissionManagerServiceImpl.shouldGrantPermissionBySignature",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.permission.PermissionManagerServiceImpl",
                    memberName = "shouldGrantPermissionBySignature"
                )
            )
        )
    )
    }

    val noDarkForce: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noDarkForce",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "UiModeManagerService.setForceDark",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.UiModeManagerService",
                    memberName = "setForceDark",
                    parameterTypes = listOf(Context::class.java)
                ),
                criticality = Criticality.OPTIONAL
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "SecurityManagerService.getAppDarkModeForUser",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.server.SecurityManagerService",
                    memberName = "getAppDarkModeForUser",
                    parameterTypes = listOf(STRING, INT)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "DarkModeAppSettingsInfo.getOverrideEnableValue",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.DarkModeAppSettingsInfo",
                    memberName = "getOverrideEnableValue",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val stickyFloatingWindows: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "stickyFloatingWindows",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ActivityStarterInjector.modifyLaunchActivityOptionIfNeed",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.wm.ActivityStarterInjector",
                    memberName = "modifyLaunchActivityOptionIfNeed"
                )
            )
        )
    )
    }

    val screenAnim: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "screenAnim",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "DisplayPowerController.initialize",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.display.DisplayPowerController",
                    memberName = "initialize",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val rotationAnimation: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "rotationAnimation",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AppTransitionInjector.createAnimation",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.wm.AppTransitionInjector",
                    memberName = "createAnimation"
                )
            )
        )
    )
    }

    val notificationVolume: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "notificationVolume",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AudioService.updateStreamVolumeAlias",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.audio.AudioService",
                    memberName = "updateStreamVolumeAlias",
                    parameterTypes = listOf(BOOLEAN, STRING)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AudioService.VolumeStreamState.readSettings",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.audio.AudioService\$VolumeStreamState",
                    memberName = "readSettings",
                    parameterTypes = emptyList()
                ),
                criticality = Criticality.OPTIONAL
            )
        )
    )
    }

    val selectiveVibration: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "selectiveVibration",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "VibratorManagerService.systemReady",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.vibrator.VibratorManagerService",
                    memberName = "systemReady",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val wallpaperScaleLevel: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "wallpaperScaleLevel",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "WallpaperController.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.server.wm.WallpaperController"
                )
            )
        )
    )
    }

    val appsDisableService: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "appsDisableService",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PackageManagerServiceImpl.canBeDisabled",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.pm.PackageManagerServiceImpl",
                    memberName = "canBeDisabled",
                    parameterTypes = listOf(STRING, INT)
                )
            )
        )
    )
    }

    val noAccessDeviceLogsRequest: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noAccessDeviceLogsRequest",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "LogcatManagerService.onLogAccessRequested",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.logcat.LogcatManagerService",
                    memberName = "onLogAccessRequested"
                )
            )
        )
    )
    }

    val autoGroupNotifications: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "autoGroupNotifications",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "GroupHelper.adjustAutogroupingSummary",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.notification.GroupHelper",
                    memberName = "adjustAutogroupingSummary",
                    parameterTypes = listOf(INT, STRING, STRING, BOOLEAN)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "GroupHelper.adjustNotificationBundling",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.notification.GroupHelper",
                    memberName = "adjustNotificationBundling",
                    parameterTypes = listOf(List::class.java, BOOLEAN)
                )
            )
        )
    )
    }

    val appLockTimeout: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "appLockTimeout",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "SecurityManagerService.addAccessControlPassForUser",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.server.SecurityManagerService",
                    memberName = "addAccessControlPassForUser",
                    parameterTypes = listOf(STRING, INT)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "SecurityManagerService.checkAccessControlPassLocked",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.server.SecurityManagerService",
                    memberName = "checkAccessControlPassLocked",
                    parameterTypes = listOf(STRING, Intent::class.java, INT)
                )
            )
        )
    )
    }
}
