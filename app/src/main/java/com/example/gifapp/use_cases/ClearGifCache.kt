package com.example.gifapp.use_cases

import com.example.gifapp.domain.DataState
import com.example.gifapp.domain.DataState.Loading.LoadingState.Active
import com.example.gifapp.domain.DataState.Loading.LoadingState.Idle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ClearGifCache {
    fun execute(): Flow<DataState<Unit>>
}

/**
 * Use-case for clearing all the cached files from the path via [CacheProvider]
 */
class ClearGifCacheUseCase constructor(
    private val cacheProvided: CacheProvider
) : ClearGifCache {
    override fun execute(): Flow<DataState<Unit>> = flow {
        emit(DataState.Loading(Active()))
        try {
            clearGifCache(cacheProvided = cacheProvided)
            emit(DataState.Data(Unit))// Done
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CLEAR_CACHED_FILES_ERROR))
        }
        emit(DataState.Loading(Idle))
    }

    companion object {
        const val CLEAR_CACHED_FILES_ERROR = "An error occurred deleting the cached files"

        /**
         * Clears all the cached files from the path provided via[CacheProvider]
         */
        private fun clearGifCache(
            cacheProvided: CacheProvider
        ) {
            val internalStorageDirectory = cacheProvided.gifCache()
            val files = internalStorageDirectory.listFiles()
            files?.forEach { file ->
                file.delete()
            }
        }
    }

}

