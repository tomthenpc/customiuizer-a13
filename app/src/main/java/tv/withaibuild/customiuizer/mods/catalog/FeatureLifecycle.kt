package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.utils.ProcessScope

/**
 * Unified, A14-style lifecycle for catalog features.
 *
 * [FeatureLifecycle] is the single source of truth for the end-to-end state of
 * a feature from "user disabled" through "installed" or "failed". It maps to
 * the existing A13 [DiagnosticState] ordering but adds explicit transient and
 * permanent failure states and separates process rejection from compatibility.
 */
enum class FeatureLifecycle {
    DISABLED,
    UNSUPPORTED_PROCESS,
    INCOMPATIBLE,
    READY,
    INSTALLING,
    INSTALLED,
    ALREADY_INSTALLED,
    FAILED_TRANSIENT,
    FAILED_PERMANENT
}

/**
 * Immutable result of a single feature install attempt.
 *
 * [lifecycle] is the human/machine-readable conclusion; [diagnostic] is the
 * A13 legacy diagnostic state that maps to it for backward compatibility.
 */
data class FeatureInstallResult(
    val lifecycle: FeatureLifecycle,
    val diagnostic: DiagnosticState,
    val reasonCode: String? = null,
    val detail: String? = null
)

object FeatureLifecycles {

    @JvmStatic
    fun fromDiagnosticState(state: DiagnosticState): FeatureLifecycle = when (state) {
        DiagnosticState.DISABLED -> FeatureLifecycle.DISABLED
        DiagnosticState.REQUESTED,
        DiagnosticState.COMPATIBLE -> FeatureLifecycle.READY
        DiagnosticState.DEGRADED -> FeatureLifecycle.READY
        DiagnosticState.INCOMPATIBLE -> FeatureLifecycle.INCOMPATIBLE
        DiagnosticState.DISPATCHED,
        DiagnosticState.INSTALLED -> FeatureLifecycle.INSTALLED
        DiagnosticState.FAILED -> FeatureLifecycle.FAILED_TRANSIENT
    }

    @JvmStatic
    fun fromProcessScope(scope: ProcessScope): FeatureLifecycle = when (scope) {
        ProcessScope.SYSTEM_SERVER,
        ProcessScope.SYSTEM_UI,
        ProcessScope.SYSTEM_UI_PLUGIN,
        ProcessScope.LAUNCHER,
        ProcessScope.SETTINGS_MAIN,
        ProcessScope.SECURITY_CENTER_MAIN,
        ProcessScope.POWER_KEEPER,
        ProcessScope.WALLPAPER,
        ProcessScope.MEDIA,
        ProcessScope.PHONE,
        ProcessScope.PACKAGE_INSTALLER,
        ProcessScope.INPUT_METHOD,
        ProcessScope.GENERIC_APP -> FeatureLifecycle.READY
        ProcessScope.SETTINGS_REMOTE,
        ProcessScope.SECURITY_CENTER_REMOTE,
        ProcessScope.SECURITY_CENTER_BOOTAWARE,
        ProcessScope.NETWORK_STACK,
        ProcessScope.UNSUPPORTED -> FeatureLifecycle.UNSUPPORTED_PROCESS
    }

    @JvmStatic
    fun fromInstallOutcome(outcome: InstallOutcome, alreadyInstalled: Boolean = false): FeatureLifecycle = when (outcome) {
        InstallOutcome.INSTALLED,
        InstallOutcome.DISPATCHED -> if (alreadyInstalled) FeatureLifecycle.ALREADY_INSTALLED else FeatureLifecycle.INSTALLED
        InstallOutcome.DEGRADED -> FeatureLifecycle.FAILED_TRANSIENT
        InstallOutcome.FAILED -> FeatureLifecycle.FAILED_PERMANENT
    }
}
