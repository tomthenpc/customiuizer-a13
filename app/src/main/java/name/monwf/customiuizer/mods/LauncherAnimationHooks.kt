package name.monwf.customiuizer.mods

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import name.monwf.customiuizer.MainModule
import name.monwf.customiuizer.mods.utils.HookerClassHelper
import name.monwf.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import name.monwf.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import name.monwf.customiuizer.mods.utils.HookerClassHelper.MethodHook
import name.monwf.customiuizer.mods.utils.ModuleHelper
import name.monwf.customiuizer.mods.utils.XposedHelpers
import name.monwf.customiuizer.utils.Helpers

@Suppress("UNUSED_PARAMETER")
object LauncherAnimationHooks {

    private fun scaleStiffness(`val`: Float, scale: Float): Float {
        return (if (scale < 1.0f) 2f / scale else 1.0f / scale) * `val`
    }

    @JvmStatic
    fun FixAnimHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.miui.home.launcher.animate.SpringAnimator", lpparam.classLoader, "getSpringForce", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                var scale = Helpers.getAnimationScale(2)
                if (scale == 1.0f) return
                if (scale == 0f) scale = 0.01f
                param.getArgs()[2] = scaleStiffness(param.getArgs()[2] as? Float ?: 0f, scale)
            }
        })

        val hook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                var scale = Helpers.getAnimationScale(2)
                if (scale == 1.0f) return
                if (scale == 0f) scale = 0.01f
                XposedHelpers.setFloatField(param.getThisObject(), "mCenterXStiffness", scaleStiffness(XposedHelpers.getFloatField(param.getThisObject(), "mCenterXStiffness"), scale))
                XposedHelpers.setFloatField(param.getThisObject(), "mCenterYStiffness", scaleStiffness(XposedHelpers.getFloatField(param.getThisObject(), "mCenterYStiffness"), scale))
                XposedHelpers.setFloatField(param.getThisObject(), "mWidthStiffness", scaleStiffness(XposedHelpers.getFloatField(param.getThisObject(), "mWidthStiffness"), scale))
                XposedHelpers.setFloatField(param.getThisObject(), "mRadiusStiffness", scaleStiffness(XposedHelpers.getFloatField(param.getThisObject(), "mRadiusStiffness"), scale))
                XposedHelpers.setFloatField(param.getThisObject(), "mAlphaStiffness", scaleStiffness(XposedHelpers.getFloatField(param.getThisObject(), "mAlphaStiffness"), scale))
                try {
                    XposedHelpers.setFloatField(param.getThisObject(), "mRatioStiffness", scaleStiffness(XposedHelpers.getFloatField(param.getThisObject(), "mRatioStiffness"), scale))
                } catch (_: Throwable) {
                    XposedHelpers.setFloatField(param.getThisObject(), "mRadioStiffness", scaleStiffness(XposedHelpers.getFloatField(param.getThisObject(), "mRadioStiffness"), scale))
                }
            }
        }

        if (!ModuleHelper.hookAllMethodsSilently("com.miui.home.recents.util.RectFSpringAnim", lpparam.classLoader, "start", hook))
            ModuleHelper.hookAllMethods("com.miui.home.recents.util.RectFSpringAnim", lpparam.classLoader, "initAllAnimations", hook)
    }

    @JvmStatic
    fun NoUnlockAnimationHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.miui.launcher.utils.MiuiSettingsUtils", lpparam.classLoader, "isSystemAnimationOpen", HookerClassHelper.returnConstant(false))
    }

    @JvmStatic
    fun NoZoomAnimationHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.miui.home.recents.util.SpringAnimationUtils", lpparam.classLoader, "startShortcutMenuLayerFadeOutAnim", HookerClassHelper.DO_NOTHING)
        ModuleHelper.hookAllMethods("com.miui.home.recents.util.SpringAnimationUtils", lpparam.classLoader, "startShortcutMenuLayerFadeInAnim", HookerClassHelper.DO_NOTHING)
    }

    @JvmStatic
    fun UseOldLaunchAnimationHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.miui.home.recents.QuickstepAppTransitionManagerImpl", lpparam.classLoader, "hasControlRemoteAppTransitionPermission", HookerClassHelper.returnConstant(false))
    }

    @JvmStatic
    fun ReverseLauncherPortraitHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "onCreate", Bundle::class.java, object : MethodHook() {
            @SuppressLint("SourceLockedOrientationActivity")
            override fun after(param: AfterHookCallback) {
                val act = param.getThisObject() as? Activity ?: return
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        })
    }

    @JvmStatic
    fun RecentsBlurRatioHook(lpparam: PackageReadyParam) {
        val utilsClass = XposedHelpers.findClassIfExists("com.miui.home.launcher.common.BlurUtils", lpparam.classLoader)
        if (utilsClass == null) {
            XposedHelpers.log("RecentsBlurRatioHook", "Cannot find blur utility class")
            return
        }

        ModuleHelper.hookAllMethods(utilsClass, "fastBlurWhenEnterRecents", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mIsFromFsGesture = XposedHelpers.getBooleanField(param.getArgs()[1], "mIsFromFsGesture")
                if (!mIsFromFsGesture) {
                    val launcher = param.getArgs()[0] as? Activity ?: return
                    val blurRatio = MainModule.mPrefs.getInt("system_recents_blur", 100) / 100f
                    XposedHelpers.callStaticMethod(utilsClass, "fastBlur", blurRatio, launcher.window, param.getArgs()[2])
                    param.returnAndSkip(null)
                }
            }
        })
        ModuleHelper.hookAllMethods(utilsClass, "fastBlurWhenGestureResetTaskView", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                XposedHelpers.setAdditionalStaticField(utilsClass, "customBlurRatio", true)
            }
        })

        ModuleHelper.hookAllMethods(utilsClass, "fastBlur", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.getArgs().size == 3) {
                    if (XposedHelpers.getAdditionalStaticField(utilsClass, "customBlurRatio") != null) {
                        val blurRatio = MainModule.mPrefs.getInt("system_recents_blur", 100) / 100f
                        param.getArgs()[0] = blurRatio
                        XposedHelpers.removeAdditionalStaticField(utilsClass, "customBlurRatio")
                    }
                }
            }
        })
    }

    @JvmStatic
    fun DisableUnlockWallpaperScale(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.miwallpaper.manager.WallpaperServiceController", lpparam.classLoader, "noNeedDesktopWallpaperScaleAnim", HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun DisableLauncherWallpaperScale(lpparam: PackageReadyParam) {
        val wallpaperZoomManagerKtClass = XposedHelpers.findClassIfExists("com.miui.home.launcher.wallpaper.WallpaperZoomManagerKt", lpparam.classLoader)
        if (MainModule.mPrefs.getBoolean("launcher_disable_wallpaperscale")) {
            XposedHelpers.setStaticBooleanField(wallpaperZoomManagerKtClass, "ZOOM_ENABLED", false)
            ModuleHelper.findAndHookMethod("com.miui.home.recents.DimLayer", lpparam.classLoader, "isSupportDim", HookerClassHelper.returnConstant(false))
            return
        }
        ModuleHelper.hookAllMethods("com.miui.home.recents.OverviewState", lpparam.classLoader, "onStateEnabled", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (wallpaperZoomManagerKtClass != null) {
                    XposedHelpers.setStaticBooleanField(wallpaperZoomManagerKtClass, "ZOOM_ENABLED", false)
                }
            }
            override fun after(param: AfterHookCallback) {
                if (wallpaperZoomManagerKtClass != null) {
                    XposedHelpers.setStaticBooleanField(wallpaperZoomManagerKtClass, "ZOOM_ENABLED", true)
                }
            }
        })
    }
}
