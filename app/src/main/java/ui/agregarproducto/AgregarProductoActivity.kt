package com.example.puntodeventagenerico.ui.agregarproducto

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.ProductoEntity
import com.example.puntodeventagenerico.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AgregarProductoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        val etNombre = findViewById<EditText>(R.id.etNombreProducto)
        val etPrecioPublico = findViewById<EditText>(R.id.etPrecioPublico)
        val etCostoUnitario = findViewById<EditText>(R.id.etCostoUnitario)
        val etPersonalizaciones = findViewById<EditText>(R.id.etPersonalizaciones)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarProducto)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "punto_de_venta_db"
        ).build()

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val precioPublico = etPrecioPublico.text.toString().toDoubleOrNull()
            val costoUnitario = etCostoUnitario.text.toString().toDoubleOrNull()
            val personalizaciones = etPersonalizaciones.text.toString().trim()

            if (nombre.isEmpty() || precioPublico == null || costoUnitario == null) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoProducto = ProductoEntity(
                nombre = nombre,
                precioPublico = precioPublico,
                costoUnitario = costoUnitario,
                personalizaciones = if (personalizaciones.isEmpty()) null else personalizaciones
            )

            CoroutineScope(Dispatchers.IO).launch {
                db.productoDao().insertarProducto(nuevoProducto)
                runOnUiThread {
                    Toast.makeText(this@AgregarProductoActivity, "Producto guardado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}

