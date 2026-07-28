package name.monwf.customiuizer.subs

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import name.monwf.customiuizer.MainFragment
import name.monwf.customiuizer.R
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.prefs.PreferenceEx
import name.monwf.customiuizer.utils.Helpers

class CategorySelector : SubFragment() {

    private var cat: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        cat = arguments?.getString("cat")
        when (cat) {
            "pref_key_system" -> {
                toolbarMenu = true
                activeMenus = "systemui"
            }
            "pref_key_launcher" -> {
                toolbarMenu = true
                activeMenus = "launcher"
            }
            else -> toolbarMenu = false
        }
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val screen = findPreference<PreferenceScreen>("pref_key_cat") ?: return
        val count = screen.preferenceCount
        for (i in 0 until count) {
            screen.getPreference(i).onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference ->
                    if (preference !is PreferenceEx) return@OnPreferenceClickListener false
                    val bundle = Bundle().apply { putString("sub", preference.key) }
                    val mainFragment = targetFragment as? MainFragment
                        ?: return@OnPreferenceClickListener false
                    when (cat) {
                        "pref_key_system" -> openSubFragment(
                            mainFragment.prefSystem,
                            bundle,
                            Helpers.SettingsType.Preference,
                            Helpers.ActionBarType.HomeUp,
                            R.string.system_mods,
                            R.xml.prefs_system
                        )
                        "pref_key_launcher" -> openSubFragment(
                            mainFragment.prefLauncher,
                            bundle,
                            Helpers.SettingsType.Preference,
                            Helpers.ActionBarType.HomeUp,
                            R.string.launcher_title,
                            R.xml.prefs_launcher
                        )
                        "pref_key_controls" -> openSubFragment(
                            mainFragment.prefControls,
                            bundle,
                            Helpers.SettingsType.Preference,
                            Helpers.ActionBarType.HomeUp,
                            R.string.controls_mods,
                            R.xml.prefs_controls
                        )
                    }
                    true
                }
        }
    }
}
