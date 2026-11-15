package com.example.myapplication

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class WaterReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = context.getSharedPreferences(
        WaterReminderWorker.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _reminderCount = MutableStateFlow(
        prefs.getInt(WaterReminderWorker.KEY_REMINDER_COUNT, 0)
    )
    val reminderCount: StateFlow<Int> = _reminderCount.asStateFlow()

    private val _isEnabled = MutableStateFlow(
        prefs.getBoolean(WaterReminderWorker.KEY_IS_ENABLED, false)
    )
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _intervalInput = MutableStateFlow(
        prefs.getString(WaterReminderWorker.KEY_INTERVAL_MINUTES, "15") ?: "15"
    )
    val intervalInput: StateFlow<String> = _intervalInput.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        if (key == WaterReminderWorker.KEY_REMINDER_COUNT) {
            _reminderCount.value = sharedPrefs.getInt(WaterReminderWorker.KEY_REMINDER_COUNT, 0)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    fun onIntervalChanged(newValue: String) {
        val digitsOnly = newValue.filter { it.isDigit() }
        _intervalInput.value = digitsOnly
        prefs.edit().putString(WaterReminderWorker.KEY_INTERVAL_MINUTES, digitsOnly).apply()
    }

    fun startReminder(intervalMinutes: Long) {
        val safeInterval = if (intervalMinutes < 15L) 15L else intervalMinutes
        _intervalInput.value = safeInterval.toString()

        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            safeInterval,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        _isEnabled.value = true
        prefs.edit()
            .putBoolean(WaterReminderWorker.KEY_IS_ENABLED, true)
            .putString(WaterReminderWorker.KEY_INTERVAL_MINUTES, safeInterval.toString())
            .apply()
    }

    fun stopReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        _isEnabled.value = false
        prefs.edit().putBoolean(WaterReminderWorker.KEY_IS_ENABLED, false).apply()
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onCleared()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "water_reminder"
    }
}