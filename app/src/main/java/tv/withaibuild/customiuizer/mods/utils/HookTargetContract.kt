package tv.withaibuild.customiuizer.mods.utils

/**
 * An atomic, complete target bundle for a feature.
 *
 * A [FeatureTargetVariant] is the unit of selection. Its [requirements] must all
 * be evaluated together; it is never allowed to combine a successful
 * [HookRequirement] from one variant with a successful [HookRequirement] from
 * another variant.
 */
data class FeatureTargetVariant(
    val id: String,
    val requirements: List<HookRequirement>
) {
    init {
        require(requirements.isNotEmpty()) { "FeatureTargetVariant $id must contain at least one requirement" }
    }

    val allTargets: List<HookTargetSpec>
        get() = requirements.flatMap { req ->
            when (req) {
                is SingleTargetRequirement -> listOf(req.target)
                is AnyOfRequirement -> req.candidates
            }
        }

    fun candidateToRequirement(): Map<String, String> = requirements.flatMap { req ->
        val ids = when (req) {
            is SingleTargetRequirement -> listOf(req.target.id)
            is AnyOfRequirement -> req.candidates.map { it.id }
        }
        ids.map { it to req.id }
    }.toMap()
}

/**
 * Single, typed requirement model for a feature contract.
 *
 * Requirements are the only source of truth for:
 * - criticality (REQUIRED / OPTIONAL);
 * - fallback ordering (AnyOf candidates are listed primary-first).
 *
 * The old duplicate sources ([HookTargetContract.required] / [optional],
 * [HookTargetSpec.required], [fallbackGroup] / [fallbackOrder]) are deleted.
 */
sealed interface HookRequirement {
    val id: String
    val criticality: Criticality
}

/**
 * A single target that must or may succeed.
 */
data class SingleTargetRequirement(
    val target: HookTargetSpec,
    override val criticality: Criticality = Criticality.REQUIRED,
    override val id: String = target.id
) : HookRequirement

/**
 * A group of ordered candidates. The first candidate is the primary; later
 * candidates are fallbacks. The requirement is satisfied when any candidate
 * succeeds. Satisfaction through a non-primary candidate marks fallback as used.
 *
 * Note: an [AnyOfRequirement] is only intended to express a single
 * requirement's candidate list. Cross-class bundles that must succeed together
 * must be expressed as separate [FeatureTargetVariant]s.
 */
data class AnyOfRequirement(
    val candidates: List<HookTargetSpec>,
    override val criticality: Criticality = Criticality.REQUIRED,
    override val id: String
) : HookRequirement {
    init {
        require(candidates.isNotEmpty()) { "AnyOfRequirement $id must contain at least one candidate" }
        val ids = candidates.map { it.id }
        require(ids.distinct().size == ids.size) { "AnyOfRequirement $id has duplicate candidate ids" }
    }
}

/**
 * A typed contract that declares every target a feature depends on.
 *
 * The contract is used for two things:
 * - compatibility probing (resolve classes/methods/constructors/fields);
 * - post-install evidence (compare the contract against the actual hooks
 *   installed by [HookInstaller]).
 *
 * [HookRequirement] is the single source of truth for criticality and fallback
 * grouping inside a [FeatureTargetVariant]. Counts (required/optional
 * total/installed, fallback used) are produced by [HookEvidenceEvaluator] and
 * counted by requirement, not by candidate target count.
 */
data class HookTargetContract(
    val featureId: String,
    val variants: List<FeatureTargetVariant> = emptyList()
) {
    /**
     * Backward-compatible factory that treats a flat list of [HookRequirement]
     * as a single primary [FeatureTargetVariant].
     *
     * It is a companion [operator fun invoke] so the call site remains
     * `HookTargetContract(featureId, requirements = ...)` without a JVM
     * signature clash with the primary (featureId, variants) constructor.
     */
    companion object {
        operator fun invoke(
            featureId: String,
            requirements: List<HookRequirement> = emptyList()
        ): HookTargetContract = HookTargetContract(
            featureId,
            listOf(
                FeatureTargetVariant(
                    id = "primary",
                    requirements = requirements
                )
            )
        )
    }

    init {
        require(variants.isNotEmpty()) { "HookTargetContract $featureId must contain at least one variant" }
        val variantIds = variants.map { it.id }
        require(variantIds.distinct().size == variantIds.size) { "HookTargetContract $featureId has duplicate variant ids" }

        val allTargetIds = allTargets.map { it.id }
        require(allTargetIds.distinct().size == allTargetIds.size) { "HookTargetContract $featureId has duplicate target ids across variants" }
    }

    /** Flat view of all requirements across all variants. Kept for compatibility. */
    val requirements: List<HookRequirement>
        get() = variants.flatMap { it.requirements }

    val allTargets: List<HookTargetSpec>
        get() = variants.flatMap { it.allTargets }

    fun candidateToRequirement(): Map<String, String> = variants.flatMap { it.candidateToRequirement().entries }.associate { it.key to it.value }

    fun variantForTarget(targetId: String): FeatureTargetVariant? =
        variants.find { variant -> variant.allTargets.any { it.id == targetId } }

    fun containsTarget(targetId: String): Boolean =
        variants.any { variant -> variant.allTargets.any { it.id == targetId } }
}
