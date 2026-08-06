package android.app;

import android.content.Context;

/**
 * Minimal test stub for android.app.ActivityThread.
 *
 * Provides a fake current application so ModuleHelper.findContext() does not
 * return null during SystemUI installer integration tests.
 */
public final class ActivityThread {

    private static final Application sCurrentApplication = new Application();

    public static Application currentApplication() {
        return sCurrentApplication;
    }

    public static ActivityThread currentActivityThread() {
        return new ActivityThread();
    }

    public Context getSystemContext() {
        return currentApplication();
    }
}
