package com.example.allote.ui.survey

import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.example.allote.data.AnnotationMedia
import com.example.allote.data.Job
import com.example.allote.data.Lote
import com.example.allote.data.SurveyGeometry

data class AnnotationCategory(
    val id: String,
    val label: String,
    val colorHex: String,
    val icon: String,
    val isDefault: Boolean = true
) {
    val color: Color
        get() = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
            .getOrDefault(Color(0xFF00796B))
}

data class SurveyAnnotationItem(
    val id: Int,
    val category: AnnotationCategory,
    val title: String?,
    val description: String?,
    val geometry: SurveyGeometry,
    val isCritical: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val media: List<AnnotationMedia> = emptyList()
)

data class AnnotationDraft(
    val category: AnnotationCategory,
    val geometry: SurveyGeometry,
    val existingAnnotationId: Int? = null,
    val initialTitle: String? = null,
    val initialDescription: String? = null,
    val initialIsCritical: Boolean = false
)

enum class BaseLayer {
    SATELLITE,
    SKETCH;

    companion object {
        fun from(value: String?): BaseLayer {
            return when (value?.uppercase()) {
                "SKETCH" -> SKETCH
                else -> SATELLITE
            }
        }
    }

    fun toStorage(): String = name
}

enum class SurveyTool {
    SELECT,
    ADD_MARKER,
    DRAW
}

enum class SketchTool {
    FREEHAND,
    LINE,
    RECTANGLE,
    CIRCLE,
    ARROW,
    PAN
}

data class BoundaryPoint(val latitude: Double, val longitude: Double)

data class ShareRequest(
    val uri: Uri,
    val fileName: String,
    val mimeType: String = "application/pdf"
)

data class FieldSurveyUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val job: Job? = null,
    val lote: Lote? = null,
    val surveyId: Int? = null,
    val baseLayer: BaseLayer = BaseLayer.SATELLITE,
    val boundary: List<BoundaryPoint> = emptyList(),
    val categories: List<AnnotationCategory> = emptyList(),
    val activeCategoryId: String? = null,
    val annotations: List<SurveyAnnotationItem> = emptyList(),
    val pendingDraft: AnnotationDraft? = null,
    val showAnnotationDialog: Boolean = false,
    val showCustomCategoryDialog: Boolean = false,
    val showBoundaryDialog: Boolean = false,
    val activeTool: SurveyTool = SurveyTool.SELECT,
    val activeSketchTool: SketchTool = SketchTool.FREEHAND,
    val canUndoSketch: Boolean = false,
    val shareRequest: ShareRequest? = null,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
) {
    fun activeCategory(): AnnotationCategory? = categories.firstOrNull { it.id == activeCategoryId }

    val mapAnnotations: List<SurveyAnnotationItem>
        get() = annotations.filter { it.geometry is SurveyGeometry.MapPoint || it.geometry is SurveyGeometry.MapPolyline || it.geometry is SurveyGeometry.MapPolygon }

    val sketchAnnotations: List<SurveyAnnotationItem>
        get() = annotations.filter { it.geometry is SurveyGeometry.SketchPath || it.geometry is SurveyGeometry.SketchShape }

    val isEditingDraft: Boolean
        get() = pendingDraft?.existingAnnotationId != null
}
