package com.android.systemui.controlcenter.policy

open class ControlCenterControllerImpl {
    var collapsed = false
    open var useControlCenter = true

    open fun isUseControlCenter(): Boolean = useControlCenter

    fun collapseControlCenter(immediate: Boolean) {
        collapsed = immediate
    }
}
