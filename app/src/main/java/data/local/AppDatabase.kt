package com.example.puntodeventagenerico.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductoEntity::class,
        SubcategoriaEntity::class,
        PersonalizacionEntity::class,
        ComandaEntity::class,
        HistorialPersonalizacionEntity::class
    ],
    version = 4, // 🔹 Sube la versión para forzar la regeneración del esquema
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun subcategoriaDao(): SubcategoriaDao
    abstract fun personalizacionDao(): PersonalizacionDao
    abstract fun comandaDao(): ComandaDao
    abstract fun historialPersonalizacionDao(): HistorialPersonalizacionDao

    companion object {
        const val DATABASE_NAME = "punto_venta_db"
    }
}

