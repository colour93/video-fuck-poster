package icu.fur93.videofuckposter.ui.component

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ShareImageButton(imageFile: File?) {
    val context = LocalContext.current

    Button(
        enabled = imageFile != null,
        onClick = {

            if (imageFile == null) {
                Toast.makeText(context, "请先生成图片", Toast.LENGTH_SHORT).show()
                return@Button
            }

            // 确保文件存在
            if (imageFile.exists()) {
                // 创建分享意图
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider", // 这里需要使用你的 FileProvider 的 authority
                    imageFile
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/*" // 设置 MIME 类型为图片
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 允许其他应用读取文件
                }

                // 启动分享意图
                context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
            }

        }) {
        Text("分享图片")
    }
}