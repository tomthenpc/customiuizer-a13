package tv.withaibuild.customiuizer.mods

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalActionsFastRebootTest {

    private class FailingPowerManager

    private class FakeRebootService {
        var rebooted = false
        var shouldThrow = false

        @Suppress("unused")
        fun reboot(confirm: Boolean, reason: String?, wait: Boolean) {
            if (shouldThrow) throw RuntimeException("reboot failed")
            rebooted = true
        }
    }

    private class FakePowerManager(mService: Any?) {
        @Suppress("unused")
        val mService: Any? = mService
    }

    @Test
    fun constantsAreDistinctAndUseResultFirstUser() {
        assertEquals(Activity.RESULT_FIRST_USER, GlobalActions.ACTION_UNHANDLED)
        assertEquals(Activity.RESULT_FIRST_USER + 1, GlobalActions.ACTION_HANDLED)
    }

    @Test
    fun orderedBroadcastWithSuccessfulRebootMarksHandled() {
        val service = FakeRebootService()
        val pm = FakePowerManager(service)
        var resultCode = 0

        GlobalActions.performFastReboot(pm, true) { resultCode = it }

        assertTrue(service.rebooted)
        assertEquals(GlobalActions.ACTION_HANDLED, resultCode)
    }

    @Test
    fun orderedBroadcastWithRebootExceptionMarksUnhandled() {
        val service = FakeRebootService().apply { shouldThrow = true }
        val pm = FakePowerManager(service)
        var resultCode = 0

        GlobalActions.performFastReboot(pm, true) { resultCode = it }

        assertEquals(GlobalActions.ACTION_UNHANDLED, resultCode)
    }

    @Test
    fun orderedBroadcastWithMissingServiceFieldMarksUnhandled() {
        var resultCode = 0

        GlobalActions.performFastReboot(FailingPowerManager(), true) { resultCode = it }

        assertEquals(GlobalActions.ACTION_UNHANDLED, resultCode)
    }

    @Test
    fun nonOrderedBroadcastDoesNotCrash() {
        val service = FakeRebootService()
        val pm = FakePowerManager(service)
        var resultCode = 0

        GlobalActions.performFastReboot(pm, false) { resultCode = it }

        assertTrue(service.rebooted)
        assertEquals(0, resultCode)
    }

    @Test
    fun fastRebootCallerStillSucceedsWithBroadcastReceiver() {
        // Smoke test: the public overload accepts a BroadcastReceiver without crashing.
        // Real ordered-broadcast semantics are tested via the internal overload above.
        val service = FakeRebootService()
        val pm = FakePowerManager(service)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {}
        }

        GlobalActions.performFastReboot(receiver, pm)

        assertTrue(service.rebooted)
    }
}
