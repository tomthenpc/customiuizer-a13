package tv.withaibuild.customiuizer.mods.catalog


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
 * The registry is the only production path that turns a [FeatureSpec] into a
 * [FeatureInstallResult]. It guards every install with:
 *
 * - id lookup
 * - per-process idempotency state
 * - preference condition
 * - process scope and install phase
 * - compatibility probe
 * - installer invocation
 * - diagnostic recording
 *
 * Fatal JVM errors ([OutOfMemoryError], [ThreadDeath], [VirtualMachineError])
 * are always rethrown. All other failures are isolated to the single feature.
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
            FeatureState.INSTALLED -> return FeatureInstallResult.AlreadyInstalled
            FeatureState.FAILED_PERMANENT -> return FeatureInstallResult.FailedPermanent("previous permanent failure")
            FeatureState.INSTALLING -> return FeatureInstallResult.FailedTransient("reentrant install")
            else -> {}
        }

        if (!spec.condition(runtime.prefs)) {
            DiagnosticRecorder.record(
                id = spec.diagnosticId,
                enabled = EnabledState.DISABLED,
                reasonCode = ReasonCode.PREFERENCE_DISABLED,
                detail = spec.id
            )
            return FeatureInstallResult.Disabled
        }

        val specProcessScope = spec.processScope
        if (specProcessScope != null && specProcessScope != scope) {
            return recordAndReturn(
                spec,
                FeatureInstallResult.UnsupportedProcess(scope.name),
                compatibility = CompatibilityState.INCOMPATIBLE,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = "expected scope ${specProcessScope}, got $scope"
            )
        }

        if (!spec.processTarget.matches(runtime.processName)) {
            return recordAndReturn(
                spec,
                FeatureInstallResult.UnsupportedProcess(runtime.processName),
                compatibility = CompatibilityState.INCOMPATIBLE,
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
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = "expected phase ${specInstallPhase}, got $phase"
            )
        }

        val compatibility = try {
            spec.compatibilityCheck(runtime)
        } catch (t: Throwable) {
            if (isFatal(t)) throw t
            DiagnosticRecorder.record(
                id = spec.diagnosticId,
                compatibility = CompatibilityState.INCOMPATIBLE,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = t.message
            )
            return FeatureInstallResult.Incompatible(t.message ?: "compatibility check threw")
        }

        when (compatibility) {
            CompatibilityState.INCOMPATIBLE -> {
                return recordAndReturn(
                    spec,
                    FeatureInstallResult.Incompatible("required target not compatible"),
                    compatibility = CompatibilityState.INCOMPATIBLE,
                    reasonCode = ReasonCode.TARGET_NOT_FOUND,
                    detail = spec.id
                )
            }
            CompatibilityState.DEGRADED -> {
                // proceed but a failure is transient
            }
            CompatibilityState.COMPATIBLE -> {
                // proceed
            }
        }

        states[stateKey] = FeatureState.INSTALLING
        val result = try {
            spec.installer(runtime)
        } catch (t: Throwable) {
            if (isFatal(t)) {
                states[stateKey] = FeatureState.FAILED_PERMANENT
                throw t
            }
            FeatureInstallResult.FailedTransient(t.message ?: "installer threw")
        }

        val finalState = when (result) {
            is FeatureInstallResult.Installed,
            is FeatureInstallResult.AlreadyInstalled -> FeatureState.INSTALLED
            is FeatureInstallResult.FailedPermanent -> FeatureState.FAILED_PERMANENT
            else -> FeatureState.FAILED_TRANSIENT
        }
        states[stateKey] = finalState

        val outcome = when (result) {
            is FeatureInstallResult.Installed,
            is FeatureInstallResult.AlreadyInstalled -> InstallOutcome.INSTALLED
            is FeatureInstallResult.FailedTransient -> InstallOutcome.DEGRADED
            else -> InstallOutcome.FAILED
        }
        val reasonCode = when (result) {
            is FeatureInstallResult.Installed,
            is FeatureInstallResult.AlreadyInstalled -> ReasonCode.INSTALLER_SUCCEEDED
            is FeatureInstallResult.Disabled -> ReasonCode.PREFERENCE_DISABLED
            is FeatureInstallResult.UnsupportedProcess,
            is FeatureInstallResult.WrongPhase,
            is FeatureInstallResult.Incompatible -> ReasonCode.TARGET_NOT_FOUND
            is FeatureInstallResult.FailedTransient,
            is FeatureInstallResult.FailedPermanent -> ReasonCode.INSTALLER_FAILED
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
            detail = detail
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
        DiagnosticRecorder.record(
            id = spec.diagnosticId,
            compatibility = compatibility,
            installation = installation,
            reasonCode = reasonCode,
            detail = detail
        )
        return result
    }

    private fun isFatal(t: Throwable): Boolean =
        t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError
}
