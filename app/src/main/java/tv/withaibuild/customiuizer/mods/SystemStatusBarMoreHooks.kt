package tv.withaibuild.customiuizer.mods

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.HookUtils

object SystemStatusBarMoreHooks {

    @JvmStatic
    fun HideIconsBattery1Hook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.views.MiuiBatteryMeterView", lpparam.classLoader, "initMiuiView", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mBatteryIconView = XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView") as? ImageView ?: return
                mBatteryIconView.visibility = View.GONE
            }
        })
    }

    @JvmStatic
    fun HideIconsBattery2Hook(lpparam: PackageReadyParam) {
        val hideNormalPercentage = MainModule.mPrefs.getBoolean("system_statusbaricons_battery2")
        val batteryId = ResourceHooks.getFakeResId("batterview_in_statusbar")
        if (hideNormalPercentage) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mBatteryView = XposedHelpers.getObjectField(param.thisObject, "mBattery") as? View ?: return
                    mBatteryView.setTag(batteryId, true)
                }
            })
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mBatteryView = XposedHelpers.getObjectField(param.thisObject, "mBatteryView") as? View ?: return
                    mBatteryView.setTag(batteryId, true)
                }
            })
        }
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.views.MiuiBatteryMeterView", lpparam.classLoader, "updateChargeAndText", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (MainModule.mPrefs.getBoolean("system_statusbaricons_battery4")) {
                    val mBatteryPercentMarkView = XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentMarkView") as? TextView ?: return
                    mBatteryPercentMarkView.visibility = View.GONE
                }
                if (MainModule.mPrefs.getBoolean("system_statusbaricons_battery3")) {
                    val mBatteryChargingView = XposedHelpers.getObjectField(param.thisObject, "mBatteryChargingView") as? ImageView ?: return
                    mBatteryChargingView.visibility = View.GONE
                    try {
                        val mBatteryChargingInView = XposedHelpers.getObjectField(param.thisObject, "mBatteryChargingInView") as? ImageView
                        mBatteryChargingInView?.visibility = View.GONE
                    } catch (ignore: Throwable) {
                        if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
}
                }
                if (hideNormalPercentage) {
                    val mBatteryView = param.thisObject as? View ?: return
                    if (mBatteryView.getTag(batteryId) != null) {
                        val percentView = XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentMarkView") as? View ?: return
                        percentView.visibility = View.GONE
                        val percentView2 = XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentView") as? View ?: return
                        percentView2.visibility = View.GONE
                    }
                }
            }
        })
    }

    private var lastState = false

    private fun updateAlarmVisibility(thisObject: Any, state: Boolean) {
        try {
            var mIconController = XposedHelpers.getObjectField(thisObject, "mIconController")
            if (!state) {
                XposedHelpers.callMethod(mIconController, "setIconVisibility", "alarm_clock", false)
                return
            }

            val mContext = XposedHelpers.getObjectField(thisObject, "mContext") as? Context ?: return
            val nowTime = java.lang.System.currentTimeMillis()
            val nextTime = try {
                XposedHelpers.getAdditionalInstanceField(thisObject, "mNextAlarmTime") as? Long
            } catch (t: Throwable) {
                if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                null
            } ?: ModuleHelper.getNextMIUIAlarmTime(mContext)
            var finalNextTime = nextTime
            if (finalNextTime == 0L) finalNextTime = HookUtils.getNextStockAlarmTime(mContext)

            var diffMSec = finalNextTime - nowTime
            if (diffMSec < 0) diffMSec += 7 * 24 * 60 * 60 * 1000
            val diffHours = (diffMSec - 59 * 1000) / (1000f * 60f * 60f)
            val vis = diffHours <= MainModule.mPrefs.getInt("system_statusbaricons_alarmn", 0).toFloat()
            XposedHelpers.callMethod(mIconController, "setIconVisibility", "alarm_clock", vis)
            mIconController = XposedHelpers.getObjectField(thisObject, "miuiDripLeftStatusBarIconController")
            XposedHelpers.callMethod(mIconController, "setIconVisibility", "alarm_clock", vis)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            XposedHelpers.log(t)
        }
    }

    @JvmStatic
    fun HideIconsSelectiveAlarmHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val thisObject = param.thisObject
                val mContext = XposedHelpers.getObjectField(thisObject, "mContext") as? Context ?: return
                XposedHelpers.setAdditionalInstanceField(thisObject, "mNextAlarmTime", ModuleHelper.getNextMIUIAlarmTime(mContext))
                val resolver = mContext.contentResolver

                val alarmObserver = object : ContentObserver(Handler(mContext.mainLooper)) {
                    override fun onChange(selfChange: Boolean) {
                        ModuleHelper.guarded {
                            if (selfChange) return@guarded
                            XposedHelpers.setAdditionalInstanceField(thisObject, "mNextAlarmTime", ModuleHelper.getNextMIUIAlarmTime(mContext))
                            updateAlarmVisibility(thisObject, lastState)
                        }
                    }
                }
                resolver.registerContentObserver(Settings.System.getUriFor("next_alarm_clock_formatted"), false, alarmObserver)
                ModuleHelper.replaceModuleRegistration(
                    "systemui.alarmObserver",
                    Runnable {
                        ModuleHelper.guarded("SystemStatusBarMoreHooks.unregisterAlarmObserver") {
                            resolver.unregisterContentObserver(alarmObserver)
                        }
                    }
                )

                val filter = IntentFilter().apply {
                    addAction("android.intent.action.TIME_TICK")
                    addAction("android.intent.action.TIME_SET")
                    addAction("android.intent.action.TIMEZONE_CHANGED")
                    addAction("android.intent.action.LOCALE_CHANGED")
                }
                val alarmReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        ModuleHelper.guarded("SystemStatusBarMoreHooks.alarmReceiver") {
                            updateAlarmVisibility(thisObject, lastState)
                        }
                    }
                }
                ModuleHelper.registerModuleReceiver(
                    mContext,
                    "systemui.alarmReceiver",
                    alarmReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", lpparam.classLoader, "updateAlarm", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                lastState = XposedHelpers.getObjectField(param.thisObject, "mHasAlarm") as? Boolean ?: false
                updateAlarmVisibility(param.thisObject, lastState)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy", lpparam.classLoader, "onMiuiAlarmChanged", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                lastState = XposedHelpers.getObjectField(param.thisObject, "mHasAlarm") as? Boolean ?: false
                updateAlarmVisibility(param.thisObject, lastState)
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun HideIconsBluetoothHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy", lpparam.classLoader, "updateBluetooth", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val opt = MainModule.mPrefs.getString("system_statusbaricons_bluetooth", "1").toInt()
                val isBluetoothConnected = XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mBluetooth"), "isBluetoothConnected") as? Boolean ?: false
                if (opt == 3 || (opt == 2 && !isBluetoothConnected)) {
                    val mIconController = XposedHelpers.getObjectField(param.thisObject, "mIconController")
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth", false)
                }
            }
        })
    }

    @JvmStatic
    fun DisplayWifiStandardHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarWifiView", lpparam.classLoader, "applyWifiState", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val wifiState = param.getArg(0)
                if (wifiState != null) {
                    val opt = MainModule.mPrefs.getStringAsInt("system_statusbaricons_wifistandard", 1)
                    if (opt == 1) return
                    val wifiStandard = XposedHelpers.getObjectField(wifiState, "wifiStandard") as? Int ?: 0
                    XposedHelpers.setObjectField(wifiState, "showWifiStandard", opt == 2 && wifiStandard > 0)
                }
            }
        })
    }

    @JvmStatic
    fun MobileNetworkTypeHook(lpparam: PackageReadyParam) {
        val MobileController = "com.android.systemui.statusbar.connectivity.MobileSignalController"
        ModuleHelper.findAndHookMethod(MobileController, lpparam.classLoader, "getMobileTypeName", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val net = param.result as? String ?: return
                if (MainModule.mPrefs.getBoolean("system_4gtolte")) {
                    when (net) {
                        "4G" -> param.setResult("LTE")
                        "4G+" -> param.setResult("LTE+")
                    }
                } else {
                    val mobileType = MainModule.mPrefs.getString("system_statusbar_mobile_showname", "")
                    param.setResult(mobileType)
                }
            }
        })
    }

    @JvmStatic
    fun NetworkIndicatorWifi(lpparam: PackageReadyParam) {
        val hideWifiActivity = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mWifiActivityView = XposedHelpers.getObjectField(param.thisObject, "mWifiActivityView")
                XposedHelpers.callMethod(mWifiActivityView, "setVisibility", View.INVISIBLE)
            }
        }
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarWifiView", lpparam.classLoader, "applyWifiState", hideWifiActivity)
    }
}
