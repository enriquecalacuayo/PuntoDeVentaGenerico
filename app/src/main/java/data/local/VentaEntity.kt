package com.example.puntodeventagenerico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ventas")
data class VentaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productosVendidos: String,
    val totalVenta: Double,
    val ganancia: Double,
    val fecha: Long = System.currentTimeMillis(),
    val pagoConTarjeta: Boolean = false
)
