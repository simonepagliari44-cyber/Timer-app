package com.simonecompany.timer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StopwatchState(
    val elapsedMs: Long = 0L,
    val isRunning: Boolean = false
)

class StopwatchViewModel : ViewModel() {

    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)

        job = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _state.value.elapsedMs
            while (true) {
                delay(10L)
                _state.value = _state.value.copy(
                    elapsedMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(isRunning = false)
    }

    fun reset() {
        job?.cancel()
        job = null
        _state.value = StopwatchState()
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
