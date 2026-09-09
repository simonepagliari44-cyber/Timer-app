package com.simonecompany.timer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simonecompany.timer.data.TimerStore
import com.simonecompany.timer.notification.TimerNotifier
import com.simonecompany.timer.service.AlarmScheduler
import com.simonecompany.timer.service.TimerRingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class TimerState(
    val id: Int,
    val hours: String = "0",
    val minutes: String = "0",
    val seconds: String = "0",
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _timers = MutableStateFlow<List<TimerState>>(emptyList())
    val timers: StateFlow<List<TimerState>> = _timers.asStateFlow()

    private var _nextId = 1
    private var timerJobs = mutableMapOf<Int, Job?>()

    private val context get() = getApplication<Application>()
    private val store = TimerStore(context)

    init {
        restoreRunningTimers()
    }

    private fun restoreRunningTimers() {
        val running = store.readRunningTimers()
        if (running.isEmpty()) return

        val now = System.currentTimeMillis()
        _nextId = (running.keys.max() + 1).coerceAtLeast(_nextId)

        val restored = running.map { (id, timer) ->
            val (total, deadline) = timer
            val remaining = ceil((deadline - now) / 1000.0).toInt().coerceAtLeast(0)
            if (remaining > 0) {
                AlarmScheduler.schedule(context, id, deadline)
            } else {
                store.remove(id)
            }
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            TimerState(
                id = id,
                hours = hours.toString(),
                minutes = minutes.toString(),
                seconds = seconds.toString(),
                totalSeconds = total,
                remainingSeconds = remaining,
                isRunning = remaining > 0
            )
        }
        _timers.value = restored
    }

    fun addTimer() {
        val id = _nextId
        _nextId += 1
        _timers.value = _timers.value + TimerState(id = id)
    }

    fun updateDuration(id: Int, hours: String, minutes: String, seconds: String) {
        val h = hours.filter { it.isDigit() }.toIntOrNull() ?: 0
        val m = minutes.filter { it.isDigit() }.toIntOrNull() ?: 0
        val s = seconds.filter { it.isDigit() }.toIntOrNull() ?: 0
        val total = h * 3600 + m * 60 + s
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                totalSeconds = total,
                remainingSeconds = total,
                isFinished = false
            ) else it
        }
    }

    fun startTimer(id: Int) {
        val timer = _timers.value.find { it.id == id } ?: return
        if (timer.isRunning || timer.remainingSeconds <= 0) return

        val deadline = System.currentTimeMillis() + timer.remainingSeconds * 1000L
        val number = timerNumber(id)

        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = true, isFinished = false) else it
        }
        store.saveRunningTimer(id, timer.totalSeconds, deadline)
        AlarmScheduler.schedule(context, id, deadline)
        TimerNotifier.showRunning(context, id, number, timer.remainingSeconds)

        timerJobs[id]?.cancel()
        timerJobs[id] = viewModelScope.launch {
            var lastShown = timer.remainingSeconds
            while (isActive) {
                val remaining = ceil((deadline - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(0)
                _timers.value = _timers.value.map {
                    if (it.id == id) it.copy(remainingSeconds = remaining) else it
                }
                if (remaining != lastShown) {
                    lastShown = remaining
                    TimerNotifier.showRunning(context, id, timerNumber(id), remaining)
                }
                if (remaining <= 0) break
                delay(250L)
            }
            finishTimer(id)
        }
    }

    private fun timerNumber(id: Int): Int =
        _timers.value.indexOfFirst { it.id == id }.let { if (it >= 0) it + 1 else id }

    fun stopTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs[id] = null
        AlarmScheduler.cancel(context, id)
        store.remove(id)
        TimerNotifier.cancel(context, id)
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = false) else it
        }
    }

    fun resetTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs[id] = null
        AlarmScheduler.cancel(context, id)
        store.remove(id)
        TimerNotifier.cancel(context, id)
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
        AlarmScheduler.cancel(context, id)
        store.remove(id)
        TimerNotifier.cancel(context, id)
        _timers.value = _timers.value.filter { it.id != id }
    }

    private fun finishTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs[id] = null
        AlarmScheduler.cancel(context, id)
        val shouldRing = store.isRunning(id)
        store.remove(id)
        TimerNotifier.cancel(context, id)
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(remainingSeconds = 0, isRunning = false, isFinished = true) else it
        }
        if (shouldRing) startRing(id)
    }

    private fun startRing(id: Int) {
        val index = _timers.value.indexOfFirst { it.id == id }
        val number = if (index >= 0) index + 1 else id

        val intent = Intent(context, TimerRingService::class.java)
            .putExtra(TimerRingService.EXTRA_TIMER_NUMBER, number)
        ContextCompat.startForegroundService(context, intent)
    }
}