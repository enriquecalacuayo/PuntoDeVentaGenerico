package com.example.puntodeventagenerico.ui.historial

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.*
import kotlinx.coroutines.launch

class EstadisticasDiaActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var txtTotalVentas: TextView
    private lateinit var txtTotalEfectivo: TextView
    private lateinit var txtTotalTarjeta: TextView
    private lateinit var txtGanancia: TextView
    private lateinit var layoutProductos: LinearLayout
    private lateinit var etCajaInicio: EditText
    private lateinit var etCajaFinal: EditText
    private lateinit var txtResultadoCaja: TextView
    private lateinit var btnGuardarCaja: Button

    private lateinit var etNombreGasto: EditText
    private lateinit var etMontoGasto: EditText
    private lateinit var btnAgregarGasto: Button
    private lateinit var listViewGastos: ListView
    private lateinit var gastosAdapter: ArrayAdapter<String>

    private var fechaSeleccionada: Long = 0L
    private var totalVentas = 0.0
    private var totalEfectivo = 0.0
    private var totalTarjeta = 0.0
    private var totalGanancia = 0.0
    private val listaGastos = mutableListOf<GastoEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas_dia)

        txtTotalVentas = findViewById(R.id.txtTotalVentas)
        txtTotalEfectivo = findViewById(R.id.txtTotalEfectivo) // 👈 agrega en tu XML
        txtTotalTarjeta = findViewById(R.id.txtTotalTarjeta)   // 👈 agrega en tu XML
        txtGanancia = findViewById(R.id.txtGanancia)
        layoutProductos = findViewById(R.id.layoutProductosVendidos)
        etCajaInicio = findViewById(R.id.etCajaInicio)
        etCajaFinal = findViewById(R.id.etCajaFinal)
        txtResultadoCaja = findViewById(R.id.txtResultadoCaja)
        btnGuardarCaja = findViewById(R.id.btnGuardarCaja)

        etNombreGasto = findViewById(R.id.etNombreGasto)
        etMontoGasto = findViewById(R.id.etMontoGasto)
        btnAgregarGasto = findViewById(R.id.btnAgregarGasto)
        listViewGastos = findViewById(R.id.listViewGastos)

        // Detectar cambios en los campos de caja
        etCajaInicio.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                calcularResultado(listaGastos.sumOf { it.monto })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etCajaFinal.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                calcularResultado(listaGastos.sumOf { it.monto })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

        fechaSeleccionada = intent.getLongExtra("fecha", 0L)

        gastosAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listViewGastos.adapter = gastosAdapter

        cargarEstadisticas(fechaSeleccionada)
        btnGuardarCaja.setOnClickListener { guardarCajaDelDia() }
        btnAgregarGasto.setOnClickListener { agregarGasto() }

        // Permitir eliminar un gasto con long click
        listViewGastos.setOnItemLongClickListener { _, _, position, _ ->
            val gasto = listaGastos[position]
            eliminarGasto(gasto)
            true
        }
    }

    private fun cargarEstadisticas(fecha: Long) {
        lifecycleScope.launch {
            val inicioDia = fecha
            val finDia = inicioDia + 86_400_000L

            val ventas = db.ventaDao().obtenerPorDia(inicioDia, finDia)
            val gastos = db.gastoDao().obtenerPorFecha(fecha)
            listaGastos.clear()
            listaGastos.addAll(gastos)

            // 🧮 Cálculo general
            totalVentas = ventas.sumOf { it.totalVenta }
            totalGanancia = ventas.sumOf { it.ganancia }

            // 💳 Separar ventas según tipo de pago
            totalTarjeta = ventas.filter { it.pagoConTarjeta }.sumOf { it.totalVenta }
            totalEfectivo = ventas.filter { !it.pagoConTarjeta }.sumOf { it.totalVenta }

            val totalGastos = listaGastos.sumOf { it.monto }

            // Contador de productos vendidos
            val contador = mutableMapOf<String, Int>()
            ventas.forEach { venta ->
                venta.productosVendidos.split("\n").forEach { linea ->
                    val partes = linea.split(" x")
                    if (partes.size == 2) {
                        val nombre = partes[0].trim()
                        val cantidad = partes[1].toIntOrNull() ?: 1
                        contador[nombre] = contador.getOrDefault(nombre, 0) + cantidad
                    }
                }
            }

            val productosOrdenados = contador.toList().sortedByDescending { it.second }

            runOnUiThread {
                txtTotalVentas.text = "💵 Total vendido: $${"%.2f".format(totalVentas)}"
                txtTotalEfectivo.text = "🪙 En caja (efectivo): $${"%.2f".format(totalEfectivo)}"
                txtTotalTarjeta.text = "💳 Pagos con tarjeta: $${"%.2f".format(totalTarjeta)}"
                txtGanancia.text = "📈 Ganancia neta: $${"%.2f".format(totalGanancia)}"

                layoutProductos.removeAllViews()
                productosOrdenados.forEach { (nombre, cantidad) ->
                    val txt = TextView(this@EstadisticasDiaActivity)
                    txt.text = "• $nombre x$cantidad"
                    txt.textSize = 16f
                    layoutProductos.addView(txt)
                }

                actualizarListaGastos()
                calcularResultado(totalGastos)
            }
        }
    }

    private fun agregarGasto() {
        val nombre = etNombreGasto.text.toString().trim()
        val monto = etMontoGasto.text.toString().toDoubleOrNull() ?: 0.0

        if (nombre.isEmpty() || monto <= 0) {
            Toast.makeText(this, "Ingresa un nombre y monto válidos", Toast.LENGTH_SHORT).show()
            return
        }

        val gasto = GastoEntity(fecha = fechaSeleccionada, nombre = nombre, monto = monto)

        lifecycleScope.launch {
            db.gastoDao().insertar(gasto)
            listaGastos.add(gasto)

            runOnUiThread {
                etNombreGasto.text.clear()
                etMontoGasto.text.clear()
                actualizarListaGastos()
                calcularResultado(listaGastos.sumOf { it.monto })
            }
        }
    }

    private fun eliminarGasto(gasto: GastoEntity) {
        lifecycleScope.launch {
            db.gastoDao().eliminarPorId(gasto.id)
            runOnUiThread {
                listaGastos.remove(gasto)
                actualizarListaGastos()
                calcularResultado(listaGastos.sumOf { it.monto })
                Toast.makeText(this@EstadisticasDiaActivity, "Gasto eliminado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarListaGastos() {
        val lista = listaGastos.map { "${it.nombre} - $${"%.2f".format(it.monto)}" }
        gastosAdapter.clear()
        gastosAdapter.addAll(lista)
        gastosAdapter.notifyDataSetChanged()
    }

    private fun calcularResultado(totalGastos: Double) {
        val inicio = etCajaInicio.text.toString().toDoubleOrNull() ?: 0.0
        val final = etCajaFinal.text.toString().toDoubleOrNull() ?: 0.0

        if (inicio == 0.0 && final == 0.0 && totalVentas == 0.0) {
            txtResultadoCaja.text = ""
            return
        }

        // ✅ Ahora solo cuenta efectivo, no tarjeta
        val esperado = inicio + totalEfectivo - totalGastos
        val diferencia = final - esperado

        val textoResultado = if (diferencia >= 0)
            "✅ SOBRANTE: $${"%.2f".format(diferencia)}"
        else
            "⚠️ FALTANTE: $${"%.2f".format(-diferencia)}"

        txtResultadoCaja.text = textoResultado
    }

    private fun guardarCajaDelDia() {
        lifecycleScope.launch {
            val inicio = etCajaInicio.text.toString().toDoubleOrNull() ?: 0.0
            val final = etCajaFinal.text.toString().toDoubleOrNull() ?: 0.0
            val caja = CajaDiaEntity(
                fecha = fechaSeleccionada,
                cajaInicio = inicio,
                gastos = listaGastos.sumOf { it.monto },
                cajaFinal = final
            )
            db.cajaDiaDao().insertar(caja)

            runOnUiThread {
                calcularResultado(listaGastos.sumOf { it.monto })
                Toast.makeText(this@EstadisticasDiaActivity, "Registro de caja guardado ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
