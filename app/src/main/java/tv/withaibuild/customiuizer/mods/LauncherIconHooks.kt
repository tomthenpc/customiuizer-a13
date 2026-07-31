package tv.withaibuild.customiuizer.mods

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.os.UserHandle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

@Suppress("UNUSED_PARAMETER", "DEPRECATION")
object LauncherIconHooks {

    private fun modifyTitle(thisObject: Any) {
        val isApplication = XposedHelpers.callMethod(thisObject, "isApplicatoin") as? Boolean ?: false
        if (!isApplication) return
        val pkgName = XposedHelpers.callMethod(thisObject, "getPackageName") as? String ?: return
        val actName = XposedHelpers.callMethod(thisObject, "getClassName") as? String ?: return
        val user = XposedHelpers.getObjectField(thisObject, "user") as? UserHandle ?: return
        val newTitle = MainModule.mPrefs.getString("launcher_renameapps_list:$pkgName|$actName|${user.hashCode()}", "")
        if (!TextUtils.isEmpty(newTitle)) XposedHelpers.setObjectField(thisObject, "mLabel", newTitle)
    }

    @JvmStatic
    fun RenameShortcutsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "onCreate", android.os.Bundle::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                ModuleHelper.observeOwnedPreferenceChange("launcher.renameShortcuts", param.getThisObject()) { owner, key ->
                    try {
                        if (!key.contains("pref_key_launcher_renameapps_list")) return@observeOwnedPreferenceChange
                        val newTitle = MainModule.mPrefs.getString(key, "")
                        val mAllLoadedApps: HashSet<*>? = when {
                            XposedHelpers.findFieldIfExists(owner.javaClass, "mAllLoadedShortcut") != null ->
                                XposedHelpers.getObjectField(owner, "mAllLoadedShortcut") as? HashSet<*>
                            XposedHelpers.findFieldIfExists(owner.javaClass, "mAllLoadedApps") != null ->
                                XposedHelpers.getObjectField(owner, "mAllLoadedApps") as? HashSet<*>
                            else ->
                                XposedHelpers.getObjectField(owner, "mLoadedAppsAndShortcut") as? HashSet<*>
                        }
                        val act = owner as? Activity ?: return@observeOwnedPreferenceChange
                        if (mAllLoadedApps != null) for (shortcut in mAllLoadedApps) {
                            val isApp = XposedHelpers.callMethod(shortcut, "isApplicatoin") as? Boolean ?: false
                            if (!isApp) continue
                            val pkgName = XposedHelpers.callMethod(shortcut, "getPackageName") as? String ?: continue
                            val actName = XposedHelpers.callMethod(shortcut, "getClassName") as? String ?: continue
                            val user = XposedHelpers.getObjectField(shortcut, "user") as? UserHandle ?: continue
                            if ("pref_key_launcher_renameapps_list:$pkgName|$actName|${user.hashCode()}" == key) {
                                val newStr = if (TextUtils.isEmpty(newTitle))
                                    XposedHelpers.getAdditionalInstanceField(shortcut, "mLabelOrig") as? CharSequence
                                else
                                    newTitle
                                XposedHelpers.setObjectField(shortcut, "mLabel", newStr)

                                act.runOnUiThread {
                                    if (act.packageName == "com.miui.home") {
                                        XposedHelpers.callMethod(shortcut, "updateBuddyIconView", act)
                                    } else {
                                        val buddyIconView = XposedHelpers.callMethod(shortcut, "getBuddyIconView")
                                        if (buddyIconView != null) XposedHelpers.callMethod(buddyIconView, "updateInfo", owner, shortcut)
                                    }
                                }
                                break
                            }
                        }
                    } catch (t: Throwable) {
                        XposedHelpers.log(t)
                    }
                }
            }
        })

        ModuleHelper.hookAllConstructors("com.miui.home.launcher.ShortcutInfo", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "mLabelOrig", XposedHelpers.getObjectField(param.getThisObject(), "mLabel"))
                if (param.getArgsCount() > 0) modifyTitle(param.getThisObject())
            }
        })

        @Suppress("ResultOfMethodCallIgnored")
        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.ShortcutInfo", lpparam.classLoader, "loadToggleInfo", Context::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "mLabelOrig", XposedHelpers.getObjectField(param.getThisObject(), "mLabel"))
                modifyTitle(param.getThisObject())
            }
        })

        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.ShortcutInfo", lpparam.classLoader, "setLabelAndUpdateDB", CharSequence::class.java, Context::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "mLabelOrig", param.getArg(0))
                modifyTitle(param.getThisObject())
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ShortcutInfo", lpparam.classLoader, "load", Context::class.java, Cursor::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                modifyTitle(param.getThisObject())
            }
        })

        ModuleHelper.hookAllMethodsSilently("com.miui.home.launcher.BaseAppInfo", lpparam.classLoader, "resetTitle", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                modifyTitle(param.getThisObject())
            }
        })
    }

    @JvmStatic
    fun TitleShadowHook(lpparam: PackageReadyParam) {
        if (lpparam.packageName == "com.miui.home")
            ModuleHelper.findAndHookMethod("com.miui.home.launcher.WallpaperUtils", lpparam.classLoader, "getIconTitleShadowColor", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val color = param.getResult() as? Int ?: return
                    if (color == Color.TRANSPARENT) return
                    param.setResult(Color.argb(Math.round(Color.alpha(color) + (255 - Color.alpha(color)) / 1.9f), Color.red(color), Color.green(color), Color.blue(color)))
                }
            })
        else
            ModuleHelper.findAndHookMethod("com.miui.home.launcher.WallpaperUtils", lpparam.classLoader, "getTitleShadowColor", Int::class.javaPrimitiveType, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val color = param.getResult() as? Int ?: return
                    if (color == Color.TRANSPARENT) return
                    param.setResult(Color.argb(Math.round(Color.alpha(color) + (255 - Color.alpha(color)) / 1.9f), Color.red(color), Color.green(color), Color.blue(color)))
                }
            })
    }

    @JvmStatic
    fun IconScaleHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ShortcutIcon", lpparam.classLoader, "restoreToInitState", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mIconContainer = XposedHelpers.getObjectField(param.getThisObject(), "mIconContainer") as? ViewGroup ?: return
                if (mIconContainer.getChildAt(0) == null) return
                val multx = Math.sqrt(MainModule.mPrefs.getInt("launcher_iconscale", 100) / 100.0).toFloat()
                mIconContainer.getChildAt(0).scaleX = multx
                mIconContainer.getChildAt(0).scaleY = multx
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val multx = Math.sqrt(MainModule.mPrefs.getInt("launcher_iconscale", 100) / 100.0).toFloat()

                val mIconContainer = XposedHelpers.getObjectField(param.getThisObject(), "mIconContainer") as? ViewGroup
                if (mIconContainer != null && mIconContainer.getChildAt(0) != null) {
                    mIconContainer.getChildAt(0).scaleX = multx
                    mIconContainer.getChildAt(0).scaleY = multx
                    mIconContainer.clipToPadding = false
                    mIconContainer.clipChildren = false
                }

                if (multx > 1) {
                    val mMessage = XposedHelpers.getObjectField(param.getThisObject(), "mMessage") as? TextView
                    if (mMessage != null)
                        mMessage.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable) {
                                ModuleHelper.guarded("LauncherIconHooks.messageTextWatcher") {
                                    val maxWidth = mMessage.resources.getDimensionPixelSize(mMessage.resources.getIdentifier("icon_message_max_width", "dimen", lpparam.packageName))
                                    mMessage.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST))
                                    mMessage.translationX = -mMessage.measuredWidth * (multx - 1) / 2f
                                    mMessage.translationY = mMessage.measuredHeight * (multx - 1) / 2f
                                }
                            }
                        })
                }

                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "mMessageAnimationOrig", XposedHelpers.getObjectField(param.getThisObject(), "mMessageAnimation"))
                XposedHelpers.setObjectField(param.getThisObject(), "mMessageAnimation", Runnable {
                    try {
                        val mMessageAnimationOrig = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "mMessageAnimationOrig") as? Runnable
                        mMessageAnimationOrig?.run()
                        val mIsShowMessageAnimation = XposedHelpers.getBooleanField(param.getThisObject(), "mIsShowMessageAnimation")
                        if (mIsShowMessageAnimation) {
                            val mMessage = XposedHelpers.getObjectField(param.getThisObject(), "mMessage") as? View
                            mMessage?.animate()?.cancel()
                            mMessage?.animate()?.scaleX(multx)?.scaleY(multx)?.setStartDelay(0)?.start()
                        }
                    } catch (t: Throwable) {
                        XposedHelpers.log(t)
                    }
                })
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "getIconLocation", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val multx = Math.sqrt(MainModule.mPrefs.getInt("launcher_iconscale", 100) / 100.0).toFloat()
                val rect = param.getResult() as? android.graphics.Rect ?: return
                rect.right = rect.left + Math.round(rect.width() * multx)
                rect.bottom = rect.top + Math.round(rect.height() * multx)
                param.setResult(rect)
            }
        })

        @Suppress("ResultOfMethodCallIgnored")
        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.gadget.ClearButton", lpparam.classLoader, "onCreate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mIconContainer = XposedHelpers.getObjectField(param.getThisObject(), "mIconContainer") as? ViewGroup ?: return
                if (mIconContainer.getChildAt(0) == null) return
                val multx = Math.sqrt(MainModule.mPrefs.getInt("launcher_iconscale", 100) / 100.0).toFloat()
                mIconContainer.getChildAt(0).scaleX = multx
                mIconContainer.getChildAt(0).scaleY = multx
            }
        })
    }

    @JvmStatic
    fun TitleFontSizeHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mTitle = XposedHelpers.getObjectField(param.getThisObject(), "mTitle") as? TextView ?: return
                mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainModule.mPrefs.getInt("launcher_titlefontsize", 5).toFloat())
            }
        })

        if (lpparam.packageName == "com.mi.android.globallauncher")
            ModuleHelper.hookAllMethods("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "setTitleColorMode", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mTitle = XposedHelpers.getObjectField(param.getThisObject(), "mTitle") as? TextView ?: return
                    mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainModule.mPrefs.getInt("launcher_titlefontsize", 5).toFloat())
                }
            })

        ModuleHelper.hookAllMethods("com.miui.home.launcher.ShortcutIcon", lpparam.classLoader, "fromXml", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val buddyIcon = XposedHelpers.callMethod(param.getArg(3), "getBuddyIconView", param.getArg(2))
                if (buddyIcon == null) return
                val mTitle = XposedHelpers.getObjectField(buddyIcon, "mTitle") as? TextView ?: return
                mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainModule.mPrefs.getInt("launcher_titlefontsize", 5).toFloat())
            }
        })

        if (lpparam.packageName == "com.miui.home") {
            ModuleHelper.hookAllMethods("com.miui.home.launcher.ShortcutIcon", lpparam.classLoader, "createShortcutIcon", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val buddyIcon = param.getResult() ?: return
                    val mTitle = XposedHelpers.getObjectField(buddyIcon, "mTitle") as? TextView ?: return
                    mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainModule.mPrefs.getInt("launcher_titlefontsize", 5).toFloat())
                }
            })

            ModuleHelper.hookAllMethods("com.miui.home.launcher.common.Utilities", lpparam.classLoader, "adaptTitleStyleToWallpaper", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mTitle = param.getArg(1) as? TextView ?: return
                    if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home"))
                        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainModule.mPrefs.getInt("launcher_titlefontsize", 5).toFloat())
                }
            })
        }
    }

    @JvmStatic
    fun TitleTopMarginHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mTitleContainer = XposedHelpers.getObjectField(param.getThisObject(), "mTitleContainer") as? ViewGroup ?: return
                val lp = mTitleContainer.layoutParams
                val opt = Math.round((MainModule.mPrefs.getInt("launcher_titletopmargin", 0) - 11) * mTitleContainer.resources.displayMetrics.density)
                if (lp is RelativeLayout.LayoutParams) {
                    lp.topMargin = opt
                    mTitleContainer.layoutParams = lp
                } else {
                    mTitleContainer.translationY = opt.toFloat()
                    mTitleContainer.clipChildren = false
                    mTitleContainer.clipToPadding = false
                    (mTitleContainer.parent as? ViewGroup)?.clipChildren = false
                    (mTitleContainer.parent as? ViewGroup)?.clipToPadding = false
                }
            }
        })
    }

    @JvmStatic
    fun HideTitlesHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mTitleContainer = XposedHelpers.getObjectField(param.getThisObject(), "mTitleContainer") as? View
                if (mTitleContainer != null) mTitleContainer.visibility = View.GONE
            }
        })
    }
}
