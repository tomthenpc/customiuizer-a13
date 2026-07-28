package name.monwf.customiuizer.utils

object PrefPair {
    const val DELIMITER: Char = '|'

    /**
     * Return the first segment of a `first|second` pair.
     * The whole string is returned when there is no delimiter.
     * A leading delimiter means the first segment is empty.
     * Only the first delimiter is considered.
     */
    @JvmStatic
    fun first(pair: String): String {
        val idx = pair.indexOf(DELIMITER)
        return if (idx == -1) pair else pair.substring(0, idx)
    }

    /**
     * Return the second segment of a `first|second` pair.
     * An empty string is returned when there is no delimiter.
     * A trailing delimiter means the second segment is empty.
     * Only the first delimiter is considered.
     */
    @JvmStatic
    fun second(pair: String): String {
        val idx = pair.indexOf(DELIMITER)
        return if (idx == -1) "" else pair.substring(idx + 1)
    }

    /**
     * Case-insensitive comparison of the first segment with [needle].
     * Does not create a temporary substring or a Regex.
     */
    @JvmStatic
    fun firstEquals(pair: String, needle: String): Boolean {
        val idx = pair.indexOf(DELIMITER)
        val firstLen = if (idx == -1) pair.length else idx
        if (firstLen != needle.length) return false
        return pair.regionMatches(0, needle, 0, firstLen, ignoreCase = true)
    }

    /**
     * Check whether [pairs] contains an entry whose first segment equals [needle].
     * Null or empty sets return false.
     */
    @JvmStatic
    fun containsFirst(pairs: Set<String>?, needle: String): Boolean {
        if (pairs.isNullOrEmpty()) return false
        for (pair in pairs) {
            if (firstEquals(pair, needle)) return true
        }
        return false
    }
}
