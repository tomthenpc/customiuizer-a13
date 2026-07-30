package tv.withaibuild.customiuizer.mods.utils

import java.lang.reflect.Method
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class ReflectionCacheAllocationTest {

    open class Base {
        @JvmField
        var inheritedField: Int = 7

        fun inheritedNoArgMethod(): String = "base"
    }

    class Derived : Base() {
        @JvmField
        var ownField: String = "own"
    }

    private data class AllocationCounter(
        val bean: Any,
        val allocatedBytes: Method
    )

    private fun allocationCounter(): AllocationCounter? {
        return try {
            val managementFactory = Class.forName("java.lang.management.ManagementFactory")
            val threadMxBean = Class.forName("com.sun.management.ThreadMXBean")
            val bean = managementFactory.getMethod("getThreadMXBean").invoke(null)
            if (!threadMxBean.isInstance(bean)) return null
            val supported = threadMxBean.getMethod("isThreadAllocatedMemorySupported")
                .invoke(bean) as Boolean
            if (!supported) return null
            threadMxBean.getMethod(
                "setThreadAllocatedMemoryEnabled",
                Boolean::class.javaPrimitiveType
            ).invoke(bean, true)
            AllocationCounter(
                bean,
                threadMxBean.getMethod(
                    "getThreadAllocatedBytes",
                    Long::class.javaPrimitiveType
                )
            )
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun allocatedBytes(counter: AllocationCounter): Long {
        return counter.allocatedBytes.invoke(
            counter.bean,
            Thread.currentThread().id
        ) as Long
    }

    private fun measureAllocation(iterations: Int, block: () -> Unit): Long? {
        val counter = allocationCounter() ?: return null
        repeat(2_000) { block() }
        val before = allocatedBytes(counter)
        repeat(iterations) { block() }
        val after = allocatedBytes(counter)
        return after - before
    }

    @Test
    fun allocationCounterObservesTheControlAllocation() {
        val sink = arrayOfNulls<Any>(1)
        val iterations = 200_000
        val allocated = measureAllocation(iterations) { sink[0] = Any() }
        assumeTrue("thread allocation counter unavailable", allocated != null)
        assertTrue("allocation counter is not measuring", allocated!! >= iterations)
    }

    @Test
    fun cachedFieldLookupDoesNotAllocate() {
        val clazz = Derived::class.java
        val iterations = 200_000
        val allocated = measureAllocation(iterations) {
            XposedHelpers.findField(clazz, "ownField")
        }
        assumeTrue("thread allocation counter unavailable", allocated != null)
        assertTrue(
            "cached findField allocated ${allocated!! / iterations.toDouble()} bytes per call",
            allocated / iterations.toDouble() < 1.0
        )
    }

    @Test
    fun cachedNoArgMethodLookupDoesNotAllocate() {
        val clazz = Derived::class.java
        val iterations = 200_000
        val allocated = measureAllocation(iterations) {
            XposedHelpers.findMethodBestMatch(clazz, "inheritedNoArgMethod")
        }
        assumeTrue("thread allocation counter unavailable", allocated != null)
        assertTrue(
            "cached no-arg lookup allocated ${allocated!! / iterations.toDouble()} bytes per call",
            allocated / iterations.toDouble() < 1.0
        )
    }

    @Test
    fun fieldsKeepHierarchyAndPerClassSemantics() {
        val own = XposedHelpers.findField(Derived::class.java, "ownField")
        val inherited = XposedHelpers.findField(Derived::class.java, "inheritedField")

        assertSame(own, XposedHelpers.findField(Derived::class.java, "ownField"))
        assertSame(inherited, XposedHelpers.findField(Derived::class.java, "inheritedField"))
        assertEquals(Base::class.java, inherited.declaringClass)
        assertSame(
            XposedHelpers.findField(Base::class.java, "inheritedField"),
            XposedHelpers.findField(Base::class.java, "inheritedField")
        )
    }

    @Test
    fun missingFieldKeepsNegativeResultAndErrorText() {
        val expected = "${Derived::class.java.name}#noSuchField"
        repeat(2) {
            val error = runCatching {
                XposedHelpers.findField(Derived::class.java, "noSuchField")
            }.exceptionOrNull()
            assertTrue("expected NoSuchFieldError, got $error", error is NoSuchFieldError)
            assertEquals(expected, error!!.message)
        }
    }

    @Test
    fun noArgLookupKeepsInheritedAndNegativeSemantics() {
        val method = XposedHelpers.findMethodBestMatch(
            Derived::class.java,
            "inheritedNoArgMethod"
        )
        assertSame(
            method,
            XposedHelpers.findMethodBestMatch(Derived::class.java, "inheritedNoArgMethod")
        )
        assertEquals("base", method.invoke(Derived()))

        repeat(2) {
            val error = runCatching {
                XposedHelpers.findMethodBestMatch(Derived::class.java, "noSuchMethod")
            }.exceptionOrNull()
            assertTrue("expected NoSuchMethodError, got $error", error is NoSuchMethodError)
        }
    }
}
