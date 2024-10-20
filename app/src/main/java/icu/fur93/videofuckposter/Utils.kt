package icu.fur93.videofuckposter

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import icu.fur93.ffmpeg.video.VideoInfo
import java.io.File

object Utils {

    private fun getFilePathFromContentUri(
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

    fun getFilePathFromUri(
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

    fun humanReadableByteCount(bytes: Long, si: Boolean = true): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val prefix = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), prefix)
    }

    fun getPoints(value: Long, num: Int): List<Long> {
        val interval = value / (num - 1)
        return (0..<num).map { it * interval }
    }

    fun formatTimeCode(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}