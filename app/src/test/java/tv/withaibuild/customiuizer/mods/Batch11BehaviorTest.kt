package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.FeatureCatalog
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher
import tv.withaibuild.customiuizer.mods.catalog.FeatureInstallRegistry
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.InstallPhase
import tv.withaibuild.customiuizer.mods.utils.ProcessScope
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Executable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.LinkedHashSet

/**
 * Behavior tests for catalog expansion batch 11 (user-designated) features:
 * appsDisableService, noAccessDeviceLogsRequest, autoGroupNotifications and
 * appLockTimeout.
 */
class Batch11BehaviorTest {

    private val logMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        logMessages.clear()
        DiagnosticRecorder.clock = { 0L }
        DiagnosticRecorder.logger = { line ->
            if (!line.startsWith("Diagnostic[rom.environment]")) logMessages += line
        }
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
    }

    @After
    fun tearDown() {
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        MainModule.mPrefs = PrefMap()
        XposedHelpers.moduleInst = null
    }

    private fun runtime(prefs: PrefMap<String, Any>): FeatureRuntime {
        @Suppress("UNCHECKED_CAST")
        MainModule.mPrefs = prefs as PrefMap<String, Any>
        val classLoader = this.javaClass.classLoader!!
        val lpparam = Proxy.newProxyInstance(
            classLoader,
            arrayOf(io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam::class.java),
            InvocationHandler { _, m, _ ->
                when (m.name) {
                    "getClassLoader" -> classLoader
                    "getProcessName" -> "android"
                    else -> null
                }
            }
        ) as io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
        return FeatureDispatcher.createRuntime("android", lpparam, classLoader, prefs)
    }

    // ----------------------------------------------------------------------
    // Catalog-level behavior
    // ----------------------------------------------------------------------

    @Test
    fun disabledPathDoesNotExecuteInstaller() {
        val prefs = PrefMap<String, Any>()
        val server = runtime(prefs)

        assertFalse("appsDisableService disabled", FeatureDispatcher.installById("appsDisableService", server))
        assertFalse("noAccessDeviceLogsRequest disabled", FeatureDispatcher.installById("noAccessDeviceLogsRequest", server))
        assertFalse("autoGroupNotifications disabled", FeatureDispatcher.installById("autoGroupNotifications", server))
        assertFalse("appLockTimeout disabled", FeatureDispatcher.installById("appLockTimeout", server))
    }

    @Test
    fun eachFeatureInstallsOnlyOnce() {
        val prefs = PrefMap<String, Any>()
        prefs["various_disableapp"] = true
        prefs["various_disable_access_devicelogs"] = true
        prefs["system_autogroupnotif"] = "2"
        prefs["system_applock_timeout"] = 10
        val server = runtime(prefs)

        assertTrue("appsDisableService first install", FeatureDispatcher.installById("appsDisableService", server))
        assertTrue("appsDisableService second install is AlreadyInstalled", FeatureDispatcher.installById("appsDisableService", server))

        assertTrue("noAccessDeviceLogsRequest first install", FeatureDispatcher.installById("noAccessDeviceLogsRequest", server))
        assertTrue("noAccessDeviceLogsRequest second install is AlreadyInstalled", FeatureDispatcher.installById("noAccessDeviceLogsRequest", server))

        assertTrue("autoGroupNotifications first install", FeatureDispatcher.installById("autoGroupNotifications", server))
        assertTrue("autoGroupNotifications second install is AlreadyInstalled", FeatureDispatcher.installById("autoGroupNotifications", server))

        assertTrue("appLockTimeout first install", FeatureDispatcher.installById("appLockTimeout", server))
        assertTrue("appLockTimeout second install is AlreadyInstalled", FeatureDispatcher.installById("appLockTimeout", server))
    }

    @Test
    fun catalogInstallerEvidenceMatchesContract() {
        val spec = FeatureCatalog.specs().find { it.id == "autoGroupNotifications" }!!
        val prefs = PrefMap<String, Any>()
        prefs["system_autogroupnotif"] = "2"
        val server = runtime(prefs)

        val result = FeatureInstallRegistry.installById(
            "autoGroupNotifications",
            ProcessScope.SYSTEM_SERVER,
            InstallPhase.SYSTEM_SERVER_STARTING,
            server
        )

        assertTrue("autoGroupNotifications installed", result.isActive)
        val installed = result as? tv.withaibuild.customiuizer.mods.utils.FeatureInstallResult.Installed
        assertNotNull(installed)
        val summary = installed!!.installSummary
        assertNotNull(summary)
        assertEquals(2, summary!!.requiredTotal)
    }

    // ----------------------------------------------------------------------
    // Hook callbacks
    // ----------------------------------------------------------------------

    private fun fakeChain(
        target: Method,
        thisObject: Any?,
        args: List<Any>,
        result: Any? = null,
        failure: Throwable? = null
    ): FakeChain = FakeChain(target, thisObject, java.util.ArrayList(args), result, failure)

    private fun targetMethod(name: String): Method {
        return Batch11BehaviorTest::class.java.getDeclaredMethod(name)
    }

    @Test
    fun noAccessDeviceLogs_declinesRequestAndSkipsOriginal() {
        val hook = SystemDisplayAndWindowHooks.createNoAccessDeviceLogsHook()

        class FakeService {
            var declinedClient: Any? = null
            fun declineRequest(client: Any?) {
                declinedClient = client
            }
        }

        val client = Any()
        val service = FakeService()
        val chain = fakeChain(
            targetMethod("noAccessDeviceLogs_declinesRequestAndSkipsOriginal"),
            service,
            listOf(client),
            null,
            null
        )

        val result = hook.intercept(chain)
        assertNull("Original method is skipped", result)
        assertFalse("chain.proceed is not called", chain.proceeded)
        assertSame("declineRequest called with client", client, service.declinedClient)
    }

    @Test
    fun appsDisableService_doesNotMaskOriginalException() {
        val hook = Various.createAppsDisableServiceHook()
        val chain = fakeChain(
            targetMethod("appsDisableService_doesNotMaskOriginalException"),
            null,
            listOf("com.example.app" as Any, 0 as Any),
            null,
            IllegalStateException("original failure")
        )

        try {
            hook.intercept(chain)
            assertTrue("Expected original exception to propagate", false)
        } catch (t: Throwable) {
            assertTrue("Original exception rethrown", t is IllegalStateException && t.message == "original failure")
        }
    }

    @Test
    fun appsDisableService_doesNotOverrideMiuiCoreApp() {
        val hook = Various.createAppsDisableServiceHook()
        val chain = fakeChain(
            target = targetMethod("appsDisableService_doesNotOverrideMiuiCoreApp"),
            thisObject = null,
            args = listOf("com.miui.securitycenter", 0),
            result = false
        )

        val result = hook.intercept(chain)
        assertTrue("chain proceeded", chain.proceeded)
        assertEquals(false, result)
    }

    @Test
    fun appsDisableService_overridesNonCoreAppWhenDisabled() {
        val hook = Various.createAppsDisableServiceHook()
        val chain = fakeChain(
            target = targetMethod("appsDisableService_overridesNonCoreAppWhenDisabled"),
            thisObject = null,
            args = listOf("com.example.app", 0),
            result = false
        )

        val result = hook.intercept(chain)
        assertTrue("chain proceeded", chain.proceeded)
        assertEquals(true, result)
    }

    @Test
    fun appsDisableService_leavesEnabledAppsAlone() {
        val hook = Various.createAppsDisableServiceHook()
        val chain = fakeChain(
            target = targetMethod("appsDisableService_leavesEnabledAppsAlone"),
            thisObject = null,
            args = listOf("com.example.app", 0),
            result = true
        )

        val result = hook.intercept(chain)
        assertTrue("chain proceeded", chain.proceeded)
        assertEquals(true, result)
    }

    @Test
    fun autoGroup_option2SkipsBothHooks() {
        val prefs = PrefMap<String, Any>()
        prefs["system_autogroupnotif"] = "2"
        MainModule.mPrefs = prefs

        val summaryHook = SystemNotificationAndShareHooks.createAdjustAutogroupingSummaryHook()
        val bundlingHook = SystemNotificationAndShareHooks.createAdjustNotificationBundlingHook()

        val service = Any()
        val summaryChain = fakeChain(
            target = targetMethod("autoGroup_option2SkipsBothHooks"),
            thisObject = service,
            args = listOf(1, "key", "summary", true)
        )
        val bundlingChain = fakeChain(
            target = targetMethod("autoGroup_option2SkipsBothHooks"),
            thisObject = service,
            args = listOf(emptyList<Any>(), true)
        )

        assertNull(summaryHook.intercept(summaryChain))
        assertNull(bundlingHook.intercept(bundlingChain))
        assertFalse(summaryChain.proceeded)
        assertFalse(bundlingChain.proceeded)
    }

    @Test
    fun autoGroup_thresholdBelowOptionSkipsBundling() {
        val prefs = PrefMap<String, Any>()
        prefs["system_autogroupnotif"] = "5"
        MainModule.mPrefs = prefs

        val bundlingHook = SystemNotificationAndShareHooks.createAdjustNotificationBundlingHook()
        val list = listOf("a", "b", "c")
        val chain = fakeChain(
            target = targetMethod("autoGroup_thresholdBelowOptionSkipsBundling"),
            thisObject = null,
            args = listOf(list, true)
        )

        assertNull(bundlingHook.intercept(chain))
        assertFalse("bundling skipped when list size < threshold", chain.proceeded)
    }

    @Test
    fun autoGroup_thresholdMetAllowsBundling() {
        val prefs = PrefMap<String, Any>()
        prefs["system_autogroupnotif"] = "3"
        MainModule.mPrefs = prefs

        val bundlingHook = SystemNotificationAndShareHooks.createAdjustNotificationBundlingHook()
        val list = listOf("a", "b", "c")
        val chain = fakeChain(
            target = targetMethod("autoGroup_thresholdMetAllowsBundling"),
            thisObject = null,
            args = listOf(list, true),
            result = Unit
        )

        val result = bundlingHook.intercept(chain)
        assertTrue("bundling proceeds when threshold met", chain.proceeded)
        assertEquals(Unit, result)
    }

    @Test
    fun autoGroup_summaryThresholdBelowOptionSkips() {
        val prefs = PrefMap<String, Any>()
        prefs["system_autogroupnotif"] = "5"
        MainModule.mPrefs = prefs

        val summaryHook = SystemNotificationAndShareHooks.createAdjustAutogroupingSummaryHook()
        val map: MutableMap<Int, Map<String, LinkedHashSet<String>>> = mutableMapOf(
            1 to mapOf("pkg" to LinkedHashSet(listOf("a", "b")))
        )

        class FakeGroupHelper {
            @Suppress("UNCHECKED_CAST")
            val mUngroupedNotifications = map as java.util.Map<Int, Map<String, LinkedHashSet<String>>>
        }

        val chain = fakeChain(
            target = targetMethod("autoGroup_summaryThresholdBelowOptionSkips"),
            thisObject = FakeGroupHelper(),
            args = listOf(1, "pkg", "summary", true)
        )

        assertNull(summaryHook.intercept(chain))
        assertFalse("summary skipped when per-package count < threshold", chain.proceeded)
    }

    class FakeAppLockService {
        val stateAccess = mutableMapOf<Int, Any>()

        fun getApplicationAccessControlEnabledAsUser(pkgName: String, userId: Int): Boolean {
            return pkgName != "com.miui.home"
        }

        fun getUserStateLocked(userId: Int): Any {
            return stateAccess.getOrPut(userId) {
                object {
                    @Suppress("UNCHECKED_CAST")
                    val mAccessControlLastCheck = android.util.ArrayMap<String, Long>()
                }
            }
        }
    }

    @Test
    fun appLockTimeout_twoEntryPointsApplySaveAndCheck() {
        val prefs = PrefMap<String, Any>()
        prefs["system_applock_timeout"] = 1
        MainModule.mPrefs = prefs

        val hook1 = SystemLockScreenMoreHooks.createAddAccessControlPassForUserHook()
        val hook2 = SystemLockScreenMoreHooks.createCheckAccessControlPassLockedHook()

        val service = FakeAppLockService()

        val chain1 = fakeChain(
            target = targetMethod("appLockTimeout_twoEntryPointsApplySaveAndCheck"),
            thisObject = service,
            args = listOf("com.example.app", 0),
            result = null
        )
        hook1.intercept(chain1)

        val chain2 = fakeChain(
            target = targetMethod("appLockTimeout_twoEntryPointsApplySaveAndCheck"),
            thisObject = service,
            args = listOf("com.example.app", android.content.Intent(), 0),
            result = null
        )
        hook2.intercept(chain2)

        // Both hooks reach the same save/restore helpers; the userState object is
        // the same, proving both entry points share a single last-check timeline.
        assertEquals(
            "both entry points use the same user state for package/user",
            1,
            service.stateAccess.size
        )
    }
}
