package com.simonecompany.timer.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.simonecompany.timer.MainActivity
import com.simonecompany.timer.R

object TimerNotifier {

    private const val RUNNING_CHANNEL_ID = "timer_running_channel"

    fun showRunning(context: Context, id: Int, number: Int, remainingSeconds: Int) {
        ensureChannel(context)
        val nm = context.getSystemService(NotificationManager::class.java)

        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getActivity(
            context,
            id + 10_000,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_STOP_TIMER_ID, id)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle("Timer $number in esecuzione")
            .setContentText("Restano ${formatTime(remainingSeconds)}")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_stat_clock, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        nm.notify(id, notification)
    }

    fun cancel(context: Context, id: Int) {
        context.getSystemService(NotificationManager::class.java).cancel(id)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            RUNNING_CHANNEL_ID,
            "Timer in esecuzione",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Timer attivi con pulsante stop"
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hours, mins, secs)
    }

    const val EXTRA_STOP_TIMER_ID = "extra_stop_timer_id"
}