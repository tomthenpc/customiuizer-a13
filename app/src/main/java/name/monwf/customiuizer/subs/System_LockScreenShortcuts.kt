package name.monwf.customiuizer.subs

import android.os.Bundle
import androidx.preference.Preference
import name.monwf.customiuizer.SubFragment

class System_LockScreenShortcuts : SubFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        findPreference<Preference>("pref_key_system_lockscreenshortcuts_right")?.setOnPreferenceClickListener(openLockScreenActions)
    }
}
