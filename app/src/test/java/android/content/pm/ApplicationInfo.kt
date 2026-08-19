package android.content.pm

/** JVM test shadow with fields AppInfo reads from PackageInfo.applicationInfo. */
open class ApplicationInfo {
    @JvmField var sourceDir: String? = null
    @JvmField var dataDir: String? = null
    @JvmField var uid: Int = 0
    @JvmField var targetSdkVersion: Int = 0
    @JvmField var flags: Int = 0
    @JvmField var enabled: Boolean = true
    @JvmField var packageName: String? = null

    fun isSystemApp(): Boolean = false
    fun isSignedWithPlatformKey(): Boolean = false

    companion object {
        const val FLAG_SYSTEM = 1
        const val FLAG_UPDATED_SYSTEM_APP = 128
    }
}
