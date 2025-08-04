package com.example.allote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateToFormulaciones: () -> Unit,
    onSavePrice: (String, String) -> Unit,
    onSaveCurrency: (String) -> Unit,
    onSaveExchangeRate: (String) -> Unit,
    onUpdateRateFromApi: () -> Unit
) {
    var showDialogFor by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showDialogFor == "prices") {
        PricesScreen(
            uiState = uiState,
            onDismiss = { showDialogFor = null },
            onPriceChange = onSavePrice
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Configuración") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Moneda", style = MaterialTheme.typography.titleMedium)
                TextPreference(
                    title = "Moneda de Visualización",
                    summary = uiState.currencySettings.displayCurrency,
                    onClick = { showDialogFor = "currency" }
                )
                if (uiState.currencySettings.displayCurrency == "ARS") {
                    TextPreference(
                        title = "Tasa de Cambio (1 USD -> ARS)",
                        summary = uiState.currencySettings.exchangeRate.toString(),
                        onClick = { showDialogFor = "rate" }
                    )

                    Button(
                        onClick = onUpdateRateFromApi,
                        enabled = !uiState.isUpdatingRate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        if (uiState.isUpdatingRate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Actualizar Tasa Automáticamente (Dólar Blue)")
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Precios y Parámetros", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                TextPreference(
                    title = "Precios por Tipo de Aplicación",
                    summary = getPricesSummary(uiState.precioLiquida, uiState.precioSolida, uiState.precioMixta, uiState.precioVarias),
                    onClick = { showDialogFor = "prices" }
                )
                TextPreference(
                    title = "Gestionar Formulaciones",
                    summary = "Editar, ordenar y crear nuevas formulaciones",
                    onClick = onNavigateToFormulaciones
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Información", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                TextPreference(
                    title = "Sobre esta App",
                    summary = "Conoce el propósito y las funciones principales",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    when (val dialogType = showDialogFor) {
        "currency" -> CurrencySelectionDialog(
            onDismiss = { showDialogFor = null },
            onSelect = { onSaveCurrency(it); showDialogFor = null }
        )
        "rate" -> PriceDialog(
            title = "Tasa de Cambio USD a ARS", value = uiState.currencySettings.exchangeRate.toString(),
            onConfirm = { onSaveExchangeRate(it); showDialogFor = null },
            onDismiss = { showDialogFor = null }, isNumeric = true
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sobre esta App") }, // Puedes cambiar "AgriManage" por el nombre real de tu app
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Esta aplicación está diseñada para ser una herramienta integral de gestión para contratistas agrícolas, permitiendo un control total sobre trabajos, clientes y finanzas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Funciones Principales:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(modifier = Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Gestión de Trabajos y Clientes: Centraliza la información de tus clientes y todos los trabajos asociados a ellos, desde la planificación hasta la finalización.")
                    Text("• Planificación Precisa: Crea recetas de aplicación detalladas, gestiona el orden de mezcla de productos y divide los trabajos en lotes para un seguimiento exacto.")
                    Text("• Control de Ejecución: Registra la superficie real trabajada en cada lote y calcula automáticamente el sobrante de insumos, optimizando tu inventario.")
                    Text("• Administración Financiera: Genera los costos de cada trabajo y lleva un control de la cuenta corriente de cada cliente, registrando pagos y deudas.")
                    Text("• Personalización: Adapta la app a tu negocio configurando precios por tipo de aplicación, moneda de visualización y tasas de cambio.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun CurrencySelectionDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Moneda") },
        text = {
            Column {
                Row(Modifier.fillMaxWidth().clickable { onSelect("USD") }.padding(vertical = 12.dp)) { Text("USD (Dólar Estadounidense)") }
                Row(Modifier.fillMaxWidth().clickable { onSelect("ARS") }.padding(vertical = 12.dp)) { Text("ARS (Peso Argentino)") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onPriceChange: (String, String) -> Unit
) {
    var showDialogFor by remember { mutableStateOf<String?>(null) }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Configurar Precios") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") } }
            )
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                TextPreference("Aplicación Líquida", if (uiState.precioLiquida.isNotEmpty()) "${uiState.precioLiquida}/Ha" else "No establecido") { showDialogFor = "liquida" }
                HorizontalDivider()
                TextPreference("Aplicación Sólida", if (uiState.precioSolida.isNotEmpty()) "${uiState.precioSolida}/Ha" else "No establecido") { showDialogFor = "solida" }
                HorizontalDivider()
                TextPreference("Aplicación Mixta", if (uiState.precioMixta.isNotEmpty()) "${uiState.precioMixta}/Ha" else "No establecido") { showDialogFor = "mixta" }
                HorizontalDivider()
                TextPreference("Aplicaciones Varias", if (uiState.precioVarias.isNotEmpty()) "${uiState.precioVarias}/Ha" else "No establecido") { showDialogFor = "varias" }
            }
        }
    }

    showDialogFor?.let { key ->
        val initialValue = when (key) {
            "liquida" -> uiState.precioLiquida
            "solida" -> uiState.precioSolida
            "mixta" -> uiState.precioMixta
            "varias" -> uiState.precioVarias
            else -> ""
        }
        PriceDialog(
            title = "Precio Aplicación ${key.replaceFirstChar { it.uppercase() }}",
            value = initialValue,
            onConfirm = { newValue -> onPriceChange(key, newValue); showDialogFor = null },
            onDismiss = { showDialogFor = null },
            isNumeric = true
        )
    }
}

@Composable
fun PriceDialog(
    title: String,
    value: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isNumeric: Boolean,
    onValueChange: ((String) -> Unit)? = null
) {
    var tempValue by remember { mutableStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = tempValue,
                onValueChange = { newValue ->
                    if (isNumeric && (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*\$")))) {
                        tempValue = newValue
                        onValueChange?.invoke(newValue)
                    } else if (!isNumeric) {
                        tempValue = newValue
                        onValueChange?.invoke(newValue)
                    }
                },
                label = { Text("Valor") },
                singleLine = true,
                keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { onConfirm(tempValue) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun TextPreference(title: String, summary: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun getPricesSummary(liquida: String, solida: String, mixta: String, varias: String): String {
    val count = listOf(liquida, solida, mixta, varias).count { it.isNotEmpty() }
    return if (count == 0) "No configurado" else "$count de 4 tipos configurados"
}
