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
 * Real identity is the canonical [FeatureSpec.id]. User input (including
 * aliases and case-insensitive or punctuation-stripped variants) is normalized
 * to a canonical id for lookup. Collisions between canonical ids, normalized
 * forms, or aliases are rejected explicitly.
 *
 * Fatal JVM errors ([OutOfMemoryError], [ThreadDeath], [VirtualMachineError])
 * are always rethrown. All other failures are isolated to the single feature
 * and conservatively treated as transient unless the installer explicitly
 * returns [FeatureInstallResult.FailedPermanent].
 */
object FeatureInstallRegistry {

    private val canonicalSpecs = ConcurrentHashMap<String, FeatureSpec>()
    private val normalizedToCanonical = ConcurrentHashMap<String, String>()
    private val states = ConcurrentHashMap<FeatureStateKey, FeatureState>()

    /** Guards the multi-map registration so a conflict never leaves partial writes. */
    private val registerLock = Any()

    /** Typed, collision-proof per-process feature key. */
    private data class FeatureStateKey(
        val processName: String,
        val canonicalId: String
    )

    private fun normalizeId(id: String): String =
        FeatureIdentity.normalizeLookupId(id)

    /**
     * Register a [FeatureSpec] with the registry.
     *
     * The canonical [FeatureSpec.id] is the real primary key. Aliases and
     * case/punctuation variants are accepted for lookup, but any collision is
     * rejected with a descriptive exception.
     *
     * Registering the exact same [FeatureSpec] instance more than once is
     * idempotent.
     */
    fun register(spec: FeatureSpec) {
        synchronized(registerLock) {
            val canonicalId = spec.id

            canonicalSpecs[canonicalId]?.let { existing ->
                if (existing == spec) {
                    // Idempotent re-registration of the same spec.
                    return@synchronized
                }
                throw IllegalArgumentException(
                    "canonicalId collision: '$canonicalId' already registered by " +
                    "FeatureSpec(${existing.id}, ${existing.diagnosticId}); " +
                    "attempted by FeatureSpec(${spec.id}, ${spec.diagnosticId})"
                )
            }

            val normalizedCanonical = normalizeId(canonicalId)
            val existingCanonicalForNormalized = normalizedToCanonical[normalizedCanonical]
            if (existingCanonicalForNormalized != null && existingCanonicalForNormalized != canonicalId) {
                throw IllegalArgumentException(
                    "normalizedId collision: '$normalizedCanonical' resolves to canonical " +
                    "'$existingCanonicalForNormalized'; new canonical '$canonicalId' " +
                    "from FeatureSpec(${spec.id}, ${spec.diagnosticId})"
                )
            }

            for (alias in spec.aliases) {
                val normalizedAlias = normalizeId(alias)
                val existingForAlias = normalizedToCanonical[normalizedAlias]
                if (existingForAlias != null && existingForAlias != canonicalId) {
                    throw IllegalArgumentException(
                        "alias collision: alias '$alias' (normalized '$normalizedAlias') " +
                        "resolves to canonical '$existingForAlias'; new canonical '$canonicalId' " +
                        "from FeatureSpec(${spec.id}, ${spec.diagnosticId})"
                    )
                }
            }

            canonicalSpecs[canonicalId] = spec
            normalizedToCanonical[normalizedCanonical] = canonicalId
            for (alias in spec.aliases) {
                normalizedToCanonical[normalizeId(alias)] = canonicalId
            }
        }
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
        val normalized = normalizeId(id)
        val canonicalId = normalizedToCanonical[normalized]
        if (canonicalId == null) {
            DiagnosticRecorder.record(
                id = DiagnosticIds.UNKNOWN_FEATURE_ID,
                compatibility = CompatibilityState.INCOMPATIBLE,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = id
            )
            return FeatureInstallResult.FailedPermanent("unknown feature id: $id")
        }

        val spec = canonicalSpecs[canonicalId]
            ?: return FeatureInstallResult.FailedPermanent("missing spec for canonical id: $canonicalId")

        return install(spec, scope, phase, runtime)
    }

