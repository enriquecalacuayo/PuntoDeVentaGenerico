package com.example.puntodeventagenerico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personalizaciones")
data class PersonalizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productoId: Int,
    val descripcion: String,
    val costoExtra: Double = 0.0 // ✅ Nuevo campo
)

