package tv.withaibuild.customiuizer.mods

import android.media.AudioManager
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher
import tv.withaibuild.customiuizer.mods.catalog.FeatureInstallRegistry
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Real callback tests for NoCallInterruptionHook.
 */
class NoCallInterruptionCallbackTest {

    @Before
    fun setUp() {
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    private fun runtime(prefs: PrefMap<String, Any>): FeatureRuntime {
        MainModule.mPrefs = prefs
        val classLoader = this.javaClass.classLoader!!
        val lpparam = Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedModuleInterface.SystemServerStartingParam::class.java),
            InvocationHandler { _, m, _ ->
                when (m.name) {
                    "getClassLoader" -> classLoader
                    "getProcessName" -> "android"
                    else -> null
                }
            }
        ) as XposedModuleInterface.SystemServerStartingParam
        return FeatureDispatcher.createRuntime("android", lpparam, classLoader, prefs)
    }

    private fun installAndFindHook(prefs: PrefMap<String, Any>): Pair<HookerClassHelper.MethodHook, List<FakeXposedInterface.RecordedHook>> {
        val server = runtime(prefs)
        assert(FeatureDispatcher.installById("noCallInterruption", server)) { "install should succeed" }
        val recorded = FakeXposedInterface.recordedHooks.filter { it.executable.declaringClass.name == "com.android.server.audio.AudioService" }
        assert(recorded.isNotEmpty()) { "at least one AudioService hook expected" }
        val hook = recorded.first().hook
        return hook to recorded
    }

    private fun fakeBeforeCallback(args: ArrayList<Any?>): HookerClassHelper.BeforeHookCallback {
        val classLoader = this.javaClass.classLoader!!
        val chain = Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedInterface.Chain::class.java),
            InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getArgs" -> args
                    "getThisObject" -> null
                    "getExecutable" -> null
                    "proceed" -> null
                    else -> null
                }
            }
        ) as XposedInterface.Chain
        val constructor = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredConstructor(XposedInterface.Chain::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(chain)
    }

    private fun fakeAfterCallback(args: ArrayList<Any?>, result: Any?): HookerClassHelper.AfterHookCallback {
        val before = fakeBeforeCallback(args)
        val constructor = HookerClassHelper.AfterHookCallback::class.java.getDeclaredConstructor(
            HookerClassHelper.BeforeHookCallback::class.java,
            Object::class.java,
            Throwable::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(before, result, null)
    }

    private fun isSkipped(callback: HookerClassHelper.BeforeHookCallback): Boolean {
        val field = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredField("skipped")
        field.isAccessible = true
        return field.get(callback) as Boolean
    }

    private fun getResult(callback: HookerClassHelper.BeforeHookCallback): Any? {
        val field = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredField("result")
        field.isAccessible = true
        return field.get(callback)
    }

    private fun resetAudioFocusPkg() {
        val field = SystemAudioAndVisualAndMoreHooks::class.java.getDeclaredField("audioFocusPkg")
        field.isAccessible = true
        field.set(null, null)
    }

    @Test
    fun shortArgsBeforeDoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_ignorecalls"] = true
        prefs["system_ignorecalls_apps"] = setOf("com.caller")
        resetAudioFocusPkg()
        val (hook, _) = installAndFindHook(prefs)
        val callback = fakeBeforeCallback(ArrayList())
        hook.beforeHook(callback)
        assertFalse("empty args do not skip", isSkipped(callback))
    }

    @Test
    fun shortArgsAfterDoNotCrash() {
        val prefs = PrefMap<String, Any>()
        prefs["system_ignorecalls"] = true
        prefs["system_ignorecalls_apps"] = setOf("com.caller")
        resetAudioFocusPkg()
        val (hook, _) = installAndFindHook(prefs)
        val callback = fakeAfterCallback(ArrayList(), AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        hook.afterHook(callback)
        assertEquals("empty args do not update package", null, currentAudioFocusPkg())
    }

    @Test
    fun afterRemembersPackageWhenGranted() {
        val prefs = PrefMap<String, Any>()
        prefs["system_ignorecalls"] = true
        prefs["system_ignorecalls_apps"] = setOf("com.caller")
        resetAudioFocusPkg()
        val (hook, _) = installAndFindHook(prefs)
        val args = arrayListOf<Any?>(Any(), Any(), 0, 0, "SomeClient", "com.caller")
        val callback = fakeAfterCallback(args, AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        hook.afterHook(callback)
        assertEquals("granted request updates audio focus package", "com.caller", currentAudioFocusPkg())
    }

    @Test
    fun beforeSkipsIgnoredCaller() {
        val prefs = PrefMap<String, Any>()
        prefs["system_ignorecalls"] = true
        prefs["system_ignorecalls_apps"] = setOf("com.caller")
        resetAudioFocusPkg()
        val (hook, _) = installAndFindHook(prefs)

        // seed audioFocusPkg via after hook
        val afterArgs = arrayListOf<Any?>(Any(), Any(), 0, 0, "SomeClient", "com.caller")
        val afterCallback = fakeAfterCallback(afterArgs, AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        hook.afterHook(afterCallback)

        val beforeArgs = arrayListOf<Any?>(Any(), Any(), 0, 0, "AudioFocus_For_Phone_Ring_And_Calls", "com.caller")
        val beforeCallback = fakeBeforeCallback(beforeArgs)
        hook.beforeHook(beforeCallback)
        assertTrue("ignored caller should skip", isSkipped(beforeCallback))
        assertEquals("skipped result is AUDIOFOCUS_REQUEST_GRANTED", 1, getResult(beforeCallback))
    }

    @Test
    fun beforeDoesNotSkipDifferentClient() {
        val prefs = PrefMap<String, Any>()
        prefs["system_ignorecalls"] = true
        prefs["system_ignorecalls_apps"] = setOf("com.caller")
        resetAudioFocusPkg()
        val (hook, _) = installAndFindHook(prefs)

        val afterArgs = arrayListOf<Any?>(Any(), Any(), 0, 0, "SomeClient", "com.caller")
        val afterCallback = fakeAfterCallback(afterArgs, AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        hook.afterHook(afterCallback)

        val beforeArgs = arrayListOf<Any?>(Any(), Any(), 0, 0, "OtherClient", "com.caller")
        val beforeCallback = fakeBeforeCallback(beforeArgs)
        hook.beforeHook(beforeCallback)
        assertFalse("different client should not skip", isSkipped(beforeCallback))
    }

    private fun currentAudioFocusPkg(): String? {
        val field = SystemAudioAndVisualAndMoreHooks::class.java.getDeclaredField("audioFocusPkg")
        field.isAccessible = true
        return field.get(null) as? String
    }
}
