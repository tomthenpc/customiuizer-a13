package android.net

/** JVM test shadow so Intent(ACTION_VIEW, Uri.parse(...)) keeps the URI string. */
class Uri(private val value: String) {
    override fun toString(): String = value

    companion object {
        @JvmStatic
        fun parse(uriString: String?): Uri = Uri(uriString ?: "")
    }
}
