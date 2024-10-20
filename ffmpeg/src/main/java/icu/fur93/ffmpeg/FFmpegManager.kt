package icu.fur93.ffmpeg

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.google.gson.Gson
import icu.fur93.ffmpeg.video.VideoInfo
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FFmpegManager {

    private val gson = Gson()

    init {
        System.loadLibrary("ffmpeg")
    }

    // 获取 FFmpeg 版本
    fun getFFmpegVersion(): String {
        return FFmpegJni.getVersion()
    }

    // 获取视频信息
    fun getVideoInfo(filePath: String): VideoInfo? {
        val json = FFmpegJni.getVideoInfo(filePath) ?: return null
        return gson.fromJson(json, VideoInfo::class.java)
    }

    // 获取视频截图
    fun captureFrame(videoPath: String, timeInSeconds: Float, outputPath: String): Boolean {
        return FFmpegJni.captureFrame(videoPath, timeInSeconds, outputPath)
    }

    fun captureFrameToFile(videoPath: String, timeInSeconds: Float): File? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "VideoFuckPoster/cache",
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val bmpFile = File(
            dir,
            "${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
            }.bmp"
        )
        val result = captureFrame(
            videoPath,
            timeInSeconds,
            bmpFile.path
        )
        if (result) {
            val rawBmp: Bitmap = BitmapFactory.decodeFile(bmpFile.toString());
            val pngFile = File(
                dir,
                "${
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                }.png"
            )
            FileOutputStream(pngFile).use { outputStream ->
                rawBmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }.also {
                bmpFile.delete()
            }
            return pngFile
        } else {
            return null;
        }
    }

}
