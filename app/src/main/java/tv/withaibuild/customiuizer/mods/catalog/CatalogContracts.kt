package tv.withaibuild.customiuizer.mods.catalog

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

    val screenDimTime = HookTargetContract(
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

    val firstVolumePress = HookTargetContract(
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

    val networkIndicatorWifi = HookTargetContract(
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

    val muteVisibleNotifications = HookTargetContract(
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

    val hideLauncherTitles = HookTargetContract(
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

    val fixAppInfoLaunch = HookTargetContract(
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

    // Catalog expansion batch 2: system_server
    val hideProximityWarning = HookTargetContract(
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

    val clearAllTasks = HookTargetContract(
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

    // Catalog expansion batch 2: SystemUI
    val hideDismissView = HookTargetContract(
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

    val hideLockScreenHint = HookTargetContract(
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

    // Catalog expansion batch 2: Launcher
    val folderColumns = HookTargetContract(
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
            )
        )
    )

    val titleTopMargin = HookTargetContract(
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

    // Catalog expansion batch 3: system_server
    val noLightUpOnCharge = HookTargetContract(
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

    val allRotations = HookTargetContract(
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

    // Catalog expansion batch 3: SystemUI
    val noNetworkSpeedSeparator = HookTargetContract(
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

    val hideIconsClock = HookTargetContract(
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

    // Catalog expansion batch 3: Launcher
    val noUnlockAnimation = HookTargetContract(
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

    val maxHotseatIconsCount = HookTargetContract(
        featureId = "maxHotseatIconsCount",
        requirements = listOf(
            AnyOfRequirement(
                id = "maxHotseatIconsCount.hotseat",
                criticality = Criticality.REQUIRED,
                candidates = listOf(
                    HookTargetSpec(
                        id = "DeviceConfig.getHotseatCount",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.miui.home.launcher.DeviceConfig",
                        memberName = "getHotseatCount",
                        parameterTypes = emptyList()
                    ),
                    HookTargetSpec(
                        id = "DeviceConfig.getHotseatMaxCount",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.miui.home.launcher.DeviceConfig",
                        memberName = "getHotseatMaxCount",
                        parameterTypes = emptyList()
                    )
                )
            )
        )
    )
}
