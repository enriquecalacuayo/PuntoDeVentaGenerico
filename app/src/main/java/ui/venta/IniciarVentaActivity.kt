package com.example.puntodeventagenerico.ui.venta

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.CarritoItem
import com.example.puntodeventagenerico.data.local.ProfileManager
import com.example.puntodeventagenerico.data.local.ComandaEntity
import com.example.puntodeventagenerico.data.local.ProductoEntity
import com.example.puntodeventagenerico.data.local.PersonalizacionEntity
import com.example.puntodeventagenerico.data.local.VentaEntity
import com.example.puntodeventagenerico.ui.comandas.VerComandasActivity

import kotlinx.coroutines.launch

class IniciarVentaActivity : AppCompatActivity() {

    private lateinit var spinnerSubcategoria: Spinner
    private lateinit var listViewProductos: ListView
    private lateinit var listViewCarrito: ListView
    private lateinit var btnEnviarComanda: Button

    private lateinit var carritoAdapter: CarritoAdapter
    private lateinit var productoAdapter: ArrayAdapter<String>

    private val carrito = mutableListOf<CarritoItem>()
    private val listaProductos = mutableListOf<ProductoEntity>()
    private lateinit var db: AppDatabase
    private lateinit var txtTotalCarrito: TextView

    private lateinit var chkPagoConTarjeta: CheckBox
    private lateinit var layoutCobro: LinearLayout
    private lateinit var etPagoCliente: EditText
    private lateinit var txtCambio: TextView
    private lateinit var txtSugerenciaCambio: TextView

