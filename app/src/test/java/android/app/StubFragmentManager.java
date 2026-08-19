package android.app;

/** Test stub: findFragmentById returns null so AppInfo falls back to mSupportFragment. */
public class StubFragmentManager extends FragmentManager {
    @Override
    public Fragment findFragmentById(int id) {
        return null;
    }

    @Override
    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {}

    @Override
    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {}

    @Override
    public Fragment findFragmentByTag(String tag) {
        return null;
    }

    @Override
    public boolean executePendingTransactions() {
        return false;
    }

    @Override
    public FragmentTransaction beginTransaction() {
        return null;
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    @Override
    public void popBackStack() {}

    @Override
    public void popBackStack(String name, int flags) {}

    @Override
    public void popBackStack(int id, int flags) {}

    @Override
    public boolean popBackStackImmediate() {
        return false;
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags) {
        return false;
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags) {
        return false;
    }

    @Override
    public int getBackStackEntryCount() {
        return 0;
    }

    @Override
    public BackStackEntry getBackStackEntryAt(int index) {
        return null;
    }

    @Override
    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {}

    @Override
    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {}

    @Override
    public void putFragment(android.os.Bundle bundle, String key, Fragment fragment) {}

    @Override
    public Fragment getFragment(android.os.Bundle bundle, String key) {
        return null;
    }

    @Override
    public java.util.List<Fragment> getFragments() {
        return java.util.Collections.emptyList();
    }

    @Override
    public Fragment.SavedState saveFragmentInstanceState(Fragment f) {
        return null;
    }

    @Override
    public boolean isStateSaved() {
        return false;
    }

    @Override
    public Fragment getPrimaryNavigationFragment() {
        return null;
    }

    @Override
    public void dump(String prefix, java.io.FileDescriptor fd, java.io.PrintWriter writer, String[] args) {}
}
