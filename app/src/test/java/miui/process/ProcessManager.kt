package miui.process

object ProcessManager {
    @JvmField
    var foregroundInfo: ForegroundInfo? = null

    @JvmField
    var throwOnGet: Throwable? = null

    @JvmStatic
    fun getForegroundInfo(): ForegroundInfo? {
        throwOnGet?.let { throw it }
        return foregroundInfo
    }

    @JvmStatic
    fun reset() {
        foregroundInfo = null
        throwOnGet = null
    }
}
