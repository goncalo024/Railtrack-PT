package com.example.guiacaminhosferro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.guiacaminhosferro.HorariosActivity

class DetalheEstacaoActivity : AppCompatActivity() {

    // 1) Só DECLARA dentro da classe, sem duplicar no topo do ficheiro:
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_estacao)

        // 2) Inicializa **logo** no início do onCreate:
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // 3) Recebe o estacaoId e aborta se não existir
        val estacaoId = intent.getStringExtra("estacaoId")
        if (estacaoId.isNullOrEmpty()) {
            Toast.makeText(this, "ID da estação ausente", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 4) Agora lê os restantes extras em segurança
        val nome              = intent.getStringExtra("nome").orEmpty()
        val morada            = intent.getStringExtra("morada").orEmpty()
        val descricao         = intent.getStringExtra("descricao").orEmpty()
        val contextoHistorico = intent.getStringExtra("contextoHistorico").orEmpty()
        val latitude          = intent.getStringExtra("latitude")
        val longitude         = intent.getStringExtra("longitude")
        val imagensLista      = intent.getStringArrayListExtra("imagens") ?: arrayListOf()

        // 5) Toolbar com voltar
        findViewById<Toolbar>(R.id.toolbarDetalhe).apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener { finish() }
        }

        // 6) Preenche textos
        findViewById<TextView>(R.id.estacaoNome).text      = nome
        findViewById<TextView>(R.id.estacaoMorada).text    = morada
        findViewById<TextView>(R.id.estacaoDescricao).text = descricao

        // 7) ViewPager de imagens
        findViewById<ViewPager2>(R.id.viewPagerImagens).adapter =
            ImagePagerAdapter(this, imagensLista)

        // 8) Botão contexto histórico
        findViewById<Button>(R.id.botaoContextoHistorico).setOnClickListener {
            Intent(this, ContextoHistoricoActivity::class.java).also {
                it.putExtra("nome", nome)
                it.putExtra("contextoHistorico", contextoHistorico)
                it.putStringArrayListExtra("imagens", imagensLista)
                startActivity(it)
            }
        }

        // 9) Botão rota
        findViewById<Button>(R.id.botaoVerRota).setOnClickListener {
            if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()) {
                val uri = Uri.parse("geo:0,0?q=$latitude,$longitude($nome)")
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }.let { mapIntent ->
                    startActivity(
                        if (mapIntent.resolveActivity(packageManager) != null)
                            mapIntent
                        else
                            Intent(Intent.ACTION_VIEW, uri)
                    )
                }
            } else {
                Toast.makeText(this, "Coordenadas indisponíveis.", Toast.LENGTH_SHORT).show()
            }
        }

        // 10) Botão horários
        findViewById<Button>(R.id.btnVerHorarios).setOnClickListener {
            Intent(this, HorariosActivity::class.java).apply {
                putExtra("estacaoId", estacaoId)
                startActivity(this)
            }
        }

        // 11) FAVORITOS (usa auth e database já inicializados)
        val user = auth.currentUser
        if (user != null) {
            val btnFavorito = findViewById<ImageView>(R.id.botaoFavorito)
            val favRef      = database.child("Favoritos").child(user.uid).child(estacaoId)
            var isFav       = false

            fun atualizarIcone() {
                btnFavorito.setImageResource(
                    if (isFav) R.drawable.ic_star else R.drawable.ic_star_border
                )
            }

            favRef.get().addOnSuccessListener {
                isFav = it.exists()
                atualizarIcone()
            }
            btnFavorito.setOnClickListener {
                if (isFav) favRef.removeValue()
                else favRef.setValue(true)
                isFav = !isFav
                atualizarIcone()
            }
        }

        // 12) COMENTÁRIOS
        val edtComentario  = findViewById<EditText>(R.id.editarComentario)
        val ratingBar      = findViewById<RatingBar>(R.id.estacaoRatingBar)
        val btnEnviar      = findViewById<Button>(R.id.enviarComentarioButton)
        val txtComentarios = findViewById<TextView>(R.id.comentariosListagem)

        fun carregarComentarios() {
            database.child("Comentarios").child(estacaoId).get()
                .addOnSuccessListener { snap ->
                    val sb = StringBuilder()
                    snap.children.forEach { c ->
                        val n  = c.child("nome").value.toString()
                        val t  = c.child("comentario").value.toString()
                        val av = c.child("avaliacao").value.toString().toIntOrNull() ?: 0
                        sb.append("⭐".repeat(av))
                            .append("\n$n: $t\n\n")
                    }
                    txtComentarios.text = sb.toString()
                }
        }
        btnEnviar.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            val texto = edtComentario.text.toString()
            val av    = ratingBar.rating.toInt()
            database.child("Comentarios")
                .child(estacaoId)
                .child(user.uid)
                .setValue(mapOf(
                    "nome"       to (user.displayName ?: user.email),
                    "comentario" to texto,
                    "avaliacao"  to av
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, "Comentário enviado!", Toast.LENGTH_SHORT).show()
                    edtComentario.text.clear()
                    carregarComentarios()
                }
        }
        carregarComentarios()
    }
}
