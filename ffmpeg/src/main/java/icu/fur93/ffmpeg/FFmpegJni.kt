package icu.fur93.ffmpeg

object FFmpegJni {
    // 外部函数用于获取 FFmpeg 版本
    external fun getVersion(): String

    // 外部函数用于获取视频信息
    external fun getVideoInfo(filePath: String): String
}
