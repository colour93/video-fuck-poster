package icu.fur93.videofuckposter.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import icu.fur93.videofuckposter.ui.DataViewModel

@Composable
fun HomeView (viewModel: DataViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VideoInfoText(viewModel)
    }
}


@Composable
fun VideoInfoText(viewModel: DataViewModel) {
    // 观察 uiState
    val uiState = viewModel.uiState.collectAsState().value

    Text(
        text = "视频信息",
        style = MaterialTheme.typography.titleLarge
    )

    // 根据 videoInfo 更新 UI
    if (uiState.videoInfo != null) {
        // 显示视频信息
        Text(text = "路径: ${uiState.videoInfo.path}")
        Text(text = "视频时长: ${uiState.videoInfo.duration} 秒")
        Text(text = "比特率: ${uiState.videoInfo.bitrate} bps")
        Text(text = "轨道数量: ${uiState.videoInfo.numStreams}")

        uiState.videoInfo.videoStreams.forEachIndexed { index, track ->
            Text(text = "视频轨道 #$index:")
            Text(text = "  编解码器: ${track.codec}")
            Text(text = "  分辨率: ${track.resolution}")
            Text(text = "  帧率: ${track.frameRate} fps")
        }
        uiState.videoInfo.audioStreams.forEachIndexed { index, track ->
            Text(text = "音频轨道 #$index:")
            Text(text = "  编解码器: ${track.codec}")
            Text(text = "  通道数: ${track.channels}")
            Text(text = "  采样率: ${track.sampleRate} Hz")
        }
    } else {
        // 显示无视频信息的提示
        Text(text = "未获取到视频信息")
    }
}
