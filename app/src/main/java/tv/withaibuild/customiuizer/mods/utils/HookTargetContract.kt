package tv.withaibuild.customiuizer.mods.utils

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
 * grouping. Counts (required/optional total/installed, fallback used) are
 * produced by [HookEvidenceEvaluator] and counted by requirement, not by
 * candidate target count.
 */
data class HookTargetContract(
    val featureId: String,
    val requirements: List<HookRequirement> = emptyList()
) {
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
