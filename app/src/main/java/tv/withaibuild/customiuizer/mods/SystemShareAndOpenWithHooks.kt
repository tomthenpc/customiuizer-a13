package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.BadParcelableException
import android.view.View
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.Helpers

object SystemShareAndOpenWithHooks {

    @JvmStatic
    fun CleanShareMenuHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner", lpparam.classLoader, "run", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mOriginalIntent = XposedHelpers.getObjectField(param.thisObject, "mOriginalIntent") as? Intent ?: return
                val action = mOriginalIntent.action ?: return
                if (action != Intent.ACTION_SEND && action != Intent.ACTION_SENDTO && action != Intent.ACTION_SEND_MULTIPLE) return
                if (mOriginalIntent.dataString?.contains(":") == true) return

                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val mAimPackageName = XposedHelpers.getObjectField(param.thisObject, "mAimPackageName") as? String ?: return
                val selectedApps = MainModule.mPrefs.getStringSet("system_cleanshare_apps")
                val mRootView = XposedHelpers.getObjectField(param.thisObject, "mRootView") as? View ?: return
                val appResId1 = mContext.resources.getIdentifier("app1", "id", "android.miui")
                val appResId2 = mContext.resources.getIdentifier("app2", "id", "android.miui")
                val removeOriginal = selectedApps.contains(mAimPackageName) || selectedApps.contains(mAimPackageName + "|0")
                val removeDual = selectedApps.contains(mAimPackageName + "|999")
                val originalApp = mRootView.findViewById<View>(appResId1)
                val dualApp = mRootView.findViewById<View>(appResId2)
                if (removeOriginal) dualApp?.performClick()
                else if (removeDual) originalApp?.performClick()
            }
        })
    }

    @JvmStatic
    fun CleanShareMenuServiceHook(lpparam: SystemServerStartingParam) {
        val hook = object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun after(param: AfterHookCallback) {
                try {
                    if (param.args[0] == null) return
                    if (param.args.size < 6) return
                    val origIntent = param.args[0] as? Intent ?: return
                    val action = origIntent.action ?: return
                    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SENDTO && action != Intent.ACTION_SEND_MULTIPLE) return
                    val intent = origIntent.clone() as? Intent ?: return
                    if (intent.dataString?.contains(":") == true) return
                    if (intent.hasExtra("CustoMIUIzer") && intent.getBooleanExtra("CustoMIUIzer", false)) return
                    val selectedApps = MainModule.mPrefs.getStringSet("system_cleanshare_apps")
                    val resolved = param.result as? MutableList<ResolveInfo> ?: return
                    val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                    val pm = mContext.packageManager
                    val itr = resolved.iterator()
                    while (itr.hasNext()) {
                        val resolveInfo = itr.next()
                        val removeOriginal = selectedApps.contains(resolveInfo.activityInfo.packageName) || selectedApps.contains(resolveInfo.activityInfo.packageName + "|0")
                        val removeDual = selectedApps.contains(resolveInfo.activityInfo.packageName + "|999")
                        var hasDual = false
                        try {
                            hasDual = XposedHelpers.callMethod(pm, "getPackageInfoAsUser", resolveInfo.activityInfo.packageName, 0, 999) != null
                        } catch (ignore: Throwable) {}
                        if ((removeOriginal && !hasDual) || removeOriginal && hasDual && removeDual) itr.remove()
                    }
                    param.setResult(resolved)
                } catch (t: Throwable) {
                    if (t !is BadParcelableException) XposedHelpers.log(t)
                }
            }
        }

        val actQueryService = "com.android.server.pm.ComputerEngine"
        ModuleHelper.hookAllMethods(actQueryService, lpparam.classLoader, "queryIntentActivitiesInternal", hook)
    }

    private fun hideMimeType(mimeFlags: Int, mimeType: String?): Boolean {
        var dataType = Helpers.MimeType.OTHERS
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) dataType = Helpers.MimeType.IMAGE
            else if (mimeType.startsWith("audio/")) dataType = Helpers.MimeType.AUDIO
            else if (mimeType.startsWith("video/")) dataType = Helpers.MimeType.VIDEO
            else if (mimeType.startsWith("text/") ||
                mimeType.startsWith("application/pdf") ||
                mimeType.startsWith("application/msword") ||
                mimeType.startsWith("application/vnd.ms-") ||
                mimeType.startsWith("application/vnd.openxmlformats-")) dataType = Helpers.MimeType.DOCUMENT
            else if (mimeType.startsWith("application/vnd.android.package-archive") ||
                mimeType.startsWith("application/zip") ||
                mimeType.startsWith("application/x-zip") ||
                mimeType.startsWith("application/octet-stream") ||
                mimeType.startsWith("application/rar") ||
                mimeType.startsWith("application/x-rar") ||
                mimeType.startsWith("application/x-tar") ||
                mimeType.startsWith("application/x-bzip") ||
                mimeType.startsWith("application/gzip") ||
                mimeType.startsWith("application/x-lz") ||
                mimeType.startsWith("application/x-compress") ||
                mimeType.startsWith("application/x-7z") ||
                mimeType.startsWith("application/java-archive")) dataType = Helpers.MimeType.ARCHIVE
            else if (mimeType.startsWith("link/")) dataType = Helpers.MimeType.LINK
        }
        return (mimeFlags and dataType) == dataType
    }

    private fun getContentType(context: Context, intent: Intent): String? {
        val scheme = intent.scheme
        val linkSchemes = "http" == scheme || "https" == scheme || "vnd.youtube" == scheme
        var mimeType = intent.type
        if (mimeType == null && linkSchemes) mimeType = "link/*"
        if (mimeType == null && intent.data != null) try {
            mimeType = context.contentResolver.getType(intent.data!!)
        } catch (ignore: Throwable) {}
        return mimeType
    }

    private fun isRemoveApp(dynamic: Boolean, context: Context, pkgName: String, selectedApps: Set<String>, mimeType: String?): Pair<Boolean, Boolean> {
        val key = "system_cleanopenwith_apps"
        val mimeFlags0: Int
        val mimeFlags999: Int
        if (dynamic) {
            mimeFlags0 = MainModule.mPrefs.getInt(key + "_" + pkgName + "|0", Helpers.MimeType.ALL)
            mimeFlags999 = MainModule.mPrefs.getInt(key + "_" + pkgName + "|999", Helpers.MimeType.ALL)
        } else {
            mimeFlags0 = MainModule.mPrefs.getInt(key + "_" + pkgName + "|0", Helpers.MimeType.ALL)
            mimeFlags999 = MainModule.mPrefs.getInt(key + "_" + pkgName + "|999", Helpers.MimeType.ALL)
        }
        val removeOriginal = (selectedApps.contains(pkgName) || selectedApps.contains(pkgName + "|0")) && hideMimeType(mimeFlags0, mimeType)
        val removeDual = selectedApps.contains(pkgName + "|999") && hideMimeType(mimeFlags999, mimeType)
        return Pair(removeOriginal, removeDual)
    }

    @JvmStatic
    fun CleanOpenWithMenuHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner", lpparam.classLoader, "run", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mOriginalIntent = XposedHelpers.getObjectField(param.thisObject, "mOriginalIntent") as? Intent ?: return
                val action = mOriginalIntent.action ?: return
                if (action != Intent.ACTION_VIEW) return

                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val mAimPackageName = XposedHelpers.getObjectField(param.thisObject, "mAimPackageName") as? String ?: return
                val selectedApps = MainModule.mPrefs.getStringSet("system_cleanopenwith_apps")
                val mimeType = getContentType(mContext, mOriginalIntent)
                val isRemove = isRemoveApp(true, mContext, mAimPackageName, selectedApps, mimeType)

                val mRootView = XposedHelpers.getObjectField(param.thisObject, "mRootView") as? View ?: return
                val appResId1 = mContext.resources.getIdentifier("app1", "id", "android.miui")
                val appResId2 = mContext.resources.getIdentifier("app2", "id", "android.miui")
                val originalApp = mRootView.findViewById<View>(appResId1)
                val dualApp = mRootView.findViewById<View>(appResId2)
                if (isRemove.first) dualApp?.performClick()
                else if (isRemove.second) originalApp?.performClick()
            }
        })
    }

    @JvmStatic
    fun CleanOpenWithMenuServiceHook(lpparam: SystemServerStartingParam) {
        val hook = object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun after(param: AfterHookCallback) {
                try {
                    if (param.args[0] == null) return
                    if (param.args.size < 6) return
                    val origIntent = param.args[0] as? Intent ?: return
                    val intent = origIntent.clone() as? Intent ?: return
                    val action = intent.action ?: return
                    if (action != Intent.ACTION_VIEW) return
                    if (intent.hasExtra("CustoMIUIzer") && intent.getBooleanExtra("CustoMIUIzer", false)) return
                    val scheme = intent.scheme
                    val validSchemes = "http" == scheme || "https" == scheme || "vnd.youtube" == scheme
                    if (intent.type == null && !validSchemes) return

                    val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                    val mimeType = getContentType(mContext, intent)

                    val key = "system_cleanopenwith_apps"
                    val selectedApps = MainModule.mPrefs.getStringSet(key)
                    val resolved = param.result as? MutableList<ResolveInfo> ?: return
                    val pm = mContext.packageManager
                    val itr = resolved.iterator()
                    while (itr.hasNext()) {
                        val resolveInfo = itr.next()
                        val isRemove = isRemoveApp(false, mContext, resolveInfo.activityInfo.packageName, selectedApps, mimeType)
                        var hasDual = false
                        try {
                            hasDual = XposedHelpers.callMethod(pm, "getPackageInfoAsUser", resolveInfo.activityInfo.packageName, 0, 999) != null
                        } catch (ignore: Throwable) {}
                        if ((isRemove.first && !hasDual) || isRemove.first && hasDual && isRemove.second) itr.remove()
                    }

                    param.setResult(resolved)
                } catch (t: Throwable) {
                    if (t !is BadParcelableException) XposedHelpers.log(t)
                }
            }
        }

        val actQueryService = "com.android.server.pm.ComputerEngine"
        ModuleHelper.hookAllMethods(actQueryService, lpparam.classLoader, "queryIntentActivitiesInternal", hook)
    }
}
