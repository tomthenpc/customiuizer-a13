package tv.withaibuild.customiuizer.subs

import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.prefs.CheckBoxPreferenceEx
import tv.withaibuild.customiuizer.utils.Helpers

class Various : SubFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        findPreference<Preference>("pref_key_various_alarmcompat_apps")?.setOnPreferenceClickListener(openAppsEdit)

        findPreference<Preference>("pref_key_various_calluibright_cat")?.setOnPreferenceClickListener {
            openSubFragment(Various_CallUIBright(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.various_calluibright_title, R.xml.prefs_various_calluibright)
            true
        }

        findPreference<Preference>("pref_key_various_hiddenfeatures_cat")?.setOnPreferenceClickListener {
            openSubFragment(Various_HiddenFeatures(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.various_hiddenfeatures_title, R.xml.prefs_various_hiddenfeatures)
            true
        }

        val untrustedTouchPref = findPreference<Preference>("pref_key_various_allow_untrusted_touch") as? CheckBoxPreferenceEx
        untrustedTouchPref?.setOnPreferenceClickListener {
            Settings.Global.putInt(
                context?.contentResolver,
                "block_untrusted_touches",
                if (untrustedTouchPref.isChecked) 0 else 2
            )
            true
        }

        try {
            val act = requireActivity()
            val pkgInfo = act.packageManager.getApplicationInfo("com.miui.packageinstaller", PackageManager.MATCH_DISABLED_COMPONENTS)
            if (!pkgInfo.enabled) throw Throwable()
        } catch (e: Throwable) {
            val pref = findPreference<Preference>("pref_key_various_miuiinstaller") as? CheckBoxPreferenceEx
            pref?.isChecked = false
            pref?.setUnsupported(true)
            pref?.setSummary(R.string.various_miuiinstaller_error)
        }
    }
}
