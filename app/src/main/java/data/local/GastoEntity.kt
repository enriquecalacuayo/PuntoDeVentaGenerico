package com.example.puntodeventagenerico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gastos_dia")
data class GastoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: Long,
    val nombre: String,
    val monto: Double
)
