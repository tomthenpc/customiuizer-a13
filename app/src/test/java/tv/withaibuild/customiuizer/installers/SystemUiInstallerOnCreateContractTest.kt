package tv.withaibuild.customiuizer.installers

import android.os.Build
import android.util.SparseArray
import android.util.SparseIntArray
import com.android.systemui.SystemUIApplication
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
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
 * Direct contract tests for [SystemUiInstaller.install].
 *
 * These tests bypass [MainModule] routing and call the installer directly so
 * they can prove the [SystemUIApplication#onCreate] after-callback:
 * - installs exactly one onCreate hook
 * - invokes the supplied [watchPreferences] [Runnable] exactly once
 * - de-duplicates the callback with its [isHooked] flag
 * - still executes [SystemUIStatusBarHooks.setupStatusBar] side effects
 */
class SystemUiInstallerOnCreateContractTest {

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

    private fun fakesCount(): Int {
        val resHooks = MainModule.getResHooks()
        val f = ResourceHooks::class.java.getDeclaredField("fakes")
        f.isAccessible = true
        return (f.get(resHooks) as? SparseIntArray)?.size() ?: 0
    }

    private fun executeSystemUiOnCreate(): Any {
        val app = SystemUIApplication()
        val recorded = FakeXposedInterface.findHook("com.android.systemui.SystemUIApplication", "onCreate")
        assertNotNull("SystemUIApplication#onCreate hook must be installed", recorded)
        FakeXposedInterface.executeAfter(recorded!!, app)
        return app
    }

    @Test
    fun onCreateAfterCallbackInvokesWatchPreferencesExactlyOnce() {
        val watchCalls = AtomicInteger(0)
        val lpparam = packageReadyParam("com.android.systemui", "com.android.systemui")
        val watchPreferences = Runnable { watchCalls.incrementAndGet() }

        // A valid SystemUI global action causes the installer to hook onCreate.
        MainModule.mPrefs["controls_backlong_action"] = 2

        val baseFakes = fakesCount()
        val beforeHooks = FakeXposedInterface.recordedHooks.size

        SystemUiInstaller.install(lpparam, watchPreferences)

        // Exactly one onCreate hook should have been registered.
        val onCreateHooks = FakeXposedInterface.recordedHooks.filter {
            it.executable.declaringClass?.name == "com.android.systemui.SystemUIApplication" &&
                it.executable.name == "onCreate"
        }
        assertEquals(1, onCreateHooks.size)
        assertTrue("additional hooks may be installed", FakeXposedInterface.recordedHooks.size > beforeHooks)

        // Before the first callback the Runnable has not been executed.
        assertEquals(0, watchCalls.get())
        assertEquals(baseFakes, fakesCount())

        // First onCreate after-callback runs setupStatusBar and then watchPreferences.
        executeSystemUiOnCreate()
        assertEquals(1, watchCalls.get())
        assertTrue("setupStatusBar should have added at least one fake resource", fakesCount() > baseFakes)

        // Second callback is a no-op due to the MethodHook isHooked flag.
        executeSystemUiOnCreate()
        assertEquals(1, watchCalls.get())
        assertEquals(baseFakes + 1, fakesCount())
    }

    @Test
    fun sameWatchPreferencesInstanceIsUsed() {
        val watchCalls = AtomicInteger(0)
        val lpparam = packageReadyParam("com.android.systemui", "com.android.systemui")
        val watchPreferences = Runnable { watchCalls.incrementAndGet() }

        MainModule.mPrefs["controls_backlong_action"] = 2
        SystemUiInstaller.install(lpparam, watchPreferences)

        // The callback should receive the exact Runnable supplied to install().
        executeSystemUiOnCreate()
        assertEquals(1, watchCalls.get())
    }
}
