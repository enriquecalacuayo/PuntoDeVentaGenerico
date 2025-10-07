package com.example.puntodeventagenerico.ui.agregarproducto

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.*
import kotlinx.coroutines.launch

class AgregarProductoActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    // Subcategorías
    private lateinit var spinnerSubcategoria: Spinner
    private lateinit var adaptadorSubcategorias: ArrayAdapter<String>
    private var listaSubcategorias = mutableListOf<SubcategoriaEntity>()

    // Personalizaciones
    private lateinit var etNuevaPersonalizacion: EditText
    private lateinit var etCostoExtra: EditText
    private lateinit var btnAgregarPersonalizacion: Button
    private lateinit var listViewPersonalizaciones: ListView
    private lateinit var adaptadorPersonalizaciones: ArrayAdapter<String>
    private val listaPersonalizaciones = mutableListOf<String>()

    // Campos de producto
    private lateinit var etNombre: EditText
    private lateinit var etPrecioPublico: EditText
    private lateinit var etCostoUnitario: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        // Inicializar base de datos con migraciones destructivas
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "punto_venta_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        // Referencias a vistas
        etNombre = findViewById(R.id.etNombreProducto)
        etPrecioPublico = findViewById(R.id.etPrecioPublico)
        etCostoUnitario = findViewById(R.id.etCostoUnitario)
        btnGuardar = findViewById(R.id.btnGuardarProducto)

        // Subcategorías
        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria)
        val btnNueva = findViewById<Button>(R.id.btnNuevaSubcategoria)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarSubcategoria)
        cargarSubcategorias()
        btnNueva.setOnClickListener { mostrarDialogNuevaSubcategoria() }
        btnEliminar.setOnClickListener { eliminarSubcategoriaSeleccionada() }

        // Personalizaciones
        etNuevaPersonalizacion = findViewById(R.id.etNuevaPersonalizacion)
        etCostoExtra = findViewById(R.id.etCostoExtra)
        btnAgregarPersonalizacion = findViewById(R.id.btnAgregarPersonalizacion)
        listViewPersonalizaciones = findViewById(R.id.listViewPersonalizaciones)

        adaptadorPersonalizaciones = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        listViewPersonalizaciones.adapter = adaptadorPersonalizaciones

        btnAgregarPersonalizacion.setOnClickListener {
            val descripcion = etNuevaPersonalizacion.text.toString().trim()
            val costoTexto = etCostoExtra.text.toString().trim()
            val costo = costoTexto.toDoubleOrNull() ?: 0.0

            if (descripcion.isNotEmpty()) {
                val textoMostrar = if (costo > 0.0)
                    "$descripcion (+$${String.format("%.2f", costo)})"
                else
                    descripcion

                listaPersonalizaciones.add("$descripcion|$costo") // almacenamos ambos valores internamente
                adaptadorPersonalizaciones.add(textoMostrar)

                etNuevaPersonalizacion.text.clear()
                etCostoExtra.text.clear()
            } else {
                Toast.makeText(this, "Ingresa una descripción válida", Toast.LENGTH_SHORT).show()
            }
        }

        // Eliminar personalización al mantener presionado
        listViewPersonalizaciones.setOnItemLongClickListener { _, _, position, _ ->
            val item = adaptadorPersonalizaciones.getItem(position)
            AlertDialog.Builder(this)
                .setTitle("Eliminar personalización")
                .setMessage("¿Deseas eliminar '$item'?")
                .setPositiveButton("Sí") { _, _ ->
                    listaPersonalizaciones.removeAt(position)
                    adaptadorPersonalizaciones.remove(item)
                    adaptadorPersonalizaciones.notifyDataSetChanged()
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        // Guardar producto
        btnGuardar.setOnClickListener { guardarProducto() }
    }

    // ======================
    // FUNCIONES DE APOYO
    // ======================

    private fun cargarSubcategorias() {
        lifecycleScope.launch {
            listaSubcategorias = db.subcategoriaDao().obtenerTodas().toMutableList()
            val nombres = listaSubcategorias.map { it.nombre }
            adaptadorSubcategorias = ArrayAdapter(
                this@AgregarProductoActivity,
                android.R.layout.simple_spinner_item,
                nombres
            )
            adaptadorSubcategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSubcategoria.adapter = adaptadorSubcategorias
        }
    }

    private fun mostrarDialogNuevaSubcategoria() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Nueva subcategoría")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.subcategoriaDao().insertar(SubcategoriaEntity(nombre = nombre))
                        cargarSubcategorias()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarSubcategoriaSeleccionada() {
        val posicion = spinnerSubcategoria.selectedItemPosition
        if (posicion >= 0 && listaSubcategorias.isNotEmpty()) {
            val sub = listaSubcategorias[posicion]
            lifecycleScope.launch {
                db.subcategoriaDao().eliminar(sub)
                cargarSubcategorias()
            }
        }
    }

    private fun guardarProducto() {
        val nombre = etNombre.text.toString().trim()
        val precioPublico = etPrecioPublico.text.toString().toDoubleOrNull() ?: 0.0
        val costoUnitario = etCostoUnitario.text.toString().toDoubleOrNull() ?: 0.0

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa un nombre de producto", Toast.LENGTH_SHORT).show()
            return
        }

        val subcategoriaSeleccionada =
            if (listaSubcategorias.isNotEmpty() && spinnerSubcategoria.selectedItemPosition >= 0)
                listaSubcategorias[spinnerSubcategoria.selectedItemPosition]
            else null

        lifecycleScope.launch {
            val producto = ProductoEntity(
                nombre = nombre,
                categoria = subcategoriaSeleccionada?.nombre ?: "Sin categoría",
                precioPublico = precioPublico,
                costoUnitario = costoUnitario
            )

            val productoId = db.productoDao().insertar(producto).toInt()

            for (p in listaPersonalizaciones) {
                val partes = p.split("|")
                val descripcion = partes[0]
                val costo = if (partes.size > 1) partes[1].toDoubleOrNull() ?: 0.0 else 0.0

                db.personalizacionDao().insertar(
                    PersonalizacionEntity(
                        productoId = productoId,
                        descripcion = descripcion,
                        costoExtra = costo
                    )
                )
            }

            runOnUiThread {
                Toast.makeText(
                    this@AgregarProductoActivity,
                    "Producto guardado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                limpiarCampos()
            }
        }
    }

    private fun limpiarCampos() {
        etNombre.text.clear()
        etPrecioPublico.text.clear()
        etCostoUnitario.text.clear()
        etNuevaPersonalizacion.text.clear()
        etCostoExtra.text.clear()
        listaPersonalizaciones.clear()
        adaptadorPersonalizaciones.clear()
        adaptadorPersonalizaciones.notifyDataSetChanged()
    }
}
