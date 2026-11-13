package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadUiState(
    val progress: Int = 0,
    val status: String = "Готово до завантаження",
    val isDownloading: Boolean = false
)

class DownloadViewModel : ViewModel() {

    private val fileRepository = FileRepository()

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    fun startDownload() {
        if (_uiState.value.isDownloading) return

        downloadJob = viewModelScope.launch {
            _uiState.value = DownloadUiState(
                progress = 0,
                status = "Завантаження: 0%",
                isDownloading = true
            )

            fileRepository.downloadFile()
                .onCompletion { cause ->
                    when (cause) {
                        null -> _uiState.update {
                            it.copy(
                                progress = 100,
                                status = "Файл успішно скачано!",
                                isDownloading = false
                            )
                        }
                        is CancellationException -> _uiState.update {
                            it.copy(
                                status = "Завантаження скасовано",
                                isDownloading = false
                            )
                        }
                        else -> _uiState.update {
                            it.copy(
                                status = "Помилка завантаження: ${cause.message}",
                                isDownloading = false
                            )
                        }
                    }
                }
                .collect { progress ->
                    _uiState.update {
                        it.copy(
                            progress = progress,
                            status = "Завантаження: $progress%"
                        )
                    }
                }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }
}