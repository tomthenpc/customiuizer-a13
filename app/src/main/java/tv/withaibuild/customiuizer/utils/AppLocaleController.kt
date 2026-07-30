package tv.withaibuild.customiuizer.utils

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.LocaleList
import android.os.Process
import android.util.Log
import androidx.core.os.LocaleListCompat
import androidx.preference.Preference
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.prefs.ListPreferenceEx
import java.util.Locale

object AppLocaleController {

    const val LOCALE_PREF_KEY = "pref_key_miuizer_locale"
    const val APPLIED_LOCALE_PREF_KEY = "pref_key_miuizer_locale_applied"

    private const val AUTO = "auto"
    private const val RECONCILE_MARKER = ""
    private const val LEGACY_AUTO = "1"
    private const val TAG = "AppLocaleController"

    private val SUPPORTED_LOCALE_TAGS = listOf(
        "auto",
        "en",
        "zh-CN",
        "zh-TW",
        "ru-RU",
        "ja-JP",
        "vi-VN",
        "cs-CZ",
        "pt-BR",
        "tr-TR",
        "es-ES"
    )

    @JvmField
    var applicationLocaleApplier: ((LocaleListCompat) -> Unit)? = null

    @JvmField
    var applicationLocaleProvider: (() -> LocaleListCompat)? = null

    @JvmStatic
    fun normalizeLocaleTag(tag: String?): String = when {
        tag == null || tag.isBlank() -> AUTO
        tag == LEGACY_AUTO -> AUTO
        SUPPORTED_LOCALE_TAGS.contains(tag) -> tag
        tag.equals(AUTO, ignoreCase = true) -> AUTO
        else -> AUTO
    }

    @JvmStatic
    fun getUserLocale(prefs: SharedPreferences?): String =
        normalizeLocaleTag(prefs?.getString(LOCALE_PREF_KEY, AUTO))

    @JvmStatic
    fun setUserLocale(prefs: SharedPreferences, tag: String): Boolean {
        val normalized = normalizeLocaleTag(tag)
        val written = prefs.edit().putString(LOCALE_PREF_KEY, normalized).commit()
        if (!written) Log.e(TAG, "setUserLocale commit failed for tag: $normalized")
        return written
    }

    /**
     * Reconciles the persisted choice with Android 13's per-application locale service.
     *
     * `auto` with no marker is the untouched default and returns before looking up a
     * service. An explicit tag writes `LocaleManager.applicationLocales`; returning to
     * `auto` writes an empty list and therefore hands locale ownership back to Android.
     */
    @JvmStatic
    @JvmOverloads
    fun apply(prefs: SharedPreferences?, context: Context? = null): Boolean {
        val tag = getUserLocale(prefs)
        if (tag == AUTO && !hasAppliedLocale(prefs)) return false

        val target = toLocaleListCompat(tag)
        val current = getCurrentApplicationLocales(context)
        if (localeListsEqual(current, target)) {
            syncAppliedMarker(prefs, tag)
            return false
        }

        val applier = applicationLocaleApplier
        val applied = when {
            applier != null -> {
                applier(target)
                true
            }
            context != null -> setFrameworkApplicationLocales(context, target)
            else -> {
                Log.e(TAG, "apply() without Context cannot update LocaleManager")
                false
            }
        }
        if (applied) syncAppliedMarker(prefs, tag)
        return applied
    }

    @JvmStatic
    fun invalidateFastPath(prefs: SharedPreferences?) {
        prefs?.edit()?.putString(APPLIED_LOCALE_PREF_KEY, RECONCILE_MARKER)?.apply()
    }

    private fun hasAppliedLocale(prefs: SharedPreferences?): Boolean =
        prefs?.getString(APPLIED_LOCALE_PREF_KEY, null) != null

    private fun syncAppliedMarker(prefs: SharedPreferences?, tag: String) {
        prefs ?: return
        val stored = prefs.getString(APPLIED_LOCALE_PREF_KEY, null)
        if (tag == AUTO) {
            if (stored != null) prefs.edit().remove(APPLIED_LOCALE_PREF_KEY).apply()
        } else if (stored != tag) {
            prefs.edit().putString(APPLIED_LOCALE_PREF_KEY, tag).apply()
        }
    }

