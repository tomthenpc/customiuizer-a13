package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.mods.PackagePermissions
import tv.withaibuild.customiuizer.mods.SystemAudioAndVolumeHooks
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks
import tv.withaibuild.customiuizer.mods.SystemLockScreenMoreHooks
import tv.withaibuild.customiuizer.mods.SystemUIScreenshotHooks
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
import tv.withaibuild.customiuizer.mods.utils.HookInstallResult
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

    private val registrySpecsInternal by lazy(LazyThreadSafetyMode.NONE) { listOf(
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
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
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
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
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
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
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
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
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
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
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
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
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
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
        )
    ) }

    private val adaptedSpecsInternal by lazy(LazyThreadSafetyMode.NONE) { listOf(
        // Catalog expansion batch 1: system_server
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.screenDimTime,
            id = "screenDimTime",
            diagnosticId = DiagnosticIds.SCREEN_DIM_TIME,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_dimtime"),
            condition = { prefs ->
                prefs.getInt("system_dimtime", 0) > 0
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.screenDimTime,
                    diagnosticId = DiagnosticIds.SCREEN_DIM_TIME
                ) {
                    SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.firstVolumePress,
            id = "firstVolumePress",
            diagnosticId = DiagnosticIds.FIRST_VOLUME_PRESS,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_firstpress"),
            condition = { prefs ->
                prefs.getBoolean("system_firstpress", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.firstVolumePress,
                    diagnosticId = DiagnosticIds.FIRST_VOLUME_PRESS
                ) {
                    SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook(
                    runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.volumeSteps,
            id = "volumeSteps",
            diagnosticId = DiagnosticIds.VOLUME_STEPS,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_volumesteps"),
            condition = { prefs ->
                prefs.getInt("system_volumesteps", 0) > 0
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.volumeSteps,
                    diagnosticId = DiagnosticIds.VOLUME_STEPS
                ) {
                    SystemAudioAndVolumeHooks.VolumeStepsHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.toastTime,
            id = "toastTime",
            diagnosticId = DiagnosticIds.TOAST_TIME,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_toasttime"),
            condition = { prefs ->
                prefs.getInt("system_toasttime", 0) > 0
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.toastTime,
                    diagnosticId = DiagnosticIds.TOAST_TIME
                ) {
                    SystemAudioAndVisualAndMoreHooks.ToastTimeHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 1: SystemUI
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.networkIndicatorWifi,
            id = "networkIndicatorWifi",
            diagnosticId = DiagnosticIds.NETWORK_INDICATOR_WIFI,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_networkindicator_wifi"),
            condition = { prefs ->
                prefs.getBoolean("system_networkindicator_wifi", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.networkIndicatorWifi,
                    diagnosticId = DiagnosticIds.NETWORK_INDICATOR_WIFI
                ) {
                    SystemStatusBarMoreHooks.NetworkIndicatorWifi(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.muteVisibleNotifications,
            id = "muteVisibleNotifications",
            diagnosticId = DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_mutevisiblenotif"),
            condition = { prefs ->
                prefs.getBoolean("system_mutevisiblenotif", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.muteVisibleNotifications,
                    diagnosticId = DiagnosticIds.MUTE_VISIBLE_NOTIFICATIONS
                ) {
                    SystemNotificationMoreHooks.MuteVisibleNotificationsHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 1: Launcher
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideLauncherTitles,
            id = "hideLauncherTitles",
            diagnosticId = DiagnosticIds.HIDE_LAUNCHER_TITLES,
            processScope = ProcessScope.LAUNCHER,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_hidetitles"),
            condition = { prefs ->
                prefs.getBoolean("launcher_hidetitles", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideLauncherTitles,
                    diagnosticId = DiagnosticIds.HIDE_LAUNCHER_TITLES
                ) {
                    LauncherIconHooks.HideTitlesHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.fixAppInfoLaunch,
            id = "fixAppInfoLaunch",
            diagnosticId = DiagnosticIds.FIX_APP_INFO_LAUNCH,
            processScope = ProcessScope.LAUNCHER,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_fixlaunch"),
            condition = { prefs ->
                prefs.getBoolean("launcher_fixlaunch", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.fixAppInfoLaunch,
                    diagnosticId = DiagnosticIds.FIX_APP_INFO_LAUNCH
                ) {
                    LauncherSystemHooks.FixAppInfoLaunchHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 2: system_server
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideProximityWarning,
            id = "hideProximityWarning",
            diagnosticId = DiagnosticIds.HIDE_PROXIMITY_WARNING,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_hideproxywarn"),
            condition = { prefs ->
                prefs.getBoolean("system_hideproxywarn", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideProximityWarning,
                    diagnosticId = DiagnosticIds.HIDE_PROXIMITY_WARNING
                ) {
                    SystemDisplayAndWindowHooks.HideProximityWarningHook(
                    runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.clearAllTasks,
            id = "clearAllTasks",
            diagnosticId = DiagnosticIds.CLEAR_ALL_TASKS,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_clearalltasks"),
            condition = { prefs ->
                prefs.getBoolean("system_clearalltasks", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.clearAllTasks,
                    diagnosticId = DiagnosticIds.CLEAR_ALL_TASKS
                ) {
                    SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook(
                    runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 2: SystemUI
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideDismissView,
            id = "hideDismissView",
            diagnosticId = DiagnosticIds.HIDE_DISMISS_VIEW,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_removedismiss"),
            condition = { prefs ->
                prefs.getBoolean("system_removedismiss", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideDismissView,
                    diagnosticId = DiagnosticIds.HIDE_DISMISS_VIEW
                ) {
                    SystemUINotificationHooks.HideDismissViewHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideLockScreenHint,
            id = "hideLockScreenHint",
            diagnosticId = DiagnosticIds.HIDE_LOCK_SCREEN_HINT,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidelshint"),
            condition = { prefs ->
                prefs.getBoolean("system_hidelshint", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideLockScreenHint,
                    diagnosticId = DiagnosticIds.HIDE_LOCK_SCREEN_HINT
                ) {
                    SystemLockScreenMoreHooks.HideLockScreenHintHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 2: Launcher
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.folderColumns,
            id = "folderColumns",
            diagnosticId = DiagnosticIds.FOLDER_COLUMNS,
            processScope = ProcessScope.LAUNCHER,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_folder_cols"),
            condition = { prefs ->
                prefs.getInt("launcher_folder_cols", 1) > 1
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.folderColumns,
                    diagnosticId = DiagnosticIds.FOLDER_COLUMNS
                ) {
                    LauncherFolderHooks.FolderColumnsHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.titleTopMargin,
            id = "titleTopMargin",
            diagnosticId = DiagnosticIds.TITLE_TOP_MARGIN,
            processScope = ProcessScope.LAUNCHER,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_titletopmargin"),
            condition = { prefs ->
                prefs.getInt("launcher_titletopmargin", 0) > 0
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.titleTopMargin,
                    diagnosticId = DiagnosticIds.TITLE_TOP_MARGIN
                ) {
                    LauncherIconHooks.TitleTopMarginHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 3: system_server
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noLightUpOnCharge,
            id = "noLightUpOnCharge",
            diagnosticId = DiagnosticIds.NO_LIGHT_UP_ON_CHARGE,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_nolightuponcharges"),
            condition = { prefs ->
                prefs.getStringAsInt("system_nolightuponcharges", 1) > 1
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noLightUpOnCharge,
                    diagnosticId = DiagnosticIds.NO_LIGHT_UP_ON_CHARGE
                ) {
                    SystemDisplayAndWindowHooks.NoLightUpOnChargeHook(
                    runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.allRotations,
            id = "allRotations",
            diagnosticId = DiagnosticIds.ALL_ROTATIONS,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_allrotations2"),
            condition = { prefs ->
                prefs.getStringAsInt("system_allrotations2", 1) > 1
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.allRotations,
                    diagnosticId = DiagnosticIds.ALL_ROTATIONS
                ) {
                    SystemAudioAndVisualAndMoreHooks.AllRotationsHook(
                    runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 3: SystemUI
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noNetworkSpeedSeparator,
            id = "noNetworkSpeedSeparator",
            diagnosticId = DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_nonetspeedseparator"),
            condition = { prefs ->
                prefs.getBoolean("system_nonetspeedseparator", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noNetworkSpeedSeparator,
                    diagnosticId = DiagnosticIds.NO_NETWORK_SPEED_SEPARATOR
                ) {
                    SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideIconsClock,
            id = "hideIconsClock",
            diagnosticId = DiagnosticIds.HIDE_ICONS_CLOCK,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_statusbaricons_clock"),
            condition = { prefs ->
                prefs.getBoolean("system_statusbaricons_clock", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideIconsClock,
                    diagnosticId = DiagnosticIds.HIDE_ICONS_CLOCK
                ) {
                    SystemUIStatusBarHooks.HideIconsClockHook(
                    runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 3: Launcher
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noUnlockAnimation,
            id = "noUnlockAnimation",
            diagnosticId = DiagnosticIds.NO_UNLOCK_ANIMATION,
            processScope = ProcessScope.LAUNCHER,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Launcher,
            preferenceKeys = setOf("launcher_nounlockanim"),
            condition = { prefs ->
                prefs.getBoolean("launcher_nounlockanim", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noUnlockAnimation,
                    diagnosticId = DiagnosticIds.NO_UNLOCK_ANIMATION
                ) {
                    LauncherAnimationHooks.NoUnlockAnimationHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.LAUNCHER_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 4: SystemUI screenshot
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.tempHideOverlaySystemUI,
            id = "tempHideOverlaySystemUI",
            diagnosticId = DiagnosticIds.TEMP_HIDE_OVERLAY_SYSTEMUI,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_screenshot_overlay"),
            condition = { prefs ->
                prefs.getBoolean("system_screenshot_overlay", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.tempHideOverlaySystemUI,
                    diagnosticId = DiagnosticIds.TEMP_HIDE_OVERLAY_SYSTEMUI
                ) {
                    SystemUIScreenshotHooks.TempHideOverlaySystemUIHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        )
    ) }

    /**
     * Wrap a legacy installer in a [HookInstaller] session so that the install
     * evidence (required/optional counts, fallback, reason code) is captured and
     * surfaced through [FeatureInstallResult.Installed] for the registry.
     */
    private inline fun legacyInstall(
        runtime: FeatureRuntime,
        compatResult: HookInstallResult,
        contract: HookTargetContract,
        diagnosticId: String,
        crossinline installer: () -> Unit
    ): FeatureInstallResult {
        val session = HookInstaller.withSession(
            resolver = runtime.resolver,
            contract = compatResult.resolvedContract ?: contract,
            diagnosticId = diagnosticId,
            classLoader = runtime.classLoader,
            compatibilityResult = compatResult
        ) {
            installer()
        }

        return when (session.installation) {
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
                session.detail ?: "$diagnosticId session failed"
            )
        }
    }

    /**
     * Build probe used by tests to prove that [registrySpecs] does not touch the
     * adapted list and that [specs] combines both lists without double-counting.
     * Production code leaves the counters at their default values.
     */
    internal object CatalogBuildProbe {
        @JvmField
        var registrySpecsBuilt: Int = 0

        @JvmField
        var adaptedSpecsBuilt: Int = 0

        @JvmStatic
        fun reset() {
            registrySpecsBuilt = 0
            adaptedSpecsBuilt = 0
        }
    }

    private val allSpecsInternal by lazy(LazyThreadSafetyMode.NONE) {
        registrySpecsInternal + adaptedSpecsInternal
    }

    private val specByCanonicalIdInternal by lazy(LazyThreadSafetyMode.NONE) {
        allSpecsInternal.associateBy(FeatureSpec::id)
    }

    /**
     * Returns only the specs that have been migrated to the production
     * [FeatureInstallRegistry]. Remaining catalog features are routed through
     * the typed [FeatureDispatcher] paths.
     */
    @JvmStatic
    fun registrySpecs(): List<FeatureSpec> {
        val result = registrySpecsInternal
        CatalogBuildProbe.registrySpecsBuilt += result.size
        return result
    }

    /**
     * Returns the catalog specs that are still installed through typed
     * legacy installers. These are still first-class [FeatureSpec]s and are
     * included in [specs].
     */
    @JvmStatic
    fun adaptedSpecs(): List<FeatureSpec> {
        val result = adaptedSpecsInternal
        CatalogBuildProbe.adaptedSpecsBuilt += result.size
        return result
    }

    /**
     * Returns a snapshot of all specs for documentation and audit.
     *
     * The list is cached; repeated calls do not allocate a new list.
     */
    @JvmStatic
    fun specs(): List<FeatureSpec> {
        registrySpecs()
        adaptedSpecs()
        return allSpecsInternal
    }

    /**
     * O(1) lookup of a [FeatureSpec] by its canonical id.
     *
     * Returns `null` when the id is unknown to this catalog.
     */
    @JvmStatic
    fun specByCanonicalId(id: String): FeatureSpec? = specByCanonicalIdInternal[id]
}
