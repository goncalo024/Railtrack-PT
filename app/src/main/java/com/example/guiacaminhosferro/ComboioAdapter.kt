package com.example.guiacaminhosferro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guiacaminhosferro.R
import com.example.guiacaminhosferro.model.Comboio

class ComboioAdapter(private val items: List<Comboio>) :
    RecyclerView.Adapter<ComboioAdapter.VH>() {

    inner class VH(view: View): RecyclerView.ViewHolder(view) {
        val tvTipo       = view.findViewById<TextView>(R.id.tvTipo)
        val tvOrigem     = view.findViewById<TextView>(R.id.tvOrigem)
        val tvDestino    = view.findViewById<TextView>(R.id.tvDestino)
        val tvHora       = view.findViewById<TextView>(R.id.tvHora)
        val tvPlatform   = view.findViewById<TextView>(R.id.tvPlatform)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comboio, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val c = items[pos]

        holder.tvTipo.text = c.type.orEmpty()
        holder.tvOrigem.text = c.origin.orEmpty()
        holder.tvDestino.text = c.destination.orEmpty()

        // exibe hora chegada ou partida, conforme o que existe
        holder.tvHora.text = when {
            !c.arrival.isNullOrEmpty()  -> "Chegada: ${c.arrival}"
            !c.departure.isNullOrEmpty() -> "Saída: ${c.departure}"
            else                         -> ""
        }

        holder.tvPlatform.text = c.platform?.let { it } ?: ""
    }

    override fun getItemCount() = items.size
}