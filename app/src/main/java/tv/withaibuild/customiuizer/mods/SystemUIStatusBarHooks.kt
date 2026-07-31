package tv.withaibuild.customiuizer.mods

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.telephony.SubscriptionManager
import android.provider.Settings
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.DeviceInfoMonitor
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks
import tv.withaibuild.customiuizer.mods.utils.StepCounterController
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.HookUtils
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Arrays
import java.util.Iterator
import java.util.Locale
import java.util.Properties

@Suppress("UNUSED_PARAMETER")
object SystemUIStatusBarHooks {

    @JvmField
    var newStyle: Boolean = false

    private var statusbarTextIconLayoutResId = 0
    private val textIconTagId = ResourceHooks.getFakeResId("text_icon_tag")
    private val viewInitedTag = ResourceHooks.getFakeResId("view_inited_tag")
    private var statusbarIconList: List<String>? = null
    private val mStatusbarTextIcons = ArrayList<WeakReference<View>>()

    private var netSpeedStyleHookLogged = false
    private var netSpeedViewLogged = false
    private var initNetSpeedStyleLogged = false

    data class TextIcon(var atRight: Boolean, var iconType: Int)

    @JvmStatic
    fun setupStatusBar(mContext: Context) {
        statusbarTextIconLayoutResId = if (newStyle) {
            MainModule.resHooks.addResource("statusbar_text_icon", R.layout.statusbar_text_icon_new)
        } else {
            MainModule.resHooks.addResource("statusbar_text_icon", R.layout.statusbar_text_icon)
        }
        if (MainModule.mPrefs.getBoolean("system_statusbar_topmargin")) {
            val topMargin = MainModule.mPrefs.getInt("system_statusbar_topmargin_val", 1)
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_padding_top", topMargin.toFloat())
        }
        if (MainModule.mPrefs.getBoolean("system_statusbar_horizmargin")) {
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_padding_start", 0f)
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_padding_end", 0f)
        }
        if (MainModule.mPrefs.getBoolean("system_cc_enable_style_switch")) {
            MainModule.resHooks.setObjectReplacement("com.android.systemui", "integer", "force_use_control_panel", 0)
        }
        if (MainModule.mPrefs.getBoolean("system_qs_force_systemfonts")) {
            MainModule.resHooks.setObjectReplacement("com.android.systemui", "bool", "header_big_time_use_system_font", true)
        }
        if (MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")) {
            MainModule.resHooks.setObjectReplacement("com.android.systemui", "string", "network_speed_suffix", "%1\$s\n%2\$s")
        }
        if (MainModule.mPrefs.getBoolean("system_compactnotif")) {
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "notification_row_extra_padding", 0f)
        }
        if (MainModule.mPrefs.getBoolean("system_volumetimer")) {
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "array", "miui_volume_timer_segments", R.array.miui_volume_timer_segments)
        }
        if (MainModule.mPrefs.getBoolean("system_cc_tile_roundedrect")) {
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_unavailable", R.drawable.ic_qs_tile_bg_disabled)
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_disabled", R.drawable.ic_qs_tile_bg_disabled)
            MainModule.resHooks.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_warning", R.drawable.ic_qs_tile_bg_warning)
        }
        val iconSize = MainModule.mPrefs.getInt("system_statusbar_iconsize", 6)
        if (iconSize > 6) {
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_size", iconSize.toFloat())
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_clock_size", iconSize + 0.4f)
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_drawing_size", iconSize.toFloat())
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_drawing_size_dark", iconSize.toFloat())
            val notifyPadding = 2.5f * iconSize / 13
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_notification_icon_padding", notifyPadding)
            val iconHeight = 20.5f * iconSize / 13
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_height", iconHeight)
        }
        if (MainModule.mPrefs.getBoolean("system_cc_show_stepcount")) {
            StepCounterController.initContext(mContext)
        }
        Settings.System.putLong(mContext.contentResolver, "systemui_restart_time", java.lang.System.currentTimeMillis())

        val swapWifiSignal = MainModule.mPrefs.getBoolean("system_statusbaricons_swap_wifi_mobile")
        val moveSignalLeft = MainModule.mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft")
        if (swapWifiSignal || moveSignalLeft) {
            val resIconsId = mContext.resources.getIdentifier("config_statusBarIcons", "array", "com.android.systemui")
            statusbarIconList = Arrays.asList(*mContext.resources.getStringArray(resIconsId))
        }
    }

    @JvmStatic
    fun getSlotNameByType(mIconType: Int): String {
        return when (mIconType) {
            91 -> "battery_info"
            92 -> "device_temp"
            else -> ""
        }
    }

    /**
     * Immutable snapshot of all device-info monitor settings that the ticker must read.
     *
     * Fixed settings (master toggles, left/right slot, font/size/margins) are captured at
     * hook installation and require a SystemUI restart to change. This snapshot is rebuilt
     * once per tick so the whole pass uses a single consistent view of the dynamic settings.
     */
    internal data class DeviceMonitorSnapshot(
        val batteryInCharge: Boolean,
        val batteryContentOpt: Int,
        val batteryTempDecimal: Boolean,
        val batteryFixCurrentRatio: Boolean,
        val batteryPositive: Boolean,
        val batterySingleRow: Boolean,
        val batteryReverseOrder: Boolean,
        val batteryHideUnit: Int,
        val deviceTempContentOpt: Int,
        val deviceTempHideUnit: Boolean,
        val deviceTempSingleRow: Boolean,
        val deviceTempReverseOrder: Boolean
    )

    @JvmStatic
    internal fun readDeviceMonitorSnapshot(prefs: PrefMap<String, Any>): DeviceMonitorSnapshot {
        return DeviceMonitorSnapshot(
            batteryInCharge = prefs.getBoolean("system_statusbar_batterytempandcurrent_incharge"),
            batteryContentOpt = prefs.getStringAsInt("system_statusbar_batterytempandcurrent_content", 1),
            batteryTempDecimal = prefs.getBoolean("system_statusbar_batterytempandcurrent_temp_decimal"),
            batteryFixCurrentRatio = prefs.getBoolean("system_statusbar_batterytempandcurrent_fixcurrentratio"),
            batteryPositive = prefs.getBoolean("system_statusbar_batterytempandcurrent_positive"),
            batterySingleRow = prefs.getBoolean("system_statusbar_batterytempandcurrent_singlerow"),
            batteryReverseOrder = prefs.getBoolean("system_statusbar_batterytempandcurrent_reverseorder"),
            batteryHideUnit = prefs.getStringAsInt("system_statusbar_batterytempandcurrent_hideunit", 0),
            deviceTempContentOpt = prefs.getStringAsInt("system_statusbar_showdevicetemperature_content", 1),
            deviceTempHideUnit = prefs.getBoolean("system_statusbar_showdevicetemperature_hideunit"),
            deviceTempSingleRow = prefs.getBoolean("system_statusbar_showdevicetemperature_singlerow"),
            deviceTempReverseOrder = prefs.getBoolean("system_statusbar_showdevicetemperature_reverseorder")
        )
    }

    private fun parseSysfsInt(raw: String?, fallback: Int = 0): Int {
        return raw?.trim()?.toIntOrNull() ?: fallback
    }

    @JvmStatic
    internal fun buildBatteryInfo(snap: DeviceMonitorSnapshot, props: Properties): String {
        val opt = snap.batteryContentOpt
        var simpleTempVal = ""
        if (opt == 1 || opt == 4) {
            val tempVal = parseSysfsInt(props.getProperty("POWER_SUPPLY_TEMP"))
            simpleTempVal = if (snap.batteryTempDecimal) {
                (tempVal / 10f).toString()
            } else {
                if (tempVal % 10 == 0) (tempVal / 10).toString() else (tempVal / 10f).toString()
            }
        }

        var currVal = ""
        var preferred = "mA"
        var currentRatio = 1000f
        if (snap.batteryFixCurrentRatio) currentRatio = 1f
        var rawCurr = -1 * Math.round(parseSysfsInt(props.getProperty("POWER_SUPPLY_CURRENT_NOW")) / currentRatio)

        if (opt == 1 || opt == 3 || opt == 5) {
            if (snap.batteryPositive) rawCurr = Math.abs(rawCurr)
            if (Math.abs(rawCurr) > 999) {
                currVal = String.format(Locale.getDefault(), "%.2f", rawCurr / 1000f)
                preferred = "A"
            } else {
                currVal = rawCurr.toString()
            }
        }

        val hideUnit = snap.batteryHideUnit
        val tempUnit = if (hideUnit == 1 || hideUnit == 2) "" else "℃"
        val powerUnit = if (hideUnit == 1 || hideUnit == 3) "" else "W"
        val currUnit = if (hideUnit == 1 || hideUnit == 3) "" else preferred

        var simpleWatt = ""
        if (opt == 2 || opt == 4 || opt == 5) {
            val voltVal = parseSysfsInt(props.getProperty("POWER_SUPPLY_VOLTAGE_NOW")) / 1000f / 1000f
            simpleWatt = String.format(Locale.getDefault(), "%.2f", Math.abs(voltVal * rawCurr) / 1000)
        }

        val splitChar = if (snap.batterySingleRow) " " else "\n"
        return when (opt) {
            1 -> if (snap.batteryReverseOrder) {
                "$currVal$currUnit$splitChar$simpleTempVal$tempUnit"
            } else {
                "$simpleTempVal$tempUnit$splitChar$currVal$currUnit"
            }
            4 -> if (snap.batteryReverseOrder) {
                "$simpleWatt$powerUnit$splitChar$simpleTempVal$tempUnit"
            } else {
                "$simpleTempVal$tempUnit$splitChar$simpleWatt$powerUnit"
            }
            2 -> "$simpleWatt$powerUnit"
            5 -> if (snap.batteryReverseOrder) {
                "$simpleWatt$powerUnit$splitChar$currVal$currUnit"
            } else {
                "$currVal$currUnit$splitChar$simpleWatt$powerUnit"
            }
            else -> "$currVal$currUnit"
        }
    }

    @JvmStatic
    internal fun buildDeviceInfo(snap: DeviceMonitorSnapshot, batteryTemp: String?, cpuTemp: String?): String {
        val opt = if (snap.deviceTempContentOpt in 1..3) snap.deviceTempContentOpt else 1
        val hideUnit = snap.deviceTempHideUnit
        val tempUnit = if (hideUnit) "" else "℃"
        val splitChar = if (snap.deviceTempSingleRow) " " else "\n"
        return when (opt) {
            1 -> {
                val simpleBatteryTemp = String.format(Locale.getDefault(), "%.1f", parseSysfsInt(batteryTemp) / 10f)
                val simpleCpuTemp = String.format(Locale.getDefault(), "%.1f", parseSysfsInt(cpuTemp) / 1000f)
                if (snap.deviceTempReverseOrder) {
                    "$simpleCpuTemp$tempUnit$splitChar$simpleBatteryTemp$tempUnit"
                } else {
                    "$simpleBatteryTemp$tempUnit$splitChar$simpleCpuTemp$tempUnit"
                }
            }
            2 -> {
                val simpleBatteryTemp = String.format(Locale.getDefault(), "%.1f", parseSysfsInt(batteryTemp) / 10f)
                "$simpleBatteryTemp$tempUnit"
            }
            else -> {
                val simpleCpuTemp = String.format(Locale.getDefault(), "%.1f", parseSysfsInt(cpuTemp) / 1000f)
                "$simpleCpuTemp$tempUnit"
            }
        }
    }

    @JvmStatic
    fun MonitorDeviceInfoHook(lpparam: PackageReadyParam) {
        // Fixed settings: master toggles and left/right slot position determine which hooks are
        // installed and where the icon lives. Changing them requires a SystemUI restart.
        val showBatteryDetail = MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent")
        val showDeviceTemp = MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature")
        val DarkIconDispatcherClass = XposedHelpers.findClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
        val Dependency = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)
        val StatusBarIconHolder = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconHolder", lpparam.classLoader)
        val batteryAtRight = MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent_atright")
        val tempAtRight = MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature_atright")
        val textIcons = ArrayList<TextIcon>()
        if (showBatteryDetail) textIcons.add(TextIcon(batteryAtRight, 91))
        if (showDeviceTemp) textIcons.add(TextIcon(tempAtRight, 92))

        val hasRightIcon = textIcons.any { it.atRight }
        val hasLeftIcon = textIcons.any { !it.atRight }

        if (hasRightIcon && !MainModule.mPrefs.getBoolean("system_statusbar_dualrows")) {
            ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val iconController = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBarIconController")
                    for (ti in textIcons) {
                        if (ti.atRight) {
                            val slotIndex = XposedHelpers.callMethod(iconController, "getSlotIndex", getSlotNameByType(ti.iconType)) as Int
                            var iconHolder = XposedHelpers.callMethod(iconController, "getIcon", slotIndex, 0)
                            if (iconHolder == null) {
                                iconHolder = XposedHelpers.newInstance(StatusBarIconHolder)
                                XposedHelpers.setObjectField(iconHolder, "mType", ti.iconType)
                                XposedHelpers.callMethod(iconController, "setIcon", slotIndex, iconHolder)
                            }
                        }
                    }
                }
            })

            ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.StatusBarIconController\$IconManager", lpparam.classLoader, "addHolder", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    if (param.getArgsCount() != 4) return
                    val iconHolder = param.getArg(3)
                    val type = XposedHelpers.callMethod(iconHolder, "getType") as Int
                    if (type == 91 || type == 92) {
                        val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                        val lp = XposedHelpers.callMethod(param.getThisObject(), "onCreateLayoutParams") as? LinearLayout.LayoutParams ?: return
                        var createIcon: TextIcon? = null
                        for (ti in textIcons) {
                            if (ti.iconType == type) {
                                createIcon = ti
                                break
                            }
                        }
                        val iconView = createStatusbarTextIcon(mContext, lp, createIcon ?: return)
                        val i = param.getArg(0) as Int
                        val mGroup = XposedHelpers.getObjectField(param.getThisObject(), "mGroup") as? ViewGroup ?: return
                        mGroup.addView(iconView, i)
                        registerStatusbarTextIcon(iconView)
                        param.returnAndSkip(iconView)
                    }
                }
            })
        }

        if (hasLeftIcon) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View::class.java, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mContext = XposedHelpers.callMethod(param.getThisObject(), "getContext") as? Context ?: return
                    val DarkIconDispatcher = XposedHelpers.callStaticMethod(Dependency, "get", DarkIconDispatcherClass)
                    val baseAnchor = if (newStyle) {
                        XposedHelpers.getObjectField(param.getThisObject(), "mClockView") as? View
                    } else {
                        XposedHelpers.getObjectField(param.getThisObject(), "mDripNetworkSpeedSplitter") as? View
                    } ?: return
                    val leftIconsContainer = baseAnchor.parent as? ViewGroup ?: return
                    val bvIndex = leftIconsContainer.indexOfChild(baseAnchor)
                    val lp = baseAnchor.layoutParams as? LinearLayout.LayoutParams ?: return
                    for (ti in textIcons) {
                        if (!ti.atRight) {
                            val iconView = createStatusbarTextIcon(mContext, lp, ti)
                            leftIconsContainer.addView(iconView, bvIndex + 1)
                            registerStatusbarTextIcon(iconView)
                            XposedHelpers.callMethod(DarkIconDispatcher, "addDarkReceiver", iconView)
                        }
                    }
                }
            })
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "showSystemIconArea", Boolean::class.javaPrimitiveType, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    forEachStatusbarTextIcon { iconView, ti ->
                        if (!ti.atRight) {
                            XposedHelpers.callMethod(iconView, "setVisibilityByController", true)
                        }
                    }
                }
            })
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "hideSystemIconArea", Boolean::class.javaPrimitiveType, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    forEachStatusbarTextIcon { iconView, ti ->
                        if (!ti.atRight) {
                            XposedHelpers.callMethod(iconView, "setVisibilityByController", false)
                        }
                    }
                }
            })
        }

        val NetworkSpeedViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader)
        ModuleHelper.findAndHookMethod(NetworkSpeedViewClass, "getSlot", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val nsView = param.getThisObject() as? View ?: return
                val tagData = nsView.getTag(textIconTagId)
                if (tagData != null) {
                    val ti = tagData as? TextIcon
                    param.returnAndSkip(getSlotNameByType(ti?.iconType ?: 0))
                }
            }
        })

        DeviceInfoMonitor.hook(lpparam, showBatteryDetail, showDeviceTemp)
    }

    private fun getIconTextView(iconView: View): TextView {
        return if (newStyle) {
            XposedHelpers.getObjectField(iconView, "mNetworkSpeedNumberText") as TextView
        } else {
            iconView as TextView
        }
    }

    /**
     * Style and layout settings are applied once when the icon view is created.
     * Runtime changes to font size, margins, alignment, or fixed width require
     * a SystemUI restart.
     */
    private fun initStatusbarTextIcon(mContext: Context, lp: LinearLayout.LayoutParams, ti: TextIcon, iconView: View) {
        XposedHelpers.setObjectField(iconView, "mVisibleByController", true)
        XposedHelpers.setObjectField(iconView, "mShown", true)
        val iconTextView = getIconTextView(iconView)
        val res = mContext.resources
        val styleId = res.getIdentifier("TextAppearance.StatusBar.Clock", "style", "com.android.systemui")
        iconTextView.setTextAppearance(styleId)
        val subKey = if (ti.iconType == 91) "batterytempandcurrent" else "showdevicetemperature"
        val fontSize = MainModule.mPrefs.getInt("system_statusbar_${subKey}_fontsize", 16) * 0.5f
        val opt = MainModule.mPrefs.getStringAsInt("system_statusbar_${subKey}_content", 1)
        if ((opt == 1 || opt == 4 || opt == 5) && !MainModule.mPrefs.getBoolean("system_statusbar_${subKey}_singlerow")) {
            iconTextView.maxLines = 2
            iconTextView.setLineSpacing(0f, if (fontSize > 8.5f) 0.85f else 0.9f)
        }
        iconTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
        if (MainModule.mPrefs.getBoolean("system_statusbar_${subKey}_bold")) {
            iconTextView.typeface = Typeface.DEFAULT_BOLD
        }
        var leftMargin = MainModule.mPrefs.getInt("system_statusbar_${subKey}_leftmargin", 8)
        leftMargin = HookUtils.dp2px(leftMargin * 0.5f).toInt()
        var rightMargin = MainModule.mPrefs.getInt("system_statusbar_${subKey}_rightmargin", 8)
        rightMargin = HookUtils.dp2px(rightMargin * 0.5f).toInt()
        var topMargin = 0
        val verticalOffset = MainModule.mPrefs.getInt("system_statusbar_${subKey}_verticaloffset", 8)
        if (verticalOffset != 8) {
            topMargin = HookUtils.dp2px((verticalOffset - 8) * 0.5f).toInt()
        }
        iconTextView.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)
        val fixedWidth = MainModule.mPrefs.getInt("system_statusbar_${subKey}_fixedcontent_width", 10)
        if (fixedWidth > 10) {
            lp.width = HookUtils.dp2px(fixedWidth.toFloat()).toInt()
        }
        iconTextView.layoutParams = lp

        val align = MainModule.mPrefs.getStringAsInt("system_statusbar_${subKey}_align", 1)
        when (align) {
            2 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            3 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            4 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        }
    }

    private fun createStatusbarTextIcon(mContext: Context, lp: LinearLayout.LayoutParams, ti: TextIcon): View {
        val iconView = LayoutInflater.from(mContext).inflate(statusbarTextIconLayoutResId, null)
        iconView.setTag(textIconTagId, ti)
        if (!newStyle) {
            XposedHelpers.setObjectField(iconView, "mVisibilityByDisableInfo", 0)
        } else {
            val mNumber = iconView.findViewWithTag<View>("network_speed_number")
            XposedHelpers.setObjectField(iconView, "mNetworkSpeedNumberText", mNumber)
            val mUnit = iconView.findViewWithTag<View>("network_speed_unit")
            XposedHelpers.setObjectField(iconView, "mNetworkSpeedUnitText", mUnit)
        }
        initStatusbarTextIcon(mContext, lp, ti, iconView)
        return iconView
    }

    private fun registerStatusbarTextIcon(iconView: View) {
        if (iconView == null) return
        val it = mStatusbarTextIcons.iterator()
        while (it.hasNext()) {
            val ref = it.next()
            val existing = ref.get()
            if (existing == null || existing === iconView) it.remove()
        }
        mStatusbarTextIcons.add(WeakReference(iconView))
    }

    private fun forEachStatusbarTextIcon(consumer: (View, TextIcon) -> Unit) {
        val it = mStatusbarTextIcons.iterator()
        while (it.hasNext()) {
            val ref = it.next()
            val iconView = ref.get()
            if (iconView == null) {
                it.remove()
                continue
            }
            val tagData = iconView.getTag(textIconTagId)
            if (tagData == null) continue
            val ti = tagData as? TextIcon ?: continue
            consumer(iconView, ti)
        }
    }

    @JvmStatic
    internal fun updateDeviceInfoIcon(type: Int, show: Boolean, text: String) {
        forEachStatusbarTextIcon { view, icon ->
            if (icon.iconType != type) return@forEachStatusbarTextIcon
            XposedHelpers.callMethod(view, "setBlocked", !show)
            if (show) {
                if (newStyle) {
                    XposedHelpers.callMethod(view, "setNetworkSpeed", text, "")
                } else {
                    XposedHelpers.callMethod(view, "setNetworkSpeed", text)
                }
            }
        }
    }

    @JvmStatic
    fun DualRowStatusbarHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                var firstRowLeftPadding = 0
                var firstRowRightPadding = 0
                if (MainModule.mPrefs.getBoolean("system_statusbar_dualrows_firstrow_horizmargin")) {
                    firstRowLeftPadding = MainModule.mPrefs.getInt("system_statusbar_dualrows_firstrow_horizmargin_left", 0)
                    firstRowRightPadding = MainModule.mPrefs.getInt("system_statusbar_dualrows_firstrow_horizmargin_right", 0)
                }
                val clock2Rows = MainModule.mPrefs.getBoolean("system_statusbar_dualrows_clock_span2rows")
                val sbView = param.getThisObject() as FrameLayout
                val mContext = sbView.context
                val leftContainer = XposedHelpers.getObjectField(sbView, "mStatusBarLeftContainer") as? LinearLayout ?: return
                val statusBarcontents = leftContainer.parent as? LinearLayout ?: return
                val leftLayout = LinearLayout(mContext)
                val rightLayout = LinearLayout(mContext)
                statusBarcontents.addView(leftLayout, 0)
                statusBarcontents.addView(rightLayout)
                val leftGroup: LinearLayout

                if (clock2Rows) {
                    val mMiuiClock = XposedHelpers.getObjectField(sbView, "mMiuiClock") as? TextView ?: return
                    leftContainer.removeView(mMiuiClock)
                    leftGroup = LinearLayout(mContext)
                    leftLayout.addView(mMiuiClock)
                    leftLayout.addView(leftGroup)
                    leftLayout.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    val groupLp = LinearLayout.LayoutParams(0, -1, 1f)
                    leftGroup.layoutParams = groupLp
                } else {
                    leftGroup = leftLayout
                    if (firstRowLeftPadding > 0) {
                        leftContainer.setPaddingRelative(firstRowLeftPadding, 0, 0, 0)
                    }
                }
                statusBarcontents.removeView(leftContainer)
                leftGroup.addView(leftContainer)
                val secondLeft = LinearLayout(mContext)
                leftGroup.addView(secondLeft)
                leftLayout.id = leftContainer.id
                leftContainer.id = View.NO_ID
                XposedHelpers.setObjectField(sbView, "mStatusBarLeftContainer", leftLayout)

                val rightContainer = XposedHelpers.getObjectField(param.getThisObject(), "mSystemIconArea") as? ViewGroup ?: return
                val mFullscreenStatusBarNotificationIconArea = XposedHelpers.getObjectField(param.getThisObject(), "mFullscreenStatusBarNotificationIconArea") as? View ?: return
                rightContainer.removeView(mFullscreenStatusBarNotificationIconArea)
                secondLeft.addView(mFullscreenStatusBarNotificationIconArea)
                val mDripStatusBarNotificationIconArea = XposedHelpers.getObjectField(param.getThisObject(), "mDripStatusBarNotificationIconArea") as? View ?: return
                leftContainer.removeView(mDripStatusBarNotificationIconArea)
                secondLeft.addView(mDripStatusBarNotificationIconArea)

                leftGroup.orientation = LinearLayout.VERTICAL
                val leftLp = LinearLayout.LayoutParams(-1, 0, 1f)
                leftContainer.layoutParams = leftLp
                secondLeft.layoutParams = leftLp
                secondLeft.gravity = Gravity.START or Gravity.CENTER_VERTICAL

                rightLayout.id = rightContainer.id
                XposedHelpers.setObjectField(param.getThisObject(), "mSystemIconArea", rightLayout)
                val firstRight = LinearLayout(mContext)
                rightLayout.addView(firstRight)
                firstRight.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                if (firstRowRightPadding > 0) {
                    firstRight.setPaddingRelative(0, 0, firstRowRightPadding, 0)
                }
                val secondRight = LinearLayout(mContext)
                rightLayout.addView(secondRight)
                secondRight.gravity = Gravity.END or Gravity.CENTER_VERTICAL

                rightLayout.orientation = LinearLayout.VERTICAL
                val rightLp = LinearLayout.LayoutParams(-1, 0, 1f)
                firstRight.layoutParams = rightLp
                secondRight.layoutParams = rightLp

                val resSystemIconsId = sbView.resources.getIdentifier("system_icons", "id", lpparam.packageName)
                val rightChildCount = rightContainer.childCount
                for (i in rightChildCount - 1 downTo 0) {
                    val child = rightContainer.getChildAt(i)
                    if (child.id != resSystemIconsId) {
                        rightContainer.removeView(child)
                        firstRight.addView(child, 0)
                    }
                }

                val mStatusBarStatusIcons = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBarStatusIcons") as? View ?: return
                (mStatusBarStatusIcons.parent as? ViewGroup)?.removeView(mStatusBarStatusIcons)
                firstRight.addView(mStatusBarStatusIcons, 0)
                firstRight.id = resSystemIconsId

                val mBattery = XposedHelpers.getObjectField(param.getThisObject(), "mBattery") as? View ?: return
                (mBattery.parent as? ViewGroup)?.removeView(mBattery)
                secondRight.addView(mBattery)

                val showBatteryDetail = MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent")
                val showDeviceTemp = MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature")
                val batteryAtRight = showBatteryDetail && MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent_atright")
                val tempAtRight = showDeviceTemp && MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature_atright")
                val textIcons = ArrayList<TextIcon>()
                if (batteryAtRight) textIcons.add(TextIcon(true, 91))
                if (tempAtRight) textIcons.add(TextIcon(true, 92))
                if (textIcons.isNotEmpty()) {
                    val DarkIconDispatcherClass = XposedHelpers.findClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
                    val Dependency = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)
                    val DarkIconDispatcher = XposedHelpers.callStaticMethod(Dependency, "get", DarkIconDispatcherClass)
                    for (ti in textIcons) {
                        val iconView = createStatusbarTextIcon(mContext, LinearLayout.LayoutParams(-2, -2), ti)
                        secondRight.addView(iconView, 0)
                        registerStatusbarTextIcon(iconView)
                        XposedHelpers.callMethod(DarkIconDispatcher, "addDarkReceiver", iconView)
                    }
                }

                if (MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atsecondrow") && !newStyle) {
                    val mDripNetworkSpeedView = XposedHelpers.getObjectField(param.getThisObject(), "mDripNetworkSpeedView") as? View ?: return
                    leftContainer.removeView(mDripNetworkSpeedView)
                    secondRight.addView(mDripNetworkSpeedView, 0)
                }

                statusBarcontents.removeView(rightContainer)

                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "leftLayout", leftLayout)
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "rightLayout", rightLayout)
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "updateCutoutLocation", object : MethodHook(-1000) {
            override fun after(param: AfterHookCallback) {
                val mCurrentStatusBarType = XposedHelpers.getObjectField(param.getThisObject(), "mCurrentStatusBarType") as? Int ?: return
                val leftLayout = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "leftLayout") as? LinearLayout ?: return
                val rightLayout = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "rightLayout") as? LinearLayout ?: return

                if (mCurrentStatusBarType == 0) {
                    leftLayout.layoutParams = LinearLayout.LayoutParams(0, -1, 4f)
                    rightLayout.layoutParams = LinearLayout.LayoutParams(0, -1, 6f)
                } else {
                    leftLayout.layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
                    rightLayout.layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "showSystemIconArea", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mStatusBar = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBar") ?: return
                val rightLayout = XposedHelpers.getAdditionalInstanceField(mStatusBar, "rightLayout") as? View ?: return
                val leftLayout = XposedHelpers.getAdditionalInstanceField(mStatusBar, "leftLayout") as? View ?: return
                leftLayout.visibility = LinearLayout.VISIBLE
                rightLayout.visibility = LinearLayout.VISIBLE
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "hideSystemIconArea", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mStatusBar = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBar") ?: return
                val rightLayout = XposedHelpers.getAdditionalInstanceField(mStatusBar, "rightLayout") as? View ?: return
                val leftLayout = XposedHelpers.getAdditionalInstanceField(mStatusBar, "leftLayout") as? View ?: return
                leftLayout.visibility = LinearLayout.GONE
                rightLayout.visibility = LinearLayout.GONE
            }
        })
    }

    @JvmStatic
    fun DualRowSignalHook(lpparam: PackageReadyParam) {
        val mobileTypeSingle = MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single")
        if (!mobileTypeSingle) {
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_half_to_top_distance", 3f)
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_left_inout_over_strength", 0f)
            MainModule.resHooks.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_middle_to_strength_start", -0.4f)
        }

        val dualSignalResMap = HashMap<String, Int>()
        val colorModeList = arrayOf("", "dark", "tint")
        val selectedIconStyle = MainModule.mPrefs.getString("system_statusbar_dualsimin2rows_style", "")

        ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", object : MethodHook() {
            private var isHooked = false
            override fun after(param: AfterHookCallback) {
                if (!isHooked) {
                    isHooked = true
                    val mContext = XposedHelpers.callMethod(param.getThisObject(), "getApplicationContext") as? Context ?: return
                    val modRes = ModuleHelper.getModuleRes(mContext)
                    for (slot in 1..2) {
                        for (lvl in 0..5) {
                            for (colorMode in colorModeList) {
                                if (selectedIconStyle != "theme" || colorMode != "tint") {
                                    val stylePart = if (selectedIconStyle.isNotEmpty()) "_$selectedIconStyle" else ""
                                    val colorPart = if (colorMode.isNotEmpty()) "_$colorMode" else ""
                                    val dualIconResName = "statusbar_signal_${slot}_${lvl}${colorPart}${stylePart}"
                                    val iconResId = modRes.getIdentifier(dualIconResName, "drawable", HookUtils.modulePkg)
                                    dualSignalResMap[dualIconResName] = MainModule.resHooks.addResource(dualIconResName, iconResId)
                                }
                            }
                        }
                    }
                }
            }
        })

        val signalResToLevelMap = SparseIntArray()
        val moveSignalLeft = MainModule.mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft")
        val ControllerImplName = if (moveSignalLeft) "MiuiDripLeftStatusBarIconControllerImpl" else "StatusBarIconControllerImpl"
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.$ControllerImplName", lpparam.classLoader, "setMobileIcons", object : MethodHook() {
            private var isHooked = false
            override fun before(param: BeforeHookCallback) {
                if (!isHooked) {
                    isHooked = true
                    val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                    val res = mContext.resources
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_0", "drawable", lpparam.packageName), 0)
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_1", "drawable", lpparam.packageName), 1)
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_2", "drawable", lpparam.packageName), 2)
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_3", "drawable", lpparam.packageName), 3)
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_4", "drawable", lpparam.packageName), 4)
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_5", "drawable", lpparam.packageName), 5)
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_null", "drawable", lpparam.packageName), 6)
                }
                val iconStates = param.getArg(1) as? List<*> ?: return
                if (iconStates.size == 2) {
                    val mainIconState = iconStates[0]
                    val subIconState = iconStates[1]
                    val subDataConnected = XposedHelpers.getObjectField(subIconState, "dataConnected") as? Boolean ?: false
                    XposedHelpers.setObjectField(subIconState, "visible", false)
                    val mainSignalResId = XposedHelpers.getObjectField(mainIconState, "strengthId") as? Int ?: return
                    val subSignalResId = XposedHelpers.getObjectField(subIconState, "strengthId") as? Int ?: return
                    val mainLevel = signalResToLevelMap.get(mainSignalResId)
                    val subLevel = signalResToLevelMap.get(subSignalResId)
                    val level: Int
                    if (subDataConnected) {
                        level = subLevel * 10 + mainLevel
                        val syncFields = arrayOf("showName", "activityIn", "activityOut")
                        for (field in syncFields) {
                            XposedHelpers.setObjectField(mainIconState, field, XposedHelpers.getObjectField(subIconState, field))
                        }
                        XposedHelpers.setObjectField(mainIconState, "dataConnected", true)
                    } else {
                        level = mainLevel * 10 + subLevel
                    }
                    XposedHelpers.setObjectField(mainIconState, "strengthId", level)
                    param.getArgs()[1] = iconStates
                }
            }
        })

        val stateUpdateHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mobileIconState = param.getArg(0)
                val visible = XposedHelpers.getObjectField(mobileIconState, "visible") as? Boolean ?: false
                val airplane = XposedHelpers.getObjectField(mobileIconState, "airplane") as? Boolean ?: false
                val level = XposedHelpers.getObjectField(mobileIconState, "strengthId") as? Int ?: 0
                if (!visible || airplane || level == 0 || level > 100) {
                    XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "subStrengthId", -1)
                } else {
                    XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "subStrengthId", level % 10)
                }
            }

            override fun after(param: AfterHookCallback) {
                val subStrengthId = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "subStrengthId") as? Int ?: return
                if (subStrengthId < 0) return
                val mSmallHd = XposedHelpers.getObjectField(param.getThisObject(), "mSmallHd")
                XposedHelpers.callMethod(mSmallHd, "setVisibility", 8)
                val mSmallRoaming = XposedHelpers.getObjectField(param.getThisObject(), "mSmallRoaming")
                XposedHelpers.callMethod(mSmallRoaming, "setVisibility", 0)
            }
        }
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "initViewState", stateUpdateHook)
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "updateState", stateUpdateHook)

        val resetImageDrawable = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                var subStrengthId = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "subStrengthId") as? Int ?: return
                if (subStrengthId == 6) subStrengthId = 0
                val mobileIconState = XposedHelpers.getObjectField(param.getThisObject(), "mState")
                var level1 = XposedHelpers.getObjectField(mobileIconState, "strengthId") as? Int ?: return
                level1 /= 10
                if (level1 == 6) level1 = 0
                val mLight = XposedHelpers.getObjectField(param.getThisObject(), "mLight") as? Boolean ?: false
                val mUseTint = XposedHelpers.getObjectField(param.getThisObject(), "mUseTint") as? Boolean ?: false
                val mSmallRoaming = XposedHelpers.getObjectField(param.getThisObject(), "mSmallRoaming")
                val mMobile = XposedHelpers.getObjectField(param.getThisObject(), "mMobile")
                val colorMode = if (mUseTint && selectedIconStyle != "theme") {
                    "_tint"
                } else if (!mLight) {
                    "_dark"
                } else {
                    ""
                }
                val iconStyle = if (selectedIconStyle.isNotEmpty()) "_$selectedIconStyle" else ""
                val sim1IconId = "statusbar_signal_1_${level1}${colorMode}${iconStyle}"
                val sim2IconId = "statusbar_signal_2_${subStrengthId}${colorMode}${iconStyle}"
                val sim1ResId = dualSignalResMap[sim1IconId]
                val sim2ResId = dualSignalResMap[sim2IconId]
                if (sim1ResId != null) XposedHelpers.callMethod(mMobile, "setImageResource", sim1ResId)
                if (sim2ResId != null) XposedHelpers.callMethod(mSmallRoaming, "setImageResource", sim2ResId)
            }
        }
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "applyDarknessInternal", resetImageDrawable)
        val rightMargin = MainModule.mPrefs.getInt("system_statusbar_dualsimin2rows_rightmargin", 0)
        val leftMargin = MainModule.mPrefs.getInt("system_statusbar_dualsimin2rows_leftmargin", 0)
        val iconScale = MainModule.mPrefs.getInt("system_statusbar_dualsimin2rows_scale", 10)
        val verticalOffset = MainModule.mPrefs.getInt("system_statusbar_dualsimin2rows_verticaloffset", 8)
        if (rightMargin > 0 || leftMargin > 0 || iconScale != 10 || verticalOffset != 8) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "init", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mobileView = param.getThisObject() as? LinearLayout ?: return
                    val mContext = mobileView.context
                    val res = mContext.resources
                    val rightSpacing = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        rightMargin * 0.5f,
                        res.displayMetrics
                    ).toInt()
                    val leftSpacing = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        leftMargin * 0.5f,
                        res.displayMetrics
                    ).toInt()
                    mobileView.setPadding(leftSpacing, 0, rightSpacing, 0)
                    val mMobile = XposedHelpers.getObjectField(param.getThisObject(), "mMobile") as? View ?: return
                    if (verticalOffset != 8) {
                        val marginTop = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            (verticalOffset - 8) * 0.5f,
                            res.displayMetrics
                        )
                        val mobileIcon = mMobile.parent as? FrameLayout
                        mobileIcon?.translationY = marginTop
                    }
                    if (iconScale != 10) {
                        val mSmallRoaming = XposedHelpers.getObjectField(param.getThisObject(), "mSmallRoaming") as? View ?: return
                        val layoutParams = mMobile.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(-2, -1)
                        val mIconHeight = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            20 * iconScale / 10f,
                            res.displayMetrics
                        ).toInt()
                        layoutParams.height = mIconHeight
                        layoutParams.gravity = Gravity.CENTER
                        mMobile.layoutParams = layoutParams
                        mSmallRoaming.layoutParams = layoutParams
                    }
                }
            })
        }
    }

    @JvmStatic
    fun StatusBarIconsPositionAdjustHook(lpparam: PackageReadyParam, moveRight: Boolean, moveLeft: Boolean) {
        val dualRows = MainModule.mPrefs.getBoolean("system_statusbar_dualrows")
        val swapWifiSignal = MainModule.mPrefs.getBoolean("system_statusbaricons_swap_wifi_mobile")
        val moveSignalLeft = MainModule.mPrefs.getBoolean("system_statusbaricons_wifi_mobile_atleft")
        val netspeedAtRow2 = dualRows && MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atsecondrow")
        val netspeedRight = !netspeedAtRow2 && MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atright")
        val netspeedLeft = !netspeedAtRow2 && MainModule.mPrefs.getBoolean("system_statusbar_netspeed_atleft")
        val DripLeftController = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", lpparam.classLoader)

        val rightOnly2LeftIcons = ArrayList<String>()
        if (MainModule.mPrefs.getBoolean("system_statusbar_gps_atleft")) {
            rightOnly2LeftIcons.add("location")
        }

        val signalRelatedIcons = if (!swapWifiSignal) {
            listOf("no_sim", "hd", "mobile", "demo_mobile", "airplane", "hotspot", "slave_wifi", "wifi", "demo_wifi")
        } else {
            listOf("hotspot", "slave_wifi", "wifi", "demo_wifi", "no_sim", "hd", "mobile", "demo_mobile", "airplane")
        }

        if (moveLeft) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl", lpparam.classLoader, "setIconVisibility", String::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val slot = param.getArg(0) as? String ?: return
                    if (
                        ("alarm_clock" == slot && MainModule.mPrefs.getBoolean("system_statusbar_alarm_atleft")) ||
                        ("volume" == slot && MainModule.mPrefs.getBoolean("system_statusbar_sound_atleft")) ||
                        ("zen" == slot && MainModule.mPrefs.getBoolean("system_statusbar_dnd_atleft")) ||
                        ("nfc" == slot && MainModule.mPrefs.getBoolean("system_statusbar_nfc_atleft")) ||
                        ("headset" == slot && MainModule.mPrefs.getBoolean("system_statusbar_headset_atleft"))
                    ) {
                        param.getArgs()[1] = false
                    }
                }
            })
        }
        if (moveRight) {
            ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", lpparam.classLoader, "setIconVisibility", String::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val slot = param.getArg(0) as? String ?: return
                    if (
                        ("alarm_clock" == slot && MainModule.mPrefs.getBoolean("system_statusbar_alarm_atright")) ||
                        ("volume" == slot && MainModule.mPrefs.getBoolean("system_statusbar_sound_atright")) ||
                        ("zen" == slot && MainModule.mPrefs.getBoolean("system_statusbar_dnd_atright")) ||
                        ("nfc" == slot && MainModule.mPrefs.getBoolean("system_statusbar_nfc_atright")) ||
                        ("headset" == slot && MainModule.mPrefs.getBoolean("system_statusbar_headset_atright"))
                    ) {
                        param.getArgs()[1] = false
                    }
                }
            })
        }
        if (moveRight || netspeedRight) {
            ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", object : MethodHook() {
                private var isHooked = false

                override fun after(param: AfterHookCallback) {
                    if (!isHooked) {
                        isHooked = true
                        val MiuiEndIconManager = if (DripLeftController != null) {
                            XposedHelpers.findClass("com.android.systemui.statusbar.phone.MiuiEndIconManager", lpparam.classLoader)
                        } else {
                            XposedHelpers.findClass("com.android.systemui.statusbar.phone.MiuiIconManagerUtils", lpparam.classLoader)
                        }
                        val blockList = ModuleHelper.getStaticObjectFieldSilently(MiuiEndIconManager, "RIGHT_BLOCK_LIST")
                        val rightBlockList = blockList as? ArrayList<String> ?: return
                        if (netspeedRight) rightBlockList.remove("network_speed")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_alarm_atright")) rightBlockList.remove("alarm_clock")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_sound_atright")) rightBlockList.remove("volume")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_dnd_atright")) rightBlockList.remove("zen")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_btbattery_atright")) rightBlockList.remove("bluetooth_handsfree_battery")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_nfc_atright")) rightBlockList.remove("nfc")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_headset_atright")) rightBlockList.remove("headset")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_vpn_atright")) rightBlockList.remove("vpn")
                        XposedHelpers.setStaticObjectField(MiuiEndIconManager, "RIGHT_BLOCK_LIST", rightBlockList)
                    }
                }
            })
        }
        val dripLeftIcons = ArrayList<String>()
        if (swapWifiSignal || moveSignalLeft || moveLeft) {
            ModuleHelper.findAndHookConstructor("com.android.systemui.statusbar.phone.StatusBarIconList", lpparam.classLoader, Array<String>::class.java, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val isRightController = "StatusBarIconControllerImpl" == param.getThisObject().javaClass.simpleName
                    if (isRightController) {
                        if (swapWifiSignal || moveSignalLeft) {
                            val allStatusIcons = ArrayList(Arrays.asList(*(param.getArg(0) as? Array<String> ?: return)))
                            allStatusIcons.removeAll(signalRelatedIcons)
                            if (swapWifiSignal) {
                                for (slotName in signalRelatedIcons) {
                                    if (statusbarIconList?.contains(slotName) == true) {
                                        allStatusIcons.add(slotName)
                                    }
                                }
                            }
                            param.getArgs()[0] = allStatusIcons.toTypedArray()
                        }
                    } else if (moveSignalLeft || moveLeft) {
                        val allStatusIcons = ArrayList(Arrays.asList(*(param.getArg(0) as? Array<String> ?: return)))
                        allStatusIcons.addAll(rightOnly2LeftIcons)
                        dripLeftIcons.addAll(allStatusIcons)
                        if (moveSignalLeft) {
                            for (i in signalRelatedIcons.size - 1 downTo 0) {
                                val slotName = signalRelatedIcons[i]
                                if (statusbarIconList?.contains(slotName) == true) {
                                    allStatusIcons.add(0, slotName)
                                }
                            }
                        }
                        param.getArgs()[0] = allStatusIcons.toTypedArray()
                    }
                }
            })
        }

        val rightOnly2LeftWithSignal = ArrayList<String>(rightOnly2LeftIcons)
        if (moveSignalLeft && DripLeftController != null) {
            rightOnly2LeftWithSignal.add("slave_wifi")
            rightOnly2LeftWithSignal.add("hotspot")
            ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.phone.StatusBarSignalPolicy", lpparam.classLoader, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val dripLeftController = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader), "get", DripLeftController)
                    XposedHelpers.setObjectField(param.getThisObject(), "mIconController", dripLeftController)
                }
            })
        }
        if (rightOnly2LeftWithSignal.isNotEmpty() && DripLeftController != null) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl", lpparam.classLoader, "setIcon", String::class.java, Int::class.javaPrimitiveType, CharSequence::class.java, object : MethodHook(XposedInterface.PRIORITY_HIGHEST) {
                override fun before(param: BeforeHookCallback) {
                    val slot = param.getArg(0) as? String ?: return
                    if (rightOnly2LeftWithSignal.contains(slot)) {
                        val dripLeftController = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader), "get", DripLeftController)
                        XposedHelpers.callMethod(dripLeftController, "setIcon", param.getArg(0), param.getArg(1), param.getArg(2))
                        param.returnAndSkip(null)
                    }
                }
            })
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl", lpparam.classLoader, "setIconVisibility", String::class.java, Boolean::class.javaPrimitiveType, object : MethodHook(XposedInterface.PRIORITY_HIGHEST) {
                override fun before(param: BeforeHookCallback) {
                    val slot = param.getArg(0) as? String ?: return
                    if (rightOnly2LeftWithSignal.contains(slot)) {
                        val dripLeftController = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader), "get", DripLeftController)
                        XposedHelpers.callMethod(dripLeftController, "setIconVisibility", param.getArg(0), param.getArg(1))
                        param.returnAndSkip(null)
                    }
                }
            })
        }
        if (DripLeftController != null && (moveSignalLeft || moveLeft)) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View::class.java, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mStatusBar = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBar") ?: return
                    val mCurrentStatusBarType = XposedHelpers.getIntField(mStatusBar, "mCurrentStatusBarType")
                    if (mCurrentStatusBarType != 1) {
                        val mDripIconManager = XposedHelpers.getObjectField(param.getThisObject(), "mDripLeftDarkIconManager")
                        val blockList = ArrayList(dripLeftIcons)
                        if (MainModule.mPrefs.getBoolean("system_statusbar_alarm_atleft")) blockList.remove("alarm_clock")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_sound_atleft")) blockList.remove("volume")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_dnd_atleft")) blockList.remove("zen")
                        if (MainModule.mPrefs.getBoolean("system_statusbar_gps_atleft")) blockList.remove("location")
                        XposedHelpers.callMethod(mDripIconManager, "setBlockList", blockList)
                    }
                }
            })
        }
        if (DripLeftController != null) {
            ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "updateCutoutLocation", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mCurrentStatusBarType = XposedHelpers.getObjectField(param.getThisObject(), "mCurrentStatusBarType") as? Int ?: return
                    if (mCurrentStatusBarType == 1) {
                        if (netspeedRight) {
                            val mDripNetworkSpeedView = XposedHelpers.getObjectField(param.getThisObject(), "mDripNetworkSpeedView")
                            XposedHelpers.callMethod(mDripNetworkSpeedView, "setBlocked", true)
                        }
                    } else {
                        if (moveSignalLeft || moveLeft) {
                            val mDripStatusBarLeftStatusIconArea = XposedHelpers.getObjectField(param.getThisObject(), "mDripStatusBarLeftStatusIconArea") as? View ?: return
                            mDripStatusBarLeftStatusIconArea.visibility = View.VISIBLE
                        }
                        if (netspeedLeft || netspeedAtRow2) {
                            val mDripNetworkSpeedView = XposedHelpers.getObjectField(param.getThisObject(), "mDripNetworkSpeedView")
                            XposedHelpers.callMethod(mDripNetworkSpeedView, "setBlocked", false)
                        }
                    }
                }
            })
        }

        if (netspeedRight && DripLeftController != null) {
            ModuleHelper.hookAllMethods("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, "setDripNetworkSpeedView", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    param.getArgs()[0] = null
                }
            })
        }
        if (netspeedLeft || netspeedAtRow2) {
            ModuleHelper.hookAllMethods("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader, "setVisibilityByController", object : MethodHook() {
                private var leftViewId = 0

                override fun before(param: BeforeHookCallback) {
                    val meter = param.getThisObject() as? TextView ?: return
                    val slot = XposedHelpers.callMethod(param.getThisObject(), "getSlot") as? String ?: return
                    if (leftViewId == 0) {
                        leftViewId = meter.resources.getIdentifier("drip_network_speed_view", "id", lpparam.packageName)
                    }
                    if ("network_speed" == slot && meter.id != leftViewId) {
                        param.getArgs()[0] = false
                    }
                }
            })
        }
    }

    @JvmStatic
    fun StatusBarClockPositionHook(lpparam: PackageReadyParam) {
        val pos = MainModule.mPrefs.getStringAsInt("system_statusbar_clock_position", 1)
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val sbView = param.getThisObject() as? FrameLayout ?: return
                val mContext = sbView.context
                val res = mContext.resources
                val mClockView = XposedHelpers.getObjectField(param.getThisObject(), "mMiuiClock") as? TextView ?: return
                val leftIconsContainer = mClockView.parent as? LinearLayout ?: return
                val clockIndex = leftIconsContainer.indexOfChild(mClockView)
                leftIconsContainer.removeView(mClockView)
                val contentId = res.getIdentifier("status_bar_contents", "id", lpparam.packageName)
                val mContentsContainer = sbView.findViewById<LinearLayout>(contentId) ?: return
                val spaceView = XposedHelpers.getObjectField(param.getThisObject(), "mCutoutSpace") as? View ?: return
                val spaceIndex = mContentsContainer.indexOfChild(spaceView)
                val rightContainer = LinearLayout(mContext)
                val rightLp = LinearLayout.LayoutParams(0, -1, 1.0f)
                val mSystemIconArea = XposedHelpers.getObjectField(param.getThisObject(), "mSystemIconArea") as? View ?: return
                mContentsContainer.removeView(mSystemIconArea)
                mContentsContainer.addView(rightContainer, spaceIndex + 1, rightLp)
                rightContainer.addView(mSystemIconArea)
                val mDripStatusBarLeftStatusIconArea = XposedHelpers.getObjectField(param.getThisObject(), "mDripStatusBarLeftStatusIconArea") as? View ?: return
                leftIconsContainer.removeView(mDripStatusBarLeftStatusIconArea)
                leftIconsContainer.addView(mDripStatusBarLeftStatusIconArea, clockIndex)

                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                if (pos == 2) {
                    lp.gravity = Gravity.CENTER
                    mContentsContainer.addView(mClockView, spaceIndex, lp)
                } else {
                    rightContainer.addView(mClockView, lp)
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader, "updateLayoutForCutout", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val sbView = param.getThisObject() as? FrameLayout ?: return
                val mCurrentStatusBarType = XposedHelpers.getObjectField(param.getThisObject(), "mCurrentStatusBarType") as? Int ?: return
                val mSystemIconArea = XposedHelpers.getObjectField(param.getThisObject(), "mSystemIconArea") as? View ?: return
                val mStatusBarLeftContainer = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBarLeftContainer") as? View ?: return
                if (mCurrentStatusBarType == 0) {
                    val mSystemIconAreaLp = mSystemIconArea.layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(0, -1)
                    mSystemIconAreaLp.width = 0
                    mSystemIconAreaLp.weight = 1.0f
                    if (pos == 2) {
                        val rightContainer = mSystemIconArea.parent as? LinearLayout
                        val mDripStatusBarNotificationIconArea = XposedHelpers.getObjectField(param.getThisObject(), "mDripStatusBarNotificationIconArea") as? View
                        mDripStatusBarNotificationIconArea?.visibility = View.VISIBLE
                        val mStatusBarLeftContainerLp = mStatusBarLeftContainer.layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(0, -1)
                        mStatusBarLeftContainerLp.width = 0
                        mStatusBarLeftContainerLp.weight = 1.0f
                        mStatusBarLeftContainer.layoutParams = mStatusBarLeftContainerLp
                        val leftPadding = sbView?.paddingStart ?: 0
                        val rightPadding = sbView?.paddingEnd ?: 0
                        val topPadding = sbView?.paddingTop ?: 0
                        val bottomPadding = sbView?.paddingBottom ?: 0
                        mStatusBarLeftContainer.setPadding(leftPadding, 0, 0, 0)
                        rightContainer?.setPadding(0, 0, rightPadding, 0)
                        sbView?.setPadding(0, topPadding, 0, bottomPadding)
                    }
                } else {
                    val mCutoutSpace = XposedHelpers.getObjectField(param.getThisObject(), "mCutoutSpace") as? View ?: return
                    if (pos == 2) {
                        mCutoutSpace.visibility = View.GONE
                        mStatusBarLeftContainer.setPadding(0, 0, 0, 0)
                        val rightContainer = mSystemIconArea.parent as? LinearLayout ?: return
                        rightContainer.setPadding(0, 0, 0, 0)
                    }
                }
            }
        })
        if (pos == 2) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "updateNotificationIconAreaInnnerParent", object : MethodHook() {
                private var originType = 0

                override fun before(param: BeforeHookCallback) {
                    val mCurrentStatusBarType = XposedHelpers.getIntField(param.getThisObject(), "mCurrentStatusBarType")
                    if (mCurrentStatusBarType == 0) {
                        XposedHelpers.setObjectField(param.getThisObject(), "mCurrentStatusBarType", 1)
                    }
                    originType = mCurrentStatusBarType
                }

                override fun after(param: AfterHookCallback) {
                    XposedHelpers.setObjectField(param.getThisObject(), "mCurrentStatusBarType", originType)
                }
            })
        }
    }

    @JvmStatic
    fun NoNetworkSpeedSeparatorHook(lpparam: PackageReadyParam) {
        val hideSplitterHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tv = param.getThisObject() as? TextView ?: return
                tv.visibility = View.GONE
                param.returnAndSkip(null)
            }
        }
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedSplitter", lpparam.classLoader, "onClockVisibilityChanged", Int::class.javaPrimitiveType, hideSplitterHook)
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedSplitter", lpparam.classLoader, "onNetworkSpeedVisibilityChanged", Int::class.javaPrimitiveType, hideSplitterHook)
    }

    @JvmStatic
    fun FormatNetworkSpeedHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, "formatSpeed", Context::class.java, Long::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val hideLow = MainModule.mPrefs.getBoolean("system_detailednetspeed_low")
                if (hideLow) {
                    val lowLevel = MainModule.mPrefs.getInt("system_detailednetspeed_lowlevel", 1) * 1024
                    val speedVal = param.getArg(1) as? Long ?: 0L
                    if (speedVal < lowLevel) {
                        if (newStyle) {
                            param.returnAndSkip(arrayOf("", ""))
                        } else {
                            param.returnAndSkip("")
                        }
                    }
                }
            }

            override fun after(param: AfterHookCallback) {
                val hideUnit = MainModule.mPrefs.getBoolean("system_detailednetspeed_secunit")
                if (hideUnit && !newStyle) {
                    var speedText = param.getResult() as? String ?: return
                    speedText = speedText.replaceFirst("B?[/']s".toRegex(), "")
                    param.setResult(speedText)
                }
            }
        })
    }

    private fun initNetSpeedStyle(meter: View) {
        val isFirst = !initNetSpeedStyleLogged
        if (isFirst) {
            initNetSpeedStyleLogged = true
            XposedHelpers.log("CustoMIUIzer NetSpeed", "initNetSpeedStyle start: meterClass=${meter.javaClass.name}, newStyle=$newStyle, dualRow=${MainModule.mPrefs.getBoolean("system_detailednetspeed") || MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")}")
        }
        try {
            val dualRow = MainModule.mPrefs.getBoolean("system_detailednetspeed") || MainModule.mPrefs.getBoolean("system_detailednetspeed_fakedualrow")
            val iconTextView = getIconTextView(meter)
            var fontSize = MainModule.mPrefs.getInt("system_netspeed_fontsize", 13)
            if (dualRow) {
                if (newStyle) {
                    val unitView = XposedHelpers.getObjectField(meter, "mNetworkSpeedUnitText") as? View
                    unitView?.visibility = View.GONE
                }
                if (fontSize > 23 || fontSize == 13) fontSize = 16
            } else {
                if (fontSize < 20 && fontSize != 13) fontSize = 27
            }
            if (dualRow || fontSize != 13) {
                iconTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)
            }
            if (MainModule.mPrefs.getBoolean("system_netspeed_bold")) {
                iconTextView.typeface =
                    Typeface.create(iconTextView.typeface, Typeface.BOLD)
            }

            var leftMargin = MainModule.mPrefs.getInt("system_netspeed_leftmargin", 0)
            leftMargin = HookUtils.dp2px(leftMargin * 0.5f).toInt()
            var rightMargin = MainModule.mPrefs.getInt("system_netspeed_rightmargin", 0)
            rightMargin = HookUtils.dp2px(rightMargin * 0.5f).toInt()
            var topMargin = 0
            val verticalOffset = MainModule.mPrefs.getInt("system_netspeed_verticaloffset", 8)
            if (verticalOffset != 8) {
                topMargin = HookUtils.dp2px((verticalOffset - 8) * 0.5f).toInt()
            }
            iconTextView.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)

            val align = MainModule.mPrefs.getStringAsInt("system_detailednetspeed_align", 1)
            when (align) {
                2 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                3 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                4 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            }

            if (dualRow) {
                val rowSpacing = MainModule.mPrefs.getInt("system_netspeed_rowspacing", 100)
                val spacing = resolveNetSpeedLineSpacing(fontSize, rowSpacing)
                iconTextView.setSingleLine(false)
                iconTextView.maxLines = 2
                iconTextView.setLineSpacing(0f, spacing)
            }
            if (isFirst) {
                XposedHelpers.log("CustoMIUIzer NetSpeed", "initNetSpeedStyle completed")
            }
        } catch (t: Throwable) {
            if (isFirst) {
                XposedHelpers.log("CustoMIUIzer NetSpeed", "initNetSpeedStyle failed: ${t.javaClass.name}: ${t.message}")
            }
            throw t
        }
    }

    internal fun resolveNetSpeedLineSpacing(fontSize: Int, adjustmentPercent: Int): Float {
        val baseSpacing = if (fontSize > 17) 0.85f else 0.90f
        val adjustment = adjustmentPercent.coerceIn(70, 130)
        return baseSpacing * adjustment / 100f
    }

    @JvmStatic
    fun NetSpeedStyleHook(lpparam: PackageReadyParam) {
        if (!netSpeedStyleHookLogged) {
            netSpeedStyleHookLogged = true
            XposedHelpers.log("CustoMIUIzer NetSpeed", "NetSpeedStyleHook installed, newStyle=$newStyle")
        }
        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val meter = param.getThisObject() as? View ?: return
                if (!netSpeedViewLogged) {
                    netSpeedViewLogged = true
                    XposedHelpers.log("CustoMIUIzer NetSpeed", "NetworkSpeedView created: class=${meter.javaClass.name}, isTextView=${meter is TextView}, isViewGroup=${meter is ViewGroup}, tag=${meter.tag}, newStyle=$newStyle")
                }
                val inited = meter.getTag(viewInitedTag)
                if (inited == null && "slot_text_icon" != meter.tag) {
                    meter.setTag(viewInitedTag, true)
                    val fixedWidth = MainModule.mPrefs.getInt("system_netspeed_fixedcontent_width", 10)
                    if (fixedWidth > 10) {
                        var lp = meter.layoutParams ?: ViewGroup.LayoutParams(0, -1)
                        val viewWidth = (meter.resources.displayMetrics.density * fixedWidth).toInt()
                        lp.width = viewWidth
                        meter.layoutParams = lp
                    }
                    meter.postDelayed({ ModuleHelper.guarded { initNetSpeedStyle(meter) } }, 200)
                }
            }
        })
    }

    @JvmStatic
    fun MobileTypeSingleHook(lpparam: PackageReadyParam) {
        val singleTypeHook = object : MethodHook(XposedInterface.PRIORITY_HIGHEST) {
            override fun before(param: BeforeHookCallback) {
                val mobileIconState = param.getArg(0)
                XposedHelpers.setObjectField(mobileIconState, "showMobileDataTypeSingle", true)
            }

            override fun after(param: AfterHookCallback) {
                val mMobileLeftContainer = XposedHelpers.getObjectField(param.getThisObject(), "mMobileLeftContainer")
                XposedHelpers.callMethod(mMobileLeftContainer, "setVisibility", 8)
            }
        }
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "initViewState", singleTypeHook)
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "updateState", singleTypeHook)

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "init", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val res = mContext.resources
                val mMobileGroup = XposedHelpers.getObjectField(param.getThisObject(), "mMobileGroup") as? LinearLayout ?: return
                val mMobileTypeSingle = XposedHelpers.getObjectField(param.getThisObject(), "mMobileTypeSingle") as? TextView ?: return
                if (!MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single_atleft")) {
                    mMobileGroup.removeView(mMobileTypeSingle)
                    mMobileGroup.addView(mMobileTypeSingle)
                }
                val mlp = mMobileTypeSingle.layoutParams as? ViewGroup.MarginLayoutParams ?: ViewGroup.MarginLayoutParams(0, 0)
                val leftMargin = MainModule.mPrefs.getInt("system_statusbar_mobiletype_single_leftmargin", 4)
                val marginLeft = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    leftMargin * 0.5f,
                    res.displayMetrics
                )
                mlp.leftMargin = marginLeft.toInt()
                val rightMargin = MainModule.mPrefs.getInt("system_statusbar_mobiletype_single_rightmargin", 0)
                if (rightMargin > 0) {
                    val marginRight = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        rightMargin * 0.5f,
                        res.displayMetrics
                    )
                    mlp.rightMargin = marginRight.toInt()
                }
                val verticalOffset = MainModule.mPrefs.getInt("system_statusbar_mobiletype_single_verticaloffset", 8)
                if (verticalOffset != 8) {
                    val marginTop = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        (verticalOffset - 8) * 0.5f,
                        res.displayMetrics
                    )
                    mlp.topMargin = marginTop.toInt()
                }
                mMobileTypeSingle.layoutParams = mlp
                val fontSize = MainModule.mPrefs.getInt("system_statusbar_mobiletype_single_fontsize", 27)
                mMobileTypeSingle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)
                if (MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single_bold")) {
                    mMobileTypeSingle.typeface = Typeface.DEFAULT_BOLD
                }
            }
        })
    }

    @JvmStatic
    fun HorizMarginHook(lpparam: PackageReadyParam) {
        val horizHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val leftMargin = MainModule.mPrefs.getInt("system_statusbar_horizmargin_left", 16)
                val marginLeft = HookUtils.dp2px(leftMargin.toFloat())
                val rightMargin = MainModule.mPrefs.getInt("system_statusbar_horizmargin_right", 16)
                val marginRight = HookUtils.dp2px(rightMargin.toFloat())
                param.returnAndSkip(android.util.Pair(marginLeft.toInt(), marginRight.toInt()))
            }
        }
        val StatusBarWindowViewCls = "com.android.systemui.statusbar.window.StatusBarWindowView"
        ModuleHelper.hookAllMethods(StatusBarWindowViewCls, lpparam.classLoader, "paddingNeededForCutoutAndRoundedCorner", horizHook)
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider", lpparam.classLoader, "getStatusBarContentInsetsForCurrentRotation", horizHook)
    }

    @JvmStatic
    fun HideIconsClockHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "showClock", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                XposedHelpers.callMethod(param.getThisObject(), "hideClockInternal", 8, false)
                if (!newStyle) {
                    XposedHelpers.callMethod(param.getThisObject(), "hideNetworkSpeedSplitter", 8, false)
                }
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun HideIconsVoWiFiHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethodSilently("com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig", lpparam.classLoader, "getHideVowifi", HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun HideIconsSignalHook(lpparam: PackageReadyParam) {
        val beforeUpdate = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mobileIconState = param.getArg(0)
                if (MainModule.mPrefs.getBoolean("system_statusbaricons_signal")) {
                    if (!MainModule.mPrefs.getBoolean("system_statusbaricons_signal_wificonnected") || XposedHelpers.getBooleanField(mobileIconState, "wifiAvailable")) {
                        XposedHelpers.setObjectField(mobileIconState, "visible", false)
                        return
                    }
                }
                val subId = XposedHelpers.getObjectField(mobileIconState, "subId") as? Int ?: return
                val dataSubId = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
                val slotId = android.telephony.SubscriptionManager.getSlotIndex(subId)
                if (
                    (MainModule.mPrefs.getBoolean("system_statusbaricons_sim1") && slotId == 0) ||
                    (MainModule.mPrefs.getBoolean("system_statusbaricons_sim2") && slotId == 1) ||
                    (MainModule.mPrefs.getBoolean("system_statusbaricons_sim_nodata") && subId != dataSubId)
                ) {
                    XposedHelpers.setObjectField(mobileIconState, "visible", false)
                    return
                }
                if (MainModule.mPrefs.getBoolean("system_statusbaricons_roaming")) {
                    XposedHelpers.setObjectField(mobileIconState, "roaming", false)
                }
                if (MainModule.mPrefs.getBoolean("system_statusbaricons_volte")) {
                    XposedHelpers.setObjectField(mobileIconState, "volte", false)
                    XposedHelpers.setObjectField(mobileIconState, "speechHd", false)
                }
            }
        }
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "initViewState", beforeUpdate)
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "updateState", beforeUpdate)
    }

    private fun checkSlot(slotName: String?): Boolean {
        return try {
            ("headset" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_headset")) ||
            ("volume" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_sound")) ||
            ("zen" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_dnd")) ||
            ("alarm_clock" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_alarm")) ||
            ("managed_profile" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_profile")) ||
            ("vpn" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_vpn")) ||
            ("airplane" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_airplane")) ||
            ("nfc" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_nfc")) ||
            ("second_space" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_secondspace")) ||
            ("location" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_gps")) ||
            ("wifi" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_wifi")) ||
            ("slave_wifi" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_dualwifi")) ||
            ("hotspot" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_hotspot")) ||
            ("no_sim" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_nosims")) ||
            ("bluetooth_handsfree_battery" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_btbattery")) ||
            ("ble_unlock_mode" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_ble_unlock")) ||
            ("hd" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_volte"))
        } catch (t: Throwable) {
            XposedHelpers.log(t)
            false
        }
    }

    @JvmStatic
    fun HideIconsHook(lpparam: PackageReadyParam) {
        val iconHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val iconType = param.getArgs()[0] as? String ?: return
                if (checkSlot(iconType)) {
                    param.getArgs()[1] = false
                }
            }
        }
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl", lpparam.classLoader, "setIconVisibility", String::class.java, Boolean::class.javaPrimitiveType, iconHook)
        if (!newStyle) {
            ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", lpparam.classLoader, "setIconVisibility", String::class.java, Boolean::class.javaPrimitiveType, iconHook)
        }
    }

    @JvmStatic
    fun HideIconsFromSystemManager(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl", lpparam.classLoader, "setIcon", String::class.java, "com.android.internal.statusbar.StatusBarIcon", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val slotName = param.getArg(0) as? String ?: return
                if (
                    ("stealth" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_privacy")) ||
                    ("mute" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_mute")) ||
                    ("speakerphone" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_speaker")) ||
                    ("call_record" == slotName && MainModule.mPrefs.getBoolean("system_statusbaricons_record"))
                ) {
                    XposedHelpers.setObjectField(param.getArg(1), "visible", false)
                }
            }
        })
    }

    private var measureTime = 0L
    private var txBytesTotal = 0L
    private var rxBytesTotal = 0L
    private var txSpeed = 0L
    private var rxSpeed = 0L

    private fun getTrafficBytes(thisObject: Any?): android.util.Pair<Long, Long> {
        var tx = -1L
        var rx = -1L
        try {
            val list = java.net.NetworkInterface.getNetworkInterfaces()
            while (list.hasMoreElements()) {
                val iface = list.nextElement()
                if (iface.isUp && !iface.isVirtual && !iface.isLoopback && !iface.isPointToPoint && "" != iface.name) {
                    tx += XposedHelpers.callStaticMethod(TrafficStats::class.java, "getTxBytes", iface.name) as? Long ?: 0L
                    rx += XposedHelpers.callStaticMethod(TrafficStats::class.java, "getRxBytes", iface.name) as? Long ?: 0L
                }
            }
        } catch (t: Throwable) {
            XposedHelpers.log(t)
            tx = TrafficStats.getTotalTxBytes()
            rx = TrafficStats.getTotalRxBytes()
        }
        return android.util.Pair(tx, rx)
    }

    @SuppressLint("DefaultLocale")
    private fun humanReadableByteCount(ctx: Context, bytes: Long): String {
        return try {
            val modRes = ModuleHelper.getModuleRes(ctx)
            val hideSecUnit = MainModule.mPrefs.getBoolean("system_detailednetspeed_secunit")
            var unitSuffix = modRes.getString(R.string.Bs)
            if (hideSecUnit) {
                unitSuffix = ""
            }
            var f = bytes / 1024.0f
            var expIndex = 0
            if (f > 999.0f) {
                expIndex = 1
                f /= 1024.0f
            }
            val pre = modRes.getString(R.string.speedunits).toCharArray()[expIndex]
            (if (f < 100.0f) String.format("%.1f", f) else String.format("%.0f", f)) + String.format("%s$unitSuffix", pre)
        } catch (t: Throwable) {
            XposedHelpers.log(t)
            ""
        }
    }

    @JvmStatic
    fun NetSpeedIntervalHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, "postUpdateNetworkSpeedDelay", Long::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val originInterval = param.getArgs()[0] as? Long ?: return
                if (originInterval == 4000L) {
                    val newInterval = MainModule.mPrefs.getInt("system_netspeedinterval", 4) * 1000L
                    param.getArgs()[0] = newInterval
                }
            }
        })
    }

    @JvmStatic
    fun DetailedNetSpeedHook(lpparam: PackageReadyParam) {
        val nscCls = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader)
        if (nscCls == null) {
            XposedHelpers.log("DetailedNetSpeedHook", "No NetworkSpeed view or controller")
            return
        }

        ModuleHelper.findAndHookMethod(nscCls, "getTotalByte", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val bytes = getTrafficBytes(param.getThisObject())
                txBytesTotal = bytes.first
                rxBytesTotal = bytes.second
                measureTime = java.lang.System.nanoTime()
            }
        })

        ModuleHelper.findAndHookMethod(nscCls, "updateNetworkSpeed", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                var isConnected = false
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mConnectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
                val nw = mConnectivityManager.activeNetwork
                if (nw != null) {
                    val capabilities = mConnectivityManager.getNetworkCapabilities(nw)
                    if (capabilities != null && (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR))) {
                        isConnected = true
                    }
                }
                if (isConnected) {
                    val nanoTime = java.lang.System.nanoTime()
                    var newTime = nanoTime - measureTime
                    measureTime = nanoTime
                    if (newTime == 0L) newTime = Math.round(4 * Math.pow(10.0, 9.0))
                    val bytes = getTrafficBytes(param.getThisObject())
                    val newTxBytes = bytes.first
                    val newRxBytes = bytes.second
                    var newTxBytesFixed = newTxBytes - txBytesTotal
                    var newRxBytesFixed = newRxBytes - rxBytesTotal
                    if (newTxBytesFixed < 0 || txBytesTotal == 0L) newTxBytesFixed = 0
                    if (newRxBytesFixed < 0 || rxBytesTotal == 0L) newRxBytesFixed = 0
                    txSpeed = Math.round(newTxBytesFixed / (newTime / Math.pow(10.0, 9.0)))
                    rxSpeed = Math.round(newRxBytesFixed / (newTime / Math.pow(10.0, 9.0)))
                    txBytesTotal = newTxBytes
                    rxBytesTotal = newRxBytes
                } else {
                    txSpeed = 0
                    rxSpeed = 0
                }
            }
        })

        ModuleHelper.hookAllMethods(nscCls, "updateText", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val hideLow = MainModule.mPrefs.getBoolean("system_detailednetspeed_low")
                val lowLevel = MainModule.mPrefs.getInt("system_detailednetspeed_lowlevel", 1) * 1024
                val icons = Integer.parseInt(MainModule.mPrefs.getString("system_detailednetspeed_icon", "2"))

                var txarrow = ""
                var rxarrow = ""
                if (icons == 2) {
                    txarrow = if (txSpeed < lowLevel) "△" else "▲"
                    rxarrow = if (rxSpeed < lowLevel) "▽" else "▼"
                } else if (icons == 3) {
                    txarrow = if (txSpeed < lowLevel) " ☖" else " ☗"
                    rxarrow = if (rxSpeed < lowLevel) " ⛉" else " ⛊"
                }

                val tx = if (hideLow && txSpeed < lowLevel) "" else humanReadableByteCount(mContext, txSpeed) + txarrow
                val rx = if (hideLow && rxSpeed < lowLevel) "" else humanReadableByteCount(mContext, rxSpeed) + rxarrow
                if (newStyle) {
                    param.getArgs()[0] = arrayOf(tx + "\n" + rx, "")
                } else {
                    param.getArgs()[0] = tx + "\n" + rx
                }
            }
        })
    }

    @JvmStatic
    fun ForceClockUseSystemFontsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.clock.MiuiBaseClock", lpparam.classLoader, "updateViewsTextSize", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mTimeText = XposedHelpers.getObjectField(param.getThisObject(), "mTimeText") as? TextView ?: return
                mTimeText.typeface = Typeface.DEFAULT
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.clock.MiuiLeftTopLargeClock", lpparam.classLoader, "onLanguageChanged", String::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mTimeText = XposedHelpers.getObjectField(param.getThisObject(), "mCurrentDateLarge") as? TextView ?: return
                mTimeText.typeface = Typeface.DEFAULT
            }
        })
    }

    @JvmStatic
    fun HideMobileNetworkIndicatorHook(lpparam: PackageReadyParam) {
        val singleMobileType = MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_single")
        val showOnWifi = MainModule.mPrefs.getBoolean("system_statusbar_mobiletype_show_wificonnected")
        val hideMobileActivity = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val opt = MainModule.mPrefs.getStringAsInt("system_mobiletypeicon", 1)
                val hideIndicator = MainModule.mPrefs.getBoolean("system_networkindicator_mobile")
                val mMobileType = XposedHelpers.getObjectField(param.getThisObject(), "mMobileType") as? View ?: return
                val mobileIconState = param.getArg(0)
                val dataConnected = XposedHelpers.getObjectField(mobileIconState, "dataConnected") as? Boolean ?: false
                val wifiAvailable = XposedHelpers.getObjectField(mobileIconState, "wifiAvailable") as? Boolean ?: false
                if (opt == 3) {
                    if (singleMobileType) {
                        val mMobileTypeSingle = XposedHelpers.getObjectField(param.getThisObject(), "mMobileTypeSingle") as? TextView
                        mMobileTypeSingle?.visibility = View.GONE
                    } else {
                        mMobileType.visibility = View.GONE
                    }
                } else if (opt == 1) {
                    val viz = if (dataConnected && (!wifiAvailable || showOnWifi)) View.VISIBLE else View.GONE
                    if (singleMobileType) {
                        val mMobileTypeSingle = XposedHelpers.getObjectField(param.getThisObject(), "mMobileTypeSingle") as? TextView
                        mMobileTypeSingle?.visibility = viz
                    } else {
                        mMobileType.visibility = viz
                    }
                } else if (opt == 2) {
                    val viz = if (!wifiAvailable || showOnWifi) View.VISIBLE else View.GONE
                    if (singleMobileType) {
                        val mMobileTypeSingle = XposedHelpers.getObjectField(param.getThisObject(), "mMobileTypeSingle") as? TextView
                        mMobileTypeSingle?.visibility = viz
                    } else {
                        mMobileType.visibility = viz
                    }
                }
                val mLeftInOut = XposedHelpers.getObjectField(param.getThisObject(), "mLeftInOut") as? View ?: return
                if (hideIndicator) {
                    val mRightInOut = XposedHelpers.getObjectField(param.getThisObject(), "mRightInOut") as? View ?: return
                    mLeftInOut.visibility = View.GONE
                    mRightInOut.visibility = View.GONE
                }
                if (wifiAvailable && showOnWifi && !miui.os.Build.IS_INTERNATIONAL_BUILD && (dataConnected || opt == 2)) {
                    val mSmallHd = XposedHelpers.getObjectField(param.getThisObject(), "mSmallHd") as? View ?: return
                    mSmallHd.visibility = View.GONE
                }
                if (!singleMobileType) {
                    val mMobileLeftContainer = XposedHelpers.getObjectField(param.getThisObject(), "mMobileLeftContainer") as? View ?: return
                    mMobileLeftContainer.visibility = if (mMobileType.visibility == View.GONE && mLeftInOut.visibility == View.GONE) View.GONE else View.VISIBLE
                }
            }
        }
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "initViewState", hideMobileActivity)
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "updateState", hideMobileActivity)
    }
}
