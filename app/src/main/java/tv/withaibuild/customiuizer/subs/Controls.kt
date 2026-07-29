package tv.withaibuild.customiuizer.subs

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.utils.AppHelper

class Controls : SubFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        selectSub()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        when (sub) {
            "pref_key_controls_cat_power" -> {
                findPreference<Preference>("pref_key_controls_powerdt")?.onPreferenceClickListener = openLaunchActions
            }
            "pref_key_controls_cat_volume" -> {
                findPreference<Preference>("pref_key_controls_volumecursor_apps")?.onPreferenceClickListener = openAppsEdit
                findPreference<Preference>("pref_key_controls_mediaplayer_apps")?.onPreferenceClickListener = openAppsEdit
            }
            "pref_key_controls_cat_navbar" -> {
                findPreference<Preference>("pref_key_controls_backlong")?.onPreferenceClickListener = openNavbarActions
                findPreference<Preference>("pref_key_controls_homelong")?.onPreferenceClickListener = openNavbarActions
                findPreference<Preference>("pref_key_controls_menulong")?.onPreferenceClickListener = openNavbarActions

                findPreference<Preference>("pref_key_controls_navbarleft")?.onPreferenceClickListener = openNavbarActions
                findPreference<Preference>("pref_key_controls_navbarleftlong")?.onPreferenceClickListener = openNavbarActions
                findPreference<Preference>("pref_key_controls_navbarright")?.onPreferenceClickListener = openNavbarActions
                findPreference<Preference>("pref_key_controls_navbarrightlong")?.onPreferenceClickListener = openNavbarActions
            }
            "pref_key_controls_cat_fingerprint" -> {
                findPreference<Preference>("pref_key_controls_fingerprint1")?.onPreferenceClickListener = openControlsActions
                findPreference<Preference>("pref_key_controls_fingerprint2")?.onPreferenceClickListener = openControlsActions
                findPreference<Preference>("pref_key_controls_fingerprintlong")?.onPreferenceClickListener = openControlsActions

                findPreference<Preference>("pref_key_controls_fingerprintsuccess_ignore")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_controls_fingerprintsuccess", "1") != "1"

                findPreference<Preference>("pref_key_controls_fingerprintsuccess")?.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        findPreference<Preference>("pref_key_controls_fingerprintsuccess_ignore")?.isEnabled =
                            newValue != "1"
                        true
                    }

                findPreference<Preference>("pref_key_controls_fingerprint_accept")?.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        val other = AppHelper.getStringOfAppPrefs("pref_key_controls_fingerprint_reject", "1")
                        if (newValue == other) {
                            AppHelper.appPrefs?.edit()
                                ?.putString("pref_key_controls_fingerprint_reject", "1")
                                ?.apply()
                            val msg = getString(R.string.controls_fingerprint_conflict) + " " +
                                getString(R.string.controls_fingerprint_conflict_reset_reject)
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            activity?.recreate()
                        }
                        true
                    }

                findPreference<Preference>("pref_key_controls_fingerprint_reject")?.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        val other = AppHelper.getStringOfAppPrefs("pref_key_controls_fingerprint_accept", "1")
                        if (newValue == other) {
                            AppHelper.appPrefs?.edit()
                                ?.putString("pref_key_controls_fingerprint_accept", "1")
                                ?.apply()
                            val msg = getString(R.string.controls_fingerprint_conflict) + " " +
                                getString(R.string.controls_fingerprint_conflict_reset_accept)
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            activity?.recreate()
                        }
                        true
                    }
            }
            "pref_key_controls_cat_fsg" -> {
                findPreference<Preference>("pref_key_controls_fsg_horiz_apps")?.onPreferenceClickListener = openAppsEdit
                findPreference<Preference>("pref_key_controls_fsg_assist_left")?.onPreferenceClickListener = openNavbarActions
                findPreference<Preference>("pref_key_controls_fsg_assist_right")?.onPreferenceClickListener = openNavbarActions
                findPreference<Preference>("pref_key_controls_fsg_swipeandstop")?.onPreferenceClickListener = openNavbarActions

                val enableSwipeAndStop =
                    AppHelper.getIntOfAppPrefs("pref_key_controls_fsg_swipeandstop_action", 1) > 1
                findPreference<Preference>("pref_key_controls_fsg_swipeandstop_disablevibrate")?.isEnabled = enableSwipeAndStop
            }
        }
    }
}
