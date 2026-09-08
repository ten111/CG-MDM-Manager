package com.example.cloud

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

object GoogleDriveBackupScheduler {

    const val ACTION_AUTO_DRIVE_BACKUP = "com.example.poshan.ACTION_AUTO_DRIVE_BACKUP"
    const val CHANNEL_ID = "pm_poshan_cloud_backup_channel"
    private const val NOTIFICATION_ID = 4001
    private const val REQUEST_CODE = 8801

    fun scheduleAutoBackup(context: Context, config: AutoBackupConfig) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GoogleDriveBackupReceiver::class.java).apply {
            action = ACTION_AUTO_DRIVE_BACKUP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!config.enabled || config.frequency == BackupFrequency.MANUAL_ONLY) {
            alarmManager.cancel(pendingIntent)
            Log.d("BackupScheduler", "Auto backup disabled or manual only. Alarm cancelled.")
            return
        }

        val (hour, minute) = try {
            val parts = config.scheduledTime.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(16, 0) // Default 4:00 PM
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (config.frequency == BackupFrequency.WEEKLY_SATURDAY) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }

        // If time already passed, schedule for next cycle
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            if (config.frequency == BackupFrequency.WEEKLY_SATURDAY) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("BackupScheduler", "Scheduled auto backup for: ${calendar.time}")
        } catch (e: Exception) {
            Log.e("BackupScheduler", "Failed to schedule alarm", e)
        }
    }

    fun cancelAutoBackup(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GoogleDriveBackupReceiver::class.java).apply {
            action = ACTION_AUTO_DRIVE_BACKUP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showBackupNotification(
        context: Context,
        success: Boolean,
        message: String,
        backupId: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_backup", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (success) {
            "CG-MDM Manager: गूगल ड्राइव बैकअप सफल ✓"
        } else {
            "CG-MDM Manager: गूगल ड्राइव बैकअप असफल ⚠"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\nफ़ोल्डर: CG-MDM Manager Mobile\nबैकअप ID: $backupId"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CG-MDM Manager Google Drive Cloud Backups",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Automated Google Drive Cloud Backup notifications and integrity status"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
