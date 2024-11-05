package com.example.belugafitness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        val startDetectionButton: Button = findViewById(R.id.button2)
        startDetectionButton.setOnClickListener {
            val intent = Intent(this, DetectionActivity::class.java)
            startActivity(intent)
        }
    }
}