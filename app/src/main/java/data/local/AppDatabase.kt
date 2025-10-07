package com.example.puntodeventagenerico.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductoEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
}
