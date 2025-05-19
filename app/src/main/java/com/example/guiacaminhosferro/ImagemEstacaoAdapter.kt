package com.example.guiacaminhosferro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImagemEstacaoAdapter(private val imagens: List<String>) :
    RecyclerView.Adapter<ImagemEstacaoAdapter.ImagemViewHolder>() {

    inner class ImagemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagemView: ImageView = view.findViewById(R.id.imagemEstacaoPager)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagem_pager, parent, false)
        return ImagemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImagemViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(imagens[position])
            .placeholder(R.drawable.placeholder_imagem)
            .into(holder.imagemView)
    }

    override fun getItemCount(): Int = imagens.size
}
