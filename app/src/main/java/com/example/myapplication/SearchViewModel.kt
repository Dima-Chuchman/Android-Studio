package com.example.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {

    private val searchRepository = SearchRepository()

    private val searchQueryFlow = MutableStateFlow("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val searchResults: StateFlow<List<String>> = searchQueryFlow
        .debounce(400L)
        .filter { query -> query.trim().length >= 3 }
        .distinctUntilChanged()
        .onEach { _isLoading.value = true }
        .map { query -> searchRepository.search(query) }
        .flowOn(Dispatchers.IO)
        .onEach { _isLoading.value = false }
        .catch { e ->
            Log.e("SearchViewModel", "Помилка пошуку: ${e.message}")
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun onQueryChanged(query: String) {
        searchQueryFlow.value = query
    }
}