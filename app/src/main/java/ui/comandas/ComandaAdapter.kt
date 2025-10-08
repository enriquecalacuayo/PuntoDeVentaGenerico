package com.example.puntodeventagenerico.ui.comandas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.ComandaEntity

class ComandaAdapter(private val comandas: List<ComandaEntity>) :
    RecyclerView.Adapter<ComandaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDescripcion: TextView = view.findViewById(R.id.txtDescripcionComanda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comanda, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comanda = comandas[position]
        holder.txtDescripcion.text = comanda.descripcion
    }

    override fun getItemCount() = comandas.size
}
