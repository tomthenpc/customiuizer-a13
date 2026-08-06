package tv.withaibuild.customiuizer

import android.os.Build
import android.util.SparseArray
import android.util.SparseIntArray
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks
import tv.withaibuild.customiuizer.utils.FakeSharedPreferences
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests for MainModule -> LauncherInstaller startup gating.
 */
class MainModuleLauncherRoutingTest {

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

    private fun moduleWithPrefs(
        prefs: Map<String, Any>,
        process: String = "com.miui.home"
    ): MainModule {
        fakeSharedPreferences.setAll(prefs)
        fakeSharedPreferences.failFirstRegister = true
        fakeSharedPreferences.reset()
        fakeSharedPreferences.failFirstRegister = true
        fakeSharedPreferences.setAll(prefs)
        val module = MainModule()
        module.attachFramework(FakeXposedInterface.create())
        module.onModuleLoaded(moduleLoadedParam(process))
        return module
    }

    private fun resourceHookCount(): Int {
        val resHooks = MainModule.getResHooks()
        val f = ResourceHooks::class.java.getDeclaredField("unresolved")
        f.isAccessible = true
        return (f.get(resHooks) as? ConcurrentHashMap<*, *>)?.size ?: 0
    }

    @Test
    fun allOff_doesNotInstallLauncherHooksOrWatcher() {
        val module = moduleWithPrefs(emptyMap())
        module.onPackageReady(packageReadyParam("com.miui.home", "com.miui.home"))

        assertEquals(0, FakeXposedInterface.recordedHooks.size)
        assertEquals(0, resourceHookCount())
        assertEquals(0, fakeSharedPreferences.registerCount)
    }

    @Test
    fun packageReadyOnly_installsResourceHooksButNoAttachHookOrWatcher() {
        val module = moduleWithPrefs(mapOf("launcher_horizmargin" to 10))
        module.onPackageReady(packageReadyParam("com.miui.home", "com.miui.home"))

        val attachHooks = FakeXposedInterface.recordedHooks.count { it.executable.name == "attach" }
        assertEquals(0, attachHooks)
        assertTrue("resource hooks should be installed", resourceHookCount() > 0)
        assertEquals(0, fakeSharedPreferences.registerCount)
    }

    @Test
    fun applicationOnly_installsAttachHookAndWatcher() {
        val module = moduleWithPrefs(mapOf("launcher_swipedown_action" to 2))
        module.onPackageReady(packageReadyParam("com.miui.home", "com.miui.home"))

        val attachHooks = FakeXposedInterface.recordedHooks.count { it.executable.name == "attach" }
        assertEquals(1, attachHooks)
        assertEquals(0, resourceHookCount())
        assertEquals(1, fakeSharedPreferences.registerCount)
    }

    @Test
    fun both_installsResourceAndAttachHooksAndOneWatcher() {
        val module = moduleWithPrefs(mapOf("launcher_horizmargin" to 10, "launcher_swipedown_action" to 2))
        module.onPackageReady(packageReadyParam("com.miui.home", "com.miui.home"))

        val attachHooks = FakeXposedInterface.recordedHooks.count { it.executable.name == "attach" }
        assertEquals(1, attachHooks)
        assertTrue("resource hooks should be installed", resourceHookCount() > 0)
        assertEquals(1, fakeSharedPreferences.registerCount)
    }

    @Test
    fun nonLauncherPackage_doesNotInstallLauncherHooks() {
        val module = moduleWithPrefs(mapOf("launcher_swipedown_action" to 2), "com.android.systemui")
        module.onPackageReady(packageReadyParam("com.android.systemui", "com.android.systemui"))

        val attachHooks = FakeXposedInterface.recordedHooks.count { it.executable.name == "attach" }
        assertEquals(0, attachHooks)
        assertEquals(0, resourceHookCount())
        assertEquals(0, fakeSharedPreferences.registerCount)
    }
}
