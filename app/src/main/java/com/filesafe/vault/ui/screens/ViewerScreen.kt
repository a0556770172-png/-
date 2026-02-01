package com.filesafe.vault.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.filesafe.vault.viewmodel.AppViewModelFactory
import com.filesafe.vault.viewmodel.ItemDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ViewerScreen(factory: AppViewModelFactory, itemId: String, onBack: () -> Unit) {
    val viewModel: ItemDetailsViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    val tempFileState = remember { mutableStateOf<File?>(null) }
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            tempFileState.value?.delete()
        }
    }

    LaunchedEffect(itemId) {
        viewModel.load(itemId)
        val file = withContext(Dispatchers.IO) { viewModel.decryptToTemp(itemId) }
        tempFileState.value = file
        val mimeType = viewModel.item.value?.mimeType ?: ""
        if (file != null && mimeType.startsWith("image/")) {
            bitmapState.value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) }
        } else if (file != null && mimeType == "application/pdf") {
            bitmapState.value = withContext(Dispatchers.IO) { renderPdfFirstPage(file) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Viewer", style = MaterialTheme.typography.headlineMedium)
        val mimeType = viewModel.item.value?.mimeType ?: ""
        when {
            bitmapState.value != null -> {
                Image(bitmap = bitmapState.value!!.asImageBitmap(), contentDescription = "content")
            }
            mimeType.startsWith("video/") || mimeType.startsWith("audio/") -> {
                tempFileState.value?.let { file ->
                    AndroidView(
                        modifier = Modifier.weight(1f),
                        factory = { ctx ->
                            val player = ExoPlayer.Builder(ctx).build().apply {
                                setMediaItem(MediaItem.fromUri(file.toURI().toString()))
                                prepare()
                                playWhenReady = false
                            }
                            PlayerView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                this.player = player
                                tag = player
                            }
                        },
                        onRelease = { view ->
                            (view.tag as? ExoPlayer)?.release()
                        }
                    )
                }
            }
            else -> {
                Text(text = "Unsupported format")
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

private fun renderPdfFirstPage(file: File): Bitmap? {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    descriptor.use {
        val renderer = PdfRenderer(it)
        renderer.use { pdf ->
            if (pdf.pageCount == 0) return null
            val page = pdf.openPage(0)
            page.use { p ->
                val bitmap = Bitmap.createBitmap(p.width, p.height, Bitmap.Config.ARGB_8888)
                p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }
}
