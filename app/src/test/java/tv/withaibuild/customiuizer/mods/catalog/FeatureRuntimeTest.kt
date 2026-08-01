package tv.withaibuild.customiuizer.mods.catalog

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
}
