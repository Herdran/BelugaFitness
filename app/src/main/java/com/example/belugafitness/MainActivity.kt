package com.example.belugafitness

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest

private const val notificationPermissionRequest = 1001

class MainActivity : AppCompatActivity() {

    private lateinit var streakTextView: TextView
    private val PREFS_NAME = "StreakPrefs"
    private val STREAK_KEY = "streak"
    private val LAST_DATE_KEY = "lastDate"
    private var streakDoneToday: Boolean = false
    var streak: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)
        streakTextView = findViewById(R.id.streak_test_view_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        createNotificationChannel(this)

        // Request notification permissions (if Android 13 or above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notificationPermissionRequest
                )
            } else {
                scheduleDailyNotifications(this)
            }
        } else {
            scheduleDailyNotifications(this)
        }

        val startDetectionButton: Button = findViewById(R.id.button2)
        startDetectionButton.setOnClickListener {
            val intent = Intent(this, WorkoutActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        setupStreak()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequest) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleDailyNotifications(this)
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "daily_notifications",
            "Daily Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for daily notifications"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleDailyNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule for 12:00
        val intent12 = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_type", "morning")
        }
        val pendingIntent12 = PendingIntent.getBroadcast(
            context, 0, intent12, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar12 = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (calendar12.before(Calendar.getInstance())) {
            calendar12.add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar12.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent12
        )

        // Schedule for 23:00
        val intent23 = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_type", "night")
        }
        val pendingIntent23 = PendingIntent.getBroadcast(
            context, 1, intent23, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar23 = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (calendar23.before(Calendar.getInstance())) {
            calendar23.add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar23.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent23
        )
    }

    private fun setupStreak() {
        streakSetupHelper()
        if (streakDoneToday) {
            val leftDrawable = streakTextView.compoundDrawables[0]
            streakTextView.setTextColor(Color.parseColor("#FFC107"))
            leftDrawable?.let {
                it.clearColorFilter()
                streakTextView.setCompoundDrawablesWithIntrinsicBounds(it, null, null, null)
            }
        } else {
            val leftDrawable = streakTextView.compoundDrawables[0]
            streakTextView.setTextColor(Color.GRAY)
            leftDrawable?.let {
                it.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
                streakTextView.setCompoundDrawablesWithIntrinsicBounds(it, null, null, null)
            }

        }
        streakTextView.text = "$streak"
    }

    private fun streakSetupHelper() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        streak = sharedPreferences.getInt(STREAK_KEY, 0)
        val lastDate = sharedPreferences.getString(LAST_DATE_KEY, "")

        val today = getCurrentDate()

        if (lastDate != today && !isYesterday(lastDate)) {
            streak = 0
            with(sharedPreferences.edit()) {
                putInt(STREAK_KEY, streak)
                apply()
            }
        } else if (today == lastDate) {
            streakDoneToday = true
        } else {
            streakDoneToday = false
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    private fun isYesterday(date: String?): Boolean {
        if (date.isNullOrEmpty()) return false

        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar

        val dateParts = date.split("-")
        if (dateParts.size != 3) return false

        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // Months are 0-indexed
        val day = dateParts[2].toInt()

        calendar.set(year, month, day)
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }
}