package com.example.allote.ui.productdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Formulacion
import com.example.allote.data.Product
import com.example.allote.data.ProductDetailRepository
import com.example.allote.ui.AppDestinations // <-- IMPORTANTE: Añadir esta importación
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val allFormulaciones: List<Formulacion> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: ProductDetailRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- CORRECCIÓN CLAVE ---
    // Antes: private val productId: Int? = savedStateHandle["PRODUCT_ID"]
    // Ahora: Usamos la constante correcta desde AppDestinations
    private val productId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.PRODUCT_ID_ARG, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProductDetailUiState> = productId.flatMapLatest { id ->
        if (id == 0) {
            flowOf(ProductDetailUiState(isLoading = false)) // ID inválido, terminamos la carga
        } else {
            // Combinamos los dos flujos: el producto específico y la lista de todas las formulaciones
            repository.getProductStream(id).combine(repository.getAllFormulacionesStream()) { product, formulations ->
                ProductDetailUiState(
                    product = product,
                    allFormulaciones = formulations,
                    isLoading = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ProductDetailUiState(isLoading = true)
    )

    fun updateProduct(product: Product) = viewModelScope.launch {
        repository.updateProduct(product)
    }
}