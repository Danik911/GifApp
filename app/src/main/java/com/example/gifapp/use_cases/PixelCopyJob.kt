package com.example.gifapp.use_cases

import android.graphics.Bitmap
import android.os.Build
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt


// I use an interface for testing
interface PixelCopyJob {

    suspend fun execute(
        capturingViewBonds: Rect?,
        view: View,
        window: Window
    ): PixelCopyJobState

    sealed class PixelCopyJobState {

        data class Done(
            val bitmap: Bitmap
        ) : PixelCopyJobState()

        data class Error(
            val message: String
        ) : PixelCopyJobState()
    }
}

class PixelCopyJobUseCase : PixelCopyJob {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun execute(
        capturingViewBonds: Rect?,
        view: View,
        window: Window
    ): PixelCopyJob.PixelCopyJobState = suspendCancellableCoroutine { suspendableBlock ->
        try {
            check(capturingViewBonds != null) { "Invalid capture area" }
            val bitmap = Bitmap.createBitmap(
                view.width, view.height,
                Bitmap.Config.ARGB_8888
            )

            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            val xCoordinate = locationOfViewInWindow[0]
            val yCoordinate = locationOfViewInWindow[1]
            val scope = android.graphics.Rect(
                xCoordinate,
                yCoordinate,
                xCoordinate + view.width,
                yCoordinate + view.height
            )
            // Take a screenshot
            PixelCopy.request(
                window,
                scope,
                bitmap,
                { p0 ->
                    if (p0 == PixelCopy.SUCCESS) {
                        // Crop the screenshot
                        val bmp = Bitmap.createBitmap(
                            bitmap,
                            capturingViewBonds.left.toInt(),
                            capturingViewBonds.top.toInt(),
                            capturingViewBonds.width.roundToInt(),
                            capturingViewBonds.height.roundToInt()
                        )
                        suspendableBlock.resume(PixelCopyJob.PixelCopyJobState.Done(bmp))
                    } else {
                        suspendableBlock.resume(
                            PixelCopyJob.PixelCopyJobState.Error(message = PIXEL_COPY_ERROR)
                        )
                    }
                },
                android.os.Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            suspendableBlock.resume(
                PixelCopyJob.PixelCopyJobState.Error(
                    message = e.message ?: PIXEL_COPY_ERROR
                )
            )
        }
    }

    companion object {
        const val PIXEL_COPY_ERROR = "An error occurred while running PixelCopy"
    }

}






















