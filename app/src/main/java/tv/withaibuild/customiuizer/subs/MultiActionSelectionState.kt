package tv.withaibuild.customiuizer.subs

import android.app.Activity
import android.content.Intent

data class MultiActionSelectionState(
    val appValue: String? = null,
    val appUser: Int = -1,
    val shortcutValue: String? = null,
    val shortcutName: String? = null,
    val shortcutIcon: String? = null,
    val shortcutIntent: Intent? = null,
    val activityValue: String? = null,
    val activityUser: Int = -1
)

object MultiActionSelectionStateReducer {
    fun reduce(
        current: MultiActionSelectionState,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): MultiActionSelectionState {
        if (resultCode != Activity.RESULT_OK) return current
        return when (requestCode) {
            0 -> current.copy(
                appValue = data?.getStringExtra("app"),
                appUser = data?.getIntExtra("user", 0) ?: 0
            )
            1 -> current.copy(
                shortcutValue = data?.getStringExtra("shortcut_contents"),
                shortcutName = data?.getStringExtra("shortcut_name"),
                shortcutIcon = data?.getStringExtra("shortcut_icon"),
                shortcutIntent = data?.getParcelableExtra("shortcut_intent")
            )
            2 -> current.copy(
                activityValue = data?.getStringExtra("activity"),
                activityUser = data?.getIntExtra("user", 0) ?: 0
            )
            else -> current
        }
    }
}
