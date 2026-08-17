package com.android.systemui.statusbar.notification.row

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import tv.withaibuild.customiuizer.mods.BaseExpandableNotificationRow
import tv.withaibuild.customiuizer.mods.BaseNotification
import tv.withaibuild.customiuizer.mods.RecordingMenuContainer
import tv.withaibuild.customiuizer.mods.RecordingMenuItemView

open class MiuiNotificationMenuRow {

    var mContext: Context? = null
    val mMenuItems: ArrayList<Any> = ArrayList()
    var mSbn: BaseNotification? = null
    var mParent: BaseExpandableNotificationRow? = null
    var mMenuMargin: Int = 0
    var mMenuContainer: LinearLayout? = null
    var originalClearsAndAddsSystemItems: Boolean = true
    var failGetMenuView: Boolean = false

    fun createMenuViews(animate: Boolean, fromLeft: Boolean) {
        if (originalClearsAndAddsSystemItems) {
            mMenuItems.clear()
            (mMenuContainer as? RecordingMenuContainer)?.addedChildren?.clear()
            mMenuContainer?.removeAllViews()
            addSystemItem("system_first")
            addSystemItem("system_second")
        }
    }

    private fun addSystemItem(tag: String) {
        val context = mContext ?: return
        val container = mMenuContainer ?: return
        val item = MiuiNotificationMenuItem(context, 0, null, 0)
        val menuView = item.getMenuView()
        menuView.tag = tag
        mMenuItems.add(item)
        container.addView(menuView, LinearLayout.LayoutParams(-2, -2))
    }

    inner class MiuiNotificationMenuItem(
        context: Context,
        val titleResId: Int,
        val gutsContent: NotificationGuts.GutsContent?,
        val iconResId: Int
    ) {
        private val view = RecordingMenuItemView(context)

        fun getMenuView(): View {
            if (this@MiuiNotificationMenuRow.failGetMenuView && titleResId != 0) {
                throw RuntimeException("getMenuView simulated failure")
            }
            return view
        }
    }
}
