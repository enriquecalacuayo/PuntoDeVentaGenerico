package com.example.puntodeventagenerico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comandas")
data class ComandaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val descripcion: String,
    val fechaHora: Long = System.currentTimeMillis()
)
