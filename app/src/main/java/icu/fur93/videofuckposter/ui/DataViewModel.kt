package icu.fur93.videofuckposter.ui

import androidx.lifecycle.ViewModel
import icu.fur93.ffmpeg.video.VideoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DataViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateVideoInfo(newVideoInfo: VideoInfo?) {
        _uiState.value = _uiState.value.copy(videoInfo = newVideoInfo)
    }

}

data class UiState(
    val videoInfo: VideoInfo? = null
)