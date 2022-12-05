package com.example.gifapp.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gifapp.domain.model.DataState.Loading.LoadingState

@Composable
fun RecordActionBar(
    modifier: Modifier,
    bitmapLoadingState: LoadingState,
    startBitmapCaptureJob: () -> Unit,
    stopBitmapCaptureJob: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(3f)
                .height(50.dp)
                .background(Color.Transparent)
        ) {
            when (bitmapLoadingState) {
                is LoadingState.Active -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color.Black,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(45.dp)
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        progress = bitmapLoadingState.progress ?: 0f,
                        backgroundColor = Color.White,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }
        RecordButton(
            modifier = Modifier.weight(1f),
            startBitmapCaptureJob = startBitmapCaptureJob,
            stopBitmapCaptureJob = stopBitmapCaptureJob,
            bitmapLoadingState = bitmapLoadingState
        )

    }
}

@Composable
fun RecordButton(
    modifier: Modifier,
    bitmapLoadingState: LoadingState,
    startBitmapCaptureJob: () -> Unit,
    stopBitmapCaptureJob: () -> Unit,
) {
    val isRecording = when (bitmapLoadingState) {
        is LoadingState.Active -> true
        LoadingState.Idle -> false

    }
    Button(
        modifier = modifier.wrapContentWidth(),
        onClick = {
            if (!isRecording){
                startBitmapCaptureJob()
            } else {
                stopBitmapCaptureJob()
            }
        },
        colors = if (isRecording) {
            ButtonDefaults.buttonColors(backgroundColor = Color.Red)
        } else {
            ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
        }

    ) {
        Text(text = if (isRecording) "End" else "Record")
    }
}