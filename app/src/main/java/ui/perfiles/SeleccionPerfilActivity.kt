package com.example.puntodeventagenerico.ui.perfiles

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.puntodeventagenerico.MenuPrincipalActivity
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.Perfil
import com.example.puntodeventagenerico.data.local.ProfileManager

class SeleccionPerfilActivity : AppCompatActivity() {

    private lateinit var lvPerfiles: ListView
    private lateinit var tvSinPerfiles: TextView
    private lateinit var btnCrearPerfil: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_perfil)

        lvPerfiles = findViewById(R.id.lvPerfiles)
        tvSinPerfiles = findViewById(R.id.tvSinPerfiles)
        btnCrearPerfil = findViewById(R.id.btnCrearPerfil)

        btnCrearPerfil.setOnClickListener {
            mostrarDialogoCrearPerfil()
        }

        actualizarLista()
    }

    private fun actualizarLista() {
        val perfiles = ProfileManager.getPerfiles(this)

        if (perfiles.isEmpty()) {
            lvPerfiles.visibility = View.GONE
            tvSinPerfiles.visibility = View.VISIBLE
        } else {
            lvPerfiles.visibility = View.VISIBLE
            tvSinPerfiles.visibility = View.GONE

            val adapter = object : ArrayAdapter<Perfil>(this, R.layout.item_perfil, perfiles) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: layoutInflater.inflate(R.layout.item_perfil, parent, false)
                    val perfil = getItem(position)!!

                    view.findViewById<TextView>(R.id.tvNombrePerfil).text = perfil.nombre
                    view.findViewById<TextView>(R.id.tvDescripcionPerfil).text =
                        perfil.descripcion.ifEmpty { "Sin descripcion" }

                    view.setOnClickListener {
                        seleccionarPerfil(perfil)
                    }

                    view.findViewById<Button>(R.id.btnEliminarPerfil).setOnClickListener {
                        confirmarEliminar(perfil)
                    }

                    return view
                }
            }

            lvPerfiles.adapter = adapter
        }
    }

    private fun seleccionarPerfil(perfil: Perfil) {
        ProfileManager.setPerfilActivo(this, perfil)
        val intent = Intent(this, MenuPrincipalActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun mostrarDialogoCrearPerfil() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_crear_perfil, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombrePerfil)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcionPerfil)

        AlertDialog.Builder(this)
            .setTitle("Nuevo perfil")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val descripcion = etDescripcion.text.toString().trim()
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val perfil = ProfileManager.crearPerfil(this, nombre, descripcion)
                Toast.makeText(this, "Perfil \"${perfil.nombre}\" creado", Toast.LENGTH_SHORT).show()
                actualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(perfil: Perfil) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar perfil")
            .setMessage("¿Eliminar el perfil \"${perfil.nombre}\"?\n\nSe eliminara el acceso a este perfil pero los datos de ventas se conservan en el dispositivo.")
            .setPositiveButton("Eliminar") { _, _ ->
                ProfileManager.eliminarPerfil(this, perfil.id)
                Toast.makeText(this, "Perfil eliminado", Toast.LENGTH_SHORT).show()
                actualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
