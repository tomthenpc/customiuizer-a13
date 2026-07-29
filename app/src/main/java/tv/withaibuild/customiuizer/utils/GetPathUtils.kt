package tv.withaibuild.customiuizer.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object GetPathUtils {

    private const val PATH_TREE = "tree"
    private const val PRIMARY_TYPE = "primary"
    private const val RAW_TYPE = "raw"

    /**
     * @param uri DocumentsUI URI
     * @return file path of Uri
     */
    @JvmStatic
    fun getDirectoryPathFromUri(context: Context, uri: Uri?): String? {
        if (uri == null) return null

        if ("file" == uri.scheme) return uri.path

        if (isTreeUri(uri)) {
            val treeId = getTreeDocumentId(uri) ?: return null

            val paths = treeId.split(":")
            val type = paths[0]
            val subPath = if (paths.size == 2) paths[1] else ""

            return when {
                RAW_TYPE.equals(type, ignoreCase = true) -> {
                    treeId.substring(treeId.indexOf(File.separator))
                }
                PRIMARY_TYPE.equals(type, ignoreCase = true) -> {
                    Environment.getExternalStorageDirectory().toString() + File.separator + subPath
                }
                else -> {
                    val pathSegment = treeId.split(":")
                    val rootPath = getRemovableStorageRootPath(context, paths[0])
                    if (pathSegment.size == 1) {
                        rootPath
                    } else {
                        rootPath + File.separator + pathSegment[1]
                    }
                }
            }
        }
        return null
    }

    private fun getRemovableStorageRootPath(context: Context, storageId: String): String {
        val rootPath = StringBuilder()
        val externalFilesDirs = context.getExternalFilesDirs(null)
        for (fileDir in externalFilesDirs) {
            if (fileDir.path.contains(storageId)) {
                val pathSegment = fileDir.path.split(File.separator)
                for (segment in pathSegment) {
                    if (segment == storageId) {
                        rootPath.append(storageId)
                        break
                    }
                    rootPath.append(segment).append(File.separator)
                }
                break
            }
        }
        return rootPath.toString()
    }

    // https://github.com/rcketscientist/DocumentActivity/blob/master/library/src/main/java/com/anthonymandra/framework/DocumentUtil.java#L56
    /**
     * Extract the via [DocumentsContract.Document.COLUMN_DOCUMENT_ID] from the given URI.
     * From [DocumentsContract] but return null instead of throw
     */
    private fun getTreeDocumentId(uri: Uri): String? {
        val paths = uri.pathSegments
        return if (paths.size >= 2 && PATH_TREE == paths[0]) {
            paths[1]
        } else null
    }

    private fun isTreeUri(uri: Uri): Boolean {
        val paths = uri.pathSegments
        return paths.size == 2 && PATH_TREE == paths[0]
    }
}
