package name.monwf.customiuizer.subs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.prefs.ListPreferenceEx

class System_AirplaneModeConfig : SubFragment() {

    private var radios: MutableList<String> = mutableListOf()
    private var radiosToggle: MutableList<String> = mutableListOf()

    private val listener = Preference.OnPreferenceChangeListener { _, _ ->
        Handler(Looper.getMainLooper()).post { processValues() }
        true
    }

    private fun processValues() {
        radios.clear()
        radiosToggle.clear()
        val screen = preferenceScreen ?: return
        for (i in 0 until screen.preferenceCount) {
            val pref = screen.getPreference(i) as? ListPreferenceEx ?: continue
            val dev = when (pref.key) {
                "pref_key_system_airplanemodeconfig_cell" -> "cell"
                "pref_key_system_airplanemodeconfig_bt" -> "bluetooth"
                "pref_key_system_airplanemodeconfig_wifi" -> "wifi"
                "pref_key_system_airplanemodeconfig_nfc" -> "nfc"
                "pref_key_system_airplanemodeconfig_wimax" -> "wimax"
                else -> null
            } ?: continue
            val value = pref.value
            if (value == "1") {
                radios.add(dev)
                radiosToggle.add(dev)
            } else if (value == "2") {
                radios.add(dev)
            }
        }
        val resolver = activity?.contentResolver ?: return
        Settings.Global.putString(resolver, "airplane_mode_radios", TextUtils.join(",", radios))
        Settings.Global.putString(resolver, "airplane_mode_toggleable_radios", TextUtils.join(",", radiosToggle))
    }

    private fun setupPref(name: String, dev: String) {
        val pref = findPreference<Preference>(name) as? ListPreferenceEx ?: return
        when {
            radios.contains(dev) && radiosToggle.contains(dev) -> pref.setValue("1")
            radios.contains(dev) -> pref.setValue("2")
            else -> pref.setValue("0")
        }
        pref.setOnPreferenceChangeListener(listener)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val resolver = activity?.contentResolver
        radios = try {
            Settings.Global.getString(resolver, "airplane_mode_radios")?.split(",")?.toMutableList() ?: mutableListOf()
        } catch (t: Throwable) {
            mutableListOf()
        }
        radiosToggle = try {
            Settings.Global.getString(resolver, "airplane_mode_toggleable_radios")?.split(",")?.toMutableList() ?: mutableListOf()
        } catch (t: Throwable) {
            mutableListOf()
        }

        setupPref("pref_key_system_airplanemodeconfig_cell", "cell")
        setupPref("pref_key_system_airplanemodeconfig_bt", "bluetooth")
        setupPref("pref_key_system_airplanemodeconfig_wifi", "wifi")
        setupPref("pref_key_system_airplanemodeconfig_nfc", "nfc")
        setupPref("pref_key_system_airplanemodeconfig_wimax", "wimax")
        findPreference<Preference>("pref_key_system_airplanemodeconfig_note")?.isEnabled = false
    }
}