    private fun install(
        spec: FeatureSpec,
        scope: ProcessScope,
        phase: InstallPhase,
        runtime: FeatureRuntime
    ): FeatureInstallResult {
        val stateKey = FeatureStateKey(runtime.processName, spec.id)

        // Fast, non-mutating state check. Concurrent readers may pass this point,
        // but only one will win the INSTALLING claim below.
        when (val current = states[stateKey]) {
            FeatureState.INSTALLED -> {
                record(spec, installation = InstallOutcome.ALREADY_INSTALLED, reasonCode = ReasonCode.ALREADY_INSTALLED)
                return FeatureInstallResult.AlreadyInstalled
            }
            FeatureState.FAILED_PERMANENT -> return FeatureInstallResult.FailedPermanent("previous permanent failure")
            FeatureState.INSTALLING -> return FeatureInstallResult.FailedTransient("install already in progress")
            else -> {}
        }

        // Condition evaluation must never crash the whole process. The feature
        // simply fails transiently and can be retried later.
        val conditionResult = runCondition(spec, runtime)
        if (conditionResult != null) {
            return conditionResult
        }

        // REQUESTED: exactly once per successful attempt that reaches the registry.
        record(spec, enabled = EnabledState.REQUESTED, reasonCode = ReasonCode.REQUESTED)

        val specProcessScope = spec.processScope
        if (specProcessScope != null && specProcessScope != scope) {
            record(
                spec,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = "expected scope ${specProcessScope}, got $scope"
            )
            return FeatureInstallResult.UnsupportedProcess(scope.name)
        }

        if (!spec.processTarget.matches(runtime.processName)) {
            record(
                spec,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.UNKNOWN,
                detail = "process target mismatch: ${runtime.processName}"
            )
            return FeatureInstallResult.UnsupportedProcess(runtime.processName)
        }

        val specInstallPhase = spec.installPhase
        if (specInstallPhase != null && specInstallPhase != phase) {
            record(
                spec,
                installation = InstallOutcome.FAILED,
                reasonCode = ReasonCode.TARGET_NOT_FOUND,
                detail = "expected phase ${specInstallPhase}, got $phase"
            )
            return FeatureInstallResult.WrongPhase(specInstallPhase, phase)
        }

        // Acquire atomic ownership of this feature in this process.
        val claim = acquireInstalling(stateKey)
        when (claim) {
            is InstallClaim.AlreadyInstalled -> {
                record(spec, installation = InstallOutcome.ALREADY_INSTALLED, reasonCode = ReasonCode.ALREADY_INSTALLED)
                return FeatureInstallResult.AlreadyInstalled
            }
            is InstallClaim.PermanentlyFailed -> return FeatureInstallResult.FailedPermanent("previous permanent failure")
            is InstallClaim.Installing -> return FeatureInstallResult.FailedTransient("install already in progress")
            is InstallClaim.Acquired -> { /* continue */ }
        }

        // From this point on the current thread owns INSTALLING. Every exit path
        // must replace INSTALLING with a terminal or transient state.
        val result = runCompatibilityAndInstaller(spec, scope, phase, runtime, stateKey)

        val finalState = when (result) {
            is FeatureInstallResult.Installed -> FeatureState.INSTALLED
            is FeatureInstallResult.AlreadyInstalled -> FeatureState.INSTALLED
            is FeatureInstallResult.FailedPermanent -> FeatureState.FAILED_PERMANENT
            else -> FeatureState.FAILED_TRANSIENT
        }
        states[stateKey] = finalState

        // INSTALLATION: exactly once per install attempt.
        val (outcome, reasonCode, detail) = installationOutcome(result)
        record(spec, installation = outcome, reasonCode = reasonCode, detail = detail, installSummary = (result as? FeatureInstallResult.Installed)?.installSummary)

        return result
    }

    private sealed interface InstallClaim {
        data object Acquired : InstallClaim
        data object AlreadyInstalled : InstallClaim
        data object Installing : InstallClaim
        data object PermanentlyFailed : InstallClaim
    }

    /**
     * Atomically transitions the state to [FeatureState.INSTALLING] for this
     * process/feature pair. Only one thread per key can own the INSTALLING state
     * at a time; others observe the previous terminal or transient state.
     */
    private fun acquireInstalling(stateKey: FeatureStateKey): InstallClaim {
        var previous: FeatureState? = null
        states.compute(stateKey) { _, current ->
            previous = current
            when (current) {
                FeatureState.INSTALLED -> FeatureState.INSTALLED
                FeatureState.FAILED_PERMANENT -> FeatureState.FAILED_PERMANENT
                FeatureState.INSTALLING -> FeatureState.INSTALLING
                else -> FeatureState.INSTALLING
            }
        }
        return when (previous ?: FeatureState.NOT_INSTALLED) {
            FeatureState.INSTALLED -> InstallClaim.AlreadyInstalled
            FeatureState.FAILED_PERMANENT -> InstallClaim.PermanentlyFailed
            FeatureState.INSTALLING -> InstallClaim.Installing
            else -> InstallClaim.Acquired
        }
    }

    /**
     * Evaluates the feature condition under an isolated exception boundary.
     *
     * Fatal JVM errors propagate. All other throwables are converted to a
     * transient failure with a stable [ReasonCode.CONDITION_EVALUATION_FAILED].
     */
    private fun runCondition(
        spec: FeatureSpec,
        runtime: FeatureRuntime
    ): FeatureInstallResult? = try {
        if (spec.condition(runtime.prefs)) {
            null
        } else {
            FeatureInstallResult.Disabled
        }
    } catch (t: Throwable) {
        if (isFatal(t)) throw t
        val detail = "${t.javaClass.simpleName}: ${t.message}"
        record(
            spec,
            installation = InstallOutcome.FAILED,
            reasonCode = ReasonCode.CONDITION_EVALUATION_FAILED,
            detail = detail
        )
        FeatureInstallResult.FailedTransient(detail)
    }

