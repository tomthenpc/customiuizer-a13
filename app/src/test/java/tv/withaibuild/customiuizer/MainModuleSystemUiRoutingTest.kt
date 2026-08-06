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
 * - listener registration deltas (PreferenceBootstrap baseline vs SystemUI watcher)
 * - manual execution of the onCreate callback and its `isHooked` de-duplication
 *
 * FeatureRuntime creation is NOT verified in this harness, because the JVM
 * unit test path returns early from the restart-time guard. The guard's
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
        fakeSharedPreferences.failFirstRegister = true
        fakeSharedPreferences.reset()
        fakeSharedPreferences.failFirstRegister = true
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

    private data class GateSnapshot(
        val hooks: Int,
        val resources: Int,
        val registerAttempts: Int,
        val registerSuccess: Int,
        val watcherAttempts: Int,
        val activeListeners: Set<String>
    )

    private fun before(module: MainModule): GateSnapshot {
        return GateSnapshot(
            hooks = FakeXposedInterface.recordedHooks.size,
            resources = resourceHookCount(),
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
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertEquals("no onCreate hook", base.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(base.resources, resourceHookCount())
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        // PreferenceBootstrap baseline listener may be registered by initPrefs();
        // the SystemUI watcher must not be.
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
    }

    @Test
    fun unknownActionInSystemUiProcess_doesNotTriggerInstall() {
        val module = moduleWithPrefs(mapOf("unknown_feature_action" to 2))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertEquals("no onCreate hook", base.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(base.resources, resourceHookCount())
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
    }

    @Test
    fun validSystemUiAction_installsOnCreateHookAndRegistersWatcherOnce() {
        val module = moduleWithPrefs(mapOf("controls_backlong_action" to 2))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertTrue("SystemUI onCreate hook should be installed", FakeXposedInterface.recordedHooks.size > base.hooks)
        assertEquals(base.resources, resourceHookCount())

        // Before onCreate executes, the SystemUI watcher is not yet registered.
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)

        // First onCreate after-callback registers the watcher exactly once.
        // In this harness initPrefs() is set to fail its first listener attempt,
        // so the first successful registration happens during onCreate.
        executeSystemUiOnCreate()
        assertEquals(base.watcherAttempts + 1, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        assertEquals(base.registerSuccess + 1, fakeSharedPreferences.registerCount)
        assertEquals(base.activeListeners.size + 1, fakeSharedPreferences.activeListenerIdentities.size)

        // Second onCreate after-callback is a no-op due to isHooked.
        val beforeSecond = before(module)
        executeSystemUiOnCreate()
        assertEquals(beforeSecond.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        assertEquals(beforeSecond.registerSuccess, fakeSharedPreferences.registerCount)
        assertEquals(beforeSecond.activeListeners.size, fakeSharedPreferences.activeListenerIdentities.size)
    }

    @Test
    fun resourceOnlyFeature_isNotSkippedByGate() {
        val module = moduleWithPrefs(mapOf("system_statusbarheight" to 20))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertTrue("Resource hooks should be installed for system_statusbarheight", resourceHookCount() > base.resources)
        assertTrue("onCreate hook should still be installed", FakeXposedInterface.recordedHooks.size > base.hooks)

        // Watcher is only registered after onCreate.
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
        executeSystemUiOnCreate()
        assertEquals(base.watcherAttempts + 1, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
    }

    @Test
    fun navbarResourceOnlyFeature_isNotSkippedByGate() {
        val module = moduleWithPrefs(mapOf("controls_navbarheight" to 20))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        assertTrue("Resource hooks should be installed for controls_navbarheight", resourceHookCount() > base.resources)
        assertTrue("onCreate hook should still be installed", FakeXposedInterface.recordedHooks.size > base.hooks)
    }

    @Test
    fun nonSystemUiPackage_doesNotInstallSystemUiHooks() {
        val module = moduleWithPrefs(mapOf("controls_backlong_action" to 2))
        val base = before(module)
        module.onPackageReady(packageReadyParam("com.some.other.app", "com.some.other.app"))

        assertEquals(base.hooks, FakeXposedInterface.recordedHooks.size)
        assertEquals(base.watcherAttempts, fakeSharedPreferences.systemUiWatcherRegisterAttempts)
    }
}
