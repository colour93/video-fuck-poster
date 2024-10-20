package icu.fur93.videofuckposter.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import icu.fur93.ffmpeg.FFmpegManager
import icu.fur93.ffmpeg.video.VideoInfo
import icu.fur93.videofuckposter.Poster
import icu.fur93.videofuckposter.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DataViewModel() : ViewModel() {

    private val _ffmpegManager = FFmpegManager()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun videoPickerHandler(videoPath: String, ctx: Context) {
        Log.d("video path", videoPath)
        val newVideoInfo =
            videoPath.let { _ffmpegManager.getVideoInfo(it) }
        if (newVideoInfo == null) {
            Toast.makeText(ctx, "获取视频信息失败", Toast.LENGTH_SHORT).show()
            return;
        }
        Toast.makeText(ctx, "选择成功：${File(videoPath).name}", Toast.LENGTH_LONG).show()
        _uiState.value = _uiState.value.copy(videoInfo = newVideoInfo)
    }

    fun captureFrame(ctx: Context) {
        if (uiState.value.videoInfo == null) {
            Toast.makeText(ctx, "请选择视频", Toast.LENGTH_SHORT).show()
            return;
        }
        _uiState.value = _uiState.value.copy(pending = true)
        GlobalScope.launch(Dispatchers.IO) {
            val imageFile = _ffmpegManager.captureFrameToFile(
                uiState.value.videoInfo!!.path,
                uiState.value.captureTime
            )
            _uiState.value = _uiState.value.copy(pending = false)
            if (imageFile == null) {
                Toast.makeText(ctx, "截图失败", Toast.LENGTH_SHORT).show()
                return@launch;
            }
            _uiState.value = _uiState.value.copy(videoCapturePath = imageFile);
        }
    }

    fun updateCaptureTime(newTime: Float) {
        _uiState.value = _uiState.value.copy(captureTime = newTime)
    }

    fun updatePosterConfig(newConfig: PosterConfig) {
        _uiState.value = _uiState.value.copy(videoPosterConfig = newConfig)
    }

    fun generatePoster(ctx: Context) {
        if (uiState.value.videoInfo == null) {
            Toast.makeText(ctx, "请选择视频", Toast.LENGTH_SHORT).show()
            return;
        }

        _uiState.value = _uiState.value.copy(pending = true)

        GlobalScope.launch(Dispatchers.IO) {

            val videoPosterFile =
                Poster.drawVideoPoster(
                    _ffmpegManager,
                    uiState.value.videoInfo!!,
                    uiState.value.videoPosterConfig.rows,
                    uiState.value.videoPosterConfig.cols
                )

            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, "生成完毕", Toast.LENGTH_SHORT).show()
                _uiState.value = _uiState.value.copy(pending = false)
                _uiState.value = _uiState.value.copy(videoPosterPath = videoPosterFile)
            }
        }
    }

}

data class PosterConfig(
    val cols: Int = 3,
    val rows: Int = 4,
)

data class UiState(
    val pending: Boolean = false,
    val videoInfo: VideoInfo? = null,
    val videoCapturePath: File? = null,
    val captureTime: Float = 0f,
    val videoPosterConfig: PosterConfig = PosterConfig(),
    val videoPosterPath: File? = null
)