package tv.withaibuild.customiuizer.mods

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.SurfaceControl
import android.view.View
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers

@Suppress("UNUSED_PARAMETER")
object SystemUIScreenshotHooks {

    @JvmStatic
    fun TempHideOverlaySystemUIHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.wm.shell.pip.PipTaskOrganizer", lpparam.classLoader, "onTaskAppeared", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val organizer = param.getThisObject() ?: return
                val mContext = XposedHelpers.getObjectField(organizer, "mContext") as? Context ?: return
                val oldReceiver = XposedHelpers.getAdditionalInstanceField(organizer, "pipScreenshotReceiver") as? BroadcastReceiver
                if (oldReceiver != null) {
                    try { mContext.unregisterReceiver(oldReceiver) } catch (_: Throwable) {}
                }
                val intentFilter = IntentFilter("miui.intent.TAKE_SCREENSHOT")
                val pipScreenshotReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action != "miui.intent.TAKE_SCREENSHOT") return
                        val state = intent.getBooleanExtra("IsFinished", true)
                        val mState = XposedHelpers.getObjectField(organizer, "mPipTransitionState") ?: return
                        val isPip = XposedHelpers.callMethod(mState, "isInPip") as? Boolean ?: return
                        if (isPip) {
                            val mSurfaceControlTransactionFactory = XposedHelpers.getObjectField(organizer, "mSurfaceControlTransactionFactory")
                            val transaction = XposedHelpers.callMethod(mSurfaceControlTransactionFactory, "getTransaction") as? SurfaceControl.Transaction ?: return
                            val mLeash = XposedHelpers.getObjectField(organizer, "mLeash") as? SurfaceControl ?: return
                            transaction.setVisibility(mLeash, state)
                            transaction.apply()
                        }
                    }
                }
                mContext.registerReceiver(pipScreenshotReceiver, intentFilter, Context.RECEIVER_EXPORTED)
                XposedHelpers.setAdditionalInstanceField(organizer, "pipScreenshotReceiver", pipScreenshotReceiver)
            }
        })
    }

    @JvmStatic
    fun HideStatusBarBeforeScreenshotHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val view = param.getArgs()[0] as? View ?: return
                val oldBr = XposedHelpers.getAdditionalInstanceField(view, "hideStatusBarScreenshotReceiver") as? BroadcastReceiver
                if (oldBr != null) {
                    try { view.context.unregisterReceiver(oldBr) } catch (_: Throwable) {}
                }
                val br = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if ("miui.intent.TAKE_SCREENSHOT" == intent.action) {
                            val finished = intent.getBooleanExtra("IsFinished", true)
                            view.visibility = if (finished) View.VISIBLE else View.INVISIBLE
                        }
                    }
                }
                view.context.registerReceiver(br, IntentFilter("miui.intent.TAKE_SCREENSHOT"), Context.RECEIVER_EXPORTED)
                XposedHelpers.setAdditionalInstanceField(view, "hideStatusBarScreenshotReceiver", br)
            }
        })
    }

    @JvmStatic
    fun HideNavBarBeforeScreenshotHook(lpparam: PackageReadyParam) {
        val hideNavHook = object : MethodHook() {
            var visibleState = 0
            override fun after(param: AfterHookCallback) {
                val view = XposedHelpers.callMethod(param.getThisObject(), "getView") as? View ?: return
                val oldBr = XposedHelpers.getAdditionalInstanceField(param.getThisObject(), "hideNavBarScreenshotReceiver") as? BroadcastReceiver
                if (oldBr != null) {
                    try { view.context.unregisterReceiver(oldBr) } catch (_: Throwable) {}
                }
                val br = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if ("miui.intent.TAKE_SCREENSHOT" == intent.action) {
                            val finished = intent.getBooleanExtra("IsFinished", true)
                            if (!finished) visibleState = view.visibility
                            view.visibility = if (finished) visibleState else View.INVISIBLE
                        }
                    }
                }
                view.context.registerReceiver(br, IntentFilter("miui.intent.TAKE_SCREENSHOT"), Context.RECEIVER_EXPORTED)
                XposedHelpers.setAdditionalInstanceField(param.getThisObject(), "hideNavBarScreenshotReceiver", br)
            }
        }
        ModuleHelper.findAndHookMethod("com.android.systemui.navigationbar.NavigationBar", lpparam.classLoader, "onInit", hideNavHook)
    }
}
