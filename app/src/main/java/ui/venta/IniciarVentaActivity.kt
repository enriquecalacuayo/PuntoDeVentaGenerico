package com.example.puntodeventagenerico.ui.venta

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.*
import kotlinx.coroutines.launch

class IniciarVentaActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var spinnerSubcategoria: Spinner
    private lateinit var listViewProductos: ListView
    private lateinit var listViewCarrito: ListView
    private lateinit var tvTotal: TextView

    private val carrito = mutableListOf<CarritoItem>()
    private lateinit var adaptadorCarrito: ArrayAdapter<String>

    private var listaSubcategorias = listOf<SubcategoriaEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciar_venta)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "punto_venta_db"
        ).fallbackToDestructiveMigration().build()

        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria)
        listViewProductos = findViewById(R.id.listViewProductos)
        listViewCarrito = findViewById(R.id.listViewCarrito)
        tvTotal = findViewById(R.id.tvTotal)

        adaptadorCarrito = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listViewCarrito.adapter = adaptadorCarrito

        cargarSubcategorias()
    }

    private fun cargarSubcategorias() {
        lifecycleScope.launch {
            listaSubcategorias = db.subcategoriaDao().obtenerTodas()
            val nombres = listaSubcategorias.map { it.nombre }

            runOnUiThread {
                val adaptador = ArrayAdapter(this@IniciarVentaActivity, android.R.layout.simple_spinner_item, nombres)
                adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSubcategoria.adapter = adaptador

                spinnerSubcategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        val subcategoriaSeleccionada = nombres[position]
                        cargarProductosPorSubcategoria(subcategoriaSeleccionada)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun cargarProductosPorSubcategoria(nombreSub: String) {
        lifecycleScope.launch {
            val productos = db.productoDao().obtenerPorCategoria(nombreSub)
            runOnUiThread {
                val nombres = productos.map { it.nombre }
                val adaptador = ArrayAdapter(this@IniciarVentaActivity, android.R.layout.simple_list_item_1, nombres)
                listViewProductos.adapter = adaptador

                listViewProductos.setOnItemClickListener { _, _, position, _ ->
                    val producto = productos[position]
                    mostrarDialogPersonalizaciones(producto)
                }
            }
        }
    }

    private fun mostrarDialogPersonalizaciones(producto: ProductoEntity) {
        lifecycleScope.launch {
            val personalizaciones = db.personalizacionDao().obtenerPorProducto(producto.id)

            if (personalizaciones.isEmpty()) {
                agregarAlCarrito(producto, emptyList())
                return@launch
            }

            val nombres = personalizaciones.map {
                if (it.costoExtra > 0) "${it.descripcion} (+$${it.costoExtra})"
                else it.descripcion
            }.toTypedArray()

            val seleccionados = BooleanArray(personalizaciones.size)

            runOnUiThread {
                AlertDialog.Builder(this@IniciarVentaActivity)
                    .setTitle("Personalizar ${producto.nombre}")
                    .setMultiChoiceItems(nombres, seleccionados) { _, which, isChecked ->
                        seleccionados[which] = isChecked
                    }
                    .setPositiveButton("Agregar") { _, _ ->
                        val seleccion = personalizaciones.filterIndexed { i, _ -> seleccionados[i] }
                        agregarAlCarrito(producto, seleccion)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun agregarAlCarrito(producto: ProductoEntity, personalizaciones: List<PersonalizacionEntity>) {
        val existente = carrito.find {
            it.producto.id == producto.id && it.personalizaciones.map { p -> p.descripcion } == personalizaciones.map { p -> p.descripcion }
        }

        if (existente != null) {
            existente.cantidad++
        } else {
            carrito.add(CarritoItem(producto, personalizaciones))
        }

        actualizarCarrito()
    }

    private fun actualizarCarrito() {
        val listaTexto = carrito.map { it.descripcionCompleta() }
        adaptadorCarrito.clear()
        adaptadorCarrito.addAll(listaTexto)
        adaptadorCarrito.notifyDataSetChanged()

        val total = carrito.sumOf { it.calcularSubtotal() }
        tvTotal.text = "Total: $${String.format("%.2f", total)}"

        listViewCarrito.setOnItemClickListener { _, _, position, _ ->
            mostrarOpcionesCarrito(position)
        }
    }

    private fun mostrarOpcionesCarrito(pos: Int) {
        val item = carrito[pos]
        val opciones = arrayOf("➕ Aumentar", "➖ Disminuir", "❌ Eliminar")

        AlertDialog.Builder(this)
            .setTitle(item.producto.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> item.cantidad++
                    1 -> if (item.cantidad > 1) item.cantidad-- else carrito.removeAt(pos)
                    2 -> carrito.removeAt(pos)
                }
                actualizarCarrito()
            }
            .show()
    }
}
