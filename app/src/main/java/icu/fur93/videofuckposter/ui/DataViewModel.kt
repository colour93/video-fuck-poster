package icu.fur93.videofuckposter.ui

import androidx.lifecycle.ViewModel
import icu.fur93.ffmpeg.video.VideoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DataViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateVideoInfo(newVideoInfo: VideoInfo?) {
        _uiState.value = _uiState.value.copy(videoInfo = newVideoInfo)
    }

    fun updateVideoCapturePath(newVideoCapturePath: File?) {
        _uiState.value = _uiState.value.copy(videoCapturePath = newVideoCapturePath)
    }

    fun updateCaptureTime(newTime: Float) {
        _uiState.value = _uiState.value.copy(captureTime = newTime)
    }

}

data class UiState(
    val videoInfo: VideoInfo? = null,
    val videoCapturePath: File? = null,
    val captureTime: Float = 0f
)