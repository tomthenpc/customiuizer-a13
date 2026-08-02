package tv.withaibuild.customiuizer.mods.catalog

/**
 * Single source of truth for feature identity normalization.
 *
 * Any lookup that accepts human-entered ids (camelCase, snake_case, spaced,
 * mixed case) is converted to the same normalized form before it is compared
 * with canonical or alias ids.
 */
object FeatureIdentity {

    @JvmStatic
    fun normalizeLookupId(id: String): String =
        id.lowercase().replace("_", "").replace(" ", "")
}
