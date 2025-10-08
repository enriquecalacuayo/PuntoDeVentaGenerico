package com.example.puntodeventagenerico.data.local

import androidx.room.*

@Dao
interface ComandaDao {
    @Insert
    suspend fun insertar(comanda: ComandaEntity)

    @Query("SELECT * FROM comandas ORDER BY fechaHora ASC")
    suspend fun obtenerTodas(): List<ComandaEntity>

    @Delete
    suspend fun eliminar(comanda: ComandaEntity)
}
