package tv.withaibuild.customiuizer.mods

import android.content.ContentResolver
import android.media.AudioSystem
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

class AudioServiceHotPathCallbackTest {

    companion object {
        const val MISSING_SETTINGS_VALUE = -2
    }

    private lateinit var childClassLoader: ChildFirstClassLoader

    @Before
    fun setUp() {
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        childClassLoader = ChildFirstClassLoader.forTest(this.javaClass.classLoader!!)
        resetAudioServiceMaxStreamVolume()
        setSettingsOverride(MISSING_SETTINGS_VALUE)
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
        childClassLoader.close()
    }

    private fun resetAudioServiceMaxStreamVolume() {
        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        val field = audioClass.getField("MAX_STREAM_VOLUME")
        field.set(null, intArrayOf(5, 7, 15, 7, 7, 15, 7, 15, 7, 15))
    }

    private fun setSettingsOverride(value: Int) {
        val settingsClass = childClassLoader.loadClass("android.provider.Settings\$System")
        val method = settingsClass.getMethod("setOverrideValue", Int::class.javaPrimitiveType)
        method.invoke(null, value)
    }

    private fun runtime(prefs: PrefMap<String, Any>): FeatureRuntime {
        MainModule.mPrefs = prefs
        val parent = this.javaClass.classLoader!!
        val lpparam = Proxy.newProxyInstance(
            parent,
            arrayOf(XposedModuleInterface.SystemServerStartingParam::class.java),
            InvocationHandler { _, m, _ ->
                when (m.name) {
                    "getClassLoader" -> childClassLoader
                    "getProcessName" -> "android"
                    else -> null
                }
            }
        ) as XposedModuleInterface.SystemServerStartingParam
        return FeatureDispatcher.createRuntime("android", lpparam, childClassLoader, prefs)
    }

    private fun fakeBeforeCallback(thisObject: Any?, args: ArrayList<Any?> = ArrayList()): HookerClassHelper.BeforeHookCallback {
        val parent = this.javaClass.classLoader!!
        val chain = Proxy.newProxyInstance(
            parent,
            arrayOf(XposedInterface.Chain::class.java),
            InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getArgs" -> args
                    "getThisObject" -> thisObject
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

    private fun isSkipped(callback: HookerClassHelper.BeforeHookCallback): Boolean {
        val field = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredField("skipped")
        field.isAccessible = true
        return field.get(callback) as Boolean
    }

    private fun getRecordedHook(targetClassName: String, targetMethodName: String): HookerClassHelper.MethodHook {
        val recorded = FakeXposedInterface.recordedHooks.filter {
            it.executable.declaringClass.name == targetClassName && it.executable.name == targetMethodName
        }
        assertTrue("expected hook for $targetClassName#$targetMethodName", recorded.isNotEmpty())
        return recorded.first().hook
    }

    private fun createAudioService(): Any {
        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        return audioClass.getConstructor().newInstance()
    }

    private fun createVolumeStreamState(audioService: Any, streamType: Int): Any {
        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        val inner = audioClass.declaredClasses.first { it.simpleName == "VolumeStreamState" }
        val volumeState = inner.getConstructor(audioClass).newInstance(audioService)
        val streamTypeField = inner.getDeclaredField("mStreamType")
        streamTypeField.isAccessible = true
        streamTypeField.setInt(volumeState, streamType)
        return volumeState
    }

    private fun getField(volumeState: Any, name: String): Any? {
        val inner = childClassLoader.loadClass("com.android.server.audio.AudioService\$VolumeStreamState")
        val field = inner.getDeclaredField(name)
        field.isAccessible = true
        return field.get(volumeState)
    }

    @Test
    fun volumeStepsHook_modifiesMaxStreamVolume() {
        val prefs = PrefMap<String, Any>()
        prefs["system_volumesteps"] = 150

        val server = runtime(prefs)
        assertTrue(FeatureDispatcher.installById("volumeSteps", server))

        val hook = getRecordedHook("com.android.server.audio.AudioService", "createStreamStates")
        val audioService = createAudioService()
        val callback = fakeBeforeCallback(audioService)
        hook.beforeHook(callback)

        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        val maxStreamVolume = audioClass.getField("MAX_STREAM_VOLUME").get(null) as IntArray
        assertEquals(8, maxStreamVolume[0])
        assertEquals(11, maxStreamVolume[1])
        assertEquals(23, maxStreamVolume[2])
    }

    @Test
    fun volumeStepsHook_doesNotInstallWhenPreferenceIsZero() {
        val prefs = PrefMap<String, Any>()
        prefs["system_volumesteps"] = 0

        val server = runtime(prefs)
        val installed = FeatureDispatcher.installById("volumeSteps", server)
        assertFalse("volumeSteps should not install with zero preference", installed)
    }

    @Test
    fun readSettingsHook_returnsEarly_forNonStreamType1() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true

        val server = runtime(prefs)
        assertTrue(FeatureDispatcher.installById("notificationVolume", server))

        val hook = getRecordedHook("com.android.server.audio.AudioService\$VolumeStreamState", "readSettings")
        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 5)

        val callback = fakeBeforeCallback(volumeState)
        hook.beforeHook(callback)

        assertFalse("callback should not skip for non-stream-type-1", isSkipped(callback))
        @Suppress("UNCHECKED_CAST")
        val requestedDeviceTypes = getField(volumeState, "requestedDeviceTypes") as List<Int>
        assertTrue("getSettingNameForDevice should not be called", requestedDeviceTypes.isEmpty())
    }

    @Test
    fun readSettingsHook_buildsIndexMap_forStreamType1() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true

        val server = runtime(prefs)
        assertTrue(FeatureDispatcher.installById("notificationVolume", server))

        val hook = getRecordedHook("com.android.server.audio.AudioService\$VolumeStreamState", "readSettings")
        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 1)

        setSettingsOverride(5)
        val callback = fakeBeforeCallback(volumeState)
        hook.beforeHook(callback)

        assertTrue("callback should skip original", isSkipped(callback))
        @Suppress("UNCHECKED_CAST")
        val requestedDeviceTypes = getField(volumeState, "requestedDeviceTypes") as List<Int>
        @Suppress("UNCHECKED_CAST")
        val validIndexCalls = getField(volumeState, "validIndexCalls") as List<Pair<Int, Int>>
        assertTrue("getSettingNameForDevice should be called for default device", 2 in requestedDeviceTypes)
        assertTrue("getValidIndex should be called for all devices", validIndexCalls.size >= AudioSystem.DEVICE_OUT_ALL_SET.size)
        assertTrue("validIndex for default device should be 50", validIndexCalls.any { it.first == 2 && it.second == 50 })
    }

    @Test
    fun readSettingsHook_keepsIndexMapEmpty_forMissingSettingsValue() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true

        val server = runtime(prefs)
        assertTrue(FeatureDispatcher.installById("notificationVolume", server))

        val hook = getRecordedHook("com.android.server.audio.AudioService\$VolumeStreamState", "readSettings")
        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 1)

        setSettingsOverride(MISSING_SETTINGS_VALUE)
        val callback = fakeBeforeCallback(volumeState)
        hook.beforeHook(callback)

        assertTrue("callback should skip original", isSkipped(callback))
        @Suppress("UNCHECKED_CAST")
        val validIndexCalls = getField(volumeState, "validIndexCalls") as List<Pair<Int, Int>>
        assertEquals(1, validIndexCalls.size)
        assertEquals(2, validIndexCalls[0].first)
        assertEquals(50, validIndexCalls[0].second)
    }
}
