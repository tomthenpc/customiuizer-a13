package name.monwf.customiuizer.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import name.monwf.customiuizer.mods.GlobalActions

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.getBundleExtra(Constants.EXTRA_BUNDLE) ?: return
        val sendIntent = Intent(GlobalActions.ACTION_PREFIX + "UnlockSetForced").apply {
            putExtras(bundle)
        }
        context.sendBroadcast(sendIntent)
    }
}
