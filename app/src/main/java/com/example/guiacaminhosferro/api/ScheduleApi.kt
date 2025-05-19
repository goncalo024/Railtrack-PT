package com.example.guiacaminhosferro.api

import com.example.guiacaminhosferro.model.Comboio
import com.example.guiacaminhosferro.model.ComboioDetalhes
import com.example.guiacaminhosferro.Estacao
import retrofit2.http.GET
import retrofit2.http.Path

interface ScheduleApi {
    @GET("api/stations")
    suspend fun getEstacoes(): List<Estacao>

    @GET("api/stations/{id}/trains")
    suspend fun getComboios(@Path("id") estacaoId: String): List<Comboio>

    @GET("api/trains/{id}")
    suspend fun getComboioDetalhes(@Path("id") comboioId: String): ComboioDetalhes
}