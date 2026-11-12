package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOpenSecond: Button = findViewById(R.id.btnOpenSecond)
        btnOpenSecond.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}