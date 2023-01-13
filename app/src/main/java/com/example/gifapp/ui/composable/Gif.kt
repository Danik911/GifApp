package com.example.gifapp.ui.composable

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.gifapp.domain.model.DataState

@Composable
fun Gif(
    imageLoader: ImageLoader,
    gifUri: Uri?,
    discardGif: () -> Unit,
    onSaveGif: () -> Unit,
    loadingState: DataState.Loading.LoadingState,
    adjustedBytes: Int,
    updatedAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    currentGifSize: Int,
    isResizedGif: Boolean,
    resizeGif: () -> Unit,
    resetToOriginal: () -> Unit,
    gifResizingLoadingState: DataState.Loading.LoadingState
) {
    StandardLoadingUI(loadingState = loadingState)
    ResizingGifLoadingUI(gifResizingLoadingState = gifResizingLoadingState)

    val configuration = LocalConfiguration.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        if (gifUri != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)

            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = discardGif,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Discard the Gif",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onSaveGif,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Green
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Save",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }

                val image: Painter = rememberAsyncImagePainter(
                    model = gifUri,
                    imageLoader = imageLoader
                )
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((configuration.screenHeightDp * 0.6).dp),
                    painter = image,
                    contentDescription = "Gif image"
                )
                GifFooter(
                    adjustedBytes = adjustedBytes,
                    updatedAdjustedBytes = updatedAdjustedBytes,
                    sizePercentage = sizePercentage,
                    updateSizePercentage = updateSizePercentage,
                    gifSize = currentGifSize,
                    isResizedGif = isResizedGif,
                    resizeGif = resizeGif,
                    resetResizing = resetToOriginal
                )
            }
        }
    }
}

@Composable
fun GifFooter(
    adjustedBytes: Int,
    updatedAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    gifSize: Int,
    isResizedGif: Boolean,
    resizeGif: () -> Unit,
    resetResizing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.End),
            text = "Approximate gif size",
            style = MaterialTheme.typography.h6
        )
        Text(
            modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.body1,
            text = "${adjustedBytes / 1024} KB"
        )
        if (isResizedGif) {
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = resetResizing
            ) {
                Text(
                    text = "Reset resizing",
                    style = MaterialTheme.typography.body1
                )
            }
        } else {
            Text(
                text = "$sizePercentage %",
                style = MaterialTheme.typography.body1
            )
            var sliderPosition by remember {
                mutableStateOf(100f)
            }
            Slider(
                value = sliderPosition,
                valueRange = 1f..100f,
                onValueChange = {
                    sliderPosition = it
                    updateSizePercentage(sliderPosition.toInt())
                    updatedAdjustedBytes(gifSize * sliderPosition.toInt() / 100)
                }
            )
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = resizeGif
            ) {
                Text(
                    text = "Resize",
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}