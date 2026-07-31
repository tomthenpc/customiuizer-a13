package tv.withaibuild.customiuizer.utils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import tv.withaibuild.customiuizer.mods.utils.ModuleHelper;
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers;

/**
 * Thread-safe, state-machine based RemotePreferences bootstrap.
 *
 * Enforces a single Preference Listener per process, double-read stable
 * snapshot publication, and explicit failure recovery without sleeping.
 */
public class PreferenceBootstrap {

    public enum State {
        UNINITIALIZED,
        UNAVAILABLE,
        EMPTY_PENDING,
        LOADED,
        VALID_EMPTY
    }

    private static final int MAX_ATTEMPTS = 3;

    private final Function<String, SharedPreferences> provider;
    private final String name;
    private final PrefMap<String, Object> snapshot;

    // All state transitions are guarded by this.
    private final Object lock = new Object();

    private State state = State.UNINITIALIZED;
    private int attempts = 0;
    private int emptyConfirmations = 0;
    private SharedPreferences remote;
    private OnSharedPreferenceChangeListener listener;
    private boolean watcherRegistered = false;
    private String lastFailureStage = null;
    private Throwable lastError = null;

    public PreferenceBootstrap(
        Function<String, SharedPreferences> provider,
        String name,
        PrefMap<String, Object> snapshot
    ) {
        this.provider = provider;
        this.name = name;
        this.snapshot = snapshot;
    }

    public SharedPreferences resolveRemote() {
        synchronized (lock) {
            if (remote != null) return remote;
            SharedPreferences prefs;
            try {
                prefs = provider.apply(name);
            } catch (Throwable t) {
                recordFailure("resolve_remote", t);
                return null;
            }
            if (prefs == null) {
                recordFailure("resolve_remote_null", null);
                return null;
            }
            this.remote = prefs;
            return prefs;
        }
    }

    public State start() {
        synchronized (lock) {
            if (state == State.LOADED || state == State.VALID_EMPTY) return state;
            if (attempts >= MAX_ATTEMPTS) {
                if (state == State.UNINITIALIZED) state = State.UNAVAILABLE;
                return state;
            }
            attempts++;

            // 1. Get RemotePreferences
            SharedPreferences prefs = resolveRemote();
            if (prefs == null) {
                state = State.UNAVAILABLE;
                return state;
            }

            // 2. First getAll()
            Map<String, ?> first;
            try {
                first = prefs.getAll();
            } catch (Throwable t) {
                recordFailure("first_getAll", t);
                state = State.UNAVAILABLE;
                return state;
            }
            if (first == null) {
                recordFailure("first_getAll_null", null);
                state = State.UNAVAILABLE;
                return state;
            }

            // 3. Register the unique listener.
            // The same process must have at most one live listener.
            if (!watcherRegistered) {
                ensureListenerLocked();
            }

            // 4. Second getAll() after listener is registered.
            Map<String, ?> second;
            try {
                second = prefs.getAll();
            } catch (Throwable t) {
                recordFailure("second_getAll", t);
                state = State.UNAVAILABLE;
                return state;
            }
            if (second == null) {
                recordFailure("second_getAll_null", null);
                state = State.UNAVAILABLE;
                return state;
            }

            // 5. Publish the stable second snapshot.
            publishSnapshot(second);

            if (!second.isEmpty()) {
                state = State.LOADED;
            } else {
                if (watcherRegistered) {
                    state = State.VALID_EMPTY;
                    emptyConfirmations++;
                } else {
                    state = State.EMPTY_PENDING;
                }
            }

            return state;
        }
    }

