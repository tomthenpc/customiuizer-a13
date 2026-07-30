package tv.withaibuild.customiuizer.mods.utils

import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, per-[ClassLoader] cache for hook targets.
 *
 * - Class / method / field resolution results (including negative ones) are
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
