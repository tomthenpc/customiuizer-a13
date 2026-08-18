package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

/**
 * B3A-R2: missing ROM class / field must fail-open and not block later independent hooks.
 */
class LauncherB3AR2CompatibilityTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    @Test
    fun unlockGridsMissingDeviceConfig_stillInstallsScreenUtils() {
        LauncherLayoutHooks.UnlockGridsHook(lpparam(hidingClassLoader(DEVICE_CONFIG)))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == SCREEN_UTILS &&
                    it.executable.name == "getScreenCellsSizeOptions"
            }
        )
        assertFalse(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == DEVICE_CONFIG
            }
        )
    }

    @Test
    fun disableLogMissingIsEnableField_stillInstallsCollectorHooks() {
        LauncherSystemHooks.DisableLauncherLogHook(lpparam(parentClassLoader))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == COLLECTOR &&
                    it.executable.name == "canTrackLaunchAppEvent"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == JOB_SERVICE &&
                    it.executable.name == "onStartJob"
            }
        )
    }

    @Test
    fun fsGesturesMissingBaseRecents_stillInstallsIndependentHooks() {
        LauncherGestureHooks.FSGesturesHook(lpparam(hidingClassLoader(BASE_RECENTS)))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == DEVICE_CONFIG &&
                    it.executable.name == "usingFsGesture"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == GESTURE_STUB &&
                    it.executable.name == "onTouchEvent"
            }
        )
        assertFalse(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == BASE_RECENTS
            }
        )
    }

    @Test
    fun wallpaperScaleMissingZoomClass_stillInstallsDimLayer() {
        MainModule.mPrefs["launcher_disable_wallpaperscale"] = true
        LauncherAnimationHooks.DisableLauncherWallpaperScale(lpparam(hidingClassLoader(WALLPAPER_ZOOM)))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == DIM_LAYER &&
                    it.executable.name == "isSupportDim"
            }
        )
    }

    private fun lpparam(classLoader: ClassLoader): PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.miui.home"
                "getProcessName" -> "com.miui.home"
                "getClassLoader" -> classLoader
                else -> null
            }
        } as PackageReadyParam
    }

    private fun hidingClassLoader(hidden: String): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == hidden) {
                    throw ClassNotFoundException(name)
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    companion object {
        private const val DEVICE_CONFIG = "com.miui.home.launcher.DeviceConfig"
        private const val SCREEN_UTILS = "com.miui.home.launcher.ScreenUtils"
        private const val COLLECTOR = "com.miui.home.launcher.AnalyticalDataCollector"
        private const val JOB_SERVICE = "com.miui.home.launcher.AnalyticalDataCollectorJobService"
        private const val BASE_RECENTS = "com.miui.home.recents.BaseRecentsImpl"
        private const val GESTURE_STUB = "com.miui.home.recents.GestureStubView"
        private const val WALLPAPER_ZOOM = "com.miui.home.launcher.wallpaper.WallpaperZoomManagerKt"
        private const val DIM_LAYER = "com.miui.home.recents.DimLayer"
    }
}
