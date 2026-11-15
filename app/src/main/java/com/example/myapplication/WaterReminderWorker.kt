package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WaterReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newCount = prefs.getInt(KEY_REMINDER_COUNT, 0) + 1
        prefs.edit().putInt(KEY_REMINDER_COUNT, newCount).apply()

        Log.d("WaterReminderWorker", "Нагадування відправлено (#$newCount)")

        showWaterNotification()
        return Result.success()
    }

    private fun showWaterNotification() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Нагадування про воду",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Регулярні нагадування випити склянку води"
            }
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("WaterReminderWorker", "Немає дозволу POST_NOTIFICATIONS")
            return
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Трекер води")
            .setContentText("Час випити склянку води! 💧")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val PREFS_NAME = "water_reminder_prefs"
        const val KEY_REMINDER_COUNT = "reminder_count"
        const val KEY_IS_ENABLED = "is_enabled"
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val CHANNEL_ID = "water_channel"
    }
}