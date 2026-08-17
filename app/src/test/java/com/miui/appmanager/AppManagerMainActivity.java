package com.miui.appmanager;

import android.app.Activity;
import android.os.Bundle;

/**
 * SecurityCenter App Manager host used by B2B-D2.
 *
 * Declared fields intentionally omit a Fragment so PACKAGE_READY cannot
 * false-succeed; nested onActivityCreated discovery waits for a runtime
 * subclass that actually holds a fragment field.
 */
public class AppManagerMainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {}
}
