package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.PackagePermissions
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticState
import tv.withaibuild.customiuizer.mods.utils.HookTargetResolver
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Static, type-safe feature directory.
 *
 * [FeatureCatalog] holds [FeatureSpec] declarations and drives the install
 * lifecycle for each feature:
 *
 *     requested → preference condition → compatibility resolution
 *     → installed / degraded / failed → diagnostic snapshot
 *
 * [MainModule] preserves the original call order by invoking the catalog at
 * the same positions where the migrated hooks used to be called directly.
 */
object FeatureCatalog {

    private val features = listOf(
        FeatureSpec(
            id = "packagePermissions",
            diagnosticId = DiagnosticIds.PACKAGE_PERMISSIONS,
            processTarget = ProcessTarget.SystemServer,
            preferenceKeys = emptySet(),
            condition = { true },
            compatibilityCheck = { CompatibilityState.COMPATIBLE },
            restartTarget = RestartTarget.REBOOT,
            hotReloadable = false,
            installer = { runtime ->
                PackagePermissions.hook(runtime.lpparam as SystemServerStartingParam)
            }
        ),
        FeatureSpec(
            id = "statusBarClockTweak",
            diagnosticId = DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
            processTarget = ProcessTarget.Package("com.android.systemui"),
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
                val resolution = runtime.resolver.resolveFirstClass(
                    DiagnosticIds.STATUSBAR_CLOCK_TWEAK,
                    "com.android.systemui.statusbar.policy.MiuiStatusBarClockController",
                    "com.android.systemui.statusbar.policy.StatusBarClockController"
                )
                if (resolution.value != null) {
                    CompatibilityState.COMPATIBLE
                } else {
                    CompatibilityState.INCOMPATIBLE
                }
            },
            restartTarget = RestartTarget.SYSTEMUI_RESTART,
            hotReloadable = true,
            installer = { runtime ->
                SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook(
                    runtime.lpparam as PackageReadyParam
                )
            }
        )
    )

    @JvmStatic
    fun installForSystemServer(lpparam: SystemServerStartingParam) {
        @Suppress("UNCHECKED_CAST")
        val prefs = MainModule.mPrefs as PrefMap<String, Any?>
        val runtime = FeatureRuntime(
            processName = "android",
            lpparam = lpparam,
            classLoader = lpparam.classLoader,
            resolver = HookTargetResolver(lpparam.classLoader),
            prefs = prefs
        )
        install(runtime, ProcessTarget.SystemServer)
    }

    @JvmStatic
    fun installForPackage(lpparam: PackageReadyParam, pkg: String) {
        @Suppress("UNCHECKED_CAST")
        val prefs = MainModule.mPrefs as PrefMap<String, Any?>
        val runtime = FeatureRuntime(
            processName = pkg,
            lpparam = lpparam,
            classLoader = lpparam.classLoader,
            resolver = HookTargetResolver(lpparam.classLoader),
            prefs = prefs
        )
        install(runtime, ProcessTarget.Package(pkg))
    }

    private fun install(runtime: FeatureRuntime, target: ProcessTarget) {
        for (feature in features) {
            if (feature.processTarget != target) continue

            DiagnosticRecorder.record(
                feature.diagnosticId,
                DiagnosticState.REQUESTED,
                reason = "process=${runtime.processName}"
            )

            if (!feature.condition(runtime.prefs)) continue

            val compat = feature.compatibilityCheck(runtime)
            val resolution = runtime.resolver.lastResolution(feature.diagnosticId)

            when (compat) {
                CompatibilityState.COMPATIBLE,
                CompatibilityState.DEGRADED -> {
                    try {
                        feature.installer(runtime)
                    } catch (t: Throwable) {
                        DiagnosticRecorder.record(
                            feature.diagnosticId,
                            DiagnosticState.FAILED,
                            reason = "installer threw: ${t.javaClass.name}",
                            throwable = t
                        )
                        continue
                    }
                    DiagnosticRecorder.record(
                        feature.diagnosticId,
                        DiagnosticState.INSTALLED,
                        reason = "process=${runtime.processName}"
                    )
                }
                CompatibilityState.INCOMPATIBLE -> {
                    DiagnosticRecorder.record(
                        feature.diagnosticId,
                        DiagnosticState.FAILED,
                        reason = "incompatible: ${resolution?.failures?.joinToString(", ")}"
                    )
                    continue
                }
            }
        }
    }

    /**
     * Returns a snapshot of the specs for documentation and audit.
     */
    @JvmStatic
    fun specs(): List<FeatureSpec> = features
}
