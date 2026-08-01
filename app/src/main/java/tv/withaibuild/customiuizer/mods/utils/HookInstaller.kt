package tv.withaibuild.customiuizer.mods.utils

import java.lang.reflect.Method

/**
 * Thread-local recorder that turns legacy hook calls inside a canary installer
 * into a typed [HookInstallResult].
 *
 * - [withSession] is called by [tv.withaibuild.customiuizer.mods.catalog.FeatureCatalog]
 *   around the legacy installer.
 * - [ModuleHelper] wrappers call the `record*` methods only when [isRecording]
 *   is true, so non-catalog calls pay only a boolean check.
 * - The session is removed in `finally` so ThreadLocal is always cleared,
 *   even when the installer throws.
 *
 * The recorder does not hold Context, ClassLoader, MethodHook callbacks or
 * Throwables. It only keeps stable target ids, resolution/hook booleans and
 * a short failure reason.
 */
object HookInstaller {

    private class Session(
        val resolver: HookTargetResolver,
        val contract: HookTargetContract,
        val selectedVariant: FeatureTargetVariant,
        val diagnosticId: String,
        val classLoader: ClassLoader
    ) {
        private val candidateToRequirement = selectedVariant.candidateToRequirement()
        val recordsById: MutableMap<String, HookTargetRecord> =
            HashMap<String, HookTargetRecord>(selectedVariant.allTargets.size).apply {
                for (target in selectedVariant.allTargets) {
                    put(
                        target.id,
                        HookTargetRecord(
                            spec = target,
                            requirementId = candidateToRequirement[target.id] ?: "",
                            resolved = false,
                            installed = false,
                            failureReason = null,
                            installedCount = 0
                        )
                    )
                }
            }
    }

    private val session = ThreadLocal<Session?>()

    @JvmStatic
    fun isRecording(): Boolean = session.get() != null

    /**
     * Run a legacy installer inside a scoped install session.
     *
     * - Nested sessions are rejected with [IllegalStateException].
     * - The session is bound to the current thread and the supplied ClassLoader.
     * - The session is removed in `finally`, even on exception.
     * - The result is produced by [HookEvidenceEvaluator] and is an immutable
     *   value object.
     */
    @JvmStatic
    fun withSession(
        resolver: HookTargetResolver,
        contract: HookTargetContract,
        diagnosticId: String,
        classLoader: ClassLoader,
        compatibilityResult: HookInstallResult? = null,
        block: () -> Unit
    ): HookInstallResult {
        if (session.get() != null) {
            throw IllegalStateException("HookInstaller session already active on this thread")
        }
        val selected = compatibilityResult?.selectedVariant ?: contract.variants.single()
        val s = Session(resolver, contract, selected, diagnosticId, classLoader)
        session.set(s)
        try {
            if (compatibilityResult != null) {
                populateFromCompatibility(compatibilityResult)
            }
            block()
            return finalize(s)
        } finally {
            session.remove()
        }
    }

    private fun finalize(s: Session): HookInstallResult {
        val records = ArrayList<HookTargetRecord>(s.selectedVariant.allTargets.size)
        val requirementMap = s.selectedVariant.candidateToRequirement()
        for (spec in s.selectedVariant.allTargets) {
            records.add(
                s.recordsById[spec.id] ?: HookTargetRecord(
                    spec = spec,
                    requirementId = requirementMap[spec.id] ?: "",
                    resolved = false,
                    installed = false,
                    failureReason = null,
                    installedCount = 0
                )
            )
        }
        return HookEvidenceEvaluator.evaluate(s.selectedVariant, records, HookEvidenceEvaluator.EvidencePhase.INSTALLATION)
    }

    private fun populateFromCompatibility(result: HookInstallResult) {
        val s = session.get() ?: return
        for (record in result.records) {
            val sessionRecord = s.recordsById[record.spec.id] ?: continue
            val isPassive = record.spec.operation == HookOperation.CLASS_RESOLUTION ||
                    record.spec.operation == HookOperation.FIELD_RESOLUTION
            s.recordsById[record.spec.id] = sessionRecord.copy(
                resolved = record.resolved,
                installed = if (isPassive) record.resolved else sessionRecord.installed,
                failureReason = record.failureReason
            )
        }
    }

