package com.example.gifapp

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gifapp.domain.DataState
import com.example.gifapp.domain.DataState.Loading.LoadingState.Active
import com.example.gifapp.domain.DataState.Loading.LoadingState.Idle
import com.example.gifapp.domain.util.RealVersionProvider
import com.example.gifapp.use_cases.*
import com.example.gifapp.use_cases.CaptureBitmapsUseCase.Companion.CAPTURE_BITMAP_ERROR
import com.example.gifapp.use_cases.CaptureBitmapsUseCase.Companion.CAPTURE_BITMAP_SUCCESS
import com.example.gifapp.use_cases.SaveGifToExternalStorageUseCase.Companion.SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import timber.log.Timber
import java.util.*

class MainViewModel : ViewModel() {

    private val dispatcher = IO
    private val pixelCopy: PixelCopyJob = PixelCopyJobUseCase()
    private val mainDispatcher = Dispatchers.Main
    private val versionProvider = RealVersionProvider()
    private val captureBitmaps: CaptureBitmaps =
        CaptureBitmapsUseCase(
            pixelCopyJob = pixelCopy,
            mainDispatcher = mainDispatcher,
            versionProvider = versionProvider
        )
    private var cacheProvider: CacheProvider? = null
    private val saveGifToExternalStorage: SaveGifToExternalStorage =
        SaveGifToExternalStorageUseCase(
            versionProvider = versionProvider
        )

    var state by mutableStateOf<MainState>(MainState.Initial)
        private set

    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay

    private val _errorEventRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(setOf())
    val errorEventRelay: StateFlow<Set<ErrorEvent>> get() = _errorEventRelay

    fun setCacheProvider(cacheProvider: CacheProvider) {
        this.cacheProvider = cacheProvider
    }

