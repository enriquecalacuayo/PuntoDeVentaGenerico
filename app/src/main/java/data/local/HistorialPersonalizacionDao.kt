package com.example.puntodeventagenerico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistorialPersonalizacionDao {
    @Insert
    suspend fun insertar(historial: HistorialPersonalizacionEntity)

    @Query("SELECT * FROM historial_personalizacion ORDER BY id DESC")
    suspend fun obtenerTodos(): List<HistorialPersonalizacionEntity>
}
