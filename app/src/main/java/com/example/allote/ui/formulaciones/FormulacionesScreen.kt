package com.example.allote.ui.formulaciones

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var showHelpDialog by remember { mutableStateOf(false) } // Estado para el diálogo de ayuda

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
                            .pointerInput(formulacion) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
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
                        if (isTarget && !isBeingDragged) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .align(if (dragDropState.isDraggingDown) Alignment.BottomCenter else Alignment.TopCenter)
                            )
                        }

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
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
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
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Help,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ayuda: Orden de Mezcla",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
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
                }

                // Content
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Esta pantalla es crucial para asegurar la correcta preparación de las mezclas en el tanque.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HelpFeatureCard(
                        icon = Icons.Default.DragIndicator,
                        title = "Orden de Mezcla",
                        description = "El número indica el orden de agregado. Arrastra y suelta para reordenar.",
                        color = Color(0xFF4CAF50)
                    )

                    HelpFeatureCard(
                        icon = Icons.Default.Add,
                        title = "Gestionar Formulaciones",
                        description = "Usa el botón flotante (+) para añadir y los iconos para editar o eliminar.",
                        color = Color(0xFF2196F3)
                    )

                    HelpFeatureCard(
                        icon = Icons.Default.AutoMode,
                        title = "Integración con Recetas",
                        description = "El orden se usa automáticamente en la pantalla de 'Recetas' para guiar al operario.",
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormulacionesHeader(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "Orden de Mezcla",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Arrastra para reordenar formulaciones",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
        isBeingDragged -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        dragDropState.draggedItem != null -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isBeingDragged) 12.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Arrastrar para reordenar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Order number
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${formulacion.ordenMezcla}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formulacion.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (formulacion.tipoUnidad == "LIQUIDO")
                            Icons.Default.WaterDrop
                        else
                            Icons.Default.Grain,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formulacion.tipoUnidad,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            CircleShape
                        )
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
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (formulacion == null) "Agregar Formulación" else "Editar Formulación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        label = { Text("Tipo de Unidad") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Desplegar"
                                )
                            }
                        }
                    )
                    DropdownMenu(
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
                                    Text("LÍQUIDO")
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
                                    Text("SÓLIDO")
                                }
                            },
                            onClick = { tipo = "SOLIDO"; expanded = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = { onConfirm(nombre, tipo) },
                        enabled = nombre.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
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

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Eliminar Formulación",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    errorMessage != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage!!,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    isInUse == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text("Verificando uso...")
                            }
                        }
                    }

                    isInUse == true -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                    alpha = 0.3f
                                )
                            )
                        ) {
                            Text(
                                text = "No se puede eliminar. La formulación está en uso por uno o más productos.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    isInUse == false -> {
                        Text(
                            text = "¿Está seguro de que desea eliminar la formulación '${formulacion.nombre}'?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = isInUse == false,
                        modifier = Modifier.weight(1f),
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
