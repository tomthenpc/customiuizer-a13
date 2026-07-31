package tv.withaibuild.customiuizer.utils

import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
class PrefMap<K : Any, V : Any> : ConcurrentHashMap<K, V>() {

    private data class CachedInt(val raw: String, val value: Int?)

    private val parsedIntCache = ConcurrentHashMap<K, CachedInt>()

    fun getObject(key: String, defValue: Any?): Any? {
        return get(normalizeKey(key) as K) ?: defValue
    }

    fun getInt(key: String, defValue: Int): Int {
        val normalized = normalizeKey(key) as K
        return when (val value = get(normalized)) {
            null -> defValue
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: defValue
            else -> defValue
        }
    }

    fun getLong(key: String, defValue: Long): Long {
        val normalized = normalizeKey(key) as K
        return when (val value = get(normalized)) {
            null -> defValue
            is Long -> value
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: defValue
            else -> defValue
        }
    }

    fun getString(key: String, defValue: String): String {
        val normalized = normalizeKey(key) as K
        return when (val value = get(normalized)) {
            null -> defValue
            is String -> value
            else -> defValue
        }
    }

    fun getStringAsInt(key: String, defValue: Int): Int {
        val normalized = normalizeKey(key) as K
        val value = get(normalized) ?: return defValue
        if (value is Number) return value.toInt()
        if (value !is String) return defValue

        val cached = parsedIntCache[normalized]
        if (cached != null && cached.raw == value) return cached.value ?: defValue

        val parsed = value.toIntOrNull()
        parsedIntCache[normalized] = CachedInt(value, parsed)
        return parsed ?: defValue
    }

    fun getStringSet(key: String): Set<String> {
        val normalized = normalizeKey(key) as K
        val value = get(normalized)
        return when (value) {
            null -> emptySet()
            is Set<*> -> value.filterIsInstance<String>().toSet()
            else -> emptySet()
        }
    }

    fun getBoolean(key: String): Boolean {
        return getBoolean(key, false)
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        val normalized = normalizeKey(key) as K
        return when (val value = get(normalized)) {
            null -> defValue
            is Boolean -> value
            "true", "1" -> true
            "false", "0" -> false
            else -> defValue
        }
    }

    override fun put(key: K, value: V): V? {
        val normalized = normalizeKey(key as String) as K
        parsedIntCache.remove(normalized)
        return super.put(normalized, value)
    }

    override fun putAll(from: Map<out K, V>) {
        val normalized = from.mapKeys { normalizeKey(it.key as String) as K }
        for (k in normalized.keys) parsedIntCache.remove(k)
        super.putAll(normalized)
    }

    override fun remove(key: K): V? {
        val normalized = normalizeKey(key as String) as K
        parsedIntCache.remove(normalized)
        return super.remove(normalized)
    }

    override fun clear() {
        parsedIntCache.clear()
        super.clear()
    }

    private fun normalizeKey(key: String): String =
        if (key.startsWith("pref_key_")) key else "pref_key_$key"
}
