package com.example.allote.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.ApplicationType
import com.example.allote.data.Formulacion
import com.example.allote.data.Product
import com.example.allote.data.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.allote.ui.products.ProductsUiState

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val repository: ProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    // === AÑADIMOS EL ESTADO Y FUNCIONES PARA LA BÚSQUEDA ===

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    init {
        combine(repository.getProductsStream(), repository.getFormulacionesStream()) { products, formulations ->
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    allProducts = products,
                    formulaciones = formulations
                )
            }
            filterProducts() // Actualizar la lista filtrada cada vez que cambian los productos
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            repository.importVademecumIfNeeded()
            updateVademecumCount()
            filterProducts() // Actualizar después de importar
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterProducts()
    }

    fun onTabSelected(tab: ApplicationType) {
        _uiState.update { it.copy(selectedTab = tab) }
        filterProducts()
    }

    fun saveProduct(product: Product) = viewModelScope.launch {
        repository.saveProduct(product)
    }

    fun onClearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun deleteProduct(product: Product) = viewModelScope.launch {
        repository.deleteProduct(product)
    }

    // --- NUEVA FUNCIÓN ---
    fun searchProducts(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                // En esta pantalla, la búsqueda no necesita filtrar por tipo de app,
                // ya que el filtrado principal se hace con las pestañas.
                // Buscamos en todos los productos.
                _searchResults.value = repository.searchProductsByName(query)
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    // --- NUEVA FUNCIÓN ---
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun importVademecum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingVademecum = true) }
            try {
                repository.importVademecumIfNeeded()
                updateVademecumCount()
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _uiState.update { it.copy(isImportingVademecum = false) }
            }
        }
    }

    private suspend fun updateVademecumCount() {
        // Implementar conteo de productos del Vademécum
        // val count = repository.getVademecumProductsCount()
        // _uiState.update { it.copy(vademecumProductsCount = count) }
    }

    private fun filterProducts() {
        val currentState = _uiState.value
        val filtered = if (currentState.searchQuery.isBlank()) {
            currentState.allProducts.filter { product ->
                when (currentState.selectedTab) {
                    ApplicationType.PULVERIZACION ->
                        product.applicationType == ApplicationType.PULVERIZACION.name ||
                                product.applicationType == ApplicationType.AMBOS.name
                    ApplicationType.ESPARCIDO ->
                        product.applicationType == ApplicationType.ESPARCIDO.name ||
                                product.applicationType == ApplicationType.AMBOS.name
                    ApplicationType.AMBOS -> true
                }
            }
        } else {
            viewModelScope.launch {
                val searchResults = repository.searchProductsByNameAndType(
                    currentState.searchQuery,
                    currentState.selectedTab
                )
                _uiState.update { it.copy(filteredProducts = searchResults) }
            }
            return
        }

        _uiState.update { it.copy(filteredProducts = filtered) }
    }
}