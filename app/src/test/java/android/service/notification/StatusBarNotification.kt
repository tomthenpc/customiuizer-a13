package android.service.notification

/**
 * Minimal test shadow of [android.service.notification.StatusBarNotification] for JVM unit tests.
 *
 * The framework stub jar does not initialize the internal [mPkgName] field or allow
 * [isSubstituteNotification] to be controlled, so this shadow provides the fields used by
 * [SystemUINotificationHooks.OpenNotifyInFloatingWindowHook].
 */
open class StatusBarNotification {

    open var mPkgName: String? = null
    open var mPackageName: String? = null
    open var mId: Int = 0
    open var mTag: String? = null
    open var mAppUid: Int = 0
    open var isSubstitute: Boolean = false

    constructor()

    constructor(
        pkg: String?,
        opPkg: String?,
        id: Int,
        tag: String?,
        uid: Int,
        initialPid: Int,
        score: Int,
        notification: Any?,
        user: Any?,
        postTime: Long
    ) {
        this.mPackageName = pkg
        this.mPkgName = pkg
        this.mId = id
        this.mTag = tag
        this.mAppUid = uid
    }

    constructor(
        pkg: String?,
        opPkg: String?,
        id: Int,
        tag: String?,
        uid: Int,
        initialPid: Int,
        score: Int,
        notification: Any?,
        user: Any?,
        postTime: Long,
        key: Any?
    ) {
        this.mPackageName = pkg
        this.mPkgName = pkg
        this.mId = id
        this.mTag = tag
        this.mAppUid = uid
    }

    open fun getPackageName(): String? = mPackageName

    open fun isSubstituteNotification(): Boolean = isSubstitute

    open fun getAppUid(): Int = mAppUid

    open fun getId(): Int = mId

    open fun getTag(): String? = mTag

    override fun toString(): String = "StatusBarNotification{mPkgName=$mPkgName, mPackageName=$mPackageName, isSubstitute=$isSubstitute}"
}
