package icu.fur93.videofuckposter.ui.view

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import icu.fur93.videofuckposter.Utils
import icu.fur93.videofuckposter.ui.DataViewModel
import kotlin.math.floor

@Composable()
fun CaptureView(viewModel: DataViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), // 添加内边距
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CaptureFrameButton(viewModel)
        }
        TimePicker(viewModel)
        CapturePreview(viewModel)
    }
}


@Composable()
fun CaptureFrameButton(viewModel: DataViewModel) {
    val ctx = LocalContext.current;
    Button(onClick = {
        viewModel.captureFrame(ctx)
    }) {
        Text("截取图片")
    }
}

@Composable
fun TimePicker(viewModel: DataViewModel) {

    // 观察 uiState
    val uiState = viewModel.uiState.collectAsState().value

    if (uiState.videoInfo != null) {

        // 将秒转换为 HH:MM:SS 格式
        val hours = floor(uiState.captureTime / 3600).toInt()
        val minutes = floor(uiState.captureTime / 60).toInt()
        val seconds = (uiState.captureTime % 60).toInt()
        val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 显示已选中的时间
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineSmall
            )

            // 滑动条用于选择时间长度
            Slider(
                value = uiState.captureTime,
                onValueChange = { viewModel.updateCaptureTime(it) },
                valueRange = 0f..uiState.videoInfo.duration.toFloat(),
                steps = (uiState.videoInfo.duration - 1).toInt()
            )
        }
    }

}

@Composable
fun CapturePreview(viewModel: DataViewModel) {
    // 观察 uiState
    val uiState = viewModel.uiState.collectAsState().value

    if (uiState.videoCapturePath != null) {
        val imageUri: Uri = Uri.fromFile(uiState.videoCapturePath)
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(),
            contentScale = ContentScale.Crop
        )
    }
}
