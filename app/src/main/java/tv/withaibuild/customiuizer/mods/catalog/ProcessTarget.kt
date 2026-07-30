package tv.withaibuild.customiuizer.mods.catalog

/**
 * The OS process where a feature is installed.
 *
 * Using a sealed class instead of a nullable package string makes the
 * system-server vs package distinction explicit and type-safe.
 */
sealed class ProcessTarget {
    object SystemServer : ProcessTarget()
    data class Package(val packageName: String) : ProcessTarget()
    object SystemUI : ProcessTarget()
    object Launcher : ProcessTarget()

    fun matches(packageName: String): Boolean = when (this) {
        is SystemServer -> packageName == "android"
        is Package -> this.packageName == packageName
        is SystemUI -> packageName == "com.android.systemui"
        is Launcher -> packageName == "com.miui.home" || packageName == "com.mi.android.globallauncher"
    }
}
