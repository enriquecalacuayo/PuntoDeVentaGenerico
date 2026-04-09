package com.example.puntodeventagenerico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CajaDiaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(caja: CajaDiaEntity)

    @Query("SELECT * FROM caja_dia WHERE fecha = :fecha LIMIT 1")
    suspend fun obtenerPorFecha(fecha: Long): CajaDiaEntity?

    @Query("SELECT * FROM caja_dia")
    suspend fun obtenerTodas(): List<CajaDiaEntity>

    @Query("DELETE FROM caja_dia")
    suspend fun eliminarTodas()
}
