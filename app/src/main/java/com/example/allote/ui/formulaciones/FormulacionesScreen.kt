package com.example.allote.ui.formulaciones

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.TopAppBar
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
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
                                onDragStart = { offset -> dragDropState.onDragStart(formulacion, offset) },
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
                        dragDropState = dragDropState, // <-- 2. PASA EL PARÁMETRO AQUÍ
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

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Orden de Mezcla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Esta pantalla es crucial para asegurar la correcta preparación de las mezclas en el tanque.")
                Text("• Orden: El número a la izquierda indica el orden en que los productos con este tipo de formulación deben ser agregados. Arrastra y suelta los ítems para ajustar el orden correcto.")
                Text("• Gestión: Usa el botón flotante (+) para añadir nuevas formulaciones y los íconos en cada ítem para editar o eliminar.")
                Text("El orden que establezcas aquí se usará automáticamente en la pantalla de 'Recetas' para guiar al operario.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

@Composable
fun FormulacionItem(
    formulacion: Formulacion,
    isBeingDragged: Boolean,
    dragDropState: DragDropState, // <-- 1. AÑADE EL PARÁMETRO AQUÍ
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isBeingDragged) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBeingDragged) {
                MaterialTheme.colorScheme.surfaceVariant
            } else if (dragDropState.draggedItem != null) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Arrastrar para reordenar",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formulacion.ordenMezcla}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formulacion.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formulacion.tipoUnidad,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Editar")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

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
            val (targetItemY, targetItemHeight) = itemLayouts[targetItem.id] ?: return@forEachIndexed
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
private fun rememberDragDropState(
    list: List<Formulacion>,
    onMove: (from: Int, to: Int) -> Unit
): DragDropState {
    val state = remember { DragDropState(list, onMove) }
    LaunchedEffect(list) {
        state.updateList(list)
    }
    return state
}

@Composable
fun AddEditFormulacionDialog(
    formulacion: Formulacion? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(formulacion?.nombre ?: "") }
    var tipo by remember { mutableStateOf(formulacion?.tipoUnidad ?: "LIQUIDO") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (formulacion == null) "Agregar Formulación" else "Editar Formulación") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        label = { Text("Tipo de Unidad") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("LIQUIDO") },
                            onClick = { tipo = "LIQUIDO"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("SOLIDO") },
                            onClick = { tipo = "SOLIDO"; expanded = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nombre, tipo) },
                enabled = nombre.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteFormulacionDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Formulación") },
        text = {
            when {
                errorMessage != null -> Text(errorMessage!!)
                isInUse == null -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                isInUse == true -> Text("No se puede eliminar. La formulación está en uso por uno o más productos.")
                isInUse == false -> Text("¿Está seguro de que desea eliminar la formulación '${formulacion.nombre}'?")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = isInUse == false, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Eliminar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}