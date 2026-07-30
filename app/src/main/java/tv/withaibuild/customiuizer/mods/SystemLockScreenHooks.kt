package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.SystemClock
import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.util.Collections

object SystemLockScreenHooks {

    @JvmStatic
    fun ScramblePINHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.keyguard.KeyguardPINView", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mViews = XposedHelpers.getObjectField(param.thisObject, "mViews") as? Array<Array<View?>> ?: return
                val mRandomViews = ArrayList<View>()
                for (row in 1..3) {
                    for (col in 0..2) {
                        mViews[row][col]?.let { mRandomViews.add(it) }
                    }
                }
                mRandomViews.add(mViews[4][1] ?: return)
                Collections.shuffle(mRandomViews)

                val pinview = param.thisObject as? View ?: return
                val res = pinview.resources
                fun findRow(id: String): ViewGroup? = pinview.findViewById(res.getIdentifier(id, "id", "com.android.systemui"))
                val row1 = findRow("row1") ?: return
                val row2 = findRow("row2") ?: return
                val row3 = findRow("row3") ?: return
                val row4 = findRow("row4") ?: return

                row1.removeAllViews()
                row2.removeAllViews()
                row3.removeAllViews()
                if (row4.childCount > 1) row4.removeViewAt(1)

                val m0 = mRandomViews[0]
                val m1 = mRandomViews[1]
                val m2 = mRandomViews[2]
                mViews[1] = arrayOf(m0, m1, m2)
                row1.addView(m0)
                row1.addView(m1)
                row1.addView(m2)

                val m3 = mRandomViews[3]
                val m4 = mRandomViews[4]
                val m5 = mRandomViews[5]
                mViews[2] = arrayOf(m3, m4, m5)
                row2.addView(m3)
                row2.addView(m4)
                row2.addView(m5)

                val m6 = mRandomViews[6]
                val m7 = mRandomViews[7]
                val m8 = mRandomViews[8]
                mViews[3] = arrayOf(m6, m7, m8)
                row3.addView(m6)
                row3.addView(m7)
                row3.addView(m8)

                val m9 = mRandomViews[9]
                val old4 = mViews[4][2]
                mViews[4] = arrayOf(null, m9, old4)
                row4.addView(m9, 1)

                XposedHelpers.setObjectField(param.thisObject, "mViews", mViews)
            }
        })
    }

    @JvmStatic
    fun NoPasswordHook(lpparam: PackageReadyParam) {
        val isAllowed = "isBiometricAllowedForUser"
        ModuleHelper.findAndHookMethod(
            "com.android.internal.widget.LockPatternUtils\$StrongAuthTracker",
            lpparam.classLoader,
            isAllowed,
            Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            HookerClassHelper.returnConstant(true)
        )
        ModuleHelper.findAndHookMethod(
            "com.android.internal.widget.LockPatternUtils",
            lpparam.classLoader,
            isAllowed,
            Int::class.javaPrimitiveType,
            HookerClassHelper.returnConstant(true)
        )
    }

    @JvmStatic
    fun EnhancedSecurityHook(lpparam: SystemServerStartingParam) {
        val hook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mPWMContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val kgMgr = mPWMContext.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager ?: return
                if (kgMgr.isKeyguardLocked && kgMgr.isKeyguardSecure) {
                    val mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler") as? Handler
                    val mEndCallLongPress = XposedHelpers.getObjectField(param.thisObject, "mEndCallLongPress")
                    if (mHandler != null && mEndCallLongPress != null) {
                        mHandler.removeCallbacks(mEndCallLongPress as Runnable)
                    }
                }
            }
        }
        ModuleHelper.hookAllMethodsSilently("com.android.server.policy.PhoneWindowManager", lpparam.classLoader, "interceptPowerKeyDown", hook)

        val keyguardCheck = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mPWMContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val kgMgr = mPWMContext.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager ?: return
                if (kgMgr.isKeyguardLocked && kgMgr.isKeyguardSecure) {
                    param.returnAndSkip(null)
                }
            }
        }
        ModuleHelper.hookAllMethodsSilently("com.android.server.policy.PhoneWindowManager", lpparam.classLoader, "powerLongPress", keyguardCheck)
        ModuleHelper.findAndHookMethodSilently("com.android.server.policy.PhoneWindowManager", lpparam.classLoader, "showGlobalActions", keyguardCheck)
        ModuleHelper.findAndHookMethodSilently("com.android.server.policy.PhoneWindowManager", lpparam.classLoader, "showGlobalActionsInternal", keyguardCheck)
    }
}
