package com.example.allote.ui.formulaciones

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.data.Formulacion
import kotlinx.coroutines.launch

// --- DragDropState remains the same as it's a logic holder ---
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

    private var initialDragIndex by mutableStateOf(-1)
    private val itemLayouts = mutableStateMapOf<Any, Pair<Float, Float>>()

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
    var showHelpDialog by remember { mutableStateOf(false) }

    val dragDropState = rememberDragDropState(formulaciones) { from, to -> onMove(from, to) }

    LaunchedEffect(true) {
        setFabAction { showAddDialog = true }
    }

    DisposableEffect(Unit) {
        onDispose {
            onAutoSave {}
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orden de Mezcla") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            FormulacionesHeader(modifier = Modifier.padding(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(formulaciones, key = { _, item -> item.id }) { index, formulacion ->
                    val isBeingDragged = dragDropState.draggedItem?.id == formulacion.id
                    val isTarget = dragDropState.overIndex == index

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { layoutCoordinates ->
                                dragDropState.registerItemLayout(
                                    key = formulacion.id,
                                    y = layoutCoordinates.positionInParent().y,
                                    height = layoutCoordinates.size.height.toFloat()
                                )
                            }
                    ) {
                        if (isTarget && !isBeingDragged) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .align(if (dragDropState.isDraggingDown) Alignment.BottomCenter else Alignment.TopCenter)
                            )
                        }

                        FormulacionItem(
                            formulacion = formulacion,
                            isBeingDragged = isBeingDragged,
                            onEdit = { showEditDialog = formulacion },
                            onDelete = { showDeleteDialog = formulacion },
                            modifier = Modifier
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationY = dragDropState.draggedItemOffset.y
                                        alpha = 0.9f
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                                }
                                .pointerInput(formulacion) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset -> dragDropState.onDragStart(formulacion, offset) },
                                        onDragEnd = { dragDropState.onDragEnd() },
                                        onDragCancel = { dragDropState.onDragEnd() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDropState.onDrag(dragAmount)
                                        }
                                    )
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

// --- Improved Helper Composables ---

@Composable
private fun FormulacionesHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                text = "Prioridad de Mezcla",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Mantén presionado y arrastra para reordenar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormulacionItem(
    formulacion: Formulacion,
    isBeingDragged: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val elevation by animateDpAsState(if (isBeingDragged) 8.dp else 2.dp, label = "elevation")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(if (isBeingDragged) 4.dp else 1.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle and Order
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Arrastrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${formulacion.ordenMezcla}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formulacion.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (formulacion.tipoUnidad == "LIQUIDO") Icons.Default.WaterDrop else Icons.Default.Grain,
                        contentDescription = formulacion.tipoUnidad,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formulacion.tipoUnidad.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
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

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp)
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
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Orden de Mezcla",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Esta pantalla es crucial para asegurar la correcta preparación de las mezclas en el tanque.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    HelpFeature(
                        icon = Icons.Default.DragIndicator,
                        title = "Orden de Mezcla",
                        description = "El número indica el orden de agregado. Arrastra y suelta para reordenar.",
                        color = MaterialTheme.colorScheme.primary
                    )

                    HelpFeature(
                        icon = Icons.Default.Add,
                        title = "Gestionar Formulaciones",
                        description = "Usa el botón flotante (+) para añadir y los iconos para editar o eliminar.",
                        color = MaterialTheme.colorScheme.secondary
                    )

                    HelpFeature(
                        icon = Icons.Default.AutoMode,
                        title = "Integración con Recetas",
                        description = "El orden se usa automáticamente en la pantalla de 'Recetas' para guiar al operario.",
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Entendido")
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpFeature(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (formulacion == null) "Nueva Formulación" else "Editar Formulación",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = tipo.lowercase().replaceFirstChar { it.titlecase() },
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
                            text = { Text("Líquido") },
                            onClick = { tipo = "LIQUIDO"; expanded = false },
                            leadingIcon = { Icon(Icons.Default.WaterDrop, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sólido") },
                            onClick = { tipo = "SOLIDO"; expanded = false },
                            leadingIcon = { Icon(Icons.Default.Grain, null) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(nombre, tipo) },
                        enabled = nombre.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
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
            } catch (_: Exception) {
                errorMessage = "Error al verificar el uso."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Confirmar Eliminación",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (isInUse) {
                    null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Verificando uso...")
                        }
                    }
                    true -> {
                        Text(
                            text = "No se puede eliminar. La formulación '${formulacion.nombre}' está en uso por uno o más productos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    false -> {
                        Text(
                            text = "¿Está seguro de que desea eliminar la formulación '${formulacion.nombre}'? Esta acción no se puede deshacer.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
