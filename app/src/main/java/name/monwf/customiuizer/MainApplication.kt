package name.monwf.customiuizer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.Helpers
import java.util.Locale

class MainApplication : Application() {

    override fun attachBaseContext(base: Context) {
        Helpers.withinAppContext = true
        val sp: SharedPreferences = AppHelper.getSharedPrefs(base, false)
        AppHelper.appPrefs = sp
        val locale = sp.getString("pref_key_miuizer_locale", "auto")
        if (locale != null && locale != "auto" && locale != "1") {
            Locale.setDefault(Locale.forLanguageTag(locale))
        }
        super.attachBaseContext(base)
    }
}
