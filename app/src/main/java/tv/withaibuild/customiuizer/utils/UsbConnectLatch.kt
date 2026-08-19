package tv.withaibuild.customiuizer.utils

/**
 * Rising-edge gate for USB_STATE `connected` extras on the existing
 * [system_defaultusb] hook. Disconnect clears the latch so a later plug
 * is a new apply opportunity in the same system_server lifetime.
 */
object UsbConnectLatch {

    @JvmStatic
    fun shouldAttemptApply(latchedConnected: Boolean, connectedNow: Boolean): Boolean {
        return connectedNow && !latchedConnected
    }

    /**
     * Disconnect always returns false. A rising-edge apply path commits true
     * only when [commitConnect] is true; early-exits leave the latch unchanged
     * so a later USB_STATE can retry.
     */
    @JvmStatic
    fun nextLatch(
        latchedConnected: Boolean,
        connectedNow: Boolean,
        commitConnect: Boolean,
    ): Boolean {
        if (!connectedNow) return false
        return if (commitConnect) true else latchedConnected
    }
}
