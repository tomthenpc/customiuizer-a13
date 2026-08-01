package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.mods.PackagePermissions
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks.AutoBrightnessVariant

import tv.withaibuild.customiuizer.mods.SystemLockScreenMoreHooks
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemStatusBarMoreHooks
import tv.withaibuild.customiuizer.mods.SystemUIBatteryHooks
import tv.withaibuild.customiuizer.mods.SystemUINotificationHooks
import tv.withaibuild.customiuizer.mods.SystemUIStatusBarHooks
import tv.withaibuild.customiuizer.mods.LauncherAnimationHooks
import tv.withaibuild.customiuizer.mods.LauncherFolderHooks
import tv.withaibuild.customiuizer.mods.LauncherIconHooks
import tv.withaibuild.customiuizer.mods.LauncherLayoutHooks
import tv.withaibuild.customiuizer.mods.LauncherSystemHooks
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.EnabledState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.InstallSummary
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.FeatureTargetVariant
import tv.withaibuild.customiuizer.mods.utils.HookInstaller
import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Runtime feature dispatcher.
 *
 * [FeatureDispatcher] is the hot-path entry point for installing catalog
 * features. It avoids the allocation cost of building the full [FeatureSpec]
 * list at runtime by using a plain `when` over the known feature ids.
 */
object FeatureDispatcher {

    @JvmStatic
    fun createRuntime(
        processName: String,
        lpparam: Any,
        classLoader: ClassLoader,
        prefs: PrefMap<String, Any>
    ): FeatureRuntime = FeatureRuntime(processName, lpparam, classLoader, prefs)

    @JvmStatic
    fun installById(featureId: String, runtime: FeatureRuntime): Boolean {
        val feature = FeatureId.fromString(featureId)
        if (feature == null) {
            DiagnosticRecorder.record(
                id = DiagnosticIds.UNKNOWN_FEATURE_ID,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = featureId
            )
            return false
        }
        return install(feature, runtime)
    }

    @JvmStatic
    fun install(feature: FeatureId, runtime: FeatureRuntime): Boolean = when (feature) {
        FeatureId.PACKAGE_PERMISSIONS -> installPackagePermissions(runtime)
        FeatureId.STATUS_BAR_CLOCK_TWEAK -> installStatusBarClockTweak(runtime)
        FeatureId.AUTO_BRIGHTNESS_RANGE -> installAutoBrightnessRange(runtime)
        FeatureId.MUFFLED_VIBRATION -> installMuffledVibration(runtime)
        FeatureId.NO_MORE_ICON -> installNoMoreIcon(runtime)
        FeatureId.BATTERY_INDICATOR -> installBatteryIndicator(runtime)
        FeatureId.NO_CLOCK_HIDE -> installNoClockHide(runtime)
        FeatureId.NO_WIDGET_ONLY -> installNoWidgetOnly(runtime)
        FeatureId.SCREEN_DIM_TIME -> installScreenDimTime(runtime)
        FeatureId.FIRST_VOLUME_PRESS -> installFirstVolumePress(runtime)
        FeatureId.NETWORK_INDICATOR_WIFI -> installNetworkIndicatorWifi(runtime)
        FeatureId.MUTE_VISIBLE_NOTIFICATIONS -> installMuteVisibleNotifications(runtime)
        FeatureId.HIDE_LAUNCHER_TITLES -> installHideLauncherTitles(runtime)
        FeatureId.FIX_APP_INFO_LAUNCH -> installFixAppInfoLaunch(runtime)
        FeatureId.HIDE_PROXIMITY_WARNING -> installHideProximityWarning(runtime)
        FeatureId.CLEAR_ALL_TASKS -> installClearAllTasks(runtime)
        FeatureId.HIDE_DISMISS_VIEW -> installHideDismissView(runtime)
        FeatureId.HIDE_LOCK_SCREEN_HINT -> installHideLockScreenHint(runtime)
        FeatureId.FOLDER_COLUMNS -> installFolderColumns(runtime)
        FeatureId.TITLE_TOP_MARGIN -> installTitleTopMargin(runtime)
        FeatureId.NO_LIGHT_UP_ON_CHARGE -> installNoLightUpOnCharge(runtime)
        FeatureId.ALL_ROTATIONS -> installAllRotations(runtime)
        FeatureId.NO_NETWORK_SPEED_SEPARATOR -> installNoNetworkSpeedSeparator(runtime)
        FeatureId.HIDE_ICONS_CLOCK -> installHideIconsClock(runtime)
        FeatureId.NO_UNLOCK_ANIMATION -> installNoUnlockAnimation(runtime)
    }

