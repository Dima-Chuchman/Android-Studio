package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val _syncStatus = MutableStateFlow("Очікування запуску...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NewsSyncScreen(
                    statusFlow = syncStatus,
                    onSyncNowClick = { startOneTimeSync() },
                    onEnableNightSyncClick = { scheduleNightPeriodicSync() }
                )
            }
        }
    }

    private fun startOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("user_token", "abc123")
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<NewsSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(oneTimeRequest)

        workManager.getWorkInfoByIdLiveData(oneTimeRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null) {
                    _syncStatus.value = when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> "Очікує... (ENQUEUED)"
                        WorkInfo.State.RUNNING -> "Виконується... (RUNNING)"
                        WorkInfo.State.SUCCEEDED -> {
                            val message = workInfo.outputData.getString("result") ?: "Синхронізовано"
                            "$message (SUCCEEDED)"
                        }
                        WorkInfo.State.FAILED -> "Помилка синхронізації (FAILED)"
                        WorkInfo.State.BLOCKED -> "Заблоковано умовами (BLOCKED)"
                        WorkInfo.State.CANCELLED -> "Скасовано (CANCELLED)"
                    }
                }
            }
    }

    private fun scheduleNightPeriodicSync() {
        val nightConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val inputData = Data.Builder()
            .putString("user_token", "abc123")
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(nightConstraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "night_news_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        _syncStatus.value = "Нічну синхронізацію заплановано (Wi-Fi + Зарядка)"
    }
}

@Composable
fun NewsSyncScreen(
    statusFlow: StateFlow<String>,
    onSyncNowClick: () -> Unit,
    onEnableNightSyncClick: () -> Unit
) {
    val statusText by statusFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = statusText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSyncNowClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Синхронізувати зараз")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onEnableNightSyncClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Увімкнути нічну синхронізацію")
        }
    }
}