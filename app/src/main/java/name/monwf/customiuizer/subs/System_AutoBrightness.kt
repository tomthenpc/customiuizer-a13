package name.monwf.customiuizer.subs

import android.os.Bundle
import android.widget.SeekBar
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.prefs.SeekBarPreference

class System_AutoBrightness : SubFragment() {

    private var minBrightness: SeekBarPreference? = null
    private var maxBrightness: SeekBarPreference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        maxBrightness = findPreference("pref_key_system_autobrightness_max") as? SeekBarPreference
        minBrightness = findPreference("pref_key_system_autobrightness_min") as? SeekBarPreference
        minBrightness?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                if ((maxBrightness?.getValue() ?: 0) <= progress) {
                    maxBrightness?.setValue(progress + 1)
                }
                maxBrightness?.setMinValue(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
}
