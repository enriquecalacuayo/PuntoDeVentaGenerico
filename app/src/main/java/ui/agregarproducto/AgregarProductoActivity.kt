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
    private lateinit var etNuevaPersonalizacion: AutoCompleteTextView
    private lateinit var etCostoExtra: EditText
    private lateinit var btnAgregarPersonalizacion: Button
    private lateinit var listViewPersonalizaciones: ListView
    private lateinit var adaptadorPersonalizaciones: ArrayAdapter<String>
    private val listaPersonalizaciones = mutableListOf<String>()

    // Autocompletado historial
    private lateinit var adaptadorHistorial: ArrayAdapter<String>
    private var listaHistorial = mutableListOf<HistorialPersonalizacionEntity>()

    // Campos de producto
    private lateinit var etNombre: EditText
    private lateinit var etPrecioPublico: EditText
    private lateinit var etCostoUnitario: EditText
    private lateinit var btnGuardar: Button
    private lateinit var chkOcultarEnComandas: CheckBox // 👈 Nuevo campo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "punto_venta_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        // Referencias UI
        etNombre = findViewById(R.id.etNombreProducto)
        etPrecioPublico = findViewById(R.id.etPrecioPublico)
        etCostoUnitario = findViewById(R.id.etCostoUnitario)
        btnGuardar = findViewById(R.id.btnGuardarProducto)
        chkOcultarEnComandas = findViewById(R.id.chkOcultarEnComandas) // 👈 Referencia al nuevo checkbox

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

        // Cargar historial de personalizaciones previas
        cargarHistorialPersonalizaciones()

        // Cuando se elija una sugerencia del historial
        etNuevaPersonalizacion.setOnItemClickListener { _, _, position, _ ->
            val seleccion = listaHistorial[position]
            etNuevaPersonalizacion.setText(seleccion.descripcion)
            etCostoExtra.setText(seleccion.costoExtra.toString())
        }

        // Botón agregar personalización
        btnAgregarPersonalizacion.setOnClickListener {
            val descripcion = etNuevaPersonalizacion.text.toString().trim()
            val costoTexto = etCostoExtra.text.toString().trim()
            val costo = costoTexto.toDoubleOrNull() ?: 0.0

            if (descripcion.isNotEmpty()) {
                val textoMostrar = if (costo > 0.0)
                    "$descripcion (+$${String.format("%.2f", costo)})"
                else descripcion

                listaPersonalizaciones.add("$descripcion|$costo")
                adaptadorPersonalizaciones.add(textoMostrar)

                lifecycleScope.launch {
                    db.historialPersonalizacionDao().insertar(
                        HistorialPersonalizacionEntity(descripcion = descripcion, costoExtra = costo)
                    )
                }

                etNuevaPersonalizacion.text.clear()
                etCostoExtra.text.clear()
            } else {
                Toast.makeText(this, "Ingresa una descripción válida", Toast.LENGTH_SHORT).show()
            }
        }

        // Eliminar personalización con long press
        listViewPersonalizaciones.setOnItemLongClickListener { _, _, position, _ ->
            val item = adaptadorPersonalizaciones.getItem(position)
            AlertDialog.Builder(this)
                .setTitle("Eliminar personalización")
                .setMessage("¿Deseas eliminar '$item'?")
                .setPositiveButton("Sí") { _, _ ->
                    listaPersonalizaciones.removeAt(position)
                    adaptadorPersonalizaciones.remove(item)
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        btnGuardar.setOnClickListener { guardarProducto() }
    }

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

    private fun cargarHistorialPersonalizaciones() {
        lifecycleScope.launch {
            listaHistorial = db.historialPersonalizacionDao().obtenerTodos().toMutableList()
            val nombres = listaHistorial.map {
                if (it.costoExtra > 0.0)
                    "${it.descripcion} (+$${String.format("%.2f", it.costoExtra)})"
                else it.descripcion
            }
            adaptadorHistorial = ArrayAdapter(
                this@AgregarProductoActivity,
                android.R.layout.simple_dropdown_item_1line,
                nombres
            )
            runOnUiThread {
                etNuevaPersonalizacion.setAdapter(adaptadorHistorial)
            }
        }
    }

    private fun guardarProducto() {
        val nombre = etNombre.text.toString().trim()
        val precioPublico = etPrecioPublico.text.toString().toDoubleOrNull() ?: 0.0
        val costoUnitario = etCostoUnitario.text.toString().toDoubleOrNull() ?: 0.0
        val ocultarEnComandas = chkOcultarEnComandas.isChecked // 👈 valor del checkbox

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
                costoUnitario = costoUnitario,
                ocultarEnComandas = ocultarEnComandas // 👈 se guarda el valor
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
                cargarHistorialPersonalizaciones()
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
        chkOcultarEnComandas.isChecked = false // 👈 limpiar también el checkbox
    }
}
