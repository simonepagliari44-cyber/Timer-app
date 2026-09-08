package com.simonecompany.timer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerState(
    val id: Int,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class TimerViewModel : ViewModel() {

    private val _timers = MutableStateFlow<List<TimerState>>(emptyList())
    val timers: StateFlow<List<TimerState>> = _timers.asStateFlow()

    private val _nextId = MutableStateFlow(1)
    private var timerJobs = mutableMapOf<Int, Job?>()

    fun addTimer() {
        val id = _nextId.value
        _nextId.value = id + 1
        _timers.value = _timers.value + TimerState(id = id)
    }

    fun updateSeconds(id: Int, seconds: String) {
        val value = seconds.filter { it.isDigit() }.toIntOrNull() ?: 0
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(totalSeconds = value, remainingSeconds = value, isFinished = false) else it
        }
    }

    fun startTimer(id: Int) {
        val timer = _timers.value.find { it.id == id } ?: return
        if (timer.isRunning || timer.remainingSeconds <= 0) return

        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = true, isFinished = false) else it
        }

        timerJobs[id]?.cancel()
        timerJobs[id] = viewModelScope.launch {
            var remaining = timer.remainingSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _timers.value = _timers.value.map {
                    if (it.id == id) it.copy(remainingSeconds = remaining) else it
                }
            }
            _timers.value = _timers.value.map {
                if (it.id == id) it.copy(isRunning = false, isFinished = true) else it
            }
        }
    }

    fun stopTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs[id] = null
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = false) else it
        }
    }

    fun resetTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs[id] = null
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(
                remainingSeconds = it.totalSeconds,
                isRunning = false,
                isFinished = false
            ) else it
        }
    }

    fun removeTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs.remove(id)
        _timers.value = _timers.value.filter { it.id != id }
    }

    override fun onCleared() {
        super.onCleared()
        timerJobs.values.forEach { it?.cancel() }
    }
}
