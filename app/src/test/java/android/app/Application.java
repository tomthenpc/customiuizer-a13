package android.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;

/**
 * Minimal test stub for android.app.Application.
 *
 * The mockable android jar used for unit tests does not expose the
 * protected {@code attach(Context)} method, which breaks
 * {@code LauncherInstaller.installApplication()} in tests. This stub
 * provides {@code attach} and inherits {@link ContextWrapper} so that
 * tests can exercise the Application lifecycle hook path.
 */
public class Application extends ContextWrapper {

    public Application() {
        super(null);
    }

    /**
     * Exposes the real Android entry point that LauncherInstaller hooks.
     */
    public final void attach(Context base) {
        // no-op for tests
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public ContentResolver getContentResolver() {
        // Test Settings stub does not consult the resolver.
        return null;
    }
}
