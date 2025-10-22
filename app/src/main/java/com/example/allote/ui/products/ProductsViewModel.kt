package com.example.allote.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.ApplicationType
import com.example.allote.data.Product
import com.example.allote.data.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val repository: ProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private val allProductsFlow = MutableStateFlow<List<Product>>(emptyList())
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedTabFlow = MutableStateFlow(ApplicationType.PULVERIZACION)

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    // Numeros de registro especificos para productos de esparcido (del CSV)
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

    // Conjunto normalizado para comparacion robusta
    private val esparcidoRegistrosNormalized: Set<String> =
        esparcidoRegistrosSet.map { normalizarRegistro(it) }.toSet()

    init {
        repository.getProductsStream()
            .onEach { products ->
                allProductsFlow.value = products
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        allProducts = products
                    )
                }
            }
            .launchIn(viewModelScope)

        repository.getFormulacionesStream()
            .onEach { formulations ->
                _uiState.update { it.copy(formulaciones = formulations) }
            }
            .launchIn(viewModelScope)

        combine(
            allProductsFlow,
            selectedTabFlow,
            searchQueryFlow
                .onStart { emit(searchQueryFlow.value) }
                .debounce(250)
                .distinctUntilChanged()
        ) { products, tab, query ->
            Triple(products, tab, query)
        }
            .map { (products, tab, query) ->
                filterProducts(products, query, tab)
            }
            .flowOn(Dispatchers.Default)
            .onEach { filtered ->
                _uiState.update { it.copy(filteredProducts = filtered) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            repository.importVademecumIfNeeded()
            updateVademecumCount()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun onTabSelected(tab: ApplicationType) {
        _uiState.update { it.copy(selectedTab = tab) }
        selectedTabFlow.value = tab
    }

    fun saveProduct(product: Product) = viewModelScope.launch {
        repository.saveProduct(product)
    }

    fun onClearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "") }
        searchQueryFlow.value = ""
    }

    fun deleteProduct(product: Product) = viewModelScope.launch {
        repository.deleteProduct(product)
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                _searchResults.value = repository.searchProductsByName(query)
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun importVademecum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingVademecum = true) }
            try {
                repository.importVademecumIfNeeded()
                updateVademecumCount()
            } finally {
                _uiState.update { it.copy(isImportingVademecum = false) }
            }
        }
    }

    private suspend fun updateVademecumCount() {
        // val count = repository.getVademecumProductsCount()
        // _uiState.update { it.copy(vademecumProductsCount = count) }
    }

    private fun filterProducts(
        products: List<Product>,
        queryRaw: String,
        tab: ApplicationType
    ): List<Product> {
        val query = queryRaw.trim()
        if (query.isBlank()) {
            val filtered = products.filter { product ->
                val registroNormalizado = normalizarRegistro(product.numeroRegistroSenasa)
                val esDeEsparcidoPorRegistro = esparcidoRegistrosNormalized.any { key ->
                    registroNormalizado == key ||
                        registroNormalizado.contains(key) ||
                        key.contains(registroNormalizado)
                }
                when (tab) {
                    ApplicationType.PULVERIZACION ->
                        (product.applicationType == ApplicationType.PULVERIZACION.name ||
                            product.applicationType == ApplicationType.AMBOS.name) &&
                            !esDeEsparcidoPorRegistro

                    ApplicationType.ESPARCIDO ->
                        esDeEsparcidoPorRegistro || product.applicationType == ApplicationType.ESPARCIDO.name

                    ApplicationType.AMBOS -> true
                }
            }
            val total = products.size
            val posiblesEsparcido = products.count { p ->
                val registro = normalizarRegistro(p.numeroRegistroSenasa)
                esparcidoRegistrosNormalized.any { key ->
                    registro == key || registro.contains(key) || key.contains(registro)
                }
            }
            println("[Productos] total=$total, enSetEsparcido=$posiblesEsparcido, mostrados($tab)=${filtered.size}")
            return filtered
        }

        val queryNormalizado = normalizarTexto(query)
        val queryRegistro = normalizarRegistro(query)

        val textMatches = products.filter { product ->
            val nombre = normalizarTexto(product.nombreComercial)
            val activo = normalizarTexto(product.principioActivo)
            val tipo = normalizarTexto(product.tipo)
            val registro = normalizarRegistro(product.numeroRegistroSenasa)
            nombre.contains(queryNormalizado) ||
                activo.contains(queryNormalizado) ||
                tipo.contains(queryNormalizado) ||
                (queryRegistro.isNotEmpty() && registro.contains(queryRegistro))
        }

        val filtered = textMatches.filter { product ->
            val registroNormalizado = normalizarRegistro(product.numeroRegistroSenasa)
            val esDeEsparcidoPorRegistro = esparcidoRegistrosNormalized.any { key ->
                registroNormalizado == key ||
                    registroNormalizado.contains(key) ||
                    key.contains(registroNormalizado)
            }
            when (tab) {
                ApplicationType.PULVERIZACION -> !esDeEsparcidoPorRegistro
                ApplicationType.ESPARCIDO ->
                    esDeEsparcidoPorRegistro || product.applicationType == ApplicationType.ESPARCIDO.name
                ApplicationType.AMBOS -> true
            }
        }

        val finalResults = if (filtered.isEmpty() && textMatches.isNotEmpty()) {
            println("[Busqueda] fallback sin tab: q='$query' matches=${textMatches.size}")
            textMatches
        } else {
            filtered
        }
        println("[Busqueda] q='$query' resultados=${finalResults.size} tab=$tab")
        return finalResults
    }

    private fun normalizarRegistro(registro: String?): String =
        registro
            ?.lowercase()
            ?.replace("[^a-z0-9]".toRegex(), "")
            ?.trim()
            ?: ""

    private fun normalizarTexto(texto: String?): String =
        texto?.let {
            val normalized = Normalizer.normalize(it, Normalizer.Form.NFD)
            normalized.replace("\\p{M}+".toRegex(), "")
                .lowercase()
                .replace("[^a-z0-9\\s]".toRegex(), "")
                .replace("\\s+".toRegex(), " ")
                .trim()
        } ?: ""
}
