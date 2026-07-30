package tv.withaibuild.customiuizer.mods.catalog

/**
 * The OS process where a feature is installed.
 *
 * Matching is value-based on the package/process name, so equality checks are
 * not needed and different instances with the same package compare correctly.
 */
sealed class ProcessTarget {

    abstract fun matches(processName: String): Boolean

    object SystemServer : ProcessTarget() {
        override fun matches(processName: String): Boolean =
            processName == "android" || processName == "system_server"
    }

    object SystemUI : ProcessTarget() {
        override fun matches(processName: String): Boolean =
            processName == "com.android.systemui"
    }

    /**
     * The default MIUI launcher package. Modern global builds also use the
     * `com.miui.home` package name.
     */
    object Launcher : ProcessTarget() {
        override fun matches(processName: String): Boolean =
            processName == "com.miui.home" ||
            processName == "com.mi.android.globallauncher"
    }

    data class Package(val packageName: String) : ProcessTarget() {
        override fun matches(processName: String): Boolean = processName == packageName
    }

    object Any : ProcessTarget() {
        override fun matches(processName: String): Boolean = true
    }
}
