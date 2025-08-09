package com.example.allote.ui.checklists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.allote.data.Checklist
import com.example.allote.data.ChecklistItem
import com.example.allote.ui.checklists.components.ChecklistBottomSheet
import com.example.allote.ui.checklists.components.ChecklistCard
import com.example.allote.ui.checklists.components.ChecklistItemRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChecklistsScreen(
    uiState: ChecklistsUiState,
    onCreateChecklist: (String) -> Unit,
    onRenameChecklist: (Checklist, String) -> Unit,
    onDeleteChecklist: (Checklist) -> Unit,
    onSelectChecklist: (Int) -> Unit,
    onAddItem: (String) -> Unit,
    onToggleItem: (ChecklistItem) -> Unit,
    onUpdateItemText: (ChecklistItem, String) -> Unit,
    onDeleteItem: (ChecklistItem) -> Unit,
    onReorderItem: (Int, Int) -> Unit,
    onShowBottomSheet: () -> Unit,
    onHideBottomSheet: () -> Unit,
    onStartEditingItem: (ChecklistItem) -> Unit,
    onCancelEditingItem: () -> Unit,
    setFabAction: (() -> Unit) -> Unit
) {
    var showNewChecklistDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var checklistToRename by remember { mutableStateOf<Checklist?>(null) }
    var checklistToDelete by remember { mutableStateOf<Checklist?>(null) }
    var newChecklistTitle by remember { mutableStateOf("") }
    var newItemText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val fabAction by rememberUpdatedState(newValue = {
        if (uiState.checklists.isEmpty()) {
            showNewChecklistDialog = true
        } else {
            onShowBottomSheet()
        }
    })

    DisposableEffect(setFabAction) {
        setFabAction { fabAction() }
        onDispose { }
    }

    if (showNewChecklistDialog) {
        AlertDialog(
            onDismissRequest = { showNewChecklistDialog = false },
            title = { Text("Nueva Checklist") },
            text = {
                OutlinedTextField(
                    value = newChecklistTitle,
                    onValueChange = { newChecklistTitle = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newChecklistTitle.isNotBlank()) {
                            onCreateChecklist(newChecklistTitle)
                            scope.launch {
                                snackbarHostState.showSnackbar("Checklist creada")
                            }
                        }
                        newChecklistTitle = ""
                        showNewChecklistDialog = false
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChecklistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showRenameDialog && checklistToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar Checklist") },
            text = {
                OutlinedTextField(
                    value = newChecklistTitle,
                    onValueChange = { newChecklistTitle = it },
                    label = { Text("Nuevo título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newChecklistTitle.isNotBlank()) {
                            onRenameChecklist(checklistToRename!!, newChecklistTitle)
                            scope.launch {
                                snackbarHostState.showSnackbar("Checklist renombrada")
                            }
                        }
                        newChecklistTitle = ""
                        showRenameDialog = false
                        checklistToRename = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    checklistToRename = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (checklistToDelete != null) {
        AlertDialog(
            onDismissRequest = { checklistToDelete = null },
            title = { Text("Eliminar Checklist") },
            text = { Text("¿Estás seguro de que quieres eliminar '${checklistToDelete!!.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteChecklist(checklistToDelete!!)
                        scope.launch {
                            snackbarHostState.showSnackbar("Checklist eliminada")
                        }
                        checklistToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { checklistToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (uiState.showBottomSheet) {
        ChecklistBottomSheet(
            checklists = uiState.checklists,
            selectedChecklistId = uiState.selectedChecklist?.id,
            onSelectChecklist = onSelectChecklist,
            onCreateNew = { showNewChecklistDialog = true },
            onRename = { checklist ->
                checklistToRename = checklist
                newChecklistTitle = checklist.title
                showRenameDialog = true
            },
            onDelete = { checklist ->
                checklistToDelete = checklist
            },
            onDismiss = onHideBottomSheet
        )
    }

    if (uiState.editingItem != null) {
        val editingItem = uiState.editingItem!!
        var editText by remember(editingItem) { mutableStateOf(editingItem.text) }

        AlertDialog(
            onDismissRequest = onCancelEditingItem,
            title = { Text("Editar Item") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateItemText(editingItem, editText)
                    onCancelEditingItem()
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = onCancelEditingItem) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Checklists",
                            style = MaterialTheme.typography.titleLarge
                        )
                        AnimatedVisibility(
                            visible = uiState.selectedChecklist != null,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Text(
                                text = uiState.selectedChecklist?.title ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = uiState.checklists.isNotEmpty(),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        IconButton(onClick = onShowBottomSheet) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Ver todas las checklists"
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.checklists.isEmpty()) {
                EmptyChecklistsState(
                    onCreateNew = { showNewChecklistDialog = true }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedVisibility(
                        visible = uiState.checklists.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(
                                items = uiState.checklists,
                                key = { it.id }
                            ) { checklist ->
                                val progress = uiState.checklistProgress[checklist.id] ?: (0 to 0)
                                ChecklistCard(
                                    checklist = checklist,
                                    isSelected = checklist.id == uiState.selectedChecklist?.id,
                                    completedItems = progress.first,
                                    totalItems = progress.second,
                                    onClick = { onSelectChecklist(checklist.id) },
                                    modifier = Modifier
                                        .width(280.dp)
                                        .animateItem()
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = uiState.selectedChecklist,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
                        },
                        label = "checklist_content"
                    ) { selectedChecklist ->
                        if (selectedChecklist != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newItemText,
                                            onValueChange = { newItemText = it },
                                            label = { Text("Nuevo Item") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                                            )
                                        )
                                        FilledIconButton(
                                            onClick = {
                                                if (newItemText.isNotBlank()) {
                                                    onAddItem(newItemText)
                                                    newItemText = ""
                                                }
                                            },
                                            enabled = newItemText.isNotBlank()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Agregar"
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (uiState.items.isEmpty()) {
                                    EmptyItemsState()
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(
                                            items = uiState.items,
                                            key = { _, item -> item.id }
                                        ) { index, item ->
                                            var currentIndex by remember(item.id) { mutableStateOf(index) }
                                            LaunchedEffect(index) { currentIndex = index }
                                            var accumulatedDrag by remember(item.id) { mutableStateOf(0f) }
                                            val itemHeightPx = remember { mutableStateOf(0) }

                                            ChecklistItemRow(
                                                item = item,
                                                onToggle = { onToggleItem(item) },
                                                onEdit = { onStartEditingItem(item) },
                                                onDelete = { onDeleteItem(item) },
                                                onStartDrag = {},
                                                onDrag = {},
                                                onDragEnd = {},
                                                modifier = Modifier
                                                    .onGloballyPositioned { coords ->
                                                        itemHeightPx.value = coords.size.height
                                                    }
                                                    .pointerInput(item.id) {
                                                        detectDragGestures(
                                                            onDragStart = { },
                                                            onDrag = { change, dragAmount ->
                                                                change.consumeAllChanges()
                                                                accumulatedDrag += dragAmount.y
                                                                val threshold = if (itemHeightPx.value > 0) itemHeightPx.value / 2f else return@detectDragGestures
                                                                val steps = (accumulatedDrag / threshold).toInt()
                                                                if (steps != 0) {
                                                                    val targetIndex = (currentIndex + steps).coerceIn(0, uiState.items.lastIndex)
                                                                    if (targetIndex != currentIndex) {
                                                                        onReorderItem(currentIndex, targetIndex)
                                                                        currentIndex = targetIndex
                                                                        accumulatedDrag -= steps * threshold
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                accumulatedDrag = 0f
                                                            },
                                                            onDragCancel = {
                                                                accumulatedDrag = 0f
                                                            }
                                                        )
                                                    }
                                                    .then(Modifier.animateItem(
                                                        fadeInSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        ),
                                                        fadeOutSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    ))
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Selecciona una checklist",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun EmptyChecklistsState(
    onCreateNew: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "No hay checklists",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Crea tu primera checklist para comenzar",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            FilledTonalButton(
                onClick = onCreateNew,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Checklist")
            }
        }
    }
}

@Composable
private fun EmptyItemsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Text(
                text = "Sin items",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Agrega tu primer item usando el campo de arriba",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}