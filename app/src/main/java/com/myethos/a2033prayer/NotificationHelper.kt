package com.myethos.a2033prayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Countdown notification channel
            val countdownChannel = NotificationChannel(
                COUNTDOWN_CHANNEL_ID,
                "Prayer Countdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows countdown to prayer time"
                enableVibration(false)
            }

            // Prayer notification channel
            val prayerChannel = NotificationChannel(
                PRAYER_CHANNEL_ID,
                "Prayer Time",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when it's time to pray"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(countdownChannel)
            notificationManager.createNotificationChannel(prayerChannel)
        }
    }

    fun showCountdownNotification(remainingMinutes: Int, remainingSeconds: Int) {
        val timeText = if (remainingMinutes > 0) {
            "$remainingMinutes min ${remainingSeconds}s until prayer"
        } else {
            "$remainingSeconds seconds until prayer"
        }

        val notification = NotificationCompat.Builder(context, COUNTDOWN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("2033 Mission Prayer Time")
            .setContentText(timeText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(COUNTDOWN_NOTIFICATION_ID, notification)
    }

    fun showPrayerNotification() {
        // Create an intent for the "Amen" button
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "PRAYER_AMEN_ACTION"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get default notification sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, PRAYER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("2033 Mission Prayer Time")
            .setContentText("Father, thy kingdom come, thy will be done, on earth as it is in heaven. Come Holy Spirit.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .addAction(R.drawable.ic_amen, "Amen", pendingIntent)
            .setAutoCancel(true)
            .build()

        // Cancel the countdown notification
        notificationManager.cancel(COUNTDOWN_NOTIFICATION_ID)

        // Show the prayer notification
        notificationManager.notify(PRAYER_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val COUNTDOWN_CHANNEL_ID = "prayer_countdown_channel"
        private const val PRAYER_CHANNEL_ID = "prayer_time_channel"

        private const val COUNTDOWN_NOTIFICATION_ID = 1001
        private const val PRAYER_NOTIFICATION_ID = 1002
    }
}