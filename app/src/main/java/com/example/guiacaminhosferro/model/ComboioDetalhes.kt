package com.example.guiacaminhosferro.model

data class ComboioDetalhes(
    val trainId: String,
    val route: String?,
    val stops: List<String>?
)