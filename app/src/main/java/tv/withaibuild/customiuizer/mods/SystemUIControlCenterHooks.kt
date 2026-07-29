package tv.withaibuild.customiuizer.mods

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.pm.ApplicationInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.UserHandle
import android.util.TypedValue
import android.telephony.SubscriptionManager
import android.text.TextUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.SparseIntArray
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import miui.process.ForegroundInfo
import miui.process.ProcessManager
import miui.telephony.TelephonyManager
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks
import tv.withaibuild.customiuizer.mods.utils.StepCounterController
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.Helpers
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
object SystemUIControlCenterHooks {

    private const val STATUS_BAR_CLS = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
    private var pluginLoader: ClassLoader? = null
    private var scaledTileWidthDim = -1f
    private var blurCollapsed = 0.0f
    private var blurExpanded = 0.0f
    private var notifVolumeOnResId = 0
    private var notifVolumeOffResId = 0

    private var isSlidingStart = false
    private var isSliding = false
    private var tapStartX = 0f
    private var tapStartY = 0f
    private var tapStartPointers = 1f
    private var tapStartBrightness = 0f
    private var topMinimumBacklight = 0.0f
    private var topMaximumBacklight = 1.0f
    private var currentTouchX = 0f
    private var currentTouchTime = 0L
    private var currentDownTime = 0L
    private var currentDownX = 0f

