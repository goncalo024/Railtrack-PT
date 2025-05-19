package com.example.guiacaminhosferro

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FavoritosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var estacaoAdapter: EstacaoAdapter
    private val listaFavoritos = mutableListOf<Estacao>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favoritos)

        // ---- toolbar ----
        val toolbar = findViewById<Toolbar>(R.id.toolbarFavoritos)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            title = "Favoritos"
        }
        toolbar.setNavigationOnClickListener { finish() }

        // ---- recyclerview e adapter ----
        recyclerView = findViewById(R.id.recyclerViewFavoritos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Criamos o adapter SEM listener de "ver mais"
        estacaoAdapter = EstacaoAdapter(
            listaFavoritos,
            verMaisListener = null
        )
        recyclerView.adapter = estacaoAdapter

        // ---- busca favoritos no Firebase ----
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Utilizador não autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = user.uid
        val favoritosRef = FirebaseDatabase.getInstance()
            .getReference("Favoritos")
            .child(uid)
        val estacoesRef = FirebaseDatabase.getInstance()
            .getReference("Estacoes")

        favoritosRef.get()
            .addOnSuccessListener { favSnapshot ->
                if (!favSnapshot.exists()) {
                    Toast.makeText(this, "Nenhum favorito guardado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Carrega todas as estações e filtra só as que estão nos favoritos
                estacoesRef.get()
                    .addOnSuccessListener { estacoesSnapshot ->
                        listaFavoritos.clear()
                        for (estacaoSnap in estacoesSnapshot.children) {
                            val e = estacaoSnap.getValue(Estacao::class.java)
                            // usa o nome como key de favoritos
                            val nomeKey = e?.nome ?: continue
                            if (favSnapshot.hasChild(nomeKey)) {
                                listaFavoritos.add(e)
                            }
                        }
                        // atualiza adapter e OCULTA o botão "Ver Mais"
                        estacaoAdapter.atualizarLista(listaFavoritos, /* mostrarBotao = */ false)
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Erro ao carregar estações",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("FavoritosActivity", "Erro ao buscar estações", it)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar favoritos", Toast.LENGTH_SHORT).show()
                Log.e("FavoritosActivity", "Erro: ", it)
            }
    }
}
