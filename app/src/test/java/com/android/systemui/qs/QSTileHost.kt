package com.android.systemui.qs

import android.content.Context
import java.util.LinkedHashMap

open class QSTileHost {
    @JvmField
    var mContext: Context? = null

    @JvmField
    var mTiles: LinkedHashMap<String, Any> = LinkedHashMap()
}
