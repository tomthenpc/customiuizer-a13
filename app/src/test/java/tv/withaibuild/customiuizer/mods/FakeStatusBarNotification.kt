package tv.withaibuild.customiuizer.mods

import android.app.PendingIntent

open class FakeStatusBarNotification(
    val mPackageName: String = "com.example.app",
    val mAppUid: Int = 1000
) {
    fun getPackageName(): String = mPackageName
    fun getAppUid(): Int = mAppUid
}

open class FakeExpandableNotificationRow(
    val mMiniWindowTargetPkg: Any? = "com.example.app",
    val mPendingIntent: PendingIntent? = null
) {
    fun getMiniWindowTargetPkg(): Any? = mMiniWindowTargetPkg
    fun getPendingIntent(): Any? = mPendingIntent
}
