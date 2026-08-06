package android.os;

/**
 * Minimal test stub for android.os.Build.
 *
 * The unit-test classpath mockable jar strips or alters final fields,
 * which breaks MainModule version gating. This test source provides
 * the exact fields the module and tests reference, taking precedence
 * over the mockable jar during test compilation and execution.
 */
public class Build {

    public static final boolean IS_INTERNATIONAL_BUILD = false;
    public static final String DISPLAY = "test";

    public static class VERSION {
        public static int SDK_INT = 33;
        public static final String INCREMENTAL = "V14.0.0.0.TEST";
    }

    public static class VERSION_CODES {
        public static final int O = 26;
        public static final int P = 28;
        public static final int TIRAMISU = 33;
    }
}