    /**
     * Resolve a class from the session resolver, or fall back to plain
     * ClassLoader resolution. Used by [ModuleHelper] wrappers to avoid a second
     * Class.forName call when a class was already resolved during compatibility.
     */
    @JvmStatic
    fun resolveClassIfRecording(className: String, classLoader: ClassLoader): Class<*>? {
        val s = session.get() ?: return null
        return s.resolver.resolveClass(className, s.diagnosticId)
            ?: runCatching { Class.forName(className, false, classLoader) }.getOrNull()
    }

    /**
     * Resolve a method from the session resolver. Returns null if no session is
     * active so the caller can fall back to the legacy path.
     */
    @JvmStatic
    fun resolveMethodIfRecording(className: String, methodName: String, parameterTypes: Array<Class<*>>): Method? {
        val s = session.get() ?: return null
        return s.resolver.resolveMethod(className, methodName, *parameterTypes, diagnosticId = s.diagnosticId)
    }

    /**
     * Mark a contract candidate as installed.
     *
     * [count] is used by [ModuleHelper.hookAllMethods] /
     * [ModuleHelper.hookAllConstructors] to record how many overloads/constructors
     * were actually hooked.
     *
     * The [operation] and [parameterTypes] are matched against the contract
     * candidate so exact-method and all-method targets do not impersonate each
     * other, and same-name overloads do not collide.
     */
    @JvmStatic
    fun recordInstall(
        className: String,
        memberName: String?,
        operation: HookOperation,
        parameterTypes: List<Class<*>>,
        count: Int = 1
    ) {
        val s = session.get() ?: return
        for (record in matchingRecords(s, className, memberName, operation, parameterTypes)) {
            val spec = record.spec
            s.recordsById[spec.id] = record.copy(
                installed = true,
                failureReason = null,
                installedCount = if (count > 1) count else record.installedCount
            )
        }
    }

    /** Mark a target as failed, but only if it is not already installed. */
    @JvmStatic
    fun recordFailure(
        className: String,
        memberName: String?,
        operation: HookOperation,
        parameterTypes: List<Class<*>>,
        reason: HookFailureReason
    ) {
        val s = session.get() ?: return
        for (record in matchingRecords(s, className, memberName, operation, parameterTypes)) {
            if (!record.installed && record.failureReason == null) {
                s.recordsById[record.spec.id] = record.copy(failureReason = reason)
            }
        }
    }

    /**
     * Mark every target that uses [className] as failed because the class itself
     * could not be found.
     */
    @JvmStatic
    fun recordClassFailure(className: String, reason: HookFailureReason) {
        val s = session.get() ?: return
        for (record in s.recordsById.values) {
            if (record.spec.className == className && !record.installed && record.failureReason == null) {
                s.recordsById[record.spec.id] = record.copy(failureReason = reason)
            }
        }
    }

    private fun matchingRecords(
        s: Session,
        className: String,
        memberName: String?,
        operation: HookOperation,
        parameterTypes: List<Class<*>>
    ): List<HookTargetRecord> {
        val result = ArrayList<HookTargetRecord>()
        for (record in s.recordsById.values) {
            if (record.spec.className == className &&
                record.spec.operation == operation &&
                (memberName == null || record.spec.memberName == memberName) &&
                parameterTypesMatch(record.spec, operation, parameterTypes)
            ) {
                result.add(record)
            }
        }
        return result
    }

    private fun parameterTypesMatch(
        spec: HookTargetSpec,
        operation: HookOperation,
        parameterTypes: List<Class<*>>
    ): Boolean {
        return when (operation) {
            HookOperation.ALL_METHODS_BY_NAME,
            HookOperation.ALL_CONSTRUCTORS,
            HookOperation.CLASS_RESOLUTION,
            HookOperation.FIELD_RESOLUTION -> true
            else -> spec.parameterTypes == parameterTypes
        }
    }
}
