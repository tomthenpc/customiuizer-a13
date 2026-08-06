package tv.withaibuild.customiuizer.installers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiRestartGuardTest {

    @Test
    fun differenceUnderWindow_isWithinGuard() {
        assertTrue(SystemUiInstaller.isWithinSystemUiRestartGuard(1000L, 10999L))
    }

    @Test
    fun differenceJustUnderWindow_isWithinGuard() {
        assertTrue(SystemUiInstaller.isWithinSystemUiRestartGuard(0L, 9999L))
    }

    @Test
    fun differenceEqualToWindow_isNotWithinGuard() {
        assertFalse(SystemUiInstaller.isWithinSystemUiRestartGuard(0L, 10000L))
    }

    @Test
    fun differenceOverWindow_isNotWithinGuard() {
        assertFalse(SystemUiInstaller.isWithinSystemUiRestartGuard(0L, 20000L))
    }

    @Test
    fun zeroRestartTime_normalStartup_isNotWithinGuard() {
        assertFalse(SystemUiInstaller.isWithinSystemUiRestartGuard(0L, 30000L))
    }

    @Test
    fun restartTimeInFuture_maintainsExpressionSemantics() {
        // currentTime < restartTime, so currentTime - restartTime is negative,
        // which is less than 10000, therefore within guard.
        assertTrue(SystemUiInstaller.isWithinSystemUiRestartGuard(1000L, 500L))
    }
}
