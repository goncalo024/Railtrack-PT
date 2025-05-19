package com.example.guiacaminhosferro

import android.os.Parcelable
import com.google.firebase.database.PropertyName
import kotlinx.parcelize.Parcelize

data class Estacao(
    @get:PropertyName("Id") @set:PropertyName("Id")
    var id: String? = null,

    @get:PropertyName("Nome")      @set:PropertyName("Nome")
    var nome: String? = null,

    @get:PropertyName("Morada")    @set:PropertyName("Morada")
    var morada: String? = null,

    @get:PropertyName("Latitude")  @set:PropertyName("Latitude")
    var latitude: String? = null,

    @get:PropertyName("Longitude") @set:PropertyName("Longitude")
    var longitude: String? = null,

    @get:PropertyName("Descrição") @set:PropertyName("Descrição")
    var descricao: String? = null,

    @get:PropertyName("ContextoHistórico") @set:PropertyName("ContextoHistórico")
    var contextoHistorico: String? = null,

    @get:PropertyName("Imagens")   @set:PropertyName("Imagens")
    var imagens: String? = null
)