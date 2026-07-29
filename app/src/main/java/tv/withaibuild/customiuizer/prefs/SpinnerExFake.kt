package tv.withaibuild.customiuizer.prefs

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Pair
import tv.withaibuild.customiuizer.utils.AppHelper

class SpinnerExFake(context: Context, attrs: AttributeSet) : SpinnerEx(context, attrs) {

    private var value: String? = null
    private val others = ArrayList<Pair<String, String>>()

    init {
        others.clear()
    }

    fun setValue(`val`: String) {
        value = `val`
    }

    fun getValue(): String? = value

    fun addValue(key: String, `val`: String?) {
        var sVal = `val`
        if (sVal == null) sVal = AppHelper.getStringOfAppPrefs(key, null)
        if (sVal != null) others.add(Pair(key, sVal))
    }

    fun addValue(key: String, `val`: Intent?) {
        val sVal = `val`?.toUri(0) ?: AppHelper.getStringOfAppPrefs(key, null)
        if (sVal != null) others.add(Pair(key, sVal))
    }

    fun applyOthers() {
        if (others.isEmpty()) return
        val editor = AppHelper.appPrefs?.edit() ?: return
        for (pref in others) {
            editor.putString(pref.first, pref.second)
        }
        editor.apply()
    }
}
