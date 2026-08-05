package tv.withaibuild.customiuizer.subs

import android.app.Activity
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import miui.os.Build
import tv.withaibuild.customiuizer.CredentialsLauncher
import tv.withaibuild.customiuizer.PrefsProvider
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.prefs.CheckBoxPreferenceEx
import tv.withaibuild.customiuizer.prefs.ListPreferenceEx
import tv.withaibuild.customiuizer.prefs.PreferenceEx
import tv.withaibuild.customiuizer.prefs.SeekBarPreference
import tv.withaibuild.customiuizer.qs.AutoRotateService
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.Helpers
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
class System : SubFragment() {

    override fun onCreatePreferences(@Nullable savedInstanceState: Bundle?, @Nullable rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        selectSub()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (sub) {
            "pref_key_system_cat_recents" -> {
                toolbarMenu = true
                activeMenus = "launcher"
            }
            "pref_key_system_cat_statusbar",
            "pref_key_system_cat_lockscreen",
            "pref_key_system_cat_qs",
            "pref_key_system_cat_drawer" -> {
                toolbarMenu = true
                activeMenus = "systemui"
            }
            else -> toolbarMenu = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        when (sub) {
            "pref_key_system_cat_screen" -> {
                findPreference<Preference>("pref_key_system_orientationlock")?.setOnPreferenceChangeListener { _, newValue ->
                    val act = activity ?: return@setOnPreferenceChangeListener true
                    val pm = act.packageManager
                    val enabled = newValue as? Boolean ?: false
                    val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    pm.setComponentEnabledSetting(ComponentName(act, AutoRotateService::class.java), state, PackageManager.DONT_KILL_APP)
                    true
                }

                findPreference<Preference>("pref_key_system_autobrightness_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_AutoBrightness(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_autobrightness_title, R.xml.prefs_system_autobrightness)
                    true
                }
            }

            "pref_key_system_cat_audio" -> {
                findPreference<Preference>("pref_key_system_ignorecalls_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_visualizer_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_Visualizer(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_visualizer_title, R.xml.prefs_system_visualizer)
                    true
                }
            }

            "pref_key_system_cat_vibration" -> {
                findPreference<Preference>("pref_key_system_vibration_apps")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_system_vibration", "1") != "1"
                findPreference<Preference>("pref_key_system_vibration")?.setOnPreferenceChangeListener { _, newValue ->
                    findPreference<Preference>("pref_key_system_vibration_apps")?.isEnabled = newValue != "1"
                    true
                }
                findPreference<Preference>("pref_key_system_vibration_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_vibration_amp_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_VibrationAmp(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_vibration_amp_title, R.xml.prefs_system_vibration_amp)
                    true
                }
            }

            "pref_key_system_cat_toasts" -> {
                findPreference<Preference>("pref_key_system_blocktoasts_apps")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_system_blocktoasts", "1") != "1"
                findPreference<Preference>("pref_key_system_blocktoasts")?.setOnPreferenceChangeListener { _, newValue ->
                    findPreference<Preference>("pref_key_system_blocktoasts_apps")?.isEnabled = newValue != "1"
                    true
                }
                findPreference<Preference>("pref_key_system_blocktoasts_apps")?.setOnPreferenceClickListener(openAppsEdit)
            }

            "pref_key_system_cat_statusbar" -> {
                findPreference<Preference>("pref_key_system_statusbarcolor_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_detailednetspeed_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_detailednetspeed)
                    true
                }
                findPreference<Preference>("pref_key_system_statusbar_batterytempandcurrent_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle().apply { putBoolean("isDynamic", true) })
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_statusbar_batterytempandcurrent)
                    true
                }
                findPreference<Preference>("prefs_system_statusbar_showdevicetemperature_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle().apply { putBoolean("isDynamic", true) })
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_statusbar_showdevicetemperature)
                    true
                }
                findPreference<Preference>("pref_key_system_statusbar_batterystyle_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle())
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_statusbar_batterystyle)
                    true
                }
                findPreference<Preference>("pref_key_system_statusbar_mobile_signal_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle().apply { putBoolean("isDynamic", true) })
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_statusbar_mobilesignal)
                    true
                }
                findPreference<Preference>("pref_key_system_statusbaricons_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle().apply { putBoolean("isDynamic", true) })
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_hideicons)
                    true
                }
                findPreference<Preference>("pref_key_system_statusbaricons_atright_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle().apply { putBoolean("isDynamic", true) })
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_statusbar_righticons)
                    true
                }
                findPreference<Preference>("pref_key_system_statusbar_clocktweak_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle().apply { putBoolean("isDynamic", true) })
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_statusbar_clock)
                    true
                }
                findPreference<Preference>("pref_key_system_batteryindicator_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_BatteryIndicator(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_batteryindicator_title, R.xml.prefs_system_batteryindicator)
                    true
                }
                findPreference<Preference>("pref_key_system_statusbarcontrols_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_StatusbarControls(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_statusbarcontrols_title, R.xml.prefs_system_statusbarcontrols)
                    true
                }
            }

            "pref_key_system_cat_drawer" -> {
                findPreference<Preference>("pref_key_system_shortcut_app")?.setOnPreferenceClickListener {
                    openStandaloneApp(it, this, 0)
                    true
                }
                findPreference<Preference>("pref_key_system_clock_app")?.setOnPreferenceClickListener {
                    openStandaloneApp(it, this, 1)
                    true
                }
                findPreference<Preference>("pref_key_system_calendar_app")?.setOnPreferenceClickListener {
                    openStandaloneApp(it, this, 2)
                    true
                }
            }

            "pref_key_system_cat_notifications" -> {
                findPreference<Preference>("pref_key_system_expandnotifs_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_expandnotifs_apps")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_system_expandnotifs", "1") != "1"
                findPreference<Preference>("pref_key_system_expandnotifs")?.setOnPreferenceChangeListener { _, newValue ->
                    findPreference<Preference>("pref_key_system_expandnotifs_apps")?.isEnabled = newValue != "1"
                    true
                }
                findPreference<Preference>("pref_key_system_notify_openinfw_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_colorizenotifs_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_colorizenotifs_apps")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_system_colorizenotifs", "1") != "1"
                findPreference<Preference>("pref_key_system_colorizenotifs")?.setOnPreferenceChangeListener { _, newValue ->
                    findPreference<Preference>("pref_key_system_colorizenotifs_apps")?.isEnabled = newValue != "1"
                    true
                }
            }

            "pref_key_system_cat_qs" -> {
                if (Build.IS_INTERNATIONAL_BUILD) {
                    findPreference<Preference>("pref_key_system_cc_switch_qsandnotification")?.isVisible = false
                }
                findPreference<Preference>("pref_key_system_qshaptics_ignore")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_system_qshaptics", "1") != "1"
                findPreference<Preference>("pref_key_system_qshaptics")?.setOnPreferenceChangeListener { _, newValue ->
                    findPreference<Preference>("pref_key_system_qshaptics_ignore")?.isEnabled = newValue != "1"
                    true
                }
                findPreference<Preference>("pref_key_system_cc_clocktweak_cat")?.setOnPreferenceClickListener {
                    val preference = it
                    val args = Bundle().apply {
                        putBoolean("isStandalone", true)
                        putBundle("catInfo", Bundle().apply { putBoolean("isDynamic", true) })
                        putString("sub", preference.key)
                    }
                    openSubFragment(System(), args, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, preference.title.toString(), R.xml.prefs_system_controlcenter_clock)
                    true
                }

                findPreference<SeekBarPreference>("pref_key_system_qqsgridcolumns")?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        var value = progress
                        if (value < 3) value = 5
                        try {
                            activity?.contentResolver?.let { resolver ->
                                Settings.Secure.putInt(resolver, "sysui_qqs_count", value)
                            }
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                            t.printStackTrace()
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })
            }

            "pref_key_system_cat_recents" -> {
                findPreference<Preference>("pref_key_system_hidefromrecents_apps")?.setOnPreferenceClickListener(openAppsEdit)
            }

            "pref_key_system_cat_betterpopups" -> {
                findPreference<Preference>("pref_key_system_betterpopups_allowfloat_apps")?.setOnPreferenceClickListener(openAppsBWEdit)
                findPreference<Preference>("pref_key_system_expandheadups_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_expandheadups_apps")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_system_expandheadups", "1") != "1"
                findPreference<Preference>("pref_key_system_expandheadups")?.setOnPreferenceChangeListener { _, newValue ->
                    findPreference<Preference>("pref_key_system_expandheadups_apps")?.isEnabled = newValue != "1"
                    true
                }
            }

            "pref_key_system_cat_floatingwindows" -> {
                findPreference<Preference>("pref_key_system_fw_forcein_actionsend_apps")?.setOnPreferenceClickListener(openAppsEdit)
            }

            "pref_key_system_cat_applock" -> {
                findPreference<Preference>("pref_key_system_applock_list")?.setOnPreferenceClickListener {
                    openLockedAppEdit(this, 0)
                    true
                }
                findPreference<Preference>("pref_key_system_applock_skip_activities")?.setOnPreferenceClickListener(openActivitiesList)
            }

            "pref_key_system_cat_lockscreen" -> {
                findPreference<Preference>("pref_key_system_noscreenlock_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_NoScreenLock(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_noscreenlock_title, R.xml.prefs_system_noscreenlock)
                    true
                }
                findPreference<Preference>("pref_key_system_lockscreenshortcuts_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_LockScreenShortcuts(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_lockscreenshortcuts_title, R.xml.prefs_system_lockscreenshortcuts)
                    true
                }
                findPreference<Preference>("pref_key_system_albumartonlock_cat")?.setOnPreferenceClickListener {
                    openSubFragment(SubFragment(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_albumartonlock_title, R.xml.prefs_system_albumartonlock)
                    true
                }
                findPreference<Preference>("pref_key_system_charginginfo_cat")?.setOnPreferenceClickListener {
                    openSubFragment(SubFragment(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_charginginfo_title, R.xml.prefs_system_charginginfo)
                    true
                }
                findPreference<Preference>("pref_key_system_lsalarm_cat")?.setOnPreferenceClickListener {
                    openSubFragment(SubFragment(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_lsalarm_title, R.xml.prefs_system_alarmonlock)
                    true
                }
                findPreference<Preference>("pref_key_system_secureqs_cat")?.setOnPreferenceClickListener {
                    openSubFragment(SubFragment(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_secureqs_title, R.xml.prefs_system_secureqs)
                    true
                }

                findPreference<Preference>("pref_key_system_credentials")?.setOnPreferenceChangeListener { _, newValue ->
                    val act = activity ?: return@setOnPreferenceChangeListener true
                    val pm = act.packageManager
                    val enabled = newValue as? Boolean ?: false
                    val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    pm.setComponentEnabledSetting(ComponentName(act, CredentialsLauncher::class.java), state, PackageManager.DONT_KILL_APP)
                    true
                }

                findPreference<CheckBoxPreferenceEx>("pref_key_system_credentials")?.apply {
                    val act = activity
                    if (act != null) {
                        isChecked = act.packageManager.getComponentEnabledSetting(ComponentName(act, CredentialsLauncher::class.java)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    }
                }
                if (Helpers.isDeviceEncrypted(context)) {
                    findPreference<CheckBoxPreferenceEx>("pref_key_system_nopassword")?.apply {
                        isChecked = false
                        setUnsupported(true)
                    }
                }
            }

            "pref_key_system_cat_other" -> {
                findPreference<Preference>("pref_key_system_forceclose_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_nooverscroll_apps")?.setOnPreferenceClickListener(openAppsEdit)
                findPreference<Preference>("pref_key_system_cleanshare_apps")?.setOnPreferenceClickListener(openShareEdit)
                findPreference<Preference>("pref_key_system_cleanshare_test")?.setOnPreferenceClickListener {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "CustoMIUIzer is the best!")
                        type = "*/*"
                    }
                    context?.startActivity(Intent.createChooser(sendIntent, null))
                    true
                }
                findPreference<Preference>("pref_key_system_cleanopenwith_apps")?.setOnPreferenceClickListener(openOpenWithEdit)
                findPreference<Preference>("pref_key_system_cleanopenwith_test")?.setOnPreferenceClickListener {
                    val act = activity ?: return@setOnPreferenceClickListener true
                    val alert = AlertDialog.Builder(act)
                    alert.setTitle(R.string.system_cleanopenwith_testdata)
                    alert.setSingleChoiceItems(R.array.openwithtest, -1) { dialog: DialogInterface, which: Int ->
                        dialog.dismiss()
                        val viewIntent = Intent().apply {
                            action = Intent.ACTION_VIEW
                            val type = when (which) {
                                0 -> "image/*"
                                1 -> "audio/*"
                                2 -> "video/*"
                                3 -> "text/*"
                                4 -> "application/zip"
                                else -> "*/*"
                            }
                            setDataAndType(Uri.parse("content://${PrefsProvider.AUTHORITY}/test/$which"), type)
                        }
                        context?.startActivity(Intent.createChooser(viewIntent, null))
                    }
                    alert.setNeutralButton(android.R.string.cancel, null)
                    alert.show()
                    true
                }

                findPreference<Preference>("pref_key_system_screenshot_cat")?.setOnPreferenceClickListener {
                    openSubFragment(System_ScreenshotConfig(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_screenshot_title, R.xml.prefs_system_screenshot)
                    true
                }

                findPreference<PreferenceEx>("pref_key_system_airplanemodeconfig")?.apply {
                    val act = activity as? AppCompatActivity
                    setUnsupported(act == null || !Helpers.checkSettingsPerm(act))
                    setOnPreferenceClickListener {
                        openSubFragment(System_AirplaneModeConfig(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_airplanemodeconfig_title, R.xml.prefs_system_airplanemode)
                        true
                    }
                }

                AppHelper.appPrefs?.edit()?.putInt("pref_key_system_animationscale_window", (Helpers.getAnimationScale(0) * 10).roundToInt())?.apply()
                findPreference<SeekBarPreference>("pref_key_system_animationscale_window")?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        Helpers.setAnimationScale(0, seekBar.progress / 10f)
                    }
                })

                AppHelper.appPrefs?.edit()?.putInt("pref_key_system_animationscale_transition", (Helpers.getAnimationScale(1) * 10).roundToInt())?.apply()
                findPreference<SeekBarPreference>("pref_key_system_animationscale_transition")?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        Helpers.setAnimationScale(1, seekBar.progress / 10f)
                    }
                })

                AppHelper.appPrefs?.edit()?.putInt("pref_key_system_animationscale_animator", (Helpers.getAnimationScale(2) * 10).roundToInt())?.apply()
                findPreference<SeekBarPreference>("pref_key_system_animationscale_animator")?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        Helpers.setAnimationScale(2, seekBar.progress / 10f)
                    }
                })

                if (!checkAnimationPermission()) {
                    listOf(
                        "pref_key_system_animationscale_window",
                        "pref_key_system_animationscale_transition",
                        "pref_key_system_animationscale_animator"
                    ).forEach { key ->
                        findPreference<Preference>(key)?.apply {
                            isEnabled = false
                            setSummary(R.string.launcher_privacyapps_fail)
                        }
                    }
                }

                findPreference<Preference>("pref_key_system_defaultusb_unsecure")?.isEnabled =
                    AppHelper.getStringOfAppPrefs("pref_key_system_defaultusb", "none") != "none"
                findPreference<ListPreferenceEx>("pref_key_system_defaultusb")?.setOnPreferenceChangeListener { _, newValue ->
                    findPreference<Preference>("pref_key_system_defaultusb_unsecure")?.isEnabled = newValue != "none"
                    true
                }

                if (!checkUSBPermission()) {
                    findPreference<Preference>("pref_key_system_defaultusb")?.apply {
                        isEnabled = false
                        setSummary(R.string.launcher_privacyapps_fail)
                    }
                    findPreference<Preference>("pref_key_system_defaultusb_unsecure")?.isEnabled = false
                }
            }

            "pref_key_system_detailednetspeed_cat" -> {
                findPreference<Preference>("pref_key_system_detailednetspeed")?.setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as? Boolean ?: false
                    findPreference<SeekBarPreference>("pref_key_system_netspeed_fontsize")?.setValue(if (enabled) 16 else 13, true)
                    true
                }
            }

            "pref_key_system_statusbar_clocktweak_cat" -> {
                val pref = findPreference<PreferenceEx>("pref_key_system_statusbar_clock_customformat")
                val prefKey = pref?.key ?: return
                val format = AppHelper.getStringOfAppPrefs(prefKey, "") ?: ""
                pref.setCustomSummary(resources.getString(if (format.isEmpty()) R.string.value_is_empty else R.string.value_is_set))
                val formatSummId = R.string.system_clock_customformat_help_summ
                pref.setOnPreferenceClickListener {
                    val act = activity ?: return@setOnPreferenceClickListener true
                    AppHelper.showInputDialog(act, prefKey, R.string.system_clock_customformat_setting_title, formatSummId, 2, object : Helpers.InputCallback {
                        override fun onInputFinished(key: String, text: String) {
                            val output = if (text.isEmpty()) "" else {
                                val lines = text.split("\n")
                                if (lines.size > 2) lines[0] + "\n" + lines[1] else text
                            }
                            if (output.isEmpty()) {
                                AppHelper.appPrefs?.edit()?.remove(key)?.apply()
                            } else {
                                AppHelper.appPrefs?.edit()?.putString(key, output)?.apply()
                            }
                            pref.setCustomSummary(resources.getString(if (output.isEmpty()) R.string.value_is_empty else R.string.value_is_set))
                        }
                    })
                    true
                }

                findPreference<Preference>("pref_key_system_statusbar_clock_chip_startcolor")?.setOnPreferenceClickListener(openColorSelector)
                findPreference<Preference>("pref_key_system_statusbar_clock_chip_endcolor")?.setOnPreferenceClickListener(openColorSelector)
                findPreference<Preference>("pref_key_system_statusbar_clock_chip_textcolor")?.setOnPreferenceClickListener(openColorSelector)
            }

            else -> {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val key = when (requestCode) {
                0 -> "pref_key_system_shortcut_app"
                1 -> "pref_key_system_clock_app"
                2 -> "pref_key_system_calendar_app"
                else -> null
            }
            key?.let {
                val user = data?.getIntExtra("user", 0) ?: 0
                AppHelper.appPrefs?.edit()?.putString(it, data?.getStringExtra("app"))?.putInt("${it}_user", user)?.apply()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkUSBPermission(): Boolean {
        val act = activity ?: return false
        return act.packageManager.checkPermission("android.permission.MANAGE_USB", Helpers.modulePkg) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAnimationPermission(): Boolean {
        val act = activity ?: return false
        return act.packageManager.checkPermission("android.permission.SET_ANIMATION_SCALE", Helpers.modulePkg) == PackageManager.PERMISSION_GRANTED
    }
}
