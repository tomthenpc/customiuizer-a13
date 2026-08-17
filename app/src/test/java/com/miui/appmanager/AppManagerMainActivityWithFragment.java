package com.miui.appmanager;

/** Runtime subclass whose declared fragment field is discovered on first onCreate. */
public class AppManagerMainActivityWithFragment extends AppManagerMainActivity {
    public AppsManagerFragment mAppsFragment = new AppsManagerFragment();
}
