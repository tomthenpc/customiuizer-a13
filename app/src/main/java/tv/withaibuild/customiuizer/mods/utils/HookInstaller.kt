package tv.withaibuild.customiuizer.mods.utils

import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode
import java.lang.reflect.Method

/**
 * Thread-local recorder that turns legacy hook calls inside a canary installer
 * into a typed [HookInstallResult].
 *
 * - `begin()` is called by [tv.withaibuild.customiuizer.mods.catalog.FeatureCatalog]
 *   before the legacy installer runs.
 * - `ModuleHelper` wrappers call the `record*` methods only when [isRecording]
 *   is true, so non-catalog calls pay only a boolean check.
 * - `end()` returns the final [HookInstallResult] and removes the session.
 *
 * The recorder does not hold Context, ClassLoader, MethodHook callbacks or
 * Throwables. It only keeps stable target ids, resolution/hook booleans and
 * a short failure reason.
 */
object HookInstaller {

    private class Session(
        val resolver: HookTargetResolver,
        val contract: HookTargetContract,
        val diagnosticId: String,
        val classLoader: ClassLoader
    ) {
        val recordsById: MutableMap<String, HookTargetRecord> =
            contract.allTargets.associateTo(LinkedHashMap()) { it.id to HookTargetRecord(spec = it) }
        val installedCount: MutableMap<String, Int> = mutableMapOf()
    }

    private val session = ThreadLocal<Session?>()

    @JvmStatic
    fun isRecording(): Boolean = session.get() != null

    /**
     * Start a new install session. Reuses the same [HookTargetResolver] that the
     * compatibility probe used, so resolved classes are cached.
     *
     * @param compatibilityResult Optional result from
     *        [tv.withaibuild.customiuizer.mods.utils.evaluateContract]. Field and
     *        class targets that were resolved are pre-marked as installed so the
     *        install phase does not have to repeat reflection for them.
     */
    @JvmStatic
    fun begin(
        resolver: HookTargetResolver,
        contract: HookTargetContract,
        diagnosticId: String,
        classLoader: ClassLoader,
        compatibilityResult: HookInstallResult? = null
    ) {
        session.set(Session(resolver, contract, diagnosticId, classLoader))
        if (compatibilityResult != null) {
            populateFromCompatibility(compatibilityResult)
        }
    }

    private fun populateFromCompatibility(result: HookInstallResult) {
        val s = session.get() ?: return
        for (record in result.records) {
            val sessionRecord = s.recordsById[record.spec.id] ?: continue
            val isPassive = record.spec.kind == HookTargetKind.CLASS || record.spec.kind == HookTargetKind.FIELD
            s.recordsById[record.spec.id] = sessionRecord.copy(
                resolved = record.resolved,
                installed = if (isPassive) record.resolved else sessionRecord.installed,
                failureReason = record.failureReason
            )
        }
    }

    /**
     * End the current session and return the final install result.
     *
     * If no session is active, returns the [HookInstallResult.DISPATCHED] marker.
     */
    @JvmStatic
    fun end(): HookInstallResult {
        val s = session.get() ?: return HookInstallResult.DISPATCHED
        session.remove()

        val records = s.contract.allTargets.map { spec ->
            val base = s.recordsById[spec.id] ?: HookTargetRecord(spec)
            val count = s.installedCount[spec.id] ?: 0
            if (count > 0 && base.installed) base.copy(installedCount = count) else base
        }

        return records.toHookInstallResult(s.contract, s.diagnosticId)
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
        return if (parameterTypes.isEmpty()) {
            s.resolver.resolveMethod(className, methodName, diagnosticId = s.diagnosticId)
        } else {
            s.resolver.resolveMethod(className, methodName, *parameterTypes, diagnosticId = s.diagnosticId)
        }
    }

    /**
     * Mark all contract targets matching [className] and [memberName] as installed.
     *
     * [count] is used by [hookAllMethods] / [hookAllConstructors] to record how
     * many overloads/constructors were actually hooked.
     */
    @JvmStatic
    fun recordInstall(className: String, memberName: String?, count: Int = 1) {
        val s = session.get() ?: return
        for (record in matchingRecords(s, className, memberName)) {
            val spec = record.spec
            s.recordsById[spec.id] = record.copy(installed = true, failureReason = null)
            if (count > 1) s.installedCount[spec.id] = count
        }
    }

