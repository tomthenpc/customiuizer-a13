package com.miui.appmanager;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/** Present on the test classpath so D1 can hide it independently of later hooks. */
public final class AppManageUtils {
    private AppManageUtils() {}

    public static ApplicationInfo getAppInfo(
        Object ignored,
        PackageManager pm,
        String packageName,
        int flags,
        int userId
    ) {
        return null;
    }
}
