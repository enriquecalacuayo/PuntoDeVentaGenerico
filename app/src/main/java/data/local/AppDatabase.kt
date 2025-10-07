package com.example.puntodeventagenerico.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProductoEntity::class, SubcategoriaEntity::class],
    version = 2, // ⚠️ Si ya estaba en 2, súbelo a 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun subcategoriaDao(): SubcategoriaDao
}