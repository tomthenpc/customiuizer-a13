package com.android.systemui.statusbar.notification.row

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import tv.withaibuild.customiuizer.mods.FakeExpandableNotificationRow
import tv.withaibuild.customiuizer.mods.FakeStatusBarNotification
import tv.withaibuild.customiuizer.mods.RecordingMenuContainer
import tv.withaibuild.customiuizer.mods.RecordingMenuItemView

open class MiuiNotificationMenuRow {

    var mContext: Context? = null
    val mMenuItems: ArrayList<Any> = ArrayList()
    var mSbn: FakeStatusBarNotification? = null
    var mParent: FakeExpandableNotificationRow? = null
    var mMenuMargin: Int = 0
    var mMenuContainer: LinearLayout? = null

    fun createMenuViews(animate: Boolean, fromLeft: Boolean) {}

    inner class MiuiNotificationMenuItem(
        context: Context,
        val titleResId: Int,
        val icon: Drawable?,
        val iconResId: Int
    ) {
        val menuView: View = RecordingMenuItemView(context)
    }
}
