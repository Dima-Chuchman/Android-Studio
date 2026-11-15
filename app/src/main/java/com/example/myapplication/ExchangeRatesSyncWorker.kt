package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExchangeRatesSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val inputDate = inputData.getString(KEY_DATE)?.trim()
        val targetDate = if (!inputDate.isNullOrEmpty()) {
            inputDate
        } else {
            SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
        }

        val validationError = validateDate(targetDate)
        if (validationError != null) {
            Log.w(TAG, "Некоректна дата ($targetDate): $validationError")
            return Result.failure(workDataOf(KEY_MESSAGE to validationError))
        }

        return try {
            Log.d(TAG, "Завантаження курсів валют за дату: $targetDate")
            val response = RetrofitClient.api.getExchangeArchive(targetDate)

            if (!response.isSuccessful) {
                val code = response.code()
                Log.e(TAG, "HTTP помилка сервера: $code")
                return if (code in 500..599) {
                    Result.retry()
                } else {
                    Result.failure(workDataOf(KEY_MESSAGE to "Помилка сервера (HTTP $code)"))
                }
            }

            val body = response.body()
                ?: return Result.failure(workDataOf(KEY_MESSAGE to "Сервер повернув порожню відповідь"))

            val responseDate = body.date.trim()
            if (responseDate.isNotEmpty() && responseDate != targetDate) {
                Log.e(TAG, "Невідповідність дат: запитано $targetDate, отримано $responseDate")
                return Result.failure(
                    workDataOf(KEY_MESSAGE to "Дата відповіді ($responseDate) не відповідає запитаній ($targetDate)")
                )
            }

            val effectiveDate = responseDate.ifEmpty { targetDate }
            val entities = body.exchangeRate
                .filter { !it.currency.isNullOrBlank() }
                .map { item ->
                    ExchangeRateEntity(
                        date = effectiveDate,
                        currency = item.currency!!.trim(),
                        baseCurrency = item.baseCurrency?.takeIf { it.isNotBlank() } ?: "UAH",
                        purchaseRate = item.purchaseRate,
                        saleRate = item.saleRate
                    )
                }

            if (entities.isEmpty()) {
                Log.i(TAG, "Сервер повернув порожній список курсів на $effectiveDate")
                return Result.success(workDataOf(KEY_MESSAGE to "Сервер не повернув курсів"))
            }

            val dao = ExchangeDatabase.getInstance(applicationContext).exchangeRateDao()
            dao.upsertAll(entities)

            Log.d(TAG, "Успішно збережено у Room ${entities.size} записів на $effectiveDate")
            Result.success(
                workDataOf(KEY_MESSAGE to "Оновлено (${entities.size} валют) на $effectiveDate")
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (io: IOException) {
            Log.e(TAG, "Тимчасова помилка мережі: ${io.message}")
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Непередбачена помилка: ${e.message}")
            Result.failure(workDataOf(KEY_MESSAGE to "Помилка обробки даних: ${e.localizedMessage}"))
        }
    }

    private fun validateDate(dateStr: String): String? {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply {
            isLenient = false
        }

        val parsedDate = try {
            sdf.parse(dateStr)
        } catch (_: ParseException) {
            null
        } ?: return "Некоректна дата. Використовуйте формат дд.мм.рррр."

        if (sdf.format(parsedDate) != dateStr) {
            return "Некоректний формат дати. Використовуйте дд.мм.рррр."
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val minArchiveDate = (today.clone() as Calendar).apply {
            add(Calendar.YEAR, -4)
        }

        val selectedCal = Calendar.getInstance().apply {
            time = parsedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (selectedCal.after(today)) {
            return "Неможливо завантажити курси для майбутньої дати."
        }
        if (selectedCal.before(minArchiveDate)) {
            return "Архів ПриватБанку доступний лише за останні 4 роки."
        }

        return null
    }

    companion object {
        private const val TAG = "ExchangeRatesSyncWorker"
        const val KEY_DATE = "date"
        const val KEY_MESSAGE = "message"
    }
}