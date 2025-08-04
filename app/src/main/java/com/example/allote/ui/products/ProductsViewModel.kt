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

data class ProductsUiState(
    val isLoading: Boolean = true,
    val allProducts: List<Product> = emptyList(),
    val formulaciones: List<Formulacion> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: ApplicationType = ApplicationType.PULVERIZACION
) {
    val filteredProducts: List<Product>
        get() = allProducts.filter { product ->
            val matchesTab = product.applicationType == selectedTab
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                product.nombreComercial.contains(searchQuery, ignoreCase = true) ||
                        (product.principioActivo?.contains(searchQuery, ignoreCase = true) ?: false)
            }
            matchesTab && matchesSearch
        }
}

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
        // La lógica de combine se queda igual
        combine(repository.getProductsStream(), repository.getFormulacionesStream()) { products, formulations ->
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    allProducts = products,
                    formulaciones = formulations
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTabSelected(tab: ApplicationType) {
        _uiState.update { it.copy(selectedTab = tab) }
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
}