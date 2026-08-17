package com.miui.appmanager;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * App-info fragment fixture. Does not extend androidx Fragment because
 * {@code getActivity()} is final there; production uses callMethod("getActivity").
 */
public class AMAppInformationFragment {
    public PackageInfo mPackageInfo;
    public Activity host;
    public final List<String> addedKeys = new ArrayList<>();

    public void addPref(String key, String title, String value) {
        addedKeys.add(key);
    }

    public boolean onPreferenceTreeClick(Object preference) {
        return false;
    }

    public Activity getActivity() {
        return host;
    }

    public static PackageInfo packageInfo(String packageName, String sourceDir, String dataDir) {
        PackageInfo info = new PackageInfo();
        info.packageName = packageName;
        info.versionCode = 1;
        ApplicationInfo app = new ApplicationInfo();
        app.uid = 10000;
        app.targetSdkVersion = 33;
        app.sourceDir = sourceDir;
        app.dataDir = dataDir;
        info.applicationInfo = app;
        return info;
    }
}
