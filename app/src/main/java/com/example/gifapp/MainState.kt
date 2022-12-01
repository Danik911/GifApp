package com.example.gifapp

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import com.example.gifapp.domain.DataState.Loading.LoadingState

sealed class MainState {

    object Initial : MainState()

    object DisplaySelectBackgroundAsset : MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = listOf(),

        //Displayed as a LinearProgressIndicator in the RecordActionBar
        val bitmapCaptureLoadingState: LoadingState = LoadingState.Idle,


        // Displayed as a CircularIndeterminateProgressBar overlaid in the center of the screen
        val loadingState: LoadingState = LoadingState.Idle

    ) : MainState()

    data class DisplayGif(
        val gifUri: Uri?,
        val originalGifSize: Int,

        //Carry around the original background assert URI in-case user resets the gif
        val backgroundAssetUri: Uri
    ) : MainState()


}