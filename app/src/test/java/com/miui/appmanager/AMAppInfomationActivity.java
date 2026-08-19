package com.miui.appmanager;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.StubFragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import tv.withaibuild.customiuizer.mods.FakeContext;

/** SecurityCenter app-info host used by B2B-D3. */
public class AMAppInfomationActivity extends Activity {
    public final List<Intent> started = new ArrayList<>();
    public AMAppInformationFragment contentFragment;
    private final FakeContext resourcesHost = new FakeContext();

    public void onCreate(Bundle savedInstanceState) {}

    @Override
    public FragmentManager getFragmentManager() {
        return new StubFragmentManager();
    }

    @Override
    public void startActivity(Intent intent) {
        started.add(intent);
    }

    @Override
    public Resources getResources() {
        return resourcesHost.getResources();
    }

    @Override
    public Context createPackageContext(String packageName, int flags) {
        return resourcesHost;
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        return resourcesHost;
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return resourcesHost;
    }

    @Override
    public String getPackageName() {
        return "com.miui.securitycenter";
    }
}
