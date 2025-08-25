package com.example.allote.ui.recetas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.*
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.Normalizer

data class ProductoRecetaItem(
    val id: Int,
    val productId: Int,
    val nombreComercial: String,
    val dosis: Double,
    val unidadDosis: String, // "L/ha", "Cc/ha", "Gr/ha", "Kg/ha"
    var cantidadTotal: Double,
    val ordenMezclado: Int,
    val bandaToxicologica: String?,
    val tipoUnidad: String // "SOLIDO" o "LIQUIDO"
)

// RecetasUiState ahora es una clase de datos pura, sin lógica de negocio.
data class RecetasUiState(
    val isLoading: Boolean = true,
    val job: Job? = null,
    val receta: Recipe? = null,
    val productosEnReceta: List<ProductoRecetaItem> = emptyList(),
    val hectareasText: String = "",
    val caudalText: String = "",
    val caldoPorTachadaText: String = "",
    val resumenActual: String? = null,
    val allFormulations: List<Formulacion> = emptyList(),
    val availableProducts: List<Product> = emptyList(),
    val productSearchQuery: String = "",
    val summaryIsDirty: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecetasViewModel @Inject constructor(
    private val repository: RecetasRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: Int = savedStateHandle[AppDestinations.JOB_ID_ARG] ?: 0

    private val _uiState = MutableStateFlow(RecetasUiState())
    val uiState: StateFlow<RecetasUiState> = _uiState.asStateFlow()

    // --- LÓGICA DE NEGOCIO Y PROPIEDADES COMPUTADAS ---
    // La lógica que antes estaba en la data class ahora vive aquí.

    val isSolidApplication: StateFlow<Boolean> = uiState
        .map { it.job?.tipoAplicacion.equals("Aplicacion solida", ignoreCase = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val filteredAvailableProducts: StateFlow<List<Product>> = uiState
        .map { state ->
            // availableProducts ya está filtrado por tipo de aplicación del trabajo en init{}
            val base = state.availableProducts
            val q = state.productSearchQuery.trim().lowercase()
            val qAscii = Normalizer.normalize(q, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            val qNorm = qAscii.replace("[^a-z0-9]".toRegex(), "")
            val filtered = if (q.isBlank()) {
                base
            } else {
                base.filter {
                    val nombre = it.nombreComercial.lowercase()
                    val nombreAscii = Normalizer.normalize(nombre, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    val nombreNorm = nombreAscii.replace("[^a-z0-9]".toRegex(), "")
                    val pa = it.principioActivo?.lowercase()
                    val paAscii = pa?.let { s -> Normalizer.normalize(s, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "") }
                    val paNorm = paAscii?.replace("[^a-z0-9]".toRegex(), "")
                    val reg = it.numeroRegistroSenasa?.lowercase()
                    val regAscii = reg?.let { s -> Normalizer.normalize(s, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "") }
                    val regNorm = regAscii?.replace("[^a-z0-9]".toRegex(), "")

                    // Coincidir por nombre/principio activo (texto) o por número de registro (normalizado)
                    nombre.contains(q) || nombreAscii.contains(qAscii) || nombreNorm.contains(qNorm) ||
                            (pa?.contains(q) == true) || (paAscii?.contains(qAscii) == true) || (paNorm?.contains(qNorm) == true) ||
                            (reg?.contains(q) == true) || (regAscii?.contains(qAscii) == true) || (regNorm?.contains(qNorm) == true)
                }
            }
            println("[Recetas] filter base=${base.size}, query=\"$q\" -> result=${filtered.size}")
            filtered
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun isHectareasValid(state: RecetasUiState = _uiState.value): Boolean = state.hectareasText.toDoubleOrNull()?.let { it > 0 } ?: false
    fun isCaudalValid(state: RecetasUiState = _uiState.value): Boolean = isSolidApplication.value || state.caudalText.toDoubleOrNull()?.let { it > 0 } ?: false
    fun isCaldoPorTachadaValid(state: RecetasUiState = _uiState.value): Boolean = state.caldoPorTachadaText.toDoubleOrNull()?.let { it > 0 } ?: true


    init {
        if (jobId != 0) {
            combine(
                repository.getJobStream(jobId),
                repository.getRecipeStream(jobId).flatMapLatest { recipe ->
                    if (recipe == null) flowOf(Pair(null, emptyList()))
                    else repository.getRecipeProductsStream(recipe.id).map { products -> Pair(recipe, products) }
                },
                repository.getAllProductsStream(),
                repository.getFormulacionesStream()
            ) { job, recipeAndProducts, allProducts, allFormulations ->
                val (recipe, recipeProducts) = recipeAndProducts

                val appType = if (job?.tipoAplicacion.equals("Aplicacion solida", true)) ApplicationType.ESPARCIDO else ApplicationType.PULVERIZACION
                val availableProductsForJob = allProducts.filter { it.applicationType == appType.name || it.applicationType == ApplicationType.AMBOS.name }

                val productosRecetaItems = recipeProducts.mapNotNull { rp ->
                    allProducts.find { it.id == rp.productId }?.let { product ->
                        val formulacion = allFormulations.find { f -> f.id == product.formulacionId }
                        val tipoUnidadFormulacion = formulacion?.tipoUnidad ?: "LIQUIDO"
                        val unidadCargada = rp.unidadDosis.ifBlank { if (tipoUnidadFormulacion.equals("SOLIDO", true)) "Gr/ha" else "L/ha" }
                        val tipoUnidad = if (unidadCargada == "Gr/ha" || unidadCargada == "Kg/ha") "SOLIDO" else tipoUnidadFormulacion
                        ProductoRecetaItem(
                            rp.id,
                            rp.productId,
                            product.nombreComercial,
                            rp.dosis,
                            unidadCargada,
                            rp.cantidadTotal,
                            formulacion?.ordenMezcla ?: 99,
                            product.bandaToxicologica,
                            tipoUnidad
                        )
                    }
                }

                _uiState.value.copy(
                    isLoading = false,
                    job = job,
                    receta = recipe,
                    productosEnReceta = productosRecetaItems.sortedBy { it.ordenMezclado },
                    allFormulations = allFormulations,
                    availableProducts = availableProductsForJob,
                    hectareasText = _uiState.value.hectareasText.ifEmpty { recipe?.hectareas?.toString() ?: job?.surface?.toString() ?: "" },
                    caudalText = _uiState.value.caudalText.ifEmpty { recipe?.caudal?.toString() ?: "" },
                    caldoPorTachadaText = _uiState.value.caldoPorTachadaText.ifEmpty { recipe?.caldoPorTachada?.takeIf { it > 0 }?.toString() ?: "" },
                    resumenActual = if (_uiState.value.summaryIsDirty) null else recipe?.resumen,
                    summaryIsDirty = false
                )
            }.onEach { newState ->
                _uiState.value = newState
            }.launchIn(viewModelScope)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onHectareasChange(newValue: String) { _uiState.update { it.copy(hectareasText = newValue, summaryIsDirty = true) } }
    fun onCaudalChange(newValue: String) { _uiState.update { it.copy(caudalText = newValue, summaryIsDirty = true) } }
    fun onCaldoPorTachadaChange(newValue: String) { _uiState.update { it.copy(caldoPorTachadaText = newValue, summaryIsDirty = true) } }

    fun onProductSearchQueryChanged(query: String) {
        println("[Recetas] search query=\"$query\"")
        _uiState.update { it.copy(productSearchQuery = query) }
    }
    fun clearProductSearch() { _uiState.update { it.copy(productSearchQuery = "") } }

    fun agregarProducto(product: Product, dosis: Double, unidad: String) {
        val allFormulations = _uiState.value.allFormulations
        val formulacion = allFormulations.find { it.id == product.formulacionId }
        // Si la unidad seleccionada es Gr/ha o Kg/ha, forzar tipoUnidad a SOLIDO
        val tipoUnidad = if (unidad == "Gr/ha" || unidad == "Kg/ha") "SOLIDO" else (formulacion?.tipoUnidad ?: "LIQUIDO")
        val ordenMezclado = formulacion?.ordenMezcla ?: 99
        val newItem = ProductoRecetaItem(0, product.id, product.nombreComercial, dosis, unidad, 0.0, ordenMezclado, product.bandaToxicologica, tipoUnidad)

        val updatedList = (_uiState.value.productosEnReceta + newItem).sortedBy { it.ordenMezclado }
        _uiState.update { it.copy(productosEnReceta = updatedList, summaryIsDirty = true) }
    }

    fun eliminarProductoDeReceta(productId: Int) {
        val updatedList = _uiState.value.productosEnReceta.filter { it.productId != productId }
        _uiState.update { it.copy(productosEnReceta = updatedList, summaryIsDirty = true) }
    }

    fun eliminarReceta() {
        if (jobId == 0) return
        viewModelScope.launch {
            _uiState.value.receta?.let { repository.deleteFullRecipe(it) }
        }
    }

    fun calcularYGuardarReceta() {
        if (jobId == 0) return
        viewModelScope.launch {
            val state = _uiState.value
            if (!isHectareasValid(state) || !isCaudalValid(state) || !isCaldoPorTachadaValid(state)) return@launch

            val hectareas = state.hectareasText.toDoubleOrNull() ?: 0.0
            val caudal = if (isSolidApplication.value) 0.0 else state.caudalText.toDoubleOrNull() ?: 0.0
            val capacidadTachada = state.caldoPorTachadaText.toDoubleOrNull()

            val (productosCalculados, resumenFinal) = calculateRecipeSummary(
                hectareas, caudal, capacidadTachada, state.productosEnReceta, isSolidApplication.value
            )
            val totalMezcla = if (isSolidApplication.value) productosCalculados.sumOf { it.cantidadTotal } else caudal * hectareas

            repository.saveFullRecipe(jobId, state.receta, hectareas, caudal, capacidadTachada, totalMezcla, resumenFinal, productosCalculados)
        }
    }

    private fun calculateRecipeSummary(
        hectareas: Double,
        caudal: Double,
        capacidadTachada: Double?,
        productosEnReceta: List<ProductoRecetaItem>,
        isSolidApplication: Boolean
    ): Pair<List<ProductoRecetaItem>, String> {
        val resumenBuilder = StringBuilder()
        resumenBuilder.append("🔍 RESUMEN DE LA RECETA\n")

        val productosCalculados = productosEnReceta.map { item ->
            val unidad = item.unidadDosis.trim()
            val cantidadTotal = when (unidad) {
                "Gr/ha" -> (item.dosis * hectareas) / 1000.0 // total en Kg
                "Kg/ha" -> item.dosis * hectareas // total en Kg
                "Cc/ha" -> (item.dosis * hectareas) / 1000.0 // total en Litros (1000 cc = 1 L)
                else /* "L/ha" */ -> item.dosis * hectareas // total en Litros
            }
            item.copy(cantidadTotal = cantidadTotal)
        }

        if (isSolidApplication) {
            val totalKilos = productosCalculados.sumOf { it.cantidadTotal }
            resumenBuilder.append("Total a aplicar: %.2f Kgs\n".format(totalKilos))

            if (capacidadTachada == null || capacidadTachada <= 0 || totalKilos <= capacidadTachada) {
                resumenBuilder.append("📦 Carga única\n")
                productosCalculados.forEach {
                    resumenBuilder.append("• %s: %.2f Kgs\n".format(it.nombreComercial, it.cantidadTotal))
                }
            } else {
                val numCargasCompletas = (totalKilos / capacidadTachada).toInt()
                val cargaFinal = totalKilos % capacidadTachada
                if (numCargasCompletas > 0) {
                    resumenBuilder.append("\n📦 Cargas 1 a $numCargasCompletas (%.2f Kgs c/u)\n".format(capacidadTachada))
                    productosCalculados.forEach { producto ->
                        val dosisCarga = producto.dosis * (capacidadTachada / (productosCalculados.sumOf { it.dosis }))
                        resumenBuilder.append("• %s: %.2f Kgs\n".format(producto.nombreComercial, dosisCarga))
                    }
                }
                if (cargaFinal > 0.01) {
                    resumenBuilder.append("\n📦 Carga ${numCargasCompletas + 1} (%.2f Kgs)\n".format(cargaFinal))
                    productosCalculados.forEach { producto ->
                        val dosisCarga = producto.dosis * (cargaFinal / (productosCalculados.sumOf { it.dosis }))
                        resumenBuilder.append("• %s: %.2f Kgs\n".format(producto.nombreComercial, dosisCarga))
                    }
                }
            }
        } else {
            val totalCaldo = caudal * hectareas
            // Consideramos líquidos los que fueron medidos en L/ha o Cc/ha
            val totalProductosLiquidos = productosCalculados.filter { it.unidadDosis == "L/ha" || it.unidadDosis == "Cc/ha" }.sumOf { it.cantidadTotal }
            val agua = totalCaldo - totalProductosLiquidos

            if (agua < 0) return Pair(productosCalculados, "Error: La cantidad de productos supera el total del caldo.")

            resumenBuilder.append("Total Caldo: %.2f Litros\n".format(totalCaldo))
            resumenBuilder.append("Total Agua: %.2f Litros\n".format(agua))
            resumenBuilder.append("Total Productos: %.2f Litros + %.2f Kgs\n".format(
                totalProductosLiquidos,
                productosCalculados.filter { it.tipoUnidad.equals("SOLIDO", ignoreCase = true) }.sumOf { it.cantidadTotal }
            ))

            if (capacidadTachada == null || capacidadTachada <= 0 || totalCaldo <= capacidadTachada) {
                resumenBuilder.append("💧 Carga única\n")
                resumenBuilder.append("• Agua: %.2f L\n".format(agua))
                productosCalculados.sortedBy { it.ordenMezclado }.forEach {
                    val unidadSalida = if (it.unidadDosis == "Gr/ha" || it.unidadDosis == "Kg/ha") "Kgs" else "Litros"
                    resumenBuilder.append("• %s: %.2f %s\n".format(it.nombreComercial, it.cantidadTotal, unidadSalida))
                }
            } else {
                val numCargasCompletas = (totalCaldo / capacidadTachada).toInt()
                val cargaFinal = totalCaldo % capacidadTachada

                if (numCargasCompletas > 0) {
                    resumenBuilder.append("\n🧪 Cargas 1 a $numCargasCompletas (%.2f Litros c/u)\n".format(capacidadTachada))
                    val aguaPorTachada = agua / totalCaldo * capacidadTachada
                    resumenBuilder.append("💧 Agua: %.2f Litros\n".format(aguaPorTachada))
                    productosCalculados.sortedBy { it.ordenMezclado }.forEach { producto ->
                        val cantidadPorTachada = producto.cantidadTotal / totalCaldo * capacidadTachada
                        val unidadSalida = if (producto.unidadDosis == "Gr/ha" || producto.unidadDosis == "Kg/ha") "Kgs" else "Litros"
                        resumenBuilder.append("• %s: %.2f %s\n".format(producto.nombreComercial, cantidadPorTachada, unidadSalida))
                    }
                }
                if (cargaFinal > 0.01) {
                    resumenBuilder.append("\n🧪 Carga ${numCargasCompletas + 1} (%.2f Litros)\n".format(cargaFinal))
                    val aguaCargaFinal = agua / totalCaldo * cargaFinal
                    resumenBuilder.append("💧 Agua: %.2f Litros\n".format(aguaCargaFinal))
                    productosCalculados.sortedBy { it.ordenMezclado }.forEach { producto ->
                        val cantidadCargaFinal = producto.cantidadTotal / totalCaldo * cargaFinal
                        val unidadSalida = if (producto.unidadDosis == "Gr/ha" || producto.unidadDosis == "Kg/ha") "Kgs" else "Litros"
                        resumenBuilder.append("• %s: %.2f %s\n".format(producto.nombreComercial, cantidadCargaFinal, unidadSalida))
                    }
                }
            }
        }
        return Pair(productosCalculados, resumenBuilder.toString())
    }
}
