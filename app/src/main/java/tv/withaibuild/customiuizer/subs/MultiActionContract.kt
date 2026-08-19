package tv.withaibuild.customiuizer.subs

/**
 * MultiAction stored-value contract.
 *
 * "No action" is value 1. Historical `0`, negatives, and IDs missing from the
 * current action array must reload and save as the legal default.
 */
object MultiActionContract {
    const val NO_ACTION = 1
    const val TOGGLE_ACTION = 10

    fun normalize(saved: Int, legalValues: IntArray, default: Int = NO_ACTION): Int {
        if (saved > 0) {
            for (legal in legalValues) {
                if (legal == saved) return saved
            }
        }
        for (legal in legalValues) {
            if (legal == default) return default
        }
        return if (legalValues.isNotEmpty()) legalValues[0] else default
    }

    fun persistSelection(selected: Int?, legalValues: IntArray, default: Int = NO_ACTION): Int {
        return normalize(selected ?: default, legalValues, default)
    }
}
