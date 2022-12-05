package com.example.gifapp.use_cases

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.example.gifapp.domain.model.DataState
import com.example.gifapp.domain.model.DataState.*
import com.example.gifapp.domain.util.AnimatedGIFWriter
import com.example.gifapp.domain.util.FileNameBuilder
import com.example.gifapp.domain.util.GifUtil.buildGifAndSaveToInternalStorage
import com.example.gifapp.domain.util.VersionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import java.io.File

interface BuildGif {

    fun execute(
        bitmaps: List<Bitmap>,
        contentResolver: ContentResolver
    ): Flow<DataState<BuildGifResult>>

    data class BuildGifResult(
        val uri: Uri, val gifSize: Int
    )
}

/**
 * Use-case for building a gif given a list of [Bitmap]'s. The result gif is saved it to the internal
 * storage. We don't need read/write permission because saving to the cache does not require it.
 */
class BuildGifUseCase
constructor(
    private val versionProvider: VersionProvider,
    private val cacheProvider: CacheProvider
) : BuildGif {

    override fun execute(
        bitmaps: List<Bitmap>,
        contentResolver: ContentResolver
    ): Flow<DataState<BuildGif.BuildGifResult>> = flow {

        emit(Loading(Loading.LoadingState.Active()))

        try {
            val result = buildGifAndSaveToInternalStorage(
                bitmaps = bitmaps,
                contentResolver = contentResolver,
                cacheProvider = cacheProvider,
                versionProvider = versionProvider
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
      }
}