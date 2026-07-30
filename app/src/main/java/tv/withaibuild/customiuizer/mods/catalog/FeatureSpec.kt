package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * How a configuration change in a running process can be handled.
 *
 * - NONE: the feature can only take effect after the activation restart.
 * - PARTIAL: some sub-settings can be changed without a restart.
 * - FULL: the feature can be fully enabled or disabled at runtime.
 */
enum class ConfigReloadMode { NONE, PARTIAL, FULL }

/**
 * A strongly-typed, auditable feature definition.
 *
 * @param id Stable feature id used as the key by [FeatureCatalog.installById].
 * @param diagnosticId Diagnostic id used for [DiagnosticRecorder] snapshots.
 * @param processTarget The host process where the feature is installed.
 * @param preferenceKeys The preference keys that the feature owns; must exist in
 *                       [PreferenceSchema] and must cover the key checked by
 *                       [condition].
 * @param condition Whether the feature is enabled for the current process.
 * @param compatibilityCheck Probe for the target class/method/field. It should
 *                           return [CompatibilityState] and emit its own
 *                           dimension record through [DiagnosticRecorder].
 * @param contract Optional typed target contract. When present, [FeatureCatalog]
 *                 uses it for both compatibility probing and install evidence.
 * @param installer Type-safe installer that returns an [InstallOutcome]. Old
 *                  Unit-style hooks must return [InstallOutcome.DISPATCHED].
 * @param activationRestartTarget The restart required for the feature to take
 *                                effect for the first time.
 * @param configReloadMode Whether the feature can be reconfigured at runtime.
 */
data class FeatureSpec(
    val contract: HookTargetContract? = null,
    val id: String,
    val diagnosticId: String,
    val processTarget: ProcessTarget,
    val preferenceKeys: Set<String>,
    val condition: (PrefMap<String, Any?>) -> Boolean,
    val compatibilityCheck: (FeatureRuntime) -> CompatibilityState,
    val installer: (FeatureRuntime) -> InstallOutcome,
    val activationRestartTarget: RestartTarget,
    val configReloadMode: ConfigReloadMode
)
