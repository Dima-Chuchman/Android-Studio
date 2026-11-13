package com.example.myapplication

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class DiceModel {

    suspend fun rollSingleDie(
        dieIndex: Int,
        onFaceChanged: (index: Int, face: Int) -> Unit
    ): Int {
        // Випадкова тривалість обертання для кожної кістки: від 1.5 с (15) до 3.0 с (30 кроків)
        val iterations = (15..30).random()
        var currentFace = 1

        repeat(iterations) {
            delay(100L)
            currentFace = (1..6).random()
            onFaceChanged(dieIndex, currentFace)
        }

        return currentFace
    }

    suspend fun rollAllDice(
        onDiceUpdated: (List<Int>) -> Unit
    ): List<Int> = coroutineScope {
        val currentFaces = IntArray(5) { 1 }

        val deferredRolls = List(5) { dieIndex ->
            async {
                rollSingleDie(dieIndex) { index, newFace ->
                    currentFaces[index] = newFace
                    onDiceUpdated(currentFaces.toList())
                }
            }
        }

        // Очікуємо завершення всіх 5 паралельних корутин
        deferredRolls.awaitAll()
    }
}