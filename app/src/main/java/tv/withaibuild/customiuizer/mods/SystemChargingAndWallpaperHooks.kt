package tv.withaibuild.customiuizer.mods

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.TextView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.HookUtils
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.*

object SystemChargingAndWallpaperHooks {

    @JvmStatic
    fun ChargingInfoHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.keyguard.charge.ChargeUtils", lpparam.classLoader, "getChargingHintText", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val charge = param.args[2] as? Int ?: return
                val hint = param.result as? String ?: return

                if (charge < 100) {
                    val showCurr = MainModule.mPrefs.getBoolean("system_charginginfo_current")
                    val showVolt = MainModule.mPrefs.getBoolean("system_charginginfo_voltage")
                    val showWatt = MainModule.mPrefs.getBoolean("system_charginginfo_wattage")
                    val showTemp = MainModule.mPrefs.getBoolean("system_charginginfo_temp")

                    val values = ArrayList<String>()
                    var props: Properties? = null
                    var fis: FileInputStream? = null
                    try {
                        fis = FileInputStream("/sys/class/power_supply/battery/uevent")
                        props = Properties()
                        props.load(fis)
                    } catch (ign: Throwable) {
                    } finally {
                        try {
                            fis?.close()
                        } catch (ign: Throwable) {
                        }
                    }
                    if (props != null) {
                        val currVal = Math.abs((props.getProperty("POWER_SUPPLY_CURRENT_NOW")?.toIntOrNull() ?: 0) / 1000f / 1000f)
                        if (showCurr) values.add(String.format(Locale.getDefault(), "%.2f", currVal) + " A")
                        val voltVal = (props.getProperty("POWER_SUPPLY_VOLTAGE_NOW")?.toIntOrNull() ?: 0) / 1000f / 1000f
                        if (showVolt) values.add(String.format(Locale.getDefault(), "%.1f", voltVal) + " V")
                        if (showWatt) values.add(String.format(Locale.getDefault(), "%.1f", voltVal * currVal) + " W")
                        if (showTemp) {
                            val tempVal = props.getProperty("POWER_SUPPLY_TEMP")?.toIntOrNull() ?: 0
                            values.add(Math.round(tempVal / 10f).toString() + " ℃")
                        }
                    }
                    if (values.size == 0) return
                    val info = TextUtils.join(" · ", values)

                    val opt = MainModule.mPrefs.getStringAsInt("system_charginginfo_view", 1)
                    when (opt) {
                        1 -> param.setResult(hint + "\n" + info)
                        2 -> param.setResult(hint + " · " + info)
                        3 -> param.setResult(info + " · " + hint)
                    }
                }
            }
        })

        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.phone.KeyguardIndicationTextView", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val opt = MainModule.mPrefs.getStringAsInt("system_charginginfo_view", 1)
                if (opt != 1) return
                val indicator = param.thisObject as? TextView ?: return
                indicator.setSingleLine(false)
            }
        })
    }

    @JvmStatic
    fun SetLockscreenWallpaperHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.wallpaper.WallpaperManagerService", lpparam.classLoader, "setWallpaper", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (param.throwable != null || param.result == null || param.args[5] == 1 || "com.android.thememanager" == param.args[1]) return

                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val thisObject = param.thisObject

                var handleIncomingUser = 0
                try {
                    handleIncomingUser = XposedHelpers.callStaticMethod(ActivityManager::class.java, "handleIncomingUser", Binder.getCallingPid(), Binder.getCallingUid(), param.args[7], false, true, "changing wallpaper", null) as? Int ?: 0
                } catch (ignore: Throwable) {
                }
                val wallpaperData = XposedHelpers.callMethod(param.thisObject, "getWallpaperSafeLocked", handleIncomingUser, param.args[5])
                val wallpaper = XposedHelpers.getObjectField(wallpaperData, "wallpaperFile") as? File ?: return

                var wallpaperHandler = XposedHelpers.getAdditionalInstanceField(thisObject, "mWallpaperHandler") as? Handler
                if (wallpaperHandler == null) {
                    wallpaperHandler = Handler(Looper.getMainLooper())
                    XposedHelpers.setAdditionalInstanceField(thisObject, "mWallpaperHandler", wallpaperHandler)
                }
                val oldWallpaperRunnable = XposedHelpers.getAdditionalInstanceField(thisObject, "mWallpaperRunnable") as? Runnable
                if (oldWallpaperRunnable != null) wallpaperHandler.removeCallbacks(oldWallpaperRunnable)

                val wallpaperRunnable = Runnable {
                    ModuleHelper.guarded("SystemChargingAndWallpaperHooks.wallpaperWriter") {
                        if (!wallpaper.exists()) return@guarded

                        val lockWallpaperPath = "/data/system/theme/thirdparty_lock_wallpaper"
                        HookUtils.copyFile(wallpaper.absolutePath, lockWallpaperPath)
                        val ThemeUtils = XposedHelpers.findClass("miui.content.res.ThemeNativeUtils", lpparam.classLoader)
                        XposedHelpers.callStaticMethod(ThemeUtils, "updateFilePermissionWithThemeContext", lockWallpaperPath)
                        val data = JSONObject()
                        val ex = JSONObject()
                        try {
                            val lockWallpaper = File(lockWallpaperPath)
                            ex
                            .put("link_type", "0")
                            .put("title_size", "26")
                            .put("item_id", "wallpaper1")
                            .put("title_color", "#ffffffff")
                            .put("index_in_album", "1")
                            .put("tag_list", "CustoMIUIzer,mod")
                            .put("content_color", "#ffffffff")
                            .put("total_of_album", "1")
                            .put("img_level", "0")
                            .put("album_id", "1")
                            .put("title_customized", "0")
                            .put("lks_entry_text", "Some wallpaper")

                            data
                            .put("authority", "tv.withaibuild.customiuizer.mods.set_lockscreen_wallpaper")
                            .put("content", "Wallpaper set by some app")
                            .put("contentColorValue", 0)
                            .put("cp", "CustoMIUIzer")
                            .put("cpColorValue", 0)
                            .put("definition", -1)
                            .put("ex", ex.toString())
                            .put("fromColorValue", 0)
                            .put("hasAcc", false)
                            .put("indexInAlbum", -1)
                            .put("isAd", false)
                            .put("isCustom", false)
                            .put("isFd", false)
                            .put("isFrontCover", false)
                            .put("key", "wallpaper1")
                            .put("like", false)
                            .put("linkType", 0)
                            .put("noApply", false)
                            .put("noDislike", false)
                            .put("noSave", false)
                            .put("noShare", false)
                            .put("pos", 0)
                            .put("supportLike", true)
                            .put("title", "Some wallpaper")
                            .put("titleColorValue", 0)
                            .put("titleTextSize", -1)
                            .put("totalOfAlbum", -1)
                            .put("wallpaperUri", lockWallpaper.toURI())
                        } catch (t: Throwable) {
                            XposedHelpers.log(t)
                        }

                        val setIntent = Intent("com.miui.miwallpaper.UPDATE_LOCKSCREEN_WALLPAPER")
                        setIntent.putExtra("wallpaperInfo", data.toString())
                        setIntent.putExtra("apply", true)
                        mContext.sendBroadcast(setIntent)
                    }
                }
                wallpaperHandler.postDelayed(wallpaperRunnable, 1800)
                XposedHelpers.setAdditionalInstanceField(thisObject, "mWallpaperRunnable", wallpaperRunnable)
            }
        })
    }
}
