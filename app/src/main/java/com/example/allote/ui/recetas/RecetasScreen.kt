package com.example.allote.ui.recetas

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onAgregarProducto: (Product, Double, String) -> Unit,
    onEliminarProductoDeReceta: (Int) -> Unit,
    onProductSearchQueryChanged: (String) -> Unit,
    onClearProductSearch: () -> Unit,
    onNavigateToLotes: (Int) -> Unit
) {
    var showSelectProductDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showActionMenuForRecipe by remember { mutableStateOf<Recipe?>(null) }

    val resumenText = when {
        uiState.summaryIsDirty -> "Los datos han cambiado. Vuelve a calcular para ver el resumen actualizado."
        uiState.resumenActual?.isNotBlank() == true -> uiState.resumenActual
        else -> "Calcula una receta para ver el resumen."
    }

    if (showHelpDialog) {
        EnhancedHelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showActionMenuForRecipe != null) {
        AlertDialog(
            onDismissRequest = { showActionMenuForRecipe = null },
            title = { Text("Acciones de Receta") },
            text = { Text("Acciones para la receta: ${showActionMenuForRecipe?.id}") },
            confirmButton = {
                TextButton(onClick = { showActionMenuForRecipe = null }) {
                    Text("Cerrar")
                }
            }
        )
    }


    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Receta para: ${uiState.job?.clientName ?: "Trabajo"}",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = "Ayuda",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
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
                // Sección de inputs mejorada
                EnhancedInputSection(
                    uiState = uiState,
                    isSolidApplication = isSolidApplication,
                    isHectareasValid = isHectareasValid,
                    isCaudalValid = isCaudalValid,
                    isCaldoPorTachadaValid = isCaldoPorTachadaValid,
                    onHectareasChange = onHectareasChange,
                    onCaudalChange = onCaudalChange,
                    onCaldoPorTachadaChange = onCaldoPorTachadaChange
                )

                // Botones de acción mejorados
                EnhancedActionButtons(
                    onAddProduct = { showSelectProductDialog = true },
                    onCalculate = onCalcularClick,
                    isCalculateEnabled = isHectareasValid() && isCaudalValid() && isCaldoPorTachadaValid() && uiState.productosEnReceta.isNotEmpty(),
                    onNavigateToLotes = { uiState.job?.id?.let(onNavigateToLotes) }
                )

                // Lista de productos mejorada
                if (uiState.productosEnReceta.isNotEmpty()) {
                    EnhancedProductsSection(
                        productos = uiState.productosEnReceta,
                        onDelete = onEliminarProductoDeReceta
                    )
                }

                // Sección de resumen mejorada
                EnhancedSummarySection(
                    resumenText = resumenText,
                    isSummaryDirty = uiState.summaryIsDirty,
                    receta = uiState.receta,
                    onShowActionMenu = { showActionMenuForRecipe = it }
                )
            }
        }
    }

    // Diálogos existentes...
    if (showSelectProductDialog) {
        SelectProductDialog(
            uiState = uiState,
            filteredProducts = filteredProducts,
            onDismiss = { showSelectProductDialog = false },
            onProductSelected = { product, dose, unidad ->
                onAgregarProducto(product, dose, unidad)
                showSelectProductDialog = false
            },
            onSearchQueryChanged = onProductSearchQueryChanged,
            onClearSearch = onClearProductSearch
        )
    }

    // ... resto de diálogos sin cambios ...
}

