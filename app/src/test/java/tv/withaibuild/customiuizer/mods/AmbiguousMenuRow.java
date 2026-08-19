package tv.withaibuild.customiuizer.mods;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Fixture for the ambiguous-constructor fail-open test.
 *
 * The two explicit constructors differ only in the fourth parameter type
 * (Drawable vs String). Both are non-static inner-class constructors, so the
 * compiled bytecode contains two constructors whose synthetic first parameter
 * is the outer AmbiguousMenuRow instance. The resolver must reject the
 * ambiguity and return null instead of picking one.
 */
public class AmbiguousMenuRow {
    public class AmbiguousMenuItem {
        public AmbiguousMenuItem(Context context, int titleResId, Drawable icon, int iconResId) {
        }

        public AmbiguousMenuItem(Context context, int titleResId, String text, int iconResId) {
        }
    }
}
