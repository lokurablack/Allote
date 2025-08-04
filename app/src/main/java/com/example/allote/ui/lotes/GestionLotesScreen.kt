package com.example.allote.ui.lotes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.LocationPickerDialog
import com.example.allote.LocationViewerDialog
import com.example.allote.data.Lote
import com.example.allote.data.ProductSurplusInfo
import com.example.allote.data.SurplusSummary
import com.example.allote.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionLotesScreen(
    uiState: GestionLotesUiState,
    recipeSummary: String?,
    isLoadingRecipe: Boolean,
    onAddLote: (String, Double) -> Unit,
    onUpdateLote: (Lote) -> Unit,
    onDeleteLote: (Lote) -> Unit,
    onUpdateLoteLocation: (Lote, Double, Double) -> Unit,
    onLoadRecipe: (Lote) -> Unit,
    onDismissRecipe: () -> Unit,
    setFabAction: (() -> Unit) -> Unit,
    onRegistrarTrabajoRealizado: (Lote, Double) -> Unit,
    onGenerateSurplusSummary: () -> Unit,
    onClearSurplusSummary: () -> Unit
) {
    var showAddOrEditDialog by remember { mutableStateOf<Lote?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var loteForActions by remember { mutableStateOf<Lote?>(null) }
    var showLocationDialog by remember { mutableStateOf<Lote?>(null) }
    var loteParaRegistrar by remember { mutableStateOf<Lote?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        setFabAction {
            isAddingNew = true
            showAddOrEditDialog = Lote(jobId = 0, nombre = "", hectareas = 0.0)
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Lotes") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.lotes.isEmpty()) {
            EmptyState(
                title = "No hay lotes",
                subtitle = "Añade tu primer lote usando el botón '+'",
                icon = Icons.Default.Map
            )
        } else {
            Column(Modifier.fillMaxSize().padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.lotes, key = { it.id }) { lote ->
                        LoteListItem(
                            lote = lote,
                            onClick = { onLoadRecipe(lote) },
                            onLongClick = { loteForActions = lote }
                        )
                    }
                }
                Button(
                    onClick = onGenerateSurplusSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = uiState.lotes.any { it.hectareasReales != null }
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Generar Resumen de Sobrantes")
                }
            }
        }
    }

    val loteParaEditar = showAddOrEditDialog
    if (loteParaEditar != null) {
        LoteEditDialog(
            lote = loteParaEditar,
            isAddingNew = isAddingNew,
            lotesActuales = uiState.lotes,
            hectareasTotalesTrabajo = uiState.jobHectareasTotales,
            onDismiss = { showAddOrEditDialog = null; isAddingNew = false },
            onSave = { nombre, hectareas ->
                if (isAddingNew) {
                    onAddLote(nombre, hectareas)
                } else {
                    onUpdateLote(loteParaEditar.copy(nombre = nombre, hectareas = hectareas))
                }
                showAddOrEditDialog = null
                isAddingNew = false
            }
        )
    }

    loteForActions?.let { lote ->
        LoteActionDialog(
            lote = lote,
            onDismiss = { loteForActions = null },
            onEdit = { showAddOrEditDialog = lote; loteForActions = null },
            onDelete = { onDeleteLote(lote); loteForActions = null },
            onLocation = { showLocationDialog = lote; loteForActions = null },
            onRegistrarReal = { loteParaRegistrar = lote; loteForActions = null }
        )
    }

    loteParaRegistrar?.let { lote ->
        RegistroRealDialog(
            lote = lote,
            onDismiss = { loteParaRegistrar = null },
            onConfirm = { hectareasReales ->
                onRegistrarTrabajoRealizado(lote, hectareasReales)
                loteParaRegistrar = null
            }
        )
    }

    showLocationDialog?.let { lote ->
        var isPickerMode by remember(lote) { mutableStateOf(lote.latitude == null || lote.longitude == null) }
        val isEditing = lote.latitude != null && lote.longitude != null

        if (isPickerMode) {
            LocationPickerDialog(
                initialLat = lote.latitude ?: -32.36,
                initialLng = lote.longitude ?: -62.31,
                isEditing = isEditing,
                onConfirm = { lat, lng ->
                    onUpdateLoteLocation(lote, lat, lng)
                    showLocationDialog = null
                },
                onDismiss = { showLocationDialog = null }
            )
        } else {
            lote.latitude?.let { lat ->
                lote.longitude?.let { lng ->
                    LocationViewerDialog(
                        lat = lat,
                        lng = lng,
                        description = lote.nombre,
                        onDismiss = { showLocationDialog = null },
                        onEdit = { isPickerMode = true }
                    )
                }
            }
        }
    }

    if (recipeSummary != null) {
        LoteRecipeSummaryDialog(
            summary = recipeSummary,
            isLoading = isLoadingRecipe,
            onDismiss = onDismissRecipe
        )
    }

    uiState.surplusSummary?.let { summary ->
        SurplusSummaryDialog(
            summary = summary,
            onDismiss = onClearSurplusSummary
        )
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Gestión de Lotes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Flujo de Trabajo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("1. Cree los lotes: Use el botón (+) para añadir los lotes con sus hectáreas planificadas.")
                    Text("2. Registre el trabajo real: Al terminar un lote, haga una pulsación larga sobre él y seleccione 'Registrar Trabajo Realizado' para ingresar la superficie real trabajada.")
                    Text("3. Calcule el sobrante: Una vez registrado el trabajo real en al menos un lote, el botón 'Generar Resumen de Sobrantes' se activará para mostrarle el producto que no se utilizó.")
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acciones Adicionales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("• Pulsación corta: Muestra la receta específica para las hectáreas planificadas de ese lote.")
                    Text("• Pulsación larga: Permite editar, eliminar, registrar el trabajo real o asignar una ubicación GPS al lote.")
                }
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
fun LoteListItem(lote: Lote, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
            detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = lote.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (lote.hectareasReales != null) {
                    Text(text = "Planificado: ${lote.hectareas} ha", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Real: ${lote.hectareasReales} ha",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(text = "${lote.hectareas} ha", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (lote.hectareasReales != null) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Trabajo registrado", tint = MaterialTheme.colorScheme.primary)
            } else if (lote.latitude != null && lote.longitude != null) {
                Icon(Icons.Default.PinDrop, contentDescription = "Ubicación guardada", tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun LoteActionDialog(
    lote: Lote,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLocation: () -> Unit,
    onRegistrarReal: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column {
                Text(lote.nombre, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ListItem(
                        headlineContent = { Text("Registrar Trabajo Realizado") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.FactCheck, null) },
                        modifier = Modifier.clickable(onClick = onRegistrarReal)
                    )
                    ListItem(
                        headlineContent = { Text("Editar Planificado") },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.clickable(onClick = onEdit)
                    )
                    ListItem(
                        headlineContent = { Text(if (lote.latitude != null) "Ver/Editar Ubicación" else "Definir Ubicación") },
                        leadingContent = { Icon(Icons.Default.LocationOn, null) },
                        modifier = Modifier.clickable(onClick = onLocation)
                    )
                    ListItem(
                        headlineContent = { Text("Eliminar Lote") },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable(onClick = onDelete)
                    )
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
fun RegistroRealDialog(lote: Lote, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var hectareasReales by remember {
        mutableStateOf(lote.hectareasReales?.toString() ?: lote.hectareas.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Trabajo Real") },
        text = {
            Column {
                Text("Introduce las hectáreas que se trabajaron realmente para el lote '${lote.nombre}'.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = hectareasReales,
                    onValueChange = { hectareasReales = it },
                    label = { Text("Hectáreas Reales") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                hectareasReales.toDoubleOrNull()?.let {
                    onConfirm(it)
                }
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun LoteEditDialog(
    lote: Lote,
    isAddingNew: Boolean,
    lotesActuales: List<Lote>,
    hectareasTotalesTrabajo: Double?,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var nombreLote by remember(lote.nombre) { mutableStateOf(lote.nombre) }
    var hectareasLote by remember(lote.hectareas) { mutableStateOf(if(isAddingNew) "" else lote.hectareas.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAddingNew) "Agregar Lote" else "Editar Lote") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombreLote,
                    onValueChange = { nombreLote = it },
                    label = { Text("Nombre del lote") },
                    isError = errorMessage != null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hectareasLote,
                    onValueChange = {
                        hectareasLote = it
                        errorMessage = null
                                    },
                    label = { Text("Hectáreas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val hectareas = hectareasLote.toDoubleOrNull()
                if (nombreLote.isBlank() || hectareas == null || hectareas <= 0) {
                    errorMessage = "Nombre y hectáreas son obligatorios."
                    return@Button
                }

                val totalPermitido = hectareasTotalesTrabajo ?: Double.MAX_VALUE
                val hectareasOtrosLotes = if (isAddingNew) {
                    lotesActuales.sumOf { it.hectareas }
                } else {
                    lotesActuales.filter { it.id != lote.id }.sumOf { it.hectareas }
                }

                if (hectareasOtrosLotes + hectareas > totalPermitido) {
                    errorMessage = "Error: La suma de hectáreas (%.2f ha) supera el total del trabajo (%.2f ha)".format(
                        hectareasOtrosLotes + hectareas,
                        totalPermitido
                    )
                } else {
                    errorMessage = null
                    onSave(nombreLote, hectareas)
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurplusSummaryDialog(summary: SurplusSummary, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Resumen de Sobrantes") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Planificado", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "%.2f ha".format(summary.totalHectareasPlanificadas),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Trabajo Real", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "%.2f ha".format(summary.totalHectareasReales),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Detalle de Productos",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(summary.productSummaries) { productInfo ->
                    ProductSurplusCard(info = productInfo)
                }
            }
        }
    }
}

@Composable
fun ProductSurplusCard(info: ProductSurplusInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = info.productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Planificado:", style = MaterialTheme.typography.bodyMedium)
                Text("%.2f %s".format(info.cantidadPlanificada, info.unidad), style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Utilizado:", style = MaterialTheme.typography.bodyMedium)
                Text("%.2f %s".format(info.cantidadUtilizada, info.unidad), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sobrante:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "%.2f %s".format(info.cantidadSobrante, info.unidad),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
@Composable
fun LoteRecipeSummaryDialog(summary: String, isLoading: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resumen de Receta para el Lote") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(text = summary, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}