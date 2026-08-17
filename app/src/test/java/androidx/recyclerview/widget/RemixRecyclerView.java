package androidx.recyclerview.widget;

/**
 * Test fixture for B2A-D2 [NoOverscrollAppHook] RemixRecyclerView constructor after.
 * Does not extend View; the production hook's View cast is optional.
 */
public class RemixRecyclerView {
    public boolean mSpringEnabled = true;
    public static Throwable setSpringEnabledFailure;

    public void setSpringEnabled(boolean enabled) throws Throwable {
        if (setSpringEnabledFailure == null) {
            mSpringEnabled = enabled;
            return;
        }
        Throwable failure = setSpringEnabledFailure;
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        throw new RuntimeException(failure);
    }
}
