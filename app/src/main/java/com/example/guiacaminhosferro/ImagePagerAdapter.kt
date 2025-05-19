package com.example.guiacaminhosferro

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImagePagerAdapter(private val context: Context, private val imagens: List<String>) :
    RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagemView: ImageView = itemView.findViewById(R.id.imagemViewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_imagem, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagem = imagens[position]

        if (imagem == "placeholder" || imagem.isBlank()) {
            // não há URL real -> exibe recurso local
            holder.imagemView.setImageResource(R.drawable.placeholder_imagem)
        } else {
            Glide.with(context)
                .load(imagem)
                .placeholder(R.drawable.placeholder_imagem)
                .error(R.drawable.placeholder_imagem)
                .into(holder.imagemView)
        }
    }

    override fun getItemCount(): Int = imagens.size
}
