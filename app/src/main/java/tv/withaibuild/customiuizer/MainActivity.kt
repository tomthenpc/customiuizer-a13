package tv.withaibuild.customiuizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.github.libxposed.service.XposedServiceHelper
import tv.withaibuild.customiuizer.mods.GlobalActions
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.Helpers

class MainActivity : AppCompatActivity() {

    private var mainFrag: MainFragment? = null
    private var prefsChanged: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ignoreKeys = hashSetOf(
            "pref_key_miuizer_locale",
            "pref_key_miuizer_locale_applied",
            "pref_key_miuizer_launchericon",
            "pref_key_miuizer_synced_from_lsposed"
        )
        AppHelper.setMirrorIgnoreKeys(ignoreKeys)

        if (AppHelper.remotePrefs == null) {
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: io.github.libxposed.service.XposedService) {
                    AppHelper.moduleConnectionObserved = true
                    AppHelper.moduleActive = true
                    AppHelper.remotePrefs = service.getRemotePreferences(AppHelper.prefsName + "_remote") as io.github.libxposed.service.RemotePreferences
                    AppHelper.reconcileRemotePreferences(AppHelper.remotePrefs)
                }

                override fun onServiceDied(service: io.github.libxposed.service.XposedService) {
                    AppHelper.moduleConnectionObserved = true
                    AppHelper.moduleActive = false
                    AppHelper.remotePrefs = null
                }
            })
        }

        val myToolbar = findViewById<Toolbar>(R.id.mainActionBar)
        setSupportActionBar(myToolbar)

        if (savedInstanceState != null) {
            mainFrag = supportFragmentManager.getFragment(savedInstanceState, "mainFrag") as? MainFragment
        } else if (mainFrag == null) {
            val newFrag = MainFragment()
            mainFrag = newFrag
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, newFrag)
                .commit()
        }

        prefsChanged = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            val value = if (key == null) null else AppHelper.appPrefs?.all?.get(key)
            AppHelper.onLocalPreferenceChanged(AppHelper.remotePrefs, key, value)
        }

        AppHelper.appPrefs?.registerOnSharedPreferenceChangeListener(prefsChanged)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        mainFrag?.let { supportFragmentManager.putFragment(savedInstanceState, "mainFrag", it) }
        super.onSaveInstanceState(savedInstanceState)
    }

    @SuppressLint("ApplySharedPref")
    override fun onDestroy() {
        try {
            prefsChanged?.let { AppHelper.appPrefs?.unregisterOnSharedPreferenceChangeListener(it) }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment == null) {
                    finish()
                } else if (fragment is MainFragment) {
                    finish()
                } else {
                    (fragment as? SubFragment)?.finish()
                }
                true
            }
            R.id.resetsettings -> {
                showResetSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_settings)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                AppHelper.appPrefs?.edit()?.clear()?.commit()
                AlertDialog.Builder(this)
                    .setTitle(R.string.reset_settings_done)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val pid = android.os.Process.myPid()
                        android.os.Process.killProcess(pid)
                    }
                    .show()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_MENU) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isEmpty()) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        when (requestCode) {
            Helpers.REQUEST_PERMISSIONS_WIFI -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        Toast.makeText(this, R.string.permission_scan, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, R.string.permission_permanent, Toast.LENGTH_LONG).show()
                    }
                }
            }
            Helpers.REQUEST_PERMISSIONS_BLUETOOTH -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                        Toast.makeText(this, R.string.permission_scan, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, R.string.permission_permanent, Toast.LENGTH_LONG).show()
                    }
                }
            }
            Helpers.REQUEST_PERMISSIONS_REPORT -> {
                Toast.makeText(this, ":(", Toast.LENGTH_SHORT).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
