package tv.withaibuild.customiuizer

import android.os.Build
import android.util.SparseArray
import android.util.SparseIntArray
import com.android.systemui.SystemUIApplication
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeSharedPreferences
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests for MainModule -> SystemUiInstaller startup gating.
 *
 * These tests assert gate-driven side effects on the SystemUI package:
 * - `SystemUIApplication#onCreate` hook installation
 * - resource hook registration
 * - listener registration lifecycle (PreferenceBootstrap baseline)
 * - manual execution of the onCreate callback and its `isHooked` de-duplication
 *
 * Important: `MainModule.watchPreferenceChange()` delegates to
 * `PreferenceBootstrap.ensureWatcher()`, which is a no-op when the baseline
 * listener is already registered. Therefore `onCreate` does NOT add a second
 * listener; it only runs `SystemUIStatusBarHooks.setupStatusBar()` once and
 * triggers the ensure path. The previous R1-B0 harness used
 * `failFirstRegister=true` to force a re-registration, which created the
 * illusion of a separate SystemUI watcher.
 *
 * FeatureRuntime creation is NOT verified in this harness, because the JVM
 * unit-test path returns early from the restart-time guard. The guard's
 * pure logic is tested separately in [SystemUiRestartGuardTest].
 */
class MainModuleSystemUiRoutingTest {

