package tv.withaibuild.customiuizer.mods

import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.HookUtils
import java.util.concurrent.ConcurrentHashMap

object PackagePermissions {

    private val systemPackages: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @JvmStatic
    fun hook(lpparam: SystemServerStartingParam) {
        systemPackages.add(HookUtils.modulePkg)

        ModuleHelper.hookAllMethods(
            "com.android.server.pm.permission.PermissionManagerServiceImpl",
            lpparam.classLoader,
            "shouldGrantPermissionBySignature",
            object : MethodHook() {
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    val packageName = XposedHelpers.callMethod(chain.args[0], "getPackageName") as? String
                    return if (packageName != null && systemPackages.contains(packageName)) true else chain.proceed()
                }
            }
        )

        ModuleHelper.hookAllMethodsSilently(
            "com.android.server.pm.PackageManagerServiceUtils",
            lpparam.classLoader,
            "verifySignatures",
            object : MethodHook() {
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    val packageName = XposedHelpers.callMethod(chain.args[0], "getName") as? String
                    return if (packageName != null && systemPackages.contains(packageName)) true else chain.proceed()
                }
            }
        )

        ModuleHelper.hookAllMethodsSilently(
            "com.android.server.wm.ActivityRecordInjector",
            lpparam.classLoader,
            "canShowWhenLocked",
            tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.returnConstant(true)
        )

        ModuleHelper.findAndHookMethod(
            "android.content.pm.ApplicationInfo",
            lpparam.classLoader,
            "isSystemApp",
            object : MethodHook() {
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    val ai = chain.thisObject as? ApplicationInfo
                    return if (ai != null && ai.packageName != null && systemPackages.contains(ai.packageName)) true else chain.proceed()
                }
            }
        )

        ModuleHelper.findAndHookMethodSilently(
            "android.content.pm.ApplicationInfo",
            lpparam.classLoader,
            "isSignedWithPlatformKey",
            object : MethodHook() {
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    val ai = chain.thisObject as? ApplicationInfo
                    return if (ai != null && ai.packageName != null && systemPackages.contains(ai.packageName)) true else chain.proceed()
                }
            }
        )

        try {
            val dpgpiClass = XposedHelpers.findClass(
                "com.android.server.pm.MiuiDefaultPermissionGrantPolicy",
                lpparam.classLoader
            )
            val miuiSystemApps = XposedHelpers.getStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS") as? Array<*>
            val mySystemApps = (miuiSystemApps?.mapNotNull { it as? String } ?: emptyList()).toMutableList()
            mySystemApps.addAll(systemPackages)
            XposedHelpers.setStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS", mySystemApps.toTypedArray())
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }
}
