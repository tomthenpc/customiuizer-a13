package android.provider;

import android.content.ContentResolver;

public final class Settings {
    public static final class System {
        // Use -2 as "value missing, fall back to def"
        public static final int MISSING = -2;
        private static int overrideValue = MISSING;

        public static void setOverrideValue(int value) {
            overrideValue = value;
        }

        public static int getIntForUser(ContentResolver resolver, String name, int def, int user) {
            if (overrideValue == MISSING) return def;
            return overrideValue;
        }
    }
}
