package com.android.server.audio

import android.content.ContentResolver
import android.util.SparseIntArray

class AudioService {

    companion object {
        @JvmField
        var MAX_STREAM_VOLUME = intArrayOf(5, 7, 15, 7, 7, 15, 7, 15, 7, 15)
    }

    var mContentResolver: ContentResolver? = null

    fun createStreamStates() {}

    fun updateStreamVolumeAlias(updateTypes: Boolean, caller: String) {}

    fun shouldZenMuteStream(streamType: Int): Boolean = false

    inner class VolumeController {
        fun suppressAdjustment(streamType: Int, direction: Int, isMuteAdjust: Boolean): Boolean = false
    }

    inner class VolumeStreamState {
        var mStreamType: Int = 0
        var mIndexMap: SparseIntArray = SparseIntArray()

        val requestedDeviceTypes = mutableListOf<Int>()
        val validIndexCalls = mutableListOf<Pair<Int, Int>>()

        fun getSettingNameForDevice(deviceType: Int): String {
            requestedDeviceTypes.add(deviceType)
            return "volume_${mStreamType}_$deviceType"
        }

        fun getValidIndex(index: Int, allowMax: Boolean): Int {
            val deviceType = requestedDeviceTypes.lastOrNull() ?: -1
            validIndexCalls.add(deviceType to index)
            return index.coerceIn(0, 100)
        }

        fun readSettings() {}
    }

    fun requestAudioFocus(clientId: String, packageName: String): Int = -1

    fun requestAudioFocus(clientId: String, packageName: String, another: String): Int = -1

    fun requestAudioFocus(
        dispatcher: Any?,
        streamType: Int,
        durationHint: Int,
        focusChange: Int,
        clientId: String,
        callingPackage: String
    ): Int = -1
}
