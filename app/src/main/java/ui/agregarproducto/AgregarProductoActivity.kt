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
    private lateinit var spinnerSubcategoria: Spinner
    private lateinit var adaptadorSubcategorias: ArrayAdapter<String>
    private var listaSubcategorias = mutableListOf<SubcategoriaEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "punto_venta_db"
        ).build()

        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria)
        val btnNueva = findViewById<Button>(R.id.btnNuevaSubcategoria)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarSubcategoria)

        cargarSubcategorias()

        btnNueva.setOnClickListener { mostrarDialogNuevaSubcategoria() }
        btnEliminar.setOnClickListener { eliminarSubcategoriaSeleccionada() }
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
}


