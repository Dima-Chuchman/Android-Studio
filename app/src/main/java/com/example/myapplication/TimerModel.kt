package com.example.myapplication

import android.os.CountDownTimer
import timber.log.Timber

class TimerModel {

    private var countDownTimer: CountDownTimer? = null

    fun start(
        durationSeconds: Long = DEFAULT_DURATION_SECONDS,
        onTick: (Long) -> Unit,
        onFinish: () -> Unit
    ) {
        // Скасовуємо попередній таймер, якщо він ще працює
        stop()

        // Одразу передаємо початкове значення секунд
        onTick(durationSeconds)

        countDownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished + 500L) / 1000L
                Timber.d("TimerModel: залишилось $secondsRemaining с")
                onTick(secondsRemaining)
            }

            override fun onFinish() {
                Timber.d("TimerModel: відлік завершено")
                countDownTimer = null
                onTick(0L)
                onFinish()
            }
        }.start()
    }

    fun stop() {
        if (countDownTimer != null) {
            countDownTimer?.cancel()
            countDownTimer = null
            Timber.d("TimerModel: таймер зупинено")
        }
    }

    companion object {
        const val DEFAULT_DURATION_SECONDS = 30L
    }
}