    // Denominaciones válidas en México (monedas y billetes)
    private val denominaciones = listOf(5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciar_venta)

        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria)
        listViewProductos = findViewById(R.id.listViewProductos)
        listViewCarrito = findViewById(R.id.listViewCarrito)
        btnEnviarComanda = findViewById(R.id.btnEnviarComanda)
        txtTotalCarrito = findViewById(R.id.txtTotalCarrito)
        chkPagoConTarjeta = findViewById(R.id.chkPagoConTarjeta)
        layoutCobro = findViewById(R.id.layoutCobro)
        etPagoCliente = findViewById(R.id.etPagoCliente)
        txtCambio = findViewById(R.id.txtCambio)
        txtSugerenciaCambio = findViewById(R.id.txtSugerenciaCambio)


        // Inicializar base de datos
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            ProfileManager.getDatabaseName(applicationContext)
        )
            .fallbackToDestructiveMigration()
            .build()

        // Adaptador para carrito
        carritoAdapter = CarritoAdapter(this, carrito) {
            actualizarTotalCarrito()
        }
        listViewCarrito.adapter = carritoAdapter

        // Cargar subcategorías y productos
        cargarSubcategorias()

        spinnerSubcategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long
            ) {
                val subcategoriaSeleccionada = spinnerSubcategoria.selectedItem.toString()
                cargarProductos(subcategoriaSeleccionada)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Al hacer clic en un producto se agrega al carrito
        listViewProductos.setOnItemClickListener { _, _, position, _ ->
            val productoSeleccionado = listaProductos[position]

            lifecycleScope.launch {
                val personalizaciones = db.personalizacionDao().obtenerPorProducto(productoSeleccionado.id)

                if (personalizaciones.isNotEmpty()) {
                    mostrarDialogPersonalizacion(productoSeleccionado, personalizaciones)
                } else {
                    agregarAlCarrito(productoSeleccionado)
                }
            }
        }

        // Mostrar/ocultar sección de cobro según tipo de pago
        chkPagoConTarjeta.setOnCheckedChangeListener { _, isCard ->
            layoutCobro.visibility = if (isCard) View.GONE else View.VISIBLE
            if (isCard) {
                etPagoCliente.text?.clear()
                txtCambio.text = "$0.00"
                txtSugerenciaCambio.visibility = View.GONE
            }
        }

        // Calcular cambio y sugerencia mientras el cajero escribe
        etPagoCliente.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                actualizarCobro()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Cerrar teclado al presionar "Listo" en el teclado numérico
        etPagoCliente.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etPagoCliente.windowToken, 0)
                true
            } else false
        }

        // Enviar comanda al presionar botón
        btnEnviarComanda.setOnClickListener {
            enviarComanda()
        }
    }

    private fun actualizarTotalCarrito() {
        val total = carrito.sumOf { it.precioTotal() }
        txtTotalCarrito.text = "Total: $${"%.2f".format(total)}"
        // Recalcular cobro si ya hay algo escrito
        if (etPagoCliente.text.isNotEmpty()) actualizarCobro()
    }

    private fun actualizarCobro() {
        val total = carrito.sumOf { it.precioTotal() }
        val pagado = etPagoCliente.text.toString().toDoubleOrNull() ?: 0.0
        val cambio = pagado - total

        if (pagado <= 0.0) {
            txtCambio.text = "$0.00"
            txtCambio.setTextColor(getColor(android.R.color.holo_green_dark))
            txtSugerenciaCambio.visibility = View.GONE
            return
        }

        if (cambio < 0.0) {
            txtCambio.text = "Faltan $${"%.2f".format(-cambio)}"
            txtCambio.setTextColor(getColor(android.R.color.holo_red_dark))
            txtSugerenciaCambio.visibility = View.GONE
            return
        }

        txtCambio.text = "$${"%.2f".format(cambio)}"
        txtCambio.setTextColor(getColor(android.R.color.holo_green_dark))

        val sugerencia = calcularSugerencia(cambio)
        if (sugerencia != null) {
            txtSugerenciaCambio.text = "💡 $sugerencia"
            txtSugerenciaCambio.visibility = View.VISIBLE
        } else {
            txtSugerenciaCambio.visibility = View.GONE
        }
    }

    /**
     * Sugiere pedirle al cliente un monto adicional pequeño para devolver
     * un billete cerrado de mayor denominación en lugar de monedas sueltas.
     *
     * Ejemplo: cambio = $25 → pedir $25 más → devolver billete de $50.
     */
    private fun calcularSugerencia(cambio: Double): String? {
        // Si el cambio ya es una denominación limpia, no hay nada que sugerir
        if (denominaciones.any { Math.abs(it - cambio) < 0.01 }) return null

        for (denom in denominaciones) {
            if (denom > cambio) {
                val extra = denom - cambio
                // Solo sugerir si el extra que se pide es menor o igual al cambio
                // y no supera los $50 (no incomodar al cliente)
                if (extra <= cambio && extra <= 50.0) {
                    return "Pide al cliente $${"%.2f".format(extra)} más\n" +
                           "para devolverle un billete de $${"%.0f".format(denom)}"
                }
                break
            }
        }
        return null
    }


    private fun mostrarDialogPersonalizacion(
        producto: ProductoEntity,
        personalizaciones: List<PersonalizacionEntity>
    ) {
        val opciones = personalizaciones.map { "${it.descripcion} (+$${it.costoExtra})" }.toTypedArray()
        val seleccionados = mutableListOf<Int>()

        AlertDialog.Builder(this)
            .setTitle("Personaliza ${producto.nombre}")
            .setMultiChoiceItems(opciones, null) { _, which, isChecked ->
                if (isChecked) seleccionados.add(which)
                else seleccionados.remove(which)
            }
            .setPositiveButton("Agregar al carrito") { _, _ ->
                val seleccionadas = seleccionados.map { personalizaciones[it] }

                // Calcular precio final con extras
                val precioFinal = producto.precioPublico + seleccionadas.sumOf { it.costoExtra }

                val productoPersonalizado = producto.copy(
                    nombre = buildString {
                        append(producto.nombre)
                        if (seleccionadas.isNotEmpty()) {
                            append(" (")
                            append(seleccionadas.joinToString(", ") { it.descripcion })
                            append(")")
                        }
                    },
                    precioPublico = precioFinal // ✅ ya incluye los extras
                )

                // Agregar al carrito
                agregarAlCarrito(productoPersonalizado, seleccionadas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun cargarSubcategorias() {
        lifecycleScope.launch {
            val subcategorias = db.subcategoriaDao().obtenerTodas()
            val nombres = subcategorias.map { it.nombre }
            val adapter = ArrayAdapter(
                this@IniciarVentaActivity,
                android.R.layout.simple_spinner_item,
                nombres
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSubcategoria.adapter = adapter
        }
    }

    private fun cargarProductos(subcategoria: String) {
        lifecycleScope.launch {
            listaProductos.clear()
            listaProductos.addAll(db.productoDao().obtenerPorCategoria(subcategoria))

            val nombres = listaProductos.map { "${it.nombre} - $${it.precioPublico}" }
            productoAdapter = ArrayAdapter(
                this@IniciarVentaActivity,
                android.R.layout.simple_list_item_1,
                nombres
            )
            listViewProductos.adapter = productoAdapter
        }
    }

    private fun agregarAlCarrito(
        producto: ProductoEntity,
        personalizaciones: List<PersonalizacionEntity> = emptyList()
    ) {
        val itemExistente = carrito.find { item ->
            item.producto.nombre == producto.nombre &&
                    item.personalizaciones.map { it.descripcion }.sorted() ==
                    personalizaciones.map { it.descripcion }.sorted()
        }

        if (itemExistente != null) {
            itemExistente.cantidad++
        } else {
            carrito.add(CarritoItem(producto, 1, personalizaciones))
        }

        carritoAdapter.notifyDataSetChanged()
        actualizarTotalCarrito() // ✅ solo se llama una vez
    }


    private fun enviarComanda() {
        lifecycleScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                ProfileManager.getDatabaseName(applicationContext)
            )
                .fallbackToDestructiveMigration()
                .build()

            // 1️⃣ Enviar comandas a cocina
            for (item in carrito) {
                // Evitar enviar productos ocultos a comandas
                if (!item.producto.ocultarEnComandas) {
                    val descripcion = item.descripcionCompleta()
                    db.comandaDao().insertar(ComandaEntity(descripcion = descripcion))
                }
            }

            // 2️⃣ Guardar la venta en la tabla de ventas
            val totalVenta = carrito.sumOf { it.precioTotal() }
            val ganancia = carrito.sumOf {
                (it.producto.precioPublico - it.producto.costoUnitario) * it.cantidad
            }

            val productosResumen = carrito.joinToString("\n") {
                "${it.producto.nombre} x${it.cantidad}"
            }

            val venta = VentaEntity(
                productosVendidos = productosResumen,
                totalVenta = totalVenta,
                ganancia = ganancia,
                pagoConTarjeta = chkPagoConTarjeta.isChecked // 💳 se guarda el tipo de pago
            )

            db.ventaDao().insertar(venta)

            // 3️⃣ Limpiar carrito y campos de cobro
            carrito.clear()
            carritoAdapter.notifyDataSetChanged()

            runOnUiThread {
                etPagoCliente.text?.clear()
                txtCambio.text = "$0.00"
                txtSugerenciaCambio.visibility = View.GONE
                actualizarTotalCarrito()

                Toast.makeText(
                    this@IniciarVentaActivity,
                    "Comanda enviada y venta registrada",
                    Toast.LENGTH_SHORT
                ).show()

                // 4️⃣ Ir directamente a la vista de comandas
                val intent = Intent(this@IniciarVentaActivity, VerComandasActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }


    private fun guardarVenta() {
        lifecycleScope.launch {
            val totalVenta = carrito.sumOf { it.precioTotal() }
            val ganancia = carrito.sumOf { it.producto.precioPublico - it.producto.costoUnitario }

            val productosResumen = carrito.joinToString("\n") {
                "${it.producto.nombre} x${it.cantidad}"
            }

            val venta = VentaEntity(
                productosVendidos = productosResumen,
                totalVenta = totalVenta,
                ganancia = ganancia
            )

            db.ventaDao().insertar(venta)
            carrito.clear()
            carritoAdapter.notifyDataSetChanged()
            Toast.makeText(this@IniciarVentaActivity, "Venta guardada", Toast.LENGTH_SHORT).show()
        }
    }



}