    fun saveGif(
        launchPermissionRequest: () -> Unit,
        checkFilePermission: () -> Boolean,
        contentResolver: ContentResolver,
        context: Context
    ) {
        check(state is MainState.DisplayGif) { "saveGif: Invalid state: $state" }
        //Ask permission if necessary
        if (versionProvider.provideVersion() < Build.VERSION_CODES.Q && !checkFilePermission()) {
            launchPermissionRequest()
            return
        }
        val uriToSave = (state as MainState.DisplayGif).gifUri ?: throw Exception(
            SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR
        )
        saveGifToExternalStorage.execute(
            contentResolver = contentResolver,
            cachedUri = uriToSave,
            context = context,
            checkFilePermission = checkFilePermission
        ).onEach { dataState ->
            when (dataState) {
                is DataState.Data -> showToast(message = "Saved")
                is DataState.Loading -> {
                    updateState(
                        (state as MainState.DisplayGif).copy(loadingState = dataState.loadingState)
                    )
                }
                is DataState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                }
            }
        }.onCompletion {
            // Whether or not this succeeds we want to clear the cache
            // Because if something goes wrong we want to reset anyway
            clearCachedFiles()

            // reset state to display the selected background
            state = MainState.DisplayBackgroundAsset(
                backgroundAssetUri = (state as MainState.DisplayGif).backgroundAssetUri
            )
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    private fun buildGif(
        contentResolver: ContentResolver
    ) {
        check(state is MainState.DisplayBackgroundAsset) { "buildGif: Invalid state:  $state" }
        val capturedBitmaps = (state as MainState.DisplayBackgroundAsset).capturedBitmaps
        check(capturedBitmaps.isNotEmpty()) { "You have no bitmaps to create a GIF" }
        updateState(
            (state as MainState.DisplayBackgroundAsset).copy(loadingState = Active())
        )
        // TODO: This will be injected into the ViewModel later
        val buildGif: BuildGif = BuildGifUseCase(
            versionProvider = versionProvider,
            cacheProvider = cacheProvider!! // TODO: !! will be removed
        )
        buildGif.execute(
            contentResolver = contentResolver,
            bitmaps = capturedBitmaps
        ).onEach { dataState: DataState<BuildGif.BuildGifResult> ->
            when (dataState) {
                is DataState.Data -> {
                    (state as MainState.DisplayBackgroundAsset).let {
                        val gifSize = dataState.data?.gifSize ?: 0
                        val gifUri = dataState.data?.uri
                        updateState(
                            MainState.DisplayGif(
                                gifUri = gifUri,
                                originalGifSize = gifSize,
                                backgroundAssetUri = it.backgroundAssetUri
                            )
                        )

                    }

                }
                is DataState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                    updateState(
                        (state as MainState.DisplayBackgroundAsset).copy(loadingState = Idle)
                    )
                }
                is DataState.Loading -> {
                    // Need to check here since there is a state change to DisplayGif and loading
                    // emissions can technically still come after the job is complete
                    if (state is MainState.DisplayBackgroundAsset) {
                        updateState(
                            (state as MainState.DisplayBackgroundAsset)
                                .copy(loadingState = dataState.loadingState)
                        )
                    }
                }
            }
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    fun runBitmapCaptureJob(
        contentResolver: ContentResolver,
        view: View,
        window: Window
    ) {

        check(state is MainState.DisplayBackgroundAsset) { "Invalid state: $state" }
        updateState(
            (state as MainState.DisplayBackgroundAsset).copy(
                bitmapCaptureLoadingState = Active(
                    0f
                )
            )
        )
        // We need a way to stop the job if a user press "STOP". So I created a Job for this
        val bitmapJob = Job()
        // Create convenience function for checking if the user pressed "STOP"
        val checkShouldCancelJob: (MainState) -> Unit = { mainState ->
            val shouldCancel = when (mainState) {
                is MainState.DisplayBackgroundAsset -> {
                    mainState.bitmapCaptureLoadingState !is Active
                }
                else -> true
            }
            if (shouldCancel) {
                bitmapJob.cancel(CAPTURE_BITMAP_SUCCESS)
            }
        }
        // Execute the use-case
        captureBitmaps.execute(
            capturingViewBounds = (state as MainState.DisplayBackgroundAsset).capturingViewBounds,
            view = view,
            window = window
        ).onEach { dataState: DataState<List<Bitmap>> ->
            // If the user hits the "STOP" button, complete the job by cancelling
            // Also cancel if there was some kind of state change
            checkShouldCancelJob(state)
            when (dataState) {
                is DataState.Data -> {
                    dataState.data?.let { bitmaps: List<Bitmap> ->
                        updateState(
                            (state as MainState.DisplayBackgroundAsset)
                                .copy(capturedBitmaps = bitmaps)
                        )
                    }
                }
                is DataState.Error -> {
                    // For this use-case, if an error occurs we need to stop the job
                    // Otherwise it will keep trying to capture bitmaps and failing over and over
                    bitmapJob.cancel(CAPTURE_BITMAP_ERROR)
                    updateState(
                        (state as MainState.DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = Idle
                        )
                    )
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                }
                is DataState.Loading -> {
                    updateState(
                        (state as MainState.DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = dataState.loadingState
                        )
                    )
                }
            }
        }
            .flowOn(dispatcher)
            .launchIn(viewModelScope + bitmapJob)
            .invokeOnCompletion { throwable ->
                updateState(
                    (state as MainState.DisplayBackgroundAsset)
                        .copy(bitmapCaptureLoadingState = Idle)
                )
                val onSuccess: () -> Unit = {
                    buildGif(contentResolver = contentResolver)
                }
                // If the throwable is null OR the message = CAPTURE_BITMAP_SUCCESS, it was successful
                when (throwable) {
                    null -> onSuccess()
                    else -> {
                        if (throwable.message == CAPTURE_BITMAP_SUCCESS) {
                            onSuccess()
                        } else { // Id an error occurs, do not try to build the gif
                            publishErrorEvent(
                                ErrorEvent(
                                    id = UUID.randomUUID().toString(),
                                    message = throwable.message ?: CAPTURE_BITMAP_ERROR
                                )
                            )
                        }
                    }
                }
            }
    }

    private fun clearCachedFiles() {
        // TODO: It will be injected with Hilt
        val clearGifCache: ClearGifCache = ClearGifCacheUseCase(
            cacheProvided = cacheProvider!! // TODO: get rid of !! later
        )
        clearGifCache.execute().onEach { _ ->
            // Do not update UI here. Should just succeed of fail silently
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    fun deleteGif() {
        clearCachedFiles()
        check(state is MainState.DisplayGif) { "deleteGif: Invalid state: $state" }
        state =
            MainState.DisplayBackgroundAsset(
                backgroundAssetUri = (state as MainState.DisplayGif).backgroundAssetUri
            )
    }


    fun updateState(mainState: MainState) {
        state = mainState
    }

    fun showToast(
        id: String = UUID.randomUUID().toString(),
        message: String
    ) {
        Timber.d("Show toast triggered")
        _toastEventRelay.tryEmit(
            ToastEvent(
                id = id,
                message = message
            )
        )
    }

    private fun publishErrorEvent(errorEvent: ErrorEvent) {
        val current = _errorEventRelay.value.toMutableSet()
        current.add(errorEvent)
        _errorEventRelay.value = current
    }

    fun clearErrorEvents() {
        _errorEventRelay.value = setOf()
    }

    fun stopBitmapCaptureJob() {
        Timber.d("stopBitmap triggered")
        updateState((state as MainState.DisplayBackgroundAsset).copy(bitmapCaptureLoadingState = Idle))
    }
}