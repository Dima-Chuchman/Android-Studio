package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        Timber.d("SecondActivity: onCreate() викликано")

        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("SecondActivity: onStart() викликано")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("SecondActivity: onResume() викликано")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("SecondActivity: onPause() викликано")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("SecondActivity: onStop() викликано")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SecondActivity: onDestroy() викликано")
    }
}