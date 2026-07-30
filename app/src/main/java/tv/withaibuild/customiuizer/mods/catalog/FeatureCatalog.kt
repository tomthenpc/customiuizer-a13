package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.PackagePermissions
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Static feature directory.
 *
 * Each [FeatureSpec] describes one installable unit: its ID, the target
 * package, the runtime condition, the restart requirement and the installer.
 * This keeps the feature metadata in one place while [MainModule] preserves
 * the original call order by invoking the catalog at the same positions where
 * the migrated hooks used to be called directly.
 */
object FeatureCatalog {

    private val systemServerFeatures = listOf(
        FeatureSpec(
            id = "packagePermissions",
            targetPackage = null,
            condition = { true },
            requiresReboot = true,
            requiresSystemUIRestart = false,
            installer = PackagePermissions::hook
        )
    )

    private val packageFeatures = listOf(
        FeatureSpec(
            id = "statusBarClockTweak",
            targetPackage = "com.android.systemui",
            condition = { prefs ->
                prefs.getBoolean("system_statusbar_clocktweak") ||
                prefs.getBoolean("system_cc_clocktweak") ||
                prefs.getBoolean("system_cc_hidedate") ||
                prefs.getString("system_cc_dateformat", "").isNotEmpty()
            },
            requiresReboot = false,
            requiresSystemUIRestart = true,
            installer = SystemStatusBarClockAndMoreHooks::StatusBarClockTweakHook
        )
    )

    @JvmStatic
    fun installForSystemServer(lpparam: SystemServerStartingParam) {
        @Suppress("UNCHECKED_CAST")
        val prefs = MainModule.mPrefs as PrefMap<String, Any?>
        for (feature in systemServerFeatures) {
            if (feature.condition(prefs)) {
                feature.installer(lpparam)
            }
        }
    }

    @JvmStatic
    fun installForPackage(lpparam: PackageReadyParam, pkg: String) {
        @Suppress("UNCHECKED_CAST")
        val prefs = MainModule.mPrefs as PrefMap<String, Any?>
        for (feature in packageFeatures) {
            if (feature.targetPackage == pkg && feature.condition(prefs)) {
                feature.installer(lpparam)
            }
        }
    }

    /**
     * Returns a snapshot of the specs for documentation and audit.
     */
    @JvmStatic
    fun specs(): List<FeatureSpec<*>> = systemServerFeatures + packageFeatures
}

/**
 * Declarative description of one CustoMIUIzer feature.
 *
 * @param id Stable, machine-readable feature ID.
 * @param targetPackage The package name where this feature is installed.
 *        `null` means the system_server process.
 * @param condition Runtime check using the loaded preference map.
 * @param requiresReboot Whether the feature needs a full reboot to take effect.
 * @param requiresSystemUIRestart Whether a SystemUI restart is sufficient.
 * @param installer The hook installation lambda. Kept as a method reference
 *        to avoid object initializers running at class-load time.
 */
data class FeatureSpec<in P : Any>(
    val id: String,
    val targetPackage: String?,
    val condition: (PrefMap<String, Any?>) -> Boolean,
    val requiresReboot: Boolean,
    val requiresSystemUIRestart: Boolean,
    val installer: (P) -> Unit
)
