package tv.withaibuild.customiuizer.utils

import android.util.Log

/**
 * Lightweight, stateless diagnostics helper for the settings app.
 *
 * This object intentionally:
 * - holds no Context or Android owner references
 * - allocates no collections or caches
 * - does not start threads, coroutines, or async work
 * - does not reference [Helpers], [HookUtils] or Xposed APIs
 *
 * It is designed to be shared by multiple settings-app files while keeping
 * the diagnostic surface small and deterministic.
 */
internal object SettingsDiagnostics {
    private const val TAG = "CustoMIUIzer-Settings"

    fun failure(operation: String, throwable: Throwable) {
        Log.e(TAG, operation, throwable)
    }
}
