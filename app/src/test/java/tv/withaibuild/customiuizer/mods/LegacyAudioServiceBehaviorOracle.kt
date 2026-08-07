package tv.withaibuild.customiuizer.mods

import android.provider.Settings
import android.util.SparseIntArray
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Faithful baseline behavior oracle extracted from `3d38cdd53a6190c68187a803badaf201dfda25cd`.
 *
 * This oracle replicates the legacy `createStreamStates` and `VolumeStreamState#readSettings`
 * callback semantics using callback-time reflection lookup and `MainModule.mPrefs` reads.
 * It does not apply the P1B-2 optimizations and does not "fix" legacy behavior.
 */
object LegacyAudioServiceBehaviorOracle {

    fun legacyCreateStreamStates(audioService: Any, classLoader: ClassLoader, prefs: PrefMap<String, Any>) {
        val audioCls = XposedHelpers.findClass("com.android.server.audio.AudioService", classLoader)
        val maxStreamVolume = XposedHelpers.getStaticObjectField(audioCls, "MAX_STREAM_VOLUME") as? IntArray ?: return
        val mult = prefs.getInt("system_volumesteps", 0)
        if (mult <= 0) return
        for (i in maxStreamVolume.indices) {
            maxStreamVolume[i] = Math.round(maxStreamVolume[i] * mult / 100.0f)
        }
        XposedHelpers.setStaticObjectField(audioCls, "MAX_STREAM_VOLUME", maxStreamVolume)
    }

    fun legacyReadSettings(volumeState: Any, classLoader: ClassLoader): Boolean {
        val mStreamType = XposedHelpers.getIntField(volumeState, "mStreamType")
        if (mStreamType != 1) return false
        synchronized(volumeState.javaClass) {
            val audioSystem = XposedHelpers.findClass("android.media.AudioSystem", classLoader)
            val deviceOutAll = XposedHelpers.getStaticObjectField(audioSystem, "DEVICE_OUT_ALL_SET") as? Set<*> ?: return false
            val deviceOutDefault = XposedHelpers.getStaticIntField(audioSystem, "DEVICE_OUT_DEFAULT")
            val defaultStreamVolume = XposedHelpers.getStaticObjectField(audioSystem, "DEFAULT_STREAM_VOLUME") as? IntArray ?: return false
            val mContentResolver = XposedHelpers.getObjectField(XposedHelpers.getSurroundingThis(volumeState), "mContentResolver")
            val mIndexMap = XposedHelpers.getObjectField(volumeState, "mIndexMap") as? SparseIntArray ?: return false
            for (rawDeviceType in deviceOutAll) {
                val deviceType = rawDeviceType as? Int ?: continue
                val name = XposedHelpers.callMethod(volumeState, "getSettingNameForDevice", deviceType) as? String ?: continue
                val defaultValue = if (deviceType == deviceOutDefault) defaultStreamVolume[mStreamType] else -1
                val index = XposedHelpers.callStaticMethod(
                    Settings.System::class.java,
                    "getIntForUser",
                    mContentResolver, name, defaultValue, -2
                ) as? Int ?: continue
                if (index != -1) {
                    val validIndex = XposedHelpers.callMethod(volumeState, "getValidIndex", 10 * index, true) as? Int ?: continue
                    mIndexMap.put(deviceType, validIndex)
                }
            }
            XposedHelpers.setObjectField(volumeState, "mIndexMap", mIndexMap)
        }
        return true
    }
}
