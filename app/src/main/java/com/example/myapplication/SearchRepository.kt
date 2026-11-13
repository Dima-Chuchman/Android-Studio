package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.delay

class SearchRepository {

    private val items = listOf(
        "Ukraine", "Kyiv", "Kyiv Oblast", "Kharkiv", "Odesa",
        "Dnipro", "Lviv", "Zaporizhzhia", "Vinnytsia", "Poltava",
        "Chernihiv", "Cherkasy", "Zhytomyr", "Sumy", "Rivne",
        "Ivano-Frankivsk", "Ternopil", "Lutsk", "Uzhhorod",
        "Poland", "Germany", "France", "Spain", "Italy"
    )

    suspend fun search(query: String): List<String> {
        Log.d("SearchRepository", "Запит до бази даних: '$query'")
        delay(500L)
        return items.filter { it.contains(query.trim(), ignoreCase = true) }
    }
}