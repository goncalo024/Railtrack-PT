package com.example.guiacaminhosferro

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guiacaminhosferro.ComboioAdapter
import com.example.guiacaminhosferro.api.ApiClient
import com.example.guiacaminhosferro.model.Comboio
import kotlinx.coroutines.launch

class HorariosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHorarios)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val estacaoId = intent.getStringExtra("estacaoId")
        if (estacaoId.isNullOrEmpty()) {
            Toast.makeText(this, "Estação inválida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // chama o endpoint via ApiClient
                val comboios: List<Comboio> =
                    ApiClient.scheduleApi.getComboios(estacaoId)

                recyclerView.adapter = ComboioAdapter(comboios)
            } catch (e: Exception) {
                Toast.makeText(this@HorariosActivity,
                    "Erro ao carregar horários: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
