package tv.withaibuild.customiuizer.mods.utils

import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.InstallSummary

/**
 * Outcome of installing a single feature.
 *
 * This is the only production result type for feature installation. Each
 * variant is explicit about why a feature was or was not activated.
 */
sealed interface FeatureInstallResult {

    /** Whether this result means the feature is active in the target process. */
    val isActive: Boolean
        get() = this is Installed || this is AlreadyInstalled

    data class Installed(val installSummary: InstallSummary? = null) : FeatureInstallResult
    data object AlreadyInstalled : FeatureInstallResult
    data object Disabled : FeatureInstallResult
    data class UnsupportedProcess(val scope: String? = null) : FeatureInstallResult
    data class WrongPhase(val expected: InstallPhase, val actual: InstallPhase) : FeatureInstallResult
    data class Incompatible(val reason: String) : FeatureInstallResult
    data class FailedTransient(val reason: String) : FeatureInstallResult
    data class FailedPermanent(val reason: String) : FeatureInstallResult

    fun toInstallOutcome(): InstallOutcome = when (this) {
        is Installed,
        is AlreadyInstalled -> InstallOutcome.INSTALLED
        is Disabled,
        is UnsupportedProcess,
        is WrongPhase,
        is Incompatible -> InstallOutcome.FAILED
        is FailedTransient -> InstallOutcome.FAILED
        is FailedPermanent -> InstallOutcome.FAILED
    }

    fun toDiagnosticState(): DiagnosticState = when (this) {
        is Installed,
        is AlreadyInstalled -> DiagnosticState.INSTALLED
        is Disabled -> DiagnosticState.DISABLED
        is UnsupportedProcess,
        is WrongPhase -> DiagnosticState.INCOMPATIBLE
        is Incompatible -> DiagnosticState.INCOMPATIBLE
        is FailedTransient -> DiagnosticState.FAILED
        is FailedPermanent -> DiagnosticState.FAILED
    }
}
