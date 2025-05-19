package com.example.guiacaminhosferro.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Base URL do teu serviço Node.js
    private const val BASE_URL = "http://10.0.2.2:3000/"
    // ↑ se estiveres no emulador usa 10.0.2.2, no device real põe o IP da máquina

    // Retrofit instance
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Exposição da tua interface ScheduleApi
    val scheduleApi: ScheduleApi = retrofit.create(ScheduleApi::class.java)
}
