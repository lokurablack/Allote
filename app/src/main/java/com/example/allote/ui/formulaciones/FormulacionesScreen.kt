package com.example.allote.ui.formulaciones

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.allote.data.Formulacion
import kotlinx.coroutines.launch

// --- Moved from FormulacionesScreen for better structure and to avoid recomposition issues ---

class DragDropState(
    private var list: List<Formulacion>,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    var draggedItem by mutableStateOf<Formulacion?>(null)
        private set
    var draggedItemOffset by mutableStateOf(Offset.Zero)
        private set
    var overIndex by mutableStateOf(-1)
        private set
    var isDraggingDown by mutableStateOf(false)
        private set

    var initialDragIndex by mutableStateOf(-1)
    val itemLayouts = mutableStateMapOf<Any, Pair<Float, Float>>()

    fun updateList(newList: List<Formulacion>) {
        this.list = newList
    }

    fun registerItemLayout(key: Any, y: Float, height: Float) {
        itemLayouts[key] = Pair(y, height)
    }

    fun onDragStart(item: Formulacion, offset: Offset) {
        draggedItem = item
        initialDragIndex = list.indexOf(item)
        draggedItemOffset = Offset.Zero
        overIndex = initialDragIndex
    }

    fun onDrag(offset: Offset) {
        draggedItemOffset += offset
        isDraggingDown = offset.y > 0

        val currentItem = draggedItem ?: return
        val (currentItemY, currentItemHeight) = itemLayouts[currentItem.id] ?: return
        val draggedItemCenterY = currentItemY + draggedItemOffset.y + (currentItemHeight / 2)

        var closestIndex = -1
        var minDistance = Float.MAX_VALUE

        list.forEachIndexed { index, targetItem ->
            val (targetItemY, targetItemHeight) = itemLayouts[targetItem.id]
                ?: return@forEachIndexed
            val targetCenterY = targetItemY + targetItemHeight / 2
            val distance = kotlin.math.abs(draggedItemCenterY - targetCenterY)

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }

        if (closestIndex != -1) {
            overIndex = closestIndex
        }
    }

    fun onDragEnd() {
        if (initialDragIndex != -1 && overIndex != -1 && initialDragIndex != overIndex) {
            onMove(initialDragIndex, overIndex)
        }

        draggedItem = null
        draggedItemOffset = Offset.Zero
        initialDragIndex = -1
        overIndex = -1
    }
}

@Composable
fun rememberDragDropState(
    list: List<Formulacion>,
    onMove: (from: Int, to: Int) -> Unit
): DragDropState {
    val state = remember { DragDropState(list, onMove) }
    LaunchedEffect(list) {
        state.updateList(list)
    }
    return state
}

@Preview
@Composable
fun RememberDragDropStatePreview() {
    val formulaciones = listOf(
        Formulacion(id = 1, nombre = "Formulacion 1", ordenMezcla = 1, tipoUnidad = "LIQUIDO"),
        Formulacion(id = 2, nombre = "Formulacion 2", ordenMezcla = 2, tipoUnidad = "SOLIDO")
    )
    rememberDragDropState(list = formulaciones, onMove = { _, _ -> })
}

