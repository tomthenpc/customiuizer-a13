package com.android.systemui.controlcenter.policy

class ControlCenterControllerImpl {
    var collapsed = false
    var useControlCenter = true

    fun isUseControlCenter(): Boolean = useControlCenter

    fun collapseControlCenter(immediate: Boolean) {
        collapsed = immediate
    }
}
