package name.monwf.customiuizer

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AboutFragment : SubFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headLayoutId = R.layout.fragment_about_head
        tailLayoutId = R.layout.fragment_about_tail
    }

    override fun fixStubLayout(view: View, postion: Int) {
        if (postion == 2) {
            val lp = view.layoutParams as RelativeLayout.LayoutParams
            lp.addRule(RelativeLayout.BELOW, android.R.id.list_container)
            view.layoutParams = lp
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view?.let { v ->
            try {
                val version = v.findViewById<TextView>(R.id.about_version)
                var versionName = BuildConfig.VERSION_NAME
                if (BuildConfig.BUILD_TYPE == "develop") {
                    val formatter = SimpleDateFormat("yy.MM.dd", Locale.getDefault())
                    versionName = formatter.format(Date(BuildConfig.BUILD_TIME)) + "-test"
                }
                version.text = getString(R.string.about_version, versionName)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        view?.findViewById<View>(R.id.miuizer_icon)?.visibility =
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) View.GONE else View.VISIBLE
        super.onConfigurationChanged(newConfig)
    }
}
