package com.example.allote.ui.survey

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.FieldSurvey
import com.example.allote.data.FieldSurveyRepository
import com.example.allote.data.FieldSurveyWithAnnotations
import com.example.allote.data.Job
import com.example.allote.data.Lote
import com.example.allote.data.SurveyAnnotation
import com.example.allote.data.SurveyGeometry
import com.example.allote.ui.AppDestinations
import com.example.allote.ui.survey.export.FieldSurveyExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class FieldSurveyViewModel @Inject constructor(
    private val repository: FieldSurveyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: Int = savedStateHandle[AppDestinations.JOB_ID_ARG] ?: 0
    private val loteId: Int? = savedStateHandle.get<Int?>(AppDestinations.LOTE_ID_ARG)?.takeIf { it != 0 }

    private val _uiState = MutableStateFlow(FieldSurveyUiState())
    val uiState: StateFlow<FieldSurveyUiState> = _uiState.asStateFlow()

    private var observeJob: CoroutineJob? = null
    private var currentSurvey: FieldSurvey? = null
    private var currentJob: Job? = null
    private var currentLote: Lote? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                currentJob = repository.getJob(jobId)
                currentLote = loteId?.let { repository.getLote(it) }
                val survey = repository.ensureSurvey(jobId, loteId)
                currentSurvey = survey
                startObservingSurvey(survey.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Error al cargar datos: ${e.message}")
                }
            }
        }
    }

    private fun startObservingSurvey(surveyId: Int) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeSurveyWithAnnotations(surveyId).collectLatest { snapshot ->
                if (snapshot != null) {
                    currentSurvey = snapshot.survey
                    val categories = buildCategories(snapshot)
                    val items = snapshot.annotations.mapNotNull { annotation ->
                        val geometry = SurveyGeometry.fromJson(annotation.annotation.geometryPayload)
                        val category = categories.firstOrNull { it.id == annotation.annotation.category }
                        if (geometry != null && category != null) {
                            SurveyAnnotationItem(
                                id = annotation.annotation.id,
                                category = category,
                                title = annotation.annotation.title,
                                description = annotation.annotation.description,
                                geometry = geometry,
                                isCritical = annotation.annotation.isCritical,
                                createdAt = annotation.annotation.createdAt,
                                updatedAt = annotation.annotation.updatedAt,
                                media = annotation.media
                            )
                        } else {
                            null
                        }
                    }.sortedWith(compareByDescending<SurveyAnnotationItem> { it.isCritical }.thenByDescending { it.updatedAt })

                    _uiState.update { prev ->
                        prev.copy(
                            isLoading = false,
                            surveyId = snapshot.survey.id,
                            job = currentJob,
                            lote = currentLote,
                            baseLayer = BaseLayer.from(snapshot.survey.baseLayer),
                            boundary = parseBoundary(snapshot.survey.boundaryGeoJson),
                            categories = categories,
                            annotations = items,
                            canUndoSketch = items.any { it.geometry is SurveyGeometry.SketchPath || it.geometry is SurveyGeometry.SketchShape },
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { prev ->
                        prev.copy(
                            isLoading = false,
                            surveyId = surveyId,
                            job = currentJob,
                            lote = currentLote,
                            annotations = emptyList(),
                            canUndoSketch = false
                        )
                    }
                }
            }
        }
    }

    private fun buildCategories(snapshot: FieldSurveyWithAnnotations): List<AnnotationCategory> {
        val custom = FieldSurveyDefaults.parseCustomCategories(snapshot.survey.customCategoriesJson)
        return FieldSurveyDefaults.defaultCategories + custom
    }

    private fun parseBoundary(json: String?): List<BoundaryPoint> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(BoundaryPoint(obj.getDouble("lat"), obj.getDouble("lng")))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun onBaseLayerChanged(layer: BaseLayer) {
        val survey = currentSurvey ?: return
        if (survey.baseLayer.equals(layer.toStorage(), ignoreCase = true)) return
        viewModelScope.launch {
            try {
                repository.updateSurveyBaseLayer(survey.id, layer.toStorage())
                _uiState.update { state ->
                    val tool = when {
                        state.activeCategoryId != null && layer == BaseLayer.SATELLITE -> SurveyTool.ADD_MARKER
                        state.activeCategoryId != null && layer == BaseLayer.SKETCH -> SurveyTool.DRAW
                        else -> SurveyTool.SELECT
                    }
                    state.copy(baseLayer = layer, activeTool = tool)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo actualizar la capa: ${e.message}") }
            }
        }
    }

    fun onToggleCategory(categoryId: String) {
        _uiState.update { state ->
            val newId = if (state.activeCategoryId == categoryId) null else categoryId
            val newTool = when {
                newId == null -> SurveyTool.SELECT
                state.baseLayer == BaseLayer.SATELLITE -> SurveyTool.ADD_MARKER
                else -> SurveyTool.DRAW
            }
            state.copy(
                activeCategoryId = newId,
                activeTool = newTool
            )
        }
    }

    fun onSelectTool(tool: SurveyTool) {
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun onSketchToolSelected(tool: SketchTool) {
        _uiState.update { it.copy(activeSketchTool = tool) }
    }

    fun onCancelAnnotationDraft() {
        _uiState.update { it.copy(pendingDraft = null, showAnnotationDialog = false) }
    }

    fun onMapLocationPicked(lat: Double, lng: Double) {
        val category = _uiState.value.activeCategory() ?: return
        val geometry = SurveyGeometry.MapPoint(lat, lng)
        _uiState.update {
            it.copy(
                pendingDraft = AnnotationDraft(category, geometry),
                showAnnotationDialog = true
            )
        }
    }

    fun onMapShapeFinished(points: List<Pair<Double, Double>>, tool: SketchTool) {
        val category = _uiState.value.activeCategory() ?: return
        if (points.size < 2) return
        val geometry = when (tool) {
            SketchTool.FREEHAND -> SurveyGeometry.MapPolyline(points)
            SketchTool.LINE -> SurveyGeometry.MapPolyline(points.take(2))
            SketchTool.ARROW -> SurveyGeometry.MapPolyline(points)
            SketchTool.RECTANGLE, SketchTool.CIRCLE -> SurveyGeometry.MapPolygon(points)
            SketchTool.PAN -> null
        } ?: return

        _uiState.update {
            it.copy(
                pendingDraft = AnnotationDraft(category, geometry),
                showAnnotationDialog = true
            )
        }
    }

    fun onSketchShapeFinished(points: List<Pair<Float, Float>>, tool: SketchTool) {
        val category = _uiState.value.activeCategory() ?: return
        if (points.isEmpty()) return
        val geometry = when (tool) {
            SketchTool.FREEHAND -> if (points.size >= 2) SurveyGeometry.SketchPath(points) else null
            SketchTool.LINE -> if (points.size >= 2) SurveyGeometry.SketchShape("line", listOf(points.first(), points.last())) else null
            SketchTool.ARROW -> if (points.size >= 2) SurveyGeometry.SketchShape("arrow", listOf(points.first(), points.last())) else null
            SketchTool.RECTANGLE -> if (points.size >= 2) SurveyGeometry.SketchShape("rectangle", listOf(points.first(), points.last())) else null
            SketchTool.CIRCLE -> if (points.size >= 2) SurveyGeometry.SketchShape("circle", listOf(points.first(), points.last())) else null
            SketchTool.PAN -> null
        } ?: return

        _uiState.update {
            it.copy(
                pendingDraft = AnnotationDraft(category, geometry),
                showAnnotationDialog = true
            )
        }
    }

    fun onSubmitAnnotation(title: String, description: String, isCritical: Boolean) {
        val draft = _uiState.value.pendingDraft ?: return
        val survey = currentSurvey ?: return
        val trimmedTitle = title.trim().ifBlank { null }
        val trimmedDescription = description.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                if (draft.existingAnnotationId != null) {
                    repository.getAnnotationById(draft.existingAnnotationId)?.let { existing ->
                        repository.updateAnnotation(
                            existing.copy(
                                title = trimmedTitle,
                                description = trimmedDescription,
                                isCritical = isCritical
                            )
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            pendingDraft = null,
                            showAnnotationDialog = false,
                            snackbarMessage = "Anotación actualizada"
                        )
                    }
                } else {
                    val geometry = draft.geometry
                    repository.addAnnotation(
                        SurveyAnnotation(
                            id = 0,
                            surveyId = survey.id,
                            category = draft.category.id,
                            title = trimmedTitle,
                            description = trimmedDescription,
                            geometryType = geometry.type,
                            geometryPayload = geometry.toJson(),
                            colorHex = draft.category.colorHex,
                            icon = draft.category.icon,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            sortOrder = calculateNextSortOrder(draft.category.id),
                            isCritical = isCritical,
                            metadataJson = null
                        )
                    )
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            pendingDraft = null,
                            showAnnotationDialog = false,
                            snackbarMessage = "Anotación guardada"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Error al guardar anotación: ${e.message}"
                    )
                }
            }
        }
    }

    private fun calculateNextSortOrder(categoryId: String): Int {
        val sameCategory = _uiState.value.annotations.filter { it.category.id == categoryId }
        return sameCategory.size
    }

    fun onDeleteAnnotation(annotationId: Int) {
        val survey = currentSurvey ?: return
        viewModelScope.launch {
            try {
                repository.deleteAnnotationById(annotationId)
                repository.updateSurvey(survey.copy(updatedAt = System.currentTimeMillis()))
                _uiState.update { it.copy(snackbarMessage = "Anotación eliminada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo eliminar: ${e.message}") }
            }
        }
    }

    fun onUndoSketch() {
        val last = _uiState.value.annotations.firstOrNull {
            it.geometry is SurveyGeometry.SketchPath || it.geometry is SurveyGeometry.SketchShape
        } ?: return
        onDeleteAnnotation(last.id)
    }

    fun onSnackbarConsumed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onErrorConsumed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onAddCustomCategory(label: String, colorHex: String) {
        val survey = currentSurvey ?: return
        val custom = FieldSurveyDefaults.parseCustomCategories(survey.customCategoriesJson).toMutableList()
        val id = "custom-${System.currentTimeMillis()}"
        custom.add(
            AnnotationCategory(
                id = id,
                label = label,
                colorHex = colorHex,
                icon = "custom",
                isDefault = false
            )
        )
        val json = FieldSurveyDefaults.serializeCustomCategories(custom)
        viewModelScope.launch {
            try {
                repository.updateSurveyCustomCategories(survey.id, json)
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Categoría \"$label\" creada",
                        activeCategoryId = id
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo crear categoría: ${e.message}") }
            }
        }
    }

    fun onBoundaryDefined(points: List<BoundaryPoint>) {
        val survey = currentSurvey ?: return
        val json = JSONArray().apply {
            points.forEach { point ->
                put(
                    JSONObject().apply {
                        put("lat", point.latitude)
                        put("lng", point.longitude)
                    }
                )
            }
        }.toString()
        viewModelScope.launch {
            try {
                repository.updateSurveyBoundary(survey.id, json)
                _uiState.update { it.copy(snackbarMessage = "Perímetro actualizado", showBoundaryDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo guardar perímetro: ${e.message}") }
            }
        }
    }

    fun onBoundaryCleared() {
        val survey = currentSurvey ?: return
        viewModelScope.launch {
            try {
                repository.updateSurveyBoundary(survey.id, null)
                _uiState.update { it.copy(snackbarMessage = "Perímetro eliminado", showBoundaryDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo limpiar el perímetro: ${e.message}") }
            }
        }
    }

    fun onShowBoundaryDialog(show: Boolean) {
        _uiState.update { it.copy(showBoundaryDialog = show) }
    }

    fun onShowCustomCategoryDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomCategoryDialog = show) }
    }

    fun startEditingAnnotation(annotationId: Int) {
        val annotation = _uiState.value.annotations.firstOrNull { it.id == annotationId } ?: return
        val draft = AnnotationDraft(
            category = annotation.category,
            geometry = annotation.geometry,
            existingAnnotationId = annotation.id,
            initialTitle = annotation.title,
            initialDescription = annotation.description,
            initialIsCritical = annotation.isCritical
        )
        _uiState.update {
            it.copy(
                pendingDraft = draft,
                showAnnotationDialog = true
            )
        }
    }

    fun onShowAnnotationDialog(show: Boolean) {
        _uiState.update { it.copy(showAnnotationDialog = show) }
    }

    fun onAddMedia(annotationId: Int, uri: Uri, mimeType: String) {
        viewModelScope.launch {
            try {
                repository.addMedia(
                    annotationId,
                    com.example.allote.data.AnnotationMedia(
                        id = 0,
                        annotationId = annotationId,
                        uri = uri.toString(),
                        type = mimeType,
                        description = null,
                        createdAt = System.currentTimeMillis(),
                        isUploaded = false
                    )
                )
                _uiState.update { it.copy(snackbarMessage = "Foto agregada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo adjuntar la foto: ${e.message}") }
            }
        }
    }

    fun onRemoveMedia(mediaId: Int) {
        viewModelScope.launch {
            try {
                repository.removeMedia(mediaId)
                _uiState.update { it.copy(snackbarMessage = "Adjunto eliminado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo eliminar el adjunto: ${e.message}") }
            }
        }
    }

    fun exportSurvey(context: Context) {
        val surveyId = currentSurvey?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val snapshot = repository.getSurveySnapshot(surveyId)
                if (snapshot == null) {
                    _uiState.update { it.copy(isExporting = false, errorMessage = "No se pudo obtener el relevamiento") }
                    return@launch
                }
                val uri = withContext(Dispatchers.IO) {
                    FieldSurveyExporter.exportToPdf(context, snapshot, currentJob, currentLote)
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        shareRequest = ShareRequest(
                            uri = uri,
                            fileName = "relevamiento_${snapshot.survey.id}.pdf"
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "No se pudo generar el PDF: ${e.message}"
                    )
                }
            }
        }
    }

    fun onShareConsumed() {
        _uiState.update { it.copy(shareRequest = null) }
    }
}
