package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Type-safe, declarative description of one CustoMIUIzer feature.
 *
 * @param id Stable, machine-readable feature ID. Must match one or more
 *        [tv.withaibuild.customiuizer.prefs.PreferenceEntry.ownerFeature] values.
 * @param diagnosticId Stable diagnostic ID used for telemetry and audit.
 * @param processTarget The OS process this feature installs into.
 * @param preferenceKeys The preference keys that control this feature. They
 *        are used for fast condition checks and schema validation.
 * @param condition Runtime check using the loaded preference map.
 * @param compatibilityCheck One-time probe that determines whether the ROM
 *        targets required by this feature are present.
 * @param restartTarget Smallest restart granularity for this feature.
 * @param hotReloadable Whether live preference changes can take effect without
 *        a restart.
 * @param installer The hook installation lambda. It receives a [FeatureRuntime]
 *        and must cast [FeatureRuntime.lpparam] to the appropriate libxposed
 *        parameter itself.
 */
data class FeatureSpec(
    val id: String,
    val diagnosticId: String,
    val processTarget: ProcessTarget,
    val preferenceKeys: Set<String>,
    val condition: (PrefMap<String, Any?>) -> Boolean,
    val compatibilityCheck: (FeatureRuntime) -> CompatibilityState,
    val restartTarget: RestartTarget,
    val hotReloadable: Boolean,
    val installer: (FeatureRuntime) -> Unit
)
