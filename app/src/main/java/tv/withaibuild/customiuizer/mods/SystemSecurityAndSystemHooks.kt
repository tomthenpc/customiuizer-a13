package tv.withaibuild.customiuizer.mods

import android.content.ContentResolver
import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import android.widget.Button
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

object SystemSecurityAndSystemHooks {

    @JvmStatic
    fun NoVersionCheckHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.pm.PackageManagerServiceUtils", lpparam.classLoader, "checkDowngrade", HookerClassHelper.DO_NOTHING)
    }

    @JvmStatic
    fun ForceCloseHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllConstructors("com.android.server.policy.BaseMiuiPhoneWindowManager", lpparam.classLoader, object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun after(param: AfterHookCallback) {
                val mSystemKeyPackages = XposedHelpers.getObjectField(param.thisObject, "mSystemKeyPackages") as? HashSet<String> ?: return
                mSystemKeyPackages.remove("com.miui.securitycenter")
                mSystemKeyPackages.remove("com.miui.securityadd")
                mSystemKeyPackages.remove("com.android.phone")
                mSystemKeyPackages.remove("com.android.mms")
                mSystemKeyPackages.remove("com.android.contacts")
                mSystemKeyPackages.remove("com.miui.home")
                mSystemKeyPackages.remove("com.jeejen.family.miui")
                mSystemKeyPackages.remove("com.miui.backup")
                mSystemKeyPackages.remove("com.xiaomi.mihomemanager")
                mSystemKeyPackages.addAll(MainModule.mPrefs.getStringSet("system_forceclose_apps"))
            }
        })
    }

    @JvmStatic
    fun DisableSystemIntegrityHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("android.util.apk.ApkSignatureVerifier", lpparam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", Int::class.javaPrimitiveType, HookerClassHelper.returnConstant(1))
    }

    @JvmStatic
    fun NoSignatureVerifyServiceHook(lpparam: SystemServerStartingParam) {
        val SignDetails = XposedHelpers.findClassIfExists("android.content.pm.SigningDetails", lpparam.classLoader)
        val signUnknown = XposedHelpers.getStaticObjectField(SignDetails, "UNKNOWN")
        ModuleHelper.hookAllMethods(SignDetails, "checkCapability", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.thisObject == signUnknown || param.args[0] == signUnknown) {
                    param.returnAndSkip(false)
                    return
                }
                val flags = param.args[1] as? Int ?: return
                if (flags != 4) param.returnAndSkip(true)
            }
        })

        ModuleHelper.hookAllConstructors("android.util.jar.StrictJarVerifier", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setObjectField(param.thisObject, "signatureSchemeRollbackProtectionsEnforced", false)
            }
        })
        ModuleHelper.hookAllMethods("android.util.jar.StrictJarVerifier", lpparam.classLoader, "verifyMessageDigest", HookerClassHelper.returnConstant(true))
        ModuleHelper.hookAllMethods("android.util.jar.StrictJarVerifier", lpparam.classLoader, "verify", HookerClassHelper.returnConstant(true))
        ModuleHelper.hookAllMethods("com.android.server.pm.PackageManagerServiceUtils", lpparam.classLoader, "verifySignatures", HookerClassHelper.returnConstant(false))
        ModuleHelper.hookAllMethods("com.android.server.pm.InstallPackageHelper", lpparam.classLoader, "doesSignatureMatchForPermissions", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val packageName = XposedHelpers.callMethod(param.args[1], "getPackageName") as? String ?: return
                val sourcePackageName = param.args[0] as? String ?: return
                if (sourcePackageName == packageName) {
                    param.returnAndSkip(true)
                }
            }
        })
        ModuleHelper.hookAllMethods("com.android.server.pm.InstallPackageHelper", lpparam.classLoader, "cannotInstallWithBadPermissionGroups", HookerClassHelper.returnConstant(false))
        ModuleHelper.hookAllMethods("com.android.server.pm.permission.PermissionManagerServiceImpl", lpparam.classLoader, "shouldGrantPermissionBySignature", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val isSystem = XposedHelpers.callMethod(param.args[0], "isSystem") as? Boolean ?: return
                if (isSystem) {
                    param.returnAndSkip(true)
                }
            }
        })
    }

    @JvmStatic
    fun RemoveSecureHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.wm.WindowState", lpparam.classLoader, "isSecureLocked", HookerClassHelper.returnConstant(false))
        ModuleHelper.findAndHookMethod("com.android.server.wm.WindowSurfaceController", lpparam.classLoader, "setSecure", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = false
            }
        })
        ModuleHelper.hookAllConstructors("com.android.server.wm.WindowSurfaceController", lpparam.classLoader, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                var flags = param.args[2] as? Int ?: return
                val secureFlag = 128
                flags = flags and secureFlag.inv()
                param.args[2] = flags
            }
        })
    }

    @JvmStatic
    fun RemoveActStartConfirmHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethodsSilently("com.miui.server.SecurityManagerService", lpparam.classLoader, "checkAllowStartActivity", HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun NoSOSHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.keyguard.EmergencyButton", lpparam.classLoader, "updateEmergencyCallButton", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mSOS = param.thisObject as? Button ?: return
                if (mSOS.visibility == View.VISIBLE) {
                    mSOS.visibility = View.INVISIBLE
                }
            }
        })
    }

    @JvmStatic
    fun NoDarkForceHook(lpparam: SystemServerStartingParam) {
        val hook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val contentResolver = (XposedHelpers.callMethod(param.thisObject, "getContext") as? Context)?.contentResolver ?: return
                val userId = XposedHelpers.callStaticMethod(UserHandle::class.java, "getCallingUserId")
                XposedHelpers.callStaticMethod(Settings.System::class.java, "putIntForUser", contentResolver, "smart_dark_enable", 0, userId)
                val systemProperties = XposedHelpers.findClassIfExists("android.os.SystemProperties", lpparam.classLoader)
                XposedHelpers.callStaticMethod(systemProperties, "set", "debug.hwui.force_dark", "false")
            }
        }
        ModuleHelper.findAndHookMethodSilently("com.android.server.UiModeManagerService", lpparam.classLoader, "setForceDark", Context::class.java, hook)
        ModuleHelper.findAndHookMethod("com.miui.server.SecurityManagerService", lpparam.classLoader, "getAppDarkModeForUser", String::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(false)
            }
        })
        ModuleHelper.findAndHookMethod("com.android.server.DarkModeAppSettingsInfo", lpparam.classLoader, "getOverrideEnableValue", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(2)
            }
        })
    }
}
