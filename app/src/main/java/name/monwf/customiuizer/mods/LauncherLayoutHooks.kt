package name.monwf.customiuizer.mods

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
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
object LauncherLayoutHooks {

    @JvmStatic
    fun HideNavBarHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "loadScreenSize", Context::class.java, Resources::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                android.provider.Settings.Global.putInt((param.getArgs()[0] as? Context)?.contentResolver ?: return, "force_immersive_nav_bar", 1)
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.home.recents.views.RecentsContainer", lpparam.classLoader, "showLandscapeOverviewGestureView", Boolean::class.javaPrimitiveType, HookerClassHelper.DO_NOTHING)
        ModuleHelper.findAndHookMethod("com.miui.home.recents.NavStubView", lpparam.classLoader, "isMistakeTouch", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val navView = param.getThisObject() as? View ?: return
                var misTouch = false
                val setting = android.provider.Settings.Global.getInt(navView.context.contentResolver, "show_mistake_touch_toast", 1) != 0
                if (setting) {
                    val mIsShowStatusBar = XposedHelpers.getBooleanField(param.getThisObject(), "mIsShowStatusBar")
                    if (!mIsShowStatusBar) {
                        misTouch = XposedHelpers.callMethod(param.getThisObject(), "isLandScapeActually") as? Boolean ?: false
                    }
                }
                param.returnAndSkip(misTouch)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.recents.NavStubView", lpparam.classLoader, "onPointerEvent", android.view.MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mIsInFsMode = XposedHelpers.getBooleanField(param.getThisObject(), "mIsInFsMode")
                if (!mIsInFsMode) {
                    val motionEvent = param.getArgs()[0] as? android.view.MotionEvent ?: return
                    if (motionEvent.action == 0) {
                        XposedHelpers.setObjectField(param.getThisObject(), "mHideGestureLine", true)
                    }
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.home.recents.NavStubView", lpparam.classLoader, "updateScreenSize", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                XposedHelpers.setObjectField(param.getThisObject(), "mHideGestureLine", false)
            }
        })
    }

    @JvmStatic
    fun HideSeekPointsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.pageindicators.AllAppsIndicator", lpparam.classLoader, "shouldHide", HookerClassHelper.returnConstant(true))
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.pageindicators.AllAppsIndicator", lpparam.classLoader, "hideAllAppsArrow", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mLauncher = XposedHelpers.getObjectField(param.getThisObject(), "mLauncher")
                if (mLauncher == null) return
                val workspace = XposedHelpers.getObjectField(mLauncher, "mWorkspace") as? View ?: return
                val isInEditingMode = XposedHelpers.callMethod(workspace, "isInNormalEditingMode") as? Boolean ?: false
                val mContext = workspace.context
                var mHandler = XposedHelpers.getAdditionalInstanceField(workspace, "mHandlerEx") as? android.os.Handler
                if (mHandler == null) {
                    mHandler = android.os.Handler(mContext.mainLooper) { msg ->
                        val seekBar = msg.obj as? View
                        if (seekBar != null) {
                            seekBar.animate().alpha(0.0f).setDuration(300).withEndAction { seekBar.visibility = View.GONE }.start()
                        }
                        true
                    }
                    XposedHelpers.setAdditionalInstanceField(workspace, "mHandlerEx", mHandler)
                }
                if (mHandler.hasMessages(666)) mHandler.removeMessages(666)
                val mScreenSeekBar = XposedHelpers.getObjectField(param.getThisObject(), "mScreenIndicator") as? View ?: return
                mScreenSeekBar.animate().cancel()
                if (!isInEditingMode && MainModule.mPrefs.getBoolean("launcher_hideseekpoints_edit")) {
                    mScreenSeekBar.alpha = 0.0f
                    mScreenSeekBar.visibility = View.GONE
                    return
                }
                mScreenSeekBar.visibility = View.VISIBLE
                mScreenSeekBar.animate().alpha(1.0f).setDuration(300).start()
                if (!isInEditingMode) {
                    val msg = android.os.Message.obtain(mHandler, 666)
                    msg.obj = mScreenSeekBar
                    mHandler.sendMessageDelayed(msg, 600)
                }
            }
        })
    }

    @JvmStatic
    fun InfiniteScrollHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ScreenView", lpparam.classLoader, "getSnapToScreenIndex", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArgs()[0] != param.getResult()) return
                val screenCount = (XposedHelpers.callMethod(param.getThisObject(), "getScreenCount") as? Int) ?: 0
                if ((param.getArgs()[2] as? Int ?: 0) == -1 && (param.getArgs()[0] as? Int ?: 0) == 0)
                    param.setResult(screenCount)
                else if ((param.getArgs()[2] as? Int ?: 0) == 1 && (param.getArgs()[0] as? Int ?: 0) == screenCount - 1)
                    param.setResult(0)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ScreenView", lpparam.classLoader, "getSnapUnitIndex", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mCurrentScreenIndex = XposedHelpers.getIntField(param.getThisObject(), if (lpparam.packageName == "com.miui.home") "mCurrentScreenIndex" else "mCurrentScreen")
                if (mCurrentScreenIndex != (param.getResult() as? Int ?: 0)) return
                val screenCount = (XposedHelpers.callMethod(param.getThisObject(), "getScreenCount") as? Int) ?: 0
                if ((param.getResult() as? Int ?: 0) == 0)
                    param.setResult(screenCount)
                else if ((param.getResult() as? Int ?: 0) == screenCount - 1)
                    param.setResult(0)
            }
        })
    }

    @JvmStatic
    fun UnlockGridsRes() {
        MainModule.resHooks.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x", 3)
        MainModule.resHooks.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y", 4)
        MainModule.resHooks.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x_min", 3)
        MainModule.resHooks.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y_min", 4)
        MainModule.resHooks.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x_max", 8)
        MainModule.resHooks.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y_max", 10)
    }

    @JvmStatic
    fun UnlockGridsHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethodsSilently("com.miui.home.launcher.compat.LauncherCellCountCompatDevice", lpparam.classLoader, "shouldUseDeviceValue", HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.compat.LauncherCellCountCompatDeviceFold", lpparam.classLoader, "shouldUseDeviceValue", Context::class.java, Int::class.javaPrimitiveType, HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("com.miui.home.settings.MiuiHomeSettings", lpparam.classLoader, "onCreatePreferences", android.os.Bundle::class.java, String::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.callMethod(XposedHelpers.getObjectField(param.getThisObject(), "mScreenCellsConfig"), "setVisible", true)
            }
        })
        val deviceConfigClass = XposedHelpers.findClass("com.miui.home.launcher.DeviceConfig", lpparam.classLoader)
        ModuleHelper.findAndHookMethod(deviceConfigClass, "loadCellsCountConfig", Context::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val sCellCountY = XposedHelpers.getStaticIntField(deviceConfigClass, "sCellCountY")
                if (sCellCountY > 6) {
                    val cellHeight = XposedHelpers.callStaticMethod(deviceConfigClass, "getCellHeight") as? Int ?: 0
                    XposedHelpers.setStaticObjectField(deviceConfigClass, "sFolderCellHeight", cellHeight)
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ScreenUtils", lpparam.classLoader, "getScreenCellsSizeOptions", Context::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val arrayList = ArrayList<CharSequence>()
                var cellCountXMin = 3
                val cellCountXMax = 8
                val cellCountYMin = 4
                val cellCountYMax = 10
                while (cellCountXMin <= cellCountXMax) {
                    for (i in cellCountYMin..cellCountYMax) {
                        arrayList.add("${cellCountXMin}x$i")
                    }
                    cellCountXMin++
                }
                param.returnAndSkip(arrayList)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.compat.LauncherCellCountCompatNoWord", lpparam.classLoader, "setLoadResCellConfig", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.getArgs()[0] = true
            }
        })

        ModuleHelper.hookAllMethods("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "isCellSizeChangedByTheme", object : MethodHook() {
            private var nowordHook: HookerClassHelper.CustomMethodUnhooker? = null
            override fun before(param: BeforeHookCallback) {
                nowordHook = ModuleHelper.findAndHookMethod("com.miui.home.launcher.common.Utilities", lpparam.classLoader, "isNoWordModel", HookerClassHelper.returnConstant(false))
            }
            override fun after(param: AfterHookCallback) {
                nowordHook?.unhook()
            }
        })
    }

    @JvmStatic
    fun HorizontalSpacingRes() {
        val opt = MainModule.mPrefs.getInt("launcher_horizmargin", 0) - 21
        MainModule.resHooks.setDensityReplacement("com.miui.home", "dimen", "workspace_cell_padding_side", opt.toFloat())
        MainModule.resHooks.setDensityReplacement("com.miui.home", "dimen", "workspace_cell_padding_side_no_word", opt.toFloat())
        MainModule.resHooks.setDensityReplacement("com.miui.home", "dimen", "workspace_cell_padding_side_rotatable", opt.toFloat())
        MainModule.resHooks.setDensityReplacement("com.mi.android.globallauncher", "dimen", "workspace_cell_padding_side", opt.toFloat())
    }

    @JvmStatic
    fun IndicatorHeightRes() {
        val opt = MainModule.mPrefs.getInt("launcher_indicatorheight", 9)
        MainModule.resHooks.setDensityReplacement("com.miui.home", "dimen", "slide_bar_height", opt.toFloat())
        MainModule.resHooks.setDensityReplacement("com.mi.android.globallauncher", "dimen", "slide_bar_height", opt.toFloat())
    }

    @JvmStatic
    fun ShowHotseatTitlesRes() {
        MainModule.resHooks.setObjectReplacement("com.miui.home", "bool", "config_hide_hotseats_app_title", false)
        MainModule.resHooks.setObjectReplacement("com.mi.android.globallauncher", "bool", "config_hide_hotseats_app_title", false)
    }

    @JvmStatic
    fun DockMarginTopHook(lpparam: PackageReadyParam) {
        val opt = MainModule.mPrefs.getInt("launcher_dock_topmargin", 0)
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "calcHotSeatsMarginTop", Context::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(Math.round(Helpers.dp2px(opt.toFloat())))
            }
        })
    }

    @JvmStatic
    fun DockMarginBottomHook(lpparam: PackageReadyParam) {
        val opt = MainModule.mPrefs.getInt("launcher_dock_bottommargin", 0)
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "calcHotSeatsMarginBottom", Context::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(Math.round(Helpers.dp2px(opt.toFloat())))
            }
        })
    }

    @JvmStatic
    fun WorkspaceCellPaddingTopHook(lpparam: PackageReadyParam) {
        val opt = MainModule.mPrefs.getInt("launcher_topmargin", 0) - 21
        val hook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(Math.round(Helpers.dp2px(opt.toFloat())))
            }
        }

        val newLauncher = ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "getWorkspaceCellPaddingTop", Context::class.java, hook)
        if (!newLauncher) {
            ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "getWorkspaceCellPaddingTop", hook)
        }
    }

    @JvmStatic
    fun IndicatorMarginTopHook(lpparam: PackageReadyParam) {
        val opt = MainModule.mPrefs.getInt("launcher_indicator_topmargin", 0) - 21
        MainModule.resHooks.setDensityReplacement("com.miui.home", "dimen", "slide_bar_margin_top", opt.toFloat())
        MainModule.resHooks.setDensityReplacement("com.mi.android.globallauncher", "dimen", "slide_bar_margin_top", opt.toFloat())
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.util.DimenUtils1X", lpparam.classLoader, "getDimensionPixelSize", Context::class.java, String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val resKey = param.getArgs()[1] as? String ?: return
                if ("slide_bar_margin_top" == resKey) {
                    param.returnAndSkip(Math.round(Helpers.dp2px(opt.toFloat())))
                }
            }
        })
    }

    @JvmStatic
    fun HorizontalWidgetSpacingHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "getMiuiWidgetSizeSpec", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArgs().size < 4) return
                val spec = param.getResult() as? Long ?: return
                var width = (spec shr 32).toInt()
                var height = (spec - ((spec shr 32) shl 32)).toInt()
                val opt = Math.round((MainModule.mPrefs.getInt("launcher_horizwidgetmargin", 0) - 21) * Resources.getSystem().displayMetrics.density) * 2
                width -= opt
                param.setResult((width.toLong() shl 32) or height.toLong())
            }
        })

        ModuleHelper.hookAllMethods("com.miui.home.launcher.MIUIWidgetUtil", lpparam.classLoader, "getMiuiWidgetPadding", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                param.setResult(Rect())
            }
        })
    }

    @JvmStatic
    fun NoWidgetOnlyHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.CellLayout", lpparam.classLoader, "setScreenType", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.getArgs()[0] = 0
            }
        })
    }

    @JvmStatic
    fun MaxHotseatIconsCountHook(lpparam: PackageReadyParam) {
        val methodName = if (lpparam.packageName == "com.mi.android.globallauncher") "getHotseatCount" else "getHotseatMaxCount"
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, methodName, HookerClassHelper.returnConstant(666))
    }

    @JvmStatic
    fun ResizableWidgetsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("android.appwidget.AppWidgetHostView", lpparam.classLoader, "getAppWidgetInfo", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val widgetInfo = param.getResult() as? android.appwidget.AppWidgetProviderInfo ?: return
                widgetInfo.resizeMode = android.appwidget.AppWidgetProviderInfo.RESIZE_VERTICAL or android.appwidget.AppWidgetProviderInfo.RESIZE_HORIZONTAL
                widgetInfo.minHeight = 0
                widgetInfo.minWidth = 0
                widgetInfo.minResizeHeight = 0
                widgetInfo.minResizeWidth = 0
                param.setResult(widgetInfo)
            }
        })
    }
}
