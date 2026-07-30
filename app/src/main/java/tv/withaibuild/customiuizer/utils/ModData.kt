package tv.withaibuild.customiuizer.utils

import java.util.Locale

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
    @JvmField var titleSearchKey: String = ""
    @JvmField var breadcrumbsSortKey: String = ""

    fun prepareSearchKeys() {
        titleSearchKey = title.orEmpty().lowercase(Locale.ROOT)
        breadcrumbsSortKey = breadcrumbs.orEmpty().lowercase(Locale.ROOT)
    }
}
