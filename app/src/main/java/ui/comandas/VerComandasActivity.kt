package com.example.puntodeventagenerico.ui.comandas

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.ComandaEntity
import kotlinx.coroutines.launch

class VerComandasActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComandaAdapter
    private val listaComandas = mutableListOf<ComandaEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_comandas)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "punto_venta_db"
        ).build()

        recyclerView = findViewById(R.id.recyclerComandas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComandaAdapter(listaComandas)
        recyclerView.adapter = adapter

        cargarComandas()

        // 👇 Al mantener presionado se elimina directamente
        adapter.setOnItemLongClickListener { comanda ->
            eliminarComanda(comanda)
        }
    }

    private fun cargarComandas() {
        lifecycleScope.launch {
            val comandas = db.comandaDao().obtenerTodas()
            listaComandas.clear()
            listaComandas.addAll(comandas)
            runOnUiThread { adapter.notifyDataSetChanged() }
        }
    }

    private fun eliminarComanda(comanda: ComandaEntity) {
        lifecycleScope.launch {
            db.comandaDao().eliminar(comanda)
            listaComandas.remove(comanda)
            runOnUiThread {
                adapter.notifyDataSetChanged()
                Toast.makeText(this@VerComandasActivity, "✅ Comanda eliminada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
