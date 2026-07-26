package name.monwf.customiuizer.utils

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Configuration
import android.text.InputType
import android.util.Log
import android.util.Pair
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import io.github.libxposed.service.RemotePreferences
import name.monwf.customiuizer.R
import name.monwf.customiuizer.mods.GlobalActions
import java.util.Locale

@Suppress("NAME_SHADOWING")
object AppHelper {

    const val prefsName: String = "customiuizer_prefs"

    @JvmField
    var appPrefs: SharedPreferences? = null

    @JvmField
    var moduleActive: Boolean = false

    @JvmField
    var remotePrefs: RemotePreferences? = null

    private const val TAG = "LSPosed-Bridge"

    @JvmField
    var silentSync: Boolean = false

    @JvmField
    var RESTORED_FROM_BACKUP: String = "restored_from_backup"

    @JvmStatic
    fun log(line: String) {
        Log.i(TAG, "[CustoMIUIzer] $line")
    }

    @JvmStatic
    fun log(t: Throwable) {
        Log.e(TAG, "[CustoMIUIzer] ${Log.getStackTraceString(t)}")
    }

    @JvmStatic
    fun log(mod: String, line: String) {
        Log.i(TAG, "[CustoMIUIzer][$mod] $line")
    }

    @JvmStatic
    fun log(mod: String, t: Throwable) {
        Log.e(TAG, "[CustoMIUIzer][$mod] ${Log.getStackTraceString(t)}")
    }

