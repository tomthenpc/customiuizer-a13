package name.monwf.customiuizer.subs

import android.os.Bundle
import androidx.preference.Preference
import name.monwf.customiuizer.SubFragment

class System_StatusbarControls : SubFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        findPreference<Preference>("pref_key_system_statusbarcontrols_dt")?.setOnPreferenceClickListener(openStatusbarActions)
        findPreference<Preference>("pref_key_system_statusbarcontrols_longpress")?.setOnPreferenceClickListener(openStatusbarActions)
    }
}