    private val classLoader = this.javaClass.classLoader!!
    private lateinit var fakeSharedPreferences: FakeSharedPreferences

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        Build.VERSION.SDK_INT = Build.VERSION_CODES.TIRAMISU
        resetResourceHooks()
        fakeSharedPreferences = FakeSharedPreferences()
        FakeXposedInterface.remotePreferences = fakeSharedPreferences
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
        Build.VERSION.SDK_INT = 0
        fakeSharedPreferences.reset()
        resetResourceHooks()
    }

    private fun resetResourceHooks() {
        val resHooks = MainModule.getResHooks()
        val installStateClass = Class.forName("tv.withaibuild.customiuizer.mods.utils.ResourceHooks\$InstallState")
        val uninstallField = installStateClass.getDeclaredField("UNINSTALLED").apply { isAccessible = true }
        ResourceHooks::class.java.getDeclaredField("installState").apply {
            isAccessible = true
            (get(resHooks) as AtomicReference<Any>).set(uninstallField.get(null))
        }
        ResourceHooks::class.java.getDeclaredField("installedMask").apply {
            isAccessible = true
            (get(resHooks) as AtomicInteger).set(0)
        }
        ResourceHooks::class.java.getDeclaredField("fakes").apply {
            isAccessible = true
            (get(resHooks) as SparseIntArray).clear()
        }
        ResourceHooks::class.java.getDeclaredField("unresolved").apply {
            isAccessible = true
            (get(resHooks) as ConcurrentHashMap<*, *>).clear()
        }
        ResourceHooks::class.java.getDeclaredField("active").apply {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (get(resHooks) as AtomicReference<SparseArray<Any?>>).set(SparseArray())
        }
    }

    private fun moduleLoadedParam(process: String): XposedModuleInterface.ModuleLoadedParam =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedModuleInterface.ModuleLoadedParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getProcessName" -> process
                "getPackageName" -> process
                else -> null
            }
        } as XposedModuleInterface.ModuleLoadedParam

    private fun packageReadyParam(pkg: String, process: String): XposedModuleInterface.PackageReadyParam =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedModuleInterface.PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> pkg
                "getProcessName" -> process
                "getClassLoader" -> classLoader
                "isFirstPackage" -> true
                else -> null
            }
        } as XposedModuleInterface.PackageReadyParam

    private fun moduleWithPrefs(prefs: Map<String, Any>): MainModule {
        fakeSharedPreferences.setAll(prefs)
        fakeSharedPreferences.failFirstRegister = false
        fakeSharedPreferences.reset()
        fakeSharedPreferences.failFirstRegister = false
        fakeSharedPreferences.setAll(prefs)
        val module = MainModule()
        module.attachFramework(FakeXposedInterface.create())
        module.onModuleLoaded(moduleLoadedParam("com.android.systemui"))
        return module
    }

    private fun resourceHookCount(): Int {
        val resHooks = MainModule.getResHooks()
        val f = ResourceHooks::class.java.getDeclaredField("unresolved")
        f.isAccessible = true
        return (f.get(resHooks) as? ConcurrentHashMap<*, *>)?.size ?: 0
    }

    private fun fakesCount(): Int {
        val resHooks = MainModule.getResHooks()
        val f = ResourceHooks::class.java.getDeclaredField("fakes")
        f.isAccessible = true
        return (f.get(resHooks) as? SparseIntArray)?.size() ?: 0
    }

    private data class GateSnapshot(
        val hooks: Int,
        val resources: Int,
        val fakes: Int,
        val registerAttempts: Int,
        val registerSuccess: Int,
        val watcherAttempts: Int,
        val activeListeners: Set<String>
    )

    private fun before(module: MainModule): GateSnapshot {
        return GateSnapshot(
            hooks = FakeXposedInterface.recordedHooks.size,
            resources = resourceHookCount(),
            fakes = fakesCount(),
            registerAttempts = fakeSharedPreferences.registerAttemptCount,
            registerSuccess = fakeSharedPreferences.registerCount,
            watcherAttempts = fakeSharedPreferences.systemUiWatcherRegisterAttempts,
            activeListeners = fakeSharedPreferences.activeListenerIdentities
        )
    }

    private fun executeSystemUiOnCreate(): Any {
        val app = SystemUIApplication()
        val recorded = FakeXposedInterface.findHook("com.android.systemui.SystemUIApplication", "onCreate")
        assertNotNull("SystemUIApplication#onCreate hook must be installed", recorded)
        FakeXposedInterface.executeAfter(recorded!!, app)
        return app
    }

    @Test
    fun allOff_doesNotInstallSystemUiHooksOrOnCreate() {
        val module = moduleWithPrefs(emptyMap())
        val beforeLoaded = before(module)

        // onModuleLoaded does not register the preference listener.
        assertEquals(0, beforeLoaded.registerSuccess)
        assertEquals(0, beforeLoaded.activeListeners.size)

        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertEquals("no onCreate hook", base.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(base.resources, resourceHookCount())
        assertEquals(base.fakes, fakesCount())
        // PreferenceBootstrap baseline listener registered by initPrefs().
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
        assertEquals(base.activeListeners.size + 1, fakeSharedPreferences.activeListenerIdentities.size)
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        assertEquals(0, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
    }

    @Test
    fun launcherActionInSystemUiProcess_doesNotTriggerInstall() {
        val module = moduleWithPrefs(mapOf("launcher_swipedown_action" to 2))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertEquals("no onCreate hook", base.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(base.resources, resourceHookCount())
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
        assertEquals(base.activeListeners.size + 1, fakeSharedPreferences.activeListenerIdentities.size)
    }

    @Test
    fun unknownActionInSystemUiProcess_doesNotTriggerInstall() {
        val module = moduleWithPrefs(mapOf("unknown_feature_action" to 2))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertEquals("no onCreate hook", base.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(base.resources, resourceHookCount())
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
        assertEquals(base.activeListeners.size + 1, fakeSharedPreferences.activeListenerIdentities.size)
    }

    @Test
    fun validSystemUiAction_installsOnCreateHookAndExecutesWatchPreferencesOnce() {
        val module = moduleWithPrefs(mapOf("controls_backlong_action" to 2))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertTrue("SystemUI onCreate hook should be installed", FakeXposedInterface.recordedHooks.size > base.hooks)
        assertEquals(base.resources, resourceHookCount())

        // After onPackageReady the baseline is active; no SystemUI watcher has
        // registered yet (and ensureWatcher will be a no-op once it is called).
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
        assertEquals(base.activeListeners.size + 1, fakeSharedPreferences.activeListenerIdentities.size)
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)

        // First onCreate after-callback runs setupStatusBar and watchPreferences.
        // Because the baseline listener is already active, ensureWatcher does not
        // register a second listener; it only verifies the existing watcher.
        MainModule.getResHooks().addResource("test", 123)
        assertEquals(base.fakes + 1, fakesCount())
        executeSystemUiOnCreate()
        assertEquals(base.fakes + 2, fakesCount())
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
        assertEquals(base.activeListeners.size + 1, fakeSharedPreferences.activeListenerIdentities.size)
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)

        // Second onCreate after-callback is a no-op due to isHooked.
        val beforeSecond = before(module)
        executeSystemUiOnCreate()
        assertEquals(beforeSecond.fakes, fakesCount())
        assertEquals(beforeSecond.registerSuccess, fakeSharedPreferences.registerCount)
        assertEquals(beforeSecond.activeListeners.size, fakeSharedPreferences.activeListenerIdentities.size)
        assertEquals(beforeSecond.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
    }

    @Test
    fun resourceOnlyFeature_isNotSkippedByGate() {
        val module = moduleWithPrefs(mapOf("system_statusbarheight" to 20))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertTrue("Resource hooks should be installed for system_statusbarheight", resourceHookCount() > base.resources)
        assertTrue("onCreate hook should still be installed", FakeXposedInterface.recordedHooks.size > base.hooks)
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)

        // Watcher/ensure path is only invoked after onCreate.
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        executeSystemUiOnCreate()
        assertEquals(base.fakes + 1, fakesCount())
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
    }

    @Test
    fun navbarResourceOnlyFeature_isNotSkippedByGate() {
        val module = moduleWithPrefs(mapOf("controls_navbarheight" to 20))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertTrue("Resource hooks should be installed for controls_navbarheight", resourceHookCount() > base.resources)
        assertTrue("onCreate hook should still be installed", FakeXposedInterface.recordedHooks.size > base.hooks)
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
    }

    @Test
    fun nonSystemUiPackage_doesNotInstallSystemUiHooks() {
        val module = moduleWithPrefs(mapOf("controls_backlong_action" to 2))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.some.other.app", "com.some.other.app"))

        assertEquals(base.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        assertEquals(base.registerSuccess, fakeSharedPreferences.registerCount)
        assertEquals(0, fakeSharedPreferences.registerCount)
    }

    @Test
    fun consecutiveRunsAreIsolated() {
        // First run: full valid SystemUI lifecycle.
        val first = moduleWithPrefs(mapOf("controls_backlong_action" to 2))
        first.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))
        executeSystemUiOnCreate()
        val afterFirst = before(first)

        // Simulate tearDown / setUp reset manually.
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        fakeSharedPreferences.reset()
        resetResourceHooks()
        FakeXposedInterface.remotePreferences = fakeSharedPreferences

        // Second run: all-off. No previous watcher should leak.
        val second = moduleWithPrefs(emptyMap())
        val beforeSecond = before(second)
        second.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertEquals("second run starts with no hooks", 0, beforeSecond.hooks)
        assertEquals("second run starts with no active listeners", 0, beforeSecond.activeListeners.size)
        assertEquals("second run starts with no register attempts", 0, beforeSecond.registerAttempts)

        assertEquals("no onCreate hook", beforeSecond.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(0, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        assertEquals(1, fakeSharedPreferences.registerCount)
        assertEquals(1, fakeSharedPreferences.activeListenerIdentities.size)

        // Original first-run state should not be confused with the second.
        assertTrue("first run had more hooks", afterFirst.hooks > 0)
        assertTrue("first run had baseline listener", afterFirst.registerSuccess > 0)
    }
}
