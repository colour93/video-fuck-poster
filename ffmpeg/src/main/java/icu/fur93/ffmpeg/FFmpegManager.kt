package icu.fur93.ffmpeg

import com.google.gson.Gson
import icu.fur93.ffmpeg.video.VideoInfo

class FFmpegManager {

    val gson = Gson()

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

}
