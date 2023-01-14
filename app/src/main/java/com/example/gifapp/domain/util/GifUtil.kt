package com.example.gifapp.domain.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.example.gifapp.use_cases.BuildGif
import com.example.gifapp.use_cases.BuildGifUseCase
import java.io.ByteArrayOutputStream
import java.io.File

object GifUtil {

    /**
     * Build a Gif from a list of [Bitmap]'s and save it to internal storage in
     * [CacheProvider.gifCache]. Return a [BuildGif.BuildGifResult] containing the Size of the new[Bitmap]
     */
    fun buildGifAndSaveToInternalStorage(
        bitmaps: List<Bitmap>,
        contentResolver: ContentResolver,
        cacheProvider: CacheProvider,
        versionProvider: VersionProvider
    ): BuildGif.BuildGifResult {

        check(bitmaps.isNotEmpty()) { BuildGifUseCase.NO_BITMAPS_ERROR }
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
        } ?: throw Exception(BuildGifUseCase.SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)
    }
}