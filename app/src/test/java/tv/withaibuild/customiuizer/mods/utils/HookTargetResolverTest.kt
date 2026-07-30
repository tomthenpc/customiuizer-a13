package tv.withaibuild.customiuizer.mods.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HookTargetResolverTest {

    @Test
    fun resolveClass_cachesPositiveResult() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = ResolverTestTarget::class.java.name

        val first = resolver.resolveClass(className)
        assertNotNull(first)
        assertEquals(1, loader.findClassCalls.get())

        val second = resolver.resolveClass(className)
        assertSame(first, second)
        assertEquals(1, loader.findClassCalls.get())
        assertTrue(cacheContains(resolver, className))
    }

    @Test
    fun resolveClass_cachesNegativeResult() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = "tv.withaibuild.customiuizer.mods.utils.MissingClass"

        assertNull(resolver.resolveClass(className))
        assertEquals(1, loader.missingCalls.get())

        assertNull(resolver.resolveClass(className))
        assertEquals(1, loader.missingCalls.get())
        assertTrue(cacheContains(resolver, className))
    }

    @Test
    fun resolveMethod_returnsAndCachesPositiveResult() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = ResolverTestTarget::class.java.name

        val method = resolver.resolveMethod(
            className,
            "method",
            String::class.java
        )
        assertNotNull(method)
        assertSame(method, resolver.resolveMethod(className, "method", String::class.java))
    }

    @Test
    fun resolveMethod_returnsNullForMissingMethodAndCaches() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = ResolverTestTarget::class.java.name

        assertNull(resolver.resolveMethod(className, "nonExistentMethod", String::class.java))
        assertNull(resolver.resolveMethod(className, "nonExistentMethod", String::class.java))
    }

    @Test
    fun resolveField_returnsAndCachesPositiveResult() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = ResolverTestTarget::class.java.name

        val field = resolver.resolveField(className, "field")
        assertNotNull(field)
        assertSame(field, resolver.resolveField(className, "field"))
    }

    @Test
    fun resolveField_returnsNullForMissingFieldAndCaches() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = ResolverTestTarget::class.java.name

        assertNull(resolver.resolveField(className, "nonExistentField"))
        assertNull(resolver.resolveField(className, "nonExistentField"))
    }

    private fun cacheContains(resolver: HookTargetResolver, key: String): Boolean {
        val field = resolver.javaClass.getDeclaredField("cache")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = field.get(resolver) as ConcurrentHashMap<String, Any?>
        return cache.containsKey(key)
    }

    private class CountingClassLoader : ClassLoader(ClassLoader.getSystemClassLoader()) {
        val findClassCalls = AtomicInteger(0)
        val missingCalls = AtomicInteger(0)

        override fun loadClass(name: String?, resolve: Boolean): Class<*> {
            if (name == ResolverTestTarget::class.java.name) {
                findLoadedClass(name)?.let { return it }
                findClassCalls.incrementAndGet()
                val resource = name!!.replace('.', '/') + ".class"
                val bytes = ClassLoader.getSystemResourceAsStream(resource)?.readBytes()
                    ?: throw ClassNotFoundException(name)
                return defineClass(name, bytes, 0, bytes.size)
            }
            if (name == "tv.withaibuild.customiuizer.mods.utils.MissingClass") {
                findLoadedClass(name)?.let { return it }
                missingCalls.incrementAndGet()
                throw ClassNotFoundException(name)
            }
            return super.loadClass(name, resolve)
        }
    }
}

class ResolverTestTarget {
    @JvmField
    val field: String = "value"

    fun method(arg: String): Int = 42
}
