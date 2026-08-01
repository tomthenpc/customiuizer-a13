package tv.withaibuild.customiuizer.mods.utils

/**
 * Typed identity for a feature.
 *
 * The [id] is a stable, compact integer used for arrays and bit sets. It must
 * be unique and should not change once assigned.
 */
interface FeatureId {

    /** Stable compact integer for arrays and bit sets. */
    val id: Int

    /** Human-readable name for diagnostics. */
    val name: String
}
