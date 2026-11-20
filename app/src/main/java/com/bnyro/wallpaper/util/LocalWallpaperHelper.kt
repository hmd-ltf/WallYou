package com.bnyro.wallpaper.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.util.Log
import androidx.core.net.toUri
import com.bnyro.wallpaper.obj.WallpaperConfig

object LocalWallpaperHelper {
    private const val TAG = "LocalWallpaperHelper"

    private val PROJECTION = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE
    )

    /**
     * List the URIs of all images inside the config's folders, including sub folders that
     * are not hidden.
     *
     * This runs one query per directory instead of going through
     * [androidx.documentfile.provider.DocumentFile], which runs a separate query for every
     * property of every file and takes minutes on large folders.
     *
     * A folder that can't be read (e.g. because it was deleted or its permission was revoked)
     * is skipped, so the other folders keep working.
     */
    fun getLocalWalls(context: Context, config: WallpaperConfig): List<Uri> {
        return config.localFolderUris.flatMap { folder ->
            runCatching {
                val treeUri = folder.toUri()
                val rootDocumentId = if (DocumentsContract.isDocumentUri(context, treeUri)) {
                    DocumentsContract.getDocumentId(treeUri)
                } else {
                    DocumentsContract.getTreeDocumentId(treeUri)
                }
                listImages(context, treeUri, rootDocumentId)
            }.getOrElse {
                Log.e(TAG, "Can't list wallpapers in $folder", it)
                emptyList()
            }
        }
    }

    private fun listImages(context: Context, treeUri: Uri, parentDocumentId: String): List<Uri> {
        val images = mutableListOf<Uri>()
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)

        context.contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0) ?: continue
                val name = cursor.getString(1).orEmpty()
                val mimeType = cursor.getString(2).orEmpty()

                if (mimeType == Document.MIME_TYPE_DIR) {
                    if (!name.startsWith(".")) images += listImages(context, treeUri, documentId)
                } else if (mimeType.startsWith("image/")) {
                    images += DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                }
            }
        }

        return images
    }
}