    private fun setFrameworkApplicationLocales(
        context: Context,
        locales: LocaleListCompat
    ): Boolean {
        val manager = context.getSystemService(LocaleManager::class.java)
        if (manager == null) {
            Log.e(TAG, "LocaleManager unavailable; locale not applied")
            return false
        }
        return try {
            manager.applicationLocales = LocaleList.forLanguageTags(locales.toLanguageTags())
            true
        } catch (t: Throwable) {
            Log.e(TAG, "LocaleManager rejected the locale list", t)
            false
        }
    }

    private fun getCurrentApplicationLocales(context: Context?): LocaleListCompat = try {
        val provider = applicationLocaleProvider
        when {
            provider != null -> provider()
            context != null -> {
                val manager = context.getSystemService(LocaleManager::class.java)
                if (manager == null) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.wrap(manager.applicationLocales)
            }
            else -> LocaleListCompat.getEmptyLocaleList()
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Unable to read application locales", t)
        LocaleListCompat.getEmptyLocaleList()
    }

    private fun localeListsEqual(first: LocaleListCompat, second: LocaleListCompat): Boolean =
        first.toLanguageTags() == second.toLanguageTags()

    @JvmStatic
    fun toLocaleListCompat(tag: String): LocaleListCompat = try {
        when (val normalized = normalizeLocaleTag(tag)) {
            AUTO -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(normalized)
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Unable to build application locale list", t)
        LocaleListCompat.getEmptyLocaleList()
    }

    @JvmStatic
    fun getEffectiveLocale(
        tag: String,
        systemLocaleProvider: () -> Locale = { getSystemLocale() }
    ): Locale = when (normalizeLocaleTag(tag)) {
        AUTO -> systemLocaleProvider()
        else -> Locale.forLanguageTag(normalizeLocaleTag(tag))
    }

    @JvmStatic
    fun getSystemLocale(): Locale {
        val locales = Resources.getSystem().configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }

    /**
     * Compatibility boundary for older callers. Framework application locales already
     * flow into application contexts, so no Configuration override is created here.
     */
    @JvmStatic
    fun getLocaleContext(base: Context, prefs: SharedPreferences?): Context = base

    @JvmStatic
    fun buildLocaleDisplayData(context: Context): Pair<Array<CharSequence>, Array<String>> =
        buildLocaleDisplayData(context.getString(R.string.array_system_default))

    @JvmStatic
    fun buildLocaleDisplayData(systemDefaultLabel: String): Pair<Array<CharSequence>, Array<String>> {
        val displayNames = ArrayList<CharSequence>(SUPPORTED_LOCALE_TAGS.size)
        val values = ArrayList<String>(SUPPORTED_LOCALE_TAGS.size)
        for (tag in SUPPORTED_LOCALE_TAGS) {
            values.add(tag)
            displayNames.add(
                when (tag) {
                    AUTO -> systemDefaultLabel
                    "zh-TW" -> "繁體中文（台灣）"
                    else -> buildLanguageDisplayName(tag)
                }
            )
        }
        return Pair(displayNames.toTypedArray(), values.toTypedArray())
    }

    private fun buildLanguageDisplayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        val result = StringBuilder(locale.getDisplayLanguage(locale))
        if (result.isNotEmpty()) result.setCharAt(0, Character.toUpperCase(result[0]))
        if (tag == "pt-BR") result.append(" (Brasil)")
        return result.toString()
    }

    @JvmStatic
    fun setupLocalePreference(pref: ListPreferenceEx?, prefs: SharedPreferences?) {
        if (pref == null) return
        pref.isPersistent = false
        if (prefs == null) {
            pref.isEnabled = false
            return
        }

        val (entries, values) = buildLocaleDisplayData(pref.context)
        pref.entries = entries
        pref.entryValues = values
        val current = getUserLocale(prefs)
        pref.value = if (values.contains(current)) current else AUTO

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val tag = normalizeLocaleTag(newValue as? String)
            if (!values.contains(tag)) return@OnPreferenceChangeListener false
            if (tag == getUserLocale(prefs)) return@OnPreferenceChangeListener true
            if (!setUserLocale(prefs, tag)) return@OnPreferenceChangeListener false
            pref.value = tag
            findActivity(pref.context)?.let { exitApplicationAfterLocaleSave(it) }
            false
        }
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            val base = current.baseContext
            if (base === current) return null
            current = base
        }
        return current as? Activity
    }

    @JvmStatic
    fun exitApplicationAfterLocaleSave(activity: Activity) {
        activity.finishAffinity()
        Process.killProcess(Process.myPid())
    }
}