    private fun installPackagePermissions(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false

        recordRequested(DiagnosticIds.PACKAGE_PERMISSIONS)
        return installWithContract(
            DiagnosticIds.PACKAGE_PERMISSIONS,
            runtime,
            CanaryContracts.packagePermissions
        ) {
            PackagePermissions.hook(runtime.lpparam as SystemServerStartingParam)
            InstallOutcome.DISPATCHED
        }
    }

    private fun installStatusBarClockTweak(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        val statusBarClockTweakEnabled = runtime.prefs.getBoolean("system_statusbar_clocktweak")
        val controlCenterClockTweakEnabled = runtime.prefs.getBoolean("system_cc_clocktweak")
        val hideControlCenterDate = runtime.prefs.getBoolean("system_cc_hidedate")
        if (!statusBarClockTweakEnabled &&
            !controlCenterClockTweakEnabled &&
            !hideControlCenterDate &&
            runtime.prefs.getString("system_cc_dateformat", "").isEmpty()
        ) {
            return false
        }

        recordRequested(DiagnosticIds.STATUSBAR_CLOCK_TWEAK)
        return installWithContract(
            DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
            runtime,
            CanaryContracts.statusBarClockTweakForInstall(
                statusBarClockTweakEnabled,
                controlCenterClockTweakEnabled,
                hideControlCenterDate
            )
        ) {
            SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installAutoBrightnessRange(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_autobrightness", false)) return false

        recordRequested(DiagnosticIds.AUTO_BRIGHTNESS_RANGE)
        return installWithContractVariant(
            DiagnosticIds.AUTO_BRIGHTNESS_RANGE,
            runtime,
            CanaryContracts.autoBrightnessRange
        ) { selectedVariant ->
            val variant = when (selectedVariant.id) {
                "automatic_brightness_controller" ->
                    AutoBrightnessVariant.AUTOMATIC_BRIGHTNESS_CONTROLLER
                "display_power_controller" ->
                    AutoBrightnessVariant.DISPLAY_POWER_CONTROLLER
                else -> throw IllegalArgumentException(
                    "Unknown autoBrightnessRange variant: ${selectedVariant.id}"
                )
            }
            SystemDisplayAndWindowHooks.AutoBrightnessRangeHook(
                runtime.lpparam as SystemServerStartingParam,
                variant
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installMuffledVibration(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_vibration_amp", false)) return false

        recordRequested(DiagnosticIds.MUFFLED_VIBRATION)
        return installWithContract(
            DiagnosticIds.MUFFLED_VIBRATION,
            runtime,
            CanaryContracts.muffledVibration
        ) {
            SystemAudioAndVisualAndMoreHooks.MuffledVibrationHook(
                runtime.lpparam as SystemServerStartingParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installNoMoreIcon(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_hidemoreicon", false)) return false

        recordRequested(DiagnosticIds.NO_MORE_ICON)
        return installWithContract(
            DiagnosticIds.NO_MORE_ICON,
            runtime,
            CanaryContracts.noMoreIcon
        ) {
            SystemNotificationMoreHooks.NoMoreIconHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installBatteryIndicator(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_batteryindicator", false)) return false

        recordRequested(DiagnosticIds.BATTERY_INDICATOR)
        return installWithContract(
            DiagnosticIds.BATTERY_INDICATOR,
            runtime,
            CanaryContracts.batteryIndicator
        ) {
            SystemUIBatteryHooks.BatteryIndicatorHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installNoClockHide(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.Launcher.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("launcher_noclockhide", false)) return false

        recordRequested(DiagnosticIds.NO_CLOCK_HIDE)
        return installWithContract(
            DiagnosticIds.NO_CLOCK_HIDE,
            runtime,
            CanaryContracts.noClockHide
        ) {
            LauncherSystemHooks.NoClockHideHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installNoWidgetOnly(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.Launcher.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("launcher_nowidgetonly", false)) return false

        recordRequested(DiagnosticIds.NO_WIDGET_ONLY)
        return installWithContract(
            DiagnosticIds.NO_WIDGET_ONLY,
            runtime,
            CanaryContracts.noWidgetOnly
        ) {
            LauncherLayoutHooks.NoWidgetOnlyHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installScreenDimTime(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (runtime.prefs.getInt("system_dimtime", 0) <= 0) return false

        recordRequested(DiagnosticIds.SCREEN_DIM_TIME)
        return installWithContract(
            DiagnosticIds.SCREEN_DIM_TIME,
            runtime,
            CatalogContracts.screenDimTime
        ) {
            SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook(
                runtime.lpparam as SystemServerStartingParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installFirstVolumePress(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_firstpress", false)) return false

        recordRequested(DiagnosticIds.FIRST_VOLUME_PRESS)
        return installWithContract(
            DiagnosticIds.FIRST_VOLUME_PRESS,
            runtime,
            CatalogContracts.firstVolumePress
        ) {
            SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook(
                runtime.lpparam as SystemServerStartingParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installNetworkIndicatorWifi(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_networkindicator_wifi", false)) return false

        recordRequested(DiagnosticIds.NETWORK_INDICATOR_WIFI)
        return installWithContract(
            DiagnosticIds.NETWORK_INDICATOR_WIFI,
            runtime,
            CatalogContracts.networkIndicatorWifi
        ) {
            SystemStatusBarMoreHooks.NetworkIndicatorWifi(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installMuteVisibleNotifications(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_mutevisiblenotif", false)) return false

        recordRequested(DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS)
        return installWithContract(
            DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS,
            runtime,
            CatalogContracts.muteVisibleNotifications
        ) {
            SystemNotificationMoreHooks.MuteVisibleNotificationsHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installHideLauncherTitles(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.Launcher.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("launcher_hidetitles", false)) return false

        recordRequested(DiagnosticIds.HIDE_LAUNCHER_TITLES)
        return installWithContract(
            DiagnosticIds.HIDE_LAUNCHER_TITLES,
            runtime,
            CatalogContracts.hideLauncherTitles
        ) {
            LauncherIconHooks.HideTitlesHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installFixAppInfoLaunch(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.Launcher.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("launcher_fixlaunch", false)) return false

        recordRequested(DiagnosticIds.FIX_APP_INFO_LAUNCH)
        return installWithContract(
            DiagnosticIds.FIX_APP_INFO_LAUNCH,
            runtime,
            CatalogContracts.fixAppInfoLaunch
        ) {
            LauncherSystemHooks.FixAppInfoLaunchHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installHideProximityWarning(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_hideproxywarn", false)) return false

        recordRequested(DiagnosticIds.HIDE_PROXIMITY_WARNING)
        return installWithContract(
            DiagnosticIds.HIDE_PROXIMITY_WARNING,
            runtime,
            CatalogContracts.hideProximityWarning
        ) {
            SystemDisplayAndWindowHooks.HideProximityWarningHook(
                runtime.lpparam as SystemServerStartingParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installClearAllTasks(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_clearalltasks", false)) return false

        recordRequested(DiagnosticIds.CLEAR_ALL_TASKS)
        return installWithContract(
            DiagnosticIds.CLEAR_ALL_TASKS,
            runtime,
            CatalogContracts.clearAllTasks
        ) {
            SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook(
                runtime.lpparam as SystemServerStartingParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installHideDismissView(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_removedismiss", false)) return false

        recordRequested(DiagnosticIds.HIDE_DISMISS_VIEW)
        return installWithContract(
            DiagnosticIds.HIDE_DISMISS_VIEW,
            runtime,
            CatalogContracts.hideDismissView
        ) {
            SystemUINotificationHooks.HideDismissViewHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installHideLockScreenHint(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_hidelshint", false)) return false

        recordRequested(DiagnosticIds.HIDE_LOCK_SCREEN_HINT)
        return installWithContract(
            DiagnosticIds.HIDE_LOCK_SCREEN_HINT,
            runtime,
            CatalogContracts.hideLockScreenHint
        ) {
            SystemLockScreenMoreHooks.HideLockScreenHintHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installFolderColumns(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.Launcher.matches(runtime.processName)) return false
        if (runtime.prefs.getInt("launcher_folder_cols", 1) <= 1) return false

        recordRequested(DiagnosticIds.FOLDER_COLUMNS)
        return installWithContract(
            DiagnosticIds.FOLDER_COLUMNS,
            runtime,
            CatalogContracts.folderColumns
        ) {
            LauncherFolderHooks.FolderColumnsHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installTitleTopMargin(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.Launcher.matches(runtime.processName)) return false
        if (runtime.prefs.getInt("launcher_titletopmargin", 0) <= 0) return false

        recordRequested(DiagnosticIds.TITLE_TOP_MARGIN)
        return installWithContract(
            DiagnosticIds.TITLE_TOP_MARGIN,
            runtime,
            CatalogContracts.titleTopMargin
        ) {
            LauncherIconHooks.TitleTopMarginHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installNoLightUpOnCharge(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (runtime.prefs.getStringAsInt("system_nolightuponcharges", 1) <= 1) return false

        recordRequested(DiagnosticIds.NO_LIGHT_UP_ON_CHARGE)
        return installWithContract(
            DiagnosticIds.NO_LIGHT_UP_ON_CHARGE,
            runtime,
            CatalogContracts.noLightUpOnCharge
        ) {
            SystemDisplayAndWindowHooks.NoLightUpOnChargeHook(
                runtime.lpparam as SystemServerStartingParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installAllRotations(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemServer.matches(runtime.processName)) return false
        if (runtime.prefs.getStringAsInt("system_allrotations2", 1) <= 1) return false

        recordRequested(DiagnosticIds.ALL_ROTATIONS)
        return installWithContract(
            DiagnosticIds.ALL_ROTATIONS,
            runtime,
            CatalogContracts.allRotations
        ) {
            SystemAudioAndVisualAndMoreHooks.AllRotationsHook(
                runtime.lpparam as SystemServerStartingParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installNoNetworkSpeedSeparator(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_nonetspeedseparator", false)) return false

        recordRequested(DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR)
        return installWithContract(
            DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR,
            runtime,
            CatalogContracts.noNetworkSpeedSeparator
        ) {
            SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installHideIconsClock(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.SystemUI.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("system_statusbaricons_clock", false)) return false

        recordRequested(DiagnosticIds.HIDE_ICONS_CLOCK)
        return installWithContract(
            DiagnosticIds.HIDE_ICONS_CLOCK,
            runtime,
            CatalogContracts.hideIconsClock
        ) {
            SystemUIStatusBarHooks.HideIconsClockHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun installNoUnlockAnimation(runtime: FeatureRuntime): Boolean {
        if (!ProcessTarget.Launcher.matches(runtime.processName)) return false
        if (!runtime.prefs.getBoolean("launcher_nounlockanim", false)) return false

        recordRequested(DiagnosticIds.NO_UNLOCK_ANIMATION)
        return installWithContract(
            DiagnosticIds.NO_UNLOCK_ANIMATION,
            runtime,
            CatalogContracts.noUnlockAnimation
        ) {
            LauncherAnimationHooks.NoUnlockAnimationHook(
                runtime.lpparam as PackageReadyParam
            )
            InstallOutcome.DISPATCHED
        }
    }

    private fun recordRequested(diagnosticId: String) {
        DiagnosticRecorder.record(
            diagnosticId,
            enabled = EnabledState.REQUESTED,
            reasonCode = ReasonCode.REQUESTED
        )
    }

    private inline fun installWithContract(
        diagnosticId: String,
        runtime: FeatureRuntime,
        contract: HookTargetContract,
        crossinline installer: () -> InstallOutcome
    ): Boolean = installWithContractVariant(diagnosticId, runtime, contract) { _ ->
        installer()
    }

    private inline fun installWithContractVariant(
        diagnosticId: String,
        runtime: FeatureRuntime,
        contract: HookTargetContract,
        crossinline installer: (FeatureTargetVariant) -> InstallOutcome
    ): Boolean {
        // Trigger the per-process ROM environment detection once, on the first
        // enabled catalog feature that reaches the install cold path. Disabled
        // features never call installWithContract, so the environment is not
        // initialized for them.
        @Suppress("UNUSED_VARIABLE")
        val environment = runtime.environment

        val (compat, compatResult) = runtime.resolver.evaluateContract(contract, diagnosticId)

        DiagnosticRecorder.record(
            diagnosticId,
            compatibility = compat,
            reasonCode = compatResult.reasonCode,
            detail = compatResult.detail
        )

        if (compat == CompatibilityState.INCOMPATIBLE) {
            DiagnosticRecorder.record(
                diagnosticId,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = compatResult.detail
            )
            return false
        }

        val selectedVariant = compatResult.selectedVariant
            ?: throw IllegalStateException("Contract ${contract.featureId} resolved without a selected variant")

        return try {
            val result = HookInstaller.withSession(
                resolver = runtime.resolver,
                contract = contract,
                diagnosticId = diagnosticId,
                classLoader = runtime.classLoader,
                compatibilityResult = compatResult
            ) {
                installer(selectedVariant)
            }

            val summary = InstallSummary(
                requiredInstalled = result.requiredInstalled,
                requiredTotal = result.requiredTotal,
                optionalInstalled = result.optionalInstalled,
                optionalTotal = result.optionalTotal,
                fallbackUsed = result.fallbackUsed,
                installation = result.installation ?: InstallOutcome.FAILED,
                reasonCode = result.reasonCode
            )

            DiagnosticRecorder.record(
                diagnosticId,
                installation = result.installation,
                reasonCode = result.reasonCode,
                detail = result.detail,
                installSummary = summary
            )
            result.installation != InstallOutcome.FAILED
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            DiagnosticRecorder.record(
                diagnosticId,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.INSTALLER_FAILED,
                detail = t.javaClass.name,
                throwable = t
            )
            false
        }
    }

    private inline fun installWithLegacyCheck(
        diagnosticId: String,
        runtime: FeatureRuntime,
        compatibilityCheck: () -> CompatibilityState,
        installer: () -> InstallOutcome
    ): Boolean {
        val compat = compatibilityCheck()

        return when (compat) {
            CompatibilityState.COMPATIBLE,
            CompatibilityState.DEGRADED -> {
                try {
                    val outcome = installer()
                    DiagnosticRecorder.record(
                        diagnosticId,
                        installation = outcome,
                        reasonCode = if (outcome == InstallOutcome.INSTALLED) {
                            ReasonCode.INSTALLER_SUCCEEDED
                        } else {
                            ReasonCode.INSTALLER_DISPATCHED
                        },
                        detail = if (outcome == InstallOutcome.DISPATCHED) {
                            "legacy unit installer"
                        } else null
                    )
                    outcome != InstallOutcome.FAILED
                } catch (oom: OutOfMemoryError) {
                    throw oom
                } catch (t: Throwable) {
                    DiagnosticRecorder.record(
                        diagnosticId,
                        installation = InstallOutcome.FAILED,
                        reasonCode = ReasonCode.INSTALLER_FAILED,
                        detail = t.javaClass.name,
                        throwable = t
                    )
                    false
                }
            }
            CompatibilityState.INCOMPATIBLE -> {
                DiagnosticRecorder.record(
                    diagnosticId,
                    installation = InstallOutcome.FAILED,
                    reasonCode = ReasonCode.TARGET_NOT_FOUND,
                    detail = runtime.resolver.lastResolution(diagnosticId)?.failures?.joinToString(", ")
                )
                false
            }
        }
    }
}
