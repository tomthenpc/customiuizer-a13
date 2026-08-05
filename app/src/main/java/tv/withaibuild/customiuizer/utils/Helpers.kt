package tv.withaibuild.customiuizer.utils

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager.WakeLock
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.LruCache
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceScreen
import miui.util.HapticFeedbackUtil
import tv.withaibuild.customiuizer.BuildConfig
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.prefs.PreferenceCategoryEx
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Settings app utilities. Functions also available in [HookUtils] are kept here for
 * settings-UI compatibility; hook code must use [HookUtils] to avoid running the
 * Helpers initializer in `system_server`, SystemUI or Launcher.
 */
@Suppress("WeakerAccess")
object Helpers {

    @JvmField
    var installedAppsList: ArrayList<AppData>? = null
    @JvmField
    var launchableAppsList: ArrayList<AppData>? = null
    @JvmField
    var openWithAppsList: ArrayList<AppData>? = null
    @JvmField
    var shareAppsList: ArrayList<AppData>? = null
    @JvmField
    var allModsList: ArrayList<ModData> = ArrayList()

    @JvmField
    var mWakeLock: WakeLock? = null
    @JvmField
    var showNewMods: Boolean = true
    @JvmField
    var withinAppContext: Boolean = false

    private val ICON_CACHE_KB =
        (Runtime.getRuntime().maxMemory() / 1024 / 16)
            .toInt()
            .coerceIn(512, 8 * 1024)

    @JvmField
    val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(ICON_CACHE_KB) {
        override fun sizeOf(key: String, icon: Bitmap?): Int {
            return icon?.allocationByteCount?.div(1024) ?: (130 * 130 * 4 / 1024)
        }
    }

    @JvmStatic
    fun invalidateAppCaches() {
        memoryCache.evictAll()
        installedAppsList = null
        launchableAppsList = null
        openWithAppsList = null
        shareAppsList = null
    }

    @JvmField
    val newMods: HashSet<String> = HashSet(listOf("pref_key_launcher_nozoomanim"))

    const val modulePkg: String = BuildConfig.APPLICATION_ID
    const val ANDROID_NS: String = "http://schemas.android.com/apk/res/android"
    const val MIUIZER_NS: String = "http://schemas.android.com/apk/res-auto"
    const val ACCESS_SECURITY_CENTER: String = "com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER"
    const val NEW_MODS_SEARCH_QUERY: String = "\uD83C\uDD95"

    @JvmField
    val markColor: Int = Color.rgb(205, 73, 97)
    @JvmField
    val markColorVibrant: Int = Color.rgb(255, 0, 0)

    const val REQUEST_PERMISSIONS_WIFI: Int = 3
    const val REQUEST_PERMISSIONS_REPORT: Int = 4
    const val REQUEST_PERMISSIONS_BLUETOOTH: Int = 5
    const val REQUEST_PERMISSIONS_SECURITY_CENTER: Int = 6

    @JvmField
    val isMIUI14: Boolean = try {
        miui.os.Build.getMiUiVersionCode()?.toIntOrNull()?.let { it > 13 } ?: false
    } catch (t: Throwable) {
        if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
        false
    }

    enum class SettingsType {
        Preference, Edit
    }

    enum class AppAdapterType {
        Default, Standalone, Mutli, CustomTitles, Activities
    }

    enum class ActionBarType {
        HomeUp, Edit
    }

    object MimeType {
        const val IMAGE: Int = 1
        const val AUDIO: Int = 2
        const val VIDEO: Int = 4
        const val DOCUMENT: Int = 8
        const val ARCHIVE: Int = 16
        const val LINK: Int = 32
        const val OTHERS: Int = 64
        const val ALL: Int = IMAGE or AUDIO or VIDEO or DOCUMENT or ARCHIVE or LINK or OTHERS
    }

    @JvmStatic
    fun setMiuiCheckbox(checkbox: CheckBox?) {
        if (checkbox == null) return
        checkbox.background = null
        val btnResID = checkbox.resources.getIdentifier(
            if (isNightMode(checkbox.context)) "btn_checkbox_dark" else "btn_checkbox_light",
            "drawable",
            "miui"
        )
        try {
            checkbox.setButtonDrawable(if (btnResID == 0) R.drawable.btn_checkbox else btnResID)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            checkbox.setButtonDrawable(R.drawable.btn_checkbox)
        }
    }

