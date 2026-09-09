package com.simonecompany.timer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.simonecompany.timer.data.TimerStore

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleOnBoot(context)
            return
        }

        val id = intent.getIntExtra(AlarmScheduler.EXTRA_TIMER_ID, -1)
        if (id == -1) return

        val store = TimerStore(context)
        if (!store.isRunning(id)) return

        val runningIds = store.readRunningTimers().keys.sorted()
        val number = runningIds.indexOf(id) + 1
        store.remove(id)

        val ringIntent = Intent(context, TimerRingService::class.java)
            .putExtra(TimerRingService.EXTRA_TIMER_NUMBER, number.coerceAtLeast(1))
        ContextCompat.startForegroundService(context, ringIntent)
    }

    private fun rescheduleOnBoot(context: Context) {
        val store = TimerStore(context)
        val now = System.currentTimeMillis()
        store.readRunningTimers().forEach { (id, timer) ->
            val (_, deadline) = timer
            if (deadline > now) {
                AlarmScheduler.schedule(context, id, deadline)
            } else {
                store.remove(id)
            }
        }
    }
}