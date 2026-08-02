package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.mods.PackagePermissions
import tv.withaibuild.customiuizer.mods.SystemAudioAndVolumeHooks
import tv.withaibuild.customiuizer.mods.SystemAudioAndVisualAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemChargingAndWallpaperHooks
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks
import tv.withaibuild.customiuizer.mods.SystemFreeformAndMultiWindowHooks
import tv.withaibuild.customiuizer.mods.SystemLockScreenHooks
import tv.withaibuild.customiuizer.mods.SystemLockScreenMoreHooks
import tv.withaibuild.customiuizer.mods.SystemUIScreenshotHooks
import tv.withaibuild.customiuizer.mods.SystemNotificationMoreHooks
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks
import tv.withaibuild.customiuizer.mods.SystemStatusBarMoreHooks
import tv.withaibuild.customiuizer.mods.SystemShareAndOpenWithHooks
import tv.withaibuild.customiuizer.mods.SystemSecurityAndSystemHooks
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
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideStatusBarBeforeScreenshot,
            id = "hideStatusBarBeforeScreenshot",
            diagnosticId = DiagnosticIds.HIDE_STATUS_BAR_BEFORE_SCREENSHOT,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("system_hidestatusbar_whenscreenshot"),
            condition = { prefs ->
                prefs.getBoolean("system_hidestatusbar_whenscreenshot", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideStatusBarBeforeScreenshot,
                    diagnosticId = DiagnosticIds.HIDE_STATUS_BAR_BEFORE_SCREENSHOT
                ) {
                    SystemUIScreenshotHooks.HideStatusBarBeforeScreenshotHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.hideNavBarBeforeScreenshot,
            id = "hideNavBarBeforeScreenshot",
            diagnosticId = DiagnosticIds.HIDE_NAV_BAR_BEFORE_SCREENSHOT,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf("controls_hidenavbar_whenscreenshot"),
            condition = { prefs ->
                prefs.getBoolean("controls_hidenavbar_whenscreenshot", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.hideNavBarBeforeScreenshot,
                    diagnosticId = DiagnosticIds.HIDE_NAV_BAR_BEFORE_SCREENSHOT
                ) {
                    SystemUIScreenshotHooks.HideNavBarBeforeScreenshotHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 5: Share/OpenWith menu cleaning
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.cleanShareMenu,
            id = "cleanShareMenu",
            diagnosticId = DiagnosticIds.CLEAN_SHARE_MENU,
            processScope = ProcessScope.ANDROID_PACKAGE,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Package("android"),
            preferenceKeys = setOf("system_cleanshare", "system_cleanshare_apps"),
            condition = { prefs ->
                prefs.getBoolean("system_cleanshare", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.cleanShareMenu,
                    diagnosticId = DiagnosticIds.CLEAN_SHARE_MENU
                ) {
                    SystemShareAndOpenWithHooks.CleanShareMenuHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.cleanShareMenuService,
            id = "cleanShareMenuService",
            diagnosticId = DiagnosticIds.CLEAN_SHARE_MENU_SERVICE,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_cleanshare", "system_cleanshare_apps"),
            condition = { prefs ->
                prefs.getBoolean("system_cleanshare", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.cleanShareMenuService,
                    diagnosticId = DiagnosticIds.CLEAN_SHARE_MENU_SERVICE
                ) {
                    SystemShareAndOpenWithHooks.CleanShareMenuServiceHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.cleanOpenWithMenu,
            id = "cleanOpenWithMenu",
            diagnosticId = DiagnosticIds.CLEAN_OPEN_WITH_MENU,
            processScope = ProcessScope.ANDROID_PACKAGE,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.Package("android"),
            preferenceKeys = setOf("system_cleanopenwith", "system_cleanopenwith_apps"),
            condition = { prefs ->
                prefs.getBoolean("system_cleanopenwith", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.cleanOpenWithMenu,
                    diagnosticId = DiagnosticIds.CLEAN_OPEN_WITH_MENU
                ) {
                    SystemShareAndOpenWithHooks.CleanOpenWithMenuHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.cleanOpenWithMenuService,
            id = "cleanOpenWithMenuService",
            diagnosticId = DiagnosticIds.CLEAN_OPEN_WITH_MENU_SERVICE,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_cleanopenwith", "system_cleanopenwith_apps"),
            condition = { prefs ->
                prefs.getBoolean("system_cleanopenwith", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.cleanOpenWithMenuService,
                    diagnosticId = DiagnosticIds.CLEAN_OPEN_WITH_MENU_SERVICE
                ) {
                    SystemShareAndOpenWithHooks.CleanOpenWithMenuServiceHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 6: Charging info and lockscreen wallpaper
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.chargingInfo,
            id = "chargingInfo",
            diagnosticId = DiagnosticIds.CHARGING_INFO,
            processScope = ProcessScope.SYSTEM_UI,
            installPhase = InstallPhase.PACKAGE_READY,
            processTarget = ProcessTarget.SystemUI,
            preferenceKeys = setOf(
                "system_charginginfo",
                "system_charginginfo_current",
                "system_charginginfo_voltage",
                "system_charginginfo_wattage",
                "system_charginginfo_temp",
                "system_charginginfo_view"
            ),
            condition = { prefs ->
                prefs.getBoolean("system_charginginfo", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.chargingInfo,
                    diagnosticId = DiagnosticIds.CHARGING_INFO
                ) {
                    SystemChargingAndWallpaperHooks.ChargingInfoHook(
                        runtime.lpparam as PackageReadyParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.SYSTEMUI_RESTART,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.setLockscreenWallpaper,
            id = "setLockscreenWallpaper",
            diagnosticId = DiagnosticIds.SET_LOCKSCREEN_WALLPAPER,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_lswallpaper"),
            condition = { prefs ->
                prefs.getBoolean("system_lswallpaper", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.setLockscreenWallpaper,
                    diagnosticId = DiagnosticIds.SET_LOCKSCREEN_WALLPAPER
                ) {
                    SystemChargingAndWallpaperHooks.SetLockscreenWallpaperHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 7: SystemServer security hooks
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noVersionCheck,
            id = "noVersionCheck",
            diagnosticId = DiagnosticIds.NO_VERSION_CHECK,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_downgrade"),
            condition = { prefs ->
                prefs.getBoolean("system_downgrade", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noVersionCheck,
                    diagnosticId = DiagnosticIds.NO_VERSION_CHECK
                ) {
                    SystemSecurityAndSystemHooks.NoVersionCheckHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.removeActStartConfirm,
            id = "removeActStartConfirm",
            diagnosticId = DiagnosticIds.REMOVE_ACT_START_CONFIRM,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_remove_startactconfirm"),
            condition = { prefs ->
                prefs.getBoolean("system_remove_startactconfirm", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.removeActStartConfirm,
                    diagnosticId = DiagnosticIds.REMOVE_ACT_START_CONFIRM
                ) {
                    SystemSecurityAndSystemHooks.RemoveActStartConfirmHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.forceClose,
            id = "forceClose",
            diagnosticId = DiagnosticIds.FORCE_CLOSE,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_forceclose", "system_forceclose_apps"),
            condition = { prefs ->
                prefs.getBoolean("system_forceclose", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.forceClose,
                    diagnosticId = DiagnosticIds.FORCE_CLOSE
                ) {
                    SystemSecurityAndSystemHooks.ForceCloseHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.disableSystemIntegrity,
            id = "disableSystemIntegrity",
            diagnosticId = DiagnosticIds.DISABLE_SYSTEM_INTEGRITY,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_disableintegrity"),
            condition = { prefs ->
                prefs.getBoolean("system_disableintegrity", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.disableSystemIntegrity,
                    diagnosticId = DiagnosticIds.DISABLE_SYSTEM_INTEGRITY
                ) {
                    SystemSecurityAndSystemHooks.DisableSystemIntegrityHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 8: SystemServer notification hooks
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.orientationLock,
            id = "orientationLock",
            diagnosticId = DiagnosticIds.ORIENTATION_LOCK,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_orientationlock"),
            condition = { prefs ->
                prefs.getBoolean("system_orientationlock", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.orientationLock,
                    diagnosticId = DiagnosticIds.ORIENTATION_LOCK
                ) {
                    SystemNotificationMoreHooks.OrientationLockHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noDucking,
            id = "noDucking",
            diagnosticId = DiagnosticIds.NO_DUCKING,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_noducking"),
            condition = { prefs ->
                prefs.getBoolean("system_noducking", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noDucking,
                    diagnosticId = DiagnosticIds.NO_DUCKING
                ) {
                    SystemNotificationMoreHooks.NoDuckingHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.disable72hStrongAuth,
            id = "disable72hStrongAuth",
            diagnosticId = DiagnosticIds.DISABLE_72H_STRONG_AUTH,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_lockscreen_disable_strongauth_72h"),
            condition = { prefs ->
                prefs.getBoolean("system_lockscreen_disable_strongauth_72h", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.disable72hStrongAuth,
                    diagnosticId = DiagnosticIds.DISABLE_72H_STRONG_AUTH
                ) {
                    SystemNotificationMoreHooks.Disable72hStrongAuthHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.disableAnyNotificationBlock,
            id = "disableAnyNotificationBlock",
            diagnosticId = DiagnosticIds.DISABLE_ANY_NOTIFICATION_BLOCK,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_disableanynotif"),
            condition = { prefs ->
                prefs.getBoolean("system_disableanynotif", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.disableAnyNotificationBlock,
                    diagnosticId = DiagnosticIds.DISABLE_ANY_NOTIFICATION_BLOCK
                ) {
                    SystemNotificationMoreHooks.DisableAnyNotificationBlockHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 9: Lock screen and call interruption hooks
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.enhancedSecurity,
            id = "enhancedSecurity",
            diagnosticId = DiagnosticIds.ENHANCED_SECURITY,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_securelock"),
            condition = { prefs ->
                prefs.getBoolean("system_securelock", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.enhancedSecurity,
                    diagnosticId = DiagnosticIds.ENHANCED_SECURITY
                ) {
                    SystemLockScreenHooks.EnhancedSecurityHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.appLock,
            id = "appLock",
            diagnosticId = DiagnosticIds.APP_LOCK,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_applock"),
            condition = { prefs ->
                prefs.getBoolean("system_applock", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.appLock,
                    diagnosticId = DiagnosticIds.APP_LOCK
                ) {
                    SystemLockScreenMoreHooks.AppLockHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.skipAppLock,
            id = "skipAppLock",
            diagnosticId = DiagnosticIds.SKIP_APP_LOCK,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_applock_skip", "system_applock_skip_activities"),
            condition = { prefs ->
                prefs.getBoolean("system_applock_skip", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.skipAppLock,
                    diagnosticId = DiagnosticIds.SKIP_APP_LOCK
                ) {
                    SystemLockScreenMoreHooks.SkipAppLockHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noCallInterruption,
            id = "noCallInterruption",
            diagnosticId = DiagnosticIds.NO_CALL_INTERRUPTION,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_ignorecalls", "system_ignorecalls_apps"),
            condition = { prefs ->
                prefs.getBoolean("system_ignorecalls", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noCallInterruption,
                    diagnosticId = DiagnosticIds.NO_CALL_INTERRUPTION
                ) {
                    SystemAudioAndVisualAndMoreHooks.NoCallInterruptionHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 10: Security and floating window hooks
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.removeSecure,
            id = "removeSecure",
            diagnosticId = DiagnosticIds.REMOVE_SECURE,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_removesecure"),
            condition = { prefs ->
                prefs.getBoolean("system_removesecure", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.removeSecure,
                    diagnosticId = DiagnosticIds.REMOVE_SECURE
                ) {
                    SystemSecurityAndSystemHooks.RemoveSecureHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noSignatureVerify,
            id = "noSignatureVerify",
            diagnosticId = DiagnosticIds.NO_SIGNATURE_VERIFY,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_apksign"),
            condition = { prefs ->
                prefs.getBoolean("system_apksign", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noSignatureVerify,
                    diagnosticId = DiagnosticIds.NO_SIGNATURE_VERIFY
                ) {
                    SystemSecurityAndSystemHooks.NoSignatureVerifyServiceHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.noDarkForce,
            id = "noDarkForce",
            diagnosticId = DiagnosticIds.NO_DARK_FORCE,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_nodarkforce"),
            condition = { prefs ->
                prefs.getBoolean("system_nodarkforce", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.noDarkForce,
                    diagnosticId = DiagnosticIds.NO_DARK_FORCE
                ) {
                    SystemSecurityAndSystemHooks.NoDarkForceHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.stickyFloatingWindows,
            id = "stickyFloatingWindows",
            diagnosticId = DiagnosticIds.STICKY_FLOATING_WINDOWS,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_fw_sticky"),
            condition = { prefs ->
                prefs.getBoolean("system_fw_sticky", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.stickyFloatingWindows,
                    diagnosticId = DiagnosticIds.STICKY_FLOATING_WINDOWS
                ) {
                    SystemFreeformAndMultiWindowHooks.StickyFloatingWindowsHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        // Catalog expansion batch 11: Display, audio and notification hooks
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.screenAnim,
            id = "screenAnim",
            diagnosticId = DiagnosticIds.SCREEN_ANIM,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_screenanim_duration"),
            condition = { prefs ->
                prefs.getInt("system_screenanim_duration", 0) > 0
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.screenAnim,
                    diagnosticId = DiagnosticIds.SCREEN_ANIM
                ) {
                    SystemDisplayAndWindowHooks.ScreenAnimHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.rotationAnimation,
            id = "rotationAnimation",
            diagnosticId = DiagnosticIds.ROTATION_ANIMATION,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_rotateanim"),
            condition = { prefs ->
                prefs.getStringAsInt("system_rotateanim", 1) > 1
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.rotationAnimation,
                    diagnosticId = DiagnosticIds.ROTATION_ANIMATION
                ) {
                    SystemDisplayAndWindowHooks.RotationAnimationHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.notificationVolume,
            id = "notificationVolume",
            diagnosticId = DiagnosticIds.NOTIFICATION_VOLUME,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_separatevolume"),
            condition = { prefs ->
                prefs.getBoolean("system_separatevolume", false)
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.notificationVolume,
                    diagnosticId = DiagnosticIds.NOTIFICATION_VOLUME
                ) {
                    SystemAudioAndVolumeHooks.NotificationVolumeServiceHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.selectiveVibration,
            id = "selectiveVibration",
            diagnosticId = DiagnosticIds.SELECTIVE_VIBRATION,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_vibration", "system_vibration_apps"),
            condition = { prefs ->
                prefs.getStringAsInt("system_vibration", 1) > 1
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.selectiveVibration,
                    diagnosticId = DiagnosticIds.SELECTIVE_VIBRATION
                ) {
                    SystemNotificationMoreHooks.SelectiveVibrationHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
            configReloadMode = ConfigReloadMode.NONE
        ),
        FeatureSpec(
            compatibilityPolicy = CompatibilityPolicy.CONTRACT_REQUIRED,
            contract = CatalogContracts.wallpaperScaleLevel,
            id = "wallpaperScaleLevel",
            diagnosticId = DiagnosticIds.WALLPAPER_SCALE_LEVEL,
            processScope = ProcessScope.SYSTEM_SERVER,
            installPhase = InstallPhase.SYSTEM_SERVER_STARTING,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = setOf("system_other_wallpaper_scale"),
            condition = { prefs ->
                prefs.getInt("system_other_wallpaper_scale", 6) > 6
            },
            installer = { runtime, compatResult ->
                legacyInstall(
                    runtime = runtime,
                    compatResult = compatResult,
                    contract = CatalogContracts.wallpaperScaleLevel,
                    diagnosticId = DiagnosticIds.WALLPAPER_SCALE_LEVEL
                ) {
                    SystemNotificationMoreHooks.WallpaperScaleLevelHook(
                        runtime.lpparam as SystemServerStartingParam
                    )
                }
            },
            activationRestartTarget = RestartTarget.REBOOT,
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
