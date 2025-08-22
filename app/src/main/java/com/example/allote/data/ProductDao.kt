package com.example.allote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY nombreComercial ASC")
    fun getAllStream(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY nombreComercial ASC")
    suspend fun getAll(): List<Product>

    // Búsqueda por nombre, principio activo Y número de registro
    @Query("""
        SELECT * FROM products 
        WHERE (nombreComercial LIKE :searchQuery 
               OR principioActivo LIKE :searchQuery 
               OR numeroRegistroSenasa LIKE :searchQuery) 
               AND (applicationType = :appType OR applicationType = 'AMBOS')
        ORDER BY nombreComercial ASC
    """)
    suspend fun searchByNameAndType(searchQuery: String, appType: ApplicationType): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(products: List<Product>): List<Long>

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products WHERE id = :productId")
    fun getByIdStream(productId: Int): Flow<Product?>

    @Query("SELECT * FROM products WHERE nombreComercial LIKE :searchQuery OR principioActivo LIKE :searchQuery OR numeroRegistroSenasa LIKE :searchQuery ORDER BY nombreComercial ASC")
    suspend fun searchByName(searchQuery: String): List<Product>

    // Verificar si ya hay productos del Vademécum importados
    @Query("SELECT COUNT(*) FROM products WHERE isFromVademecum = 1")
    suspend fun getVademecumProductsCount(): Int
}