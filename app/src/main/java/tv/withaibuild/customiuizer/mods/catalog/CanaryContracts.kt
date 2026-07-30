package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.mods.utils.HookTargetKind
import tv.withaibuild.customiuizer.mods.utils.HookTargetSpec

/**
 * Typed target contracts for the 8 canary features.
 *
 * Each contract is derived from the actual hook calls in the corresponding
 * legacy installer. The contract is used for compatibility probing (via
 * [tv.withaibuild.customiuizer.mods.utils.evaluateContract]) and for real
 * install evidence (via [tv.withaibuild.customiuizer.mods.utils.HookInstaller]).
 */
object CanaryContracts {

    private val FLOAT = Float::class.javaPrimitiveType!!
    private val INT = Int::class.javaPrimitiveType!!
    private val LONG = Long::class.javaPrimitiveType!!
    private val BOOLEAN = Boolean::class.javaPrimitiveType!!

    val packagePermissions = HookTargetContract(
        featureId = "packagePermissions",
        required = listOf(
            HookTargetSpec(
                id = "PermissionManagerServiceImpl.shouldGrantPermissionBySignature",
                kind = HookTargetKind.METHOD,
                className = "com.android.server.pm.permission.PermissionManagerServiceImpl",
                memberName = "shouldGrantPermissionBySignature"
            ),
            HookTargetSpec(
                id = "PackageManagerServiceUtils.verifySignatures",
                kind = HookTargetKind.METHOD,
                className = "com.android.server.pm.PackageManagerServiceUtils",
                memberName = "verifySignatures"
            ),
            HookTargetSpec(
                id = "ApplicationInfo.isSystemApp",
                kind = HookTargetKind.METHOD,
                className = "android.content.pm.ApplicationInfo",
                memberName = "isSystemApp"
            ),
            HookTargetSpec(
                id = "ApplicationInfo.isSignedWithPlatformKey",
                kind = HookTargetKind.METHOD,
                className = "android.content.pm.ApplicationInfo",
                memberName = "isSignedWithPlatformKey",
                required = false
            )
        ),
        optional = listOf(
            HookTargetSpec(
                id = "ActivityRecordInjector.canShowWhenLocked",
                kind = HookTargetKind.METHOD,
                className = "com.android.server.wm.ActivityRecordInjector",
                memberName = "canShowWhenLocked",
                required = false
            )
        )
    )

    val autoBrightnessRange = HookTargetContract(
        featureId = "autoBrightnessRange",
        required = listOf(
            HookTargetSpec(
                id = "AutomaticBrightnessController.clampScreenBrightness",
                kind = HookTargetKind.METHOD,
                className = "com.android.server.display.AutomaticBrightnessController",
                memberName = "clampScreenBrightness",
                parameterTypes = listOf(FLOAT),
                required = true,
                fallbackGroup = "clamp",
                fallbackOrder = 0
            ),
            HookTargetSpec(
                id = "DisplayPowerController.clampScreenBrightness",
                kind = HookTargetKind.METHOD,
                className = "com.android.server.display.DisplayPowerController",
                memberName = "clampScreenBrightness",
                parameterTypes = listOf(FLOAT),
                required = true,
                fallbackGroup = "clamp",
                fallbackOrder = 1
            ),
            HookTargetSpec(
                id = "AutomaticBrightnessController.constructors",
                kind = HookTargetKind.CONSTRUCTOR,
                className = "com.android.server.display.AutomaticBrightnessController",
                required = true,
                fallbackGroup = "constructor",
                fallbackOrder = 0
            ),
            HookTargetSpec(
                id = "DisplayPowerController.constructors",
                kind = HookTargetKind.CONSTRUCTOR,
                className = "com.android.server.display.DisplayPowerController",
                required = true,
                fallbackGroup = "constructor",
                fallbackOrder = 1
            )
        )
    )

    val muffledVibration = HookTargetContract(
        featureId = "muffledVibration",
        required = listOf(
            HookTargetSpec(
                id = "VibratorService.doVibratorOn",
                kind = HookTargetKind.METHOD,
                className = "com.android.server.VibratorService",
                memberName = "doVibratorOn"
            )
        )
    )

