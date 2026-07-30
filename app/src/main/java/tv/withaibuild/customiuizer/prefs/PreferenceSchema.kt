package tv.withaibuild.customiuizer.prefs

data class PreferenceEntry(
    val key: String,
    val type: PreferenceType,
    val defaultValue: Any,
    val allowedRange: String,
    val ownerFeature: String,
    val requiresRestart: String
)

enum class PreferenceType {
    BOOLEAN, INT, STRING, STRING_SET
}

object PreferenceSchema {

    val entries: List<PreferenceEntry> = listOf(
        PreferenceEntry(
            key = "system_statusbar_clocktweak",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            allowedRange = "",
            ownerFeature = "statusbarClock",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "system_cc_clocktweak",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            allowedRange = "",
            ownerFeature = "statusbarClock",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "system_cc_hidedate",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            allowedRange = "",
            ownerFeature = "statusbarClock",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "system_cc_dateformat",
            type = PreferenceType.STRING,
            defaultValue = "",
            allowedRange = "",
            ownerFeature = "statusbarClock",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "system_statusbarheight",
            type = PreferenceType.INT,
            defaultValue = 19,
            allowedRange = "0-100",
            ownerFeature = "statusbarLayout",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "system_detailednetspeed",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            allowedRange = "",
            ownerFeature = "statusbarNetSpeed",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "system_albumartonlock",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            allowedRange = "",
            ownerFeature = "lockScreen",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "controls_navbarheight",
            type = PreferenceType.INT,
            defaultValue = 19,
            allowedRange = "0-100",
            ownerFeature = "navBar",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "various_showcallui",
            type = PreferenceType.INT,
            defaultValue = 0,
            allowedRange = "0-3",
            ownerFeature = "various",
            requiresRestart = "systemui"
        ),
        PreferenceEntry(
            key = "system_scramblepin",
            type = PreferenceType.BOOLEAN,
            defaultValue = false,
            allowedRange = "",
            ownerFeature = "lockScreen",
            requiresRestart = "systemui"
        )
    )

    val byKey: Map<String, PreferenceEntry> = entries.associateBy { it.key }
}
