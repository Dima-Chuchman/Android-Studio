package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class NewsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val token = inputData.getString("user_token")
        Log.d("NewsSyncWorker", "Початок синхронізації новин (токен: $token)")

        if (token.isNullOrBlank()) {
            return Result.failure()
        }

        delay(3000L)

        val outputData = Data.Builder()
            .putString("result", "Успіх: завантажено 15 новин")
            .build()

        Log.d("NewsSyncWorker", "Синхронізацію завершено успішно")
        return Result.success(outputData)
    }
}