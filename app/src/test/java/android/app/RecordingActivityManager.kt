package android.app

open class RecordingActivityManager : ActivityManager() {

    val forceStopCalls = mutableListOf<Pair<String, Int?>>()

    fun forceStopPackage(pkgName: String) {
        forceStopCalls.add(pkgName to null)
    }

    fun forceStopPackageAsUser(pkgName: String, user: Int) {
        forceStopCalls.add(pkgName to user)
    }
}
