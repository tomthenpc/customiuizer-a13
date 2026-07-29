package tv.withaibuild.customiuizer.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.Preference
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.prefs.ListPreferenceEx
import java.util.Locale

/**
 * 应用界面语言单一状态源。
 *
 * 唯一持久化状态是 [LOCALE_PREF_KEY] 的值；其余状态（AppCompat 应用 locale、
 * Context Configuration、[Locale.getDefault()]）均由该值与当前系统 locale 推导而来。
 */
object AppLocaleController {

    const val LOCALE_PREF_KEY = "pref_key_miuizer_locale"
    private const val LEGACY_AUTO = "1"
    private const val TAG = "AppLocaleController"

    // 显示顺序：auto，然后是支持的明确 locale。
    private val SUPPORTED_LOCALE_TAGS = listOf(
        "auto", "en", "zh-CN", "zh-TW", "ru-RU", "ja-JP", "vi-VN", "cs-CZ", "pt-BR", "tr-TR", "es-ES"
    )

    /**
     * 用于单测和非 Android 环境的缝。
     * 生产代码保持为 null，使用 [AppCompatDelegate.setApplicationLocales]。
     */
    @JvmField
    var applicationLocaleApplier: ((LocaleListCompat) -> Unit)? = null

    /**
     * 规范化用户选择或持久化值。
     *
     * 接受：
     * - null、空、未知值 -> auto
     * - 旧版 "1" -> auto
     * - 任何支持的 tag 原样返回
     */
    @JvmStatic
    fun normalizeLocaleTag(tag: String?): String = when {
        tag == null || tag.isBlank() -> "auto"
        tag == LEGACY_AUTO -> "auto"
        SUPPORTED_LOCALE_TAGS.contains(tag) -> tag
        tag.equals("auto", ignoreCase = true) -> "auto"
        else -> "auto"
    }

    /** 从 SharedPreferences 读取规范化后的用户选择。 */
    @JvmStatic
    fun getUserLocale(prefs: SharedPreferences?): String =
        normalizeLocaleTag(prefs?.getString(LOCALE_PREF_KEY, "auto"))

    /**
     * 同步写入新的用户选择并立即应用。
     *
     * 同步 [commit] 保证下一次 Activity/Fragment 重建时读取到同一值。
     * 语言切换是低频冷路径，阻塞 I/O 可接受。
     */
    @JvmStatic
    fun setUserLocale(prefs: SharedPreferences, tag: String): Boolean {
        val normalized = normalizeLocaleTag(tag)
        val written = prefs.edit().putString(LOCALE_PREF_KEY, normalized).commit()
        if (!written) {
            Log.e(TAG, "setUserLocale commit failed for tag: $normalized")
            return false
        }
        applyLocale(normalized)
        return true
    }

    /**
     * 仅应用已规范化的 tag，不写入。
     *
     * 用于应用启动时，持久化值已经是唯一状态源。
     */
    @JvmStatic
    fun applyLocale(tag: String) {
        val normalized = normalizeLocaleTag(tag)
        val effective = getEffectiveLocale(normalized) { getSystemLocale() }
        Locale.setDefault(effective)
        applyToAppCompat(normalized, effective)
    }

    /**
     * 根据用户 tag 计算实际生效的 locale。
     *
     * auto 解析为当前系统 locale。回调形式允许单元测试注入确定性的系统 locale。
     */
    @JvmStatic
    fun getEffectiveLocale(
        tag: String,
        systemLocaleProvider: () -> Locale = { getSystemLocale() }
    ): Locale = when (normalizeLocaleTag(tag)) {
        "auto" -> try {
            systemLocaleProvider()
        } catch (t: Throwable) {
            Log.w(TAG, "system locale provider failed: ${t.message}; falling back to JVM default")
            Locale.getDefault()
        }
        else -> try {
            Locale.forLanguageTag(tag)
        } catch (t: Throwable) {
            try {
                systemLocaleProvider()
            } catch (_: Throwable) {
                Locale.getDefault()
            }
        }
    }

    /** 映射用户 tag 到 AppCompat 期望的 [LocaleListCompat]。 */
    @JvmStatic
    fun toLocaleListCompat(tag: String): LocaleListCompat = try {
        val normalized = normalizeLocaleTag(tag)
        when (normalized) {
            "auto" -> LocaleListCompat.getEmptyLocaleList()
            else -> try {
                LocaleListCompat.forLanguageTags(normalized)
            } catch (t: Throwable) {
                LocaleListCompat.getEmptyLocaleList()
            }
        }
    } catch (t: Throwable) {
        Log.w(TAG, "toLocaleListCompat failed: ${t.message}")
        throw t
    }