    /**
     * Retry listener registration and, if it succeeds, take a second snapshot.
     * Safe to call from late lifecycle points such as application onCreate.
     */
    public State ensureWatcher() {
        synchronized (lock) {
            if (watcherRegistered) return state;
            if (remote == null) {
                // No remote yet; start from the top.
                return start();
            }

            ensureListenerLocked();

            if (!watcherRegistered) {
                // Listener still failed. Keep current state, do not downgrade
                // a previously loaded snapshot just because updates are missing.
                return state;
            }

            Map<String, ?> current;
            try {
                current = remote.getAll();
            } catch (Throwable t) {
                recordFailure("ensureWatcher_getAll", t);
                return state;
            }
            if (current == null) {
                recordFailure("ensureWatcher_getAll_null", null);
                return state;
            }

            publishSnapshot(current);

            if (!current.isEmpty()) {
                state = State.LOADED;
            } else {
                state = State.VALID_EMPTY;
                emptyConfirmations++;
            }
            return state;
        }
    }

    public State getState() {
        synchronized (lock) { return state; }
    }

    public boolean isLoaded() {
        synchronized (lock) {
            return state == State.LOADED || state == State.VALID_EMPTY;
        }
    }

    public boolean isWatcherRegistered() {
        synchronized (lock) { return watcherRegistered; }
    }

    public SharedPreferences getRemotePreferences() {
        synchronized (lock) { return remote; }
    }

    public PrefMap<String, Object> getSnapshot() {
        synchronized (lock) { return snapshot; }
    }

    public int getAttempts() {
        synchronized (lock) { return attempts; }
    }

    public int getEmptyConfirmations() {
        synchronized (lock) { return emptyConfirmations; }
    }

    public String getLastFailureStage() {
        synchronized (lock) { return lastFailureStage; }
    }

    public Throwable getLastError() {
        synchronized (lock) { return lastError; }
    }

    private void ensureListenerLocked() {
        if (watcherRegistered || remote == null) return;

        OnSharedPreferenceChangeListener l = createListener();
        try {
            remote.registerOnSharedPreferenceChangeListener(l);
            this.listener = l;
            this.watcherRegistered = true;
        } catch (Throwable t) {
            recordFailure("register_listener", t);
            // Do not pretend the listener is registered.
        }
    }

    private OnSharedPreferenceChangeListener createListener() {
        return (sharedPreferences, key) -> onPreferenceChanged(sharedPreferences, key);
    }

    private void onPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;

        // Serialize the update with snapshot publishing. The actual map is a
        // ConcurrentHashMap, but we hold the bootstrap lock so that a full
        // replaceSnapshot cannot interleave with a listener update and lose a key.
        synchronized (lock) {
            if (state != State.LOADED && state != State.VALID_EMPTY && state != State.EMPTY_PENDING) {
                // Snapshot is not yet published. Drop the update; the next
                // stable publish will read the current remote state.
                return;
            }

            Object val;
            if (sharedPreferences.contains(key)) {
                // Read the raw value through getAll() once. This avoids mismatched
                // type-specific getters when a key changes type between updates, and
                // prevents a single malformed preference from crashing the update path.
                val = sharedPreferences.getAll().get(key);
            } else {
                val = null;
            }

            if (val == null) {
                snapshot.remove(key);
            } else {
                snapshot.put(key, val);
            }
        }

        if (!"pref_key_systemui_restart_time".equals(key)) {
            ModuleHelper.handlePreferenceChanged(key);
        }
    }

    private void publishSnapshot(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            snapshot.clear();
        } else {
            snapshot.replaceSnapshot(normalizeMap(source));
        }
    }

    private Map<String, Object> normalizeMap(Map<String, ?> source) {
        PrefMap<String, Object> out = new PrefMap<>();
        for (Map.Entry<String, ?> e : source.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k != null && v != null) out.put(k, v);
        }
        return out.asMap();
    }

    private void recordFailure(String stage, Throwable t) {
        lastFailureStage = stage;
        lastError = t;
        String msg = "PreferenceBootstrap failed at " + stage;
        if (t != null) XposedHelpers.log(msg, t);
        else XposedHelpers.log(msg);
    }
}
