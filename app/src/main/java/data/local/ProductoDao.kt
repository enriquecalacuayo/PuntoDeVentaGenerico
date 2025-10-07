package com.example.puntodeventagenerico.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: ProductoEntity): Long

    @Query("SELECT * FROM productos")
    suspend fun obtenerTodos(): List<ProductoEntity>

    @Delete
    suspend fun eliminar(producto: ProductoEntity)
}
