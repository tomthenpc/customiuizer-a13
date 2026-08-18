package tv.withaibuild.customiuizer.mods

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.RuntimeFatality
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

@Suppress("UNUSED_PARAMETER")
object LauncherFolderHooks {

    @JvmStatic
    fun CloseFolderOnLaunchHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "launch", "com.miui.home.launcher.ShortcutInfo", View::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (MainModule.mPrefs.getStringAsInt("launcher_closefolders", 1) != 2) return
                val mHasLaunchedAppFromFolder = XposedHelpers.getBooleanField(param.getThisObject(), "mHasLaunchedAppFromFolder")
                if (mHasLaunchedAppFromFolder) XposedHelpers.callMethod(param.getThisObject(), "closeFolder")
            }
        })
    }

    private fun applyFolderWidth(folder: Any) {
        if (!MainModule.mPrefs.getBoolean("launcher_folderwidth")) return
        val content = XposedHelpers.getObjectField(folder, "mContent") as? View ?: return
        val lp = content.layoutParams ?: return
        if (lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            content.layoutParams = lp
        }
    }

    @JvmStatic
    fun FolderColumnsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Folder", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val folder = param.getThisObject() ?: return
                val cols = MainModule.mPrefs.getInt("launcher_folder_cols", 1)

                val content = XposedHelpers.getObjectField(folder, "mContent")
                if (content is GridView) {
                    content.numColumns = cols
                }

                applyFolderWidth(folder)

                if (cols > 3 && MainModule.mPrefs.getBoolean("launcher_folderspace")) {
                    val mBackgroundView = XposedHelpers.getObjectField(folder, "mBackgroundView") as? ViewGroup
                    if (mBackgroundView != null) {
                        val original = XposedHelpers.getAdditionalInstanceField(mBackgroundView, "folderOriginalPadding") as? IntArray
                        val (left, right) = if (original != null) {
                            original[0] to original[1]
                        } else {
                            val left = mBackgroundView.paddingLeft
                            val right = mBackgroundView.paddingRight
                            XposedHelpers.setAdditionalInstanceField(mBackgroundView, "folderOriginalPadding", intArrayOf(left, right))
                            left to right
                        }
                        mBackgroundView.setPadding(left / 3, mBackgroundView.paddingTop, right / 3, mBackgroundView.paddingBottom)
                    }
                }
            }
        })

        ModuleHelper.hookAllMethods("com.miui.home.launcher.Folder", lpparam.classLoader, "onLayout", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                applyFolderWidth(param.getThisObject() ?: return)
            }

            override fun after(param: AfterHookCallback) {
                if (!MainModule.mPrefs.getBoolean("launcher_folderwidth")) return
                val mContent = XposedHelpers.getObjectField(param.getThisObject(), "mContent")
                val contentView = if (mContent is View) mContent else null
                val mFakeIcon = XposedHelpers.getObjectField(param.getThisObject(), "mFakeIcon") as? ImageView ?: return
                if (contentView != null) {
                    mFakeIcon.layout(contentView.left, contentView.top, contentView.right, contentView.top + contentView.width)
                }
            }
        })

        ModuleHelper.findAndHookMethodSilently("com.miui.home.launcher.Folder", lpparam.classLoader, "resetViewsLayoutParams", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                applyFolderWidth(param.getThisObject() ?: return)
            }
        })
    }

    @JvmStatic
    fun PrivacyFolderHook(lpparam: PackageReadyParam) {
        if (MainModule.mPrefs.getBoolean("launcher_privacyapps_gest")) {
            ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "registerBroadcastReceivers", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val act = param.getThisObject() as? Activity ?: return
                    val intentFilter = IntentFilter()
                    intentFilter.addAction("android.telephony.action.SECRET_CODE")
                    intentFilter.addDataAuthority("233233", null)
                    intentFilter.addDataScheme("android_secret_code")

                    val secretCodeReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            try {
                                if (intent.action == null) return
                                if ("android.telephony.action.SECRET_CODE" == intent.action) {
                                    XposedHelpers.setAdditionalInstanceField(act, "fromSecretCode", true)
                                    XposedHelpers.callMethod(act, "startSecurityHide")
                                }
                            } catch (t: Throwable) {
                                RuntimeFatality.throwIfFatal(t)
                                XposedHelpers.log(t)
                            }
                        }
                    }
                    ModuleHelper.registerModuleReceiver(
                        act,
                        "launcher.secretCodeReceiver",
                        secretCodeReceiver,
                        intentFilter,
                        Context.RECEIVER_NOT_EXPORTED
                    )
                }
            })
        }
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "startSecurityHide", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "fromSecretCode") != null) {
                    XposedHelpers.removeAdditionalInstanceField(param.getThisObject(), "fromSecretCode")
                    return
                }
                if (GlobalActions.handleAction(param.getThisObject() as? Activity, "launcher_spread")) {
                    param.returnAndSkip(null)
                    return
                }
                val opt = MainModule.mPrefs.getBoolean("launcher_privacyapps_gest")
                if (opt) param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun FolderBlurHook(lpparam: PackageReadyParam) {
        val blurUtils = XposedHelpers.findClassIfExists("com.miui.home.launcher.common.BlurUtils", lpparam.classLoader)
        if (blurUtils != null) {
            ModuleHelper.hookAllMethods(blurUtils, "getLauncherBlur", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val isFolderShowing = XposedHelpers.callMethod(param.getArg(0), "isFolderShowing") as? Boolean ?: false
                    if (isFolderShowing) {
                        val blurPct = MainModule.mPrefs.getInt("launcher_folderblur_opacity", 0)
                        val blurRatio = blurPct / 100f
                        param.returnAndSkip(blurRatio)
                    }
                }
            })

            ModuleHelper.findAndHookMethod("com.miui.home.launcher.FolderCling", lpparam.classLoader, "open", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val launcher = XposedHelpers.getObjectField(param.getThisObject(), "mLauncher") as? Activity ?: return

                    val blurPct = MainModule.mPrefs.getInt("launcher_folderblur_opacity", 0)
                    val blurRatio = blurPct / 100f
                    XposedHelpers.callStaticMethod(blurUtils, "fastBlur", blurRatio, launcher.window, true)
                }
            })

            ModuleHelper.findAndHookMethod("com.miui.home.launcher.FolderCling", lpparam.classLoader, "close", Boolean::class.javaPrimitiveType, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val launcher = XposedHelpers.getObjectField(param.getThisObject(), "mLauncher") as? Activity ?: return
                    XposedHelpers.callStaticMethod(blurUtils, "fastBlur", 0f, launcher.window, param.getArg(0))
                }
            })

            ModuleHelper.findAndHookMethod("com.miui.home.launcher.Launcher", lpparam.classLoader, "cancelShortcutMenu", Int::class.javaPrimitiveType, "com.miui.home.launcher.shortcuts.CancelShortcutMenuReason", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val isFolderShowing = XposedHelpers.callMethod(param.getThisObject(), "isFolderShowing") as? Boolean ?: false
                    if (isFolderShowing) {
                        val blurPct = MainModule.mPrefs.getInt("launcher_folderblur_opacity", 0)
                        val blurRatio = blurPct / 100f
                        val launcher = param.getThisObject() as? Activity ?: return
                        XposedHelpers.callStaticMethod(blurUtils, "fastBlur", blurRatio, launcher.window, true)
                    }
                }
            })
        }
    }

    @JvmStatic
    fun CloseFolderOrDrawerOnLaunchShortcutMenuHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.home.launcher.shortcuts.AppShortcutMenuItem", lpparam.classLoader, "getOnClickListener", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val listener = param.getResult() as? View.OnClickListener ?: return
                param.setResult(View.OnClickListener { view ->
                    listener.onClick(view)
                    val appCls = XposedHelpers.findClassIfExists("com.miui.home.launcher.Application", lpparam.classLoader)
                    if (appCls == null) return@OnClickListener
                    val launcher = XposedHelpers.callStaticMethod(appCls, "getLauncher") ?: return@OnClickListener
                    if (MainModule.mPrefs.getBoolean("launcher_closedrawer")) XposedHelpers.callMethod(launcher, "hideAppView")
                    if (MainModule.mPrefs.getStringAsInt("launcher_closefolders", 1) > 1) XposedHelpers.callMethod(launcher, "closeFolder")
                })
            }
        })
    }
}
