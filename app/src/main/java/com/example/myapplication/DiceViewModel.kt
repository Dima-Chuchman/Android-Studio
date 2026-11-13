package com.example.myapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DiceViewModel : ViewModel() {

    private val diceModel = DiceModel()

    private val _diceValues = MutableLiveData(List(5) { 1 })
    val diceValues: LiveData<List<Int>> = _diceValues

    private val _isRolling = MutableLiveData(false)
    val isRolling: LiveData<Boolean> = _isRolling

    private val _buttonText = MutableLiveData("ROLL")
    val buttonText: LiveData<String> = _buttonText

    private var rollingJob: Job? = null

    fun rollDice() {
        // Якщо натиснуто RESTART під час обертання — скасовуємо поточну корутину
        rollingJob?.cancel()

        rollingJob = viewModelScope.launch {
            _isRolling.value = true
            _buttonText.value = "RESTART"

            val finalValues = diceModel.rollAllDice { updatedFaces ->
                _diceValues.value = updatedFaces
            }

            // Повертаємо кнопку в стан ROLL лише якщо корутина завершилася штатно, а не через cancel()
            if (isActive) {
                _diceValues.value = finalValues
                _isRolling.value = false
                _buttonText.value = "ROLL"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("DiceViewModel", "onCleared(): viewModelScope автоматично скасував усі корутини")
    }
}