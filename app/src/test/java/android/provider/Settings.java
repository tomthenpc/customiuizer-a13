package android.provider;

import android.content.ContentResolver;

public final class Settings {
    public static final class System {
        // Use -2 as "value missing, fall back to def"
        public static final int MISSING = -2;
        private static int overrideValue = MISSING;
        private static long overrideLong = java.lang.Long.MAX_VALUE;

        public static void setOverrideValue(int value) {
            overrideValue = value;
        }

        public static void setOverrideLong(long value) {
            overrideLong = value;
        }

        public static int getIntForUser(ContentResolver resolver, String name, int def, int user) {
            if (overrideValue == MISSING) return def;
            return overrideValue;
        }

        public static long getLong(ContentResolver resolver, String name, long def) {
            // Returning a large value causes SystemUiInstaller to bail out
            // early after the minimal always-on hook setup, matching the
            // restart-time guard path on a real device.
            return overrideLong;
        }
    }
}
