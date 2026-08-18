package com.miui.home.launcher

import android.content.Context

class DeviceConfig {
    companion object {
        @JvmField
        var sCellCountY: Int = 4

        @JvmStatic
        fun loadCellsCountConfig(context: Context, flag: Boolean) {}

        @JvmStatic
        fun getCellHeight(): Int = 1

        @JvmStatic
        fun isCellSizeChangedByTheme(): Boolean = false

        @JvmStatic
        fun usingFsGesture(): Boolean = false
    }
}