    @JvmStatic
    fun setMiuiPrefItem(item: View?) {
        if (item == null) return
        item.setBackgroundResource(R.drawable.list_item_bg)
        val title: TextView? = item.findViewById(android.R.id.title)
        var resId = item.resources.getIdentifier("preference_item_bg", "drawable", "miui")
        if (resId != 0) item.setBackgroundResource(resId)
        resId = item.resources.getIdentifier("normal_text_size", "dimen", "miui")
        if (resId != 0 && title != null) {
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, item.resources.getDimensionPixelSize(resId).toFloat())
        }
        resId = item.resources.getIdentifier("secondary_text_size", "dimen", "miui")
        if (resId != 0) {
            val summary: TextView? = item.findViewById(android.R.id.summary)
            val text1: TextView? = item.findViewById(android.R.id.text1)
            val text2: TextView? = item.findViewById(android.R.id.text2)
            val size = item.resources.getDimensionPixelSize(resId)
            summary?.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
            text1?.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
            text2?.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        }
        if (title != null && "header" == title.tag) {
            val resIdSize = item.resources.getIdentifier("preference_category_text_size", "dimen", "miui")
            if (resIdSize != 0) title.setTextSize(TypedValue.COMPLEX_UNIT_PX, item.resources.getDimensionPixelSize(resIdSize).toFloat())
        }

