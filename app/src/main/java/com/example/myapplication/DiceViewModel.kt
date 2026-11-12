package com.example.myapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class DiceViewModel : ViewModel() {

    private val _diceValues = MutableLiveData(List(5) { 1 })
    val diceValues: LiveData<List<Int>> = _diceValues

    private val _isRolling = MutableLiveData(false)
    val isRolling: LiveData<Boolean> = _isRolling

    private val lock = Any()
    private val currentValues = IntArray(5) { 1 }
    private val activeThreads = Collections.synchronizedList(mutableListOf<Thread>())

    @Volatile
    private var isCancelled = false

    fun rollDice() {
        if (_isRolling.value == true) return

        isCancelled = false
        _isRolling.value = true

        val remainingThreads = AtomicInteger(5)

        for (dieIndex in 0 until 5) {
            // Випадкова тривалість обертання для кожної кістки: 1.5 – 3.0 с
            val iterations = (15..30).random()

            val worker = thread(start = false, name = "DiceWorker-$dieIndex") {
                try {
                    repeat(iterations) {
                        if (isCancelled || Thread.currentThread().isInterrupted) return@thread
                        Thread.sleep(100L)

                        val rolledFace = (1..6).random()
                        val snapshot = synchronized(lock) {
                            currentValues[dieIndex] = rolledFace
                            currentValues.toList()
                        }
                        _diceValues.postValue(snapshot)
                    }
                } catch (_: InterruptedException) {
                    // Потік коректно перервано у onCleared()
                } finally {
                    activeThreads.remove(Thread.currentThread())
                    if (remainingThreads.decrementAndGet() == 0 && !isCancelled) {
                        _isRolling.postValue(false)
                    }
                }
            }

            activeThreads.add(worker)
            worker.start()
        }
    }

    override fun onCleared() {
        super.onCleared()
        isCancelled = true
        synchronized(activeThreads) {
            activeThreads.forEach { it.interrupt() }
            activeThreads.clear()
        }
        Log.d("DiceViewModel", "onCleared(): усі 5 фонових потоків зупинено")
    }
}