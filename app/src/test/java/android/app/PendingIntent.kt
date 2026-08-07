package android.app

/**
 * Minimal test shadow of [android.app.PendingIntent] for JVM unit tests.
 *
 * Provides settable [creatorPackage] so notification intent launch tests can
 * exercise the non-substitute package path without needing real Android
 * PendingIntent state.
 */
open class PendingIntent {

    open var creatorPackage: String? = null

    open var isActivity: Boolean = true

    constructor()

    constructor(creatorPackage: String?) {
        this.creatorPackage = creatorPackage
    }
}
