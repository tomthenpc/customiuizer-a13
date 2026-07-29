package tv.withaibuild.customiuizer.mods

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.ArrayMap
import android.view.View
import java.lang.reflect.Method
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

object SystemLockScreenMoreHooks {

    private const val StatusBarCls = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"

    @JvmStatic
    fun AppLockHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.miui.server.SecurityManagerService", lpparam.classLoader, "removeAccessControlPassLocked", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if ("*" != param.args[1]) return
                val mode = XposedHelpers.callMethod(param.thisObject, "getAccessControlLockMode", param.args[0]) as? Int ?: 0
                if (mode != 1) param.returnAndSkip(null)
            }
        })
    }

    @Suppress("UNCHECKED_CAST")
    private fun saveLastCheck(thisObject: Any, pkgName: String?, userId: Int) {
        val enabled = if (pkgName != null && pkgName != "com.miui.home") {
            XposedHelpers.callMethod(thisObject, "getApplicationAccessControlEnabledAsUser", pkgName, userId) as? Boolean ?: false
        } else false
        val userState = XposedHelpers.callMethod(thisObject, "getUserStateLocked", userId)
        XposedHelpers.setAdditionalInstanceField(userState, "mAccessControlLastCheckSaved",
            if (enabled) ArrayMap<String, Long>(XposedHelpers.getObjectField(userState, "mAccessControlLastCheck") as? ArrayMap<String, Long>?) else null
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkLastCheck(thisObject: Any, userId: Int) {
        val userState = XposedHelpers.callMethod(thisObject, "getUserStateLocked", userId)
        val mAccessControlLastCheckSaved = XposedHelpers.getAdditionalInstanceField(userState, "mAccessControlLastCheckSaved") as? ArrayMap<String, Long> ?: return
        val mAccessControlLastCheck = XposedHelpers.getObjectField(userState, "mAccessControlLastCheck") as? ArrayMap<String, Long> ?: return
        if (mAccessControlLastCheck.size == 0) return
        val timeout = MainModule.mPrefs.getInt("system_applock_timeout", 1) * 60L * 1000L
        for ((pkg, time) in mAccessControlLastCheck) {
            if (mAccessControlLastCheckSaved.containsKey(pkg)) {
                val oldTime = mAccessControlLastCheckSaved[pkg]
                if (time != oldTime) {
                    mAccessControlLastCheck[pkg] = time + (timeout - 60000L)
                    XposedHelpers.setObjectField(userState, "mAccessControlLastCheck", mAccessControlLastCheck)
                }
            } else {
                mAccessControlLastCheck[pkg] = time + (timeout - 60000L)
                XposedHelpers.setObjectField(userState, "mAccessControlLastCheck", mAccessControlLastCheck)
            }
        }
    }

    @JvmStatic
    fun AppLockTimeoutHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.miui.server.SecurityManagerService", lpparam.classLoader, "addAccessControlPassForUser", String::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                saveLastCheck(param.thisObject, param.args[0] as? String, param.args[1] as? Int ?: 0)
            }

            override fun after(param: AfterHookCallback) {
                checkLastCheck(param.thisObject, param.args[1] as? Int ?: 0)
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.server.SecurityManagerService", lpparam.classLoader, "checkAccessControlPassLocked", String::class.java, Intent::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                saveLastCheck(param.thisObject, param.args[0] as? String, param.args[2] as? Int ?: 0)
            }

            override fun after(param: AfterHookCallback) {
                checkLastCheck(param.thisObject, param.args[2] as? Int ?: 0)
            }
        })
    }

    @JvmStatic
    fun SkipAppLockHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.miui.server.AccessController", lpparam.classLoader, "skipActivity", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val intent = param.args[0] as? Intent ?: return
                if (intent.component == null) return
                val pkgName = intent.component!!.packageName
                val actName = intent.component!!.className
                val key = "system_applock_skip_activities"
                val itemStr = MainModule.mPrefs.getString(key, "")
                if (itemStr.isEmpty()) return
                val itemArr = itemStr.trim().split("\\|".toRegex())
                for (uuid in itemArr) {
                    val pkgAct = MainModule.mPrefs.getString(key + "_" + uuid + "_activity", "")
                    if (pkgAct == "$pkgName|$actName") param.setResult(true)
                }
            }
        })
    }

    @JvmStatic
    fun HideLockScreenClockHook(lpparam: PackageReadyParam) {
        val hideClockHook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mClockFrame = XposedHelpers.getObjectField(param.thisObject, "mClockFrame") as? View ?: return
                mClockFrame.visibility = View.INVISIBLE
                val mLargeClockFrame = XposedHelpers.getObjectField(param.thisObject, "mLargeClockFrame") as? View ?: return
                mLargeClockFrame.visibility = View.INVISIBLE
            }
        }
        ModuleHelper.hookAllMethods("com.android.keyguard.KeyguardClockSwitch", lpparam.classLoader, "setClockPlugin", hideClockHook)
        ModuleHelper.findAndHookMethod("com.android.keyguard.KeyguardClockSwitch", lpparam.classLoader, "updateClockViews", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, hideClockHook)
    }

    @JvmStatic
    fun HideLockScreenHintHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController", lpparam.classLoader, "hasIndicationsExceptResting", HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun HideLockScreenStatusBarHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods(StatusBarCls, lpparam.classLoader, "makeStatusBarView", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mKeyguardStatusBar = XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mNotificationPanelViewController"), "mKeyguardStatusBar") as? View ?: return
                mKeyguardStatusBar.translationY = -999f
            }
        })
    }

    @Suppress("RedundantIfStatement")
    private fun isAuthOnce(): Boolean {
        val req = MainModule.mPrefs.getStringAsInt("system_noscreenlock_req", 1)
        if (req <= 1) return true
        if (req == 2 && !isUnlockedWithFingerprint && !isUnlockedWithStrong) return false
        if (req == 3 && !isUnlockedWithStrong) return false
        return true
    }

    private fun isTrusted(mContext: Context, classLoader: ClassLoader): Boolean {
        return isTrustedWiFi(mContext) || isTrustedBt(classLoader)
    }

    private fun isTrustedWiFi(mContext: Context): Boolean {
        val wifiManager = mContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
        if (!wifiManager.isWifiEnabled) return false
        val trustedNetworks = MainModule.mPrefs.getStringSet("system_noscreenlock_wifi")
        return tv.withaibuild.customiuizer.utils.Helpers.containsStringPair(trustedNetworks, wifiManager.connectionInfo?.bssid)
    }

    @SuppressLint("MissingPermission")
    private fun isTrustedBt(classLoader: ClassLoader): Boolean {
        try {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!mBluetoothAdapter.isEnabled) return false
            val trustedDevices = MainModule.mPrefs.getStringSet("system_noscreenlock_bt")
            val mController = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("com.android.systemui.Dependency", classLoader),
                "get",
                XposedHelpers.findClass("com.android.systemui.statusbar.policy.BluetoothController", classLoader)
            )
            val cachedDevices = XposedHelpers.callMethod(mController, "getDevices") as? Collection<*> ?: return false
            for (device in cachedDevices) {
                val mDevice = XposedHelpers.getObjectField(device, "mDevice") as? BluetoothDevice ?: continue
                if (mDevice.bondState == BluetoothDevice.BOND_BONDED &&
                    XposedHelpers.callMethod(device, "isConnected") as? Boolean == true &&
                    tv.withaibuild.customiuizer.utils.Helpers.containsStringPair(trustedDevices, mDevice.address)
                ) return true
            }
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
        return false
    }

    private fun isUnlocked(mContext: Context, classLoader: ClassLoader): Boolean {
        val kmvClas = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", classLoader)
        XposedHelpers.setAdditionalStaticField(kmvClas, "isScreenLockDisabled", false)
        if (!isAuthOnce()) return false
        var opt = MainModule.mPrefs.getStringAsInt("system_noscreenlock", 1)
        if (forcedOption == 1) opt = 2
        var isTrusted = false
        if (opt == 3) isTrusted = isTrusted(mContext, classLoader)
        if (opt == 2 || opt == 3 && isTrusted) {
            XposedHelpers.setAdditionalStaticField(kmvClas, "isScreenLockDisabled", true)
            return true
        }
        return false
    }

    private var isUnlockedInnerCall = false
    private var isUnlockedWithFingerprint = false
    private var isUnlockedWithStrong = false
    private var forcedOption = -1

    @JvmStatic
    fun NoScreenLockHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.keyguard.KeyguardViewMediator", lpparam.classLoader, "handleKeyguardDone", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (isUnlockedInnerCall) {
                    isUnlockedInnerCall = false
                    return
                }
                isUnlockedWithStrong = true
            }
        })

        ModuleHelper.findAndHookMethod("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader, "onFingerprintAuthenticated", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                isUnlockedWithFingerprint = true
            }
        })

        ModuleHelper.hookAllConstructors("com.android.systemui.keyguard.KeyguardSecurityContainerController", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val controller = param.thisObject
                val mContext = XposedHelpers.callMethod(controller, "getContext") as? Context ?: return
                val oldReceiver = XposedHelpers.getAdditionalInstanceField(controller, "strongAuthReceiver") as? BroadcastReceiver
                if (oldReceiver != null) {
                    try { mContext.unregisterReceiver(oldReceiver) } catch (ignored: Throwable) {}
                }
                val strongAuthReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        try {
                            val mCallback = XposedHelpers.getObjectField(controller, "mKeyguardSecurityCallback")
                            XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", 0, true, 0, 0)
                        } catch (t: Throwable) {
                            XposedHelpers.log(t)
                        }
                    }
                }
                mContext.registerReceiver(strongAuthReceiver, IntentFilter(GlobalActions.ACTION_PREFIX + "UnlockStrongAuth"), Context.RECEIVER_NOT_EXPORTED)
                XposedHelpers.setAdditionalInstanceField(controller, "strongAuthReceiver", strongAuthReceiver)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.keyguard.KeyguardViewMediator", lpparam.classLoader, "doKeyguardLocked", Bundle::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (forcedOption == 0) return
                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                if (!isUnlocked(mContext, lpparam.classLoader)) return

                val skip = MainModule.mPrefs.getBoolean("system_noscreenlock_skip")
                if (skip) {
                    param.returnAndSkip(null)
                    XposedHelpers.callMethod(param.thisObject, "keyguardDone")
                }
                isUnlockedInnerCall = true
                val unlockIntent = Intent(GlobalActions.ACTION_PREFIX + "UnlockStrongAuth")
                unlockIntent.setPackage("com.android.systemui")
                mContext.sendBroadcast(unlockIntent)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.keyguard.KeyguardViewMediator", lpparam.classLoader, "setupLocked", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mediator = param.thisObject
                val mContext = XposedHelpers.getObjectField(mediator, "mContext") as? Context ?: return
                val oldReceiver = XposedHelpers.getAdditionalInstanceField(mediator, "smartLockReceiver") as? BroadcastReceiver
                if (oldReceiver != null) {
                    try { mContext.unregisterReceiver(oldReceiver) } catch (ignored: Throwable) {}
                }
                val filter = IntentFilter()
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                filter.addAction(GlobalActions.ACTION_PREFIX + "UnlockSetForced")
                filter.addAction(GlobalActions.ACTION_PREFIX + "UnlockBTConnection")
                val smartLockReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val action = intent.action ?: return

                        if (action == GlobalActions.ACTION_PREFIX + "UnlockSetForced")
                            forcedOption = intent.getIntExtra("system_noscreenlock_force", -1)

                        val isShowing = XposedHelpers.callMethod(mediator, "isShowing") as? Boolean ?: false
                        if (!isShowing) return
                        if (!isAuthOnce()) return

                        var isTrusted = false
                        if (forcedOption == 0) isTrusted = false
                        else if (forcedOption == 1) isTrusted = true
                        else if (MainModule.mPrefs.getStringAsInt("system_noscreenlock", 1) == 3) {
                            if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                                val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                                if (netInfo?.state != NetworkInfo.State.CONNECTED && netInfo?.state != NetworkInfo.State.DISCONNECTED)
                                    return
                                if (netInfo.isConnected) isTrusted = isTrustedWiFi(mContext)
                            } else if (action == GlobalActions.ACTION_PREFIX + "UnlockBTConnection") {
                                isTrusted = isTrustedBt(lpparam.classLoader)
                            }
                        }

                        XposedHelpers.setAdditionalStaticField(mediator, "isScreenLockDisabled", isTrusted)
                        if (isTrusted) {
                            val skip = MainModule.mPrefs.getBoolean("system_noscreenlock_skip")
                            if (skip)
                                XposedHelpers.callMethod(mediator, "keyguardDone")
                            else
                                XposedHelpers.callMethod(mediator, "resetStateLocked")
                            isUnlockedInnerCall = true
                            val unlockIntent = Intent(GlobalActions.ACTION_PREFIX + "UnlockStrongAuth")
                            unlockIntent.setPackage("com.android.systemui")
                            mContext.sendBroadcast(unlockIntent)
                        } else try {
                            val mLockUserManager = XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader),
                                "get",
                                XposedHelpers.findClassIfExists("com.android.systemui.statusbar.NotificationLockscreenUserManager", lpparam.classLoader)
                            )
                            XposedHelpers.callMethod(mLockUserManager, "updatePublicMode")
                        } catch (t: Throwable) {
                            XposedHelpers.log(t)
                        }
                    }
                }
                mContext.registerReceiver(smartLockReceiver, filter, Context.RECEIVER_EXPORTED)
                XposedHelpers.setAdditionalInstanceField(mediator, "smartLockReceiver", smartLockReceiver)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.keyguard.KeyguardSecurityModel", lpparam.classLoader, "getSecurityMode", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (forcedOption == 0) return
                val skip = MainModule.mPrefs.getBoolean("system_noscreenlock_skip")
                if (skip) return
                val mContext = ModuleHelper.findContext(lpparam)
                if (!isUnlocked(mContext, lpparam.classLoader)) return

                val securityModeEnum = XposedHelpers.findClass("com.android.keyguard.KeyguardSecurityModel\$SecurityMode", lpparam.classLoader)
                val securityModeNone = XposedHelpers.getStaticObjectField(securityModeEnum, "None")
                val securityModePassword = XposedHelpers.getStaticObjectField(securityModeEnum, "Password")
                val securityModePattern = XposedHelpers.getStaticObjectField(securityModeEnum, "Pattern")
                val securityModePin = XposedHelpers.getStaticObjectField(securityModeEnum, "PIN")

                val secModeResult = param.result
                if (securityModePassword == secModeResult ||
                    securityModePattern == secModeResult ||
                    securityModePin == secModeResult
                ) param.setResult(securityModeNone)
            }
        })

        val startClass = XposedHelpers.findClassIfExists("com.android.keyguard.faceunlock.FaceUnlockManager", lpparam.classLoader)
        if (startClass != null) {
            ModuleHelper.hookAllMethods(startClass, "startFaceUnlock", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val skip = MainModule.mPrefs.getBoolean("system_noscreenlock_nofaceunlock")
                    if (!skip) return
                    if (param.args.size == 0) return
                    val isScreenLockDisabled = XposedHelpers.getAdditionalStaticField(
                        XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", lpparam.classLoader),
                        "isScreenLockDisabled"
                    ) as? Boolean ?: false
                    if (isScreenLockDisabled) param.returnAndSkip(null)
                }
            })

            var showMsgMethod: Method? = XposedHelpers.findMethodExactIfExists(startClass, "isShowMessageWhenFaceUnlockSuccess")
            if (showMsgMethod == null) showMsgMethod = XposedHelpers.findMethodExactIfExists(startClass, "isFaceUnlockSuccessAndShowMessage")
            if (showMsgMethod == null) {
                XposedHelpers.log("NoScreenLockHook", "Show notification message method not found")
            } else {
                ModuleHelper.hookAllMethods(startClass, showMsgMethod.name, object : MethodHook() {
                    override fun after(param: AfterHookCallback) {
                        val skip = MainModule.mPrefs.getBoolean("system_noscreenlock_nofaceunlock")
                        if (!skip || !(param.result as? Boolean ?: false)) return
                        val isScreenLockDisabled = XposedHelpers.getAdditionalStaticField(
                            XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", lpparam.classLoader),
                            "isScreenLockDisabled"
                        ) as? Boolean ?: false
                        if (isScreenLockDisabled) param.setResult(false)
                    }
                })
            }
        }

        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.policy.BluetoothControllerImpl", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val controller = param.thisObject
                val mContext = param.args[0] as? Context ?: return
                val oldReceiver = XposedHelpers.getAdditionalInstanceField(controller, "fetchCachedDevicesReceiver") as? BroadcastReceiver
                if (oldReceiver != null) {
                    try { mContext.unregisterReceiver(oldReceiver) } catch (ignored: Throwable) {}
                }
                val fetchCachedDevicesReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val deviceList = ArrayList<BluetoothDevice>()
                        val updateIntent = Intent(GlobalActions.EVENT_PREFIX + "CACHEDDEVICESUPDATE")
                        val cachedDevices = XposedHelpers.callMethod(controller, "getDevices") as? Collection<*> ?: return
                        for (device in cachedDevices) {
                            val mDevice = XposedHelpers.getObjectField(device, "mDevice") as? BluetoothDevice
                            if (mDevice != null) deviceList.add(mDevice)
                        }
                        updateIntent.putParcelableArrayListExtra("device_list", deviceList)
                        updateIntent.setPackage(tv.withaibuild.customiuizer.utils.Helpers.modulePkg)
                        mContext.sendBroadcast(updateIntent)
                    }
                }
                mContext.registerReceiver(fetchCachedDevicesReceiver, IntentFilter(GlobalActions.ACTION_PREFIX + "FetchCachedDevices"), Context.RECEIVER_EXPORTED)
                XposedHelpers.setAdditionalInstanceField(controller, "fetchCachedDevicesReceiver", fetchCachedDevicesReceiver)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.BluetoothControllerImpl", lpparam.classLoader, "updateConnected", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                if (mContext != null) {
                    mContext.sendBroadcast(Intent(GlobalActions.ACTION_PREFIX + "UnlockBTConnection"))
                }
            }
        })
    }
}
