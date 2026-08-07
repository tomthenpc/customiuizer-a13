package com.android.systemui.statusbar.notification.modal

open class ModalController {
    var animExitCalled: Boolean = false

    open fun animExitModelCollapsePanels() {
        animExitCalled = true
    }
}
