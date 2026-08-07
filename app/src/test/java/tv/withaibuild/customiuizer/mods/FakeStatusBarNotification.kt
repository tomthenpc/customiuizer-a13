package tv.withaibuild.customiuizer.mods

import android.app.PendingIntent

open class BaseNotification

open class FakeStatusBarNotification(
    val mPackageName: String = "com.example.app",
    val mAppUid: Int = 1000
) : BaseNotification() {
    fun getPackageName(): String = mPackageName
    fun getAppUid(): Int = mAppUid
}

class RuntimeStatusBarNotification(
    private val packageName: String = "com.example.runtime",
    private val appUid: Int = 1000
) : BaseNotification() {
    fun getPackageName(): String = packageName
    fun getAppUid(): Int = appUid
}

open class BaseExpandableNotificationRow

open class FakeExpandableNotificationRow(
    val mMiniWindowTargetPkg: Any? = "com.example.app",
    val mPendingIntent: PendingIntent? = null
) : BaseExpandableNotificationRow() {
    fun getMiniWindowTargetPkg(): Any? = mMiniWindowTargetPkg
    fun getPendingIntent(): Any? = mPendingIntent
}

class RuntimeExpandableNotificationRow(
    private val miniWindowTargetPkg: Any? = "com.example.runtime",
    private val pendingIntent: PendingIntent? = null
) : BaseExpandableNotificationRow() {
    fun getMiniWindowTargetPkg(): Any? = miniWindowTargetPkg
    fun getPendingIntent(): Any? = pendingIntent
}
