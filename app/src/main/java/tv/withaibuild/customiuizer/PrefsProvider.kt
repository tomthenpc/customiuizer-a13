package tv.withaibuild.customiuizer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics
import java.io.FileNotFoundException

class PrefsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.sharedprefs"
        private const val TEST_ASSET = 5

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "test/*", TEST_ASSET)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    @Throws(FileNotFoundException::class)
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val ctx = context ?: return null

        val parts = uri.pathSegments
        if (uriMatcher.match(uri) == TEST_ASSET) {
            val fileType = parts.getOrNull(1)
            val filename = when (fileType) {
                "0" -> "test0.png"
                "1" -> "test1.mp3"
                "2" -> "test2.mp4"
                "3", "5" -> "test3.txt"
                "4" -> "test4.zip"
                else -> null
            }

            if (filename != null) try {
                return ctx.assets.openFd(filename)
            } catch (t: Throwable) {
                if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                SettingsDiagnostics.failure("PrefsProvider.openTestAsset", t)
            }
        }

        return null
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}
