package tv.withaibuild.customiuizer.mods

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.lang.ref.WeakReference
import java.util.*

object SystemStatusBarAndClockHooks {

    @JvmStatic
    fun StatusBarHeightRes() {
        val opt = MainModule.mPrefs.getInt("system_statusbarheight", 19)
        val heightDpi = if (opt == 19) 27 else opt
        MainModule.resHooks.setDensityReplacement("*", "dimen", "status_bar_height_default", heightDpi.toFloat())
        MainModule.resHooks.setDensityReplacement("*", "dimen", "status_bar_height", heightDpi.toFloat())
        MainModule.resHooks.setDensityReplacement("*", "dimen", "status_bar_height_portrait", heightDpi.toFloat())
        MainModule.resHooks.setDensityReplacement("*", "dimen", "status_bar_height_landscape", heightDpi.toFloat())
    }

    @JvmStatic
    fun HideMemoryCleanHook(lpparam: PackageReadyParam, isInLauncher: Boolean) {
        val raClass = if (isInLauncher) "com.miui.home.recents.views.RecentsContainer" else "com.android.systemui.recents.RecentsActivity"
        if (isInLauncher && XposedHelpers.findClassIfExists(raClass, lpparam.classLoader) == null) return
        ModuleHelper.findAndHookMethod(raClass, lpparam.classLoader, "setupVisible", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mMemoryAndClearContainer = XposedHelpers.getObjectField(param.thisObject, "mMemoryAndClearContainer") as? ViewGroup ?: return
                mMemoryAndClearContainer.visibility = View.GONE
            }
        })
    }

    private const val NOCOLOR = 0x01010101
    private var actionBarColor = NOCOLOR

    private fun isIgnored(context: Context): Boolean {
        return !MainModule.mPrefs.getStringSet("system_statusbarcolor_apps").contains(context.packageName)
    }

    private fun getActionBarColor(window: Window, oldColor: Int): Int {
        if (actionBarColor != NOCOLOR) return actionBarColor

        val outValue = TypedValue()
        window.context.theme.resolveAttribute(android.R.attr.actionBarStyle, outValue, true)
        val abStyle = window.context.theme.obtainStyledAttributes(outValue.resourceId, intArrayOf(android.R.attr.background))
        val bg = abStyle.getDrawable(0)
        abStyle.recycle()

        return if (bg is ColorDrawable) bg.color else oldColor
    }

    @Suppress("UNCHECKED_CAST")
    private fun hookToolbar(thisObject: Any, bg: Drawable) {
        if (bg !is ColorDrawable) return
        actionBarColor = bg.color
        val mDecorToolbar = XposedHelpers.getObjectField(thisObject, "mDecorToolbar")
        val mToolbar = XposedHelpers.getObjectField(mDecorToolbar, "mToolbar") as? ViewGroup ?: return
        val mDecorContext = mToolbar.rootView.context ?: return
        val mActivityContext = XposedHelpers.getObjectField(mDecorContext, "mActivityContext") as? WeakReference<Context> ?: return
        val mContext = mActivityContext.get()
        if (mContext != null && !isIgnored(mContext)) {
            (mContext as? Activity)?.window?.statusBarColor = actionBarColor
        }
    }

    private fun hookWindowDecor(thisObject: Any, bg: Drawable) {
        if (bg !is ColorDrawable) return
        actionBarColor = bg.color
        val mActivity = XposedHelpers.getObjectField(thisObject, "mActivity") as? Activity ?: return
        if (!isIgnored(mActivity)) {
            mActivity.window.statusBarColor = actionBarColor
        }
    }

    @JvmStatic
    fun StatusBarBackgroundHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.internal.policy.PhoneWindow", lpparam.classLoader, "generateLayout", "com.android.internal.policy.DecorView", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val wnd = param.thisObject as? Window ?: return
                if (isIgnored(wnd.context)) return
                val mStatusBarColor = XposedHelpers.getIntField(param.thisObject, "mStatusBarColor")
                if (mStatusBarColor == -16777216) return
                val newColor = getActionBarColor(wnd, mStatusBarColor)
                if (newColor != mStatusBarColor) {
                    XposedHelpers.callMethod(param.thisObject, "setStatusBarColor", newColor)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.internal.policy.PhoneWindow", lpparam.classLoader, "setStatusBarColor", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val wnd = param.thisObject as? Window ?: return
                if (isIgnored(wnd.context)) return
                if (actionBarColor != NOCOLOR) param.args[0] = actionBarColor
                else if (Color.alpha(param.args[0] as? Int ?: 0) < 255) param.args[0] = Color.TRANSPARENT
            }
        })

        ModuleHelper.findAndHookMethod("com.android.internal.app.ToolbarActionBar", lpparam.classLoader, "setBackgroundDrawable", Drawable::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                hookToolbar(param.thisObject, param.args[0] as? Drawable ?: return)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.internal.app.WindowDecorActionBar", lpparam.classLoader, "setBackgroundDrawable", Drawable::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                hookWindowDecor(param.thisObject, param.args[0] as? Drawable ?: return)
            }
        })
    }

    @JvmStatic
    fun StatusBarBackgroundCompatHook(lpparam: PackageReadyParam) {
        var androidx = false

        val tabCls = XposedHelpers.findClassIfExists("androidx.appcompat.app.ToolbarActionBar", lpparam.classLoader)
        var sbdMethod = XposedHelpers.findMethodExactIfExists(tabCls, "setBackgroundDrawable", Drawable::class.java)
        if (sbdMethod != null) androidx = true
        if (sbdMethod != null) {
            ModuleHelper.hookMethod(sbdMethod, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    hookToolbar(param.thisObject, param.args[0] as? Drawable ?: return)
                }
            })
        }

        val wdabCls = XposedHelpers.findClassIfExists("androidx.appcompat.app.WindowDecorActionBar", lpparam.classLoader)
        sbdMethod = XposedHelpers.findMethodExactIfExists(wdabCls, "setBackgroundDrawable", Drawable::class.java)
        if (sbdMethod != null) androidx = true
        if (sbdMethod != null) {
            ModuleHelper.hookMethod(sbdMethod, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    hookWindowDecor(param.thisObject, param.args[0] as? Drawable ?: return)
                }
            })
        }

        if (!androidx) {
            val tabv7Cls = XposedHelpers.findClassIfExists("android.support.v7.internal.app.ToolbarActionBar", lpparam.classLoader)
            sbdMethod = XposedHelpers.findMethodExactIfExists(tabv7Cls, "setBackgroundDrawable", Drawable::class.java)
            if (sbdMethod != null) {
                ModuleHelper.hookMethod(sbdMethod, object : MethodHook() {
                    override fun before(param: BeforeHookCallback) {
                        hookToolbar(param.thisObject, param.args[0] as? Drawable ?: return)
                    }
                })
            }

            val wdabv7Cls = XposedHelpers.findClassIfExists("android.support.v7.internal.app.WindowDecorActionBar", lpparam.classLoader)
            sbdMethod = XposedHelpers.findMethodExactIfExists(wdabv7Cls, "setBackgroundDrawable", Drawable::class.java)
            if (sbdMethod != null) {
                ModuleHelper.hookMethod(sbdMethod, object : MethodHook() {
                    override fun before(param: BeforeHookCallback) {
                        hookWindowDecor(param.thisObject, param.args[0] as? Drawable ?: return)
                    }
                })
            }
        }
    }

    private fun checkToast(pkgName: String): Boolean {
        return try {
            val opt = MainModule.mPrefs.getStringAsInt("system_blocktoasts", 1)
            val selectedApps = MainModule.mPrefs.getStringSet("system_blocktoasts_apps")
            val isSelected = selectedApps != null && selectedApps.contains(pkgName)
            opt == 2 && !isSelected || opt == 3 && isSelected
        } catch (t: Throwable) {
            XposedHelpers.log(t)
            false
        }
    }

    @JvmStatic
    fun SelectiveToastsHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.notification.NotificationManagerService", lpparam.classLoader, "tryShowToast", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val pkgName = XposedHelpers.getObjectField(param.args[0], "pkg") as? String ?: return
                if (checkToast(pkgName)) param.returnAndSkip(false)
            }
        })
    }
}
