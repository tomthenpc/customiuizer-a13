package tv.withaibuild.customiuizer.mods

import android.app.PendingIntent
import android.content.Intent
import android.service.notification.StatusBarNotification
import com.android.systemui.Dependency
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.policy.AppMiniWindowManager
import com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import miui.process.ForegroundInfo
import miui.process.ProcessManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class OpenNotifyInFloatingWindowHookTest {

    private val parentClassLoader: ClassLoader
        get() = this.javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        AppMiniWindowManager.getInstance().calls.clear()
        ProcessManager.foregroundInfo = null
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
        AppMiniWindowManager.getInstance().calls.clear()
        ProcessManager.foregroundInfo = null
    }

    private fun lpparam(): XposedModuleInterface.PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(XposedModuleInterface.PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.android.systemui"
                "getProcessName" -> "com.android.systemui"
                "getClassLoader" -> parentClassLoader
                else -> null
            }
        } as XposedModuleInterface.PackageReadyParam
    }

    private fun fakePendingIntent(): PendingIntent {
        return XposedHelpers.newInstance(PendingIntent::class.java, *emptyArray<Any?>()) as PendingIntent
    }

    private fun fakeSbn(): StatusBarNotification {
        return XposedHelpers.newInstance(
            StatusBarNotification::class.java,
            "com.example.app", "com.example.app", 1, null, 1000, 0, 0, null, null, 0L
        ) as StatusBarNotification
    }

    private fun fakeBeforeCallback(executable: Method, args: List<Any?>): HookerClassHelper.BeforeHookCallback {
        val chain = FakeChain(executable, null, args, null, null)
        val constructor = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredConstructor(XposedInterface.Chain::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(chain)
    }

    private fun installHook(prefs: PrefMap<String, Any>) {
        MainModule.mPrefs = prefs
        SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam())
    }

    private fun getRecordedHook(): HookerClassHelper.MethodHook {
        val recorded = FakeXposedInterface.recordedHooks.filter {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter" &&
                it.executable.name == "startNotificationIntent"
        }
        assertTrue("expected hook for startNotificationIntent", recorded.isNotEmpty())
        return recorded.first().hook
    }

    private fun isSkipped(before: HookerClassHelper.BeforeHookCallback): Boolean {
        val field = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredField("skipped")
        field.isAccessible = true
        return field.getBoolean(before)
    }

    @Test
    fun openNotifyInFloatingWindowHook_featureDisabled_doesNotRegister() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = false
        installHook(prefs)
        assertTrue(FakeXposedInterface.recordedHooks.none {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter"
        })
    }

    @Test
    fun openNotifyInFloatingWindowHook_featureEnabled_registersHook() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        installHook(prefs)
        val hook = getRecordedHook()
        assertNotNull(hook)
    }

    @Test
    fun startNotificationIntent_before_nullPendingIntent_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = FakeXposedInterface.recordedHooks.first {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter" &&
                it.executable.name == "startNotificationIntent"
        }.executable as Method

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val before = fakeBeforeCallback(executable, listOf(null, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_nullSbn_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = FakeXposedInterface.recordedHooks.first {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter" &&
                it.executable.name == "startNotificationIntent"
        }.executable as Method

        val entry = NotificationEntry()
        val pendingIntent = fakePendingIntent()
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_foregroundIsSamePackage_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = FakeXposedInterface.recordedHooks.first {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter" &&
                it.executable.name == "startNotificationIntent"
        }.executable as Method

        ProcessManager.foregroundInfo = ForegroundInfo("")

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent()
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_whitelistEnabledWithoutEmptyPackage_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = true
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = FakeXposedInterface.recordedHooks.first {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter" &&
                it.executable.name == "startNotificationIntent"
        }.executable as Method

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent()
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_conditionsMet_launchesMiniWindowAndSkips() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = FakeXposedInterface.recordedHooks.first {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter" &&
                it.executable.name == "startNotificationIntent"
        }.executable as Method

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent()
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("", AppMiniWindowManager.getInstance().calls[0].first)
        assertSame(pendingIntent, AppMiniWindowManager.getInstance().calls[0].second)
    }
}
