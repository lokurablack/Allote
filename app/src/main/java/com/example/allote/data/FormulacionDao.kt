package com.example.allote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FormulacionDao {

    @Query("SELECT * FROM formulaciones ORDER BY ordenMezcla ASC")
    fun getAllFormulacionesStream(): Flow<List<Formulacion>>

    // Añadido: Versión `suspend` para obtener todas las formulaciones de una sola vez
    @Query("SELECT * FROM formulaciones ORDER BY ordenMezcla ASC")
    suspend fun getAllFormulaciones(): List<Formulacion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(formulaciones: List<Formulacion>)

    @Query("SELECT * FROM formulaciones ORDER BY ordenMezcla ASC")
    fun getAllStream(): Flow<List<Formulacion>>

    @Update
    suspend fun updateFormulaciones(formulaciones: List<Formulacion>)

    @Delete
    suspend fun delete(formulaciones: List<Formulacion>)

    // Corregido: El parámetro debe ser un Int (el ID), no un String (el nombre)
    @Query("SELECT COUNT(*) FROM products WHERE formulacionId = :formulacionId")
    suspend fun getProductCountForFormulacion(formulacionId: Int): Int

    @Query("SELECT * FROM formulaciones WHERE id = :formulacionId")
    fun getByIdStream(formulacionId: Int): Flow<Formulacion?>

    @Query("SELECT * FROM formulaciones WHERE nombre = :nombre LIMIT 1")
    suspend fun getByName(nombre: String): Formulacion?
}