package com.android.server.audio

class AudioService {

    inner class VolumeController {
        fun suppressAdjustment(streamType: Int, direction: Int, isMuteAdjust: Boolean): Boolean = false
    }
}
