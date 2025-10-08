package com.example.puntodeventagenerico

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.puntodeventagenerico.ui.agregarproducto.AgregarProductoActivity
import com.example.puntodeventagenerico.ui.venta.IniciarVentaActivity
import com.example.puntodeventagenerico.ui.vereditarproductos.VerEditarProductosActivity
import com.example.puntodeventagenerico.ui.comandas.VerComandasActivity
import com.example.puntodeventagenerico.ui.historial.HistorialVentasActivity

class MenuPrincipalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        val btnIniciarVenta = findViewById<Button>(R.id.btnIniciarVenta)
        val btnAgregarProducto = findViewById<Button>(R.id.btnAgregarProducto)
        val btnVerProductos = findViewById<Button>(R.id.btnVerProductos)
        val btnHistorialVentas = findViewById<Button>(R.id.btnHistorialVentas)
        val btnVerComandas = findViewById<Button>(R.id.btnVerComandas)

        // 🛒 Iniciar venta
        btnIniciarVenta.setOnClickListener {
            startActivity(Intent(this, IniciarVentaActivity::class.java))
        }

        // ➕ Agregar producto
        btnAgregarProducto.setOnClickListener {
            startActivity(Intent(this, AgregarProductoActivity::class.java))
        }

        // ✏️ Ver o editar productos
        btnVerProductos.setOnClickListener {
            startActivity(Intent(this, VerEditarProductosActivity::class.java))
        }

        // 📊 Historial de ventas (por ahora sin funcionalidad)
        btnHistorialVentas.setOnClickListener {
            startActivity(Intent(this, HistorialVentasActivity::class.java))
        }

        // ☕ Ver comandas
        btnVerComandas.setOnClickListener {
            startActivity(Intent(this, VerComandasActivity::class.java))
        }
    }
}
