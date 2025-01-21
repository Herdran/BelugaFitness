package com.example.belugafitness

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log

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

    fun streakSetupHelper() {
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
        }
        else if (today == lastDate) {
            streakDoneToday = true
        }
        else {
            streakDoneToday = false
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    private fun isYesterday(date: String?): Boolean {
        if (date == null || date.isEmpty()) return false

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