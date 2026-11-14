package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BroadcastRepository(private val context: Context) {

    fun observeSystemEvents(): Flow<BroadcastEvent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return

                val action = intent.action ?: "UNKNOWN_ACTION"
                val now = Date()
                val shortTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
                val fullTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(now)

                val eventTitle = mapActionToTitle(action)
                val extrasText = extractExtras(intent)

                val details = buildString {
                    appendLine("Опис: $eventTitle")
                    appendLine("Системний Action: $action")
                    appendLine("Точний час: $fullTimestamp")
                    append("Параметри (Extras):\n$extrasText")
                }

                val event = BroadcastEvent(
                    id = UUID.randomUUID().toString(),
                    type = eventTitle,
                    timestamp = shortTime,
                    details = details
                )

                trySend(event)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_HEADSET_PLUG)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun mapActionToTitle(action: String): String = when (action) {
        Intent.ACTION_POWER_CONNECTED -> "Зарядний пристрій підключено"
        Intent.ACTION_POWER_DISCONNECTED -> "Зарядний пристрій відключено"
        Intent.ACTION_AIRPLANE_MODE_CHANGED -> "Зміна режиму «У літаку»"
        Intent.ACTION_BATTERY_LOW -> "Критично низький заряд батареї"
        Intent.ACTION_BATTERY_OKAY -> "Рівень заряду батареї в нормі"
        Intent.ACTION_HEADSET_PLUG -> "Зміна стану гарнітури/навушників"
        else -> action.substringAfterLast('.')
    }

    @Suppress("DEPRECATION")
    private fun extractExtras(intent: Intent): String {
        val extras = intent.extras ?: return "  • Додаткові параметри відсутні"
        val keys = extras.keySet()
        if (keys.isEmpty()) return "  • Додаткові параметри відсутні"

        return keys.joinToString(separator = "\n") { key ->
            "  • $key = ${extras.get(key)}"
        }
    }
}