@Composable
private fun EnhancedInputSection(
    uiState: RecetasUiState,
    isSolidApplication: Boolean,
    isHectareasValid: () -> Boolean,
    isCaudalValid: () -> Boolean,
    isCaldoPorTachadaValid: () -> Boolean,
    onHectareasChange: (String) -> Unit,
    onCaudalChange: (String) -> Unit,
    onCaldoPorTachadaChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Science,
                            contentDescription = "Parámetros",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Parámetros de Aplicación",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Campos de entrada
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EnhancedTextField(
                        value = uiState.hectareasText,
                        onValueChange = onHectareasChange,
                        label = "Hectáreas",
                        isError = uiState.hectareasText.isNotBlank() && !isHectareasValid(),
                        errorText = "Debe ser > 0",
                        modifier = Modifier.weight(1f)
                    )

                    if (!isSolidApplication) {
                        EnhancedTextField(
                            value = uiState.caudalText,
                            onValueChange = onCaudalChange,
                            label = "Caudal (L/ha)",
                            isError = uiState.caudalText.isNotBlank() && !isCaudalValid(),
                            errorText = "Debe ser > 0",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                EnhancedTextField(
                    value = uiState.caldoPorTachadaText,
                    onValueChange = onCaldoPorTachadaChange,
                    label = if (isSolidApplication) "Kgs por tachada" else "Caldo por tachada (L)",
                    isError = uiState.caldoPorTachadaText.isNotBlank() && !isCaldoPorTachadaValid(),
                    errorText = "Debe ser > 0",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    errorText: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        isError = isError,
        supportingText = { if (isError) Text(errorText) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun EnhancedActionButtons(
    onAddProduct: () -> Unit,
    onCalculate: () -> Unit,
    isCalculateEnabled: Boolean,
    onNavigateToLotes: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón Agregar Producto
            Button(
                onClick = onAddProduct,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Agregar Producto",
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Botón Calcular
            Button(
                onClick = onCalculate,
                enabled = isCalculateEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Calcular",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Botón Lotes
        Button(
            onClick = onNavigateToLotes,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(
                "Gestionar Lotes",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EnhancedProductsSection(
    productos: List<ProductoRecetaItem>,
    onDelete: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header de la sección
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Science,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = "Productos en la Receta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Lista de productos
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            productos.forEach { item ->
                EnhancedProductoRecetaItemView(
                    item = item,
                    onDelete = { onDelete(item.productId) }
                )
            }
        }
    }
}

@Composable
fun EnhancedProductoRecetaItemView(item: ProductoRecetaItem, onDelete: () -> Unit) {
    val unidad = when (item.unidadDosis) {
        "Gr/ha", "Kg/ha" -> "Kgs"
        else -> "Litros"
    }
    val bandaColorMap = remember {
        mapOf(
            "ia" to Color(0xFFD32F2F), "ib" to Color(0xFFD32F2F),
            "la" to Color(0xFFD32F2F), "lb" to Color(0xFFD32F2F),
            "ii" to Color(0xFFFBC02D), "iii" to Color(0xFF1976D2),
            "iv" to Color(0xFF388E3C)
        )
    }
    val color = bandaColorMap[item.bandaToxicologica?.trim()?.lowercase()] ?: Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Barra de color toxicológica
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(80.dp)
                    .background(color)
            )

            // Contenido principal
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.nombreComercial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Información de dosis
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dosis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.2f %s".format(item.dosis, item.unidadDosis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Información de cantidad total
                    if (item.cantidadTotal > 0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%.2f %s".format(item.cantidadTotal, unidad),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Botón eliminar
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar producto",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EnhancedSummarySection(
    resumenText: String,
    isSummaryDirty: Boolean,
    receta: Recipe?,
    onShowActionMenu: (Recipe) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header de la sección
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Resumen de la Receta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Card de resumen con indicador de estado
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { receta?.let(onShowActionMenu) }
                ),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box {
                // Indicador de estado
                if (isSummaryDirty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Blue.copy(alpha = 0.7f),
                                        Color.Red.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSummaryDirty) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Datos cambiados",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Blue
                            )
                            Text(
                                text = "Datos modificados",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Blue
                            )
                        }
                    }

                    Text(
                        text = resumenText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSummaryDirty)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedHelpDialog(onDismiss: () -> Unit) {
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
                // Header con gradiente
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
                                Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ayuda: Recetas y Lotes",
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

                // Contenido
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EnhancedHelpSection(
                        icon = Icons.Default.Science,
                        title = "Creación de Recetas",
                        description = "Proceso paso a paso para crear recetas precisas:",
                        color = MaterialTheme.colorScheme.primary,
                        items = listOf(
                            "Agrega productos: Pulsa 'Agregar Producto' para seleccionar los insumos de tu lista.",
                            "Define la dosis: Para cada producto, especifica la dosis por hectárea (L/ha para líquidos, gr/ha para sólidos).",
                            "Ingresa los parámetros: Completa las hectáreas a tratar, el caudal de aplicación y la capacidad de tu tanque.",
                            "Calcula: Presiona 'Calcular' para generar el resumen con las cantidades totales y la mezcla por carga."
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    EnhancedHelpSection(
                        icon = Icons.Default.Grain,
                        title = "Gestión de Lotes",
                        description = "Usa el botón 'Lotes' para dividir el trabajo total. En la siguiente pantalla podrás:",
                        color = MaterialTheme.colorScheme.secondary,
                        items = listOf(
                            "Registrar las hectáreas realmente trabajadas para cada lote.",
                            "Generar un resumen de sobrantes para saber cuánto producto quedó sin usar."
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedHelpSection(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    items: List<String> = emptyList()
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (items.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color.copy(alpha = 0.6f),
                                    CircleShape
                                )
                                .padding(top = 6.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectProductDialog(
    uiState: RecetasUiState,
    filteredProducts: List<Product>,
    onDismiss: () -> Unit,
    onProductSelected: (Product, Double, String) -> Unit,
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
            onConfirm = { dose, unidad ->
                onProductSelected(product, dose, unidad)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseInputDialog(
    product: Product,
    isSolidProduct: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var doseText by remember { mutableStateOf("") }
    // Unidad por defecto basada en formulación
    var expanded by remember { mutableStateOf(false) }
    val unidades = listOf("L/ha", "Cc/ha", "Gr/ha", "Kg/ha")
    var selectedUnidad by remember { mutableStateOf(if (isSolidProduct) "Gr/ha" else "L/ha") }
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
                    label = { Text("Dosis (${selectedUnidad})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))
                // Selector de unidad
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedUnidad,
                        onValueChange = {},
                        label = { Text("Unidad de dosis") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        unidades.forEach { unidad ->
                            DropdownMenuItem(
                                text = { Text(unidad) },
                                onClick = {
                                    selectedUnidad = unidad
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(doseText.toDouble(), selectedUnidad) }, enabled = isDoseValid) {
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
