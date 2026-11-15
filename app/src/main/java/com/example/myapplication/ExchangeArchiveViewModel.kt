package com.example.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed interface ExchangeUiState {
    data object Initial : ExchangeUiState
    data object Loading : ExchangeUiState
    data class Success(
        val responseDate: String,
        val allRates: List<ExchangeRateItem>
    ) : ExchangeUiState
    data class Error(val message: String) : ExchangeUiState
}

data class ExchangeScreenState(
    val selectedDate: String = "01.11.2026",
    val searchQuery: String = "",
    val sortAlphabetically: Boolean = false,
    val uiState: ExchangeUiState = ExchangeUiState.Initial
) {
    val visibleRates: List<ExchangeRateItem>
        get() {
            val success = uiState as? ExchangeUiState.Success ?: return emptyList()
            val filtered = if (searchQuery.isBlank()) {
                success.allRates
            } else {
                val query = searchQuery.trim()
                success.allRates.filter { item ->
                    item.currency?.contains(query, ignoreCase = true) == true
                }
            }
            return if (sortAlphabetically) {
                filtered.sortedBy { it.currency.orEmpty() }
            } else {
                filtered
            }
        }
}

class ExchangeArchiveViewModel : ViewModel() {

    private val _state = MutableStateFlow(ExchangeScreenState())
    val state: StateFlow<ExchangeScreenState> = _state.asStateFlow()

    private var fetchJob: Job? = null

    fun onDateTextChanged(newDate: String) {
        if (_state.value.uiState is ExchangeUiState.Loading) return
        _state.update { it.copy(selectedDate = newDate) }
    }

    fun onDatePickedFromCalendar(utcMillis: Long) {
        if (_state.value.uiState is ExchangeUiState.Loading) return
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formatted = formatter.format(Date(utcMillis))
        _state.update { it.copy(selectedDate = formatted) }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onToggleSort() {
        _state.update { it.copy(sortAlphabetically = !it.sortAlphabetically) }
    }

    fun loadRates() {
        if (_state.value.uiState is ExchangeUiState.Loading) return

        val rawDate = _state.value.selectedDate.trim()
        val validationError = validateDate(rawDate)
        if (validationError != null) {
            _state.update { it.copy(uiState = ExchangeUiState.Error(validationError)) }
            return
        }

        fetchJob?.cancel()
        _state.update { it.copy(uiState = ExchangeUiState.Loading) }

        Log.d("PrivatBankAPI", "Старт мережевого запиту для дати: $rawDate")
        fetchJob = viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getExchangeArchive(rawDate)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        _state.update {
                            it.copy(uiState = ExchangeUiState.Error("Сервер повернув порожню відповідь."))
                        }
                        return@launch
                    }

                    // Відкидаємо записи без коду валюти (наприклад, базовий запис UAH -> UAH)
                    val validRates = body.exchangeRate.filter { !it.currency.isNullOrBlank() }
                    val resolvedDate = body.date.ifBlank { rawDate }

                    Log.d("PrivatBankAPI", "Успішно отримано ${validRates.size} валют на $resolvedDate")
                    _state.update {
                        it.copy(
                            uiState = ExchangeUiState.Success(
                                responseDate = resolvedDate,
                                allRates = validRates
                            )
                        )
                    }
                } else {
                    Log.e("PrivatBankAPI", "HTTP помилка: ${response.code()}")
                    _state.update {
                        it.copy(
                            uiState = ExchangeUiState.Error(
                                "Помилка сервера (код ${response.code()}). Спробуйте пізніше."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PrivatBankAPI", "Помилка мережі: ${e.message}")
                _state.update {
                    it.copy(
                        uiState = ExchangeUiState.Error(
                            "Не вдалося завантажити курси. Перевірте з'єднання та спробуйте знову."
                        )
                    )
                }
            }
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
        } ?: return "Некоректна дата. Введіть існуючу дату у форматі дд.мм.рррр (наприклад, 01.11.2026)."

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
            return "Неможливо отримати курс для майбутньої дати."
        }

        if (selectedCal.before(minArchiveDate)) {
            return "Архів ПриватБанку доступний лише за останні 4 роки."
        }

        return null
    }

    override fun onCleared() {
        fetchJob?.cancel()
        super.onCleared()
    }
}