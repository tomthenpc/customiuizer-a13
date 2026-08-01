package tv.withaibuild.customiuizer.mods.utils

import java.lang.reflect.Constructor
import java.lang.reflect.Method

internal class VariantTestResolver(
    private val resolvedMethods: Set<String> = emptySet(),
    private val resolvedClasses: Set<String> = emptySet()
) : HookTargetResolver(VariantTestResolver::class.java.classLoader!!) {

    private val lengthMethod by lazy { String::class.java.getDeclaredMethod("length") }

    override fun resolveClass(className: String, diagnosticId: String): Class<*>? {
        val hasMethod = resolvedMethods.any { it.startsWith("$className.") }
        return if (hasMethod || className in resolvedClasses) String::class.java else null
    }

    override fun resolveMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>,
        diagnosticId: String
    ): Method? {
        return if ("$className.$methodName" in resolvedMethods) lengthMethod else null
    }

    override fun resolveAllConstructors(
        className: String,
        diagnosticId: String
    ): List<Constructor<*>>? {
        return if (className in resolvedClasses) {
            listOf(*String::class.java.declaredConstructors)
        } else null
    }
}
