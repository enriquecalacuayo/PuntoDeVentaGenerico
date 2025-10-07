package com.example.puntodeventagenerico

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.example.puntodeventagenerico.ui.agregarproducto.AgregarProductoActivity
import com.example.puntodeventagenerico.ui.venta.IniciarVentaActivity

class MenuPrincipalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        val btnIniciarVenta = findViewById<Button>(R.id.btnIniciarVenta)
        val btnAgregarProducto = findViewById<Button>(R.id.btnAgregarProducto)
        val btnVerProductos = findViewById<Button>(R.id.btnVerProductos)
        val btnHistorialVentas = findViewById<Button>(R.id.btnHistorialVentas)
        val btnVerComandas = findViewById<Button>(R.id.btnVerComandas)

        // Navegación (por ahora solo placeholders)
        btnIniciarVenta.setOnClickListener {
            val intent = Intent(this, IniciarVentaActivity::class.java)
            startActivity(intent)
        }

        btnAgregarProducto.setOnClickListener {
            val intent = Intent(this, AgregarProductoActivity::class.java)
            startActivity(intent)
        }

        btnVerProductos.setOnClickListener {
            //startActivity(Intent(this, VerProductosActivity::class.java))
        }

        btnHistorialVentas.setOnClickListener {
            //startActivity(Intent(this, HistorialVentasActivity::class.java))
        }

        btnVerComandas.setOnClickListener {
            //startActivity(Intent(this, VerComandasActivity::class.java))
        }
    }
}
