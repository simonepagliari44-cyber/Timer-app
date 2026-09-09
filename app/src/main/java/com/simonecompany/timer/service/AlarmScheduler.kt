package com.simonecompany.timer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {

    fun schedule(context: Context, id: Int, deadline: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, id)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pendingIntent)
        }
    }

    fun cancel(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, id))
    }

    private fun pendingIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, TimerAlarmReceiver::class.java).putExtra(EXTRA_TIMER_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    const val EXTRA_TIMER_ID = "extra_timer_id"
}