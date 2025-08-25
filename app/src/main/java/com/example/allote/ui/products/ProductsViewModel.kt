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
import java.text.Normalizer

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val repository: ProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    // === AÑADIMOS EL ESTADO Y FUNCIONES PARA LA BÚSQUEDA ===

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    // Números de registro específicos para productos de ESPARCIDO (del CSV)
    private val esparcidoRegistrosSet = setOf(
        "30274", "33056", "33211", "33218", "33238", "33329", "33667", "34245", "34982", "35201", 
        "35265", "35737", "36507", "36975", "37369", "40289", "40290", "40292", "40294", "40296", 
        "40297", "40439", "40513", "40798", "42094 BIO", "SE-179", "SE-180", "SE-181", "SE-182", 
        "32916", "33423", "34251", "35771", "36686", "31888", "33446", "33569", "33720", "33872", 
        "33884", "33906", "34362", "34499", "34947", "35084", "35145", "35146", "36304", "36377", 
        "36782", "37861", "37870", "37950", "38413", "39251", "39503", "39703", "39856", "39904", 
        "39907", "40008", "40066", "40203", "40205", "40328", "40371", "40451", "40768", "40899", 
        "40916", "40999", "41146", "41147", "41288", "41295", "41635", "LJ 00483", "LJ 00492", 
        "LJ 00493", "LJ 00500", "LJ 00501", "LJ 00504", "LJ 00505", "LJ 00512", "LJ 00513", 
        "LJ 00516", "LJ 00517", "LJ 00518", "LJ 00521", "LJ 00522", "LJ 00525", "SE-203", 
        "SE-439", "SE-495"
    )

    // Conjunto normalizado para comparación robusta
    private val esparcidoRegistrosNormalized: Set<String> = esparcidoRegistrosSet.map { normalizarRegistro(it) }.toSet()

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
        filterProducts()
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

    private fun normalizarRegistro(registro: String?): String =
        registro
            ?.lowercase()
            // eliminar cualquier carácter no alfanumérico para maximizar el match
            ?.replace("[^a-z0-9]".toRegex(), "")
            ?.trim()
            ?: ""

    private fun normalizarTexto(texto: String?): String =
        texto?.let {
            val n = Normalizer.normalize(it, Normalizer.Form.NFD)
            n.replace("\\p{M}+".toRegex(), "") // quitar acentos
                .lowercase()
                .replace("[^a-z0-9\\s]".toRegex(), "") // quitar símbolos no alfanuméricos (®, ™, puntuación)
                .replace("\\s+".toRegex(), " ") // colapsar espacios múltiples
                .trim()
        } ?: ""

    private fun filterProducts() {
        val currentState = _uiState.value
        val filtered = if (currentState.searchQuery.isBlank()) {
            currentState.allProducts.filter { product ->
                val registroNormalizado = normalizarRegistro(product.numeroRegistroSenasa)
                val esDeEsparcidoPorRegistro = esparcidoRegistrosNormalized.any { key ->
                    registroNormalizado == key ||
                    registroNormalizado.contains(key) ||
                    key.contains(registroNormalizado)
                }
                when (currentState.selectedTab) {
                    ApplicationType.PULVERIZACION ->
                        // Mostrar productos que NO estén marcados como de esparcido por registro
                        // y que sean de pulverización o ambos
                        (product.applicationType == ApplicationType.PULVERIZACION.name || product.applicationType == ApplicationType.AMBOS.name) &&
                        !esDeEsparcidoPorRegistro
                    ApplicationType.ESPARCIDO ->
                        // Mostrar productos que sí estén en la lista de registros de esparcido
                        // o que tengan el tipo de aplicación esparcido explícitamente
                        esDeEsparcidoPorRegistro || product.applicationType == ApplicationType.ESPARCIDO.name
                    ApplicationType.AMBOS -> true
                }
            }
        } else {
            val q = currentState.searchQuery.trim()
            val qNorm = normalizarTexto(q)
            val qReg = normalizarRegistro(q)
            val textMatches = currentState.allProducts.filter { p ->
                val nombre = normalizarTexto(p.nombreComercial)
                val activo = normalizarTexto(p.principioActivo)
                val tipo = normalizarTexto(p.tipo)
                val reg = normalizarRegistro(p.numeroRegistroSenasa)
                nombre.contains(qNorm) || activo.contains(qNorm) || tipo.contains(qNorm) || (qReg.isNotEmpty() && reg.contains(qReg))
            }
            val results = textMatches.filter { product ->
                val registroNormalizado = normalizarRegistro(product.numeroRegistroSenasa)
                val esDeEsparcidoPorRegistro = esparcidoRegistrosNormalized.any { key ->
                    registroNormalizado == key ||
                    registroNormalizado.contains(key) ||
                    key.contains(registroNormalizado)
                }
                when (currentState.selectedTab) {
                    ApplicationType.PULVERIZACION -> !esDeEsparcidoPorRegistro
                    ApplicationType.ESPARCIDO -> esDeEsparcidoPorRegistro || product.applicationType == ApplicationType.ESPARCIDO.name
                    ApplicationType.AMBOS -> true
                }
            }
            val finalResults = if (results.isEmpty() && textMatches.isNotEmpty()) {
                println("[Busqueda] fallback sin tab: q='${currentState.searchQuery}' matches=${textMatches.size}")
                textMatches
            } else results
            println("[Busqueda] q='${currentState.searchQuery}' resultados=${finalResults.size} tab=${currentState.selectedTab}")
            _uiState.update { it.copy(filteredProducts = finalResults) }
            return
        }

        // Logs de diagnóstico mínimos
        if (currentState.searchQuery.isBlank()) {
            val total = currentState.allProducts.size
            val posiblesEsparcido = currentState.allProducts.count { p ->
                val rn = normalizarRegistro(p.numeroRegistroSenasa)
                esparcidoRegistrosNormalized.any { key -> rn == key || rn.contains(key) || key.contains(rn) }
            }
            val mostrados = filtered.size
            println("[Productos] total=$total, enSetEsparcido=$posiblesEsparcido, mostrados(${currentState.selectedTab})=$mostrados")
        }

        _uiState.update { it.copy(filteredProducts = filtered) }
    }
}