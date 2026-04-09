package com.example.puntodeventagenerico.ui.comandas

import android.media.RingtoneManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.puntodeventagenerico.R
import com.example.puntodeventagenerico.data.local.AppDatabase
import com.example.puntodeventagenerico.data.local.ComandaEntity
import com.example.puntodeventagenerico.data.local.ComandaNetworkManager
import com.example.puntodeventagenerico.data.local.ProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VerComandasActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComandaAdapter
    private val listaComandas = mutableListOf<ComandaConEstado>()

    private var listenerJob: Job? = null
    private var escuchando = false
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_comandas)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            ProfileManager.getDatabaseName(applicationContext)
        ).fallbackToDestructiveMigration().build()

        recyclerView = findViewById(R.id.recyclerComandas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComandaAdapter(listaComandas)
        recyclerView.adapter = adapter

        adapter.setOnItemLongClickListener { comanda ->
            eliminarComanda(comanda)
        }

        cargarComandas()
    }

    override fun onResume() {
        super.onResume()
        iniciarEscucha()
    }

    override fun onPause() {
        super.onPause()
        detenerEscucha()
    }

    // ── Cargar comandas existentes desde la DB local ─────────────────────────
    private fun cargarComandas() {
        lifecycleScope.launch {
            val comandas = db.comandaDao().obtenerTodas()
            listaComandas.clear()
            // Comandas de DB: no son nuevas (no vienen de red en este momento)
            listaComandas.addAll(comandas.map { ComandaConEstado(it, esNueva = false) })
            runOnUiThread { adapter.notifyDataSetChanged() }
        }
    }

    // ── Eliminar comanda (long press) ────────────────────────────────────────
    private fun eliminarComanda(comanda: ComandaEntity) {
        lifecycleScope.launch {
            // Solo eliminar de DB si tiene ID real (comandas de red tienen id=0)
            if (comanda.id != 0) {
                db.comandaDao().eliminar(comanda)
            }
            val index = listaComandas.indexOfFirst { it.comanda.id == comanda.id && it.comanda.descripcion == comanda.descripcion }
            if (index != -1) {
                listaComandas.removeAt(index)
                runOnUiThread {
                    adapter.notifyItemRemoved(index)
                    Toast.makeText(this@VerComandasActivity, "Comanda eliminada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Iniciar listener UDP en background ───────────────────────────────────
    private fun iniciarEscucha() {
        // Adquirir MulticastLock: sin esto el chip WiFi filtra los paquetes UDP
        // broadcast a nivel hardware y nunca llegan a la app
        multicastLock = ComandaNetworkManager.adquirirMulticastLock(applicationContext)

        escuchando = true
        listenerJob = lifecycleScope.launch(Dispatchers.IO) {
            ComandaNetworkManager.escuchar(
                context = applicationContext,
                shouldContinue = { escuchando },
                onRecibida = { descripcion, fechaHora ->
                    val comanda = ComandaEntity(
                        id = 0,
                        descripcion = descripcion,
                        fechaHora = fechaHora
                    )
                    val item = ComandaConEstado(comanda, esNueva = true)

                    runOnUiThread {
                        listaComandas.add(0, item)
                        adapter.notifyItemInserted(0)
                        recyclerView.scrollToPosition(0)
                        disparararAlerta()
                    }
                }
            )
        }
    }

    private fun detenerEscucha() {
        escuchando = false
        listenerJob?.cancel()
        listenerJob = null
        // Liberar el lock para no consumir batería en segundo plano
        multicastLock?.release()
        multicastLock = null
    }

    // ── Alerta visual + sonora al recibir comanda por red ───────────────────
    private fun disparararAlerta() {
        // Sonido de notificación del sistema
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri)?.play()
        } catch (_: Exception) { }

        Toast.makeText(this, "Nueva comanda recibida", Toast.LENGTH_SHORT).show()
    }
}
