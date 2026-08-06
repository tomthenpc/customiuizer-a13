package tv.withaibuild.customiuizer.utils

import android.content.SharedPreferences

class FakeSharedPreferences : SharedPreferences {

    private val values = HashMap<String, Any?>()
    private val listeners = ArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    var commitResult: Boolean = true

    @JvmField
    var registerCount: Int = 0

    /**
     * When true, the first call to registerOnSharedPreferenceChangeListener
     * throws and does not count. This lets tests distinguish the baseline
     * initPrefs registration (which may be disabled) from watchPreferenceChange
     * re-registration.
     */
    var failFirstRegister: Boolean = false
    private var firstRegisterFailed = false

    private var getAllException: RuntimeException? = null
    private var registerException: RuntimeException? = null

    fun put(key: String, value: Any?) {
        values[key] = value
        dispatchChange(key)
    }

    fun remove(key: String) {
        values.remove(key)
        dispatchChange(key)
    }

    fun set(map: Map<String, Any?>) {
        setAll(map)
    }

    fun setAll(map: Map<String, Any?>) {
        val changed = ArrayList<String>(map.keys.size)
        values.clear()
        values.putAll(map)
        changed.addAll(map.keys)
        for (key in changed) dispatchChange(key)
    }

    fun reset() {
        values.clear()
        listeners.clear()
        registerCount = 0
        firstRegisterFailed = false
    }

    fun change(key: String, value: Any?) {
        if (value == null) remove(key) else put(key, value)
    }

    fun setGetAllException(e: RuntimeException?) {
        getAllException = e
    }

    fun setRegisterException(e: RuntimeException?) {
        registerException = e
    }

    private fun dispatchChange(key: String) {
        for (l in listeners) l.onSharedPreferenceChanged(this, key)
    }

    override fun getAll(): Map<String, *> {
        getAllException?.let { throw it }
        return HashMap(values)
    }

    override fun getString(key: String, defValue: String?): String? {
        val v = values[key]
        return if (v is String) v else defValue
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        val v = values[key]
        @Suppress("UNCHECKED_CAST")
        return if (v is Set<*>) v as Set<String> else defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        val v = values[key]
        return if (v is Int) v else defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        val v = values[key]
        return if (v is Long) v else defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        val v = values[key]
        return if (v is Float) v else defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        val v = values[key]
        return if (v is Boolean) v else defValue
    }

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(values)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        registerException?.let { throw it }
        if (failFirstRegister && !firstRegisterFailed) {
            firstRegisterFailed = true
            throw RuntimeException("simulated first registration failure")
        }
        registerCount++
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    inner class FakeEditor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {

        private val staged = HashMap<String, Any?>()
        private val removed = HashSet<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            if (value != null) staged[key] = value else removed.add(key)
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            if (values != null) staged[key] = values else removed.add(key)
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            staged[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            staged[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            staged[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            staged[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            removed.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            return applyAndReturn(commitResult)
        }

        override fun apply() {
            applyAndReturn(true)
        }

        private fun applyAndReturn(result: Boolean): Boolean {
            if (result) {
                if (clearAll) values.clear()
                values.putAll(staged)
                for (key in removed) values.remove(key)
            }
            staged.clear()
            removed.clear()
            clearAll = false
            return result
        }
    }
}
