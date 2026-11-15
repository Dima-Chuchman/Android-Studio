package com.example.myapplication

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ExchangeWorkScheduler {

    const val PERIODIC_WORK_NAME = "exchange_rates_periodic"

    fun uniqueWorkNameForDate(date: String): String = "exchange_rates_$date"

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest =
            PeriodicWorkRequestBuilder<ExchangeRatesSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun enqueueManualSync(context: Context, selectedDate: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<ExchangeRatesSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(ExchangeRatesSyncWorker.KEY_DATE to selectedDate))
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            uniqueWorkNameForDate(selectedDate),
            ExistingWorkPolicy.KEEP,
            oneTimeRequest
        )
    }
}