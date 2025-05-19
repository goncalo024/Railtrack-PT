package com.example.guiacaminhosferro

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2

class ContextoHistoricoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contexto_historico)

        val toolbar = findViewById<Toolbar>(R.id.toolbarContexto)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val nome = intent.getStringExtra("nome")
        val contexto = intent.getStringExtra("contextoHistorico")
        val imagens = intent.getStringArrayListExtra("imagens") ?: arrayListOf()

        val titulo = findViewById<TextView>(R.id.textTituloEstacao)
        val textoContexto = findViewById<TextView>(R.id.textContextoHistorico)
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerContexto)

        titulo.text = nome ?: "Estação"
        textoContexto.text = contexto ?: "Sem contexto histórico"

        val adapter = ImagePagerAdapter(this, imagens)
        viewPager.adapter = adapter
    }
}
