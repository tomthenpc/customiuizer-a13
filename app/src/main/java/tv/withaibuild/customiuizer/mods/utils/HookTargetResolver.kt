package tv.withaibuild.customiuizer.mods.utils

import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, per-[ClassLoader] cache for hook targets.
 *
 * - Class / method / constructor / field resolution results (including negative ones) are
 *   cached so hot paths do not repeat reflection.
 * - [resolveFirstClass] / [resolveFirstMethod] / [resolveFirstField] try a list
 *   of candidates in order and return the first hit. The first candidate
 *   produces [CompatibilityState.COMPATIBLE], any later candidate produces
 *   [CompatibilityState.DEGRADED], and total failure produces
 *   [CompatibilityState.INCOMPATIBLE].
 * - Resolution results are emitted once per diagnostic id through
 *   [DiagnosticRecorder] with stable [ReasonCode]s.
 */
class HookTargetResolver(private val classLoader: ClassLoader) {

    private val cache = ConcurrentHashMap<String, Any?>()
    private val resolutionLog = ConcurrentHashMap<String, ResolutionLog>()

    /** Resolve a class by name, caching the result (including a negative result). */
    fun resolveClass(className: String, diagnosticId: String = DiagnosticIds.HOOK_TARGET_RESOLVER): Class<*>? {
        val k = key("class", className)
        val cached = cache[k]
        if (cached != null) return if (cached === NULL) null else cached as Class<*>

        return try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader)
            if (clazz == null) cache[k] = NULL else cache[k] = clazz
            clazz
        } catch (t: Throwable) {
            cache[k] = NULL
            null
        }
    }

    /**
     * Resolve a declared method by name and parameter types.
     * Returns null on any failure (no throw) and caches the negative result.
     */
    fun resolveMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>,
        diagnosticId: String = DiagnosticIds.HOOK_TARGET_RESOLVER
    ): Method? {
        val paramNames = parameterTypes.map { it.name }.toTypedArray()
        val k = key("method", className, methodName, *paramNames)
        val cached = cache[k]
        if (cached != null) return if (cached === NULL) null else cached as Method

        val clazz = resolveClass(className, diagnosticId)
        if (clazz == null) {
            cache[k] = NULL
            return null
        }

        return try {
            val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
            method.isAccessible = true
            cache[k] = method
            method
        } catch (t: Throwable) {
            cache[k] = NULL
            null
        }
    }

    /**
     * Resolve all declared methods with the given name, regardless of signature.
     * Returns null when the class itself cannot be found.
     */
    fun resolveAllMethods(
        className: String,
        methodName: String,
        diagnosticId: String = DiagnosticIds.HOOK_TARGET_RESOLVER
    ): List<Method>? {
        val clazz = resolveClass(className, diagnosticId) ?: return null
        return clazz.declaredMethods.filter { it.name == methodName }
    }

    /**
     * Resolve a declared constructor by parameter types.
     * Returns null on any failure (no throw) and caches the negative result.
     */
    fun resolveConstructor(
        className: String,
        vararg parameterTypes: Class<*>,
        diagnosticId: String = DiagnosticIds.HOOK_TARGET_RESOLVER
    ): Constructor<*>? {
        val paramNames = parameterTypes.map { it.name }.toTypedArray()
        val k = key("constructor", className, *paramNames)
        val cached = cache[k]
        if (cached != null) return if (cached === NULL) null else cached as Constructor<*>

        val clazz = resolveClass(className, diagnosticId)
        if (clazz == null) {
            cache[k] = NULL
            return null
        }

        return try {
            val ctor = clazz.getDeclaredConstructor(*parameterTypes)
            ctor.isAccessible = true
            cache[k] = ctor
            ctor
        } catch (t: Throwable) {
            cache[k] = NULL
            null
        }
    }

    /**
     * Resolve all declared constructors of a class.
     * Returns null when the class itself cannot be found.
     */
    fun resolveAllConstructors(
        className: String,
        diagnosticId: String = DiagnosticIds.HOOK_TARGET_RESOLVER
    ): List<Constructor<*>>? {
        val clazz = resolveClass(className, diagnosticId) ?: return null
        return clazz.declaredConstructors.toList()
    }

    /**
     * Resolve a declared field by name.
     * Returns null on any failure (no throw) and caches the negative result.
     */
    fun resolveField(
        className: String,
        fieldName: String,
        diagnosticId: String = DiagnosticIds.HOOK_TARGET_RESOLVER
    ): Field? {
        val k = key("field", className, fieldName)
        val cached = cache[k]
        if (cached != null) return if (cached === NULL) null else cached as Field

        val clazz = resolveClass(className, diagnosticId)
        if (clazz == null) {
            cache[k] = NULL
            return null
        }

        return try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            cache[k] = field
            field
        } catch (t: Throwable) {
            cache[k] = NULL
            null
        }
    }

    /**
     * Evaluate a contract for compatibility using this resolver.
     *
     * Resolves every candidate target and then produces a single, immutable
     * [HookInstallResult] through [HookEvidenceEvaluator]. The returned result
     * uses the compatibility evidence (resolved == true) and is passed to
     * [HookInstaller.withSession] to seed the install pass.
     */
    fun evaluateContract(
        contract: HookTargetContract,
        diagnosticId: String
    ): Pair<CompatibilityState, HookInstallResult> {
        val reqToCand = contract.candidateToRequirement()
        val records = contract.allTargets.map { spec ->
            resolveCandidate(spec, reqToCand[spec.id] ?: "", diagnosticId)
        }
        val result = HookEvidenceEvaluator.evaluate(contract, records, HookEvidenceEvaluator.EvidencePhase.COMPATIBILITY)
        return result.compatibility!! to result
    }

    /**
     * Try a list of candidate class names in order.
     *
     * @return a [Resolution] containing the first matching class or `null` and
     *         a stable summary of the failed attempts.
     */
    fun resolveFirstClass(
        diagnosticId: String,
        vararg classNames: String
    ): Resolution<Class<*>> = resolveCandidates(
        diagnosticId,
        "class",
        classNames.toList()
    ) { className ->
        resolveClass(className, diagnosticId)
    }

    /**
     * Try a list of candidate classes for a method in order.
     */
    fun resolveFirstMethod(
        diagnosticId: String,
        methodName: String,
        vararg parameterTypes: Class<*>,
        candidateClasses: List<String>
    ): Resolution<Method> = resolveCandidates(
        diagnosticId,
        "method",
        candidateClasses
    ) { className ->
        resolveMethod(className, methodName, *parameterTypes, diagnosticId = diagnosticId)
    }

    /**
     * Try a list of candidate classes for a field in order.
     */
    fun resolveFirstField(
        diagnosticId: String,
        fieldName: String,
        vararg candidateClasses: String
    ): Resolution<Field> = resolveCandidates(
        diagnosticId,
        "field",
        candidateClasses.toList()
    ) { className ->
        resolveField(className, fieldName, diagnosticId)
    }

    /** Returns the resolution log for the last [resolveFirstClass] etc. call. */
    fun lastResolution(diagnosticId: String): ResolutionLog? = resolutionLog[diagnosticId]

    private fun resolveCandidate(
        spec: HookTargetSpec,
        requirementId: String,
        diagnosticId: String
    ): HookTargetRecord {
        val (resolved, failureReason, count) = when (spec.operation) {
            HookOperation.CLASS_RESOLUTION -> {
                val clazz = resolveClass(spec.className, diagnosticId)
                Triple(clazz != null, if (clazz == null) HookFailureReason.CLASS_NOT_FOUND else null, 0)
            }
            HookOperation.FIELD_RESOLUTION -> {
                val field = spec.memberName?.let { resolveField(spec.className, it, diagnosticId) }
                Triple(field != null, failureReasonFor(spec.className, field, diagnosticId), 0)
            }
            HookOperation.EXACT_METHOD -> {
                val method = spec.memberName?.let {
                    resolveMethod(spec.className, it, *spec.parameterTypes.toTypedArray(), diagnosticId = diagnosticId)
                }
                Triple(method != null, failureReasonFor(spec.className, method, diagnosticId), 0)
            }
            HookOperation.ALL_METHODS_BY_NAME -> {
                val methods = spec.memberName?.let { resolveAllMethods(spec.className, it, diagnosticId) }
                Triple(
                    methods?.isNotEmpty() == true,
                    failureReasonForAllMethods(spec.className, methods, diagnosticId),
                    methods?.size ?: 0
                )
            }
            HookOperation.EXACT_CONSTRUCTOR -> {
                val ctor = resolveConstructor(spec.className, *spec.parameterTypes.toTypedArray(), diagnosticId = diagnosticId)
                Triple(ctor != null, failureReasonFor(spec.className, ctor, diagnosticId), 0)
            }
            HookOperation.ALL_CONSTRUCTORS -> {
                val ctors = resolveAllConstructors(spec.className, diagnosticId)
                Triple(
                    ctors?.isNotEmpty() == true,
                    failureReasonForAllConstructors(spec.className, ctors, diagnosticId),
                    ctors?.size ?: 0
                )
            }
        }

        return HookTargetRecord(
            spec = spec,
            requirementId = requirementId,
            resolved = resolved,
            installed = false,
            failureReason = failureReason,
            installedCount = if (count > 0) count else 0
        )
    }

    private fun failureReasonFor(className: String, found: Any?, diagnosticId: String): HookFailureReason? {
        return if (found != null) null else {
            if (resolveClass(className, diagnosticId) == null) HookFailureReason.CLASS_NOT_FOUND else HookFailureReason.MEMBER_NOT_FOUND
        }
    }

    private fun failureReasonForAllMethods(
        className: String,
        methods: List<Method>?,
        diagnosticId: String
    ): HookFailureReason? {
        return when {
            methods == null -> if (resolveClass(className, diagnosticId) == null) HookFailureReason.CLASS_NOT_FOUND else HookFailureReason.MEMBER_NOT_FOUND
            methods.isNotEmpty() -> null
            else -> if (resolveClass(className, diagnosticId) == null) HookFailureReason.CLASS_NOT_FOUND else HookFailureReason.MEMBER_NOT_FOUND
        }
    }

    private fun failureReasonForAllConstructors(
        className: String,
        ctors: List<Constructor<*>>?,
        diagnosticId: String
    ): HookFailureReason? {
        return when {
            ctors == null -> if (resolveClass(className, diagnosticId) == null) HookFailureReason.CLASS_NOT_FOUND else HookFailureReason.MEMBER_NOT_FOUND
            ctors.isNotEmpty() -> null
            else -> if (resolveClass(className, diagnosticId) == null) HookFailureReason.CLASS_NOT_FOUND else HookFailureReason.MEMBER_NOT_FOUND
        }
    }

    private fun <T : Any> resolveCandidates(
        diagnosticId: String,
        kind: String,
        candidates: List<String>,
        resolver: (String) -> T?
    ): Resolution<T> {
        val failures = mutableListOf<String>()
        var hit: T? = null
        var hitCandidate: String? = null
        var hitIndex = -1

        for ((index, candidate) in candidates.withIndex()) {
            val result = resolver(candidate)
            if (result != null) {
                hit = result
                hitCandidate = candidate
                hitIndex = index
                break
            }
            failures.add("$candidate: not found")
        }

        val log = ResolutionLog(kind = kind, hit = hitCandidate, failures = failures)
        resolutionLog[diagnosticId] = log

        val (compatibility, reasonCode, detail) = when {
            hit == null -> Triple(
                CompatibilityState.INCOMPATIBLE,
                ReasonCode.TARGET_NOT_FOUND,
                "no ${kind} candidate resolved; tried: ${failures.joinToString(", ")}"
            )
            hitIndex == 0 -> Triple(
                CompatibilityState.COMPATIBLE,
                ReasonCode.PRIMARY_TARGET_FOUND,
                hitCandidate
            )
            else -> Triple(
                CompatibilityState.DEGRADED,
                ReasonCode.FALLBACK_TARGET_FOUND,
                hitCandidate
            )
        }

        DiagnosticRecorder.record(
            id = diagnosticId,
            compatibility = compatibility,
            reasonCode = reasonCode,
            detail = detail
        )

        return Resolution(hit, log, compatibility)
    }

    private fun key(vararg parts: String): String = parts.joinToString("#")

    private companion object {
        private val NULL = Any()
    }
}

