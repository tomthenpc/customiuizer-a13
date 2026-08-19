package tv.withaibuild.customiuizer.prefs

/**
 * Pure selection helpers for [SpinnerEx].
 *
 * Invalid stored values must not produce `setSelection(-1)` or an array OOB on save.
 * This layer does not remap a legitimate stored `0` used by unrelated spinners.
 */
object SpinnerSelection {
    fun indexOfValue(value: Int, values: IntArray?): Int {
        values ?: return -1
        return values.indexOf(value)
    }

    fun valueAt(values: IntArray?, position: Int): Int? {
        if (values == null || position !in values.indices) return null
        return values[position]
    }
}
