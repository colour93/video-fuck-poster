package icu.fur93.videofuckposter

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import icu.fur93.ffmpeg.video.VideoInfo
import icu.fur93.videofuckposter.ui.DataViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val ffmpegManager = FFmpegManager()
    private var videoInfo: VideoInfo? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

        setContent {
            App()
        }
    }

    private fun getFilePathFromContentUri(
        contentUri: Uri,
        contentResolver: ContentResolver
    ): String? {
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

    private fun getFilePathFromUri(
        uri: Uri,
        contentResolver: ContentResolver
    ): String? {
        if (uri.authority == "media") {
            return getFilePathFromContentUri(uri, contentResolver)
        } else if (uri.authority == "com.android.externalstorage.documents") {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            if ("primary".equals(type, true)) {
                return "${Environment.getExternalStorageDirectory()}/${split[1]}"
            }
        }
        return null
    }

    private fun requestManageAllFilesPermission(context: Context) {
        requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "已拥有所有文件访问权限", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "当前设备无需此权限", Toast.LENGTH_SHORT).show()
        }
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
            PermissionGrantButton()
            VideoPickerButton(viewModel)
            CaptureFrameButton()
            VideoInfoText(viewModel)
            PosterPreview()
        }
    }

    @Composable()
    fun PermissionGrantButton() {
        val context = LocalContext.current;
        Button(onClick = {
            requestManageAllFilesPermission(context)
        }) {
            Text("申请权限")
        }
    }

    @Composable
    fun VideoPickerButton(viewModel: DataViewModel) {
        val context = LocalContext.current
        val contentResolver: ContentResolver = context.contentResolver
        val videoPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { videoUri ->
//                val videoPath = getRealPathFromURI(videoUri, contentResolver)
                Log.d("video uri authority", "${uri.authority}")
                val videoPath = getFilePathFromUri(videoUri, contentResolver)
                Log.d("video uri", videoUri.toString())
                if (videoPath != null) {
                    Log.d("video path", videoPath)
                }
                val newVideoInfo =
                    videoPath?.let { ffmpegManager.getVideoInfo(it) } // 假设这个函数返回 VideoInfo?
                videoInfo = newVideoInfo
                viewModel.updateVideoInfo(newVideoInfo)
            }
        }

        Button(onClick = {
            videoPickerLauncher.launch("video/*") // 选择视频文件
        }) {
            Text(text = "选择视频")
        }
    }

    @Composable()
    fun CaptureFrameButton() {
        val context = LocalContext.current

        Button(onClick = {
            if (videoInfo == null) {
                Toast.makeText(context, "未选择视频", Toast.LENGTH_SHORT).show()
                return@Button
            }
            Log.d("capture", "starting")
            // 获取内部存储的根目录
            val dir = getExternalFilesDir(null)
            val file = File(dir, "example.txt")
            file.writeText("Hello, World!")
            // 创建 capture-1.png 文件
            val imageFile = File(dir, "capture-1.png")
            Log.d("internal storage", imageFile.path)
            val result = ffmpegManager.captureFrame(videoInfo!!.path, imageFile.path, "00:00:01")
//            Toast.makeText(context, if (result) "截图成功" else "截图失败", Toast.LENGTH_SHORT)
//                .show()
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
        }) {
            Text("截取图片")
        }
    }

    @Composable
    fun VideoInfoText(viewModel: DataViewModel) {
        // 观察 uiState
        val uiState = viewModel.uiState.collectAsState().value

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

    @Composable
    fun PosterPreview() {
        Text("test")
    }
}