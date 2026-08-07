package com.android.systemui.qs.tiles

import android.view.View
import com.android.systemui.qs.tileimpl.QSTileImpl

open class FakeNfcTile : QSTileImpl() {
    var clickCount: Int = 0
        private set
    var secondaryClickCount: Int = 0
        private set

    override fun handleClick(v: View?) {
        clickCount++
    }

    override fun handleSecondaryClick(v: View?) {
        secondaryClickCount++
    }
}
