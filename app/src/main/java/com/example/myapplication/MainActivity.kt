package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.d("MainActivity: onCreate() викликано")

        val btnOpenSecond: Button = findViewById(R.id.btnOpenSecond)
        btnOpenSecond.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("MainActivity: onStart() викликано")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("MainActivity: onResume() викликано")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("MainActivity: onPause() викликано")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("MainActivity: onStop() викликано")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity: onDestroy() викликано")
    }
}