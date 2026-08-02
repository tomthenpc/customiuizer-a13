package tv.withaibuild.customiuizer.mods.catalog

import androidx.annotation.VisibleForTesting
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.EnabledState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import tv.withaibuild.customiuizer.mods.utils.FeatureInstallResult
import tv.withaibuild.customiuizer.mods.utils.FeatureState
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified, process-scoped feature install registry.
 *
 * The registry is the sole owner of feature lifecycle diagnostics. It turns a
 * [FeatureSpec] into a [FeatureInstallResult] while recording every relevant
 * state transition through [DiagnosticRecorder].
 *
 * Fatal JVM errors ([OutOfMemoryError], [ThreadDeath], [VirtualMachineError])
 * are always rethrown. All other failures are isolated to the single feature
 * and conservatively treated as transient unless the installer explicitly
 * returns [FeatureInstallResult.FailedPermanent].
 */
object FeatureInstallRegistry {

    private val specs = ConcurrentHashMap<String, FeatureSpec>()
    private val states = ConcurrentHashMap<String, FeatureState>()

    private fun normalizeId(id: String): String =
        id.lowercase().replace("_", "").replace(" ", "")

    fun register(spec: FeatureSpec) {
        specs[normalizeId(spec.id)] = spec
    }

    fun registerAll(specs: List<FeatureSpec>) {
        specs.forEach { register(it) }
    }

    @JvmStatic
    fun installById(
        id: String,
        scope: ProcessScope,
        phase: InstallPhase,
        runtime: FeatureRuntime
    ): FeatureInstallResult {
        val spec = specs[normalizeId(id)]
        if (spec == null) {
            DiagnosticRecorder.record(
                id = DiagnosticIds.UNKNOWN_FEATURE_ID,
                compatibility = CompatibilityState.INCOMPATIBLE,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = id
            )
            return FeatureInstallResult.FailedPermanent("unknown feature id: $id")
        }
        return install(spec, scope, phase, runtime)
    }

