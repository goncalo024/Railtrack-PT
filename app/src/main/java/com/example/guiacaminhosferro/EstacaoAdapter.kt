package com.example.guiacaminhosferro

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.guiacaminhosferro.Estacao
import com.example.guiacaminhosferro.DetalheEstacaoActivity
import java.text.Normalizer

class EstacaoAdapter(
    private val listaOriginal: List<Estacao>,
    private val verMaisListener: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    private var listaFiltrada: MutableList<Estacao> = listaOriginal.toMutableList()
    private var mostrarBotaoVerMais: Boolean = true

    companion object {
        private const val VIEW_TYPE_ESTACAO = 0
        private const val VIEW_TYPE_VER_MAIS = 1
    }

    inner class EstacaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome       : TextView  = itemView.findViewById(R.id.textNome)
        val morada     : TextView  = itemView.findViewById(R.id.textMorada)
        val descricao  : TextView  = itemView.findViewById(R.id.textDescricao)
        val imagemView : ImageView = itemView.findViewById(R.id.imageViewEstacao)
    }

    inner class VerMaisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnVerMais: Button = itemView.findViewById(R.id.btnVerMais)
    }

    override fun getItemViewType(position: Int): Int {
        return if (mostrarBotaoVerMais
            && verMaisListener != null
            && position == listaFiltrada.size
        ) VIEW_TYPE_VER_MAIS else VIEW_TYPE_ESTACAO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_ESTACAO) {
            val view = inflater.inflate(R.layout.item_estacao, parent, false)
            EstacaoViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_ver_mais, parent, false)
            VerMaisViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is EstacaoViewHolder) {
            val estacao = listaFiltrada[position]
            holder.nome.text      = estacao.nome ?: "Sem Nome"
            holder.morada.text    = estacao.morada ?: "Sem Morada"
            holder.descricao.text = estacao.descricao ?: ""

            // Pega primeira URL (separada por ;)
            val primeiraImagem: String? = estacao.imagens
                ?.split(Regex("""\s*;\s*"""))
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }

            if (!primeiraImagem.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(primeiraImagem)
                    .placeholder(R.drawable.placeholder_imagem)
                    .error(R.drawable.placeholder_imagem)
                    .into(holder.imagemView)
            } else {
                holder.imagemView.setImageResource(R.drawable.placeholder_imagem)
            }

            holder.itemView.setOnClickListener {
                val ctx = holder.itemView.context
                val intent = Intent(ctx, DetalheEstacaoActivity::class.java).apply {
                    putExtra("estacaoId",         estacao.id)
                    putExtra("nome",              estacao.nome)
                    putExtra("morada",            estacao.morada)
                    putExtra("descricao",         estacao.descricao)
                    putExtra("contextoHistorico", estacao.contextoHistorico)
                    putExtra("latitude",          estacao.latitude)
                    putExtra("longitude",         estacao.longitude)

                    // lista de imagens como ArrayList<String>
                    val imgs = estacao.imagens
                        ?.split(Regex("""\s*;\s*"""))
                        ?: emptyList()
                    putStringArrayListExtra("imagens", ArrayList(imgs))
                }
                ctx.startActivity(intent)
            }
        } else if (holder is VerMaisViewHolder) {
            holder.btnVerMais.setOnClickListener {
                verMaisListener?.invoke()
            }
        }
    }

    override fun getItemCount(): Int {
        val extra = if (mostrarBotaoVerMais && verMaisListener != null) 1 else 0
        return listaFiltrada.size + extra
    }

    /**
     * Atualiza a lista exibida e controla se o botão “Ver Mais” deve aparecer.
     */
    fun atualizarLista(novaLista: List<Estacao>, mostrarBotao: Boolean = true) {
        listaFiltrada      = novaLista.toMutableList()
        mostrarBotaoVerMais = mostrarBotao
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint
                ?.toString()
                ?.normalizar()
                ?.trim()

            val resultados = if (query.isNullOrEmpty()) {
                listaOriginal
            } else {
                listaOriginal.filter {
                    it.nome?.normalizar()?.contains(query) == true ||
                            it.morada?.normalizar()?.contains(query) == true
                }
            }

            return FilterResults().apply { values = resultados }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            listaFiltrada      = (results?.values as? List<Estacao>)?.toMutableList() ?: mutableListOf()
            mostrarBotaoVerMais = false
            notifyDataSetChanged()
        }
    }

    // Remove acentuação e converte para minúsculas
    private fun String.normalizar(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
    }
}
