package com.simonecompany.timer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.simonecompany.timer.R

class TimerRingService : Service() {

    private var ringtone: Ringtone? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val number = intent?.getIntExtra(EXTRA_TIMER_NUMBER, 0) ?: 0
        stopRinging()

        val notification = buildNotification(number)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)

        startRinging()
        return START_NOT_STICKY
    }

    private fun startRinging() {
        ringtone = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_ALARM_ALERT_URI)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
        }
        ringtone?.play()
        vibrate(700)
    }

    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
    }

    private fun vibrate(millis: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }

    private fun buildNotification(number: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            number,
            Intent(this, TimerRingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle("Timer $number completato")
            .setContentText("Il tempo è scaduto!")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_stat_clock, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(0)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Suoneria quando un timer scade"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "timer_ring_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.simonecompany.timer.action.STOP_RING"
        const val EXTRA_TIMER_NUMBER = "extra_timer_number"
    }
}