package com.example.myapplication

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PrivatBankApi {

    @GET("exchange_rates?json")
    suspend fun getExchangeArchive(
        @Query("date") date: String
    ): Response<ExchangeArchiveResponse>
}