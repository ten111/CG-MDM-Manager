package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ReminderManager {
    const val PREFS_NAME = "poshan_reminder_prefs"
    const val KEY_REMINDER_ENABLED = "key_reminder_enabled"
    const val KEY_REMINDER_HOUR = "key_reminder_hour"
    const val KEY_REMINDER_MINUTE = "key_reminder_minute"
    const val KEY_SOUND_ENABLED = "key_sound_enabled"
    const val KEY_VIBRATE_ENABLED = "key_vibrate_enabled"
    const val KEY_SECOND_REMINDER_ENABLED = "key_second_reminder_enabled"
    const val KEY_SECOND_REMINDER_HOUR = "key_second_reminder_hour"
    const val KEY_SECOND_REMINDER_MINUTE = "key_second_reminder_minute"

    const val CHANNEL_ID = "poshan_daily_attendance_reminders"
    const val NOTIFICATION_ID = 220801
    const val NOTIFICATION_ID_SECOND = 220802

    const val ACTION_DAILY_REMINDER = "com.example.poshan.ACTION_DAILY_REMINDER"
    const val ACTION_SECOND_REMINDER = "com.example.poshan.ACTION_SECOND_REMINDER"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isReminderEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_REMINDER_ENABLED, true)

    fun getReminderHour(context: Context): Int =
        getPrefs(context).getInt(KEY_REMINDER_HOUR, 12)

    fun getReminderMinute(context: Context): Int =
        getPrefs(context).getInt(KEY_REMINDER_MINUTE, 30)

    fun isSoundEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)

    fun isVibrateEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_VIBRATE_ENABLED, true)

    fun isSecondReminderEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SECOND_REMINDER_ENABLED, true)

    fun getSecondReminderHour(context: Context): Int =
        getPrefs(context).getInt(KEY_SECOND_REMINDER_HOUR, 15)

    fun getSecondReminderMinute(context: Context): Int =
        getPrefs(context).getInt(KEY_SECOND_REMINDER_MINUTE, 0)

    fun saveSettings(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        sound: Boolean,
        vibrate: Boolean,
        secondEnabled: Boolean,
        secondHour: Int,
        secondMinute: Int
    ) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_REMINDER_ENABLED, enabled)
            putInt(KEY_REMINDER_HOUR, hour)
            putInt(KEY_REMINDER_MINUTE, minute)
            putBoolean(KEY_SOUND_ENABLED, sound)
            putBoolean(KEY_VIBRATE_ENABLED, vibrate)
            putBoolean(KEY_SECOND_REMINDER_ENABLED, secondEnabled)
            putInt(KEY_SECOND_REMINDER_HOUR, secondHour)
            putInt(KEY_SECOND_REMINDER_MINUTE, secondMinute)
            apply()
        }

        if (enabled) {
            scheduleReminders(context)
        } else {
            cancelReminders(context)
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val name = "दैनिक उपस्थिति रिमाइंडर (Daily Attendance Reminders)"
                val descriptionText = "PM पोषण मध्याह्न भोजन एवं छात्र उपस्थिति दर्ज करने हेतु दैनिक सूचनाएं"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 350, 150, 350)
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    if (soundUri != null) {
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build()
                        setSound(soundUri, audioAttributes)
                    }
                }
                val notificationManager: NotificationManager? =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            } catch (t: Throwable) {
                // Ignore notification channel creation error
            }
        }
    }

    fun scheduleReminders(context: Context) {
        try {
            createNotificationChannel(context)
            if (!isReminderEnabled(context)) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // Schedule Primary Reminder
            val primaryHour = getReminderHour(context)
            val primaryMinute = getReminderMinute(context)
            scheduleSpecificAlarm(
                context = context,
                alarmManager = alarmManager,
                action = ACTION_DAILY_REMINDER,
                requestCode = 1001,
                targetHour = primaryHour,
                targetMinute = primaryMinute
            )

            // Schedule Second/Afternoon Reminder if enabled
            if (isSecondReminderEnabled(context)) {
                val secondHour = getSecondReminderHour(context)
                val secondMinute = getSecondReminderMinute(context)
                scheduleSpecificAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    action = ACTION_SECOND_REMINDER,
                    requestCode = 1002,
                    targetHour = secondHour,
                    targetMinute = secondMinute
                )
            } else {
                cancelSpecificAlarm(context, alarmManager, ACTION_SECOND_REMINDER, 1002)
            }
        } catch (t: Throwable) {
            // Ignore alarm scheduling error
        }
    }

    private fun scheduleSpecificAlarm(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int,
        targetHour: Int,
        targetMinute: Int
    ) {
        val istZone = TimeZone.getTimeZone("Asia/Kolkata")
        val now = Calendar.getInstance(istZone)
        val target = Calendar.getInstance(istZone).apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time has already passed today, schedule for tomorrow
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        target.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        target.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Throwable) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            } catch (t: Throwable) {
                // Ignore fallback error
            }
        }
    }

    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelSpecificAlarm(context, alarmManager, ACTION_DAILY_REMINDER, 1001)
        cancelSpecificAlarm(context, alarmManager, ACTION_SECOND_REMINDER, 1002)
    }

    private fun cancelSpecificAlarm(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int
    ) {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelTodayNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(NOTIFICATION_ID_SECOND)
    }

    fun showTestNotification(context: Context, isHindi: Boolean = true) {
        createNotificationChannel(context)
        val istZone = TimeZone.getTimeZone("Asia/Kolkata")
        val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).apply {
            timeZone = istZone
        }.format(Date())

        val title = if (isHindi) "🔔 PM पोषण: दैनिक उपस्थिति रिमाइंडर (Test Alert)"
        else "🔔 PM POSHAN: Daily Attendance Reminder (Test Alert)"

        val message = if (isHindi)
            "यह एक टेस्ट रिमाइंडर है। आज ($todayStr) की मध्याह्न भोजन एवं छात्र उपस्थिति समय पर दर्ज करें।"
        else
            "This is a test reminder. Please record today's ($todayStr) Mid-Day Meal and student attendance."

        sendNotification(context, title, message, NOTIFICATION_ID)
    }

    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO_MEAL", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val sound = isSoundEnabled(context)
        val vibrate = isVibrateEnabled(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (sound) {
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }

        if (vibrate) {
            builder.setVibrate(longArrayOf(0, 350, 150, 350))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }
}
