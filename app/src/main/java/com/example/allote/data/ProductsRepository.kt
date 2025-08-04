package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class ProductsRepository(
    private val productDao: ProductDao,
    private val formulacionDao: FormulacionDao
) {
    // Exponemos los datos como flujos para que la UI reaccione a los cambios
    fun getProductsStream(): Flow<List<Product>> = productDao.getAllStream()
    fun getFormulacionesStream(): Flow<List<Formulacion>> = formulacionDao.getAllFormulacionesStream()

    // Funciones para modificar la base de datos
    suspend fun saveProduct(product: Product) {
        productDao.insert(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
    }

    suspend fun searchProductsByName(query: String): List<Product> {
        // Llamamos a la función del DAO que busca por nombre en todos los productos
        return productDao.searchByName("%$query%")
    }
}