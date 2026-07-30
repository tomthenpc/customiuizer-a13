package tv.withaibuild.customiuizer.mods.utils

import tv.withaibuild.customiuizer.mods.catalog.CompatibilityState
import tv.withaibuild.customiuizer.mods.diagnostics.ReasonCode

/**
 * A typed contract that declares every target a feature depends on.
 *
 * The contract is used for two things:
 * - compatibility probing (resolve classes/methods/constructors/fields);
 * - post-install evidence (compare the contract against the actual hooks
 *   installed by [HookInstaller]).
 *
 * [required] targets must all be installed for a feature to be [InstallOutcome.INSTALLED].
 * [optional] targets may fail without failing the feature, but their failure
 * may push the outcome to [InstallOutcome.DEGRADED] if any required target
 * succeeded.
 *
 * Targets that share a non-null [HookTargetSpec.fallbackGroup] are primary/fallback
 * candidates. Only one success is needed inside a group. The earliest target
 * (lowest [HookTargetSpec.fallbackOrder]) that succeeds is selected as the
 * primary; later successes are recorded in [HookInstallResult.selectedFallbacks].
 */
data class HookTargetContract(
    val featureId: String,
    val required: List<HookTargetSpec> = emptyList(),
    val optional: List<HookTargetSpec> = emptyList()
) {
    val allTargets: List<HookTargetSpec> get() = required + optional
}

/**
 * Evaluate a contract for compatibility using a [HookTargetResolver].
 *
 * This only resolves targets (no hooks are installed) and populates the
 * resolver cache so the later install phase can reuse the resolved members.
 */
fun HookTargetResolver.evaluateContract(
    contract: HookTargetContract,
    diagnosticId: String
): Pair<CompatibilityState, HookInstallResult> {
    val records = mutableListOf<HookTargetRecord>()
    val requiredFailures = mutableListOf<HookTargetRecord>()
    val fallbackGroups = contract.allTargets
        .filter { it.fallbackGroup != null }
        .groupBy { it.fallbackGroup!! }

    // Helper that resolves a target and produces a record.
    fun resolve(spec: HookTargetSpec): HookTargetRecord {
        val record = when (spec.kind) {
            HookTargetKind.CLASS -> {
                val clazz = resolveClass(spec.className, diagnosticId)
                HookTargetRecord(
                    spec = spec,
                    resolved = clazz != null,
                    installed = false,
                    failureReason = if (clazz == null) HookFailureReason.CLASS_NOT_FOUND else null
                )
            }
            HookTargetKind.METHOD -> {
                val method = if (spec.parameterTypes.isEmpty() && spec.memberName != null) {
                    resolveMethod(spec.className, spec.memberName, diagnosticId = diagnosticId)
                } else if (spec.memberName != null) {
                    resolveMethod(spec.className, spec.memberName, *spec.parameterTypes.toTypedArray(), diagnosticId = diagnosticId)
                } else {
                    null
                }
                HookTargetRecord(
                    spec = spec,
                    resolved = method != null,
                    installed = false,
                    failureReason = if (method == null) {
                        if (resolveClass(spec.className, diagnosticId) == null) {
                            HookFailureReason.CLASS_NOT_FOUND
                        } else {
                            HookFailureReason.MEMBER_NOT_FOUND
                        }
                    } else null
                )
            }
            HookTargetKind.CONSTRUCTOR -> {
                val ctor = if (spec.parameterTypes.isEmpty()) {
                    // First declared constructor.
                    resolveClass(spec.className, diagnosticId)?.declaredConstructors?.firstOrNull()
                } else {
                    resolveClass(spec.className, diagnosticId)?.getDeclaredConstructor(*spec.parameterTypes.toTypedArray())
                }?.apply { isAccessible = true }
                HookTargetRecord(
                    spec = spec,
                    resolved = ctor != null,
                    installed = false,
                    failureReason = if (ctor == null) {
                        if (resolveClass(spec.className, diagnosticId) == null) {
                            HookFailureReason.CLASS_NOT_FOUND
                        } else {
                            HookFailureReason.MEMBER_NOT_FOUND
                        }
                    } else null
                )
            }
            HookTargetKind.FIELD -> {
                val field = if (spec.memberName != null) {
                    resolveField(spec.className, spec.memberName, diagnosticId)
                } else null
                HookTargetRecord(
                    spec = spec,
                    resolved = field != null,
                    installed = false,
                    failureReason = if (field == null) {
                        if (resolveClass(spec.className, diagnosticId) == null) {
                            HookFailureReason.CLASS_NOT_FOUND
                        } else {
                            HookFailureReason.MEMBER_NOT_FOUND
                        }
                    } else null
                )
            }
        }
        records.add(record)
        return record
    }

    // Resolve every target. For fallback groups, only one success is needed.
    val selectedFallbacks = mutableListOf<HookTargetRecord>()
    for (group in fallbackGroups.values) {
        val ordered = group.sortedBy { it.fallbackOrder }
        var installedInGroup = false
        for (spec in ordered) {
            val record = resolve(spec)
            if (record.resolved && !installedInGroup) {
                if (spec.fallbackOrder!! > 0) {
                    selectedFallbacks.add(record)
                }
                installedInGroup = true
                // Continue resolving other group members so they appear as
                // optional failures if they are declared elsewhere, but stop
                // recording them as selected fallbacks.
            }
        }
    }
    for (spec in contract.allTargets.filter { it.fallbackGroup == null }) {
        resolve(spec)
    }

    // Determine compatibility state and reason code.
    val compatibility = when {
        contract.required.all { spec ->
            val rec = records.first { it.spec.id == spec.id }
            rec.resolved
        } -> {
            if (selectedFallbacks.isNotEmpty()) {
                CompatibilityState.DEGRADED
            } else {
                CompatibilityState.COMPATIBLE
            }
        }
        contract.optional.any { spec ->
            val rec = records.first { it.spec.id == spec.id }
            rec.resolved
        } || selectedFallbacks.isNotEmpty() -> CompatibilityState.DEGRADED
        else -> CompatibilityState.INCOMPATIBLE
    }

    val reasonCode = when (compatibility) {
        CompatibilityState.COMPATIBLE -> ReasonCode.PRIMARY_TARGET_FOUND
        CompatibilityState.DEGRADED -> if (selectedFallbacks.isNotEmpty()) {
            ReasonCode.FALLBACK_TARGET_FOUND
        } else {
            ReasonCode.PRIMARY_TARGET_FOUND
        }
        CompatibilityState.INCOMPATIBLE -> ReasonCode.TARGET_NOT_FOUND
    }

    return compatibility to HookInstallResult(
        records = records,
        reasonCode = reasonCode,
        detail = when (compatibility) {
            CompatibilityState.COMPATIBLE -> "all required targets resolved"
            CompatibilityState.DEGRADED -> if (selectedFallbacks.isNotEmpty()) {
                "fallback target resolved: ${selectedFallbacks.joinToString(", ") { it.spec.id }}"
            } else {
                "some required targets resolved, optional/fallback missing"
            }
            CompatibilityState.INCOMPATIBLE -> "required target(s) not found"
        }
    )
}
