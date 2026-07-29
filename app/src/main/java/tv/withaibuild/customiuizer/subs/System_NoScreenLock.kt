package tv.withaibuild.customiuizer.subs

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.prefs.ListPreferenceEx
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.Helpers

class System_NoScreenLock : SubFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val noscreenlockValue = AppHelper.getStringOfAppPrefs("pref_key_system_noscreenlock", "1")
        findPreference<Preference>("pref_key_system_noscreenlock_wifi")?.isEnabled = noscreenlockValue == "3"
        findPreference<Preference>("pref_key_system_noscreenlock_bt")?.isEnabled = noscreenlockValue == "3"

        findPreference<Preference>("pref_key_system_noscreenlock")?.setOnPreferenceChangeListener { _, newValue ->
            findPreference<Preference>("pref_key_system_noscreenlock_wifi")?.isEnabled = newValue == "3"
            findPreference<Preference>("pref_key_system_noscreenlock_bt")?.isEnabled = newValue == "3"
            true
        }

        findPreference<Preference>("pref_key_system_noscreenlock_wifi")?.setOnPreferenceClickListener {
            val activity = requireActivity() as? AppCompatActivity ?: return@setOnPreferenceClickListener false
            if (!Helpers.checkPermAndRequest(activity, Manifest.permission.ACCESS_FINE_LOCATION, Helpers.REQUEST_PERMISSIONS_WIFI)) return@setOnPreferenceClickListener false
            openWifiNetworks()
            true
        }

        findPreference<Preference>("pref_key_system_noscreenlock_bt")?.setOnPreferenceClickListener {
            val activity = requireActivity() as? AppCompatActivity ?: return@setOnPreferenceClickListener false
            if (!Helpers.checkPermAndRequest(activity, Manifest.permission.BLUETOOTH_CONNECT, Helpers.REQUEST_PERMISSIONS_BLUETOOTH)) return@setOnPreferenceClickListener false
            openBtNetworks()
            true
        }

        if (Helpers.isDeviceEncrypted(context)) {
            val req = findPreference<Preference>("pref_key_system_noscreenlock_req") as? ListPreferenceEx
            req?.setValue("3")
            req?.isEnabled = false
        }
    }

    private fun openWifiNetworks() {
        val args = Bundle()
        args.putString("key", "pref_key_system_noscreenlock_wifi")
        openSubFragment(WiFiList(), args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.wifi_networks, R.layout.prefs_wifi_networks)
    }

    private fun openBtNetworks() {
        val args = Bundle()
        args.putString("key", "pref_key_system_noscreenlock_bt")
        openSubFragment(BTList(), args, Helpers.SettingsType.Edit, Helpers.ActionBarType.HomeUp, R.string.bt_devices, R.layout.prefs_bt_networks)
    }
}
