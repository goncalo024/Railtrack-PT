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

        // 1) Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarFavoritos)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            title = "Favoritos"
        }
        toolbar.setNavigationOnClickListener { finish() }

        // 2) RecyclerView + Adapter
        recyclerView = findViewById(R.id.recyclerViewFavoritos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        estacaoAdapter = EstacaoAdapter(listaFavoritos, verMaisListener = null)
        recyclerView.adapter = estacaoAdapter

        // 3) Usuário autenticado
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

        // 4) Buscar chaves de favoritos (só estacaoId)
        favoritosRef.get()
            .addOnSuccessListener { favSnap ->
                if (!favSnap.exists()) {
                    Toast.makeText(this, "Nenhum favorito guardado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val favIds = favSnap.children.mapNotNull { it.key }.toList()
                if (favIds.isEmpty()) {
                    Toast.makeText(this, "Nenhum favorito guardado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 5) Para cada favId, busca diretamente /Estacoes/{favId}
                listaFavoritos.clear()
                var carregados = 0
                favIds.forEach { favId ->
                    estacoesRef.child(favId).get()
                        .addOnSuccessListener { estSnap ->
                            estSnap.getValue(Estacao::class.java)
                                ?.apply { id = favId }
                                ?.let { listaFavoritos.add(it) }
                        }
                        .addOnCompleteListener {
                            carregados++
                            // Só atualiza o adapter quando já tentámos todos
                            if (carregados == favIds.size) {
                                if (listaFavoritos.isEmpty()) {
                                    Toast.makeText(
                                        this,
                                        "Sem favoritos a mostrar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                estacaoAdapter.atualizarLista(listaFavoritos, mostrarBotao = false)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar favoritos", Toast.LENGTH_SHORT).show()
                Log.e("FavoritosActivity", "Erro ao buscar Favoritos", e)
            }
    }
}

