package com.example.allote.ui.parametros

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametrosScreen(
    uiState: ParametrosUiState,
    onSave: (Double?, Double?, Double?, Double?, Double?, String?, Double?) -> Unit,
    onBackPressed: () -> Unit
) {
    var isInEditMode by remember(uiState.parametros) {
        mutableStateOf(uiState.parametros?.dosis == null)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Parámetros del Trabajo") }) },
        floatingActionButton = {
            if (isInEditMode) {
                // El FAB de guardar se manejará desde dentro del formulario
            } else {
                FloatingActionButton(
                    onClick = { isInEditMode = true }
                ) {
                    Icon(Icons.Default.Edit, "Editar Parámetros")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.job == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Error: Trabajo no encontrado.") }
        } else {
            AnimatedVisibility(
                visible = !isInEditMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ParametrosSummaryCard(
                    uiState = uiState,
                    modifier = Modifier.padding(padding),
                    onLongClick = { isInEditMode = true }
                )
            }
            AnimatedVisibility(
                visible = isInEditMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ParametrosEditForm(
                    uiState = uiState,
                    modifier = Modifier.padding(padding),
                    onSave = onSave,
                    onBackPressed = onBackPressed
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParametrosSummaryCard(
    uiState: ParametrosUiState,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit
) {
    val p = uiState.parametros
    Column(modifier = modifier.padding(16.dp).combinedClickable(onClick = {}, onLongClick = onLongClick)) {
        Text("Parámetros Guardados", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
        Text("Mantén presionado para editar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                p?.dosis?.let { InfoRow(Icons.Default.Science, "Dosis", "$it ${if(uiState.isSolidApplication) "Kg/ha" else "L/ha"}") }
                InfoRow(Icons.Default.Straighten, "Interlineado", "${p?.interlineado ?: "N/A"} m")
                InfoRow(Icons.AutoMirrored.Filled.TrendingUp, "Velocidad", "${p?.velocidad ?: "N/A"} km/h")
                InfoRow(Icons.Default.Height, "Altura", "${p?.altura ?: "N/A"} m")
                if (uiState.isSolidApplication) {
                    InfoRow(Icons.Default.Album, "Disco Utilizado", p?.discoUtilizado ?: "N/A")
                    InfoRow(Icons.Default.Sync, "Revoluciones", "${p?.revoluciones ?: "N/A"} RPM")
                } else {
                    InfoRow(Icons.Default.Grain, "Tamaño de Gota", "${p?.tamanoGota ?: "N/A"} μm")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParametrosEditForm(
    uiState: ParametrosUiState,
    modifier: Modifier = Modifier,
    onSave: (Double?, Double?, Double?, Double?, Double?, String?, Double?) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var dosis by remember(uiState.parametros) { mutableStateOf(uiState.parametros?.dosis?.toString() ?: "") }
    var tamanoGota by remember(uiState.parametros) { mutableStateOf(uiState.parametros?.tamanoGota?.toString() ?: "") }
    var interlineado by remember(uiState.parametros) { mutableStateOf(uiState.parametros?.interlineado?.toString() ?: "") }
    var velocidad by remember(uiState.parametros) { mutableStateOf(uiState.parametros?.velocidad?.toString() ?: "") }
    var altura by remember(uiState.parametros) { mutableStateOf(uiState.parametros?.altura?.toString() ?: "") }
    var revoluciones by remember(uiState.parametros) { mutableStateOf(uiState.parametros?.revoluciones?.toString() ?: "") }
    var discoUtilizado by remember(uiState.parametros) { mutableStateOf(uiState.parametros?.discoUtilizado ?: "") }
    var expandedDisco by remember { mutableStateOf(false) }
    val discoOptions = listOf("Disco grande", "Disco chico")

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val dosisUnit = if (uiState.isSolidApplication) "Kg/ha" else "L/ha"
            OutlinedTextField(
                value = dosis,
                onValueChange = { dosis = it },
                label = { Text("Dosis ($dosisUnit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Default.Science, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Parámetros de Vuelo", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = interlineado, onValueChange = { interlineado = it }, label = { Text("Interlineado (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = velocidad, onValueChange = { velocidad = it }, label = { Text("Velocidad (km/h)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = altura, onValueChange = { altura = it }, label = { Text("Altura (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                }
            }
            if (uiState.isSolidApplication) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Parámetros de Esparcido", style = MaterialTheme.typography.titleMedium)
                        ExposedDropdownMenuBox(expanded = expandedDisco, onExpandedChange = { expandedDisco = it }) {
                            OutlinedTextField(value = discoUtilizado, onValueChange = {}, readOnly = true, label = { Text("Disco Utilizado") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDisco) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = expandedDisco, onDismissRequest = { expandedDisco = false }) {
                                discoOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { discoUtilizado = option; expandedDisco = false })
                                }
                            }
                        }
                        OutlinedTextField(value = revoluciones, onValueChange = { revoluciones = it }, label = { Text("Revoluciones (RPM)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Parámetros de Pulverización", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = tamanoGota, onValueChange = { tamanoGota = it }, label = { Text("Tamaño de Gota (μm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        ExtendedFloatingActionButton(
            text = { Text("Guardar Parámetros") },
            icon = { Icon(Icons.Default.Save, null) },
            onClick = {
                onSave(
                    dosis.toDoubleOrNull(), tamanoGota.toDoubleOrNull(), interlineado.toDoubleOrNull(),
                    velocidad.toDoubleOrNull(), altura.toDoubleOrNull(),
                    if (uiState.isSolidApplication) discoUtilizado else null,
                    revoluciones.toDoubleOrNull()
                )
                Toast.makeText(context, "Parámetros guardados", Toast.LENGTH_SHORT).show()
                onBackPressed()
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text = "$label:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}