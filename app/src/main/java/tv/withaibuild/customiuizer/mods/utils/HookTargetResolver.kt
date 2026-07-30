package tv.withaibuild.customiuizer.mods.utils

import android.util.Log
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, per-ClassLoader cache for hook targets.
 *
 * Keeps class/method/field reflection results so hot paths do not repeat it.
 * Failures are remembered and logged once per key.
 */
class HookTargetResolver(private val classLoader: ClassLoader) {

    private val cache = ConcurrentHashMap<String, Any?>()
    private val failedKeys = ConcurrentHashMap.newKeySet<String>()

    /** Resolve a class by name, caching the result (including a negative result). */
    fun resolveClass(className: String): Class<*>? {
        val k = key(className)
        val cached = cache[k]
        if (cached != null) return if (cached === NULL) null else cached as Class<*>

        return try {
            put(k, XposedHelpers.findClassIfExists(className, classLoader))
        } catch (t: Throwable) {
            put(k, null, "Failed to resolve class $className: $t")
        }
    }

    /**
     * Resolve a declared method by name and parameter types.
     * Returns null on any failure (no throw) and caches the negative result.
     */
    fun resolveMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        val paramNames = parameterTypes.map { it.name }.toTypedArray()
        val k = key(className, methodName, *paramNames)
        val cached = cache[k]
        if (cached != null) return if (cached === NULL) null else cached as Method

        val clazz = resolveClass(className)
        if (clazz == null) {
            cache[k] = NULL
            return null
        }

        return try {
            val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
            method.isAccessible = true
            cache[k] = method
            method
        } catch (t: Throwable) {
            put(k, null, "Failed to resolve method $className.$methodName: $t")
        }
    }

    /**
     * Resolve a declared field by name.
     * Returns null on any failure (no throw) and caches the negative result.
     */
    fun resolveField(className: String, fieldName: String): Field? {
        val k = key(className, fieldName)
        val cached = cache[k]
        if (cached != null) return if (cached === NULL) null else cached as Field

        val clazz = resolveClass(className)
        if (clazz == null) {
            cache[k] = NULL
            return null
        }

        return try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            cache[k] = field
            field
        } catch (t: Throwable) {
            put(k, null, "Failed to resolve field $className.$fieldName: $t")
        }
    }

    private fun key(vararg parts: String): String = parts.joinToString("#")

    private fun <T : Any> put(key: String, value: T?, message: String? = null): T? {
        if (value == null) {
            cache[key] = NULL
            if (failedKeys.add(key)) {
                Log.w(TAG, message ?: "Hook target not found: $key")
            }
            return null
        }
        cache[key] = value
        return value
    }

    private companion object {
        private const val TAG = "HookTargetResolver"
        private val NULL = Any()
    }
}
