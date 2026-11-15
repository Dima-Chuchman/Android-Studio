package com.example.myapplication

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Upsert
    suspend fun upsertAll(rates: List<ExchangeRateEntity>)

    @Query("SELECT * FROM exchange_rates WHERE date = :date ORDER BY currency ASC")
    fun observeRates(date: String): Flow<List<ExchangeRateEntity>>

    @Query(
        """
        SELECT DISTINCT date FROM exchange_rates 
        ORDER BY substr(date, 7, 4) || substr(date, 4, 2) || substr(date, 1, 2) DESC
        """
    )
    fun observeSavedDates(): Flow<List<String>>
}