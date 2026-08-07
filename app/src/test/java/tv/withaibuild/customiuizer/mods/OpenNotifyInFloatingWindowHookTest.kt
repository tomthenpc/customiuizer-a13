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
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files

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

    private fun installHook(prefs: PrefMap<String, Any>, classLoader: ClassLoader) {
        MainModule.mPrefs = prefs
        val lpparam = Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(XposedModuleInterface.PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.android.systemui"
                "getProcessName" -> "com.android.systemui"
                "getClassLoader" -> classLoader
                else -> null
            }
        } as XposedModuleInterface.PackageReadyParam
        SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam)
    }

    private fun classLoaderWithFakeStatusBarNotification(
        hasIsSubstituteMethod: Boolean,
        hasMPkgNameField: Boolean,
        staticIsSubstitute: Boolean = false
    ): ClassLoader {
        val javaHome = File(System.getProperty("java.home") ?: "")
        val javac = listOfNotNull(
            javaHome.parentFile?.resolve("bin/javac.exe"),
            javaHome.resolve("bin/javac.exe"),
            javaHome.parentFile?.resolve("bin/javac"),
            javaHome.resolve("bin/javac")
        ).firstOrNull { it.exists() } ?: throw IllegalStateException(
            "javac not found near java.home=${javaHome.absolutePath}; cannot compile fake StatusBarNotification"
        )

        val tempDir = Files.createTempDirectory("fake-statusbar").toFile()
        tempDir.deleteOnExit()

        val pkgDir = File(tempDir, "android/service/notification").apply { mkdirs() }
        val source = File(pkgDir, "StatusBarNotification.java")
        val method = if (hasIsSubstituteMethod) {
            if (staticIsSubstitute) "public static boolean isSubstituteNotification() { return true; }" else "public boolean isSubstituteNotification() { return true; }"
        } else ""
        val field = if (hasMPkgNameField) "public String mPkgName;" else ""
        source.writeText(
            """
            package android.service.notification;
            public class StatusBarNotification {
                $field
                $method
            }
            """.trimIndent()
        )

        val process = ProcessBuilder(
            javac.absolutePath,
            "-d", tempDir.absolutePath,
            source.absolutePath
        ).apply { environment()["JAVA_HOME"] = javaHome.absolutePath }
            .start()
        val output = process.inputStream.bufferedReader().readText() + process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) throw IllegalStateException("failed to compile fake StatusBarNotification: $output")

        val fakeOnlyLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()), null)
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == "android.service.notification.StatusBarNotification") {
                    val c = fakeOnlyLoader.loadClass(name)
                    if (resolve) resolveClass(c)
                    return c
                }
                return super.loadClass(name, resolve)
            }
        }
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

    private fun getThrowable(before: HookerClassHelper.BeforeHookCallback): Throwable? {
        val field = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredField("throwable")
        field.isAccessible = true
        return field.get(before) as? Throwable
    }

    private fun invokeBeforeDirectly(hook: HookerClassHelper.MethodHook, before: HookerClassHelper.BeforeHookCallback) {
        val method = HookerClassHelper.MethodHook::class.java.getDeclaredMethod(
            "before",
            HookerClassHelper.BeforeHookCallback::class.java
        )
        method.isAccessible = true
        try {
            method.invoke(hook, before)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
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
    fun startNotificationIntent_before_launchOrdinaryFailure_propagatesInvocationTargetError() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        AppMiniWindowManager.getInstance().throwOnCall = RuntimeException("launch failed")
        val chain = FakeChain(executable, null, listOf(pendingIntent, null, entry, null, false, false), null, null)

        try {
            hook.intercept(chain)
            fail("expected XposedHelpers.InvocationTargetError to propagate")
        } catch (e: XposedHelpers.InvocationTargetError) {
            assertNotNull(e.cause)
            assertEquals("launch failed", (e.cause as? RuntimeException)?.message)
        }

        assertFalse("original method must not be called after launch failure", chain.proceeded)
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
    fun startNotificationIntent_before_launchSideEffectThenThrow_propagatesInvocationTargetError() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        AppMiniWindowManager.getInstance().sideEffectThenThrow = RuntimeException("after side effect")
        val chain = FakeChain(executable, null, listOf(pendingIntent, null, entry, null, false, false), null, null)

        try {
            hook.intercept(chain)
            fail("expected XposedHelpers.InvocationTargetError to propagate")
        } catch (e: XposedHelpers.InvocationTargetError) {
            assertNotNull(e.cause)
            assertEquals("after side effect", (e.cause as? RuntimeException)?.message)
        }

        assertFalse("original method must not be called after launch failure", chain.proceeded)
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
    }

    @Test
    fun startNotificationIntent_before_processManagerOrdinaryException_propagatesToMethodHookBoundary() {
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

        assertThrows(
            "ProcessManager.getForegroundInfo() is not wrapped; ordinary exceptions propagate out of before()",
            RuntimeException::class.java
        ) {
            invokeBeforeDirectly(hook, before)
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

    @Test
    fun startNotificationIntent_before_isSubstituteOrdinaryFailure_failOpen() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val sbn = fakeSbn().apply { throwOnIsSubstitute = RuntimeException("isSubstitute failed") }
        val entry = NotificationEntry().apply { mSbn = sbn }
        val pendingIntent = fakePendingIntent("com.target.app")
        val chain = FakeChain(executable, null, listOf(pendingIntent, null, entry, null, false, false), null, null)

        hook.intercept(chain)

        assertTrue("pre-side-effect ordinary failure must fail-open to original", chain.proceeded)
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test(expected = OutOfMemoryError::class)
    fun startNotificationIntent_before_isSubstituteNestedFatal_rethrowsFatal() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val sbn = fakeSbn().apply { throwOnIsSubstitute = RuntimeException(RuntimeException(OutOfMemoryError("oom"))) }
        val entry = NotificationEntry().apply { mSbn = sbn }
        val pendingIntent = fakePendingIntent("com.target.app")
        val chain = FakeChain(executable, null, listOf(pendingIntent, null, entry, null, false, false), null, null)

        hook.intercept(chain)
    }

    @Test
    fun startNotificationIntent_before_mPkgNameNullValue_launchesWithEmptyPackageName() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val sbn = fakeSubstituteSbn().apply { mPkgName = null }
        val entry = NotificationEntry().apply { mSbn = sbn }
        val pendingIntent = fakePendingIntent("com.creator.app")
        val before = fakeBeforeCallback(executable, listOf(pendingIntent, null, entry, null, false, false))
        hook.beforeHook(before)

        assertTrue(isSkipped(before))
        assertEquals(1, AppMiniWindowManager.getInstance().calls.size)
        assertEquals("legacy empty mPkgName must be used when value is null", "", AppMiniWindowManager.getInstance().calls[0].first)
    }

    @Test
    fun startNotificationIntent_before_mPkgNameReadOrdinaryFailure_failOpen() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        val classLoader = classLoaderWithFakeStatusBarNotification(
            hasIsSubstituteMethod = true,
            hasMPkgNameField = true,
            staticIsSubstitute = true
        )
        installHook(prefs, classLoader)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = "not-a-statusbar-instance" }
        val pendingIntent = fakePendingIntent("com.target.app")
        val chain = FakeChain(executable, null, listOf(pendingIntent, null, entry, null, false, false), null, null)

        hook.intercept(chain)

        assertTrue("mPkgName read ordinary failure must fail-open to original", chain.proceeded)
        assertTrue(AppMiniWindowManager.getInstance().calls.isEmpty())
    }

    @Test
    fun startNotificationIntent_before_install_isSubstituteMethodMissing_zeroHooks() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        val classLoader = classLoaderWithFakeStatusBarNotification(
            hasIsSubstituteMethod = false,
            hasMPkgNameField = true
        )
        installHook(prefs, classLoader)
        assertTrue(
            "missing isSubstituteNotification must prevent hook installation",
            FakeXposedInterface.recordedHooks.none {
                it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter"
            }
        )
    }

    @Test
    fun startNotificationIntent_before_install_mPkgNameFieldMissing_zeroHooks() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        val classLoader = classLoaderWithFakeStatusBarNotification(
            hasIsSubstituteMethod = true,
            hasMPkgNameField = false
        )
        installHook(prefs, classLoader)
        assertTrue(
            "missing mPkgName field must prevent hook installation",
            FakeXposedInterface.recordedHooks.none {
                it.executable.declaringClass.name == "com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter"
            }
        )
    }

    @Test(expected = OutOfMemoryError::class)
    fun startNotificationIntent_before_launchNestedWrappedFatal_rethrowsFatal() {
        val prefs = PrefMap<String, Any>()
        prefs["system_notify_openinfw"] = true
        prefs["system_notify_openinfw_in_whitelist"] = false
        installHook(prefs)
        val hook = getRecordedHook()

        val executable = getRecordedExecutable()

        val entry = NotificationEntry().apply { mSbn = fakeSbn() }
        val pendingIntent = fakePendingIntent("com.target.app")
        AppMiniWindowManager.getInstance().throwOnCall = RuntimeException(RuntimeException(OutOfMemoryError("oom")))
        val chain = FakeChain(executable, null, listOf(pendingIntent, null, entry, null, false, false), null, null)

        hook.intercept(chain)
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
