package com.example.puntodeventagenerico.data.local

import android.content.Context
import android.net.Uri
import androidx.room.Room
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {

    private const val BACKUP_VERSION = 1
    private const val BACKUPS_FOLDER = "backups"

    // ─── AUTO-BACKUP ─────────────────────────────────────────────────────────────
    // Guarda un backup local en getExternalFilesDir("backups")
    // Se llama automáticamente desde MenuPrincipalActivity.onResume()
    suspend fun autoBackup(context: Context) {
        try {
            val perfil = ProfileManager.getPerfilActivo(context) ?: return
            val db = abrirDB(context) ?: return
            val json = serializarDB(db, perfil)
            db.close()

            val carpeta = File(context.getExternalFilesDir(null), BACKUPS_FOLDER)
            if (!carpeta.exists()) carpeta.mkdirs()

            val nombreArchivo = "backup_${sanitizar(perfil.nombre)}.json"
            File(carpeta, nombreArchivo).writeText(json.toString(2))
        } catch (_: Exception) { }
    }

    // ─── EXPORTAR A URI (SAF) ─────────────────────────────────────────────────
    // El usuario elige dónde guardar (Descargas, Drive, etc.)
    suspend fun exportar(context: Context, uri: Uri): Boolean {
        return try {
            val perfil = ProfileManager.getPerfilActivo(context) ?: return false
            val db = abrirDB(context) ?: return false
            val json = serializarDB(db, perfil)
            db.close()

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) { false }
    }

    // ─── IMPORTAR DESDE URI (SAF) ─────────────────────────────────────────────
    // Lee el archivo elegido por el usuario y restaura los datos en el perfil activo
    // Retorna el nombre del perfil del backup, o null si hubo error
    suspend fun importar(context: Context, uri: Uri): String? {
        return try {
            val contenido = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return null

            val json = JSONObject(contenido)
            val perfilNombreBackup = json.optString("perfilNombre", "Desconocido")

            val perfil = ProfileManager.getPerfilActivo(context) ?: return null
            val db = abrirDB(context) ?: return null
            restaurarDB(db, json)
            db.close()

            perfilNombreBackup
        } catch (_: Exception) { null }
    }

    // ─── SERIALIZACIÓN ───────────────────────────────────────────────────────────
    private suspend fun serializarDB(db: AppDatabase, perfil: Perfil): JSONObject {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("perfilId", perfil.id)
        root.put("perfilNombre", perfil.nombre)
        root.put("fechaBackup", System.currentTimeMillis())

        // Subcategorías
        root.put("subcategorias", JSONArray().apply {
            db.subcategoriaDao().obtenerTodas().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("nombre", s.nombre)
                })
            }
        })

        // Productos
        root.put("productos", JSONArray().apply {
            db.productoDao().obtenerTodos().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("nombre", p.nombre)
                    put("categoria", p.categoria)
                    put("precioPublico", p.precioPublico)
                    put("costoUnitario", p.costoUnitario)
                    put("ocultarEnComandas", p.ocultarEnComandas)
                })
            }
        })

        // Personalizaciones
        root.put("personalizaciones", JSONArray().apply {
            db.personalizacionDao().obtenerTodas().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("productoId", p.productoId)
                    put("descripcion", p.descripcion)
                    put("costoExtra", p.costoExtra)
                })
            }
        })

        // Historial personalizaciones
        root.put("historialPersonalizaciones", JSONArray().apply {
            db.historialPersonalizacionDao().obtenerTodos().forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("descripcion", h.descripcion)
                    put("costoExtra", h.costoExtra)
                })
            }
        })

        // Ventas
        root.put("ventas", JSONArray().apply {
            db.ventaDao().obtenerTodas().forEach { v ->
                put(JSONObject().apply {
                    put("id", v.id)
                    put("productosVendidos", v.productosVendidos)
                    put("totalVenta", v.totalVenta)
                    put("ganancia", v.ganancia)
                    put("fecha", v.fecha)
                    put("pagoConTarjeta", v.pagoConTarjeta)
                })
            }
        })

        // Comandas
        root.put("comandas", JSONArray().apply {
            db.comandaDao().obtenerTodas().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("descripcion", c.descripcion)
                    put("fechaHora", c.fechaHora)
                })
            }
        })

        // Caja diaria
        root.put("cajaDias", JSONArray().apply {
            db.cajaDiaDao().obtenerTodas().forEach { c ->
                put(JSONObject().apply {
                    put("fecha", c.fecha)
                    put("cajaInicio", c.cajaInicio)
                    put("gastos", c.gastos)
                    put("cajaFinal", c.cajaFinal)
                })
            }
        })

        // Gastos
        root.put("gastos", JSONArray().apply {
            db.gastoDao().obtenerTodos().forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("fecha", g.fecha)
                    put("nombre", g.nombre)
                    put("monto", g.monto)
                })
            }
        })

        return root
    }

    // ─── RESTAURACIÓN ────────────────────────────────────────────────────────────
    private suspend fun restaurarDB(db: AppDatabase, json: JSONObject) {
        // Limpiar tablas respetando dependencias
        db.personalizacionDao().eliminarTodas()
        db.historialPersonalizacionDao().eliminarTodos()
        db.productoDao().eliminarTodos()
        db.subcategoriaDao().eliminarTodas()
        db.ventaDao().eliminarTodas()
        db.comandaDao().eliminarTodas()
        db.cajaDiaDao().eliminarTodas()
        db.gastoDao().eliminarTodos()

        // Restaurar subcategorías
        val subcats = json.optJSONArray("subcategorias") ?: JSONArray()
        for (i in 0 until subcats.length()) {
            val o = subcats.getJSONObject(i)
            db.subcategoriaDao().insertar(SubcategoriaEntity(
                id = o.getInt("id"),
                nombre = o.getString("nombre")
            ))
        }

        // Restaurar productos
        val productos = json.optJSONArray("productos") ?: JSONArray()
        for (i in 0 until productos.length()) {
            val o = productos.getJSONObject(i)
            db.productoDao().insertar(ProductoEntity(
                id = o.getInt("id"),
                nombre = o.getString("nombre"),
                categoria = o.getString("categoria"),
                precioPublico = o.getDouble("precioPublico"),
                costoUnitario = o.getDouble("costoUnitario"),
                ocultarEnComandas = o.optBoolean("ocultarEnComandas", false)
            ))
        }

        // Restaurar personalizaciones
        val persos = json.optJSONArray("personalizaciones") ?: JSONArray()
        for (i in 0 until persos.length()) {
            val o = persos.getJSONObject(i)
            db.personalizacionDao().insertar(PersonalizacionEntity(
                id = o.getInt("id"),
                productoId = o.getInt("productoId"),
                descripcion = o.getString("descripcion"),
                costoExtra = o.getDouble("costoExtra")
            ))
        }

        // Restaurar historial personalizaciones
        val historial = json.optJSONArray("historialPersonalizaciones") ?: JSONArray()
        for (i in 0 until historial.length()) {
            val o = historial.getJSONObject(i)
            db.historialPersonalizacionDao().insertar(HistorialPersonalizacionEntity(
                id = o.getInt("id"),
                descripcion = o.getString("descripcion"),
                costoExtra = o.getDouble("costoExtra")
            ))
        }

        // Restaurar ventas
        val ventas = json.optJSONArray("ventas") ?: JSONArray()
        for (i in 0 until ventas.length()) {
            val o = ventas.getJSONObject(i)
            db.ventaDao().insertar(VentaEntity(
                id = o.getInt("id"),
                productosVendidos = o.getString("productosVendidos"),
                totalVenta = o.getDouble("totalVenta"),
                ganancia = o.getDouble("ganancia"),
                fecha = o.getLong("fecha"),
                pagoConTarjeta = o.optBoolean("pagoConTarjeta", false)
            ))
        }

        // Restaurar comandas
        val comandas = json.optJSONArray("comandas") ?: JSONArray()
        for (i in 0 until comandas.length()) {
            val o = comandas.getJSONObject(i)
            db.comandaDao().insertar(ComandaEntity(
                id = o.getInt("id"),
                descripcion = o.getString("descripcion"),
                fechaHora = o.getLong("fechaHora")
            ))
        }

        // Restaurar caja diaria
        val cajas = json.optJSONArray("cajaDias") ?: JSONArray()
        for (i in 0 until cajas.length()) {
            val o = cajas.getJSONObject(i)
            db.cajaDiaDao().insertar(CajaDiaEntity(
                fecha = o.getLong("fecha"),
                cajaInicio = o.getDouble("cajaInicio"),
                gastos = o.getDouble("gastos"),
                cajaFinal = o.getDouble("cajaFinal")
            ))
        }

        // Restaurar gastos
        val gastos = json.optJSONArray("gastos") ?: JSONArray()
        for (i in 0 until gastos.length()) {
            val o = gastos.getJSONObject(i)
            db.gastoDao().insertar(GastoEntity(
                id = o.getInt("id"),
                fecha = o.getLong("fecha"),
                nombre = o.getString("nombre"),
                monto = o.getDouble("monto")
            ))
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────
    private fun abrirDB(context: Context): AppDatabase? {
        return try {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                ProfileManager.getDatabaseName(context)
            ).fallbackToDestructiveMigration().build()
        } catch (_: Exception) { null }
    }

    fun getNombreArchivoExport(context: Context): String {
        val perfil = ProfileManager.getPerfilActivo(context)
        val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val nombre = if (perfil != null) sanitizar(perfil.nombre) else "perfil"
        return "backup_${nombre}_$fecha.json"
    }

    private fun sanitizar(texto: String): String =
        texto.lowercase().replace(Regex("[^a-z0-9]"), "_")
}
