package com.example.allote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY nombreComercial ASC")
    fun getAllStream(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY nombreComercial ASC")
    suspend fun getAll(): List<Product>

    // === FUNCIÓN CORREGIDA ===
    // Ahora acepta el tipo de aplicación como parámetro para filtrar en la consulta.
    @Query("SELECT * FROM products WHERE (nombreComercial LIKE :searchQuery OR principioActivo LIKE :searchQuery) AND applicationType = :appType ORDER BY nombreComercial ASC")
    suspend fun searchByNameAndType(searchQuery: String, appType: ApplicationType): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products WHERE id = :productId")
    fun getByIdStream(productId: Int): Flow<Product?>

    @Query("SELECT * FROM products WHERE nombreComercial LIKE :searchQuery OR principioActivo LIKE :searchQuery ORDER BY nombreComercial ASC")
    suspend fun searchByName(searchQuery: String): List<Product>
}