    /**
     * Runs the compatibility probe and installer while this thread owns the
     * [FeatureState.INSTALLING] state. The state is always released to a terminal
     * or transient value before returning.
     */
    private fun runCompatibilityAndInstaller(
        spec: FeatureSpec,
        scope: ProcessScope,
        phase: InstallPhase,
        runtime: FeatureRuntime,
        stateKey: FeatureStateKey
    ): FeatureInstallResult {
        val compatibility = try {
            spec.compatibilityCheck(runtime)
        } catch (t: Throwable) {
            if (isFatal(t)) {
                states.remove(stateKey)
                throw t
            }
            states[stateKey] = FeatureState.FAILED_TRANSIENT
            record(spec, compatibility = CompatibilityState.INCOMPATIBLE, reasonCode = ReasonCode.TARGET_NOT_FOUND, detail = t.message)
            return FeatureInstallResult.Incompatible(t.message ?: "compatibility check threw")
        }

        // COMPATIBILITY: exactly once per install attempt.
        record(spec, compatibility = compatibility.compatibility, reasonCode = compatibility.reasonCode, detail = compatibility.detail)

        if (compatibility.compatibility == CompatibilityState.INCOMPATIBLE) {
            states[stateKey] = FeatureState.FAILED_TRANSIENT
            return FeatureInstallResult.Incompatible("required target not compatible")
        }

        return try {
            spec.installer(runtime, compatibility.hookResult)
        } catch (t: Throwable) {
            if (isFatal(t)) {
                states.remove(stateKey)
                throw t
            }
            states[stateKey] = FeatureState.FAILED_TRANSIENT
            classifyThrownException(t)
        }
    }

    private fun record(
        spec: FeatureSpec,
        enabled: EnabledState? = null,
        compatibility: CompatibilityState? = null,
        installation: InstallOutcome? = null,
        reasonCode: ReasonCode,
        detail: String? = null,
        installSummary: tv.withaibuild.customiuizer.mods.diagnostics.InstallSummary? = null
    ) {
        DiagnosticRecorder.record(
            id = spec.diagnosticId,
            enabled = enabled,
            compatibility = compatibility,
            installation = installation,
            reasonCode = reasonCode,
            detail = detail,
            installSummary = installSummary
        )
    }

    private fun installationOutcome(result: FeatureInstallResult): Triple<InstallOutcome, ReasonCode, String?> {
        val outcome = when (result) {
            is FeatureInstallResult.Installed -> result.installSummary?.installation ?: InstallOutcome.INSTALLED
            is FeatureInstallResult.AlreadyInstalled -> InstallOutcome.ALREADY_INSTALLED
            is FeatureInstallResult.Disabled -> InstallOutcome.FAILED
            is FeatureInstallResult.UnsupportedProcess,
            is FeatureInstallResult.WrongPhase,
            is FeatureInstallResult.Incompatible -> InstallOutcome.FAILED
            is FeatureInstallResult.FailedTransient -> InstallOutcome.FAILED
            is FeatureInstallResult.FailedPermanent -> InstallOutcome.FAILED
        }
        val reasonCode = when (result) {
            is FeatureInstallResult.Installed -> result.installSummary?.reasonCode ?: ReasonCode.INSTALLER_SUCCEEDED
            is FeatureInstallResult.AlreadyInstalled -> ReasonCode.ALREADY_INSTALLED
            is FeatureInstallResult.Disabled -> ReasonCode.PREFERENCE_DISABLED
            is FeatureInstallResult.UnsupportedProcess,
            is FeatureInstallResult.WrongPhase,
            is FeatureInstallResult.Incompatible -> ReasonCode.TARGET_NOT_FOUND
            is FeatureInstallResult.FailedTransient -> ReasonCode.TRANSIENT_INSTALLER_FAILED
            is FeatureInstallResult.FailedPermanent -> ReasonCode.PERMANENT_INSTALLER_FAILED
        }
        val detail = when (result) {
            is FeatureInstallResult.UnsupportedProcess -> result.scope
            is FeatureInstallResult.WrongPhase -> "expected ${result.expected}, got ${result.actual}"
            is FeatureInstallResult.Incompatible -> result.reason
            is FeatureInstallResult.FailedTransient -> result.reason
            is FeatureInstallResult.FailedPermanent -> result.reason
            is FeatureInstallResult.Disabled -> null
            else -> null
        }
        return Triple(outcome, reasonCode, detail)
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
    internal fun clearStatesForTesting() {
        states.clear()
    }

    /** Test-only reset of both registered specs and runtime state. */
    @VisibleForTesting
    internal fun resetRegistryForTesting() {
        canonicalSpecs.clear()
        normalizedToCanonical.clear()
        states.clear()
    }
}
