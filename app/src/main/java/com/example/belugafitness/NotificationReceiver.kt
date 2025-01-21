package com.example.belugafitness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.Manifest

class NotificationReceiver : BroadcastReceiver() {
    private val PREFS_NAME = "StreakPrefs"
    private val LAST_DATE_KEY = "lastDate"
    private val STREAK_KEY = "streak"

    override fun onReceive(context: Context, intent: Intent?) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val lastDate = sharedPreferences.getString(LAST_DATE_KEY, "")
        val streak = sharedPreferences.getInt(STREAK_KEY, 0)
        val currentDate = getCurrentDate()

        if (lastDate != currentDate) {
            val message: String
            if (streak == 0){
                message = "The Beluga is waiting for you to workout together!!!"
            }
            else {
                val notificationType = intent?.getStringExtra("notification_type")
                message = when (notificationType) {
                    "morning" -> "Don't forget, your streak is waiting for you!"
                    "night" -> "Only an hour left! Can you make it in time?"
                    else -> "Test notification message"
                }
            }

            sendNotification(context, message)
        }
    }

    fun sendNotification(context: Context, message: String) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val notificationId = (0..999999).random()
            val builder = NotificationCompat.Builder(context, "daily_notifications")
                .setSmallIcon(R.drawable.beluga) // Replace with your app icon
                .setContentTitle("Daily Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } else {
            Log.w("NotificationReceiver", "Notification permission not granted.")
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }
}