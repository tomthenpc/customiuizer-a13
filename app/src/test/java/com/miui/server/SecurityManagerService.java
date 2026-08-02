package com.miui.server;

import android.content.Intent;

/**
 * Test stub for SecurityManagerService used by catalog batch 11 tests.
 * Only declares the methods referenced by the appLockTimeout hook contract.
 */
public class SecurityManagerService {
    public void addAccessControlPassForUser(String packageName, int userId) {
        // no-op stub
    }

    public boolean checkAccessControlPassLocked(String packageName, Intent intent, int userId) {
        return false;
    }
}
