package name.monwf.customiuizer.subs

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.utils.AppHelper

class System_BatteryIndicator : SubFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val colorval = AppHelper.getStringOfAppPrefs("pref_key_system_batteryindicator_color", "1")
        findPreference<Preference>("pref_key_system_batteryindicator_colorval1")?.isEnabled = colorval != "3"
        findPreference<Preference>("pref_key_system_batteryindicator_colorval2")?.isEnabled = colorval != "3"
        findPreference<Preference>("pref_key_system_batteryindicator_colorval3")?.isEnabled = colorval != "3"
        findPreference<Preference>("pref_key_system_batteryindicator_colorval4")?.isEnabled = colorval != "3"

        findPreference<Preference>("pref_key_system_batteryindicator_color")?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue != "3"
            findPreference<Preference>("pref_key_system_batteryindicator_colorval1")?.isEnabled = enabled
            findPreference<Preference>("pref_key_system_batteryindicator_colorval2")?.isEnabled = enabled
            findPreference<Preference>("pref_key_system_batteryindicator_colorval3")?.isEnabled = enabled
            findPreference<Preference>("pref_key_system_batteryindicator_colorval4")?.isEnabled = enabled
            true
        }

        findPreference<Preference>("pref_key_system_batteryindicator_colorval1")?.setOnPreferenceClickListener(openColorSelector)
        findPreference<Preference>("pref_key_system_batteryindicator_colorval2")?.setOnPreferenceClickListener(openColorSelector)
        findPreference<Preference>("pref_key_system_batteryindicator_colorval3")?.setOnPreferenceClickListener(openColorSelector)
        findPreference<Preference>("pref_key_system_batteryindicator_colorval4")?.setOnPreferenceClickListener(openColorSelector)

        findPreference<Preference>("pref_key_system_batteryindicator_test")?.setOnPreferenceClickListener {
            activity?.sendBroadcast(Intent("name.monwf.customiuizer.mods.BatteryIndicatorTest"))
            true
        }
    }
}
