package name.monwf.customiuizer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.AppLocaleController
import name.monwf.customiuizer.utils.Helpers

class MainApplication : Application() {

    override fun attachBaseContext(base: Context) {
        Helpers.withinAppContext = true
        val sp: SharedPreferences = AppHelper.getSharedPrefs(base, false)
        AppHelper.appPrefs = sp
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        AppHelper.appPrefs?.let { AppLocaleController.applyLocale(AppLocaleController.getUserLocale(it)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(
                NotificationChannel("customiuizer_default", getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }
}
