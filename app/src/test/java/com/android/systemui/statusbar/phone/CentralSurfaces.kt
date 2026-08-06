package com.android.systemui.statusbar.phone

class CentralSurfaces {
    val postedRunnables: MutableList<Pair<Boolean, Runnable>> = mutableListOf()

    fun postQSRunnableDismissingKeyguard(keepOpened: Boolean, runnable: Runnable) {
        postedRunnables.add(keepOpened to runnable)
    }
}
