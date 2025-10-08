package com.example.puntodeventagenerico.ui.comandas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
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

        // 🔄 Swipe para eliminar comandas atendidas
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val comanda = listaComandas[position]
                eliminarComanda(comanda)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
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
            runOnUiThread { adapter.notifyDataSetChanged() }
        }
    }
}
