package tv.withaibuild.customiuizer.utils

import android.content.SharedPreferences

class FakeSharedPreferences : SharedPreferences {

    private val values = HashMap<String, Any?>()
    private val listeners = ArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    var commitResult: Boolean = true

    /** Successful listener registrations observed by this fake. */
    @JvmField
    var registerCount: Int = 0

    /** All listener registration attempts, including those that threw. */
    @JvmField
    var registerAttemptCount: Int = 0

    /** Listener unregistrations observed by this fake. */
    @JvmField
    var unregisterCount: Int = 0

    /** Snapshot of currently registered listeners, keyed by identity string. */
    val activeListenerIdentities: Set<String>
        get() = listeners.map { System.identityHashCode(it).toString(16) }.toSet()

    /** Stack traces for each registration attempt, useful for caller classification in tests. */
    val registerAttemptStacks = ArrayList<List<String>>()

    /** Returns registration attempts whose stack contains the PreferenceBootstrap baseline path. */
    val baselineRegisterAttempts: Int
        get() = registerAttemptStacks.count { stack ->
            stack.any { it.startsWith("tv.withaibuild.customiuizer.utils.PreferenceBootstrap.start") ||
                       it.startsWith("tv.withaibuild.customiuizer.utils.PreferenceBootstrap.ensureListenerLocked") }
        }

    /** Returns registration attempts whose stack contains the PreferenceBootstrap.ensureWatcher path. */
    val systemUiWatcherRegisterAttempts: Int
        get() = registerAttemptStacks.count { stack ->
            stack.any { it.startsWith("tv.withaibuild.customiuizer.utils.PreferenceBootstrap.ensureWatcher") }
        }

    /**
     * When true, the first call to registerOnSharedPreferenceChangeListener
     * throws and does not count. This lets tests distinguish the baseline
     * initPrefs registration (which may be disabled) from watchPreferenceChange
     * re-registration.
     */
    var failFirstRegister: Boolean = false
    private var firstRegisterFailed = false

    private var getAllException: Throwable? = null
    private var registerException: Throwable? = null

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
        registerAttemptCount = 0
        unregisterCount = 0
        registerAttemptStacks.clear()
        firstRegisterFailed = false
    }

    fun change(key: String, value: Any?) {
        if (value == null) remove(key) else put(key, value)
    }

    fun setGetAllException(e: Throwable?) {
        getAllException = e
    }

    fun setRegisterException(e: Throwable?) {
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
        registerAttemptCount++
        registerAttemptStacks.add(Thread.currentThread().stackTrace.map { it.className + "." + it.methodName })
        registerException?.let { throw it }
        if (failFirstRegister && !firstRegisterFailed) {
            firstRegisterFailed = true
            throw RuntimeException("simulated first registration failure")
        }
        registerCount++
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        unregisterCount++
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
