package com.example.allote.ui.administracion

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.allote.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministracionScreen(
    uiState: AdministracionUiState,
    onCostoChange: (String) -> Unit,
    onHectareasChange: (String) -> Unit,
    onIvaChange: (Boolean) -> Unit,
    onCalcularClick: () -> Unit,
    onGuardarClick: () -> Unit
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración del Trabajo") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                JobHeaderCard(
                    description = uiState.job?.description ?: "N/A",
                    hectareasText = uiState.hectareasText,
                    onHectareasChange = onHectareasChange
                )
                CalculationAndActionsCard(
                    uiState = uiState,
                    onCostoChange = onCostoChange,
                    onIvaChange = onIvaChange,
                    onCalcularClick = onCalcularClick,
                    onGuardarClick = onGuardarClick
                )
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Administración del Trabajo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Esta pantalla genera el movimiento contable (la 'factura') para este trabajo.")
                Text("1. Hectáreas a Facturar: Confirma o ajusta la superficie final del trabajo.")
                Text("2. Costo por Hectárea: Ingresa el precio acordado.")
                Text("3. IVA: Marca la casilla si el trabajo lleva IVA.")
                Text("4. Calcular: Muestra un resumen de los totales sin guardar.")
                Text("5. Guardar: Confirma los montos y crea el movimiento en la contabilidad del cliente y en la general.", style = MaterialTheme.typography.bodySmall)
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
private fun JobHeaderCard(
    description: String,
    hectareasText: String,
    onHectareasChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(description, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = hectareasText,
                onValueChange = onHectareasChange,
                label = { Text("Hectáreas a Facturar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CalculationAndActionsCard(
    uiState: AdministracionUiState,
    onCostoChange: (String) -> Unit,
    onIvaChange: (Boolean) -> Unit,
    onCalcularClick: () -> Unit,
    onGuardarClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Cálculo de Costos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.costoPorHectareaText,
                onValueChange = onCostoChange,
                label = { Text("Costo por Hectárea") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onIvaChange(!uiState.aplicarIVA) }) {
                Checkbox(checked = uiState.aplicarIVA, onCheckedChange = { onIvaChange(it) })
                Text("Aplicar IVA (10.5%)")
            }
            Spacer(Modifier.height(8.dp))
            val context = LocalContext.current
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCalcularClick, modifier = Modifier.weight(1f)) { Text("Calcular") }
                Button(onClick = {
                    onGuardarClick()
                    Toast.makeText(context, "Guardado exitosamente", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.weight(1f)) { Text("Guardar") }
            }

            if (uiState.showResults) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
                ResumenUI(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ResumenUI(uiState: AdministracionUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Detalles de Facturación",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        InfoRow(label = "Hectáreas:", value = "%.2f".format(uiState.hectareasText.toDoubleOrNull() ?: 0.0))
        InfoRow(label = "Costo por hectárea:", value = CurrencyFormatter.format(uiState.costoPorHectareaText.toDoubleOrNull() ?: 0.0, uiState.currencySettings))
        InfoRow(label = "IVA aplicado:", value = if (uiState.aplicarIVA) "Sí (10.5%)" else "No")
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        InfoRow(label = "Total sin IVA:", value = CurrencyFormatter.format(uiState.totalSinIVA, uiState.currencySettings), isHighlight = true)
        InfoRow(label = "Total con IVA:", value = CurrencyFormatter.format(uiState.totalConIVA, uiState.currencySettings), isHighlight = true)
    }
}

@Composable
private fun InfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if(isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = if(isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if(isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if(isHighlight) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    }
}