package com.android.systemui;

import android.app.Application;

/**
 * Minimal test stub for com.android.systemui.SystemUIApplication.
 *
 * Allows SystemUiInstaller tests to verify that the onCreate hook is
 * registered without needing the real SystemUI class on the unit-test
 * classpath.
 */
public class SystemUIApplication extends Application {

    public void onCreate() {
        // no-op for tests
    }
}
