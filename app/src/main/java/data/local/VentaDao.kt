package com.example.puntodeventagenerico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VentaDao {

    @Insert
    suspend fun insertar(venta: VentaEntity)

    // 🔹 Obtener todas las ventas
    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    suspend fun obtenerTodas(): List<VentaEntity>

    // 🔹 Obtener las ventas de un día específico
    @Query("""
        SELECT * FROM ventas 
        WHERE fecha BETWEEN :inicioDia AND :finDia 
        ORDER BY fecha DESC
    """)
    suspend fun obtenerPorDia(inicioDia: Long, finDia: Long): List<VentaEntity>

    @Query("DELETE FROM ventas WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}
