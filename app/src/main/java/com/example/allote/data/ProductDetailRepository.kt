package com.example.allote.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// === CAMBIAMOS EL CONSTRUCTOR PARA QUE SEA INYECTABLE POR HILT ===
@Singleton
class ProductDetailRepository @Inject constructor(
    private val productDao: ProductDao,
    private val formulacionDao: FormulacionDao
) {
    // Obtenemos un producto específico por su ID como un Flow
    fun getProductStream(productId: Int): Flow<Product?> {
        return productDao.getByIdStream(productId)
    }

    

    // === MÉTODO AÑADIDO ===
    // Obtenemos TODAS las formulaciones para poblar el menú desplegable
    fun getAllFormulacionesStream(): Flow<List<Formulacion>> {
        return formulacionDao.getAllFormulacionesStream()
    }

    // Función para actualizar un producto
    suspend fun updateProduct(product: Product) {
        productDao.update(product)
    }
}