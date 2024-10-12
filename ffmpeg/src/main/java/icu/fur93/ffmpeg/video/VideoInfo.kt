package icu.fur93.ffmpeg.video

data class VideoInfo(
    val format: String,
    val duration: Long,
    val bitrate: Long,
    val numStreams: Int,
    val videoStreams: List<VideoStream>,
    val audioStreams: List<AudioStream>
)

data class VideoStream(
    val codec: String,
    val resolution: Resolution,
    val frameRate: Double
)

data class AudioStream(
    val codec: String,
    val sampleRate: Int,
    val channels: Int
)

data class Resolution(
    val width: Int,
    val height: Int
)