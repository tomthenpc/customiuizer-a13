package tv.withaibuild.customiuizer.mods

import android.os.Bundle
import com.miui.appmanager.AMAppInfomationActivity
import com.miui.appmanager.AMAppInformationFragment
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

/**
 * B2B-D3 behavioral coverage: one onPreferenceTreeClick interceptor that
 * uses the current fragment's Activity / PackageInfo, not the first Activity.
 */
class AppInfoNestedHookOwnershipTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        ModuleHelper.mModuleContext = null
        Various.mSupportFragment = null
        Various.mLastPackageInfo = null
    }

    @After
    fun tearDown() {
        Various.mSupportFragment = null
        Various.mLastPackageInfo = null
        ModuleHelper.mModuleContext = null
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    @Test
    fun secondActivityClickUsesCurrentPackageInfo_andHookInstallsOnce() {
        Various.AppInfoHook(lpparam())

        val onCreate = requireNotNull(
            FakeXposedInterface.findHook(AM_APP_INFO, "onCreate")
        )

        val activityA = AMAppInfomationActivity()
        val fragA = AMAppInformationFragment()
        fragA.host = activityA
        fragA.mPackageInfo = AMAppInformationFragment.packageInfo(
            "pkg.a",
            "/data/app/a.apk",
            "/data/data/pkg.a"
        )
        Various.mSupportFragment = fragA
        FakeXposedInterface.executeAfter(onCreate, activityA)

        assertEquals(1, clickHookCount())
        assertTrue("first onCreate must still add preference rows", fragA.addedKeys.contains("apk_filename"))

        val activityB = AMAppInfomationActivity()
        val fragB = AMAppInformationFragment()
        fragB.host = activityB
        fragB.mPackageInfo = AMAppInformationFragment.packageInfo(
            "pkg.b",
            "/data/app/b.apk",
            "/data/data/pkg.b"
        )
        Various.mSupportFragment = fragB
        FakeXposedInterface.executeAfter(onCreate, activityB)

        assertEquals("repeat onCreate must not add another click interceptor", 1, clickHookCount())
        assertTrue(fragB.addedKeys.contains("apk_filename"))

        val click = FakeXposedInterface.recordedHooks.find {
            it.executable.declaringClass == AMAppInformationFragment::class.java &&
                it.executable.name == "onPreferenceTreeClick"
        }
        assertNotNull(click)

        FakeXposedInterface.executeBefore(click!!, fragB, FakePreference("open_in_store", "store"))

        assertEquals("Activity A must not receive B's click", 0, activityA.started.size)
        assertEquals(1, activityB.started.size)
        val data = activityB.started[0].getData().toString()
        assertTrue("click must use B's package name, not A's", data.contains("pkg.b"))
        assertFalse(data.contains("pkg.a"))
    }

    private fun clickHookCount(): Int {
        return FakeXposedInterface.recordedHooks.count { it.executable.name == "onPreferenceTreeClick" }
    }

    private fun lpparam(): PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.miui.securitycenter"
                "getProcessName" -> "com.miui.securitycenter"
                "getClassLoader" -> parentClassLoader
                else -> null
            }
        } as PackageReadyParam
    }

    class FakePreference(private val key: String, private val title: String) {
        fun getKey(): String = key
        fun getTitle(): CharSequence = title
    }

    companion object {
        private const val AM_APP_INFO = "com.miui.appmanager.AMAppInfomationActivity"
    }
}
