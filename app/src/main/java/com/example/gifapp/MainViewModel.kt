package com.example.gifapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class MainViewModel : ViewModel() {

    var state by mutableStateOf<MainState>(MainState.Initial)
        private set

    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay

    private val _errorEventRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(setOf())
    val errorEventRelay: StateFlow<Set<ErrorEvent>> get() = _errorEventRelay

    fun updateState(mainState: MainState) {
        state = mainState
    }

    fun showToast(
        id: String = UUID.randomUUID().toString(),
        message: String
    ) {
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
}