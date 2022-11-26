package com.example.gifapp

import android.view.View
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.gifapp.use_cases.PixelCopyJob
import com.example.gifapp.use_cases.PixelCopyJobUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel : ViewModel() {

    private val dispatcher = IO
    private val pixelCopy: PixelCopyJob = PixelCopyJobUseCase()

    var state by mutableStateOf<MainState>(MainState.Initial)
        private set

    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay

    private val _errorEventRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(setOf())
    val errorEventRelay: StateFlow<Set<ErrorEvent>> get() = _errorEventRelay

    fun captureScreenshot(
        view: View,
        window: Window
    ) {
        val screenshotState = state
        check(screenshotState is MainState.DisplayBackgroundAsset) { "Invalid state: $screenshotState" }
        CoroutineScope(dispatcher).launch {
            val result = pixelCopy.execute(
                capturingViewBonds = screenshotState.capturingViewBounds,
                view = view,
                window = window
            )
            when (result) {
                is PixelCopyJob.PixelCopyJobState.Done -> {
                    state = screenshotState.copy(capturedBitmap = result.bitmap)
                }
                is PixelCopyJob.PixelCopyJobState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = result.message
                        )
                    )
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