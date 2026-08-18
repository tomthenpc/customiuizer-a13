package tv.withaibuild.customiuizer.mods

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Message
import android.widget.ImageView
import android.widget.TextView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.UsbDefaultFunctionMapper

object SystemSettingsMoreHooks {

    private var mUSBConnected = false

    @JvmStatic
    fun USBConfigHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.power.PowerManagerService", lpparam.classLoader, "systemReady", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val service = param.thisObject
                val mContext = XposedHelpers.getObjectField(service, "mContext") as? Context ?: return
                val usbStateReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        try {
                            val mConnected = intent.getBooleanExtra("connected", false)
                            if (mConnected && mConnected != mUSBConnected) {
                                try {
                                    val mPlugType = XposedHelpers.getIntField(service, "mPlugType")
                                    if (mPlugType != BatteryManager.BATTERY_PLUGGED_USB) return
                                    val func = UsbDefaultFunctionMapper.toA13Function(
                                        MainModule.mPrefs.getString("system_defaultusb", "none")
                                    ) ?: return
                                    val usbMgr = mContext.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
                                    if (XposedHelpers.callMethod(usbMgr, "isFunctionEnabled", func) as? Boolean == true) return
                                    XposedHelpers.callMethod(usbMgr, "setCurrentFunction", func, MainModule.mPrefs.getBoolean("system_defaultusb_unsecure"))
                                } catch (t: Throwable) {
                                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                                    XposedHelpers.log(t)
                                }
                                mUSBConnected = mConnected
                            }
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                            XposedHelpers.log(t)
                        }
                    }
                }
                ModuleHelper.registerModuleReceiver(
                    mContext,
                    "system.usbStateReceiver",
                    usbStateReceiver,
                    IntentFilter("android.hardware.usb.action.USB_STATE"),
                    Context.RECEIVER_NOT_EXPORTED
                )
            }
        })

        if (MainModule.mPrefs.getBoolean("system_defaultusb_unsecure")) {
            if (!ModuleHelper.findAndHookMethodSilently("com.android.server.usb.UsbDeviceManager\$UsbHandler", lpparam.classLoader, "isUsbDataTransferActive", Long::class.javaPrimitiveType, HookerClassHelper.returnConstant(true))) {
                ModuleHelper.findAndHookMethod("com.android.server.usb.UsbDeviceManager\$UsbHandler", lpparam.classLoader, "isUsbDataTransferActive", HookerClassHelper.returnConstant(true))
            }
            ModuleHelper.findAndHookMethod("com.android.server.usb.UsbDeviceManager\$UsbHandler", lpparam.classLoader, "handleMessage", Message::class.java, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val msg = param.args[0] as? Message ?: return
                    var setUnlockedFunc = 12
                    try {
                        setUnlockedFunc = XposedHelpers.getStaticIntField(XposedHelpers.findClass("com.android.server.usb.UsbDeviceManager", lpparam.classLoader), "MSG_SET_SCREEN_UNLOCKED_FUNCTIONS")
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    }
                    if (msg.what == setUnlockedFunc) {
                        msg.obj = 0L
                        param.args[0] = msg
                    }
                }
            })
        }
    }

    @JvmStatic
    fun USBConfigSettingsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethodSilently("com.android.settings.connecteddevice.usb.UsbModeChooserReceiver", lpparam.classLoader, "onReceive", Context::class.java, Intent::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val func = UsbDefaultFunctionMapper.toA13Function(
                    MainModule.mPrefs.getString("system_defaultusb", "none")
                )
                if (func != null) param.returnAndSkip(null)
            }
        })
    }
}
