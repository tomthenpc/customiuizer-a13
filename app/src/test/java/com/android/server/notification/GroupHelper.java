package com.android.server.notification;

import java.util.List;

/**
 * Test stub for GroupHelper used by catalog batch 11 tests.
 * Only declares the methods referenced by the autoGroupNotifications hook contract.
 */
public class GroupHelper {
    public void adjustAutogroupingSummary(int userId, String packageName, String key, boolean summary) {
        // no-op stub
    }

    public void adjustNotificationBundling(List<?> keys, boolean summary) {
        // no-op stub
    }
}
