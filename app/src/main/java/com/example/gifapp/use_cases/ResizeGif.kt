package com.example.gifapp.use_cases

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.example.gifapp.domain.model.DataState
import com.example.gifapp.domain.model.DataState.Loading
import com.example.gifapp.domain.model.DataState.Loading.LoadingState.Active
import com.example.gifapp.domain.util.GifUtil
import com.example.gifapp.domain.util.VersionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ResizeGif {
    fun execute(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        bilinearFiltering: Boolean = true,
        discardCachedGif: (Uri) -> Unit
    ): Flow<DataState<ResizeGifResult>>

    /**
     * Result returned from [ResizeGif]
     * @param uri: [Uri] of the gif saved to internal storage
     * @param gifSize: Size of the gif as it exists in internal storage
     */
    data class ResizeGifResult(
        val uri: Uri,
        val gifSize: Int
    )
}

/**
 * Use-case for resizing a gif
 *
 * The only way to resize Gif accurately is to iteratively resize in until
 * you reach the target size
 */
class ResizeGifUseCase constructor(
    private val versionProvider: VersionProvider,
    private val cacheProvider: CacheProvider
) : ResizeGif {
    override fun execute(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        bilinearFiltering: Boolean,
        discardCachedGif: (Uri) -> Unit
    ): Flow<DataState<ResizeGif.ResizeGifResult>> = flow {

        var previousUri: Uri? = null
        var progress: Float
        var percentageLoss = percentageLossIncrementSize

        emit(Loading(Active(percentageLoss)))
        try {
            var resizing = true
            while (resizing) {
                //Delete the previously resized gif since we're moving to the next iteration
                previousUri?.let {
                    try {
                        discardCachedGif(it)
                    } catch (e: Exception) {
                        throw Exception(e.message ?: RESIZE_GIF_ERROR)
                    }
                }
                val resizedBitmaps: MutableList<Bitmap> = mutableListOf()
                capturedBitmaps.forEach { bitmap ->
                    val resizedBitmap = resizeBitmap(
                        bitmap = bitmap,
                        sizePercentage = 1 - percentageLoss,
                        bilinearFiltering = bilinearFiltering
                    )
                    resizedBitmaps.add(resizedBitmap)
                }
                val result = GifUtil.buildGifAndSaveToInternalStorage(
                    contentResolver = contentResolver,
                    versionProvider = versionProvider,
                    cacheProvider = cacheProvider,
                    bitmaps = resizedBitmaps
                )
                val uri = result.uri
                val newSize = result.gifSize
                progress = (originalGifSize - newSize) / (originalGifSize - targetSize)
                emit(Loading(Active(progress)))

                //Continue to next iteration
                if (newSize > targetSize) {
                    previousUri = uri
                    percentageLoss += percentageLossIncrementSize
                } else {
                    //Done resizing
                    emit(
                        DataState.Data(
                            ResizeGif.ResizeGifResult(
                                uri = uri,
                                gifSize = newSize
                            )
                        )
                    )
                    resizing = false
                }
            }
            emit(Loading(Loading.LoadingState.Idle))
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: RESIZE_GIF_ERROR))
        }

    }

    private fun resizeBitmap(
        bitmap: Bitmap,
        sizePercentage: Float,
        bilinearFiltering: Boolean,
    ): Bitmap {
        val targetWidth = (bitmap.width * sizePercentage).toInt()
        val targetHeight = (bitmap.height * sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, bilinearFiltering)
    }

    companion object {
        const val RESIZE_GIF_ERROR = "An error occurred while resizing the gif"

        /**
         * How much the gif gets resized after each iteration
         * 0.05 = 5%
         */
        private const val percentageLossIncrementSize = 0.05f
    }

}












