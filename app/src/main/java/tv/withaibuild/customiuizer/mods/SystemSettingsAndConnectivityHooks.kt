package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.Button
import android.widget.TextView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

object SystemSettingsAndConnectivityHooks {

    @JvmStatic
    fun ViewWifiPasswordHook(lpparam: PackageReadyParam) {
        val titleId = MainModule.resHooks.addResource("system_wifipassword_btn_title", R.string.system_wifipassword_btn_title)
        val dlgTitleId = MainModule.resHooks.addResource("system_wifi_password_dlgtitle", R.string.system_wifi_password_dlgtitle)

        ModuleHelper.hookAllMethods("com.android.settings.wifi.SavedAccessPointPreference", lpparam.classLoader, "onBindViewHolder", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val view = XposedHelpers.getObjectField(param.thisObject, "mView") as? View ?: return
                val btnId = view.resources.getIdentifier("btn_delete", "id", "com.android.settings")
                val button = view.findViewById<Button>(btnId)
                button?.setText(titleId)
            }
        })

        val wifiSharedKey = arrayOfNulls<String>(1)
        val passwordTitle = arrayOfNulls<String>(1)

        ModuleHelper.findAndHookMethod("miuix.appcompat.app.AlertDialog\$Builder", lpparam.classLoader, "setTitle", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (wifiSharedKey[0] != null) {
                    param.args[0] = dlgTitleId
                }
            }
        })

        ModuleHelper.findAndHookMethod("miuix.appcompat.app.AlertDialog\$Builder", lpparam.classLoader, "setMessage", CharSequence::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (wifiSharedKey[0] != null) {
                    val title = passwordTitle[0] ?: return
                    val str = (param.args[0] as? CharSequence ?: return).toString() + "\n" + title + ": " + wifiSharedKey[0]
                    param.args[0] = str
                }
            }
        })

        ModuleHelper.hookAllMethods("miuix.appcompat.app.AlertDialog", lpparam.classLoader, "onCreate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (wifiSharedKey[0] != null) {
                    val messageView = XposedHelpers.callMethod(param.thisObject, "getMessageView") as? TextView ?: return
                    messageView.setTextIsSelectable(true)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings", lpparam.classLoader, "showDeleteDialog", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val wifiEntry = param.args[0]
                val canShare = XposedHelpers.callMethod(wifiEntry, "canShare") as? Boolean ?: false
                if (canShare) {
                    if (passwordTitle[0] == null) {
                        val context = XposedHelpers.callMethod(param.thisObject, "getContext") as? Context ?: return
                        val modRes = ModuleHelper.getModuleRes(context)
                        passwordTitle[0] = modRes.getString(R.string.system_wifi_password_label)
                    }
                    val mWifiManager = XposedHelpers.getObjectField(param.thisObject, "mWifiManager")
                    val wifiConfiguration = XposedHelpers.callMethod(wifiEntry, "getWifiConfiguration")
                    val WifiDppUtilsClass = XposedHelpers.findClass("com.android.settings.wifi.dpp.WifiDppUtils", lpparam.classLoader)
                    var sharedKey = XposedHelpers.callStaticMethod(WifiDppUtilsClass, "getPresharedKey", mWifiManager, wifiConfiguration) as? String ?: return
                    sharedKey = XposedHelpers.callStaticMethod(WifiDppUtilsClass, "removeFirstAndLastDoubleQuotes", sharedKey) as? String ?: return
                    wifiSharedKey[0] = sharedKey
                }
            }

            override fun after(param: AfterHookCallback) {
                val wifiEntry = param.args[0]
                val canShare = XposedHelpers.callMethod(wifiEntry, "canShare") as? Boolean ?: false
                if (canShare) {
                    wifiSharedKey[0] = null
                }
            }
        })
    }

    @JvmStatic
    fun HideCCOperatorHook(lpparam: PackageReadyParam) {
        val hideOperatorHook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mCarrierText = try {
                    XposedHelpers.getObjectField(param.thisObject, "carrierText") as? TextView
                } catch (e: Throwable) {
                    XposedHelpers.getObjectField(param.thisObject, "mCarrierText") as? TextView
                } ?: return
                mCarrierText.visibility = View.GONE
            }
        }

        val hookedFlaresInfo = ModuleHelper.hookAllMethodsSilently("com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar", lpparam.classLoader, "updateFlaresInfo", hideOperatorHook)
        if (!hookedFlaresInfo) {
            ModuleHelper.findAndHookMethod("com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar", lpparam.classLoader, "onFinishInflate", hideOperatorHook)
        }

        ModuleHelper.findAndHookMethod("com.android.systemui.qs.MiuiNotificationHeaderView", lpparam.classLoader, "updateCarrierTextVisibility", hideOperatorHook)

        if (!ModuleHelper.findAndHookMethodSilently("com.android.systemui.qs.MiuiQSHeaderView", lpparam.classLoader, "updateCarrierVisibility", hideOperatorHook)) {
            ModuleHelper.findAndHookMethod("com.android.systemui.qs.MiuiQSHeaderView", lpparam.classLoader, "onFinishInflate", hideOperatorHook)
        }
    }

    @JvmStatic
    fun HideCCOperatorDelimiterHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController", lpparam.classLoader, "fireCarrierTextChanged", String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mCurrentCarrier = param.args[0] as? String ?: return
                param.args[0] = mCurrentCarrier.replace(" | ", "")
            }
        })
    }

    @JvmStatic
    fun CollapseCCAfterClickHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader, "click", View::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mState = XposedHelpers.callMethod(param.thisObject, "getState")
                val state = XposedHelpers.getIntField(mState, "state")
                if (state != 0) {
                    val tileSpec = XposedHelpers.callMethod(param.thisObject, "getTileSpec") as? String ?: return
                    if ("edit" != tileSpec) {
                        val mHost = XposedHelpers.getObjectField(param.thisObject, "mHost")
                        XposedHelpers.callMethod(mHost, "collapsePanels")
                    }
                }
            }
        })
    }

    @JvmStatic
    fun DisableBluetoothRestrictHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.settingslib.bluetooth.LocalBluetoothAdapter", lpparam.classLoader, "isSupportBluetoothRestrict", Context::class.java, HookerClassHelper.returnConstant(false))
    }
}
