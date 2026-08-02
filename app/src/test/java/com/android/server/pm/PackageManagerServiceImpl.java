package com.android.server.pm;

/**
 * Test stub for PackageManagerServiceImpl used by catalog batch 11 tests.
 * Only declares the methods referenced by the appsDisableService hook contract.
 */
public class PackageManagerServiceImpl {
    public boolean canBeDisabled(String pkgName, int userId) {
        return false;
    }
}
