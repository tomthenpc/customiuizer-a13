package com.android.systemui.qs.tiles

import android.content.Intent
import android.view.View
import com.android.systemui.qs.tileimpl.QSTileImpl

/** Minimal ABI stub so AddCustomTileHook can install independent NfcTile hooks. */
open class MiuiNfcTile : QSTileImpl() {
    fun isAvailable(): Boolean = true
    fun getTileLabel(): String = "nfc"
    fun handleSetListening(listening: Boolean) {}
    fun getLongClickIntent(): Intent? = null
    fun handleUpdateState(state: Any?, arg: Any?) {}
    override fun handleClick(v: View?) {}
}
