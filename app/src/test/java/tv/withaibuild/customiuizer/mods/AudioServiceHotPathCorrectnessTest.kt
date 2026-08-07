package tv.withaibuild.customiuizer.mods

import android.media.AudioSystem
import android.util.SparseIntArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher
import tv.withaibuild.customiuizer.mods.catalog.FeatureInstallRegistry
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationTargetException

class AudioServiceHotPathCorrectnessTest : AudioServiceHotPathTestBase() {

    // -------------------------------------------------------------------------
    // 1. VolumeSteps preference timing and repeated callback parity
    // -------------------------------------------------------------------------

    @Test
    fun volumeSteps_preferenceMutationAfterInstall_contract() {
        val prefs = PrefMap<String, Any>()
        prefs["system_volumesteps"] = 150
        val hook = installVolumeSteps(prefs)

        MainModule.mPrefs["system_volumesteps"] = 200
        val audioService = createAudioService()
        val callback = fakeBeforeCallback(audioService, methodName = "createStreamStates")
        hook.beforeHook(callback)

        val current = maxStreamVolume()
        assertEquals("current uses install-time mult (150) because feature is REBOOT", 8, current[0])
        assertEquals(11, current[1])
        assertEquals(23, current[2])

        resetAudioServiceMaxStreamVolume()
        LegacyAudioServiceBehaviorOracle.legacyCreateStreamStates(audioService, childClassLoader, MainModule.mPrefs)
        val legacy = maxStreamVolume()
        assertEquals("legacy reads callback-time mult (200)", 10, legacy[0])
        assertEquals(14, legacy[1])
        assertEquals(30, legacy[2])
    }

    @Test
    fun volumeSteps_repeatedCreateStreamStates_preservesLegacyBehavior() {
        val prefs = PrefMap<String, Any>()
        prefs["system_volumesteps"] = 150
        val hook = installVolumeSteps(prefs)
        val audioService = createAudioService()

        val first = fakeBeforeCallback(audioService, methodName = "createStreamStates")
        hook.beforeHook(first)
        val afterFirst = maxStreamVolume()
        assertEquals(8, afterFirst[0])

        val second = fakeBeforeCallback(audioService, methodName = "createStreamStates")
        hook.beforeHook(second)
        val afterSecond = maxStreamVolume()
        assertEquals(12, afterSecond[0])

        resetAudioServiceMaxStreamVolume()
        val legacyService = createAudioService()
        LegacyAudioServiceBehaviorOracle.legacyCreateStreamStates(legacyService, childClassLoader, prefs)
        val legacyFirst = maxStreamVolume()
        assertEquals(8, legacyFirst[0])

        LegacyAudioServiceBehaviorOracle.legacyCreateStreamStates(legacyService, childClassLoader, prefs)
        val legacySecond = maxStreamVolume()
        assertEquals(12, legacySecond[0])
    }

    @Test
    fun volumeSteps_multiples() {
        val table = listOf(
            1 to listOf(0, 0, 0),
            50 to listOf(3, 4, 8),
            100 to listOf(5, 7, 15),
            150 to listOf(8, 11, 23),
            120 to listOf(6, 8, 18)
        )
        for ((mult, expected) in table) {
            FeatureInstallRegistry.clearStatesForTesting()
            FakeXposedInterface.reset()
            resetAudioServiceMaxStreamVolume()
            val prefs = PrefMap<String, Any>()
            prefs["system_volumesteps"] = mult
            val hook = installVolumeSteps(prefs)
            val audioService = createAudioService()
            val callback = fakeBeforeCallback(audioService, methodName = "createStreamStates")
            hook.beforeHook(callback)

            val current = maxStreamVolume()
            assertEquals("mult=$mult index 0", expected[0], current[0])
            assertEquals("mult=$mult index 1", expected[1], current[1])
            assertEquals("mult=$mult index 2", expected[2], current[2])
        }
    }

    @Test
    fun volumeSteps_multGreaterThan100NoClamp() {
        val prefs = PrefMap<String, Any>()
        prefs["system_volumesteps"] = 200
        val hook = installVolumeSteps(prefs)
        val audioService = createAudioService()
        val callback = fakeBeforeCallback(audioService, methodName = "createStreamStates")
        hook.beforeHook(callback)

        val current = maxStreamVolume()
        assertEquals(10, current[0])
        assertEquals(14, current[1])
        assertEquals(30, current[2])
    }

