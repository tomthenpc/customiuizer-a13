package tv.withaibuild.customiuizer.utils

@Suppress("UNCHECKED_CAST")
class PrefMap<K, V> : HashMap<K, V>() {

    private data class CachedInt(val raw: String, val value: Int?)

    private val parsedIntCache = java.util.concurrent.ConcurrentHashMap<K, CachedInt>()

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
        val value = get(normalizeKey(key) as K) ?: return defValue
        if (value is Number) return value.toInt()
        if (value !is String) return defValue

        val normalized = normalizeKey(key) as K
        val cached = parsedIntCache[normalized]
        if (cached != null && cached.raw == value) return cached.value ?: defValue

        val parsed = value.toIntOrNull()
        parsedIntCache[normalized] = CachedInt(value, parsed)
        return parsed ?: defValue
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
        return when (value) {
            null -> defValue
            is Boolean -> value
            "true", "1" -> true
            "false", "0" -> false
            else -> defValue
        }
    }

    override fun put(key: K, value: V): V? {
        parsedIntCache.remove(key)
        return super.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, _) in from) parsedIntCache.remove(k)
        super.putAll(from)
    }

    override fun remove(key: K): V? {
        parsedIntCache.remove(key)
        return super.remove(key)
    }

    override fun clear() {
        parsedIntCache.clear()
        super.clear()
    }

    private fun normalizeKey(key: String): String =
        if (key.startsWith("pref_key_")) key else "pref_key_$key"
}
