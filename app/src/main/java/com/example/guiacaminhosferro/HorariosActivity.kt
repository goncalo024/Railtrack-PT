package com.example.guiacaminhosferro

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guiacaminhosferro.api.ApiClient
import com.example.guiacaminhosferro.model.Comboio
import kotlinx.coroutines.launch


class HorariosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        // 1) toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            title = "Horários"
        }
        toolbar.setNavigationOnClickListener { finish() }

        // 2) recycler
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHorarios)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 3) lê estacaoId e stationName
        val estacaoId   = intent.getStringExtra("estacaoId")
        val stationName = intent.getStringExtra("stationName")
        if (estacaoId.isNullOrEmpty() || stationName.isNullOrEmpty()) {
            Toast.makeText(this, "Estação inválida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 4) normaliza só uma vez
        val normStation = stationName.normalizeForSearch()

        // 5) procura comboios
        lifecycleScope.launch {
            try {
                // 1) procura todos os comboios
                val comboios: List<Comboio> =
                    ApiClient.scheduleApi.getComboios(estacaoId)

                // 2) passa a lista completa para o adapter
                recyclerView.adapter = ComboioAdapter(comboios)
            } catch (e: Exception) {
                Toast.makeText(
                    this@HorariosActivity,
                    "Erro ao carregar horários: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
