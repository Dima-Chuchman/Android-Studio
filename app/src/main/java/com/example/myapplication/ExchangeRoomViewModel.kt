package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SyncBannerState(
    val text: String? = null,
    val isInProgress: Boolean = false,
    val isError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = ExchangeDatabase.getInstance(appContext).exchangeRateDao()
    private val workManager = WorkManager.getInstance(appContext)

    private val _selectedDate = MutableStateFlow(todayFormatted())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _localValidationError = MutableStateFlow<String?>(null)

    val savedDates: StateFlow<List<String>> = dao.observeSavedDates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val rawRatesForSelectedDate: StateFlow<List<ExchangeRateEntity>> = _selectedDate
        .flatMapLatest { date -> dao.observeRates(date.trim()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val visibleRates: StateFlow<List<ExchangeRateEntity>> = combine(
        rawRatesForSelectedDate,
        _searchQuery
    ) { rates, query ->
        if (query.isBlank()) {
            rates
        } else {
            val cleanQuery = query.trim()
            rates.filter { it.currency.contains(cleanQuery, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val hasAnySavedRatesForDate: StateFlow<Boolean> = rawRatesForSelectedDate
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    private val workerBannerState: StateFlow<SyncBannerState> = _selectedDate
        .flatMapLatest { date ->
            workManager.getWorkInfosForUniqueWorkFlow(
                ExchangeWorkScheduler.uniqueWorkNameForDate(date.trim())
            )
        }
        .map { workInfos ->
            val info = workInfos.firstOrNull() ?: return@map SyncBannerState()
            when (info.state) {
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.BLOCKED -> SyncBannerState(
                    text = "Очікує мережі / запуску...",
                    isInProgress = true
                )
                WorkInfo.State.RUNNING -> SyncBannerState(
                    text = "Оновлюється...",
                    isInProgress = true
                )
                WorkInfo.State.SUCCEEDED -> {
                    val msg = info.outputData.getString(ExchangeRatesSyncWorker.KEY_MESSAGE)
                        ?: "Курси успішно оновлено"
                    SyncBannerState(text = msg)
                }
                WorkInfo.State.FAILED -> {
                    val msg = info.outputData.getString(ExchangeRatesSyncWorker.KEY_MESSAGE)
                        ?: "Не вдалося оновити курси"
                    SyncBannerState(text = msg, isError = true)
                }
                WorkInfo.State.CANCELLED -> SyncBannerState(
                    text = "Оновлення скасовано",
                    isError = true
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SyncBannerState()
        )

    val syncBanner: StateFlow<SyncBannerState> = combine(
        _localValidationError,
        workerBannerState
    ) { localError, workerState ->
        if (localError != null) {
            SyncBannerState(text = localError, isError = true)
        } else {
            workerState
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SyncBannerState()
    )

    fun onDateTextChanged(newDate: String) {
        _localValidationError.value = null
        _selectedDate.value = newDate
    }

    fun onDatePickedFromCalendar(utcMillis: Long) {
        _localValidationError.value = null
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        _selectedDate.value = formatter.format(Date(utcMillis))
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun refreshSelectedDate() {
        val cleanDate = _selectedDate.value.trim()
        val error = validateDate(cleanDate)
        if (error != null) {
            _localValidationError.value = error
            return
        }

        _localValidationError.value = null
        ExchangeWorkScheduler.enqueueManualSync(appContext, cleanDate)
    }

    private fun validateDate(dateStr: String): String? {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply {
            isLenient = false
        }
        val parsed = try {
            sdf.parse(dateStr)
        } catch (_: ParseException) {
            null
        } ?: return "Некоректна дата. Введіть існуючу дату у форматі дд.мм.рррр."

        if (sdf.format(parsed) != dateStr) {
            return "Некоректний формат дати. Використовуйте дд.мм.рррр."
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val minDate = (today.clone() as Calendar).apply { add(Calendar.YEAR, -4) }
        val selected = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (selected.after(today)) return "Неможливо запитати курси для майбутньої дати."
        if (selected.before(minDate)) return "Архів ПриватБанку доступний лише за останні 4 роки."
        return null
    }

    private fun todayFormatted(): String =
        SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
}