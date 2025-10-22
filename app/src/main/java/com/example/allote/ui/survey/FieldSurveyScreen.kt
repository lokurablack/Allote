package com.example.allote.ui.survey

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Point
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.allote.data.SurveyGeometry
import com.example.allote.ui.survey.components.LotBoundaryDialog
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FieldSurveyScreen(
    state: FieldSurveyUiState,
    onBack: () -> Unit,
    onBaseLayerChanged: (BaseLayer) -> Unit,
    onCategorySelected: (String) -> Unit,
    onMapLocationPicked: (Double, Double) -> Unit,
    onMapShapeFinished: (List<Pair<Double, Double>>, SketchTool) -> Unit,
    onSketchShapeFinished: (List<Pair<Float, Float>>, SketchTool) -> Unit,
    onShowBoundaryDialog: (Boolean) -> Unit,
    onBoundaryDefined: (List<BoundaryPoint>) -> Unit,
    onBoundaryCleared: () -> Unit,
    onShowCustomCategoryDialog: (Boolean) -> Unit,
    onAddCustomCategory: (String, String) -> Unit,
    onStartEditingAnnotation: (Int) -> Unit,
    onDeleteAnnotation: (Int) -> Unit,
    onAddMedia: (Int, Uri, String) -> Unit,
    onRemoveMedia: (Int) -> Unit,
    onCancelDraft: () -> Unit,
    onSubmitAnnotation: (String, String, Boolean) -> Unit,
    onSelectTool: (SurveyTool) -> Unit,
    onSketchToolSelected: (SketchTool) -> Unit,
    onUndoSketch: () -> Unit,
    onExportPdf: (Context) -> Unit,
    onSnackbarConsumed: () -> Unit,
    onErrorConsumed: () -> Unit,
    onShareConsumed: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSnackbarConsumed()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorConsumed()
        }
    }

    LaunchedEffect(state.shareRequest) {
        val share = state.shareRequest ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = share.mimeType
            putExtra(Intent.EXTRA_STREAM, share.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir relevamiento"))
        onShareConsumed()
    }

    val cameraTempUri = remember { mutableStateOf<Uri?>(null) }
    var pendingMediaAnnotationId by remember { mutableStateOf<Int?>(null) }
    var showMediaPicker by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val annotationId = pendingMediaAnnotationId
        val uri = cameraTempUri.value
        if (success && annotationId != null && uri != null) {
            onAddMedia(annotationId, uri, "image/jpeg")
        }
        if (!success) {
            uri?.let { context.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        pendingMediaAnnotationId = null
        showMediaPicker = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val annotationId = pendingMediaAnnotationId
        if (annotationId != null && uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/*"
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
            onAddMedia(annotationId, uri, mime)
        }
        pendingMediaAnnotationId = null
        showMediaPicker = false
    }

    var isMapExpanded by rememberSaveable { mutableStateOf(false) }
    var showBoundaryDistances by rememberSaveable { mutableStateOf(true) }
    var isControlsPanelExpanded by rememberSaveable { mutableStateOf(false) }
    var photoLightboxUri by remember { mutableStateOf<String?>(null) }

    // Lógica automática: cambiar herramienta basándose en categoría seleccionada
    LaunchedEffect(state.activeCategoryId, state.baseLayer) {
        if (state.baseLayer == BaseLayer.SATELLITE) {
            if (state.activeCategoryId != null && state.activeTool != SurveyTool.DRAW) {
                // Si hay categoría y no estamos en modo dibujo, activar ADD_MARKER
                onSelectTool(SurveyTool.ADD_MARKER)
            } else if (state.activeCategoryId == null && state.activeTool != SurveyTool.SELECT) {
                // Sin categoría, volver a modo navegación
                onSelectTool(SurveyTool.SELECT)
            }
        }
    }

    LaunchedEffect(state.baseLayer) {
        if (state.baseLayer != BaseLayer.SATELLITE && isMapExpanded) {
            isMapExpanded = false
        }
    }

    LaunchedEffect(state.baseLayer, state.activeTool, state.activeSketchTool) {
        if (
            state.baseLayer == BaseLayer.SATELLITE &&
            state.activeTool == SurveyTool.DRAW &&
            state.activeSketchTool == SketchTool.PAN
        ) {
            onSketchToolSelected(SketchTool.FREEHAND)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Relevamiento", maxLines = 1, fontSize = 18.sp)
                        state.job?.let { job ->
                            val jobLabel = job.description?.takeIf { it.isNotBlank() }
                                ?: job.clientName.takeIf { it.isNotBlank() }
                                ?: "Trabajo #${job.id}"
                            Text(
                                text = jobLabel,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // FAB contextual: Mostrar solo en modo mapa con categoría seleccionada
            if (state.baseLayer == BaseLayer.SATELLITE && state.activeCategoryId != null && !isMapExpanded) {
                if (state.activeTool == SurveyTool.DRAW) {
                    // ExtendedFAB cuando está dibujando (más visible)
                    ExtendedFloatingActionButton(
                        onClick = { onSelectTool(SurveyTool.ADD_MARKER) },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Finalizar dibujo"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // FAB normal para activar dibujo
                    FloatingActionButton(
                        onClick = { onSelectTool(SurveyTool.DRAW) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Dibujar en mapa"
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Panel de controles compacto y colapsable
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header: Vista + Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selector de vista compacto
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.baseLayer == BaseLayer.SATELLITE,
                                onClick = { onBaseLayerChanged(BaseLayer.SATELLITE) },
                                label = { Text("🗺️ Mapa", fontSize = 13.sp) },
                                modifier = Modifier.height(40.dp)
                            )
                            FilterChip(
                                selected = state.baseLayer == BaseLayer.SKETCH,
                                onClick = { onBaseLayerChanged(BaseLayer.SKETCH) },
                                label = { Text("📝 Croquis", fontSize = 13.sp) },
                                modifier = Modifier.height(40.dp)
                            )
                        }

                        // Toggle de expansión
                        IconButton(
                            onClick = { isControlsPanelExpanded = !isControlsPanelExpanded },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isControlsPanelExpanded)
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                else
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isControlsPanelExpanded) "Ocultar opciones" else "Mostrar opciones"
                            )
                        }
                    }

                    // Panel expandido
                    if (isControlsPanelExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Acciones rápidas
                        QuickActionsRow(
                            isExporting = state.isExporting,
                            onShowBoundary = { onShowBoundaryDialog(true) },
                            onShowNewCategory = { onShowCustomCategoryDialog(true) },
                            onExportPdf = { onExportPdf(context) }
                        )

                        // Opciones de visualización de distancias
                        if (state.boundary.isNotEmpty() && state.baseLayer == BaseLayer.SATELLITE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterChip(
                                selected = showBoundaryDistances,
                                onClick = { showBoundaryDistances = !showBoundaryDistances },
                                label = { Text("Mostrar distancias", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (showBoundaryDistances) Icons.Default.Check else Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }

                    // Categorías (siempre visibles)
                    Spacer(modifier = Modifier.height(12.dp))
                    CategorySelector(
                        categories = state.categories,
                        activeCategoryId = state.activeCategoryId,
                        onCategorySelected = onCategorySelected
                    )

                    if (state.activeCategoryId == null &&
                        (state.baseLayer == BaseLayer.SKETCH || state.activeTool != SurveyTool.SELECT)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Selecciona una categoría para comenzar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Herramientas de dibujo
            val showSketchTools = state.baseLayer == BaseLayer.SKETCH ||
                (state.baseLayer == BaseLayer.SATELLITE && state.activeTool == SurveyTool.DRAW)
            if (showSketchTools) {
                val availableTools = if (state.baseLayer == BaseLayer.SATELLITE) {
                    listOf(
                        SketchTool.FREEHAND,
                        SketchTool.LINE,
                        SketchTool.ARROW,
                        SketchTool.RECTANGLE,
                        SketchTool.CIRCLE
                    )
                } else {
                    listOf(
                        SketchTool.FREEHAND,
                        SketchTool.LINE,
                        SketchTool.ARROW,
                        SketchTool.RECTANGLE,
                        SketchTool.CIRCLE,
                        SketchTool.PAN
                    )
                }
                SketchToolSelector(
                    selectedTool = state.activeSketchTool,
                    availableTools = availableTools,
                    onToolSelected = onSketchToolSelected,
                    showUndo = state.baseLayer == BaseLayer.SKETCH,
                    canUndo = state.baseLayer == BaseLayer.SKETCH && state.canUndoSketch,
                    onUndo = onUndoSketch
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Mapa o croquis
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                when (state.baseLayer) {
                    BaseLayer.SATELLITE -> {
                        if (!isMapExpanded) {
                            SurveyMap(
                                state = state,
                                activeSketchTool = state.activeSketchTool,
                                showBoundaryDistances = showBoundaryDistances,
                                onMapLocationPicked = onMapLocationPicked,
                                onMapShapeFinished = onMapShapeFinished
                            )
                        }
                    }
                    BaseLayer.SKETCH -> {
                        SketchCanvas(
                            annotations = state.sketchAnnotations,
                            activeCategory = state.activeCategory(),
                            sketchTool = state.activeSketchTool,
                            onShapeFinished = onSketchShapeFinished
                        )
                    }
                }

                if (state.baseLayer == BaseLayer.SATELLITE) {
                    FilledIconButton(
                        onClick = { isMapExpanded = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Ampliar mapa"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de anotaciones
            AnnotationList(
                annotations = state.annotations,
                onDeleteAnnotation = onDeleteAnnotation,
                onEditAnnotation = onStartEditingAnnotation,
                onAddMedia = { annotationId ->
                    pendingMediaAnnotationId = annotationId
                    showMediaPicker = true
                },
                onRemoveMedia = onRemoveMedia,
                onPhotoClick = { uri -> photoLightboxUri = uri }
            )
        }
    }

    if (isMapExpanded && state.baseLayer == BaseLayer.SATELLITE) {
        ExpandedMapDialog(
            state = state,
            onDismiss = { isMapExpanded = false },
            onMapLocationPicked = onMapLocationPicked,
            onMapShapeFinished = onMapShapeFinished,
            onCategorySelected = onCategorySelected,
            onToolSelected = onSelectTool,
            onSketchToolSelected = onSketchToolSelected
        )
    }

    if (state.showAnnotationDialog) {
        AnnotationDialog(
            category = state.pendingDraft?.category,
            isSaving = state.isSaving,
            isEditing = state.isEditingDraft,
            initialTitle = state.pendingDraft?.initialTitle.orEmpty(),
            initialDescription = state.pendingDraft?.initialDescription.orEmpty(),
            initialIsCritical = state.pendingDraft?.initialIsCritical ?: false,
            onCancel = onCancelDraft,
            onConfirm = onSubmitAnnotation
        )
    }

    if (state.showCustomCategoryDialog) {
        CustomCategoryDialog(
            onCancel = { onShowCustomCategoryDialog(false) },
            onConfirm = { label, color ->
                onAddCustomCategory(label, color)
                onShowCustomCategoryDialog(false)
            }
        )
    }

    if (state.showBoundaryDialog) {
        LotBoundaryDialog(
            initialPoints = state.boundary,
            onConfirm = onBoundaryDefined,
            onDismiss = { onShowBoundaryDialog(false) },
            onClear = onBoundaryCleared
        )
    }

    if (showMediaPicker && pendingMediaAnnotationId != null) {
        MediaSourceDialog(
            onDismiss = {
                showMediaPicker = false
                pendingMediaAnnotationId = null
            },
            onCamera = {
                val annotationId = pendingMediaAnnotationId ?: return@MediaSourceDialog
                val uri = createTempImageUri(context)
                cameraTempUri.value = uri
                cameraLauncher.launch(uri)
            },
            onGallery = {
                galleryLauncher.launch("image/*")
            }
        )
    }

    // Lightbox para ver fotos ampliadas
    photoLightboxUri?.let { uri ->
        PhotoLightbox(
            uri = uri,
            onDismiss = { photoLightboxUri = null }
        )
    }
}

@Composable
private fun QuickActionsRow(
    isExporting: Boolean,
    onShowBoundary: () -> Unit,
    onShowNewCategory: () -> Unit,
    onExportPdf: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AssistChip(
            onClick = onShowBoundary,
            label = { Text("Perímetro", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.weight(1f)
        )
        AssistChip(
            onClick = onShowNewCategory,
            label = { Text("Categoría", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.weight(1f)
        )
        AssistChip(
            enabled = !isExporting,
            onClick = onExportPdf,
            label = {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                } else {
                    Text("PDF", fontSize = 12.sp)
                }
            },
            leadingIcon = if (!isExporting) {
                { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LayerAndToolSelector(
    baseLayer: BaseLayer,
    onLayerChanged: (BaseLayer) -> Unit
) {
    Column {
        Text(
            text = "Vista",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = baseLayer == BaseLayer.SATELLITE,
                onClick = { onLayerChanged(BaseLayer.SATELLITE) },
                label = { Text("Mapa", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Map, null, Modifier.size(16.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
            FilterChip(
                selected = baseLayer == BaseLayer.SKETCH,
                onClick = { onLayerChanged(BaseLayer.SKETCH) },
                label = { Text("Croquis", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Photo, null, Modifier.size(16.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun SketchToolSelector(
    selectedTool: SketchTool,
    availableTools: List<SketchTool>,
    onToolSelected: (SketchTool) -> Unit,
    showUndo: Boolean,
    canUndo: Boolean,
    onUndo: () -> Unit
) {
    Column {
        Text(
            text = "Herramientas",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // LazyRow para scroll horizontal (asegura que se vean todas las herramientas)
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(availableTools) { tool ->
                    FilterChip(
                        selected = selectedTool == tool,
                        onClick = { onToolSelected(tool) },
                        label = { Text(sketchToolLabel(tool), fontSize = 12.sp) },
                        modifier = Modifier.height(42.dp)
                    )
                }
            }
            if (showUndo) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer", Modifier.size(20.dp))
                }
            }
        }
    }
}


@Composable
private fun CategorySelector(
    categories: List<AnnotationCategory>,
    activeCategoryId: String?,
    onCategorySelected: (String) -> Unit
) {
    // Mostrar solo las primeras 4 categorías + un menú "Más" para el resto
    val visibleCategories = categories.take(4)
    val hiddenCategories = categories.drop(4)
    var showMoreMenu by remember { mutableStateOf(false) }

    Column {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(visibleCategories, key = { it.id }) { category ->
                val selected = activeCategoryId == category.id
                FilterChip(
                    selected = selected,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.label, fontSize = 12.sp, maxLines = 1) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(category.color)
                        )
                    },
                    modifier = Modifier.height(44.dp)
                )
            }

            // Botón "Más" si hay categorías ocultas
            if (hiddenCategories.isNotEmpty()) {
                item {
                    Box {
                        FilterChip(
                            selected = hiddenCategories.any { it.id == activeCategoryId },
                            onClick = { showMoreMenu = !showMoreMenu },
                            label = { Text("Más ⋮", fontSize = 12.sp) },
                            modifier = Modifier.height(44.dp)
                        )

                        // Menú dropdown
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            hiddenCategories.forEach { category ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(category.color)
                                            )
                                            Text(category.label, fontSize = 13.sp)
                                        }
                                    },
                                    onClick = {
                                        onCategorySelected(category.id)
                                        showMoreMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyMap(
    state: FieldSurveyUiState,
    activeSketchTool: SketchTool,
    showBoundaryDistances: Boolean,
    onMapLocationPicked: (Double, Double) -> Unit,
    onMapShapeFinished: (List<Pair<Double, Double>>, SketchTool) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        val defaultLatLng = when {
            state.boundary.isNotEmpty() -> {
                val lat = state.boundary.map { it.latitude }.average()
                val lng = state.boundary.map { it.longitude }.average()
                LatLng(lat, lng)
            }
            state.job?.latitude != null && state.job.longitude != null -> {
                LatLng(state.job.latitude!!, state.job.longitude!!)
            }
            else -> LatLng(-31.436, -63.548)
        }
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 15f)
    }

    val drawingEnabled = state.baseLayer == BaseLayer.SATELLITE &&
        state.activeTool == SurveyTool.DRAW &&
        state.activeCategoryId != null

    val drawingTool = if (activeSketchTool == SketchTool.PAN) SketchTool.FREEHAND else activeSketchTool
    val activeCategoryColor = state.activeCategory()?.color ?: MaterialTheme.colorScheme.primary

    var previewPolyline by remember(state.surveyId, drawingEnabled, drawingTool) {
        mutableStateOf(emptyList<LatLng>())
    }
    var previewPolygon by remember(state.surveyId, drawingEnabled, drawingTool) {
        mutableStateOf(emptyList<LatLng>())
    }

    LaunchedEffect(drawingEnabled) {
        if (!drawingEnabled) {
            previewPolyline = emptyList()
            previewPolygon = emptyList()
        }
    }

    val mapUiSettings = remember(drawingEnabled) {
        MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = !drawingEnabled,
            zoomGesturesEnabled = !drawingEnabled,
            rotationGesturesEnabled = !drawingEnabled,
            tiltGesturesEnabled = !drawingEnabled,
            myLocationButtonEnabled = false
        )
    }

    val instructionText = remember(drawingTool) { drawInstructionForTool(drawingTool) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = if (state.baseLayer == BaseLayer.SATELLITE) MapType.HYBRID else MapType.NORMAL,
                isMyLocationEnabled = false
            ),
            uiSettings = mapUiSettings,
            onMapClick = { latLng ->
                if (!drawingEnabled && state.activeCategoryId != null && state.activeTool == SurveyTool.ADD_MARKER) {
                    onMapLocationPicked(latLng.latitude, latLng.longitude)
                }
            }
        ) {
            if (state.boundary.isNotEmpty()) {
                Polygon(
                    points = state.boundary.map { LatLng(it.latitude, it.longitude) },
                    strokeColor = MaterialTheme.colorScheme.primary,
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )

                // Agregar marcadores de distancia en cada segmento del perímetro
                if (showBoundaryDistances) {
                    val context = LocalContext.current
                    for (i in 0 until state.boundary.size) {
                        val point1 = state.boundary[i]
                        val point2 = state.boundary[(i + 1) % state.boundary.size]

                        // Calcular distancia entre puntos consecutivos
                        val distance = calculateDistance(
                            point1.latitude, point1.longitude,
                            point2.latitude, point2.longitude
                        )

                        // Calcular punto medio del segmento
                        val midpoint = calculateMidpoint(
                            point1.latitude, point1.longitude,
                            point2.latitude, point2.longitude
                        )

                        // Crear marcador con la distancia
                        val distanceText = formatDistance(distance)
                        val markerIcon = rememberDistanceMarkerDescriptor(context, distanceText)

                        Marker(
                            state = MarkerState(position = LatLng(midpoint.first, midpoint.second)),
                            icon = markerIcon,
                            anchor = Offset(0.5f, 0.5f),
                            zIndex = 3f,
                            flat = true
                        )
                    }
                }
            }
            state.mapAnnotations.forEach { annotation ->
                key(annotation.id) {
                    when (val geometry = annotation.geometry) {
                        is SurveyGeometry.MapPoint -> {
                            val markerDescriptor = rememberMarkerDescriptor(annotation)
                            Marker(
                                state = MarkerState(position = LatLng(geometry.latitude, geometry.longitude)),
                                title = annotation.title ?: annotation.category.label,
                                snippet = annotation.description,
                                icon = markerDescriptor,
                                anchor = Offset(0.5f, 1f),
                                zIndex = 1f
                            )
                        }
                        is SurveyGeometry.MapPolyline -> {
                            Polyline(
                                points = geometry.points.map { LatLng(it.first, it.second) },
                                color = annotation.category.color,
                                width = 8f
                            )
                            // Agregar marcador en el centro de la línea
                            if (annotation.title?.isNotBlank() == true || annotation.description?.isNotBlank() == true) {
                                val centerPoint = calculatePolylineCenter(geometry.points)
                                val markerDescriptor = rememberMarkerDescriptor(annotation)
                                Marker(
                                    state = MarkerState(position = LatLng(centerPoint.first, centerPoint.second)),
                                    title = annotation.title ?: annotation.category.label,
                                    snippet = annotation.description,
                                    icon = markerDescriptor,
                                    anchor = Offset(0.5f, 1f),
                                    zIndex = 2f
                                )
                            }
                        }
                        is SurveyGeometry.MapPolygon -> {
                            Polygon(
                                points = geometry.points.map { LatLng(it.first, it.second) },
                                strokeColor = annotation.category.color,
                                fillColor = annotation.category.color.copy(alpha = 0.2f)
                            )
                            // Agregar marcador en el centro del polígono
                            if (annotation.title?.isNotBlank() == true || annotation.description?.isNotBlank() == true) {
                                val centerPoint = calculatePolygonCenter(geometry.points)
                                val markerDescriptor = rememberMarkerDescriptor(annotation)
                                Marker(
                                    state = MarkerState(position = LatLng(centerPoint.first, centerPoint.second)),
                                    title = annotation.title ?: annotation.category.label,
                                    snippet = annotation.description,
                                    icon = markerDescriptor,
                                    anchor = Offset(0.5f, 1f),
                                    zIndex = 2f
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }

            if (previewPolyline.size >= 2) {
                Polyline(
                    points = previewPolyline,
                    color = activeCategoryColor,
                    width = 8f,
                    zIndex = 2f
                )
            }
            if (previewPolygon.size >= 3) {
                Polygon(
                    points = previewPolygon,
                    strokeColor = activeCategoryColor,
                    fillColor = activeCategoryColor.copy(alpha = 0.2f),
                    zIndex = 2f
                )
            }
        }

        if (drawingEnabled) {
            MapDrawingTouchLayer(
                cameraPositionState = cameraPositionState,
                activeSketchTool = drawingTool,
                onPreviewPolyline = { previewPolyline = it },
                onPreviewPolygon = { previewPolygon = it },
                onFinished = { latLngs ->
                    if (latLngs.size >= 2) {
                        onMapShapeFinished(
                            latLngs.map { it.latitude to it.longitude },
                            drawingTool
                        )
                    }
                    previewPolyline = emptyList()
                    previewPolygon = emptyList()
                },
                onCancel = {
                    previewPolyline = emptyList()
                    previewPolygon = emptyList()
                }
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = instructionText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MapDrawingTouchLayer(
    cameraPositionState: CameraPositionState,
    activeSketchTool: SketchTool,
    onPreviewPolyline: (List<LatLng>) -> Unit,
    onPreviewPolygon: (List<LatLng>) -> Unit,
    onFinished: (List<LatLng>) -> Unit,
    onCancel: () -> Unit
) {
    var startLatLng by remember { mutableStateOf<LatLng?>(null) }
    var freehandPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var latestPolyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var latestPolygon by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    fun reset() {
        startLatLng = null
        freehandPoints = emptyList()
        latestPolyline = emptyList()
        latestPolygon = emptyList()
        onPreviewPolyline(emptyList())
        onPreviewPolygon(emptyList())
    }

    LaunchedEffect(activeSketchTool) {
        reset()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cameraPositionState, activeSketchTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val projection = cameraPositionState.projection ?: return@detectDragGestures
                        val start = projection.fromScreenLocation(Point(offset.x.roundToInt(), offset.y.roundToInt()))
                        startLatLng = start
                        freehandPoints = listOf(start)
                        latestPolyline = listOf(start)
                        onPreviewPolyline(listOf(start))
                        onPreviewPolygon(emptyList())
                    },
                    onDrag = { change, _ ->
                        val projection = cameraPositionState.projection ?: return@detectDragGestures
                        val start = startLatLng ?: return@detectDragGestures
                        val current = projection.fromScreenLocation(Point(change.position.x.roundToInt(), change.position.y.roundToInt()))
                        when (activeSketchTool) {
                            SketchTool.FREEHAND -> {
                                val updated = appendFreehandPoint(freehandPoints, current)
                                freehandPoints = updated
                                latestPolyline = updated
                                onPreviewPolyline(updated)
                                onPreviewPolygon(emptyList())
                            }
                            SketchTool.LINE -> {
                                val points = listOf(start, current)
                                latestPolyline = points
                                onPreviewPolyline(points)
                                onPreviewPolygon(emptyList())
                            }
                            SketchTool.ARROW -> {
                                val arrow = buildArrowPolyline(start, current)
                                latestPolyline = arrow
                                onPreviewPolyline(arrow)
                                onPreviewPolygon(emptyList())
                            }
                            SketchTool.RECTANGLE -> {
                                val polygon = buildRectanglePolygon(start, current)
                                latestPolygon = polygon
                                onPreviewPolygon(polygon)
                                onPreviewPolyline(emptyList())
                            }
                            SketchTool.CIRCLE -> {
                                val polygon = buildCirclePolygon(start, current)
                                latestPolygon = polygon
                                onPreviewPolygon(polygon)
                                onPreviewPolyline(emptyList())
                            }
                            SketchTool.PAN -> Unit
                        }
                    },
                    onDragEnd = {
                        val result = when {
                            latestPolygon.size >= 3 -> latestPolygon
                            latestPolyline.size >= 2 -> latestPolyline
                            else -> emptyList()
                        }
                        if (result.size >= 2) {
                            onFinished(result)
                        } else {
                            onCancel()
                        }
                        reset()
                    },
                    onDragCancel = {
                        reset()
                        onCancel()
                    }
                )
            }
    )
}

private fun appendFreehandPoint(points: List<LatLng>, newPoint: LatLng): List<LatLng> {
    if (points.isEmpty()) return listOf(newPoint)
    val last = points.last()
    return if (haversineDistanceMeters(last, newPoint) < MIN_DRAW_DISTANCE_METERS) points else points + newPoint
}

private fun buildRectanglePolygon(start: LatLng, end: LatLng): List<LatLng> {
    val (dx, dy) = meterDelta(start, end)
    if (abs(dx) < MIN_DRAW_DISTANCE_METERS || abs(dy) < MIN_DRAW_DISTANCE_METERS) return emptyList()

    // Calcular el ángulo del arrastre
    val angle = atan2(dy, dx)

    // Calcular la distancia del arrastre
    val distance = sqrt(dx * dx + dy * dy)

    // Usar la mitad de la distancia como ancho, y mantener proporción para el alto
    val width = distance
    val height = distance * 0.5  // Relación 2:1 para el rectángulo

    // Calcular los 4 puntos del rectángulo rotado
    // El rectángulo se construye con start como punto de inicio y end como punto diagonal opuesto
    val centerLat = (start.latitude + end.latitude) / 2
    val centerLng = (start.longitude + end.longitude) / 2
    val center = LatLng(centerLat, centerLng)

    // Calcular los offsets para cada esquina (en metros)
    val halfWidth = width / 2
    val halfHeight = height / 2

    // Los 4 puntos del rectángulo en coordenadas locales
    val corners = listOf(
        Pair(-halfWidth, -halfHeight),
        Pair(halfWidth, -halfHeight),
        Pair(halfWidth, halfHeight),
        Pair(-halfWidth, halfHeight),
        Pair(-halfWidth, -halfHeight) // Cerrar el polígono
    )

    // Rotar y trasladar cada punto
    return corners.map { (x, y) ->
        // Rotar el punto
        val rotatedX = x * cos(angle) - y * sin(angle)
        val rotatedY = x * sin(angle) + y * cos(angle)

        // Convertir de metros a coordenadas geográficas
        val latOffset = rotatedY / EARTH_RADIUS_METERS * (180.0 / PI)
        val lngOffset = rotatedX / (EARTH_RADIUS_METERS * cos(Math.toRadians(center.latitude))) * (180.0 / PI)

        LatLng(center.latitude + latOffset, center.longitude + lngOffset)
    }
}

private fun buildCirclePolygon(center: LatLng, edge: LatLng, segments: Int = 36): List<LatLng> {
    val radius = haversineDistanceMeters(center, edge)
    if (radius < MIN_DRAW_DISTANCE_METERS) return emptyList()
    val points = mutableListOf<LatLng>()
    val step = (2 * PI) / segments
    for (i in 0..segments) {
        val angle = i * step
        val x = cos(angle) * radius
        val y = sin(angle) * radius
        points.add(offsetLatLng(center, x, y))
    }
    return points
}

private fun buildArrowPolyline(start: LatLng, end: LatLng): List<LatLng> {
    val (dx, dy) = meterDelta(start, end)
    val length = sqrt(dx * dx + dy * dy)
    if (length < MIN_DRAW_DISTANCE_METERS) return listOf(start, end)
    val headLength = min(length * 0.25, 25.0)
    val headAngle = Math.toRadians(25.0)
    val angle = atan2(dy, dx)
    val left = offsetLatLng(end, cos(angle + PI - headAngle) * headLength, sin(angle + PI - headAngle) * headLength)
    val right = offsetLatLng(end, cos(angle + PI + headAngle) * headLength, sin(angle + PI + headAngle) * headLength)
    return listOf(start, end, left, end, right)
}

private fun haversineDistanceMeters(a: LatLng, b: LatLng): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLat = lat2 - lat1
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val sinLat = sin(dLat / 2)
    val sinLng = sin(dLng / 2)
    val c = 2 * atan2(
        sqrt(sinLat * sinLat + cos(lat1) * cos(lat2) * sinLng * sinLng),
        sqrt(1 - sinLat * sinLat - cos(lat1) * cos(lat2) * sinLng * sinLng)
    )
    return EARTH_RADIUS_METERS * c
}

private fun meterDelta(origin: LatLng, target: LatLng): Pair<Double, Double> {
    val latRad = Math.toRadians(origin.latitude)
    val dLat = Math.toRadians(target.latitude - origin.latitude)
    val dLng = Math.toRadians(target.longitude - origin.longitude)
    val dy = dLat * EARTH_RADIUS_METERS
    val dx = dLng * EARTH_RADIUS_METERS * cos(latRad).coerceAtLeast(1e-6)
    return dx to dy
}

private fun offsetLatLng(origin: LatLng, dxMeters: Double, dyMeters: Double): LatLng {
    val latRad = Math.toRadians(origin.latitude)
    val newLat = origin.latitude + Math.toDegrees(dyMeters / EARTH_RADIUS_METERS)
    val denominator = (EARTH_RADIUS_METERS * cos(latRad)).coerceAtLeast(1e-6)
    val newLng = origin.longitude + Math.toDegrees(dxMeters / denominator)
    return LatLng(newLat, newLng)
}

private fun sketchToolLabel(tool: SketchTool): String = when (tool) {
    SketchTool.FREEHAND -> "Libre"
    SketchTool.LINE -> "Linea"
    SketchTool.ARROW -> "Flecha"
    SketchTool.RECTANGLE -> "Rect"
    SketchTool.CIRCLE -> "Circulo"
    SketchTool.PAN -> "Mover"
}

private fun drawInstructionForTool(tool: SketchTool): String = when (tool) {
    SketchTool.FREEHAND -> "Arrastra para dibujar un trazo libre sobre el mapa."
    SketchTool.LINE -> "Arrastra para definir una linea entre dos puntos."
    SketchTool.ARROW -> "Arrastra para indicar una direccion con una flecha."
    SketchTool.RECTANGLE -> "Arrastra para crear un rectangulo. La direccion del arrastre define la rotacion."
    SketchTool.CIRCLE -> "Arrastra para definir un circulo desde su centro."
    SketchTool.PAN -> "Selecciona otra herramienta de dibujo para comenzar."
}

private const val EARTH_RADIUS_METERS = 6_378_137.0
private const val MIN_DRAW_DISTANCE_METERS = 1.5

private fun calculatePolylineCenter(points: List<Pair<Double, Double>>): Pair<Double, Double> {
    if (points.isEmpty()) return 0.0 to 0.0
    if (points.size == 1) return points.first()

    // Para líneas, usar el punto medio de la línea
    val midIndex = points.size / 2
    return points[midIndex]
}

private fun calculatePolygonCenter(points: List<Pair<Double, Double>>): Pair<Double, Double> {
    if (points.isEmpty()) return 0.0 to 0.0
    if (points.size == 1) return points.first()

    // Calcular el centroide del polígono (promedio de todos los puntos)
    val lat = points.map { it.first }.average()
    val lng = points.map { it.second }.average()
    return lat to lng
}

@Composable
private fun rememberMarkerDescriptor(annotation: SurveyAnnotationItem): BitmapDescriptor {
    val context = LocalContext.current
    val displayTitle = annotation.title?.takeIf { it.isNotBlank() } ?: annotation.category.label
    val appContext = context.applicationContext
    return remember(
        annotation.id,
        annotation.updatedAt,
        annotation.category.id,
        annotation.category.label,
        annotation.category.colorHex,
        annotation.category.icon,
        displayTitle
    ) {
        BitmapDescriptorFactory.fromBitmap(
            createMarkerBitmap(appContext, annotation.category, displayTitle)
        )
    }
}

private fun createMarkerBitmap(
    context: Context,
    category: AnnotationCategory,
    title: String
): Bitmap {
    val density = context.resources.displayMetrics.density
    val displayTitle = shortenForMarker(title.ifBlank { category.label })
    val glyph = glyphForCategory(category)

    val circleRadius = 14f * density
    val pointerHeight = 12f * density
    val outerPadding = 6f * density
    val textPadding = 8f * density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 12f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textWidth = max(textPaint.measureText(displayTitle), 1f)
    val textMetrics = textPaint.fontMetrics
    val textHeight = textMetrics.descent - textMetrics.ascent
    val labelHeight = textHeight + textPadding

    val baseWidth = circleRadius * 2f + outerPadding * 2f
    val contentWidth = textWidth + textPadding * 2f
    val finalWidth = max(baseWidth, contentWidth)
    val finalHeight = circleRadius * 2f + pointerHeight + labelHeight + outerPadding * 2f

    val bitmap = Bitmap.createBitmap(
        finalWidth.roundToInt().coerceAtLeast(1),
        finalHeight.roundToInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    bitmap.eraseColor(AndroidColor.TRANSPARENT)
    val canvas = AndroidCanvas(bitmap)

    val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = category.color.toArgb()
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(230, 33, 33, 33)
        style = Paint.Style.FILL
    }

    val centerX = finalWidth / 2f
    val circleCenterY = outerPadding + circleRadius
    canvas.drawCircle(centerX, circleCenterY, circleRadius, markerPaint)
    canvas.drawCircle(centerX, circleCenterY, circleRadius, strokePaint)

    val pointerTopY = circleCenterY + circleRadius * 0.6f
    val pointerPath = AndroidPath().apply {
        moveTo(centerX - circleRadius * 0.6f, pointerTopY)
        lineTo(centerX + circleRadius * 0.6f, pointerTopY)
        lineTo(centerX, pointerTopY + pointerHeight)
        close()
    }
    canvas.drawPath(pointerPath, markerPaint)
    canvas.drawPath(pointerPath, strokePaint)

    val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 12f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val glyphMetrics = glyphPaint.fontMetrics
    val glyphBaseline = circleCenterY - (glyphMetrics.ascent + glyphMetrics.descent) / 2f
    canvas.drawText(glyph, centerX, glyphBaseline, glyphPaint)

    val labelTop = pointerTopY + pointerHeight + outerPadding
    val halfLabelWidth = textWidth / 2f + textPadding
    val labelRect = RectF(
        centerX - halfLabelWidth,
        labelTop,
        centerX + halfLabelWidth,
        labelTop + labelHeight
    )
    val cornerRadius = 10f * density
    canvas.drawRoundRect(labelRect, cornerRadius, cornerRadius, labelBackgroundPaint)

    val textBaseline = labelRect.centerY() - (textMetrics.ascent + textMetrics.descent) / 2f
    canvas.drawText(displayTitle, centerX, textBaseline, textPaint)

    return bitmap
}

private fun glyphForCategory(category: AnnotationCategory): String {
    val iconKey = category.icon.lowercase(Locale.ROOT)
    val explicit = when (iconKey) {
        "access" -> "AC"
        "refuelling" -> "RF"
        "no_entry" -> "X"
        "warning" -> "!"
        "neighbor" -> "NB"
        "hazard" -> "HZ"
        "note" -> "NT"
        else -> null
    }
    val fallbackSource = explicit
        ?: category.icon.takeIf { it.isNotBlank() }?.take(2)
        ?: category.label.takeIf { it.isNotBlank() }?.take(2)
        ?: category.id.take(2)
        ?: "?"
    return fallbackSource.uppercase(Locale.ROOT).take(2)
}

private fun shortenForMarker(text: String, maxChars: Int = 24): String {
    val trimmed = text.trim()
    if (trimmed.length <= maxChars) return trimmed
    val safeLength = (maxChars - 3).coerceAtLeast(1)
    return trimmed.take(safeLength).trimEnd() + "..."
}

@Composable
private fun ExpandedMapDialog(
    state: FieldSurveyUiState,
    onDismiss: () -> Unit,
    onMapLocationPicked: (Double, Double) -> Unit,
    onMapShapeFinished: (List<Pair<Double, Double>>, SketchTool) -> Unit,
    onCategorySelected: (String) -> Unit,
    onToolSelected: (SurveyTool) -> Unit,
    onSketchToolSelected: (SketchTool) -> Unit
) {
    var isControlsExpanded by rememberSaveable { mutableStateOf(true) }
    var showBoundaryDistances by rememberSaveable { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SurveyMap(
                    state = state,
                    activeSketchTool = state.activeSketchTool,
                    showBoundaryDistances = showBoundaryDistances,
                    onMapLocationPicked = onMapLocationPicked,
                    onMapShapeFinished = onMapShapeFinished
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .width(280.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Controles",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { isControlsExpanded = !isControlsExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isControlsExpanded)
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                    else
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = if (isControlsExpanded) "Contraer" else "Expandir",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isControlsExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LayerAndToolSelector(
                                baseLayer = state.baseLayer,
                                onLayerChanged = {}
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CategorySelector(
                                categories = state.categories,
                                activeCategoryId = state.activeCategoryId,
                                onCategorySelected = onCategorySelected
                            )

                            // Control de visibilidad de distancias
                            if (state.boundary.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FilterChip(
                                    selected = showBoundaryDistances,
                                    onClick = { showBoundaryDistances = !showBoundaryDistances },
                                    label = { Text("Distancias", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (showBoundaryDistances) Icons.Default.Check else Icons.Default.Map,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }

                            if (state.baseLayer == BaseLayer.SATELLITE && state.activeTool == SurveyTool.DRAW) {
                                Spacer(modifier = Modifier.height(10.dp))
                                SketchToolSelector(
                                    selectedTool = state.activeSketchTool,
                                    availableTools = listOf(
                                        SketchTool.FREEHAND,
                                        SketchTool.LINE,
                                        SketchTool.ARROW,
                                        SketchTool.RECTANGLE,
                                        SketchTool.CIRCLE
                                    ),
                                    onToolSelected = onSketchToolSelected,
                                    showUndo = false,
                                    canUndo = false,
                                    onUndo = {}
                                )
                            }
                        }
                    }
                }

                // FAB contextual para activar modo dibujo (igual que en pantalla normal)
                if (state.activeCategoryId != null) {
                    if (state.activeTool == SurveyTool.DRAW) {
                        // ExtendedFAB cuando está dibujando
                        ExtendedFloatingActionButton(
                            onClick = { onToolSelected(SurveyTool.ADD_MARKER) },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finalizar dibujo"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finalizar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // FAB normal para activar dibujo
                        FloatingActionButton(
                            onClick = { onToolSelected(SurveyTool.DRAW) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Dibujar en mapa"
                            )
                        }
                    }
                }

                FilledIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FullscreenExit,
                        contentDescription = "Cerrar mapa ampliado"
                    )
                }
            }
        }
    }
}

@Composable
private fun SketchCanvas(
    annotations: List<SurveyAnnotationItem>,
    activeCategory: AnnotationCategory?,
    sketchTool: SketchTool,
    onShapeFinished: (List<Pair<Float, Float>>, SketchTool) -> Unit
) {
    val backgroundBrush = Brush.linearGradient(listOf(Color(0xFFFDFDFD), Color(0xFFF0F0F0)))
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val minScale = 1f
    val maxScale = 3.5f

    val currentPath: MutableState<List<Offset>> = remember { mutableStateOf(emptyList()) }
    val currentShape: MutableState<Pair<Offset, Offset>?> = remember { mutableStateOf(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .pointerInput(sketchTool) {
                if (sketchTool == SketchTool.PAN) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(minScale, maxScale)
                        offset += pan
                    }
                }
            }
            .pointerInput(sketchTool, activeCategory) {
                if (activeCategory == null) return@pointerInput
                if (sketchTool == SketchTool.PAN) return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        val canvasPos = toCanvasSpace(start, offset, scale)
                        when (sketchTool) {
                            SketchTool.FREEHAND -> currentPath.value = listOf(canvasPos)
                            SketchTool.LINE, SketchTool.ARROW, SketchTool.RECTANGLE, SketchTool.CIRCLE ->
                                currentShape.value = canvasPos to canvasPos
                            SketchTool.PAN -> Unit
                        }
                    },
                    onDrag = { change, _ ->
                        val canvasPos = toCanvasSpace(change.position, offset, scale)
                        when (sketchTool) {
                            SketchTool.FREEHAND -> currentPath.value = currentPath.value + canvasPos
                            SketchTool.LINE, SketchTool.ARROW, SketchTool.RECTANGLE, SketchTool.CIRCLE ->
                                currentShape.value = currentShape.value?.first?.let { it to canvasPos }
                            SketchTool.PAN -> Unit
                        }
                    },
                    onDragEnd = {
                        if (canvasSize.width == 0f || canvasSize.height == 0f) {
                            currentPath.value = emptyList()
                            currentShape.value = null
                            return@detectDragGestures
                        }
                        when (sketchTool) {
                            SketchTool.FREEHAND -> {
                                val normalized = currentPath.value.map { offsetToNormalized(it, canvasSize) }
                                onShapeFinished(normalized, SketchTool.FREEHAND)
                                currentPath.value = emptyList()
                            }
                            SketchTool.LINE, SketchTool.ARROW, SketchTool.RECTANGLE, SketchTool.CIRCLE -> {
                                currentShape.value?.let { (start, end) ->
                                    val normalized = listOf(offsetToNormalized(start, canvasSize), offsetToNormalized(end, canvasSize))
                                    onShapeFinished(normalized, sketchTool)
                                }
                                currentShape.value = null
                            }
                            SketchTool.PAN -> Unit
                        }
                    },
                    onDragCancel = {
                        currentPath.value = emptyList()
                        currentShape.value = null
                    }
                )
            }
    ) {
        withTransform({
            translate(offset.x, offset.y)
            scale(scaleX = scale, scaleY = scale)
        }) {
            canvasSize = size
            drawRect(Color.White, size = size)
            annotations.forEach { annotation ->
                when (val geometry = annotation.geometry) {
                    is SurveyGeometry.SketchPath -> drawSketchPath(geometry.points, annotation.category.color)
                    is SurveyGeometry.SketchShape -> drawSketchShape(geometry.points, geometry.shape, annotation.category.color)
                    else -> Unit
                }
            }

            if (currentPath.value.isNotEmpty()) {
                drawSketchPath(
                    currentPath.value.map { offsetToNormalized(it, size) },
                    activeCategory?.color ?: Color.Magenta
                )
            }
            currentShape.value?.let { (start, end) ->
                drawSketchShape(
                    listOf(offsetToNormalized(start, size), offsetToNormalized(end, size)),
                    when (sketchTool) {
                        SketchTool.LINE -> "line"
                        SketchTool.ARROW -> "arrow"
                        SketchTool.RECTANGLE -> "rectangle"
                        SketchTool.CIRCLE -> "circle"
                        else -> "line"
                    },
                    activeCategory?.color ?: Color.Magenta
                )
            }
        }
    }
}

private fun DrawScope.drawSketchPath(points: List<Pair<Float, Float>>, color: Color) {
    if (points.size < 2) return
    val path = Path()
    val first = points.first()
    path.moveTo(first.first * size.width, first.second * size.height)
    points.drop(1).forEach { point ->
        path.lineTo(point.first * size.width, point.second * size.height)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawSketchShape(points: List<Pair<Float, Float>>, shape: String, color: Color) {
    if (points.size < 2) return
    val start = points.first()
    val end = points.last()
    when (shape.lowercase(Locale.getDefault())) {
        "line" -> drawLine(
            color = color,
            start = Offset(start.first * size.width, start.second * size.height),
            end = Offset(end.first * size.width, end.second * size.height),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        "arrow" -> {
            val startOffset = Offset(start.first * size.width, start.second * size.height)
            val endOffset = Offset(end.first * size.width, end.second * size.height)
            drawLine(color, startOffset, endOffset, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            val angle = atan2((endOffset.y - startOffset.y), (endOffset.x - startOffset.x))
            val arrowHeadLength = 18f
            val angle1 = angle - Math.toRadians(25.0).toFloat()
            val angle2 = angle + Math.toRadians(25.0).toFloat()
            val point1 = Offset(
                (endOffset.x - arrowHeadLength * cos(angle1)),
                (endOffset.y - arrowHeadLength * sin(angle1))
            )
            val point2 = Offset(
                (endOffset.x - arrowHeadLength * cos(angle2)),
                (endOffset.y - arrowHeadLength * sin(angle2))
            )
            drawLine(color, endOffset, point1, strokeWidth = 3.dp.toPx())
            drawLine(color, endOffset, point2, strokeWidth = 3.dp.toPx())
        }
        "rectangle" -> {
            val left = min(start.first, end.first) * size.width
            val right = max(start.first, end.first) * size.width
            val top = min(start.second, end.second) * size.height
            val bottom = max(start.second, end.second) * size.height
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4.dp.toPx())
            )
        }
        "circle" -> {
            val center = Offset(start.first * size.width, start.second * size.height)
            val edge = Offset(end.first * size.width, end.second * size.height)
            val radius = hypot((edge.x - center.x).toDouble(), (edge.y - center.y).toDouble()).toFloat()
            drawCircle(
                color = color,
                center = center,
                radius = radius,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}

private fun toCanvasSpace(position: Offset, offset: Offset, scale: Float): Offset {
    return (position - offset) / scale
}

private fun offsetToNormalized(offset: Offset, size: Size): Pair<Float, Float> {
    val x = (offset.x / size.width).coerceIn(0f, 1f)
    val y = (offset.y / size.height).coerceIn(0f, 1f)
    return x to y
}

enum class AnnotationListSize(val dp: Int, val label: String) {
    SMALL(100, "S"),
    MEDIUM(180, "M"),
    LARGE(280, "L")
}

@Composable
private fun AnnotationList(
    annotations: List<SurveyAnnotationItem>,
    onDeleteAnnotation: (Int) -> Unit,
    onEditAnnotation: (Int) -> Unit,
    onAddMedia: (Int) -> Unit,
    onRemoveMedia: (Int) -> Unit,
    onPhotoClick: (String) -> Unit
) {
    var listSize by rememberSaveable { mutableStateOf(AnnotationListSize.MEDIUM) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header con contador y botones de tamaño
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anotaciones (${annotations.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                // Botones de tamaño
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnnotationListSize.values().forEach { size ->
                        FilterChip(
                            selected = listSize == size,
                            onClick = { listSize = size },
                            label = { Text(size.label, fontSize = 11.sp) },
                            modifier = Modifier.size(width = 36.dp, height = 32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (annotations.isEmpty()) {
                Text(
                    text = "💡 Toca el mapa para agregar anotaciones",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(listSize.dp.dp)
                ) {
                    items(annotations, key = { it.id }) { annotation ->
                        AnnotationRow(
                            annotation = annotation,
                            onDeleteAnnotation = onDeleteAnnotation,
                            onEditAnnotation = onEditAnnotation,
                            onAddMedia = onAddMedia,
                            onRemoveMedia = onRemoveMedia,
                            onPhotoClick = onPhotoClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationRow(
    annotation: SurveyAnnotationItem,
    onDeleteAnnotation: (Int) -> Unit,
    onEditAnnotation: (Int) -> Unit,
    onAddMedia: (Int) -> Unit,
    onRemoveMedia: (Int) -> Unit,
    onPhotoClick: (String) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = annotation.title ?: annotation.category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = annotation.category.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp
                    )
                    annotation.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                    }
                    if (annotation.isCritical) {
                        Text(
                            text = "⚠ Crítico",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontSize = 10.sp
                        )
                    }
                }
                Row {
                    IconButton(onClick = { onEditAnnotation(annotation.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Editar", Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDeleteAnnotation(annotation.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Eliminar", Modifier.size(16.dp))
                    }
                }
            }

            if (annotation.media.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(annotation.media, key = { it.id }) { media ->
                        MediaThumbnail(
                            uri = media.uri,
                            onRemove = { onRemoveMedia(media.id) },
                            onClick = { onPhotoClick(media.uri) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { onAddMedia(annotation.id) },
                modifier = Modifier.height(24.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, null, Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Foto", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MediaThumbnail(uri: String, onRemove: () -> Unit, onClick: () -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(width = 80.dp, height = 60.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = uri,
                contentDescription = "Adjunto - Toca para ampliar",
                modifier = Modifier.fillMaxSize()
            )
            FilledIconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun PhotoLightbox(uri: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Foto ampliada",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )

            // Botón cerrar
            FilledIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AnnotationDialog(
    category: AnnotationCategory?,
    isSaving: Boolean,
    isEditing: Boolean,
    initialTitle: String,
    initialDescription: String,
    initialIsCritical: Boolean,
    onCancel: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var isCritical by remember { mutableStateOf(initialIsCritical) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (isEditing) "Editar" else "Guardar", fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                category?.let {
                    Text("Categoría: ${it.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detalle", fontSize = 12.sp) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCritical, onCheckedChange = { isCritical = it })
                    Text("Dato crítico", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, description, isCritical) }, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEditing) "Actualizar" else "Guardar", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) {
                Text("Cancelar", fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun CustomCategoryDialog(
    onCancel: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    val colorOptions = listOf("#0D47A1", "#1B5E20", "#E65100", "#6A1B9A", "#00838F")
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Nueva categoría", fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nombre", fontSize = 12.sp) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text("Color", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color)
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 1.dp,
                                    color = if (selectedColor == colorHex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(label.trim(), selectedColor) }, enabled = label.isNotBlank()) {
                Text("Crear", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar", fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun MediaSourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjuntar foto", fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AddAPhoto, null, Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tomar foto", fontSize = 13.sp)
                }
                Button(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Photo, null, Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galería", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontSize = 12.sp)
            }
        }
    )
}

private fun createTempImageUri(context: Context): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imagesDir = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val imageFile = File(imagesDir, "IMG_$timestamp.jpg")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

/**
 * Calcula la distancia en metros entre dos puntos geográficos usando la fórmula de Haversine
 */
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
}

/**
 * Calcula el punto medio entre dos coordenadas geográficas
 */
private fun calculateMidpoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Pair<Double, Double> {
    return ((lat1 + lat2) / 2.0) to ((lon1 + lon2) / 2.0)
}

/**
 * Formatea la distancia en metros a un string legible
 */
private fun formatDistance(distanceMeters: Double): String {
    return when {
        distanceMeters >= 1000 -> String.format("%.2f km", distanceMeters / 1000)
        distanceMeters >= 100 -> String.format("%.0f m", distanceMeters)
        else -> String.format("%.1f m", distanceMeters)
    }
}

/**
 * Crea un bitmap simple con texto de distancia para usar como marcador
 */
private fun createDistanceMarkerBitmap(context: Context, distanceText: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val padding = 8f * density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 13f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val textWidth = textPaint.measureText(distanceText)
    val textMetrics = textPaint.fontMetrics
    val textHeight = textMetrics.descent - textMetrics.ascent

    val width = (textWidth + padding * 2).roundToInt().coerceAtLeast(1)
    val height = (textHeight + padding * 2).roundToInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(AndroidColor.TRANSPARENT)
    val canvas = AndroidCanvas(bitmap)

    // Fondo semi-transparente con bordes redondeados
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(240, 33, 150, 243) // Azul Material
        style = Paint.Style.FILL
    }
    val cornerRadius = 6f * density
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

    // Borde blanco
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

    // Texto
    val textBaseline = height / 2f - (textMetrics.ascent + textMetrics.descent) / 2f
    canvas.drawText(distanceText, width / 2f, textBaseline, textPaint)

    return bitmap
}

@Composable
private fun rememberDistanceMarkerDescriptor(context: Context, distanceText: String): BitmapDescriptor {
    return remember(distanceText) {
        BitmapDescriptorFactory.fromBitmap(
            createDistanceMarkerBitmap(context, distanceText)
        )
    }
}
