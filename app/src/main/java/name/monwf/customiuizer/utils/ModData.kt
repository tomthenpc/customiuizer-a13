package name.monwf.customiuizer.utils

class ModData {

    enum class ModCat {
        pref_key_system,
        pref_key_launcher,
        pref_key_controls,
        pref_key_various
    }

    @JvmField var title: String? = null
    @JvmField var breadcrumbs: String? = null
    @JvmField var key: String? = null
    @JvmField var cat: ModCat? = null
    @JvmField var sub: String? = null
    @JvmField var order: Int = 0
}
