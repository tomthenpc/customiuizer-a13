package miui.process

object ProcessManager {
    @JvmField
    var foregroundInfo: ForegroundInfo? = null

    @JvmStatic
    fun getForegroundInfo(): ForegroundInfo? = foregroundInfo
}
