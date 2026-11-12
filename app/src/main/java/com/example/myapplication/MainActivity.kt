package com.example.myapplication

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Підписка на оновлення секунд таймера
        viewModel.timerLiveData.observe(this) { seconds ->
            binding.tvTimer.text = "$seconds с"
        }

        // Підписка на стан роботи таймера (блокування кнопки Старт під час відліку)
        viewModel.isTimerRunning.observe(this) { isRunning ->
            binding.btnStart.isEnabled = !isRunning
        }

        binding.btnStart.setOnClickListener {
            val inputSeconds = binding.etSeconds.text.toString().trim().toLongOrNull()
            val duration = if (inputSeconds != null && inputSeconds > 0L) {
                inputSeconds
            } else {
                TimerModel.DEFAULT_DURATION_SECONDS
            }
            viewModel.startTimer(duration)
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetTimer()
        }
    }
}