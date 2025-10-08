package com.example.puntodeventagenerico.ui.venta

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.CarritoItem
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciar_venta)

        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria)
        listViewProductos = findViewById(R.id.listViewProductos)
        listViewCarrito = findViewById(R.id.listViewCarrito)
        btnEnviarComanda = findViewById(R.id.btnEnviarComanda)
        txtTotalCarrito = findViewById(R.id.txtTotalCarrito)

        // Inicializar base de datos
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
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

        // Enviar comanda al presionar botón
        btnEnviarComanda.setOnClickListener {
            enviarComanda()
        }
    }

    private fun actualizarTotalCarrito() {
        val total = carrito.sumOf { it.precioTotal() }
        txtTotalCarrito.text = "Total: $${"%.2f".format(total)}"
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

                val descripcionFinal = if (seleccionadas.isNotEmpty()) {
                    val extras = seleccionadas.joinToString(", ") { it.descripcion }
                    "${producto.nombre} ($extras)"
                } else {
                    producto.nombre
                }

                val productoPersonalizado = producto.copy(
                    nombre = descripcionFinal,
                    precioPublico = producto.precioPublico + seleccionadas.sumOf { it.costoExtra }
                )

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

    private fun agregarAlCarrito(producto: ProductoEntity, personalizaciones: List<PersonalizacionEntity> = emptyList()) {
        // Buscar si ya existe un producto con el mismo id y las mismas personalizaciones
        val itemExistente = carrito.find { item ->
            item.producto.id == producto.id &&
                    item.personalizaciones.map { it.descripcion }.sorted() ==
                    personalizaciones.map { it.descripcion }.sorted()
        }

        if (itemExistente != null) {
            // Si existe uno idéntico (mismo producto + mismas personalizaciones), solo aumenta cantidad
            itemExistente.cantidad++
        } else {
            // Si es distinto (personalizado o nuevo), se agrega como nuevo ítem
            carrito.add(CarritoItem(producto, 1, personalizaciones))
        }

        carritoAdapter.notifyDataSetChanged()
        actualizarTotalCarrito()
    }


    private fun enviarComanda() {
        lifecycleScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()

            // 1️⃣ Enviar comandas a cocina
            for (item in carrito) {
                val descripcion = item.descripcionCompleta()
                db.comandaDao().insertar(ComandaEntity(descripcion = descripcion))
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
                ganancia = ganancia
            )

            db.ventaDao().insertar(venta)

            // 3️⃣ Limpiar carrito
            carrito.clear()
            carritoAdapter.notifyDataSetChanged()

            runOnUiThread {
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
