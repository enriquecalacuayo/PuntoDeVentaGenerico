package com.example.puntodeventagenerico.ui.vereditarproductos

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.PersonalizacionEntity
import kotlinx.coroutines.launch

class VerEditarProductosActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var productoId: Int = 0

    private lateinit var etNombre: EditText
    private lateinit var etPrecio: EditText
    private lateinit var etCosto: EditText
    private lateinit var btnGuardar: Button

    private lateinit var listViewPersonalizaciones: ListView
    private lateinit var btnAgregarPersonalizacion: Button

    private lateinit var adaptador: ArrayAdapter<String>
    private val listaPersonalizaciones = mutableListOf<PersonalizacionEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_editar_productos)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

        productoId = intent.getIntExtra("productoId", 0)

        etNombre = findViewById(R.id.etNombre)
        etPrecio = findViewById(R.id.etPrecio)
        etCosto = findViewById(R.id.etCosto)
        btnGuardar = findViewById(R.id.btnGuardar)
        listViewPersonalizaciones = findViewById(R.id.listViewPersonalizaciones)
        btnAgregarPersonalizacion = findViewById(R.id.btnAgregarPersonalizacion)

        // Cargar datos del producto
        cargarProducto()
        cargarPersonalizaciones()

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        btnAgregarPersonalizacion.setOnClickListener {
            mostrarDialogAgregarPersonalizacion()
        }

        listViewPersonalizaciones.setOnItemClickListener { _, _, position, _ ->
            val personalizacion = listaPersonalizaciones[position]
            mostrarDialogEditarPersonalizacion(personalizacion)
        }

        listViewPersonalizaciones.setOnItemLongClickListener { _, _, position, _ ->
            val personalizacion = listaPersonalizaciones[position]
            eliminarPersonalizacion(personalizacion)
            true
        }
    }

    private fun cargarProducto() {
        lifecycleScope.launch {
            val producto = db.productoDao().obtenerPorId(productoId)
            runOnUiThread {
                if (producto != null) {
                    etNombre.setText(producto.nombre)
                    etPrecio.setText(producto.precioPublico.toString())
                    etCosto.setText(producto.costoUnitario.toString())
                } else {
                    Toast.makeText(
                        this@VerEditarProductosActivity,
                        "Error: no se encontró el producto con ID $productoId",
                        Toast.LENGTH_LONG
                    ).show()
                    finish() // Cerramos la pantalla para evitar seguir con datos vacíos
                }
            }
        }
    }

    private fun cargarPersonalizaciones() {
        lifecycleScope.launch {
            listaPersonalizaciones.clear()
            listaPersonalizaciones.addAll(db.personalizacionDao().obtenerPorProducto(productoId))
            val nombres = listaPersonalizaciones.map {
                "${it.descripcion} (+$${"%.2f".format(it.costoExtra)})"
            }
            runOnUiThread {
                adaptador = ArrayAdapter(this@VerEditarProductosActivity, android.R.layout.simple_list_item_1, nombres)
                listViewPersonalizaciones.adapter = adaptador
            }
        }
    }

    private fun mostrarDialogAgregarPersonalizacion() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_personalizacion, null)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val etCosto = dialogView.findViewById<EditText>(R.id.etCostoExtra)

        AlertDialog.Builder(this)
            .setTitle("Agregar personalización")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val descripcion = etDescripcion.text.toString().trim()
                val costo = etCosto.text.toString().toDoubleOrNull() ?: 0.0

                if (descripcion.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.personalizacionDao().insertar(
                            PersonalizacionEntity(
                                productoId = productoId,
                                descripcion = descripcion,
                                costoExtra = costo
                            )
                        )
                        cargarPersonalizaciones()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEditarPersonalizacion(personalizacion: PersonalizacionEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_personalizacion, null)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val etCosto = dialogView.findViewById<EditText>(R.id.etCostoExtra)

        etDescripcion.setText(personalizacion.descripcion)
        etCosto.setText(personalizacion.costoExtra.toString())

        AlertDialog.Builder(this)
            .setTitle("Editar personalización")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevaDescripcion = etDescripcion.text.toString().trim()
                val nuevoCosto = etCosto.text.toString().toDoubleOrNull() ?: 0.0

                lifecycleScope.launch {
                    db.personalizacionDao().actualizar(
                        personalizacion.copy(
                            descripcion = nuevaDescripcion,
                            costoExtra = nuevoCosto
                        )
                    )
                    cargarPersonalizaciones()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPersonalizacion(personalizacion: PersonalizacionEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar personalización")
            .setMessage("¿Deseas eliminar '${personalizacion.descripcion}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    db.personalizacionDao().eliminar(personalizacion)
                    cargarPersonalizaciones()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarCambios() {
        lifecycleScope.launch {
            val nombre = etNombre.text.toString()
            val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val costo = etCosto.text.toString().toDoubleOrNull() ?: 0.0

            val producto = db.productoDao().obtenerPorId(productoId)
            db.productoDao().actualizar(producto.copy(nombre = nombre, precioPublico = precio, costoUnitario = costo))
            runOnUiThread {
                Toast.makeText(this@VerEditarProductosActivity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
