package com.android.systemui.qs.tileimpl

import com.android.systemui.qs.tiles.FakeNfcTile
import com.android.systemui.qs.tiles.FakeWifiTile

open class MiuiQSFactory {

    open fun createTileInternal(tileName: String): QSTileImpl {
        return when (tileName) {
            "wifi" -> FakeWifiTile()
            "bt" -> FakeNfcTile()
            "custom_5G" -> FakeNfcTile()
            else -> FakeNfcTile()
        }
    }
}