    @JvmStatic
    fun MIUIVolumeDialogHook(lpparam: PackageReadyParam) {
        val pluginLoaderClass = "com.android.systemui.shared.plugins.PluginInstance\$Factory"
        ModuleHelper.hookAllMethods(pluginLoaderClass, lpparam.classLoader, "getClassLoader", object : MethodHook() {
            private var isHooked = false
            override fun after(param: AfterHookCallback) {
                val appInfo = param.getArgs()[0] as? ApplicationInfo ?: return
                if ("miui.systemui.plugin" == appInfo.packageName && !isHooked) {
                    isHooked = true
                    if (pluginLoader == null) {
                        pluginLoader = param.getResult() as? ClassLoader
                    }
                    if (MainModule.mPrefs.getBoolean("system_separatevolume") && MainModule.mPrefs.getBoolean("system_separatevolume_slider")) {
                        pluginLoader?.let { SingleNotificationSliderHook(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_nosilentvibrate")) {
                        pluginLoader?.let { ModuleHelper.hookAllMethods("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", it, "vibrateH", HookerClassHelper.DO_NOTHING) }
                    }
                    if (MainModule.mPrefs.getInt("system_volumedialogdelay_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumedialogdelay_expanded", 0) > 0) {
                        pluginLoader?.let { VolumeDialogAutohideDelayHook(it) }
                    }
                    if (MainModule.mPrefs.getInt("system_volumeblur_collapsed", 0) > 0 || MainModule.mPrefs.getInt("system_volumeblur_expanded", 0) > 0) {
                        pluginLoader?.let { BlurVolumeDialogBackgroundHook(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_volumebar_blur_mtk")) {
                        pluginLoader?.let { BlurMTKVolumeBarHook(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_qs_force_systemfonts")) {
                        pluginLoader?.let {
                            ModuleHelper.findAndHookMethod("miui.systemui.util.SystemUIResourcesHelperImpl", it, "getBoolean", String::class.java, object : MethodHook() {
                                override fun before(param2: BeforeHookCallback) {
                                    val key = param2.getArgs()[0] as? String ?: return
                                    if (key == "header_big_time_use_system_font") param2.returnAndSkip(java.lang.Boolean.TRUE)
                                }
                            })
                        }
                    }
                    if (MainModule.mPrefs.getBoolean("system_qsnolabels")) {
                        pluginLoader?.let { HideCCLabelsHook(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_volumetimer")) {
                        pluginLoader?.let { VolumeTimerValuesRes(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_cc_tile_roundedrect")) {
                        pluginLoader?.let { CCTileCornerHook(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_cc_volume_showpct")) {
                        pluginLoader?.let { ShowVolumePctHook(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_cc_hidedate")) {
                        pluginLoader?.let { HideCCDateView(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_cc_clocktweak")) {
                        pluginLoader?.let { initCCClockStyle(it) }
                    }
                    if (MainModule.mPrefs.getBoolean("system_cc_hide_shortcuticons")) {
                        pluginLoader?.let { hideCCSettingsTilesEdit(it) }
                    }
                    if (MainModule.mPrefs.getStringAsInt("system_cc_bluetooth_tile_style", 1) > 1) {
                        pluginLoader?.let { BluetoothTileStyleHook(it) }
                    }
                }
            }
        })
    }

    @JvmStatic
    fun VolumeDialogAutohideDelayHook(classLoader: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", classLoader, "computeTimeoutH", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mHovering = XposedHelpers.getBooleanField(param.getThisObject(), "mHovering")
                if (mHovering) {
                    param.returnAndSkip(16000)
                    return
                }
                val mSafetyWarning = try {
                    XposedHelpers.getObjectField(param.getThisObject(), "mIsSafetyShowing") as? Boolean
                        ?: XposedHelpers.getObjectField(param.getThisObject(), "mSafetyWarning") as? Boolean
                        ?: false
                } catch (e: Throwable) {
                    XposedHelpers.getObjectField(param.getThisObject(), "mSafetyWarning") as? Boolean ?: false
                }
                if (mSafetyWarning) {
                    val opt = MainModule.mPrefs.getInt("system_volumedialogdelay_expanded", 0)
                    param.returnAndSkip(if (opt > 0) opt else 5000)
                    return
                }
                val mExpanded = XposedHelpers.getBooleanField(param.getThisObject(), "mExpanded")
                val opt = MainModule.mPrefs.getInt(if (mExpanded) "system_volumedialogdelay_expanded" else "system_volumedialogdelay_collapsed", 0)
                if (opt > 0) param.returnAndSkip(opt)
            }
        })
    }

    @JvmStatic
    fun BlurVolumeDialogBackgroundHook(classLoader: ClassLoader) {
        MainModule.resHooks.setObjectReplacement("miui.systemui.plugin", "fraction", "miui_volume_dim_behind_collapsed", 0f)
        MainModule.resHooks.setObjectReplacement("miui.systemui.plugin", "fraction", "miui_volume_dim_behind_expanded", 0f)

        ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", classLoader, "updateDialogWindowH", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mExpanded = XposedHelpers.getBooleanField(param.getThisObject(), "mExpanded")
                var blurRatio = blurCollapsed
                val isVisible = param.getArgs()[0] as? Boolean ?: false
                if (mExpanded && !isVisible) {
                    blurRatio = blurExpanded
                }
                if (!mExpanded && blurCollapsed > 0.001f) {
                    val mWindow = XposedHelpers.getObjectField(param.getThisObject(), "mWindow") as? Window
                    mWindow?.clearFlags(8)
                }
                if (mExpanded) {
                    XposedHelpers.callMethod(param.getThisObject(), "startBlurAnim", 0f, blurRatio, 0)
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", classLoader, "showH", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (blurCollapsed > 0.001f) {
                    val mWindow = XposedHelpers.getObjectField(param.getThisObject(), "mWindow") as? Window
                    mWindow?.clearFlags(8)
                    XposedHelpers.callMethod(param.getThisObject(), "startBlurAnim", 0f, blurCollapsed, 0)
                }
            }
        })
        ModuleHelper.hookAllMethods("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", classLoader, "initDialog", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                blurCollapsed = MainModule.mPrefs.getInt("system_volumeblur_collapsed", 0) / 100f
                blurExpanded = MainModule.mPrefs.getInt("system_volumeblur_expanded", 0) / 100f
                ModuleHelper.observePreferenceChange(object : ModuleHelper.PreferenceObserver {
                    override fun onChange(key: String) {
                        try {
                            if (key == "system_volumeblur_collapsed") blurCollapsed = MainModule.mPrefs.getInt(key, 0) / 100f
                            if (key == "system_volumeblur_expanded") blurExpanded = MainModule.mPrefs.getInt(key, 0) / 100f
                        } catch (t: Throwable) {
                            XposedHelpers.log(t)
                        }
                    }
                })
            }
        })
    }

    @JvmStatic
    fun BlurMTKVolumeBarHook(classLoader: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.Util", classLoader, "isSupportBlurS", HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun SingleNotificationSliderHook(classLoader: ClassLoader) {
        val UtilCls = XposedHelpers.findClassIfExists("com.android.systemui.miui.volume.Util", classLoader)
        var newSingleSlider = false
        if (UtilCls != null) {
            val hasFeature = ModuleHelper.getStaticObjectFieldSilently(UtilCls, "sIsNotificationSingle")
            newSingleSlider = !ModuleHelper.NOT_EXIST_SYMBOL.equals(hasFeature)
        }
        if (newSingleSlider) {
            XposedHelpers.setStaticBooleanField(UtilCls, "sIsNotificationSingle", true)
            ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.Util", classLoader, "isNotificationSingle", Context::class.java, Int::class.javaPrimitiveType, HookerClassHelper.returnConstant(true))
        } else {
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_content_width_expanded", R.dimen.miui_volume_content_width_expanded)
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_ringer_layout_width_expanded", R.dimen.miui_volume_ringer_layout_width_expanded)
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_width_expanded", R.dimen.miui_volume_column_width_expanded)
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_margin_horizontal_expanded", R.dimen.miui_volume_column_margin_horizontal_expanded)
            notifVolumeOnResId = MainModule.resHooks.addResource("ic_miui_volume_notification", R.drawable.ic_miui_volume_notification)
            notifVolumeOffResId = MainModule.resHooks.addResource("ic_miui_volume_notification_mute", R.drawable.ic_miui_volume_notification_mute)
            ModuleHelper.hookAllMethods("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", classLoader, "addColumn", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    if (param.getArgs().size != 4) return
                    val streamType = param.getArgs()[0] as? Int ?: return
                    if (streamType == 4) {
                        XposedHelpers.callMethod(param.getThisObject(), "addColumn", 5, notifVolumeOnResId, notifVolumeOffResId, true, false)
                    }
                }
            })
        }
    }

    @JvmStatic
    fun SystemCCGridHook(lpparam: PackageReadyParam) {
        val cols = MainModule.mPrefs.getInt("system_ccgridcolumns", 4)
        val rows = MainModule.mPrefs.getInt("system_ccgridrows", 4)
        if (cols > 4) {
            MainModule.resHooks.setObjectReplacement(lpparam.packageName, "dimen", "qs_control_tiles_columns", cols)
        }

        ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", object : MethodHook() {
            private var isHooked = false
            override fun after(param: AfterHookCallback) {
                if (!isHooked) {
                    isHooked = true
                    val mContext = XposedHelpers.callMethod(param.getThisObject(), "getApplicationContext") as? Context ?: return
                    val res = mContext.resources
                    val density = res.displayMetrics.density
                    val tileWidthResId = res.getIdentifier("qs_control_center_tile_width", "dimen", "com.android.systemui")
                    var tileWidthDim = res.getDimension(tileWidthResId)
                    if (cols > 4) {
                        tileWidthDim /= density
                        scaledTileWidthDim = tileWidthDim * 4 / cols
                        MainModule.resHooks.setDensityReplacement(lpparam.packageName, "dimen", "qs_control_center_tile_width", scaledTileWidthDim)
                        MainModule.resHooks.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_control_center_tile_width", scaledTileWidthDim)
                        MainModule.resHooks.setDensityReplacement(lpparam.packageName, "dimen", "qs_control_tile_icon_bg_size", scaledTileWidthDim)
                        MainModule.resHooks.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_control_tile_icon_bg_size", scaledTileWidthDim)
                        MainModule.resHooks.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85f)
                    }
                }
            }
        })

        val pluginLoaderClass = "com.android.systemui.shared.plugins.PluginInstance\$Factory"
        ModuleHelper.hookAllMethods(pluginLoaderClass, lpparam.classLoader, "getClassLoader", object : MethodHook() {
            private var isHooked = false
            override fun after(param: AfterHookCallback) {
                val appInfo = param.getArgs()[0] as? ApplicationInfo ?: return
                if ("miui.systemui.plugin" == appInfo.packageName && !isHooked) {
                    isHooked = true
                    if (pluginLoader == null) {
                        pluginLoader = param.getResult() as? ClassLoader
                    }
                    if (cols > 4) {
                        pluginLoader?.let {
                            ModuleHelper.findAndHookConstructor("miui.systemui.controlcenter.qs.QSPager", it, Context::class.java, AttributeSet::class.java, object : MethodHook() {
                                override fun after(param2: AfterHookCallback) {
                                    XposedHelpers.setObjectField(param2.getThisObject(), "columns", cols)
                                }
                            })
                            if (!MainModule.mPrefs.getBoolean("system_qsnolabels")) {
                                ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.StandardTileView", it, "createLabel", Boolean::class.javaPrimitiveType, object : MethodHook() {
                                    override fun after(param2: AfterHookCallback) {
                                        val label = XposedHelpers.getObjectField(param2.getThisObject(), "label") as? TextView ?: return
                                        label.maxLines = 1
                                        label.setSingleLine(true)
                                        label.ellipsize = TextUtils.TruncateAt.MARQUEE
                                        label.marqueeRepeatLimit = 0
                                        val labelContainer = XposedHelpers.getObjectField(param2.getThisObject(), "labelContainer") as? View
                                        labelContainer?.setPadding(4, 0, 4, 0)
                                    }
                                })
                            }
                        }
                    }
                    if (rows != 4) {
                        pluginLoader?.let {
                            ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.qs.QSPager", it, "distributeTiles", object : MethodHook() {
                                @Suppress("UNCHECKED_CAST")
                                override fun after(param2: AfterHookCallback) {
                                    val collapse = XposedHelpers.getObjectField(param2.getThisObject(), "collapse") as? Boolean ?: return
                                    if (collapse) {
                                        val pages = XposedHelpers.getObjectField(param2.getThisObject(), "pages") as? ArrayList<Any> ?: return
                                        for (tileLayoutImpl in pages) {
                                            XposedHelpers.callMethod(tileLayoutImpl, "removeTiles")
                                        }
                                        val pageTiles = ArrayList<Any>()
                                        var currentRow = 2
                                        val records = XposedHelpers.getObjectField(param2.getThisObject(), "records") as? ArrayList<*> ?: return
                                        val it2 = records.iterator()
                                        var i3 = 0
                                        var pageNow = 0
                                        val bigHeader = XposedHelpers.getObjectField(param2.getThisObject(), "header")
                                        while (it2.hasNext()) {
                                            val tileRecord = it2.next() ?: continue
                                            pageTiles.add(tileRecord)
                                            i3++
                                            if (i3 >= cols) {
                                                currentRow++
                                                i3 = 0
                                            }
                                            if (currentRow >= rows || !it2.hasNext()) {
                                                XposedHelpers.callMethod(pages[pageNow], "setTiles", pageTiles, if (pageNow == 0) bigHeader else null)
                                                pageTiles.clear()
                                                val totalRows = XposedHelpers.getObjectField(param2.getThisObject(), "rows") as? Int ?: 0
                                                if (currentRow > totalRows) {
                                                    XposedHelpers.setObjectField(param2.getThisObject(), "rows", currentRow)
                                                }
                                                if (it2.hasNext()) {
                                                    pageNow++
                                                    currentRow = 0
                                                }
                                            }
                                        }
                                        val it3 = pages.iterator()
                                        while (it3.hasNext()) {
                                            val next2 = it3.next()
                                            val isEmpty = XposedHelpers.callMethod(next2, "isEmpty") as? Boolean ?: false
                                            if (isEmpty) it3.remove()
                                        }
                                        val pageIndicator = XposedHelpers.getObjectField(param2.getThisObject(), "pageIndicator")
                                        if (pageIndicator != null) {
                                            XposedHelpers.callMethod(pageIndicator, "setNumPages", pages.size)
                                        }
                                        val adapter = XposedHelpers.getObjectField(param2.getThisObject(), "adapter")
                                        XposedHelpers.callMethod(param2.getThisObject(), "setAdapter", adapter)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        })
    }

    @JvmStatic
    fun QQSGridRes() {
        val cols = MainModule.mPrefs.getInt("system_qqsgridcolumns", 2)
        val colsResId = when (cols) {
            3 -> R.integer.quick_quick_settings_num_rows_3
            4 -> R.integer.quick_quick_settings_num_rows_4
            5 -> R.integer.quick_quick_settings_num_rows_5
            6 -> R.integer.quick_quick_settings_num_rows_6
            7 -> R.integer.quick_quick_settings_num_rows_7
            else -> R.integer.quick_quick_settings_num_rows_5
        }
        MainModule.resHooks.setResReplacement("com.android.systemui", "integer", "quick_settings_qqs_count", colsResId)
    }

    @JvmStatic
    fun QSGridRes() {
        val cols = MainModule.mPrefs.getInt("system_qsgridcolumns", 2)
        val rows = MainModule.mPrefs.getInt("system_qsgridrows", 1)
        val colsRes = when (cols) {
            3 -> R.integer.quick_settings_num_columns_3
            4 -> R.integer.quick_settings_num_columns_4
            5 -> R.integer.quick_settings_num_columns_5
            6 -> R.integer.quick_settings_num_columns_6
            7 -> R.integer.quick_settings_num_columns_7
            else -> R.integer.quick_settings_num_columns_3
        }
        val rowsRes = when (rows) {
            2 -> R.integer.quick_settings_num_rows_2
            3 -> R.integer.quick_settings_num_rows_3
            4 -> R.integer.quick_settings_num_rows_4
            5 -> R.integer.quick_settings_num_rows_5
            else -> R.integer.quick_settings_num_rows_4
        }
        if (cols > 2) MainModule.resHooks.setResReplacement("com.android.systemui", "integer", "quick_settings_num_columns", colsRes)
        if (rows > 1) MainModule.resHooks.setResReplacement("com.android.systemui", "integer", "quick_settings_num_rows", rowsRes)
    }

    @JvmStatic
    fun QSGridLabelsHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.qs.MiuiTileLayout", lpparam.classLoader, "addTile", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val viewGroup = param.getThisObject() as? ViewGroup ?: return
                updateLabelsVisibility(param.getArgs()[0], XposedHelpers.getIntField(param.getThisObject(), "mRows"), viewGroup.resources.configuration.orientation)
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.qs.MiuiPagedTileLayout", lpparam.classLoader, "addTile", object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun before(param: BeforeHookCallback) {
                val viewGroup = param.getThisObject() as? ViewGroup ?: return
                val mPages = XposedHelpers.getObjectField(param.getThisObject(), "mPages") as? ArrayList<Any> ?: return
                var mRows = 0
                if (mPages.size > 0) mRows = XposedHelpers.getIntField(mPages[0], "mRows")
                updateLabelsVisibility(param.getArgs()[0], mRows, viewGroup.resources.configuration.orientation)
            }
        })

        val rows = MainModule.mPrefs.getInt("system_qsgridrows", 1)
        if (rows == 4) {
            ModuleHelper.findAndHookMethod("com.android.systemui.qs.tileimpl.MiuiQSTileView", lpparam.classLoader, "createLabel", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mLabelContainer = XposedHelpers.getObjectField(param.getThisObject(), "mLabelContainer") as? ViewGroup ?: return
                    mLabelContainer.setPadding(
                        mLabelContainer.paddingLeft,
                        Math.round(mLabelContainer.resources.displayMetrics.density * 2),
                        mLabelContainer.paddingRight,
                        mLabelContainer.paddingBottom
                    )
                }
            })
        }
    }

    private fun updateLabelsVisibility(mRecord: Any?, mRows: Int, orientation: Int) {
        if (mRecord == null) return
        val tileView = XposedHelpers.getObjectField(mRecord, "tileView")
        if (tileView != null) {
            var mLabelContainer: ViewGroup? = null
            try {
                mLabelContainer = XposedHelpers.getObjectField(tileView, "mLabelContainer") as? ViewGroup
            } catch (_: Throwable) {}
            if (mLabelContainer != null) {
                mLabelContainer.visibility = if (
                    MainModule.mPrefs.getBoolean("system_qsnolabels") ||
                    (orientation == Configuration.ORIENTATION_PORTRAIT && mRows >= 5) ||
                    (orientation == Configuration.ORIENTATION_LANDSCAPE && mRows >= 3)
                ) View.GONE else View.VISIBLE
            }
        }
    }

    private fun HideCCLabelsHook(pluginLoader: ClassLoader) {
        MainModule.resHooks.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85f)
        val QSController = XposedHelpers.findClassIfExists("miui.systemui.controlcenter.qs.tileview.StandardTileView", pluginLoader) ?: return
        ModuleHelper.hookAllMethods(QSController, "init", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.getArgs().size != 1) return
                val mLabelContainer = XposedHelpers.getObjectField(param.getThisObject(), "labelContainer") as? View ?: return
                mLabelContainer.visibility = View.GONE
            }
        })
    }

    @JvmStatic
    fun VolumeTimerValuesRes(pluginLoader: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeTimerDrawableHelper", pluginLoader, "initTimerString", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mTimeSegmentTitle = Array<String>(11) { "" }
                val timerOffId = mContext.resources.getIdentifier("timer_off", "string", "miui.systemui.plugin")
                val minuteId = mContext.resources.getIdentifier("timer_30_minutes", "string", "miui.systemui.plugin")
                val hourId = mContext.resources.getIdentifier("timer_1_hour", "string", "miui.systemui.plugin")
                mTimeSegmentTitle[0] = mContext.resources.getString(timerOffId)
                mTimeSegmentTitle[1] = mContext.resources.getString(minuteId, 30)
                mTimeSegmentTitle[2] = mContext.resources.getString(hourId, 1)
                mTimeSegmentTitle[3] = mContext.resources.getString(hourId, 2)
                mTimeSegmentTitle[4] = mContext.resources.getString(hourId, 3)
                mTimeSegmentTitle[5] = mContext.resources.getString(hourId, 4)
                mTimeSegmentTitle[6] = mContext.resources.getString(hourId, 5)
                mTimeSegmentTitle[7] = mContext.resources.getString(hourId, 6)
                mTimeSegmentTitle[8] = mContext.resources.getString(hourId, 8)
                mTimeSegmentTitle[9] = mContext.resources.getString(hourId, 10)
                mTimeSegmentTitle[10] = mContext.resources.getString(hourId, 12)
                XposedHelpers.setObjectField(param.getThisObject(), "mTimeSegmentTitle", mTimeSegmentTitle)
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.TimerItem", pluginLoader, "getTimePos", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val timer = XposedHelpers.getObjectField(param.getThisObject(), "mTimerTime")
                val halfTimerWidth = (XposedHelpers.callMethod(timer, "getWidth") as? Int ?: 0) / 2.0f
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mTimerSeekbarWidth = ModuleHelper.getObjectFieldSilently(param.getThisObject(), "mTimerSeekbarWidth")
                val seekbarWidthResId = if (ModuleHelper.NOT_EXIST_SYMBOL.equals(mTimerSeekbarWidth)) {
                    mContext.resources.getIdentifier("miui_volume_timer_seelbar_width", "dimen", "miui.systemui.plugin")
                } else {
                    mTimerSeekbarWidth as? Int ?: 0
                }
                val mTimerSeekbarMarginLeft = mContext.resources.getIdentifier("miui_volume_timer_seekbar_margin_left", "dimen", "miui.systemui.plugin")
                val seekWidth = mContext.resources.getDimension(seekbarWidthResId)
                val marginLeft = mContext.resources.getDimensionPixelSize(mTimerSeekbarMarginLeft)
                val seg = XposedHelpers.getObjectField(param.getThisObject(), "mDeterminedSegment") as? Int ?: 0
                param.returnAndSkip(seekWidth / 10 * seg + marginLeft - halfTimerWidth)
            }
        })

        val segHook = object : MethodHook() {
            var prevSeg = 0
            override fun before(param: BeforeHookCallback) {
                prevSeg = XposedHelpers.getIntField(param.getThisObject(), "mCurrentSegment")
                if (prevSeg < 3 || (prevSeg == 3 && XposedHelpers.getIntField(param.getThisObject(), "mDeterminedSegment") == 3)) {
                    XposedHelpers.setIntField(param.getThisObject(), "mCurrentSegment", 0)
                }
            }
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setIntField(param.getThisObject(), "mCurrentSegment", prevSeg)
            }
        }

        ModuleHelper.findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeTimerDrawableHelper", pluginLoader, "updateDrawables", segHook)
    }

    @JvmStatic
    fun CCTileCornerHook(pluginLoader: ClassLoader) {
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.ExpandableIconView", pluginLoader, "setCornerRadius", Float::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = XposedHelpers.callMethod(param.getThisObject(), "getPluginContext") as? Context ?: return
                var radius = 18f
                if (scaledTileWidthDim > 0) {
                    radius *= scaledTileWidthDim / 65
                }
                param.getArgs()[0] = mContext.resources.displayMetrics.density * radius
            }
        })

        ModuleHelper.findAndHookMethod("miui.systemui.dagger.PluginComponentFactory", pluginLoader, "create", Context::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = param.getArgs()[0] as? Context ?: return
                val enabledTileBackgroundResId = mContext.resources.getIdentifier("qs_background_enabled", "drawable", "miui.systemui.plugin")
                val enabledTileColorResId = mContext.resources.getIdentifier("qs_enabled_color", "color", "miui.systemui.plugin")
                val tintColor = mContext.resources.getColor(enabledTileColorResId, null)
                val modRes = ModuleHelper.getModuleRes(mContext)
                val imgHook = object : MethodHook() {
                    override fun before(param2: BeforeHookCallback) {
                        val resId = param2.getArgs()[0] as? Int ?: return
                        if (resId == enabledTileBackgroundResId && resId != 0) {
                            val enableTile = modRes.getDrawable(R.drawable.ic_qs_tile_bg_enabled, null)
                            enableTile?.setTint(tintColor)
                            param2.returnAndSkip(enableTile)
                        }
                    }
                }
                ModuleHelper.findAndHookMethod("android.content.res.Resources", pluginLoader, "getDrawable", Int::class.javaPrimitiveType, imgHook)
                ModuleHelper.findAndHookMethod("android.content.res.Resources.Theme", pluginLoader, "getDrawable", Int::class.javaPrimitiveType, imgHook)
            }
        })
    }

    @JvmStatic
    @SuppressLint("WrongConstant")
    fun StatusBarGesturesHook(lpparam: PackageReadyParam) {
        val hook = object : MethodHook() {
            var mBrightnessController: Any? = null
            var sbHeight = -1

            @SuppressLint("SetTextI18n")
            override fun before(param: BeforeHookCallback) {
                val clsName = param.getThisObject()?.javaClass?.simpleName ?: ""
                val isInControlCenter = clsName == "ControlPanelWindowView" || clsName == "ControlCenterWindowViewImpl"
                if (isInControlCenter) {
                    if (param.getArgs().size == 2 && param.getArgs()[1] as? Boolean == true) {
                        return
                    }
                    val statusBarStateController = XposedHelpers.getObjectField(param.getThisObject(), "statusBarStateController")
                    val state = XposedHelpers.callMethod(statusBarStateController, "getState") as? Int ?: 0
                    if (state == 1 || state == 2) return
                }
                val mContext: Context = if (isInControlCenter) {
                    (param.getThisObject() as? View)?.context ?: return
                } else {
                    XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                }
                val res = mContext.resources
                if (sbHeight == -1) {
                    sbHeight = res.getDimensionPixelSize(res.getIdentifier("status_bar_height", "dimen", "android"))
                }
                val event = param.getArgs()[0] as? MotionEvent ?: return
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        tapStartX = event.x
                        tapStartY = event.y
                        isSlidingStart = if (isInControlCenter) tapStartY <= sbHeight else !XposedHelpers.getBooleanField(param.getThisObject(), "mPanelExpanded")
                        tapStartPointers = 1f
                        if (mBrightnessController == null) {
                            val mControlCenterController: Any? = if (isInControlCenter) {
                                XposedHelpers.getObjectField(param.getThisObject(), "controlCenterController")
                            } else {
                                XposedHelpers.callStaticMethod(
                                    XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader),
                                    "get",
                                    XposedHelpers.findClassIfExists("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl", lpparam.classLoader)
                                )
                            }
                            mBrightnessController = XposedHelpers.callMethod(XposedHelpers.getObjectField(mControlCenterController, "brightnessController"), "get")
                        }
                        val mDisplayManager = XposedHelpers.getObjectField(mBrightnessController, "mDisplayManager")
                        val mDisplayId = mContext.display?.displayId ?: 0
                        topMinimumBacklight = (XposedHelpers.getObjectField(mBrightnessController, "mMinimumBacklight") as? Float ?: 0f)
                        topMaximumBacklight = (XposedHelpers.getObjectField(mBrightnessController, "mMaximumBacklight") as? Float ?: 1f)
                        tapStartBrightness = XposedHelpers.callMethod(mDisplayManager, "getBrightness", mDisplayId) as? Float ?: 0f
                        if (isSlidingStart) {
                            currentDownTime = java.lang.System.currentTimeMillis()
                            currentDownX = tapStartX
                        } else {
                            currentDownTime = 0L
                            currentDownX = 0f
                        }
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        tapStartPointers = event.pointerCount.toFloat()
                    }
                    MotionEvent.ACTION_UP -> {
                        val lastTouchTime = currentTouchTime
                        val lastTouchX = currentTouchX
                        currentTouchTime = java.lang.System.currentTimeMillis()
                        currentTouchX = event.x
                        val mTouchX = currentTouchX
                        val mTouchTime = currentTouchTime
                        if (currentTouchTime - lastTouchTime < 250L && Math.abs(mTouchX - lastTouchX) < 100F) {
                            currentTouchTime = 0L
                            currentTouchX = 0F
                            GlobalActions.handleAction(mContext, "system_statusbarcontrols_dt")
                        }
                        if ((mTouchTime - currentDownTime > 600 && mTouchTime - currentDownTime < 4000)
                            && Math.abs(mTouchX - currentDownX) < 100F) {
                            if (MainModule.mPrefs.getBoolean("system_statusbarcontrols_longpress_vibrate")) {
                                val ignoreOff = MainModule.mPrefs.getBoolean("system_statusbarcontrols_longpress_vibrate_ignoreoff")
                                Helpers.performStrongVibration(mContext, ignoreOff)
                            }
                            GlobalActions.handleAction(mContext, "system_statusbarcontrols_longpress")
                        }
                        currentDownTime = 0L
                        currentDownX = 0f
                    }
                    MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        isSlidingStart = false
                        isSliding = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!isSlidingStart) return
                        if (event.y - tapStartY > sbHeight) {
                            currentDownTime = 0L
                            currentDownX = 0f
                            return
                        }
                        val metrics = res.displayMetrics
                        val delta = event.x - tapStartX
                        if (delta == 0f) return
                        if (!isSliding && Math.abs(delta) > metrics.widthPixels / 10f) isSliding = true
                        if (!isSliding) return
                        val opt = MainModule.mPrefs.getStringAsInt(if (tapStartPointers == 2f) "system_statusbarcontrols_dual" else "system_statusbarcontrols_single", 1)
                        if (opt == 2) {
                            val sens = MainModule.mPrefs.getStringAsInt("system_statusbarcontrols_sens_bright", 2)
                            var ratio = delta / metrics.widthPixels
                            ratio = (if (sens == 1) 0.66f else if (sens == 3) 1.66f else 1.0f) * ratio * 0.618f
                            val nextLevel = Math.min(topMaximumBacklight.toDouble(), Math.max(topMinimumBacklight.toDouble(), (tapStartBrightness + (topMaximumBacklight - topMinimumBacklight) * ratio).toDouble())).toFloat()
                            XposedHelpers.callMethod(mBrightnessController, "setBrightness", nextLevel)
                        } else if (opt == 3) {
                            val sens = MainModule.mPrefs.getStringAsInt("system_statusbarcontrols_sens_vol", 2)
                            if (Math.abs(delta) < metrics.widthPixels / ((if (sens == 1) 0.66f else if (sens == 3) 1.66f else 1.0f) * 20 * metrics.density)) return
                            tapStartX = event.x
                            val audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                            audioManager.adjustVolume(if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                                1 shl 12 or AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_ALLOW_RINGER_MODES or AudioManager.FLAG_VIBRATE)
                        }
                    }
                }
            }
        }

        ModuleHelper.findAndHookMethod(STATUS_BAR_CLS, lpparam.classLoader, "onTouchEvent", MotionEvent::class.java, hook)
        val pluginLoaderClass = "com.android.systemui.shared.plugins.PluginInstance\$Factory"
        ModuleHelper.hookAllMethods(pluginLoaderClass, lpparam.classLoader, "getClassLoader", object : MethodHook() {
            private var isHooked = false
            override fun after(param: AfterHookCallback) {
                val appInfo = param.getArgs()[0] as? ApplicationInfo ?: return
                if ("miui.systemui.plugin" == appInfo.packageName && !isHooked) {
                    isHooked = true
                    if (pluginLoader == null) {
                        pluginLoader = param.getResult() as? ClassLoader
                    }
                    pluginLoader?.let {
                        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl", it, "handleMotionEvent", MotionEvent::class.java, Boolean::class.javaPrimitiveType, hook)
                    }
                }
            }
        })
    }

    @SuppressLint("StaticFieldLeak")
    private var mPct: TextView? = null

    private fun initPct(container: ViewGroup, source: Int, context: Context) {
        val res = context.resources
        if (mPct == null) {
            mPct = TextView(container.context)
            mPct!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            mPct!!.gravity = Gravity.CENTER
            val density = res.displayMetrics.density
            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = Math.round(MainModule.mPrefs.getInt("system_showpct_top", 28) * density)
            lp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            mPct!!.setPadding(Math.round(20 * density), Math.round(10 * density), Math.round(18 * density), Math.round(12 * density))
            mPct!!.layoutParams = lp
            try {
                val modRes = ModuleHelper.getModuleRes(context)
                mPct!!.setTextColor(modRes.getColor(R.color.color_on_surface_variant, context.theme))
                mPct!!.background = ResourcesCompat.getDrawable(modRes, R.drawable.input_background, context.theme)
            } catch (err: Throwable) {
                XposedHelpers.log(err)
            }
            container.addView(mPct)
        }
        mPct!!.tag = source
        mPct!!.visibility = View.GONE
    }

    private fun removePct(mPctText: TextView?) {
        if (mPctText != null) {
            mPctText.visibility = View.GONE
            val p = mPctText.parent as? ViewGroup
            p?.removeView(mPctText)
            mPct = null
        }
    }

    @JvmStatic
    fun BrightnessPctHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.BrightnessMirrorController", lpparam.classLoader, "showMirror", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mStatusBarWindow = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBarWindow") as? ViewGroup ?: run {
                    XposedHelpers.log("BrightnessPctHook", "mStatusBarWindow is null")
                    return
                }
                initPct(mStatusBarWindow, 1, mStatusBarWindow.context)
                mPct?.visibility = View.VISIBLE
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.BrightnessMirrorController", lpparam.classLoader, "hideMirror", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                removePct(mPct)
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", lpparam.classLoader, "onStart", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mMirror = XposedHelpers.getObjectField(param.getThisObject(), "mControl")
                val controlCenterWindowViewController = XposedHelpers.getObjectField(mMirror, "controlCenterWindowViewController")
                var resolvedController = controlCenterWindowViewController
                val ClsName = resolvedController?.javaClass?.name ?: ""
                if (ClsName != "ControlCenterWindowViewController") {
                    resolvedController = XposedHelpers.callMethod(resolvedController, "get")
                }
                val windowView = XposedHelpers.callMethod(resolvedController, "getView") as? ViewGroup ?: run {
                    XposedHelpers.log("BrightnessPctHook", "mControlPanelContentView is null")
                    return
                }
                initPct(windowView, 2, mContext)
                mPct?.visibility = View.VISIBLE
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", lpparam.classLoader, "onStop", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                removePct(mPct)
            }
        })

        val BrightnessUtils = XposedHelpers.findClassIfExists("com.android.systemui.controlcenter.policy.BrightnessUtils", lpparam.classLoader)
        ModuleHelper.hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", lpparam.classLoader, "onChanged", object : MethodHook() {
            @SuppressLint("SetTextI18n")
            override fun after(param: AfterHookCallback) {
                val pctTag = if (mPct?.tag != null) mPct!!.tag as? Int ?: 0 else 0
                if (pctTag == 0 || mPct == null) return
                val currentLevel = param.getArgs()[3] as? Int ?: return
                if (BrightnessUtils != null) {
                    val maxLevel = XposedHelpers.getStaticObjectField(BrightnessUtils, "GAMMA_SPACE_MAX") as? Int ?: 1
                    mPct!!.text = ((currentLevel * 100) / maxLevel).toString() + "%"
                }
            }
        })
    }

    @JvmStatic
    fun ShowVolumePctHook(pluginLoader: ClassLoader) {
        val MiuiVolumeDialogImpl = XposedHelpers.findClassIfExists("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", pluginLoader) ?: return
        ModuleHelper.findAndHookMethod(MiuiVolumeDialogImpl, "showVolumeDialogH", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mDialogView = XposedHelpers.getObjectField(param.getThisObject(), "mDialogView") as? View ?: return
                val windowView = mDialogView.parent as? FrameLayout ?: return
                initPct(windowView, 3, windowView.context)
            }
        })

        ModuleHelper.findAndHookMethod(MiuiVolumeDialogImpl, "dismissH", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                removePct(mPct)
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener", pluginLoader, "onProgressChanged", object : MethodHook() {
            private var nowLevel = -233

            @SuppressLint("SetTextI18n")
            override fun after(param: AfterHookCallback) {
                val argLevel = param.getArgs()[1] as? Int ?: return
                if (nowLevel == argLevel) return
                val pctTag = if (mPct?.tag != null) mPct!!.tag as? Int ?: 0 else 0
                if (pctTag != 3 || mPct == null) return
                val mColumn = XposedHelpers.getObjectField(param.getThisObject(), "mColumn")
                val ss = XposedHelpers.getObjectField(mColumn, "ss")
                if (ss == null) return
                if (XposedHelpers.getIntField(mColumn, "stream") == 10) return

                val fromUser = param.getArgs()[2] as? Boolean ?: false
                var currentLevel: Int
                if (fromUser) {
                    currentLevel = argLevel
                } else {
                    val anim = XposedHelpers.getObjectField(mColumn, "anim") as? ObjectAnimator
                    if (anim == null || !anim.isRunning) return
                    currentLevel = XposedHelpers.getIntField(mColumn, "animTargetProgress")
                }
                nowLevel = currentLevel
                mPct!!.visibility = View.VISIBLE
                val levelMin = XposedHelpers.getIntField(ss, "levelMin")
                if (levelMin > 0 && currentLevel < levelMin * 1000) {
                    currentLevel = levelMin * 1000
                }
                val seekBar = param.getArgs()[0] as? SeekBar ?: return
                val max = seekBar.max
                val maxLevel = max / 1000
                if (currentLevel != 0) {
                    val i3 = maxLevel - 1
                    currentLevel = if (currentLevel == max) maxLevel else (currentLevel * i3 / max) + 1
                }
                mPct!!.text = ((currentLevel * 100) / maxLevel).toString() + "%"
            }
        })
    }

    @JvmStatic
    fun HideCCDateView(pluginLoader: ClassLoader) {
        val hideDateView = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val dateView = XposedHelpers.getObjectField(param.getThisObject(), "dateView") as? TextView ?: return
                XposedHelpers.setObjectField(dateView, "mVisibility", 8)
                dateView.visibility = View.GONE
            }
        }
        val fixClockView = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val clockView = XposedHelpers.getObjectField(param.getThisObject(), "clockView") as? TextView ?: return
                val ConstraintSetClass = pluginLoader.loadClass("androidx.constraintlayout.widget.ConstraintSet")
                val constraintSet = XposedHelpers.newInstance(ConstraintSetClass)
                val headerView = XposedHelpers.getObjectField(param.getThisObject(), "view")
                XposedHelpers.callMethod(constraintSet, "clone", headerView)
                val clockId = clockView.id
                XposedHelpers.callMethod(constraintSet, "clear", clockId, 7)
                XposedHelpers.callMethod(constraintSet, "applyTo", headerView)
            }
        }
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.windowview.MainPanelHeaderController", pluginLoader, "updateVisibility", hideDateView)
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.windowview.MainPanelHeaderController", pluginLoader, "updateConstraint", fixClockView)
    }

    @JvmStatic
    fun hideCCSettingsTilesEdit(pluginLoader: ClassLoader) {
        val hideIcons = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val headerView = XposedHelpers.callMethod(param.getThisObject(), "getView") as? ViewGroup ?: return
                var iconId = headerView.resources.getIdentifier("settings_shortcut", "id", "miui.systemui.plugin")
                var iconView = headerView.findViewById<ImageView>(iconId)
                iconView?.visibility = View.GONE
                iconId = headerView.resources.getIdentifier("tiles_edit", "id", "miui.systemui.plugin")
                iconView = headerView.findViewById(iconId)
                iconView?.visibility = View.GONE
            }
        }
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.windowview.MainPanelHeaderController", pluginLoader, "updateVisibility", hideIcons)

        if (MainModule.mPrefs.getBoolean("system_cc_custom_clock_action")) {
            ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.windowview.MainPanelHeaderController", pluginLoader, "addClockViews", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val clockView = XposedHelpers.getObjectField(param.getThisObject(), "clockView") as? TextView ?: return
                    clockView.setOnClickListener {
                        ModuleHelper.guarded {
                            val activityStarter = XposedHelpers.getObjectField(param.getThisObject(), "activityStarter")
                            val addFlags = Intent("android.settings.SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            XposedHelpers.callMethod(activityStarter, "postStartActivityDismissingKeyguard", addFlags, 350)
                        }
                    }
                    clockView.setOnLongClickListener {
                        ModuleHelper.guarded(false) {
                            val lazyQsCustomizer = XposedHelpers.getObjectField(param.getThisObject(), "qsCustomizer")
                            val qsCustomizer = XposedHelpers.callMethod(lazyQsCustomizer, "get")
                            XposedHelpers.callMethod(qsCustomizer, "show")
                            val hapticFeedback = XposedHelpers.getObjectField(param.getThisObject(), "hapticFeedback")
                            XposedHelpers.callMethod(hapticFeedback, "postLongClick")
                            true
                        }
                    }
                }
            })
        }
    }

    @JvmStatic
    fun initCCClockStyle(pluginLoader: ClassLoader) {
        val defaultClockSize = 9
        val ccClockFontSize = MainModule.mPrefs.getInt("system_cc_clock_fontsize", defaultClockSize)
        if (ccClockFontSize > defaultClockSize) {
            ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.windowview.MainPanelHeaderController", pluginLoader, "updateClocksAppearance", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val clock = XposedHelpers.getObjectField(param.getThisObject(), "clockView") as? TextView ?: return
                    clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, ccClockFontSize.toFloat())
                    clock.setLineSpacing(0f, 1f)
                }
            })
        }
        val defaultVerticalOffset = 10
        val verticalOffset = MainModule.mPrefs.getInt("system_cc_clock_verticaloffset", defaultVerticalOffset)
        if (verticalOffset != defaultVerticalOffset) {
            val topMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (verticalOffset - defaultVerticalOffset).toFloat(),
                Resources.getSystem().displayMetrics
            ).toInt()
            val verticalOffsetHook = object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val clock = XposedHelpers.getObjectField(param.getThisObject(), "clockView") as? TextView ?: return
                    val ConstraintSetClass = pluginLoader.loadClass("androidx.constraintlayout.widget.ConstraintSet")
                    val constraintSet = XposedHelpers.newInstance(ConstraintSetClass)
                    val headerView = XposedHelpers.getObjectField(param.getThisObject(), "view")
                    XposedHelpers.callMethod(constraintSet, "clone", headerView)
                    val clockId = clock.id
                    XposedHelpers.callMethod(constraintSet, "setMargin", clockId, 4, -topMargin)
                    XposedHelpers.callMethod(constraintSet, "applyTo", headerView)
                }
            }
            ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.windowview.MainPanelHeaderController", pluginLoader, "updateConstraint", verticalOffsetHook)
        }
    }

    @JvmStatic
    fun HideSafeVolumeDlgHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.volume.VolumeDialogControllerImpl", lpparam.classLoader, "onShowSafetyWarningW", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mAudio = XposedHelpers.getObjectField(param.getThisObject(), "mAudio")
                XposedHelpers.callMethod(mAudio, "disableSafeMediaVolume")
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun SwitchCCAndNotificationHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "handleEvent", MotionEvent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val useCC = XposedHelpers.callMethod(XposedHelpers.getObjectField(param.getThisObject(), "mPanelController"), "isExpandable") as? Boolean ?: false
                if (useCC) {
                    val bar = param.getThisObject() as? FrameLayout ?: return
                    val mControlPanelWindowManager = XposedHelpers.getObjectField(param.getThisObject(), "mControlPanelWindowManager")
                    val dispatchToControlPanel = XposedHelpers.callMethod(mControlPanelWindowManager, "dispatchToControlPanel", param.getArgs()[0], bar.width) as? Boolean ?: false
                    XposedHelpers.callMethod(mControlPanelWindowManager, "setTransToControlPanel", dispatchToControlPanel)
                    param.returnAndSkip(dispatchToControlPanel)
                    return
                }
                param.returnAndSkip(false)
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.controlcenter.phone.ControlPanelWindowManager", lpparam.classLoader, "dispatchToControlPanel", MotionEvent::class.java, Float::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val added = XposedHelpers.getBooleanField(param.getThisObject(), "added")
                if (added) {
                    val useCC = XposedHelpers.getBooleanField(XposedHelpers.getObjectField(param.getThisObject(), "mControlCenterController"), "useControlCenter")
                    if (useCC) {
                        val motionEvent = param.getArgs()[0] as? MotionEvent ?: return
                        if (motionEvent.actionMasked == 0) {
                            XposedHelpers.setObjectField(param.getThisObject(), "mDownX", motionEvent.rawX)
                        }
                        val controlCenterWindowView = XposedHelpers.getObjectField(param.getThisObject(), "mControlPanel")
                        if (controlCenterWindowView == null) {
                            param.returnAndSkip(false)
                        } else {
                            val mDownX = XposedHelpers.getFloatField(param.getThisObject(), "mDownX")
                            val width = param.getArgs()[1] as? Float ?: 0f
                            if (mDownX < width / 2.0f) {
                                param.returnAndSkip(XposedHelpers.callMethod(controlCenterWindowView, "handleMotionEvent", motionEvent, true))
                            } else {
                                param.returnAndSkip(false)
                            }
                        }
                        return
                    }
                }
                param.returnAndSkip(false)
            }
        })
    }

    @JvmStatic
    fun ShowCCStepCountHook(lpparam: PackageReadyParam) {
        val updateStyleHook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val headView = param.getThisObject() as? View ?: return
                val (carrierId, tag) = if (headView.javaClass.simpleName.contains("ControlCenterStatusBar")) {
                    "carrierText" to "StepInControlCenter"
                } else {
                    "mCarrierText" to "StepInNotification"
                }
                val mCarrierText = XposedHelpers.getObjectField(param.getThisObject(), carrierId) as? TextView ?: return
                val mSystemIconContainer = mCarrierText.parent as? LinearLayout ?: return
                var stepView = mSystemIconContainer.findViewWithTag(tag) as? TextView
                if (stepView == null) {
                    StepCounterController.removeStepViewByTag(tag)
                    stepView = TextView(headView.context)
                    val res = headView.resources
                    val styleId = res.getIdentifier("TextAppearance.StatusBar.Clock", "style", "com.android.systemui")
                    stepView.setTextAppearance(styleId)
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                    val horizMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        3f,
                        res.displayMetrics
                    )
                    lp.rightMargin = horizMargin.toInt()
                    lp.gravity = Gravity.CENTER_VERTICAL
                    mSystemIconContainer.addView(stepView, mSystemIconContainer.indexOfChild(mCarrierText), lp)
                    stepView.gravity = Gravity.CENTER_VERTICAL
                    stepView.tag = tag
                    StepCounterController.addStepView(stepView)
                }
                stepView.setTextColor(mCarrierText.textColors)
            }
        }
        if (MainModule.mPrefs.getBoolean("system_drawer_show_stepcount")) {
            ModuleHelper.findAndHookMethod("com.android.systemui.qs.MiuiNotificationHeaderView", lpparam.classLoader, "themeChanged", updateStyleHook)
        }
        if (MainModule.mPrefs.getBoolean("system_cc_show_stepcount")) {
            ModuleHelper.findAndHookMethod("com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar", lpparam.classLoader, "updateHeaderColor", updateStyleHook)
        }
    }

    @JvmStatic
    fun BluetoothTileStyleHook(pluginLoader: ClassLoader) {
        val tileResIds = IntArray(1)
        ModuleHelper.findAndHookMethod("miui.systemui.dagger.PluginComponentFactory", pluginLoader, "create", Context::class.java, Context::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val pluginContext = param.getArgs()[1] as? Context ?: return
                tileResIds[0] = pluginContext.resources.getIdentifier("big_tile", "layout", "miui.systemui.plugin")
            }
        })
        ModuleHelper.hookAllMethods("miui.systemui.controlcenter.dagger.ControlCenterViewModule", pluginLoader, "createBigTileGroup", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mView = param.getResult() as? ViewGroup ?: return
                val li = XposedHelpers.callMethod(param.getArgs()[0], "injectable", param.getArgs()[1]) as? LayoutInflater ?: return
                val btTileView = li.inflate(tileResIds[0], null)
                mView.addView(btTileView, 2)
                btTileView.tag = "big_tile_bt"
            }
        })
        val styleId = MainModule.mPrefs.getStringAsInt("system_cc_bluetooth_tile_style", 1)
        val updateStyleHook = object : MethodHook() {
            private var inited = false

            override fun after(param: AfterHookCallback) {
                val mView = XposedHelpers.callMethod(param.getThisObject(), "getView") as? ViewGroup ?: return
                val bigTileB = XposedHelpers.getObjectField(param.getThisObject(), "bigTileB") as? View ?: return
                if (!inited) {
                    inited = true
                    val factory = XposedHelpers.getObjectField(param.getThisObject(), "tileViewFactory")
                    val btTileView = mView.findViewWithTag("big_tile_bt") as? View ?: return
                    val btTileId = ResourceHooks.getFakeResId("bt_big_tile")
                    btTileView.id = btTileId
                    val btController = XposedHelpers.callMethod(factory, "create", btTileView, "bt")
                    XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "btTileView", btTileView)
                    XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "btController", btController)

                    val ConstraintSetClass = pluginLoader.loadClass("androidx.constraintlayout.widget.ConstraintSet")
                    val constraintSet = XposedHelpers.newInstance(ConstraintSetClass)
                    XposedHelpers.callMethod(constraintSet, "clone", mView)
                    val bigTileA = XposedHelpers.getObjectField(param.getThisObject(), "bigTileA") as? View ?: return
                    if (styleId == 2) {
                        XposedHelpers.callMethod(constraintSet, "connect", bigTileB.id, 7, btTileId, 6)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 6, bigTileB.id, 7)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 7, bigTileA.id, 7)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 3, bigTileB.id, 3)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 4, 0, 4)
                        XposedHelpers.callMethod(constraintSet, "setMargin", btTileId, 6, Helpers.dp2px(10f).toInt())
                        val labelResId = mView.resources.getIdentifier("label_container", "id", "miui.systemui.plugin")
                        bigTileB.findViewById<View>(labelResId)?.visibility = View.GONE
                        btTileView.findViewById<View>(labelResId)?.visibility = View.GONE
                        val iconResId = mView.resources.getIdentifier("status_icon", "id", "miui.systemui.plugin")
                        val layoutParams1 = bigTileB.findViewById<View>(iconResId)?.layoutParams as? LinearLayout.LayoutParams
                        layoutParams1?.leftMargin = Helpers.dp2px(3f).toInt()
                        val layoutParams2 = btTileView.findViewById<View>(iconResId)?.layoutParams as? LinearLayout.LayoutParams
                        layoutParams2?.leftMargin = Helpers.dp2px(3f).toInt()
                    } else {
                        XposedHelpers.callMethod(constraintSet, "connect", bigTileB.id, 4, btTileId, 3)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 6, bigTileA.id, 6)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 7, bigTileA.id, 7)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 3, bigTileB.id, 4)
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 4, 0, 4)
                    }
                    XposedHelpers.callMethod(constraintSet, "constrainWidth", btTileId, 0)
                    XposedHelpers.callMethod(constraintSet, "constrainHeight", btTileId, 0)
                    XposedHelpers.callMethod(constraintSet, "applyTo", mView)
                }
                if (styleId == 3) {
                    val layoutParams = bigTileB.layoutParams as? ViewGroup.MarginLayoutParams
                    val verticalMargin = Helpers.dp2px(4f).toInt()
                    layoutParams?.topMargin = verticalMargin
                    layoutParams?.bottomMargin = verticalMargin
                }
            }
        }
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "updateResources", updateStyleHook)
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "setListening", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val btController = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "btController")
                if (btController != null) {
                    XposedHelpers.callMethod(btController, "setListening", param.getArgs()[0])
                }
            }
        })
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "getRowViews", Int::class.javaPrimitiveType, object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun after(param: AfterHookCallback) {
                val row = param.getArgs()[0] as? Int ?: return
                val btTileView = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "btTileView")
                if (row == 1 && btTileView != null) {
                    (param.getResult() as? ArrayList<Any>)?.add(btTileView)
                }
            }
        })
        ModuleHelper.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "getChildControllers", object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun after(param: AfterHookCallback) {
                val btController = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "btController")
                if (btController != null) {
                    (param.getResult() as? ArrayList<Any>)?.add(btController)
                }
            }
        })
    }
}
