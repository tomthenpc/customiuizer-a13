package tv.withaibuild.customiuizer.utils

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Suppress("UNCHECKED_CAST")
class PrefMap<K : Any, V : Any> {

    private data class CachedInt(val raw: String, val value: Int?)

    private val parsedIntCache = ConcurrentHashMap<K, CachedInt>()
    private val state = AtomicReference<Map<K, V>>(emptyMap())

    /** Return the current immutable snapshot without copying. */
    fun asMap(): Map<K, V> = state.get()

    /** Expose the snapshot's entry set for Java callers. */
    fun entrySet(): Set<Map.Entry<K, V>> = state.get().entries

    val size: Int
        get() = state.get().size

    val keys: Set<K>
        get() = state.get().keys

    /** Atomically replace the entire snapshot. Readers see only the old or the new map. */
    fun replaceSnapshot(map: Map<K, V>) {
        state.set(Collections.unmodifiableMap(HashMap(map)))
        parsedIntCache.clear()
    }

    fun getObject(key: String, defValue: Any?): Any? {
        val normalized = normalizeKey(key) as K
        return state.get()[normalized] ?: defValue
    }

    fun getInt(key: String, defValue: Int): Int {
        val snap = state.get()
        val normalized = normalizeKey(key) as K
        return when (val value = snap[normalized]) {
            null -> defValue
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: defValue
            else -> defValue
        }
    }

    fun getLong(key: String, defValue: Long): Long {
        val snap = state.get()
        val normalized = normalizeKey(key) as K
        return when (val value = snap[normalized]) {
            null -> defValue
            is Long -> value
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: defValue
            else -> defValue
        }
    }

    fun getString(key: String, defValue: String): String {
        val snap = state.get()
        val normalized = normalizeKey(key) as K
        return when (val value = snap[normalized]) {
            null -> defValue
            is String -> value
            else -> defValue
        }
    }

    fun getStringAsInt(key: String, defValue: Int): Int {
        val snap = state.get()
        val normalized = normalizeKey(key) as K
        val value = snap[normalized] ?: return defValue
        if (value is Number) return value.toInt()
        if (value !is String) return defValue

        val cached = parsedIntCache[normalized]
        if (cached != null && cached.raw == value) return cached.value ?: defValue

        val parsed = value.toIntOrNull()
        parsedIntCache[normalized] = CachedInt(value, parsed)
        return parsed ?: defValue
    }

    fun getStringSet(key: String): Set<String> {
        val snap = state.get()
        val normalized = normalizeKey(key) as K
        val value = snap[normalized]
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
        val snap = state.get()
        val normalized = normalizeKey(key) as K
        return when (val value = snap[normalized]) {
            null -> defValue
            is Boolean -> value
            "true", "1" -> true
            "false", "0" -> false
            else -> defValue
        }
    }

    /** Map-style read. Normalizes string keys to the `pref_key_` form. */
    operator fun get(key: K): V? {
        val normalized = if (key is String) normalizeKey(key) as K else key
        return state.get()[normalized]
    }

    /** Map-style write. */
    operator fun set(key: K, value: V) {
        put(key, value)
    }

    fun put(key: K, value: V): V? {
        val normalized = normalizeKey(key as String) as K
        var old: Map<K, V> = state.get()
        val next = HashMap(old)
        next[normalized] = value
        while (!state.compareAndSet(old, Collections.unmodifiableMap(next))) {
            old = state.get()
            next.clear()
            next.putAll(old)
            next[normalized] = value
        }
        parsedIntCache.remove(normalized)
        return old[normalized]
    }

    fun putAll(from: Map<out K, V>) {
        val normalized = from.mapKeys { normalizeKey(it.key as String) as K }
        var old: Map<K, V> = state.get()
        val next = HashMap(old)
        next.putAll(normalized)
        while (!state.compareAndSet(old, Collections.unmodifiableMap(next))) {
            old = state.get()
            next.clear()
            next.putAll(old)
            next.putAll(normalized)
        }
        parsedIntCache.keys.removeAll(normalized.keys)
    }

    fun remove(key: K): V? {
        val normalized = normalizeKey(key as String) as K
        var old: Map<K, V> = state.get()
        val next = HashMap(old)
        next.remove(normalized)
        while (!state.compareAndSet(old, Collections.unmodifiableMap(next))) {
            old = state.get()
            next.clear()
            next.putAll(old)
            next.remove(normalized)
        }
        parsedIntCache.remove(normalized)
        return old[normalized]
    }

    fun clear() {
        state.set(emptyMap())
        parsedIntCache.clear()
    }

    fun containsKey(key: K): Boolean {
        return if (key is String) {
            state.get().containsKey(normalizeKey(key) as K)
        } else {
            state.get().containsKey(key)
        }
    }

    /** Kotlin `in` operator. Normalizes string keys. */
    operator fun contains(key: K): Boolean = containsKey(key)

    private fun normalizeKey(key: String): String =
        if (key.startsWith("pref_key_")) key else "pref_key_$key"
}
