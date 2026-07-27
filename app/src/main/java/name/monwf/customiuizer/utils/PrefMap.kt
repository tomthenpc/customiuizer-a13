package name.monwf.customiuizer.utils

@Suppress("UNCHECKED_CAST")
class PrefMap<K, V> : HashMap<K, V>() {

    fun getObject(key: String, defValue: Any?): Any? {
        return get(key as K) ?: defValue
    }

    fun getInt(key: String, defValue: Int): Int {
        val value = get(normalizeKey(key) as K)
        return if (value == null) defValue else value as Int
    }

    fun getLong(key: String, defValue: Long): Long {
        val value = get(normalizeKey(key) as K)
        return if (value == null) defValue else value as Long
    }

    fun getString(key: String, defValue: String): String {
        val value = get(normalizeKey(key) as K)
        return if (value == null) defValue else value as String
    }

    fun getStringAsInt(key: String, defValue: Int): Int {
        val value = get(normalizeKey(key) as K)
        return if (value is String) value.toIntOrNull() ?: defValue else defValue
    }

    fun getStringSet(key: String): Set<String> {
        val value = get(normalizeKey(key) as K)
        return if (value == null) emptySet() else value as Set<String>
    }

    fun getBoolean(key: String): Boolean {
        return getBoolean(key, false)
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        val value = get(normalizeKey(key) as K)
        return if (value == null) defValue else value as Boolean
    }

    private fun normalizeKey(key: String): String =
        if (key.startsWith("pref_key_")) key else "pref_key_$key"
}
