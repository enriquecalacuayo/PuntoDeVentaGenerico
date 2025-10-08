package com.example.puntodeventagenerico.ui.vereditarproductos

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.*
import kotlinx.coroutines.launch

class VerEditarProductosActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var spinnerSubcategoria: Spinner
    private lateinit var listViewProductos: ListView

    private var listaSubcategorias = listOf<SubcategoriaEntity>()
    private var listaProductos = listOf<ProductoEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_editar_productos)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "punto_venta_db"
        ).fallbackToDestructiveMigration().build()

        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria)
        listViewProductos = findViewById(R.id.listViewProductos)

        cargarSubcategorias()
    }

    private fun cargarSubcategorias() {
        lifecycleScope.launch {
            listaSubcategorias = db.subcategoriaDao().obtenerTodas()
            val nombres = listaSubcategorias.map { it.nombre }

            runOnUiThread {
                val adaptador = ArrayAdapter(this@VerEditarProductosActivity, android.R.layout.simple_spinner_item, nombres)
                adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSubcategoria.adapter = adaptador

                spinnerSubcategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        val subcategoriaSeleccionada = nombres[position]
                        cargarProductos(subcategoriaSeleccionada)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun cargarProductos(nombreSub: String) {
        lifecycleScope.launch {
            listaProductos = db.productoDao().obtenerPorCategoria(nombreSub)
            val nombres = listaProductos.map { "${it.nombre}  -  $${it.precioPublico}" }

            runOnUiThread {
                val adaptador = ArrayAdapter(this@VerEditarProductosActivity, android.R.layout.simple_list_item_1, nombres)
                listViewProductos.adapter = adaptador

                listViewProductos.setOnItemClickListener { _, _, position, _ ->
                    mostrarDialogEditarProducto(listaProductos[position])
                }
            }
        }
    }

    private fun mostrarDialogEditarProducto(producto: ProductoEntity) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_producto, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etPrecio = view.findViewById<EditText>(R.id.etPrecio)
        val etCosto = view.findViewById<EditText>(R.id.etCosto)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)

        etNombre.setText(producto.nombre)
        etPrecio.setText(producto.precioPublico.toString())
        etCosto.setText(producto.costoUnitario.toString())

        lifecycleScope.launch {
            val categorias = db.subcategoriaDao().obtenerTodas()
            val nombres = categorias.map { it.nombre }

            runOnUiThread {
                val adaptador = ArrayAdapter(this@VerEditarProductosActivity, android.R.layout.simple_spinner_item, nombres)
                adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategoria.adapter = adaptador
                spinnerCategoria.setSelection(nombres.indexOf(producto.categoria))
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Editar producto")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                val nuevoPrecio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
                val nuevoCosto = etCosto.text.toString().toDoubleOrNull() ?: 0.0
                val nuevaCategoria = spinnerCategoria.selectedItem.toString()

                lifecycleScope.launch {
                    db.productoDao().actualizar(
                        producto.copy(
                            nombre = nuevoNombre,
                            precioPublico = nuevoPrecio,
                            costoUnitario = nuevoCosto,
                            categoria = nuevaCategoria
                        )
                    )
                    cargarProductos(nuevaCategoria)
                }
            }
            .setNegativeButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    db.productoDao().eliminar(producto)
                    cargarProductos(producto.categoria)
                }
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
}
