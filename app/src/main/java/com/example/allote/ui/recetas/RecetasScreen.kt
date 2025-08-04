package com.example.allote.ui.recetas

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.data.Product
import com.example.allote.data.Recipe

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecetasScreen(
    uiState: RecetasUiState,
    isSolidApplication: Boolean,
    filteredProducts: List<Product>,
    isHectareasValid: () -> Boolean,
    isCaudalValid: () -> Boolean,
    isCaldoPorTachadaValid: () -> Boolean,
    onHectareasChange: (String) -> Unit,
    onCaudalChange: (String) -> Unit,
    onCaldoPorTachadaChange: (String) -> Unit,
    onCalcularClick: () -> Unit,
    onEliminarClick: () -> Unit,
    onAgregarProducto: (Product, Double) -> Unit,
    onEliminarProductoDeReceta: (Int) -> Unit,
    onProductSearchQueryChanged: (String) -> Unit,
    onClearProductSearch: () -> Unit,
    onNavigateToLotes: (Int) -> Unit
) {
    var showSelectProductDialog by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf<Recipe?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) } // Estado para el diálogo de ayuda

    val resumenText = when {
        uiState.summaryIsDirty -> "Los datos han cambiado. Vuelve a calcular para ver el resumen actualizado."
        uiState.resumenActual?.isNotBlank() == true -> uiState.resumenActual
        else -> "Calcula una receta para ver el resumen."
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Receta para: ${uiState.job?.clientName ?: "Trabajo"}") },
                    actions = {
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InputSection(
                    uiState = uiState,
                    isSolidApplication = isSolidApplication,
                    isHectareasValid = isHectareasValid,
                    isCaudalValid = isCaudalValid,
                    isCaldoPorTachadaValid = isCaldoPorTachadaValid,
                    onHectareasChange = onHectareasChange,
                    onCaudalChange = onCaudalChange,
                    onCaldoPorTachadaChange = onCaldoPorTachadaChange
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showSelectProductDialog = true }, modifier = Modifier.weight(1f)) { Text("Agregar Producto") }
                    Button(
                        onClick = onCalcularClick,
                        modifier = Modifier.weight(1f),
                        enabled = isHectareasValid() && isCaudalValid() && isCaldoPorTachadaValid() && uiState.productosEnReceta.isNotEmpty()
                    ) { Text("Calcular") }
                }

                Button(onClick = { uiState.job?.id?.let(onNavigateToLotes) }, modifier = Modifier.fillMaxWidth()) { Text("Lotes") }

                if (uiState.productosEnReceta.isNotEmpty()) {
                    Text("Productos en la receta", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.productosEnReceta.forEach { item ->
                            ProductoRecetaItemView(
                                item = item,
                                onDelete = { onEliminarProductoDeReceta(item.productId) }
                            )
                        }
                    }
                }

                Text("Resumen", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { uiState.receta?.let { showActionMenu = it } }),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Text(resumenText, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }

    if (showSelectProductDialog) {
        SelectProductDialog(
            uiState = uiState,
            filteredProducts = filteredProducts,
            onDismiss = { showSelectProductDialog = false },
            onProductSelected = { product, dose ->
                onAgregarProducto(product, dose)
                showSelectProductDialog = false
            },
            onSearchQueryChanged = onProductSearchQueryChanged,
            onClearSearch = onClearProductSearch
        )
    }

    showActionMenu?.let { recipe ->
        // ... (código del diálogo sin cambios)
    }
    if (showEditDialog) {
        EditarRecetaDialog(
            uiState = uiState,
            isSolidApplication = isSolidApplication,
            onDismiss = { showEditDialog = false },
            onConfirmar = { h, c, ct ->
                onHectareasChange(h)
                onCaudalChange(c)
                onCaldoPorTachadaChange(ct)
                onCalcularClick()
                showEditDialog = false
            }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Receta") },
            text = { Text("¿Está seguro? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = { onEliminarClick(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun InputSection(
    uiState: RecetasUiState,
    isSolidApplication: Boolean,
    isHectareasValid: () -> Boolean,
    isCaudalValid: () -> Boolean,
    isCaldoPorTachadaValid: () -> Boolean,
    onHectareasChange: (String) -> Unit,
    onCaudalChange: (String) -> Unit,
    onCaldoPorTachadaChange: (String) -> Unit
) {
    val isHectareasError = uiState.hectareasText.isNotBlank() && !isHectareasValid()
    val isCaudalError = uiState.caudalText.isNotBlank() && !isCaudalValid()
    val isCaldoError = uiState.caldoPorTachadaText.isNotBlank() && !isCaldoPorTachadaValid()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = uiState.hectareasText,
                    onValueChange = onHectareasChange,
                    label = { Text("Hectáreas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = isHectareasError,
                    supportingText = { if (isHectareasError) Text("Debe ser > 0") }
                )

                if (!isSolidApplication) {
                    OutlinedTextField(
                        value = uiState.caudalText,
                        onValueChange = onCaudalChange,
                        label = { Text("Caudal (L/ha)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = isCaudalError,
                        supportingText = { if (isCaudalError) Text("Debe ser > 0") }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.caldoPorTachadaText,
                onValueChange = onCaldoPorTachadaChange,
                label = { Text(if (isSolidApplication) "Kgs por tachada" else "Caldo por tachada (L)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = isCaldoError,
                supportingText = { if (isCaldoError) Text("Debe ser > 0") }
            )
        }
    }
}

@Composable
fun ProductoRecetaItemView(item: ProductoRecetaItem, onDelete: () -> Unit) {
    val unidad = if (item.tipoUnidad.equals("SOLIDO", ignoreCase = true)) "kgs" else "Litros"
    val bandaColorMap = remember { mapOf(
        "ia" to Color(0xFFFF0000), "ib" to Color(0xFFFF0000), "la" to Color(0xFFFF0000), "lb" to Color(0xFFFF0000),
        "ii" to Color(0xFFFFC107), "iii" to Color(0xFF00008B), "iv" to Color(0xFF388E3C)
    )}
    val color = bandaColorMap[item.bandaToxicologica?.trim()?.lowercase()] ?: Color.DarkGray

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(8.dp).height(60.dp).background(color))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = item.nombreComercial, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Dosis: %.2f %s".format(item.dosis, if (item.tipoUnidad.equals("SOLIDO", ignoreCase = true)) "Grs/ha" else "Lts/ha"),style = MaterialTheme.typography.bodyMedium)
                if (item.cantidadTotal > 0) {
                    Text(text = "Cantidad total: %.2f %s".format(item.cantidadTotal, unidad), style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar producto", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EditarRecetaDialog(
    uiState: RecetasUiState,
    isSolidApplication: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (String, String, String) -> Unit
) {
    var hectareas by remember { mutableStateOf(uiState.hectareasText) }
    var caudal by remember { mutableStateOf(uiState.caudalText) }
    var caldoPorTachada by remember { mutableStateOf(uiState.caldoPorTachadaText) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Receta") },
        text = {
            Column {
                OutlinedTextField(value = hectareas, onValueChange = { hectareas = it }, label = { Text("Hectáreas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                if (!isSolidApplication) {
                    OutlinedTextField(value = caudal, onValueChange = { caudal = it }, label = { Text("Caudal (L/ha)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(value = caldoPorTachada, onValueChange = { caldoPorTachada = it }, label = { Text(if (isSolidApplication) "Kgs por tachada" else "Caldo por tachada (L)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if ((hectareas.toDoubleOrNull() ?: 0.0) <= 0) {
                    Toast.makeText(context, "Hectáreas debe ser un número mayor a 0", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                onConfirmar(hectareas, caudal, caldoPorTachada)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectProductDialog(
    uiState: RecetasUiState,
    filteredProducts: List<Product>,
    onDismiss: () -> Unit,
    onProductSelected: (Product, Double) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    var productForDoseDialog by remember { mutableStateOf<Product?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Seleccionar Producto") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar") }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = uiState.productSearchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = { Text("Buscar...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.productSearchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) { Icon(Icons.Default.Clear, contentDescription = "Limpiar") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductSelectionItem(
                            product = product,
                            onClick = { productForDoseDialog = product }
                        )
                    }
                }
            }
        }
    }

    productForDoseDialog?.let { product ->
        val isSolid = uiState.allFormulations.find { it.id == product.formulacionId }?.tipoUnidad.equals("SOLIDO", ignoreCase = true)
        DoseInputDialog(
            product = product,
            isSolidProduct = isSolid,
            onDismiss = { productForDoseDialog = null },
            onConfirm = { dose ->
                onProductSelected(product, dose)
                productForDoseDialog = null
            }
        )
    }
}

@Composable
fun ProductSelectionItem(product: Product, onClick: () -> Unit) {
    val bandaColor = remember(product.bandaToxicologica) {
        when (product.bandaToxicologica?.trim()?.uppercase()) {
            "LA", "IA", "LB", "IB" -> Color(0xFFD32F2F)
            "II" -> Color(0xFFFBC02D)
            "III" -> Color(0xFF1976D2)
            "IV" -> Color(0xFF388E3C)
            else -> Color.Transparent
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(8.dp).fillMaxHeight().background(bandaColor))
            Column(Modifier.padding(16.dp)) {
                Text(product.nombreComercial, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = product.principioActivo ?: product.tipo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DoseInputDialog(
    product: Product,
    isSolidProduct: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var doseText by remember { mutableStateOf("") }
    val isDoseValid = doseText.toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Establecer Dosis") },
        text = {
            Column {
                Text("Producto: ${product.nombreComercial}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = doseText,
                    onValueChange = { doseText = it },
                    label = { Text(if (isSolidProduct) "Dosis (gr/ha)" else "Dosis (L/ha)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(doseText.toDouble()) }, enabled = isDoseValid) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Recetas y Lotes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Sección de Recetas
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Creación de Recetas:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("1. Agrega productos: Pulsa 'Agregar Producto' para seleccionar los insumos de tu lista.")
                    Text("2. Define la dosis: Para cada producto, especifica la dosis por hectárea (L/ha para líquidos, gr/ha para sólidos).")
                    Text("3. Ingresa los parámetros: Completa las hectáreas a tratar, el caudal de aplicación y la capacidad de tu tanque.")
                    Text("4. Calcula: Presiona 'Calcular' para generar el resumen con las cantidades totales y la mezcla por carga.")
                }
                HorizontalDivider()
                // Sección de Lotes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gestión de Lotes:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Usa el botón 'Lotes' para dividir el trabajo total. En la siguiente pantalla podrás:")
                    Text(" • Registrar las hectáreas realmente trabajadas para cada lote.")
                    Text(" • Generar un resumen de sobrantes para saber cuánto producto quedó sin usar.")
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
