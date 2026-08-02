package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.mods.PackagePermissions
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks
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
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.InstallSummary
import tv.withaibuild.customiuizer.mods.utils.FeatureInstallResult
import tv.withaibuild.customiuizer.mods.utils.HookInstaller
import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Static, type-safe feature directory.
 *
 * [FeatureCatalog] holds [FeatureSpec] declarations for documentation,
 * schema validation and audit. Runtime dispatch has been split into
 * [FeatureDispatcher] so the spec list is only built when explicitly
 * requested.
 */
object FeatureCatalog {

    private fun statusBarClockTweakContract(prefs: PrefMap<String, Any>): HookTargetContract {
        val statusBar = prefs.getBoolean("system_statusbar_clocktweak")
        val controlCenter = prefs.getBoolean("system_cc_clocktweak")
        val hideDate = prefs.getBoolean("system_cc_hidedate")
        return CanaryContracts.statusBarClockTweakForInstall(statusBar, controlCenter, hideDate)
    }

    private val auditSpecs by lazy(LazyThreadSafetyMode.NONE) { listOf(
        FeatureSpec(
            contract = CanaryContracts.packagePermissions,
            id = "packagePermissions",
            diagnosticId = DiagnosticIds.PACKAGE_PERMISSIONS,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = emptySet(),
            condition = { true },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.packagePermissions,
                    diagnosticId = DiagnosticIds.PACKAGE_PERMISSIONS,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    PackagePermissions.hook(runtime.lpparam as SystemServerStartingParam)
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "packagePermissions session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            id = "statusBarClockTweak",
            diagnosticId = DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
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
            compatibilityPolicy = CompatibilityPolicy.CUSTOM,
            compatibilityCheck = { runtime ->
                val contract = statusBarClockTweakContract(runtime.prefs)
                val (compat, result) = runtime.resolver.evaluateContract(
                    contract,
                    DiagnosticIds.STATUSBAR_CLOCK_TWEAK
                )
                CompatibilityResult(
                    compat,
                    result.reasonCode,
                    result.detail,
                    result.copy(resolvedContract = contract)
                )
            },
            installer = { runtime, compatResult ->
                val contract = compatResult.resolvedContract
                    ?: statusBarClockTweakContract(runtime.prefs)
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = contract,
                    diagnosticId = DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "statusBarClockTweak session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.PARTIAL
        ),
        // Canary: system_server
        FeatureSpec(
            contract = CanaryContracts.autoBrightnessRange,
            id = "autoBrightnessRange",
            diagnosticId = DiagnosticIds.AUTO_BRIGHTNESS_RANGE,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_autobrightness"),
            condition = { prefs ->
                prefs.getBoolean("system_autobrightness", false)
            },
            installer = { runtime, compatResult ->
                val variant = when (compatResult.selectedVariant?.id) {
                    "automatic_brightness_controller" ->
                        SystemDisplayAndWindowHooks.AutoBrightnessVariant.AUTOMATIC_BRIGHTNESS_CONTROLLER
                    "display_power_controller" ->
                        SystemDisplayAndWindowHooks.AutoBrightnessVariant.DISPLAY_POWER_CONTROLLER
                    else -> null
                }

                if (variant == null) {
                    FeatureInstallResult.Incompatible("no selectable autoBrightness variant")
                } else {
                    val session = HookInstaller.withSession(
                        resolver = runtime.resolver,
                        contract = compatResult.resolvedContract ?: CanaryContracts.autoBrightnessRange,
                        diagnosticId = DiagnosticIds.AUTO_BRIGHTNESS_RANGE,
                        classLoader = runtime.classLoader,
                        compatibilityResult = compatResult
                    ) {
                        SystemDisplayAndWindowHooks.AutoBrightnessRangeHook(
                            runtime.lpparam as SystemServerStartingParam,
                            variant
                        )
                    }
                    when (session.installation) {
                        InstallOutcome.INSTALLED,
                        InstallOutcome.DEGRADED,
                        InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                            InstallSummary(
                                requiredInstalled = session.requiredInstalled,
                                requiredTotal = session.requiredTotal,
                                optionalInstalled = session.optionalInstalled,
                                optionalTotal = session.optionalTotal,
                                fallbackUsed = session.fallbackUsed,
                                installation = session.installation ?: InstallOutcome.FAILED,
                                reasonCode = session.reasonCode
                            )
                        )
                        else -> FeatureInstallResult.FailedTransient(
                            session.detail ?: "autoBrightnessRange session failed"
                        )
                    }
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CanaryContracts.muffledVibration,
            id = "muffledVibration",
            diagnosticId = DiagnosticIds.MUFFLED_VIBRATION,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_vibration_amp"),
            condition = { prefs ->
                prefs.getBoolean("system_vibration_amp", false)
            },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.muffledVibration,
                    diagnosticId = DiagnosticIds.MUFFLED_VIBRATION,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    SystemAudioAndVisualAndMoreHooks.MuffledVibrationHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "muffledVibration session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Canary: SystemUI
        FeatureSpec(
            contract = CanaryContracts.noMoreIcon,
            id = "noMoreIcon",
            diagnosticId = DiagnosticIds.NO_MORE_ICON,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidemoreicon"),
            condition = { prefs ->
                prefs.getBoolean("system_hidemoreicon", false)
            },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.noMoreIcon,
                    diagnosticId = DiagnosticIds.NO_MORE_ICON,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    SystemNotificationMoreHooks.NoMoreIconHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "noMoreIcon session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CanaryContracts.batteryIndicator,
            id = "batteryIndicator",
            diagnosticId = DiagnosticIds.BATTERY_INDICATOR,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_batteryindicator"),
            condition = { prefs ->
                prefs.getBoolean("system_batteryindicator", false)
            },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.batteryIndicator,
                    diagnosticId = DiagnosticIds.BATTERY_INDICATOR,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    SystemUIBatteryHooks.BatteryIndicatorHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "batteryIndicator session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Canary: Launcher
        FeatureSpec(
            contract = CanaryContracts.noClockHide,
            id = "noClockHide",
            diagnosticId = DiagnosticIds.NO_CLOCK_HIDE,
            processScope = ProcessScope.LAUNCHER,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_noclockhide"),
            condition = { prefs ->
                prefs.getBoolean("launcher_noclockhide", false)
            },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.noClockHide,
                    diagnosticId = DiagnosticIds.NO_CLOCK_HIDE,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    LauncherSystemHooks.NoClockHideHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "noClockHide session failed"
                    )
                }
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CanaryContracts.noWidgetOnly,
            id = "noWidgetOnly",
            diagnosticId = DiagnosticIds.NO_WIDGET_ONLY,
            processScope = ProcessScope.LAUNCHER,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_nowidgetonly"),
            condition = { prefs ->
                prefs.getBoolean("launcher_nowidgetonly", false)
            },
            installer = { runtime, compatResult ->
                val session = HookInstaller.withSession(
                    resolver = runtime.resolver,
                    contract = compatResult.resolvedContract ?: CanaryContracts.noWidgetOnly,
                    diagnosticId = DiagnosticIds.NO_WIDGET_ONLY,
                    classLoader = runtime.classLoader,
                    compatibilityResult = compatResult
                ) {
                    LauncherLayoutHooks.NoWidgetOnlyHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
                when (session.installation) {
                    InstallOutcome.INSTALLED,
                    InstallOutcome.DEGRADED,
                    InstallOutcome.DISPATCHED -> FeatureInstallResult.Installed(
                        InstallSummary(
                            requiredInstalled = session.requiredInstalled,
                            requiredTotal = session.requiredTotal,
                            optionalInstalled = session.optionalInstalled,
                            optionalTotal = session.optionalTotal,
                            fallbackUsed = session.fallbackUsed,
                            installation = session.installation ?: InstallOutcome.FAILED,
                            reasonCode = session.reasonCode
                        )
                    )
                    else -> FeatureInstallResult.FailedTransient(
                        session.detail ?: "noWidgetOnly session failed"
                    )
                }
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
                        installer = { runtime, compatResult ->
                SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                SystemStatusBarMoreHooks.NetworkIndicatorWifi(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                SystemNotificationMoreHooks.MuteVisibleNotificationsHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                LauncherIconHooks.HideTitlesHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                LauncherSystemHooks.FixAppInfoLaunchHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                SystemDisplayAndWindowHooks.HideProximityWarningHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                SystemUINotificationHooks.HideDismissViewHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                SystemLockScreenMoreHooks.HideLockScreenHintHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                LauncherFolderHooks.FolderColumnsHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
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
                        installer = { runtime, compatResult ->
                LauncherIconHooks.TitleTopMarginHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 3: system_server
        FeatureSpec(
            contract = CatalogContracts.noLightUpOnCharge,
            id = "noLightUpOnCharge",
            diagnosticId = DiagnosticIds.NO_LIGHT_UP_ON_CHARGE,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_nolightuponcharges"),
            condition = { prefs ->
                prefs.getStringAsInt("system_nolightuponcharges", 1) > 1
            },
                        installer = { runtime, compatResult ->
                SystemDisplayAndWindowHooks.NoLightUpOnChargeHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                FeatureInstallResult.Installed()
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.allRotations,
            id = "allRotations",
            diagnosticId = DiagnosticIds.ALL_ROTATIONS,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_allrotations2"),
            condition = { prefs ->
                prefs.getStringAsInt("system_allrotations2", 1) > 1
            },
                        installer = { runtime, compatResult ->
                SystemAudioAndVisualAndMoreHooks.AllRotationsHook(
                    runtime.lpparam as SystemServerStartingParam
                )
                FeatureInstallResult.Installed()
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 3: SystemUI
        FeatureSpec(
            contract = CatalogContracts.noNetworkSpeedSeparator,
            id = "noNetworkSpeedSeparator",
            diagnosticId = DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_nonetspeedseparator"),
            condition = { prefs ->
                prefs.getBoolean("system_nonetspeedseparator", false)
            },
                        installer = { runtime, compatResult ->
                SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            contract = CatalogContracts.hideIconsClock,
            id = "hideIconsClock",
            diagnosticId = DiagnosticIds.HIDE_ICONS_CLOCK,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_statusbaricons_clock"),
            condition = { prefs ->
                prefs.getBoolean("system_statusbaricons_clock", false)
            },
                        installer = { runtime, compatResult ->
                SystemUIStatusBarHooks.HideIconsClockHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 3: Launcher
        FeatureSpec(
            contract = CatalogContracts.noUnlockAnimation,
            id = "noUnlockAnimation",
            diagnosticId = DiagnosticIds.NO_UNLOCK_ANIMATION,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_nounlockanim"),
            condition = { prefs ->
                prefs.getBoolean("launcher_nounlockanim", false)
            },
                        installer = { runtime, compatResult ->
                LauncherAnimationHooks.NoUnlockAnimationHook(
                    runtime.lpparam as PackageReadyParam
                )
                FeatureInstallResult.Installed()
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )
    ) }

    private val registryMigratedIds = setOf(
        "packagePermissions",
        "statusBarClockTweak",
        "autoBrightnessRange",
        "muffledVibration",
        "noMoreIcon",
        "batteryIndicator",
        "noClockHide",
        "noWidgetOnly"
    )

    /**
     * Returns only the specs that have been migrated to the production
     * [FeatureInstallRegistry]. Non-migrated catalog features remain routed
     * through the legacy [FeatureDispatcher] paths.
     */
    @JvmStatic
    fun registrySpecs(): List<FeatureSpec> =
        auditSpecs.filter { it.id in registryMigratedIds }

    /**
     * Returns a snapshot of the specs for documentation and audit.
     */
    @JvmStatic
    fun specs(): List<FeatureSpec> = auditSpecs
}
