package tv.withaibuild.customiuizer.mods.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap

class FeatureRuntimeTest {

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        DiagnosticRecorder.clock = { 0L }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
    }

    private fun runtime(): FeatureRuntime {
        val prefs = PrefMap<String, Any>()
        val classLoader = this.javaClass.classLoader!!
        val lpparam = Any()
        return FeatureRuntime("com.android.systemui", lpparam, classLoader, prefs)
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
    fun secondEnvironmentAccessReturnsSameObject() {
        val rt = runtime()
        val first = rt.environment
        val second = rt.environment
        assertSame(first, second)
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
    fun differentRuntimesHaveDifferentEnvironments() {
        val rt1 = runtime()
        val rt2 = runtime()
        assertTrue(rt1.environment !== rt2.environment)
    }

    @Test
    fun environmentRecordDoesNotPreventResolver() {
        val rt = runtime()
        rt.environment
        val resolver = rt.resolver
        assertNotNull(resolver)
    }
}
