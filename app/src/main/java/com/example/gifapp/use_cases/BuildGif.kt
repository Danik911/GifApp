package com.example.gifapp.use_cases

import android.graphics.Bitmap
import android.net.Uri
import com.example.gifapp.domain.DataState
import com.example.gifapp.domain.DataState.*
import com.example.gifapp.domain.util.AnimatedGIFWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream

interface BuildGif {

    fun execute(
        bitmaps: List<Bitmap>
    ): Flow<DataState<BuildGifResult>>

    data class BuildGifResult(
        val uri: Uri,
        val gigSize: Int
    )
}

/**
 * Use-case for building a gif given a list of [Bitmap]'s. The result gif is saved it to the internal
 * storage. We don't need read/write permission because saving to the cache does not require it.
 */
class BuildGifUseCase
constructor() : BuildGif {

    override fun execute(bitmaps: List<Bitmap>): Flow<DataState<BuildGif.BuildGifResult>> = flow {

        emit(Loading(Loading.LoadingState.Active()))

        try {
            val result = buildGifAndSaveToInternalStorage(
                bitmaps = bitmaps
            )
            emit(Data(result))
        } catch (e: Exception) {
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(Loading.LoadingState.Idle))
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif"
        const val NO_BITMAPS_ERROR = "You can't build a gif when there are no Bitmaps"
        const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR =
            "An error occurred while trying to save the gif to internal storage"

        /**
         * Build a Gif from a list of [Bitmap]'s and save it to internal storage in
         * [CacheProvider.gifCache]. Return a [BuildResult] containing the Size of the new[Bitmap]
         */
        fun buildGifAndSaveToInternalStorage(
            bitmaps: List<Bitmap>
        ): BuildGif.BuildGifResult {

            check(bitmaps.isNotEmpty()) { NO_BITMAPS_ERROR }
            val writer = AnimatedGIFWriter(true)
            val bos = ByteArrayOutputStream()
            writer.prepareForWrite(bos, -1, -1)
            for (bitmap in bitmaps) {
                writer.writeFrame(bos, bitmap)
            }
            writer.finishWrite(bos)
            val byteArray = bos.toByteArray()
            val uri = saveGifToInternalStorage() // TODO
            return BuildGif.BuildGifResult(uri = uri, byteArray.size)
        }

        fun saveGifToInternalStorage(): Uri {
            TODO("Save gif to cache")
        }
    }

}