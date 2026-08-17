package tv.withaibuild.customiuizer.mods

import android.os.Bundle
import com.miui.appmanager.AppManagerMainActivity
import com.miui.appmanager.AppManagerMainActivityWithFragment
import com.miui.appmanager.AppsManagerFragment
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.installers.SecurityCenterInstaller
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

/**
 * B2B-D1 / D2 / D4 behavioral coverage for SecurityCenterInstaller paths.
 */
class SecurityCenterB2BCorrectiveTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        AppsManagerFragment.getContextFailure = null
    }

    @After
    fun tearDown() {
        AppsManagerFragment.getContextFailure = null
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    @Test
    fun appsRestrictMissing_doesNotBlockIndependentRestrictHooks_orLaterInstallerFeature() {
        MainModule.mPrefs["various_restrictapp"] = true
        MainModule.mPrefs["various_privacyapps_column_nums4"] = true

        SecurityCenterInstaller.install(lpparam(hidingClassLoader(APP_MANAGE_UTILS)))

        assertTrue(
            "ShowAppDetailFragment must still install when AppManageUtils is missing",
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == SHOW_APP_DETAIL &&
                    it.executable.name == "initFirewallData"
            }
        )
        assertTrue(
            "FirewallService must still install when AppManageUtils is missing",
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == FIREWALL_SERVICE &&
                    it.executable.name == "setSystemAppWifiRuleAllow"
            }
        )
        assertTrue(
            "later PrivacyAppsLayoutHook must still install",
            privacyOnCreateInstalled()
        )
        assertTrue(
            "missing AppManageUtils must not be hooked",
            FakeXposedInterface.recordedHooks.none {
                it.executable.declaringClass.name == APP_MANAGE_UTILS
            }
        )
    }

    @Test
    fun interceptPermMissing_doesNotBlockLaterInstallerFeature() {
        MainModule.mPrefs["various_skip_interceptperm"] = true
        MainModule.mPrefs["various_privacyapps_column_nums4"] = true

        SecurityCenterInstaller.install(lpparam(hidingClassLoader(INTERCEPT_BASE_FRAGMENT)))

        assertTrue(privacyOnCreateInstalled())
        assertTrue(
            FakeXposedInterface.recordedHooks.none {
                it.executable.declaringClass.name == INTERCEPT_BASE_FRAGMENT
            }
        )
    }

    @Test
    fun batteryFragmentMissing_doesNotBlockLaterInstallerFeature() {
        MainModule.mPrefs["various_show_battery_temperature"] = true
        MainModule.mPrefs["various_privacyapps_column_nums4"] = true

        SecurityCenterInstaller.install(lpparam(hidingClassLoader(BATTERY_FRAGMENT)))

        assertTrue(privacyOnCreateInstalled())
        assertTrue(
            FakeXposedInterface.recordedHooks.none {
                it.executable.declaringClass.name == BATTERY_FRAGMENT
            }
        )
    }

    @Test
    fun appsortOnly_installsAppsDefaultSortOnce() {
        MainModule.mPrefs["various_appsort"] = "2"
        SecurityCenterInstaller.install(lpparam(parentClassLoader))
        assertEquals(1, appManagerOnCreateCount())
        assertEquals(0, onActivityCreatedCount())
    }

    @Test
    fun skipOnly_stillInstallsAppsDefaultSort() {
        MainModule.mPrefs["various_skip"] = "2"
        SecurityCenterInstaller.install(lpparam(parentClassLoader))
        assertEquals(1, appManagerOnCreateCount())
    }

    @Test
    fun appsortAndSkip_stillInstallsAppsDefaultSortOnce() {
        MainModule.mPrefs["various_appsort"] = "3"
        MainModule.mPrefs["various_skip"] = "2"
        SecurityCenterInstaller.install(lpparam(parentClassLoader))
        assertEquals(1, appManagerOnCreateCount())
    }

    @Test
    fun packageReadyWithoutFragmentField_doesNotInstallOnActivityCreated() {
        Various.AppsDefaultSortHook(lpparam(parentClassLoader))
        assertEquals(1, appManagerOnCreateCount())
        assertEquals(0, onActivityCreatedCount())
    }

    @Test
    fun firstOnCreateWithoutFragment_doesNotPermanentSucceed_thenSubclassInstallsOnce() {
        Various.AppsDefaultSortHook(lpparam(parentClassLoader))
        val onCreate = requireNotNull(FakeXposedInterface.findHook(APP_MANAGER_MAIN, "onCreate"))

        FakeXposedInterface.executeBefore(onCreate, AppManagerMainActivity(), Bundle())
        assertEquals("missing fragment field must not install onActivityCreated", 0, onActivityCreatedCount())

        FakeXposedInterface.executeBefore(onCreate, AppManagerMainActivityWithFragment(), Bundle())
        assertEquals(1, onActivityCreatedCount())

        FakeXposedInterface.executeBefore(onCreate, AppManagerMainActivityWithFragment(), Bundle())
        assertEquals("repeat onCreate must not add another onActivityCreated interceptor", 1, onActivityCreatedCount())
    }

    @Test
    fun checkBundle_writesSortKeys_andNullContextReturnsNull() {
        assertNull(Various.checkBundle(null, Bundle()))

        MainModule.mPrefs["various_appsort"] = "3"
        val out = Various.checkBundle(FakeContext(), Bundle())
        assertNotNull(out)
        assertEquals(2, out!!.getInt("current_sory_type"))
        assertEquals(2, out.getInt("current_sort_type"))
    }

    @Test
    fun onActivityCreated_wrappedOomPropagatesOriginal() {
        val failure = OutOfMemoryError("sort wrapped oom")
        assertSame(failure, thrownFatal { runOnActivityCreatedCatch(RuntimeException(failure)) })
    }

    @Test
    fun onActivityCreated_directThreadDeathPropagates() {
        val failure = ThreadDeath()
        assertSame(failure, thrownFatal { runOnActivityCreatedCatch(failure) })
    }

    @Test
    fun onActivityCreated_directInternalErrorPropagates() {
        val failure = InternalError("sort vm")
        assertSame(failure, thrownFatal { runOnActivityCreatedCatch(failure) })
    }

    @Test
    fun onActivityCreated_ordinaryFailureFailOpen() {
        runOnActivityCreatedCatch(IllegalStateException("ordinary getContext"))
    }

    private fun runOnActivityCreatedCatch(failure: Throwable) {
        Various.AppsDefaultSortHook(lpparam(parentClassLoader))
        val onCreate = requireNotNull(FakeXposedInterface.findHook(APP_MANAGER_MAIN, "onCreate"))
        FakeXposedInterface.executeBefore(onCreate, AppManagerMainActivityWithFragment(), Bundle())
        val nested = FakeXposedInterface.recordedHooks.find {
            it.executable.declaringClass.name == APPS_MANAGER_FRAGMENT &&
                it.executable.name == "onActivityCreated"
        }
        assertNotNull(nested)
        AppsManagerFragment.getContextFailure = failure
        try {
            FakeXposedInterface.executeBefore(nested!!, AppsManagerFragment(), Bundle())
        } finally {
            AppsManagerFragment.getContextFailure = null
        }
    }

    private fun privacyOnCreateInstalled(): Boolean {
        return FakeXposedInterface.recordedHooks.any {
            it.executable.declaringClass.name == PRIVACY_APPS &&
                it.executable.name == "onCreate"
        }
    }

    private fun appManagerOnCreateCount(): Int {
        return FakeXposedInterface.recordedHooks.count {
            it.executable.declaringClass.name == APP_MANAGER_MAIN &&
                it.executable.name == "onCreate"
        }
    }

    private fun onActivityCreatedCount(): Int {
        return FakeXposedInterface.recordedHooks.count { it.executable.name == "onActivityCreated" }
    }

    private fun lpparam(classLoader: ClassLoader): PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.miui.securitycenter"
                "getProcessName" -> "com.miui.securitycenter"
                "getClassLoader" -> classLoader
                else -> null
            }
        } as PackageReadyParam
    }

    private fun hidingClassLoader(hidden: String): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == hidden) {
                    throw ClassNotFoundException(name)
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    private inline fun thrownFatal(block: () -> Unit): Throwable {
        try {
            block()
            fail("expected fatal throwable")
            throw AssertionError("unreachable")
        } catch (oom: OutOfMemoryError) {
            return oom
        } catch (td: ThreadDeath) {
            return td
        } catch (vm: VirtualMachineError) {
            return vm
        }
    }

    companion object {
        private const val APP_MANAGE_UTILS = "com.miui.appmanager.AppManageUtils"
        private const val SHOW_APP_DETAIL =
            "com.miui.networkassistant.ui.fragment.ShowAppDetailFragment"
        private const val FIREWALL_SERVICE =
            "com.miui.networkassistant.service.FirewallService"
        private const val INTERCEPT_BASE_FRAGMENT =
            "com.miui.permcenter.privacymanager.InterceptBaseFragment"
        private const val BATTERY_FRAGMENT = "com.miui.powercenter.BatteryFragment"
        private const val PRIVACY_APPS = "com.miui.privacyapps.ui.PrivacyAppsActivity"
        private const val APP_MANAGER_MAIN = "com.miui.appmanager.AppManagerMainActivity"
        private const val APPS_MANAGER_FRAGMENT = "com.miui.appmanager.AppsManagerFragment"
    }
}
