@file:Suppress("DEPRECATION")

package tv.withaibuild.customiuizer.mods

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.TypedArray
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import miui.process.ForegroundInfo
import miui.process.ProcessManager
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.Helpers
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@SuppressLint("StaticFieldLeak")
object Various {

    @JvmField
    var mLastPackageInfo: PackageInfo? = null

    @JvmField
    var mSupportFragment: Any? = null

    private val MIUI_CORE_APPS = setOf(
        "com.lbe.security.miui", "com.miui.securitycenter", "com.miui.packageinstaller"
    )

    @JvmStatic
    fun AppInfoHook(lpparam: PackageReadyParam) {
        val amaCls = XposedHelpers.findClassIfExists("com.miui.appmanager.AMAppInfomationActivity", lpparam.classLoader)
        if (amaCls == null) {
            XposedHelpers.log("AppInfoHook", "Cannot find activity class!")
            return
        }

        val xfragCls = XposedHelpers.findClassIfExists("androidx.fragment.app.Fragment", lpparam.classLoader)
        val fragmentCls = XposedHelpers.findClassIfExists("android.app.Fragment", lpparam.classLoader)
        if (xfragCls != null || fragmentCls != null) {
            ModuleHelper.findAndHookConstructor("androidx.fragment.app.Fragment", lpparam.classLoader, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    try {
                        val piField = XposedHelpers.findFirstFieldByExactType(param.thisObject.javaClass, PackageInfo::class.java)
                        if (piField != null) mSupportFragment = param.thisObject
                    } catch (_: Throwable) { }
                }
            })
        }

        ModuleHelper.findAndHookMethod(amaCls, "onCreate", Bundle::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    val act = param.thisObject as? Activity ?: return@post
                    val contentFrag = act.fragmentManager.findFragmentById(android.R.id.content)
                    val frag = contentFrag ?: mSupportFragment
                    if (frag == null) {
                        XposedHelpers.log("AppInfoHook", "Unable to find fragment")
                        return@post
                    }

                    try {
                        val modRes = ModuleHelper.getModuleRes(act)
                        val piField = XposedHelpers.findFirstFieldByExactType(frag.javaClass, PackageInfo::class.java)
                        mLastPackageInfo = piField.get(frag) as? PackageInfo
                        val addPref = XposedHelpers.findMethodsByExactParameters(
                            frag.javaClass,
                            Void.TYPE,
                            String::class.java,
                            String::class.java,
                            String::class.java
                        )
                        if (mLastPackageInfo == null || addPref.isEmpty()) {
                            XposedHelpers.log("AppInfoHook", "Unable to find field/class/method in SecurityCenter to hook")
                            return@post
                        } else {
                            addPref[0].isAccessible = true
                        }
                        addPref[0].invoke(frag, "apk_versioncode", modRes.getString(R.string.appdetails_apk_version_code), mLastPackageInfo!!.versionCode.toString())
                        addPref[0].invoke(frag, "apk_filename", modRes.getString(R.string.appdetails_apk_file), mLastPackageInfo!!.applicationInfo?.sourceDir)
                        addPref[0].invoke(frag, "data_path", modRes.getString(R.string.appdetails_data_path), mLastPackageInfo!!.applicationInfo?.dataDir)
                        addPref[0].invoke(frag, "app_uid", modRes.getString(R.string.appdetails_app_uid), mLastPackageInfo!!.applicationInfo?.uid.toString())
                        addPref[0].invoke(frag, "target_sdk", modRes.getString(R.string.appdetails_sdk), mLastPackageInfo!!.applicationInfo?.targetSdkVersion.toString())
                        handler.post {
                            try {
                                addPref[0].invoke(frag, "open_in_store", modRes.getString(R.string.appdetails_playstore), "")
                                addPref[0].invoke(frag, "launch_app", modRes.getString(R.string.appdetails_launch), "")
                            } catch (t: Throwable) {
                                XposedHelpers.log(t)
                            }
                        }

                    ModuleHelper.hookAllMethods(frag.javaClass, "onPreferenceTreeClick", object : MethodHook() {
                        override fun before(param: BeforeHookCallback) {
                            val key = XposedHelpers.callMethod(param.args[0], "getKey") as? String ?: return
                            val title = XposedHelpers.callMethod(param.args[0], "getTitle") as? CharSequence
                            when (key) {
                                "apk_filename" -> {
                                    (act.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText(title, mLastPackageInfo?.applicationInfo?.sourceDir))
                                    Toast.makeText(act, act.resources.getIdentifier("app_manager_copy_pkg_to_clip", "string", act.packageName), Toast.LENGTH_SHORT).show()
                                    param.returnAndSkip(true)
                                }
                                "data_path" -> {
                                    (act.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText(title, mLastPackageInfo?.applicationInfo?.dataDir))
                                    Toast.makeText(act, act.resources.getIdentifier("app_manager_copy_pkg_to_clip", "string", act.packageName), Toast.LENGTH_SHORT).show()
                                    param.returnAndSkip(true)
                                }
                                "open_in_store" -> {
                                    try {
                                        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + mLastPackageInfo!!.packageName))
                                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                        act.startActivity(launchIntent)
                                    } catch (_: android.content.ActivityNotFoundException) {
                                        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + mLastPackageInfo!!.packageName))
                                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                        act.startActivity(launchIntent)
                                    }
                                    param.returnAndSkip(true)
                                }
                                "launch_app" -> {
                                    var launchIntent = act.packageManager.getLaunchIntentForPackage(mLastPackageInfo!!.packageName)
                                    if (launchIntent == null) {
                                        Toast.makeText(act, modRes.getString(R.string.appdetails_nolaunch), Toast.LENGTH_SHORT).show()
                                    } else {
                                        var user = 0
                                        try {
                                            val uid = act.intent.getIntExtra("am_app_uid", -1)
                                            user = XposedHelpers.callStaticMethod(UserHandle::class.java, "getUserId", uid) as? Int ?: 0
                                        } catch (t: Throwable) {
                                            XposedHelpers.log(t)
                                        }

                                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                        if (user != 0) try {
                                            XposedHelpers.callMethod(act, "startActivityAsUser", launchIntent, XposedHelpers.newInstance(UserHandle::class.java, user))
                                        } catch (t: Throwable) {
                                            XposedHelpers.log(t)
                                        } else {
                                            act.startActivity(launchIntent)
                                        }
                                    }
                                    param.returnAndSkip(true)
                                }
                            }
                        }
                    })
                } catch (t: Throwable) {
                    XposedHelpers.log(t)
                    return@post
                }

                }
            }
        })
    }

    @JvmStatic
    fun checkBundle(context: Context?, bundle: Bundle?): Bundle? {
        if (context == null) {
            XposedHelpers.log("AppsDefaultSortHook", "Context is null!")
            return null
        }
        var newBundle = bundle ?: Bundle()
        var order = MainModule.mPrefs.getStringAsInt("various_appsort", 1)
        order -= 1
        newBundle.putInt("current_sory_type", order)
        newBundle.putInt("current_sort_type", order)
        return newBundle
    }

    @JvmStatic
    fun AppsDefaultSortHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.appmanager.AppManagerMainActivity", lpparam.classLoader, "onCreate", Bundle::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = checkBundle(param.thisObject as? Context, param.args[0] as? Bundle)

                val xfragCls = XposedHelpers.findClassIfExists("androidx.fragment.app.Fragment", lpparam.classLoader)
                val fragmentCls = XposedHelpers.findClassIfExists("android.app.Fragment", lpparam.classLoader)
                val fields = param.thisObject.javaClass.declaredFields
                var fragCls: String? = null
                for (field in fields) {
                    if ((fragmentCls != null && fragmentCls.isAssignableFrom(field.type)) ||
                        (xfragCls != null && xfragCls.isAssignableFrom(field.type))
                    ) {
                        fragCls = field.type.canonicalName
                        break
                    }
                }

                if (fragCls != null) {
                    ModuleHelper.hookAllMethods(fragCls, lpparam.classLoader, "onActivityCreated", object : MethodHook() {
                        override fun before(param: BeforeHookCallback) {
                            try {
                                param.args[0] = checkBundle(
                                    XposedHelpers.callMethod(param.thisObject, "getContext") as? Context,
                                    param.args[0] as? Bundle
                                )
                            } catch (t: Throwable) {
                                XposedHelpers.log("AppsDefaultSortHook", t.message)
                            }
                        }
                    })
                }
            }
        })
    }

    private fun setAppState(act: Activity, pkgName: String, item: MenuItem, enable: Boolean) {
        try {
            val pm = act.packageManager
            pm.setApplicationEnabledSetting(pkgName, if (enable) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0)
            val state = pm.getApplicationEnabledSetting(pkgName)
            val isEnabledOrDefault = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            if ((enable && isEnabledOrDefault) || (!enable && !isEnabledOrDefault)) {
                item.setTitle(act.resources.getIdentifier(if (enable) "app_manager_disable_text" else "app_manager_enable_text", "string", "com.miui.securitycenter"))
                Toast.makeText(act, act.resources.getIdentifier(if (enable) "app_manager_enabled" else "app_manager_disabled", "string", "com.miui.securitycenter"), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(act, ModuleHelper.getModuleRes(act).getString(R.string.disable_app_fail), Toast.LENGTH_LONG).show()
            }
            Handler(Looper.getMainLooper()).postDelayed(Runnable { ModuleHelper.guarded { act.invalidateOptionsMenu() } }, 500)
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }

    @JvmStatic
    fun AppsDisableServiceHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.pm.PackageManagerServiceImpl", lpparam.classLoader, "canBeDisabled", String::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                // Do not mask an exception thrown by the original PackageManager call;
                // only override the result on a normal return path.
                if (param.throwable != null) return
                val pkgName = param.getArg(0) as? String ?: return
                val canBeDisabled = param.result as? Boolean ?: return
                if (!canBeDisabled && !MIUI_CORE_APPS.contains(pkgName)) {
                    param.setResult(true)
                }
            }
        })
    }

    @JvmStatic
    fun AppsDisableHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", lpparam.classLoader, "onCreateOptionsMenu", Menu::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val act = param.thisObject as? Activity ?: return
                val menu = param.args[0] as? Menu ?: return
                val dis = menu.add(0, 666, 1, act.resources.getIdentifier("app_manager_disable_text", "string", lpparam.packageName))
                dis.setIcon(act.resources.getIdentifier("action_button_stop", "drawable", lpparam.packageName))
                dis.isEnabled = true
                dis.setShowAsAction(1)

                val pm = act.packageManager
                val piField = XposedHelpers.findFirstFieldByExactType(act.javaClass, PackageInfo::class.java)
                val mPackageInfo = piField.get(act) as? PackageInfo ?: return
                val appInfo = try {
                    pm.getApplicationInfo(mPackageInfo.packageName, PackageManager.GET_META_DATA)
                } catch (_: Throwable) {
                    return
                }
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                dis.setTitle(act.resources.getIdentifier(if (appInfo.enabled) "app_manager_disable_text" else "app_manager_enable_text", "string", lpparam.packageName))

                if (!appInfo.enabled || (isSystem && !isUpdatedSystem)) {
                    val item = menu.findItem(2)
                    item?.isVisible = false
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", lpparam.classLoader, "onOptionsItemSelected", MenuItem::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val item = param.args[0] as? MenuItem ?: return
                if (item.itemId != 666) return

                val act = param.thisObject as? Activity ?: return
                val modRes = ModuleHelper.getModuleRes(act)
                val piField = XposedHelpers.findFirstFieldByExactType(act.javaClass, PackageInfo::class.java)
                val mPackageInfo = piField.get(act) as? PackageInfo ?: return
                if (MIUI_CORE_APPS.contains(mPackageInfo.packageName)) {
                    Toast.makeText(act, modRes.getString(R.string.disable_app_settings), Toast.LENGTH_SHORT).show()
                    return
                }

                val pm = act.packageManager
                val appInfo = pm.getApplicationInfo(mPackageInfo.packageName, PackageManager.GET_META_DATA)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val state = pm.getApplicationEnabledSetting(mPackageInfo.packageName)
                val isEnabledOrDefault = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                if (isEnabledOrDefault) {
                    if (isSystem) {
                        val title = modRes.getString(R.string.disable_app_title)
                        val text = modRes.getString(R.string.disable_app_text)
                        AlertDialog.Builder(act).setTitle(title).setMessage(text).setPositiveButton(android.R.string.ok) { _, _ ->
                            setAppState(act, mPackageInfo.packageName, item, false)
                        }.setNegativeButton(android.R.string.cancel, null).show()
                    } else {
                        setAppState(act, mPackageInfo.packageName, item, false)
                    }
                } else {
                    setAppState(act, mPackageInfo.packageName, item, true)
                }
                param.setResult(true)
            }
        })
    }

    @JvmStatic
    fun AppsRestrictHook(lpparam: PackageReadyParam) {
        val mGetAppInfo = XposedHelpers.findMethodsByExactParameters(
            XposedHelpers.findClass("com.miui.appmanager.AppManageUtils", lpparam.classLoader),
            ApplicationInfo::class.java,
            Any::class.java,
            PackageManager::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        if (mGetAppInfo.isEmpty()) {
            XposedHelpers.log("AppsRestrictHook", "Cannot find getAppInfo method!")
        } else {
            ModuleHelper.hookMethod(mGetAppInfo[0], object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    if ((param.args[3] as? Int ?: -1) == 128 && (param.args[4] as? Int ?: -1) == 0) {
                        val appInfo = param.result as? ApplicationInfo ?: return
                        appInfo.flags = appInfo.flags and ApplicationInfo.FLAG_SYSTEM.inv()
                        param.setResult(appInfo)
                    }
                }
            })
        }

        ModuleHelper.findAndHookMethod("com.miui.networkassistant.ui.fragment.ShowAppDetailFragment", lpparam.classLoader, "initFirewallData", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mAppInfo = XposedHelpers.getObjectField(param.thisObject, "mAppInfo")
                if (mAppInfo != null) XposedHelpers.setBooleanField(mAppInfo, "isSystemApp", false)
            }
        })

        ModuleHelper.hookAllMethods("com.miui.networkassistant.service.FirewallService", lpparam.classLoader, "setSystemAppWifiRuleAllow", HookerClassHelper.DO_NOTHING)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun AppsRestrictPowerHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.powerkeeper.provider.PowerKeeperConfigureManager", lpparam.classLoader, "pkgHasIcon", String::class.java, HookerClassHelper.returnConstant(true))

        ModuleHelper.findAndHookMethod("com.miui.powerkeeper.provider.PreSetGroup", lpparam.classLoader, "initGroup", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mGroupHeadUidMap = XposedHelpers.getStaticObjectField(
                    XposedHelpers.findClass("com.miui.powerkeeper.provider.PreSetGroup", lpparam.classLoader),
                    "mGroupHeadUidMap"
                ) as? MutableMap<String, Int> ?: return
                mGroupHeadUidMap.clear()
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.powerkeeper.provider.PreSetApp", lpparam.classLoader, "isPreSetApp", String::class.java, HookerClassHelper.returnConstant(false))
        ModuleHelper.hookAllMethods("com.miui.powerkeeper.utils.Utils", lpparam.classLoader, "pkgHasIcon", HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun PersistBatteryOptimizationHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.miui.powerkeeper.utils.CommonAdapter", lpparam.classLoader, "addPowerSaveWhitelistApps", HookerClassHelper.DO_NOTHING)
        ModuleHelper.hookAllMethods("com.miui.powerkeeper.millet.MilletPolicy", lpparam.classLoader, "dealSleepModeWhiteList", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val addWhiteList = param.args[1] as? Boolean ?: false
                if (addWhiteList) {
                    param.returnAndSkip(null)
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.powerkeeper.statemachine.ForceDozeController", lpparam.classLoader, "restoreWhiteListAppsIfQuitForceIdle", HookerClassHelper.DO_NOTHING)
    }

    private fun showSideBar(view: View, dockLocation: Int) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val y = location[1]
        val uptimeMillis = SystemClock.uptimeMillis()
        val downEvent: MotionEvent
        val moveEvent: MotionEvent
        val upEvent: MotionEvent
        if (dockLocation == 0) {
            downEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis, MotionEvent.ACTION_DOWN, 4f, (y + 15).toFloat(), 0)
            moveEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 20, MotionEvent.ACTION_MOVE, 160f, (y + 15).toFloat(), 0)
            upEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 21, MotionEvent.ACTION_UP, 160f, (y + 15).toFloat(), 0)
        } else {
            val x = location[0]
            downEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis, MotionEvent.ACTION_DOWN, (x - 4).toFloat(), (y + 15).toFloat(), 0)
            moveEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 20, MotionEvent.ACTION_MOVE, (x - 160).toFloat(), (y + 15).toFloat(), 0)
            upEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 21, MotionEvent.ACTION_UP, (x - 160).toFloat(), (y + 15).toFloat(), 0)
        }
        downEvent.setSource(9999)
        moveEvent.setSource(9999)
        upEvent.setSource(9999)
        view.dispatchTouchEvent(downEvent)
        view.dispatchTouchEvent(moveEvent)
        view.dispatchTouchEvent(upEvent)
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
    }

    @JvmStatic
    fun AddSideBarExpandReceiverHook(lpparam: PackageReadyParam) {
        val isHooked = booleanArrayOf(false, false)
        val enableSideBar = MainModule.mPrefs.getBoolean("various_swipe_expand_sidebar")
        if (!enableSideBar) {
            MainModule.resHooks.setDensityReplacement("com.miui.securitycenter", "dimen", "sidebar_height_default", 8f)
            MainModule.resHooks.setDensityReplacement("com.miui.securitycenter", "dimen", "sidebar_height_vertical", 8f)
        }
        val regionSamplingHelper = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.gestural.RegionSamplingHelper", lpparam.classLoader)
        if (regionSamplingHelper == null) {
            XposedHelpers.log("AddSideBarExpandReceiverHook", "failed to find RegionSamplingHelper")
            return
        }
        ModuleHelper.hookAllConstructors(regionSamplingHelper, object : MethodHook() {
            private var originDockLocation = -1
            override fun after(param: AfterHookCallback) {
                if (isHooked[0]) return
                isHooked[0] = true
                val view = param.args[0] as? View ?: return
                if (originDockLocation == -1) {
                    originDockLocation = view.context.getSharedPreferences("sp_video_box", 0).getInt("dock_line_location", 0)
                }
                val showReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val bundle = intent?.getBundleExtra("actionInfo")
                        var pos = originDockLocation
                        if (bundle != null) {
                            pos = bundle.getInt("inDirection", 0)
                            view.context.getSharedPreferences("sp_video_box", 0).edit().putInt("dock_line_location", pos).commit()
                        }
                        showSideBar(view, pos)
                    }
                }
                view.context.registerReceiver(showReceiver, IntentFilter(GlobalActions.ACTION_PREFIX + "ShowSideBar"), Context.RECEIVER_EXPORTED)
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "showReceiver", showReceiver)

                if (!isHooked[1]) {
                    isHooked[1] = true
                    val myhandler = Handler(Looper.getMainLooper())
                    val removeBg = object : Runnable {
                        override fun run() {
                            try {
                                myhandler.removeCallbacks(this)
                                if (!enableSideBar) {
                                    val li = XposedHelpers.getObjectField(view, "mListenerInfo")
                                    if (li != null) {
                                        val mOnTouchListener = XposedHelpers.getObjectField(li, "mOnTouchListener")
                                        if (mOnTouchListener != null) {
                                            ModuleHelper.findAndHookMethod(mOnTouchListener.javaClass, "onTouch", View::class.java, MotionEvent::class.java, object : MethodHook() {
                                                override fun before(param: BeforeHookCallback) {
                                                    val me = param.args[1] as? MotionEvent ?: return
                                                    if (me.source != 9999) {
                                                        param.returnAndSkip(false)
                                                    }
                                                }
                                            })
                                        }
                                    }
                                    view.background?.let { bg ->
                                        ModuleHelper.findAndHookMethod(bg.javaClass, "draw", Canvas::class.java, object : MethodHook() {
                                            override fun before(param: BeforeHookCallback) {
                                                param.returnAndSkip(null)
                                            }
                                        })
                                    }
                                    view.setBackground(null)
                                }
                            } catch (t: Throwable) { XposedHelpers.log(t) }
                        }
                    }
                    myhandler.postDelayed(removeBg, 150)
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "sideBarHandler", myhandler)
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "sideBarRemoveBg", removeBg)
                }
            }
        })
        ModuleHelper.findAndHookMethod(regionSamplingHelper, "onViewDetachedFromWindow", View::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                isHooked[0] = false
                val showReceiver = XposedHelpers.getAdditionalInstanceField(param.thisObject, "showReceiver") as? BroadcastReceiver
                if (showReceiver != null) {
                    val view = param.args[0] as? View ?: return
                    view.context.unregisterReceiver(showReceiver)
                    XposedHelpers.removeAdditionalInstanceField(param.thisObject, "showReceiver")
                }
                val sideBarHandler = XposedHelpers.getAdditionalInstanceField(param.thisObject, "sideBarHandler") as? Handler
                val sideBarRemoveBg = XposedHelpers.getAdditionalInstanceField(param.thisObject, "sideBarRemoveBg") as? Runnable
                if (sideBarHandler != null && sideBarRemoveBg != null) {
                    sideBarHandler.removeCallbacks(sideBarRemoveBg)
                    XposedHelpers.removeAdditionalInstanceField(param.thisObject, "sideBarHandler")
                    XposedHelpers.removeAdditionalInstanceField(param.thisObject, "sideBarRemoveBg")
                }
            }
        })
        val methods = XposedHelpers.findMethodsByExactParameters(regionSamplingHelper, Void.TYPE, Rect::class.java)
        if (methods.isEmpty()) {
            XposedHelpers.log("AddSideBarExpandReceiverHook", "Cannot find appropriate start method")
            return
        }
        ModuleHelper.hookMethod(methods[0], object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun InterceptPermHook(lpparam: PackageReadyParam) {
        val interceptBaseFragmentClass = XposedHelpers.findClass("com.miui.permcenter.privacymanager.InterceptBaseFragment", lpparam.classLoader)
        val innerClasses = interceptBaseFragmentClass.declaredClasses
        var handlerClass: Class<*>? = null
        for (innerClass in innerClasses) {
            if (Handler::class.java.isAssignableFrom(innerClass)) {
                handlerClass = innerClass
                break
            }
        }
        if (handlerClass != null) {
            ModuleHelper.hookAllConstructors(handlerClass, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    if (param.args.size == 2) {
                        param.args[1] = 0
                    }
                }
            })
            val methods = XposedHelpers.findMethodsByExactParameters(handlerClass, Void.TYPE, Int::class.javaPrimitiveType)
            if (methods.isNotEmpty()) {
                ModuleHelper.hookMethod(methods[0], object : MethodHook() {
                    override fun before(param: BeforeHookCallback) {
                        param.args[0] = 0
                    }
                })
            }
        }
    }

    @JvmStatic
    fun PrivacyAppsLayoutHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.privacyapps.ui.PrivacyAppsActivity", lpparam.classLoader, "onCreate", Bundle::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val act = param.thisObject as? Activity ?: return
                val gridViewId = act.resources.getIdentifier("privacy_apps_gridview", "id", "com.miui.securitycenter")
                val gridView = act.findViewById<GridView>(gridViewId)
                gridView?.numColumns = 4
                val params = gridView?.layoutParams as? LinearLayout.LayoutParams
                if (params != null) {
                    params.rightMargin = Helpers.dp2px(16f).toInt()
                    params.leftMargin = params.rightMargin
                    gridView.layoutParams = params
                }
            }
        })
    }

    @JvmStatic
    fun NoLowBatteryWarningHook() {
        val settingHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val key = param.args[1] as? String ?: return
                if ("low_battery_dialog_disabled" == key) param.returnAndSkip(1)
                else if ("low_battery_sound" == key) param.returnAndSkip(null)
            }
        }
        ModuleHelper.hookAllMethods(Settings.System::class.java, "getInt", settingHook)
        ModuleHelper.hookAllMethods(Settings.Global::class.java, "getString", settingHook)
    }

    @JvmStatic
    fun OpenByDefaultHook(lpparam: PackageReadyParam) {
        var defaultViewId = -1
        ModuleHelper.findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", lpparam.classLoader, "initView", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (defaultViewId == -1) {
                    val act = param.thisObject as? Activity ?: return
                    defaultViewId = act.resources.getIdentifier("am_detail_default", "id", "com.miui.securitycenter")
                    MainModule.resHooks.setResReplacement("com.miui.securitycenter", "string", "app_manager_default_open_title", R.string.various_open_by_default_title)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", lpparam.classLoader, "onClick", View::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val view = param.args[0] as? View ?: return
                if (view.id == defaultViewId && defaultViewId != -1) {
                    val act = param.thisObject as? Activity ?: return
                    val intent = Intent("android.settings.APP_OPEN_BY_DEFAULT_SETTINGS")
                    val pkgName = act.intent.getStringExtra("package_name")
                    intent.data = Uri.parse("package:".plus(pkgName))
                    act.startActivity(intent)
                    param.returnAndSkip(null)
                }
            }
        })
    }

    @JvmStatic
    fun SkipSecurityScanHook(lpparam: PackageReadyParam) {
        val skipScan = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip(ArrayList<Any>())
            }
        }
        ModuleHelper.findAndHookMethod("com.miui.securityscan.model.ModelFactory", lpparam.classLoader, "produceSystemGroupModel", Context::class.java, skipScan)
        ModuleHelper.findAndHookMethod("com.miui.securityscan.model.ModelFactory", lpparam.classLoader, "produceManualGroupModel", Context::class.java, skipScan)
        ModuleHelper.findAndHookMethod("com.miui.common.customview.ScoreTextView", lpparam.classLoader, "setScore", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = 100
            }
        })
        ModuleHelper.findAndHookMethod(ContentResolver::class.java, "call", Uri::class.java, String::class.java, String::class.java, Bundle::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if ("callPreference" == param.args[1] as? String && "GET" == param.args[2] as? String) {
                    val extras = param.args[3] as? Bundle ?: return
                    if ("latest_optimize_date" == extras.getString("key")) {
                        val res = Bundle()
                        res.putLong("latest_optimize_date", java.lang.System.currentTimeMillis() - 10000)
                        param.returnAndSkip(res)
                    }
                }
            }
        })
        ModuleHelper.findAndHookMethod("com.miui.securityscan.ui.main.MainContentFrame", lpparam.classLoader, "onClick", View::class.java, HookerClassHelper.DO_NOTHING)
    }

    @JvmStatic
    fun SmartClipboardActionHook(lpparam: PackageReadyParam) {
        val opt = MainModule.mPrefs.getStringAsInt("various_clipboard_defaultaction", 1)
        if (opt == 3) {
            ModuleHelper.findAndHookMethod("com.lbe.security.ui.ClipboardTipDialog", lpparam.classLoader, "customReadClipboardDialog", Context::class.java, String::class.java, HookerClassHelper.returnConstant(false))
        } else {
            ModuleHelper.findAndHookMethod("com.lbe.security.ui.ClipboardTipDialog", lpparam.classLoader, "customReadClipboardDialog", Context::class.java, String::class.java, HookerClassHelper.returnConstant(true))

            val securityPromptHandler = XposedHelpers.findClass("com.lbe.security.ui.SecurityPromptHandler", lpparam.classLoader)
            ModuleHelper.hookAllMethods(securityPromptHandler, "handleNewRequest", object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val permissionRequest = param.args[0] ?: return
                    val permId = XposedHelpers.callMethod(permissionRequest, "getPermission") as? Long ?: return
                    if (permId == 274877906944L) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "currentStopped", XposedHelpers.getBooleanField(param.thisObject, "mStopped"))
                    }
                }

                override fun after(param: AfterHookCallback) {
                    val permissionRequest = param.args[0] ?: return
                    val permId = XposedHelpers.callMethod(permissionRequest, "getPermission") as? Long ?: return
                    if (permId == 274877906944L) {
                        val mStopped = XposedHelpers.getAdditionalInstanceField(param.thisObject, "currentStopped") as? Boolean ?: return
                        if (mStopped) {
                            XposedHelpers.callMethod(param.thisObject, "gotChoice", 3, true, true)
                        }
                        XposedHelpers.removeAdditionalInstanceField(param.thisObject, "currentStopped")
                    }
                }
            })
        }
    }

    @JvmStatic
    fun ShowTempInBatteryHook(lpparam: PackageReadyParam) {
        val interceptBaseFragmentClass = XposedHelpers.findClass("com.miui.powercenter.BatteryFragment", lpparam.classLoader)
        val innerClasses = interceptBaseFragmentClass.declaredClasses
        var handlerClass: Class<*>? = null
        for (innerClass in innerClasses) {
            if (WeakReference::class.java.isAssignableFrom(innerClass)) {
                handlerClass = innerClass
                break
            }
        }
        if (handlerClass == null) return
        val fields = handlerClass.declaredFields
        var fieldName: String? = null
        for (field in fields) {
            if (WeakReference::class.java.isAssignableFrom(field.type)) {
                fieldName = field.name
                break
            }
        }
        if (fieldName == null) return
        val finalFieldName = fieldName
        ModuleHelper.findAndHookMethod(handlerClass, "handleMessage", Message::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val msg = param.args[0] as? Message ?: return
                if (msg.what != 1) return
                val wk = XposedHelpers.getObjectField(param.thisObject, finalFieldName)
                val frag = XposedHelpers.callMethod(wk, "get")
                val batteryView = XposedHelpers.callMethod(frag, "getActivity") as? Activity ?: return
                val temp = (batteryView.registerReceiver(null, IntentFilter("android.intent.action.BATTERY_CHANGED"), Context.RECEIVER_NOT_EXPORTED)?.getIntExtra("temperature", 0) ?: 0) / 10
                val symbolResId = batteryView.resources.getIdentifier("temp_symbol", "id", lpparam.packageName)
                val stateResId = batteryView.resources.getIdentifier("current_temperature_state", "id", lpparam.packageName)
                val stateTv = batteryView.findViewById<TextView>(stateResId)
                if (symbolResId > 0) {
                    stateTv?.visibility = View.GONE
                    val symbolTv = batteryView.findViewById<TextView>(symbolResId)
                    symbolTv?.visibility = View.VISIBLE
                    val digitResId = batteryView.resources.getIdentifier("current_temperature_value", "id", lpparam.packageName)
                    val digitTv = batteryView.findViewById<TextView>(digitResId)
                    digitTv?.text = temp.toString()
                    digitTv?.visibility = View.VISIBLE
                } else {
                    stateTv?.text = "${temp}℃"
                }
            }
        })
    }

    @JvmStatic
    fun DisableDockSuggestHook(lpparam: PackageReadyParam) {
        val clearHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val blackList = ArrayList<String>()
                blackList.add("xx.yy.zz")
                var topMethod = 10
                val stackTrace = Thread.currentThread().stackTrace
                for (el in stackTrace) {
                    if (el != null && topMethod < 20 && (el.className.contains("edit.DockAppEditActivity") || el.className.contains("BubblesSettings"))) {
                        return
                    }
                    topMethod++
                }
                param.returnAndSkip(blackList)
            }
        }
        ModuleHelper.hookAllMethodsSilently("android.util.MiuiMultiWindowUtils", lpparam.classLoader, "getFreeformSuggestionList", clearHook)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun UnlockClipboardAndLocationHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.permcenter.settings.PrivacyLabActivity", lpparam.classLoader, "onCreateFragment", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val utilCls = XposedHelpers.findClassIfExists("com.miui.permcenter.utils.h", lpparam.classLoader)
                if (utilCls != null) {
                    val fm = ModuleHelper.getStaticObjectFieldSilently(utilCls, "b")
                    if (fm != ModuleHelper.NOT_EXIST_SYMBOL) {
                        try {
                            val featMap = fm as? MutableMap<String, Int> ?: return
                            featMap["mi_lab_ai_clipboard_enable"] = 0
                            featMap["mi_lab_blur_location_enable"] = 0
                        } catch (_: Throwable) { }
                    }
                }
            }
        })
    }

    @JvmStatic
    fun AlarmCompatHook() {
        ModuleHelper.findAndHookMethod(Settings.System::class.java, "getStringForUser", ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val key = param.args[1] as? String ?: return
                if ("next_alarm_formatted" == key) {
                    param.args[1] = "next_alarm_clock_formatted"
                }
            }
        })
    }

    @JvmStatic
    fun AlarmCompatServiceHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.alarm.AlarmManagerService", lpparam.classLoader, "onBootPhase", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if ((param.args[0] as? Int ?: -1) != 500) return

                val mContext = XposedHelpers.callMethod(param.thisObject, "getContext") as? Context ?: run {
                    XposedHelpers.log("AlarmCompatServiceHook", "Context is NULL")
                    return
                }
                val resolver = mContext.contentResolver
                val alarmObserver = object : ContentObserver(Handler(mContext.mainLooper)) {
                    override fun onChange(selfChange: Boolean) {
                        ModuleHelper.guarded {
                            if (selfChange) return@guarded
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mNextAlarmTime", ModuleHelper.getNextMIUIAlarmTime(mContext))
                        }
                    }
                }
                alarmObserver.onChange(false)
                resolver.registerContentObserver(Settings.System.getUriFor("next_alarm_clock_formatted"), false, alarmObserver)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.alarm.AlarmManagerService", lpparam.classLoader, "getNextAlarmClockImpl", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.callMethod(param.thisObject, "getContext") as? Context ?: return
                val pkgName = mContext.packageManager.getNameForUid(Binder.getCallingUid())
                val mNextAlarmTime = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mNextAlarmTime")
                if (mNextAlarmTime != null && MainModule.mPrefs.getStringSet("various_alarmcompat_apps")?.contains(pkgName) == true) {
                    val time = mNextAlarmTime as? Long ?: 0L
                    param.setResult(if (time == 0L) null else AlarmManager.AlarmClockInfo(time, null))
                }
            }
        })
    }

    @JvmStatic
    fun AnswerCallInHeadUpHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.incallui.InCallPresenter", lpparam.classLoader, "answerIncomingCall", Context::class.java, String::class.java, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val showUi = param.args[3] as? Boolean ?: false
                if (showUi) {
                    val foregroundInfo = ProcessManager.getForegroundInfo()
                    if (foregroundInfo != null) {
                        val topPackage = foregroundInfo.mForegroundPackageName
                        if ("com.miui.home" != topPackage) {
                            param.args[3] = false
                        }
                    }
                }
            }
        })
    }

    @JvmStatic
    fun ShowCallUIHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.incallui.InCallPresenter", lpparam.classLoader, "startUi", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if ((param.result as? Boolean != true) || param.args[0]?.toString() != "INCOMING") return
                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) == 3) {
                    val topPackage = Settings.Global.getString(mContext.contentResolver, Helpers.modulePkg + ".foreground.package")
                    if (topPackage != null && topPackage != "com.miui.home") {
                        return
                    }
                }

                if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) == 1) {
                    val fullScreen = Settings.Global.getInt(mContext.contentResolver, Helpers.modulePkg + ".foreground.fullscreen", 0)
                    if (fullScreen == 1) return
                }

                XposedHelpers.callMethod(param.thisObject, "showInCall", false, false)
                val mStatusBarNotifier = XposedHelpers.getObjectField(param.thisObject, "mStatusBarNotifier")
                if (mStatusBarNotifier != null) XposedHelpers.callMethod(mStatusBarNotifier, "cancelInCall")
                param.setResult(true)
            }
        })
    }

    @JvmStatic
    fun InCallBrightnessHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.incallui.InCallActivity", lpparam.classLoader, "onCreate", Bundle::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val act = param.thisObject as? Activity ?: return

                var opt = MainModule.mPrefs.getStringAsInt("various_calluibright_type", 0)
                if (opt == 1 || opt == 2) {
                    val presenter = XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("com.android.incallui.InCallPresenter", lpparam.classLoader),
                        "getInstance"
                    )
                    if (presenter == null) {
                        XposedHelpers.log("InCallBrightnessHook", "InCallPresenter is null")
                        return
                    }

                    val state = XposedHelpers.callMethod(presenter, "getInCallState")?.toString() ?: ""
                    if (opt == 1 && state != "INCOMING") return
                    else if (opt == 2 && state != "OUTGOING" && state != "PENDING_OUTGOING") return
                }

                val key = "various_calluibright_night"
                val checkNight = MainModule.mPrefs.getBoolean(key)
                if (checkNight) {
                    val startHour = MainModule.mPrefs.getInt(key + "_start_hour", 0)
                    val startMinute = MainModule.mPrefs.getInt(key + "_start_minute", 0)
                    val endHour = MainModule.mPrefs.getInt(key + "_end_hour", 0)
                    val endMinute = MainModule.mPrefs.getInt(key + "_end_minute", 0)

                    val formatter = SimpleDateFormat("H:m", Locale.ENGLISH)
                    formatter.timeZone = TimeZone.getDefault()
                    val start = formatter.parse("$startHour:$startMinute") ?: return
                    val end = formatter.parse("$endHour:$endMinute") ?: return
                    val now = formatter.parse(formatter.format(Date())) ?: return

                    val isNight = if (start.before(end)) now.after(start) && now.before(end) else now.before(end) || now.after(start)
                    if (isNight) return
                }

                val params = act.window.attributes
                val value = MainModule.mPrefs.getInt("various_calluibright_val", 0)
                if (value == 0) return
                params.screenBrightness = value / 100f
                act.window.setAttributes(params)
            }
        })
    }

    private fun createTitleTextView(context: Context, lp: ViewGroup.LayoutParams, resId: Int): TextView {
        val tv = TextView(context)
        tv.maxLines = 1
        @Suppress("DEPRECATION")
        tv.setSingleLine(true)
        tv.gravity = Gravity.START
        tv.layoutParams = lp
        @Suppress("DEPRECATION")
        tv.setTextAppearance(resId.takeIf { it != -1 } ?: android.R.style.TextAppearance_DeviceDefault)
        return tv
    }

    private fun createValueTextView(context: Context, lp: ViewGroup.LayoutParams, resId: Int, gravity: Int): TextView {
        val tv = TextView(context)
        tv.maxLines = 1
        @Suppress("DEPRECATION")
        tv.setSingleLine(true)
        tv.gravity = gravity
        tv.ellipsize = TextUtils.TruncateAt.START
        tv.layoutParams = lp
        @Suppress("DEPRECATION")
        tv.setTextAppearance(resId.takeIf { it != -1 } ?: android.R.style.TextAppearance_DeviceDefault)
        return tv
    }

    @JvmStatic
    fun AppInfoDuringMiuiInstallHook(lpparam: PackageReadyParam) {
        val appInfoViewObjectClass = XposedHelpers.findClassIfExists("com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject", lpparam.classLoader)
        if (appInfoViewObjectClass != null) {
            val viewHolderClass = XposedHelpers.findClassIfExists("com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject\$ViewHolder", lpparam.classLoader)
            val methods = XposedHelpers.findMethodsByExactParameters(appInfoViewObjectClass, Void.TYPE, viewHolderClass)
            if (methods.isEmpty()) {
                XposedHelpers.log("AppInfoDuringMiuiInstallHook", "Cannot find appropriate method")
                return
            }
            val apkInfoClass = XposedHelpers.findClassIfExists("com.miui.packageInstaller.model.ApkInfo", lpparam.classLoader)

            val fields = appInfoViewObjectClass.declaredFields
            var apkInfoFieldName: String? = null
            for (field in fields) {
                if (apkInfoClass != null && apkInfoClass.isAssignableFrom(field.type)) {
                    apkInfoFieldName = field.name
                    break
                }
            }
            if (apkInfoFieldName == null) return
            val finalApkInfoFieldName = apkInfoFieldName
            ModuleHelper.hookMethod(methods[0], object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val viewHolder = param.args[0] ?: return
                    val tvAppVersion = XposedHelpers.callMethod(viewHolder, "getTvDes") as? TextView ?: return
                    val tvAppSize = XposedHelpers.callMethod(viewHolder, "getAppSize") as? TextView ?: return
                    val tvAppName = XposedHelpers.callMethod(viewHolder, "getTvAppName") as? TextView ?: return

                    val appNameLp = tvAppName.layoutParams as? ViewGroup.MarginLayoutParams
                    if (appNameLp != null) {
                        appNameLp.topMargin = 0
                        tvAppName.layoutParams = appNameLp
                    }

                    val apkInfo = XposedHelpers.getObjectField(param.thisObject, finalApkInfoFieldName)
                    val mAppInfo = XposedHelpers.callMethod(apkInfo, "getInstalledPackageInfo") as? ApplicationInfo
                    val mPkgInfo = XposedHelpers.callMethod(apkInfo, "getPackageInfo") as? PackageInfo ?: return
                    val modRes = ModuleHelper.getModuleRes(tvAppVersion.context)
                    val builder = SpannableStringBuilder()
                    builder.append(modRes.getString(R.string.various_installappinfo_vername)).append(": ")
                    if (mAppInfo != null) builder.append((XposedHelpers.callMethod(apkInfo, "getInstalledVersionName") as? String) ?: "").append(" ➟ ")
                    builder.append(mPkgInfo.versionName).append("\n")
                    builder.append(tvAppSize.text).append("\n")
                    builder.append(modRes.getString(R.string.various_installappinfo_vercode)).append(": ")
                    if (mAppInfo != null) builder.append((XposedHelpers.callMethod(apkInfo, "getInstalledVersionCode") as? Int)?.toString() ?: "").append(" ➟ ")
                    builder.append(mPkgInfo.longVersionCode.toString()).append("\n")
                    builder.append(modRes.getString(R.string.various_installappinfo_sdk)).append(": ")
                    if (mAppInfo != null) builder.append(mAppInfo.minSdkVersion.toString()).append("-").append(mAppInfo.targetSdkVersion.toString()).append(" ➟ ")
                    builder.append(mPkgInfo.applicationInfo?.minSdkVersion?.toString() ?: "").append("-").append(mPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "")

                    tvAppVersion.text = builder
                    @Suppress("DEPRECATION")
                    tvAppVersion.setSingleLine(false)
                    tvAppVersion.maxLines = 10
                    val layout = tvAppVersion.parent as? LinearLayout
                    val versionSizeLp = layout?.layoutParams as? ViewGroup.MarginLayoutParams
                    if (versionSizeLp != null) {
                        versionSizeLp.topMargin = 0
                        layout.layoutParams = versionSizeLp
                    }
                    layout?.removeAllViews()
                    layout?.addView(tvAppVersion)
                }
            })
        } else {
            val installActivity = XposedHelpers.findClassIfExists("com.android.packageinstaller.PackageInstallerActivity", lpparam.classLoader)
            if (installActivity == null) {
                XposedHelpers.log("AppInfoDuringMiuiInstallHook", "Cannot find appropriate activity")
                return
            }
            val methods = XposedHelpers.findMethodsByExactParameters(installActivity, Void.TYPE, String::class.java)
            if (methods.isEmpty()) {
                XposedHelpers.log("AppInfoDuringMiuiInstallHook", "Cannot find appropriate method")
                return
            }
            for (method in methods) {
                ModuleHelper.hookMethod(method, object : MethodHook() {
                    override fun after(param: AfterHookCallback) {
                        val act = param.thisObject as? Activity ?: return
                        val version = act.findViewById<TextView>(act.resources.getIdentifier("install_version", "id", lpparam.packageName))
                        val fPkgInfo = XposedHelpers.findFirstFieldByExactType(param.thisObject.javaClass, PackageInfo::class.java)
                        val mPkgInfo = fPkgInfo.get(param.thisObject) as? PackageInfo ?: return
                        if (version == null) return

                        val source = act.findViewById<TextView>(act.resources.getIdentifier("install_source", "id", lpparam.packageName))
                        source?.gravity = Gravity.CENTER_HORIZONTAL
                        source?.text = mPkgInfo.packageName

                        val mAppInfo = try {
                            act.packageManager.getPackageInfo(mPkgInfo.packageName, 0)
                        } catch (_: Throwable) {
                            null
                        }

                        val modRes = ModuleHelper.getModuleRes(act)
                        val builder = SpannableStringBuilder()
                        builder.append(modRes.getString(R.string.various_installappinfo_vername)).append(":\t\t")
                        if (mAppInfo != null) builder.append(mAppInfo.versionName ?: "").append("  ➟  ")
                        builder.append(mPkgInfo.versionName).append("\n")
                        builder.append(modRes.getString(R.string.various_installappinfo_vercode)).append(":\t\t")
                        if (mAppInfo != null) builder.append(mAppInfo.versionCode.toString()).append("  ➟  ")
                        builder.append(mPkgInfo.versionCode.toString()).append("\n")
                        builder.append(modRes.getString(R.string.various_installappinfo_sdk)).append(":\t\t")
                        if (mAppInfo != null) builder.append(mAppInfo.applicationInfo?.minSdkVersion?.toString() ?: "").append("-").append(mAppInfo.applicationInfo?.targetSdkVersion?.toString() ?: "").append("  ➟  ")
                        builder.append(mPkgInfo.applicationInfo?.minSdkVersion?.toString() ?: "").append("-").append(mPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "")

                        version.gravity = Gravity.CENTER_HORIZONTAL
                        @Suppress("DEPRECATION")
                        version.setSingleLine(false)
                        version.maxLines = 10
                        version.text = builder
                        version.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.09f)
                    }
                })
            }
        }
    }

    @JvmStatic
    fun MiuiPackageInstallerHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "getBoolean", String::class.java, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if ("persist.sys.allow_sys_app_update" == param.args[0] as? String) {
                    param.returnAndSkip(true)
                }
            }
        })
        ModuleHelper.findAndHookMethodSilently("com.miui.packageInstaller.InstallStart", lpparam.classLoader, "getCallingPackage", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.returnAndSkip("com.android.fileexplorer")
            }
        })
    }

    @JvmStatic
    fun GboardPaddingHook(lpparam: PackageReadyParam) {
        val cls = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)
        ModuleHelper.findAndHookMethod(cls, "get", String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val key = param.args[0] as? String ?: return
                if (key == "ro.com.google.ime.kb_pad_port_b") {
                    val opt = MainModule.mPrefs.getInt("various_gboardpadding_port", 0)
                    if (opt > 0) param.returnAndSkip(opt.toString())
                } else if (key == "ro.com.google.ime.kb_pad_land_b") {
                    val opt = MainModule.mPrefs.getInt("various_gboardpadding_land", 0)
                    if (opt > 0) param.returnAndSkip(opt.toString())
                }
            }
        })
    }

    @JvmStatic
    fun FixInputMethodBottomMarginHook(lpparam: PackageReadyParam) {
        val inputMethodServiceInjectorClass = XposedHelpers.findClassIfExists("android.inputmethodservice.InputMethodServiceInjector", lpparam.classLoader)
        ModuleHelper.hookAllMethods(inputMethodServiceInjectorClass, "addMiuiBottomView", object : MethodHook() {
            private var isHooked = false
            override fun after(param: AfterHookCallback) {
                if (isHooked) return
                val sClassLoader = XposedHelpers.getStaticObjectField(inputMethodServiceInjectorClass, "sClassLoader") as? ClassLoader ?: return
                isHooked = true
                val inputMethodUtil = XposedHelpers.findClassIfExists("com.miui.inputmethod.InputMethodUtil", sClassLoader) ?: return
                XposedHelpers.setStaticBooleanField(inputMethodUtil, "sIsGestureLineEnable", false)
                ModuleHelper.findAndHookMethod(inputMethodUtil, "updateGestureLineEnable", Context::class.java, object : MethodHook() {
                    override fun before(param: BeforeHookCallback) {
                        XposedHelpers.setStaticBooleanField(inputMethodUtil, "sIsGestureLineEnable", false)
                        param.returnAndSkip(null)
                    }
                })
            }
        })
    }
}
