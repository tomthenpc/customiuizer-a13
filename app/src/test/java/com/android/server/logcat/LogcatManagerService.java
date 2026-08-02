package com.android.server.logcat;

/**
 * Test stub for LogcatManagerService used by catalog batch 11 tests.
 * Only declares the methods referenced by the noAccessDeviceLogsRequest hook contract.
 */
public class LogcatManagerService {
    public void onLogAccessRequested(Object client) {
        // no-op stub
    }

    public void declineRequest(Object client) {
        // no-op stub
    }
}
