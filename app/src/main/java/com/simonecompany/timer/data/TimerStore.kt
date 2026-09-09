package com.simonecompany.timer.data

import android.content.Context
import android.content.SharedPreferences

class TimerStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("timers", Context.MODE_PRIVATE)

    fun saveRunningTimer(id: Int, totalSeconds: Int, deadline: Long) {
        prefs.edit()
            .putInt(totalKey(id), totalSeconds)
            .putLong(deadlineKey(id), deadline)
            .apply()
    }

    fun readRunningTimers(): Map<Int, Pair<Int, Long>> =
        prefs.all.keys
            .filter { it.startsWith(PREFIX_TOTAL) }
            .mapNotNull { key ->
                val id = key.removePrefix(PREFIX_TOTAL).toIntOrNull() ?: return@mapNotNull null
                val total = prefs.getInt(key, -1)
                val deadline = prefs.getLong(deadlineKey(id), -1L)
                if (total > 0 && deadline > 0) id to (total to deadline) else null
            }
            .toMap()

    fun isRunning(id: Int): Boolean = prefs.contains(deadlineKey(id))

    fun remove(id: Int) {
        prefs.edit()
            .remove(totalKey(id))
            .remove(deadlineKey(id))
            .apply()
    }

    private fun totalKey(id: Int) = "$PREFIX_TOTAL$id"
    private fun deadlineKey(id: Int) = "$PREFIX_DEADLINE$id"

    companion object {
        private const val PREFIX_TOTAL = "total_"
        private const val PREFIX_DEADLINE = "deadline_"
    }
}