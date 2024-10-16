package icu.fur93.ffmpeg

import com.google.gson.Gson
import icu.fur93.ffmpeg.video.VideoInfo

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
    fun captureFrame(filePath: String, outputImagePath: String, time: String): String {
//        return FFmpegJni.captureFrame(
//            filePath, outputImagePath, time
//        ) == "success"
        return FFmpegJni.captureFrame(filePath, outputImagePath, time)
    }

}
