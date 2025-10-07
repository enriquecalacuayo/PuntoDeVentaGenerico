package com.example.puntodeventagenerico.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductoEntity::class,
        SubcategoriaEntity::class,
        PersonalizacionEntity::class
    ],
    version = 5, // ⬆️ Súbelo al siguiente número
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun subcategoriaDao(): SubcategoriaDao
    abstract fun personalizacionDao(): PersonalizacionDao
}