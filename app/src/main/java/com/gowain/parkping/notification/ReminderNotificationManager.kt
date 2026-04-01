package com.gowain.parkping.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gowain.parkping.MainActivity
import com.gowain.parkping.R
import com.gowain.parkping.receiver.NotificationActionReceiver
import com.gowain.parkping.util.ReminderIntentActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun showReminder(reminderText: String) {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_parking)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(reminderText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderText))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .addAction(0, context.getString(R.string.notification_action_open), openAppPendingIntent())
            .addAction(
                0,
                context.getString(R.string.notification_action_done),
                actionPendingIntent(ReminderIntentActions.ACTION_MARK_DONE, MARK_DONE_REQUEST_CODE),
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_snooze),
                actionPendingIntent(ReminderIntentActions.ACTION_SNOOZE, SNOOZE_REQUEST_CODE),
            )
            .build()
        notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    fun cancelReminder() {
        notificationManager.cancel(REMINDER_NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NotificationActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val CHANNEL_ID = "parking_reminders"
        private const val REMINDER_NOTIFICATION_ID = 4101
        private const val OPEN_APP_REQUEST_CODE = 2000
        private const val MARK_DONE_REQUEST_CODE = 2001
        private const val SNOOZE_REQUEST_CODE = 2002
    }
}
