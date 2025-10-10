package com.example.puntodeventagenerico.ui.venta

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.CarritoItem
import android.app.AlertDialog
import android.widget.CheckBox
import android.widget.EditText


class CarritoAdapter(
    private val context: Context,
    private val carrito: MutableList<CarritoItem>,
    private val onCarritoActualizado: () -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = carrito.size
    override fun getItem(position: Int): Any = carrito[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_carrito, parent, false)

        val item = carrito[position]

        val txtNombreProducto = view.findViewById<TextView>(R.id.txtNombreProducto)
        val txtPersonalizaciones = view.findViewById<TextView>(R.id.txtPersonalizaciones)
        val txtCantidad = view.findViewById<TextView>(R.id.txtCantidad)
        val txtSubtotal = view.findViewById<TextView>(R.id.txtSubtotal)
        val btnSumar = view.findViewById<ImageButton>(R.id.btnSumar)
        val btnRestar = view.findViewById<ImageButton>(R.id.btnRestar)
        val btnEliminar = view.findViewById<ImageButton>(R.id.btnEliminar)
        val btnAgregarComentario = view.findViewById<Button>(R.id.btnAgregarComentario)


        // ✅ Mostrar solo el nombre (con o sin paréntesis, según ya venga)
        txtNombreProducto.text = item.producto.nombre

        // ✅ No mostrar nada si no hay personalizaciones
        if (item.personalizaciones.isEmpty()) {
            txtPersonalizaciones.visibility = View.GONE
        } else {
            txtPersonalizaciones.visibility = View.GONE // No las mostramos porque ya están en el nombre
        }

        txtCantidad.text = item.cantidad.toString()
        txtSubtotal.text = "$${String.format("%.2f", item.producto.precioPublico * item.cantidad)}"

        // Botón +
        btnSumar.setOnClickListener {
            item.cantidad++
            txtCantidad.text = item.cantidad.toString()
            txtSubtotal.text =
                "$${String.format("%.2f", item.producto.precioPublico * item.cantidad)}"
            onCarritoActualizado()
        }

        // Botón –
        btnRestar.setOnClickListener {
            if (item.cantidad > 1) {
                item.cantidad--
                txtCantidad.text = item.cantidad.toString()
                txtSubtotal.text =
                    "$${String.format("%.2f", item.producto.precioPublico * item.cantidad)}"
                onCarritoActualizado()
            } else {
                carrito.removeAt(position)
                notifyDataSetChanged()
                onCarritoActualizado()
                Toast.makeText(context, "Producto eliminado del carrito", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón 🗑️
        btnEliminar.setOnClickListener {
            carrito.removeAt(position)
            notifyDataSetChanged()
            onCarritoActualizado()
            Toast.makeText(context, "Producto eliminado del carrito", Toast.LENGTH_SHORT).show()
        }

        btnAgregarComentario.setOnClickListener {
            val input = EditText(context)
            input.hint = "Ej. bien caliente, sin azúcar..."
            input.setText(item.comentario)

            AlertDialog.Builder(context)
                .setTitle("Agregar comentario")
                .setView(input)
                .setPositiveButton("Guardar") { _, _ ->
                    item.comentario = input.text.toString().trim()
                    notifyDataSetChanged()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        return view
    }
}
