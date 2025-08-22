package com.example.allote.ui.products

import com.example.allote.data.ApplicationType
import com.example.allote.data.Formulacion
import com.example.allote.data.Product

data class ProductsUiState(
    val allProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val formulaciones: List<Formulacion> = emptyList(),
    val selectedTab: ApplicationType = ApplicationType.PULVERIZACION,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val vademecumProductsCount: Int = 0, // Nuevo campo
    val isImportingVademecum: Boolean = false // Nuevo campo
)
