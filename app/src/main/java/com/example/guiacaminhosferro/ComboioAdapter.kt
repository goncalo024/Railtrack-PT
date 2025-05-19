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
        val txtNumero: TextView = view.findViewById(R.id.txtNumeroComboio)
        val txtHorario: TextView = view.findViewById(R.id.txtHorario)
        val txtTipo: TextView   = view.findViewById(R.id.txtTipoComboio)
        val txtPlataforma: TextView = view.findViewById(R.id.txtPlataforma)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comboio, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.txtNumero.text = c.number
        holder.txtHorario.text = "${c.departure} → ${c.arrival}"
        holder.txtTipo.text = c.type
        holder.txtPlataforma.text = "Plataforma ${c.platform}"
    }

    override fun getItemCount() = items.size
}
