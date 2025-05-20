package com.example.guiacaminhosferro

import java.text.Normalizer
import java.util.Locale

/**
 * Normaliza uma string para comparação:
 *  • remove acentos
 *  • remove espaços à frente / atrás
 *  • converte tudo para minúsculas
 */
fun String.normalizeForSearch(): String {
    // 1) desacentua
    val noAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    // 2) trim + lowercase
    return noAccents.trim().lowercase()
}