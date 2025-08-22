package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class ProductsRepository(
    private val productDao: ProductDao,
    private val formulacionDao: FormulacionDao,
    private val vademecumImporter: VademecumImporter
) {
    fun getProductsStream(): Flow<List<Product>> = productDao.getAllStream()
    fun getFormulacionesStream(): Flow<List<Formulacion>> = formulacionDao.getAllFormulacionesStream()

    suspend fun saveProduct(product: Product) {
        productDao.insert(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
    }

    suspend fun searchProductsByName(query: String): List<Product> {
        return productDao.searchByName("%$query%")
    }

    suspend fun searchProductsByNameAndType(query: String, appType: ApplicationType): List<Product> {
        return productDao.searchByNameAndType("%$query%", appType)
    }

    // Nueva función para importar Vademécum
    suspend fun importVademecumIfNeeded() {
        vademecumImporter.importVademecumIfNeeded()
    }
}