    private fun install(
        spec: FeatureSpec,
        scope: ProcessScope,
        phase: InstallPhase,
        runtime: FeatureRuntime
    ): FeatureInstallResult {
        val stateKey = "${runtime.processName}#${spec.id}"

        when (val current = states[stateKey]) {
            FeatureState.INSTALLED -> {
                DiagnosticRecorder.record(
                    id = spec.diagnosticId,
                    installation = InstallOutcome.ALREADY_INSTALLED,
                    reasonCode = ReasonCode.ALREADY_INSTALLED,
                    detail = spec.id
                )
                return FeatureInstallResult.AlreadyInstalled
            }
            FeatureState.FAILED_PERMANENT -> return FeatureInstallResult.FailedPermanent("previous permanent failure")
            FeatureState.INSTALLING -> return FeatureInstallResult.FailedTransient("reentrant install")
            else -> {}
        }

        if (!spec.condition(runtime.prefs)) {
            return FeatureInstallResult.Disabled
        }

        DiagnosticRecorder.record(
            id = spec.diagnosticId,
            enabled = EnabledState.REQUESTED,
            reasonCode = ReasonCode.REQUESTED,
            detail = spec.id
        )

        val specProcessScope = spec.processScope
        if (specProcessScope != null && specProcessScope != scope) {
            return recordAndReturn(
                spec,
                FeatureInstallResult.UnsupportedProcess(scope.name),
                compatibility = CompatibilityState.INCOMPATIBLE,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = "expected scope ${specProcessScope}, got $scope"
            )
        }

        if (!spec.processTarget.matches(runtime.processName)) {
            return recordAndReturn(
                spec,
                FeatureInstallResult.UnsupportedProcess(runtime.processName),
                compatibility = CompatibilityState.INCOMPATIBLE,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = "process target mismatch: ${runtime.processName}"
            )
        }

        val specInstallPhase = spec.installPhase
        if (specInstallPhase != null && specInstallPhase != phase) {
            return recordAndReturn(
                spec,
                FeatureInstallResult.WrongPhase(specInstallPhase, phase),
                compatibility = CompatibilityState.INCOMPATIBLE,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = "expected phase ${specInstallPhase}, got $phase"
            )
        }

        val compatibility = try {
            spec.compatibilityCheck(runtime)
        } catch (t: Throwable) {
            if (isFatal(t)) throw t
            return recordAndReturn(
                spec,
                FeatureInstallResult.Incompatible(t.message ?: "compatibility check threw"),
                compatibility = CompatibilityState.INCOMPATIBLE,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = t.message
            )
        }

        DiagnosticRecorder.record(
            id = spec.diagnosticId,
            compatibility = compatibility.compatibility,
            reasonCode = compatibility.reasonCode,
            detail = compatibility.detail
        )

        when (compatibility.compatibility) {
            CompatibilityState.INCOMPATIBLE -> {
                return recordAndReturn(
                    spec,
                    FeatureInstallResult.Incompatible("required target not compatible"),
                    compatibility = CompatibilityState.INCOMPATIBLE,
                    installation = InstallOutcome.FAILED,
                    reasonCode = ReasonCode.TARGET_NOT_FOUND,
                    detail = compatibility.detail
                )
            }
            CompatibilityState.DEGRADED,
            CompatibilityState.COMPATIBLE -> {
                // proceed
            }
        }

        states[stateKey] = FeatureState.INSTALLING

        val result = try {
            spec.installer(runtime, compatibility.hookResult)
        } catch (t: Throwable) {
            if (isFatal(t)) {
                states.remove(stateKey)
                throw t
            }
            classifyThrownException(t)
        }

        val finalState = when (result) {
            is FeatureInstallResult.Installed -> FeatureState.INSTALLED
            is FeatureInstallResult.AlreadyInstalled -> FeatureState.INSTALLED
            is FeatureInstallResult.FailedPermanent -> FeatureState.FAILED_PERMANENT
            else -> FeatureState.FAILED_TRANSIENT
        }
        states[stateKey] = finalState

        val (outcome, reasonCode) = when (result) {
            is FeatureInstallResult.Installed -> InstallOutcome.INSTALLED to ReasonCode.INSTALLER_SUCCEEDED
            is FeatureInstallResult.AlreadyInstalled -> InstallOutcome.ALREADY_INSTALLED to ReasonCode.ALREADY_INSTALLED
            is FeatureInstallResult.Disabled -> InstallOutcome.FAILED to ReasonCode.PREFERENCE_DISABLED
            is FeatureInstallResult.UnsupportedProcess,
            is FeatureInstallResult.WrongPhase,
            is FeatureInstallResult.Incompatible -> InstallOutcome.FAILED to ReasonCode.TARGET_NOT_FOUND
            is FeatureInstallResult.FailedTransient -> InstallOutcome.FAILED to ReasonCode.TRANSIENT_INSTALLER_FAILED
            is FeatureInstallResult.FailedPermanent -> InstallOutcome.FAILED to ReasonCode.PERMANENT_INSTALLER_FAILED
        }
        val detail = when (result) {
            is FeatureInstallResult.UnsupportedProcess -> result.scope
            is FeatureInstallResult.WrongPhase -> "expected ${result.expected}, got ${result.actual}"
            is FeatureInstallResult.Incompatible -> result.reason
            is FeatureInstallResult.FailedTransient -> result.reason
            is FeatureInstallResult.FailedPermanent -> result.reason
            else -> null
        }

        DiagnosticRecorder.record(
            id = spec.diagnosticId,
            installation = outcome,
            reasonCode = reasonCode,
            detail = detail,
            installSummary = (result as? FeatureInstallResult.Installed)?.installSummary
        )

        return result
    }

    private fun recordAndReturn(
        spec: FeatureSpec,
        result: FeatureInstallResult,
        compatibility: CompatibilityState? = null,
        installation: InstallOutcome? = null,
        reasonCode: ReasonCode,
        detail: String?
    ): FeatureInstallResult {
        if (compatibility != null) {
            DiagnosticRecorder.record(
                id = spec.diagnosticId,
                compatibility = compatibility,
                reasonCode = reasonCode,
                detail = detail
            )
        }
        if (installation != null) {
            DiagnosticRecorder.record(
                id = spec.diagnosticId,
                installation = installation,
                reasonCode = reasonCode,
                detail = detail
            )
        }
        return result
    }

    /**
     * Conservative classification for exceptions escaping the installer.
     *
     * - Class/member not found at install time usually means the target is not
     *   yet prepared by the framework; allow a retry.
     * - All other non-fatal exceptions are treated as transient to avoid a single
     *   hiccup permanently disabling a feature.
     */
    private fun classifyThrownException(t: Throwable): FeatureInstallResult =
        when (t) {
            is ClassNotFoundException,
            is NoClassDefFoundError,
            is NoSuchMethodError,
            is NoSuchFieldError ->
                FeatureInstallResult.FailedTransient("${t.javaClass.simpleName}: ${t.message}")
            else -> FeatureInstallResult.FailedTransient("${t.javaClass.simpleName}: ${t.message}")
        }

    private fun isFatal(t: Throwable): Boolean =
        t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError

    /** Test-only reset of per-process install state. Production code must not call this. */
    @VisibleForTesting
    internal fun clear() {
        states.clear()
    }
}
