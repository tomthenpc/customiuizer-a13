package com.android.systemui.qs.tileimpl

import android.content.Context
import android.view.View

open class QSTileImpl {
    @JvmField
    var mContext: Context? = null

    @Suppress("UNUSED_PARAMETER")
    open fun handleClick(v: View?) {
        // Stub for testing
    }

    @Suppress("UNUSED_PARAMETER")
    open fun handleSecondaryClick(v: View?) {
        // Stub for testing
    }
}