    @JvmStatic
    fun getSharedPrefs(context: Context, protectedStorage: Boolean): SharedPreferences {
        var ctx = context
        if (protectedStorage) ctx = getProtectedContext(context)
        return ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    private fun normalizeKey(key: String): String =
        if (key.startsWith("pref_key_")) key else "pref_key_$key"

    @JvmStatic
    fun getIntOfAppPrefs(key: String, defValue: Int): Int {
        return appPrefs!!.getInt(normalizeKey(key), defValue)
    }

    @JvmStatic
    fun getStringOfAppPrefs(key: String, defValue: String?): String? {
        return appPrefs!!.getString(normalizeKey(key), defValue)
    }

    @JvmStatic
    fun getStringAsIntOfAppPrefs(key: String, defValue: Int): Int {
        val prefValue = getStringOfAppPrefs(key, null)
        return prefValue?.toIntOrNull() ?: defValue
    }

    @JvmStatic
    fun getStringSetOfAppPrefs(key: String, defValue: Set<String>?): Set<String>? {
        return appPrefs!!.getStringSet(normalizeKey(key), defValue)
    }

    @JvmStatic
    @JvmOverloads
    fun getBooleanOfAppPrefs(key: String, defValue: Boolean = false): Boolean {
        return appPrefs!!.getBoolean(normalizeKey(key), defValue)
    }

    @JvmStatic
    @Synchronized
    @Throws(Throwable::class)
    fun getLocaleContext(context: Context): Context {
        if (appPrefs != null) {
            val locale = getStringOfAppPrefs("pref_key_miuizer_locale", "auto")
            if (locale == "auto" || locale == "1") return context
            val config = context.resources.configuration
            config.setLocale(Locale.forLanguageTag(locale ?: "auto"))
            return context.createConfigurationContext(config)
        }
        return context
    }

    @JvmStatic
    @Synchronized
    fun getProtectedContext(context: Context): Context {
        return getProtectedContext(context, null)
    }

    @JvmStatic
    @Synchronized
    fun getProtectedContext(context: Context, config: Configuration?): Context {
        return try {
            val mContext = if (context.isDeviceProtectedStorage) context else context.createDeviceProtectedStorageContext()
            getLocaleContext(if (config == null) mContext else mContext.createConfigurationContext(config))
        } catch (t: Throwable) {
            context
        }
    }

    @JvmStatic
    @JvmOverloads
    fun showInputDialog(
        context: Context,
        key: String,
        titleRes: Int,
        summRes: Int,
        maxLines: Int,
        callback: Helpers.InputCallback,
        prefDefault: Boolean = true
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(titleRes)
        val input = EditText(context)
        input.setText(if (prefDefault) getStringOfAppPrefs(key, "") else key)

        if (maxLines > 1) {
            input.isSingleLine = false
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        } else {
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        }

        val container = LinearLayout(context)
        val horizPadding = context.resources.getDimensionPixelSize(R.dimen.preference_item_child_padding)
        container.setPadding(horizPadding, 0, horizPadding, 0)
        container.orientation = LinearLayout.VERTICAL
        if (summRes > 0) {
            val msg = TextView(context)
            msg.setText(summRes)
            msg.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            container.addView(msg)
        }
        container.addView(input)

        builder.setView(container)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            callback.onInputFinished(key, input.text.toString())
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    @JvmStatic
    fun addStringPair(hayStack: MutableSet<String>?, needle1: String, needle2: String) {
        hayStack?.add("$needle1|$needle2")
    }

    @JvmStatic
    fun removeStringPair(hayStack: MutableSet<String>?, needle: String) {
        hayStack ?: return
        val iterator = hayStack.iterator()
        while (iterator.hasNext()) {
            val pair = iterator.next()
            val needles = pair.split("\\|".toRegex(), 2)
            if (needles[0] == needle) {
                iterator.remove()
                return
            }
        }
    }

    @JvmStatic
    fun getActionNameLocal(context: Context, key: String): Pair<String, String>? {
        return try {
            val action = getIntOfAppPrefs(key + "_action", 1)
            val modRes = context.resources
            var pair: Pair<String, String>? = null
            val resId = GlobalActions.getActionResId(action)
            if (resId != 0) {
                pair = Pair(modRes.getString(resId), "")
            } else if (action == 8) {
                val appName = Helpers.getAppName(context, getStringOfAppPrefs(key + "_app", "") ?: "", true)?.toString() ?: ""
                pair = Pair(modRes.getString(R.string.array_global_actions_launch), appName)
            } else if (action == 9) {
                pair = Pair(modRes.getString(R.string.array_global_actions_shortcut), getStringOfAppPrefs(key + "_shortcut_name", "") ?: "")
            } else if (action == 10) {
                val what = getIntOfAppPrefs(key + "_toggle", 0)
                val toggle = modRes.getString(R.string.array_global_actions_toggle)
                val sub = when (what) {
                    1 -> modRes.getString(R.string.array_global_toggle_wifi)
                    2 -> modRes.getString(R.string.array_global_toggle_bt)
                    3 -> modRes.getString(R.string.array_global_toggle_gps)
                    4 -> modRes.getString(R.string.array_global_toggle_nfc)
                    5 -> modRes.getString(R.string.array_global_toggle_sound)
                    6 -> modRes.getString(R.string.array_global_toggle_brightness)
                    7 -> modRes.getString(R.string.array_global_toggle_rotation)
                    8 -> modRes.getString(R.string.array_global_toggle_torch)
                    9 -> modRes.getString(R.string.array_global_toggle_mobiledata)
                    else -> ""
                }
                if (!sub.isNullOrEmpty()) pair = Pair(toggle, sub)
            } else if (action == 20) {
                val pref = getStringOfAppPrefs(key + "_activity", "") ?: ""
                var name = Helpers.getAppName(context, pref)?.toString() ?: ""
                if (name.isEmpty()) name = Helpers.getAppName(context, pref, true)?.toString() ?: ""
                pair = Pair(modRes.getString(R.string.array_global_actions_activity), name)
            }
            pair
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun syncPrefsToAnother(
        entries: Map<String, *>?,
        prefs: SharedPreferences,
        clearType: Int,
        ignoreKeys: Set<String>?,
        commitAction: Boolean
    ) {
        if (entries.isNullOrEmpty()) return
        val prefEdit = prefs.edit()
        if (clearType == 1) {
            prefEdit.clear()
        } else if (clearType == 2) {
            for (key in prefs.all.keys) {
                if (!entries.containsKey(key)) {
                    prefEdit.remove(key)
                }
            }
        }

        for ((key, value) in entries) {
            if (ignoreKeys != null && ignoreKeys.contains(key)) continue
            when (value) {
                is Boolean -> prefEdit.putBoolean(key, value)
                is Float -> prefEdit.putFloat(key, value)
                is Int -> prefEdit.putInt(key, value)
                is Long -> prefEdit.putLong(key, value)
                is String -> prefEdit.putString(key, value)
                is Set<*> -> prefEdit.putStringSet(key, value as Set<String>)
            }
        }

        if (commitAction) {
            prefEdit.commit()
        } else {
            prefEdit.apply()
        }
    }
}
