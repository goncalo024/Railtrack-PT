package com.example.guiacaminhosferro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DetalheEstacaoActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // Views
    private lateinit var viewPager: ViewPager2
    private lateinit var imageContainer: View
    private lateinit var btnContexto: Button
    private lateinit var btnRota: Button
    private lateinit var btnHorarios: Button

    private lateinit var headerAvaliacoes: LinearLayout
    private lateinit var contentAvaliacoes: LinearLayout
    private lateinit var setaToggle: ImageView

    private lateinit var edtComentario: EditText
    private lateinit var ratingBar: RatingBar
    private lateinit var btnEnviar: Button
    private lateinit var comentariosListagem: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_estacao)

        // 2) Inicializa Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // 3) Recebe o estacaoId e aborta se não existir
        val estacaoId = intent.getStringExtra("estacaoId")
        if (estacaoId.isNullOrEmpty()) {
            Toast.makeText(this, "ID da estação ausente", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 4) Extras
        val nome              = intent.getStringExtra("nome").orEmpty()
        val morada            = intent.getStringExtra("morada").orEmpty()
        val descricao         = intent.getStringExtra("descricao").orEmpty()
        val contextoHistorico = intent.getStringExtra("contextoHistorico").orEmpty()
        val latitude          = intent.getStringExtra("latitude")
        val longitude         = intent.getStringExtra("longitude")
        val imagensLista      = intent.getStringArrayListExtra("imagens") ?: arrayListOf()

        // 5) Toolbar com voltar
        findViewById<MaterialToolbar>(R.id.toolbarDetalhe).apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener { finish() }
            title = nome
        }

        // 6) Preenche textos
        findViewById<TextView>(R.id.estacaoNome).text      = nome
        findViewById<TextView>(R.id.estacaoMorada).text    = morada
        findViewById<TextView>(R.id.estacaoDescricao).text = descricao

        // 7) ViewPager de imagens + esconder se não houver
        imageContainer = findViewById(R.id.imageContainer)
        viewPager      = findViewById(R.id.viewPagerImagens)

        if (imagensLista.isEmpty()) {
            imageContainer.visibility = View.GONE
        } else {
            imageContainer.visibility = View.VISIBLE
            viewPager.adapter         = ImagePagerAdapter(this, imagensLista)

        // 8) Botão contexto histórico
        btnContexto = findViewById(R.id.botaoContextoHistorico)
        btnContexto.setOnClickListener {
            Intent(this, ContextoHistoricoActivity::class.java).also {
                it.putExtra("nome", nome)
                it.putExtra("contextoHistorico", contextoHistorico)
                it.putStringArrayListExtra("imagens", imagensLista)
                startActivity(it)
            }
        }

        // 9) Botão rota
        btnRota = findViewById(R.id.botaoVerRota)
        btnRota.setOnClickListener {
            if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()) {
                val uri = Uri.parse("geo:0,0?q=$latitude,$longitude($nome)")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    .setPackage("com.google.android.apps.maps")
                startActivity(
                    if (mapIntent.resolveActivity(packageManager) != null)
                        mapIntent else Intent(Intent.ACTION_VIEW, uri)
                )
            } else {
                Toast.makeText(this, "Coordenadas indisponíveis.", Toast.LENGTH_SHORT).show()
            }
        }

        // 10) Botão horários (remove margem extra)
        btnHorarios = findViewById(R.id.btnVerHorarios)
        // se queres manter a mesma aparência dos outros, podes remover o marginTop no XML
        btnHorarios.setOnClickListener {
            Intent(this, HorariosActivity::class.java).apply {
                putExtra("estacaoId", estacaoId)
                putExtra("stationName", nome)
                startActivity(this)
            }
        }

        // 11) FAVORITOS
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
                if (isFav) favRef.removeValue() else favRef.setValue(true)
                isFav = !isFav
                atualizarIcone()
            }
        }

        // 12) COMENTÁRIOS: bind views e toggle
        headerAvaliacoes   = findViewById(R.id.avaliacoesHeader)
        contentAvaliacoes  = findViewById(R.id.avaliacoesContent)
        setaToggle         = findViewById(R.id.setaToggle)

        comentariosListagem = findViewById(R.id.comentariosListagem)

        edtComentario      = findViewById(R.id.editarComentario)
        ratingBar          = findViewById(R.id.estacaoRatingBar)
        btnEnviar          = findViewById(R.id.enviarComentarioButton)

        headerAvaliacoes.setOnClickListener {
            if (contentAvaliacoes.visibility == View.GONE) {
                contentAvaliacoes.visibility = View.VISIBLE
                setaToggle.setImageResource(R.drawable.ic_arrow_up)
            } else {
                contentAvaliacoes.visibility = View.GONE
                setaToggle.setImageResource(R.drawable.ic_arrow_down)
            }
        }

        // função para mostrar o contador dinamicamente
        fun atualizarContagem(total: Long) {
            findViewById<TextView>(R.id.avaliacoesTitle).text =
                "Avaliações ($total)"
        }

        // Função para carregar comentários
        fun carregarComentarios() {
            comentariosListagem.removeAllViews()
            database.child("Comentarios")
                .child(estacaoId)
                .get()
                .addOnSuccessListener { snap ->
                    atualizarContagem(snap.childrenCount)
                    snap.children.forEach { c ->
                        val nome  = c.child("nome").getValue(String::class.java) ?: "Anónimo"
                        val texto = c.child("comentario").getValue(String::class.java) ?: ""
                        val av    = c.child("avaliacao").getValue(Int::class.java) ?: 0
                        val uid   = c.key

                        // linha horizontal
                        val row = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0,8,0,8) }
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        // estrelas
                        val stars = RatingBar(this,null,android.R.attr.ratingBarStyleSmall).apply {
                            numStars = 5
                            stepSize = 1f
                            rating   = av.toFloat()
                            setIsIndicator(true)
                        }
                        row.addView(stars)
                        // texto
                        val tv = TextView(this).apply {
                            text = "$nome: $texto"
                            setPadding(16,0,0,0)
                            layoutParams = LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,1f)
                        }
                        row.addView(tv)
                        // ícone de lixo para o próprio
                        if (auth.currentUser?.uid == uid) {
                            val del = ImageButton(this).apply {
                                setImageResource(R.drawable.ic_delete)
                                background = null
                                setOnClickListener {
                                    database.child("Comentarios")
                                        .child(estacaoId)
                                        .child(uid!!)
                                        .removeValue()
                                        .addOnSuccessListener { carregarComentarios() }
                                }
                            }
                            row.addView(del)
                        }
                        comentariosListagem.addView(row)
                    }
                    // se não houver, mostramos só uma label
                    if (snap.childrenCount == 0L) {
                        comentariosListagem.addView(TextView(this).apply {
                            text = "Sem comentários."
                            setPadding(8,8,8,8)
                        })
                    }
                }
                .addOnFailureListener {
                    comentariosListagem.addView(TextView(this).apply {
                        text = "Falha ao carregar comentários."
                        setPadding(8,8,8,8)
                    })
                }
        }


        // Envio de um novo comentário
        btnEnviar.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "Faça login para comentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val texto = edtComentario.text.toString().trim()
            val av    = ratingBar.rating.toInt()
            when {
                av == 0 -> {
                    Toast.makeText(this, "Escolha uma classificação!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                texto.isBlank() -> {
                    edtComentario.error = "Comentário vazio"
                    return@setOnClickListener
                }
            }
            // grava
            database.child("Comentarios")
                .child(estacaoId)
                .child(user.uid)
                .setValue(mapOf(
                    "nome"       to (user.displayName ?: user.email ?: "Anónimo"),
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

        // 1ª carga
        carregarComentarios()
    }
}
}
