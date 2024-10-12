package icu.fur93.videofuckposter

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import icu.fur93.ffmpeg.FFmpegManager
import icu.fur93.videofuckposter.ui.DataViewModel

class MainActivity : ComponentActivity() {
    private val ffmpegManager = FFmpegManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    private fun getRealPathFromURI(contentUri: Uri, contentResolver: ContentResolver): String? {
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

    @Composable
    fun App() {

        val viewModel: DataViewModel = viewModel()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // 添加内边距
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoPickerScreen(viewModel)
            VideoInfoText(viewModel)
            PosterPreview()
        }
    }

    @Composable
    fun VideoPickerScreen(viewModel: DataViewModel) {
        val context = LocalContext.current
        val contentResolver: ContentResolver = context.contentResolver
        val videoPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { videoUri ->
                val videoPath = getRealPathFromURI(videoUri, contentResolver)
                Log.d("video uri", videoUri.toString())
                if (videoPath != null) {
                    Log.d("video path", videoPath)
                }
                val videoInfo = videoPath?.let { ffmpegManager.getVideoInfo(it) } // 假设这个函数返回 VideoInfo?

                viewModel.updateVideoInfo(videoInfo)
            }
        }

        Button(onClick = {
            videoPickerLauncher.launch("video/*") // 选择视频文件
        }) {
            Text(text = "选择视频")
        }
    }

    @Composable
    fun VideoInfoText(viewModel: DataViewModel) {
        // 观察 uiState
        val uiState = viewModel.uiState.collectAsState().value

        // 根据 videoInfo 更新 UI
        if (uiState.videoInfo != null) {
            // 显示视频信息
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

    @Composable
    fun PosterPreview() {
        Text("test")
    }
}