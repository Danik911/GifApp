package com.example.gifapp

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gifapp.domain.DataState
import com.example.gifapp.use_cases.CaptureBitmaps
import com.example.gifapp.use_cases.CaptureBitmapsUseCase
import com.example.gifapp.use_cases.CaptureBitmapsUseCase.Companion.CAPTURE_BITMAP_ERROR
import com.example.gifapp.use_cases.CaptureBitmapsUseCase.Companion.CAPTURE_BITMAP_SUCCESS
import com.example.gifapp.use_cases.PixelCopyJob
import com.example.gifapp.use_cases.PixelCopyJobUseCase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import java.util.*

class MainViewModel : ViewModel() {

    private val dispatcher = IO
    private val pixelCopy: PixelCopyJob = PixelCopyJobUseCase()
    private val captureBitmaps: CaptureBitmaps = CaptureBitmapsUseCase(pixelCopy)

    var state by mutableStateOf<MainState>(MainState.Initial)
        private set

    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay

    private val _errorEventRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(setOf())
    val errorEventRelay: StateFlow<Set<ErrorEvent>> get() = _errorEventRelay

    fun runBitmapCaptureJob(
        view: View,
        window: Window
    ) {
        val bitmapState = state
        check(bitmapState is MainState.DisplayBackgroundAsset) { "Invalid state: $bitmapState" }
        // We need a way to stop the job if a user press "STOP". So I created a Job for this
        val bitmapJob = Job()
        // Create convenience function for checking if the user pressed "STOP"
        val checkShouldCancelJob: () -> Unit = {
            // TODO("Add logic for determining if state is correct before cancelling")
            // bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
        }
        // Execute the use-case
        captureBitmaps.execute(
            capturingViewBounds = bitmapState.capturingViewBounds,
            view = view,
            window = window
        ).onEach { dataState: DataState<List<Bitmap>> ->
            // If the user hits the "STOP" button, complete the job by cancelling
            checkShouldCancelJob()
            when (dataState) {
                is DataState.Data -> {
                    dataState.data?.let { bitmaps: List<Bitmap> ->
                        state = bitmapState.copy(capturedBitmaps = bitmaps)
                    }
                }
                is DataState.Error -> {
                    // For this use-case, if an error occurs we need to stop the job
                    // Otherwise it will keep trying to capture bitmaps and failing over and over
                    bitmapJob.cancel(CAPTURE_BITMAP_ERROR)
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                }
            }
        }
            .flowOn(dispatcher)
            .launchIn(viewModelScope + bitmapJob)
            .invokeOnCompletion { throwable ->
                // TODO("Update loading state")
                val onSuccess: () -> Unit = {
                    // TODO("Build the gif from list of captured bitmaps")
                    val newState = state
                    if (newState is MainState.DisplayBackgroundAsset) {
                        Log.d(
                            "MainViewModel",
                            "number of captured bitmaps:${newState.capturedBitmaps.size}"
                        )
                    }
                }
                // If the throwable is null OR the message = CAPTURE_BITMAP_SUCCESS, it was successful
                when (throwable) {
                    null -> onSuccess()
                    else -> {
                        if (throwable.message == CAPTURE_BITMAP_SUCCESS) {
                            onSuccess()
                        } else {
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


    fun updateState(mainState: MainState) {
        state = mainState
    }

    fun showToast(
        id: String = UUID.randomUUID().toString(), message: String
    ) {
        _toastEventRelay.tryEmit(
            ToastEvent(
                id = id, message = message
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
}