// --- Main Screen Composable ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FormulacionesScreen(
    formulaciones: List<Formulacion>,
    onMove: (from: Int, to: Int) -> Unit,
    onAddFormulacion: (String, String) -> Unit,
    onUpdateFormulacion: (Int, String, String) -> Unit,
    onDeleteFormulacion: (Formulacion) -> Unit,
    isFormulacionInUse: suspend (Formulacion) -> Boolean,
    setFabAction: (() -> Unit) -> Unit,
    onAutoSave: (() -> Unit) -> Unit = { }
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Formulacion?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Formulacion?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) } // Estado para el diálogo de ayuda
    val haptics = LocalHapticFeedback.current

    val dragDropState = rememberDragDropState(formulaciones) { from, to -> onMove(from, to) }

    LaunchedEffect(true) {
        setFabAction { showAddDialog = true }
    }

    // Auto-guardado al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            onAutoSave {
                // Opcional: mostrar toast de confirmación
                // Toast.makeText(context, "Cambios guardados automáticamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ordenar Formulaciones") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Header
            FormulacionesHeader(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(formulaciones, key = { _, item -> item.id }) { index, formulacion ->
                    val draggedItem = dragDropState.draggedItem
                    val isBeingDragged = draggedItem?.id == formulacion.id
                    
                    val draggedItemHeight = remember(draggedItem) {
                        if (draggedItem != null) {
                            dragDropState.itemLayouts[draggedItem.id]?.second ?: 0f
                        } else {
                            0f
                        }
                    }

                    val displacement by animateFloatAsState(
                        targetValue = when {
                            draggedItem == null -> 0f
                            isBeingDragged -> 0f
                            else -> {
                                val initialIndex = dragDropState.initialDragIndex
                                val hoverIndex = dragDropState.overIndex
                                when {
                                    initialIndex == -1 || hoverIndex == -1 || initialIndex == hoverIndex -> 0f
                                    index in (initialIndex + 1)..hoverIndex -> -draggedItemHeight
                                    index in hoverIndex..(initialIndex - 1) -> draggedItemHeight
                                    else -> 0f
                                }
                            }
                        }, label = "displacementAnimation"
                    )

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { layoutCoordinates ->
                                dragDropState.registerItemLayout(
                                    key = formulacion.id,
                                    y = layoutCoordinates.positionInParent().y,
                                    height = layoutCoordinates.size.height.toFloat()
                                )
                            }
                            .pointerInput(formulacion) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragDropState.onDragStart(
                                            formulacion,
                                            offset
                                        )
                                    },
                                    onDragEnd = { dragDropState.onDragEnd() },
                                    onDragCancel = { dragDropState.onDragEnd() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDropState.onDrag(dragAmount)
                                    }
                                )
                            }
                    ) {
                        FormulacionItem(
                            formulacion = formulacion,
                            isBeingDragged = isBeingDragged,
                            dragDropState = dragDropState,
                            onEdit = { showEditDialog = formulacion },
                            onDelete = { showDeleteDialog = formulacion },
                            modifier = Modifier
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationY = dragDropState.draggedItemOffset.y
                                        alpha = 0.8f
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        shadowElevation = 8f
                                    } else {
                                        translationY = displacement
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditFormulacionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nombre, tipo ->
                onAddFormulacion(nombre, tipo)
                showAddDialog = false
            }
        )
    }
    showEditDialog?.let { formulacion ->
        AddEditFormulacionDialog(
            formulacion = formulacion,
            onDismiss = { showEditDialog = null },
            onConfirm = { nombre, tipo ->
                onUpdateFormulacion(formulacion.id, nombre, tipo)
                showEditDialog = null
            }
        )
    }
    showDeleteDialog?.let { formulacion ->
        DeleteFormulacionDialog(
            formulacion = formulacion,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                onDeleteFormulacion(formulacion)
                showDeleteDialog = null
            },
            isFormulacionInUse = { isFormulacionInUse(formulacion) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview
@Composable
fun FormulacionesScreenPreview() {
    val formulaciones = listOf(
        Formulacion(id = 1, nombre = "Formulacion A", ordenMezcla = 1, tipoUnidad = "LIQUIDO"),
        Formulacion(id = 2, nombre = "Formulacion B", ordenMezcla = 2, tipoUnidad = "SOLIDO"),
        Formulacion(id = 3, nombre = "Formulacion C", ordenMezcla = 3, tipoUnidad = "LIQUIDO")
    )
    FormulacionesScreen(
        formulaciones = formulaciones,
        onMove = { _, _ -> },
        onAddFormulacion = { _, _ -> },
        onUpdateFormulacion = { _, _, _ -> },
        onDeleteFormulacion = {},
        isFormulacionInUse = { false },
        setFabAction = {},
        onAutoSave = {}
    )
}

// --- Helper Composables ---

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Help,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White
                                )
                            }
                        }
                        Text(
                            text = "Ayuda: Orden de Mezcla",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Esta pantalla es crucial para asegurar la correcta preparación de las mezclas en el tanque.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HelpFeatureCard(
                        icon = Icons.Default.DragIndicator,
                        title = "Orden de Mezcla",
                        description = "El número indica el orden de agregado. Arrastra y suelta para reordenar.",
                        color = MaterialTheme.colorScheme.primary
                    )

                    HelpFeatureCard(
                        icon = Icons.Default.Add,
                        title = "Gestionar Formulaciones",
                        description = "Usa el botón flotante (+) para añadir y los iconos para editar o eliminar.",
                        color = MaterialTheme.colorScheme.secondary
                    )

                    HelpFeatureCard(
                        icon = Icons.Default.AutoMode,
                        title = "Integración con Recetas",
                        description = "El orden se usa automáticamente en la pantalla de 'Recetas' para guiar al operario.",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun HelpDialogPreview() {
    HelpDialog(onDismiss = {})
}

@Composable
private fun FormulacionesHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
            .padding(top = 24.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = "Orden de Mezcla",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Arrastra para reordenar las formulaciones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
fun FormulacionesHeaderPreview() {
    FormulacionesHeader()
}

@Composable
private fun FormulacionItem(
    formulacion: Formulacion,
    isBeingDragged: Boolean,
    dragDropState: DragDropState,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = when {
        isBeingDragged -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isBeingDragged) 8.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (isBeingDragged) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth().height(IntrinsicSize.Min) // Reduced height
                .padding(horizontal = 8.dp, vertical = 8.dp), // Adjusted vertical padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Order number
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background( // Ensure this background doesn't push height
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${formulacion.ordenMezcla}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formulacion.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val icon = if (formulacion.tipoUnidad == "LIQUIDO") Icons.Default.WaterDrop else Icons.Default.Grain
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formulacion.tipoUnidad.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(
                    onClick = onEdit,
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onDelete,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun FormulacionItemPreview() {
    val formulacion = Formulacion(id = 1, nombre = "Test Formulacion", ordenMezcla = 1, tipoUnidad = "LIQUIDO")
    val dragDropState = rememberDragDropState(list = listOf(formulacion), onMove = {_,_ ->})
    FormulacionItem(
        formulacion = formulacion,
        isBeingDragged = false,
        dragDropState = dragDropState,
        onEdit = {},
        onDelete = {}
    )
}


@Composable
private fun HelpFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
fun HelpFeatureCardPreview() {
    HelpFeatureCard(
        icon = Icons.Default.Info,
        title = "Sample Feature",
        description = "This is a sample description for the feature card.",
        color = MaterialTheme.colorScheme.secondary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditFormulacionDialog(
    formulacion: Formulacion? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(formulacion?.nombre ?: "") }
    var tipo by remember { mutableStateOf(formulacion?.tipoUnidad ?: "LIQUIDO") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                val title = if (formulacion == null) "Agregar Formulación" else "Editar Formulación"
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la formulación") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = tipo.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        label = { Text("Tipo de Unidad") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Líquido")
                                }
                            },
                            onClick = { tipo = "LIQUIDO"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Grain,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Sólido")
                                }
                            },
                            onClick = { tipo = "SOLIDO"; expanded = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(nombre, tipo) },
                        enabled = nombre.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AddEditFormulacionDialogPreview() {
    AddEditFormulacionDialog(
        onDismiss = {},
        onConfirm = { _, _ -> }
    )
}

@Composable
private fun DeleteFormulacionDialog(
    formulacion: Formulacion,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isFormulacionInUse: suspend () -> Boolean
) {
    var isInUse by remember { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(formulacion) {
        scope.launch {
            try {
                isInUse = isFormulacionInUse()
            } catch (e: Exception) {
                errorMessage = "Error al verificar el uso de la formulación."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Eliminar Formulación",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        errorMessage != null -> {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        isInUse == null -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text("Verificando...")
                            }
                        }
                        isInUse == true -> {
                            Text(
                                text = "Esta formulación no se puede eliminar porque está en uso.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        isInUse == false -> {
                            Text(
                                text = "¿Confirmas que quieres eliminar la formulación '${formulacion.nombre}'? Esta acción no se puede deshacer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = isInUse == false,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DeleteFormulacionDialogPreview() {
    val formulacion = Formulacion(id = 1, nombre = "Formulacion X", ordenMezcla = 1, tipoUnidad = "SOLIDO")
    DeleteFormulacionDialog(
        formulacion = formulacion,
        onDismiss = {},
        onConfirm = {},
        isFormulacionInUse = { false }
    )
}
