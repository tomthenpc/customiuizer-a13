package tv.withaibuild.customiuizer.mods

import android.app.Notification
import android.app.WallpaperColors
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.HookUtils

object SystemNotificationAndShareHooks {

    private val hookedTiles = ArrayList<String>()

    @JvmStatic
    fun ColorizeNotificationCardHook(lpparam: PackageReadyParam) {
        val ColorScheme = XposedHelpers.findClassIfExists("com.android.systemui.monet.ColorScheme", lpparam.classLoader)
        var contentStyle: Any? = null
        val MonetStyle = XposedHelpers.findClassIfExists("com.android.systemui.monet.Style", lpparam.classLoader)
        val styles = MonetStyle?.enumConstants
        if (styles != null) {
            for (o in styles) {
                if (o.toString().contains("CONTENT")) {
                    contentStyle = o
                    break
                }
            }
        }
        val finalContentStyle = contentStyle

        ModuleHelper.findAndHookConstructor("android.app.Notification\$Builder", lpparam.classLoader, Context::class.java, Notification::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArg(1) != null) {
                    val mN = param.getArg(1) as? Notification ?: return
                    if (XposedHelpers.getAdditionalInstanceField(mN, "mPrimaryTextColor") != null) {
                        val builder = param.thisObject
                        val mParams = XposedHelpers.getObjectField(builder, "mParams")
                        XposedHelpers.callMethod(builder, "getColors", mParams)
                        val mColors = XposedHelpers.getObjectField(builder, "mColors")
                        XposedHelpers.setObjectField(mColors, "mProtectionColor", XposedHelpers.getAdditionalInstanceField(mN, "mProtectionColor"))
                        XposedHelpers.setObjectField(mColors, "mPrimaryTextColor", XposedHelpers.getAdditionalInstanceField(mN, "mPrimaryTextColor"))
                        XposedHelpers.setObjectField(mColors, "mSecondaryTextColor", XposedHelpers.getAdditionalInstanceField(mN, "mSecondaryTextColor"))
                    }
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader, "updateNotificationColor", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mEntry = XposedHelpers.getObjectField(param.thisObject, "mEntry")
                val mSbn = XposedHelpers.getObjectField(mEntry, "mSbn")
                val notify = XposedHelpers.callMethod(mSbn, "getNotification") as? Notification ?: return
                val overflowColor = XposedHelpers.getAdditionalInstanceField(notify, "mSecondaryTextColor")
                if (overflowColor != null) {
                    XposedHelpers.setObjectField(param.thisObject, "mNotificationColor", overflowColor)
                    param.returnAndSkip(null)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader, "onNotificationUpdated", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mEntry = XposedHelpers.getObjectField(param.thisObject, "mEntry") ?: return
                val mSbn = XposedHelpers.getObjectField(mEntry, "mSbn")
                val notify = XposedHelpers.callMethod(mSbn, "getNotification") as? Notification ?: return
                val mNotifyBackgroundColor = XposedHelpers.getAdditionalInstanceField(notify, "mNotifyBackgroundColor")
                if (mNotifyBackgroundColor != null) {
                    var bgColor = mNotifyBackgroundColor as? Int ?: return
                    val mCurrentBackgroundTint = XposedHelpers.getIntField(param.thisObject, "mCurrentBackgroundTint")
                    if (mCurrentBackgroundTint != bgColor) {
                        bgColor = Color.argb(158, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
                        XposedHelpers.callMethod(param.thisObject, "setBackgroundTintColor", bgColor)
                    }
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.row.NotificationBackgroundView", lpparam.classLoader, "setTint", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if ((param.getArg(0) as? Int ?: 0) == 0) {
                    param.returnAndSkip(null)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper", lpparam.classLoader, "getCustomBackgroundColor", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(XposedHelpers.getObjectField(param.thisObject, "mBackgroundColor"))
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.HybridGroupManager", lpparam.classLoader, "bindFromNotificationWithStyle", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mN = XposedHelpers.callMethod(param.getArg(2), "getNotification") as? Notification ?: return
                if (XposedHelpers.getAdditionalInstanceField(mN, "mSecondaryTextColor") != null) {
                    val hybridNotificationView = param.result as? LinearLayout ?: return
                    val mTitleView = XposedHelpers.getObjectField(hybridNotificationView, "mTitleView") as? TextView ?: return
                    val mTextView = XposedHelpers.getObjectField(hybridNotificationView, "mTextView") as? TextView ?: return
                    mTitleView.setTextColor(XposedHelpers.getAdditionalInstanceField(mN, "mPrimaryTextColor") as? Int ?: 0)
                    mTextView.setTextColor(XposedHelpers.getAdditionalInstanceField(mN, "mSecondaryTextColor") as? Int ?: 0)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector", lpparam.classLoader, "handle3thThemeColor", object : MethodHook() {
            private var sAppIconManager: Any? = null
            override fun before(param: BeforeHookCallback) {
                val builder = param.getArg(1) as? Notification.Builder ?: return
                val mN = XposedHelpers.getObjectField(builder, "mN") as? Notification ?: return
                if (XposedHelpers.callMethod(mN, "isColorized") as? Boolean == true) return
                if (XposedHelpers.callMethod(mN, "isMediaNotification") as? Boolean == true) return
                val applicationInfo = mN.extras.getParcelable<ApplicationInfo>("android.appInfo") ?: return
                val mContext = param.getArg(0) as? Context ?: return
                val pkgName = applicationInfo.packageName
                val opt = MainModule.mPrefs.getString("system_colorizenotifs", "1").toInt()
                val isSelected = MainModule.mPrefs.getStringSet("system_colorizenotifs_apps").contains(pkgName)
                if (opt == 2 && !isSelected || opt == 3 && isSelected) {
                    XposedHelpers.callMethod(builder, "makeNotificationGroupHeader")
                    if (sAppIconManager == null) {
                        val Dependency = XposedHelpers.findClassIfExists("com.android.systemui.Dependency", lpparam.classLoader)
                        val AppIconManager = XposedHelpers.findClassIfExists("com.miui.systemui.graphics.AppIconsManager", lpparam.classLoader)
                        sAppIconManager = XposedHelpers.callStaticMethod(Dependency, "get", AppIconManager)
                    }
                    val notifyIcon = XposedHelpers.callMethod(sAppIconManager, "getAppIconBitmap", pkgName) as? Bitmap ?: return
                    val wc = WallpaperColors.fromBitmap(notifyIcon)
                    var primaryColor = wc.primaryColor.toArgb()
                    val lux = Color.luminance(primaryColor)
                    if (lux > 0.9) {
                        val secColor = wc.secondaryColor
                        if (secColor != null) {
                            primaryColor = secColor.toArgb()
                        }
                    }
                    val dark = mContext.resources.configuration.isNightModeActive
                    val cs = XposedHelpers.newInstance(ColorScheme, primaryColor, dark, finalContentStyle)
                    val accent1 = XposedHelpers.callMethod(cs, "getAccent1") as? List<Int> ?: return
                    val n1 = XposedHelpers.getObjectField(cs, "neutral1") as? List<Int> ?: return
                    val n2 = XposedHelpers.getObjectField(cs, "neutral2") as? List<Int> ?: return

                    val bgColor = accent1[if (dark) 5 else 6]
                    val mParams = XposedHelpers.getObjectField(builder, "mParams")
                    XposedHelpers.callMethod(mParams, "reset")
                    XposedHelpers.callMethod(builder, "getColors", mParams)
                    val mColors = XposedHelpers.getObjectField(builder, "mColors")
                    val mProtectionColor = ColorUtils.blendARGB(n1[1], bgColor, 0.7f)
                    val mPrimaryTextColor = n1[if (dark) 1 else 10]
                    val mSecondaryTextColor = n2[if (dark) 3 else 8]
                    XposedHelpers.setObjectField(mColors, "mProtectionColor", mProtectionColor)
                    XposedHelpers.setAdditionalInstanceField(mN, "mProtectionColor", mProtectionColor)
                    XposedHelpers.setObjectField(mColors, "mPrimaryTextColor", mPrimaryTextColor)
                    XposedHelpers.setAdditionalInstanceField(mN, "mPrimaryTextColor", mPrimaryTextColor)
                    XposedHelpers.setObjectField(mColors, "mSecondaryTextColor", mSecondaryTextColor)
                    XposedHelpers.setAdditionalInstanceField(mN, "mSecondaryTextColor", mSecondaryTextColor)
                    XposedHelpers.setAdditionalInstanceField(mN, "mNotifyBackgroundColor", bgColor)
                    param.returnAndSkip(null)
                }
            }
        })

        val textColorHook = object : MethodHook() {
            private var titleResId = 0
            private var subTextResId = 0
            override fun after(param: AfterHookCallback) {
                val baseContent = param.result as? RemoteViews ?: return
                val mContext = param.getArg(param.getArgsCount() - 1) as? Context ?: return
                if (titleResId == 0) {
                    titleResId = mContext.resources.getIdentifier("title", "id", "com.android.systemui")
                    subTextResId = mContext.resources.getIdentifier("text", "id", "com.android.systemui")
                }
                val builder = param.getArg(0) as? Notification.Builder ?: return
                val mN = XposedHelpers.getObjectField(builder, "mN") as? Notification ?: return
                if (XposedHelpers.callMethod(mN, "isMediaNotification") as? Boolean == true) return
                val primary = XposedHelpers.getAdditionalInstanceField(mN, "mPrimaryTextColor") as? Int ?: return
                val secondary = XposedHelpers.getAdditionalInstanceField(mN, "mSecondaryTextColor") as? Int ?: return
                baseContent.setTextColor(titleResId, primary)
                baseContent.setTextColor(subTextResId, secondary)
            }
        }

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector", lpparam.classLoader, "createMiuiContentView", textColorHook)
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector", lpparam.classLoader, "createMiuiExpandedView", textColorHook)
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector", lpparam.classLoader, "createMiuiPublicView", textColorHook)
    }

    var abHeight = 39

    @JvmStatic
    fun CompactNotificationsHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper", lpparam.classLoader, "wrap", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.getArgsCount() > 3) return
                val res = param.result ?: return
                val mView = XposedHelpers.getObjectField(res, "mView") as? View ?: return
                val container = mView.findViewById<FrameLayout>(mView.resources.getIdentifier("actions_container", "id", "android"))
                container ?: return
                val density = mView.resources.displayMetrics.density
                val height = Math.round(density * abHeight)
                val actions = container.getChildAt(0) as? ViewGroup ?: return
                val lp1 = actions.layoutParams as? FrameLayout.LayoutParams ?: return
                lp1.height = height
                actions.layoutParams = lp1
                actions.setPadding(0, 0, 0, 0)
                for (c in 0 until actions.childCount) {
                    val button = actions.getChildAt(c)
                    val lp2 = button.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
                    lp2.height = height
                    lp2.bottomMargin = 0
                    lp2.topMargin = 0
                }
            }
        })
    }

    @JvmStatic
    fun QSHapticHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.qs.tileimpl.QSFactoryImpl", lpparam.classLoader, "createTileInternal", String::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val tile = param.result ?: return
                val tileClass = tile.javaClass.canonicalName ?: return
                if (!hookedTiles.contains(tileClass)) {
                    ModuleHelper.hookAllMethods(tileClass, lpparam.classLoader, "handleClick", object : MethodHook() {
                        override fun after(param: AfterHookCallback) {
                            val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                            val ignoreSystem = MainModule.mPrefs.getBoolean("system_qshaptics_ignore")
                            val opt = MainModule.mPrefs.getStringAsInt("system_qshaptics", 1)
                            if (opt == 2) HookUtils.performLightVibration(mContext, ignoreSystem)
                            else if (opt == 3) HookUtils.performStrongVibration(mContext, ignoreSystem)
                        }
                    })
                    hookedTiles.add(tileClass)
                }
            }
        })
    }

    @JvmStatic
    fun AutoGroupNotificationsHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.notification.GroupHelper", lpparam.classLoader, "adjustAutogroupingSummary", Int::class.javaPrimitiveType, String::class.java, String::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun before(param: BeforeHookCallback) {
                val opt = MainModule.mPrefs.getString("system_autogroupnotif", "1").toInt()
                if (opt == 2) {
                    param.returnAndSkip(null)
                    return
                }
                val mUngroupedNotifications = XposedHelpers.getObjectField(param.thisObject, "mUngroupedNotifications") as? Map<Int, Map<String, LinkedHashSet<String>>> ?: return
                val obj = mUngroupedNotifications[param.getArg(0)]
                if (obj != null) {
                    val list = obj[param.getArg(1)]
                    if (list != null && list.size < opt) param.returnAndSkip(null)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.notification.GroupHelper", lpparam.classLoader, "adjustNotificationBundling", List::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val list = param.getArg(0) as? List<*>
                val opt = MainModule.mPrefs.getString("system_autogroupnotif", "1").toInt()
                if (opt == 2 || (list != null && list.size < opt)) param.returnAndSkip(null)
            }
        })
    }
}