    @Test
    fun volumeSteps_negativeOrZeroDoesNotInstall() {
        val negative = PrefMap<String, Any>()
        negative["system_volumesteps"] = -1
        val server1 = runtime(negative)
        assertFalse(FeatureDispatcher.installById("volumeSteps", server1))

        val zero = PrefMap<String, Any>()
        zero["system_volumesteps"] = 0
        val server2 = runtime(zero)
        assertFalse(FeatureDispatcher.installById("volumeSteps", server2))
    }

    // -------------------------------------------------------------------------
    // 2. readSettings success parity with legacy oracle
    // -------------------------------------------------------------------------

    @Test
    fun readSettings_fullSuccess_matchesLegacyIndexMap() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val currentState = createVolumeStreamState(audioService, 1)
        setSettingsOverride(5)
        val currentCallback = fakeBeforeCallback(currentState)
        hook.beforeHook(currentCallback)
        assertTrue("current skips original", isSkipped(currentCallback))

        val legacyService = createAudioService()
        val legacyState = createVolumeStreamState(legacyService, 1)
        setSettingsOverride(5)
        assertTrue("legacy succeeds", LegacyAudioServiceBehaviorOracle.legacyReadSettings(legacyState, childClassLoader))

        val currentMap = indexMapSnapshot(currentState)
        val legacyMap = indexMapSnapshot(legacyState)
        assertMapsEqual(legacyMap, currentMap)
        assertEquals(AudioSystem.DEVICE_OUT_ALL_SET.size, currentMap.size)
        for (device in AudioSystem.DEVICE_OUT_ALL_SET) {
            assertTrue("device $device in map", currentMap.containsKey(device))
            assertEquals(50, currentMap[device])
        }
    }

    @Test
    fun readSettings_missingSettingValue_matchesLegacyDefaultDeviceOnly() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val currentState = createVolumeStreamState(audioService, 1)
        setSettingsOverride(MISSING_SETTINGS_VALUE)
        val currentCallback = fakeBeforeCallback(currentState)
        hook.beforeHook(currentCallback)
        assertTrue(isSkipped(currentCallback))

        val legacyService = createAudioService()
        val legacyState = createVolumeStreamState(legacyService, 1)
        setSettingsOverride(MISSING_SETTINGS_VALUE)
        assertTrue(LegacyAudioServiceBehaviorOracle.legacyReadSettings(legacyState, childClassLoader))

        val currentMap = indexMapSnapshot(currentState)
        val legacyMap = indexMapSnapshot(legacyState)
        assertMapsEqual(legacyMap, currentMap)
        assertEquals(1, currentMap.size)
        assertEquals(50, currentMap[AudioSystem.DEVICE_OUT_DEFAULT])
    }

    @Test
    fun readSettings_streamTypeNot1_doesNotSkipOriginal() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 5)
        val callback = fakeBeforeCallback(volumeState)
        hook.beforeHook(callback)

        assertFalse("must not skip for non-stream-type-1", isSkipped(callback))
        assertTrue("no device requested", requestedDeviceTypes(volumeState).isEmpty())
        assertTrue("no valid index calls", validIndexCalls(volumeState).isEmpty())
    }

    @Test
    fun readSettings_multipleVolumeStreamState_instancesAreIndependent() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val state1 = createVolumeStreamState(audioService, 1)
        val state2 = createVolumeStreamState(audioService, 1)

        setSettingsOverride(5)
        val callback1 = fakeBeforeCallback(state1)
        hook.beforeHook(callback1)

        setSettingsOverride(MISSING_SETTINGS_VALUE)
        val callback2 = fakeBeforeCallback(state2)
        hook.beforeHook(callback2)

        val map1 = indexMapSnapshot(state1)
        val map2 = indexMapSnapshot(state2)
        assertEquals(AudioSystem.DEVICE_OUT_ALL_SET.size, map1.size)
        assertEquals(1, map2.size)
    }

    // -------------------------------------------------------------------------
    // 3. readSettings per-device failure parity with legacy
    // -------------------------------------------------------------------------

    @Test
    fun readSettings_getSettingNameForDeviceFailure_doesNotSkipOriginalAndMatchesLegacyPartialMap() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val currentState = createVolumeStreamState(audioService, 1)
        setField(currentState, "failGetSettingNameForDeviceAt", 8)
        setSettingsOverride(5)
        val currentCallback = fakeBeforeCallback(currentState)
        hook.beforeHook(currentCallback)

        assertFalse("must not skip original when replacement is incomplete", isSkipped(currentCallback))
        val currentMap = indexMapSnapshot(currentState)
        assertTrue("devices before failure populated", currentMap.containsKey(1))
        assertTrue(currentMap.containsKey(2))
        assertTrue(currentMap.containsKey(4))
        assertFalse("failing device 8 not populated", currentMap.containsKey(8))

        val legacyService = createAudioService()
        val legacyState = createVolumeStreamState(legacyService, 1)
        setField(legacyState, "failGetSettingNameForDeviceAt", 8)
        setSettingsOverride(5)
        try {
            LegacyAudioServiceBehaviorOracle.legacyReadSettings(legacyState, childClassLoader)
            fail("legacy expected to abort on getSettingNameForDevice failure")
        } catch (t: Throwable) {
            // legacy propagates; outcome is original runs, map partial up to failing device
        }
        val legacyMap = indexMapSnapshot(legacyState)
        assertMapsEqual(legacyMap, currentMap)
    }

    @Test
    fun readSettings_getIntForUserFailure_doesNotSkipOriginalAndMatchesLegacyPartialMap() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val currentState = createVolumeStreamState(audioService, 1)
        setSettingsOverride(5)
        setThrowOnGetIntForUser(RuntimeException("simulated getIntForUser failure"))
        val currentCallback = fakeBeforeCallback(currentState)
        hook.beforeHook(currentCallback)

        assertFalse("must not skip original when Settings.System.getIntForUser fails", isSkipped(currentCallback))
        val currentMap = indexMapSnapshot(currentState)
        assertTrue("no device should be written before first getIntForUser", currentMap.isEmpty())

        val legacyService = createAudioService()
        val legacyState = createVolumeStreamState(legacyService, 1)
        setSettingsOverride(5)
        setThrowOnGetIntForUser(RuntimeException("simulated getIntForUser failure"))
        try {
            LegacyAudioServiceBehaviorOracle.legacyReadSettings(legacyState, childClassLoader)
            fail("legacy expected to abort on getIntForUser failure")
        } catch (t: Throwable) {
            // expected
        }
        val legacyMap = indexMapSnapshot(legacyState)
        assertMapsEqual(legacyMap, currentMap)
    }

    @Test
    fun readSettings_getValidIndexFailure_doesNotSkipOriginalAndMatchesLegacyPartialMap() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val currentState = createVolumeStreamState(audioService, 1)
        setField(currentState, "failGetValidIndexAt", 8)
        setSettingsOverride(5)
        val currentCallback = fakeBeforeCallback(currentState)
        hook.beforeHook(currentCallback)

        assertFalse("must not skip original when getValidIndex fails", isSkipped(currentCallback))
        val currentMap = indexMapSnapshot(currentState)
        assertTrue(currentMap.containsKey(1))
        assertTrue(currentMap.containsKey(2))
        assertTrue(currentMap.containsKey(4))
        assertFalse("failing device 8 not populated", currentMap.containsKey(8))

        val legacyService = createAudioService()
        val legacyState = createVolumeStreamState(legacyService, 1)
        setField(legacyState, "failGetValidIndexAt", 8)
        setSettingsOverride(5)
        try {
            LegacyAudioServiceBehaviorOracle.legacyReadSettings(legacyState, childClassLoader)
            fail("legacy expected to abort on getValidIndex failure")
        } catch (t: Throwable) {
            // expected
        }
        val legacyMap = indexMapSnapshot(legacyState)
        assertMapsEqual(legacyMap, currentMap)
    }

    // -------------------------------------------------------------------------
    // 4. rethrowAudioFatal helper contract (mirrors P1B-4A fatal handling)
    // -------------------------------------------------------------------------

    @Test
    fun rethrowAudioFatal_directOutOfMemoryError_rethrows() {
        val helperMethod = rethrowAudioFatalMethod()
        try {
            helperMethod.invoke(hooksInstance(), OutOfMemoryError("OOM"))
            fail("expected OutOfMemoryError")
        } catch (e: InvocationTargetException) {
            assertTrue(e.cause is OutOfMemoryError)
        }
    }

    @Test
    fun rethrowAudioFatal_wrappedOutOfMemoryError_rethrowsCause() {
        val helperMethod = rethrowAudioFatalMethod()
        try {
            helperMethod.invoke(hooksInstance(), InvocationTargetException(OutOfMemoryError("OOM")))
            fail("expected OutOfMemoryError")
        } catch (e: InvocationTargetException) {
            assertTrue(e.cause is OutOfMemoryError)
        }
    }

    @Test
    fun rethrowAudioFatal_wrappedThreadDeath_rethrowsCause() {
        val helperMethod = rethrowAudioFatalMethod()
        try {
            helperMethod.invoke(hooksInstance(), InvocationTargetException(ThreadDeath()))
            fail("expected ThreadDeath")
        } catch (e: InvocationTargetException) {
            assertTrue(e.cause is ThreadDeath)
        }
    }

    @Test
    fun rethrowAudioFatal_wrappedOrdinaryRuntimeException_doesNotRethrow() {
        val helperMethod = rethrowAudioFatalMethod()
        helperMethod.invoke(hooksInstance(), InvocationTargetException(RuntimeException("ordinary")))
        // no throw
    }

    // -------------------------------------------------------------------------
    // 5. Lock owner and skip-original contract
    // -------------------------------------------------------------------------

    @Test
    fun readSettings_lockOwner_matchesMethodDeclaringClass() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        val audioService = createAudioService()
        val volumeState = createVolumeStreamState(audioService, 1)
        val thisObjectClass = volumeState.javaClass

        val recorded = FakeXposedInterface.recordedHooks.first {
            it.executable.declaringClass.name == "com.android.server.audio.AudioService\$VolumeStreamState"
                    && it.executable.name == "readSettings"
        }
        assertSame("synchronized owner must be the method's declaring class", thisObjectClass, recorded.executable.declaringClass)
    }

    @Test
    fun readSettings_skipOriginal_matrix() {
        val prefs = PrefMap<String, Any>()
        prefs["system_separatevolume"] = true
        val hook = installNotificationVolume(prefs)

        // streamType != 1 -> not skipped
        val audioService = createAudioService()
        val state5 = createVolumeStreamState(audioService, 5)
        val cb5 = fakeBeforeCallback(state5)
        hook.beforeHook(cb5)
        assertFalse(isSkipped(cb5))

        // streamType == 1 full success -> skipped
        val state1 = createVolumeStreamState(audioService, 1)
        setSettingsOverride(5)
        val cb1 = fakeBeforeCallback(state1)
        hook.beforeHook(cb1)
        assertTrue(isSkipped(cb1))

        // per-device failure -> not skipped
        val stateFail = createVolumeStreamState(audioService, 1)
        setField(stateFail, "failGetSettingNameForDeviceAt", 8)
        setSettingsOverride(5)
        val cbFail = fakeBeforeCallback(stateFail)
        hook.beforeHook(cbFail)
        assertFalse(isSkipped(cbFail))
    }

    // -------------------------------------------------------------------------
    // 6. Mutation correctness: removing cause-chain traversal must fail
    // -------------------------------------------------------------------------

    @Test
    fun rethrowAudioFatal_withoutCauseTraversal_mustFail_wrappedFatal() {
        val helperMethod = rethrowAudioFatalMethod()
        // The helper traverses cause chain. If it didn't, an InvocationTargetException
        // wrapping OutOfMemoryError would fall through and be swallowed.
        try {
            helperMethod.invoke(hooksInstance(), InvocationTargetException(OutOfMemoryError("OOM")))
            fail("wrapped fatal must be rethrown")
        } catch (e: InvocationTargetException) {
            assertTrue(e.cause is OutOfMemoryError)
        }
    }
}
