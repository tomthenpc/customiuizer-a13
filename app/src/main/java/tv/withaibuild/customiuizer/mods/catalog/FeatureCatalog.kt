package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.PackagePermissions
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks
import tv.withaibuild.customiuizer.mods.SystemLockScreenMoreHooks
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemStatusBarMoreHooks
import tv.withaibuild.customiuizer.mods.SystemUIBatteryHooks
import tv.withaibuild.customiuizer.mods.SystemUIScreenshotHooks
import tv.withaibuild.customiuizer.mods.SystemUINotificationHooks
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
import tv.withaibuild.customiuizer.mods.utils.HookInstaller
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Static, type-safe feature directory.
 *
 * [FeatureCatalog] holds [FeatureSpec] declarations and drives the install
 * lifecycle for each feature:
 *
 *     disabled / requested → compatibility resolution
 *     → dispatched / installed / degraded / failed → diagnostic snapshot
 *
 * [MainModule] preserves the original call order by invoking the catalog at
 * the same positions where the migrated hooks used to be called directly.
 */
object FeatureCatalog {

    private val features = listOf(
        FeatureSpec(
            contract = CanaryContracts.packagePermissions,
            id = "packagePermissions",
            diagnosticId = DiagnosticIds.PACKAGE_PERMISSIONS,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = emptySet(),
            condition = { true },
            compatibilityCheck = { _ ->
                DiagnosticRecorder.record(
                    DiagnosticIds.PACKAGE_PERMISSIONS,
                    compatibility = CompatibilityState.COMPATIBLE,
                    reasonCode = ReasonCode.PRIMARY_TARGET_FOUND,
                    detail = "package permissions always checked at system_server startup"
                )
                CompatibilityState.COMPATIBLE
            },
            installer = { runtime ->
                PackagePermissions.hook(runtime.lpparam as SystemServerStartingParam)
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CanaryContracts.statusBarClockTweak,
            id = "statusBarClockTweak",
            diagnosticId = DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf(
                "system_statusbar_clocktweak",
                "system_cc_clocktweak",
                "system_cc_hidedate",
                "system_cc_dateformat"
            ),
            condition = { prefs ->
                prefs.getBoolean("system_statusbar_clocktweak") ||
                prefs.getBoolean("system_cc_clocktweak") ||
                prefs.getBoolean("system_cc_hidedate") ||
                prefs.getString("system_cc_dateformat", "").isNotEmpty()
            },
            compatibilityCheck = { runtime ->
                runtime.resolver.resolveFirstClass(
                    DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
                    "com.android.systemui.statusbar.policy.MiuiStatusBarClockController",
                    "com.android.systemui.statusbar.policy.StatusBarClockController"
                ).compatibility
            },
            installer = { runtime ->
                SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.PARTIAL
        ),
        // Canary: system_server
        FeatureSpec(
            contract = CanaryContracts.autoBrightnessRange,
            id = "autoBrightnessRange",
            diagnosticId = DiagnosticIds.AUTO_BRIGHTNESS_RANGE,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_autobrightness"),
            condition = { prefs ->
                prefs.getBoolean("system_autobrightness", false)
            },
            compatibilityCheck = { runtime ->
                runtime.resolver.resolveFirstClass(
                    DiagnosticIds.AUTO_BRIGHTNESS_RANGE,
                    "com.android.server.display.AutomaticBrightnessController"
                ).compatibility
            },
            installer = { runtime ->
                SystemDisplayAndWindowHooks.AutoBrightnessRangeHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CanaryContracts.muffledVibration,
            id = "muffledVibration",
            diagnosticId = DiagnosticIds.MUFFLED_VIBRATION,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_vibration_amp"),
            condition = { prefs ->
                prefs.getBoolean("system_vibration_amp", false)
            },
            compatibilityCheck = { runtime ->
                runtime.resolver.resolveFirstClass(
                    DiagnosticIds.MUFFLED_VIBRATION,
                    "com.android.server.VibratorService"
                ).compatibility
            },
            installer = { runtime ->
                SystemAudioAndVisualAndMoreHooks.MuffledVibrationHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Canary: SystemUI
        FeatureSpec(
            contract = CanaryContracts.noMoreIcon,
            id = "noMoreIcon",
            diagnosticId = DiagnosticIds.NO_MORE_ICON,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidemoreicon"),
            condition = { prefs ->
                prefs.getBoolean("system_hidemoreicon", false)
            },
            compatibilityCheck = { runtime ->
                runtime.resolver.resolveFirstClass(
                    DiagnosticIds.NO_MORE_ICON,
                    "com.android.systemui.statusbar.phone.NotificationIconAreaController"
                ).compatibility
            },
            installer = { runtime ->
                SystemNotificationMoreHooks.NoMoreIconHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CanaryContracts.batteryIndicator,
            id = "batteryIndicator",
            diagnosticId = DiagnosticIds.BATTERY_INDICATOR,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_batteryindicator"),
            condition = { prefs ->
                prefs.getBoolean("system_batteryindicator", false)
            },
            compatibilityCheck = { runtime ->
                runtime.resolver.resolveFirstClass(
                    DiagnosticIds.BATTERY_INDICATOR,
                    "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
                ).compatibility
            },
            installer = { runtime ->
                SystemUIBatteryHooks.BatteryIndicatorHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Canary: Launcher
        FeatureSpec(
            contract = CanaryContracts.noClockHide,
            id = "noClockHide",
            diagnosticId = DiagnosticIds.NO_CLOCK_HIDE,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_noclockhide"),
            condition = { prefs ->
                prefs.getBoolean("launcher_noclockhide", false)
            },
            compatibilityCheck = { runtime ->
                runtime.resolver.resolveFirstClass(
                    DiagnosticIds.NO_CLOCK_HIDE,
                    "com.miui.home.launcher.Launcher"
                ).compatibility
            },
            installer = { runtime ->
                LauncherSystemHooks.NoClockHideHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CanaryContracts.noWidgetOnly,
            id = "noWidgetOnly",
            diagnosticId = DiagnosticIds.NO_WIDGET_ONLY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_nowidgetonly"),
            condition = { prefs ->
                prefs.getBoolean("launcher_nowidgetonly", false)
            },
            compatibilityCheck = { runtime ->
                runtime.resolver.resolveFirstClass(
                    DiagnosticIds.NO_WIDGET_ONLY,
                    "com.miui.home.launcher.CellLayout"
                ).compatibility
            },
            installer = { runtime ->
                LauncherLayoutHooks.NoWidgetOnlyHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 1: system_server
        FeatureSpec(
            contract = CatalogContracts.screenDimTime,
            id = "screenDimTime",
            diagnosticId = DiagnosticIds.SCREEN_DIM_TIME,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_dimtime"),
            condition = { prefs ->
                prefs.getInt("system_dimtime", 0) > 0
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.firstVolumePress,
            id = "firstVolumePress",
            diagnosticId = DiagnosticIds.FIRST_VOLUME_PRESS,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_firstpress"),
            condition = { prefs ->
                prefs.getBoolean("system_firstpress", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 1: SystemUI
        FeatureSpec(
            contract = CatalogContracts.networkIndicatorWifi,
            id = "networkIndicatorWifi",
            diagnosticId = DiagnosticIds.NETWORK_INDICATOR_WIFI,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_networkindicator_wifi"),
            condition = { prefs ->
                prefs.getBoolean("system_networkindicator_wifi", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemStatusBarMoreHooks.NetworkIndicatorWifi(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.muteVisibleNotifications,
            id = "muteVisibleNotifications",
            diagnosticId = DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_mutevisiblenotif"),
            condition = { prefs ->
                prefs.getBoolean("system_mutevisiblenotif", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemNotificationMoreHooks.MuteVisibleNotificationsHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 1: Launcher
        FeatureSpec(
            contract = CatalogContracts.hideLauncherTitles,
            id = "hideLauncherTitles",
            diagnosticId = DiagnosticIds.HIDE_LAUNCHER_TITLES,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_hidetitles"),
            condition = { prefs ->
                prefs.getBoolean("launcher_hidetitles", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                LauncherIconHooks.HideTitlesHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.fixAppInfoLaunch,
            id = "fixAppInfoLaunch",
            diagnosticId = DiagnosticIds.FIX_APP_INFO_LAUNCH,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_fixlaunch"),
            condition = { prefs ->
                prefs.getBoolean("launcher_fixlaunch", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                LauncherSystemHooks.FixAppInfoLaunchHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 2: system_server
        FeatureSpec(
            contract = CatalogContracts.hideProximityWarning,
            id = "hideProximityWarning",
            diagnosticId = DiagnosticIds.HIDE_PROXIMITY_WARNING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_hideproxywarn"),
            condition = { prefs ->
                prefs.getBoolean("system_hideproxywarn", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemDisplayAndWindowHooks.HideProximityWarningHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.clearAllTasks,
            id = "clearAllTasks",
            diagnosticId = DiagnosticIds.CLEAR_ALL_TASKS,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_clearalltasks"),
            condition = { prefs ->
                prefs.getBoolean("system_clearalltasks", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 2: SystemUI
        FeatureSpec(
            contract = CatalogContracts.hideDismissView,
            id = "hideDismissView",
            diagnosticId = DiagnosticIds.HIDE_DISMISS_VIEW,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_removedismiss"),
            condition = { prefs ->
                prefs.getBoolean("system_removedismiss", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemUINotificationHooks.HideDismissViewHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.hideLockScreenHint,
            id = "hideLockScreenHint",
            diagnosticId = DiagnosticIds.HIDE_LOCK_SCREEN_HINT,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidelshint"),
            condition = { prefs ->
                prefs.getBoolean("system_hidelshint", false)
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                SystemLockScreenMoreHooks.HideLockScreenHintHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 2: Launcher
        FeatureSpec(
            contract = CatalogContracts.folderColumns,
            id = "folderColumns",
            diagnosticId = DiagnosticIds.FOLDER_COLUMNS,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_folder_cols"),
            condition = { prefs ->
                prefs.getInt("launcher_folder_cols", 1) > 1
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                LauncherFolderHooks.FolderColumnsHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.titleTopMargin,
            id = "titleTopMargin",
            diagnosticId = DiagnosticIds.TITLE_TOP_MARGIN,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_titletopmargin"),
            condition = { prefs ->
                prefs.getInt("launcher_titletopmargin", 0) > 0
            },
            compatibilityCheck = { _ -> CompatibilityState.COMPATIBLE },
            installer = { runtime ->
                LauncherIconHooks.TitleTopMarginHook(
                    runtime.lpparam as PackageReadyParam
                )
                InstallOutcome.DISPATCHED
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )
    )

    private val byId = features.associateBy { it.id }

    /**
     * Create a [FeatureRuntime] for a host process.
     *
     * The runtime and its [HookTargetResolver] are reused for every
     * [installById] call in the same process.
     */
    @JvmStatic
    fun createRuntime(
        processName: String,
        lpparam: Any,
        classLoader: ClassLoader,
        prefs: PrefMap<String, Any?>
    ): FeatureRuntime = FeatureRuntime(processName, lpparam, classLoader, prefs)

    /**
     * Install a single feature by its stable [id] using a reusable [runtime].
     *
     * - Returns `true` only when the feature is requested, compatible/degraded,
     *   and the installer returned an outcome (DISPATCHED, INSTALLED or DEGRADED).
     * - Returns `false` when disabled, incompatible, or the installer threw.
     * - A thrown installer is recorded as FAILED and does not affect subsequent
     *   calls to [installById] for other features.
     */
    @JvmStatic
    fun installById(featureId: String, runtime: FeatureRuntime): Boolean {
        val feature = byId[featureId] ?: return false
        if (!feature.processTarget.matches(runtime.processName)) {
            return false
        }

        val enabled = if (feature.condition(runtime.prefs)) EnabledState.REQUESTED else EnabledState.DISABLED
        if (enabled == EnabledState.DISABLED) {
            DiagnosticRecorder.record(
                feature.diagnosticId,
                enabled = enabled,
                reasonCode = ReasonCode.PREFERENCE_DISABLED
            )
            return false
        }

        DiagnosticRecorder.record(
            feature.diagnosticId,
            enabled = enabled,
            reasonCode = ReasonCode.REQUESTED
        )

        val contract = feature.contract
        return if (contract != null) {
            installWithContract(feature, runtime, contract)
        } else {
            installWithLegacyCheck(feature, runtime)
        }
    }

    private fun installWithContract(
        feature: FeatureSpec,
        runtime: FeatureRuntime,
        contract: tv.withaibuild.customiuizer.mods.utils.HookTargetContract
    ): Boolean {
        val (compat, compatResult) = runtime.resolver.evaluateContract(contract, feature.diagnosticId)

        DiagnosticRecorder.record(
            feature.diagnosticId,
            compatibility = compat,
            reasonCode = compatResult.reasonCode,
            detail = compatResult.detail
        )

        if (compat == CompatibilityState.INCOMPATIBLE) {
            DiagnosticRecorder.record(
                feature.diagnosticId,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = compatResult.detail
            )
            return false
        }

        return try {
            val result = HookInstaller.withSession(
                resolver = runtime.resolver,
                contract = contract,
                diagnosticId = feature.diagnosticId,
                classLoader = runtime.classLoader,
                compatibilityResult = compatResult
            ) {
                feature.installer(runtime)
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
                feature.diagnosticId,
                installation = result.installation,
                reasonCode = result.reasonCode,
                detail = result.detail,
                installSummary = summary
            )
            result.installation != InstallOutcome.FAILED
        } catch (t: Throwable) {
            DiagnosticRecorder.record(
                feature.diagnosticId,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.INSTALLER_FAILED,
                detail = t.javaClass.name,
                throwable = t
            )
            false
        }
    }

    private fun installWithLegacyCheck(feature: FeatureSpec, runtime: FeatureRuntime): Boolean {
        val compat = feature.compatibilityCheck(runtime)

        return when (compat) {
            CompatibilityState.COMPATIBLE,
            CompatibilityState.DEGRADED -> {
                try {
                    val outcome = feature.installer(runtime)
                    DiagnosticRecorder.record(
                        feature.diagnosticId,
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
                } catch (t: Throwable) {
                    DiagnosticRecorder.record(
                        feature.diagnosticId,
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
                    feature.diagnosticId,
                    installation = InstallOutcome.FAILED,
                    reasonCode = ReasonCode.TARGET_NOT_FOUND,
                    detail = runtime.resolver.lastResolution(feature.diagnosticId)?.failures?.joinToString(", ")
                )
                false
            }
        }
    }

    /**
     * Returns a snapshot of the specs for documentation and audit.
     */
    @JvmStatic
    fun specs(): List<FeatureSpec> = features
}
