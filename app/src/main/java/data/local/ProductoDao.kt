package com.example.puntodeventagenerico.data.local
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductoDao {

    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    @Query("SELECT * FROM productos WHERE categoria = :categoria")
    suspend fun obtenerPorCategoria(categoria: String): List<ProductoEntity>

    @Query("SELECT * FROM productos")
    suspend fun obtenerTodos(): List<ProductoEntity>

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    @Delete
    suspend fun eliminar(producto: ProductoEntity)

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): ProductoEntity
}