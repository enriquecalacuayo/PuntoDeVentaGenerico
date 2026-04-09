package com.example.puntodeventagenerico

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.puntodeventagenerico.data.local.BackupManager
import com.example.puntodeventagenerico.data.local.ProfileManager
import com.example.puntodeventagenerico.ui.agregarproducto.AgregarProductoActivity
import com.example.puntodeventagenerico.ui.comandas.VerComandasActivity
import com.example.puntodeventagenerico.ui.historial.HistorialVentasActivity
import com.example.puntodeventagenerico.ui.perfiles.SeleccionPerfilActivity
import com.example.puntodeventagenerico.ui.venta.IniciarVentaActivity
import com.example.puntodeventagenerico.ui.vereditarproductos.ListaProductosActivity
import kotlinx.coroutines.launch

class MenuPrincipalActivity : AppCompatActivity() {

    // SAF: exportar → el usuario elige dónde guardar el archivo
    private val exportarLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val ok = BackupManager.exportar(this@MenuPrincipalActivity, uri)
            runOnUiThread {
                if (ok) Toast.makeText(this@MenuPrincipalActivity, "Backup exportado correctamente", Toast.LENGTH_LONG).show()
                else    Toast.makeText(this@MenuPrincipalActivity, "Error al exportar backup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SAF: importar → el usuario elige el archivo de backup
    private val importarLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        confirmarImportacion {
            lifecycleScope.launch {
                val perfilBackup = BackupManager.importar(this@MenuPrincipalActivity, uri)
                runOnUiThread {
                    if (perfilBackup != null)
                        Toast.makeText(this@MenuPrincipalActivity, "Backup de \"$perfilBackup\" importado correctamente", Toast.LENGTH_LONG).show()
                    else
                        Toast.makeText(this@MenuPrincipalActivity, "Error al importar backup. Verifica que el archivo sea valido.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        val btnIniciarVenta    = findViewById<Button>(R.id.btnIniciarVenta)
        val btnAgregarProducto = findViewById<Button>(R.id.btnAgregarProducto)
        val btnVerProductos    = findViewById<Button>(R.id.btnVerProductos)
        val btnHistorialVentas = findViewById<Button>(R.id.btnHistorialVentas)
        val btnVerComandas     = findViewById<Button>(R.id.btnVerComandas)
        val btnCambiarPerfil   = findViewById<Button>(R.id.btnCambiarPerfil)
        val btnExportar        = findViewById<Button>(R.id.btnExportarBackup)
        val btnImportar        = findViewById<Button>(R.id.btnImportarBackup)

        btnCambiarPerfil.setOnClickListener {
            startActivity(Intent(this, SeleccionPerfilActivity::class.java))
        }

        btnIniciarVenta.setOnClickListener {
            startActivity(Intent(this, IniciarVentaActivity::class.java))
        }

        btnAgregarProducto.setOnClickListener {
            startActivity(Intent(this, AgregarProductoActivity::class.java))
        }

        btnVerProductos.setOnClickListener {
            startActivity(Intent(this, ListaProductosActivity::class.java))
        }

        btnHistorialVentas.setOnClickListener {
            startActivity(Intent(this, HistorialVentasActivity::class.java))
        }

        btnVerComandas.setOnClickListener {
            startActivity(Intent(this, VerComandasActivity::class.java))
        }

        btnExportar.setOnClickListener {
            val nombre = BackupManager.getNombreArchivoExport(this)
            exportarLauncher.launch(nombre)
        }

        btnImportar.setOnClickListener {
            importarLauncher.launch(arrayOf("application/json", "*/*"))
        }
    }

    override fun onResume() {
        super.onResume()

        // Si no hay perfil activo, ir a selección de perfiles
        val perfilActivo = ProfileManager.getPerfilActivo(this)
        if (perfilActivo == null) {
            startActivity(Intent(this, SeleccionPerfilActivity::class.java))
            finish()
            return
        }

        // Mostrar nombre del perfil activo
        findViewById<TextView>(R.id.tvPerfilActivo).text = "Perfil: ${perfilActivo.nombre}"

        // Auto-backup silencioso al volver al menú principal
        lifecycleScope.launch {
            BackupManager.autoBackup(this@MenuPrincipalActivity)
        }
    }

    private fun confirmarImportacion(onConfirmar: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Importar backup")
            .setMessage("Esto reemplazara TODOS los datos del perfil activo con el contenido del backup.\n\n¿Deseas continuar?")
            .setPositiveButton("Importar") { _, _ -> onConfirmar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
