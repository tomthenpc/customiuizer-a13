package name.monwf.customiuizer.subs

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import name.monwf.customiuizer.R
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.prefs.SeekBarPreference
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.Helpers

class Launcher : SubFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        selectSub()
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val onPrivacyAppClick = Preference.OnPreferenceClickListener {
            val act = activity as? AppCompatActivity ?: return@OnPreferenceClickListener false
            if (!Helpers.checkPermAndRequest(
                    act,
                    Helpers.ACCESS_SECURITY_CENTER,
                    Helpers.REQUEST_PERMISSIONS_SECURITY_CENTER
                )
            ) return@OnPreferenceClickListener false
            openPrivacyAppEdit(this@Launcher, 0)
            true
        }

        val onLaunchableClick = Preference.OnPreferenceClickListener { preference ->
            openLaunchableList(preference, this@Launcher, 0)
            true
        }

        val opt = AppHelper.getStringAsIntOfAppPrefs("pref_key_launcher_mods", 1)

        when (sub) {
            "pref_key_launcher_cat_folders" -> {
                val folderCols = findPreference<SeekBarPreference>("pref_key_launcher_folder_cols")
                findPreference<Preference>("pref_key_launcher_folderwidth")?.isEnabled =
                    AppHelper.getIntOfAppPrefs("pref_key_launcher_folder_cols", 1) > 1
                findPreference<Preference>("pref_key_launcher_folderspace")?.isEnabled =
                    AppHelper.getIntOfAppPrefs("pref_key_launcher_folder_cols", 1) > 3

                folderCols?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        findPreference<Preference>("pref_key_launcher_folderwidth")?.isEnabled =
                            seekBar.progress > 0
                        findPreference<Preference>("pref_key_launcher_folderspace")?.isEnabled =
                            seekBar.progress > 2
                    }
                })
            }
            "pref_key_launcher_cat_gestures" -> {
                findPreference<Preference>("pref_key_launcher_swipedown")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_swipedown2")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_swipeup")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_swipeup2")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_swiperight")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_swipeleft")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_shake")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_doubletap")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_pinch")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_spread")?.onPreferenceClickListener = openLauncherActions
                findPreference<Preference>("pref_key_launcher_swipeup")?.isEnabled = opt == 1
            }
            "pref_key_launcher_cat_privacyapps" -> {
                findPreference<Preference>("pref_key_launcher_cat_privacyapps")?.isEnabled = opt == 1
                findPreference<Preference>("pref_key_launcher_privacyapps_list")?.onPreferenceClickListener = onPrivacyAppClick
            }
            "pref_key_launcher_cat_titles" -> {
                findPreference<Preference>("pref_key_launcher_renameapps_list")?.onPreferenceClickListener = onLaunchableClick
            }
            "pref_key_launcher_cat_bugfixes" -> {
                findPreference<Preference>("pref_key_launcher_fixanim")?.isEnabled = opt == 1
            }
            "pref_key_launcher_cat_other" -> {
                findPreference<Preference>("pref_key_launcher_unlockgrids")?.isEnabled = opt == 1
                findPreference<Preference>("pref_key_launcher_hideseekpoints")?.isEnabled = opt == 1
                findPreference<Preference>("pref_key_launcher_nounlockanim")?.isEnabled = opt == 1
                findPreference<Preference>("pref_key_launcher_oldlaunchanim")?.isEnabled = opt == 1
                findPreference<Preference>("pref_key_launcher_closedrawer")?.isEnabled = opt == 1
            }
        }
    }
}
