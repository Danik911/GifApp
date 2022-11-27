package com.example.gifapp.use_cases

import android.graphics.Bitmap
import android.os.Build
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.applyCanvas
import com.example.gifapp.domain.DataState
import com.example.gifapp.domain.DataState.Loading
import com.example.gifapp.domain.DataState.Loading.LoadingState
import com.example.gifapp.domain.VersionProvider
import com.example.gifapp.use_cases.CaptureBitmapsUseCase.Companion.CAPTURE_INTERVAL_MS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

interface CaptureBitmaps {

    /**
     * @param window is only required if [Build.VERSION_CODES] >= [Build.VERSION_CODES.O]
     * Otherwise this can be null
     */

    fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window?
    ): Flow<DataState<List<Bitmap>>>
}

/**
 * UseCase for capturing a list of bitmaps by screenshotting the device every [CAPTURE_INTERVAL_MS]
 *
 * The way I capture a screenshot diverges for [Build.VERSION_CODES] >= [Build.VERSION_CODES.O]
 * We must use [PixelCopy] for API level 26 (o) and above
 * We have to convert [PixelCopy] callback into coroutine and emmit into flow
 */

class CaptureBitmapsUseCase
constructor(
    private val pixelCopyJob: PixelCopyJob,
    val mainDispatcher: CoroutineDispatcher,
    val versionProvider: VersionProvider
    ) :
    CaptureBitmaps {

    override fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window?
    ): Flow<DataState<List<Bitmap>>> = flow {

        emit(Loading(LoadingState.Active()))
        try {
            check(capturingViewBounds != null) { "Invalid view bounds" }
            check(view != null) { "View hasn't been found" }
            var elapsedTime = 0f
            val bitmaps: MutableList<Bitmap> = mutableListOf()
            while (elapsedTime < TOTAL_CAPTURE_TIME_MS) {
                delay(CAPTURE_INTERVAL_MS.toLong())
                elapsedTime += CAPTURE_INTERVAL_MS
                emit(Loading(LoadingState.Active(progress = elapsedTime / TOTAL_CAPTURE_TIME_MS)))
                val bitmap =
                    if (versionProvider.provideVersion() >= Build.VERSION_CODES.O) {
                        check(window != null) { "Window is required for PixelCopy" }

                        val pixelCopyJobState = pixelCopyJob.execute(
                            capturingViewBonds = capturingViewBounds,
                            view = view,
                            window = window
                        )
                        when (pixelCopyJobState) {
                            is PixelCopyJob.PixelCopyJobState.Done -> {
                                pixelCopyJobState.bitmap
                            }
                            is PixelCopyJob.PixelCopyJobState.Error -> {
                                throw Exception(pixelCopyJobState.message)
                            }
                        }
                    } else {
                        captureBitmap(
                            rect = capturingViewBounds,
                            view = view
                        )
                    }
                //Every time a new bitmap is captured, emit the updated list
                bitmaps.add(bitmap)
                emit(DataState.Data(data = bitmaps.toList()))


            }

        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }
        emit(Loading(LoadingState.Idle))
    }

    /**
     *  Capture a screenshot on API < [Build.VERSION_CODES.O]
     */
    private suspend fun captureBitmap(
        rect: Rect?,
        view: View
    ) = withContext(context = mainDispatcher) {
        check(rect != null) { "Invalid capture area" }
        val bitmap = Bitmap.createBitmap(
            rect.width.roundToInt(),
            rect.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-rect.left, -rect.top)
            view.draw(this)
        }
        return@withContext bitmap
    }

    companion object {
        const val TOTAL_CAPTURE_TIME_MS = 4000f
        const val CAPTURE_INTERVAL_MS = 250f
        const val CAPTURE_BITMAP_ERROR = "An error occurred while capturing the bitmaps"
        const val CAPTURE_BITMAP_SUCCESS = "Completed Successfully"
    }

}