    val statusBarClockTweak = HookTargetContract(
        featureId = "statusBarClockTweak",
        required = listOf(
            HookTargetSpec(
                id = "MiuiStatusBarClockController.constructors",
                kind = HookTargetKind.CONSTRUCTOR,
                className = "com.android.systemui.statusbar.policy.MiuiStatusBarClockController"
            ),
            HookTargetSpec(
                id = "MiuiStatusBarClockController.fireTimeChange",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.policy.MiuiStatusBarClockController",
                memberName = "fireTimeChange"
            ),
            HookTargetSpec(
                id = "MiuiClock.constructors",
                kind = HookTargetKind.CONSTRUCTOR,
                className = "com.android.systemui.statusbar.views.MiuiClock"
            ),
            HookTargetSpec(
                id = "MiuiClock.updateTime",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.views.MiuiClock",
                memberName = "updateTime"
            )
        ),
        optional = listOf(
            HookTargetSpec(
                id = "MiuiClock.setClockVisibility",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.views.MiuiClock",
                memberName = "setClockVisibility",
                parameterTypes = listOf(INT),
                required = false
            ),
            HookTargetSpec(
                id = "MiuiPhoneStatusBarView.onAttachedToWindow",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
                memberName = "onAttachedToWindow",
                required = false
            )
        )
    )

    val noMoreIcon = HookTargetContract(
        featureId = "noMoreIcon",
        required = listOf(
            HookTargetSpec(
                id = "NotificationIconAreaController.setIconsVisibility",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.phone.NotificationIconAreaController",
                memberName = "setIconsVisibility"
            )
        )
    )

    val batteryIndicator = HookTargetContract(
        featureId = "batteryIndicator",
        required = listOf(
            HookTargetSpec(
                id = "CentralSurfacesImpl.createAndAddWindows",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                memberName = "createAndAddWindows"
            ),
            HookTargetSpec(
                id = "CentralSurfacesImpl.setPanelExpanded",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                memberName = "setPanelExpanded",
                parameterTypes = listOf(BOOLEAN)
            ),
            HookTargetSpec(
                id = "CentralSurfacesImpl.setQsExpanded",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                memberName = "setQsExpanded",
                parameterTypes = listOf(BOOLEAN)
            ),
            HookTargetSpec(
                id = "CentralSurfacesImpl.updateIsKeyguard",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                memberName = "updateIsKeyguard",
                parameterTypes = listOf(BOOLEAN)
            ),
            HookTargetSpec(
                id = "NotificationIconAreaController.onDarkChanged",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.phone.NotificationIconAreaController",
                memberName = "onDarkChanged"
            ),
            HookTargetSpec(
                id = "MiuiBatteryControllerImpl.fireBatteryLevelChanged",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl",
                memberName = "fireBatteryLevelChanged"
            ),
            HookTargetSpec(
                id = "BatteryControllerImpl.firePowerSaveChanged",
                kind = HookTargetKind.METHOD,
                className = "com.android.systemui.statusbar.policy.BatteryControllerImpl",
                memberName = "firePowerSaveChanged"
            )
        )
    )

    val noClockHide = HookTargetContract(
        featureId = "noClockHide",
        required = listOf(
            HookTargetSpec(
                id = "Launcher.updateStatusBarClock",
                kind = HookTargetKind.METHOD,
                className = "com.miui.home.launcher.Launcher",
                memberName = "updateStatusBarClock",
                parameterTypes = listOf(LONG)
            )
        )
    )

    val noWidgetOnly = HookTargetContract(
        featureId = "noWidgetOnly",
        required = listOf(
            HookTargetSpec(
                id = "CellLayout.setScreenType",
                kind = HookTargetKind.METHOD,
                className = "com.miui.home.launcher.CellLayout",
                memberName = "setScreenType",
                parameterTypes = listOf(INT)
            )
        )
    )
}
