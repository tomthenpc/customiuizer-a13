package tv.withaibuild.customiuizer.mods

import android.app.FakeKeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Handler
import android.os.Looper

class FakeContext : ContextWrapper(null) {

    var keyguardManager: FakeKeyguardManager? = FakeKeyguardManager()
    val sentBroadcasts: MutableList<Intent> = mutableListOf()
    val registeredReceivers: MutableList<BroadcastReceiver> = mutableListOf()
    val startedActivities: MutableList<Intent> = mutableListOf()
    var fakePackageManager: PackageManager? = null

    private val looper: Looper by lazy {
        val ctor = Looper::class.java.getDeclaredConstructor()
        ctor.isAccessible = true
        ctor.newInstance()
    }

    override fun getMainLooper(): Looper = looper

    override fun getApplicationContext(): Context = this

    override fun getSystemService(name: String): Any? {
        if (Context.KEYGUARD_SERVICE == name) return keyguardManager
        return null
    }

    override fun getResources(): Resources = Resources.getSystem()

    override fun getPackageManager(): PackageManager = fakePackageManager ?: super.getPackageManager()

    override fun startActivity(intent: Intent) {
        startedActivities.add(intent)
    }

    override fun sendBroadcast(intent: Intent) {
        sentBroadcasts.add(intent)
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? {
        if (receiver != null) registeredReceivers.add(receiver)
        return null
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter, flags: Int): Intent? {
        if (receiver != null) registeredReceivers.add(receiver)
        return null
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter,
        broadcastPermission: String?,
        scheduler: Handler?
    ): Intent? {
        if (receiver != null) registeredReceivers.add(receiver)
        return null
    }
}
