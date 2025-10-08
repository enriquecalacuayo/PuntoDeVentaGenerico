package com.example.puntodeventagenerico.ui.historial

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.VentaEntity
import kotlinx.coroutines.launch

class VentasDelDiaActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var listViewVentas: ListView
    private lateinit var btnEstadisticas: Button
    private val listaVentas = mutableListOf<VentaEntity>()
    private lateinit var adapter: ArrayAdapter<String>
    private var fechaSeleccionada: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ventas_dia)

        listViewVentas = findViewById(R.id.listViewVentasDia)
        btnEstadisticas = findViewById(R.id.btnVerEstadisticas)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

        fechaSeleccionada = intent.getLongExtra("fecha", 0L)

        cargarVentasDelDia(fechaSeleccionada)

        // ✅ Eliminar venta al mantener presionado
        listViewVentas.setOnItemLongClickListener { _, _, position, _ ->
            val venta = listaVentas[position]
            eliminarVenta(venta)
            true
        }

        btnEstadisticas.setOnClickListener {
            val intent = Intent(this, EstadisticasDiaActivity::class.java)
            intent.putExtra("fecha", fechaSeleccionada)
            startActivity(intent)
        }
    }

    private fun cargarVentasDelDia(fecha: Long) {
        lifecycleScope.launch {
            val inicioDia = fecha
            val finDia = inicioDia + 86_400_000L
            val ventas = db.ventaDao().obtenerPorDia(inicioDia, finDia)

            listaVentas.clear()
            listaVentas.addAll(ventas)

            val ventasTexto = ventas.map {
                "🧾 ${it.productosVendidos}\n💵 Total: $${String.format("%.2f", it.totalVenta)}"
            }

            runOnUiThread {
                adapter = ArrayAdapter(
                    this@VentasDelDiaActivity,
                    android.R.layout.simple_list_item_1,
                    ventasTexto.toMutableList()
                )
                listViewVentas.adapter = adapter
            }
        }
    }

    private fun eliminarVenta(venta: VentaEntity) {
        lifecycleScope.launch {
            db.ventaDao().eliminarPorId(venta.id)

            // ✅ Actualizar lista local y el adaptador
            runOnUiThread {
                val index = listaVentas.indexOf(venta)
                if (index != -1) {
                    listaVentas.removeAt(index)
                    adapter.remove(adapter.getItem(index))
                    adapter.notifyDataSetChanged()
                }

                Toast.makeText(
                    this@VentasDelDiaActivity,
                    "Venta eliminada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

