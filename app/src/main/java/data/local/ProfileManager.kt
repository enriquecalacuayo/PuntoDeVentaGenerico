package com.example.puntodeventagenerico.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ProfileManager {

    private const val PREFS_NAME = "perfiles_prefs"
    private const val KEY_PERFILES = "perfiles"
    private const val KEY_PERFIL_ACTIVO_ID = "perfil_activo_id"

    // Retorna la lista de perfiles guardados
    fun getPerfiles(context: Context): List<Perfil> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PERFILES, "[]") ?: "[]"
        return parsePerfiles(json)
    }

    // Retorna el perfil actualmente activo, o null si no hay ninguno
    fun getPerfilActivo(context: Context): Perfil? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activoId = prefs.getString(KEY_PERFIL_ACTIVO_ID, null) ?: return null
        return getPerfiles(context).find { it.id == activoId }
    }

    // Establece el perfil activo por ID
    fun setPerfilActivo(context: Context, perfil: Perfil) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PERFIL_ACTIVO_ID, perfil.id).apply()
    }

    // Crea un nuevo perfil y lo guarda
    fun crearPerfil(context: Context, nombre: String, descripcion: String): Perfil {
        val perfil = Perfil(
            id = UUID.randomUUID().toString().replace("-", "").take(12),
            nombre = nombre.trim(),
            descripcion = descripcion.trim()
        )
        val lista = getPerfiles(context).toMutableList()
        lista.add(perfil)
        guardarPerfiles(context, lista)
        return perfil
    }

    // Elimina un perfil por ID
    fun eliminarPerfil(context: Context, perfilId: String) {
        val lista = getPerfiles(context).filter { it.id != perfilId }
        guardarPerfiles(context, lista)
        // Si el perfil activo fue eliminado, limpiar selección
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_PERFIL_ACTIVO_ID, null) == perfilId) {
            prefs.edit().remove(KEY_PERFIL_ACTIVO_ID).apply()
        }
    }

    // Retorna el nombre de la base de datos para el perfil activo
    fun getDatabaseName(context: Context): String {
        val activo = getPerfilActivo(context)
        return if (activo != null) "pv_${activo.id}.db" else "punto_venta_db"
    }

    private fun guardarPerfiles(context: Context, perfiles: List<Perfil>) {
        val array = JSONArray()
        perfiles.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("nombre", p.nombre)
            obj.put("descripcion", p.descripcion)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PERFILES, array.toString()).apply()
    }

    private fun parsePerfiles(json: String): List<Perfil> {
        val lista = mutableListOf<Perfil>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                Perfil(
                    id = obj.getString("id"),
                    nombre = obj.getString("nombre"),
                    descripcion = obj.getString("descripcion")
                )
            )
        }
        return lista
    }
}
