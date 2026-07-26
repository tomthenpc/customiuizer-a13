package name.monwf.customiuizer.subs

import android.os.Bundle
import androidx.preference.Preference
import name.monwf.customiuizer.SubFragment

import name.monwf.customiuizer.utils.AppHelper

class System_Visualizer : SubFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        findPreference<Preference>("pref_key_system_visualizer_colorval")?.isEnabled =
            AppHelper.getStringOfAppPrefs("pref_key_system_visualizer_color", "1") == "2"
        findPreference<Preference>("pref_key_system_visualizer_dyntime")?.isEnabled =
            AppHelper.getStringOfAppPrefs("pref_key_system_visualizer_color", "1") == "5"

        findPreference<Preference>("pref_key_system_visualizer_color")?.setOnPreferenceChangeListener { _, newValue ->
            findPreference<Preference>("pref_key_system_visualizer_colorval")?.isEnabled = newValue == "2"
            findPreference<Preference>("pref_key_system_visualizer_dyntime")?.isEnabled = newValue == "5"
            true
        }

        findPreference<Preference>("pref_key_system_visualizer_colorval")?.setOnPreferenceClickListener(openColorSelector)
    }
}
