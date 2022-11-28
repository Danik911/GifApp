package com.example.gifapp.use_cases

import android.app.Application
import java.io.File

interface CacheProvider {

    /**
     * Provides the directory where cached gif are kept
     */
    fun gifCache(): File
}

class RealCacheProvided
constructor(
    private val app: Application
) : CacheProvider {
    override fun gifCache(): File {
        val file = File("${app.cacheDir}/temp_gifs")
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

}