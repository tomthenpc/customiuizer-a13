package tv.withaibuild.customiuizer.mods.utils

import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

/**
 * Outcome classification for a single hook target.
 *
 * This is a report type. It does not hold Context, ClassLoader, MethodHook
 * callbacks or Throwables (stack traces are summarized immediately and never
 * stored).
 */
enum class HookTargetKind { CLASS, METHOD, CONSTRUCTOR, FIELD }

enum class HookFailureReason {
    CLASS_NOT_FOUND,
    MEMBER_NOT_FOUND,
    HOOK_FAILED,
    UNKNOWN
}

/**
 * A target that the feature wants to hook or resolve.
 *
 * @param id Stable, unique id inside the feature contract (e.g. `class/member`).
 * @param kind Whether the target is a class, method, constructor or field.
 * @param className Fully qualified class name.
 * @param memberName Method/constructor/field name, null for plain class checks.
 * @param parameterTypes JVM parameter classes for method/constructor resolution.
 * @param required If true, failure of this target makes the feature FAILED,
 *                 unless a member of the same fallback group succeeded.
 * @param fallbackGroup Targets in the same group are ordered primary/fallback.
 * @param fallbackOrder 0 = primary, 1+ = fallback. A group succeeds if any
 *        member is installed. The earliest installed member is selected;
 *        if it is not the primary, the feature is DEGRADED.
 */
data class HookTargetSpec(
    val id: String,
    val kind: HookTargetKind,
    val className: String,
    val memberName: String? = null,
    val parameterTypes: List<Class<*>> = emptyList(),
    val required: Boolean = true,
    val fallbackGroup: String? = null,
    val fallbackOrder: Int = 0
)

/**
 * The actual result for one target during or after installation.
 *
 * @param spec The target spec.
 * @param resolved True if the Class/Method/Constructor/Field was found by the
 *        compatibility probe.
 * @param installed True if the target was actually hooked or, for a field,
 *        successfully resolved during install.
 * @param failureReason Reason when resolved or installed is false.
 * @param installedCount For [HookTargetKind.METHOD] with [hookAllMethods],
 *        the number of overloads successfully hooked.
 */
data class HookTargetRecord(
    val spec: HookTargetSpec,
    val resolved: Boolean = false,
    val installed: Boolean = false,
    val failureReason: HookFailureReason? = null,
    val installedCount: Int = 0
)

/**
 * A privacy-safe, throwable-free report produced by the reporting hook APIs.
 *
 * It is intentionally not a diagnostic type; it is fed into
 * [tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder] by the
 * catalog after the install attempt.
 */
data class HookInstallResult(
    val records: List<HookTargetRecord> = emptyList(),
    val reasonCode: ReasonCode = ReasonCode.UNKNOWN,
    val detail: String? = null
) {
    val attemptedTargets: List<HookTargetRecord> get() = records
    val installedTargets: List<HookTargetRecord> get() = records.filter { it.installed }

    val selectedFallbacks: List<HookTargetRecord>
        get() = selectedRecords().filter { it.spec.fallbackOrder > 0 }

    val requiredFailures: List<HookTargetRecord>
        get() = failedGroupMembers().filter { it.spec.required }

    val optionalFailures: List<HookTargetRecord>
        get() = failedGroupMembers().filter { !it.spec.required }

    val outcome: InstallOutcome
        get() = when {
            requiredFailures.isNotEmpty() -> InstallOutcome.FAILED
            selectedFallbacks.isNotEmpty() -> InstallOutcome.DEGRADED
            optionalFailures.isNotEmpty() -> InstallOutcome.DEGRADED
            installedTargets.isNotEmpty() -> InstallOutcome.INSTALLED
            else -> InstallOutcome.FAILED
        }

    val requiredInstalled: Int get() = selectedRecords().count { it.spec.required }
    val requiredTotal: Int get() = records.count { it.spec.required }
    val optionalInstalled: Int get() = selectedRecords().count { !it.spec.required }
    val optionalTotal: Int get() = records.count { !it.spec.required }
    val fallbackUsed: Boolean get() = selectedFallbacks.isNotEmpty()

    /**
     * For each fallback group, select the earliest installed member.
     * Non-grouped targets are selected if installed.
     */
    private fun selectedRecords(): List<HookTargetRecord> {
        val groups = records.filter { it.spec.fallbackGroup != null }.groupBy { it.spec.fallbackGroup }
        val selected = mutableListOf<HookTargetRecord>()
        for ((_, members) in groups) {
            members.filter { it.installed }.minByOrNull { it.spec.fallbackOrder }?.let { selected.add(it) }
        }
        selected.addAll(records.filter { it.spec.fallbackGroup == null && it.installed })
        return selected
    }

    /**
     * Targets that failed and are not covered by a selected group member.
     */
    private fun failedGroupMembers(): List<HookTargetRecord> {
        val selectedGroupMembers = records.filter { it.spec.fallbackGroup != null }.groupBy { it.spec.fallbackGroup }
            .mapNotNull { (_, members) ->
                members.filter { it.installed }.minByOrNull { it.spec.fallbackOrder }
            }
        val selectedIds = (selectedRecords() + selectedGroupMembers).map { it.spec.id }.toSet()
        return records.filter { it.spec.id !in selectedIds && !it.installed }
    }

    companion object {
        /** Convenience constant for unrecorded legacy Unit installers. */
        @JvmField
        val DISPATCHED = HookInstallResult(
            records = emptyList(),
            reasonCode = ReasonCode.INSTALLER_DISPATCHED,
            detail = "legacy unit installer"
        )
    }
}
