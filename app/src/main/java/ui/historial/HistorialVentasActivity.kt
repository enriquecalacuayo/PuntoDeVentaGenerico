package com.example.puntodeventagenerico.ui.historial

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistorialVentasActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var listViewDias: ListView
    private lateinit var adapter: ArrayAdapter<String>

    // Guardamos las fechas Long para poder enviarlas al seleccionar
    private val fechasLong = mutableListOf<Long>()
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_ventas)

        listViewDias = findViewById(R.id.listViewDias)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

        cargarDiasConVentas()

        // ✅ Cuando el usuario selecciona un día, abrimos VentasDelDiaActivity
        listViewDias.setOnItemClickListener { _, _, position, _ ->
            val fechaSeleccionada = fechasLong[position]

            val intent = Intent(this, VentasDelDiaActivity::class.java)
            intent.putExtra("fecha", fechaSeleccionada)
            startActivity(intent)
        }
    }

    private fun cargarDiasConVentas() {
        lifecycleScope.launch {
            val ventas = db.ventaDao().obtenerTodas()

            // Agrupamos las ventas por día (basado en la fecha Long)
            val diasUnicos = ventas.map {
                val fecha = Date(it.fecha)
                formatoFecha.format(fecha)
            }.distinct()

            val diasOrdenados = diasUnicos.sortedByDescending {
                formatoFecha.parse(it)?.time ?: 0L
            }

            fechasLong.clear()
            fechasLong.addAll(diasOrdenados.mapNotNull { formatoFecha.parse(it)?.time })

            runOnUiThread {
                adapter = ArrayAdapter(
                    this@HistorialVentasActivity,
                    android.R.layout.simple_list_item_1,
                    diasOrdenados
                )
                listViewDias.adapter = adapter
            }
        }
    }
}

