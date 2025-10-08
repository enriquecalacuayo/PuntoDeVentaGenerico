package com.example.puntodeventagenerico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GastoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(gasto: GastoEntity)

    @Query("SELECT * FROM gastos_dia WHERE fecha = :fecha")
    suspend fun obtenerPorFecha(fecha: Long): List<GastoEntity>

    @Query("DELETE FROM gastos_dia WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}
