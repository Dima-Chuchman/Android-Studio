package com.example.myapplication

import androidx.room.Entity

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["date", "currency"]
)
data class ExchangeRateEntity(
    val date: String,
    val currency: String,
    val baseCurrency: String,
    val purchaseRate: Double?,
    val saleRate: Double?
)