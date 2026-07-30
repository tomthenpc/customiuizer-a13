package tv.withaibuild.customiuizer.mods.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticIds
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticState

class HookTargetResolverTest {

    private val logs = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        DiagnosticRecorder.logger = { logs += it }
        DiagnosticRecorder.clock = { 0L }
    }

    @Test
    fun resolveClass_cachesPositiveResult() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = ResolverTestTarget::class.java.name

        val before = loader.findClassCalls.get()
        val first = resolver.resolveClass(className)
        assertNotNull(first)
        val afterFirst = loader.findClassCalls.get()
        assertTrue("class resolution should have tried the classloader", afterFirst > before)

        val second = resolver.resolveClass(className)
        assertSame(first, second)
        assertEquals(
            "second resolve must not hit the classloader",
            afterFirst,
            loader.findClassCalls.get()
        )
        assertTrue(cacheContains(resolver, "class#$className"))
    }

    @Test
    fun resolveClass_cachesNegativeResult() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = "tv.withaibuild.customiuizer.mods.utils.MissingClass"

        val before = loader.missingCalls.get()
        assertNull(resolver.resolveClass(className))
        val afterFirst = loader.missingCalls.get()
        assertTrue("class resolution should have tried the classloader", afterFirst > before)

        assertNull(resolver.resolveClass(className))
        assertEquals(
            "second resolve must not hit the classloader",
            afterFirst,
            loader.missingCalls.get()
        )
        assertTrue(cacheContains(resolver, "class#$className"))
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

    @Test
    fun resolveFirstClass_fallsBackToSecondCandidate() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)
        val className = ResolverTestTarget::class.java.name

        val resolution = resolver.resolveFirstClass(
            DiagnosticIds.HOOK_TARGET_RESOLVER,
            "missing.Class",
            className
        )

        assertNotNull(resolution.value)
        assertEquals(className, resolution.log.hit)
        assertEquals(1, resolution.log.failures.size)
        assertTrue(resolution.log.failures[0].startsWith("missing.Class"))
    }

    @Test
    fun resolveFirstClass_returnsNullWhenAllCandidatesFail() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)

        val resolution = resolver.resolveFirstClass(
            DiagnosticIds.HOOK_TARGET_RESOLVER,
            "missing.One",
            "missing.Two"
        )

        assertNull(resolution.value)
        assertEquals(2, resolution.log.failures.size)
        val summary = DiagnosticRecorder.summarize()[DiagnosticIds.HOOK_TARGET_RESOLVER]
        assertNotNull(summary)
        assertEquals(DiagnosticState.DEGRADED, summary!!.state)
    }

    @Test
    fun diagnosticRecorderIsUsedInsteadOfAndroidLog() {
        val loader = CountingClassLoader()
        val resolver = HookTargetResolver(loader)

        resolver.resolveFirstClass(DiagnosticIds.HOOK_TARGET_RESOLVER, "missing.Class")

        assertTrue(logs.isNotEmpty())
        assertTrue(logs.first().contains("missing.Class"))
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
            try {
                val loaded = super.loadClass(name, resolve)
                if (loaded != null) return loaded
            } catch (_: ClassNotFoundException) {
                // fall through
            }
            missingCalls.incrementAndGet()
            throw ClassNotFoundException(name)
        }
    }
}

class ResolverTestTarget {
    @JvmField
    val field: String = "value"

    fun method(arg: String): Int = 42
}
