package com.example.allote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    // --- MÉTODOS PARA Recipe ---

    // Cambiado: Ahora tenemos una versión que devuelve Flow
    @Query("SELECT * FROM recipes WHERE jobId = :jobId LIMIT 1")
    fun getRecipeByJobIdStream(jobId: Int): Flow<Recipe?>

    // Añadido: Mantenemos la versión `suspend` para accesos de una sola vez
    @Query("SELECT * FROM recipes WHERE jobId = :jobId LIMIT 1")
    suspend fun getRecipeByJobId(jobId: Int): Recipe?

    // Nuevas consultas para recetas por lote
    @Query("SELECT * FROM recipes WHERE loteId = :loteId LIMIT 1")
    fun getRecipeByLoteIdStream(loteId: Int): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE loteId = :loteId LIMIT 1")
    suspend fun getRecipeByLoteId(loteId: Int): Recipe?

    @Query("SELECT * FROM recipes WHERE jobId = :jobId")
    fun getAllRecipesByJobIdStream(jobId: Int): Flow<List<Recipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)


    // --- MÉTODOS PARA RecipeProduct ---

    // Cambiado: Ahora tenemos una versión que devuelve Flow
    @Query("SELECT * FROM recipe_products WHERE recipeId = :recipeId ORDER BY ordenMezclado ASC")
    fun getRecipeProductsByRecipeIdStream(recipeId: Int): Flow<List<RecipeProduct>>

    // Añadido: Mantenemos la versión `suspend` para accesos de una sola vez
    @Query("SELECT * FROM recipe_products WHERE recipeId = :recipeId ORDER BY ordenMezclado ASC")
    suspend fun getRecipeProductsByRecipeId(recipeId: Int): List<RecipeProduct>

    // Cambiado: Le añadimos la estrategia de conflicto para que funcione como "insertar o actualizar"
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeProduct(recipeProduct: RecipeProduct): Long

    @Update
    suspend fun updateRecipeProduct(recipeProduct: RecipeProduct)

    @Delete
    suspend fun deleteRecipeProduct(recipeProduct: RecipeProduct)

    @Query("DELETE FROM recipe_products WHERE recipeId = :recipeId")
    suspend fun deleteRecipeProductsByRecipeId(recipeId: Int)
}