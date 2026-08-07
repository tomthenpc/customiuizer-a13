package tv.withaibuild.customiuizer.mods

import android.media.AudioSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher
import tv.withaibuild.customiuizer.utils.PrefMap

class AudioServiceHotPathCallbackTest : AudioServiceHotPathTestBase() {

    @Test
    fun volumeStepsHook_modifiesMaxStreamVolume() {
        val prefs = PrefMap<String, Any>()
        prefs["system_volumesteps"] = 150

        val hook = installVolumeSteps(prefs)
        val audioService = createAudioService()
        val callback = fakeBeforeCallback(audioService, methodName = "createStreamStates")
        hook.beforeHook(callback)

        val maxStreamVolume = maxStreamVolume()
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

        val hook = installNotificationVolume(prefs)
        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 5)

        val callback = fakeBeforeCallback(volumeState)
        hook.beforeHook(callback)

        assertFalse("callback should not skip for non-stream-type-1", isSkipped(callback))
        assertTrue("getSettingNameForDevice should not be called", requestedDeviceTypes(volumeState).isEmpty())
    }

    @Test
    fun readSettingsHook_buildsIndexMap_forStreamType1() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true

        val hook = installNotificationVolume(prefs)
        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 1)

        setSettingsOverride(5)
        val callback = fakeBeforeCallback(volumeState)
        hook.beforeHook(callback)

        assertTrue("callback should skip original", isSkipped(callback))
        assertTrue("getSettingNameForDevice should be called for default device", 2 in requestedDeviceTypes(volumeState))
        assertTrue("getValidIndex should be called for all devices", validIndexCalls(volumeState).size >= AudioSystem.DEVICE_OUT_ALL_SET.size)
        assertTrue("validIndex for default device should be 50", validIndexCalls(volumeState).any { it.first == 2 && it.second == 50 })
    }

    @Test
    fun readSettingsHook_keepsIndexMapEmpty_forMissingSettingsValue() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true

        val hook = installNotificationVolume(prefs)
        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 1)

        setSettingsOverride(MISSING_SETTINGS_VALUE)
        val callback = fakeBeforeCallback(volumeState)
        hook.beforeHook(callback)

        assertTrue("callback should skip original", isSkipped(callback))
        assertEquals(1, validIndexCalls(volumeState).size)
        assertEquals(2, validIndexCalls(volumeState)[0].first)
        assertEquals(50, validIndexCalls(volumeState)[0].second)
    }
}
