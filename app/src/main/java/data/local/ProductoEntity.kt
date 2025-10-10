package com.example.puntodeventagenerico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String,
    val precioPublico: Double,
    val costoUnitario: Double,
    val ocultarEnComandas: Boolean = false // 👈 nuevo campo
)
