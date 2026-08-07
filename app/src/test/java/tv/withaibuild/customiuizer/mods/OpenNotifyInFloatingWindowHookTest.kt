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
        AppMiniWindowManager.getInstance().reset()
        Dependency.clear()
        ProcessManager.reset()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
        AppMiniWindowManager.getInstance().reset()
        Dependency.clear()
        ProcessManager.reset()
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

    private fun fakePendingIntent(creatorPackage: String? = null): PendingIntent {
        return PendingIntent(creatorPackage)
    }

    private fun fakeSbn(): StatusBarNotification {
        return XposedHelpers.newInstance(
            StatusBarNotification::class.java,
            "com.example.app", "com.example.app", 1, null, 1000, 0, 0, null, null, 0L
        ) as StatusBarNotification
    }

    private fun fakeSubstituteSbn(mPkgName: String = "com.real.notification.owner", packageName: String = "android.system.package"): StatusBarNotification {
        val sbn = XposedHelpers.newInstance(
            StatusBarNotification::class.java,
            packageName, packageName, 1, null, 1000, 0, 0, null, null, 0L
        ) as StatusBarNotification
        sbn.mPkgName = mPkgName
        sbn.mPackageName = packageName
        sbn.isSubstitute = true
        return sbn
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

    private fun getRecordedHook(parameterCount: Int = 6, arg2Class: Class<*>? = null): HookerClassHelper.MethodHook {
        val recorded = getRecordedEntries(parameterCount, arg2Class)
        assertTrue("expected hook for startNotificationIntent", recorded.isNotEmpty())
        return recorded.first().hook
    }

    private fun getRecordedExecutable(parameterCount: Int = 6, arg2Class: Class<*>? = null): Method {
        val recorded = getRecordedEntries(parameterCount, arg2Class)
        assertTrue("expected hook for startNotificationIntent", recorded.isNotEmpty())
        return recorded.first().executable as Method
    }

    private fun getRecordedEntries(parameterCount: Int, arg2Class: Class<*>?): List<FakeXposedInterface.RecordedHook> {
        return FakeXposedInterface.recordedHooks.filter {
            it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter" &&
                it.executable.name == "startNotificationIntent" &&
                it.executable.parameterTypes.size == parameterCount &&
                (arg2Class == null || it.executable.parameterTypes[2] == arg2Class)
        }
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

        val executable = getRecordedExecutable()

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

        val executable = getRecordedExecutable()

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

        val executable = getRecordedExecutable()

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

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent()
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_substituteUsesLegacyMPkgName_notGetPackageName() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSubstituteSbn() }
        val pendingIntent = fakePendingIntent()
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue("should skip after launch", isSkipped(before))
        assertEquals("launch should be called once", 1, AppMiniWindowManager.getInstance().calls.size)
        val launchedPkg = AppMiniWindowManager.getInstance().calls.first().first
        assertEquals("substitute pkg must be legacy mPkgName", "com.real.notification.owner", launchedPkg)
        assertNotEquals("substitute pkg must not be public getPackageName()", "android.system.package", launchedPkg)
    }

    @Test
    fun startNotificationIntent_before_conditionsMet_launchesMiniWindowAndSkips() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent()
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("", AppMiniWindowManager.getInstance().calls[0].first)
        assertSame(pendingIntent, AppMiniWindowManager.getInstance().calls[0].second)
    }

    @Test
    fun startNotificationIntent_before_nonSubstituteUsesCreatorPackage() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.creator.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("com.creator.app", AppMiniWindowManager.getInstance().calls[0].first)
        assertSame(pendingIntent, AppMiniWindowManager.getInstance().calls[0].second)
    }

    @Test
    fun startNotificationIntent_before_foregroundIsMiuiHome_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()
        ProcessManager.foregroundInfo = ForegroundInfo("com.miui.home")

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.foreign.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_foregroundIsDifferentPackage_launches() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()
        ProcessManager.foregroundInfo = ForegroundInfo("com.other.app")

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("com.target.app", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun startNotificationIntent_before_whitelistListedApp_launches() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = true
        prefs["system_notify_openinfw_apps"] = setOf("com.allowed.app")
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.allowed.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("com.allowed.app", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun startNotificationIntent_before_whitelistUnlistedApp_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = true
        prefs["system_notify_openinfw_apps"] = setOf("com.allowed.app")
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.blocked.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_blacklistListedApp_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        prefs["system_notify_openinfw_apps"] = setOf("com.blocked.app")
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.blocked.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_blacklistUnlistedApp_launches() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        prefs["system_notify_openinfw_apps"] = setOf("com.blocked.app")
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.allowed.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("com.allowed.app", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun startNotificationIntent_before_dependencyGetNull_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        Dependency.setMock(com.android.systemui.statusbar.notification.policy.AppMiniWindowManager::class.java, null)
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_dependencyGetOrdinaryFailure_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        Dependency.throwOnGet = RuntimeException("dependency failed")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test(expected = OutOfMemoryError::class)
    fun startNotificationIntent_before_dependencyGetWrappedOOM_rethrowsFatal() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        Dependency.throwOnGet = RuntimeException(OutOfMemoryError("oom"))
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)
    }

    @Test(expected = ThreadDeath::class)
    fun startNotificationIntent_before_dependencyGetWrappedThreadDeath_rethrowsFatal() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        Dependency.throwOnGet = RuntimeException(ThreadDeath())
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)
    }

    @Test(expected = VirtualMachineError::class)
    fun startNotificationIntent_before_dependencyGetWrappedVMError_rethrowsFatal() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        Dependency.throwOnGet = RuntimeException(StackOverflowError("so"))
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)
    }

    @Test
    fun startNotificationIntent_before_launchOrdinaryFailure_doesNotSkip() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        AppMiniWindowManager.getInstance().throwOnCall = RuntimeException("launch failed")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test(expected = OutOfMemoryError::class)
    fun startNotificationIntent_before_launchWrappedOOM_rethrowsFatal() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        AppMiniWindowManager.getInstance().throwOnCall = RuntimeException(OutOfMemoryError("oom"))
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)
    }

    @Test
    fun startNotificationIntent_before_launchSideEffectThenThrow_observesSideEffectAndDoesNotSkip() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        AppMiniWindowManager.getInstance().sideEffectThenThrow = RuntimeException("after side effect")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
    }

    @Test
    fun startNotificationIntent_before_processManagerOrdinaryException_doesNotSkipAndDoesNotLaunch() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        ProcessManager.throwOnGet = RuntimeException("foreground unavailable")
        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))

        // ProcessManager.getForegroundInfo() is not wrapped; exception propagates to MethodHook boundary
        // and is logged; the before callback does not complete.
        try {
            hook.beforeHook(before)
        } catch (t: Throwable) {
            rethrowIfFatal(t)
        }

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_expandableNotificationRowOverload_mapsOwnMSbnField() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook(parameterCount = 4, arg2Class = com.android.systemui.statusbar.notification.row.ExpandableNotificationRow::class.java)

        val executable = getRecordedExecutable(parameterCount = 4, arg2Class = com.android.systemui.statusbar.notification.row.ExpandableNotificationRow::class.java)

        val row = com.android.systemui.statusbar.notification.row.ExpandableNotificationRow().apply {
            mSbn = fakeSbn()
        }
        val pendingIntent = fakePendingIntent("com.target.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, row, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("com.target.app", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun startNotificationIntent_before_expandableNotificationRowOverload_substituteUsesMPkgName() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook(parameterCount = 4, arg2Class = com.android.systemui.statusbar.notification.row.ExpandableNotificationRow::class.java)

        val executable = getRecordedExecutable(parameterCount = 4, arg2Class = com.android.systemui.statusbar.notification.row.ExpandableNotificationRow::class.java)

        val row = com.android.systemui.statusbar.notification.row.ExpandableNotificationRow().apply {
            mSbn = fakeSubstituteSbn()
        }
        val pendingIntent = fakePendingIntent("com.creator.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, row, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("com.real.notification.owner", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun startNotificationIntent_before_arg2Null_returns() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val pendingIntent = fakePendingIntent("com.target.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, null, null, false, false))
        hook.beforeHook(before)

        assertFalse(isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_unknownOverloadWithoutMSbn_failsOpenWithoutSkip() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)

        val unknownMethod = MiuiStatusBarNotificationActivityStarter::class.java.getDeclaredMethod(
            "startNotificationIntent",
            PendingIntent::class.java,
            Intent::class.java,
            String::class.java,
            Boolean::class.java
        )
        val recorded = FakeXposedInterface.recordedHooks.find {
            it.executable == unknownMethod
        }
        assertNotNull("unknown overload is still hooked", recorded)

        val hook = recorded!!.hook
        val pendingIntent = fakePendingIntent("com.target.app")
        val before = fakeBeforeCallback(unknownMethod, listOf(pendingIntent, null, "not-an-entry", false))
        hook.beforeHook(before)

        assertFalse("unknown overload should fail-open (not skip)", isSkipped(before))
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    private fun rethrowIfFatal(t: Throwable) {
        var current: Throwable? = t
        var depth = 0
        while (current != null && depth < 8) {
            if (current is OutOfMemoryError) throw current
            if (current is ThreadDeath) throw current
            if (current is VirtualMachineError) throw current
            val next = current.cause
            if (next == current) return
            current = next
            depth++
        }
    }
}
