package tv.withaibuild.customiuizer.mods.utils

/**
 * Lightweight, compile-time-only process classification for A13.
 *
 * No Android runtime classes are held. No collections are allocated during
 * classification. The `when` expression is the single source of truth for
 * package/process routing.
 */
enum class ProcessScope {
    SYSTEM_SERVER,
    SYSTEM_UI,
    SYSTEM_UI_PLUGIN,
    LAUNCHER,
    SETTINGS_MAIN,
    SETTINGS_REMOTE,
    SECURITY_CENTER_MAIN,
    SECURITY_CENTER_REMOTE,
    SECURITY_CENTER_BOOTAWARE,
    POWER_KEEPER,
    WALLPAPER,
    MEDIA,
    PHONE,
    PACKAGE_INSTALLER,
    INPUT_METHOD,
    NETWORK_STACK,
    GENERIC_APP,
    UNSUPPORTED
}

object ProcessScopes {

    private const val PKG_ANDROID = "android"
    private const val PKG_SYSTEM_UI = "com.android.systemui"
    private const val PKG_SETTINGS = "com.android.settings"
    private const val PKG_SECURITY_CENTER = "com.miui.securitycenter"
    private const val PKG_HOME = "com.miui.home"
    private const val PKG_GLOBAL_LAUNCHER = "com.mi.android.globallauncher"
    private const val PKG_POWER_KEEPER = "com.miui.powerkeeper"
    private const val PKG_PACKAGE_INSTALLER = "com.miui.packageinstaller"

    private val KNOWN_PACKAGES: Set<String> = setOf(
        PKG_ANDROID,
        PKG_SYSTEM_UI,
        PKG_HOME,
        PKG_GLOBAL_LAUNCHER,
        "com.miui.miwallpaper",
        "com.miui.screenshot",
        "com.miui.gallery",
        "com.lbe.security.miui",
        "com.android.incallui",
        PKG_SECURITY_CENTER,
        PKG_POWER_KEEPER,
        PKG_SETTINGS,
        PKG_PACKAGE_INSTALLER
    )

    @JvmStatic
    fun isMainProcess(packageName: String, processName: String): Boolean =
        packageName == processName || processName.isEmpty()

    @JvmStatic
    fun isKnownPackage(packageName: String): Boolean =
        packageName in KNOWN_PACKAGES
                || packageName.startsWith("com.google.android.inputmethod")
                || packageName == "com.baidu.input"
                || packageName == "com.baidu.input_mi"
                || packageName == "com.iflytek.inputmethod"
                || packageName == "com.iflytek.inputmethod.miui"
                || packageName == "com.sohu.inputmethod.sogou"
                || packageName == "com.sohu.inputmethod.sogou.xiaomi"
                || packageName.startsWith("com.touchtype.swiftkey")
                || packageName.startsWith("com.tencent.wetype")

    /**
     * Classify a package/process pair. This is a pure lookup: no reflection,
     * no service calls, no allocation beyond the returned enum.
     */
    @JvmStatic
    fun resolve(packageName: String, processName: String): ProcessScope = when (packageName) {
        PKG_ANDROID -> ProcessScope.SYSTEM_SERVER
        PKG_SYSTEM_UI -> if (isMainProcess(packageName, processName)) ProcessScope.SYSTEM_UI else ProcessScope.SYSTEM_UI_PLUGIN
        PKG_HOME, PKG_GLOBAL_LAUNCHER -> ProcessScope.LAUNCHER
        PKG_SETTINGS -> if (isMainProcess(packageName, processName)) ProcessScope.SETTINGS_MAIN else ProcessScope.SETTINGS_REMOTE
        PKG_SECURITY_CENTER -> when {
            isBootawareProcess(processName) -> ProcessScope.SECURITY_CENTER_BOOTAWARE
            !isMainProcess(packageName, processName) -> ProcessScope.SECURITY_CENTER_REMOTE
            else -> ProcessScope.SECURITY_CENTER_MAIN
        }
        PKG_POWER_KEEPER -> ProcessScope.POWER_KEEPER
        "com.miui.miwallpaper" -> ProcessScope.WALLPAPER
        "com.miui.screenshot",
        "com.miui.gallery" -> ProcessScope.MEDIA
        "com.android.incallui" -> ProcessScope.PHONE
        PKG_PACKAGE_INSTALLER -> ProcessScope.PACKAGE_INSTALLER
        "com.android.location.fused" -> ProcessScope.UNSUPPORTED
        else -> when {
            packageName.startsWith("com.android.networkstack") -> ProcessScope.NETWORK_STACK
            packageName.startsWith("com.google.android.inputmethod")
                    || packageName == "com.baidu.input"
                    || packageName == "com.baidu.input_mi"
                    || packageName == "com.iflytek.inputmethod"
                    || packageName == "com.iflytek.inputmethod.miui"
                    || packageName == "com.sohu.inputmethod.sogou"
                    || packageName == "com.sohu.inputmethod.sogou.xiaomi"
                    || packageName.startsWith("com.touchtype.swiftkey")
                    || packageName.startsWith("com.tencent.wetype") -> ProcessScope.INPUT_METHOD
            else -> ProcessScope.GENERIC_APP
        }
    }

    private fun isBootawareProcess(processName: String): Boolean =
        processName.endsWith(".bootaware") || processName.endsWith(":bootaware")

    @JvmStatic
    fun isRejected(packageName: String, processName: String): Boolean = when (resolve(packageName, processName)) {
        ProcessScope.SETTINGS_REMOTE,
        ProcessScope.SECURITY_CENTER_BOOTAWARE,
        ProcessScope.UNSUPPORTED,
        ProcessScope.NETWORK_STACK -> true
        else -> false
    }

    @JvmStatic
    fun shouldLoadPrefs(packageName: String, processName: String): Boolean =
        !isRejected(packageName, processName) && isKnownPackage(packageName)

    @JvmStatic
    fun shouldHook(packageName: String, processName: String): Boolean =
        !isRejected(packageName, processName) && isKnownPackage(packageName)
}
