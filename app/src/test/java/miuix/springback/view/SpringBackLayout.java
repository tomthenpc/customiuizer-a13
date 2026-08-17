package miuix.springback.view;

/**
 * Test fixture for B2A-D2 [NoOverscrollAppHook] SpringBackLayout constructor after.
 */
public class SpringBackLayout {
    public boolean mSpringBackEnable = true;
    public static Throwable setSpringBackEnableFailure;

    public void setSpringBackEnable(boolean enabled) throws Throwable {
        throwIfConfigured(setSpringBackEnableFailure);
        mSpringBackEnable = enabled;
    }

    private static void throwIfConfigured(Throwable failure) throws Throwable {
        if (failure == null) {
            return;
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
