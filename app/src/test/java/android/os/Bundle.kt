package android.os

/** JVM test shadow that actually stores values (android.jar putInt is a no-op). */
open class Bundle {
    private val ints = HashMap<String, Int>()
    private val strings = HashMap<String, String?>()
    private val longs = HashMap<String, Long>()

    constructor()
    constructor(capacity: Int)

    open fun putInt(key: String, value: Int) {
        ints[key] = value
    }

    open fun getInt(key: String): Int = ints[key] ?: 0

    open fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue

    open fun putString(key: String, value: String?) {
        strings[key] = value
    }

    open fun getString(key: String): String? = strings[key]

    open fun putLong(key: String, value: Long) {
        longs[key] = value
    }

    open fun getLong(key: String): Long = longs[key] ?: 0L

    open fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue

    open fun containsKey(key: String): Boolean =
        ints.containsKey(key) || strings.containsKey(key) || longs.containsKey(key)

    open fun isEmpty(): Boolean = ints.isEmpty() && strings.isEmpty() && longs.isEmpty()

    open fun size(): Int = ints.size + strings.size + longs.size
}
