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
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
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

    init {
        FeatureInstallRegistry.registerAll(FeatureCatalog.specs())
    }

    @JvmStatic
    fun createRuntime(
        processName: String,
        lpparam: Any,
        classLoader: ClassLoader,
        prefs: PrefMap<String, Any>
    ): FeatureRuntime = FeatureRuntime(processName, lpparam, classLoader, prefs)

    @JvmStatic
    fun installById(featureId: String, runtime: FeatureRuntime): Boolean = try {
        val feature = FeatureId.fromString(featureId)
        if (feature == null) {
            DiagnosticRecorder.record(
                id = DiagnosticIds.UNKNOWN_FEATURE_ID,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = featureId
            )
            false
        } else {
            install(feature, runtime)
        }
    } catch (fatal: OutOfMemoryError) {
        throw fatal
    } catch (fatal: VirtualMachineError) {
        throw fatal
    } catch (fatal: ThreadDeath) {
        throw fatal
    }

    @JvmStatic
    fun install(feature: FeatureId, runtime: FeatureRuntime): Boolean {
        val spec = FeatureCatalog.specByCanonicalId(feature.canonicalId)
        if (spec == null) {
            DiagnosticRecorder.record(
                id = DiagnosticIds.UNKNOWN_FEATURE_ID,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = feature.canonicalId
            )
            return false
        }

        val scope = spec.processScope
        val phase = spec.installPhase
        if (scope == null || phase == null) {
            DiagnosticRecorder.record(
                id = spec.diagnosticId,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = "missing processScope or installPhase for ${feature.canonicalId}"
            )
            return false
        }

        return try {
            FeatureInstallRegistry.installById(
                feature.canonicalId,
                scope,
                phase,
                runtime
            ).isActive
        } catch (fatal: OutOfMemoryError) {
            throw fatal
        } catch (fatal: VirtualMachineError) {
            throw fatal
        } catch (fatal: ThreadDeath) {
            throw fatal
        }
    }

    private fun installPackagePermissions(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "packagePermissions",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            runtime
        ).isActive
    }

    private fun installStatusBarClockTweak(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "statusBarClockTweak",
            ProcessScope.SYSTEM_UI,
            InstallPhase.PACKAGE_READY,
            runtime
        ).isActive
    }

    private fun installAutoBrightnessRange(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "autoBrightnessRange",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            runtime
        ).isActive
    }

    private fun installMuffledVibration(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "muffledVibration",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            runtime
        ).isActive
    }

    private fun installNoMoreIcon(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "noMoreIcon",
            ProcessScope.SYSTEM_UI,
            InstallPhase.PACKAGE_READY,
            runtime
        ).isActive
    }

    private fun installBatteryIndicator(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "batteryIndicator",
            ProcessScope.SYSTEM_UI,
            InstallPhase.PACKAGE_READY,
            runtime
        ).isActive
    }

    private fun installNoClockHide(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "noClockHide",
            ProcessScope.LAUNCHER,
            InstallPhase.PACKAGE_READY,
            runtime
        ).isActive
    }

    private fun installNoWidgetOnly(runtime: FeatureRuntime): Boolean {
        return FeatureInstallRegistry.installById(
            "noWidgetOnly",
            ProcessScope.LAUNCHER,
            InstallPhase.PACKAGE_READY,
            runtime
        ).isActive
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
