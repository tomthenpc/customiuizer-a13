package tv.withaibuild.customiuizer.mods.utils

import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

/**
 * Concrete operation the installer will perform for a target.
 *
 * This replaces the old [HookTargetKind] so that an empty parameter list can
 * unambiguously mean an exact no-arg method or constructor, while
 * `ALL_METHODS_BY_NAME` / `ALL_CONSTRUCTORS` are explicit, separate operations.
 */
enum class HookOperation {
    EXACT_METHOD,
    ALL_METHODS_BY_NAME,
    EXACT_CONSTRUCTOR,
    ALL_CONSTRUCTORS,
    CLASS_RESOLUTION,
    FIELD_RESOLUTION
}

enum class HookFailureReason {
    CLASS_NOT_FOUND,
    MEMBER_NOT_FOUND,
    HOOK_FAILED,
    UNKNOWN
}

enum class Criticality { REQUIRED, OPTIONAL }

/**
 * A target that the feature wants to hook or resolve.
 *
 * @param id Stable, unique candidate id inside the feature contract.
 * @param operation What kind of install/resolution the legacy installer performs.
 * @param className Fully qualified class name.
 * @param memberName Method/constructor/field name; null for constructors and
 *        plain class checks.
 * @param parameterTypes JVM parameter classes for [HookOperation.EXACT_METHOD]
 *        and [HookOperation.EXACT_CONSTRUCTOR] resolution. For all-methods and
 *        all-constructors this must be empty.
 */
data class HookTargetSpec(
    val id: String,
    val operation: HookOperation,
    val className: String,
    val memberName: String? = null,
    val parameterTypes: List<Class<*>> = emptyList()
)

/**
 * The actual result for one candidate during or after installation.
 *
 * @param spec The target spec (candidate identity).
 * @param requirementId The id of the [HookRequirement] this candidate belongs to.
 * @param resolved True if the Class/Method/Constructor/Field was found by the
 *        compatibility probe.
 * @param installed True if the target was actually hooked or, for a passive
 *        class/field, successfully resolved during install.
 * @param failureReason Reason when resolved or installed is false.
 * @param installedCount For [HookOperation.ALL_METHODS_BY_NAME] /
 *        [HookOperation.ALL_CONSTRUCTORS], the number of overloads/constructors
 *        actually hooked.
 */
data class HookTargetRecord(
    val spec: HookTargetSpec,
    val requirementId: String,
    val resolved: Boolean = false,
    val installed: Boolean = false,
    val failureReason: HookFailureReason? = null,
    val installedCount: Int = 0
)

/**
 * Immutable, fully-computed result for a contract compatibility or install pass.
 *
 * No getter on this object recomputes the outcome with a different logic. The
 * aggregation is produced in one pass by [HookEvidenceEvaluator.evaluate].
 */
data class HookInstallResult(
    val records: List<HookTargetRecord> = emptyList(),
    val compatibility: CompatibilityState? = null,
    val installation: InstallOutcome? = null,
    val requiredInstalled: Int = 0,
    val requiredTotal: Int = 0,
    val optionalInstalled: Int = 0,
    val optionalTotal: Int = 0,
    val fallbackUsed: Boolean = false,
    val requiredFailures: List<HookTargetRecord> = emptyList(),
    val optionalFailures: List<HookTargetRecord> = emptyList(),
    val reasonCode: ReasonCode = ReasonCode.UNKNOWN,
    val detail: String? = null
) {
    companion object {
        /** Convenience constant for unrecorded legacy Unit installers. */
        @JvmField
        val DISPATCHED = HookInstallResult(
            installation = InstallOutcome.DISPATCHED,
            reasonCode = ReasonCode.INSTALLER_DISPATCHED,
            detail = "legacy unit installer"
        )
    }
}
