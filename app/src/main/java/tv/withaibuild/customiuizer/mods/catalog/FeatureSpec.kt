package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.utils.FeatureInstallResult
import tv.withaibuild.customiuizer.mods.utils.HookTargetContract
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
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
 * @param id Stable feature id used as the key by [FeatureInstallRegistry.installById].
 * @param diagnosticId Diagnostic id used for [DiagnosticRecorder] snapshots.
 * @param processScope The [ProcessScope] in which the feature may be installed.
 * @param processTarget The host process where the feature is installed.
 * @param installPhase The [InstallPhase] at which the feature may be installed.
 * @param preferenceKeys The preference keys that the feature owns; must exist in
 *                       [PreferenceSchema] and must cover the key checked by
 *                       [condition].
 * @param condition Whether the feature is enabled for the current process.
 * @param compatibilityCheck Probe for the target class/method/field. It should
 *                           return [CompatibilityState] and emit its own
 *                           dimension record through [DiagnosticRecorder].
 * @param contract Optional typed target contract. When present, [FeatureInstallRegistry]
 *                 uses it for both compatibility probing and install evidence.
 * @param installer Type-safe installer that returns a [FeatureInstallResult].
 * @param activationRestartTarget The restart required for the feature to take
 *                                effect for the first time.
 * @param configReloadMode Whether the feature can be reconfigured at runtime.
 */
data class FeatureSpec(
    val contract: HookTargetContract? = null,
    val id: String,
    val diagnosticId: String,
    val processScope: ProcessScope? = null,
    val processTarget: ProcessTarget,
    val installPhase: InstallPhase? = null,
    val preferenceKeys: Set<String>,
    val condition: (PrefMap<String, Any>) -> Boolean,
    val compatibilityCheck: (FeatureRuntime) -> CompatibilityState,
    val installer: (FeatureRuntime) -> FeatureInstallResult,
    val activationRestartTarget: RestartTarget,
    val configReloadMode: ConfigReloadMode
)
