package tv.withaibuild.customiuizer.prefs

import android.content.SharedPreferences
import tv.withaibuild.customiuizer.mods.utils.ProcessScopes

/**
 * A single, reusable predicate for the preference-load decision.
 *
 * Each predicate is stateless and reads only the provided [SharedPreferences].
 * No Android owner, no lifecycle side effects, no coroutines.
 */
fun interface PreferenceLoadPredicate {
    fun shouldLoad(
        prefs: SharedPreferences,
        packageName: String
    ): Boolean
}

/**
 * A named rule carrying the preference keys it depends on and the predicate
 * that decides whether the rule is active for a given package.
 */
internal data class PreferenceLoadRule(
    val id: String,
    val preferenceKeys: Set<String>,
    val predicate: PreferenceLoadPredicate
)

/**
 * Central registry for the legacy "do we need to load prefs for this package?"
 * decision that used to live in [tv.withaibuild.customiuizer.MainModule].
 *
 * Known packages always load. Otherwise each rule is tested; if any rule says
 * the package is selected and its master switch is on, prefs are loaded.
 *
 * This object intentionally does **not** integrate with [FeatureCatalog] or
 * [PreferenceSchema]; those four legacy features do not yet own real
 * [tv.withaibuild.customiuizer.mods.catalog.FeatureSpec] entries and will be
 * migrated in a separate phase.
 */
object PreferenceLoadRegistry {

    /**
     * Stable, ordered list of all legacy preference-load rules.
     * Initialized once and reused; no runtime allocation per call.
     */
    internal val rules: List<PreferenceLoadRule> = listOf(
        PreferenceLoadRule(
            id = "various_alarmcompat",
            preferenceKeys = setOf(
                "pref_key_various_alarmcompat",
                "pref_key_various_alarmcompat_apps"
            ),
            predicate = PreferenceLoadPredicate { prefs, packageName ->
                prefs.getBoolean("pref_key_various_alarmcompat", false)
                    && prefs.getStringSet("pref_key_various_alarmcompat_apps", null)
                        ?.contains(packageName) == true
            }
        ),
        PreferenceLoadRule(
            id = "system_statusbarcolor",
            preferenceKeys = setOf(
                "pref_key_system_statusbarcolor",
                "pref_key_system_statusbarcolor_apps"
            ),
            predicate = PreferenceLoadPredicate { prefs, packageName ->
                prefs.getBoolean("pref_key_system_statusbarcolor", false)
                    && prefs.getStringSet("pref_key_system_statusbarcolor_apps", null)
                        ?.contains(packageName) == true
            }
        ),
        PreferenceLoadRule(
            id = "system_nooverscroll",
            preferenceKeys = setOf(
                "pref_key_system_nooverscroll",
                "pref_key_system_nooverscroll_apps"
            ),
            predicate = PreferenceLoadPredicate { prefs, packageName ->
                prefs.getBoolean("pref_key_system_nooverscroll", false)
                    && prefs.getStringSet("pref_key_system_nooverscroll_apps", null)
                        ?.contains(packageName) == true
            }
        ),
        PreferenceLoadRule(
            id = "controls_volumemedia",
            preferenceKeys = setOf(
                "pref_key_controls_volumemedia_up",
                "pref_key_controls_volumemedia_down",
                "pref_key_controls_mediaplayer_apps"
            ),
            predicate = PreferenceLoadPredicate { prefs, packageName ->
                val mediaEnabled = isVolumeMediaEnabled(prefs)
                val selected = prefs.getStringSet("pref_key_controls_mediaplayer_apps", null)
                    ?.contains(packageName) == true
                mediaEnabled && selected
            }
        )
    )

    /**
     * Returns true when `packageName` is a known module target or when any
     * legacy feature has selected it and enabled its master switch.
     */
    @JvmStatic
    fun shouldLoad(
        prefs: SharedPreferences,
        packageName: String
    ): Boolean {
        if (ProcessScopes.isKnownPackage(packageName)) return true
        return rules.any { it.predicate.shouldLoad(prefs, packageName) }
    }

    /**
     * Preserves the exact Java `||` short-circuit and exception behaviour of
     * the original [tv.withaibuild.customiuizer.MainModule.isVolumeMediaEnabled].
     *
     * The `try/catch` wraps the whole `(up > 0) || (down > 0)` expression, so:
     * - `up = "1"`, `down = "bad"` -> true (right side never evaluated).
     * - `up = "0"`, `down = "bad"` -> false (right side throws, caught).
     * - `up = "bad"`, `down = "1"` -> false (left side throws, caught).
     */
    private fun isVolumeMediaEnabled(prefs: SharedPreferences): Boolean {
        val up = prefs.getString("pref_key_controls_volumemedia_up", "0")
        val down = prefs.getString("pref_key_controls_volumemedia_down", "0")
        return try {
            (up != null && up.toInt() > 0)
                || (down != null && down.toInt() > 0)
        } catch (e: NumberFormatException) {
            false
        }
    }
}
