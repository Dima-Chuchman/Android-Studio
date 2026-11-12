package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.d("MainActivity успішно запущена!")

        val btnCount: Button = findViewById(R.id.btnCount)
        btnCount.setOnClickListener {
            val app = application as MyApp
            app.startCount++
            Timber.d("Поточне значення лічильника: ${app.startCount}")
        }
    }
}