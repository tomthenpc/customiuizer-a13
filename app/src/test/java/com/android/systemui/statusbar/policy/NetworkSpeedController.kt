package com.android.systemui.statusbar.policy

import android.content.Context
import android.os.Looper

/** Minimal ABI stub so DeviceInfoMonitor can still install after class-lookup fail-open. */
open class NetworkSpeedController(
    @Suppress("UNUSED_PARAMETER") context: Context,
    @Suppress("UNUSED_PARAMETER") looper: Looper
)
