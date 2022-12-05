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

        /**
         * Build a Gif from a list of [Bitmap]'s and save it to internal storage in
         * [CacheProvider.gifCache]. Return a [BuildGifResult] containing the Size of the new[Bitmap]
         */
        fun buildGifAndSaveToInternalStorage(
            bitmaps: List<Bitmap>,
            contentResolver: ContentResolver,
            cacheProvider: CacheProvider,
            versionProvider: VersionProvider
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
            val uri = saveGifToInternalStorage(
                contentResolver = contentResolver,
                bytes = byteArray,
                cacheProvider = cacheProvider,
                versionProvider = versionProvider
            )
            return BuildGif.BuildGifResult(uri = uri, byteArray.size)
        }

        /**
         * Save a [ByteArray] to internal storage.
         * You do not need permissions to write/read to internal storage at any API level(yet)
         *
         * Suppresses the version warning since we're using [VersionProvider]
         */


        @SuppressLint("NewApi")
        fun saveGifToInternalStorage(
            contentResolver: ContentResolver,
            bytes: ByteArray,
            cacheProvider: CacheProvider,
            versionProvider: VersionProvider
        ): Uri {
            val fileName = if (versionProvider.provideVersion() >= Build.VERSION_CODES.O) {
                "${FileNameBuilder.buildFileNameAPI26()}.gif"
            } else {
                "${FileNameBuilder.buildFileName()}.gif"
            }

            val file = File.createTempFile(fileName, null, cacheProvider.gifCache())
            val uri = file.toUri()
            return contentResolver.openOutputStream(uri)?.let { outputStream ->
                outputStream.write(bytes)
                outputStream.flush()
                outputStream.close()
                uri
            } ?: throw Exception(SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)
        }
    }

}