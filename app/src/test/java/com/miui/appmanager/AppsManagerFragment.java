package com.miui.appmanager;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

/** Declares {@code onActivityCreated} so hookAllMethods can attach once. */
public class AppsManagerFragment extends Fragment {
    public static Throwable getContextFailure;

    public void onActivityCreated(Bundle savedInstanceState) {}

    @Override
    public Context getContext() {
        Throwable failure = getContextFailure;
        if (failure == null) {
            return super.getContext();
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        throw new RuntimeException(failure);
    }
}
