package com.example.allote.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

// --- NUEVAS ESTRUCTURAS DE DATOS PARA EL RESUMEN ---
data class ProductSurplusInfo(
    val productName: String,
    val unidad: String,
    val cantidadPlanificada: Double,
    val cantidadUtilizada: Double,
    val cantidadSobrante: Double
)

data class SurplusSummary(
    val totalHectareasPlanificadas: Double,
    val totalHectareasReales: Double,
    val productSummaries: List<ProductSurplusInfo>
)


class GestionLotesRepository(
    private val loteDao: LoteDao,
    private val jobDao: JobDao,
    private val recipeDao: RecipeDao,
    private val productDao: ProductDao,
    private val formulacionDao: FormulacionDao
) {
    fun getLotesStream(jobId: Int): Flow<List<Lote>> = loteDao.getByJobIdStream(jobId)

    fun getJobStream(jobId: Int): Flow<Job?> = jobDao.getByIdStream(jobId)

    suspend fun saveLote(lote: Lote) {
        loteDao.insert(lote)
    }

    suspend fun updateLote(lote: Lote) {
        loteDao.update(lote)
    }

    suspend fun deleteLote(lote: Lote) {
        loteDao.delete(lote)
    }

    // --- NUEVA FUNCIÓN PARA GENERAR EL RESUMEN DE SOBRANTES (CORREGIDA) ---
    suspend fun generateSurplusSummary(jobId: Int): SurplusSummary? {
        val job = jobDao.getById(jobId) ?: return null
        val recipe = recipeDao.getRecipeByJobId(jobId) ?: return null
        val lotes = loteDao.getByJobId(jobId)
        if (lotes.isEmpty()) return null

        val recipeProducts = recipeDao.getRecipeProductsByRecipeId(recipe.id)
        val allProducts = productDao.getAllStream().first().associateBy { it.id }
        val formulaciones = formulacionDao.getAllStream().first().associateBy { it.id }

        val isSolidApplication = job.tipoAplicacion.equals("Aplicacion solida", ignoreCase = true)

        val totalHectareasPlanificadas = lotes.sumOf { it.hectareas }
        val totalHectareasReales = lotes.sumOf { it.hectareasReales ?: 0.0 }

        val productSummaries = recipeProducts.mapNotNull { rp ->
            val product = allProducts[rp.productId] ?: return@mapNotNull null
            val formulacion = formulaciones[product.formulacionId]
            val isSolid = formulacion?.tipoUnidad.equals("SOLIDO", true)

            val unidad = if (isSolidApplication || isSolid) "Kg" else "L"

            var cantidadPlanificada = rp.dosis * totalHectareasPlanificadas
            var cantidadUtilizada = rp.dosis * totalHectareasReales
            
            if (isSolid) {
                cantidadPlanificada /= 1000
                cantidadUtilizada /= 1000
            }

            val cantidadSobrante = cantidadPlanificada - cantidadUtilizada

            ProductSurplusInfo(
                productName = product.nombreComercial,
                unidad = unidad,
                cantidadPlanificada = cantidadPlanificada,
                cantidadUtilizada = cantidadUtilizada,
                cantidadSobrante = cantidadSobrante
            )
        }

        return SurplusSummary(
            totalHectareasPlanificadas = totalHectareasPlanificadas,
            totalHectareasReales = totalHectareasReales,
            productSummaries = productSummaries
        )
    }


    // --- LÓGICA PRINCIPAL ACTUALIZADA ---
    suspend fun calculateRecipeSummaryForLote(jobId: Int, lote: Lote): String {
        val job = jobDao.getById(jobId)
        val recipe = recipeDao.getRecipeByJobId(jobId)

        if (job == null || recipe == null) {
            return "No hay una receta calculada para este trabajo."
        }

        val recipeProducts = recipeDao.getRecipeProductsByRecipeId(recipe.id)
        val allProducts = productDao.getAllStream().first()
        val formulaciones = formulacionDao.getAllStream().first()
        val isSolidApplication = job.tipoAplicacion.equals("Aplicacion solida", ignoreCase = true)

        val resumenPlanificado = buildRecipeString(
            loteNombre = lote.nombre,
            hectareas = lote.hectareas, // Usa las hectáreas planificadas
            job = job,
            recipe = recipe,
            recipeProducts = recipeProducts,
            allProducts = allProducts,
            formulaciones = formulaciones,
            isSolidApplication = isSolidApplication,
            isReal = false
        )

        // Si hay hectáreas reales, añade la sección de comparación
        if (lote.hectareasReales != null) {
            val resumenReal = buildRecipeString(
                loteNombre = lote.nombre,
                hectareas = lote.hectareasReales, // Usa las hectáreas reales
                job = job,
                recipe = recipe,
                recipeProducts = recipeProducts,
                allProducts = allProducts,
                formulaciones = formulaciones,
                isSolidApplication = isSolidApplication,
                isReal = true
            )
            return "$resumenPlanificado\n\n$resumenReal"
        }

        // Si no, devuelve solo el planificado
        return resumenPlanificado
    }

    // --- NUEVA FUNCIÓN AUXILIAR REUTILIZABLE ---
    private fun buildRecipeString(
        loteNombre: String,
        hectareas: Double,
        job: Job,
        recipe: Recipe,
        recipeProducts: List<RecipeProduct>,
        allProducts: List<Product>,
        formulaciones: List<Formulacion>,
        isSolidApplication: Boolean,
        isReal: Boolean
    ): String {
        val titulo = if (isReal) "📊 CONSUMO REAL" else "📋 PLANIFICADO"
        val subtitulo = if (isReal) "Para %.2f ha trabajadas".format(hectareas) else "Para %.2f ha planificadas".format(hectareas)
        val resumen = StringBuilder()
        resumen.append("$titulo ($subtitulo)\n")

        if (isSolidApplication) {
            // Lógica para aplicaciones sólidas...
            val kilosPorTachada = recipe.caldoPorTachada
            var totalKilos = 0.0
            val productosCalculados = recipeProducts.map { rp ->
                val cantidadTotal = rp.dosis * hectareas
                totalKilos += cantidadTotal
                rp to cantidadTotal
            }

            resumen.append("Total a aplicar: %.2f Kg\n".format(totalKilos))

            if (kilosPorTachada <= 0 || totalKilos <= kilosPorTachada) {
                resumen.append("  📦 Carga única\n")
                productosCalculados.forEach { (rp, cantidad) ->
                    val productName = allProducts.find { it.id == rp.productId }?.nombreComercial ?: "N/A"
                    resumen.append("  • %s: %.2f Kg\n".format(productName, cantidad))
                }
            } else {
                val numCargasCompletas = (totalKilos / kilosPorTachada).toInt()
                val cargaFinal = totalKilos % kilosPorTachada
                val proporcionCargaCompleta = kilosPorTachada / totalKilos
                val proporcionCargaFinal = cargaFinal / totalKilos

                if (numCargasCompletas > 0) {
                    resumen.append("\n📦 Cargas 1 a $numCargasCompletas (%.2f Kg c/u)\n".format(kilosPorTachada))
                    productosCalculados.forEach { item ->
                        val rp = item.first
                        val cantidadTotal = item.second
                        val productName = allProducts.find { it.id == rp.productId }?.nombreComercial ?: "N/A"
                        val dosisCarga = cantidadTotal * proporcionCargaCompleta
                        resumen.append("• %s: %.2f Kg\n".format(productName, dosisCarga))
                    }
                }

                if (cargaFinal > 0.01) {
                    resumen.append("\n📦 Carga ${numCargasCompletas + 1} (%.2f Kg)\n".format(cargaFinal))
                    productosCalculados.forEach { item ->
                        val rp = item.first
                        val cantidadTotal = item.second
                        val productName = allProducts.find { it.id == rp.productId }?.nombreComercial ?: "N/A"
                        val dosisCarga = cantidadTotal * proporcionCargaFinal
                        resumen.append("• %s: %.2f Kg\n".format(productName, dosisCarga))
                    }
                }
            }

        } else { // Liquid Application
            val caudal = recipe.caudal
            val totalCaldo = caudal * hectareas
            var totalProductosLiquidos = 0.0

            val productosCalculados = recipeProducts.map { rp ->
                val product = allProducts.find { it.id == rp.productId }
                val formulacion = formulaciones.find { it.id == product?.formulacionId }
                val tipoUnidad = formulacion?.tipoUnidad ?: "LIQUIDO"
                val cantidadTotal = if (tipoUnidad == "SOLIDO") (rp.dosis * hectareas) / 1000 else rp.dosis * hectareas
                if (tipoUnidad == "LIQUIDO") {
                    totalProductosLiquidos += cantidadTotal
                }
                Triple(rp, cantidadTotal, tipoUnidad)
            }

            val agua = totalCaldo - totalProductosLiquidos
            if (agua < 0) return "Error: La cantidad de productos supera el total del caldo."

            resumen.append("Total Caldo: %.2f L\n".format(totalCaldo))
            resumen.append("Agua Necesaria: %.2f L\n".format(agua))

            productosCalculados.sortedBy { it.first.ordenMezclado }.forEach { (rp, cantidad, tipoUnidad) ->
                val productName = allProducts.find { it.id == rp.productId }?.nombreComercial ?: "N/A"
                val unidad = if (tipoUnidad == "SOLIDO") "kg" else "litros"
                resumen.append("• %s: %.2f %s\n".format(productName, cantidad, unidad))
            }
        }
        return resumen.toString()
    }
}