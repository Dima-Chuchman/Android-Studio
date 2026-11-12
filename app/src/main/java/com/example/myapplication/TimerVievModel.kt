package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

class TimerViewModel : ViewModel() {

    private val timerModel = TimerModel()

    private val _timerLiveData = MutableLiveData(TimerModel.DEFAULT_DURATION_SECONDS)
    val timerLiveData: LiveData<Long> = _timerLiveData

    private val _isTimerRunning = MutableLiveData(false)
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning

    fun startTimer(durationSeconds: Long = TimerModel.DEFAULT_DURATION_SECONDS) {
        val validSeconds = if (durationSeconds > 0L) {
            durationSeconds
        } else {
            TimerModel.DEFAULT_DURATION_SECONDS
        }

        _isTimerRunning.value = true
        timerModel.start(
            durationSeconds = validSeconds,
            onTick = { secondsRemaining ->
                _timerLiveData.value = secondsRemaining
            },
            onFinish = {
                _isTimerRunning.value = false
            }
        )
    }

    fun resetTimer() {
        timerModel.stop()
        _isTimerRunning.value = false
        _timerLiveData.value = TimerModel.DEFAULT_DURATION_SECONDS
    }

    override fun onCleared() {
        super.onCleared()
        timerModel.stop()
        Timber.d("TimerViewModel: onCleared() викликано, таймер зупинено")
    }
}