    /** 当前系统主 locale。 */
    @JvmStatic
    fun getSystemLocale(): Locale = try {
        val sysLocales = Resources.getSystem().configuration.locales
        if (sysLocales.isEmpty) Locale.getDefault() else sysLocales[0]
    } catch (t: Throwable) {
        Log.w(TAG, "getSystemLocale failed: ${t.message}; falling back to JVM default")
        Locale.getDefault()
    }

    /**
     * 返回应用了用户选择语言的 [Context]。
     *
     * 这是针对非 AppCompat Context（如 device-protected storage Context）的兜底。
     * Activity 应依赖 [AppCompatDelegate.setApplicationLocales]。
     */
    @JvmStatic
    fun getLocaleContext(base: Context, prefs: SharedPreferences?): Context {
        val tag = getUserLocale(prefs)
        val locale = getEffectiveLocale(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * 构造语言选择器 [ListPreference] 需要的平行数组。
     *
     * 返回的 [Pair] 为 `(displayEntries, entryValues)`。
     */
    @JvmStatic
    fun buildLocaleDisplayData(context: Context): Pair<Array<CharSequence>, Array<String>> =
        buildLocaleDisplayData(context.getString(R.string.array_system_default))

    /**
     * 可测版本。只有 [systemDefaultLabel] 是 Android 依赖值，其余从 [java.util.Locale] 推导。
     */
    @JvmStatic
    fun buildLocaleDisplayData(systemDefaultLabel: String): Pair<Array<CharSequence>, Array<String>> {
        val displayNames = ArrayList<CharSequence>(SUPPORTED_LOCALE_TAGS.size)
        val values = ArrayList<String>(SUPPORTED_LOCALE_TAGS.size)
        for (tag in SUPPORTED_LOCALE_TAGS) {
            values.add(tag)
            displayNames.add(when (tag) {
                "auto" -> systemDefaultLabel
                "zh-TW" -> "繁體中文（台灣）"
                else -> buildLanguageDisplayName(tag)
            })
        }
        return Pair(displayNames.toTypedArray(), values.toTypedArray())
    }

    private fun buildLanguageDisplayName(tag: String): String {
        val loc = Locale.forLanguageTag(tag)
        val sb = StringBuilder(loc.getDisplayLanguage(loc))
        if (sb.isNotEmpty()) sb.setCharAt(0, Character.toUpperCase(sb[0]))
        if (tag == "pt-BR") sb.append(" (Brasil)")
        return sb.toString()
    }

    /**
     * 将语言 [ListPreferenceEx] 绑定到单一状态源。
     *
     * - 在恢复任何值之前先设置稳定的 [entries] 和 [entryValues]。
     * - 禁用 preference 自身持久化，只有 [setUserLocale] 写入。
     * - 规范化每次变更并同步应用。
     * - 若当前持久化值不在支持集合中，防御性回退到 auto。
     */
    @JvmStatic
    fun setupLocalePreference(pref: ListPreferenceEx?, prefs: SharedPreferences?) {
        if (pref == null || prefs == null) return

        val (displayEntries, entryValues) = try {
            buildLocaleDisplayData(pref.context)
        } catch (t: Throwable) {
            Log.e(TAG, "Cannot build locale display data", t)
            return
        }

        if (displayEntries.size != entryValues.size) {
            Log.e(TAG, "Locale display data mismatch: entries=${displayEntries.size}, values=${entryValues.size}")
            return
        }

        pref.entries = displayEntries
        pref.entryValues = entryValues
        pref.isPersistent = false

        val current = getUserLocale(prefs)
        pref.value = if (entryValues.contains(current)) current else "auto"

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val tag = normalizeLocaleTag(newValue as? String)
            if (!entryValues.contains(tag)) {
                Log.w(TAG, "Rejected unknown locale tag: $newValue")
                return@OnPreferenceChangeListener false
            }
            // 先更新 preference 显示，即使 locale 应用后立即重建 Activity，UI 也保持一致。
            pref.value = tag
            setUserLocale(prefs, tag)
        }
    }

    private fun applyToAppCompat(tag: String, effective: Locale) {
        try {
            val localeList = if (tag == "auto") {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                try {
                    LocaleListCompat.create(effective)
                } catch (t: Throwable) {
                    LocaleListCompat.getEmptyLocaleList()
                }
            }
            (applicationLocaleApplier ?: { AppCompatDelegate.setApplicationLocales(it) })(localeList)
        } catch (t: Throwable) {
            Log.w(TAG, "applyToAppCompat skipped: ${t.message}")
        }
    }
}
