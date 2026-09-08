package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val istZone = TimeZone.getTimeZone("Asia/Kolkata")
        val calendar = Calendar.getInstance(istZone)

        // Sunday check (Sunday = 1 in Java Calendar)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) {
            // Re-arm alarms for next day and exit
            ReminderManager.scheduleReminders(context)
            return
        }

        val todayDate = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).apply {
            timeZone = istZone
        }.format(Date())

        val isSecondReminder = intent.action == ReminderManager.ACTION_SECOND_REMINDER
        val notificationId = if (isSecondReminder) ReminderManager.NOTIFICATION_ID_SECOND else ReminderManager.NOTIFICATION_ID

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val existingRecord = db.dailyMealRecordDao().getRecordForDateDirect(todayDate)

                // If meal is already recorded and submitted for today, no need to alert!
                if (existingRecord != null && existingRecord.mealServed && existingRecord.studentsServed > 0) {
                    // Already filled!
                    ReminderManager.scheduleReminders(context)
                    return@launch
                }

                // Title and message in Hindi & English
                val title = if (isSecondReminder) {
                    "⚠️ ध्यान दें: आज की उपस्थिति दर्ज नहीं हुई!"
                } else {
                    "🔔 PM पोषण: दैनिक भोजन एवं उपस्थिति दर्ज करें"
                }

                val message = if (isSecondReminder) {
                    "आज $todayDate की मध्याह्न भोजन एवं छात्र उपस्थिति अभी तक दर्ज नहीं की गई है। कृपया तुरंत विवरण दर्ज करें।"
                } else {
                    "आज $todayDate की मध्याह्न भोजन एवं छात्र उपस्थिति दर्ज करने का समय हो गया है। कृपया विवरण दर्ज करें।"
                }

                ReminderManager.sendNotification(
                    context = context,
                    title = title,
                    message = message,
                    notificationId = notificationId
                )

                // Reschedule for next day
                ReminderManager.scheduleReminders(context)
            } catch (e: Exception) {
                // Ensure reschedule even if DB read fails
                ReminderManager.scheduleReminders(context)
            }
        }
    }
}
