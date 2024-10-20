package icu.fur93.videofuckposter.ui.view

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import icu.fur93.videofuckposter.ui.DataViewModel
import icu.fur93.videofuckposter.ui.component.ShareImageButton

@Composable()
fun PosterView(viewModel: DataViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), // 添加内边距
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "海报生成",
            style = MaterialTheme.typography.titleLarge
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GeneratePosterButton(viewModel)
            ShareImageButton(uiState.videoPosterPath)
        }
        PosterPreview(viewModel)
    }
}

//@Composable()
//fun GeneratePosterConfig (viewModel: DataViewModel) {
//    // 观察 uiState
//    val uiState = viewModel.uiState.collectAsState().value
//
//    Row {
//        Column(modifier = Modifier.padding(16.dp)) {
//            OutlinedTextField(
//                value = uiState.videoPosterConfig.rows,
//                onValueChange = {  },
//                label = {  },
//                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
//            )
//            OutlinedTextField(
//                value = columns,
//                onValueChange = { columns = it },
//                label = {  },
//                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
//            )
//        }
//    }
//}

@Composable()
fun GeneratePosterButton(viewModel: DataViewModel) {
    val ctx = LocalContext.current;
    val uiState = viewModel.uiState.collectAsState().value
    Button(
        onClick = {
            viewModel.generatePoster(ctx)
        },
        enabled = !uiState.pending && (uiState.videoInfo != null)
    ) {
        Text("生成海报")
    }
}

@Composable
fun PosterPreview(viewModel: DataViewModel) {
    // 观察 uiState
    val uiState = viewModel.uiState.collectAsState().value

    if (uiState.videoPosterPath != null) {
        val imageUri: Uri = Uri.fromFile(uiState.videoPosterPath)
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(),
            contentScale = ContentScale.Crop
        )
    }
}