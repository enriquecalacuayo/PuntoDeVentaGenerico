package com.example.puntodeventagenerico.ui.vereditarproductos

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.ProductoEntity
import kotlinx.coroutines.launch

class ListaProductosActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var listViewProductos: ListView
    private lateinit var adaptador: ArrayAdapter<String>
    private val listaProductos = mutableListOf<ProductoEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_productos)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

        listViewProductos = findViewById(R.id.listViewProductos)

        cargarProductos()

        // 👉 Cuando toques un producto, abrirá VerEditarProductosActivity
        listViewProductos.setOnItemClickListener { _, _, position, _ ->
            val producto = listaProductos[position]
            val intent = Intent(this, VerEditarProductosActivity::class.java)
            intent.putExtra("productoId", producto.id)
            startActivity(intent)
        }

        // 👉 Si lo mantienes presionado, se eliminará
        listViewProductos.setOnItemLongClickListener { _, _, position, _ ->
            val producto = listaProductos[position]
            eliminarProducto(producto)
            true
        }
    }

    private fun cargarProductos() {
        lifecycleScope.launch {
            listaProductos.clear()
            listaProductos.addAll(db.productoDao().obtenerTodos())

            val nombres = listaProductos.map {
                "${it.nombre}  -  $${"%.2f".format(it.precioPublico)}"
            }

            runOnUiThread {
                adaptador = ArrayAdapter(
                    this@ListaProductosActivity,
                    android.R.layout.simple_list_item_1,
                    nombres
                )
                listViewProductos.adapter = adaptador
            }
        }
    }

    private fun eliminarProducto(producto: ProductoEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Deseas eliminar '${producto.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    db.productoDao().eliminar(producto)
                    cargarProductos()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