/**
 * Pure JVM aggregator that produces the contract compatibility or installation
 * outcome in one pass.
 *
 * Requirements are evaluated one at a time. Counts are by requirement, not by
 * candidate target count. A non-primary candidate in an [AnyOfRequirement]
 * marks fallback as used but does not make the requirement fail.
 */
object HookEvidenceEvaluator {

    enum class EvidencePhase { COMPATIBILITY, INSTALLATION }

    fun evaluate(
        contract: HookTargetContract,
        records: List<HookTargetRecord>,
        phase: EvidencePhase
    ): HookInstallResult {
        val recordsById = records.associateBy { it.spec.id }
        val requiredFailures = mutableListOf<HookTargetRecord>()
        val optionalFailures = mutableListOf<HookTargetRecord>()
        var requiredInstalled = 0
        var requiredTotal = 0
        var optionalInstalled = 0
        var optionalTotal = 0
        var fallbackUsed = false
        val fallbackUsedIds = mutableListOf<String>()

        for (req in contract.requirements) {
            val isRequired = req.criticality == Criticality.REQUIRED
            if (isRequired) requiredTotal++ else optionalTotal++

            val candidates = when (req) {
                is SingleTargetRequirement -> listOf(req.target)
                is AnyOfRequirement -> req.candidates
            }

            var selected: HookTargetRecord? = null
            for (c in candidates) {
                val record = recordsById[c.id]
                if (record != null && isEvidence(record, phase)) {
                    selected = record
                    break
                }
            }

            if (selected != null) {
                if (isRequired) requiredInstalled++ else optionalInstalled++
                if (req is AnyOfRequirement && req.candidates.isNotEmpty()) {
                    if (selected.spec.id != req.candidates.first().id) {
                        fallbackUsed = true
                        fallbackUsedIds.add(selected.spec.id)
                    }
                }
            } else {
                for (c in candidates) {
                    val record = recordsById[c.id]
                    if (record != null) {
                        if (isRequired) requiredFailures.add(record) else optionalFailures.add(record)
                    }
                }
            }
        }

        val compatibility = when {
            requiredInstalled < requiredTotal -> CompatibilityState.INCOMPATIBLE
            fallbackUsed || optionalInstalled < optionalTotal -> CompatibilityState.DEGRADED
            else -> CompatibilityState.COMPATIBLE
        }

        val installation = when {
            requiredInstalled < requiredTotal -> InstallOutcome.FAILED
            fallbackUsed || optionalInstalled < optionalTotal -> InstallOutcome.DEGRADED
            (requiredInstalled + optionalInstalled) > 0 -> InstallOutcome.INSTALLED
            else -> InstallOutcome.FAILED
        }

        val reasonCode = when (phase) {
            EvidencePhase.COMPATIBILITY -> when (compatibility) {
                CompatibilityState.COMPATIBLE -> ReasonCode.PRIMARY_TARGET_FOUND
                CompatibilityState.DEGRADED -> if (fallbackUsed) ReasonCode.FALLBACK_TARGET_FOUND else ReasonCode.PRIMARY_TARGET_FOUND
                CompatibilityState.INCOMPATIBLE -> ReasonCode.TARGET_NOT_FOUND
            }
            EvidencePhase.INSTALLATION -> when (installation) {
                InstallOutcome.INSTALLED -> ReasonCode.INSTALLER_SUCCEEDED
                InstallOutcome.DEGRADED -> if (fallbackUsed) ReasonCode.FALLBACK_TARGET_FOUND else ReasonCode.INSTALLER_SUCCEEDED
                InstallOutcome.FAILED -> ReasonCode.INSTALLER_FAILED
                InstallOutcome.DISPATCHED -> ReasonCode.INSTALLER_DISPATCHED
            }
        }

        val detail = buildString {
            append("required $requiredInstalled/$requiredTotal, optional $optionalInstalled/$optionalTotal")
            if (fallbackUsed) {
                append(", fallbacks=").append(fallbackUsedIds.joinToString(","))
            }
            if (requiredFailures.isNotEmpty()) {
                append(", required_failed=").append(requiredFailures.joinToString(",") { it.spec.id })
            }
            if (optionalFailures.isNotEmpty()) {
                append(", optional_failed=").append(optionalFailures.joinToString(",") { it.spec.id })
            }
        }

        return HookInstallResult(
            records = records,
            compatibility = compatibility,
            installation = installation,
            requiredInstalled = requiredInstalled,
            requiredTotal = requiredTotal,
            optionalInstalled = optionalInstalled,
            optionalTotal = optionalTotal,
            fallbackUsed = fallbackUsed,
            requiredFailures = requiredFailures,
            optionalFailures = optionalFailures,
            reasonCode = reasonCode,
            detail = detail
        )
    }

    private fun isEvidence(record: HookTargetRecord, phase: EvidencePhase): Boolean {
        return if (phase == EvidencePhase.COMPATIBILITY) record.resolved else record.installed
    }
}

/**
 * Result of a candidate resolution.
 *
 * @param value the resolved target, or null if no candidate matched.
 * @param log a stable, human-readable summary of the resolution attempt.
 * @param compatibility the [CompatibilityState] implied by the candidate list.
 */
data class Resolution<out T : Any>(
    val value: T?,
    val log: ResolutionLog,
    val compatibility: CompatibilityState
)

/**
 * Stable resolution log for one diagnostic ID.
 *
 * @param kind 'class', 'method' or 'field'.
 * @param hit the candidate that matched, or null.
 * @param failures the list of failed candidates with their reasons.
 */
data class ResolutionLog(
    val kind: String,
    val hit: String?,
    val failures: List<String>
)
