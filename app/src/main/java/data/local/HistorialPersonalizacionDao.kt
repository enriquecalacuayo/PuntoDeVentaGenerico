package com.example.puntodeventagenerico.data.local

import androidx.room.*

@Dao
interface HistorialPersonalizacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(historial: HistorialPersonalizacionEntity)

    @Query("SELECT * FROM historial_personalizaciones")
    suspend fun obtenerTodas(): List<HistorialPersonalizacionEntity>
}
