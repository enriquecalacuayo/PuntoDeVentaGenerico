package com.example.puntodeventagenerico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historial_personalizaciones")
data class HistorialPersonalizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val descripcion: String,
    val costoExtra: Double = 0.0
)
