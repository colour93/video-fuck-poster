package icu.fur93.videofuckposter

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import icu.fur93.videofuckposter.ui.DataViewModel
import icu.fur93.videofuckposter.ui.theme.VideoFuckPosterTheme
import icu.fur93.videofuckposter.ui.view.CaptureView
import icu.fur93.videofuckposter.ui.view.HomeView
import icu.fur93.videofuckposter.ui.view.PosterView

class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

        requestManageAllFilesPermission(this)

        val viewModel = DataViewModel()

        setContent {
            VideoFuckPosterTheme {
                App(viewModel)
            }
        }
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
}

@Composable
fun App(viewModel: DataViewModel) {

    val currentTab = remember { mutableStateOf(0) }

    val items = listOf("视频信息", "海报生成", "截图测试")
    val selectedIcons = listOf(
        painterResource(R.drawable.description_filled_24px),
        painterResource(R.drawable.gallery_thumbnail_filled_24px),
        painterResource(R.drawable.image_filled_24px)
    )
    val unselectedIcons = listOf(
        painterResource(R.drawable.description_24px),
        painterResource(R.drawable.gallery_thumbnail_24px),
        painterResource(R.drawable.image_24px)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentTab.value == index) selectedIcons[index] else unselectedIcons[index],
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = currentTab.value == index,
                        onClick = { currentTab.value = index }
                    )
                }
            }
        },
        floatingActionButton = {
            VideoPickerFloatingButton(viewModel)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab.value) {
                0 -> HomeView(viewModel)
                1 -> PosterView(viewModel)
                2 -> CaptureView(viewModel)
            }
        }
    }
}


@Composable
fun VideoPickerFloatingButton(viewModel: DataViewModel) {
    val context = LocalContext.current
    val contentResolver: ContentResolver = context.contentResolver
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { videoUri ->
            Log.d("video uri authority", "${uri.authority}")
            Log.d("video uri", videoUri.toString())
            val videoPath = Utils.getFilePathFromUri(videoUri, contentResolver)
            if (videoPath != null) {
                viewModel.videoPickerHandler(videoPath, context)
            } else {
                Toast.makeText(context, "获取视频文件路径失败，请检查权限后重试", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    FloatingActionButton(
        onClick = { videoPickerLauncher.launch("video/*") },
    ) {
        Icon(painterResource(R.drawable.video_file_filled_24px), "选择视频")
    }
}