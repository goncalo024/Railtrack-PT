package com.example.guiacaminhosferro.model

data class Comboio(
    val trainId: String,
    val number: String,
    val type: String?,
    val origin: String?,
    val destination: String?,
    val departure: String?,
    val arrival: String?,
    val platform: String?
)
