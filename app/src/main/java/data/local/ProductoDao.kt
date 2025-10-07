package com.example.puntodeventagenerico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarProducto(producto: ProductoEntity)

    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    suspend fun obtenerProductos(): List<ProductoEntity>
}