        val resIdLeft = item.resources.getIdentifier("preference_item_padding_left", "dimen", "miui")
        val resIdRight = item.resources.getIdentifier("preference_item_padding_right", "dimen", "miui")
        val resIdTop = item.resources.getIdentifier("preference_item_padding_top", "dimen", "miui")
        val resIdBottom = item.resources.getIdentifier("preference_item_padding_bottom", "dimen", "miui")
        val paddingLeft = if (resIdLeft == 0) item.paddingLeft else item.resources.getDimensionPixelSize(resIdLeft)
        val paddingRight = if (resIdRight == 0) item.paddingRight else item.resources.getDimensionPixelSize(resIdRight)
        val paddingTop = if (resIdTop == 0) item.paddingTop else item.resources.getDimensionPixelSize(resIdTop)
        val paddingBottom = if (resIdBottom == 0) item.paddingBottom else item.resources.getDimensionPixelSize(resIdBottom)
        item.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    @JvmStatic
    fun isNightMode(context: Context?): Boolean {
        if (context == null) return false
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    @JvmStatic
    fun isDeviceEncrypted(context: Context?): Boolean {
        if (context == null) return false
        val policyMgr = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val encryption = policyMgr.storageEncryptionStatus
        return encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
            encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING ||
            encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
    }

    @JvmStatic
    fun launchActivity(act: AppCompatActivity?, pkg: String, cmp: String) {
        launchActivity(act, pkg, cmp, false)
    }

    @JvmStatic
    fun launchActivity(act: AppCompatActivity?, pkg: String, cmp: String, silent: Boolean): Boolean {
        if (act == null) return false
        val pm = act.packageManager
        return try {
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            intent.component = ComponentName(pkg, cmp)
            act.startActivity(intent)
            act.overridePendingTransition(R.anim.activity_open_enter, R.anim.activity_open_exit)
            true
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            if (!silent) Toast.makeText(act, R.string.various_hiddenfeatures_not_found, Toast.LENGTH_LONG).show()
            false
        }
    }

    @JvmStatic
    fun hideKeyboard(act: AppCompatActivity?, view: View?) {
        if (view == null) return
        try {
            val context = act ?: view.context
            val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
            val currentFocusedView = act?.currentFocus ?: view
            val token = currentFocusedView.windowToken
            if (token != null) inputManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            t.printStackTrace()
        }
    }

    @JvmStatic
    fun showOKDialog(context: Context?, title: Int, text: Int) {
        if (context == null) return
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .show()
    }

    interface InputCallback {
        fun onInputFinished(key: String, text: String)
    }

    @JvmStatic
    fun checkStorageReadable(context: Context?): Boolean {
        if (context == null) return false
        val state = Environment.getExternalStorageState()
        return if (state == Environment.MEDIA_MOUNTED_READ_ONLY || state == Environment.MEDIA_MOUNTED) {
            true
        } else {
            showOKDialog(context, R.string.warning, R.string.storage_unavailable)
            false
        }
    }

    @JvmStatic
    fun checkSettingsPerm(act: AppCompatActivity?): Boolean {
        if (act == null) return false
        return act.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun checkPermAndRequest(act: AppCompatActivity?, perm: String, action: Int): Boolean {
        if (act == null) return false
        if (act.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            act.requestPermissions(arrayOf(perm), action)
            return false
        }
        return true
    }

    @JvmStatic
    fun emptyFile(pathToFile: String, forceClear: Boolean) {
        val f = File(pathToFile)
        if (f.exists() && (f.length() > 150 * 1024 || forceClear)) {
            try {
                FileOutputStream(f, false).use { fOut ->
                    OutputStreamWriter(fOut).use { output ->
                        output.write("")
                    }
                }
            } catch (ignore: Throwable) {
                if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
}
        }
    }

    @JvmStatic
    fun getNextStockAlarmTime(context: Context?): Long {
        if (context == null) return 0
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0
        return alarmMgr.nextAlarmClock?.triggerTime ?: 0
    }

    @JvmStatic
    fun updateNewModsMarking(context: Context?, opt: Int) {
        if (context == null) return
        try {
            val appInfo = context.packageManager.getApplicationInfo(modulePkg, 0)
            val appInstalled = System.currentTimeMillis() - File(appInfo.sourceDir).lastModified()
            showNewMods = when (opt) {
                0 -> false
                4 -> true
                else -> appInstalled < when (opt) {
                    1 -> 1
                    2 -> 3
                    else -> 7
                } * 24 * 60 * 60 * 1000
            }
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            t.printStackTrace()
        }
    }

    @JvmStatic
    fun applyNewMod(title: TextView?) {
        if (title == null) return
        val titleStr = title.text.toString()
        val newModStr = title.resources.getString(R.string.miuizer_new_mod) + " "
        val start = titleStr.length + 3
        val end = start + newModStr.length
        val ssb = SpannableStringBuilder(title.text.toString() + "   " + newModStr)
        ssb.setSpan(ForegroundColorSpan(markColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.setSpan(RelativeSizeSpan(0.75f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = ssb
    }

    @JvmStatic
    fun applySearchItemHighlight(finalView: View?) {
        if (finalView == null) return
        val highColor = finalView.resources.getColor(R.color.color_popup_background, finalView.context.theme)
        val colorAnim = ObjectAnimator.ofInt(finalView, "backgroundColor", highColor, Color.TRANSPARENT)
        colorAnim.duration = 1200
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.repeatCount = 1
        colorAnim.startDelay = 300
        colorAnim.start()
    }

    @JvmStatic
    fun openURL(context: Context?, url: String?) {
        if (context == null || url == null) return
        val uriIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(uriIntent)
    }

    @JvmStatic
    fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            Resources.getSystem().displayMetrics
        )
    }

    @JvmStatic
    fun isReallyVisible(view: View?): Boolean {
        if (view == null || !view.isShown || view.alpha == 0f) return false
        val actualPosition = Rect()
        view.getGlobalVisibleRect(actualPosition)
        return actualPosition.intersect(
            Rect(0, 0, view.resources.displayMetrics.widthPixels, view.resources.displayMetrics.heightPixels)
        )
    }

    @JvmStatic
    @JvmOverloads
    fun getChildViewsRecursive(view: View?, includeContainers: Boolean = true): ArrayList<View> {
        if (view == null) return ArrayList()
        return if (view is ViewGroup) {
            val list2 = ArrayList<View>()
            val viewgroup = view
            val childCount = viewgroup.childCount
            for (i in 0 until childCount) {
                val view1 = viewgroup.getChildAt(i) ?: continue
                val list3 = ArrayList<View>()
                if (includeContainers) list3.add(view)
                list3.addAll(getChildViewsRecursive(view1))
                list2.addAll(list3)
            }
            list2
        } else {
            val list1 = ArrayList<View>()
            list1.add(view)
            list1
        }
    }

    private fun getModTitle(res: Resources, title: String?): String? {
        if (title == null) return null
        val titleResId = title.substring(1).toIntOrNull() ?: return null
        if (titleResId <= 0) return null
        return res.getString(titleResId)
    }

    private fun checkMultiUserPermission(context: Context): Boolean {
        return context.packageManager.checkPermission("android.permission.INTERACT_ACROSS_USERS", modulePkg) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @JvmStatic
    fun getAnimationScale(type: Int): Float {
        return try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getDeclaredMethod("getService", String::class.java)
            getService.isAccessible = true
            val manager = getService.invoke(smClass, "window")

            val wmsClass = Class.forName("android.view.IWindowManager\$Stub")
            val asInterface = wmsClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterface.isAccessible = true
            val wm = asInterface.invoke(wmsClass, manager)

            val getAnimationScale = wm.javaClass.getDeclaredMethod("getAnimationScale", Int::class.javaPrimitiveType)
            getAnimationScale.isAccessible = true
            (getAnimationScale.invoke(wm, type) as? Float) ?: 1.0f
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            t.printStackTrace()
            1.0f
        }
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @JvmStatic
    fun setAnimationScale(type: Int, value: Float) {
        try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getDeclaredMethod("getService", String::class.java)
            getService.isAccessible = true
            val manager = getService.invoke(smClass, "window")

            val wmsClass = Class.forName("android.view.IWindowManager\$Stub")
            val asInterface = wmsClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterface.isAccessible = true
            val wm = asInterface.invoke(wmsClass, manager)

            val setAnimationScale = wm.javaClass.getDeclaredMethod("setAnimationScale", Int::class.javaPrimitiveType, Float::class.javaPrimitiveType)
            setAnimationScale.isAccessible = true
            setAnimationScale.invoke(wm, type, value)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            t.printStackTrace()
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getPackageInfoAsUser(context: Context): Method? {
        return try {
            context.packageManager.javaClass.getDeclaredMethod("getPackageInfoAsUser", String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            t.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun getInstalledApps(context: Context) {
        val pm = context.packageManager
        var includeDualApps = checkMultiUserPermission(context)
        val getPackageInfoAsUser = getPackageInfoAsUser(context)
        if (getPackageInfoAsUser == null) includeDualApps = false

        val packs = pm.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS)
        installedAppsList = ArrayList()
        var app: AppData
        for (pack in packs) try {
            app = AppData()
            app.enabled = pack.enabled
            app.label = pack.loadLabel(pm).toString()
            app.pkgName = pack.packageName
            app.actName = "-"
            installedAppsList?.add(app)
            if (includeDualApps) try {
                if (getPackageInfoAsUser?.invoke(pm, app.pkgName, 0, 999) != null) {
                    val appDual = AppData()
                    appDual.enabled = pack.enabled
                    appDual.label = pack.loadLabel(pm).toString()
                    appDual.pkgName = pack.packageName
                    appDual.actName = "-"
                    appDual.user = 999
                    installedAppsList?.add(appDual)
                }
            } catch (ignore: Throwable) {
                if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
}
        } catch (e: Throwable) {
            if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
            e.printStackTrace()
        }
        installedAppsList?.sortWith { a, b -> (a.label ?: "").compareTo((b.label ?: ""), true) }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @JvmStatic
    fun getLaunchableApps(context: Context) {
        val pm = context.packageManager
        var includeDualApps = checkMultiUserPermission(context)
        val getPackageInfoAsUser = getPackageInfoAsUser(context)
        if (getPackageInfoAsUser == null) includeDualApps = false

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packs = pm.queryIntentActivities(mainIntent, 0)
        launchableAppsList = ArrayList()
        var app: AppData
        for (pack in packs) try {
            app = AppData()
            app.pkgName = pack.activityInfo.applicationInfo.packageName
            app.actName = pack.activityInfo.name
            app.enabled = pack.activityInfo.enabled
            app.label = pack.loadLabel(pm).toString()
            launchableAppsList?.add(app)
            if (includeDualApps) try {
                if (getPackageInfoAsUser?.invoke(pm, app.pkgName, 0, 999) != null) {
                    val appDual = AppData()
                    appDual.pkgName = pack.activityInfo.applicationInfo.packageName
                    appDual.actName = pack.activityInfo.name
                    appDual.enabled = pack.activityInfo.enabled
                    appDual.label = pack.loadLabel(pm).toString()
                    appDual.user = 999
                    launchableAppsList?.add(appDual)
                }
            } catch (ignore: Throwable) {
                if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
}
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            t.printStackTrace()
        }
        launchableAppsList?.sortWith { a, b -> (a.label ?: "").compareTo((b.label ?: ""), true) }
    }

    @JvmStatic
    fun getShareApps(context: Context) {
        val pm = context.packageManager
        var includeDualApps = checkMultiUserPermission(context)
        val getPackageInfoAsUser = getPackageInfoAsUser(context)
        if (getPackageInfoAsUser == null) includeDualApps = false

        val mainIntent = Intent()
        mainIntent.action = Intent.ACTION_SEND
        mainIntent.type = "*/*"
        mainIntent.putExtra("CustoMIUIzer", true)
        val packs = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS)
        shareAppsList = ArrayList()
        var app: AppData
        for (pack in packs) try {
            var exists = false
            for (shareApp in shareAppsList.orEmpty()) {
                if (shareApp.pkgName == pack.activityInfo.applicationInfo.packageName) {
                    exists = true
                    break
                }
            }
            if (exists) continue
            app = AppData()
            app.pkgName = pack.activityInfo.applicationInfo.packageName
            app.actName = "-"
            app.enabled = pack.activityInfo.applicationInfo.enabled
            app.label = pack.activityInfo.applicationInfo.loadLabel(pm).toString()
            shareAppsList?.add(app)
            if (includeDualApps) try {
                if (getPackageInfoAsUser?.invoke(pm, app.pkgName, 0, 999) != null) {
                    val appDual = AppData()
                    appDual.pkgName = pack.activityInfo.applicationInfo.packageName
                    appDual.actName = "-"
                    appDual.enabled = pack.activityInfo.applicationInfo.enabled
                    appDual.label = pack.activityInfo.applicationInfo.loadLabel(pm).toString()
                    appDual.user = 999
                    shareAppsList?.add(appDual)
                }
            } catch (ignore: Throwable) {
                if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
}
        } catch (e: Throwable) {
            if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
            e.printStackTrace()
        }
        shareAppsList?.sortWith { a, b -> (a.label ?: "").compareTo((b.label ?: ""), true) }
    }

    @JvmStatic
    fun getOpenWithApps(context: Context) {
        val pm = context.packageManager
        var includeDualApps = checkMultiUserPermission(context)
        val getPackageInfoAsUser = getPackageInfoAsUser(context)
        if (getPackageInfoAsUser == null) includeDualApps = false

        val mainIntent = Intent()
        mainIntent.action = Intent.ACTION_VIEW
        mainIntent.setDataAndType(null, "*/*")
        mainIntent.putExtra("CustoMIUIzer", true)
        val packs = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS).toMutableList()

        val mainIntent2 = Intent()
        mainIntent2.action = Intent.ACTION_VIEW
        mainIntent2.data = Uri.parse("https://github.com")
        mainIntent2.putExtra("CustoMIUIzer", true)
        val packs2 = pm.queryIntentActivities(mainIntent2, PackageManager.MATCH_ALL)

        packs.addAll(packs2)

        openWithAppsList = ArrayList()
        var app: AppData
        for (pack in packs) try {
            var exists = false
            for (openWithApp in openWithAppsList.orEmpty()) {
                if (openWithApp.pkgName == pack.activityInfo.applicationInfo.packageName) {
                    exists = true
                    break
                }
            }
            if (exists) continue
            app = AppData()
            app.pkgName = pack.activityInfo.applicationInfo.packageName
            app.actName = "-"
            app.enabled = pack.activityInfo.applicationInfo.enabled
            app.label = pack.activityInfo.applicationInfo.loadLabel(pm).toString()
            openWithAppsList?.add(app)
            if (includeDualApps) try {
                if (getPackageInfoAsUser?.invoke(pm, app.pkgName, 0, 999) != null) {
                    val appDual = AppData()
                    appDual.pkgName = pack.activityInfo.applicationInfo.packageName
                    appDual.actName = "-"
                    appDual.enabled = pack.activityInfo.applicationInfo.enabled
                    appDual.label = pack.activityInfo.applicationInfo.loadLabel(pm).toString()
                    appDual.user = 999
                    openWithAppsList?.add(appDual)
                }
            } catch (ignore: Throwable) {
                if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
}
        } catch (e: Throwable) {
            if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
            e.printStackTrace()
        }
        openWithAppsList?.sortWith { a, b -> (a.label ?: "").compareTo((b.label ?: ""), true) }
    }

    @JvmStatic
    @JvmOverloads
    fun getAppName(context: Context, pkgActName: String?, forcePkg: Boolean = false): CharSequence? {
        if (pkgActName == null) return null
        val pm = context.packageManager
        val notSelected = context.resources.getString(R.string.notselected)
        if (pkgActName == notSelected) return null
        val pkgActArray = splitPkgAct(pkgActName)

        return if (pkgActArray[1].isEmpty() || forcePkg) {
            if (pkgActArray[0].isEmpty()) return null
            try {
                val ai = pm.getApplicationInfo(pkgActArray[0], 0)
                pm.getApplicationLabel(ai)
            } catch (e: Throwable) {
                if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                e.printStackTrace()
                null
            }
        } else {
            try {
                pm.getActivityInfo(ComponentName(pkgActArray[0], pkgActArray[1]), 0).loadLabel(pm).toString()
            } catch (e: Throwable) {
                if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                e.printStackTrace()
                null
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun getAppIcon(context: Context, pkgActName: String?, forcePkg: Boolean = false): Drawable? {
        if (pkgActName == null) return null
        val pm = context.packageManager
        val notSelected = context.resources.getString(R.string.notselected)
        if (pkgActName == notSelected) return null
        val pkgActArray = splitPkgAct(pkgActName)

        return if (pkgActArray[1].isEmpty() || forcePkg) {
            if (pkgActArray[0].isEmpty()) return null
            try {
                pm.getApplicationIcon(pkgActArray[0])
            } catch (e: Throwable) {
                if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                e.printStackTrace()
                null
            }
        } else {
            try {
                pm.getActivityIcon(ComponentName(pkgActArray[0], pkgActArray[1]))
            } catch (e: Throwable) {
                if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 针对固定 "pkg|activity" 格式的直接解析。
     *
     * 仍创建 String[] 与 substring；仅用于把 UI 中已校验的 pkg|activity 字符串拆成两段。
     * 只在合法单分隔符格式下保证 pkg/activity 两段语义：
     *   - 无分隔符：返回 [trim(input), ""]
     *   - 前导分隔符：返回 ["", 第一段之后到下一个分隔符之前的内容]
     *   - 尾部分隔符：返回 [pkg.trim(), ""]
     *   - 多个分隔符：仅取前两个 "|" 之间的内容作为 activity
     *   - 两侧空格会被 trim
     */
    private fun splitPkgAct(pkgActName: String): Array<String> {
        val idx = pkgActName.indexOf('|')
        if (idx < 0) return arrayOf(pkgActName.trim(), "")
        val next = pkgActName.indexOf('|', idx + 1)
        val pkg = pkgActName.substring(0, idx).trim()
        val act = if (next < 0) pkgActName.substring(idx + 1).trim() else pkgActName.substring(idx + 1, next).trim()
        return arrayOf(pkg, act)
    }

    @JvmStatic
    fun getShortcutIcon(context: Context, key: String): Drawable? {
        val shortcutIconPath = context.filesDir.toString() + "/shortcuts/" + key + "_shortcut.png"
        val shortcutIconFile = File(shortcutIconPath)
        return if (shortcutIconFile.exists()) {
            BitmapDrawable(context.resources, BitmapFactory.decodeFile(shortcutIconFile.absolutePath))
        } else null
    }

    @JvmStatic
    fun getActionImageLocal(context: Context, key: String): Drawable? {
        return try {
            when (AppHelper.getIntOfAppPrefs(key + "_action", 1)) {
                8 -> getAppIcon(context, AppHelper.getStringOfAppPrefs(key + "_app", ""))
                9 -> getShortcutIcon(context, key)
                20 -> getAppIcon(context, AppHelper.getStringOfAppPrefs(key + "_activity", ""), true)
                else -> null
            }
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            null
        }
    }

    @JvmStatic
    fun parsePrefXml(context: Context, xmlResId: Int) {
        val res = context.resources
        var lastPrefSub: String? = null
        var lastPrefSubTitle: String? = null
        var lastPrefSubSubTitle: String? = null
        var catResId = 0
        var catPrefKey: ModData.ModCat? = null

        when (xmlResId) {
            R.xml.prefs_system -> {
                catResId = R.string.system_mods
                catPrefKey = ModData.ModCat.pref_key_system
            }
            R.xml.prefs_launcher -> {
                catResId = R.string.launcher_title
                catPrefKey = ModData.ModCat.pref_key_launcher
            }
            R.xml.prefs_controls -> {
                catResId = R.string.controls_mods
                catPrefKey = ModData.ModCat.pref_key_controls
            }
            R.xml.prefs_various -> {
                catResId = R.string.various_mods
                catPrefKey = ModData.ModCat.pref_key_various
            }
        }

        res.getXml(xmlResId).use { xml ->
            var eventType = xml.eventType
            var order = 0
            val prefCatExName = PreferenceCategoryEx::class.java.canonicalName
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.name != PreferenceScreen::class.java.simpleName) try {
                    if (xml.name == prefCatExName) {
                        if (xml.getAttributeValue(ANDROID_NS, "key") != null) {
                            lastPrefSub = xml.getAttributeValue(ANDROID_NS, "key")
                            lastPrefSubTitle = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"))
                            lastPrefSubSubTitle = null
                            order = 1
                        } else {
                            lastPrefSubSubTitle = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"))
                            order++
                        }
                        eventType = xml.next()
                        continue
                    }

                    val isChild = xml.getAttributeBooleanValue(MIUIZER_NS, "child", false)
                    if (!isChild) {
                        val modData = ModData()
                        modData.title = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"))
                        if (modData.title != null) {
                            modData.breadcrumbs = res.getString(catResId) +
                                (if (lastPrefSubTitle == null) "" else "/$lastPrefSubTitle" +
                                (if (lastPrefSubSubTitle == null) "" else "/$lastPrefSubSubTitle"))
                            modData.key = xml.getAttributeValue(ANDROID_NS, "key")
                            modData.cat = catPrefKey
                            modData.sub = lastPrefSub
                            modData.order = order
                            modData.prepareSearchKeys()
                            allModsList.add(modData)
                        }
                    }
                    order++
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    t.printStackTrace()
                }
                eventType = xml.next()
            }
        }
    }

    @JvmStatic
    fun getAllMods(context: Context?, force: Boolean) {
        if (context == null) return
        if (force) allModsList.clear()
        else if (allModsList.size > 0) return
        parsePrefXml(context, R.xml.prefs_system)
        parsePrefXml(context, R.xml.prefs_launcher)
        parsePrefXml(context, R.xml.prefs_controls)
        parsePrefXml(context, R.xml.prefs_various)
        allModsList.sortWith(MOD_DISPLAY_ORDER)
    }

    @JvmField
    val MOD_DISPLAY_ORDER = Comparator<ModData> { first, second ->
        val breadcrumbs =
            first.breadcrumbsSortKey.compareTo(second.breadcrumbsSortKey)
        if (breadcrumbs != 0) {
            breadcrumbs
        } else {
            first.titleSearchKey.compareTo(second.titleSearchKey)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun performLightVibration(context: Context?, ignoreOff: Boolean = false) {
        performVibration(context, false, ignoreOff)
    }

    @JvmStatic
    @JvmOverloads
    fun performStrongVibration(context: Context?, ignoreOff: Boolean = false) {
        performVibration(context, true, ignoreOff)
    }

    @JvmStatic
    fun performVibration(context: Context?, isStrong: Boolean, ignoreOff: Boolean) {
        if (context == null) return
        val mHapticFeedbackUtil = HapticFeedbackUtil(context, false)
        mHapticFeedbackUtil.performHapticFeedback(
            if (isStrong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY,
            ignoreOff
        )
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun performCustomVibration(context: Context, vibration: Int, ownPattern: String) {
        if (vibration == 0) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return

        if (vibration == 1) {
            vibrator.vibrate(200L)
            return
        }
        if (vibration == 2) {
            vibrator.vibrate(400L)
            return
        }

        val pattern = when (vibration) {
            3 -> longArrayOf(0, 250, 250, 250)
            4 -> longArrayOf(0, 250, 150, 125, 100, 125)
            5 -> longArrayOf(0, 150, 150, 100, 250, 150, 150, 100)
            6 -> longArrayOf(0, 100, 150, 100, 150, 100)
            7 -> {
                if (TextUtils.isEmpty(ownPattern)) return
                getVibrationPattern(ownPattern)
            }
            else -> return
        }

        try {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            vibrator.vibrate(200L)
        }
    }

    @JvmStatic
    fun getVibrationPattern(patternStr: String?): LongArray {
        return try {
            if (patternStr.isNullOrEmpty()) return LongArray(0)
            val sPattern = patternStr.split(",")
            val pattern = LongArray(sPattern.size)
            for (i in sPattern.indices) {
                pattern[i] = if (sPattern[i].isEmpty()) 0 else sPattern[i].toLong()
            }
            pattern
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            LongArray(0)
        }
    }

    @JvmStatic
    fun getCacheFilePath(filename: String): String? {
        return when {
            File("/cache").canWrite() -> "/cache/$filename"
            File("/data/cache").canWrite() -> "/data/cache/$filename"
            File("/data/tmp").canWrite() -> "/data/tmp/$filename"
            else -> null
        }
    }

    @JvmStatic
    fun copyToClipboard(context: Context?, text: String) {
        if (context == null) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val mClipData = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(mClipData)
    }

    @JvmStatic
    fun copyFile(from: String, to: String): Boolean {
        return try {
            Files.copy(Paths.get(from), Paths.get(to), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            t.printStackTrace()
            false
        }
    }

    @JvmStatic
    fun containsStringPair(hayStack: Set<String>?, needle: String?): Boolean {
        return PrefPair.containsFirst(hayStack, needle ?: "")
    }

    @JvmStatic
    fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap? {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return null

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int

        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum

        val dv = IntArray(256 * divsum) { it / divsum }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir = stack[0]
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (y in 0 until h) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0; rsum = 0; gsum = 0; bsum = 0
            for (i in -radius..radius) {
                p = pix[yi + max(0, min(wm, i))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (x in 0 until w) {
                if (rsum < dv.size) r[yi] = dv[rsum]
                if (gsum < dv.size) g[yi] = dv[gsum]
                if (bsum < dv.size) b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0; rsum = 0; gsum = 0; bsum = 0
            yp = -radius * w
            for (i in -radius..radius) {
                yi = max(0, yp) + x

                sir = stack[i + radius]

                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]

                rbs = r1 - abs(i)

                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs

                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }

                if (i < hm) {
                    yp += w
                }
            }
            yi = x
            stackpointer = radius
            for (y in 0 until h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    @JvmStatic
    fun constrain(amount: Int, low: Int, high: Int): Int {
        return if (amount < low) low else (if (amount > high) high else amount)
    }

    @JvmStatic
    fun constrain(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else (if (amount > high) high else amount)
    }

    @JvmStatic
    fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    @JvmStatic
    fun lerp(start: Int, stop: Int, amount: Float): Float {
        return lerp(start.toFloat(), stop.toFloat(), amount)
    }

    /**
     * Returns the interpolation scalar (s) that satisfies the equation: `value = lerp(a, b, s)`
     *
     * If `a == b`, then this function will return 0.
     */
    @JvmStatic
    fun lerpInv(a: Float, b: Float, value: Float): Float {
        return if (a != b) (value - a) / (b - a) else 0.0f
    }

    /** Returns the single argument constrained between [0.0, 1.0]. */
    @JvmStatic
    fun saturate(value: Float): Float {
        return constrain(value, 0.0f, 1.0f)
    }

    /** Returns the saturated (constrained between [0, 1]) result of `lerpInv`. */
    @JvmStatic
    fun lerpInvSat(a: Float, b: Float, value: Float): Float {
        return saturate(lerpInv(a, b, value))
    }

    @JvmStatic
    fun norm(start: Float, stop: Float, value: Float): Float {
        return (value - start) / (stop - start)
    }

    private fun sq(f: Float): Float {
        return f * f
    }

    @JvmStatic
    fun exp(f: Float): Float {
        return Math.exp(f.toDouble()).toFloat()
    }

    @JvmStatic
    fun convertGammaToLinearFloat(i: Float, max: Int, f: Float, f2: Float): Float {
        val norm = norm(0.0f, max.toFloat(), i)
        val R = 0.4f
        val A = 0.2146f
        val B = 0.2847f
        val C = 0.4719f
        return lerp(
            f,
            f2,
            constrain(
                if (norm <= R) sq(norm / R) else exp((norm - C) / A) + B,
                0.0f,
                12.0f
            ) / 12.0f
        )
    }
}
