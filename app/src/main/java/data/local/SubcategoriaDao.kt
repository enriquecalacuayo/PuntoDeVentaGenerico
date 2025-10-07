package com.example.puntodeventagenerico.data.local

import androidx.room.*

@Dao
interface SubcategoriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(subcategoria: SubcategoriaEntity)

    @Delete
    suspend fun eliminar(subcategoria: SubcategoriaEntity)

    @Query("SELECT * FROM subcategorias ORDER BY nombre ASC")
    suspend fun obtenerTodas(): List<SubcategoriaEntity>
}
