package com.example.allote.ui.products

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.allote.data.ApplicationType
import com.example.allote.data.Formulacion
import com.example.allote.data.Product
import com.example.allote.ui.common.toxicBandColor
import com.example.allote.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosScreen(
    uiState: ProductsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearchQuery: () -> Unit,
    onTabSelected: (ApplicationType) -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onProductClick: (Int) -> Unit,
    onNavigateToFormulaciones: () -> Unit, // New parameter
    setFabAction: (() -> Unit) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDeleteDialog by remember { mutableStateOf<Product?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        setFabAction { showAddDialog = true }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Productos") },
                actions = {
                    // Only show formulaciones button when in Pulverización tab
                    if (uiState.selectedTab == ApplicationType.PULVERIZACION) {
                        IconButton(onClick = onNavigateToFormulaciones) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = "Gestionar Formulaciones",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(selected = uiState.selectedTab == ApplicationType.PULVERIZACION, onClick = { onTabSelected(ApplicationType.PULVERIZACION) }, text = { Text("Pulverización") })
                Tab(selected = uiState.selectedTab == ApplicationType.ESPARCIDO, onClick = { onTabSelected(ApplicationType.ESPARCIDO) }, text = { Text("Esparcido") })
            }

            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = { Text("Buscar productos") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearchQuery) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    }
                )
                // Conteo de resultados listados
                Text(
                    text = "${uiState.filteredProducts.size} productos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (uiState.searchQuery.isNotEmpty() && uiState.filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron productos para '${uiState.searchQuery}'", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                    }
                } else if (uiState.filteredProducts.isEmpty()) {
                    EmptyState("No hay productos", "Añade tu primer producto usando el botón '+'", Icons.Default.Science)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.filteredProducts, key = { it.id }) { product ->
                            ProductItem(
                                product = product,
                                onClick = { onProductClick(product.id) },
                                onLongClick = { showEditDeleteDialog = product }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        when (uiState.selectedTab) {
            ApplicationType.PULVERIZACION -> AddPulverizacionProductDialog(
                onDismiss = { showAddDialog = false },
                onSave = { nombre, principio, tipo, formulacionId ->
                    onSaveProduct(Product(
                        nombreComercial = nombre, principioActivo = principio, tipo = tipo,
                        formulacionId = formulacionId, applicationType = ApplicationType.PULVERIZACION.name
                    ))
                    showAddDialog = false
                },
                formulaciones = uiState.formulaciones,
                tipos = listOf("Herbicida", "Insecticida", "Fungicida", "Fertilizante", "Coadyuvante", "PGR", "Bactericida", "Defoliante", "Desecante", "Acaricida", "Nematicida", "Otros")
            )
            ApplicationType.ESPARCIDO -> AddEsparcidoProductDialog(
                onDismiss = { showAddDialog = false },
                onSave = { nombre, tipo ->
                    onSaveProduct(Product(
                        nombreComercial = nombre, tipo = tipo, applicationType = ApplicationType.ESPARCIDO.name
                    ))
                    showAddDialog = false
                },
                tipos = listOf("Semilla", "Fertilizante", "Cebo")
            )

            ApplicationType.AMBOS -> TODO()
        }
    }
    showEditDeleteDialog?.let { product ->
        EditDeleteDialog(
            product = product,
            onDismiss = { showEditDeleteDialog = null },
            onDelete = { onDeleteProduct(product); showEditDeleteDialog = null }
        )
    }

 }

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Catálogo de Productos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Gestiona tu catálogo completo de productos agrícolas:")
                Text("• Pulverización: Productos para aplicación líquida con formulaciones específicas.")
                Text("• Esparcido: Productos para aplicación directa (semillas, fertilizantes).")
                Text("• Vademécum SENASA: Base de datos oficial con +6000 productos registrados.")
                Text("• Búsqueda: Por nombre comercial, principio activo o número de registro SENASA.")
                Text("• Formulaciones: Gestiona el orden de mezcla en pulverizaciones.")
                Text("Usa el botón (+) para agregar productos personalizados.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItem(product: Product, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bandaColor = toxicBandColor(product.bandaToxicologica, defaultColor = Color.Transparent)
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(10.dp).fillMaxHeight().background(bandaColor))
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Text(text = product.nombreComercial, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                if (product.applicationType == ApplicationType.PULVERIZACION.name) {
                    Text(text = product.principioActivo ?: "Sin principio activo", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(text = product.tipo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun EditDeleteDialog(product: Product, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Producto") },
        text = { Text("¿Está seguro de que desea eliminar ${product.nombreComercial}?") },
        confirmButton = { Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPulverizacionProductDialog(onDismiss: () -> Unit, onSave: (String, String, String, Int) -> Unit, formulaciones: List<Formulacion>, tipos: List<String>) {
    var nombreComercial by remember { mutableStateOf("") }
    var principioActivo by remember { mutableStateOf("") }
    var selectedFormulacion by remember { mutableStateOf<Formulacion?>(null) }
    var selectedTipo by remember { mutableStateOf("") }
    var isTipoExpanded by remember { mutableStateOf(false) }
    var isFormulacionExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Producto de Pulverización", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = nombreComercial, onValueChange = { nombreComercial = it }, label = { Text("Nombre Comercial") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = principioActivo, onValueChange = { principioActivo = it }, label = { Text("Principio Activo") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = isTipoExpanded, onExpandedChange = { isTipoExpanded = it }) {
                    OutlinedTextField(value = selectedTipo, onValueChange = {}, readOnly = true, label = { Text("Tipo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTipoExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = isTipoExpanded, onDismissRequest = { isTipoExpanded = false }) {
                        tipos.forEach { tipo -> DropdownMenuItem(text = { Text(tipo) }, onClick = { selectedTipo = tipo; isTipoExpanded = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = isFormulacionExpanded, onExpandedChange = { isFormulacionExpanded = it }) {
                    OutlinedTextField(value = selectedFormulacion?.nombre ?: "", onValueChange = {}, readOnly = true, label = { Text("Formulación") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFormulacionExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = isFormulacionExpanded, onDismissRequest = { isFormulacionExpanded = false }) {
                        formulaciones.forEach { formulacion -> DropdownMenuItem(text = { Text(formulacion.nombre) }, onClick = { selectedFormulacion = formulacion; isFormulacionExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = { if (nombreComercial.isNotBlank() && principioActivo.isNotBlank() && selectedTipo.isNotBlank() && selectedFormulacion != null) { onSave(nombreComercial, principioActivo, selectedTipo, selectedFormulacion!!.id) } }) { Text("Guardar") }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEsparcidoProductDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit, tipos: List<String>) {
    var nombreComercial by remember { mutableStateOf("") }
    var selectedTipo by remember { mutableStateOf("") }
    var isTipoExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Producto de Esparcido", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = nombreComercial, onValueChange = { nombreComercial = it }, label = { Text("Nombre Comercial") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = isTipoExpanded, onExpandedChange = { isTipoExpanded = it }) {
                    OutlinedTextField(value = selectedTipo, onValueChange = {}, readOnly = true, label = { Text("Tipo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTipoExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = isTipoExpanded, onDismissRequest = { isTipoExpanded = false }) {
                        tipos.forEach { tipo -> DropdownMenuItem(text = { Text(tipo) }, onClick = { selectedTipo = tipo; isTipoExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = { if (nombreComercial.isNotBlank() && selectedTipo.isNotBlank()) { onSave(nombreComercial, selectedTipo) } }) { Text("Guardar") }
                }
            }
        }
    }
}
