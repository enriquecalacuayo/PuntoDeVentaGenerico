package com.example.puntodeventagenerico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caja_dia")
data class CajaDiaEntity(
    @PrimaryKey val fecha: Long,
    val cajaInicio: Double,
    val gastos: Double,
    val cajaFinal: Double
)
