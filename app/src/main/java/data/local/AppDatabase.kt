package com.example.puntodeventagenerico.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductoEntity::class,
        SubcategoriaEntity::class,
        PersonalizacionEntity::class,
        ComandaEntity::class,
        HistorialPersonalizacionEntity::class,
        VentaEntity::class,
        CajaDiaEntity::class,
        GastoEntity::class
    ],
    version = 5, // 🔥 Sube la versión
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun subcategoriaDao(): SubcategoriaDao
    abstract fun personalizacionDao(): PersonalizacionDao
    abstract fun comandaDao(): ComandaDao
    abstract fun historialPersonalizacionDao(): HistorialPersonalizacionDao // ✅ AGREGA ESTO
    abstract fun ventaDao(): VentaDao
    abstract fun cajaDiaDao(): CajaDiaDao
    abstract fun gastoDao(): GastoDao

    companion object {
        const val DATABASE_NAME = "punto_venta_db"
    }
}

