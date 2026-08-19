package tv.withaibuild.customiuizer.mods

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.provider.Settings
import android.util.ArrayMap
import android.view.View
import android.widget.Switch
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import miui.telephony.TelephonyManager
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

@Suppress("UNUSED_PARAMETER")
object SystemUIMonitorAndTileHooks {

    @JvmStatic
    fun AddCustomTileHook(lpparam: PackageReadyParam) {
        val enable5G = MainModule.mPrefs.getBoolean("system_fivegtile")
        val enableFps = MainModule.mPrefs.getBoolean("system_cc_fpstile")

        ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", object : MethodHook() {
            private var isListened = false
            override fun after(param: AfterHookCallback) {
                if (!isListened) {
                    isListened = true
                    val mContext = XposedHelpers.callMethod(param.getThisObject(), "getApplicationContext") as? Context ?: return
                    val stockTilesResId = mContext.resources.getIdentifier("miui_quick_settings_tiles_stock", "string", lpparam.packageName)
                    var stockTiles = mContext.getString(stockTilesResId)
                    if (enable5G) stockTiles = "$stockTiles,custom_5G"
                    if (enableFps) stockTiles = "$stockTiles,custom_FPS"
                    MainModule.getResHooks().setObjectReplacement("com.android.systemui", "string", "miui_quick_settings_tiles_stock", stockTiles)
                    MainModule.getResHooks().setObjectReplacement("miui.systemui.plugin", "string", "miui_quick_settings_tiles_stock", stockTiles)
                    MainModule.getResHooks().setObjectReplacement("miui.systemui.plugin", "string", "quick_settings_tiles_stock", stockTiles)
                }
            }
        })

        val QSFactoryCls = "com.android.systemui.qs.tileimpl.MiuiQSFactory"
        val ResourceIconClass = XposedHelpers.findClassIfExists(
            "com.android.systemui.qs.tileimpl.QSTileImpl\$ResourceIcon",
            lpparam.classLoader
        )
        ModuleHelper.findAndHookMethod(QSFactoryCls, lpparam.classLoader, "createTileInternal", String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tileName = param.getArg(0) as? String ?: return
                if (tileName.startsWith("custom_")) {
                    val nfcField = "nfcTileProvider"
                    val provider = XposedHelpers.getObjectField(param.getThisObject(), nfcField)
                    val tile = XposedHelpers.callMethod(provider, "get")
                    XposedHelpers.setAdditionalInstanceField(tile, "customName", tileName)
                    param.returnAndSkip(tile)
                }
            }
        })

        val NfcTileCls = "com.android.systemui.qs.tiles.MiuiNfcTile"
        ModuleHelper.findAndHookMethod(NfcTileCls, lpparam.classLoader, "isAvailable", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tileName = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "customName") as? String ?: return
                when (tileName) {
                    "custom_5G" -> param.returnAndSkip(enable5G && TelephonyManager.getDefault().isFiveGCapable)
                    "custom_FPS" -> param.returnAndSkip(enableFps)
                    else -> param.returnAndSkip(false)
                }
            }
        })
        ModuleHelper.findAndHookMethod(NfcTileCls, lpparam.classLoader, "getTileLabel", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tileName = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "customName") as? String ?: return
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val modRes = ModuleHelper.getModuleRes(mContext)
                when (tileName) {
                    "custom_5G" -> param.returnAndSkip(modRes.getString(R.string.qs_toggle_5g))
                    "custom_FPS" -> param.returnAndSkip(modRes.getString(R.string.qs_toggle_fps))
                }
            }
        })
        ModuleHelper.findAndHookMethod(NfcTileCls, lpparam.classLoader, "handleSetListening", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tileName = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "customName") as? String ?: return
                val mListening = param.getArg(0) as? Boolean ?: return
                if (tileName == "custom_5G") {
                    val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                    if (mListening) {
                        val tile = param.getThisObject()
                        val resolver = mContext.contentResolver
                        val contentObserver = object : ContentObserver(Handler(mContext.mainLooper)) {
                            override fun onChange(selfChange: Boolean) {
                                ModuleHelper.guarded {
                                    XposedHelpers.callMethod(tile, "refreshState")
                                }
                            }
                        }
                        resolver.registerContentObserver(Settings.Global.getUriFor("fiveg_user_enable"), false, contentObserver)
                        resolver.registerContentObserver(Settings.Global.getUriFor("dual_nr_enabled"), false, contentObserver)
                        ModuleHelper.replaceModuleRegistration(
                            "systemui.custom5gObserver",
                            Runnable {
                                ModuleHelper.guarded("SystemUIMonitorAndTileHooks.unregister5gObserver") {
                                    resolver.unregisterContentObserver(contentObserver)
                                }
                            }
                        )
                    } else {
                        ModuleHelper.clearModuleRegistration("systemui.custom5gObserver")
                    }
                } else if (tileName == "custom_FPS") {
                    if (mListening) {
                        val ServiceManager = XposedHelpers.findClass("android.os.ServiceManager", lpparam.classLoader)
                        val mSurfaceFlinger = XposedHelpers.callStaticMethod(ServiceManager, "getService", "SurfaceFlinger")
                        XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "mSurfaceFlinger", mSurfaceFlinger)
                    } else {
                        XposedHelpers.removeAdditionalInstanceField(param.getThisObject(), "mSurfaceFlinger")
                    }
                }
                param.returnAndSkip(null)
            }
        })
        ModuleHelper.findAndHookMethod(NfcTileCls, lpparam.classLoader, "getLongClickIntent", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tileName = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "customName") as? String ?: return
                if (tileName == "custom_5G") {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    intent.component = ComponentName("com.android.phone", "com.android.phone.settings.MiuiFiveGNetworkSetting")
                    param.returnAndSkip(intent)
                } else {
                    param.returnAndSkip(null)
                }
            }
        })
        ModuleHelper.findAndHookMethod(NfcTileCls, lpparam.classLoader, "handleClick", View::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tileName = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "customName") as? String ?: return
                if (tileName == "custom_5G") {
                    val manager = TelephonyManager.getDefault()
                    manager.setUserFiveGEnabled(!manager.isUserFiveGEnabled)
                } else if (tileName == "custom_FPS") {
                    val mSurfaceFlinger = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mSurfaceFlinger") as? IBinder ?: return
                    val mState = XposedHelpers.getObjectField(param.getThisObject(), "mState")
                    val enabled = XposedHelpers.getBooleanField(mState, "value")
                    val obtain = Parcel.obtain()
                    obtain.writeInterfaceToken("android.ui.ISurfaceComposer")
                    obtain.writeInt(if (enabled) 0 else 1)
                    mSurfaceFlinger.transact(1034, obtain, null, 0)
                    obtain.recycle()
                    XposedHelpers.callMethod(param.getThisObject(), "refreshState")
                }
                param.returnAndSkip(null)
            }
        })

        val tileOnResMap = ArrayMap<String, Int>()
        val tileOffResMap = ArrayMap<String, Int>()
        if (enable5G) {
            tileOnResMap["custom_5G"] = MainModule.getResHooks().addResource("ic_qs_m5g_on", R.drawable.ic_qs_5g_on)
            tileOffResMap["custom_5G"] = MainModule.getResHooks().addResource("ic_qs_m5g_off", R.drawable.ic_qs_5g_off)
        }
        if (enableFps) {
            tileOnResMap["custom_FPS"] = MainModule.getResHooks().addResource("ic_qs_mfps_on", R.drawable.ic_qs_fps_on)
            tileOffResMap["custom_FPS"] = MainModule.getResHooks().addResource("ic_qs_mfps_off", R.drawable.ic_qs_fps_off)
        }
        ModuleHelper.hookAllMethods(NfcTileCls, lpparam.classLoader, "handleUpdateState", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val tileName = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "customName") as? String ?: return
                var isEnable = false
                if (tileName == "custom_5G") {
                    isEnable = TelephonyManager.getDefault().isUserFiveGEnabled
                } else if (tileName == "custom_FPS") {
                    val mSurfaceFlinger = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mSurfaceFlinger") as? IBinder
                    if (mSurfaceFlinger == null) {
                        isEnable = false
                    } else {
                        val obtain = Parcel.obtain()
                        val obtain2 = Parcel.obtain()
                        obtain.writeInterfaceToken("android.ui.ISurfaceComposer")
                        obtain.writeInt(2)
                        mSurfaceFlinger.transact(1034, obtain, obtain2, 0)
                        isEnable = obtain2.readBoolean()
                        obtain2.recycle()
                        obtain.recycle()
                    }
                }
                if (tileName.startsWith("custom_")) {
                    val booleanState = param.getArg(0)
                    XposedHelpers.setObjectField(booleanState, "value", isEnable)
                    XposedHelpers.setObjectField(booleanState, "state", if (isEnable) 2 else 1)
                    val tileLabel = XposedHelpers.callMethod(param.getThisObject(), "getTileLabel") as? String
                    XposedHelpers.setObjectField(booleanState, "label", tileLabel)
                    XposedHelpers.setObjectField(booleanState, "contentDescription", tileLabel)
                    XposedHelpers.setObjectField(booleanState, "expandedAccessibilityClassName", Switch::class.java.name)
                    if (ResourceIconClass != null) {
                        val mIcon = XposedHelpers.callStaticMethod(
                            ResourceIconClass,
                            "get",
                            if (isEnable) tileOnResMap[tileName] else tileOffResMap[tileName]
                        )
                        XposedHelpers.setObjectField(booleanState, "icon", mIcon)
                    }
                }
                param.returnAndSkip(null)
            }
        })
    }
}
