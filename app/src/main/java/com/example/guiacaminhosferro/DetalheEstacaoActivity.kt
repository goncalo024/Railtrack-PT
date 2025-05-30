package com.example.guiacaminhosferro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DetalheEstacaoActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // Views
    private lateinit var viewPager: ViewPager2
    private lateinit var ivPlaceholder: ImageView
    private lateinit var btnContexto: Button
    private lateinit var btnRota: Button
    private lateinit var btnHorarios: Button
    private lateinit var btnFavorito: ImageView

    private lateinit var headerAvaliacoes: LinearLayout
    private lateinit var contentAvaliacoes: LinearLayout
    private lateinit var setaToggle: ImageView

    private lateinit var edtComentario: EditText
    private lateinit var ratingBar: RatingBar
    private lateinit var btnEnviar: Button
    private lateinit var comentariosListagem: LinearLayout

    // estado de favorito
    private var isFav = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_estacao)

        // 1) inicializa Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // 2) recebe estacaoId
        val estacaoId = intent.getStringExtra("estacaoId")
        if (estacaoId.isNullOrEmpty()) {
            Toast.makeText(this, "ID da estação ausente", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 3) extras
        val nome              = intent.getStringExtra("nome").orEmpty()
        val morada            = intent.getStringExtra("morada").orEmpty()
        val descricao         = intent.getStringExtra("descricao").orEmpty()
        val contextoHistorico = intent.getStringExtra("contextoHistorico").orEmpty()
        val latitude          = intent.getStringExtra("latitude")
        val longitude         = intent.getStringExtra("longitude")
        val imagensLista      = intent.getStringArrayListExtra("imagens") ?: arrayListOf()

        // 4) toolbar
        findViewById<MaterialToolbar>(R.id.toolbarDetalhe).apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener { finish() }
            title = nome
        }

        // 5) textos
        findViewById<TextView>(R.id.estacaoNome).text      = nome
        findViewById<TextView>(R.id.estacaoMorada).text    = morada
        findViewById<TextView>(R.id.estacaoDescricao).text = descricao

        // 6) pager + placeholder
        viewPager     = findViewById(R.id.viewPagerImagens)
        ivPlaceholder = findViewById(R.id.ivPlaceholder)
        if (imagensLista.isEmpty()) {
            ivPlaceholder.visibility = View.VISIBLE
            viewPager.visibility    = View.GONE
        } else {
            ivPlaceholder.visibility = View.GONE
            viewPager.visibility    = View.VISIBLE
            viewPager.adapter       = ImagePagerAdapter(this, imagensLista)
        }

        // 7) context histórico
        btnContexto = findViewById(R.id.botaoContextoHistorico)
        btnContexto.setOnClickListener {
            if (contextoHistorico.isBlank()) {
                Toast.makeText(this, "Contexto histórico indisponível para esta estação", Toast.LENGTH_SHORT).show()
            } else {
                Intent(this, ContextoHistoricoActivity::class.java).also {
                    it.putExtra("nome", nome)
                    it.putExtra("contextoHistorico", contextoHistorico)
                    it.putStringArrayListExtra("imagens", imagensLista)
                    startActivity(it)
                }
            }
        }

        // 8) rota
        btnRota = findViewById(R.id.botaoVerRota)
        btnRota.setOnClickListener {
            if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()) {
                val uri = Uri.parse("geo:0,0?q=$latitude,$longitude($nome)")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
                startActivity(if (mapIntent.resolveActivity(packageManager) != null) mapIntent else Intent(Intent.ACTION_VIEW, uri))
            } else {
                Toast.makeText(this, "Coordenadas indisponíveis.", Toast.LENGTH_SHORT).show()
            }
        }

        // 9) horários
        btnHorarios = findViewById(R.id.btnVerHorarios)
        btnHorarios.setOnClickListener {
            Intent(this, HorariosActivity::class.java).apply {
                putExtra("estacaoId", estacaoId)
                putExtra("stationName", nome)
                startActivity(this)
            }
        }

        // 10) favoritos
        btnFavorito = findViewById(R.id.botaoFavorito)
        val user = auth.currentUser
        if (user != null) {
            val favRef = database.child("Favoritos")
                .child(user.uid)
                .child(estacaoId)
            // atualiza ícone
            fun atualizarIcone() {
                btnFavorito.setImageResource(if (isFav) R.drawable.ic_star else R.drawable.ic_star_border)
            }
            // lê do Firebase
            favRef.get().addOnSuccessListener {
                isFav = it.exists()
                atualizarIcone()
            }.addOnFailureListener {
                Toast.makeText(this, "Falha ao ler favoritos", Toast.LENGTH_SHORT).show()
            }
            // ao clicar
            btnFavorito.setOnClickListener {
                if (isFav) favRef.removeValue()
                else      favRef.setValue(true)
                isFav = !isFav
                atualizarIcone()
            }
        } else {
            // esconde botão se não autenticado
            btnFavorito.visibility = View.GONE
        }

        // 11) avaliações e comentários
        headerAvaliacoes      = findViewById(R.id.avaliacoesHeader)
        contentAvaliacoes     = findViewById(R.id.avaliacoesContent)
        setaToggle            = findViewById(R.id.setaToggle)
        edtComentario         = findViewById(R.id.editarComentario)
        ratingBar             = findViewById(R.id.estacaoRatingBar)
        btnEnviar             = findViewById(R.id.enviarComentarioButton)
        comentariosListagem   = findViewById(R.id.comentariosListagem)

        headerAvaliacoes.setOnClickListener {
            if (contentAvaliacoes.visibility == View.GONE) {
                contentAvaliacoes.visibility = View.VISIBLE
                setaToggle.setImageResource(R.drawable.ic_arrow_up)
            } else {
                contentAvaliacoes.visibility = View.GONE
                setaToggle.setImageResource(R.drawable.ic_arrow_down)
            }
        }

        fun atualizarContagem(total: Long) {
            findViewById<TextView>(R.id.avaliacoesTitle).text = "Avaliações ($total)"
        }

        fun carregarComentarios() {
            comentariosListagem.removeAllViews()
            database.child("Comentarios")
                .child(estacaoId)
                .get()
                .addOnSuccessListener { snap ->
                    atualizarContagem(snap.childrenCount)
                    if (snap.childrenCount == 0L) {
                        comentariosListagem.addView(TextView(this).apply {
                            text = "Sem comentários."
                            setPadding(8, 8, 8, 8)
                        })
                    } else {
                        snap.children.forEach { c ->
                            val nomeUser = c.child("nome").getValue(String::class.java) ?: "Anónimo"
                            val texto    = c.child("comentario").getValue(String::class.java) ?: ""
                            val av       = (c.child("avaliacao").getValue(Int::class.java) ?: 0).toFloat()
                            val uidCmt   = c.key!!

                            // linha
                            val row = LinearLayout(this).apply {
                                orientation = LinearLayout.HORIZONTAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply { setMargins(0, 8, 0, 8) }
                                gravity = Gravity.CENTER_VERTICAL
                            }
                            // estrelas
                            val stars = RatingBar(this, null, android.R.attr.ratingBarStyleSmall).apply {
                                numStars    = 5
                                stepSize    = 1f
                                rating      = av
                                setIsIndicator(true)
                            }
                            row.addView(stars)
                            // texto
                            val tv = TextView(this).apply {
                                text = "$nomeUser: $texto"
                                setPadding(16, 0, 0, 0)
                                layoutParams = LinearLayout.LayoutParams(0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }
                            row.addView(tv)
                            // ícone de remover
                            if (auth.currentUser?.uid == uidCmt) {
                                val del = ImageButton(this).apply {
                                    setImageResource(R.drawable.ic_delete)
                                    background = null
                                    setOnClickListener {
                                        database.child("Comentarios")
                                            .child(estacaoId)
                                            .child(uidCmt)
                                            .removeValue()
                                            .addOnSuccessListener { carregarComentarios() }
                                    }
                                }
                                row.addView(del)
                            }
                            comentariosListagem.addView(row)
                        }
                    }
                }
                .addOnFailureListener {
                    comentariosListagem.addView(TextView(this).apply {
                        text = "Falha ao carregar comentários."
                        setPadding(8, 8, 8, 8)
                    })
                }
        }

        btnEnviar.setOnClickListener {
            val u = auth.currentUser
            if (u == null) {
                Toast.makeText(this, "Faça login para comentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val texto = edtComentario.text.toString().trim()
            val av    = ratingBar.rating.toInt()
            when {
                av == 0       -> { Toast.makeText(this, "Escolha uma classificação!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                texto.isBlank() -> { edtComentario.error = "Comentário vazio"; return@setOnClickListener }
            }
            database.child("Comentarios")
                .child(estacaoId)
                .child(u.uid)
                .setValue(mapOf(
                    "nome"       to (u.displayName ?: u.email ?: "Anónimo"),
                    "comentario" to texto,
                    "avaliacao"  to av
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, "Comentário enviado!", Toast.LENGTH_SHORT).show()
                    edtComentario.text.clear()
                    ratingBar.rating = 0f
                    carregarComentarios()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao enviar comentário.", Toast.LENGTH_SHORT).show()
                }
        }

        // primeira carga
        carregarComentarios()
    }
}
