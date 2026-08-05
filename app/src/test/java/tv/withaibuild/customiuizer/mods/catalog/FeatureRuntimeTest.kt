package tv.withaibuild.customiuizer.mods.catalog

import org.junit.After
import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap

class FeatureRuntimeTest {

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        DiagnosticRecorder.clock = { 0L }
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
    }

    private fun runtime(processName: String = "com.android.systemui"): FeatureRuntime {
        val prefs = PrefMap<String, Any>()
        val classLoader = this.javaClass.classLoader!!
        val lpparam = Any()
        return FeatureRuntime(processName, lpparam, classLoader, prefs)
    }

    @Test
    fun environmentAndResolverAreNotInitializedAtCreation() {
        val rt = runtime()
        assertFalse(rt.isEnvironmentInitialized())
        assertFalse(rt.isResolverInitialized())
    }

    @Test
    fun firstEnvironmentAccessInitializesAndRecords() {
        val rt = runtime()
        val env = rt.environment
        assertNotNull(env)
        assertTrue(rt.isEnvironmentInitialized())
        assertFalse(rt.isResolverInitialized())

        val snapshot = DiagnosticRecorder.summarize()["rom.environment"]
        assertNotNull(snapshot)
    }

    @Test
    fun secondEnvironmentAccessReturnsSameObjectAndDoesNotReinitialize() {
        val rt = runtime()
        val first = rt.environment
        val second = rt.environment
        assertSame(first, second)

        val snapshot = DiagnosticRecorder.summarize()["rom.environment"]
        assertNotNull(snapshot)
        assertEquals(1L, snapshot?.count)
    }

    @Test
    fun resolverInitializationIsIndependent() {
        val rt = runtime()
        val resolver = rt.resolver
        assertNotNull(resolver)
        assertFalse(rt.isEnvironmentInitialized())
        assertTrue(rt.isResolverInitialized())
    }

    @Test
    fun resolverDoesNotInitializeEnvironment() {
        val rt = runtime()
        rt.resolver
        assertFalse(rt.isEnvironmentInitialized())
    }

    @Test
    fun differentRuntimesHaveDifferentEnvironmentsAndEachRecordsOnce() {
        val rt1 = runtime("com.android.systemui")
        val rt2 = runtime("com.miui.home")

        assertTrue(rt1.environment !== rt2.environment)

        val snapshots = DiagnosticRecorder.summarize()
        val rom = snapshots["rom.environment"]
        assertNotNull(rom)
        assertEquals(2L, rom?.count)
    }

    @Test
    fun environmentRecordDoesNotPreventResolver() {
        val rt = runtime()
        rt.environment
        val resolver = rt.resolver
        assertNotNull(resolver)
        assertTrue(rt.isResolverInitialized())
    }

    @Test
    fun diagnosticsLoggerRuntimeExceptionDoesNotBlockEnvironment() {
        DiagnosticRecorder.logger = { throw RuntimeException("diagnostic logger failure") }
        val rt = runtime()

        val env = rt.environment
        assertNotNull(env)
        assertTrue(rt.isEnvironmentInitialized())

        val resolver = rt.resolver
        assertNotNull(resolver)
    }

    @Test
    fun diagnosticsLoggerOomIsRethrown() {
        DiagnosticRecorder.logger = { throw OutOfMemoryError() }
        val rt = runtime()
        try {
            rt.environment
            assertTrue("expected OOM", false)
        } catch (oom: OutOfMemoryError) {
            // expected
        }
    }

    @Test
    fun diagnosticsClockWrappedOutOfMemoryRethrowsOriginal() {
        val failure = OutOfMemoryError("clock oom")
        DiagnosticRecorder.clock = { throw RuntimeException(failure) }
        val rt = runtime()

        val thrown = try {
            rt.environment
            fail("expected OutOfMemoryError")
            null
        } catch (oom: OutOfMemoryError) {
            oom
        }

        assertSame(failure, thrown)
    }

    @Test
    fun diagnosticsClockWrappedThreadDeathRethrowsOriginal() {
        val failure = ThreadDeath()
        DiagnosticRecorder.clock = { throw InvocationTargetException(failure) }
        val rt = runtime()

        val thrown = try {
            rt.environment
            fail("expected ThreadDeath")
            null
        } catch (td: ThreadDeath) {
            td
        }

        assertSame(failure, thrown)
    }

    @Test
    fun diagnosticsClockDoubleWrappedInternalErrorRethrowsOriginal() {
        val failure = InternalError("clock internal")
        DiagnosticRecorder.clock = { throw RuntimeException(RuntimeException(failure)) }
        val rt = runtime()

        val thrown = try {
            rt.environment
            fail("expected InternalError")
            null
        } catch (ie: InternalError) {
            ie
        }

        assertSame(failure, thrown)
    }

    @Test
    fun diagnosticsClockWrappedOrdinaryExceptionAllowsEnvironmentAndResolver() {
        DiagnosticRecorder.clock = { throw RuntimeException(IllegalStateException("clock failed")) }
        val rt = runtime()

        val env = rt.environment
        assertNotNull(env)
        assertTrue(rt.isEnvironmentInitialized())

        val resolver = rt.resolver
        assertNotNull(resolver)
        assertTrue(rt.isResolverInitialized())
    }

    @Test
    fun diagnosticsLoggerWrappedFatalRethrowsOriginal() {
        val failure = InternalError("logger internal")
        val wrapper = RuntimeException(RuntimeException(failure))
        DiagnosticRecorder.logger = { throw wrapper }
        val rt = runtime()

        val thrown = try {
            rt.environment
            fail("expected InternalError")
            null
        } catch (ie: InternalError) {
            ie
        }

        assertSame(failure, thrown)
    }
}
