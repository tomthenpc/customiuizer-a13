package name.monwf.customiuizer.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import name.monwf.customiuizer.R

class UnlockSettings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tasker_unlock)

        val okButton = findViewById<Button>(R.id.force_ok)
        val optionGroup = findViewById<RadioGroup>(R.id.force_option)

        okButton.setOnClickListener {
            val opt = optionGroup.checkedRadioButtonId
            val value = when (opt) {
                R.id.force_locked -> 0
                R.id.force_unlocked -> 1
                else -> -1
            }

            val blurbRes = when (value) {
                1 -> R.string.system_noscreenlock_force_unlocked
                0 -> R.string.system_noscreenlock_force_locked
                else -> R.string.system_noscreenlock_force_off
            }

            val resultIntent = Intent().apply {
                putExtra(Constants.EXTRA_STRING_BLURB, getString(blurbRes))
                putExtra(Constants.EXTRA_BUNDLE, Bundle().apply {
                    putInt("system_noscreenlock_force", value)
                })
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        intent.getBundleExtra(Constants.EXTRA_BUNDLE)?.let { bundle ->
            val option = bundle.getInt("system_noscreenlock_force", -1)
            val checkId = when (option) {
                0 -> R.id.force_locked
                1 -> R.id.force_unlocked
                else -> R.id.force_off
            }
            optionGroup.check(checkId)
        }
    }
}
