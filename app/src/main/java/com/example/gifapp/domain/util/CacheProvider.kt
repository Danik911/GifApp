package com.example.gifapp.domain.util

import android.app.Application
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface CacheProvider {

    /**
     * Provides the directory where cached gif are kept
     */
    fun gifCache(): File
}
@Singleton
class RealCacheProvided
@Inject
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
@Module
@InstallIn(SingletonComponent::class)
abstract  class  CacheProviderModule{
    @Binds
    abstract fun provideCacheProvider(cacheProvider: RealCacheProvided): CacheProvider
}