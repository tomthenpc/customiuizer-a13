package tv.withaibuild.customiuizer.mods

import android.media.AudioSystem
import android.provider.Settings
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher
import tv.withaibuild.customiuizer.mods.catalog.FeatureInstallRegistry
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

abstract class AudioServiceHotPathTestBase {

    companion object {
        const val MISSING_SETTINGS_VALUE = -2
    }

    protected lateinit var childClassLoader: ChildFirstClassLoader

    @Before
    open fun setUp() {
        FeatureInstallRegistry.clearStatesForTesting()
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        childClassLoader = ChildFirstClassLoader.forTest(this.javaClass.classLoader!!)
        resetAudioServiceMaxStreamVolume()
        setSettingsOverride(MISSING_SETTINGS_VALUE)
        setThrowOnGetIntForUser(null)
    }

    @After
    open fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
        childClassLoader.close()
    }

    protected fun resetAudioServiceMaxStreamVolume() {
        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        val field = audioClass.getField("MAX_STREAM_VOLUME")
        field.set(null, intArrayOf(5, 7, 15, 7, 7, 15, 7, 15, 7, 15))
    }

    protected fun setSettingsOverride(value: Int) {
        val settingsClass = childClassLoader.loadClass("android.provider.Settings\$System")
        val method = settingsClass.getMethod("setOverrideValue", Int::class.javaPrimitiveType)
        method.invoke(null, value)
    }

    protected fun setThrowOnGetIntForUser(t: Throwable?) {
        val settingsClass = childClassLoader.loadClass("android.provider.Settings\$System")
        val method = settingsClass.getMethod("setThrowOnGetIntForUser", Throwable::class.java)
        method.invoke(null, t)
    }

    protected fun runtime(prefs: PrefMap<String, Any>): FeatureRuntime {
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

    protected fun fakeBeforeCallback(thisObject: Any?, args: ArrayList<Any?> = ArrayList(), methodName: String = "readSettings"): HookerClassHelper.BeforeHookCallback {
        val parent = this.javaClass.classLoader!!
        val executable: java.lang.reflect.Executable? = if (thisObject == null) null else {
            thisObject.javaClass.getDeclaredMethod(methodName)
        }
        val chain = Proxy.newProxyInstance(
            parent,
            arrayOf(XposedInterface.Chain::class.java),
            InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getArgs" -> args
                    "getThisObject" -> thisObject
                    "getExecutable" -> executable
                    "proceed" -> null
                    else -> null
                }
            }
        ) as XposedInterface.Chain
        val constructor = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredConstructor(XposedInterface.Chain::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(chain)
    }

    protected fun isSkipped(callback: HookerClassHelper.BeforeHookCallback): Boolean {
        val field = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredField("skipped")
        field.isAccessible = true
        return field.get(callback) as Boolean
    }

    protected fun getRecordedHook(targetClassName: String, targetMethodName: String): HookerClassHelper.MethodHook {
        val recorded = FakeXposedInterface.recordedHooks.filter {
            it.executable.declaringClass.name == targetClassName && it.executable.name == targetMethodName
        }
        assertTrue("expected hook for $targetClassName#$targetMethodName", recorded.isNotEmpty())
        return recorded.last().hook
    }

    protected fun createAudioService(): Any {
        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        return audioClass.getConstructor().newInstance()
    }

    protected fun createVolumeStreamState(audioService: Any, streamType: Int): Any {
        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        val inner = audioClass.declaredClasses.first { it.simpleName == "VolumeStreamState" }
        val volumeState = inner.getConstructor(audioClass).newInstance(audioService)
        val streamTypeField = inner.getDeclaredField("mStreamType")
        streamTypeField.isAccessible = true
        streamTypeField.setInt(volumeState, streamType)
        return volumeState
    }

    protected fun getField(volumeState: Any, name: String): Any? {
        val inner = childClassLoader.loadClass("com.android.server.audio.AudioService\$VolumeStreamState")
        val field = inner.getDeclaredField(name)
        field.isAccessible = true
        return field.get(volumeState)
    }

    protected fun setField(volumeState: Any, name: String, value: Any?) {
        val inner = childClassLoader.loadClass("com.android.server.audio.AudioService\$VolumeStreamState")
        val field = inner.getDeclaredField(name)
        field.isAccessible = true
        field.set(volumeState, value)
    }

    protected fun indexMapSnapshot(volumeState: Any): Map<Int, Int> {
        val inner = childClassLoader.loadClass("com.android.server.audio.AudioService\$VolumeStreamState")
        val field = inner.getDeclaredField("mIndexMap")
        field.isAccessible = true
        val map = field.get(volumeState) as android.util.SparseIntArray
        val result = mutableMapOf<Int, Int>()
        for (i in 0 until map.size()) {
            result[map.keyAt(i)] = map.valueAt(i)
        }
        return result
    }

    protected fun validIndexCalls(volumeState: Any): List<Pair<Int, Int>> {
        @Suppress("UNCHECKED_CAST")
        return getField(volumeState, "validIndexCalls") as List<Pair<Int, Int>>
    }

    protected fun requestedDeviceTypes(volumeState: Any): List<Int> {
        @Suppress("UNCHECKED_CAST")
        return getField(volumeState, "requestedDeviceTypes") as List<Int>
    }

    protected fun installVolumeSteps(prefs: PrefMap<String, Any>): HookerClassHelper.MethodHook {
        val server = runtime(prefs)
        assertTrue(FeatureDispatcher.installById("volumeSteps", server))
        return getRecordedHook("com.android.server.audio.AudioService", "createStreamStates")
    }

    protected fun installNotificationVolume(prefs: PrefMap<String, Any>): HookerClassHelper.MethodHook {
        val server = runtime(prefs)
        assertTrue(FeatureDispatcher.installById("notificationVolume", server))
        return getRecordedHook("com.android.server.audio.AudioService\$VolumeStreamState", "readSettings")
    }

    protected fun rethrowAudioFatalMethod(): Method {
        return SystemAudioAndVolumeHooks::class.java.getDeclaredMethod("rethrowAudioFatal", Throwable::class.java).apply {
            isAccessible = true
        }
    }

    protected fun hooksInstance(): Any {
        val field = SystemAudioAndVolumeHooks::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        return field.get(null)!!
    }

    protected fun assertMapsEqual(expected: Map<Int, Int>, actual: Map<Int, Int>) {
        assertTrue("expected $expected but was $actual", expected == actual)
    }

    protected fun maxStreamVolume(): IntArray {
        val audioClass = childClassLoader.loadClass("com.android.server.audio.AudioService")
        return audioClass.getField("MAX_STREAM_VOLUME").get(null) as IntArray
    }
}
