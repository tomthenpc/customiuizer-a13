package tv.withaibuild.customiuizer.prefs

/**
 * Type-safe constraint on a preference value.
 *
 * Replaces the free-text `allowedRange` string with a sealed class that the
 * audit scripts and tests can pattern-match on.
 */
sealed class PreferenceConstraint {
    object None : PreferenceConstraint()
    data class IntRange(val min: Int, val max: Int) : PreferenceConstraint()
    data class StringPattern(val pattern: String) : PreferenceConstraint()
    data class StringSet(val minSize: Int = 0, val maxSize: Int = Int.MAX_VALUE) : PreferenceConstraint()
    data class BooleanValue(val expected: Boolean? = null) : PreferenceConstraint()
}
