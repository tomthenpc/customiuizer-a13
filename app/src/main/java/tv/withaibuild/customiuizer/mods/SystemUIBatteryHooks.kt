package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.BatteryIndicator

@Suppress("UNUSED_PARAMETER")
object SystemUIBatteryHooks {

    private const val STATUS_BAR_CLS = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"

    @JvmStatic
    fun BatteryIndicatorHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods(STATUS_BAR_CLS, lpparam.classLoader, "createAndAddWindows", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val sbWindowController = XposedHelpers.getObjectField(param.getThisObject(), "mStatusBarWindowController")
                val mStatusBarWindow = XposedHelpers.getObjectField(sbWindowController, "mStatusBarWindowView") as? ViewGroup ?: return

                val indicator = BatteryIndicator(mContext)
                val panel = mStatusBarWindow.findViewById<ViewGroup>(mContext.resources.getIdentifier("notification_panel", "id", lpparam.packageName))
                mStatusBarWindow.addView(indicator, panel?.let { mStatusBarWindow.indexOfChild(it) + 1 } ?: maxOf(mStatusBarWindow.childCount - 1, 2))
                indicator.setAdjustViewBounds(false)
                indicator.init(param.getThisObject())
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "mBatteryIndicator", indicator)
                val mNotificationIconAreaController = XposedHelpers.getObjectField(param.getThisObject(), "mNotificationIconAreaController")
                XposedHelpers.setAdditionalInstanceField(mNotificationIconAreaController, "mBatteryIndicator", indicator)
                val mBatteryController = XposedHelpers.getObjectField(param.getThisObject(), "mBatteryController")
                XposedHelpers.setAdditionalInstanceField(mBatteryController, "mBatteryIndicator", indicator)
                XposedHelpers.callMethod(mBatteryController, "fireBatteryLevelChanged")
                XposedHelpers.callMethod(mBatteryController, "firePowerSaveChanged")
            }
        })

        ModuleHelper.findAndHookMethod(STATUS_BAR_CLS, lpparam.classLoader, "setPanelExpanded", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val isKeyguardShowing = XposedHelpers.callMethod(param.getThisObject(), "isKeyguardShowing") as? Boolean ?: return
                val indicator = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mBatteryIndicator") as? BatteryIndicator ?: return
                indicator.onExpandingChanged(!isKeyguardShowing && param.getArgs()[0] as? Boolean ?: false)
            }
        })

        ModuleHelper.findAndHookMethod(STATUS_BAR_CLS, lpparam.classLoader, "setQsExpanded", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val isKeyguardShowing = XposedHelpers.callMethod(param.getThisObject(), "isKeyguardShowing") as? Boolean ?: return
                if (isKeyguardShowing) {
                    val indicator = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mBatteryIndicator") as? BatteryIndicator ?: return
                    indicator.onExpandingChanged(param.getArgs()[0] as? Boolean ?: false)
                }
            }
        })

        ModuleHelper.findAndHookMethod(STATUS_BAR_CLS, lpparam.classLoader, "updateIsKeyguard", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val isKeyguardShowing = XposedHelpers.callMethod(param.getThisObject(), "isKeyguardShowing") as? Boolean ?: return
                val indicator = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mBatteryIndicator") as? BatteryIndicator ?: return
                indicator.onKeyguardStateChanged(isKeyguardShowing)
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.NotificationIconAreaController", lpparam.classLoader, "onDarkChanged", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val indicator = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mBatteryIndicator") as? BatteryIndicator ?: return
                indicator.onDarkModeChanged(param.getArgs()[1] as? Float ?: 0f, param.getArgs()[2] as? Int ?: 0)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl", lpparam.classLoader, "fireBatteryLevelChanged", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val indicator = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mBatteryIndicator") as? BatteryIndicator ?: return
                val mLevel = XposedHelpers.getIntField(param.getThisObject(), "mLevel")
                val mCharging = XposedHelpers.getBooleanField(param.getThisObject(), "mCharging")
                val mCharged = XposedHelpers.getBooleanField(param.getThisObject(), "mCharged")
                indicator.onBatteryLevelChanged(mLevel, mCharging, mCharged)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.BatteryControllerImpl", lpparam.classLoader, "firePowerSaveChanged", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val indicator = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mBatteryIndicator") as? BatteryIndicator ?: return
                indicator.onPowerSaveChanged(XposedHelpers.getBooleanField(param.getThisObject(), "mPowerSave"))
            }
        })
    }

    @JvmStatic
    fun StatusBarStyleBatteryIconHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.views.MiuiBatteryMeterView", lpparam.classLoader, "updateAll", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val batteryView = param.getThisObject() as? LinearLayout ?: return
                val mBatteryTextDigitView = XposedHelpers.getObjectField(param.getThisObject(), "mBatteryTextDigitView") as? TextView ?: return
                val mBatteryPercentView = XposedHelpers.getObjectField(param.getThisObject(), "mBatteryPercentView") as? TextView ?: return
                val mBatteryPercentMarkView = XposedHelpers.getObjectField(param.getThisObject(), "mBatteryPercentMarkView") as? TextView ?: return
                if (MainModule.mPrefs.getBoolean("system_statusbaricons_swap_batteryicon_percentage")) {
                    batteryView.removeView(mBatteryPercentView)
                    batteryView.removeView(mBatteryPercentMarkView)
                    batteryView.addView(mBatteryPercentMarkView, 0)
                    batteryView.addView(mBatteryPercentView, 0)
                }
                var fontSize = MainModule.mPrefs.getInt("system_statusbar_batterystyle_fontsize", 15) * 0.5f
                if (fontSize > 7.5f) {
                    mBatteryTextDigitView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
                    mBatteryPercentView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
                }
                fontSize = MainModule.mPrefs.getInt("system_statusbar_batterystyle_mark_fontsize", 15) * 0.5f
                if (fontSize > 7.5f) {
                    mBatteryPercentMarkView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
                }
                if (MainModule.mPrefs.getBoolean("system_statusbar_batterystyle_bold")) {
                    mBatteryTextDigitView.typeface = Typeface.DEFAULT_BOLD
                    mBatteryPercentView.typeface = Typeface.DEFAULT_BOLD
                }
                val res = batteryView.resources
                var leftMargin = MainModule.mPrefs.getInt("system_statusbar_batterystyle_leftmargin", 0)
                leftMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, leftMargin * 0.5f, res.displayMetrics).toInt()
                var rightMargin = MainModule.mPrefs.getInt("system_statusbar_batterystyle_rightmargin", 0)
                rightMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rightMargin * 0.5f, res.displayMetrics).toInt()
                var topMargin = 0
                var verticalOffset = MainModule.mPrefs.getInt("system_statusbar_batterystyle_verticaloffset", 8)
                if (verticalOffset != 8) {
                    val marginTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (verticalOffset - 8) * 0.5f, res.displayMetrics)
                    topMargin = marginTop.toInt()
                }
                val digitRightMargin: Int
                val markRightMargin: Int
                if (MainModule.mPrefs.getBoolean("system_statusbaricons_battery4")) {
                    digitRightMargin = rightMargin
                    markRightMargin = 0
                } else {
                    digitRightMargin = 0
                    markRightMargin = rightMargin
                }
                if (leftMargin > 0 || topMargin != 8 || digitRightMargin > 0) {
                    mBatteryPercentView.setPaddingRelative(leftMargin, topMargin, digitRightMargin, 0)
                }

                verticalOffset = MainModule.mPrefs.getInt("system_statusbar_batterystyle_mark_verticaloffset", 17)
                if (verticalOffset < 17) {
                    val marginTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (verticalOffset - 8) * 0.5f, res.displayMetrics)
                    topMargin = marginTop.toInt()
                }
                if (verticalOffset < 17 || markRightMargin > 0) {
                    mBatteryPercentMarkView.setPaddingRelative(0, topMargin, markRightMargin, 0)
                }
            }
        })
    }
}
