package com.example.allote.data

import androidx.room.withTransaction
import com.example.allote.ui.recetas.ProductoRecetaItem
import kotlinx.coroutines.flow.Flow

class RecetasRepository(
    private val jobDao: JobDao,
    private val recipeDao: RecipeDao,
    private val productDao: ProductDao,
    private val formulacionDao: FormulacionDao,
    private val loteDao: LoteDao,
    private val appDatabase: AppDatabase
) {

    fun getJobStream(jobId: Int): Flow<Job?> = jobDao.getByIdStream(jobId)
    fun getRecipeStream(jobId: Int): Flow<Recipe?> = recipeDao.getRecipeByJobIdStream(jobId)
    fun getRecipeProductsStream(recipeId: Int): Flow<List<RecipeProduct>> = recipeDao.getRecipeProductsByRecipeIdStream(recipeId)

    // Nuevas funciones para recetas por lote
    fun getRecipeByLoteStream(loteId: Int): Flow<Recipe?> = recipeDao.getRecipeByLoteIdStream(loteId)
    fun getAllRecipesByJobStream(jobId: Int): Flow<List<Recipe>> = recipeDao.getAllRecipesByJobIdStream(jobId)
    fun getLoteStream(loteId: Int): Flow<Lote?> = loteDao.getByIdStream(loteId)
    fun getAllLotesByJobStream(jobId: Int): Flow<List<Lote>> = loteDao.getByJobIdStream(jobId)

    // === CAMBIO CLAVE: Ahora estas funciones devuelven Flow y usan los métodos "Stream" del DAO ===
    fun getFormulacionesStream(): Flow<List<Formulacion>> = formulacionDao.getAllStream()
    fun getAllProductsStream(): Flow<List<Product>> = productDao.getAllStream()

    suspend fun saveFullRecipe(
        jobId: Int,
        loteId: Int? = null, // Nuevo parámetro opcional
        currentRecipe: Recipe?,
        hectareas: Double,
        caudal: Double,
        capacidadTachada: Double?,
        totalMezcla: Double,
        resumen: String,
        productosReceta: List<ProductoRecetaItem>
    ): Recipe {
        return appDatabase.withTransaction {
            val recipeToSave = (currentRecipe?.copy(
                hectareas = hectareas, caudal = caudal, caldoPorTachada = capacidadTachada ?: 0.0,
                totalCaldo = totalMezcla, fechaCreacion = System.currentTimeMillis(), resumen = resumen,
                loteId = loteId
            ) ?: Recipe(
                jobId = jobId, loteId = loteId, hectareas = hectareas, caudal = caudal,
                caldoPorTachada = capacidadTachada ?: 0.0, totalCaldo = totalMezcla,
                fechaCreacion = System.currentTimeMillis(), resumen = resumen
            ))

            val recipeId = if (recipeToSave.id == 0) {
                recipeDao.insertRecipe(recipeToSave).toInt()
            } else {
                recipeDao.updateRecipe(recipeToSave)
                recipeToSave.id
            }

            val existingDbProducts = recipeDao.getRecipeProductsByRecipeId(recipeId)
            val newProductIds = productosReceta.map { it.productId }.toSet()

            existingDbProducts.filter { it.productId !in newProductIds }.forEach {
                recipeDao.deleteRecipeProduct(it)
            }

            productosReceta.forEach { item ->
                val rp = RecipeProduct(
                    id = item.id.takeIf { it != 0 } ?: existingDbProducts.find { it.productId == item.productId }?.id ?: 0,
                    recipeId = recipeId, productId = item.productId, dosis = item.dosis,
                    cantidadTotal = item.cantidadTotal, ordenMezclado = item.ordenMezclado,
                    unidadDosis = item.unidadDosis
                )
                recipeDao.insertRecipeProduct(rp)
            }
            recipeToSave.copy(id = recipeId)
        }
    }

    suspend fun deleteFullRecipe(recipe: Recipe) {
        appDatabase.withTransaction {
            recipeDao.deleteRecipeProductsByRecipeId(recipe.id)
            recipeDao.deleteRecipe(recipe)
        }
    }
}