    /** Mark a target as failed, but only if it is not already installed. */
    @JvmStatic
    fun recordFailure(className: String, memberName: String?, reason: HookFailureReason) {
        val s = session.get() ?: return
        for (record in matchingRecords(s, className, memberName)) {
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
        for (record in s.recordsById.values.filter { it.spec.className == className && !it.installed }) {
            if (record.failureReason == null) {
                s.recordsById[record.spec.id] = record.copy(failureReason = reason)
            }
        }
    }

    private fun matchingRecords(s: Session, className: String, memberName: String?): List<HookTargetRecord> {
        return s.recordsById.values.filter {
            it.spec.className == className && (memberName == null || it.spec.memberName == memberName)
        }
    }
}

/**
 * Compute a [HookInstallResult] from a list of records using the contract and
 * fallback group rules.
 */
private fun List<HookTargetRecord>.toHookInstallResult(
    contract: HookTargetContract,
    diagnosticId: String
): HookInstallResult {
    val groups = contract.allTargets.filter { it.fallbackGroup != null }.groupBy { it.fallbackGroup!! }
    val selectedIds = mutableSetOf<String>()
    val satisfiedFallbackIds = mutableSetOf<String>()
    val selectedFallbacks = mutableListOf<HookTargetRecord>()
    val recordsBySpec = this.associateBy { it.spec.id }

    for ((_, targets) in groups) {
        val ordered = targets.sortedBy { it.fallbackOrder }
        val firstInstalled = ordered.firstOrNull { recordsBySpec[it.id]?.installed == true }
        if (firstInstalled != null) {
            val record = recordsBySpec.getValue(firstInstalled.id)
            selectedIds.add(record.spec.id)
            satisfiedFallbackIds.addAll(targets.map { it.id })
            if (firstInstalled.fallbackOrder > 0) selectedFallbacks.add(record)
        }
    }
    for (record in this.filter { it.spec.fallbackGroup == null && it.installed }) {
        selectedIds.add(record.spec.id)
    }

    val selectedRecords = this.filter { it.spec.id in selectedIds }
    val failedRecords = this.filter {
        it.spec.id !in selectedIds && it.spec.id !in satisfiedFallbackIds && !it.installed
    }
    val requiredFailures = failedRecords.filter { it.spec.required }
    val optionalFailures = failedRecords.filter { !it.spec.required }

    val requiredInstalled = selectedRecords.count { it.spec.required }
    val requiredTotal = this.count { it.spec.required }
    val optionalInstalled = selectedRecords.count { !it.spec.required }
    val optionalTotal = this.count { !it.spec.required }

    val outcome = when {
        requiredFailures.isNotEmpty() -> InstallOutcome.FAILED
        selectedFallbacks.isNotEmpty() -> InstallOutcome.DEGRADED
        optionalFailures.isNotEmpty() -> InstallOutcome.DEGRADED
        selectedIds.isNotEmpty() -> InstallOutcome.INSTALLED
        else -> InstallOutcome.FAILED
    }

    val reasonCode = when (outcome) {
        InstallOutcome.INSTALLED -> ReasonCode.INSTALLER_SUCCEEDED
        InstallOutcome.DEGRADED -> if (selectedFallbacks.isNotEmpty()) {
            ReasonCode.FALLBACK_TARGET_FOUND
        } else {
            ReasonCode.INSTALLER_SUCCEEDED
        }
        InstallOutcome.FAILED -> ReasonCode.INSTALLER_FAILED
        InstallOutcome.DISPATCHED -> ReasonCode.INSTALLER_DISPATCHED
    }

    val detail = buildString {
        append("required $requiredInstalled/$requiredTotal, optional $optionalInstalled/$optionalTotal")
        if (selectedFallbacks.isNotEmpty()) {
            append(", fallbacks=${selectedFallbacks.joinToString(",") { it.spec.id }}")
        }
        if (requiredFailures.isNotEmpty()) {
            append(", required_failed=${requiredFailures.joinToString(",") { it.spec.id }}")
        }
    }

    return HookInstallResult(records = this, reasonCode = reasonCode, detail = detail)
}
