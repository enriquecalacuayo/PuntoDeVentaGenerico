package com.example.puntodeventagenerico.ui.comandas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.ComandaEntity

class ComandaAdapter(private val comandas: MutableList<ComandaConEstado>) :
    RecyclerView.Adapter<ComandaAdapter.ViewHolder>() {

    private var onLongClickListener: ((ComandaEntity) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDescripcion: TextView = view.findViewById(R.id.txtDescripcionComanda)
        val btnAtender: Button = view.findViewById(R.id.btnAtender)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comanda, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = comandas[position]

        holder.txtDescripcion.text = item.comanda.descripcion

        if (item.esNueva) {
            // Estado NUEVA: fondo naranja, texto blanco, botón Atender visible
            holder.itemView.setBackgroundResource(R.drawable.bg_item_nueva)
            holder.txtDescripcion.setTextColor(0xFFFFFFFF.toInt())
            holder.btnAtender.visibility = View.VISIBLE
            holder.btnAtender.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFFE65100.toInt())
        } else {
            // Estado NORMAL: fondo blanco, texto negro, sin botón Atender
            holder.itemView.setBackgroundResource(R.drawable.bg_item)
            holder.txtDescripcion.setTextColor(0xFF000000.toInt())
            holder.btnAtender.visibility = View.GONE
        }

        // Botón Atender → quita la alerta visual
        holder.btnAtender.setOnClickListener {
            item.esNueva = false
            notifyItemChanged(holder.adapterPosition)
        }

        // Long press → eliminar comanda
        holder.itemView.setOnLongClickListener {
            onLongClickListener?.invoke(item.comanda)
            true
        }
    }

    override fun getItemCount() = comandas.size

    fun setOnItemLongClickListener(listener: (ComandaEntity) -> Unit) {
        onLongClickListener = listener
    }
}
