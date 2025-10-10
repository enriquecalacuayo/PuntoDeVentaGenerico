package com.example.puntodeventagenerico.data.local

import androidx.room.*

@Dao
interface PersonalizacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(personalizacion: PersonalizacionEntity)

    @Update
    suspend fun actualizar(personalizacion: PersonalizacionEntity)

    @Delete
    suspend fun eliminar(personalizacion: PersonalizacionEntity)

    @Query("SELECT * FROM personalizaciones WHERE productoId = :productoId")
    suspend fun obtenerPorProducto(productoId: Int): List<PersonalizacionEntity>
}
