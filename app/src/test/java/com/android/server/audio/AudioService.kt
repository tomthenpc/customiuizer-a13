package com.android.server.audio

class AudioService {

    inner class VolumeController {
        fun suppressAdjustment(streamType: Int, direction: Int, isMuteAdjust: Boolean): Boolean = false
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
