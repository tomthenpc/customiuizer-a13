package com.android.systemui.statusbar.notification.row

import android.content.Context
import android.graphics.drawable.Drawable
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
        item.menuView.tag = tag
        mMenuItems.add(item)
        container.addView(item.menuView, LinearLayout.LayoutParams(-2, -2))
    }

    inner class MiuiNotificationMenuItem(
        context: Context,
        val titleResId: Int,
        val icon: Drawable?,
        val iconResId: Int
    ) {
        val menuView: View = RecordingMenuItemView(context)
    }
}
