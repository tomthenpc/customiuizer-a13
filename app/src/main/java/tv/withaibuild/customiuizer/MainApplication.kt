package tv.withaibuild.customiuizer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.AppLocaleController
import tv.withaibuild.customiuizer.utils.Helpers

class MainApplication : Application() {

    override fun attachBaseContext(base: Context) {
        Helpers.withinAppContext = true
        val sp: SharedPreferences = AppHelper.getSharedPrefs(base, false)
        AppHelper.appPrefs = sp
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        AppLocaleController.apply(AppHelper.appPrefs, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(
                NotificationChannel("customiuizer_default", getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            )
        }
        registerReceiver(
            packageChangeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            },
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Helpers.invalidateAppCaches()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Helpers.memoryCache.trimToSize(Helpers.memoryCache.maxSize() / 2)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Helpers.invalidateAppCaches()
    }

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Helpers.invalidateAppCaches()
        }
    }
}
