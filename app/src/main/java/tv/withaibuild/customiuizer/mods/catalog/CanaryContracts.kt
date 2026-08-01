package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.utils.AnyOfRequirement
import tv.withaibuild.customiuizer.mods.utils.Criticality
import tv.withaibuild.customiuizer.mods.utils.FeatureTargetVariant
import tv.withaibuild.customiuizer.mods.utils.HookOperation
import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.mods.utils.HookTargetSpec
import tv.withaibuild.customiuizer.mods.utils.SingleTargetRequirement

/**
 * Typed target contracts for the 8 canary features.
 *
 * Each contract is derived from the actual hook calls in the corresponding
 * legacy installer. The contract is used for compatibility probing (via
 * [tv.withaibuild.customiuizer.mods.utils.HookTargetResolver.evaluateContract])
 * and for real install evidence (via
 * [tv.withaibuild.customiuizer.mods.utils.HookInstaller.withSession]).
 */
object CanaryContracts {

    private val FLOAT = Float::class.javaPrimitiveType!!
    private val INT = Int::class.javaPrimitiveType!!
    private val LONG = Long::class.javaPrimitiveType!!
    private val BOOLEAN = Boolean::class.javaPrimitiveType!!

    val packagePermissions: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "packagePermissions",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "PermissionManagerServiceImpl.shouldGrantPermissionBySignature",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.pm.permission.PermissionManagerServiceImpl",
                    memberName = "shouldGrantPermissionBySignature"
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
                    id = "ApplicationInfo.isSystemApp",
                    operation = HookOperation.EXACT_METHOD,
                    className = "android.content.pm.ApplicationInfo",
                    memberName = "isSystemApp",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ApplicationInfo.isSignedWithPlatformKey",
                    operation = HookOperation.EXACT_METHOD,
                    className = "android.content.pm.ApplicationInfo",
                    memberName = "isSignedWithPlatformKey",
                    parameterTypes = emptyList()
                ),
                criticality = Criticality.OPTIONAL
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "ActivityRecordInjector.canShowWhenLocked",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.wm.ActivityRecordInjector",
                    memberName = "canShowWhenLocked"
                ),
                criticality = Criticality.OPTIONAL
            )
        )
    )
    }

    private val autoBrightnessAbcVariant = FeatureTargetVariant(
        id = "automatic_brightness_controller",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AutomaticBrightnessController.clampScreenBrightness",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.display.AutomaticBrightnessController",
                    memberName = "clampScreenBrightness",
                    parameterTypes = listOf(FLOAT)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "AutomaticBrightnessController.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.server.display.AutomaticBrightnessController"
                )
            )
        )
    )

    private val autoBrightnessDpcVariant = FeatureTargetVariant(
        id = "display_power_controller",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "DisplayPowerController.clampScreenBrightness",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.server.display.DisplayPowerController",
                    memberName = "clampScreenBrightness",
                    parameterTypes = listOf(FLOAT)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "DisplayPowerController.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.server.display.DisplayPowerController"
                )
            )
        )
    )

    val autoBrightnessRange: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "autoBrightnessRange",
        variants = listOf(autoBrightnessAbcVariant, autoBrightnessDpcVariant)
    )
    }

    val muffledVibration: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "muffledVibration",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "VibratorService.doVibratorOn",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.server.VibratorService",
                    memberName = "doVibratorOn"
                )
            )
        )
    )
    }

    val statusBarClockTweak: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "statusBarClockTweak",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiStatusBarClockController.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.systemui.statusbar.policy.MiuiStatusBarClockController"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiStatusBarClockController.fireTimeChange",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.policy.MiuiStatusBarClockController",
                    memberName = "fireTimeChange",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiClock.constructors",
                    operation = HookOperation.ALL_CONSTRUCTORS,
                    className = "com.android.systemui.statusbar.views.MiuiClock"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiClock.updateTime",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.views.MiuiClock",
                    memberName = "updateTime",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiClock.setClockVisibility",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.views.MiuiClock",
                    memberName = "setClockVisibility",
                    parameterTypes = listOf(INT)
                ),
                criticality = Criticality.OPTIONAL
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiPhoneStatusBarView.onAttachedToWindow",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
                    memberName = "onAttachedToWindow",
                    parameterTypes = emptyList()
                ),
                criticality = Criticality.OPTIONAL
            )
        )
    )
    }

    val noMoreIcon: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noMoreIcon",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NotificationIconAreaController.setIconsVisibility",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.phone.NotificationIconAreaController",
                    memberName = "setIconsVisibility",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val batteryIndicator: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "batteryIndicator",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "CentralSurfacesImpl.createAndAddWindows",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                    memberName = "createAndAddWindows"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "CentralSurfacesImpl.setPanelExpanded",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                    memberName = "setPanelExpanded",
                    parameterTypes = listOf(BOOLEAN)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "CentralSurfacesImpl.setQsExpanded",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                    memberName = "setQsExpanded",
                    parameterTypes = listOf(BOOLEAN)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "CentralSurfacesImpl.updateIsKeyguard",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                    memberName = "updateIsKeyguard",
                    parameterTypes = listOf(BOOLEAN)
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "NotificationIconAreaController.onDarkChanged",
                    operation = HookOperation.ALL_METHODS_BY_NAME,
                    className = "com.android.systemui.statusbar.phone.NotificationIconAreaController",
                    memberName = "onDarkChanged"
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "MiuiBatteryControllerImpl.fireBatteryLevelChanged",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl",
                    memberName = "fireBatteryLevelChanged",
                    parameterTypes = emptyList()
                )
            ),
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "BatteryControllerImpl.firePowerSaveChanged",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.android.systemui.statusbar.policy.BatteryControllerImpl",
                    memberName = "firePowerSaveChanged",
                    parameterTypes = emptyList()
                )
            )
        )
    )
    }

    val noClockHide: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noClockHide",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "Launcher.updateStatusBarClock",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.home.launcher.Launcher",
                    memberName = "updateStatusBarClock",
                    parameterTypes = listOf(LONG)
                )
            )
        )
    )
    }

    val noWidgetOnly: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
        featureId = "noWidgetOnly",
        requirements = listOf(
            SingleTargetRequirement(
                target = HookTargetSpec(
                    id = "CellLayout.setScreenType",
                    operation = HookOperation.EXACT_METHOD,
                    className = "com.miui.home.launcher.CellLayout",
                    memberName = "setScreenType",
                    parameterTypes = listOf(INT)
                )
            )
        )
    )
    }
}
