package icu.fur93.videofuckposter

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

object Utils {

    fun getFilePathFromContentUri(
        contentUri: Uri,
        contentResolver: ContentResolver
    ): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Video.Media.DATA)

        contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            if (cursor.moveToFirst()) {
                path = cursor.getString(columnIndex)
            }
        }

        return path
    }

    public fun getFilePathFromUri(
        uri: Uri,
        contentResolver: ContentResolver
    ): String? {
        if (uri.authority == "media") {
            return getFilePathFromContentUri(uri, contentResolver)
        } else if (uri.authority == "com.android.externalstorage.documents") {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            if ("primary".equals(type, true)) {
                return "${Environment.getExternalStorageDirectory()}/${split[1]}"
            }
        }
        return null
    }
}