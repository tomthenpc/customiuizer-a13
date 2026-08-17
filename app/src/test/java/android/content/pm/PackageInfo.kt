package android.content.pm

/** JVM test shadow with the PackageInfo fields AppInfo / sort tests write. */
open class PackageInfo {
    @JvmField var packageName: String? = null
    @JvmField var versionCode: Int = 0
    @JvmField var versionName: String? = null
    @JvmField var applicationInfo: ApplicationInfo? = null
}
